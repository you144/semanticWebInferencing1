// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC.
// <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms
// of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static openllet.core.utils.TermFactory.BOTTOM;
import static openllet.core.utils.TermFactory.TOP;
import static openllet.core.utils.TermFactory.TOP_LIT;
import static openllet.core.utils.TermFactory.all;
import static openllet.core.utils.TermFactory.and;
import static openllet.core.utils.TermFactory.card;
import static openllet.core.utils.TermFactory.hasValue;
import static openllet.core.utils.TermFactory.inv;
import static openllet.core.utils.TermFactory.literal;
import static openllet.core.utils.TermFactory.max;
import static openllet.core.utils.TermFactory.maxExclusive;
import static openllet.core.utils.TermFactory.maxInclusive;
import static openllet.core.utils.TermFactory.min;
import static openllet.core.utils.TermFactory.minExclusive;
import static openllet.core.utils.TermFactory.minInclusive;
import static openllet.core.utils.TermFactory.not;
import static openllet.core.utils.TermFactory.oneOf;
import static openllet.core.utils.TermFactory.or;
import static openllet.core.utils.TermFactory.restrict;
import static openllet.core.utils.TermFactory.self;
import static openllet.core.utils.TermFactory.some;
import static openllet.core.utils.TermFactory.term;
import static openllet.core.utils.TermFactory.value;
import static openllet.test.PelletTestCase.assertIteratorValues;
import static openllet.test.PelletTestCase.assertNotSubClass;
import static openllet.test.PelletTestCase.assertSatisfiable;
import static openllet.test.PelletTestCase.assertSubClass;
import static openllet.test.PelletTestCase.assertUnsatisfiable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import junit.framework.JUnit4TestAdapter;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.KBLoader;
import openllet.core.KnowledgeBase;
import openllet.core.KnowledgeBaseImpl;
import openllet.core.OpenlletOptions;
import openllet.core.boxes.abox.Clash;
import openllet.core.boxes.rbox.Role;
import openllet.core.datatypes.Datatypes;
import openllet.core.datatypes.Facet;
import openllet.core.datatypes.types.real.XSDByte;
import openllet.core.datatypes.types.real.XSDDecimal;
import openllet.core.datatypes.types.real.XSDInteger;
import openllet.core.datatypes.types.text.XSDString;
import openllet.core.taxonomy.Taxonomy;
import openllet.core.taxonomy.TaxonomyImpl;
import openllet.core.taxonomy.TaxonomyNode;
import openllet.core.taxonomy.TaxonomyUtils;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.FileUtils;
import openllet.core.utils.PropertiesBuilder;
import openllet.core.utils.SetUtils;
import openllet.core.utils.TermFactory;
import openllet.core.utils.iterator.FlattenningIterator;
import openllet.core.utils.iterator.IteratorUtils;
import openllet.jena.JenaLoader;
import org.junit.Ignore;
import org.junit.Test;

public class MiscTests extends AbstractKBTests
{
	public static String _base = "file:" + PelletTestSuite.base + "misc/";

	public static void main(final String args[])
	{
		junit.textui.TestRunner.run(MiscTests.suite());
	}

	public static junit.framework.Test suite()
	{
		return new JUnit4TestAdapter(MiscTests.class);
	}

	/**
	 * Test that an _individual added to a complete ABox isn't discarded if backtracking occurs in that ABox
	 */
	@Test
	public void backtrackPreservesAssertedIndividuals()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		kb.addIndividual(term("x"));

		kb.addClass(term("C"));
		kb.addClass(term("D"));

		kb.addDatatypeProperty(term("p"));
		kb.addFunctionalProperty(term("p"));

		kb.addSubClass(term("C"), ATermUtils.makeSomeValues(term("p"), ATermUtils.makeValue(ATermUtils.makePlainLiteral("0"))));
		kb.addSubClass(term("D"), ATermUtils.makeSomeValues(term("p"), ATermUtils.makeValue(ATermUtils.makePlainLiteral("1"))));

		kb.addType(term("x"), ATermUtils.makeOr(ATermUtils.makeList(new ATerm[] { term("C"), term("D") })));

		/*
		 * At this point we can get onto one of two branches. In one p(x,"0"),
		 * in the other p(x,"1") The _branch point is a concept _disjunction and
		 * we're provoking a datatype clash.
		 */
		assertTrue(kb.isConsistent());

		/*
		 * Add the _individual to the now completed ABox
		 */
		kb.addIndividual(term("y"));

		assertTrue(kb.isConsistent());
		assertNotNull(kb.getABox().getIndividual(term("y")));

		/*
		 * This assertion causes a clash regardless of which _branch we are on
		 */
		kb.addPropertyValue(term("p"), term("x"), ATermUtils.makePlainLiteral("2"));

		assertFalse(kb.isConsistent());

