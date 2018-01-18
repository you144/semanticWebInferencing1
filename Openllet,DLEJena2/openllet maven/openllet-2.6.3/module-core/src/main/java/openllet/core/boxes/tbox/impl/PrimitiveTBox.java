// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.boxes.tbox.impl;

import static openllet.core.utils.TermFactory.not;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import openllet.aterm.ATermAppl;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.CollectionUtils;
import openllet.core.utils.iterator.IteratorUtils;
import openllet.shared.tools.Log;

/**
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class PrimitiveTBox
{
	public static final Logger _logger = Log.getLogger(PrimitiveTBox.class);

	private final Map<ATermAppl, Unfolding> _definitions = CollectionUtils.makeIdentityMap();
	private final Map<ATermAppl, Set<ATermAppl>> _dependencies = CollectionUtils.makeIdentityMap();

	public boolean contains(final ATermAppl concept)
	{
		return _definitions.containsKey(concept);
	}

	public Unfolding getDefinition(final ATermAppl concept)
	{
		return _definitions.get(concept);
	}

	public boolean add(final ATermAppl concept, final ATermAppl definition, final Set<ATermAppl> explanation)
	{
		if (!ATermUtils.isPrimitive(concept) || contains(concept))
			return false;

		final Set<ATermAppl> deps = ATermUtils.findPrimitives(definition);
		final Set<ATermAppl> seen = new HashSet<>();

		for (final ATermAppl current : deps)
		{
			final boolean result = findTarget(current, concept, seen);
			if (result)
				return false;
		}

		addDefinition(concept, definition, explanation);
		addDefinition(not(concept), not(definition), explanation);
		_dependencies.put(concept, deps);

		return true;
	}

	protected void addDefinition(final ATermAppl concept, final ATermAppl definition, final Set<ATermAppl> explanation)
	{
		_logger.fine(() -> "Def: " + ATermUtils.toString(concept) + " = " + ATermUtils.toString(definition));
		_definitions.put(concept, Unfolding.create(ATermUtils.normalize(definition), explanation));
	}

	protected boolean findTarget(final ATermAppl term, final ATermAppl target, final Set<ATermAppl> seen)
	{
		final List<ATermAppl> queue = new ArrayList<>();
		queue.add(term);

		while (!queue.isEmpty())
		{
			final ATermAppl current = queue.remove(queue.size() - 1);

			if (!seen.add(current))
				continue;

			if (current.equals(target))
				return true;

			final Set<ATermAppl> deps = _dependencies.get(current);
			if (deps != null)
			{
				// Shortcut
				if (deps.contains(target))
					return true;

				queue.addAll(deps);
			}
		}

		return false;
	}

	//	public static boolean remove(@SuppressWarnings("unused") final ATermAppl axiom)
	//	{
	//		return false;
	//	}

	public Iterator<Unfolding> unfold(final ATermAppl concept)
	{
		final Unfolding unfolding = _definitions.get(concept);

		return unfolding == null ? IteratorUtils.<Unfolding> emptyIterator() : IteratorUtils.singletonIterator(unfolding);
	}

	public void print(final Appendable out) throws IOException
	{
		for (final Entry<ATermAppl, Unfolding> e : _definitions.entrySet())
		{
			out.append(ATermUtils.toString(e.getKey()));
			out.append(" = ");
			out.append(e.getValue().toString());
			out.append("\n");
		}
	}
}
