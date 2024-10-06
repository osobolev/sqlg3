package sqlg3.types;

import java.io.Serializable;

/**
 * Represents column meta-info.
 */
public final class MetaColumn implements Serializable {

    /**
     * true if column is not null
     */
    public final boolean notNull;
    /**
     * For VARCHAR/VARBINARY column types - maximum length
     */
    public final int length;
    /**
     * For NUMERIC column types - precision
     */
    public final int precision;
    /**
     * For NUMERIC column types - scale
     */
    public final int scale;

    public MetaColumn(boolean notNull, int length, int precision, int scale) {
        this.notNull = notNull;
        this.length = length;
        this.precision = precision;
        this.scale = scale;
    }

    public int length() {
        return length;
    }
}
