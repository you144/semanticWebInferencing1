package openllet.core.datatypes.types.real;

import openllet.aterm.ATermAppl;
import openllet.core.datatypes.AbstractBaseDatatype;
import openllet.core.datatypes.Datatype;
import openllet.core.datatypes.RestrictedDatatype;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.Namespaces;

/**
 * <p>
 * Title: <code>owl:real</code>
 * </p>
 * <p>
 * Description: Singleton implementation of <code>owl:real</code> datatype
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
public class OWLReal extends AbstractBaseDatatype<Number>
{

	private static final OWLReal instance = new OWLReal();

	public static OWLReal getInstance()
	{
		return instance;
	}

	private final RestrictedRealDatatype dataRange;

	private OWLReal()
	{
		super(ATermUtils.makeTermAppl(Namespaces.OWL + "real"));

		dataRange = new RestrictedRealDatatype(this, IntegerInterval.allIntegers(), ContinuousRealInterval.allReals(), ContinuousRealInterval.allReals());
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
		throw new InvalidLiteralException(getName(), lexicalForm);
	}

	@Override
	public ATermAppl getLiteral(final Object value)
	{
		if (value instanceof Rational)
			return OWLRational.getInstance().getLiteral(value);
		else
			if (value instanceof Number)
				try
				{
					return XSDDecimal.getInstance().getLiteral(value);
				}
				catch (final IllegalArgumentException e)
				{
					throw new IllegalArgumentException(e);
				}
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
		throw new InvalidLiteralException(getName(), lexicalForm);
	}

	@Override
	public boolean isPrimitive()
	{
		return true;
	}

}
