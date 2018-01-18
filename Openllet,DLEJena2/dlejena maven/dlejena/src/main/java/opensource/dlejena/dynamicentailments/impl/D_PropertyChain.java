package opensource.dlejena.dynamicentailments.impl;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.RDFS;
import java.util.ArrayList;
import opensource.dlejena.dynamicentailments.ProceduralGenerator;

/**
 * Implementation of the owl:propertyChain semantics in a procedural manner.
 * We actually generate a dynamic rule for each property chain relationship.
 * 
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class D_PropertyChain extends ProceduralGenerator {

    private Resource sc;
    private RDFNode P;
    private RDFNode L;
    private static final Property propertyChain = ModelFactory.createDefaultModel().createProperty("http://www.w3.org/2002/07/owl#propertyChain");

    public D_PropertyChain(String ruleName) {
        super(ruleName);
    }

    public void formABoxRules(OntModel tbox) {

        StmtIterator find = tbox.listStatements(new SimpleSelector(null, propertyChain, (RDFNode) null));
        while (find.hasNext()) {
            Statement t = find.nextStatement();
            sc = t.getSubject();
            L = t.getObject();

            StmtIterator find2 = tbox.listStatements(new SimpleSelector(sc, RDFS.subPropertyOf, (RDFNode) null));
            while (find2.hasNext()) {
                Statement t2 = find2.nextStatement();
                P = t2.getObject();

                RDFList mylist = (RDFList) L.as(RDFList.class);

                ArrayList<ClauseEntry> head = new ArrayList<ClauseEntry>();
                ArrayList<ClauseEntry> body = new ArrayList<ClauseEntry>();
                ArrayList<Node_RuleVariable> variableList = new ArrayList<Node_RuleVariable>();

                for (int i = 0; i < mylist.size() + 1; i++)
                    variableList.add(new Node_RuleVariable("?u" + (i + 1), i));

                for (int i = 0; i < mylist.size(); i++) {
                    Resource pi = (Resource) mylist.get(i);
                    body.add(new TriplePattern(variableList.get(i), pi.asNode(), variableList.get(i + 1)));
                }
                head.add(new TriplePattern(variableList.get(0), P.asNode(), variableList.get(variableList.size() - 1)));

                addRule(new Rule(getRuleName(), head, body));

            }
        }
    }
}
