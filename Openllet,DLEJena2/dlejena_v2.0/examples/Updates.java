package examples;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import dlejena.DLEJenaReasoner;

/**
 * In this example, we show how DLEJena can handle TBox and ABox updates
 * AFTER INITIALIZATION (DLEJenaReasoner::initialize() method). ABox
 * updates are treated seamlessly, since they can be handled by the
 * entailments that have been generated for the initial TBox KB. However,
 * TBox updates are tricky since the rule based for ABox reasoning
 * should be updated in order to comply with the updated TBox. Currently, DLEJena
 * does not consider any incremental ABox entailment generation procedure, and
 * therefore, the ABox entailments are generated from scratch based on the new TBox.
 * TBox updates are handled by Pellet.

 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class Updates {

    public static void main(String[] args) {

        /*
         * Create the base model for the asserted ontology axioms. Note that
         * this model should not define any reasoning infrustructure. We just want
         * a simple OntModel that will store the ontology (e.g. OWL_MEM).
         */
        OntModel base = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

        /*
         * Set basic properties
         */
        base.createOntology("http://dlejena/examples/updates.owl");
        String uri = "http://dlejena/examples/updates.owl#";
        base.setNsPrefix("", uri);

        /*
         * Define the ontology axioms. We define a class Publication
         * subclass of the Document class. Furthermore we define the
         * instance myThesis to belong to the Publication class.
         */
        System.out.println("Defining the ontology...");
        OntClass publication = base.createClass(uri + "Publication");
        System.out.println(" - Class created: " + publication.getLocalName());
        OntClass document = base.createClass(uri + "Document");
        System.out.println(" - Class created: " + document.getLocalName());
        publication.addSuperClass(document);
        System.out.println(" - Subclass relationship: " + publication.getLocalName() + " is subclass of " + document.getLocalName());
        Individual myThesis = base.createIndividual(uri + "myThesis", publication);
        System.out.println(" - Individual of " + publication.getLocalName() + " created: " + myThesis.getLocalName());
        System.out.println("done!");

        /*
         * Define an instance of the DLEJena reasoner.
         * Register the base OntModel and initialize the reasoner
         */
        System.out.println("");
        System.out.print("Initializing DLEJena...");
        DLEJenaReasoner dle = new DLEJenaReasoner();
        dle.register(base);
        dle.initialize();
        System.out.println(" done!");

        //Get the inferred TBox and ABox reference
        OntModel tbox = dle.getTBox();
        OntModel abox = dle.getABox();

        /*
         * Get the inferred types of myThesis
         */
        System.out.println("");
        System.out.println("Get the inferred types of the instance " + myThesis.getLocalName());

        //Note that the myThesis reference needs to be updated using the inferred abox model
        //Otherwise, it would refer to the base model we have created in the beginning. The
        //same holds for the other class, property and instance references too.
        myThesis = abox.getIndividual(uri + "myThesis");
        ExtendedIterator types = myThesis.listRDFTypes(false);
        while (types.hasNext()) {
            Resource type = (Resource) types.next();
            if (!type.isAnon())
                System.out.println(" - " + type.getLocalName());
        }

        /*
         * ABOX UPDATE:
         * We will update the ABox inferred model by creating one more instance
         * of the class Publication without altering the inferred TBox model.
         * Such updates are handled seamlessly by the entailment rules that have been
         * generated for the current TBox. Note that there is no need to initialize
         * the reasoner again or to prepare the abox model.
         */
        System.out.println("");
        System.out.print("ABOX UPDATE: ");
        Individual dleiswc08 = abox.createIndividual(uri + "dle-iswc08", publication);
        System.out.println(" - Individual of " + publication.getLocalName() + " created: " + dleiswc08.getLocalName());

        //Print the types of the new instance.
        System.out.println("");
        System.out.println("Get the inferred types of the instance " + dleiswc08.getLocalName());
        types = dleiswc08.listRDFTypes(false);
        while (types.hasNext()) {
            Resource type = (Resource) types.next();
            if (!type.isAnon())
                System.out.println(" - " + type.getLocalName());
        }

        /*
         * TBOX UPDATE:
         * We will update the TBox inferred model by defining the class Thesis as subclass
         * of the Publication class and the class PhDThesis as subclass of the Thesis class.
         * After a TBox update, the *updateABoxRules()* method should be called in order to generate
         * again the ABox entailment rules that correspond to
         * the updated TBox. Otherwise, the rule base would not contain a rule able to handle, for example,
         * the subclass relationship between the Publication and the Thesis classes. Note that,
         * before initializing DLEJena, the Jena API can be freely used to define the TBox and
         * ABox of the base ontology without any special precaution, as we have done in the beginning.
         * The updateABoxRules method should be used only after reasoner initialization.
         */
        System.out.println("");
        System.out.print("TBOX UPDATE: ");
        OntClass thesis = tbox.createClass(uri + "Thesis");
        System.out.println(" - Class created: " + thesis.getLocalName());
        System.out.print("TBOX UPDATE: ");
        OntClass phdthesis = tbox.createClass(uri + "PhDThesis");
        System.out.println(" - Class created: " + phdthesis.getLocalName());
        System.out.print("TBOX UPDATE: ");
        publication = tbox.getOntClass(uri + "Publication");
        thesis.addSuperClass(publication);
        System.out.println(" - Subclass relationship: " + thesis.getLocalName() + " is subclass of " + publication.getLocalName());
        System.out.print("TBOX UPDATE: ");
        phdthesis.addSuperClass(thesis);
        System.out.println(" - Subclass relationship: " + phdthesis.getLocalName() + " is subclass of " + thesis.getLocalName());

        /*
         * Print the number of the generated ABox entailments before updating the tbox
         */
        System.out.println("");
        System.out.println("ABox entailments before updating the TBox : " + dle.getABoxRules().size());

        //Update the set of the ABox entailment rules
        dle.updateABoxRules();

        /*
         * Print the number of the generated ABox entailments after tbox update.
         * We can observe that more rules are considered now.
         */
        System.out.println("ABox entailments after the TBox update: " + dle.getABoxRules().size());

        /*
         * ABOX UPDATE:
         * We will update the ABox inferred model by defining the myThesis individual to
         * belong also to the Thesis class.
         */
        System.out.println("");
        System.out.print("ABOX UPDATE: ");
        myThesis.addRDFType(phdthesis);
        System.out.println(" - Individual of " + phdthesis.getLocalName() + " created: " + myThesis.getLocalName());

        /*
         * Print the final types of the myThesis instances
         */
        System.out.println("");
        System.out.println("Get the inferred types of the instance " + myThesis.getLocalName());
        types = myThesis.listRDFTypes(false);
        while (types.hasNext()) {
            Resource type = (Resource) types.next();
            if (!type.isAnon())
                System.out.println(" - " + type.getLocalName());
        }
    }
}
