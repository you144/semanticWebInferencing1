package opensource.dlejena.dynamicentailments;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.FBRuleInfGraph;
import com.hp.hpl.jena.reasoner.rulesys.FBRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * This is the module that handles the template rules.
 * The template rules follow the Jena syntax for defining hybrid
 * rules, that is, production rules that deduce backward rules.
 * Jena does not support the deduction of production rules based
 * on production rules. This module applies the template rules
 * over the TBox OntModel and treats any instantiated rule (head) as a forward one.
 * <p>
 * Actually, this module makes use of the instantiation capabilities
 * of the hybrid rule engine of Jena in order to generate ABox entailments in 
 * a backward manner. These rules are finally transformed into forward ones
 * and are forwarded to the ABox reasoner of DLEJena, which is forward-chaining rule engine.
 * This is an easy approach to enable the definition of forward-chaining 
 * template rules in Jena, needed in our entailment reduction approach.
 * </p>
 * 
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class TemplateProcessor {

    private FBRuleReasoner reasoner;
    private OntModel infModel;
    private final static Rule scm_int = Rule.parseRule("[scm-int: (?C owl:intersectionOf ?L) -> listMapAsObject(?C rdfs:subClassOf ?L)]");
    private final static Rule scm_uni = Rule.parseRule("[scm-uni: (?C owl:unionOf ?L)  -> listMapAsSubject(?L rdfs:subClassOf ?C)]");

    /**
     * The constructor.
     * @param tbox The TBox OntModel where the template rules will be run.
     */
    public TemplateProcessor(OntModel tbox) {
        reasoner = new FBRuleReasoner(new Vector<Rule>());
        OntModelSpec foms2 = new OntModelSpec(OntModelSpec.OWL_MEM);
        foms2.setReasoner(reasoner);

        infModel = ModelFactory.createOntologyModel(foms2);
        infModel.addSubModel(tbox);
    }

    /**
     * Starts the processor.
     * @param generators The set of the template rules that will be processed.
     * @return The instantiated forward-chaining rules that will be used for ABox reasoning
     */
    public List<Rule> start(List<Rule> generators) {

        //clear the reasoner...
        infModel.removeAll();
        reasoner.setRules(new Vector<Rule>());
        ((FBRuleInfGraph) infModel.getGraph()).rebindAll();

        List<Rule> mutatedGenerators = mutateGenerators(generators);
        mutatedGenerators.add(scm_int);
        mutatedGenerators.add(scm_uni);
        scm_int.setBackward(false);
        scm_uni.setBackward(false);//I do not want the in getBRules...

        //dlejena.utils.Print.printRuleSet(mutatedGenerators);

        reasoner.setRules(mutatedGenerators);
        ((FBRuleInfGraph) infModel.getGraph()).rebindAll();
        infModel.prepare();

        List<Rule> bRules = ((FBRuleInfGraph) infModel.getGraph()).getBRules();
        List<Rule> dynamic = recoverReducedRules(bRules);
        return dynamic;
    }

    private List<Rule> mutateGenerators(List<Rule> generators) {
        ArrayList<Rule> mutatedGenerators = new ArrayList<Rule>();

        for (Rule generator : generators) {
            //the head of each generator should be always a single rule.
            ClauseEntry[] genHead = generator.getHead();
            if (genHead.length > 1 || !(genHead[0] instanceof Rule))
                throw new UnsupportedOperationException(" " + generator.getName() + ": This is not a generator rule!");

            //exceptional rule
            if (generator.getBody().length == 0) {
                Rule exceptional = (Rule) genHead[0];
                exceptional.setBackward(true);
                mutatedGenerators.add(exceptional);
            }
            else {//wrap the rule in a functor
                genHead[0] = mutateRule((Rule) genHead[0]);
                mutatedGenerators.add(generator);
            }
        }
        return mutatedGenerators;
    }

    private Rule mutateRule(Rule headRule) {
        ClauseEntry[] headOfHeadRule = headRule.getHead();
        //3 * 'number of triples in the head of the headRule'
        Node[] functorNodes = new Node[headOfHeadRule.length * 3];

        //for each triple in the head of the headRule
        for (int i = 0; i < headOfHeadRule.length; i++) {
            TriplePattern t = (TriplePattern) headOfHeadRule[i];
            functorNodes[i * 3] = t.asTriple().getSubject();
            functorNodes[i * 3 + 1] = t.asTriple().getPredicate();
            functorNodes[i * 3 + 2] = t.asTriple().getObject();
        }

        Functor aux = new Functor("aux", functorNodes);

        TriplePattern pattern =
                new TriplePattern(Node.createLiteral("dlejena"), Node.createLiteral("dlejena"), Functor.makeFunctorNode(aux));

        Rule mutateRule = new Rule(headRule.getName(), Collections.singletonList(pattern), Arrays.asList(headRule.getBody()));
        mutateRule.setBackward(true);
        return mutateRule;
    }

    private List<Rule> recoverReducedRules(List<Rule> mutatatedRules) {
        ArrayList<Rule> dynamic = new ArrayList<Rule>();
        for (Rule reducedRule : mutatatedRules) {
            ClauseEntry[] reducedHead = reducedRule.getHead();
            
            if (reducedHead[0] instanceof Functor) {//exceptional
                dynamic.add(reducedRule);
                reducedRule.setBackward(false);
                continue;
            }
            
            TriplePattern triple = (TriplePattern) reducedHead[0];
            if (!(triple.getObject().getIndexingValue() instanceof Functor)) {//exceptional
                dynamic.add(reducedRule);
                reducedRule.setBackward(false);
                continue;
            }
            Functor functor = (Functor) triple.getObject().getIndexingValue();
            Node[] args = functor.getArgs();
            ClauseEntry[] pattern = new ClauseEntry[args.length / 3];
            for (int i = 0; i < args.length / 3; i++) {
                Node subject = args[i * 3];
                Node predicate = args[i * 3 + 1];
                Node object = args[i * 3 + 2];
                pattern[i] = new TriplePattern(subject, predicate, object);
            }
            reducedHead = pattern;
            dynamic.add(new Rule(reducedRule.getName(), pattern, reducedRule.getBody()));
        }
        return dynamic;
    }
}