		/*
		 * In 2.0.0-rc5 (and perhaps earlier, this assertion fails)
		 */
		assertNotNull(kb.getABox().getIndividual(term("y")));
	}

	@Test
	public void testFileUtilsToURI() throws MalformedURLException
	{
		assertEquals(new File("test").toURI().toURL().toString(), FileUtils.toURI("test"));
		assertEquals("http://example.com/foo", FileUtils.toURI("http://example.com/foo"));
		assertEquals("file:///foo", FileUtils.toURI("file:///foo"));
		assertEquals("ftp://example.com/foo", FileUtils.toURI("ftp://example.com/foo"));
		assertEquals("https://example.com/foo", FileUtils.toURI("https://example.com/foo"));
	}

	@Test
	public void testQualifiedCardinalityObjectProperty()
	{
		final ATermAppl sub = term("sub");
		final ATermAppl sup = term("sup");

		classes(_c, _d, sub, sup);
		objectProperties(_p, _f);

		_kb.addFunctionalProperty(_f);

		_kb.addSubClass(sub, sup);

		assertSatisfiable(_kb, and(min(_p, 2, and(_c, _d)), max(_p, 2, _c), some(_p, or(and(_c, not(_d)), _c))));
		assertSubClass(_kb, min(_p, 4, TOP), min(_p, 2, TOP));
		assertNotSubClass(_kb, min(_p, 1, TOP), min(_p, 2, TOP));
		assertNotSubClass(_kb, min(_p, 1, _c), min(_p, 1, _d));
		assertNotSubClass(_kb, and(some(_p, _c), some(_p, not(_c))), min(_p, 2, _d));
		assertSubClass(_kb, min(_p, 3, _c), min(_p, 2, _c));
		assertSubClass(_kb, min(_p, 3, _c), min(_p, 2, TOP));
		assertSubClass(_kb, min(_p, 2, _c), min(_p, 2, TOP));
		assertNotSubClass(_kb, min(_p, 2, _c), min(_p, 2, _d));
		assertSubClass(_kb, min(_p, 2, and(_c, _d)), some(_p, _c));
		assertSubClass(_kb, max(_p, 1, sup), max(_p, 2, sub));
		assertSubClass(_kb, and(max(_f, 1, TOP), all(_f, _c)), max(_f, 1, _c));
		assertSubClass(_kb, and(min(_p, 2, _c), min(_p, 2, not(_c))), min(_p, 4, TOP));
	}

	@Test
	public void testQualifiedCardinalityDataProperty()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl c = restrict(Datatypes.INTEGER, minInclusive(literal(10)));
		final ATermAppl d = restrict(Datatypes.INTEGER, maxInclusive(literal(20)));

		final ATermAppl p = term("p");
		final ATermAppl f = term("f");
		final ATermAppl sub = Datatypes.SHORT;
		final ATermAppl sup = Datatypes.INTEGER;

		kb.addDatatype(sub);
		kb.addDatatype(sup);
		kb.addDatatypeProperty(p);
		kb.addDatatypeProperty(f);
		kb.addFunctionalProperty(f);

		assertSatisfiable(kb, and(min(p, 2, and(c, d)), max(p, 2, c), some(p, or(and(c, not(d)), c))));
		assertSubClass(kb, min(p, 4, TOP_LIT), min(p, 2, TOP_LIT));
		assertNotSubClass(kb, min(p, 1, TOP_LIT), min(p, 2, TOP_LIT));
		assertNotSubClass(kb, min(p, 1, c), min(p, 1, d));
		assertNotSubClass(kb, and(some(p, c), some(p, not(c))), min(p, 2, d));
		assertSubClass(kb, min(p, 3, c), min(p, 2, c));
		assertSubClass(kb, min(p, 3, c), min(p, 2, TOP_LIT));
		assertSubClass(kb, min(p, 2, c), min(p, 2, TOP_LIT));
		assertNotSubClass(kb, min(p, 2, c), min(p, 2, d));
		assertSubClass(kb, min(p, 2, and(c, d)), some(p, c));
		assertSubClass(kb, max(p, 1, sup), max(p, 2, sub));
		assertSubClass(kb, and(max(f, 1, TOP_LIT), all(f, c)), max(f, 1, c));
		assertSubClass(kb, and(min(p, 2, c), min(p, 2, not(c))), min(p, 4, TOP_LIT));
	}

	@Test
	public void testQualifiedCardinality3()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl c = term("z");
		final ATermAppl d = term("d");
		final ATermAppl e = term("e");
		final ATermAppl notD = term("notD");

		final ATermAppl p = term("p");

		final ATermAppl x = term("x");
		final ATermAppl y3 = term("y3");

		kb.addObjectProperty(p);
		kb.addClass(c);
		kb.addClass(d);
		kb.addClass(e);
		kb.addClass(notD);

		kb.addDisjointClass(d, notD);
		// _kb.addSubClass( c, or(e,not(d)) );

		kb.addIndividual(x);
		kb.addIndividual(y3);

		kb.addType(x, and(min(p, 2, and(d, e)), max(p, 2, d)));
		kb.addType(y3, not(e));
		kb.addType(y3, some(inv(p), value(x)));
		kb.addType(y3, or(d, c));

		assertTrue(kb.isConsistent());
	}

	@Test
	public void testSelfRestrictions()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl c = term("c");
		final ATermAppl d = term("d");

		final ATermAppl p = term("p");

		kb.addClass(c);
		kb.addClass(d);

		kb.addObjectProperty(p);

		kb.addRange(p, d);

		kb.addSubClass(c, and(self(p), some(p, TOP)));

		assertTrue(kb.isConsistent());

		assertTrue(kb.isSatisfiable(c));
		assertTrue(kb.isSubClassOf(c, d));

	}

	@Test
	/**
	 * Test for the enhancement required in #252
	 */
	public void testBooleanDatatypeConstructors()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl nni = Datatypes.NON_NEGATIVE_INTEGER;
		final ATermAppl npi = Datatypes.NON_POSITIVE_INTEGER;
		final ATermAppl ni = Datatypes.NEGATIVE_INTEGER;
		final ATermAppl pi = Datatypes.POSITIVE_INTEGER;
		final ATermAppl f = Datatypes.FLOAT;

		final ATermAppl s = term("s");
		kb.addDatatypeProperty(s);

		assertSatisfiable(kb, some(s, pi));
		assertSatisfiable(kb, some(s, not(pi)));
		assertUnsatisfiable(kb, some(s, and(pi, ni)));
		assertUnsatisfiable(kb, some(s, and(f, or(pi, ni))));
		assertSatisfiable(kb, some(s, and(npi, ni)));
		assertSatisfiable(kb, some(s, and(nni, pi)));
		assertSatisfiable(kb, some(s, or(nni, npi)));
		assertSatisfiable(kb, some(s, and(nni, npi)));
	}

	@Test
	public void testSelfRestrictionRestore()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl C = term("c");
		final ATermAppl D = term("d");

		final ATermAppl p = term("p");
		final ATermAppl q = term("q");

		final ATermAppl a = term("a");

		kb.addClass(C);
		kb.addClass(D);

		kb.addObjectProperty(p);
		kb.addObjectProperty(q);

		kb.addSubClass(C, or(not(self(p)), not(self(q))));

		kb.addIndividual(a);
		kb.addType(a, C);

		assertTrue(kb.isConsistent());

		assertFalse(kb.isType(a, not(self(p))));
		assertFalse(kb.isType(a, not(self(q))));
	}

	@Test
	public void testReflexive1()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl c = term("c");
		final ATermAppl d = term("d");
		final ATermAppl sub = term("sub");
		final ATermAppl sup = term("sup");

		final ATermAppl p = term("p");
		final ATermAppl r = term("r");
		final ATermAppl weakR = term("weakR");

		final ATermAppl x = term("x");
		final ATermAppl y = term("y");

		kb.addClass(c);
		kb.addClass(d);
		kb.addClass(sub);
		kb.addClass(sup);
		kb.addSubClass(sub, sup);
		kb.addSubClass(some(weakR, TOP), self(weakR));

		kb.addObjectProperty(p);
		kb.addObjectProperty(r);
		kb.addObjectProperty(weakR);
		kb.addReflexiveProperty(r);
		kb.addRange(r, d);

		kb.addIndividual(x);
		kb.addType(x, self(p));
		kb.addType(x, not(some(weakR, value(x))));
		kb.addIndividual(y);
		kb.addPropertyValue(weakR, y, x);

		assertTrue(kb.isConsistent());

		assertTrue(kb.isSubClassOf(and(c, self(p)), some(p, c)));
		assertTrue(kb.isSubClassOf(and(c, min(r, 1, not(c))), min(r, 2, TOP)));
		assertTrue(kb.isSubClassOf(min(r, 1, c), d));
		assertTrue(kb.hasPropertyValue(x, p, x));
		assertTrue(kb.hasPropertyValue(y, weakR, y));
		assertTrue(kb.isDifferentFrom(x, y));
		assertTrue(kb.isDifferentFrom(y, x));
		assertTrue(kb.isType(x, some(r, value(x))));
		assertTrue(kb.isSatisfiable(and(self(p), self(inv(p)), max(p, 1, TOP))));
	}

	@Test
	public void testReflexive2()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl c = term("c");
		final ATermAppl r = term("r");

		kb.addIndividual(a);
		kb.addClass(c);
		kb.addSubClass(c, all(r, BOTTOM));
		kb.addSubClass(c, oneOf(a));

		kb.addObjectProperty(r);
		kb.addReflexiveProperty(r);

		assertSatisfiable(kb, c, false);
	}

	@Test
	public void testReflexive3()
	{
		classes(_C);
		objectProperties(_r);
		individuals(_a, _b, _c);

		_kb.addEquivalentClass(_C, self(_r));

		_kb.addPropertyValue(_r, _a, _a);
		_kb.addType(_b, _C);
		_kb.addPropertyValue(_r, _c, _a);
		_kb.addPropertyValue(_r, _c, _b);

		assertTrue(_kb.hasPropertyValue(_a, _r, _a));
		assertTrue(_kb.hasPropertyValue(_b, _r, _b));
		assertFalse(_kb.hasPropertyValue(_c, _r, _c));

		final Map<ATermAppl, List<ATermAppl>> allRs = _kb.getPropertyValues(_r);
		assertIteratorValues(allRs.get(_a).iterator(), new ATermAppl[] { _a });
		assertIteratorValues(allRs.get(_b).iterator(), new ATermAppl[] { _b });
		assertIteratorValues(allRs.get(_c).iterator(), new ATermAppl[] { _a, _b });

		assertTrue(_kb.isType(_a, _C));
		assertTrue(_kb.isType(_b, _C));
		assertFalse(_kb.isType(_c, _C));

		assertEquals(_kb.getInstances(_C), SetUtils.create(_a, _b));
	}

	@Test
	public void testAsymmetry()
	{
		final ATermAppl p = term("p");

		final KnowledgeBase kb = new KnowledgeBaseImpl();
		kb.addObjectProperty(p);
		kb.addAsymmetricProperty(p);

		assertTrue(kb.isIrreflexiveProperty(p));
	}

	@Test
	public void testResrictedDataRange()
	{
		final byte MIN = 0;
		final byte MAX = 127;
		final int COUNT = MAX - MIN + 1;

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl C = term("C");
		final ATermAppl D = term("D");
		final ATermAppl E = term("E");

		final ATermAppl p = term("p");

		final ATermAppl x = term("x");
		final ATermAppl y = term("y");

		kb.addClass(C);
		kb.addClass(D);
		kb.addClass(E);

		kb.addDatatypeProperty(p);
		kb.addRange(p, ATermUtils.makeRestrictedDatatype(XSDInteger.getInstance().getName(), new ATermAppl[] { ATermUtils.makeFacetRestriction(Facet.XSD.MIN_INCLUSIVE.getName(), ATermUtils.makeTypedLiteral(Byte.toString(MIN), XSDByte.getInstance().getName())), ATermUtils.makeFacetRestriction(Facet.XSD.MAX_INCLUSIVE.getName(), ATermUtils.makeTypedLiteral(Byte.toString(MAX), XSDByte.getInstance().getName())) }));

		kb.addSubClass(C, card(p, COUNT + 1, ATermUtils.TOP_LIT));
		kb.addSubClass(D, card(p, COUNT, ATermUtils.TOP_LIT));
		kb.addSubClass(E, card(p, COUNT - 1, ATermUtils.TOP_LIT));

		kb.addIndividual(x);
		kb.addType(x, D);

		kb.addIndividual(y);
		kb.addType(y, E);

		assertFalse(kb.isSatisfiable(C));
		assertTrue(kb.isSatisfiable(D));
		assertTrue(kb.isSatisfiable(E));

		assertTrue(kb.hasPropertyValue(x, p, ATermUtils.makeTypedLiteral("5", XSDInteger.getInstance().getName())));
		assertFalse(kb.hasPropertyValue(y, p, ATermUtils.makeTypedLiteral("5", XSDDecimal.getInstance().getName())));
	}

	@Test
	public void testMaxCardinality()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		kb.addObjectProperty(term("p"));
		kb.addObjectProperty(term("q"));
		kb.addFunctionalProperty(term("q"));

		kb.addClass(term("C"));
		kb.addSubClass(term("C"), ATermUtils.makeMax(term("p"), 2, ATermUtils.TOP));

		kb.addClass(term("D1"));
		kb.addClass(term("D2"));
		kb.addClass(term("D3"));
		kb.addClass(term("D4"));
		kb.addClass(term("E1"));
		kb.addClass(term("E2"));
		kb.addClass(term("E3"));
		kb.addClass(term("E4"));
		kb.addSubClass(term("D1"), ATermUtils.makeSomeValues(term("q"), term("E1")));
		kb.addSubClass(term("D2"), ATermUtils.makeSomeValues(term("q"), term("E2")));
		kb.addSubClass(term("D3"), ATermUtils.makeSomeValues(term("q"), term("E3")));
		kb.addSubClass(term("D4"), ATermUtils.makeSomeValues(term("q"), term("E4")));

		kb.addIndividual(term("x"));
		kb.addType(term("x"), term("C"));
		kb.addIndividual(term("x1"));
		kb.addType(term("x1"), term("D1"));
		kb.addIndividual(term("x2"));
		kb.addType(term("x2"), term("D2"));
		kb.addIndividual(term("x3"));
		kb.addType(term("x3"), term("D3"));
		kb.addIndividual(term("x4"));
		kb.addType(term("x4"), term("D4"));

		kb.addPropertyValue(term("p"), term("x"), term("x1"));
		kb.addPropertyValue(term("p"), term("x"), term("x2"));
		kb.addPropertyValue(term("p"), term("x"), term("x3"));
		kb.addPropertyValue(term("p"), term("x"), term("x4"));

		kb.addDisjointClass(term("E1"), term("E2"));
		kb.addDisjointClass(term("E1"), term("E4"));
		kb.addDisjointClass(term("E2"), term("E3"));

		assertTrue(kb.isConsistent());

		assertTrue(kb.isSameAs(term("x1"), term("x3")));
		assertTrue(kb.isSameAs(term("x3"), term("x1")));
		assertTrue(kb.isSameAs(term("x2"), term("x4")));

		assertTrue(kb.getSames(term("x1")).contains(term("x3")));
		assertTrue(kb.getSames(term("x2")).contains(term("x4")));
	}

	@Test
	public void testDifferentFrom2()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		kb.addClass(term("C"));
		kb.addClass(term("D"));
		kb.addDisjointClass(term("C"), term("D"));

		kb.addIndividual(term("a"));
		kb.addType(term("a"), term("C"));

		kb.addIndividual(term("b"));
		kb.addType(term("b"), term("D"));

		kb.classify();

		assertTrue(kb.getDifferents(term("a")).contains(term("b")));
		assertTrue(kb.getDifferents(term("b")).contains(term("a")));
	}

	@Test
	public void testSHOIN()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		kb.addObjectProperty(term("R1"));
		kb.addObjectProperty(term("invR1"));
		kb.addObjectProperty(term("R2"));
		kb.addObjectProperty(term("invR2"));
		kb.addObjectProperty(term("S1"));
		kb.addObjectProperty(term("invS1"));
		kb.addObjectProperty(term("S2"));
		kb.addObjectProperty(term("invS2"));

		kb.addInverseProperty(term("R1"), term("invR1"));
		kb.addInverseProperty(term("R2"), term("invR2"));
		kb.addInverseProperty(term("S1"), term("invS1"));
		kb.addInverseProperty(term("S2"), term("invS2"));

		kb.addIndividual(term("o1"));
		kb.addIndividual(term("o2"));

		kb.addSubClass(value(term("o1")), and(max(term("invR1"), 2, ATermUtils.TOP), all(term("invR1"), some(term("S1"), some(term("invS2"), some(term("R2"), value(term("o2"))))))));

		kb.addSubClass(value(term("o2")), and(max(term("invR2"), 2, ATermUtils.TOP), all(term("invR2"), some(term("S2"), some(term("invS1"), some(term("R1"), value(term("o1"))))))));

		assertTrue(kb.isConsistent());

		assertTrue(kb.isSatisfiable(and(value(term("o1")), some(term("invR1"), TOP))));
	}

	// @Test public void testEconn1() throws Exception
	// {
	// String ns = "http://www.mindswap.org/2004/multipleOnt/FactoredOntologies/EasyTests/Easy2/people.owl#";
	//
	// OWLReasoner reasoner = new OWLReasoner();
	// reasoner.setEconnEnabled( true );
	//
	// reasoner.load( ns );
	//
	// assertTrue( reasoner.isConsistent() );
	//
	// assertTrue( !reasoner.isSatisfiable( ResourceFactory.createResource( ns + "Unsat1" ) ) );
	// assertTrue( !reasoner.isSatisfiable( ResourceFactory.createResource( ns + "Unsat2" ) ) );
	// assertTrue( !reasoner.isSatisfiable( ResourceFactory.createResource( ns + "Unsat3" ) ) );
	// assertTrue( !reasoner.isSatisfiable( ResourceFactory.createResource( ns + "Unsat4" ) ) );
	// }

	@Test
	public void testCyclicTBox1()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl C = term("C");
		kb.addEquivalentClass(C, not(C));

		assertFalse(kb.isConsistent());
	}

	/**
	 * Test for ticket #123 An axiom like A = B or (not B) cause problems in classification process (runtime exception in CD classification). Due to
	 * _disjunction A is discovered to be a told subsumer of B. A is marked as non-primitive but since marking is done on unfolding map all we see is A = TOP
	 * and B is left as CD. In phase 1, B is tried to be CD-classified but A is eft for phase 2 thus unclassified at that time causing the exception.
	 */
	@Test
	public void testTopClass2()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");
		final ATermAppl C = term("C");

		final ATermAppl p = term("p");

		kb.addClass(A);
		kb.addClass(B);
		kb.addClass(C);
		kb.addObjectProperty(p);

		kb.addEquivalentClass(A, or(B, not(B)));
		// the following restriction is only to ensure we don't use the
		// EL classifier
		kb.addSubClass(C, min(p, 2, TOP));

		assertTrue(kb.isConsistent());

		kb.classify();

		assertTrue(kb.isEquivalentClass(A, TOP));
		assertFalse(kb.isEquivalentClass(B, TOP));
	}

	/**
	 * Same as {@link #testTopClass2()} but tests the EL classifier.
	 */
	@Test
	public void testTopClass2EL()
	{
		// This test was failing due to the issue explained in #157 at some point but not anymore
		// The issue explained in #157 is still valid even though this test passes
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");

		kb.addClass(A);
		kb.addClass(B);

		kb.addEquivalentClass(A, or(B, not(B)));

		assertTrue(kb.isConsistent());

		kb.classify();

		assertTrue(kb.isEquivalentClass(A, TOP));
		assertFalse(kb.isEquivalentClass(B, TOP));
	}

	/**
	 * An axiom like B = B or (not B) cause problems in classification process because B was marked disjoint with itself.
	 */
	@Test
	public void testTopClass3()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");
		final ATermAppl C = term("C");

		kb.addClass(A);
		kb.addClass(B);
		kb.addClass(C);

		kb.addEquivalentClass(A, B);
		kb.addEquivalentClass(B, or(B, not(B)));
		kb.addSubClass(C, A);

		assertTrue(kb.isConsistent());

		kb.classify();

		assertTrue(kb.isEquivalentClass(A, TOP));
		assertTrue(kb.isEquivalentClass(B, TOP));
		assertFalse(kb.isEquivalentClass(C, TOP));
	}

	/**
	 * not A subClassOf A implies A is TOP
	 */
	@Test
	public void testTopClass4()
	{
		classes(_A, _B, _C);

		_kb.addSubClass(not(_A), _A);
		_kb.addSubClass(_B, _A);

		assertTrue(_kb.isConsistent());

		_kb.classify();

		assertTrue(_kb.isEquivalentClass(_A, TOP));
	}

	/**
	 * not A subClassOf B does not imply disjointness between concepts A and B.
	 */
	@Test
	public void testNonDisjointness()
	{
		classes(_A, _B, _C);

		_kb.addSubClass(not(_A), _B);
		_kb.addSubClass(_C, and(_A, _B));

		assertTrue(_kb.isConsistent());

		_kb.classify();

		assertTrue(_kb.isSatisfiable(_C));
	}

	@Test
	public void testCyclicTBox2()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl B = term("B");
		final ATermAppl C = term("C");
		final ATermAppl D = term("D");

		kb.addClass(B);
		kb.addClass(C);
		kb.addClass(D);
		kb.addSubClass(C, B);
		kb.addSubClass(D, C);
		kb.addEquivalentClass(D, B);

		kb.classify();

		assertTrue(kb.isEquivalentClass(B, C));
		assertTrue(kb.isEquivalentClass(B, D));
		assertTrue(kb.isEquivalentClass(D, C));
	}

	@Test
	public void testCyclicTBox3()
	{
		final List<ATermAppl> classes = Arrays.asList(term("C0"), term("C1"), term("C2"));

		final Taxonomy<ATermAppl> taxonomy = new TaxonomyImpl<>(classes, ATermUtils.TOP, ATermUtils.BOTTOM);

		final TaxonomyNode<ATermAppl> top = taxonomy.getTop();
		@SuppressWarnings("unchecked")
		final TaxonomyNode<ATermAppl>[] nodes = new TaxonomyNode[classes.size()];
		int i = 0;
		for (final ATermAppl c : classes)
			nodes[i++] = taxonomy.getNode(c);

		taxonomy.addSuper(classes.get(1), classes.get(2));
		taxonomy.addSuper(classes.get(0), classes.get(1));

		taxonomy.merge(top, nodes[0]);

		assertTrue(top.getSupers().isEmpty());
		assertTrue(top.getEquivalents().containsAll(classes));
	}

	@Test
	public void testComplexTypes()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl p = term("p");
		final ATermAppl q = term("q");

		kb.addIndividual(a);

		kb.addType(a, min(p, 3, TOP));
		kb.addType(a, max(q, 2, TOP));
		kb.addType(a, min(q, 1, TOP));
		kb.addType(a, min(q, 1, TOP));

		kb.addObjectProperty(p);
		kb.addObjectProperty(q);

		assertTrue(kb.isConsistent());
	}

	@Test
	public void testBottomSub()
	{

		// See also: http://cvsdude.com/trac/clark-parsia/pellet-devel/ticket/7

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl c = term("c");
		kb.addClass(c);
		kb.addSubClass(ATermUtils.BOTTOM, c);
		kb.classify();

		assertTrue(kb.isSubClassOf(ATermUtils.BOTTOM, c));
	}

	@Test
	@Ignore("Known to fail because different lexical forms are stored in one canonical literal")
	public void testCanonicalLiteral()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl p = term("p");
		final ATermAppl q = term("q");
		final ATermAppl plain = ATermUtils.makePlainLiteral("lit");
		final ATermAppl typed = ATermUtils.makeTypedLiteral("lit", XSDString.getInstance().getName());

		kb.addIndividual(a);
		kb.addDatatypeProperty(p);
		kb.addDatatypeProperty(q);

		kb.addPropertyValue(p, a, plain);
		kb.addPropertyValue(q, a, typed);

		assertIteratorValues(kb.getDataPropertyValues(p, a).iterator(), new ATermAppl[] { plain });
		assertIteratorValues(kb.getDataPropertyValues(q, a).iterator(), new ATermAppl[] { typed });

	}

	@Test
	public void testSimpleABoxRemove()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl C = term("C");
		final ATermAppl D = term("D");

		kb.addClass(C);
		kb.addClass(D);

		kb.addIndividual(a);
		kb.addType(a, C);
		kb.addType(a, D);

		kb.removeType(a, D);

		assertTrue(kb.isConsistent());
		assertTrue(kb.isType(a, C));
		assertFalse(kb.isType(a, D));
	}

	@Test
	public void testABoxRemovalWithAllValues()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl b = term("b");

		final ATermAppl C = term("C");

		final ATermAppl p = term("p");

		kb.addClass(C);

		kb.addObjectProperty(p);

		kb.addIndividual(a);
		kb.addIndividual(b);

		kb.addType(a, all(p, C));
		kb.addType(b, C);

		kb.addPropertyValue(p, a, b);

		kb.removeType(b, C);

		kb.removePropertyValue(p, a, b);

		assertTrue(kb.isConsistent());
		assertFalse(kb.isType(b, C));
		assertFalse(kb.hasPropertyValue(a, p, b));
	}

	@Test
	public void testIncrementalTBoxDisjointRemove1()
	{
		// Add and remove disjointness

		final Properties newOptions = PropertiesBuilder.singleton("USE_TRACING", "true");
		final Properties savedOptions = OpenlletOptions.setOptions(newOptions);

		try
		{
			final ATermAppl A = ATermUtils.makeTermAppl("A");
			final ATermAppl B = ATermUtils.makeTermAppl("B");
			final ATermAppl C = ATermUtils.makeTermAppl("C");
			final ATermAppl x = ATermUtils.makeTermAppl("x");

			final KnowledgeBase kb = new KnowledgeBaseImpl();

			kb.addClass(A);
			kb.addClass(B);
			kb.addClass(C);
			kb.addIndividual(x);

			kb.addSubClass(C, A);

			kb.addType(x, C);
			kb.addType(x, B);

			final Set<Set<ATermAppl>> expectedTypes = new HashSet<>();
			expectedTypes.add(Collections.singleton(ATermUtils.TOP));
			expectedTypes.add(Collections.singleton(A));
			expectedTypes.add(Collections.singleton(B));
			expectedTypes.add(Collections.singleton(C));

			assertTrue(kb.isConsistent());

			Set<Set<ATermAppl>> actualTypes = kb.getTypes(x);
			assertEquals(expectedTypes, actualTypes);

			kb.addDisjointClass(A, B);
			assertFalse(kb.isConsistent());

			assertTrue(kb.removeAxiom(ATermUtils.makeDisjoint(A, B)));
			assertTrue(kb.isConsistent());

			actualTypes = kb.getTypes(x);
			assertEquals(expectedTypes, actualTypes);

		}
		finally
		{
			OpenlletOptions.setOptions(savedOptions);
		}
	}

	@Test
	public void testIncrementalTBoxDisjointRemove2()
	{
		// Add and remove disjointness, which is redundant with
		// another axiom

		final Properties newOptions = PropertiesBuilder.singleton("USE_TRACING", "true");
		final Properties savedOptions = OpenlletOptions.setOptions(newOptions);

		try
		{
			classes(_A, _B, _C);
			individuals(_a);

			_kb.addSubClass(_C, _A);

			_kb.addType(_a, _C);
			_kb.addType(_a, _B);

			final Set<Set<ATermAppl>> expectedTypes = new HashSet<>();
			expectedTypes.add(Collections.singleton(ATermUtils.TOP));
			expectedTypes.add(Collections.singleton(_A));
			expectedTypes.add(Collections.singleton(_B));
			expectedTypes.add(Collections.singleton(_C));

			assertTrue(_kb.isConsistent());

			Set<Set<ATermAppl>> actualTypes = _kb.getTypes(_a);
			assertEquals(expectedTypes, actualTypes);

			_kb.addSubClass(_A, ATermUtils.makeNot(_B));
			assertFalse(_kb.isConsistent());

			_kb.addDisjointClass(_A, _B);
			assertFalse(_kb.isConsistent());

			assertTrue(_kb.removeAxiom(ATermUtils.makeDisjoint(_A, _B)));
			assertFalse(_kb.isConsistent());

			assertTrue(_kb.removeAxiom(ATermUtils.makeSub(_A, ATermUtils.makeNot(_B))));
			assertTrue(_kb.isConsistent());

			actualTypes = _kb.getTypes(_a);
			assertEquals(expectedTypes, actualTypes);

		}
		finally
		{
			OpenlletOptions.setOptions(savedOptions);
		}
	}

	@Test
	public void testIncrementalTBoxDisjointRemove3()
	{
		// Repeat of testIncrementalTBoxDisjointRemove3,
		// using n-ary disjoint axiom syntax

		final Properties newOptions = PropertiesBuilder.singleton("USE_TRACING", "true");
		final Properties savedOptions = OpenlletOptions.setOptions(newOptions);

		try
		{
			classes(_A, _B, _C);
			individuals(_a);

			_kb.addSubClass(_C, _A);

			_kb.addType(_a, _C);
			_kb.addType(_a, _B);

			final Set<Set<ATermAppl>> expectedTypes = new HashSet<>();
			expectedTypes.add(Collections.singleton(ATermUtils.TOP));
			expectedTypes.add(Collections.singleton(_A));
			expectedTypes.add(Collections.singleton(_B));
			expectedTypes.add(Collections.singleton(_C));

			assertTrue(_kb.isConsistent());

			Set<Set<ATermAppl>> actualTypes = _kb.getTypes(_a);
			assertEquals(expectedTypes, actualTypes);

			_kb.addSubClass(_A, ATermUtils.makeNot(_B));
			assertFalse(_kb.isConsistent());

			final ATermList list = ATermUtils.toSet(new ATerm[] { _A, _B, _D }, 3);
			_kb.addDisjointClasses(list);
			assertFalse(_kb.isConsistent());

			assertTrue(_kb.removeAxiom(ATermUtils.makeDisjoints(list)));
			assertFalse(_kb.isConsistent());

			assertTrue(_kb.removeAxiom(ATermUtils.makeSub(_A, ATermUtils.makeNot(_B))));
			assertTrue(_kb.isConsistent());

			actualTypes = _kb.getTypes(_a);
			assertEquals(expectedTypes, actualTypes);
		}
		finally
		{
			OpenlletOptions.setOptions(savedOptions);
		}
	}

	@Test
	public void testIncrementalTBoxDisjointRemove4()
	{
		// test that a disjoint axiom absorbed into domain axiom cannot
		// be removed

		final Properties newOptions = new PropertiesBuilder().set("USE_TRACING", "true").set("USE_ROLE_ABSORPTION", "true").build();
		final Properties savedOptions = OpenlletOptions.setOptions(newOptions);

		try
		{
			final ATermAppl A = ATermUtils.makeTermAppl("A");
			final ATermAppl B = ATermUtils.makeTermAppl("B");
			final ATermAppl p = ATermUtils.makeTermAppl("p");

			final KnowledgeBase kb = new KnowledgeBaseImpl();

			kb.addClass(A);
			kb.addClass(B);
			kb.addObjectProperty(p);

			final ATermAppl or1 = or(A, some(p, A));
			final ATermAppl or2 = or(B, some(p, B));

			final ATermList list = ATermUtils.toSet(new ATerm[] { or1, or2 }, 2);
			kb.addDisjointClasses(list);

			assertTrue(kb.isConsistent());

			final ATermAppl disjoint = ATermUtils.makeDisjoints(list);
			assertFalse(kb.removeAxiom(disjoint));
		}
		finally
		{
			OpenlletOptions.setOptions(savedOptions);
		}
	}

	@Test
	public void testIncrementalTBoxDisjointRemove5()
	{
		// Same as testIncrementalTBoxDisjointRemove4 but
		// uses n-ary disjointness axioms

		final Properties newOptions = new PropertiesBuilder().set("USE_TRACING", "true").set("USE_ROLE_ABSORPTION", "true").build();
		final Properties savedOptions = OpenlletOptions.setOptions(newOptions);

		try
		{
			final ATermAppl A = ATermUtils.makeTermAppl("A");
			final ATermAppl B = ATermUtils.makeTermAppl("B");
			final ATermAppl p = ATermUtils.makeTermAppl("p");

			final KnowledgeBase kb = new KnowledgeBaseImpl();

			kb.addClass(A);
			kb.addClass(B);
			kb.addObjectProperty(p);

			final ATermAppl or1 = or(A, some(p, A));
			final ATermAppl or2 = or(B, some(p, B));

			kb.addDisjointClass(or1, or2);

			assertTrue(kb.isConsistent());

			final ATermAppl disjoint = ATermUtils.makeDisjoint(or1, or2);
			assertFalse(kb.removeAxiom(disjoint));
		}
		finally
		{
			OpenlletOptions.setOptions(savedOptions);
		}
	}

	@Test
	public void testIncrementalTBoxDisjointRemove6()
	{
		// test that a disjoint axiom absorbed into range axiom cannot
		// be removed

		final Properties newOptions = new PropertiesBuilder().set("USE_TRACING", "true").set("USE_ROLE_ABSORPTION", "true").build();
		final Properties savedOptions = OpenlletOptions.setOptions(newOptions);

		try
		{
			final ATermAppl A = ATermUtils.makeTermAppl("A");
			final ATermAppl p = ATermUtils.makeTermAppl("p");

			final KnowledgeBase kb = new KnowledgeBaseImpl();

			kb.addClass(A);
			kb.addObjectProperty(p);

			kb.addSubClass(TOP, all(p, A));

			final Role r = kb.getRole(p);

			assertTrue(kb.isConsistent());
			assertTrue(r.getRanges().contains(A));

			assertFalse(kb.removeAxiom(ATermUtils.makeSub(TOP, all(p, A))));
		}
		finally
		{
			OpenlletOptions.setOptions(savedOptions);
		}
	}

	@Test
	public void testAssertedSameAs()
	{
		// This test case is to test the processing of sameAs processing
		// where there are redundancies in the assertions (see ticket 138)

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl b = term("b");
		final ATermAppl c = term("c");
		final ATermAppl d = term("d");
		final ATermAppl e = term("e");
		final ATermAppl f = term("f");

		kb.addIndividual(a);
		kb.addIndividual(b);
		kb.addIndividual(c);
		kb.addIndividual(d);
		kb.addIndividual(e);
		kb.addIndividual(f);

		kb.addSame(a, b);
		kb.addSame(b, c);
		kb.addSame(c, d);
		kb.addSame(a, d);
		kb.addSame(b, d);
		kb.addSame(e, f);
		kb.addDifferent(e, f);

		assertFalse(kb.isConsistent());
	}

	@Test
	public void testSubPropertyRestore()
	{
		// This test case is to test the restoring of edges with
		// subproperties (see ticket 109)

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl b = term("b");
		final ATermAppl c = term("c");
		final ATermAppl d = term("d");

		final ATermAppl p = term("p");
		final ATermAppl q = term("q");
		final ATermAppl invP = term("invP");
		final ATermAppl invQ = term("invQ");

		kb.addIndividual(a);
		kb.addIndividual(b);
		kb.addIndividual(c);
		kb.addIndividual(d);

		kb.addObjectProperty(p);
		kb.addObjectProperty(q);
		kb.addObjectProperty(invP);
		kb.addObjectProperty(invQ);

		// first add the ABox assertions to make sure none is ignored
		kb.addPropertyValue(p, a, b);
		kb.addPropertyValue(q, a, b);

		// add the subproperty axiom later
		kb.addSubProperty(p, q);
		kb.addInverseProperty(p, invP);
		kb.addInverseProperty(q, invQ);

		// force b to be merged to one of c or d
		kb.addType(b, or(value(c), value(d)));

		assertTrue(kb.isConsistent());

		// ask a query that will force the merge to be restored. with the bug
		// the q would not be restored causing either an internal exception or
		// the query to fail
		assertTrue(kb.isType(b, and(some(invP, value(a)), some(invQ, value(a)))));
	}

	@Test
	public void testInverseProperty()
	{
		// This test case is to test the retrieval of inverse
		// properties (see ticket 117)

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl p = term("p");
		final ATermAppl q = term("q");
		final ATermAppl invP = term("invP");
		final ATermAppl invQ = term("invQ");

		kb.addObjectProperty(p);
		kb.addObjectProperty(q);
		kb.addObjectProperty(invP);
		kb.addObjectProperty(invQ);
		kb.addInverseProperty(p, invP);
		kb.addInverseProperty(q, invQ);

		if (OpenlletOptions.RETURN_NON_PRIMITIVE_EQUIVALENT_PROPERTIES)
		{
			{
				final Set<ATermAppl> s = kb.getInverses(p);
				System.out.println(s);
				assertTrue(s.contains(invP));
				assertTrue(s.contains(inv(p)));
				assertTrue(2 == s.size());
			}
			{
				final Set<ATermAppl> s = kb.getInverses(q);
				System.out.println(s);
				assertTrue(s.contains(invQ));
				assertTrue(s.contains(inv(q)));
				assertTrue(2 == s.size());
			}
			{
				final Set<ATermAppl> s = kb.getInverses(invP);
				System.out.println(s);
				assertTrue(s.contains(p));
				assertTrue(s.contains(inv(invP)));
				assertTrue(2 == s.size());
			}
			{
				final Set<ATermAppl> s = kb.getInverses(invQ);
				System.out.println(s);
				assertTrue(s.contains(q));
				assertTrue(s.contains(inv(invQ)));
				assertTrue(2 == s.size());
			}
		}
		else
		{
			assertEquals(Collections.singleton(invP), kb.getInverses(p));
			assertEquals(Collections.singleton(invQ), kb.getInverses(q));
			assertEquals(Collections.singleton(p), kb.getInverses(invP));
			assertEquals(Collections.singleton(q), kb.getInverses(invQ));
		}
	}

	@Test
	public void testUndefinedTerms()
	{
		// This test case is to test the retrieval of equivalences
		// for undefined classes/properties (see ticket 90)

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl C = term("C");
		kb.addClass(C);

		final ATermAppl p = term("p");
		kb.addObjectProperty(p);

		final ATermAppl undef = term("undef");

		assertEquals(Collections.singleton(p), kb.getAllEquivalentProperties(p));
		assertEquals(Collections.emptySet(), kb.getEquivalentProperties(p));
		assertEquals(Collections.singleton(Collections.singleton(TermFactory.BOTTOM_OBJECT_PROPERTY)), kb.getSubProperties(p));
		assertEquals(Collections.singleton(Collections.singleton(TermFactory.TOP_OBJECT_PROPERTY)), kb.getSuperProperties(p));

		assertEquals(Collections.singleton(C), kb.getAllEquivalentClasses(C));
		assertEquals(Collections.emptySet(), kb.getEquivalentClasses(C));
		assertEquals(Collections.emptySet(), kb.getSubClasses(p));
		assertEquals(Collections.emptySet(), kb.getSuperClasses(p));

		assertEquals(Collections.emptySet(), kb.getAllEquivalentProperties(undef));
		assertEquals(Collections.emptySet(), kb.getEquivalentProperties(undef));
		assertEquals(Collections.emptySet(), kb.getSubProperties(undef));
		assertEquals(Collections.emptySet(), kb.getSuperProperties(undef));

		assertEquals(Collections.emptySet(), kb.getAllEquivalentClasses(undef));
		assertEquals(Collections.emptySet(), kb.getEquivalentClasses(undef));
		assertEquals(Collections.emptySet(), kb.getSubClasses(undef));
		assertEquals(Collections.emptySet(), kb.getSuperClasses(undef));
	}

	@Test
	public void testDatatypeReasoner()
	{
		// This test case checks datatype reasoner to handle obvious
		// contradictions, e.g. intersection of datatypes D and not(D)
		// See the bug reported in ticket #127

		dataProperties(_p);
		individuals(_a);

		_kb.addRange(_p, Datatypes.FLOAT);

		_kb.addPropertyValue(_p, _a, literal(42.0f));

		assertTrue(_kb.isConsistent());

		assertTrue(_kb.isType(_a, some(_p, Datatypes.FLOAT)));
	}

	@Test
	public void testCRonDTP()
	{
		// Test for ticket #143

		final Properties newOptions = new PropertiesBuilder().set("SILENT_UNDEFINED_ENTITY_HANDLING", "false").build();
		final Properties savedOptions = OpenlletOptions.setOptions(newOptions);

		try
		{
			final KnowledgeBase kb = new KnowledgeBaseImpl();

			final ATermAppl p = term("p");
			final ATermAppl c = and(all(p, value(literal("s"))), min(p, 2, value(literal("l"))));

			kb.addDatatypeProperty(p);

			assertFalse(kb.isSatisfiable(c));
		}
		finally
		{
			OpenlletOptions.setOptions(savedOptions);
		}
	}

	@Test
	public void testInvalidTransitivity2()
	{
		final KBLoader[] loaders = { new JenaLoader() };
		for (final KBLoader loader : loaders)
		{
			final KnowledgeBase kb = loader.createKB(_base + "invalidTransitivity.owl");

			for (final Role r : kb.getRBox().getRoles().values())
				if (!ATermUtils.isBuiltinProperty(r.getName()))
				{
					assertTrue(r.toString(), r.isSimple());
					assertFalse(r.toString(), r.isTransitive());
				}

			for (final ATermAppl p : kb.getObjectProperties())
				if (!ATermUtils.isBuiltinProperty(p))
					assertFalse(p.toString(), kb.isTransitiveProperty(p));
		}
	}

	@Test
	public void testInternalization()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");
		final ATermAppl C = term("C");

		kb.addClass(A);
		kb.addClass(B);
		kb.addClass(C);

		kb.addSubClass(TOP, and(or(B, not(A)), or(C, not(B))));

		assertSubClass(kb, A, B);

		assertSubClass(kb, B, C);

		kb.classify();
	}

	@Test
	public void testNominalCache()
	{
		// this case tests isMergable check and specifically the correctness of
		// ConceptCache.isIndependent value. concept C below will be merged to
		// either a or b with a dependency. if that dependency is not recorded
		// isMergable returns incorrect results

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");
		final ATermAppl C = term("C");
		final ATermAppl a = term("a");
		final ATermAppl b = term("b");

		kb.addClass(C);
		kb.addIndividual(a);
		kb.addIndividual(b);

		kb.addSubClass(C, oneOf(a, b));
		kb.addEquivalentClass(A, oneOf(a));
		kb.addEquivalentClass(B, oneOf(b));
		kb.addDisjointClass(A, B);

		assertTrue(kb.isConsistent());

		assertTrue(kb.isSatisfiable(C));

		assertNotSubClass(kb, C, A);
		assertNotSubClass(kb, C, B);

		assertNotSubClass(kb, A, C);
		assertNotSubClass(kb, B, C);
		assertFalse(kb.isType(a, C));
		assertFalse(kb.isType(b, C));
		assertFalse(kb.isType(a, not(C)));
		assertFalse(kb.isType(b, not(C)));
	}

	/*
	 * From bug #312
	 */
	@Test
	public void testNominalValueInteraction()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl t1 = term("T1");
		final ATermAppl p = term("p");
		final ATermAppl i1 = term("i1");
		final ATermAppl i21 = term("i21");
		final ATermAppl i22 = term("i22");
		final ATermAppl t1eq = ATermUtils.makeAnd(ATermUtils.makeHasValue(p, i21), ATermUtils.makeHasValue(p, i22));
		final ATermAppl test = term("test");

		kb.addClass(t1);
		kb.addObjectProperty(p);
		kb.addIndividual(i1);
		kb.addIndividual(i21);
		kb.addIndividual(i22);
		kb.addIndividual(test);

		kb.addEquivalentClass(t1, t1eq);
		kb.addSame(i1, i21);
		kb.addSame(i21, i1);

		kb.addPropertyValue(p, test, i21);
		kb.addPropertyValue(p, test, i22);

		final Set<ATermAppl> t1inds = kb.retrieve(t1eq, kb.getIndividuals());
		assertEquals("Individual test should be of type T1. ", Collections.singleton(test), t1inds);

	}

	@Test
	public void testMultiEdgesWithTransitivity()
	{
		// Demonstrate the problem described in #223
		// This test is more complicated than necessary to ensure
		// that bug is triggered regardless of the traversal _order
		// in getTransitivePropertyValues function. With the bug
		// if we visit b before c we will miss the value e and if we visit c
		// before b we miss the value d.

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl b = term("b");
		final ATermAppl c = term("c");
		final ATermAppl d = term("d");
		final ATermAppl e = term("e");

		final ATermAppl p = term("p");
		final ATermAppl r = term("r");
		final ATermAppl s = term("s");

		kb.addObjectProperty(p);
		kb.addObjectProperty(r);
		kb.addObjectProperty(s);

		kb.addTransitiveProperty(r);
		kb.addTransitiveProperty(s);

		kb.addSubProperty(r, p);
		kb.addSubProperty(s, p);

		kb.addIndividual(a);
		kb.addIndividual(b);
		kb.addIndividual(c);
		kb.addIndividual(d);
		kb.addIndividual(e);

		kb.addPropertyValue(r, a, b);
		kb.addPropertyValue(r, b, c);
		kb.addPropertyValue(r, b, d);
		kb.addPropertyValue(s, a, c);
		kb.addPropertyValue(s, c, b);
		kb.addPropertyValue(s, c, e);

		assertTrue(kb.hasPropertyValue(a, p, b));
		assertTrue(kb.hasPropertyValue(a, p, c));
		assertTrue(kb.hasPropertyValue(a, p, d));
		assertTrue(kb.hasPropertyValue(a, p, e));
		assertIteratorValues(kb.getPropertyValues(p, a).iterator(), new ATermAppl[] { b, c, d, e });
	}

	@Test
	public void testLiteralMerge()
	{
		// Tests the issue described in #250
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl a = term("a");
		final ATermAppl b = term("b");
		final ATermAppl p = term("p");

		kb.addIndividual(a);
		kb.addIndividual(b);

		kb.addDatatypeProperty(p);
		kb.addFunctionalProperty(p);

		// a has a p-successor which is an integer
		kb.addType(a, some(p, XSDInteger.getInstance().getName()));
		// bogus axiom to force full datatype reasoning
		kb.addType(a, max(p, 2, TOP_LIT));

		// b has an asserted p value which is a string
		kb.addPropertyValue(p, b, literal("b"));

		// check consistency whihc
		assertTrue(kb.isConsistent());

		// this query will force a and b to be merged which will cause
		// their p values to be merged
		assertTrue(kb.isDifferentFrom(a, b));
	}

	@Test
	public void testDatatypeSubProperty1a()
	{
		// Tests the issue described in #250
		// The sub/equivalent property query was turned into a satisfiability
		// test where a fresh datatype is used. If the property in question
		// has a range the intersection of defined range with the fresh
		// datatype returned to be empty causing the reasoner to conclude
		// subproperty relation hols even though it does not

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl p = term("p");
		final ATermAppl q = term("q");

		final ATermAppl[] ranges = { null, XSDInteger.getInstance().getName(), XSDString.getInstance().getName() };

		for (final ATermAppl rangeP : ranges)
			for (final ATermAppl rangeQ : ranges)
			{
				kb.clear();

				kb.addDatatypeProperty(p);
				kb.addDatatypeProperty(q);

				if (rangeP != null)
					kb.addRange(p, rangeP);
				if (rangeQ != null)
					kb.addRange(q, rangeQ);

				assertTrue(kb.isConsistent());

				assertFalse(kb.isSubPropertyOf(p, q));
				assertFalse(kb.isSubPropertyOf(q, p));

				assertFalse(kb.isEquivalentProperty(p, q));
				assertFalse(kb.isEquivalentProperty(q, p));
			}
	}

	@Test
	public void testDatatypeSubProperty1b()
	{
		// Another variation of testDatatypeSubProperty1 where super
		// property has a range but not the sub property

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl C = term("C");

		final ATermAppl p = term("p");
		final ATermAppl q = term("q");

		kb.addClass(C);

		kb.addDatatypeProperty(p);
		kb.addDatatypeProperty(q);

		kb.addDomain(p, C);

		kb.addRange(q, XSDInteger.getInstance().getName());

		kb.addSubClass(C, some(q, TOP_LIT));

		assertTrue(kb.isConsistent());

		assertFalse(kb.isSubPropertyOf(p, q));
		assertFalse(kb.isSubPropertyOf(q, p));

		assertFalse(kb.isEquivalentProperty(p, q));
		assertFalse(kb.isEquivalentProperty(q, p));
	}

	@Test
	public void testCachedNominalEdge()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");
		final ATermAppl C = term("C");
		final ATermAppl D = term("D");

		final ATermAppl p = term("p");

		final ATermAppl b = term("b");
		final ATermAppl c = term("c");

		kb.addClass(A);
		kb.addClass(B);
		kb.addClass(C);
		kb.addClass(D);

		kb.addObjectProperty(p);

		kb.addIndividual(b);
		kb.addIndividual(c);

		kb.addEquivalentClass(A, oneOf(b, c));
		kb.addEquivalentClass(B, hasValue(p, b));
		kb.addEquivalentClass(C, hasValue(p, c));
		kb.addEquivalentClass(D, and(some(p, A), min(p, 1, value(b)), min(p, 1, value(c)), max(p, 1, TOP)));

		assertTrue(kb.isConsistent());

		kb.classify();

		assertTrue(kb.isSubClassOf(D, B));
		assertTrue(kb.isSubClassOf(D, C));
	}

	@Test
	public void testDisjoints()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");
		final ATermAppl C = term("C");
		final ATermAppl D = term("D");

		kb.addClass(A);
		kb.addClass(B);
		kb.addClass(C);
		kb.addClass(D);

		kb.addSubClass(B, A);
		kb.addSubClass(D, C);
		kb.addComplementClass(B, C);

		assertTrue(kb.isConsistent());

		assertIteratorValues(kb.getDisjointClasses(TOP).iterator(), new Object[] { singleton(BOTTOM) });
		assertIteratorValues(kb.getDisjointClasses(A).iterator(), new Object[] { singleton(BOTTOM) });
		assertIteratorValues(kb.getDisjointClasses(B).iterator(), new Object[] { singleton(BOTTOM), singleton(C), singleton(D) });
		assertIteratorValues(kb.getDisjointClasses(C).iterator(), new Object[] { singleton(BOTTOM), singleton(B) });
		assertIteratorValues(kb.getDisjointClasses(D).iterator(), new Object[] { singleton(BOTTOM), singleton(B) });
		assertIteratorValues(kb.getDisjointClasses(BOTTOM).iterator(), new Object[] { singleton(TOP), singleton(A), singleton(B), singleton(C), singleton(D), singleton(BOTTOM) });

		assertIteratorValues(kb.getComplements(TOP).iterator(), new Object[] { BOTTOM });
		assertTrue(kb.getComplements(A).isEmpty());
		assertIteratorValues(kb.getComplements(B).iterator(), new Object[] { C });
		assertIteratorValues(kb.getComplements(C).iterator(), new Object[] { B });
		assertTrue(kb.getComplements(D).isEmpty());
		assertIteratorValues(kb.getComplements(BOTTOM).iterator(), new Object[] { TOP });

		assertIteratorValues(kb.getDisjointClasses(not(A)).iterator(), new Object[] { singleton(BOTTOM), singleton(A), singleton(B) });
		assertIteratorValues(kb.getDisjointClasses(not(B)).iterator(), new Object[] { singleton(BOTTOM), singleton(B) });
		assertIteratorValues(kb.getDisjointClasses(not(C)).iterator(), new Object[] { singleton(BOTTOM), singleton(C), singleton(D) });
		assertIteratorValues(kb.getDisjointClasses(not(D)).iterator(), new Object[] { singleton(BOTTOM), singleton(D) });

		assertIteratorValues(kb.getComplements(not(A)).iterator(), new Object[] { A });
		assertIteratorValues(kb.getComplements(not(B)).iterator(), new Object[] { B });
		assertIteratorValues(kb.getComplements(not(C)).iterator(), new Object[] { C });
		assertIteratorValues(kb.getComplements(not(D)).iterator(), new Object[] { D });
	}

	/**
	 * But #305
	 */
	@Test
	public void testDisjointDataProperties()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl p = term("p");
		final ATermAppl q = term("q");

		kb.addDatatypeProperty(p);
		kb.addDatatypeProperty(q);
		kb.addRange(p, Datatypes.INT);
		kb.addRange(q, Datatypes.INT);

		assertFalse("p and q should not be disjoint!", kb.isDisjointProperty(p, q));
	}

	@Test
	public void testRemovePruned()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");
		final ATermAppl C = term("C");

		final ATermAppl p = term("p");

		final ATermAppl a = term("a");
		final ATermAppl b = term("b");

		kb.addClass(A);
		kb.addClass(B);
		kb.addClass(C);

		kb.addObjectProperty(p);

		kb.addIndividual(a);
		kb.addIndividual(b);

		kb.addEquivalentClass(A, value(a));
		kb.addSubClass(A, all(inv(p), not(B)));
		kb.addSubClass(B, or(some(p, A), C));

		kb.addType(b, B);

		assertTrue(kb.isConsistent());

		assertTrue(kb.isType(b, C));
		assertFalse(kb.isType(a, C));
	}

	@Test
	public void testDataAssertions()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");

		final ATermAppl p = term("p");

		final ATermAppl a = term("a");

		final ATermAppl oneDecimal = literal("1", Datatypes.DECIMAL);
		final ATermAppl oneInteger = literal("1", Datatypes.INTEGER);
		final ATermAppl oneByte = literal("1", Datatypes.BYTE);
		final ATermAppl oneFloat = literal("1", Datatypes.FLOAT);

		kb.addClass(A);

		kb.addDatatypeProperty(p);

		kb.addIndividual(a);

		kb.addPropertyValue(p, a, oneInteger);
		assertTrue(kb.isConsistent());

		assertTrue(kb.hasPropertyValue(a, p, oneDecimal));
		assertTrue(kb.hasPropertyValue(a, p, oneInteger));
		assertTrue(kb.hasPropertyValue(a, p, oneByte));
		assertFalse(kb.hasPropertyValue(a, p, oneFloat));
		assertEquals(singletonList(oneInteger), kb.getDataPropertyValues(p, a));
	}

	@Test
	public void testDatatypeIntersection()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl p = term("p");
		final ATermAppl a = term("a");

		final ATermAppl zeroDecimal = literal("0", Datatypes.DECIMAL);
		final ATermAppl zeroInteger = literal("0", Datatypes.INTEGER);
		final ATermAppl zeroByte = literal("0", Datatypes.BYTE);
		final ATermAppl zeroFloat = literal("0", Datatypes.FLOAT);

		kb.addClass(A);
		kb.addDatatypeProperty(p);
		kb.addIndividual(a);

		kb.addSubClass(A, some(p, Datatypes.NON_POSITIVE_INTEGER));
		kb.addSubClass(A, all(p, Datatypes.NON_NEGATIVE_INTEGER));

		kb.addType(a, A);

		assertTrue(kb.isConsistent());

		assertTrue(kb.hasPropertyValue(a, p, zeroDecimal));
		assertTrue(kb.hasPropertyValue(a, p, zeroInteger));
		assertTrue(kb.hasPropertyValue(a, p, zeroByte));
		assertFalse(kb.hasPropertyValue(a, p, zeroFloat));
		assertEquals(singletonList(zeroDecimal), kb.getDataPropertyValues(p, a));
	}

	@Test
	public void testDataOneOf()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");

		final ATermAppl p = term("p");
		final ATermAppl q = term("q");

		final ATermAppl a = term("a");

		final ATermAppl lit1 = literal("test");
		final ATermAppl lit2 = literal("1", Datatypes.DECIMAL);

		kb.addClass(A);

		kb.addDatatypeProperty(p);
		kb.addDatatypeProperty(q);

		kb.addIndividual(a);

		kb.addType(a, A);

		kb.addSubClass(A, min(p, 1, TOP_LIT));
		kb.addRange(p, oneOf(lit1));

		kb.addSubClass(A, some(q, TOP_LIT));
		kb.addRange(q, oneOf(lit2));

		assertTrue(kb.isConsistent());

		assertEquals(singletonList(lit1), kb.getDataPropertyValues(p, a));
		assertEquals(singletonList(lit2), kb.getDataPropertyValues(q, a));
		assertTrue(kb.hasPropertyValue(a, p, lit1));
		assertTrue(kb.hasPropertyValue(a, q, lit2));
	}

	@Test
	public void testDisjointSelf()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl p = term("p");

		kb.addClass(A);
		kb.addObjectProperty(p);

		kb.addDisjointClasses(Arrays.asList(A, self(p)));

		kb.classify();

		assertTrue(kb.isSatisfiable(A));
	}

	@Test
	public void testDisjointPropertiesCache()
	{
		// test case for issue #336 to verify AbstractConceptCache.isMergable does
		// not return incorrect results.
		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl p1 = term("p1");
		final ATermAppl p2 = term("p2");

		final ATermAppl a = term("a");
		final ATermAppl b = term("b");
		final ATermAppl c = term("c");

		kb.addObjectProperty(p1);
		kb.addObjectProperty(p2);
		kb.addDisjointProperty(p1, p2);

		kb.addIndividual(a);
		kb.addIndividual(b);
		kb.addIndividual(c);

		kb.addPropertyValue(p1, a, c);
		kb.addPropertyValue(p2, b, a);

		final ATermAppl notp1a = ATermUtils.makeNot(ATermUtils.makeHasValue(p1, a));

		// no caching so consistency checking will be used here
		assertFalse(kb.isType(a, notp1a));
		assertTrue(kb.isType(b, notp1a));

		// call getInstances so some caching will happen
		assertEquals(singleton(b), kb.getInstances(notp1a, false));

		// now cached _nodes will be used for mergable check
		assertFalse(kb.isType(a, notp1a));
		assertTrue(kb.isType(b, notp1a));

	}

	@Test
	public void testSynoymClassification()
	{
		// Fixes the problem identified in #270. If there are two equivalent concepts
		// where one is primitive and the other is non-primitive CD classifier was
		// picking primitive flag and returning incorrect classification results.

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl A = term("A");
		final ATermAppl B = term("B");
		final ATermAppl C = term("C");

		final ATermAppl p = term("p");

		kb.addClass(A);
		kb.addClass(B);
		kb.addClass(C);

		kb.addDatatypeProperty(p);

		// B is completely defined except this equivalence
		kb.addEquivalentClass(A, B);
		// A is not primitive because of the domain axiom
		kb.addDomain(p, A);
		// C should be inferred to be a subclass of A and B
		kb.addSubClass(C, some(p, TOP_LIT));

		kb.classify();

		assertSubClass(kb, C, A);
		assertSubClass(kb, C, B);
	}

	@Test
	public void testUndefinedProperty()
	{
		// Test for #351. Calling getPropertyValues for an undefinde property should not throw NPE

		final KnowledgeBase kb = new KnowledgeBaseImpl();

		final ATermAppl p = term("p");
		final ATermAppl q = term("q");

		final ATermAppl a = term("a");
		final ATermAppl b = term("b");

		kb.addObjectProperty(p);

		kb.addIndividual(a);
		kb.addIndividual(b);

		kb.addPropertyValue(p, a, b);

		kb.isConsistent();

		assertTrue(kb.getPropertyValues(q).isEmpty());
	}

	@Test
	public void testGetSubClassBehavior()
	{
		classes(_c, _d, _e);

		_kb.addEquivalentClass(_c, _d);
		_kb.addSubClass(_e, _d);

		final Set<Set<ATermAppl>> result = new HashSet<>();
		result.add(Collections.singleton(ATermUtils.BOTTOM));
		result.add(Collections.singleton(_e));
		assertEquals(result, _kb.getSubClasses(_c, false));
	}

	@Test
	public void test354()
	{
		// test case for issue #354.
		classes(_B);
		objectProperties(_p);
		individuals(_a, _b, _c);

		_kb.addFunctionalProperty(_p);

		_kb.addEquivalentClass(_B, oneOf(_b, _c));

		assertFalse(_kb.isType(_a, not(_B)));

		_kb.isSatisfiable(_B);

		assertFalse(_kb.isType(_a, not(_B)));
	}

	@Test
	public void test370()
	{
		// test case for issue #370.
		dataProperties(_p);
		individuals(_a);

		final ATermAppl dt = restrict(Datatypes.DECIMAL, minExclusive(literal("0.99", Datatypes.DECIMAL)), maxExclusive(literal("1.01", Datatypes.DECIMAL)));

		_kb.addType(_a, min(_p, 3, dt));

		assertTrue(_kb.isConsistent());
	}

	@Test
	public void test348()
	{
		// test case for issue #348.
		classes(_B, _C, _D, _E);
		individuals(_a, _b, _c, _d, _e);

		_kb.addType(_a, oneOf(_b, _c));
		_kb.addType(_a, oneOf(_d, _e));

		_kb.addType(_b, _B);
		_kb.addType(_c, _C);
		_kb.addType(_d, _D);
		_kb.addType(_e, _E);

		assertTrue(_kb.isConsistent());

		assertEquals(Collections.singleton(_b), _kb.retrieve(_B, Arrays.asList(_a, _b, _d, _e)));
		assertEquals(Collections.singleton(_c), _kb.retrieve(_C, Arrays.asList(_a, _c, _d, _e)));
		assertEquals(Collections.singleton(_d), _kb.retrieve(_D, Arrays.asList(_a, _d, _b, _c)));
		assertEquals(Collections.singleton(_e), _kb.retrieve(_E, Arrays.asList(_a, _e, _b, _c)));
	}

	@Test
	public void test375()
	{
		// test case for issue #375.
		classes(_A, _B, _C);
		dataProperties(_p);

		final ATermAppl dt = restrict(Datatypes.INTEGER, minExclusive(literal(1)));

		_kb.addRange(_p, XSDInteger.getInstance().getName());
		_kb.addSubClass(_A, _C);
		_kb.addEquivalentClass(_A, some(_p, dt));
		_kb.addSubClass(_B, _C);
		_kb.addEquivalentClass(_B, hasValue(_p, literal(2)));

		assertTrue(_kb.isConsistent());

		assertSubClass(_kb, _B, _A);

		_kb.classify();
		_kb.printClassTree();

		assertSubClass(_kb, _B, _A);

	}

	@Test
	public void minCardinalityOnIrreflexive()
	{
		// related to #400
		classes(_A);
		objectProperties(_p);
		individuals(_a);

		_kb.addIrreflexiveProperty(_p);
		_kb.addSubClass(_A, min(_p, 1, TOP));
		_kb.addEquivalentClass(_A, oneOf(_a));
		_kb.addEquivalentClass(TOP, _A);

		assertFalse(_kb.isConsistent());
	}

	@Test
	public void subPropertyWithSameRange()
	{
		// test #435
		classes(_A);
		objectProperties(_p, _q, _r);

		_kb.addRange(_p, _A);
		_kb.addDomain(_p, some(_q, _A));

		assertTrue(_kb.isConsistent());

		assertFalse(_kb.isSubPropertyOf(_p, _q));
		assertFalse(_kb.isSubPropertyOf(_q, _p));
	}

	@Test
	public void roleAbsorptionWithQCR()
	{
		classes(_A, _B, _C);
		objectProperties(_p);

		_kb.addSubClass(_A, _B);
		_kb.addEquivalentClass(_A, min(_p, 1, _B));
		_kb.addSubClass(_C, min(_p, 1, TOP));

		assertNotSubClass(_kb, _C, _A);
	}

	@Test
	public void testUnsatClasses1()
	{
		classes(_B, _C, _D);

		_kb.addSubClass(_B, and(_C, _D));

		assertTrue(_kb.getUnsatisfiableClasses().isEmpty());
		assertEquals(singleton(BOTTOM), _kb.getAllUnsatisfiableClasses());

		assertFalse(_kb.isClassified());

		assertTrue(_kb.getUnsatisfiableClasses().isEmpty());
		assertEquals(singleton(BOTTOM), _kb.getAllUnsatisfiableClasses());
	}

	@Test
	public void testUnsatClasses2()
	{
		classes(_B, _C, _D);

		_kb.addDisjointClass(_C, _D);
		_kb.addSubClass(_B, and(_C, _D));

		assertEquals(singleton(_B), _kb.getUnsatisfiableClasses());
		assertEquals(SetUtils.create(_B, BOTTOM), _kb.getAllUnsatisfiableClasses());

		assertFalse(_kb.isClassified());

		assertEquals(singleton(_B), _kb.getUnsatisfiableClasses());
		assertEquals(SetUtils.create(_B, BOTTOM), _kb.getAllUnsatisfiableClasses());
	}

	@Test
	public void testGuessingRule()
	{
		classes(_C, _D);
		objectProperties(_p);
		individuals(_a, _b);

		_kb.addEquivalentClass(_C, hasValue(inv(_p), _a));

		_kb.addType(_a, card(_p, 2, _D));
		_kb.addType(_a, card(_p, 3, TOP));

		assertTrue(_kb.isConsistent());
		assertTrue(_kb.isSatisfiable(_C));
	}

	@Test
	public void testGuessingRule2()
	{
		// ticket #488
		classes(_A, _B, _C);
		objectProperties(_p, _q);
		individuals(_a);

		_kb.addInverseProperty(_p, _q);
		_kb.addDomain(_p, _A);
		_kb.addRange(_p, or(_B, _C));

		_kb.addSubClass(_A, card(_p, 1, _B));
		_kb.addSubClass(_A, card(_p, 1, _C));

		_kb.addSubClass(_B, card(_q, 1, _A));
		_kb.addSubClass(_C, card(_q, 1, _A));

		_kb.addDisjointClasses(Arrays.asList(_A, _B, _C));

		_kb.addEquivalentClass(_A, oneOf(_a));

		assertTrue(_kb.isConsistent());
		assertEquals(Collections.emptySet(), _kb.getUnsatisfiableClasses());
	}

	@Test
	public void test484()
	{
		dataProperties(_p);
		individuals(_a);

		final ATermAppl dt = restrict(Datatypes.INTEGER, minExclusive(literal(0)), maxExclusive(literal(0)));

		_kb.addType(_a, some(_p, dt));

		assertFalse(_kb.isConsistent());

		assertEquals(Clash.ClashType.EMPTY_DATATYPE, _kb.getABox().getLastClash().getType());
	}

	@Test
	public void test485()
	{
		final Properties oldOptions = OpenlletOptions.setOptions(PropertiesBuilder.singleton("DISABLE_EL_CLASSIFIER", "true"));
		try
		{
			classes(_A, _B, _C);
			objectProperties(_p, _q);
			individuals(_a, _b);

			_kb.addSubClass(_B, _A);
			_kb.addSubClass(_C, _A);

			_kb.addDomain(_p, _B);
			_kb.addDomain(_q, _A);

			_kb.addType(_a, _A);
			_kb.addType(_b, _B);

			_kb.realize();

			assertEquals(SetUtils.create(_A, _B, TOP), IteratorUtils.toSet(new FlattenningIterator<>(_kb.getSuperClasses(some(_p, TOP)))));
			assertEquals(SetUtils.create(_A, TOP), IteratorUtils.toSet(new FlattenningIterator<>(_kb.getSuperClasses(some(_q, TOP)))));
		}
		finally
		{
			OpenlletOptions.setOptions(oldOptions);
		}
	}

	@Test
	public void test518()
	{
		// tests if the interaction between some values restriction and inverses ends up creating a cycle in the
		// completion graph

		classes(_A, _B, _C);
		objectProperties(_p, _q);

		_kb.addInverseFunctionalProperty(_p);
		_kb.addSubProperty(_q, inv(_p));

		assertFalse(_kb.isSatisfiable(some(_p, some(_q, all(_p, BOTTOM)))));
	}

	@Test
	public void test553()
	{
		final KnowledgeBase kb = new KnowledgeBaseImpl();
		final KnowledgeBase copyKB = kb.copy();
		assertTrue(copyKB != kb);
		assertTrue(copyKB.getABox().getKB() == copyKB);
	}

	@Test
	public void testFunctionalSubDataProperty()
	{
		// test for ticket #551

		individuals(_a);
		dataProperties(_p, _q);

		_kb.addFunctionalProperty(_p);
		_kb.addSubProperty(_q, _p);

		_kb.addPropertyValue(_p, _a, literal("val1"));
		_kb.addPropertyValue(_q, _a, literal("val2"));

		assertFalse(_kb.isConsistent());
	}

	@Test
	public void test549()
	{
		final int n = 5;
		final ATermAppl[] ind = new ATermAppl[n];
		for (int i = 0; i < n; i++)
			ind[i] = term("ind" + i);
		final ATermAppl[] cls = new ATermAppl[n];
		for (int i = 0; i < n; i++)
			cls[i] = term("C" + i);

		classes(cls);
		dataProperties(_p);
		individuals(ind);

		_kb.addClass(_C);

		float lower = 1.0f;
		final float increment = 1.0f;
		for (int i = 0; i < n; i++)
		{
			_kb.addSubClass(cls[i], _C);
			_kb.addType(ind[i], _C);

			final float upper = lower + increment;
			final ATermAppl dt = term("D" + i);
			final ATermAppl def = restrict(Datatypes.FLOAT, minInclusive(literal(lower)), maxExclusive(literal(upper)));
			_kb.addDatatypeDefinition(dt, def);

			_kb.addEquivalentClass(cls[i], some(_p, dt));
			_kb.addPropertyValue(_p, ind[i], literal(lower));
			lower = upper;
		}

		//		_kb.realize();
		//		_kb.printClassTree();

		for (int i = 0; i < n; i++)
			assertEquals(Collections.singleton(ind[i]), _kb.getInstances(cls[i]));

	}

	@Test
	public void test532a()
	{
		classes(_A, _B, _C, _D);
		individuals(_a, _b, _c, _d);
		objectProperties(_p, _q);

		_kb.addDisjointClasses(Arrays.asList(_A, _B, _C, _D));

		_kb.addType(_a, or(_A, _B));
		_kb.addType(_b, or(_C, _D));

		assertTrue(_kb.isConsistent());

		_kb.addSame(_a, _b);

		assertFalse(_kb.isConsistent());
	}

	@Test
	public void test532b()
	{
		// variation of the _condition in 532 where the _nodes involved in MaxBranch are merged
		classes(_C, _D, _E);
		individuals(_a, _b, _c, _d, _e, _f);
		objectProperties(_p);

		_kb.addType(_a, max(_p, 2, TOP));
		_kb.addType(_a, min(_p, 2, TOP));
		_kb.addPropertyValue(_p, _a, _b);
		_kb.addPropertyValue(_p, _a, _c);
		_kb.addPropertyValue(_p, _a, _d);

		assertTrue(_kb.isConsistent());

		_kb.addSame(_c, _e);
		_kb.addSame(_d, _e);

		assertTrue(_kb.isConsistent());
	}

	@Test
	public void test560()
	{
		classes(_A, _B);
		individuals(_a);
		objectProperties(_p, _q);

		_kb.addFunctionalProperty(_p);
		_kb.addSubProperty(_q, _p);
		_kb.addSubClass(_A, hasValue(_q, _a));
		_kb.addType(_a, all(inv(_q), all(inv(_p), oneOf(_a))));

		assertTrue(_kb.isConsistent());
		assertTrue(_kb.isSatisfiable(and(some(_p, _A), some(_q, _B))));
	}

	@Test
	public void testAutoRealizeEnabled()
	{
		testAutoRealize(true);
	}

	@Test
	public void testAutoRealizeDisabled()
	{
		testAutoRealize(false);
	}

	private void testAutoRealize(final boolean autoRealize)
	{
		final Properties newOptions = PropertiesBuilder.singleton("AUTO_REALIZE", String.valueOf(autoRealize));
		final Properties oldOptions = OpenlletOptions.setOptions(newOptions);

		try
		{
			classes(_A, _B, _C);
			individuals(_a, _b);

			_kb.addSubClass(_B, _A);
			_kb.addType(_a, _A);
			_kb.addType(_b, _B);

			assertTrue(_kb.isConsistent());

			assertTrue(TaxonomyUtils.getTypes(_kb.getTaxonomy(), _a, false).isEmpty());

			assertEquals(singletonSets(_A, ATermUtils.TOP), _kb.getTypes(_a, false));
			assertEquals(autoRealize, _kb.isRealized());
			assertFalse(TaxonomyUtils.getTypes(_kb.getTaxonomy(), _a, false).isEmpty());

			assertEquals(autoRealize, !TaxonomyUtils.getTypes(_kb.getTaxonomy(), _b, false).isEmpty());

			assertEquals(singletonSets(_B), _kb.getTypes(_b, true));
			assertEquals(autoRealize, _kb.isRealized());
			assertFalse(TaxonomyUtils.getTypes(_kb.getTaxonomy(), _a, false).isEmpty());
		}
		finally
		{
			OpenlletOptions.setOptions(oldOptions);
		}

	}
}
