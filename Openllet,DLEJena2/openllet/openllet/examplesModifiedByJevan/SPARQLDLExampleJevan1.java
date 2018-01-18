// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.examplesModifiedByJevan; // Jevan 2018jan13

import openllet.jena.PelletReasonerFactory;
import openllet.query.sparqldl.jena.SparqlDLExecutionFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * <p>
 * Title: SPARQLDLExample
 * </p>
 * <p>
 * Description: This program shows how to use the Pellet SPARQL-DL engine
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
public class SPARQLDLExampleJevan1 // Jevan 2018jan13
{

	// The ontology loaded as dataset
	// Jevan 2018jan13
	private static final String ontology = "file:dataFromJevan/relatives.owl";
	private static final String[] queries = new String[] {
			"file:dataFromJevan/relatives-sparql-dl-1.sparql",
			"file:dataFromJevan/relatives-sparql-dl-2.sparql" };

	public void run()
	{
		for (final String query : queries)
		{
			// First create a Jena ontology model backed by the Pellet reasoner
			// (note, the Pellet reasoner is required)
			final OntModel m = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);

			// Then read the _data from the file into the ontology model
			m.read(ontology);

			// Now read the query file into a query object
			final Query q = QueryFactory.read(query);

			// Create a SPARQL-DL query execution for the given query and
			// ontology model
			final QueryExecution qe = SparqlDLExecutionFactory.create(q, m);

			// We want to execute a SELECT query, do it, and return the result set
			final ResultSet rs = qe.execSelect();

			// Print the query for better understanding
			System.out.println(q.toString());

			// There are different things we can do with the result set, for
			// instance iterate over it and process the query solutions or, what we
			// do here, just print out the results
			ResultSetFormatter.out(rs);

			// And an empty line to make it pretty
			System.out.println();
		}
	}

	public static void main(final String[] args)
	{
		final SPARQLDLExampleJevan1 app = new SPARQLDLExampleJevan1(); // Jevan 2018jan13
		app.run();
	}

}
