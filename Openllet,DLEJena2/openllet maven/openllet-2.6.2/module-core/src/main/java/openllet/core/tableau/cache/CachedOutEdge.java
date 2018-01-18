// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.cache;

import openllet.aterm.ATermAppl;
import openllet.core.DependencySet;
import openllet.core.boxes.abox.Edge;
import openllet.core.boxes.rbox.Role;

/**
 * <p>
 * Description: Represents a cached outgoing edge.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class CachedOutEdge extends CachedEdge
{
	public CachedOutEdge(final Edge edge)
	{
		super(edge.getRole(), edge.getToName(), edge.getDepends());
	}

	public CachedOutEdge(final Role role, final ATermAppl to, final DependencySet ds)
	{
		super(role, to, ds);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ATermAppl getToName()
	{
		return _neighbor;
	}

}
