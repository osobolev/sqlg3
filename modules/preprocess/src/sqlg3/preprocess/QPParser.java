package sqlg3.preprocess;

import sqlg3.runtime.QueryReplacer;
import sqlg3.runtime.queries.QueryParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class QPParser {

    private static final class Pair {

        final String piece;
        final String ident;

        private Pair(String piece, String ident) {
            this.piece = piece;
            this.ident = ident;
        }
    }

    private final boolean allowOutParams;
    private final String pred;
    private final boolean onlySql;
    private final List<ParamName> parameters;
    private final Map<ParamName, List<ParamCutPaste>> bindMap;
    private final Function<String, ParamName> paramByName;

    QPParser(boolean allowOutParams, String pred, boolean onlySql,
             List<ParamName> parameters, Map<ParamName, List<ParamCutPaste>> bindMap,
             Function<String, ParamName> paramByName) {
        this.allowOutParams = allowOutParams;
        this.pred = pred;
        this.onlySql = onlySql;
        this.parameters = parameters;
        this.bindMap = bindMap;
        this.paramByName = paramByName;
    }

    BindVarCutPaste getStatementCutPaste(int from, int to, String sql) throws ParseException {
        List<Pair> pairs = new ArrayList<>();
        String rest = QueryReplacer.replace(sql, null, (buf, ident) -> pairs.add(new Pair(buf, ident)), false);
        return new QPBuilder().getStatementCutPaste(from, to, pairs, rest);
    }

    private static final class ParamInfo {

        final int position;
        final ParamName id;
        final String expression;
        final String programString;
        final boolean out;

        private ParamInfo(int position, ParamName id, String expression, String programString, boolean out) {
            this.position = position;
            this.id = id;
            this.expression = expression;
            this.programString = programString;
            this.out = out;
        }
    }

    private final class QPBuilder {

        private boolean first = true;
        private final StringBuilder total = new StringBuilder();
        private final List<ParamCutPaste> pieces = new ArrayList<>();

        private int append1(String what, boolean single) {
            int whatPos;
            if (single) {
                whatPos = total.length();
                total.append(what);
            } else {
                if (onlySql) {
                    if (!first) {
                        total.append(" + ");
                    }
                    whatPos = total.length();
                    total.append(what);
                } else {
                    if (first) {
                        total.append("new sqlg3.runtime.QueryBuilder(");
                    } else {
                        total.append(".appendLit(");
                    }
                    whatPos = total.length();
                    total.append(what);
                    total.append(")");
                }
            }
            first = false;
            return whatPos;
        }

        private void appendString(String str, boolean single) throws ParseException {
            if (str.length() > 0) {
                List<String> usedParameters = new ArrayList<>();
                String parsed = QueryParser.getParameters(str, usedParameters);
                String sql = QueryReplacer.escape(parsed);
                if (usedParameters.size() > 0 && !onlySql) {
                    StringBuilder params = new StringBuilder();
                    boolean first = true;
                    List<ParamInfo> paramPositions = new ArrayList<>();
                    for (String parameter : usedParameters) {
                        if (first) {
                            first = false;
                        } else {
                            params.append(", ");
                        }
                        ParamName id = paramByName.apply(parameter);
                        parameters.add(id);
                        String expr;
                        boolean out;
                        if (parameter.startsWith(">")) {
                            if (allowOutParams) {
                                expr = parameter.substring(1);
                                out = true;
                            } else {
                                throw new ParseException("OUT parameters are not allowed for PreparedStatements", id.toString());
                            }
                        } else {
                            expr = parameter;
                            out = false;
                        }
                        String pv = (out ? "outP" : "inP") + "(" + expr + ", \"" + id + "\")";
                        paramPositions.add(new ParamInfo(params.length(), id, expr, pv, out));
                        params.append(pv);
                    }
                    String qsql = "\"" + sql + "\", ";
                    int ppos = append1(qsql + params, single);
                    for (ParamInfo pos : paramPositions) {
                        ParamName id = pos.id;
                        String pv = pos.programString;
                        int from = pred.length() + ppos + qsql.length() + pos.position;
                        int to = from + pv.length();
                        ParamCutPaste cp = new ParamCutPaste(from, to, pos.expression, pos.out);
                        cp.replaceTo = pv;
                        pieces.add(cp);
                        List<ParamCutPaste> list = bindMap.computeIfAbsent(id, k -> new ArrayList<>());
                        list.add(cp);
                    }
                } else {
                    append1("\"" + sql + "\"", single);
                }
            }
        }

        private BindVarCutPaste getStatementCutPaste(int from, int to, List<Pair> pairs, String rest) throws ParseException {
            if (pairs.isEmpty()) {
                if (rest.length() <= 0) {
                    total.append("\"\"");
                } else {
                    appendString(rest, true);
                }
            } else {
                for (Pair pair : pairs) {
                    appendString(pair.piece, false);
                    append1(pair.ident, false);
                }
                appendString(rest, false);
                if (!onlySql) {
                    total.append(".toQuery()");
                }
            }
            return new BindVarCutPaste(from, to, pred + total + (onlySql ? "" : ")"), pieces);
        }
    }
}
