package sqlg3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for local variables marking that CallableStatement call should be generated.
 * CallableStatement template is taken from preceding javadoc comments.
 * <p>
 * Example:
 * <pre>
 * String[] name = new String[1];
 * String[] value = new String[1];
 * int idParam = ...;
 * /**
 *  * BEGIN
 *  *   SELECT name, value
 *  *     INTO :&gt;name, :&gt;value
 *  *     FROM table
 *  *    WHERE id = :idParam
 *  * END;
 *  *&#47;
 * &#64;Call Object[] ret = null;
 * // now name[0] and value[0] contain selected values
 * // also ret.length == 2, ret[0] == name[0] and ret[1] == value[0]
 * </pre>
 * Preprocessor generates code required to call statement with supplied parameters.
 * <p>
 * Note that parameters referenced
 * in query (as <code>:paramName</code>) should be accessible as variables in the current scope.
 * <p>
 * You can use string and query substitution in comments, see {@link sqlg3.runtime.Query}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.LOCAL_VARIABLE)
public @interface Call {
}
