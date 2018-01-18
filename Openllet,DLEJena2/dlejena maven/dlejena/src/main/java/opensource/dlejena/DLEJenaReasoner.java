package opensource.dlejena;

import aterm.ATermAppl;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.reasoner.rulesys.FBRuleInfGraph;
import com.hp.hpl.jena.reasoner.rulesys.FBRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import opensource.dlejena.dynamicentailments.TemplateProcessor;
import opensource.dlejena.dynamicentailments.impl.D_Intersection;
import opensource.dlejena.dynamicentailments.impl.D_PropertyChain;
import opensource.dlejena.exceptions.ABoxValidationException;
import opensource.dlejena.exceptions.InconsistentTBoxException;
import opensource.dlejena.exceptions.NoAttachedOntologiesException;
import opensource.dlejena.exceptions.UpdateTBoxException;
import opensource.dlejena.utils.OntologyManager;
import opensource.dlejena.utils.Print;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.semanticweb.owlapi.model.IRI;

/**
 * This is the main class of DLEJena that implements the reasoning
 * infrastructure. The reasoning is performed both by Pellet (TBox-part) and by
 * entailment rules (ABox-part) using the forwardRete Jena rule engine. The
 * entailment rules are generated dynamically, based on the Pellet TBox
 * inferencing capabilities. The example folder of the DLEJena distribution
 * contains several code examples showing how to create an instance of this
 * class and how to use it.
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class DLEJenaReasoner {

    private final OntModel aboxBaseOntModel;
    private final OntModel tboxBaseOntModel;
    private final OntModel tboxInfOntModel;
    private final OntModel aboxInfOntModel;
    private final FBRuleReasoner aboxReasoner;
    private Vector<IRI> ontologies;
    private String TEMPLATE_RULES_FILE;
    //private String EXCEPTIONAL_RULES_FILE;
    //private Vector<Rule> customExceptionalRules;
    private Vector<Rule> customTemplateRules;
    private boolean initialized = false;
    private final TemplateProcessor gp;

    /**
     * The constructor
     */
    public DLEJenaReasoner() {

        TEMPLATE_RULES_FILE = "/rules/templates.rules";
        //EXCEPTIONAL_RULES_FILE = "/rules/exceptional.rules";

        ontologies = new Vector<IRI>();
        //customExceptionalRules = new Vector<Rule>();
        customTemplateRules = new Vector<Rule>();

        aboxReasoner = new FBRuleReasoner(new Vector<Rule>());
        OntModelSpec foms2 = new OntModelSpec(OntModelSpec.OWL_MEM);
        foms2.setReasoner(aboxReasoner);

        aboxBaseOntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        tboxBaseOntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

        aboxInfOntModel = ModelFactory.createOntologyModel(foms2, aboxBaseOntModel);
        tboxInfOntModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, tboxBaseOntModel);

        gp = new TemplateProcessor(tboxInfOntModel);

        registerBuiltins();
    }

    /**
     * Use this method to register an ontology in the DLEJena reasoner.
     *
     * @param ontology The URI of the ontology (local path or Web address). The
     * set of the attached ontologies is handled by the OWLAPI, which is
     * responsible for loading also any imported ontology. Note that the
     * ontologies should be registered <b>before</b> initializing the
     * DLEJenaReasoner.
     */
    public void register(IRI ontology) {
        if (initialized) {
            String message = "Currently, DLEJena supports only the ontology updates that are made \n"
                    + "through the Jena ontology API on OntModels (e.g. createIndividual, createOntClass, etc.) \n"
                    + "and it cannot register new ontologies or new models after reasoner's initialization (initialize method).";
            throw new UnsupportedOperationException(message);
        }
        ontologies.add(ontology);
    }

    /**
     * This is a convenient method for registering at once many ontologies. It
     * actually performs multiple calls to the
     * {@link #register(java.net.URI) register(URI)} method.
     *
     * @param ontologies The list of the ontology URIs.
     */
    public void register(List<IRI> ontologies) {
        if (initialized) {
            String message = "Currently, DLEJena supports only the ontology updates that are made \n"
                    + "through the Jena ontology API on OntModels (e.g. createIndividual, createOntClass, etc.) \n"
                    + "and it cannot register new ontologies or new models after reasoner's initialization (initialize method).";
            throw new UnsupportedOperationException(message);
        }
        for (IRI onto : ontologies) {
            this.ontologies.add(onto);
        }
    }

    /**
     * This method registers the ontology that corresponds to the OntModel
     * provided as parameter. It writes the model in a temporary ontology file
     * using the Jena API and registers the URI (local path) to the reasoner.
     * The file is deleted on exit. This method is useful in cases where the
     * ontology is created using the Jena API, instead of loading it directly
     * from a URI.
     *
     * @param model The Jena OntModel whose ontology would be registered to the
     * DLEJena reasoner.
     */
    public void register(OntModel model) {
        if (initialized) {
            String message = "Currently, DLEJena supports only the ontology updates that are made \n"
                    + "through the Jena ontology API on OntModels (e.g. createIndividual, createOntClass, etc.) \n"
                    + "and it cannot register new ontologies or new models after reasoner's initialization (initialize method).";
            throw new UnsupportedOperationException(message);
        }
        try {
            File temp = File.createTempFile("temp", null);
            temp.deleteOnExit();
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(temp));
            model.write(out, "RDF/XML-ABBREV");
            out.close();
            ontologies.add(IRI.create(temp.toURI()));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DLEJenaReasoner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DLEJenaReasoner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Initializes and applies the inferencing procedure (it should be called
     * only once). This phase involves the separation of TBox from ABox axioms
     * of the registered ontologies, the Pellet TBox reasoning procedure, the
     * generation of the ABox entailment rules and the ABox reasoning procedure.
     * Therefore, it might be a costly procedure depending on the complexity and
     * the size of the loaded ontologies. This method should be called only
     * once. Subsequent calls would ignore the ABox inferences having been made
     * so far and would restart the reasoning procedure over the ontologies that
     * have been registered in the beginning. Note that any registered template
     * rule would be considered during these updates.
     */
    public void initialize() {
        if (ontologies.size() == 0) {
            throw new NoAttachedOntologiesException("There are no ontologies attached to the DLEJena reasoner");
        }
        double separatingTime = System.currentTimeMillis();
        if (DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT) {
            System.out.print("OWLAPI: Separating TBox from ABox axioms...\n");
        }
        OntologyManager sep = new OntologyManager(ontologies);
        if (DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT) {
            System.out.println(" -completed (Time: " + (System.currentTimeMillis() - separatingTime) + " ms)");
        }
        aboxBaseOntModel.removeAll();
        aboxBaseOntModel.add(sep.getAbox());
        tboxBaseOntModel.removeAll();
        tboxBaseOntModel.add(sep.getTbox());

        tboxReasoning();
        aboxReasoning();
        initialized = true;
    }

    /*
     * A convenient method for registering user-defined forward-chaining exceptional rules.
     * An exceptional rule is an ABox inferencing rule that does not depend on TBox triples in the body.
     * DLEJena comes with a predefined set of exceptional rules, as these are
     * defined in the OWL2 RL Profile. These rules are stored in the /rules/exceptional.rules
     * file of the dlejena.jar. However, users may need to apply a larger
     * set of exceptional rules, according to their requirements. The exceptional rules
     * can be registred using this method. Note that any exceptional rule should be
     * registered <b>before</b> initializing the DLEJena reasoner.
     *
     * @param newrules The list of the exceptional Jena rules.
     */
//    public void registerExceptionalRules(List<Rule> newrules) {
//        if (initialized)
//            throw new UnsupportedOperationException("The exceptional rules must be registered prior to DLEJena initialization.");
//        customGeneratorRules.addAll(newrules);
//    }
    /**
     * A convenient method for registering template rules. A template rule is a
     * rule that generates dynamically ABox production rules of a specific type.
     * The body of these rules matches triples that refer to TBox constructs,
     * whereas the head is actually an ABox production rule (see the example
     * folder of the distribution). DLEJena comes with a predefined set of
     * template rules that corresponds to the semantics defined in the OWL2 RL
     * Profile. These rules are stored in the /rules/templates.rules file of the
     * dlejena.jar. However, users may need to apply a larger set of template
     * rules, according to their requirements. The template rules can be
     * registred using this method. Note that any template rule should be
     * registered <b>before</b> initializing the DLEJena reasoner.
     *
     * @param newrules The list of the generator rules.
     */
    public void registerTemplate(List<Rule> newrules) {
        if (initialized) {
            throw new UnsupportedOperationException("The generator rules must be registered prior to DLEJena initialization.");
        }
        customTemplateRules.addAll(newrules);
    }

    /**
     * Removes the rules from the ABox rule reasoner and adds the list of rules
     * provided as parameter. This method should not be called directly.
     *
     * @param rules The rules that will be added after removing the current set
     * of rules from the ABox reasoner.
     */
    protected void resetRuleBase(List<Rule> rules) {
        aboxReasoner.setRules(rules);
        ((FBRuleInfGraph) aboxInfOntModel.getGraph()).rebindAll();
    }

    /**
     * Registering the builtins used by the DLEJenaReasoner
     */
    private void registerBuiltins() {
        //BuiltinRegistry.theRegistry.register(new Generate());
    }

    /**
     * Remove oneOf-related statements from the TBox and copy them to the base
     * ABox model
     */
    private void cleanTBox() {
        List<Statement> oneOfStatements = collectOneOfStatements(tboxInfOntModel);
        tboxInfOntModel.remove(oneOfStatements);
        aboxBaseOntModel.add(oneOfStatements);
    }

    /**
     * Collect the statement relevant to the oneOf construct
     *
     * @param model
     * @return
     */
    private List<Statement> collectOneOfStatements(Model model) {
        ArrayList<Statement> result = new ArrayList<Statement>();

        StmtIterator oneOfStatements = model.listStatements(new SimpleSelector(null, OWL.oneOf, (RDFNode) null));

        while (oneOfStatements.hasNext()) {
            Statement next = (Statement) oneOfStatements.next();
            result.add(next);

            Vector<RDFNode> temp = new Vector<RDFNode>();
            RDFNode list = next.getObject();
            temp.add(list);

            while (!temp.isEmpty()) {
                RDFNode next1 = (RDFNode) temp.get(0);
                temp.remove(0);
                result.addAll(model.listStatements(new SimpleSelector((Resource) next1.as(Resource.class), RDF.first, (RDFNode) null)).toList());
                Statement tail = (Statement) model.listStatements(new SimpleSelector((Resource) next1.as(Resource.class), RDF.rest, (RDFNode) null)).toList().get(0);
                result.add(tail);

                if (!((RDFNode) tail.getObject()).asNode().sameValueAs(RDF.nil.asNode())) {
                    temp.add((RDFNode) tail.getObject());
                }
            }
        }
        return result;
    }

    private void aboxReasoning() {
        performEntailmentReduction(); //..handles also class intersection generating dynamically rules.
        double aboxT = System.currentTimeMillis();
        if (DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT) {
            System.out.println("Jena: Performing ABox reasoning...");
        }
        ArrayList<Rule> aboxRules = new ArrayList<Rule>();
        aboxRules.addAll(DynamicRuleRegistry.getRepository());
        //aboxRules.addAll(loadExceptionalABoxRules());
        resetRuleBase(aboxRules);
        aboxInfOntModel.prepare();
        if (DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT) {
            System.out.println(" -completed (Time: " + (System.currentTimeMillis() - aboxT) + " ms)");
        }
    }

    private void performEntailmentReduction() {
        double entailT = System.currentTimeMillis();
        if (DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT) {
            System.out.print("DLEJena: Generating entailments...\n");
        }
        DynamicRuleRegistry.clear();
        BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(TEMPLATE_RULES_FILE)));
        List rules = Rule.parseRules(Rule.rulesParserFromReader(br));
        rules.addAll(customTemplateRules);

