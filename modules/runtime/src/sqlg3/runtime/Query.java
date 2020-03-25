package sqlg3.runtime;

/**
 * Query piece encapsulating query text and query parameters.
 */
public final class Query {

    public final String sql;
    public final Parameter[] data;

    /**
     * Constructor.
     *
     * @param sql SQL text
     * @param data parameters (not null)
     */
    public Query(CharSequence sql, Parameter[] data) {
        this.sql = sql.toString();
        this.data = data;
    }

    /**
     * Concatenation of query pieces. Parameter <code>that</code> can contain nulls (they are ignored).
     * Line break is inserted between pieces.
     */
    public Query add(Query... that) {
        QueryBuilder buf = new QueryBuilder(this);
        for (Query piece : that) {
            buf.append(piece);
        }
        return buf.toQuery();
    }

    /**
     * Concatenation of query piece and string. Line break is inserted between them.
     */
    public Query add(CharSequence sql) {
        return new Query(QueryBuilder.add(this.sql, sql, true), data);
    }

    public String toString() {
        return sql;
    }
}
