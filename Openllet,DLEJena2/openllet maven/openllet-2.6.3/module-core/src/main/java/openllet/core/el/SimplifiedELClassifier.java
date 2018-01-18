// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.el;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.aterm.AFun;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.KnowledgeBase;
import openllet.core.boxes.rbox.Role;
import openllet.core.taxonomy.CDOptimizedTaxonomyBuilder;
import openllet.core.taxonomy.Taxonomy;
import openllet.core.taxonomy.TaxonomyImpl;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.CollectionUtils;
import openllet.core.utils.MultiValueMap;
import openllet.core.utils.TermFactory;
import openllet.core.utils.Timers;
import openllet.shared.tools.Log;

/**
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class SimplifiedELClassifier extends CDOptimizedTaxonomyBuilder
{
	@SuppressWarnings("hiding")
	public static final Logger _logger = Log.getLogger(SimplifiedELClassifier.class);

	private static class QueueElement
	{
		public final ConceptInfo _sub;
		public final ConceptInfo _sup;

		public QueueElement(final ConceptInfo sub, final ConceptInfo sup)
		{
			_sub = sub;
			_sup = sup;
		}
	}

	public final Timers timers = new Timers();

	private static final boolean PREPROCESS_DOMAINS = false;

	private static final boolean MATERIALIZE_SUPER_PROPERTIES = false;

	private final Queue<QueueElement> _primaryQueue = new LinkedList<>();
	private final Map<ATermAppl, ConceptInfo> _concepts = CollectionUtils.makeMap();
	private final MultiValueMap<ATermAppl, ConceptInfo> _existentials = new MultiValueMap<>();
	private final MultiValueMap<ConceptInfo, ConceptInfo> _conjunctions = new MultiValueMap<>();

	private ConceptInfo TOP;
	private ConceptInfo BOTTOM;
	private boolean _hasComplexRoles;
	private RoleChainCache _roleChains;
	private RoleRestrictionCache _roleRestrictions;

	public SimplifiedELClassifier(final KnowledgeBase kb)
	{
		super(kb);
	}

	@Override
	protected void reset()
	{
		super.reset();

		_hasComplexRoles = _kb.getExpressivity().hasTransitivity() || _kb.getExpressivity().hasComplexSubRoles();

		_primaryQueue.clear();
		_concepts.clear();
		_existentials.clear();
		_conjunctions.clear();

		_roleChains = new RoleChainCache(_kb);
		_roleRestrictions = new RoleRestrictionCache(_kb.getRBox());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean classify()
	{
		_logger.fine("Reset");
		reset();

		timers.execute("createConcepts", t -> createConcepts());

		final int queueSize = _primaryQueue.size();
		_monitor.setProgressTitle("Classifiying");
		_monitor.setProgressLength(queueSize);
		_monitor.taskStarted();

		timers.execute("processQueue", t -> processQueue());

		if (_logger.isLoggable(Level.FINER))
			print();

		_monitor.setProgress(queueSize);

		_taxonomyImpl = timers.execute("buildHierarchy", () -> new ELTaxonomyBuilder().build(_concepts));

		_monitor.taskFinished();

		return true;
	}

	private void addSuccessor(final ConceptInfo pred, final ATermAppl p, final ConceptInfo succ)
	{
		if (!pred.addSuccessor(p, succ))
			return;

		_logger.finer(() -> "Adding " + pred + " -> " + ATermUtils.toString(p) + " -> " + succ);

		if (succ == BOTTOM)
		{
			addToQueue(pred, BOTTOM);
			return;
		}

		for (final ConceptInfo supOfSucc : succ.getSuperClasses())
			addSuccessor(pred, p, supOfSucc);

		if (!RoleChainCache.isAnon(p))
			if (MATERIALIZE_SUPER_PROPERTIES)
			{
				if (_existentials.contains(p, succ))
				{
					final ATermAppl some = ATermUtils.makeSomeValues(p, succ.getConcept());
					addToQueue(pred, _concepts.get(some));
				}

				final Set<Role> superRoles = _kb.getRole(p).getSuperRoles();
				for (final Role superRole : superRoles)
					addSuccessor(pred, superRole.getName(), succ);
			}
			else
			{
				final Role pRole = _kb.getRole(p);
				if (null != pRole)
				{
					final Set<Role> superRoles = pRole.getSuperRoles();
					for (final Role superRole : superRoles)
						if (_existentials.contains(superRole.getName(), succ))
						{
							final ATermAppl some = ATermUtils.makeSomeValues(superRole.getName(), succ.getConcept());
							addToQueue(pred, _concepts.get(some));
						}
				}
			}

		if (!PREPROCESS_DOMAINS)
		{
			final ATermAppl propDomain = _roleRestrictions.getDomain(p);
			if (propDomain != null)
				addToQueue(pred, _concepts.get(propDomain));
		}

		if (_hasComplexRoles)
		{
			for (final Entry<ATermAppl, Set<ConceptInfo>> entry : CollectionUtils.makeList(pred.getPredecessors().entrySet()))
			{
				final ATermAppl predProp = entry.getKey();
				for (final ATermAppl supProp : _roleChains.getAllSuperRoles(predProp, p))
					for (final ConceptInfo predOfPred : CollectionUtils.makeList(entry.getValue()))
						addSuccessor(predOfPred, supProp, succ);
			}

			for (final Entry<ATermAppl, Set<ConceptInfo>> entry : CollectionUtils.makeList(succ.getSuccessors().entrySet()))
			{
				final ATermAppl succProp = entry.getKey();
				for (final ATermAppl supProp : _roleChains.getAllSuperRoles(p, succProp))
					for (final ConceptInfo succOfSucc : CollectionUtils.makeList(entry.getValue()))
						addSuccessor(pred, supProp, succOfSucc);
			}
		}
	}

	private void addToQueue(final ConceptInfo sub, final ConceptInfo sup)
	{
		if (sub.addSuperClass(sup))
		{
			_primaryQueue.add(new QueueElement(sub, sup));
			_logger.finer(() -> "Queue " + sub + " " + sup);
		}
	}

	private void addSuperClass(final ConceptInfo sub, final ConceptInfo sup)
	{
		_logger.finer(() -> "Adding " + sub + " < " + sup);

		if (sup == BOTTOM)
		{
			final Iterator<ConceptInfo> preds = sub.getPredecessors().flattenedValues();
			while (preds.hasNext())
				addToQueue(preds.next(), sup);
			return;
		}

		for (final ConceptInfo supOfSup : sup.getSuperClasses())
			if (!supOfSup.equals(sup))
				addToQueue(sub, supOfSup);

		final ATermAppl c = sup.getConcept();
		if (ATermUtils.isAnd(c))
		{
			ATermList list = (ATermList) c.getArgument(0);
			while (!list.isEmpty())
			{
				final ATermAppl conj = (ATermAppl) list.getFirst();

				addToQueue(sub, _concepts.get(conj));

				list = list.getNext();
			}
		}
		else
			if (ATermUtils.isSomeValues(c))
			{
				final ATermAppl p = (ATermAppl) c.getArgument(0);
				final ATermAppl qualification = (ATermAppl) c.getArgument(1);

				addSuccessor(sub, p, _concepts.get(qualification));
			}
			else
				assert ATermUtils.isPrimitive(c);

		final Set<ConceptInfo> referredConjunctions = _conjunctions.get(sup);
		if (referredConjunctions != null)
			for (final ConceptInfo conjunction : referredConjunctions)
			{
				ATermList list = (ATermList) conjunction.getConcept().getArgument(0);
				while (!list.isEmpty())
				{
					final ATermAppl conj = (ATermAppl) list.getFirst();

					if (!sub.hasSuperClass(_concepts.get(conj)))
						break;

					list = list.getNext();
				}

				if (list.isEmpty())
					addToQueue(sub, conjunction);
			}

		for (final Entry<ATermAppl, Set<ConceptInfo>> e : sub.getPredecessors().entrySet())
		{
			final ATermAppl prop = e.getKey();
			if (MATERIALIZE_SUPER_PROPERTIES)
			{
				if (_existentials.contains(prop, sup))
				{
					final ATermAppl some = ATermUtils.makeSomeValues(prop, c);
					for (final ConceptInfo pred : e.getValue())
						addToQueue(pred, _concepts.get(some));
				}
			}
			else
			{
				final Role role = _kb.getRole(prop);
				if (role != null)
				{
					final Set<Role> superRoles = role.getSuperRoles();
					for (final Role superRole : superRoles)
						if (_existentials.contains(superRole.getName(), sup))
						{
							final ATermAppl some = ATermUtils.makeSomeValues(superRole.getName(), c);
							for (final ConceptInfo pred : e.getValue())
								addToQueue(pred, _concepts.get(some));
						}
				}
			}
		}
	}

	private ConceptInfo createConcept(final ATermAppl cParam)
	{
		ATermAppl c = cParam;
		ConceptInfo concept = _concepts.get(c);
		if (concept == null)
		{
			concept = new ConceptInfo(c, _hasComplexRoles, false);

			if (ATermUtils.isAnd(c))
			{
				ATermList list = (ATermList) c.getArgument(0);
				for (; !list.isEmpty(); list = list.getNext())
				{
					final ATermAppl conj = (ATermAppl) list.getFirst();

					final ConceptInfo conjConcept = createConcept(conj);
					addToQueue(concept, conjConcept);

					_conjunctions.add(conjConcept, concept);
				}
			}
			else
				if (ATermUtils.isSomeValues(c))
				{
					final ATermAppl p = (ATermAppl) c.getArgument(0);
					final ATermAppl q = (ATermAppl) c.getArgument(1);

					if (ATermUtils.isInv(p))
						throw new UnsupportedOperationException("Anonmyous inverse found in restriction: " + ATermUtils.toString(c));

					final ATermAppl range = _roleRestrictions.getRange(p);
					if (range != null)
					{
						final ATermAppl newQ = ATermUtils.makeSimplifiedAnd(Arrays.asList(range, q));
						if (!newQ.equals(q))
						{
							final ATermAppl newC = ATermUtils.makeSomeValues(p, newQ);
							concept = createConcept(newC);
							_concepts.put(c, concept);
							c = newC;
						}
					}

					final ConceptInfo succ = createConcept(q);

					_existentials.add(p, succ);

					// Add this to the _queue so that successor relation some(p,q) -> p -> q will be established. Due to
					// _sub property interactions adding successor relation here directly causes missing inferences.
					// Note that, we are taking advantage of the fact that concept.addSuperClass(concept) call has not
					// been executed yet which would have caused the add _queue call to have no effect.
					addToQueue(concept, concept);
				}

			_concepts.put(c, concept);

			concept.addSuperClass(concept);

			if (TOP != null)
				addToQueue(concept, TOP);
		}

		return concept;
	}

	private void createConceptsFromAxiom(final ATermAppl sub, final ATermAppl sup)
	{
		addToQueue(createConcept(sub), createConcept(sup));
	}

	private void createDisjointAxiom(final ATermAppl c1, final ATermAppl c2)
	{
		createConcept(c1);
		createConcept(c2);

		final ATermAppl and = ATermUtils.makeSimplifiedAnd(Arrays.asList(c1, c2));
		createConceptsFromAxiom(and, ATermUtils.BOTTOM);
	}

	private void processAxiom(final ATermAppl axiom)
	{
		final AFun fun = axiom.getAFun();

		if (fun.equals(ATermUtils.DISJOINTSFUN))
		{
			ATermList concepts = (ATermList) axiom.getArgument(0);
			final int n = concepts.getLength();
			final ATermAppl[] simplified = new ATermAppl[n];
			for (int i = 0; !concepts.isEmpty(); concepts = concepts.getNext(), i++)
				simplified[i] = ELSyntaxUtils.simplify((ATermAppl) concepts.getFirst());
			for (int i = 0; i < n - 1; i++)
				for (int j = i + 1; j < n; j++)
					createDisjointAxiom(simplified[i], simplified[j]);
		}
		else
		{
			ATermAppl sub = (ATermAppl) axiom.getArgument(0);
			ATermAppl sup = (ATermAppl) axiom.getArgument(1);

			sub = ELSyntaxUtils.simplify(sub);
			sup = ELSyntaxUtils.simplify(sup);

			if (fun.equals(ATermUtils.SUBFUN))
				createConceptsFromAxiom(sub, sup);
			else
				if (fun.equals(ATermUtils.EQCLASSFUN))
				{
					createConceptsFromAxiom(sub, sup);
					createConceptsFromAxiom(sup, sub);
				}
				else
					if (fun.equals(ATermUtils.DISJOINTFUN))
						createDisjointAxiom(sub, sup);
					else
						throw new IllegalArgumentException("Axiom " + axiom + " is not EL.");
		}
	}

	private void processAxioms()
	{
		//EquivalentClass -> SubClasses
		//Disjoint Classes -> SubClass
		//Normalize ATerm lists to sets
		final Collection<ATermAppl> assertedAxioms = _kb.getTBox().getAssertedAxioms();
		for (final ATermAppl assertedAxiom : assertedAxioms)
			processAxiom(assertedAxiom);

		if (PREPROCESS_DOMAINS)
			//Convert ATermAppl Domains to axioms
			for (final Entry<ATermAppl, ATermAppl> entry : _roleRestrictions.getDomains().entrySet())
			{
				final ATermAppl roleName = entry.getKey();
				final ATermAppl domain = entry.getValue();
				createConceptsFromAxiom(ATermUtils.makeSomeValues(roleName, ATermUtils.TOP), domain);
			}

		//Convert Reflexive Roles to axioms
		for (final Role role : _kb.getRBox().getRoles().values())
			if (role.isReflexive())
			{
				final ATermAppl range = _roleRestrictions.getRange(role.getName());
				if (range == null)
					continue;

				createConceptsFromAxiom(ATermUtils.TOP, range);
			}
	}

	private void createConcepts()
	{
		TOP = createConcept(ATermUtils.TOP);
		BOTTOM = createConcept(ATermUtils.BOTTOM);

		for (final ATermAppl c : _kb.getClasses())
			createConcept(c);

		processAxioms();

		_logger.fine("Process domain and ranges");
		for (final ATermAppl c : _roleRestrictions.getRanges().values())
			createConcept(c);

		for (final ATermAppl c : _roleRestrictions.getDomains().values())
			createConcept(c);
	}

	public void print()
	{
		for (final ATermAppl c : _concepts.keySet())
			_logger.finer(c + " " + _concepts.get(c).getSuperClasses());
		_logger.finer("");
		_roleChains.print();
	}

	private void processQueue()
	{
		final int startingSize = _primaryQueue.size();
		while (!_primaryQueue.isEmpty())
		{
			final int processed = startingSize - _primaryQueue.size();
			if (_monitor.getProgress() < processed)
				_monitor.setProgress(processed);

			final QueueElement qe = _primaryQueue.remove();
			addSuperClass(qe._sub, qe._sup);
		}
	}

	@Override
	public Map<ATermAppl, Set<ATermAppl>> getToldDisjoints()
	{
		return Collections.emptyMap();
	}

	@Override
	public Taxonomy<ATermAppl> getToldTaxonomy()
	{
		return new TaxonomyImpl<>(_kb.getTBox().getClasses(), TermFactory.TOP, TermFactory.BOTTOM);
	}
}
