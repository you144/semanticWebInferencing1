// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.utils;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 * @param <T> kind of elements
 */
@SuppressWarnings("unused")
public class IdentityHashSet<T> extends AbstractSet<T> implements Set<T>, Cloneable
{
	private static final Object VALUE = new Object();

	private transient IdentityHashMap<T, Object> _map;

	public IdentityHashSet()
	{
		_map = new IdentityHashMap<>();
	}

	public IdentityHashSet(final Collection<? extends T> c)
	{
		_map = new IdentityHashMap<>(Math.max((int) (c.size() / .75f) + 1, 16));
		addAll(c);
	}

	public IdentityHashSet(final int size)
	{
		_map = new IdentityHashMap<>(size);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(final T element)
	{
		return _map.put(element, VALUE) == null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear()
	{
		_map.clear();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object clone()
	{
		try
		{
			final IdentityHashSet<T> newSet = (IdentityHashSet<T>) super.clone();
			newSet._map = (IdentityHashMap<T, Object>) _map.clone();
			return newSet;
		}
		catch (final CloneNotSupportedException e)
		{
			throw new InternalError();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(final Object o)
	{
		return _map.containsKey(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty()
	{
		return _map.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<T> iterator()
	{
		return _map.keySet().iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(final Object element)
	{
		return _map.remove(element) != VALUE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size()
	{
		return _map.size();
	}

}
