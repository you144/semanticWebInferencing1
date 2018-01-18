package opensource.dlejena.utils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import java.util.List;

/**
 * Convenient print methods
 *
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class Print {

    public static void printModel(Model model) {
        StmtIterator listStatements = model.listStatements();
        while (listStatements.hasNext()) {
            System.out.println(listStatements.next());
        }
    }

    public static void printList(List list) {
        for (Object object : list) {
            System.out.println(object);
        }
    }

    public static void printRuleSet(List<Rule> rules) {
        for (Rule rule : rules) {
            System.out.println(rule);
        }
    }
}
