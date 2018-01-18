// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.classification;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import openllet.jena.PelletReasonerFactory;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.ReasonerVocabulary;

public class JenaClassificationTest extends AbstractClassificationTest
{
	@Override
	public void testClassification(final String inputOnt, final String classifiedOnt)
	{
		final OntModel premise = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		premise.read(inputOnt);
		premise.prepare();

		final Model conclusion = ModelFactory.createDefaultModel();
		conclusion.read(classifiedOnt);

		final StmtIterator stmtIter = conclusion.listStatements();

		final List<String> nonEntailments = new ArrayList<>();
		while (stmtIter.hasNext())
		{
			final Statement stmt = stmtIter.nextStatement();

			boolean entailed = true;
			if (stmt.getPredicate().equals(RDFS.subClassOf))
				entailed = premise.contains(stmt.getSubject(), ReasonerVocabulary.directSubClassOf, stmt.getObject());
			else
				if (stmt.getPredicate().equals(OWL.equivalentClass))
					entailed = premise.contains(stmt);

			if (!entailed)
				if (AbstractClassificationTest.FAIL_AT_FIRST_ERROR)
					fail("Not entailed: " + format(stmt));
				else
				{
					//String x = format(stmt);
					//if (!"[MaterialProperties,subClassOf,CostDriver]".equals(x))
					nonEntailments.add(format(stmt));
					final Individual i = premise.getIndividual(stmt.getSubject().asResource().getURI());
					System.out.println(i);
					System.out.println(stmt.getPredicate());
				}
		}

		assertTrue(nonEntailments.toString(), nonEntailments.isEmpty());
	}

	private static String format(final Statement stmt)
	{
		try
		{
			final StringBuilder sb = new StringBuilder();
			sb.append('[');
			sb.append(stmt.getSubject().getLocalName());
			sb.append(',');
			sb.append(stmt.getPredicate().getLocalName());
			sb.append(',');
			sb.append(stmt.getResource().getLocalName());
			sb.append(']');

			return sb.toString();
		}
		catch (final Exception e)
		{
			e.printStackTrace();

			return stmt.toString();
		}
	}

}
