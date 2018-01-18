// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules.model;

import openllet.aterm.ATermAppl;
import openllet.core.utils.ATermUtils;

/**
 * <p>
 * Title: Atom Data Constant
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
public class AtomDConstant extends AtomConstant implements AtomDObject
{

	public AtomDConstant(final ATermAppl value)
	{
		super(value);
	}

	@Override
	public void accept(final AtomObjectVisitor visitor)
	{
		visitor.visit(this);
	}

	@Override
	public String toString()
	{
		return ATermUtils.toString(getValue());
	}

}
