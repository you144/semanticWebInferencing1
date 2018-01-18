// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.exceptions;

/*
 * Superclass for all pellet-specific exceptions.
 */
public class PelletRuntimeException extends RuntimeException
{

	private static final long serialVersionUID = 6095814026634083920L;

	public PelletRuntimeException()
	{
		super();
	}

	public PelletRuntimeException(final String s)
	{
		super(s);
	}

	public PelletRuntimeException(final Throwable e)
	{
		super(e);
	}

	public PelletRuntimeException(final String msg, final Throwable cause)
	{
		super(msg, cause);
	}

}
