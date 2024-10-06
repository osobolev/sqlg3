package sqlg3.runtime;

import sqlg3.types.SQLGException;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class performs Java/SQL mapping for given class.
 */
public abstract class TypeMapper<T> {

    public final Class<T> cls;

    protected TypeMapper(Class<T> cls) {
        this.cls = cls;
    }

    public int getResultSetColumns() {
        return 1;
    }

    public int getStatementParameters() {
        return 1;
    }

    /**
     * Fetches class instance from the result set.
     *
     * @param rs result set
     * @param index first column of result set
     * @return instance of mapped class fetched from result set
     */
    public abstract T fetch(ResultSet rs, int index) throws SQLException;

    /**
     * Sets the parameter for a prepared statement.
     *
     * @param stmt prepared statement
     * @param index parameter index
     * @param value value to set. If value is nullable you should provide this case too.
     */
    public abstract void set(PreparedStatement stmt, int index, T value) throws SQLException;

    protected final void noOut(Class<T> cls) {
        throw new SQLGException("Out parameters of type " + cls.getCanonicalName() + " are not supported");
    }

    /**
     * Registers OUT parameter of mapped class for callable statement. By default this method throws
     * runtime exception. Override this method to use classes as OUT parameters.
     *
     * @param cs callable statement
     * @param index parameter index
     */
    public void register(CallableStatement cs, int index) throws SQLException {
        noOut(cls);
    }

    /**
     * Retrieves OUT parameter of mapped class from callable statement after execution.
     * By default this method throws runtime exception. Override this method to use classes as OUT parameters.
     *
     * @param cs callable statement
     * @param index parameter index
     */
    public T get(CallableStatement cs, int index) throws SQLException {
        noOut(cls);
        return null;
    }
}
