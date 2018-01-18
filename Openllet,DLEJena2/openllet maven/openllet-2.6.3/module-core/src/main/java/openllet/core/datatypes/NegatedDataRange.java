package openllet.core.datatypes;

import java.util.Iterator;

/**
 * <p>
 * Title: Negated Data Range
 * </p>
 * <p>
 * Description: A negated _data range. By definition, this is infinite.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Mike Smith
 * @param <T> kind of element
 */
public class NegatedDataRange<T> implements DataRange<T>
{
	private final DataRange<? extends T> _datarange;

	public NegatedDataRange(final DataRange<? extends T> datarange)
	{
		_datarange = datarange;
	}

	@Override
	public boolean contains(final Object value)
	{
		return !_datarange.contains(value);
	}

	@Override
	public boolean containsAtLeast(final int n)
	{
		return true;
	}

	@Override
	public boolean isEmpty()
	{
		return false;
	}

	@Override
	public boolean isEnumerable()
	{
		return false;
	}

	@Override
	public boolean isFinite()
	{
		return false;
	}

	@Override
	public Iterator<T> valueIterator()
	{
		throw new UnsupportedOperationException();
	}

	public DataRange<? extends T> getDataRange()
	{
		return _datarange;
	}
}
