package sqlg3.runtime.queries;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class QueryParser {

    public static final String SPECIAL = "S$$";

    private QueryParser() {
    }

    private abstract static class TokenMatch {

        abstract boolean matchToken(TokenKind id, String currValue, String lastValue,
                                    StringBuilder synthQuery);
    }

    private static String parseQueryAny(String statement, List<String> usedParameters, TokenMatch match) {
        StringBuilder synthQuery = new StringBuilder();
        SelectTokenizer st = new SelectTokenizer(statement);
        String lastValue = null;
        while (true) {
            TokenKind id = st.getToken();
            if (id == TokenKind.R_THEEND)
                break;
            String currValue = st.getValue(id);
            if (!match.matchToken(id, currValue, lastValue, synthQuery)) {
                lastValue = null;
                continue;
            }
            if (lastValue != null) {
                synthQuery.append(lastValue);
            }
            if (id == TokenKind.R_PARAM) {
                if (usedParameters != null) {
                    usedParameters.add(st.getParam());
                }
                lastValue = "?";
            } else {
                lastValue = currValue;
            }
        }
        if (lastValue != null) {
            synthQuery.append(lastValue);
        }
        return synthQuery.toString();
    }

    public static final class Range {

        private static final char SEPARATOR = '$';

        public final String name;
        public final int from;
        public final int to;

        private Range(String name, int from, int to) {
            this.name = name;
            this.from = from;
            this.to = to;
        }

        public String toString() {
            return name + ": " + from + "-" + to;
        }

        public static Range parseRange(String column) {
            try {
                int p1 = column.lastIndexOf(SEPARATOR);
                if (p1 >= 0) {
                    int p2 = column.lastIndexOf(SEPARATOR, p1 - 1);
                    if (p2 < p1) {
                        String name = column.substring(0, p2);
                        int from = Integer.parseInt(column.substring(p2 + 1, p1));
                        int to = Integer.parseInt(column.substring(p1 + 1));
                        return new Range(name, from, to);
                    }
                }
            } catch (NumberFormatException nfex) {
                // ignore
            }
            return null;
        }
    }

    public static String parseQuery(String statement) {
        TokenMatch tokenMatch = new TokenMatch() {

            private int brackets = 0;
            private int columnCount = 1;
            private final List<Range> specials = new ArrayList<>();

            boolean matchToken(TokenKind id, String currValue, String lastValue, StringBuilder synthQuery) {
                if (id == TokenKind.R_CHAR) {
                    if ("{".equals(currValue)) {
                        if (brackets == 0 && columnCount > 0) {
                            specials.add(new Range(lastValue, columnCount, 0));
                        }
                        return false;
                    } else if ("}".equals(currValue)) {
                        if (brackets == 0 && specials.size() > 0) {
                            int lastIndex = specials.size() - 1;
                            Range range = specials.get(lastIndex);
                            if (range.to <= 0) {
                                specials.set(lastIndex, new Range(range.name, range.from, columnCount));
                            }
                        }
                        if (lastValue != null) {
                            synthQuery.append(lastValue);
                        }
                        return false;
                    } else if ("(".equals(currValue)) {
                        brackets++;
                    } else if (")".equals(currValue)) {
                        brackets--;
                    } else if (",".equals(currValue)) {
                        if (brackets == 0 && columnCount > 0) {
                            columnCount++;
                        }
                    }
                } else if (id == TokenKind.R_ID) {
                    if (brackets == 0 && "FROM".equalsIgnoreCase(currValue)) {
                        for (Range range : specials) {
                            if (range.to > 0) {
                                synthQuery.append(", '' \"" + SPECIAL + range.name + "$" + range.from + "$" + range.to + "\"");
                            }
                        }
                        if (lastValue != null) {
                            synthQuery.append(lastValue);
                        }
                        columnCount = 0;
                        synthQuery.append(currValue);
                        return false;
                    }
                }
                return true;
            }
        };
        return parseQueryAny(statement, null, tokenMatch);
    }

    public static String getParameters(String statement, List<String> usedParameters) {
        TokenMatch tokenMatch = new TokenMatch() {
            boolean matchToken(TokenKind id, String currValue, String lastValue, StringBuilder synthQuery) {
                return true;
            }
        };
        return parseQueryAny(statement, usedParameters, tokenMatch);
    }

    public static String unparseQuery(String statement) {
        StringBuilder query = new StringBuilder();
        SelectTokenizer st = new SelectTokenizer(statement);
        int paramCount = 1;
        while (true) {
            TokenKind id = st.getToken();
            if (id == TokenKind.R_THEEND)
                break;
            if (id == TokenKind.R_PARAM) {
                query.append(':').append(st.getParam());
            } else if (id == TokenKind.R_CHAR && "?".equals(st.getValue())) {
                query.append(':').append(paramCount++);
            } else {
                query.append(st.getValue(id));
            }
        }
        return query.toString();
    }

    public static String[] parseIdent(String name, DatabaseMetaData meta) throws SQLException {
        SelectTokenizer st = new SelectTokenizer(name);
        while (true) {
            TokenKind t1 = st.getToken();
            if (t1 == TokenKind.R_WS)
                continue;
            if (t1 == TokenKind.R_ID)
                break;
            return null;
        }
        Function<String, String> canonicalizer = getCanonicalizer(meta);
        String name1 = st.getCanonicIdent(canonicalizer);
        while (true) {
            TokenKind t2 = st.getToken();
            if (t2 == TokenKind.R_THEEND)
                return new String[] {name1};
            if (t2 == TokenKind.R_WS)
                continue;
            if (t2 == TokenKind.R_CHAR && ".".equals(st.getValue()))
                break;
            return null;
        }
        while (true) {
            TokenKind t3 = st.getToken();
            if (t3 == TokenKind.R_WS)
                continue;
            if (t3 == TokenKind.R_ID)
                break;
            return null;
        }
        String name2 = st.getCanonicIdent(canonicalizer);
        while (true) {
            TokenKind t4 = st.getToken();
            if (t4 == TokenKind.R_THEEND)
                return new String[] {name1, name2};
            if (t4 == TokenKind.R_WS)
                continue;
            if (t4 == TokenKind.R_CHAR && ".".equals(st.getValue()))
                break;
            return null;
        }
        while (true) {
            TokenKind t5 = st.getToken();
            if (t5 == TokenKind.R_WS)
                continue;
            if (t5 == TokenKind.R_ID)
                break;
            return null;
        }
        String name3 = st.getCanonicIdent(canonicalizer);
        while (true) {
            TokenKind t6 = st.getToken();
            if (t6 == TokenKind.R_THEEND)
                return new String[] {name1, name2, name3};
            if (t6 == TokenKind.R_WS)
                continue;
            return null;
        }
    }

    public static Function<String, String> getCanonicalizer(DatabaseMetaData meta) throws SQLException {
        if (meta.storesUpperCaseIdentifiers()) {
            return String::toUpperCase;
        } else if (meta.storesLowerCaseIdentifiers()) {
            return String::toLowerCase;
        } else {
            return Function.identity();
        }
    }
}
