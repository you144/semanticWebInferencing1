package openllet.pellet.owlapi.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import openllet.core.rules.builtins.BuiltInRegistry;
import openllet.core.rules.builtins.FunctionBuiltIn;
import openllet.core.rules.builtins.NumericAdapter;
import openllet.core.rules.builtins.NumericFunction;
import openllet.core.utils.SetUtils;
import openllet.owlapi.OWL;
import openllet.owlapi.OWLGenericTools;
import openllet.owlapi.OWLHelper;
import openllet.owlapi.OWLManagerGroup;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.SWRL;
import openllet.owlapi.XSD;
import openllet.shared.tools.Log;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.SWRLIndividualArgument;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SWRLVariable;
import org.semanticweb.owlapi.vocab.OWLFacet;

/**
 * Test basic of openllet-owlapi
 *
 * @since 2.6.0
 */
public class TestBasic
{
	static
	{
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN");
		Log.setLevel(Level.WARNING, OWLGenericTools.class);
		Log._defaultLevel = Level.INFO;
	}

	private final OWLClass ClsA = OWL.Class("#ClsA");
	private final OWLClass ClsB = OWL.Class("#ClsB");
	private final OWLClass ClsC = OWL.Class("#ClsC");
	private final OWLClass ClsD = OWL.Class("#ClsD");
	private final OWLClass ClsE = OWL.Class("#ClsE");
	private final OWLClass ClsF = OWL.Class("#ClsF");
	private final OWLClass ClsG = OWL.Class("#ClsG");
	private final OWLNamedIndividual Ind1 = OWL.Individual("#Ind1");
	private final OWLObjectProperty propA = OWL.ObjectProperty("#mimiroux");
	private final OWLDataProperty propB = OWL.DataProperty("#propB");
	private final SWRLVariable varA = SWRL.variable(IRI.create("#a"));

	@Test
	public void rule() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.tests"), 1.0);

			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			owl.declareIndividual(ClsA, Ind1);

			{
				final List<OWLClass> entities = owl.getReasoner().getTypes(Ind1).entities().collect(Collectors.toList());
				assertTrue(entities.size() == 2);
				assertTrue(entities.contains(ClsA));
				assertTrue(entities.contains(OWL.Thing));
			}

			owl.addAxiom(SWRL.rule(//
					SWRL.antecedent(SWRL.classAtom(ClsA, varA)), //
					SWRL.consequent(SWRL.classAtom(ClsB, varA))//
			));

			{
				final List<OWLClass> entities = owl.getReasoner().getTypes(Ind1).entities().collect(Collectors.toList());
				assertTrue(entities.size() == 3);
				assertTrue(entities.contains(ClsA));
				assertTrue(entities.contains(ClsB));
				assertTrue(entities.contains(OWL.Thing));
			}

			owl.addAxiom(SWRL.rule(//
					SWRL.antecedent(SWRL.classAtom(ClsB, varA)), //
					SWRL.consequent(SWRL.classAtom(ClsC, varA))//
			));

			{
				final List<OWLClass> entities = owl.getReasoner().getTypes(Ind1).entities().collect(Collectors.toList());
				assertTrue(entities.size() == 4);
				assertTrue(entities.contains(ClsA));
				assertTrue(entities.contains(ClsB));
				assertTrue(entities.contains(ClsC));
				assertTrue(entities.contains(OWL.Thing));
			}

			owl.addClass(Ind1, ClsD);

			owl.addAxiom(SWRL.rule(//
					SWRL.antecedent(SWRL.classAtom(OWL.and(ClsD, ClsC), varA)), //
					SWRL.consequent(SWRL.classAtom(ClsE, varA))//
			));

			{
				final List<OWLClass> entities = owl.getReasoner().getTypes(Ind1).entities().collect(Collectors.toList());
				assertTrue(entities.size() == 6);
				assertTrue(entities.contains(ClsA));
				assertTrue(entities.contains(ClsB));
				assertTrue(entities.contains(ClsC));
				assertTrue(entities.contains(ClsD));
				assertTrue(entities.contains(ClsE));
				assertTrue(entities.contains(OWL.Thing));
			}

			owl.addClass(Ind1, OWL.not(ClsF)); // Mark the negation to enforce the open world assertion.

			final SWRLRule D_and_NotF = SWRL.rule(//
					SWRL.antecedent(SWRL.classAtom(OWL.and(ClsD, OWL.not(ClsF)), varA)), //
					SWRL.consequent(SWRL.classAtom(ClsG, varA))//
			);

