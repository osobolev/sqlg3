package sqlg3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for local variables marking QueryPiece to be generated.
 * SQL query is taken from preceding javadoc comments.
 * {@link sqlg3.runtime.QueryPiece} can be used to build larger SQL statement from pieces.
 * <p>
 * Example:
 * <pre>
 * int idParam = ...;
 * /**
 *  *  WHERE id = :idParam
 *  *&#47;
 * &#64;Query QueryPiece sql1 = null;
 * </pre>
 * Preprocessor generates code to encapsulate SQL text and parameters in the {@link sqlg3.runtime.QueryPiece} object.
 * <p>
 * Note that parameters referenced
 * in query (as <code>:paramName</code>) should be accessible as variables in the current scope.
 * <p>
 * You can use query pieces to build larger queries manually using {@link sqlg3.runtime.QueryBuilder} or methods
 * like {@link sqlg3.runtime.QueryPiece#add(sqlg3.runtime.QueryPiece...)}, but also you can reference query pieces in
 * javadoc comments used for queries as <code>&amp;piece</code>, example:
 * <pre>
 * /**
 *  * SELECT name, value
 *  *   FROM table
 *  *  &amp;sql1
 *  *&#47;
 * &#64;Prepare PreparedStatement stmt = null;
 * </pre>
 * In this way you can combine multiple pieces into one query. &amp;-substitution works not only for QueryPieces, but
 * also for Strings:
 * <pre>
 * String table = "table";
 * /**
 *  * SELECT name, value
 *  *   FROM &amp;table
 *  *  &amp;sql1
 *  *&#47;
 * &#64;Query QueryPiece largeQuery = null;
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.LOCAL_VARIABLE)
public @interface Query {
}
