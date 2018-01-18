// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet;

import static openllet.OpenlletCmdOptionArg.NONE;
import static openllet.OpenlletCmdOptionArg.REQUIRED;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import com.clarkparsia.owlapi.explanation.MultipleExplanationGenerator;
import com.clarkparsia.owlapi.explanation.SatisfiabilityConverter;
import com.clarkparsia.owlapi.explanation.TransactionAwareSingleExpGen;
import com.clarkparsia.owlapi.explanation.io.ExplanationRenderer;
import com.clarkparsia.owlapi.explanation.util.ExplanationProgressMonitor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import openllet.atom.OpenError;
import openllet.core.utils.Timer;
import openllet.core.utils.progress.ConsoleProgressMonitor;
import openllet.core.utils.progress.ProgressMonitor;
import openllet.owlapi.OWL;
import openllet.owlapi.OWLAPILoader;
import openllet.owlapi.OntologyUtils;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.GlassBoxExplanation;
import openllet.owlapi.explanation.io.manchester.ManchesterSyntaxExplanationRenderer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

/**
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 * @author Markus Stocker
 */
public class OpenlletExplain extends OpenlletCmdApp
{
	private SatisfiabilityConverter _converter;
	/**
	 * inferences for which there was an error while generating the explanation
	 */
	private int _errorExpCount = 0;
	private OWLAPILoader _owlApiLoader;
	private int _maxExplanations = 1;
	private boolean _useBlackBox = false;
	private ProgressMonitor _monitor;
	/**
	 * inferences whose explanation contains more than on _axiom
	 */
	private int _multiAxiomExpCount = 0;
	/**
	 * inferences with multiple explanations
	 */
	private int _multipleExpCount = 0;

	private OpenlletReasoner _reasoner;
	private OWLEntity _name1;
	private OWLEntity _name2;
	private OWLObject _name3;

	public OpenlletExplain()
	{
		GlassBoxExplanation.setup();
	}

	@Override
	public String getAppId()
	{
		return "OpenlletExplain: Explains one or more inferences in a given ontology including ontology inconsistency";
	}

	@Override
	public String getAppCmd()
	{
		return "openllet explain " + getMandatoryOptions() + "[options] <file URI>...\n\n" + "The _options --unsat, --all-unsat, --inconsistent, --subclass, \n" + "--hierarchy, and --instance are mutually exclusive. By default \n " + "--inconsistent option is assumed. In the following descriptions \n" + "C, D, and i can be URIs or local names.";
	}

	@Override
	public OpenlletCmdOptions getOptions()
	{
		final OpenlletCmdOptions options = getGlobalOptions();

		options.add(getIgnoreImportsOption());

		OpenlletCmdOption option = new OpenlletCmdOption("unsat");
		option.setType("C");
		option.setDescription("Explain why the given class is unsatisfiable");
		option.setIsMandatory(false);
		option.setArg(REQUIRED);
		options.add(option);

		option = new OpenlletCmdOption("all-unsat");
		option.setDescription("Explain all unsatisfiable classes");
		option.setDefaultValue(false);
		option.setIsMandatory(false);
		option.setArg(NONE);
		options.add(option);

		option = new OpenlletCmdOption("inconsistent");
		option.setDescription("Explain why the ontology is inconsistent");
		option.setDefaultValue(false);
		option.setIsMandatory(false);
		option.setArg(NONE);
		options.add(option);

		option = new OpenlletCmdOption("hierarchy");
		option.setDescription("Print all explanations for the class hierarchy");
		option.setDefaultValue(false);
		option.setIsMandatory(false);
		option.setArg(NONE);
		options.add(option);

		option = new OpenlletCmdOption("subclass");
		option.setDescription("Explain why C is a subclass of D");
		option.setType("C,D");
		option.setIsMandatory(false);
		option.setArg(REQUIRED);
		options.add(option);

		option = new OpenlletCmdOption("instance");
		option.setDescription("Explain why i is an instance of C");
		option.setType("i,C");
		option.setIsMandatory(false);
		option.setArg(REQUIRED);
		options.add(option);

		option = new OpenlletCmdOption("property-value");
		option.setDescription("Explain why s has value o for property p");
		option.setType("s,p,o");
		option.setIsMandatory(false);
		option.setArg(REQUIRED);
		options.add(option);

		option = new OpenlletCmdOption("method");
		option.setShortOption("m");
		option.setType("glass | black");
		option.setDescription("Method that will be used to generate explanations");
		option.setDefaultValue("glass");
		option.setIsMandatory(false);
		option.setArg(REQUIRED);
		options.add(option);

		option = new OpenlletCmdOption("max");
		option.setShortOption("x");
		option.setType("positive integer");
		option.setDescription("Maximum number of generated explanations for each inference");
		option.setDefaultValue(1);
		option.setIsMandatory(false);
		option.setArg(REQUIRED);
		options.add(option);

		option = options.getOption("verbose");
		option.setDescription("Print detailed exceptions and messages about the progress");

		return options;
	}

