package openllet.core.utils.iterator;

import java.util.Iterator;

/**
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 * @param <F> kind of function
 * @param <T> kind of elements
 */
public abstract class MapIterator<F, T> implements Iterator<T>
{
	private final Iterator<F> _iterator;

	public MapIterator(final Iterator<F> iterator)
	{
		this._iterator = iterator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasNext()
	{
		return _iterator.hasNext();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T next()
	{
		return map(_iterator.next());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove()
	{
		_iterator.remove();
	}

	public abstract T map(F obj);
}
