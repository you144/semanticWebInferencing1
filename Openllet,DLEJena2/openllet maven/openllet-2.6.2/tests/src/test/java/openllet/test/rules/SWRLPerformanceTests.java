// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.rules;

import openllet.jena.PelletInfGraph;
import openllet.jena.PelletReasonerFactory;
import openllet.test.PelletTestSuite;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.junit.Test;

public class SWRLPerformanceTests
{
	public static void main(final String[] args)
	{
		org.junit.runner.JUnitCore.main("org.mindswap.pellet.test.rules.SWRLPerformanceTests");
	}

	private final static String _base = "file:" + PelletTestSuite.base + "swrl-test/misc/";

	@Test
	public void testBasicFamily()
	{
		final String ns = "http://www.csc.liv.ac.uk/~luigi/ontologies/basicFamily#";

		final OntModel ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, null);
		ontModel.read(_base + "basicFamilyReference.owl");
		ontModel.read(_base + "basicFamilyRules.owl");
		ontModel.prepare();

		final Property uncle = ontModel.getProperty(ns + "hasUncle");
		for (final Resource ind : ontModel.listIndividuals(OWL.Thing).toList())
			System.out.println(ind.toString() + ": " + ontModel.getProperty(ind, uncle));

		((PelletInfGraph) ontModel.getGraph()).getKB().getTimers().print();

		ontModel.close();
	}

	@Test
	public void testDayCare()
	{
		final String ns = "https://mywebspace.wisc.edu/jpthielman/web/daycareontology#";

		final OntModel ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, null);
		ontModel.read(_base + "daycare.swrl.owl");
		ontModel.prepare();

		final Property exposedTo = ontModel.getProperty(ns + "is_exposed_to");

		for (final Resource ind : ontModel.listIndividuals(OWL.Thing).toList())
			System.out.println(ind.toString() + ": " + ontModel.getProperty(ind, exposedTo));

		((PelletInfGraph) ontModel.getGraph()).getKB().getTimers().print();

		ontModel.close();
	}

	@Test
	public void testProtegeFamily() throws Exception
	{
		final String ns = "http://a.com/ontology#";

		final OntModel ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, null);
		ontModel.read(_base + "family.swrl.owl");
		ontModel.prepare();

		final Property hasSibling = ontModel.getProperty(ns + "hasSibling");
		final StmtIterator iter = ontModel.listStatements((Resource) null, hasSibling, (RDFNode) null);
		while (iter.hasNext())
		{
			final Statement statement = iter.nextStatement();
			System.out.println(statement);
		}

		((PelletInfGraph) ontModel.getGraph()).getKB().getTimers().print();

		ontModel.close();
	}
}