	@Override
	public void parseArgs(final String[] args)
	{
		super.parseArgs(args);

		_maxExplanations = _options.getOption("max").getValueAsNonNegativeInteger();

		_owlApiLoader = (OWLAPILoader) getLoader("OWLAPI");

		getKB();

		_converter = new SatisfiabilityConverter(_owlApiLoader.getManager().getOWLDataFactory());

		_reasoner = _owlApiLoader.getReasoner();

		loadMethod();

		loadNames();
	}

	@Override
	public void run()
	{
		try
		{

			if (_name1 == null)
			{
				// Option --hierarchy
				verbose("Explain all the subclass relations in the ontology");
				explainClassHierarchy();
			}
			else
				if (_name2 == null)
				{
					if (((OWLClassExpression) _name1).isOWLNothing())
					{
						// Option --all-unsat
						verbose("Explain all the unsatisfiable classes");
						explainUnsatisfiableClasses();
					}
					else
					{
						// Option --inconsistent && --unsat C
						verbose("Explain unsatisfiability of " + _name1);
						explainUnsatisfiableClass((OWLClass) _name1);
					}
				}
				else
					if (_name3 != null)
					{
						// Option --property-value s,p,o
						verbose("Explain property assertion " + _name1 + " and " + _name2 + " and " + _name3);

						explainPropertyValue((OWLIndividual) _name1, (OWLProperty) _name2, _name3);
					}
					else
						if (_name1.isOWLClass() && _name2.isOWLClass())
						{
							// Option --subclass C,D
							verbose("Explain subclass relation between " + _name1 + " and " + _name2);

							explainSubClass((OWLClass) _name1, (OWLClass) _name2);
						}
						else
							if (_name1.isOWLNamedIndividual() && _name2.isOWLClass())
							{
								// Option --instance i,C
								verbose("Explain instance relation between " + _name1 + " and " + _name2);

								explainInstance((OWLIndividual) _name1, (OWLClass) _name2);
							}

			printStatistics();
		}
		catch (final OWLException e)
		{
			throw new OpenError(e);
		}
	}

	private void explainAxiom(final OWLAxiom axiom)
	{

		final MultipleExplanationGenerator expGen = new HSTExplanationGenerator(getSingleExplanationGenerator());
		final RendererExplanationProgressMonitor rendererMonitor = new RendererExplanationProgressMonitor(axiom);
		expGen.setProgressMonitor(rendererMonitor);

		final OWLClassExpression unsatClass = _converter.convert(axiom);
		final Timer timer = _timers.startTimer("explain");
		final Set<Set<OWLAxiom>> explanations = expGen.getExplanations(unsatClass, _maxExplanations);
		timer.stop();

		if (explanations.isEmpty())
			rendererMonitor.foundNoExplanations();

		if (timer.getCount() % 10 == 0)
		{
			// printStatistics();
		}

		final int expSize = explanations.size();
		if (expSize == 0)
			_errorExpCount++;
		else
			if (expSize == 1)
			{
				if (explanations.iterator().next().size() > 1)
					_multiAxiomExpCount++;
				// else
				// return;
			}
			else
				_multipleExpCount++;
	}

