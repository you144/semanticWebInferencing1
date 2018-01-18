// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.cache;

import openllet.aterm.ATermAppl;
import openllet.core.DependencySet;
import openllet.core.boxes.abox.Edge;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;
import openllet.core.boxes.rbox.Role;

/**
 * <p>
 * Description: Represents an edge cached for a {@link CachedNode}. A cached edge stores the information about only one _node (the _neighbor of the cached _node
 * where this edge is stored) and for that _node only the name is stored. This keeps the memory footprint of cached _nodes to a minimum without causing any slow
 * downs (since cached nodes are used only in limited ways).
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
public abstract class CachedEdge implements Edge
{
	protected ATermAppl _neighbor;
	private final Role _role;

	private DependencySet _depends;

	public CachedEdge(final Role role, final ATermAppl neighbor, final DependencySet ds)
	{
		_role = role;
		_neighbor = neighbor;
		_depends = ds.cache();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DependencySet getDepends()
	{
		return _depends;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Individual getFrom()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ATermAppl getFromName()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Node getNeighbor(final Node node)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Role getRole()
	{
		return _role;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Node getTo()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ATermAppl getToName()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDepends(final DependencySet ds)
	{
		_depends = ds;
	}

	@Override
	public String toString()
	{
		return "[" + _role + ", " + _neighbor + "] - " + _depends;
	}
}