			{
				owl.addAxiom(D_and_NotF);
				final List<OWLClass> entities = owl.getReasoner().getTypes(Ind1).entities().collect(Collectors.toList());
				assertTrue(entities.contains(ClsG));
				owl.removeAxiom(D_and_NotF);
			}

			final SWRLRule D_and_F = SWRL.rule(//
					SWRL.antecedent(SWRL.classAtom(OWL.and(ClsD, ClsF), varA)), //
					SWRL.consequent(SWRL.classAtom(ClsG, varA))//
			);

			{
				owl.addAxiom(D_and_F);
				final List<OWLClass> entities = owl.getReasoner().getTypes(Ind1).entities().collect(Collectors.toList());
				assertFalse(entities.contains(ClsG));
				owl.removeAxiom(D_and_F);
			}
		}
	}

	@Test
	public void incrementalStorage() throws OWLOntologyCreationException
	{
		final File file = new File("target/test.org#owlapi.inc.storage-test.org#owlapi.inc.storage_1.0.owl");

		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			group.setOntologiesDirectory(new File("target"));
			group.getPersistentManager();

			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.inc.storage"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, false);

			owl.addAxiom(OWL.declaration(ClsA));
			owl.addAxiom(OWL.declaration(ClsB));
			owl.addAxiom(OWL.propertyAssertion(Ind1, propA, Ind1));
			owl.addAxiom(OWL.propertyAssertion(Ind1, propB, OWL.constant(");alpha\"#\\\n \t\n\rbeta<xml></xml>")));

			// This test is good but a little too slow when building the project ten time a day.
			//			try
			//			{
			//				System.out.println("Waiting begin");
			//				Thread.sleep(65 * 1000);
			//				System.out.println("Waiting end");
			//			}
			//			catch (final Exception e)
			//			{
			//				e.printStackTrace();
			//			}
			group.flushIncrementalStorage();

			assertTrue(file.exists());
			file.delete();
			assertTrue(owl.getObject(Ind1, propA).get().getIRI().equals(Ind1.getIRI()));
		}

		final Set<OWLAxiom> expected = new HashSet<>();
		expected.add(OWL.declaration(ClsA));
		expected.add(OWL.declaration(ClsB));
		expected.add(OWL.propertyAssertion(Ind1, propA, Ind1));
		expected.add(OWL.propertyAssertion(Ind1, propB, OWL.constant(");alpha\"#\\\n \t\n\rbeta<xml></xml>")));

		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			group.setOntologiesDirectory(new File("target"));
			group.getPersistentManager();

			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.inc.storage"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, false);

			owl.addAxioms(expected.stream());
		} // Autoclose force a flush.

		try (final OWLManagerGroup group = new OWLManagerGroup()) // Force the manager to read the file.
		{
			assertTrue(file.exists());
			group.setOntologiesDirectory(new File("target"));
			group.getPersistentManager();

			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.inc.storage"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, false);

			final Set<OWLAxiom> set = owl.getOntology().axioms().collect(Collectors.toSet());
			assertTrue(expected.stream().allMatch(set::contains));
		} // Autoclose force a flush.

		{ // Delete the file.
			assertTrue(file.exists());
			file.delete();
		}
	}

	@Test
	public void testRegexRestriction() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.inc.regex.restriction"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			final OWLNamedIndividual x1 = OWL.Individual("#I1");
			final OWLNamedIndividual x2 = OWL.Individual("#I2");

			owl.addAxiom(OWL.equivalentClasses(ClsA, OWL.some(propB, OWL.restrict(XSD.STRING, OWL.facetRestriction(OWLFacet.PATTERN, OWL.constant("A.A"))))));
			owl.addAxiom(OWL.propertyAssertion(x1, propB, OWL.constant("AAA")));
			owl.addAxiom(OWL.propertyAssertion(x2, propB, OWL.constant("BBB")));

			owl.addAxiom(OWL.differentFrom(x1, x2));

			final OpenlletReasoner r = owl.getReasoner();
			assertTrue(r.isEntailed(OWL.classAssertion(x1, ClsA)));
			assertFalse(r.isEntailed(OWL.classAssertion(x2, ClsA)));
		}
	}

	@Test
	public void testMaxLengthRestriction() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.inc.maxLength.restriction"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			final OWLNamedIndividual x0 = OWL.Individual("#I0");
			final OWLNamedIndividual x1 = OWL.Individual("#I1");
			final OWLNamedIndividual x2 = OWL.Individual("#I2");

			owl.addAxiom(OWL.equivalentClasses(ClsA, OWL.some(propB, OWL.restrict(XSD.STRING, OWL.facetRestriction(OWLFacet.MAX_LENGTH, OWL.constant(3L))))));
			owl.addAxiom(OWL.propertyAssertion(x0, propB, OWL.constant("")));
			owl.addAxiom(OWL.propertyAssertion(x1, propB, OWL.constant("AA")));
			owl.addAxiom(OWL.propertyAssertion(x2, propB, OWL.constant("AAAAA")));

			owl.addAxiom(OWL.differentFrom(SetUtils.create(x0, x1, x2)));

			final OpenlletReasoner r = owl.getReasoner();
			assertTrue(r.isEntailed(OWL.classAssertion(x0, ClsA)));
			assertTrue(r.isEntailed(OWL.classAssertion(x1, ClsA)));
			assertFalse(r.isEntailed(OWL.classAssertion(x2, ClsA)));
		}
	}

	@Test
	public void testRestriction() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.inc.integer-float.restriction"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			final OWLNamedIndividual x1 = OWL.Individual("#I1");
			final OWLNamedIndividual x2 = OWL.Individual("#I2");

			owl.addAxiom(OWL.equivalentClasses(ClsA, OWL.some(propB, XSD.INTEGER)));
			owl.addAxiom(OWL.propertyAssertion(x1, propB, OWL.constant(1)));
			owl.addAxiom(OWL.propertyAssertion(x2, propB, OWL.constant(1.)));

			final OpenlletReasoner r = owl.getReasoner();
			assertTrue(r.isEntailed(OWL.classAssertion(x1, ClsA)));
			assertFalse(r.isEntailed(OWL.classAssertion(x2, ClsA)));
		}
	}

	@Test
	public void testOnlyProperties() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.only.properties"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			//			final OWLNamedIndividual x1 = OWL.Individual("#I1");
			//			final OWLNamedIndividual x2 = OWL.Individual("#I2");

			//owl.addAxiom(OWL.propertyAssertion(x1, propA, x2));
			owl.addAxiom(OWL.inverseProperties(OWL.ObjectProperty("#A"), OWL.ObjectProperty("#B")));
			//owl.addAxiom(OWL.declaration(ClsA));

			owl.getReasoner().getObjectPropertyDomains(OWL.ObjectProperty("#A"));
		} // The test is just about not crash.
	}

	@Test
	public void testAddAndRemove() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.add.remove"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			owl.addAxiom(OWL.declaration(OWL.DataProperty("#propA")));
			owl.addAxiom(OWL.declaration(OWL.Class("#clsA")));
			owl.addAxiom(OWL.equivalentClasses(OWL.Class("#clsA"), //
					OWL.value(OWL.DataProperty("#propA"), OWL.constant(12))//
			));
			assertTrue(owl.getReasoner().instances(OWL.Class("#clsA")).count() == 0);

			final OWLNamedIndividual x1 = OWL.Individual("#I1");

			owl.addAxiom(OWL.classAssertion(x1, OWL.Class("#clsA")));
			assertTrue(owl.getReasoner().instances(OWL.Class("#clsA")).count() == 1);

			owl.removeAxiom(OWL.classAssertion(x1, OWL.Class("#clsA")));
			assertTrue(owl.getReasoner().instances(OWL.Class("#clsA")).count() == 0);

		} // The test is just about not crash.
	}

	@Test
	public void testSubProperties() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.inc.properties"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			owl.addAxiom(OWL.subPropertyOf(OWL.ObjectProperty("#P2"), OWL.ObjectProperty("#P1"))); // p2 extends p1

			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I1"), OWL.ObjectProperty("#P1"), OWL.Individual("#I2")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I3"), OWL.ObjectProperty("#P2"), OWL.Individual("#I4")));

			assertFalse(owl.getObject(OWL.Individual("#I1"), OWL.ObjectProperty("#P2")).isPresent());
			assertTrue(owl.getObject(OWL.Individual("#I3"), OWL.ObjectProperty("#P1")).get().equals(OWL.Individual("#I4")));
		}
	}

	@Test
	public void testTransitiveSubProperties() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#owlapi.inc.transtive.properties"), 1.0);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);
			final OWLObjectProperty p1 = OWL.ObjectProperty("#P1");
			final OWLObjectProperty p2 = OWL.ObjectProperty("#P2");
			final OWLObjectProperty p3 = OWL.ObjectProperty("#P3");
			final OWLObjectProperty p4 = OWL.ObjectProperty("#P4");

			{
				owl.addAxiom(OWL.subPropertyOf(p1, p2)); // p2 extends [P1]
				final OWLObjectProperty[] chain = { p2, p1 };
				owl.addAxiom(OWL.subPropertyOf(chain, p2)); // p2 extends [P2, P1]
			}
			{
				owl.addAxiom(OWL.inverseProperties(p1, p3)); // p3 inverse of p1
			}
			{
				owl.addAxiom(OWL.subPropertyOf(p3, p4)); // p4 extends [P3]
				final OWLObjectProperty[] chain = { p4, p3 };
				owl.addAxiom(OWL.subPropertyOf(chain, p4)); // p4 extends [P4, P3]
			}

			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I1"), p1, OWL.Individual("#I2")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I2"), p1, OWL.Individual("#I3")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I3"), p1, OWL.Individual("#I4")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I4"), p1, OWL.Individual("#I5")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I5"), p1, OWL.Individual("#I6")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I6"), p1, OWL.Individual("#I7")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I7"), p1, OWL.Individual("#I8")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#I8"), p1, OWL.Individual("#IA")));
			owl.addAxiom(OWL.propertyAssertion(OWL.Individual("#IA"), p1, OWL.Individual("#IB")));

			assertTrue("direct", owl.getObjects(OWL.Individual("#I5"), p1).map(x -> x.toString()).sorted().collect(Collectors.joining("")).equals("<#I6>"));
			assertTrue("transitive", owl.getObjects(OWL.Individual("#I5"), p2).map(x -> x.toString()).sorted().collect(Collectors.joining("")).equals("<#I6><#I7><#I8><#IA><#IB>"));
			assertTrue("inverse", owl.getObjects(OWL.Individual("#I5"), p3).map(x -> x.toString()).sorted().collect(Collectors.joining("")).equals("<#I4>"));
			assertTrue("inverse transitive", owl.getObjects(OWL.Individual("#I5"), p4).map(x -> x.toString()).sorted((a, b) -> -a.compareTo(b)).collect(Collectors.joining("")).equals("<#I4><#I3><#I2><#I1>"));
		}
	}

	@Test
	public void testSwrlBuildInByVariable() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#swrl-build-in"), 1.00);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			final OWLDataProperty dpA = OWL.DataProperty("dpA");
			final OWLDataProperty dpB = OWL.DataProperty("dpB");
			final OWLNamedIndividual a = OWL.Individual("A");
			final OWLNamedIndividual b = OWL.Individual("B");
			final SWRLIndividualArgument swrlIndA = SWRL.individual(a);
			final SWRLIndividualArgument swrlIndB = SWRL.individual(b);
			final OWLLiteral ten = OWL.constant(10.);
			final OWLLiteral eleven = OWL.constant(11.);
			final SWRLVariable varX = SWRL.variable("x");
			final SWRLVariable varY = SWRL.variable("y");
			final SWRLLiteralArgument sup = SWRL.constant("sup");
			final SWRLLiteralArgument inf = SWRL.constant("inf");

			owl.addAxiom(OWL.propertyAssertion(a, dpA, ten));
			owl.addAxiom(OWL.propertyAssertion(b, dpA, eleven));
			owl.addAxiom(SWRL.rule(//
					SWRL.antecedent(//
							SWRL.propertyAtom(dpA, swrlIndA, varX), //
							SWRL.propertyAtom(dpA, swrlIndB, varY), //
							SWRL.greaterThan(varX, varY)), //
					SWRL.consequent(SWRL.propertyAtom(dpB, swrlIndA, sup))));

			owl.addAxiom(SWRL.rule(//
					SWRL.antecedent(//
							SWRL.propertyAtom(dpA, swrlIndA, varX), //
							SWRL.propertyAtom(dpA, swrlIndB, varY), //
							SWRL.lessThan(varX, varY)), //
					SWRL.consequent(SWRL.propertyAtom(dpB, swrlIndA, inf))));

			final OpenlletReasoner reasoner = owl.getReasoner();
			assertFalse(reasoner.isEntailed(OWL.propertyAssertion(a, dpB, OWL.constant("sup"))));
			assertTrue(reasoner.isEntailed(OWL.propertyAssertion(a, dpB, OWL.constant("inf"))));
		}
	}

	@Test
	public void testSwrlBuildInByConstants() throws OWLOntologyCreationException
	{
		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#swrl-build-in"), 1.01);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			final OWLDataProperty dpA = OWL.DataProperty("dpA");
			final OWLDataProperty dpB = OWL.DataProperty("dpB");
			final OWLNamedIndividual a = OWL.Individual("A");
			final OWLNamedIndividual b = OWL.Individual("B");
			final SWRLIndividualArgument swrlIndA = SWRL.individual(a);
			final OWLLiteral ten = OWL.constant(10.);
			final OWLLiteral eleven = OWL.constant(11.);
			final SWRLVariable varX = SWRL.variable("x");
			final SWRLLiteralArgument sup = SWRL.constant("sup");
			final SWRLLiteralArgument inf = SWRL.constant("inf");

			owl.addAxiom(OWL.propertyAssertion(a, dpA, ten));
			owl.addAxiom(OWL.propertyAssertion(b, dpA, eleven));
			owl.addAxiom(SWRL.rule(//
					SWRL.antecedent(//
							SWRL.propertyAtom(dpA, swrlIndA, varX), //
							SWRL.greaterThan(varX, SWRL.constant(11.))), //
					SWRL.consequent(SWRL.propertyAtom(dpB, swrlIndA, sup))));

			owl.addAxiom(SWRL.rule(//
					SWRL.antecedent(//
							SWRL.propertyAtom(dpA, swrlIndA, varX), //
							SWRL.lessThan(varX, SWRL.constant(11.))), //
					SWRL.consequent(SWRL.propertyAtom(dpB, swrlIndA, inf))));

			final OpenlletReasoner reasoner = owl.getReasoner();
			assertFalse(reasoner.isEntailed(OWL.propertyAssertion(a, dpB, OWL.constant("sup"))));
			assertTrue(reasoner.isEntailed(OWL.propertyAssertion(a, dpB, OWL.constant("inf"))));
		}
	}

	class RandomBuildIn implements NumericFunction
	{
		public volatile int _callCountBigDecimal = 0;
		public volatile int _callCountBigInteger = 0;
		public volatile int _callCountDouble = 0;
		public volatile int _callCountFloat = 0;
		public volatile Object _object = null;
		private final Random _rand = new Random();

		@Override
		public BigDecimal apply(final BigDecimal... args)
		{
			_callCountBigDecimal++;
			return (BigDecimal) (_object = new BigDecimal(_rand.nextFloat()));
		}

		@Override
		public BigInteger apply(final BigInteger... args)
		{
			_callCountBigInteger++;
			return (BigInteger) (_object = new BigInteger(Long.toString(_rand.nextInt()))); // I am using only a 'int' not an BigInt.
		}

		@Override
		public Double apply(final Double... args)
		{
			_callCountDouble++;
			return (Double) (_object = _rand.nextDouble());
		}

		@Override
		public Float apply(final Float... args)
		{
			_callCountFloat++;
			return (Float) (_object = _rand.nextFloat());
		}
	}

	@Test
	public void testSpecialBuitIn() throws OWLOntologyCreationException
	{
		final RandomBuildIn myRandomFunction = new RandomBuildIn();
		BuiltInRegistry.instance.registerBuiltIn("MyRandomFunction", new FunctionBuiltIn(new NumericAdapter(myRandomFunction)));

		try (final OWLManagerGroup group = new OWLManagerGroup())
		{
			final OWLOntologyID ontId = OWLHelper.getVersion(IRI.create("http://test.org#swrl-special-build-in"), 1.02);
			final OWLHelper owl = new OWLGenericTools(group, ontId, true);

			final OWLDataProperty property = OWL.DataProperty("property");
			final OWLNamedIndividual individual = OWL.Individual("individual");
			final OWLClass clazz = OWL.Class("clazz");

			final SWRLVariable varX = SWRL.variable("x");
			final SWRLVariable varY = SWRL.variable("y");

			owl.addAxiom(OWL.classAssertion(individual, clazz));
			// owl.addAxiom(OWL.range(property, XSD.DOUBLE)); // TODO : uncomment this to show a bug.

			owl.addAxiom(SWRL.rule(//
					SWRL.antecedent(//
							SWRL.classAtom(clazz, varX), //
							OWL._factory.getSWRLBuiltInAtom(IRI.create("MyRandomFunction"), Arrays.asList(varY))), //
					SWRL.consequent(//
							SWRL.propertyAtom(property, varX, varY))//
			));

			final Set<OWLLiteral> results = owl.getReasoner().getDataPropertyValues(individual, property);
			assertTrue(results.size() == 1);
			final OWLLiteral literal = results.iterator().next();
			assertTrue(literal.isInteger());
			assertTrue(0 == myRandomFunction._callCountBigDecimal);
			assertTrue(1 == myRandomFunction._callCountBigInteger); // Nothing in the code or spec specify to use this datatype.
			assertTrue(0 == myRandomFunction._callCountDouble);
			assertTrue(0 == myRandomFunction._callCountFloat);

			assertTrue(myRandomFunction._object.toString().equals(Integer.toString(literal.parseInteger())));
		}
	}
}