	public void explainClassHierarchy() throws OWLException
	{
		final Set<OWLClass> visited = new HashSet<>();

		_reasoner.flush();

		startTask("Classification");
		_reasoner.getKB().classify();
		finishTask("Classification");

		startTask("Realization");
		_reasoner.getKB().realize();
		finishTask("Realization");

		_monitor = new ConsoleProgressMonitor();
		_monitor.setProgressTitle("Explaining");
		_monitor.setProgressLength((int) _reasoner.getRootOntology().classesInSignature().count());
		_monitor.taskStarted();

		final Node<OWLClass> bottoms = _reasoner.getEquivalentClasses(OWL.Nothing);
		explainClassHierarchy(OWL.Nothing, bottoms, visited);

		final Node<OWLClass> tops = _reasoner.getEquivalentClasses(OWL.Thing);
		explainClassHierarchy(OWL.Thing, tops, visited);

		_monitor.taskFinished();
	}

	public void explainEquivalentClass(final OWLClass c1, final OWLClass c2)
	{
		if (c1.equals(c2))
			return;

		final OWLAxiom axiom = OWL.equivalentClasses(c1, c2);

		explainAxiom(axiom);
	}

	public void explainInstance(final OWLIndividual ind, final OWLClass c)
	{
		if (c.isOWLThing())
			return;

		final OWLAxiom axiom = OWL.classAssertion(ind, c);

		explainAxiom(axiom);
	}

	// In the following method(s) we intentionally do not use OWLPropertyExpression<?,?>
	// because of a bug in some Sun's implementation of javac
	// http://bugs.sun.com/view_bug.do?bug_id=6548436
	// Since lack of generic type generates a warning, we suppress it
	public void explainPropertyValue(final OWLIndividual s, final OWLProperty p, final OWLObject o)
	{
		if (p.isOWLObjectProperty())
			explainAxiom(OWL.propertyAssertion(s, (OWLObjectProperty) p, (OWLIndividual) o));
		else
			explainAxiom(OWL.propertyAssertion(s, (OWLDataProperty) p, (OWLLiteral) o));
	}

	public void explainSubClass(final OWLClass sub, final OWLClass sup)
	{
		if (sub.equals(sup))
			return;

		if (sub.isOWLNothing())
			return;

		if (sup.isOWLThing())
			return;

		final OWLSubClassOfAxiom axiom = OWL.subClassOf(sub, sup);
		explainAxiom(axiom);
	}

	public void explainUnsatisfiableClasses()
	{
		for (final OWLClass cls : _reasoner.getEquivalentClasses(OWL.Nothing))
		{
			if (cls.isOWLNothing())
				continue;

			explainUnsatisfiableClass(cls);
		}
	}

	public void explainUnsatisfiableClass(final OWLClass cls)
	{
		explainSubClass(cls, OWL.Nothing);
	}

	private void explainClassHierarchy(final OWLClass cls, final Node<OWLClass> eqClasses, final Set<OWLClass> visited) throws OWLException
	{
		if (visited.contains(cls))
			return;

		visited.add(cls);
		visited.addAll(eqClasses.entities().collect(Collectors.toList()));

		for (final OWLClass eqClass : eqClasses)
		{
			_monitor.incrementProgress();

			explainEquivalentClass(cls, eqClass);
		}

		_reasoner.getInstances(cls, true).entities().forEach(ind -> explainInstance(ind, cls));

		final NodeSet<OWLClass> subClasses = _reasoner.getSubClasses(cls, true);
		final Map<OWLClass, Node<OWLClass>> subClassEqs = new HashMap<>();
		for (final Node<OWLClass> equivalenceSet : subClasses)
		{
			if (equivalenceSet.isBottomNode())
				continue;

			final OWLClass subClass = equivalenceSet.getRepresentativeElement();
			subClassEqs.put(subClass, equivalenceSet);
			explainSubClass(subClass, cls);
		}

		for (final Map.Entry<OWLClass, Node<OWLClass>> entry : subClassEqs.entrySet())
			explainClassHierarchy(entry.getKey(), entry.getValue(), visited);
	}

	private TransactionAwareSingleExpGen getSingleExplanationGenerator()
	{
		if (_useBlackBox)
		{
			if (_options.getOption("inconsistent") != null)
			{
				if (!_options.getOption("inconsistent").getValueAsBoolean())
					return new BlackBoxExplanation(_reasoner.getRootOntology(), OpenlletReasonerFactory.getInstance(), _reasoner);
				else
				{
					output("WARNING: black method cannot be used to explain inconsistency. Switching to glass.");
					return new GlassBoxExplanation(_reasoner);
				}
			}
			else
				return new BlackBoxExplanation(_reasoner.getRootOntology(), OpenlletReasonerFactory.getInstance(), _reasoner);
		}
		else
			return new GlassBoxExplanation(_reasoner);
	}

