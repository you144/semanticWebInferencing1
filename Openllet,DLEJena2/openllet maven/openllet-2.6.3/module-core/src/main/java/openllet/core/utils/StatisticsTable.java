// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import openllet.shared.tools.Log;

public class StatisticsTable<ROW, COL>
{

	private static final Logger _logger = Log.getLogger(StatisticsTable.class);

	private final Map<COL, Map<ROW, Number>> _statistics = new HashMap<>();

	private final List<COL> _cols = new ArrayList<>();
	private final List<ROW> _rows = new ArrayList<>();

	private int _firstColumnSize = 10;

	public void add(final ROW row, final COL col, final Number stat)
	{
		Map<ROW, Number> getCol = _statistics.get(col);

		if (getCol == null)
		{
			getCol = new HashMap<>();
			_statistics.put(col, getCol);
			_cols.add(col);
		}

		final Number getStat = getCol.get(row);

		if (getStat != null)
			_logger.warning("Overwriting [" + row + " : " + col + "].");
		else
			if (!_rows.contains(row))
			{
				if (_firstColumnSize < row.toString().length())
					_firstColumnSize = row.toString().length();
				_rows.add(row);
			}

		getCol.put(row, stat);
	}

	public void add(final COL col, final Map<ROW, ? extends Number> stat)
	{
		for (final Entry<ROW, ? extends Number> entry : stat.entrySet())
			add(entry.getKey(), col, entry.getValue());
	}

	@Override
	public String toString()
	{
		String s = "";
		final List<Integer> colSizes = new ArrayList<>();

		for (final COL col : _cols)
			colSizes.add(col.toString().length() + 2);

		// format of first column
		final String firstCol = "| %1$-" + (_firstColumnSize + 2) + "s ";

		// format of one line
		final StringBuffer lineFormat = new StringBuffer();

		for (int i = 1; i < colSizes.size() + 1; i++)
			lineFormat.append("| %").append(i).append("$-10.10s ");

		lineFormat.append("|\n");

		// separator
		final char[] a = new char[String.format(lineFormat.toString(), _cols.toArray()).length() + String.format(firstCol, "").length()];

		Arrays.fill(a, '=');
		final String separator = new String(a);

		s += separator + "\n";
		s += String.format(firstCol, "") + String.format(lineFormat.toString(), _cols.toArray());
		s += separator + "\n";

		for (final ROW row : _rows)
		{
			final List<Number> rowData = new ArrayList<>();
			for (final COL col : _cols)
			{
				final Map<ROW, Number> map = _statistics.get(col);
				final Number stat = map.get(row);

				if (stat == null)
					rowData.add(Double.POSITIVE_INFINITY);
				else
					rowData.add(stat);
			}

			String rowName;

			// // TODO
			// try {
			// rowName = URI.create(row.toString()).getFragment();
			// } catch (Exception e) {
			rowName = row.toString();
			// }

			s += String.format(firstCol, new Object[] { rowName }) + String.format(lineFormat.toString(), rowData.toArray());
		}

		s += separator + "\n";

		return s;
	}
}
