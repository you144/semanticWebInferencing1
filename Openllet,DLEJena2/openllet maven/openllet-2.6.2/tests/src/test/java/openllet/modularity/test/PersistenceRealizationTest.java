// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.modularity.test;

import static openllet.modularity.test.TestUtils.assertInstancesEquals;
import static openllet.modularity.test.TestUtils.assertTypesEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import openllet.modularity.AxiomBasedModuleExtractor;
import openllet.modularity.IncrementalClassifier;
import openllet.modularity.ModuleExtractor;
import openllet.modularity.io.IncrementalClassifierPersistence;
import openllet.owlapi.OWL;
import openllet.owlapi.OntologyUtils;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.test.PelletTestSuite;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Blazej Bulka
 */
public class PersistenceRealizationTest
{
	public static final String base = PelletTestSuite.base + "modularity/";

	private static final String TEST_FILE = "test-persistence-realization.zip";

	public ModuleExtractor createModuleExtractor()
	{
		return new AxiomBasedModuleExtractor();
	}

	public void testFile(final String fileName) throws IOException
	{
		final String common = "file:" + base + fileName;
		testRealization(OWLManager.createConcurrentOWLOntologyManager(), common + ".owl");
	}

	public void testRealization(final OWLOntologyManager manager, final String inputOnt) throws IOException
	{
		final File testFile = new File(TEST_FILE);
		final OWLOntology ontology = OntologyUtils.loadOntology(manager, inputOnt);

		try
		{
			final OpenlletReasoner unified = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
			final ModuleExtractor moduleExtractor = createModuleExtractor();

			final IncrementalClassifier modular = new IncrementalClassifier(unified, moduleExtractor);
			modular.classify();

			// first we only persist classified-but-not-realized classifier
			assertFalse(modular.isRealized());

			try (FileOutputStream fos = new FileOutputStream(testFile))
			{
				IncrementalClassifierPersistence.save(modular, fos);
			}

			final IncrementalClassifier modular2;
			try (FileInputStream fis = new FileInputStream(testFile))
			{
				modular2 = IncrementalClassifierPersistence.load(fis);
			}
			assertTrue(testFile.delete());

			// the classifier read from file should NOT be realized at this point
			assertFalse(modular.isRealized());

			assertInstancesEquals(unified, modular2);
			assertTypesEquals(unified, modular2);

			// the previous tests should have triggered realization
			assertTrue(modular2.isRealized());

			// save the classifier again and read it back
			try (final FileOutputStream fos = new FileOutputStream(testFile))
			{
				IncrementalClassifierPersistence.save(modular2, fos);
			}

			final IncrementalClassifier modular3;
			try (final FileInputStream fis = new FileInputStream(testFile))
			{
				modular3 = IncrementalClassifierPersistence.load(fis);
			}
			assertTrue(testFile.delete());

			// the classifier read from file should be realized at this point
			assertTrue(modular3.isRealized());

			assertInstancesEquals(unified, modular3);
			assertTypesEquals(unified, modular3);

			unified.dispose();
			modular.dispose();
			modular2.dispose();
			modular3.dispose();
		}
		finally
		{
			OWL._manager.removeOntology(ontology);
		}
	}

	@Test
	public void koalaPersistenceRealizationTest() throws IOException
	{
		testFile("koala");
	}

	@Test
	public void sumoPersistenceRealizationTest() throws IOException
	{
		testFile("SUMO");
	}

	@Test
	public void sweetPersistenceRealizationTest() throws IOException
	{
		testFile("SWEET");
	}
}
