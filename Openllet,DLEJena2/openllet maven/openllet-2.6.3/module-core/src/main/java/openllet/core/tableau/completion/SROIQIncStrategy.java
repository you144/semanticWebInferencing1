// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
//
// ---
// Portions Copyright (c) 2006 Christian Halaschek-Wiener
// Halaschek-Wiener parts of this source code are available under the terms of the MIT License.
//
// The MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package openllet.core.tableau.completion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.DependencySet;
import openllet.core.OpenlletOptions;
import openllet.core.boxes.abox.ABoxImpl;
import openllet.core.boxes.abox.DefaultEdge;
import openllet.core.boxes.abox.Edge;
import openllet.core.boxes.abox.EdgeList;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;
import openllet.core.boxes.rbox.Role;
import openllet.core.expressivity.Expressivity;
import openllet.core.tableau.blocking.BlockingFactory;
import openllet.core.tableau.branch.Branch;
import openllet.core.tableau.completion.queue.NodeSelector;
import openllet.core.tableau.completion.queue.QueueElement;
import openllet.core.tracker.IncrementalChangeTracker;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.Timer;

/**
 * The completion _strategy for incremental consistency checking.
 *
 * @author Christian Halaschek-Wiener
 */
public class SROIQIncStrategy extends SROIQStrategy
{

	/**
	 * Constructor
	 *
	 * @param abox
	 */
	public SROIQIncStrategy(final ABoxImpl abox)
	{
		super(abox);
	}

	/**
	 * @return individuals to which we need to apply the initialization rules
	 */
	@Override
	public Iterator<Individual> getInitializeIterator()
	{
		return _abox.getIncrementalChangeTracker().updatedIndividuals();
	}

	/**
	 * @return individuals that were newly added
	 */
	public Iterator<Individual> getNewIterator()
	{
		return _abox.getIncrementalChangeTracker().newIndividuals();
	}

	/**
	 * @return edges that were newly added
	 */
	public Iterator<Edge> getNewEdgeIterator()
	{
		return _abox.getIncrementalChangeTracker().newEdges();
	}

	/**
	 * @return the unpruned indivdiuals
	 */
	public Iterator<Node> getUnPrunedIterator()
	{
		return _abox.getIncrementalChangeTracker().unprunedNodes();
	}

	/**
	 * @return edges that were newly added
	 */
	public Iterator<Edge> getRemovedEdgeIterator()
	{
		return _abox.getIncrementalChangeTracker().deletedEdges();
	}

	/**
	 * @return types that were deleted
	 */
	public Iterator<Map.Entry<Node, Set<ATermAppl>>> getRemovedTypeIterator()
	{
		return _abox.getIncrementalChangeTracker().deletedTypes();
	}

