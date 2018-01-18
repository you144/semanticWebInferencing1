// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.inctest;

import static openllet.test.PelletTestCase.assertIteratorValues;
import static openllet.test.PelletTestCase.assertPropertyValues;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import junit.framework.JUnit4TestAdapter;
import openllet.core.OpenlletOptions;
import openllet.core.boxes.abox.ABox;
import openllet.core.utils.PropertiesBuilder;
import openllet.jena.PelletInfGraph;
import openllet.jena.PelletReasonerFactory;
import openllet.jena.test.AbstractJenaTests;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.IntersectionClass;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Unit tests for incremental consistency checking using Jena API.
 *
 * @author Christian Halaschek-Wiener
 * @author Evren Sirin
 */

@RunWith(Parameterized.class)
public class IncJenaConsistencyTests extends AbstractJenaTests
{
	@Parameterized.Parameters
	public static Collection<Object[]> getTestCases()
	{
		final ArrayList<Object[]> cases = new ArrayList<>();
		cases.add(new Object[] { false, false, false });
		cases.add(new Object[] { true, false, false });
		cases.add(new Object[] { true, true, false });
		cases.add(new Object[] { true, true, true });
		return cases;
	}

	private final Properties _newOptions;
	private Properties _oldOptions;

	public static junit.framework.Test suite()
	{
		return new JUnit4TestAdapter(IncJenaConsistencyTests.class);
	}

	public IncJenaConsistencyTests(final boolean ucq, final boolean uic, final boolean uid)
	{
		final PropertiesBuilder pb = new PropertiesBuilder();
		pb.set("USE_COMPLETION_QUEUE", String.valueOf(ucq));
		pb.set("USE_INCREMENTAL_CONSISTENCY", String.valueOf(uic));
		pb.set("USE_INCREMENTAL_DELETION", String.valueOf(uid));

		_newOptions = pb.build();
	}

	@Override
	@Before
	public void before()
	{
		_oldOptions = OpenlletOptions.setOptions(_newOptions);

		super.before();
	}

	@Override
	@After
	public void after()
	{
		OpenlletOptions.setOptions(_oldOptions);

		super.after();
	}

