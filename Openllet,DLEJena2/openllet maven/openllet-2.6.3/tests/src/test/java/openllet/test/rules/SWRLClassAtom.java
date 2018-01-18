// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.rules;

import junit.framework.JUnit4TestAdapter;
import openllet.test.PelletTestSuite;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>
 * Title: SWRLClassAtom
 * </p>
 * <p>
 * Description: Performs SWRL class atom tests
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Markus Stocker
 */
public class SWRLClassAtom extends SWRLAbstract
{

	public static junit.framework.Test suite()
	{
		return new JUnit4TestAdapter(SWRLClassAtom.class);
	}

	@BeforeClass
	public static void setUp()
	{
		_base = "file:" + PelletTestSuite.base + "swrl-classAtom/";
	}

	@Test
	public void complex()
	{
		test("complex");
	}
}
