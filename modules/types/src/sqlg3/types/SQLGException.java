package sqlg3.types;

/**
 * SQLG runtime exception - is raised in case of internal errors.
 */
public class SQLGException extends RuntimeException {

    public SQLGException(String message) {
        super(message);
    }

    public SQLGException(String message, Throwable cause) {
        super(message, cause);
    }
}
