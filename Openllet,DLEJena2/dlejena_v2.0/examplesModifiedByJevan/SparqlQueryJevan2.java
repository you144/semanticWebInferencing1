package examplesModifiedByJevan;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.QuerySolution; /* Jevan 2017sep */
import com.hp.hpl.jena.rdf.model.Resource;
import dlejena.DLEJenaParameters;
import dlejena.DLEJenaReasoner;
import java.io.File;
import java.net.URI;

/**
 * This is a simple example of defining a SPARQL query using the
 * Jena API over the ABox of DLEJena. The same approach can be
 * used on the TBox.
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class SparqlQueryJevan2 {

    public static void main(String[] args) {


        /*
         * The physical URI (local path or Web address) of the ontology.
         * This ontology has been obtained from the Web site of KAON2:
         * http://kaon2.semanticweb.org/download/test_ontologies.zip
         */
        URI ontology = new File("dataFromJevan/relatives.owl").toURI(); /* Jevan 2017sep */

        /*
         * Define an instance of the DLEJenaReasoner class.
         * This class provides the necessary nethods for
         * using the DLEJena library
         */
        DLEJenaReasoner dle = new DLEJenaReasoner();

        /*
         * Register the ontology to the reasoner.
         */
        dle.register(ontology);

        /*
         * Initiate the reasoner. This phase involves the
         * separation of the TBox from ABox axioms (using the OWLAPI),
         * the TBox reasoning procedure (using Pellet), the generation of the
         * dynamic entailments and the ABox reasoning procedure (using
         * the forwardRete rule engine of Jena).
         */
        dle.initialize();

        /*
         * Obtain a reference to the ABox inferred model.
         */
        OntModel abox = dle.getABox();

        /*
         * Define the SPARQL query, initialize the SPARQL engine and
         * execute the query over the abox model.
         */
        String queryString = "PREFIX : <http://example.org/relatives#> " +
                "SELECT ?gp " +
                "WHERE {" +
                "?gc :hasGrandparent ?gp . " +
                "?gc a :Person . }";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, abox);

        System.out.println("");
        System.out.println("Results:"); /* Jevan 2017sep */
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            /* https://jena.apache.org/documentation/query/app_api.html */
            QuerySolution soln = results.nextSolution() ; /* Jevan 2017sep */
            Resource s = (Resource) soln.get("?gp").as(Resource.class); /* Jevan 2017sep */
            System.out.println(" - " + s.getLocalName());
        }
        qexec.close(); /* Jevan 2017sep */
    }
}
