// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules.model;

import openllet.aterm.ATermAppl;
import openllet.core.utils.URIUtils;

/**
 * <p>
 * Title: Atom Variable
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Ron Alford
 */
public abstract class AtomVariable implements AtomObject
{
	private final String _name;

	public AtomVariable(final String name)
	{
		_name = name;
	}

	public int compareTo(final ATermAppl arg0)
	{
		return getName().compareTo(arg0.getName());
	}

	/**
	 * Checks if this variable is equal to some other variable.
	 */
	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof AtomVariable))
			return false;
		return getName().equals(((AtomVariable) other).getName());
	}

	public String getName()
	{
		return _name;
	}

	@Override
	public int hashCode()
	{
		return _name.hashCode();
	}

	@Override
	public String toString()
	{
		return "?" + URIUtils.getLocalName(_name);
	}
}
