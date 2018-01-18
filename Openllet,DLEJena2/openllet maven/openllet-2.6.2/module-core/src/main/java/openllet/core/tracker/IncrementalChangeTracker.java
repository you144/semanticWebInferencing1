// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tracker;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import openllet.aterm.ATermAppl;
import openllet.core.boxes.abox.ABoxImpl;
import openllet.core.boxes.abox.Edge;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;

/**
 * <p>
 * Title: Incremental change tracker
 * </p>
 * <p>
 * Description: Tracks the changes for incremental ABox reasoning services
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
public interface IncrementalChangeTracker
{

	/**
	 * Record that a new edge has been deleted from the ABox
	 *
	 * @param e the Edge
	 * @return boolean {@code true} if delete is not already noted for edge, {@code false} else
	 */
	public boolean addDeletedEdge(Edge e);

	/**
	 * Record that a type was deleted from an _individual
	 *
	 * @param n the Node
	 * @param type the type
	 * @return boolean {@code true} if delete is not already noted for _node, type pair {@code false} else
	 */
	public boolean addDeletedType(Node n, ATermAppl type);

	/**
	 * Record that a new edge has been added to the ABox
	 *
	 * @param e the Edge
	 * @return boolean {@code true} if addition is not already noted for edge, {@code false} else
	 */
	public boolean addNewEdge(Edge e);

	/**
	 * Record that a new _individual has been added to the ABox
	 *
	 * @param i the Individual
	 * @return boolean {@code true} if addition is not already noted for _individual, {@code false} else
	 */
	public boolean addNewIndividual(Individual i);

	/**
	 * Record that a _node has been "unpruned" because a merge was reverted during restore
	 *
	 * @param n the Node
	 * @return boolean {@code true} if unpruning is not already noted for _node, {@code false} else
	 */
	public boolean addUnprunedNode(Node n);

	/**
	 * Record that an _individual has been updated
	 *
	 * @param i the Individual
	 * @return boolean {@code true} if addition is not already noted for _individual, {@code false} else
	 */
	public boolean addUpdatedIndividual(Individual i);

	/**
	 * Clear all recorded changes
	 */
	public void clear();

	/**
	 * Copy change tracker for use with a new ABox (presumably as part of {@code ABox.copy()})
	 *
	 * @param target The ABox for the copy
	 * @return a copy, with individuals in the target ABox
	 */
	public IncrementalChangeTracker copy(ABoxImpl target);

	/**
	 * Iterate over all edges deleted (see {@link #addDeletedEdge(Edge)}) since the previous {@link #clear()}
	 *
	 * @return Iterator
	 */
	public Iterator<Edge> deletedEdges();

	/**
	 * Iterate over all _nodes with deleted types (and those types) (see {@link #addDeletedType(Node, ATermAppl)}) since the previous {@link #clear()}
	 *
	 * @return Iterator
	 */
	public Iterator<Map.Entry<Node, Set<ATermAppl>>> deletedTypes();

	/**
	 * Iterate over all edges added (see {@link #addNewEdge(Edge)}) since the previous {@link #clear()}
	 *
	 * @return Iterator
	 */
	public Iterator<Edge> newEdges();

	/**
	 * Iterate over all individuals added (see {@link #addNewIndividual(Individual)}) since the previous {@link #clear()}
	 *
	 * @return Iterator
	 */
	public Iterator<Individual> newIndividuals();

	/**
	 * Iterate over all _nodes unpruned (see addUnprunedIndividual) since the previous {@link #clear()}
	 *
	 * @return Iterator
	 */
	public Iterator<Node> unprunedNodes();

	/**
	 * Iterate over all individuals updated (see {@link #addUpdatedIndividual(Individual)}) since the previous {@link #clear()}
	 *
	 * @return Iterator
	 */
	public Iterator<Individual> updatedIndividuals();
}
