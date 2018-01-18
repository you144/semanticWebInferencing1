// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.cache;

import java.util.Map;
import openllet.aterm.ATermAppl;
import openllet.core.DependencySet;
import openllet.core.boxes.abox.EdgeList;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.rbox.Role;

/**
 * <p>
 * Description: Represent the cached information for a concept or an _individual. For concepts this represents the root _node of the tableau completion graph
 * built to check the satisfiability of the concept. For individuals, this is the _individual itself ({@link Individual} implements this interface}. The cached
 * _node for concepts may be incomplete if the satisfiability status was cached when the satisfiability of another concept was being computed. Incomplete cached
 * _nodes will not have any information regarding types or edges.
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
public interface CachedNode
{
	/**
	 * Returns if this cached _node is complete.
	 *
	 * @return <code>true</code> if this cached _node is complete
	 */
	public boolean isComplete();

	/**
	 * Returns if this is the cached _node for BOTTOM concept.
	 *
	 * @return <code>true</code> if this is the cached _node for BOTTOM concept
	 */
	public boolean isTop();

	/**
	 * Returns if this is the cached _node for TOP concept.
	 *
	 * @return <code>true</code> if this is the cached _node for TOP concept
	 */
	public boolean isBottom();

	/**
	 * Returns the types and their dependencies for this _node.
	 *
	 * @return a map from concepts to dependency sets
	 */
	public Map<ATermAppl, DependencySet> getDepends();

	/**
	 * Returns the outgoing edges of this _node.
	 *
	 * @return Outgoing edges of this _node
	 */
	public EdgeList getOutEdges();

	/**
	 * Returns the incoming edges of this _node.
	 *
	 * @return Incoming edges of this node
	 */
	public EdgeList getInEdges();

	/**
	 * Checks if this node is connected to another _node with the given role (or one of its subproperties). The _node may have an incoming edge with the inverse
	 * of this role which would count as an r-neighbor.
	 *
	 * @param role
	 * @return Outgoing edges of this node
	 */
	public boolean hasRNeighbor(Role role);

	/**
	 * Returns the name of this _node. For cached concept _nodes this is the name of the concept.
	 *
	 * @return Name of this _node
	 */
	public ATermAppl getName();

	/**
	 * Returns if this _node represent a named _individual (not an anonymous _individual or a concept _node)
	 *
	 * @return If this _node represent a named _individual
	 */
	public boolean isNamedIndividual();

	/**
	 * Returns if this _node was cached without any dependency to a non-deterministic _branch. In the presence of nominals, when we are checking the
	 * satisfiability of a concept the root _node may be merged to a nominal _node and that merge may be due to a non-deterministic _branch. In such cases the
	 * types and edges that are cached do not necessarily show types and edges that will exist in every clash-free tableau completion.
	 *
	 * @return If this _node was cached without any dependency to a non-deterministic _branch
	 */
	public boolean isIndependent();
}
