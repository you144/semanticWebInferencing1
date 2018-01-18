// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet;

import openllet.atom.OpenError;

/**
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Markus Stocker
 */
public class OpenlletCmdException extends OpenError
{
	private static final long serialVersionUID = -5472994436987740189L;

	/**
	 * Create an exception with the given error message.
	 *
	 * @param msg
	 */
	public OpenlletCmdException(final String msg)
	{
		super(msg);
	}

	public OpenlletCmdException(final Throwable cause)
	{
		super(cause);
	}

	public OpenlletCmdException(final String msg, final Throwable cause)
	{
		super(msg, cause);
	}

}
