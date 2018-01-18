package openllet.core.datatypes.types.real;

import javax.xml.bind.DatatypeConverter;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.Namespaces;

/**
 * <p>
 * Title: <code>xsd:byte</code>
 * </p>
 * <p>
 * Description: Singleton implementation of <code>xsd:byte</code> datatype
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
public class XSDByte extends AbstractDerivedIntegerType
{

	private static final XSDByte instance = new XSDByte();

	public static XSDByte getInstance()
	{
		return instance;
	}

	private XSDByte()
	{
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "byte"), Byte.MIN_VALUE, Byte.MAX_VALUE);
	}

	@Override
	protected Number fromLexicalForm(final String lexicalForm) throws InvalidLiteralException
	{
		try
		{
			final int n = DatatypeConverter.parseInt(lexicalForm);
			if (n < Byte.MIN_VALUE || n > Byte.MAX_VALUE)
				throw new InvalidLiteralException(getName(), lexicalForm);
			return Byte.valueOf((byte) n);
		}
		catch (final NumberFormatException e)
		{
			throw new InvalidLiteralException(getName(), lexicalForm, e);
		}
	}
}
