package openllet.core.datatypes;

import openllet.aterm.ATermAppl;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.utils.ATermUtils;

/**
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
class NamedDatatype<T> implements Datatype<T>
{
	private final ATermAppl _name;
	private final RestrictedDatatype<T> _range;

	NamedDatatype(final ATermAppl name, final RestrictedDatatype<T> range)
	{
		if (name == null)
			throw new NullPointerException();
		if (name.getArity() != 0)
			throw new IllegalArgumentException();

		_name = name;
		_range = range;
	}

	@Override
	public RestrictedDatatype<T> asDataRange()
	{
		return _range;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final NamedDatatype<?> other = (NamedDatatype<?>) obj;
		if (_name == null)
		{
			if (other._name != null)
				return false;
		}
		else
			if (!_name.equals(other._name))
				return false;
		return true;
	}

	@Override
	public ATermAppl getCanonicalRepresentation(final ATermAppl input) throws InvalidLiteralException
	{
		return _range.getDatatype().getCanonicalRepresentation(input);
	}

	@Override
	public ATermAppl getLiteral(final Object value)
	{
		if (value instanceof ATermAppl)
		{
			final ATermAppl a = (ATermAppl) value;
			if (ATermUtils.isLiteral(a))
				if (_name.equals(a.getArgument(ATermUtils.LIT_URI_INDEX)))
					return a;
		}
		throw new IllegalArgumentException();
	}

	@Override
	public ATermAppl getName()
	{
		return _name;
	}

	@Override
	public Datatype<?> getPrimitiveDatatype()
	{
		return _range.getDatatype().getPrimitiveDatatype();
	}

	@Override
	public T getValue(final ATermAppl literal) throws InvalidLiteralException
	{
		final T value = _range.getDatatype().getValue(literal);

		if (!_range.contains(value))
			throw new InvalidLiteralException(_name, literal.getArgument(ATermUtils.LIT_VAL_INDEX).toString());

		return value;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (_name == null ? 0 : _name.hashCode());
		return result;
	}

	@Override
	public boolean isPrimitive()
	{
		return false;
	}

	@Override
	public String toString()
	{
		return _name.getName();
	}

}
