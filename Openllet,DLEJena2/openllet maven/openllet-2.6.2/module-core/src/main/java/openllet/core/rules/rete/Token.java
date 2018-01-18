// Copyright (c) 2006 - 2010, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules.rete;

import java.util.Arrays;
import openllet.core.DependencySet;

/**
 * @author Evren Sirin
 */
public abstract class Token
{
	public abstract WME get(int index);

	public abstract DependencySet getDepends(boolean doExplanation);

	public abstract boolean dependsOn(int branch);

	@SuppressWarnings("unused")
	private static class ListToken extends Token
	{
		private final ListToken _next;
		private final WME _wme;
		private final int _index;

		public ListToken(final WME wme, final ListToken tok)
		{
			_wme = wme;
			_next = tok;
			_index = (tok == null) ? 0 : tok._index + 1;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public WME get(final int index)
		{
			for (ListToken t = this; t != null; t = t._next)
				if (t._index == index)
					return t._wme;

			throw new IndexOutOfBoundsException(index + " > " + _index);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DependencySet getDepends(final boolean doExplanation)
		{
			DependencySet ds = DependencySet.INDEPENDENT;

			for (ListToken t = this; t != null; t = t._next)
				ds = ds.union(t._wme.getDepends(), doExplanation);

			return ds;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean dependsOn(final int branch)
		{
			for (ListToken t = this; t != null; t = t._next)
				if (t._wme.dependsOn(branch))
					return true;
			return false;
		}

		@Override
		public String toString()
		{
			final StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (ListToken t = this; t != null; t = t._next)
			{
				sb.append(t._wme.toString());
				sb.append(',');
			}
			sb.setCharAt(sb.length() - 1, ']');
			return sb.toString();
		}
	}

	private static class ArrayToken extends Token
	{
		private final WME[] _wmes;

		public ArrayToken(final WME wme, final ArrayToken tok)
		{
			final int l = tok == null ? 0 : tok._wmes.length;
			_wmes = tok == null ? new WME[1] : Arrays.copyOf(tok._wmes, l + 1);
			_wmes[l] = wme;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public WME get(final int index)
		{
			if (index >= _wmes.length)
				throw new ArrayIndexOutOfBoundsException();
			return _wmes[index];
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DependencySet getDepends(final boolean doExplanation)
		{
			DependencySet ds = DependencySet.INDEPENDENT;

			for (final WME wme : _wmes)
				ds = ds.union(wme.getDepends(), doExplanation);

			return ds;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean dependsOn(final int branch)
		{
			for (final WME wme : _wmes)
				if (wme.dependsOn(branch))
					return true;
			return false;
		}

		@Override
		public String toString()
		{
			return Arrays.toString(_wmes);
		}
	}

	public static Token create(final WME wme, final Token token)
	{
		//	    return new ListToken(_wme, (ListToken) token);
		return new ArrayToken(wme, (ArrayToken) token);
	}

}
