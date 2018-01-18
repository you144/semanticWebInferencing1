// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import openllet.atom.OpenError;

public class NumberUtils
{
	public static final int BYTE = 0;
	public static final int SHORT = 1;
	public static final int INT = 2;
	public static final int LONG = 3;
	public static final int INTEGER = 4;
	public static final int DECIMAL = 5;
	public static final int FLOAT = 6;
	public static final int DOUBLE = 7;
	private static final int TYPES = 8;

	public static final Byte BYTE_ZERO = Byte.valueOf((byte) 0);
	public static final Short SHORT_ZERO = Short.valueOf((short) 0);
	public static final Integer INT_ZERO = Integer.valueOf(0);
	public static final Long LONG_ZERO = Long.valueOf(0);
	public static final BigInteger INTEGER_ZERO = BigInteger.valueOf(0);
	public static final BigDecimal DECIMAL_ZERO = BigDecimal.valueOf(0);
	public static final Float FLOAT_ZERO = Float.valueOf(0);
	public static final Double DOUBLE_ZERO = Double.valueOf(0);

	private static final List<Class<?>> classes = Arrays.asList(new Class[] { Byte.class, Short.class, Integer.class, Long.class, BigInteger.class, BigDecimal.class, Float.class, Double.class });
	private static final List<String> names = Arrays.asList(new String[] { "Byte", "Short", "Integer", "Long", "BigInteger", "BigDecimal", "Float", "Double" });
	private static final List<Number> zeros = Arrays.asList(new Number[] { BYTE_ZERO, SHORT_ZERO, INT_ZERO, LONG_ZERO, INTEGER_ZERO, DECIMAL_ZERO, FLOAT_ZERO, DOUBLE_ZERO });

	public static Number parseByte(final String str) throws NumberFormatException
	{
		return parse(str, BYTE);
	}

	public static Number parseShort(final String str) throws NumberFormatException
	{
		return parse(str, SHORT);
	}

	public static Number parseInt(final String str) throws NumberFormatException
	{
		return parse(str, INT);
	}

	public static Number parseLong(final String str) throws NumberFormatException
	{
		return parse(str, LONG);
	}

	public static Number parseInteger(final String str) throws NumberFormatException
	{
		return parse(str, INTEGER);
	}

	public static Number parseDecimal(final String str) throws NumberFormatException
	{
		return parse(str, DECIMAL);
	}

	public static Float parseFloat(final String str) throws NumberFormatException
	{
		return (Float) parse(str, FLOAT);
	}

	public static Double parseDouble(final String str) throws NumberFormatException
	{
		return (Double) parse(str, DOUBLE);
	}

	public static Number parse(final String strParam, final int type) throws NumberFormatException
	{
		String str = strParam;

		if (0 > type || type >= TYPES)
			throw new UnsupportedOperationException("Unknown numeric type " + type);

		if (type == FLOAT)
			return Float.valueOf(str);
		if (type == DOUBLE)
			return Double.valueOf(str);

		int idx = 0;
		final int len = str.length();

		if (len == 0)
			throw new NumberFormatException("Empty string");

		boolean negate = false;
		int start = 0;
		switch (str.charAt(idx))
		{
			case '+':
				start = 1;
				idx++;
				break; // ignore the sign
			case '-':
				negate = true;
				idx++;
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				break;
			default:
				throw new NumberFormatException("Invalid char '" + str.charAt(idx) + "' in " + str);
		}

		if (idx == len)
			throw new NumberFormatException("Invalid number that has only sign " + (negate ? '-' : '+'));

		// skip leading '0'
		while (idx < len && str.charAt(idx) == '0')
			idx++;

		if (idx == len)
			// all the digits are skipped: that means this value is 0
			return BYTE_ZERO;

		Number number = null;

		long prev = 0;
		long curr = 0;

		// adding digits
		while (idx < len)
		{
			final char ch = str.charAt(idx++);
			if ('0' <= ch && ch <= '9')
			{
				if (negate)
					curr = (10 * curr) + ('0' - ch);
				else
					curr = (10 * curr) + (ch - '0');
				if ((curr < prev) != negate)
				{ // overflow
					final int fractionPoint = str.indexOf('.');
					if (fractionPoint == -1)
						number = new BigInteger(str.substring(start));
					else
					{
						int lastNonZeroDigit = len;
						while (lastNonZeroDigit >= fractionPoint)
							if (str.charAt(--lastNonZeroDigit) != '0')
								break;
						if (lastNonZeroDigit <= fractionPoint)
							number = new BigInteger(str.substring(start, fractionPoint));
						else
							if (lastNonZeroDigit != len)
								number = new BigDecimal(str.substring(start, lastNonZeroDigit));
							else
								number = new BigDecimal(str.substring(start));
					}
					break;
				}

				prev = curr;
			}
			else
				if (ch == '.')
				{
					int lastNonZeroDigit = len;
					final int fractionPoint = idx - 1;
					while (lastNonZeroDigit >= fractionPoint)
						if (str.charAt(--lastNonZeroDigit) != '0')
							break;
					if (lastNonZeroDigit <= fractionPoint)
						number = new BigInteger(str.substring(start, fractionPoint));
					else
						if (lastNonZeroDigit != len - 1)
						{
							str = str.substring(0, lastNonZeroDigit + 1);
							number = new BigDecimal(str.substring(start));
						}
						else
							number = new BigDecimal(str.substring(start));
					break;
				}
				else
					throw new NumberFormatException("Invalid char '" + ch + "' in " + str);
		}

		if (number == null)
			if (Byte.MIN_VALUE <= curr && curr <= Byte.MAX_VALUE)
				number = Byte.valueOf((byte) curr);
			else
				if (Short.MIN_VALUE <= curr && curr <= Short.MAX_VALUE)
					number = Short.valueOf((short) curr);
				else
					if (Integer.MIN_VALUE <= curr && curr <= Integer.MAX_VALUE)
						number = Integer.valueOf((int) curr);
					else
						number = Long.valueOf(curr);

		final int foundType = classes.indexOf(number.getClass());
		if (type < foundType)
			throw new NumberFormatException(str + " is not a valid " + names.get(type));

		return number;
	}

