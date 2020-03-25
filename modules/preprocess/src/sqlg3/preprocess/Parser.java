package sqlg3.preprocess;

import org.antlr.v4.runtime.Token;
import sqlg3.annotations.*;
import sqlg3.preprocess.lexer.Java8Lexer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

final class Parser extends ParserBase {

    private static final String SQL_ANNOTATION = annotationName(Sql.class);
    private static final String STATEMENT_ANNOTATION = annotationName(Prepare.class);
    private static final String KEY_STATEMENT_ANNOTATION = annotationName(PrepareKey.class);
    private static final String CALL_ANNOTATION = annotationName(Call.class);
    private static final String BUSINESS_ANNOTATION = annotationName(Business.class);
    private static final String CHECK_PARAMS_ANNOTATION = annotationName(CheckParams.class);
    private static final String SQLG_ANNOTATION = annotationName(SQLG.class);

    private final String displayClassName;
    private final String fullClassName;
    private final Map<String, RowTypeCutPaste> rowTypeMap;

    private final List<MethodEntry> entries = new ArrayList<>();
    private final Map<String, List<ParamCutPaste>> bindMap = new HashMap<>();
    private final List<String> parameters = new ArrayList<>();
    private final List<CutPaste> fragments = new ArrayList<>();

    Parser(String text, String displayClassName, String fullClassName, Map<String, RowTypeCutPaste> rowTypeMap) throws IOException {
        super(text);
        this.displayClassName = displayClassName;
        this.fullClassName = fullClassName;
        this.rowTypeMap = rowTypeMap;
    }

    private static String annotationName(Class<? extends Annotation> cls) {
        String name = cls.getName();
        int p = name.lastIndexOf('.');
        if (p < 0) {
            return name;
        } else {
            return name.substring(p + 1);
        }
    }

    private static String extractQuery(String comment) {
        StringBuilder buf = new StringBuilder();
        try (Scanner rdr = new Scanner(comment.substring(2, comment.length() - 2))) {
            while (rdr.hasNextLine()) {
                String s = rdr.nextLine().trim();
                while (s.startsWith("*")) {
                    s = s.substring(1).trim();
                }
                if (s.length() > 0) {
                    if (buf.length() > 0) {
                        buf.append('\n');
                    }
                    buf.append(s);
                }
            }
        }
        return buf.toString();
    }

    private Token skipTo(int tokenType) {
        while (!eof()) {
            Token t = get();
            if (t.getType() == tokenType) {
                next();
                return t;
            }
            next();
        }
        return null;
    }

    private MethodEntry parseMethodHeader(String javadoc, String annotation) throws ParseException {
        List<String> params = new ArrayList<>();
        String lastIdent = null;
        String entryName = null;
        boolean wasParen = false;
        boolean parenClosed = false;
        int parenCount = 0;
        int genCount = 0;
        while (!eof()) {
            Token t = get();
            int id = t.getType();
            if (id == Java8Lexer.LBRACE) {
                next();
                break;
            } else if (id == Java8Lexer.LPAREN) {
                if (wasParen) {
                    parenCount++;
                } else {
                    wasParen = true;
                    lastIdent = null;
                }
            } else if (id == Java8Lexer.LT) {
                if (wasParen) {
                    genCount++;
                }
            } else if (id == Java8Lexer.COMMA) {
                if (wasParen && !parenClosed) {
                    if (parenCount <= 0 && genCount <= 0) {
                        if (lastIdent != null) {
                            params.add(lastIdent);
                        }
                    }
                }
            } else if (id == Java8Lexer.RPAREN) {
                if (wasParen && !parenClosed) {
                    if (parenCount <= 0) {
                        if (lastIdent != null) {
                            params.add(lastIdent);
                        }
                        parenClosed = true;
                    } else {
                        parenCount--;
                    }
                }
            } else if (id == Java8Lexer.GT) {
                if (wasParen && !parenClosed) {
                    if (genCount > 0) {
                        genCount--;
                    }
                }
            } else if (id == Java8Lexer.Identifier) {
                lastIdent = t.getText();
                if (!wasParen) {
                    entryName = lastIdent;
                }
            } else if (id == Java8Lexer.SEMI) {
                throw new ParseException("Unexpected semicolon", displayClassName + (entryName == null ? "" : "." + entryName));
            }
            next();
        }
        if (entryName == null)
            return null;

        boolean publish = !CHECK_PARAMS_ANNOTATION.equals(annotation);
        return new MethodEntry(javadoc, entryName, params, publish);
    }

