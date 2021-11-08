package sqlg3.preprocess;

/**
 * Warning output mode for ANT task
 */
public enum SQLGWarn {
    /**
     * Ignore warnings
     */
    none,
    /**
     * Output warnings
     */
    warn,
    /**
     * Treat warnings as errors
     */
    error
}
