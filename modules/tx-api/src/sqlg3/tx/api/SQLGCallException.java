package sqlg3.tx.api;

public class SQLGCallException extends RuntimeException {

    public SQLGCallException(String message) {
        super(message);
    }

    public SQLGCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
