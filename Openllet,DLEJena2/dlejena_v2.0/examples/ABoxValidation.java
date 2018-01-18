package examples;

import dlejena.DLEJenaReasoner;
import java.io.File;
import java.net.URI;

/**
 * An example of ABox validation using DLEJena.
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class ABoxValidation {

    public static void main(String[] args) {


        /*
         * The physical URI (local path or Web address) of the ontology.
         */
        URI ontology = new File("data/aboxvalidation.owl").toURI();

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
         * Initiate the reasoning procedure. This phase involves the
         * separation of the TBox from ABox axioms (using the OWLAPI),
         * the TBox reasoning procedure (using Pellet), the generation of the
         * dynamic entailments and the ABox reasoning procedure (using
         * the forwardRete rule engine of Jena). In most cases, this
         * method should be called only once.
         */
        dle.initialize();

        /*
         * The ABox is validated only on demand in order to avoid unnecessary
         * overhead. This is done by calling the validateABox() method. The
         * TBox inconsistencies are reported by default by Pellet.
         */
        dle.validateABox();

    /*
     * In the ontology of this example, two instances are defined to be
     * same and disjoint at the same time. An exception is thrown informing
     * about this inconsistency. Note that the default operation of DLEJena
     * for ABox inconsinstencies is to throw an exception. However, DLEJena
     * can be configured to print only a message without interrupting the
     * execution. This can be done by setting:
     * DLEJenaParameters.THROW_EXCEPTION_ON_ABOX_VALIDATION = false;
     *
     * Note that TBox inconsistencies are always reported as runtime exceptions.
     */

    }
}
