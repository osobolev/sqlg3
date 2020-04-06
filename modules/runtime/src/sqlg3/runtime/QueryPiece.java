package sqlg3.runtime;

/**
 * Query piece encapsulating query text and query parameters.
 */
public final class QueryPiece {

    public final String sql;
    public final Parameter[] data;

    /**
     * Constructor.
     *
     * @param sql SQL text
     * @param data parameters (not null)
     */
    public QueryPiece(CharSequence sql, Parameter[] data) {
        this.sql = sql.toString();
        this.data = data;
    }

    /**
     * Concatenation of query pieces. Parameter <code>that</code> can contain nulls (they are ignored).
     * Line break is inserted between pieces.
     */
    public QueryPiece add(QueryPiece... that) {
        QueryBuilder buf = new QueryBuilder(this);
        for (QueryPiece piece : that) {
            buf.append(piece);
        }
        return buf.toQuery();
    }

    /**
     * Concatenation of query piece and string. Line break is inserted between them.
     */
    public QueryPiece add(CharSequence sql) {
        return new QueryPiece(QueryBuilder.add(this.sql, sql, true), data);
    }

    public String toString() {
        return sql;
    }
}
