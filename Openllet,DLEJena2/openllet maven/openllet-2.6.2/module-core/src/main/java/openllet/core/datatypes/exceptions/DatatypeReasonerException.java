package openllet.core.datatypes.exceptions;

/**
 * <p>
 * Title: Datatype Reasoner Exception
 * </p>
 * <p>
 * Description: Exception superclass for all exceptions thrown by datatype reasoner methods.
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
public class DatatypeReasonerException extends Exception
{

	private static final long serialVersionUID = 1L;

	public DatatypeReasonerException(final String message)
	{
		super(message);
	}
}
