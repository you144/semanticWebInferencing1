// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.modularity.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import openllet.atom.OpenError;
import openllet.owlapi.OWL;
import openllet.owlapi.OntologyUtils;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description: Test modular classification for correctness against unified classification
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Mike Smith
 */
public abstract class RandomizedIncrementalClassifierTest extends AbstractModularityTest
{
	private String _path;

	public RandomizedIncrementalClassifierTest(final String path)
	{
		_path = path;

		if (!new File(path).exists())
		{
			_path = "src/test/resources/" + path;

			if (!new File(_path).exists())//
				throw new OpenError("Path to _data files is not correct: " + path);
		}
	}

	private void classifyCorrectnessTest(final String file)
	{
		final int n = 5;
		final OWLOntology loadedOntology = OntologyUtils.loadOntology(_manager, "file:" + file, false);

		final List<OWLAxiom> axioms = new ArrayList<>(TestUtils.selectRandomAxioms(loadedOntology, n * 2));
		final int size = axioms.size();

		// Delete 5 axioms before the test
		loadedOntology.remove(axioms.subList(0, n));

		// Update test will add n axioms and remove n axioms
		final List<OWLAxiom> additions = axioms.subList(0, n);
		final List<OWLAxiom> deletions = axioms.subList(n, n * 2);
		try
		{
			TestUtils.runUpdateTest(loadedOntology, _modExtractor, additions, deletions);
		}
		catch (final AssertionError | RuntimeException ex)
		{
			System.err.println("Additions: " + additions);
			System.err.println("Deletions: " + deletions);
			System.err.println("#axioms:" + size + " " + loadedOntology.getAxiomCount());
			System.err.println("ex message : " + ex.getMessage());
			throw ex;
		}
		finally
		{
			OWL._manager.removeOntology(loadedOntology);
		}
	}

	@Test
	public void galenRandomizedIncrementalClassifyTest()
	{
		classifyCorrectnessTest(_path + "galen.owl");
	}

	@Test
	public void koalaRandomizedIncrementalClassifyTest()
	{
		classifyCorrectnessTest(_path + "koala.owl");
	}

	@Test
	public void sumoRandomizedIncrementalClassifyTest()
	{
		classifyCorrectnessTest(_path + "SUMO.owl");
	}

	@Test
	public void sweetRandomizedIncrementalClassifyTest()
	{
		classifyCorrectnessTest(_path + "SWEET.owl");
	}
}
