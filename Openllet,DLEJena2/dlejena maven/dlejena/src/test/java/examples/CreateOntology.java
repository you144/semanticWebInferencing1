
package examples;

import com.hp.hpl.jena.ontology.HasValueRestriction;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import opensource.dlejena.DLEJenaReasoner;

/**
 * This example shows how the Jena API can be used in order to
 * create an ontology and to load it in DLEJena, in contrast to the
 * LoadOntology.java example where the ontology is loaded from a file.
 * DLEJena can fully exploit the well-known Jena API methods.
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class CreateOntology {

    public static void main(String[] args) {

        /*
         * Create the base model for the asserted ontology axioms. Note that
         * this model should not define any reasoning infrustructure. We just want
         * a simple OntModel that will store the ontology (e.g. OWL_MEM).
         */
        OntModel base = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

        /*
         * Set basic ontology properties
         */
        base.createOntology("http://dlejena/examples/createOntology.owl");
        String uri = "http://dlejena/examples/createOntology.owl#";
        base.setNsPrefix("", uri);

        /*
         * Define the ontology axioms. We create an ontology that defines the
         * class Man to be equivalent to the intersection of the class Human and
         * of the class of all instances that have the value 'male' in the 'hasSex'
         * property (restriction class). Furthermore, we define the class Sex
         * as the enumeration of the instances 'male' and 'female'.
         */
        OntClass man = base.createClass(uri + "Man");
        OntClass human = base.createClass(uri + "Human");
        Individual male = base.createIndividual(uri + "male", OWL.Thing);
        Individual female = base.createIndividual(uri + "female", OWL.Thing);
        RDFList enums = base.createList(); enums = enums.cons(male); enums = enums.cons(female);
        OntClass sex = base.createEnumeratedClass(uri + "Sex", enums);
        OntProperty hasSex = base.createObjectProperty(uri + "hasSex");
        HasValueRestriction hasValue = base.createHasValueRestriction(null, hasSex, male);
        RDFList inters = base.createList(); inters = inters.cons(human); inters = inters.cons(hasValue);
        man.addEquivalentClass(base.createIntersectionClass(null, inters));
        Individual gmeditsk = base.createIndividual(uri + "gmeditsk", OWL.Thing);
        gmeditsk.addProperty(hasSex, male);
        gmeditsk.addRDFType(human);

        /*
         * Define an instance of the DLEJena reasoner.
         * Register the base OntModel and initialize the reasoner
         */
        DLEJenaReasoner dle = new DLEJenaReasoner();
        dle.register(base);
        dle.initialize();

        /*
         * Get references to the ABox and TBox models
         */
        OntModel tbox = dle.getTBox();
        OntModel abox = dle.getABox();

        /*
         * All the TBox-related queries should use the TBox inferred model
         */
        System.out.println("");
        System.out.println("The named superclasses of the class Man:");

        //Caution: The refernce to the Man OntClass should be updated in order to refer to
        //the inferred tbox (and not to the base model). The same holds for the other resources as well.
        man = tbox.getOntClass(uri + "Man");
        ExtendedIterator superclasses = man.listSuperClasses();
        while (superclasses.hasNext()) {
            OntClass c = (OntClass) superclasses.next();
            if (!c.isAnon())
                System.out.println(" - " + c.getURI());
        }

        /*
         * Get the inferred types of the instance gmeditsk. This query should use the ABox
         * inferred model of the dle reasoner, since it is an ABox-related query
         */
        System.out.println("");
        System.out.println("All the named classes where gmeditsk belongs to:");

        /*
         * The listRDFTypes method should be used instead of the listOntClasses
         * method in order to retrieve the types of an instance, treating the
         * results as Resources. Otherwise, an exception will be thrown, since
         * Jena would not be able to view (cast) any restriction class as an OntClass
         * (this information exists in the TBox model).
         */
        gmeditsk = abox.getIndividual(uri + "gmeditsk");
        ExtendedIterator types = gmeditsk.listRDFTypes(false);
        while (types.hasNext()) {
            Resource type = (Resource) types.next();
            if (!type.isAnon())
                System.out.println(" - " + type.getURI());
        }

        /*
         * Retrieve the values of the property hasSex of the gmeditsk instance
         */
        hasSex = tbox.getOntProperty(uri + "hasSex");
        System.out.println("");
        System.out.println("gmeditsk has sex: ");
        NodeIterator values = gmeditsk.listPropertyValues(hasSex);
        while (values.hasNext()) {
            RDFNode value = (RDFNode) values.next();
            System.out.println(" - " + value.toString());
        }
    }
}
