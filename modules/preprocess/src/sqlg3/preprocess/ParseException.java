package sqlg3.preprocess;

public final class ParseException extends Exception {

    public final String at;

    ParseException(String message, String at) {
        super(message);
        this.at = at;
    }

    ParseException(String message) {
        this(message, null);
    }
}
