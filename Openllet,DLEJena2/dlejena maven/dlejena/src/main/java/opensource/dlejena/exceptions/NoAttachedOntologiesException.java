
package opensource.dlejena.exceptions;

/**
 * This exception is thrown if the DLEJena reasoner is initialized ({@link dlejena.DLEJenaReasoner#initialize() initialize}) without any registered ontology.
 * @author Georgios Meditskos
 */
public class NoAttachedOntologiesException extends RuntimeException {

    /**
     * 
     * @param message
     */
    public NoAttachedOntologiesException(String message) {
        super(message);
    }
}
