// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules.model;

import openllet.aterm.ATermAppl;

/**
 * <p>
 * Title: Atom Constant
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
public abstract class AtomConstant implements AtomObject
{

	private final ATermAppl _value;

	public AtomConstant(final ATermAppl value)
	{
		_value = value;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof AtomConstant))
			return false;
		final Object otherValue = ((AtomConstant) other)._value;
		return _value == otherValue || _value != null && _value.equals(otherValue);
	}

	/**
	 * @return the openllet.aterm _value this constant was initialized with.
	 */
	public ATermAppl getValue()
	{
		return _value;
	}

	@Override
	public int hashCode()
	{
		return _value.hashCode();
	}

}
