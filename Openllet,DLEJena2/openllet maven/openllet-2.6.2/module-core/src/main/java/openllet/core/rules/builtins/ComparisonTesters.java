// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules.builtins;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.aterm.ATermAppl;
import openllet.core.boxes.abox.Literal;
import openllet.core.datatypes.DatatypeReasoner;
import openllet.core.datatypes.Facet;
import openllet.core.datatypes.exceptions.DatatypeReasonerException;
import openllet.core.utils.ATermUtils;
import openllet.shared.tools.Log;

/**
 * <p>
 * Title: Comparison Testers
 * </p>
 * <p>
 * Description: Implementations for each of the SWRL comparison tests.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Ron Alford
 */

public class ComparisonTesters
{

	private static Logger _logger = Log.getLogger(ComparisonTesters.class);

	private static class EqualityTester extends BinaryTester
	{

		private final boolean _flip;

		private EqualityTester(final boolean flip)
		{
			_flip = flip;
		}

		@Override
		protected boolean test(final Literal a, final Literal b)
		{
			final Object aval = a.getValue();
			final Object bval = b.getValue();

			// Numbers are a special case, since they can be promoted from Integers and Decimals to Floats and Doubles.
			if (aval instanceof Number && bval instanceof Number)
			{
				final NumericPromotion promoter = new NumericPromotion();
				final Number anum = (Number) aval;
				final Number bnum = (Number) bval;

				promoter.promote(anum, bnum);
				final NumericComparisonVisitor visitor = new NumericComparisonVisitor();
				promoter.accept(visitor);

				if (visitor.getComparison() == 0)
					return true ^ _flip;
				return false ^ _flip;
			}

			if (a.getValue() != null && b.getValue() != null)
				return (aval.getClass().equals(bval.getClass()) && aval.equals(bval)) ^ _flip;
			return false;
		}
	}

	private static class OrderingTester extends BinaryTester
	{

		private final boolean _lt, _inclusive;

		private OrderingTester(final boolean flip, final boolean inclusive)
		{
			_lt = flip;
			_inclusive = inclusive;
		}

		private boolean comparesWell(final int comparison)
		{
			if (_lt && comparison < 0)
				return true;
			if (!_lt && comparison > 0)
				return true;
			return _inclusive && comparison == 0;
		}

		@Override
		public boolean test(final Literal l1, final Literal l2)
		{
			final Object l1val = l1.getValue();
			final Object l2val = l2.getValue();

			// String comparisons between ATerms
			if (l1val instanceof ATermAppl && l2val instanceof ATermAppl)
			{
				final ATermAppl l1term = (ATermAppl) l1val;
				final ATermAppl l2term = (ATermAppl) l2val;

				final String l1str = ATermUtils.getLiteralValue(l1term);
				final String l2str = ATermUtils.getLiteralValue(l2term);
				final String l1lang = ATermUtils.getLiteralLang(l1term);
				final String l2lang = ATermUtils.getLiteralLang(l2term);
				final String l1data = ATermUtils.getLiteralDatatype(l1term);
				final String l2data = ATermUtils.getLiteralDatatype(l2term);

				if (l1lang.equals(l2lang) && l1data.equals(l2data))
					return comparesWell(l1str.compareTo(l2str));
				return false;
			}

			// Numbers are a special case, since they can be promoted from
			// Integers and Decimals to Floats and Doubles.
			if (l1val instanceof Number && l2val instanceof Number)
			{
				final NumericPromotion promoter = new NumericPromotion();
				final Number l1num = (Number) l1val;
				final Number l2num = (Number) l2val;

				promoter.promote(l1num, l2num);
				final NumericComparisonVisitor visitor = new NumericComparisonVisitor();
				promoter.accept(visitor);

				return comparesWell(visitor.getComparison());
			}

			final DatatypeReasoner dtr = l1.getABox().getDatatypeReasoner();
			final ATermAppl term1 = l1.getTerm();
			final ATermAppl type1 = (ATermAppl) term1.getArgument(ATermUtils.LIT_URI_INDEX);
			final ATermAppl type2 = (ATermAppl) l2.getTerm().getArgument(ATermUtils.LIT_URI_INDEX);
			try
			{
				/*
				 * First check if the literals' datatypes are comparable. If so,
				 * compile the comparison into a datatype reasoner
				 * satisfiability check.
				 */
				if (dtr.isSatisfiable(Arrays.asList(type1, type2)))
				{
					final Facet f = _lt ? _inclusive ? Facet.XSD.MIN_INCLUSIVE : Facet.XSD.MIN_EXCLUSIVE : _inclusive ? Facet.XSD.MAX_INCLUSIVE : Facet.XSD.MAX_EXCLUSIVE;
					final ATermAppl canon1 = dtr.getCanonicalRepresentation(term1);
					final ATermAppl baseType = (ATermAppl) canon1.getArgument(ATermUtils.LIT_URI_INDEX);
					final ATermAppl dr = ATermUtils.makeRestrictedDatatype(baseType, new ATermAppl[] { ATermUtils.makeFacetRestriction(f.getName(), canon1) });
					return dtr.isSatisfiable(Collections.singleton(dr), l2val);
				}
				else
					return false;
			}
			catch (final DatatypeReasonerException e)
			{
				final String msg = format("Unexpected datatype reasoner exception comparaing two literals ('%s','%s'). Treating as incomparable.", term1, l2.getTerm());
				_logger.log(Level.WARNING, msg, e);
				return false;
			}
		}
	}

	public final static Tester equal = new EqualityTester(false), greaterThan = new OrderingTester(false, false), greaterThanOrEqual = new OrderingTester(false, true), lessThan = new OrderingTester(true, false), lessThanOrEqual = new OrderingTester(true, true), notEqual = new EqualityTester(true);

	/**
	 * @param expected
	 * @param result
	 * @return the second argument if the first is null, else return the first . Else, return the literal if its value equals the string. Otherwise return null.
	 */
	public static Literal expectedIfEquals(final Literal expected, final Literal result)
	{
		if (expected == null)
			return result;

		if (ComparisonTesters.equal.test(new Literal[] { expected, result }))
			return expected;
		return null;
	}
}
