package sqlg3.runtime;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * For internal use.
 * Stores methods to create row type class instances from result set.
 */
interface RowTypeFactory<T> {

    T fetch(TypeMappers mappers, ResultSet rs) throws SQLException;
}
