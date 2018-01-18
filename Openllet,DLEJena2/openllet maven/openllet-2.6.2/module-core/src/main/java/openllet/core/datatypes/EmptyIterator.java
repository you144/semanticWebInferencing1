package openllet.core.datatypes;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * Title: Empty Iterator
 * </p>
 * <p>
 * Description: Re-usable empty iterator implementation. Cannot be static so that parameterization is handled correctly.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Mike Smith
 * @param <E> kind of non elements
 */
public class EmptyIterator<E> implements Iterator<E>
{

	@Override
	public boolean hasNext()
	{
		return false;
	}

	@Override
	public E next()
	{
		throw new NoSuchElementException();
	}

	@Override
	public void remove()
	{
		throw new IllegalStateException();
	}

}
