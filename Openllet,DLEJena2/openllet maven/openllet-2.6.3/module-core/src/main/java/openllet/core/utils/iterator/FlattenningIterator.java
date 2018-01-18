// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.utils.iterator;

import java.util.Iterator;

/**
 * @author Evren Sirin
 * @param <T> kind of element
 */
public class FlattenningIterator<T> extends NestedIterator<Iterable<T>, T>
{
	public FlattenningIterator(final Iterator<? extends Iterable<T>> outerIterator)
	{
		super(outerIterator);
	}

	public FlattenningIterator(final Iterable<? extends Iterable<T>> outerIterable)
	{
		super(outerIterable);
	}

	@Override
	public Iterator<T> getInnerIterator(final Iterable<T> outer)
	{
		return outer.iterator();
	}
}
