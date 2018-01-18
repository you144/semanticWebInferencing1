package openllet.core.datatypes.types.uri;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import openllet.aterm.ATermAppl;
import openllet.core.datatypes.Datatype;
import openllet.core.datatypes.RestrictedDatatype;
import openllet.core.datatypes.exceptions.InvalidConstrainingFacetException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.SetUtils;

/**
 * <p>
 * Title: Restricted URI Datatype
 * </p>
 * <p>
 * Description: A subset of the value space of xsd:anyURI
 * </p>
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class RestrictedURIDatatype implements RestrictedDatatype<ATermAppl>
{
	private final Datatype<ATermAppl> _dt;
	private final Set<Object> _excludedValues;

	public RestrictedURIDatatype(final Datatype<ATermAppl> dt)
	{
		this(dt, Collections.emptySet());
	}

	private RestrictedURIDatatype(final Datatype<ATermAppl> dt, final Set<Object> excludedValues)
	{
		_dt = dt;
		_excludedValues = excludedValues;
	}

	@Override
	public RestrictedDatatype<ATermAppl> applyConstrainingFacet(final ATermAppl facet, final Object value) throws InvalidConstrainingFacetException
	{
		// TODO: support facets
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(final Object value)
	{
		if (value instanceof ATermAppl)
		{
			final ATermAppl a = (ATermAppl) value;

			if (_excludedValues.contains(a))
				return false;

			if (ATermUtils.isLiteral(a) && XSDAnyURI.NAME.equals(a.getArgument(ATermUtils.LIT_URI_INDEX)))
				return true;
		}
		return false;
	}

	@Override
	public boolean containsAtLeast(final int n)
	{
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public RestrictedDatatype<ATermAppl> exclude(final Collection<?> values)
	{
		final Set<Object> newExcludedValues = (Set) SetUtils.create(values);
		newExcludedValues.addAll(_excludedValues);
		return new RestrictedURIDatatype(_dt, newExcludedValues);
	}

	@Override
	public Datatype<? extends ATermAppl> getDatatype()
	{
		return _dt;
	}

	@Override
	public RestrictedDatatype<ATermAppl> intersect(final RestrictedDatatype<?> other, final boolean negated)
	{
		if (other instanceof RestrictedURIDatatype)
			return this;
		else
			throw new IllegalArgumentException();
	}

	@Override
	public boolean isEmpty()
	{
		return false;
	}

	@Override
	public boolean isEnumerable()
	{
		return false;
	}

	@Override
	public boolean isFinite()
	{
		return false;
	}

	@Override
	public RestrictedDatatype<ATermAppl> union(final RestrictedDatatype<?> other)
	{
		if (other instanceof RestrictedURIDatatype)
			return this;
		else
			throw new IllegalArgumentException();
	}

	@Override
	public Iterator<ATermAppl> valueIterator()
	{
		throw new IllegalStateException();
	}

}
