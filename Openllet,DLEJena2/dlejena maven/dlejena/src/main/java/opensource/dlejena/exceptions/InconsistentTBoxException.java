package opensource.dlejena.exceptions;

/**
 * This exception is thrown whenever Pellet determines an unsatisfiable concept.
 * Note that ABox inconsistencies are determined using Jena validation rules.
 * 
 * @author Georgios Meditskos
 */
public class InconsistentTBoxException extends RuntimeException {

    /**
     * 
     * @param message
     */
    public InconsistentTBoxException(String message) {
        super(message);
    }
}
