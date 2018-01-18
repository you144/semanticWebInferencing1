// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import openllet.core.utils.AlphaNumericComparator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LiebigTestSuite
{
	public static String _base = PelletTestSuite.base + "liebig-tests/";

	private static List<String> TIMEOUTS = Arrays.asList(new String[] { "Manifest1b.rdf", "Manifest2b.rdf", "Manifest10a.rdf" });

	@Parameters
	public static List<Object[]> getParameters()
	{
		final List<Object[]> parameters = new ArrayList<>();

		final WebOntTest test = new WebOntTest();
		test.setAvoidFailTests(true);
		test.setBase("http://www.informatik.uni-ulm.de/ki/Liebig/reasoner-eval/", "file:" + _base);
		test.setShowStats(WebOntTest.NO_STATS);

		final File testDir = new File(_base);

		final File[] files = testDir.listFiles((FileFilter) file -> file.getName().indexOf("Manifest") != -1);

		Arrays.sort(files, AlphaNumericComparator.CASE_INSENSITIVE);

		for (int j = 0; j < files.length; j++)
			if (!TIMEOUTS.contains(files[j].getName()))
				parameters.add(new Object[] { new WebOntTestCase(test, files[j], "liebig-" + files[j].getName()) });

		return parameters;
	}

	private final WebOntTestCase _test;

	public LiebigTestSuite(final WebOntTestCase test)
	{
		this._test = test;
	}

	@Test
	public void run() throws IOException
	{
		_test.runTest();
	}

}
