package sqlg3.tx.api;

public class TxCallException extends RuntimeException {

    public TxCallException(String message) {
        super(message);
    }

    public TxCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
