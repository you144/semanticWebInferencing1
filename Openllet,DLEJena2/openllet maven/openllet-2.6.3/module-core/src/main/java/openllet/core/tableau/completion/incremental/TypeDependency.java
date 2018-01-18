// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.completion.incremental;

import openllet.aterm.ATermAppl;

/**
 * A _type dependency.
 *
 * @author Christian Halaschek-Wiener
 */
public class TypeDependency implements Dependency
{

	/**
	 * The _type
	 */
	private final ATermAppl _type;

	/**
	 * The _individual
	 */
	private final ATermAppl _ind;

	/**
	 * Constructor
	 *
	 * @param ind
	 * @param type
	 */
	public TypeDependency(final ATermAppl ind, final ATermAppl type)
	{
		_type = type;
		_ind = ind;
	}

	/**
	 * @return the _individual
	 */
	public ATermAppl getInd()
	{
		return _ind;
	}

	/**
	 * @return the _type
	 */
	public ATermAppl getType()
	{
		return _type;
	}

	/**
	 * ToString method
	 */
	@Override
	public String toString()
	{
		return "Type [" + _ind + "]  - [" + _type + "]";
	}

	/**
	 * Equals method
	 */
	@Override
	public boolean equals(final Object other)
	{
		if (other instanceof TypeDependency)
			return _ind.equals(((TypeDependency) other)._ind) && _type.equals(((TypeDependency) other)._type);
		else
			return false;
	}

	/**
	 * Hashcode method
	 */
	@Override
	public int hashCode()
	{
		return _ind.hashCode() + _type.hashCode();
	}

}