    private static boolean isWhitespace(int id) {
        return id == Java8Lexer.WS || id == Java8Lexer.LINE_COMMENT || id == Java8Lexer.COMMENT;
    }

    private static final class AssignDescriptor {

        final int from;
        final int to;
        final String varName;
        final String assign;

        AssignDescriptor(int from, int to, String varName, String assign) {
            this.from = from;
            this.to = to;
            this.varName = varName;
            this.assign = assign;
        }
    }

    private AssignDescriptor parseAssign() {
        int from = -1;
        int to = -1;
        int identCount = 0;
        String assign = "";
        String varName = null;
        while (!eof()) {
            Token t = get();
            if (t.getType() == Java8Lexer.ASSIGN) {
                next();
                if (!eof()) {
                    from = get().getStartIndex();
                }
                break;
            } else if (t.getType() == Java8Lexer.SEMI) {
                from = to = t.getStartIndex();
                assign = " =";
                break;
            } else if (t.getType() == Java8Lexer.Identifier) {
                identCount++;
                if (identCount == 2) {
                    varName = t.getText();
                }
            }
            next();
        }
        if (to < 0) {
            Token semi = skipTo(Java8Lexer.SEMI);
            if (semi != null) {
                to = semi.getStartIndex();
            }
        }
        return new AssignDescriptor(from, to, varName, assign);
    }

    private AssignDescriptor parseStatement(String entryName, String lastSqlQuery,
                                            boolean allowOutParams, String whatToCall, String addParameter) throws ParseException {
        if (lastSqlQuery == null)
            return null;
        AssignDescriptor desc = parseAssign();
        if (!(desc.from >= 0 && desc.to >= 0))
            return null;
        String pred = desc.assign + " " + whatToCall + "(" + addParameter;
        String location = fullClassName + (entryName == null ? "" : "." + entryName);
        QPParser appender = new QPParser(location, allowOutParams, pred, parameters, bindMap);
        BindVarCutPaste cp = appender.getStatementCutPaste(desc.from, desc.to, lastSqlQuery);
        fragments.add(cp);
        return desc;
    }

    private void skipSpaces() {
        while (!eof()) {
            if (isWhitespace(get().getType())) {
                next();
            } else {
                break;
            }
        }
    }

    private String getAnnotation() {
        next();
        skipSpaces();
        if (eof())
            return null;
        Token ann = get();
        if (ann.getType() == Java8Lexer.Identifier) {
            String annotation = ann.getText();
            next();
            return annotation;
        } else {
            return null;
        }
    }

    private String parseAutoKeys() {
        skipSpaces();
        String auto = null;
        if (!eof() && get().getType() == Java8Lexer.RPAREN) {
            next();
            skipSpaces();
            if (!eof()) {
                Token t = get();
                if (t.getType() == Java8Lexer.StringLiteral) {
                    auto = t.getText().substring(1, t.getText().length() - 1);
                    next();
                }
            }
            skipTo(Java8Lexer.RPAREN);
        }
        return auto;
    }

    private void parseMethodBody(String entryName) throws ParseException {
        int count = 1;
        String lastSqlQuery = null;
        int lastSqlQueryCount = -1;
        while (!eof()) {
            Token t = get();
            int id = t.getType();
            if (id == Java8Lexer.LBRACE) {
                count++;
            } else if (id == Java8Lexer.RBRACE) {
                count--;
                if (lastSqlQueryCount >= 0 && count < lastSqlQueryCount) {
                    lastSqlQuery = null;
                    lastSqlQueryCount = -1;
                }
                if (count <= 0) {
                    next();
                    break;
                }
            } else if (id == Java8Lexer.COMMENT) {
                lastSqlQuery = extractQuery(t.getText());
                lastSqlQueryCount = count;
            } else if (id == Java8Lexer.AT) {
                String annotation = getAnnotation();
                if (SQL_ANNOTATION.equals(annotation)) {
                    parseStatement(entryName, lastSqlQuery, false, "createQueryPiece", "");
                } else if (STATEMENT_ANNOTATION.equals(annotation)) {
                    parseStatement(entryName, lastSqlQuery, false, "prepareStatement", "");
                } else if (KEY_STATEMENT_ANNOTATION.equals(annotation)) {
                    String auto = parseAutoKeys();
                    String autoKeys;
                    if (auto != null) {
                        StringBuilder buf = new StringBuilder();
                        StringTokenizer tok = new StringTokenizer(auto, ",");
                        while (tok.hasMoreTokens()) {
                            String col = tok.nextToken().trim();
                            if (buf.length() > 0) {
                                buf.append(", ");
                            }
                            buf.append("\"" + col + "\"");
                        }
                        autoKeys = "new String[] {" + buf + "}";
                    } else {
                        autoKeys = "ALL_KEYS";
                    }
                    parseStatement(entryName, lastSqlQuery, false, "prepareStatementKey", autoKeys + ", ");
                } else if (CALL_ANNOTATION.equals(annotation)) {
                    parseStatement(entryName, lastSqlQuery, true, "executeCall", "");
                }
                continue;
            }
            next();
        }
    }

