// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.cache;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.KnowledgeBase;
import openllet.core.boxes.abox.ABox;
import openllet.core.boxes.abox.Edge;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;
import openllet.core.boxes.rbox.Role;
import openllet.core.expressivity.Expressivity;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.fsm.Transition;
import openllet.core.utils.fsm.TransitionGraph;
import openllet.core.utils.iterator.IteratorUtils;

/**
 * A _cache safety implementation that checks the cached result and the context in which we try to reuse it to decide if it is safe to reuse the result.
 *
 * @author Evren Sirin
 */
public class CacheSafetyDynamic implements CacheSafety
{
	private final Expressivity _expressivity;

	CacheSafetyDynamic(final Expressivity e)
	{
		_expressivity = new Expressivity(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean canSupport(final Expressivity expressivity)
	{
		return !expressivity.hasNominal() && _expressivity.getAnonInverses().equals(expressivity.getAnonInverses());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSafe(final ATermAppl c, final Individual ind)
	{
		final Edge parentEdge = getParentEdge(ind);
		final Role r = parentEdge.getRole();
		final Individual parent = parentEdge.getFrom();

		final ABox abox = parent.getABox();

		if (!isParentSafe(abox.getKB(), r, parent))
			return false;

		final Iterator<CachedNode> nodes = getCachedNodes(abox, c);

		if (!nodes.hasNext())
			return false;

		if (interactsWithInverses(abox.getKB(), r))
			while (nodes.hasNext())
			{
				final CachedNode node = nodes.next();

				if (node.isBottom())
					return true;
				else
					if (node.isTop() || !node.isComplete())
						return false;

				if (!isSafe(abox.getKB(), parent, r.getInverse(), node))
					return false;
			}

		return true;
	}

	protected Edge getParentEdge(final Individual ind)
	{
		Edge result = null;
		Role role = null;
		final Individual parent = ind.getParent();
		for (final Edge e : ind.getInEdges())
			if (e.getFrom().equals(parent))
				if (role == null)
				{
					role = e.getRole();
					result = e;
				}
				else
					if (e.getRole().isSubRoleOf(role))
					{
						role = e.getRole();
						result = e;
					}

		assert result != null;

		return result;
	}

	protected Iterator<CachedNode> getCachedNodes(final ABox abox, final ATermAppl c)
	{
		CachedNode node = abox.getCached(c);
		if (node != null)
			return IteratorUtils.singletonIterator(node);

		if (ATermUtils.isAnd(c))
		{
			ATermList list = (ATermList) c.getArgument(0);
			final CachedNode[] nodes = new CachedNode[list.getLength()];
			for (int i = 0; !list.isEmpty(); list = list.getNext())
			{
				final ATermAppl d = (ATermAppl) list.getFirst();
				node = abox.getCached(d);
				if (node == null)
					return IteratorUtils.emptyIterator();
				else
					if (node.isBottom())
						return IteratorUtils.singletonIterator(node);

				nodes[i++] = node;
			}
			return IteratorUtils.iterator(nodes);
		}

		return IteratorUtils.emptyIterator();
	}

	private static boolean isParentSafe(final KnowledgeBase kb, final Role role, final Individual parent)
	{
		return isParentFunctionalSafe(role, parent) && isParentMaxSafe(kb, role, parent);
	}

	private static boolean isParentFunctionalSafe(final Role role, final Individual parent)
	{
		return !role.isFunctional() || parent.getRNeighbors(role).size() <= 1;
	}

	private static boolean isParentMaxSafe(final KnowledgeBase kb, final Role role, final Individual parent)
	{
		for (final ATermAppl negatedMax : parent.getTypes(Node.MAX))
		{
			final ATermAppl max = (ATermAppl) negatedMax.getArgument(0);
			if (!isParentMaxSafe(kb, role, max))
				return false;
		}

		return true;
	}

	private static boolean isParentMaxSafe(final KnowledgeBase kb, final Role role, final ATermAppl max)
	{
		final Role maxR = kb.getRole(max.getArgument(0));

		return !role.isSubRoleOf(maxR);
	}

	private static boolean isSafe(final KnowledgeBase kb, final Individual parent, final Role role, final CachedNode node)
	{
		if (!isFunctionalSafe(role, node))
			return false;

		for (final ATermAppl c : node.getDepends().keySet())
			if (ATermUtils.isAllValues(c))
			{
				if (!isAllValuesSafe(kb, parent, role, c))
					return false;
			}
			else
				if (ATermUtils.isNot(c))
				{
					final ATermAppl arg = (ATermAppl) c.getArgument(0);
					if (ATermUtils.isMin(arg))
						if (!isMaxSafe(kb, role, arg))
							return false;
				}

		return true;
	}

	private static boolean isAllValuesSafe(final KnowledgeBase kb, final Individual parent, final Role role, final ATermAppl term)
	{
		final Role s = kb.getRole(term.getArgument(0));
		if (!s.hasComplexSubRole())
		{
			final ATermAppl c = (ATermAppl) term.getArgument(1);

			if (role.isSubRoleOf(s) && !parent.hasType(c))
				return false;
		}
		else
		{
			final TransitionGraph<Role> tg = s.getFSM();

			for (final Transition<Role> t : tg.getInitialState().getTransitions())
				if (role.isSubRoleOf(t.getName()))
					return false;
		}

		return true;
	}

	private static boolean isFunctionalSafe(final Role role, final CachedNode node)
	{
		return !role.isFunctional() || getRNeighbors(node, role).isEmpty();
	}

	private static boolean isMaxSafe(final KnowledgeBase kb, final Role role, final ATermAppl term)
	{
		final Role maxR = kb.getRole(term.getArgument(0));

		return !role.isSubRoleOf(maxR);
	}

	private static Set<ATermAppl> getRNeighbors(final CachedNode node, final Role role)
	{
		final Set<ATermAppl> neighbors = new HashSet<>();

		for (final Edge edge : node.getOutEdges())
		{
			final Role r = edge.getRole();
			if (r.isSubRoleOf(role))
				neighbors.add(edge.getToName());
		}

		if (role.isObjectRole())
		{
			final Role invRole = role.getInverse();
			for (final Edge edge : node.getInEdges())
			{
				final Role r = edge.getRole();
				if (r.isSubRoleOf(invRole))
					neighbors.add(edge.getFromName());
			}
		}

		return neighbors;
	}

	protected boolean interactsWithInverses(final KnowledgeBase kb, final Role role)
	{
		if (interactsWithInversesSimple(role))
			return true;

		return _expressivity.hasComplexSubRoles() && interactsWithInversesComplex(kb, role);
	}

	protected boolean interactsWithInversesSimple(final Role role)
	{
		for (final Role superRole : role.getSuperRoles())
			if (hasAnonInverse(superRole))
				return true;

		return false;
	}

	protected boolean interactsWithInversesComplex(final KnowledgeBase kb, final Role role)
	{
		for (final ATermAppl p : _expressivity.getAnonInverses())
		{
			final Role anonRole = kb.getRole(p);
			if (anonRole.hasComplexSubRole() && anonRole.getFSM().getAlpahabet().contains(role))
				return true;

		}

		return false;
	}

	protected boolean hasAnonInverse(final Role role)
	{
		return !role.isBuiltin() && (role.isAnon() || _expressivity.getAnonInverses().contains(role.getName()));
	}
}