	/**
	 * There are additional rule that must be fired in the event of incremental additions and deletions in _order to guarantee completeness. These are done
	 * here.
	 */
	@Override
	public void initialize(final Expressivity expr)
	{

		final Optional<Timer> timer = _abox.getKB().getTimers().startTimer("initialize");

		_logger.fine("Initialize Started");

		_mergeList.clear();

		_blocking = BlockingFactory.createBlocking(expr);

		configureTableauRules(expr);

		for (final Branch branch : _abox.getBranches())
			branch.setStrategy(this);

		// if this is an incremental addition we may need to merge _nodes and
		// handle newly added individuals

		// merge _nodes - note _branch must be temporarily set to 0 to
		// ensure that assertion
		// will not be restored during backtracking
		// int _branch = _abox.getBranch();
		_abox.setBranchIndex(0);

		_mergeList.addAll(_abox.getToBeMerged());
		if (!_mergeList.isEmpty())
			mergeAll();

		//Apply necessary initialization to any new _individual added
		//Currently, this is a replication of the code
		for (final Iterator<Individual> newIt = getNewIterator(); newIt.hasNext();)
		{
			final Individual n = newIt.next();

			if (n.isMerged())
				continue;

			applyUniversalRestrictions(n);

			_unfoldingRule.apply(n);

			_selfRule.apply(n);
		}

		for (final Iterator<Individual> it = getInitializeIterator(); it.hasNext();) //handle _nodes affected by the update
		{
			Individual n = it.next();

			_nominalRule.apply(n);

			if (n.isMerged())
				n = n.getSame();

			_allValuesRule.apply(n);
		}

		//process new edges
		for (final Iterator<Edge> it = getNewEdgeIterator(); it.hasNext();)
		{
			final Edge edge = it.next();

			final Individual subj = edge.getFrom();
			final Node obj = edge.getTo();
			if (subj.isMerged())
				subj.getSame();
			if (subj.isPruned())
				continue;

			if (obj.isMerged())
				obj.getSame();
			if (obj.isPruned())
				continue;

			final Role pred = edge.getRole();
			final DependencySet ds = edge.getDepends();

			applyDomainRange(subj, pred, obj, ds);
			if (subj.isPruned() || obj.isPruned())
				return;
			applyFunctionality(subj, pred, obj);
			if (subj.isPruned() || obj.isPruned())
				return;

			if (pred.isObjectRole())
			{
				final Individual o = (Individual) obj;
				checkReflexivitySymmetry(subj, pred, o, ds);
				checkReflexivitySymmetry(o, pred.getInverse(), subj, ds);
			}

			//if the KB has cardinality restrictions, then we need to apply the guessing rule
			if (_abox.getKB().getExpressivity().hasCardinality())
				//update the _queue so the max rule will be fired
				updateQueueAddEdge(subj, pred, obj);
		}

		//merge again if necessary
		if (!_mergeList.isEmpty())
			mergeAll();

		//set appropriate _branch
		_abox.setBranchIndex(_abox.getBranches().size() + 1);

		// we will also need to add stuff to the _queue in the event of a
		// deletion

		//Handle removed edges
		Iterator<Edge> i = getRemovedEdgeIterator();
		while (i.hasNext())
		{
			final Edge e = i.next();
			Individual subj = e.getFrom();
			Node obj = e.getTo();

			subj = subj.getSame();

			subj._applyNext[Node.SOME] = 0;
			subj._applyNext[Node.MIN] = 0;
			QueueElement qe = new QueueElement(subj);
			_abox.getCompletionQueue().add(qe, NodeSelector.EXISTENTIAL);
			_abox.getCompletionQueue().add(qe, NodeSelector.MIN_NUMBER);

			obj = obj.getSame();
			if (obj instanceof Individual)
			{
				final Individual objInd = (Individual) obj;
				objInd._applyNext[Node.SOME] = 0;
				objInd._applyNext[Node.MIN] = 0;
				qe = new QueueElement(objInd);
				_abox.getCompletionQueue().add(qe, NodeSelector.EXISTENTIAL);
				_abox.getCompletionQueue().add(qe, NodeSelector.MIN_NUMBER);
			}
		}

		//Handle removed types
		final Iterator<Map.Entry<Node, Set<ATermAppl>>> it = getRemovedTypeIterator();
		while (it.hasNext())
		{
			final Node node = it.next().getKey();

			if (node.isIndividual())
			{
				final Individual ind = (Individual) node;

				//readd the conjunctions
				readdConjunctions(ind);

				//it could be the case that the type can be added from unfolding, a forAll application on a self loop, or the _disjunction rule
				ind._applyNext[Node.ATOM] = 0;
				ind._applyNext[Node.ALL] = 0;
				ind._applyNext[Node.OR] = 0;

				final QueueElement qe = new QueueElement(ind);
				_abox.getCompletionQueue().add(qe, NodeSelector.ATOM);
				_abox.getCompletionQueue().add(qe, NodeSelector.DISJUNCTION);

				//fire the all rule as the is no explicit call to it
				_allValuesRule.apply(ind);

				//get out edges and check domains, some values and min values
				for (int j = 0; j < ind.getOutEdges().size(); j++)
				{
					final Edge e = ind.getOutEdges().get(j);

					if (e.getFrom().isPruned() || e.getTo().isPruned())
						continue;

					final Role pred = e.getRole();
					final Node obj = e.getTo();
					final DependencySet ds = e.getDepends();

					for (final ATermAppl domain : pred.getDomains())
						if (requiredAddType(ind, domain))
							if (!OpenlletOptions.USE_TRACING)
								addType(ind, domain, ds.union(DependencySet.EMPTY, _abox.doExplanation()));
							else
								addType(ind, domain, ds.union(pred.getExplainDomain(domain), _abox.doExplanation()));

					//it could be the case that this label prevented the firing of the all values, some, or min rules of the _neighbor
					if (obj instanceof Individual)
					{
						final Individual objInd = (Individual) obj;
						objInd._applyNext[Node.ALL] = 0;
						objInd._applyNext[Node.SOME] = 0;
						objInd._applyNext[Node.MIN] = 0;
						final QueueElement qeObj = new QueueElement(objInd);
						_abox.getCompletionQueue().add(qeObj, NodeSelector.EXISTENTIAL);
						_abox.getCompletionQueue().add(qeObj, NodeSelector.MIN_NUMBER);

						//apply the all values rule
						_allValuesRule.apply(ind);
					}
				}
			}

			//get out edges
			for (int j = 0; j < node.getInEdges().size(); j++)
			{
				final Edge e = node.getInEdges().get(j);

				if (e.getFrom().isPruned() || e.getTo().isPruned())
					continue;

				final Individual subj = e.getFrom();
				final Role pred = e.getRole();
				final DependencySet ds = e.getDepends();

				for (final ATermAppl range : pred.getRanges())
					if (requiredAddType(node, range))
						if (!OpenlletOptions.USE_TRACING)
							addType(node, range, ds.union(DependencySet.EMPTY, _abox.doExplanation()));
						else
							addType(node, range, ds.union(pred.getExplainRange(range), _abox.doExplanation()));

				//it could be the case that this label prevented the firing of the all values, some, or min rules of the _neighbor
				subj._applyNext[Node.ALL] = 0;
				subj._applyNext[Node.SOME] = 0;
				subj._applyNext[Node.MIN] = 0;
				final QueueElement qe = new QueueElement(subj);
				_abox.getCompletionQueue().add(qe, NodeSelector.EXISTENTIAL);
				_abox.getCompletionQueue().add(qe, NodeSelector.MIN_NUMBER);

				_allValuesRule.apply(subj);
			}
		}

		//due to unmerging _nodes, edges can actually be added
		i = getNewEdgeIterator();
		while (i.hasNext())
			applyPropertyRestrictions(i.next());

		//due to unmerging any _node that was pruned could need rules applied to it. This is because these rules
		//would have been originally applied to the _node that the pruned _node was merged into.
		for (final Iterator<Node> nodeIt = getUnPrunedIterator(); nodeIt.hasNext();)
		{
			final Node n = nodeIt.next();

			if (n.isIndividual())
			{
				final Individual ind = (Individual) n;

				//reset type pointers
				for (int j = 0; j < Node.TYPES; j++)
					ind._applyNext[j] = 0;

				//add to all queues
				_abox.getCompletionQueue().add(new QueueElement(ind));

				_allValuesRule.apply(ind);

				//get out edges
				for (int j = 0; j < ind.getOutEdges().size(); j++)
				{
					final Edge e = ind.getOutEdges().get(j);

					if (!e.getFrom().isPruned() && !e.getTo().isPruned())
						applyPropertyRestrictions(e);

					final Node obj = e.getTo();
					if (obj instanceof Individual)
					{
						final Individual objInd = (Individual) obj;
						objInd._applyNext[Node.ALL] = 0;
						_allValuesRule.apply(objInd);
					}
				}

				//get out edges
				for (int j = 0; j < ind.getInEdges().size(); j++)
				{
					final Edge e = ind.getInEdges().get(j);

					if (!e.getFrom().isPruned() && !e.getTo().isPruned())
						applyPropertyRestrictions(e);

					final Individual subj = e.getFrom();
					subj._applyNext[Node.ALL] = 0;
					_allValuesRule.apply(subj);
				}
			}
		}

		_abox.setChanged(true);
		_abox.setComplete(false);
		_abox.setInitialized(true);

		timer.ifPresent(t -> t.stop());
		_logger.fine("Initialize Ended");
	}