    private void parseClass(String nestedClassName) {
        int from = get().getStartIndex();
        int count = 1;
        int to = -1;
        while (!eof()) {
            Token t = get();
            if (t.getType() == Java8Lexer.LBRACE) {
                count++;
            } else if (t.getType() == Java8Lexer.RBRACE) {
                count--;
                if (count <= 0) {
                    to = t.getStartIndex();
                    next();
                    break;
                }
            }
            next();
        }
        if (from >= 0 && to >= 0) {
            RowTypeCutPaste rowType = new RowTypeCutPaste(from, to, nestedClassName);
            fragments.add(rowType);
            rowTypeMap.put(fullClassName + "." + nestedClassName, rowType);
        }
    }

    private boolean parseHeader() {
        boolean wasClass = false;
        boolean needsProcessing = false;
        while (!eof()) {
            Token t = get();
            int id = t.getType();
            if (id == Java8Lexer.LBRACE) {
                if (wasClass) {
                    next();
                    break;
                }
            } else if (id == Java8Lexer.AT) {
                String annotation = getAnnotation();
                if (SQLG_ANNOTATION.equals(annotation)) {
                    needsProcessing = true;
                }
                continue;
            } else if (id == Java8Lexer.CLASS) {
                wasClass = true;
            }
            next();
        }
        return needsProcessing;
    }

    ParseResult parseAll() throws ParseException {
        if (!parseHeader())
            return null;
        String lastComment = null;
        String lastIdent = null;
        String identBeforeParen = null;
        Token prevDot = null;
        while (!eof()) {
            Token t = get();
            int id = t.getType();
            if (id == Java8Lexer.AT) {
                String annotation = getAnnotation();
                if (BUSINESS_ANNOTATION.equals(annotation) || CHECK_PARAMS_ANNOTATION.equals(annotation)) {
                    if (!eof()) {
                        MethodEntry entry = parseMethodHeader(lastComment, annotation);
                        parseMethodBody(entry == null ? identBeforeParen : entry.methodToCall);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                }
                lastComment = null;
                identBeforeParen = null;
                prevDot = null;
                continue;
            } else if (id == Java8Lexer.RBRACE) {
                next();
                break;
            } else if (id == Java8Lexer.LBRACE) {
                next();
                parseMethodBody(identBeforeParen);
                lastComment = null;
                identBeforeParen = null;
                prevDot = null;
                continue;
            } else if (id == Java8Lexer.LPAREN) {
                identBeforeParen = lastIdent;
            } else if (id == Java8Lexer.Identifier) {
                lastIdent = t.getText();
            } else if (id == Java8Lexer.COMMENT) {
                lastComment = t.getText();
            } else if (id == Java8Lexer.CLASS) {
                next();
                if (prevDot == null) {
                    skipSpaces();
                    if (!eof()) {
                        Token name = get();
                        if (name.getType() == Java8Lexer.Identifier) {
                            String nestedClassName = name.getText();
                            Token lbrace = skipTo(Java8Lexer.LBRACE);
                            if (lbrace != null && !eof()) {
                                parseClass(nestedClassName);
                            }
                        }
                    }
                }
                prevDot = null;
                continue;
            }
            if (id == Java8Lexer.DOT) {
                prevDot = t;
            } else if (!isWhitespace(id)) {
                prevDot = null;
            }
            next();
        }
        return new ParseResult(text, entries, bindMap, parameters, fragments);
    }
}
