package sqlg3.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Query builder (used to concatenate multiple {@link QueryPiece}s effectively, just as StringBuilder for Strings).
 */
public final class QueryBuilder implements QueryLike {

    private final StringBuilder sql;
    private final List<Parameter> data;

    public QueryBuilder() {
        this("");
    }

    /**
     * @param sql initial query text (not null)
     */
    public QueryBuilder(CharSequence sql, Parameter... data) {
        this.sql = new StringBuilder(sql);
        this.data = new ArrayList<>(Arrays.asList(data));
    }

    /**
     * @param piece initial query piece (not null)
     */
    public QueryBuilder(QueryLike piece) {
        this.sql = new StringBuilder(piece.getSqlChars());
        this.data = new ArrayList<>(piece.getParameters());
    }

    @Override
    public CharSequence getSqlChars() {
        return sql;
    }

    @Override
    public String getSql() {
        return sql.toString();
    }

    @Override
    public List<Parameter> getParameters() {
        return data;
    }

    private void append(CharSequence sql, List<Parameter> data, boolean breakLine) {
        append(this.sql, sql, breakLine);
        this.data.addAll(data);
    }

    /**
     * Same as {@link #append(QueryLike)}
     */
    public QueryBuilder append(CharSequence sql, Parameter... params) {
        append(sql, Arrays.asList(params), true);
        return this;
    }

    /**
     * Appends query piece. Parameter <code>that</code> can be null (then it is ignored).
     * Line break is inserted between pieces, so it is safer to use than {@link #appendLit(QueryLike)} for user.
     */
    public QueryBuilder append(QueryLike that) {
        if (that != null) {
            append(that.getSqlChars(), that.getParameters(), true);
        }
        return this;
    }

    /**
     * Same as {@link #appendLit(QueryLike)}
     */
    public QueryBuilder appendLit(CharSequence sql, Parameter... params) {
        append(sql, Arrays.asList(params), false);
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
     * Appends query piece. Parameter <code>that</code> can be null (then it is ignored).
     * Nothing is inserted between pieces. Usually used by preprocessor-generated code.
     */
    public QueryBuilder appendLit(QueryLike that) {
        if (that != null) {
            append(that.getSqlChars(), that.getParameters(), false);
        }
        return this;
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
     * Returns built query
     */
    public QueryPiece toQuery() {
        return new QueryPiece(sql, data.toArray(new Parameter[0]));
    }

    public String toString() {
        return sql.toString();
    }
}
