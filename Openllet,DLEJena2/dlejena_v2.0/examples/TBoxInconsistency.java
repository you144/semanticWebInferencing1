package examples;

import dlejena.DLEJenaReasoner;
import java.io.File;
import java.net.URI;

/**
 * This example shows the behavior of DLEJena on TBox
 * inconsistencies.
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class TBoxInconsistency {

    public static void main(String[] args) {


        /*
         * The physical URI (local path or Web address) of the ontology.
         */
        URI ontology = new File("data/unsatisfiableTbox.owl").toURI();

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
     * A Runtime exception is thrown informing about the unsatisfiable concept.
     * In this example, the ontology defines the class Man to be a subclass and
     * disjoint class to the class Human. Therefore, the Man concept is unsatisfiable.
     */

    }
}
