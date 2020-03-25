package sqlg3.runtime;

import sqlg3.core.SQLGLogger;

import java.util.List;
import java.util.function.Supplier;

/**
 * Tracing of SQL statements.
 */
public interface SqlTrace {

    static void doTrace(SQLGLogger logger, String header, Supplier<List<String>> getMessages) {
        List<String> messages = getMessages.get();
        logger.error(header);
        for (String message : messages) {
            logger.error(message);
        }
    }

    static SqlTrace createDefault(SQLGLogger logger) {
        return (ok, time, getMessages) -> {
            if (!ok) {
                doTrace(logger, "SQL not completed properly", getMessages);
            }
        };
    }

    /**
     * @param ok false if SQL statement has not completed successfully
     * @param time time in milliseconds
     * @return null for no trace, not null for message to be output
     */
    void trace(boolean ok, long time, Supplier<List<String>> getMessages);
}
