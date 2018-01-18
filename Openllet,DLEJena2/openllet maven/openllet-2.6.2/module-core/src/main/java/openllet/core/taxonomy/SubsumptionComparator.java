// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.taxonomy;

import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBase;
import openllet.core.utils.PartialOrderComparator;
import openllet.core.utils.PartialOrderRelation;

/**
 * <p>
 * Title: SubsumptionComparator
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Markus Stocker
 */
public class SubsumptionComparator implements PartialOrderComparator<ATermAppl>
{

	protected KnowledgeBase _kb;

	public SubsumptionComparator(final KnowledgeBase kb)
	{
		_kb = kb;
	}

	protected boolean isSubsumedBy(final ATermAppl a, final ATermAppl b)
	{
		return _kb.isSubClassOf(a, b);
	}

	@Override
	public PartialOrderRelation compare(final ATermAppl a, final ATermAppl b)
	{
		if (isSubsumedBy(a, b))
		{
			if (isSubsumedBy(b, a))
				return PartialOrderRelation.EQUAL;
			else
				return PartialOrderRelation.LESS;
		}
		else
			if (isSubsumedBy(b, a))
				return PartialOrderRelation.GREATER;
			else
				return PartialOrderRelation.INCOMPARABLE;
	}

	public void setKB(final KnowledgeBase kb)
	{
		_kb = kb;
	}

}
