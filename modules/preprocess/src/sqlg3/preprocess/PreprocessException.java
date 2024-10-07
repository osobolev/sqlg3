package sqlg3.preprocess;

public class PreprocessException extends Exception {

    public PreprocessException(String message) {
        super(message);
    }

    public PreprocessException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreprocessException(Throwable cause) {
        super(cause);
    }
}
