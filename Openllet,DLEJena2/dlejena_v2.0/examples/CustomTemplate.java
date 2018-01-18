package examples;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import dlejena.DLEJenaReasoner;
import java.io.File;
import java.net.URI;
import java.util.Collections;

/**
 * This example shows how custom template rules can be added in DLEJena in order to
 * define more complex ABox entailments beyond OWL2 RL.
 *
 * The template rules are rules that dynamically generate the ABox inferencing rules.
 * These rules are executed over the inferred TBox model (PelletInfGraph). Therefore, the ABox rule
 * base of DLEJena is dynamically defined, according to the TBox model.
 *
 * There are two ways of adding template rules in DLEJena. The first is to add directly
 * the custom template rules in the templates.rules file that exists inside dlejena.jar.
 * The second approach is to add them procedurally through the DLEJena API. The present example
 * follows the second approach.
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class CustomTemplate {

    public static void main(String[] args) {


        /*
         * The physical URI (local path or Web address) of the ontology.
         */
        URI ontology = new File("data/parent.owl").toURI();

        /*
         * Hold the ontology logical URI needed in queries
         */
        String uri = "http://dlejena/examples/customTemplateRules.owl#";

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
         * Register the min cardinality template rule.
         * Note that this procedure should take place prior to
         * DLEJena initializaion.
         */
        dle.registerTemplate(Collections.singletonList(minCardinalityTemplate()));

        /*
         * Initiate the reasoner. This phase involves the
         * separation of the TBox from ABox axioms (using the OWLAPI),
         * the TBox reasoning procedure (using Pellet), the generation of the
         * dynamic entailments and the ABox reasoning procedure (using
         * the forwardRete rule engine of Jena).
         */
        dle.initialize();

        /*
         * The parent.owl ontology defines a class Parent as the equivalent
         * class to the class of all the instances that have at least
         * one value for the property hasChild (minCardinality 1). The restrictions
         * imposed by the OWL2 RL Profile are designed so as to avoid the need to
         * infer the existence of individuals not explicitly present in the KB.
         * By adding this custom template rule, we are able to infer anonymous
         * individuals for min cardinality restrictions, in cases where such
         * instances do not exist, as it happens in the example ontology. The ontology
         * defines an instance of the class Parent without a value in the hasChild
         * property and an anonymous instance is generated. DLEJena can
         * be extended with more template rules, according to the requirements.
         */

        System.out.println("");
        System.out.println("The hasChild values of the nick instance: ");
        Individual nick = dle.getABox().getIndividual(uri + "nick");
        OntProperty hasChild = dle.getTBox().getOntProperty(uri + "hasChild");
        NodeIterator values = nick.listPropertyValues(hasChild);
        while(values.hasNext()){
            Individual v = (Individual) values.nextNode().as(Individual.class);
            if(v.isAnon()){
                System.out.println(" - The anonymous instance (BNode) " + v.toString() + " of types " + v.listRDFTypes(false).toList());
            }
        }
    }

    /**
     * This method defines the template rule for the following generic entailment:
     * [min:
     *   (?R owl:minCardinality 1),
     *   (?R owl:onProperty ?P),
     *   (?X rdf:type ?R),
     *   noValue(?X ?P), makeTemp(?T)
     *   -> (?X ?P ?T)]
     *
     * Note that the triples that refer to TBox information should exist in the
     * body of the template rule.
     *
     * @return The parsed rule
     */
    public static Rule minCardinalityTemplate() {
        String rule = "[min: (?R owl:minCardinality 1),	" +
                            "(?R owl:onProperty ?P) " +
                            "-> [D_min:(?X rdf:type ?R), noValue(?X ?P ), makeTemp(?T) -> (?X ?P ?T)]]";
        return Rule.parseRule(rule);

    }
}
