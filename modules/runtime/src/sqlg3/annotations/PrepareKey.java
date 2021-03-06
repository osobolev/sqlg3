package sqlg3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for local variables marking PreparedStatement to be generated.
 * SQL query is taken from preceding javadoc comments.
 * <p>
 * Example:
 * <pre>
 * String messageText = ...;
 * /**
 *  * INSERT INTO log (message) VALUES (:messageText)
 *  *&#47;
 * &#64;PrepareKey("id") PreparedStatement stmt = null;
 * executeUpdate(stmt);
 * int id = getGeneratedInt();
 * </pre>
 * Preprocessor generates code required to prepare statement with given auto-generated columns and set its parameters.
 * <p>
 * Note that parameters referenced
 * in query (as <code>:paramName</code>) should be accessible as variables in the current scope.
 * <p>
 * You can use string and query substitution in comments, see {@link sqlg3.runtime.QueryPiece}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.LOCAL_VARIABLE)
public @interface PrepareKey {

    /**
     * Comma-separated list of column names which are auto-generated by the statement. Leave default value
     * to return all generated keys (but the order is unspecified, plus such feature may be not supported).
     * <p>
     * Note that upper/lower case may be significant (depends on DB).
     * <p>
     * Generated key values can be retrieved by {@link sqlg3.runtime.GBase#getGeneratedKeys(java.sql.PreparedStatement)} method,
     * which returns array of generated key values - one value for each given column (order of columns is preserved
     * if the value was specified).
     */
    String value() default "";
}
