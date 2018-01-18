package openllet.core.datatypes.types.real;

import javax.xml.bind.DatatypeConverter;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.Namespaces;

/**
 * <p>
 * Title: <code>xsd:long</code>
 * </p>
 * <p>
 * Description: Singleton implementation of <code>xsd:long</code> datatype
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
public class XSDLong extends AbstractDerivedIntegerType
{

	private static final XSDLong instance = new XSDLong();

	public static XSDLong getInstance()
	{
		return instance;
	}

	private XSDLong()
	{
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "long"), Long.MIN_VALUE, Long.MAX_VALUE);
	}

	@Override
	protected Number fromLexicalForm(final String lexicalForm) throws InvalidLiteralException
	{
		try
		{
			return DatatypeConverter.parseLong(lexicalForm);
		}
		catch (final NumberFormatException e)
		{
			throw new InvalidLiteralException(getName(), lexicalForm, e);
		}
	}
}
