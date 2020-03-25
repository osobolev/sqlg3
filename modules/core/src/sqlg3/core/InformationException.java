package sqlg3.core;

/**
 * This is "informational" exception.
 * If a business method raises exception of this type, and
 * <code>error == false</code> then runtime system does not roll back
 * transaction - call is finished successfully, but exception is rethrown.
 * That way you can pass information about exceptional situation
 * without losing DB changes. Default value of <code>error == false</code>.
 */
public class InformationException extends Exception {

    public final boolean error;

    public InformationException(String message, boolean error) {
        super(message);
        this.error = error;
    }

    public InformationException(String message) {
        this(message, false);
    }
}
