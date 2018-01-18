/*
 * Copyright (c) 2002-2007, CWI and INRIA
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, Berkeley nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package openllet.aterm.pure;

import java.util.ArrayList;
import java.util.List;

import openllet.aterm.AFun;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.aterm.ATermPlaceholder;
import openllet.aterm.Visitor;
import openllet.atom.SList;
import openllet.shared.hash.HashFunctions;
import openllet.shared.hash.SharedObject;

public class ATermListImpl extends ATermImpl implements ATermList
{
	public static final String _illegalListIndex = "illegal list index: ";

	private ATerm _first;

	private ATermList _next;

	private int _length;

	protected ATermListImpl(final PureFactory factory)
	{
		super(factory);
	}

	protected ATermListImpl(final PureFactory factory, final ATerm first, final ATermList next)
	{
		super(factory);

		_first = first;
		_next = next;

		if (first == null && next == null)
			_length = 0;
		else
			_length = 1 + next.getLength();

		setHashCode(hashFunction());
	}

	protected void init(final int hashCode, final ATerm first, final ATermList next)
	{
		super.init(hashCode);
		_first = first;
		_next = next;

		if (first == null && next == null)
			_length = 0;
		else
			_length = 1 + next.getLength();
	}

	@Override
	public int getType()
	{
		return ATerm.LIST;
	}

	/**
	 * depricated Use the new constructor instead.
	 *
	 * @param annos x
	 * @param _first x
	 * @param _next x
	 */
	protected void initHashCode(final ATerm first, final ATermList next)
	{
		_first = first;
		_next = next;
		setHashCode(hashFunction());

		if (first == null && next == null)
			_length = 0;
		else
			_length = 1 + next.getLength();
	}

	@Override
	public SharedObject duplicate()
	{
		return this;
	}

	@Override
	public boolean equivalent(final SharedObject obj)
	{
		if (obj instanceof ATermList)
		{
			final ATermList peer = (ATermList) obj;
			if (peer.getType() != getType())
				return false;

			return peer.getFirst() == _first && peer.getNext() == _next;
		}

		return false;
	}

	@Override
	public ATermList insert(final ATerm head)
	{
		return getPureFactory().makeList(head, this);
	}

	protected ATermList make(final ATerm head, final ATermList tail)
	{
		return getPureFactory().makeList(head, tail);
	}

	@Override
	public ATermList getEmpty()
	{
		return getPureFactory().makeList();
	}

	@Override
	protected boolean match(final ATerm pattern, final List<Object> list)
	{
		if (pattern.getType() == LIST)
		{
			final ATermList l = (ATermList) pattern;

			if (l.isEmpty())
				return isEmpty();

			if (l.getFirst().getType() == PLACEHOLDER)
			{
				final ATerm ph_type = ((ATermPlaceholder) l.getFirst()).getPlaceholder();
				if (ph_type.getType() == APPL)
				{
					final ATermAppl appl = (ATermAppl) ph_type;
					if (appl.getName().equals("list") && appl.getArguments().isEmpty())
					{
						list.add(this);
						return true;
					}
				}
			}

			if (!isEmpty())
			{
				List<Object> submatches = _first.match(l.getFirst());
				if (submatches == null)
					return false;

				list.addAll(submatches);

				submatches = _next.match(l.getNext());

				if (submatches == null)
					return false;

				list.addAll(submatches);
				return true;
			}
			return l.isEmpty();
		}

		return super.match(pattern, list);
	}

	@Override
	public ATerm make(final List<Object> args)
	{
		if (_first == null)
			return this;

		final ATerm head = _first.make(args);
		final ATermList tail = (ATermList) _next.make(args);
		if (isListPlaceHolder(_first))
			/*
			 * this is to solve the make([<list>],[]) problem the result should
			 * be [] and not [[]] to be compatible with the C version
			 */
			return head;
		return tail.insert(head);

	}

	private static boolean isListPlaceHolder(final ATerm pattern)
	{
		if (pattern.getType() == ATerm.PLACEHOLDER)
		{
			final ATerm type = ((ATermPlaceholder) pattern).getPlaceholder();
			if (type.getType() == ATerm.APPL)
			{
				final ATermAppl appl = (ATermAppl) type;
				final AFun afun = appl.getAFun();
				if (afun.getName().equals("list") && afun.getArity() == 0 && !afun.isQuoted())
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean isEmpty()
	{
		return _first == null && _next == null;
	}

	@Override
	public int getLength()
	{
		return _length;
	}

	@Override
	public ATerm getFirst()
	{
		return _first;
	}

	@Override
	public ATermList getNext()
	{
		return _next;
	}

	@Override
	public ATerm getLast()
	{
		ATermList cur;

		cur = this;
		while (!cur.getNext().isEmpty())
			cur = cur.getNext();

		return cur.getFirst();
	}

	private void raiseArgumentException(final int startPos)
	{
		throw new IllegalArgumentException("start (" + startPos + ") > length of list (" + _length + ")");
	}

	@Override
	public int indexOf(final ATerm el, final int start)
	{
		int startPos = start;
		int i;
		ATermList cur;

		if (startPos < 0)
			startPos += _length + 1;

		if (startPos > _length)
			raiseArgumentException(startPos);

		cur = this;
		for (i = 0; i < startPos; i++)
			cur = cur.getNext();

		while (!cur.isEmpty() && cur.getFirst() != el)
		{
			cur = cur.getNext();
			i++;
		}

		return cur.isEmpty() ? -1 : i;
	}

	@Override
	public int lastIndexOf(final ATerm el, final int start)
	{
		int startPos = start;
		int result;

		if (startPos < 0)
			startPos += _length + 1;

		if (startPos > _length)
			raiseArgumentException(startPos);

		if (startPos > 0)
		{
			result = _next.lastIndexOf(el, startPos - 1);
			if (result >= 0)
				return result + 1;
		}

		if (_first == el)
			return 0;
		return -1;
	}

	@Override
	public SList<ATerm> concat(final SList<ATerm> rhs)
	{
		if (isEmpty())
			return rhs;
		if (_next.isEmpty())
			return rhs.insert(_first);

		return _next.concat(rhs).insert(_first);
	}

	@Override
	public ATermList concat(final ATermList rhs)
	{
		if (isEmpty())
			return rhs;

		if (_next.isEmpty())
			return rhs.insert(_first);

		return _next.concat(rhs).insert(_first);
	}

	@Override
	public ATermList append(final ATerm el)
	{
		return concat(getEmpty().insert(el));
	}

	@Override
	public ATerm elementAt(final int index)
	{
		if (0 > index || index >= _length)
			throw new IllegalArgumentException(_illegalListIndex + index);

		ATermList cur = this;
		for (int i = 0; i < index; i++)
			cur = cur.getNext();

		return cur.getFirst();
	}

	@Override
	public ATermList remove(final ATerm el)
	{
		if (_first == el)
			return _next;

		final ATermList result = _next.remove(el);

		if (result == _next)
			return this;

		return result.insert(_first);
	}

	@Override
	public ATermList removeElementAt(final int index)
	{
		if (0 > index || index > _length)
			throw new IllegalArgumentException(_illegalListIndex + index);

		if (index == 0)
			return _next;

		return _next.removeElementAt(index - 1).insert(_first);
	}

	@Override
	public ATermList removeAll(final ATerm el)
	{
		if (_first == el)
			return _next.removeAll(el);

		final ATermList result = _next.removeAll(el);

		if (result == _next)
			return this;

		return result.insert(_first);
	}

	@Override
	public ATermList insertAt(final ATerm el, final int i)
	{
		if (0 > i || i > _length)
			throw new IllegalArgumentException(_illegalListIndex + i);

		if (i == 0)
			return insert(el);

		return _next.insertAt(el, i - 1).insert(_first);
	}

	@Override
	public ATermList getPrefix()
	{
		if (isEmpty())
			return this;

		ATermList cur = this;
		final List<ATerm> elems = new ArrayList<>();

		while (true)
		{
			if (cur.getNext().isEmpty())
			{
				cur = getPureFactory().getEmpty();
				for (int i = elems.size() - 1; i >= 0; i--)
					cur = cur.insert(elems.get(i));
				return cur;
			}
			elems.add(cur.getFirst());
			cur = cur.getNext();
		}
	}

	@Override
	public ATermList getSlice(final int start, final int end)
	{
		int i;
		final int size = end - start;

		ATermList list = this;
		for (i = 0; i < start; i++)
			list = list.getNext();

		final List<ATerm> buffer = new ArrayList<>(size);
		for (i = 0; i < size; i++)
		{
			buffer.add(list.getFirst());
			list = list.getNext();
		}

		ATermList result = getPureFactory().getEmpty();
		for (--i; i >= 0; i--)
			result = result.insert(buffer.get(i));

		return result;
	}

	@Override
	public ATermList replace(final ATerm el, final int i)
	{
		int lcv;

		if (0 > i || i > _length)
			throw new IllegalArgumentException(_illegalListIndex + i);

		final List<ATerm> buffer = new ArrayList<>(i);
		ATermList cur = this;

		for (lcv = 0; lcv < i; lcv++)
		{
			buffer.add(cur.getFirst());
			cur = cur.getNext();
		}

		/* Skip the old element */
		cur = cur.getNext();

		/* Add the new element */
		cur = cur.insert(el);

		/* Add the prefix */
		for (--lcv; lcv >= 0; lcv--)
			cur = cur.insert(buffer.get(lcv));

		return cur;
	}

	@Override
	public ATermList reverse()
	{
		ATermList cur = this;
		ATermList reverse = getEmpty();
		while (!cur.isEmpty())
		{
			reverse = reverse.insert(cur.getFirst());
			cur = cur.getNext();
		}
		return reverse;
	}

	@Override
	public ATerm dictGet(final ATerm key)
	{
		if (isEmpty())
			return null;

		final ATermList pair = (ATermList) _first;

		if (key.equals(pair.getFirst()))
			return pair.getNext().getFirst();

		return _next.dictGet(key);
	}

	@Override
	public ATermList dictPut(final ATerm key, final ATerm value)
	{
		if (isEmpty())
		{
			final ATermList pair = getEmpty().insert(value).insert(key);
			return getEmpty().insert(pair);
		}

		ATermList pair = (ATermList) _first;
		if (key.equals(pair.getFirst()))
		{
			pair = getEmpty().insert(value).insert(pair);
			return _next.insert(pair);

		}

		return _next.dictPut(key, value).insert(_first);
	}

	@Override
	public ATermList dictRemove(final ATerm key)
	{
		if (isEmpty())
			return this;

		final ATermList pair = (ATermList) _first;

		if (key.equals(pair.getFirst()))
			return _next;

		return _next.dictRemove(key).insert(_first);
	}

	@Override
	public ATerm accept(final Visitor<ATerm> v)
	{
		return v.visitList(this);
	}

	@Override
	public int getNrSubTerms()
	{
		return _length;
	}

	@Override
	public ATerm getSubTerm(final int index)
	{
		return elementAt(index);
	}

	@Override
	public ATerm setSubTerm(final int index, final ATerm t)
	{
		return replace(t, index);
	}

	protected int findEmptyHashCode()
	{
		int magic = 0;
		for (int x = Integer.MIN_VALUE; x < Integer.MAX_VALUE; x++)
		{
			final int a = GOLDEN_RATIO + (x << 16);
			final int c = HashFunctions.mix(a, GOLDEN_RATIO, 3);

			if (c == x)
				magic = x;
		}

		return magic;
	}

	private int hashFunction()
	{
		int a = GOLDEN_RATIO;

		if (_next != null && _first != null)
		{
			a += _next.hashCode() << 8;
			a += _first.hashCode();
		}

		return HashFunctions.mix(a, GOLDEN_RATIO, 3);
	}

}
