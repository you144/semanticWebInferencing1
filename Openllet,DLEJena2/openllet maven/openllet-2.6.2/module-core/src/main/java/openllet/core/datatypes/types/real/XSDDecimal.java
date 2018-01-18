package openllet.core.datatypes.types.real;

import static java.lang.String.format;

import java.math.BigDecimal;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import openllet.aterm.ATermAppl;
import openllet.core.datatypes.AbstractBaseDatatype;
import openllet.core.datatypes.Datatype;
import openllet.core.datatypes.OWLRealUtils;
import openllet.core.datatypes.RestrictedDatatype;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.Namespaces;
import openllet.shared.tools.Log;

/**
 * <p>
 * Title: <code>xsd:decimal</code>
 * </p>
 * <p>
 * Description: Singleton implementation of <code>xsd:decimal</code> datatype
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
public class XSDDecimal extends AbstractBaseDatatype<Number>
{

	private static final XSDDecimal _instance = new XSDDecimal();
	private static final Logger _logger = Log.getLogger(XSDDecimal.class);

	public static XSDDecimal getInstance()
	{
		return _instance;
	}

	private final RestrictedRealDatatype dataRange;

	/**
	 * Private constructor forces use of {@link #getInstance()}
	 */
	private XSDDecimal()
	{
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "decimal"));

		dataRange = new RestrictedRealDatatype(this, IntegerInterval.allIntegers(), ContinuousRealInterval.allReals(), null);
	}

	@Override
	public RestrictedDatatype<Number> asDataRange()
	{
		return dataRange;
	}

	@Override
	public ATermAppl getCanonicalRepresentation(final ATermAppl input) throws InvalidLiteralException
	{
		final String lexicalForm = getLexicalForm(input);
		try
		{
			final BigDecimal d = DatatypeConverter.parseDecimal(lexicalForm);
			/*
			 * TODO: Determine if this is, in fact a functional mapping
			 */
			final String canonicalForm = DatatypeConverter.printDecimal(d);
			if (canonicalForm.equals(lexicalForm))
				return input;
			else
				return ATermUtils.makeTypedLiteral(canonicalForm, getName());
		}
		catch (final NumberFormatException e)
		{
			_logger.severe(format("Number format exception (%s) cause while parsing decimal %s", e.getMessage(), lexicalForm));
			throw new InvalidLiteralException(getName(), lexicalForm);
		}
	}

	@Override
	public ATermAppl getLiteral(final Object value)
	{
		if (dataRange.contains(value))
			return ATermUtils.makeTypedLiteral(OWLRealUtils.print((Number) value), getName());
		else
			throw new IllegalArgumentException();
	}

	@Override
	public Datatype<?> getPrimitiveDatatype()
	{
		return this;
	}

	@Override
	public Number getValue(final ATermAppl literal) throws InvalidLiteralException
	{
		final String lexicalForm = getLexicalForm(literal);
		try
		{
			return OWLRealUtils.getCanonicalObject(DatatypeConverter.parseDecimal(lexicalForm));
		}
		catch (final NumberFormatException e)
		{
			throw new InvalidLiteralException(getName(), lexicalForm, e);
		}
	}

	@Override
	public boolean isPrimitive()
	{
		return false;
	}

}
