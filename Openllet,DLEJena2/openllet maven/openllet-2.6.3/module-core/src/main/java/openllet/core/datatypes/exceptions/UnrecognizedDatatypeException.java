package openllet.core.datatypes.exceptions;

import openllet.aterm.ATermAppl;

/**
 * <p>
 * Title: Unrecognized Datatype Exception
 * </p>
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Mike Smith
 */
public class UnrecognizedDatatypeException extends DatatypeReasonerException
{

	private static final long serialVersionUID = 1L;

	private final ATermAppl _dt;

	public UnrecognizedDatatypeException(final ATermAppl dt)
	{
		super("Unrecognized datatype " + dt.getName());
		_dt = dt;
	}

	public ATermAppl getDatatype()
	{
		return _dt;
	}
}
