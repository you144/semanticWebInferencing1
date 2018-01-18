package examples;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.io.File;
import java.util.List;
import opensource.dlejena.DLEJenaParameters;
import opensource.dlejena.DLEJenaReasoner;
import opensource.dlejena.utils.Print;
import org.semanticweb.owlapi.model.IRI;

/**
 * This is a simple example of loading an ontology in DLEJena. We exemplify also
 * on the way we can retrieve the rule base of the ABox rule reasoner
 * (forwardRete) and on some trivial TBox and ABox queries using the Jena API
 * over the inferred models.
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class LoadOntology {

    public static void main(String[] args) {

        /*
         * Enable some basic messages in the standard output.
         */
        DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT = true;

        /*
         * The physical URI (local path or Web address) of the ontology.
         */
        IRI ontology = IRI.create(new File("src/test/java/examples/man.owl").toURI());
        //or something like URI ontology = URI.create("http://127.0.0.1/man.owl");

        /*
         * Hold the ontology logical URI needed in queries
         */
        String uri = "http://dlejena/examples/man.owl#";

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
         * Validate the ABox model based against a set of validation rules
         * that are dynamically generated. Note that
         * any TBox incosistency is determined by Pellet.
         */
        dle.validateABox();

        /*
         * Obtain references to the TBox and ABox inferred models.
         */
        OntModel tbox = dle.getTBox();
        OntModel abox = dle.getABox();


        /*
         * Retrieve and print the set of the rules that have been used
         * for ABox reasoning.
         * The rules that are returned by this method are not
         * always the same: it depends on the semantics of the loaded ontologies.
         * The rule name of every dynamic rule starts with D_.
         */
        List<Rule> aboxRules = dle.getABoxRules();
        System.out.println("");
        System.out.println("-------------------------");
        System.out.println("The ABox entailment rules");
        System.out.println("-------------------------");
        Print.printRuleSet(aboxRules);

        /*
         * All the TBox-related queries should use the TBox inferred model (tbox)
         */
        System.out.println("");
        System.out.println("The named superclasses of the class Man:");
        OntClass man = tbox.getOntClass(uri + "Man");
        ExtendedIterator superclasses = man.listSuperClasses();
        while (superclasses.hasNext()) {
            OntClass c = (OntClass) superclasses.next();
            if (!c.isAnon()) {
                System.out.println(" - " + c.getURI());
            }
        }

        /*
         * Get the types of the instance gmeditsk. This query should use the ABox
         * inferred model of the dle instance, since it is an ABox-related query
         */
        Individual gmeditsk = abox.getIndividual(uri + "gmeditsk");
        System.out.println("");
        System.out.println("All the named classes where gmeditsk belongs to:");

        /*
         * The listRDFTypes method should be used instead of the listOntClasses
         * method in order to retrieve the types of an instance, treating the
         * results as Resources. Otherwise, an exception will be thrown, since
         * Jena would not be able to view (cast) any restriction class as an OntClass
         * (this information exists in the TBox model).
         */
        ExtendedIterator types = gmeditsk.listRDFTypes(false);
        while (types.hasNext()) {
            Resource type = (Resource) types.next();
            if (!type.isAnon()) {
                System.out.println(" - " + type.getURI());
            }
        }

        /*
         * Retrieve the value of the property hasSex of the gmeditsk instance
         */
        System.out.println("");
        System.out.println("gmeditsk has sex: ");
        OntProperty hasSex = tbox.getOntProperty(uri + "hasSex");
        NodeIterator values = gmeditsk.listPropertyValues(hasSex);
        while (values.hasNext()) {
            RDFNode value = (RDFNode) values.next();
            System.out.println(" - " + value.toString());
        }
    }
}
