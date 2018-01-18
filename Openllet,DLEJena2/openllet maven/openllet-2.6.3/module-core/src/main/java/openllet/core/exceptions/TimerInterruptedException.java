// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
package openllet.core.exceptions;

public class TimerInterruptedException extends PelletRuntimeException
{

	private static final long serialVersionUID = 2528189321875269169L;

	public TimerInterruptedException()
	{
		super();
	}

	public TimerInterruptedException(final String s)
	{
		super(s);
	}

	public TimerInterruptedException(final Throwable e)
	{
		super(e);
	}
}
