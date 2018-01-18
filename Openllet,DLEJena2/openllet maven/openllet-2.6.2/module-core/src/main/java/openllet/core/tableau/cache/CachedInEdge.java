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
 * Description: Represents a cached incoming edge.
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
public class CachedInEdge extends CachedEdge
{
	public CachedInEdge(final Edge edge)
	{
		super(edge.getRole(), edge.getFromName(), edge.getDepends());
	}

	public CachedInEdge(final Role role, final ATermAppl from, final DependencySet ds)
	{
		super(role, from, ds);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ATermAppl getFromName()
	{
		return _neighbor;
	}
}
