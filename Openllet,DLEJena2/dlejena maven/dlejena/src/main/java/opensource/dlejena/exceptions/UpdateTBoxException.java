
package opensource.dlejena.exceptions;

/**
 * This exception is thrown whenever the {@link dlejena.DLEJenaReasoner#updateABoxRules() updateABoxRules} method
 * is called before initializing the DLEJena reasoner.
 * @author Georgios Meditskos <gmeditsk@csd.auth.gr>
 */
public class UpdateTBoxException extends RuntimeException{

    public UpdateTBoxException(String message) {
        super(message);
    }
    

}
