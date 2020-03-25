package sqlg3.preprocess;

/**
 * Description of DB-to-Java mapping for one column of a result set.
 */
public final class ColumnInfo {

    final Class<?> type;
    final String name;

    /**
     * Constructor.
     *
     * @param type type of the Java field for this DB column
     * @param name name of the Java field for this DB column
     */
    public ColumnInfo(Class<?> type, String name) {
        this.type = type;
        this.name = name;
    }

    public String toString() {
        return type + " " + name;
    }
}
