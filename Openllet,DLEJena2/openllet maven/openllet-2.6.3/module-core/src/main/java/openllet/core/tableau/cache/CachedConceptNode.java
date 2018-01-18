// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import openllet.aterm.ATermAppl;
import openllet.core.DependencySet;
import openllet.core.OpenlletOptions;
import openllet.core.boxes.abox.Edge;
import openllet.core.boxes.abox.EdgeList;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;
import openllet.core.boxes.rbox.Role;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.CollectionUtils;

/**
 * <p>
 * Description: A _node cached as the result of satisfiability checking for a concept.
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
public class CachedConceptNode implements CachedNode
{
	private final ATermAppl _name;
	private final EdgeList _inEdges;
	private final EdgeList _outEdges;
	private final Map<ATermAppl, DependencySet> _types;
	private final boolean _isIndependent;

	/**
	 * @param name
	 * @param nodeParam
	 */
	public CachedConceptNode(final ATermAppl name, final Individual nodeParam)
	{
		_name = name;
		Individual node = nodeParam;

		// if the _node is merged, get the representative _node and check
		// also if the merge depends on a _branch
		_isIndependent = node.getMergeDependency(true).isIndependent();
		node = node.getSame();

		_outEdges = copyEdgeList(node, true);
		_inEdges = copyEdgeList(node, false);

		// collect all transitive property values
		if (node.getABox().getKB().getExpressivity().hasNominal())
			collectComplexPropertyValues(node);

		_types = CollectionUtils.makeIdentityMap(node.getDepends());
		for (final Map.Entry<ATermAppl, DependencySet> e : _types.entrySet())
			e.setValue(e.getValue().cache());
	}

	private void collectComplexPropertyValues(final Individual subj)
	{
		final Set<Role> collected = new HashSet<>();
		for (final Edge edge : subj.getOutEdges())
		{
			final Role role = edge.getRole();

			// only collect non-simple, i.e. complex, roles
			// TODO we might not need to collect all non-simple roles
			// collecting only the base ones, i.e. minimal w.r.t. role
			// ordering, would be enough
			if (role.isSimple() || !collected.add(role))
				continue;

			collected.add(role);

			collectComplexPropertyValues(subj, role);
		}

		for (final Edge edge : subj.getInEdges())
		{
			final Role role = edge.getRole().getInverse();

			if (role.isSimple() || !collected.add(role))
				continue;

			collectComplexPropertyValues(subj, role);
		}
	}

	private void collectComplexPropertyValues(final Individual subj, final Role role)
	{
		final Set<ATermAppl> knowns = new HashSet<>();
		final Set<ATermAppl> unknowns = new HashSet<>();

		subj.getABox().getObjectPropertyValues(subj.getName(), role, knowns, unknowns, false);

		for (final ATermAppl val : knowns)
			_outEdges.add(new CachedOutEdge(role, val, DependencySet.INDEPENDENT));
		for (final ATermAppl val : unknowns)
			_outEdges.add(new CachedOutEdge(role, val, DependencySet.DUMMY));
	}

	/**
	 * Create an immutable copy of the given edge list and trimmed to the size.
	 *
	 * @param edgeList
	 * @return
	 */
	private static EdgeList copyEdgeList(final Individual node, final boolean out)
	{
		final EdgeList edgeList = out ? node.getOutEdges() : node.getInEdges();
		final EdgeList cachedEdges = new EdgeList(edgeList.size());
		for (final Edge edge : edgeList)
		{
			final Edge cachedEdge = out ? new CachedOutEdge(edge) : new CachedInEdge(edge);
			cachedEdges.add(cachedEdge);

			if (OpenlletOptions.CHECK_NOMINAL_EDGES)
			{
				final Node neighbor = edge.getNeighbor(node);
				final Map<Node, DependencySet> mergedNodes = neighbor.getAllMerged();
				final DependencySet edgeDepends = edge.getDepends();
				for (final Entry<Node, DependencySet> entry : mergedNodes.entrySet())
				{
					final Node mergedNode = entry.getKey();
					if (mergedNode.isRootNominal() && !mergedNode.equals(neighbor))
					{
						final Role r = edge.getRole();
						final ATermAppl n = mergedNode.getName();
						final DependencySet ds = edgeDepends.union(entry.getValue(), false).cache();
						final Edge e = out ? new CachedOutEdge(r, n, ds) : new CachedInEdge(r, n, ds);
						cachedEdges.add(e);
					}
				}
			}
		}

		return cachedEdges;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isIndependent()
	{
		return _isIndependent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EdgeList getInEdges()
	{
		return _inEdges;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EdgeList getOutEdges()
	{
		return _outEdges;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<ATermAppl, DependencySet> getDepends()
	{
		return _types;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasRNeighbor(final Role role)
	{
		return _outEdges.hasEdge(role) || role.isObjectRole() && _inEdges.hasEdge(role.getInverse());

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isBottom()
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isComplete()
	{
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isNamedIndividual()
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTop()
	{
		return false;
	}

	@Override
	public ATermAppl getName()
	{
		return _name;
	}

	@Override
	public String toString()
	{
		return ATermUtils.toString(_name);
	}
}
