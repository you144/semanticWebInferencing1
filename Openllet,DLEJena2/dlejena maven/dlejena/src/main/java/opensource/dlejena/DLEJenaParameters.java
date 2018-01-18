package opensource.dlejena;

/**
 * This class contains static variables that can be used to modify at runtime
 * some DLEJena's parameters.
 *
 * @author George Meditskos
 */
public class DLEJenaParameters {

    /**
     * The URI of the ontology that contains the ABox axioms of the registered
     * ontologies. The default value is http://DLEJena/ABox.owl
     */
    public static String ABOX_ONTOLOGY_BASE_URI = "http://DLEJena/ABox.owl";

    /**
     * The URI of the ontology that contains the TBox axioms of the registered
     * ontologies. The default value is http://DLEJena/TBox.owl
     */
    public static String TBOX_ONTOLOGY_BASE_URI = "http://DLEJena/TBox.owl";

    /**
     * Set this variable to true or false according to whether an exception
     * should be thrown after an unsuccessful ABox validation procedure or a
     * message should be printed. The default value is true.
     */
    public static boolean THROW_EXCEPTION_ON_ABOX_VALIDATION = true;

    /**
     * A true value would cause the printing of some basic messages in the
     * standard output, such as reasoning times. The default value is false
     */
    public static boolean SHOW_MESSAGES_IN_STANDARD_OUTPUT = false;

}
