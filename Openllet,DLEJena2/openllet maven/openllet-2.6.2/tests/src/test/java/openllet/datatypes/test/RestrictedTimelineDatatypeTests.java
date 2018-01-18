package openllet.datatypes.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import openllet.aterm.ATermAppl;
import openllet.core.datatypes.Datatype;
import openllet.core.datatypes.RestrictedDatatype;
import openllet.core.datatypes.types.datetime.RestrictedTimelineDatatype;
import org.junit.Test;

/**
 * <p>
 * Title: Restricted Timeline Datatype Tests
 * </p>
 * <p>
 * Description: Unit tests for {@link RestrictedTimelineDatatype}
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
public class RestrictedTimelineDatatypeTests
{

	private final static Datatype<XMLGregorianCalendar> dt;

	static
	{
		dt = new Datatype<XMLGregorianCalendar>()
		{

			@Override
			public RestrictedDatatype<XMLGregorianCalendar> asDataRange()
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public ATermAppl getCanonicalRepresentation(final ATermAppl literal)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public ATermAppl getLiteral(final Object value)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public ATermAppl getName()
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public Datatype<?> getPrimitiveDatatype()
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public XMLGregorianCalendar getValue(final ATermAppl literal)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isPrimitive()
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public String toString()
			{
				return "StubDt";
			}

		};
	}

	private static XMLGregorianCalendar dateTime(final String s)
	{
		return RestrictedTimelineDatatype.getDatatypeFactory().newXMLGregorianCalendar(s);
	}

	/**
	 * Test that intersecting a full number line with the negation of one that only permits decimals, leaves only rationals
	 */
	@Test
	public void intersectToNZOnly()
	{
		RestrictedDatatype<XMLGregorianCalendar> dr = new RestrictedTimelineDatatype(dt, DatatypeConstants.DATETIME, false);

		assertTrue(dr.contains(dateTime("2009-01-01T12:00:00Z")));
		assertTrue(dr.contains(dateTime("2006-06-01T06:14:23")));

		dr = dr.intersect(new RestrictedTimelineDatatype(dt, DatatypeConstants.DATETIME, true), true);

		assertFalse(dr.contains(dateTime("2009-01-01T12:00:00Z")));
		assertTrue(dr.contains(dateTime("2006-06-01T06:14:23")));
	}
}
