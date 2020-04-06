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
 * int idParam = ...;
 * /**
 *  * SELECT name, value
 *  *   FROM table
 *  *  WHERE id = :idParam
 *  *&#47;
 * &#64;Prepare PreparedStatement stmt = null;
 * </pre>
 * Preprocessor generates code required to prepare statement and set its parameters.
 * <p>
 * Note that parameters referenced
 * in query (as <code>:paramName</code>) should be accessible as variables in the current scope.
 * <p>
 * You can use string and query substitution in comments, see {@link sqlg3.runtime.QueryPiece}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.LOCAL_VARIABLE)
public @interface Prepare {
}