	private void loadMethod()
	{
		final String method = _options.getOption("method").getValueAsString();

		if (method.equalsIgnoreCase("black"))
			_useBlackBox = true;
		else
			if (method.equalsIgnoreCase("glass"))
				_useBlackBox = false;
			else
				throw new OpenlletCmdException("Unrecognized method: " + method);
	}

	private void loadNames()
	{
		OpenlletCmdOption option;

		_name1 = _name2 = null;
		_name3 = null;

		if ((option = _options.getOption("hierarchy")) != null)
			if (option.getValueAsBoolean())
				return;

		if ((option = _options.getOption("all-unsat")) != null)
			if (option.getValueAsBoolean())
			{
				_name1 = OWL.Nothing;
				return;
			}

		if ((option = _options.getOption("inconsistent")) != null)
			if (option.getValueAsBoolean())
			{
				if (_useBlackBox)
					throw new OpenlletCmdException("Black box method cannot be used to explain ontology inconsistency");
				_name1 = OWL.Thing;
				return;
			}

		if ((option = _options.getOption("unsat")) != null)
		{
			final String unsatisfiable = option.getValueAsString();
			if (unsatisfiable != null)
			{
				_name1 = OntologyUtils.findEntity(unsatisfiable, _owlApiLoader.allOntologies());

				if (_name1 == null)
					throw new OpenlletCmdException("Undefined entity: " + unsatisfiable);
				else
					if (!_name1.isOWLClass())
						throw new OpenlletCmdException("Not a defined class: " + unsatisfiable);
					else
						if (_name1.isTopEntity() && _useBlackBox)
							throw new OpenlletCmdException("Black box method cannot be used to explain unsatisfiability of owl:Thing");

				return;
			}
		}

		if ((option = _options.getOption("subclass")) != null)
		{
			final String subclass = option.getValueAsString();
			if (subclass != null)
			{
				final String[] names = subclass.split(",");
				if (names.length != 2)
					throw new OpenlletCmdException("Invalid format for subclass option: " + subclass);

				_name1 = OntologyUtils.findEntity(names[0], _owlApiLoader.allOntologies());
				_name2 = OntologyUtils.findEntity(names[1], _owlApiLoader.allOntologies());

				if (_name1 == null)
					throw new OpenlletCmdException("Undefined entity: " + names[0]);
				else
					if (!_name1.isOWLClass())
						throw new OpenlletCmdException("Not a defined class: " + names[0]);
				if (_name2 == null)
					throw new OpenlletCmdException("Undefined entity: " + names[1]);
				else
					if (!_name2.isOWLClass())
						throw new OpenlletCmdException("Not a defined class: " + names[1]);
				return;
			}
		}

		if ((option = _options.getOption("instance")) != null)
		{
			final String instance = option.getValueAsString();
			if (instance != null)
			{
				final String[] names = instance.split(",");
				if (names.length != 2)
					throw new OpenlletCmdException("Invalid format for instance option: " + instance);

				_name1 = OntologyUtils.findEntity(names[0], _owlApiLoader.allOntologies());
				_name2 = OntologyUtils.findEntity(names[1], _owlApiLoader.allOntologies());

				if (_name1 == null)
					throw new OpenlletCmdException("Undefined entity: " + names[0]);
				else
					if (!_name1.isOWLNamedIndividual())
						throw new OpenlletCmdException("Not a defined _individual: " + names[0]);
				if (_name2 == null)
					throw new OpenlletCmdException("Undefined entity: " + names[1]);
				else
					if (!_name2.isOWLClass())
						throw new OpenlletCmdException("Not a defined class: " + names[1]);

				return;
			}
		}

		if ((option = _options.getOption("property-value")) != null)
		{
			final String optionValue = option.getValueAsString();
			if (optionValue != null)
			{
				final String[] names = optionValue.split(",");
				if (names.length != 3)
					throw new OpenlletCmdException("Invalid format for property-value option: " + optionValue);

				_name1 = OntologyUtils.findEntity(names[0], _owlApiLoader.allOntologies());
				_name2 = OntologyUtils.findEntity(names[1], _owlApiLoader.allOntologies());

				if (_name1 == null)
					throw new OpenlletCmdException("Undefined entity: " + names[0]);
				else
					if (!_name1.isOWLNamedIndividual())
						throw new OpenlletCmdException("Not an _individual: " + names[0]);
				if (_name2 == null)
					throw new OpenlletCmdException("Undefined entity: " + names[1]);
				else
					if (!_name2.isOWLObjectProperty() && !_name2.isOWLDataProperty())
						throw new OpenlletCmdException("Not a defined property: " + names[1]);
				if (_name2.isOWLObjectProperty())
				{
					_name3 = OntologyUtils.findEntity(names[2], _owlApiLoader.allOntologies());
					if (_name3 == null)
						throw new OpenlletCmdException("Undefined entity: " + names[2]);
					else
						if (!(_name3 instanceof OWLIndividual))
							throw new OpenlletCmdException("Not a defined _individual: " + names[2]);
				}
				else
				{
					final ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
					parser.setStringToParse(names[2]);
					try
					{
						_name3 = parser.parseLiteral(null);
					}
					catch (final ParserException e)
					{
						throw new OpenlletCmdException("Not a valid literal: " + names[2], e);
					}
				}

				return;
			}
		}

		// Per default we explain why the ontology is inconsistent
		_name1 = OWL.Thing;
		if (_useBlackBox)
			throw new OpenlletCmdException("Black box method cannot be used to explain ontology inconsistency");

		return;
	}

