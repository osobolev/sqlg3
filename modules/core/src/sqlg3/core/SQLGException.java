package sqlg3.core;

/**
 * SQLG runtime exception - is raised in case of internal errors.
 */
public class SQLGException extends RuntimeException {

    public SQLGException(String s) {
        super(s);
    }

    public SQLGException(String message, Throwable cause) {
        super(message, cause);
    }
}
