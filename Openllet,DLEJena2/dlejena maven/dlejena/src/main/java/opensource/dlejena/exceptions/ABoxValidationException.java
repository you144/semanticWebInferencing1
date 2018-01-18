
package opensource.dlejena.exceptions;

/**
 * This exception is thrown whenever the validation of the ABox produces a validity report.
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class ABoxValidationException extends RuntimeException{

    public ABoxValidationException(String message) {
        super(message);
    }

}
