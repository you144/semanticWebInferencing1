// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.utils;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import openllet.aterm.ATerm;

public class Comparators
{
	public static final Comparator<Comparable<Object>> comparator = (o1, o2) -> o1.compareTo(o2);

	public static final Comparator<ATerm> termComparator = (o1, o2) ->
	{
		final int h1 = o1.hashCode();
		final int h2 = o2.hashCode();

		if (h1 < h2)
			return -1;
		else
			if (h1 > h2)
				return 1;
			else
				if (o1 == o2)
					// openllet.aterm equality is identity equality due to maximal structure
					// sharing
					return 0;
				else
					// ATerm.toString is very inefficient but hashcodes of ATerms
					// clash very infrequently. The need to compare two different
					// terms with same hascode isn't very common either. String
					// comparison gives us a stable ordering over different runs
					return o1.toString().compareTo(o2.toString());
	};

	public static final Comparator<Number> numberComparator = (n1, n2) -> NumberUtils.compare(n1, n2);

	public static final Comparator<Object> stringComparator = (o1, o2) -> o1.toString().compareTo(o2.toString());

	public static final Comparator<Calendar> calendarComparator = (c1, c2) ->
	{
		final long t1 = c1.getTimeInMillis();
		final long t2 = c2.getTimeInMillis();

		if (t1 < t2)
			return -1;
		else
			if (t1 == t2)
				return 0;
			else
				return 1;
	};

	public static <T> Comparator<T> reverse(final Comparator<T> cmp)
	{
		return Collections.reverseOrder(cmp);
	}
}
