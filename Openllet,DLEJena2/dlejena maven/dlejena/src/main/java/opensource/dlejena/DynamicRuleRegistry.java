package opensource.dlejena;

import com.hp.hpl.jena.reasoner.rulesys.Rule;
import java.util.ArrayList;

/**
 * The registry for storing the dynamic ABox rules. This registry is used by the
 * TemplateProcessor in order to register the ABox rules that produces.
 *
 * @author George Meditskos
 */
public class DynamicRuleRegistry {

    private static ArrayList<Rule> repository = new ArrayList<Rule>();

    /**
     * The method for registering a rule
     *
     * @param r The rule
     */
    public static void register(Rule r) {
        repository.add(r);
    }

    /**
     * The method for retrieving the number of the registered rules.
     *
     * @return The size of the registry
     */
    public static int getRegistrySize() {
        return getRepository().size();
    }

    /**
     * The method for retrieving the dynamic rule repository.
     *
     * @return The list of the registered rules.
     */
    public static ArrayList<Rule> getRepository() {
        return repository;
    }

    /**
     * A method for removing all the registered rules.
     */
    public static void clear() {
        getRepository().clear();
    }
}
