// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.

package openllet.test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import openllet.core.utils.AlphaNumericComparator;
import openllet.core.utils.Comparators;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class WebOntTestSuite
{
	public static String _base = PelletTestSuite.base + "owl-test/";

	@Parameters(name = "{index}: {0}")
	public static List<Object[]> getParameters()
	{
		final List<Object[]> parameters = new ArrayList<>();

		final WebOntTest test = new WebOntTest();
		test.setAvoidFailTests(true);
		test.setBase("file:" + _base);
		test.setShowStats(WebOntTest.NO_STATS);

		final File testDir = new File(_base);
		final File[] dirs = testDir.listFiles();

		Arrays.sort(dirs, Comparators.stringComparator);

		for (final File dir : dirs)
		{
			if (dir.isFile())
				continue;

			final File[] files = dir.listFiles((FileFilter) file -> file.getName().indexOf("Manifest") != -1);

			Arrays.sort(files, AlphaNumericComparator.CASE_INSENSITIVE);

			for (final File file : files)
				parameters.add(new Object[] { new WebOntTestCase(test, file, dir.getName() + "-" + file.getName()) });

		}

		return parameters;
	}

	private final WebOntTestCase _test;

	public WebOntTestSuite(final WebOntTestCase test)
	{
		this._test = test;
	}

	@Test
	public void run() throws IOException
	{
		_test.runTest();
	}

}
