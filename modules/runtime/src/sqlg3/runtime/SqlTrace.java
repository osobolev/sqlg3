package sqlg3.runtime;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tracing of SQL statements.
 */
public interface SqlTrace {

    static void doTrace(Consumer<String> logger, String header, Supplier<List<String>> getMessages) {
        List<String> messages = getMessages.get();
        logger.accept(header);
        for (String message : messages) {
            logger.accept(message);
        }
    }

    static SqlTrace createDefault(Consumer<String> logger) {
        return (ok, time, getMessages) -> {
            if (!ok) {
                doTrace(logger, "SQL not completed properly", getMessages);
            }
        };
    }

    /**
     * @param ok false if SQL statement has not completed successfully
     * @param time time in milliseconds
     */
    void trace(boolean ok, long time, Supplier<List<String>> getMessages);
}