	private void printStatistics()
	{
		if (!_verbose)
			return;

		final Timer timer = _timers.getTimer("explain");
		if (timer != null)
		{
			verbose("Subclass relations   : " + timer.getCount());
			verbose("Multiple explanations: " + _multipleExpCount);
			verbose("Single explanation     ");
			verbose(" with multiple axioms: " + _multiAxiomExpCount);
			verbose("Error explaining     : " + _errorExpCount);
			verbose("Average time         : " + timer.getAverage() + "ms");
		}
	}

	private class RendererExplanationProgressMonitor implements ExplanationProgressMonitor
	{

		private final ExplanationRenderer _rend = new ManchesterSyntaxExplanationRenderer();
		private final OWLAxiom _axiom;
		private final Set<Set<OWLAxiom>> _setExplanations;
		private final PrintWriter _pw;

		private RendererExplanationProgressMonitor(final OWLAxiom axiom)
		{
			_axiom = axiom;
			_pw = new PrintWriter(System.out);

			_setExplanations = new HashSet<>();
			try
			{
				_rend.startRendering(_pw);
			}
			catch (final OWLException e)
			{
				System.err.println("Error rendering explanation: " + e);
			}
			catch (final IOException e)
			{
				System.err.println("Error rendering explanation: " + e);
			}
		}

		@Override
		public void foundExplanation(final Set<OWLAxiom> axioms)
		{

			if (!_setExplanations.contains(axioms))
			{
				_setExplanations.add(axioms);
				_pw.flush();
				try
				{
					_rend.render(_axiom, Collections.singleton(axioms));
				}
				catch (final IOException e)
				{
					System.err.println("Error rendering explanation: " + e);
				}
				catch (final OWLException e)
				{
					System.err.println("Error rendering explanation: " + e);
				}
			}
		}

		@Override
		public boolean isCancelled()
		{
			return false;
		}

		@Override
		public void foundAllExplanations()
		{
			try
			{
				_rend.endRendering();
			}
			catch (final OWLException e)
			{
				System.err.println("Error rendering explanation: " + e);
			}
			catch (final IOException e)
			{
				System.err.println("Error rendering explanation: " + e);
			}
		}

		public void foundNoExplanations()
		{
			try
			{
				_rend.render(_axiom, Collections.<Set<OWLAxiom>> emptySet());
				_rend.endRendering();
			}
			catch (final OWLException e)
			{
				System.err.println("Error rendering explanation: " + e);
			}
			catch (final IOException e)
			{
				System.err.println("Error rendering explanation: " + e);
			}
		}
	}
}
