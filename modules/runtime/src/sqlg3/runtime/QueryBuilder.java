package sqlg3.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Query builder (used to concatenate multiple {@link QueryPiece}s effectively, just as StringBuilder for Strings).
 */
public final class QueryBuilder {

    private final StringBuilder sql;
    private final List<Parameter> data;

    public QueryBuilder() {
        this("");
    }

    /**
     * @param sql initial query text (not null)
     */
    public QueryBuilder(CharSequence sql) {
        this.sql = new StringBuilder(sql);
        this.data = new ArrayList<>();
    }

    /**
     * @param piece initial query piece (not null)
     */
    public QueryBuilder(QueryPiece piece) {
        this.sql = new StringBuilder(piece.sql);
        this.data = new ArrayList<>(Arrays.asList(piece.data));
    }

    private QueryBuilder append(QueryPiece that, boolean breakLine) {
        if (that == null)
            return this;
        append(this.sql, that.sql, breakLine);
        data.addAll(Arrays.asList(that.data));
        return this;
    }

    private QueryBuilder append(QueryBuilder that, boolean breakLine) {
        if (that == null)
            return this;
        append(this.sql, that.sql.toString(), breakLine);
        data.addAll(that.data);
        return this;
    }

    /**
     * Appends query piece. Parameter <code>that</code> can be null (then it is ignored).
     * Line break is inserted between pieces, so it is safer to use than {@link #appendLit(QueryPiece)} for user.
     */
    public QueryBuilder append(QueryPiece that) {
        return append(that, true);
    }

    /**
     * Appends query piece. Parameter <code>that</code> can be null (then it is ignored).
     * Nothing is inserted between pieces. Usually used by preprocessor-generated code.
     */
    public QueryBuilder appendLit(QueryPiece that) {
        return append(that, false);
    }

    /**
     * Appends query builder. Parameter <code>that</code> can be null (then it is ignored).
     * Line break is inserted between pieces.
     */
    public QueryBuilder append(QueryBuilder that) {
        return append(that, true);
    }

    /**
     * Appends query piece. Parameter <code>that</code> can be null (then it is ignored).
     * Nothing is inserted between pieces. Usually used by preprocessor-generated code.
     */
    public QueryBuilder appendLit(QueryBuilder that) {
        return append(that, false);
    }

    static String add(String sql1, CharSequence sql2, boolean breakLine) {
        StringBuilder buf = new StringBuilder(sql1);
        append(buf, sql2, breakLine);
        return buf.toString();
    }

    private static void append(StringBuilder buf, CharSequence sql, String separators, String separate) {
        boolean endsWithEoln;
        if (buf.length() > 0) {
            endsWithEoln = separators.indexOf(buf.charAt(buf.length() - 1)) >= 0;
        } else {
            endsWithEoln = false;
        }
        boolean startsWithEoln;
        if (sql.length() > 0) {
            startsWithEoln = separators.indexOf(sql.charAt(0)) >= 0;
        } else {
            startsWithEoln = false;
        }
        if (!endsWithEoln && !startsWithEoln) {
            buf.append(separate);
        }
        buf.append(sql);
    }

    private static void append(StringBuilder buf, CharSequence sql, boolean breakLine) {
        if (breakLine) {
            append(buf, sql, "\r\n", "\n");
        } else {
            buf.append(sql);
        }
    }

    /**
     * Appends string query piece.
     * Line break is inserted between pieces, so it is safer to use than {@link #appendLit(CharSequence)} for user.
     */
    public QueryBuilder append(CharSequence sql) {
        append(this.sql, sql, true);
        return this;
    }

    /**
     * Appends string query piece.
     * Nothing is inserted between pieces. Usually used by preprocessor-generated code.
     */
    public QueryBuilder appendLit(CharSequence sql) {
        append(this.sql, sql, false);
        return this;
    }

    /**
     * Appends numeric query piece.
     * Nothing is inserted between pieces. Usually used by preprocessor-generated code.
     */
    public QueryBuilder appendLit(Number sql) {
        return appendLit(sql.toString());
    }

    /**
     * Returns built query
     */
    public QueryPiece toQuery() {
        return new QueryPiece(sql, data.toArray(new Parameter[0]));
    }

    public String toString() {
        return sql.toString();
    }
}
