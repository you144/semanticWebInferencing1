// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.tbox;

import static openllet.core.utils.ATermUtils.makeAnd;
import static openllet.core.utils.ATermUtils.makeEqClasses;
import static openllet.core.utils.ATermUtils.makeNot;
import static openllet.core.utils.ATermUtils.makeOr;
import static openllet.core.utils.ATermUtils.makeSub;
import static openllet.core.utils.TermFactory.and;
import static openllet.core.utils.TermFactory.inv;
import static openllet.core.utils.TermFactory.or;
import static openllet.core.utils.TermFactory.some;
import static openllet.core.utils.TermFactory.term;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import junit.framework.JUnit4TestAdapter;
import openllet.aterm.ATermAppl;
import openllet.core.OpenlletOptions;
import openllet.core.boxes.tbox.TBox;
import openllet.core.boxes.tbox.impl.Unfolding;
import openllet.core.utils.iterator.IteratorUtils;
import openllet.test.AbstractKBTests;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * Title: TBoxTests
 * </p>
 * <p>
 * Description: TBox unit tests (those than can be done without depending on the Knowledgebase or ABox)
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Mike Smith
 */
public class TBoxTests extends AbstractKBTests
{

	public static junit.framework.Test suite()
	{
		return new JUnit4TestAdapter(TBoxTests.class);
	}

	private TBox tbox;

	@Override
	@Before
	public void initializeKB()
	{
		super.initializeKB();
		tbox = _kb.getTBox();
	}

	private void prepareTBox()
	{
		tbox.prepare();
	}

	/**
	 * Test that _tbox axioms which have been "simplified away" during absorption are re-added if removal of another _tbox axiom necessitates it.
	 */
	@Test
	public void removedByAbsorbReaddedOnChange()
	{

		final boolean oldTracing = OpenlletOptions.USE_TRACING;
		OpenlletOptions.USE_TRACING = true;
		try
		{
			classes(_A, _B, _C, _D);

			final ATermAppl axiom1 = makeEqClasses(_A, makeOr(_C, _D));
			assertTrue(tbox.addAxiom(axiom1));

			final ATermAppl axiom2 = makeSub(_A, _B);
			assertTrue(tbox.addAxiom(axiom2));

			final Unfolding unfoldForAxiom2 = Unfolding.create(_B, Collections.singleton(axiom2));

			prepareTBox();

			/*
			 * At this stage the TBox does not *directly* contain A [= B , but
			 * it should be implicit by A [= C u D , C [= B , D [= B. Note that
			 * if this assertion fails, it does not necessarily mean that the
			 * TBox implementation is broken, it may mean the implementation has
			 * changed in a way that makes this test not useful.
			 */
			assertFalse(IteratorUtils.toSet(tbox.unfold(_A)).contains(unfoldForAxiom2));

			tbox.removeAxiom(axiom1);
			prepareTBox();

			/*
			 * After the equivalence is removed, any simplification (e.g., the
			 * one above) must be corrected.
			 */
			assertTrue(IteratorUtils.toSet(tbox.unfold(_A)).contains(unfoldForAxiom2));
		}
		finally
		{
			OpenlletOptions.USE_TRACING = oldTracing;
		}
	}

	@Test
	public void assertedAxioms()
	{
		classes(_A, _B, _C, _D);

		final ATermAppl axiom = makeSub(makeAnd(_A, _B), makeNot(_B));
		tbox.addAxiom(axiom);

		prepareTBox();

		assertTrue(tbox.getAxioms().size() > 1);
		assertTrue(tbox.getAxioms().contains(axiom));
		assertEquals(Collections.singleton(axiom), tbox.getAssertedAxioms());
	}

	@Test
	public void binaryAbsorption()
	{
		final ATermAppl SPECIALCLIENT = term("SPECIALCLIENT");
		final ATermAppl CLIENT = term("CLIENT");
		final ATermAppl EXPENSIVE = term("EXPENSIVE");
		final ATermAppl PROFITABLE = term("PROFITABLE");
		final ATermAppl TRUSTEDCLIENT = term("TRUSTEDCLIENT");
		final ATermAppl Recommend = term("Recommend");
		final ATermAppl Buy = term("Buy");

		classes(SPECIALCLIENT, CLIENT, EXPENSIVE, PROFITABLE, TRUSTEDCLIENT);
		objectProperties(Buy, Recommend);

		tbox.addAxiom(makeSub(SPECIALCLIENT, TRUSTEDCLIENT));
		tbox.addAxiom(makeEqClasses(SPECIALCLIENT, and(CLIENT, some(Buy, or(EXPENSIVE, PROFITABLE)), some(inv(Recommend), TRUSTEDCLIENT))));

		prepareTBox();
	}

	@Test
	public void removeAssertedAxioms()
	{
		final boolean oldTracing = OpenlletOptions.USE_TRACING;
		OpenlletOptions.USE_TRACING = true;
		try
		{
			classes(_A, _B, _C, _D);

			final ATermAppl axiom = makeSub(makeAnd(_A, _B), makeNot(_B));
			tbox.addAxiom(axiom);

			prepareTBox();

			tbox.removeAxiom(axiom);

			prepareTBox();

			assertTrue(tbox.getAxioms().isEmpty());
			assertTrue(tbox.getAssertedAxioms().isEmpty());
		}
		finally
		{
			OpenlletOptions.USE_TRACING = oldTracing;
		}
	}
}