//        FBRuleReasoner reasoner = new FBRuleReasoner(rules);
//        OntModelSpec foms = new OntModelSpec(OntModelSpec.OWL_MEM);
//        foms.setReasoner(reasoner);
//
//        OntModel temp = ModelFactory.createOntologyModel(foms, tboxInfOntModel);
//        temp.prepare(); //adds the dynamic rules to the registry
        List<Rule> dynamic = gp.start(rules);
        for (Rule rule : dynamic) {
            DynamicRuleRegistry.register(rule);
        }

        D_Intersection inter1 = new D_Intersection("D_intersection");
        inter1.formABoxRules(tboxInfOntModel);
        List<Rule> interRules = inter1.getRules();
        for (Rule r : interRules) {
            DynamicRuleRegistry.register(r);
        }

        D_PropertyChain chain = new D_PropertyChain("D_prp-spo2");
        chain.formABoxRules(tboxInfOntModel);
        List<Rule> chainRules = chain.getRules();
        for (Rule r : chainRules) {
            DynamicRuleRegistry.register(r);
        }

        if (DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT) {
            System.out.println(" -completed (Time: " + (System.currentTimeMillis() - entailT) + " ms)");
        }

        try {
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(DLEJenaReasoner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Validates the ABox model using ABox validation rules. This method uses
     * the validation infrastructure provided by the Jena API (validate method).
     * The validation rules are dynamically generated using appropriate
     * generator rules. If the validation generates a validity report, then an
     * exception is thrown. This behavior may be changed through the
     * {@link DLEJenaParameters#THROW_EXCEPTION_ON_ABOX_VALIDATION THROW_EXCEPTION_ON_ABOX_VALIDATION}
     * parameter.
     *
     */
    public void validateABox() {
        ValidityReport report = aboxInfOntModel.validate();
        Iterator iterator = report.getReports();
        while (iterator.hasNext()) {
            Report rep = (Report) iterator.next();
            if (DLEJenaParameters.THROW_EXCEPTION_ON_ABOX_VALIDATION) {
                throw new ABoxValidationException(rep.getDescription());
            } else {
                System.out.println(rep.getDescription());
            }
        }
    }

    private void tboxReasoning() {
        double tboxTime = System.currentTimeMillis();
        if (DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT) {
            System.out.print("Pellet: Performing TBox inferencing...\n");
        }
        tboxInfOntModel.prepare();
        ((PelletInfGraph) tboxInfOntModel.getGraph()).getKB().classify();
        ((PelletInfGraph) tboxInfOntModel.getGraph()).getKB().realize();

        Set<ATermAppl> classes = ((PelletInfGraph) tboxInfOntModel.getGraph()).getKB().getClasses();
        for (ATermAppl c : classes) {
            if (!((PelletInfGraph) tboxInfOntModel.getGraph()).getKB().isSatisfiable(c)) {
                throw new InconsistentTBoxException("Pellet has determined an unsatisfiable concept: " + c.getName());
            }
        }
        if (DLEJenaParameters.SHOW_MESSAGES_IN_STANDARD_OUTPUT) {
            System.out.println(" -completed (Time: " + (System.currentTimeMillis() - tboxTime) + " ms)");
        }
        cleanTBox();
    }

    /**
     * Reconstructs the set of the ABox entailments. This method should be
     * called only when the TBox model has been modified AFTER having
     * initialized the DLEJEna reasoner. TBox updates should be defined only
     * through the Jena API on the TBox OntModel returned by the
     * {@link #getTBox() getTBox} method. Currently, DLEJena does not support
     * the dynamic loading of an ontology after initialization. The method
     * applies from scratch the entailment generation procedure in order to
     * generate the ABox rules that correspond to the updated TBox (see the
     * examples of the DLEJena distribution).
     */
    public void updateABoxRules() {
        if (!initialized) {
            throw new UpdateTBoxException("TBox updates are valid only after having initialized the DLEJena instance.");
        }

        tboxReasoning();
        aboxReasoning();
    }

//    private List<Rule> loadExceptionalABoxRules() {
//        try {
//            BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(EXCEPTIONAL_RULES_FILE)));
//            List<Rule> rules = Rule.parseRules(Rule.rulesParserFromReader(br));
//            rules.addAll(customExceptionalRules);
//            br.close();
//            return rules;
//        }
//        catch (IOException ex) {
//            Logger.getLogger(DLEJenaReasoner.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return null;
//    }
    /**
     * Returns the TBox OntModel created by Pellet. Any TBox modification should
     * be perfomed on this model in order to be reflected on Pellet's base
     * model.
     *
     * @return The TBox Pellet OntModel, which can be viewed (cast) as a
     * PelletInfGraph
     */
    public OntModel getTBox() {
        return tboxInfOntModel;
    }

    /**
     * Returns the ABox OntModel created by the application of the ABox
     * production rules.
     *
     * @return The ABox Jena OntModel.
     */
    public OntModel getABox() {
        return aboxInfOntModel;
    }

    /**
     * The ABox rules that have been used for ABox reasoning.
     *
     * @return The list of the Jena ABox rules used for ABox reasoning. This set
     * involves the exceptional, the dynamically generated and any registered
     * custom rule.
     */
    public List<Rule> getABoxRules() {
        return new ArrayList<Rule>(aboxReasoner.getRules());
    }//    private boolean isPrepared() {
//        return initialized;
//    }
    /**
     * Loading an ontology after preparing DLEJena. This will cause Pellet to
     * reason over the updated TBox, as well as a new set of ABox entailments
     * will be generated and loaded to the ABox rule reasoner.
     *
     * @param ontology
     */
    /*
    public void updateOntologies(List<URI> ontologies) {
    OntologyManager om = new OntologyManager(ontologies);

    tboxBaseOntModel.add(om.getTbox());
    tboxInfOntModel.initialize();
    ((PelletInfGraph) tboxInfOntModel.getGraph()).getKB().classify();
    ((PelletInfGraph) tboxInfOntModel.getGraph()).getKB().realize();

    aboxBaseOntModel.add(om.getAbox());
    aboxReasoning();
    }
     */
}
