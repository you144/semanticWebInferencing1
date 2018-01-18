package openllet.core.datatypes.types.real;

import openllet.aterm.ATermAppl;
import openllet.core.datatypes.AbstractBaseDatatype;
import openllet.core.datatypes.Datatype;
import openllet.core.datatypes.OWLRealUtils;
import openllet.core.datatypes.RestrictedDatatype;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.Namespaces;

/**
 * <p>
 * Title: <code>owl:rational</code>
 * </p>
 * <p>
 * Description: Singleton implementation of <code>owl:rational</code> datatype
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
public class OWLRational extends AbstractBaseDatatype<Number>
{

	private static final OWLRational instance = new OWLRational();

	public static OWLRational getInstance()
	{
		return instance;
	}

	private final RestrictedRealDatatype _dataRange;

	private OWLRational()
	{
		super(ATermUtils.makeTermAppl(Namespaces.OWL + "rational"));

		_dataRange = new RestrictedRealDatatype(this, IntegerInterval.allIntegers(), ContinuousRealInterval.allReals(), ContinuousRealInterval.allReals());
	}

	@Override
	public RestrictedDatatype<Number> asDataRange()
	{
		return _dataRange;
	}

	@Override
	public ATermAppl getCanonicalRepresentation(final ATermAppl input) throws InvalidLiteralException
	{
		final Number n = getValue(input);
		if (n instanceof Rational)
		{
			final String canonicalForm = Rational.simplify((Rational) n).toString();
			if (canonicalForm.equals(ATermUtils.getLiteralValue(input)))
				return input;
			else
				return ATermUtils.makeTypedLiteral(canonicalForm, getName());
		}
		else
			return OWLReal.getInstance().getLiteral(OWLRealUtils.getCanonicalObject(n));
	}

	@Override
	public ATermAppl getLiteral(final Object value)
	{
		if (value instanceof Rational)
		{
			final Rational r = (Rational) value;
			final String lexicalForm = r.toString();
			return ATermUtils.makeTypedLiteral(lexicalForm, getName());
		}
		else
			throw new IllegalArgumentException();
	}

	@Override
	public Datatype<?> getPrimitiveDatatype()
	{
		return OWLReal.getInstance();
	}

	@Override
	public Number getValue(final ATermAppl literal) throws InvalidLiteralException
	{
		final String lexicalForm = getLexicalForm(literal);
		try
		{
			return OWLRealUtils.getCanonicalObject(Rational.valueOf(lexicalForm));
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
