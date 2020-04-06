package sqlg3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for local variables marking that variable's value should be taken
 * from comments (as a string). When extracting string from comments following
 * replacements are made:
 * <code>"a &amp;b c"</code>
 * to
 * <code>"a " + b + " c"</code>
 * (i.e. variable substitution is performed).
 * <p>
 * Example:
 * <pre>
 * String table = "table";
 * /**
 *  * SELECT name, value
 *  *   FROM &amp;table
 *  *  WHERE id = ?
 *  *&#47;
 * &#64;Sql String sql = null;
 * </pre>
 * <p>
 * Since Sql annotation works only with text (no parameters allowed), usually it's more convenient to use
 * {@link sqlg3.runtime.QueryPiece} annotation to encapsulate both SQL text and its parameters into one entity.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.LOCAL_VARIABLE)
public @interface Sql {
}