	public static Number add(final Number n1, final int n2)
	{
		final int type1 = classes.indexOf(n1.getClass());

		switch (type1)
		{
			case BYTE:
				return Integer.valueOf(((Byte) n1).intValue() + n2);
			case SHORT:
				return Integer.valueOf(((Short) n1).intValue() + n2);
			case INT:
				return Integer.valueOf(((Integer) n1).intValue() + n2);
			case LONG:
				return Long.valueOf(((Long) n1).longValue() + n2);
			case INTEGER:
				return ((BigInteger) n1).add(BigInteger.valueOf(n2));
			case DECIMAL:
				return ((BigDecimal) n1).add(BigDecimal.valueOf(n2));
			case FLOAT:
				return Float.valueOf(((Float) n1).floatValue() + (n2 * Float.MIN_VALUE));
			case DOUBLE:
				return Double.valueOf(((Double) n1).doubleValue() + (n2 * Double.MIN_VALUE));
			default:
				throw new IllegalArgumentException("Unknown number class " + n1.getClass());
		}
	}

	public static int getType(final Number number)
	{
		return classes.indexOf(number.getClass());
	}

	public static String getTypeName(final Number number)
	{
		if (number == null)
			return "null";
		return names.get(classes.indexOf(number.getClass())).toString();
	}

	//    public static boolean comparable( Number n1, Number n2 ) {
	//        return getType( n1 ) == getType( n2 );
	//    }

	private static BigDecimal convertToDecimal(final Number n)
	{
		if (n instanceof BigDecimal)
			return (BigDecimal) n;
		else
			return new BigDecimal(n.toString());
	}

	@SuppressWarnings("unchecked")
	public static int sign(final Number n)
	{
		final int type = classes.indexOf(n.getClass());
		final Number zero = zeros.get(type);

		return ((Comparable<Number>) n).compareTo(zero);
	}

	public static int compare(final Number n1, final Number n2)
	{
		final int type1 = classes.indexOf(n1.getClass());
		final int type2 = classes.indexOf(n2.getClass());

		if (type1 != type2)
		{
			if (type1 > DECIMAL || type2 > DECIMAL)
				throw new IllegalArgumentException("Trying to compare incompatible values " + n1 + " " + n2);

			if (type1 == DECIMAL || type2 == DECIMAL)
				return convertToDecimal(n1).compareTo(convertToDecimal(n2));

			if (type1 < type2)
				return -sign(n2);
			else
				return sign(n1);
		}

		switch (type1)
		{
			case BYTE:
				return ((Byte) n1).compareTo((Byte) n2);
			case SHORT:
				return ((Short) n1).compareTo((Short) n2);
			case INT:
				return ((Integer) n1).compareTo((Integer) n2);
			case LONG:
				return ((Long) n1).compareTo((Long) n2);
			case INTEGER:
				return ((BigInteger) n1).compareTo((BigInteger) n2);
			case DECIMAL:
				return ((BigDecimal) n1).compareTo((BigDecimal) n2);
			case FLOAT:
				return ((Float) n1).compareTo((Float) n2);
			case DOUBLE:
				return ((Double) n1).compareTo((Double) n2);
			default:
				throw new IllegalArgumentException("Unknown number class " + n1.getClass());
		}
	}