	/**
	 * Readd conjunction labels
	 *
	 * @param ind
	 */
	private void readdConjunctions(final Individual ind)
	{
		ind.types()//
				.filter(conj -> ATermUtils.isAnd(conj) && ind.hasType(conj))//
				.forEach(conj -> addType(ind, conj, ind.getDepends(conj)));
	}

	/**
	 * @param node
	 * @param type
	 * @return true if a type should be readded to a node
	 */
	private static boolean requiredAddType(final Node node, final ATermAppl type)
	{
		return !(type == null || node.hasType(type) && !ATermUtils.isAnd(type));
	}

	@Override
	protected void restoreAllValues()
	{
		final IncrementalChangeTracker tracker = _abox.getIncrementalChangeTracker();

		for (final Iterator<Map.Entry<Node, Set<ATermAppl>>> it = tracker.deletedTypes(); it.hasNext();)
		{

			final Map.Entry<Node, Set<ATermAppl>> entry = it.next();
			final Node node = entry.getKey();
			final Set<ATermAppl> types = entry.getValue();

			// find the edges which the all values rule needs to be
			// re-applied to
			final EdgeList av = findAllValues(node, types);

			//apply the all values rules
			for (int i = 0; i < av.size(); i++)
			{
				final Edge e = av.get(i);
				_allValuesRule.applyAllValues(e.getFrom(), e.getRole(), e.getTo(), e.getDepends());
			}
		}

		for (final Iterator<Node> i = tracker.unprunedNodes(); i.hasNext();)
		{
			final Node node = i.next();

			if (node instanceof Individual)
			{
				final Individual ind = (Individual) node;
				//reset type pointers
				for (int j = 0; j < Node.TYPES; j++)
					ind._applyNext[j] = 0;

				//add to all queues
				_abox.getCompletionQueue().add(new QueueElement(ind));

				_allValuesRule.apply(ind);

				//get out edges
				for (int j = 0; j < ind.getOutEdges().size(); j++)
				{
					final Edge e = ind.getOutEdges().get(j);

					final Node obj = e.getTo();
					if (obj instanceof Individual)
					{
						final Individual objInd = (Individual) obj;
						objInd._applyNext[Node.ALL] = 0;
						_allValuesRule.apply(objInd);
					}
				}
			}

			//get out edges
			for (int j = 0; j < node.getInEdges().size(); j++)
			{
				final Edge e = node.getInEdges().get(j);
				final Individual subj = e.getFrom();
				subj._applyNext[Node.ALL] = 0;
				_allValuesRule.apply(subj);
			}
		}
	}

