
package examples;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.RDFNode;
import dlejena.DLEJenaReasoner;
import java.io.File;
import java.net.URI;

/**
 * A simple example that demonstrates the semantics of property chains
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class PropertyChain {

    public static void main(String[] args) {

        /*
         * The physical URI (local path or Web address) of the ontology.
         */
        URI ontology = new File("data/propertyChain.owl").toURI();

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

        /**
         * Print the ABox entailment rules. Note the entailment that handles
         * the property chain isPartOf o isLocatedIn -> iPartOf:
         * [ D_prp-spo2: (?u1 http://propertyChain.owl#isPartOf ?u2) (?u2 http://propertyChain.owl#isLocatedIn ?u3) -> (?u1 http://propertyChain.owl#isPartOf ?u3) ]
         */
        System.out.println("ABox entailment rules:");
        dlejena.utils.Print.printRuleSet(dle.getABoxRules());

        /* The asserted ABox knowledge contains that
         * <a1 isPartOf a2>,
         * <a2 isLocatedIn a3>.
         *
         * Print the inferred information that finally <a1 isPartOf a3>
         */
        System.out.println("");
        Individual a1 = dle.getABox().getIndividual("http://propertyChain.owl#a1");
        OntProperty isPartOf = dle.getTBox().getOntProperty("http://propertyChain.owl#isPartOf");
        Individual v = (Individual) a1.getPropertyValue(isPartOf).as(Individual.class);
        System.out.println(a1.getLocalName() + " " + isPartOf.getLocalName() + " " + v.getLocalName());
    }
}