	public static int getTotalDigits(final Number n)
	{
		final int type = getType(n);
		final String str = n.toString();
		int totalDigitsInValue = str.length();

		if (type >= DECIMAL && str.indexOf('.') != -1)
			totalDigitsInValue -= 1;

		return totalDigitsInValue;
	}

	public static int getFractionDigits(final Number n)
	{
		final int type = getType(n);
		int fracDigitsInValue = 0;

		if (type >= DECIMAL)
		{
			final String str = n.toString();
			fracDigitsInValue = str.length() - str.indexOf(".");
		}

		return fracDigitsInValue;
	}

	public static void test(final String val, final int type, final Number test)
	{
		Number number = null;
		try
		{
			number = parse(val, type);
			if (number.equals(test) && number.getClass().equals(test.getClass()))
				System.out.println(val + " -> " + number + " (" + getTypeName(number) + ") = " + test + " (" + getTypeName(test) + ")");
			else
				System.err.println(val + " -> " + number + " (" + getTypeName(number) + ") != " + test + " (" + getTypeName(test) + ")");
		}
		catch (final RuntimeException e)
		{
			System.err.println(val + " -> " + number + " (" + getTypeName(number) + ") != " + test + " (" + getTypeName(test) + ")");
			e.printStackTrace();
		}
	}

	public static void test(final String val1, final int type1, final String val2, final int type2, final int result)
	{
		try
		{
			final Number number1 = parse(val1, type1);
			final Number number2 = parse(val2, type2);

			final int cmp = compare(number1, number2);

			if (cmp != result)
				throw new OpenError(val1 + " " + val2 + " " + cmp + " " + result);

			final String op = (cmp < 0) ? "<" : ((cmp == 0) ? "=" : ">");
			System.out.println(number1 + " (" + getTypeName(number1) + ") " + op + " " + number2 + " (" + getTypeName(number2) + ")");
		}
		catch (final RuntimeException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(final String[] args)
	{
		//        test( "0.1", DECIMAL, new BigDecimal("0.1") );
		//        test( "0.1", FLOAT, new Float(0.1) );
		//        test( "0.1", DOUBLE, new Double(0.1) );
		//        test( "1", DECIMAL, new Byte((byte)1) );
		//        test( "1", FLOAT , new Float(1));
		//        test( "1", DOUBLE, new Double(1) );
		//        test( "1.1", DECIMAL, new BigDecimal("1.1") );
		//        test( "1.1", FLOAT, new Float(1.1) );
		//        test( "1.1", DOUBLE, new Double(1.1) );
		//        test( "-00.1", DECIMAL, new BigDecimal("-0.1") );
		//        test( "-0.1", FLOAT , new Float(-0.1));
		//        test( "-00.1", DOUBLE, new Double(-0.1) );
		//        test( "-01", DECIMAL, new Byte((byte)-1) );
		//        test( "-01", FLOAT, new Float(-1) );
		//        test( "-01", DOUBLE, new Double(-1) );
		//        test( "-01.10", DECIMAL, new BigDecimal("-1.1") );
		//        test( "-01.10", FLOAT, new Float(-1.1) );
		//        test( "-01.10", DOUBLE, new Double(-1.1) );
		//        test( "-10", DECIMAL, new Byte((byte)-10) );
		//        test( "-35", DECIMAL, new Byte((byte)-35) );
		//        test( "127", DECIMAL, new Byte((byte)127) );
		//        test( "128", DECIMAL, new Short((short)128) );
		//        test( "-127", DECIMAL, new Byte((byte)-127) );
		//        test( "-128", DECIMAL, new Byte((byte)-128) );
		//        test( "-129", INTEGER, new Short((short)-129) );

		BigInteger i = new BigInteger(Long.MAX_VALUE + "");
		//        test( i.toString(), DECIMAL, new Long(Long.MAX_VALUE) );
		i = i.add(BigInteger.ONE);
		//        test( i.toString(), DECIMAL, i );
		final BigInteger TEN_INT = new BigInteger("10");
		i = i.add(TEN_INT);
		//        test( i.toString(), DECIMAL, i );
		i = i.add(i);
		test("+" + i.toString(), DECIMAL, i);

		BigDecimal d = new BigDecimal(i);
		d = d.divide(new BigDecimal("2346365"), 7, BigDecimal.ROUND_CEILING);
		test(d.toString(), DECIMAL, d);

		test("10", INTEGER, "10.1", DECIMAL, -1);
		test("120", INTEGER, "130", INTEGER, -1);
		test("10.1", DECIMAL, "130", DECIMAL, -1);
	}
}
