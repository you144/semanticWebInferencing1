// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.blocking;

import openllet.core.utils.SetUtils;

/**
 * @author Evren Sirin
 */
public class Block1Set implements BlockingCondition
{
	@Override
	public boolean isBlocked(final BlockingContext cxt)
	{
		return SetUtils.subset(cxt._blocked.getTypes(), cxt._blocker.getTypes());
	}
}
