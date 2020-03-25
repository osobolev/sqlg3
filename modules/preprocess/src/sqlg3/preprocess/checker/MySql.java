package sqlg3.preprocess.checker;

/**
 * SQL checker for MySQL
 */
public final class MySql extends Generic {

    public MySql() {
        super(new sqlg3.runtime.specific.MySql());
    }
}