	/**
	 * Find applicable all values for a removed type during a restore
	 *
	 * @param _node
	 * @param removedTypes
	 * @return
	 */
	private EdgeList findAllValues(final Node node, final Set<ATermAppl> removedTypes)
	{
		final EdgeList edges = new EdgeList();

		//handle in edges
		for (int i = 0; i < node.getInEdges().size(); i++)
		{
			final Edge e = node.getInEdges().get(i);

			edges.addAll(findAllValues(node, e.getFrom(), removedTypes, e));
		}

		if (node instanceof Individual)
		{
			final Individual ind = (Individual) node;

			//handle in edges
			for (int i = 0; i < ind.getOutEdges().size(); i++)
			{
				final Edge e = ind.getOutEdges().get(i);
				final Node to = e.getTo();

				final Role inv = e.getRole().getInverse();

				if (inv != null && to instanceof Individual)
					edges.addAll(findAllValues(ind, (Individual) to, removedTypes, new DefaultEdge(inv, (Individual) to, ind, e.getDepends())));
			}
		}

		return edges;
	}

	/**
	 * Method to find the edges which an all values could be applied to
	 *
	 * @param _node
	 * @param _neighbor
	 * @param removedTypes
	 * @param edge
	 * @return
	 */
	private EdgeList findAllValues(final Node node, final Individual neighbor, final Set<ATermAppl> removedTypes, final Edge edge)
	{
		final EdgeList edges = new EdgeList();

		boolean applicable = false;
		final List<ATermAppl> avTypes = neighbor.getTypes(Node.ALL);
		final List<ATermAppl> applicableRoles = new ArrayList<>();

		//inspect all values for a recently deleted type
		for (int i = 0; i < avTypes.size(); i++)
		{
			final ATermAppl avType = avTypes.get(i);
			final ATermAppl role = (ATermAppl) avType.getArgument(0);
			final ATermAppl type = (ATermAppl) avType.getArgument(1);

			//if we cannot use this edge then continue
			if (edge != null && !edge.getRole().isSubRoleOf(_abox.getRole(role)))
				continue;

			if (containsType(type, removedTypes))
			{
				applicable = true;
				applicableRoles.add(type);
			}
		}

		//only proceed if necessary
		if (!applicable)
			return edges;

		//two cases depending on input
		if (edge == null)
			//get all edges to this _node
			for (int i = 0; i < applicableRoles.size(); i++)
			{
				final ATerm p = applicableRoles.get(i);
				final Role role = _abox.getRole(p);

				edges.addAll(neighbor.getRNeighborEdges(role, node));
			}
		else
			edges.add(edge);

		return edges;
	}

	/**
	 * Check if a _node contains a particular type that has been removed
	 *
	 * @param type
	 * @param removedTypes
	 * @return
	 */
	private boolean containsType(final ATermAppl type, final Set<ATermAppl> removedTypes)
	{
		boolean contains = false;

		if (ATermUtils.isAnd(type))
			for (ATermList cs = (ATermList) type.getArgument(0); !cs.isEmpty(); cs = cs.getNext())
			{
				final ATermAppl conj = (ATermAppl) cs.getFirst();

				if (containsType(conj, removedTypes))
				{
					contains = true;
					break;
				}
			}
		else
			if (removedTypes.contains(type))
				contains = true;

		return contains;
	}

}
