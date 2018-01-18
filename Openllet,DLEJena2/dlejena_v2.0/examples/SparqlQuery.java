package examples;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
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
public class SparqlQuery {

    public static void main(String[] args) {


        /*
         * The physical URI (local path or Web address) of the ontology.
         * This ontology has been obtained from the Web site of KAON2:
         * http://kaon2.semanticweb.org/download/test_ontologies.zip
         */
        URI ontology = new File("data/wine_0.owl").toURI();

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
        String queryString = "PREFIX wine: <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                "SELECT ?i " +
                "WHERE {" +
                "?i rdf:type wine:Wine. " +
                "?i wine:hasFlavor wine:Strong .}";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, abox);

        System.out.println("");
        System.out.println("Wines with strong flavor:");
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            Resource s = (Resource) results.nextSolution().get("?i").as(Resource.class);
            System.out.println(" - " + s.getLocalName());
        }
    }
}
