package openllet.core.datatypes;

import static java.lang.String.format;

import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import openllet.aterm.ATermAppl;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.Namespaces;
import openllet.shared.tools.Log;

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
public class XSDLong implements Datatype<Number>
{

	private static final XSDLong instance = new XSDLong();
	private static final Logger _logger = Log.getLogger(XSDLong.class);

	public static XSDLong getInstance()
	{
		return instance;
	}

	private final RestrictedDatatype<Number> dataRange;
	private final int hashCode;
	private final ATermAppl name;

	private XSDLong()
	{
		name = ATermUtils.makeTermAppl(Namespaces.XSD + "long");
		hashCode = name.hashCode();

		final OWLRealInterval i = new OWLRealInterval(Long.MIN_VALUE, Long.MAX_VALUE, true, true, OWLRealInterval.LineType.INTEGER_ONLY);
		dataRange = new RestrictedRealDatatype(this, i);
	}

	@Override
	public RestrictedDatatype<Number> asDataRange()
	{
		return dataRange;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return getClass() == obj.getClass();
	}

	@Override
	public ATermAppl getCanonicalRepresentation(final ATermAppl input) throws InvalidLiteralException
	{
		if (!ATermUtils.isLiteral(input))
			throw new IllegalArgumentException();
		if (!input.getArgument(ATermUtils.LIT_URI_INDEX).equals(name))
			throw new IllegalArgumentException();

		final String lexicalForm = ATermUtils.getLiteralValue(input);
		try
		{
			DatatypeConverter.parseLong(lexicalForm);
		}
		catch (final NumberFormatException e)
		{
			_logger.severe(format("Number format exception (%s) cause while parsing long %s", e.getMessage(), lexicalForm));
			throw new InvalidLiteralException(name, lexicalForm);
		}
		return XSDDecimal.getInstance().getCanonicalRepresentation(ATermUtils.makeTypedLiteral(lexicalForm, XSDDecimal.getInstance().getName()));
	}

	@Override
	public ATermAppl getLiteral(final Object value)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ATermAppl getName()
	{
		return name;
	}

	@Override
	public Datatype<?> getPrimitiveDatatype()
	{
		return XSDDecimal.getInstance();
	}

	@Override
	public Number getValue(final ATermAppl literal) throws InvalidLiteralException
	{
		if (!ATermUtils.isLiteral(literal))
			throw new IllegalArgumentException();
		if (!literal.getArgument(ATermUtils.LIT_URI_INDEX).equals(name))
			throw new IllegalArgumentException();

		final String lexicalForm = ATermUtils.getLiteralValue(literal);
		try
		{
			return OWLRealUtils.getCanonicalObject(DatatypeConverter.parseLong(lexicalForm));
		}
		catch (final NumberFormatException e)
		{
			throw new InvalidLiteralException(name, lexicalForm, e);
		}
	}

	@Override
	public int hashCode()
	{
		return hashCode;
	}

	@Override
	public boolean isPrimitive()
	{
		return false;
	}

}
