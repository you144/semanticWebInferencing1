package opensource.dlejena.dynamicentailments.impl;

import com.hp.hpl.jena.ontology.OntModel;
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
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.ArrayList;
import opensource.dlejena.dynamicentailments.ProceduralGenerator;


/**
 * Implementation of the iff semantics of class intersection in a procedural manner.
 * We actually generate a dynamic rule for each intersection relationship.
 * 
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */

public class D_Intersection extends ProceduralGenerator {

    private Resource C;
    private RDFNode L;

    public D_Intersection(String ruleName) {
        super(ruleName);
    }

    public void formABoxRules(OntModel tbox) {
        StmtIterator find = tbox.listStatements(new SimpleSelector(null, OWL.intersectionOf, (RDFNode) null));
        while (find.hasNext()) {
            Statement t = find.nextStatement();
            C = t.getSubject();
            L = t.getObject();

            RDFList mylist = (RDFList) L.as(RDFList.class);
            ExtendedIterator iterator = mylist.iterator();

            ArrayList<ClauseEntry> head = new ArrayList<ClauseEntry>();
            ArrayList<ClauseEntry> body = new ArrayList<ClauseEntry>();
            Node_RuleVariable varX = new Node_RuleVariable("?X", 0);
            while (iterator.hasNext()) {
                Resource next = (Resource) iterator.next();
                body.add(new TriplePattern(varX, RDF.type.asNode(), next.asNode()));
            }
            head.add(new TriplePattern(varX, RDF.type.asNode(), C.asNode()));

            addRule(new Rule(getRuleName(), head, body));
        }

    }
}
