// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.owlapi.explanation;

import com.clarkparsia.owlapi.explanation.SingleExplanationGeneratorImpl;
import com.clarkparsia.owlapi.explanation.util.DefinitionTracker;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.aterm.ATermAppl;
import openllet.core.OpenlletOptions;
import openllet.core.taxonomy.TaxonomyUtils;
import openllet.core.utils.Pair;
import openllet.core.utils.SetUtils;
import openllet.owlapi.AxiomConverter;
import openllet.owlapi.OWL;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.PelletReasoner;
import openllet.shared.tools.Log;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLRuntimeException;

/**
 * <p>
 * Title: GlassBoxExplanation
 * </p>
 * <p>
 * Description: Implementation of SingleExplanationGenerator interface using the axiom tracing facilities of Pellet.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class GlassBoxExplanation extends SingleExplanationGeneratorImpl
{
	static
	{
		setup();
	}

	/**
	 * Very important initialization step that needs to be called once before a reasoner is created. this function will be called automatically when
	 * GlassBoxExplanation is loaded by the class loader.
	 */
	public static void setup()
	{
		// initialize PelletOptions to required values for explanations
		// to work before any Pellet reasoner instance is created
		OpenlletOptions.USE_TRACING = true;
	}

	public static final Logger _logger = Log.getLogger(GlassBoxExplanation.class);

	/**
	 * Alternative reasoner. We use a second reasoner because we do not want to lose the state in the original reasoner.
	 */
	private OpenlletReasoner _altReasoner = null;

	private boolean _altReasonerEnabled = false;

	private final AxiomConverter _axiomConverter;

	public GlassBoxExplanation(final OWLOntology ontology, final OpenlletReasonerFactory factory)
	{
		this(factory, factory.createReasoner(ontology));
	}

	public GlassBoxExplanation(final OpenlletReasoner reasoner)
	{
		this(new OpenlletReasonerFactory(), reasoner);
	}

	public GlassBoxExplanation(final OpenlletReasonerFactory factory, final OpenlletReasoner reasoner)
	{
		super(reasoner.getRootOntology(), factory, reasoner);

		_axiomConverter = new AxiomConverter(reasoner);
	}

	private void setAltReasonerEnabled(final boolean enabled)
	{
		if (enabled)
			if (_altReasoner == null)
			{
				_logger.fine("Create alt reasoner");
				_altReasoner = getReasonerFactory().createNonBufferingReasoner(getOntology());
			}

		_altReasonerEnabled = enabled;
	}

	private OWLClass getNegation(final OWLClassExpression desc)
	{
		if (!(desc instanceof OWLObjectComplementOf))
			return null;

		final OWLClassExpression not = ((OWLObjectComplementOf) desc).getOperand();
		if (not.isAnonymous())
			return null;

		return (OWLClass) not;
	}

	private Pair<OWLClass, OWLClass> getSubClassAxiom(final OWLClassExpression desc)
	{
		if (!(desc instanceof OWLObjectIntersectionOf))
			return null;

		final OWLObjectIntersectionOf conj = (OWLObjectIntersectionOf) desc;

		if (conj.operands().count() != 2)
			return null;

		final Iterator<OWLClassExpression> conjuncts = conj.operands().iterator();
		final OWLClassExpression c1 = conjuncts.next();
		final OWLClassExpression c2 = conjuncts.next();

		OWLClass sub = null;
		OWLClass sup = null;

		if (!c1.isAnonymous())
		{
			sub = (OWLClass) c1;
			sup = getNegation(c2);
		}
		else
			if (!c2.isAnonymous())
			{
				sub = (OWLClass) c2;
				sup = getNegation(c2);
			}

		if (sup == null)
			return null;

		return new Pair<>(sub, sup);
	}

	private Set<OWLAxiom> getCachedExplanation(final OWLClassExpression unsatClass)
	{
		final OpenlletReasoner pellet = getReasoner();

		if (!pellet.getKB().isClassified())
			return null;

		final Pair<OWLClass, OWLClass> pair = getSubClassAxiom(unsatClass);

		if (pair != null)
		{
			final Set<Set<ATermAppl>> exps = TaxonomyUtils.getSuperExplanations(pellet.getKB().getTaxonomy(), pellet.term(pair.first), pellet.term(pair.second));

			if (exps != null)
			{
				final Set<OWLAxiom> result = convertExplanation(exps.iterator().next());
				if (_logger.isLoggable(Level.FINE))
					_logger.fine("Cached explanation: " + result);
				return result;
			}
		}

		return null;
	}

	@Override
	public Set<OWLAxiom> getExplanation(final OWLClassExpression unsatClass)
	{
		Set<OWLAxiom> result = null;

		final boolean firstExplanation = isFirstExplanation();

		if (_logger.isLoggable(Level.FINE))
			_logger.fine("Explain: " + unsatClass + " " + "First: " + firstExplanation);

		if (firstExplanation)
		{
			_altReasoner = null;

			result = getCachedExplanation(unsatClass);

			if (result == null)
				result = getReasonerExplanation(unsatClass);
		}
		else
		{
			setAltReasonerEnabled(true);

			try
			{
				result = getReasonerExplanation(unsatClass);
			}
			catch (final RuntimeException e)
			{
				_logger.log(Level.SEVERE, "Unexpected error while trying to get explanation set", e);
				throw new OWLRuntimeException(e);
			}
			finally
			{
				setAltReasonerEnabled(false);
			}
		}

		return result;
	}

	private Set<OWLAxiom> getReasonerExplanation(final OWLClassExpression unsatClass)
	{
		final OpenlletReasoner reasoner = getReasoner();

		reasoner.getKB().prepare();

		// satisfiable if there is an undefined entity
		boolean sat = !getDefinitionTracker().isDefined(unsatClass);

		if (!sat)
			sat = isSatisfiable(reasoner, unsatClass, true);
		else
			_logger.fine(() -> "Undefined entity in " + unsatClass);

		if (sat)
			return Collections.emptySet();
		else
		{
			final Set<OWLAxiom> explanation = convertExplanation(reasoner.getKB().getExplanationSet());

			_logger.fine(() -> "Explanation " + explanation);

			final Set<OWLAxiom> prunedExplanation = pruneExplanation(unsatClass, explanation, true);

			final int prunedAxiomCount = explanation.size() - prunedExplanation.size();
			if (_logger.isLoggable(Level.FINE) && prunedAxiomCount > 0)
			{
				_logger.fine("Pruned " + prunedAxiomCount + " axioms from the explanation: " + SetUtils.difference(explanation, prunedExplanation));
				_logger.fine("New explanation " + prunedExplanation);
			}

			return prunedExplanation;
		}
	}

	private boolean isSatisfiable(final OpenlletReasoner pellet, final OWLClassExpression unsatClass, final boolean doExplanation)
	{
		pellet.getKB().setDoExplanation(doExplanation);
		final boolean sat = unsatClass.isOWLThing() ? pellet.isConsistent() : pellet.isSatisfiable(unsatClass);
		pellet.getKB().setDoExplanation(false);

		return sat;
	}

	private Set<OWLAxiom> convertExplanation(final Set<ATermAppl> explanation)
	{
		if (explanation == null || explanation.isEmpty())
			throw new OWLRuntimeException("No explanation computed");

		final Set<OWLAxiom> result = new HashSet<>();

		for (final ATermAppl term : explanation)
		{
			final OWLAxiom axiom = _axiomConverter.convert(term);
			if (axiom == null)
				throw new OWLRuntimeException("Cannot convert: " + term);
			result.add(axiom);
		}

		return result;
	}

	/**
	 * <p>
	 * Prunes the given explanation using slow pruning technique of BlackBox explanation. The explanation returned from Pellet axiom tracing is not guaranteed
	 * to be minimal so pruning is necessary to ensure minimality. The idea is to create an ontology with only the axioms in the explanation, remove an axiom,
	 * test satisfiability, and restore the axiom if the class turns to be satisfiable after the removal.
	 * <p>
	 * There are two different pruning techniques. Incremental pruning attaches the reasoner as a listener and updates the reasoner with axiom
	 * removals/restores. Non-incremental pruning clears the reasoner at each iteration and reloads the axioms from scratch each time. Incremental pruning is
	 * faster but may return incorrect answers since axiom updates are less robust.
	 */
	private Set<OWLAxiom> pruneExplanation(final OWLClassExpression unsatClass, final Set<OWLAxiom> explanation, final boolean incremental)
	{
		try
		{
			// initialize pruned explanation to be same as the given explanation
			final Set<OWLAxiom> prunedExplanation = new HashSet<>(explanation);

			// we can only prune if there is more than one axiom in the
			// explanation
			if (prunedExplanation.size() <= 1)
				return prunedExplanation;

			// create an ontology from the explanation axioms
			final OWLOntology debuggingOntology = OWL.Ontology(explanation);

			final DefinitionTracker defTracker = new DefinitionTracker(debuggingOntology);

			// since explanation size is generally small we can create and use a
			// completely new reasoner rather than destroying the state on already
			// existing reasoner
			OpenlletReasoner reasoner = getReasonerFactory().createNonBufferingReasoner(debuggingOntology);

			if (!defTracker.isDefined(unsatClass))
				_logger.warning("Some of the entities in " + unsatClass + " are not defined in the explanation " + explanation);

			if (isSatisfiable(reasoner, unsatClass, true))
				_logger.warning("Explanation incomplete: Concept " + unsatClass + " is satisfiable in the explanation " + explanation);

			// simply remove axioms one at a time. If the unsatClass turns
			// satisfiable then we know that axiom cannot be a part of minimal
			// explanation
			for (final OWLAxiom axiom : explanation)
			{
				_logger.finer(() -> "Try pruning " + axiom);

				if (!incremental)
					reasoner.dispose();

				debuggingOntology.remove(axiom);

				if (!incremental)
					reasoner = getReasonerFactory().createNonBufferingReasoner(debuggingOntology);

				reasoner.getKB().prepare();

				if (defTracker.isDefined(unsatClass) && !isSatisfiable(reasoner, unsatClass, false))
				{
					// does not affect satisfiability so remove from the results
					prunedExplanation.remove(axiom);

					_logger.finer(() -> "Pruned " + axiom);
				}
				else
					debuggingOntology.add(axiom); // affects satisfiability so add back to the ontology
			}

			if (incremental)
				// remove the listener and the ontology to avoid memory leaks
				reasoner.dispose();

			OWL._manager.removeOntology(debuggingOntology);
			OWL._manager.removeOntologyChangeListener(defTracker);

			return prunedExplanation;
		}
		catch (final OWLOntologyChangeException e)
		{
			throw new OWLRuntimeException(e);
		}
	}

	@Override
	public OpenlletReasoner getReasoner()
	{
		return _altReasonerEnabled ? _altReasoner : (PelletReasoner) super.getReasoner();
	}

	@Override
	public OpenlletReasonerFactory getReasonerFactory()
	{
		return (OpenlletReasonerFactory) super.getReasonerFactory();
	}

	@Override
	public void dispose()
	{
		getOntologyManager().removeOntologyChangeListener(getDefinitionTracker());
		if (_altReasoner != null)
			_altReasoner.dispose();
	}

	@Override
	public String toString()
	{
		return "GlassBox";
	}
}