	@Test
	public void testTBoxChange()
	{
		final String ns = "http://www.example.org/test#";

		final OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		model.setStrictMode(false);

		final DatatypeProperty p = model.createDatatypeProperty(ns + "p");
		p.addRDFType(OWL.InverseFunctionalProperty);
		p.addRange(XSD.xboolean);

		final OntClass C = model.createClass(ns + "C");
		C.addSuperClass(model.createCardinalityRestriction(null, p, 1));

		final Individual i1 = model.createIndividual(ns + "i1", C);
		final Individual i2 = model.createIndividual(ns + "i2", C);
		final Individual i3 = model.createIndividual(ns + "i3", C);

		// check consistency
		model.prepare();

		final OntClass D = model.createClass(ns + "D");
		final OntClass E = model.createClass(ns + "E");
		D.addDisjointWith(E);

		// add _individual
		final Individual i4 = model.createIndividual(ns + "i4", D);

		final PelletInfGraph graph = (PelletInfGraph) model.getGraph();

		model.prepare();

		// check that the update occurred and that the incremental consistency
		// was used
		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT) == null);
		assertIteratorValues(model.listIndividuals(), new Resource[] { i1, i2, i3, i4 });

		i4.addRDFType(C);
		model.prepare();
		assertTrue(!OpenlletOptions.USE_INCREMENTAL_CONSISTENCY || graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() == 1);
	}

	@Ignore("This test is know to fail when the processing _order of disjoint axiom changes.")
	@Test
	public void testTypeAssertions()
	{
		final String ns = "http://www.example.org/test#";

		final OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		model.setStrictMode(false);

		final DatatypeProperty p = model.createDatatypeProperty(ns + "p");
		p.addRDFType(OWL.InverseFunctionalProperty);
		p.addRange(XSD.xboolean);

		final OntClass C = model.createClass(ns + "C");
		C.addSuperClass(model.createCardinalityRestriction(null, p, 1));

		final OntClass D = model.createClass(ns + "D");
		final OntClass E = model.createClass(ns + "E");
		D.addDisjointWith(E);

		final RDFList conj = model.createList(new RDFNode[] { D, C });
		final OntClass CONJ = model.createIntersectionClass(null, conj);

		final Individual i1 = model.createIndividual(ns + "i1", C);
		i1.addRDFType(D);
		final Individual i2 = model.createIndividual(ns + "i2", C);
		i2.addRDFType(D);
		final Individual i3 = model.createIndividual(ns + "i3", C);
		i3.addRDFType(E);

		// check consistency
		model.prepare();

		// add _individual
		final Individual i4 = model.createIndividual(ns + "i4", D);

		final PelletInfGraph graph = (PelletInfGraph) model.getGraph();
		model.prepare();

		// check that the update occurred and that the incremental consistency
		// was used
		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() > 0);
		assertIteratorValues(model.listIndividuals(), new Resource[] { i1, i2, i3, i4 });

		i4.addRDFType(model.createCardinalityRestriction(null, p, 1));
		graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).reset();

		model.prepare();

		// check that incremental consistency was not used
		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() == 0);

		i4.addRDFType(E);

		// check that the kb is now inconsistent and that incremental
		// consistency was used
		assertFalse(((PelletInfGraph) model.getGraph()).isConsistent());
		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() > 0);

		i4.removeRDFType(E);

		graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).reset();

		model.prepare();

		// check that the kb is now inconsistent and that incremental
		// consistency was used
		assertTrue(((PelletInfGraph) model.getGraph()).isConsistent());
		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() == 0);

		final ObjectProperty op = model.createObjectProperty(ns + "op");
		i2.addProperty(op, i4);

		model.prepare();

		// check that the kb is now inconsistent and that incremental
		// consistency was used
		assertTrue(((PelletInfGraph) model.getGraph()).isConsistent());
		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() == 0);

		i2.addRDFType(CONJ);

		model.prepare();

		// check that the kb is now inconsistent and that incremental
		// consistency was used
		assertTrue(((PelletInfGraph) model.getGraph()).isConsistent());
		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() == 0);
	}

	@Test
	public void testPropertyAssertions()
	{
		final String ns = "http://www.example.org/test#";

		final OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		model.setStrictMode(false);

		final DatatypeProperty dp = model.createDatatypeProperty(ns + "dp");

		final ObjectProperty op = model.createObjectProperty(ns + "op");

		final OntClass C = model.createClass(ns + "C");
		final Individual a = model.createIndividual(ns + "a", C);
		final Individual b = model.createIndividual(ns + "b", C);

		final Literal one = model.createTypedLiteral("1", TypeMapper.getInstance().getTypeByName(XSD.positiveInteger.getURI()));
		a.addProperty(dp, one);

		model.prepare();

		final PelletInfGraph graph = (PelletInfGraph) model.getGraph();

		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT) == null);
		assertIteratorValues(a.listPropertyValues(dp), new Literal[] { one });

		a.addProperty(op, b);

		// check consistency
		model.prepare();

		assertIteratorValues(a.listPropertyValues(op), new Resource[] { b });
		// check that the update occurred and that the incremental consistency
		// was used
		assertTrue(!OpenlletOptions.USE_INCREMENTAL_CONSISTENCY || graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() == 1);

		final Literal two = model.createTypedLiteral("2", TypeMapper.getInstance().getTypeByName(XSD.positiveInteger.getURI()));
		b.addProperty(dp, two);

		graph.getKB().isConsistent();

		assertIteratorValues(b.listPropertyValues(dp), new Literal[] { two });
		// check that the update occurred and that the incremental consistency
		// was used
		assertTrue(!OpenlletOptions.USE_INCREMENTAL_CONSISTENCY || graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() == 2);
	}

	@Test
	public void testBnodeUpdates()
	{
		final String ns = "http://www.example.org/test#";

		final OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		model.setStrictMode(false);

		final DatatypeProperty dp = model.createDatatypeProperty(ns + "dp");

		final ObjectProperty op = model.createObjectProperty(ns + "op");

		final OntClass C = model.createClass(ns + "C");
		final Individual anon1 = model.createIndividual(C);
		final Individual a = model.createIndividual(ns + "a", C);

		final Literal one = model.createTypedLiteral("1", TypeMapper.getInstance().getTypeByName(XSD.positiveInteger.getURI()));
		a.addProperty(dp, one);

		model.prepare();

		final PelletInfGraph graph = (PelletInfGraph) model.getGraph();

		assertTrue(graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT) == null);
		assertIteratorValues(a.listPropertyValues(dp), new Literal[] { one });

		a.addProperty(op, anon1);

		// check consistency
		model.prepare();

		assertIteratorValues(a.listPropertyValues(op), new Resource[] { anon1 });
		// check that the update occurred and that the incremental consistency
		// was used
		assertTrue(!OpenlletOptions.USE_INCREMENTAL_CONSISTENCY || graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() > 0);

		final Individual anon2 = model.createIndividual(C);
		anon2.addProperty(op, a);

		// check consistency
		model.prepare();

		assertIteratorValues(anon2.listPropertyValues(op), new Resource[] { a });
		// check that the update occurred and that the incremental consistency
		// was used
		assertTrue(!OpenlletOptions.USE_INCREMENTAL_CONSISTENCY || graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount() == 2);
	}

	@Test
	public void testAnonClasses()
	{
		assumeTrue(OpenlletOptions.USE_INCREMENTAL_CONSISTENCY);

		final OntModel ontmodel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);

		final String nc = "urn:test:";

		final OntClass class1 = ontmodel.createClass(nc + "C1");
		final OntClass class2 = ontmodel.createClass(nc + "C2");

		final Individual[] inds = new Individual[6];
		for (int j = 0; j < 6; j++)
			inds[j] = ontmodel.createIndividual(nc + "Ind" + j, OWL.Thing);

		inds[0].addRDFType(class1);
		inds[1].addRDFType(class1);
		inds[2].addRDFType(class1);
		inds[3].addRDFType(class1);

		inds[2].addRDFType(class2);
		inds[3].addRDFType(class2);
		inds[4].addRDFType(class2);
		inds[5].addRDFType(class2);

		ontmodel.prepare();

		assertIteratorValues(class1.listInstances(), new Resource[] { inds[0], inds[1], inds[2], inds[3] });

		assertIteratorValues(class2.listInstances(), new Resource[] { inds[2], inds[3], inds[4], inds[5] });

		final PelletInfGraph graph = (PelletInfGraph) ontmodel.getGraph();

		//assertTrue( graph.getKB().timers.getTimer( ABox.IS_INC_CONSISTENT ) == null );
		final long prevCount = graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT) == null ? 0 : graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount();

		inds[4].addRDFType(class1);
		inds[5].addRDFType(class1);

		ontmodel.prepare();

		assertIteratorValues(class1.listInstances(), new Resource[] { inds[0], inds[1], inds[2], inds[3], inds[4], inds[5] });

		assertTrue(prevCount < graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount());

		graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).reset();

		final RDFList list = ontmodel.createList(new RDFNode[] { class1, class2 });

		final IntersectionClass class3 = ontmodel.createIntersectionClass(null, list);

		final UnionClass class4 = ontmodel.createUnionClass(null, list);

		ontmodel.prepare();
		
		graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).reset();

		assertIteratorValues(class3.listInstances(), new Resource[] { inds[2], inds[3], inds[4], inds[5] });

		assertIteratorValues(class4.listInstances(), new Resource[] { inds[0], inds[1], inds[2], inds[3], inds[4], inds[5] });

		assertEquals(0, graph.getKB().getTimers().getTimer(ABox.IS_INC_CONSISTENT).getCount());

		final Individual newind = ontmodel.createIndividual(nc + "Ind7", class4);

		ontmodel.prepare();

		assertIteratorValues(class4.listInstances(), new Resource[] { inds[0], inds[1], inds[2], inds[3], inds[4], inds[5], newind });

	}

	@Test
	public void testSimpleTypeAssertion()
	{
		final String ns = "urn:test:";

		final OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);

		// add one instance relation
		final OntClass cls = model.createClass(ns + "C");
		final Individual a = model.createIndividual(ns + "a", cls);

		// load everything and check consistency
		assertTrue(model.validate().isValid());

		// add a type relation for an existing _individual
		a.addRDFType(cls);

		// verify instance relation
		assertTrue(model.contains(a, RDF.type, cls));
		assertIteratorValues(cls.listInstances(false), new Resource[] { a });
		// check for direct types to make sure we don't get results from base
		// graph
		assertIteratorValues(cls.listInstances(true), new Resource[] { a });

		// add a new instance relation to a new _individual
		final Individual b = model.createIndividual(ns + "b", cls);

		// verify inference
		assertTrue(model.contains(b, RDF.type, cls));
		assertIteratorValues(cls.listInstances(false), new Resource[] { a, b });
		// check for direct types to make sure we don't get results from base
		// graph
		assertIteratorValues(cls.listInstances(true), new Resource[] { a, b });
	}

	@Test
	public void testSimplePropertyAssertion()
	{
		final String ns = "urn:test:";

		final OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);

		final ObjectProperty p = model.createObjectProperty(ns + "p");
		final ObjectProperty q = model.createObjectProperty(ns + "q");
		final Individual a = model.createIndividual(ns + "a", OWL.Thing);
		final Individual b = model.createIndividual(ns + "b", OWL.Thing);

		// use a subproperty to make sure we get inferred results and not just
		// results from raw graph
		p.addSubProperty(q);

		// no property assertion to infer yet
		Model inferences = ModelFactory.createDefaultModel();
		assertPropertyValues(model, q, inferences);

		// add a new property assertion between two existing individuals
		model.add(a, q, b);

		// verify inference using super property
		inferences = ModelFactory.createDefaultModel();
		inferences.add(a, p, b);
		assertPropertyValues(model, p, inferences);

		// add a new property assertion using a new _individual
		final Individual c = model.createIndividual(ns + "c", OWL.Thing);
		model.add(a, q, c);

		// verify inference using super property
		inferences = ModelFactory.createDefaultModel();
		inferences.add(a, p, b);
		inferences.add(a, p, c);
		assertPropertyValues(model, p, inferences);
	}

	@Test
	public void testSimpleDataPropertyAssertion()
	{
		Assume.assumeFalse("true".equals(_newOptions.getProperty("USE_INCREMENTAL_DELETION")));

		final String ns = "urn:test:";

		final OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);

		final DatatypeProperty p = model.createDatatypeProperty(ns + "p");
		final DatatypeProperty q = model.createDatatypeProperty(ns + "q");
		final Individual a = model.createIndividual(ns + "a", OWL.Thing);
		final Literal s1 = model.createLiteral("some test string 1");
		final Literal s2 = model.createLiteral("some test string 2");

		// use a subproperty to make sure we get inferred results and not just
		// results from raw graph
		p.addSubProperty(q);

		// no property assertion to infer yet
		Model inferences = ModelFactory.createDefaultModel();
		assertPropertyValues(model, q, inferences);

		// add a new property assertion between two existing individuals
		model.add(a, q, s1);
		model.add(a, q, s2);

		// verify inference using super property
		inferences = ModelFactory.createDefaultModel();
		inferences.add(a, p, s1);
		inferences.add(a, p, s2);
		assertPropertyValues(model, p, inferences);

		// delete one _data property assertion
		model.remove(a, q, s2);

		// verify inference using super property
		inferences = ModelFactory.createDefaultModel();
		inferences.add(a, p, s1);
		assertPropertyValues(model, p, inferences);
	}

	@Test
	public void addTypeToMergedNode()
	{
		classes(_A, _B, _C);
		individuals(_a, _b, _c);

		// a is either b or c
		_model.add(_a, RDF.type, oneOf(_b, _c));
		_model.add(_a, RDF.type, _A);
		_model.add(_b, RDF.type, _B);
		_model.add(_c, RDF.type, _C);

		assertConsistent();

		assertTrue(_model.contains(_a, RDF.type, _A));
		// we don't know which equality holds
		assertFalse(_model.contains(_a, RDF.type, _B));
		assertFalse(_model.contains(_a, RDF.type, _C));
		assertFalse(_model.contains(_a, RDF.type, _D));

		_model.add(_a, RDF.type, _D);

		assertConsistent();

		assertTrue(_model.contains(_a, RDF.type, _A));
		assertFalse(_model.contains(_a, RDF.type, _B));
		assertFalse(_model.contains(_a, RDF.type, _C));
		assertTrue(_model.contains(_a, RDF.type, _D));
	}

	@Test
	public void removeTypeFromMergedNode()
	{
		classes(_A, _B, _C, _D);
		individuals(_a, _b, _c);

		// a is either b or c
		_model.add(_a, RDF.type, oneOf(_b, _c));
		_model.add(_a, RDF.type, _A);
		_model.add(_b, RDF.type, _B);
		_model.add(_c, RDF.type, _C);
		_model.add(_a, RDF.type, _D);

		assertConsistent();

		assertTrue(_model.contains(_a, RDF.type, _A));
		assertFalse(_model.contains(_a, RDF.type, _B));
		assertFalse(_model.contains(_a, RDF.type, _C));
		assertTrue(_model.contains(_a, RDF.type, _D));

		_model.remove(_a, RDF.type, _D);

		assertConsistent();

		assertTrue(_model.contains(_a, RDF.type, _A));
		assertFalse(_model.contains(_a, RDF.type, _B));
		assertFalse(_model.contains(_a, RDF.type, _C));
		assertFalse(_model.contains(_a, RDF.type, _D));
	}

	public static void main(final String[] args)
	{
		final IncJenaConsistencyTests test = new IncJenaConsistencyTests(true, true, false);
		test.before();
		test.testTBoxChange();

	}
}
