package sqlg3.preprocess;

import org.antlr.v4.runtime.Token;
import sqlg3.annotations.*;
import sqlg3.preprocess.lexer.Java8Lexer;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.*;

final class Parser extends ParserBase {

    private static final String SQL_ANNOTATION = annotationName(Sql.class);
    private static final String QUERY_ANNOTATION = annotationName(Query.class);
    private static final String BUSINESS_ANNOTATION = annotationName(Business.class);
    private static final String CHECK_PARAMS_ANNOTATION = annotationName(CheckParams.class);
    private static final String SQLG_ANNOTATION = annotationName(SQLG.class);

    private final Path file;
    private final String displayClassName;
    private final String fullClassName;

    private final List<MethodEntry> entries = new ArrayList<>();
    private final Map<ParamName, List<ParamCutPaste>> bindMap = new HashMap<>();
    private final List<ParamName> parameters = new ArrayList<>();
    private final List<CutPaste> fragments = new ArrayList<>();

    Parser(Path file, String text, String displayClassName, String fullClassName) {
        super(text);
        this.file = file;
        this.displayClassName = displayClassName;
        this.fullClassName = fullClassName;
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

    private MethodEntry parseMethodHeader(String javadoc, Set<String> annotations) throws ParseException {
        String entryName = null;
        boolean wasParen = false;
        while (!eof()) {
            Token t = get();
            int id = t.getType();
            if (id == Java8Lexer.LBRACE) {
                next();
                break;
            } else if (id == Java8Lexer.LPAREN) {
                wasParen = true;
            } else if (id == Java8Lexer.Identifier) {
                if (!wasParen) {
                    entryName = t.getText();
                }
            } else if (id == Java8Lexer.SEMI) {
                throw new ParseException("Unexpected semicolon", displayClassName + (entryName == null ? "" : "." + entryName));
            }
            next();
        }
        if (entryName == null)
            return null;

        boolean publish = annotations.contains(BUSINESS_ANNOTATION);
        return new MethodEntry(javadoc, entryName, publish);
    }

    static boolean isWhitespace(int id) {
        return id == Java8Lexer.WS || id == Java8Lexer.LINE_COMMENT || id == Java8Lexer.COMMENT;
    }

    static boolean isClassToken(Token t, Token prevNonWhitespace) {
        int id = t.getType();
        if (id == Java8Lexer.Identifier) {
            String ident = t.getText();
            return "record".equals(ident) && prevNonWhitespace != null && prevNonWhitespace.getType() == Java8Lexer.PUBLIC;
        } else if (id == Java8Lexer.CLASS) {
            return prevNonWhitespace == null || prevNonWhitespace.getType() != Java8Lexer.DOT;
        } else {
            return id == Java8Lexer.INTERFACE;
        }
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
                                            boolean allowOutParams, boolean onlySql) throws ParseException {
        if (lastSqlQuery == null)
            return null;
        AssignDescriptor desc = parseAssign();
        if (!(desc.from >= 0 && desc.to >= 0))
            return null;
        QPParser appender = new QPParser(
            allowOutParams, desc.assign + " ", onlySql, parameters, bindMap,
            paramName -> ParamName.fromLocation(fullClassName, entryName, paramName)
        );
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
                if (QUERY_ANNOTATION.equals(annotation)) {
                    parseStatement(entryName, lastSqlQuery, true, false);
                } else if (SQL_ANNOTATION.equals(annotation)) {
                    parseStatement(entryName, lastSqlQuery, true, true);
                }
                continue;
            }
            next();
        }
    }

    private void parseClass() {
        int count = 1;
        while (!eof()) {
            Token t = get();
            if (t.getType() == Java8Lexer.LBRACE) {
                count++;
            } else if (t.getType() == Java8Lexer.RBRACE) {
                count--;
                if (count <= 0) {
                    next();
                    break;
                }
            }
            next();
        }
    }

    private void parseRecord() {
        int count = 1;
        while (!eof()) {
            Token t = get();
            if (t.getType() == Java8Lexer.LPAREN) {
                count++;
            } else if (t.getType() == Java8Lexer.RPAREN) {
                count--;
                if (count <= 0) {
                    next();
                    break;
                }
            }
            next();
        }
    }

    HeaderResult parseHeader(boolean requireSQLG) {
        boolean wasClass = false;
        boolean hasSQLG = false;
        Token prevNonWhitespace = null;
        Token brace = null;
        String lastComment = null;
        while (!eof()) {
            Token t = get();
            int id = t.getType();
            if (id == Java8Lexer.LBRACE) {
                if (wasClass) {
                    brace = t;
                    next();
                    break;
                }
            } else if (id == Java8Lexer.AT) {
                String annotation = getAnnotation();
                if (SQLG_ANNOTATION.equals(annotation)) {
                    hasSQLG = true;
                }
                continue;
            } else if (id == Java8Lexer.CLASS) {
                if (isClassToken(t, prevNonWhitespace)) {
                    wasClass = true;
                }
            } else if (id == Java8Lexer.COMMENT) {
                if (!wasClass) {
                    lastComment = t.getText();
                }
            }
            if (!isWhitespace(id)) {
                prevNonWhitespace = t;
            }
            next();
        }
        if (brace == null)
            return null;
        if (hasSQLG || !requireSQLG) {
            int spaceStart = prevNonWhitespace == null ? 0 : prevNonWhitespace.getStopIndex() + 1;
            return new HeaderResult(lastComment, spaceStart, brace.getStartIndex());
        } else {
            return null;
        }
    }

    private Set<String> parseAnnotations() {
        Set<String> annotations = new HashSet<>();
        while (!eof()) {
            Token t0 = get();
            if (t0.getType() != Java8Lexer.AT)
                break;
            String annotation = getAnnotation();
            if (annotation == null)
                break;
            annotations.add(annotation);
            skipSpaces();
            if (eof())
                break;
            Token t = get();
            if (t.getType() == Java8Lexer.LPAREN) {
                next();
                int count = 1;
                while (!eof()) {
                    Token t2 = get();
                    next();
                    if (t2.getType() == Java8Lexer.LPAREN) {
                        count++;
                    } else if (t2.getType() == Java8Lexer.RPAREN) {
                        count--;
                        if (count <= 0)
                            break;
                    }
                }
                skipSpaces();
            }
        }
        return annotations;
    }

    ParseResult parseAll(boolean requireSQLG) throws ParseException {
        HeaderResult header = parseHeader(requireSQLG);
        if (header == null)
            return null;
        String lastComment = null;
        String lastIdent = null;
        String identBeforeParen = null;
        Token prevNonWhitespace = null;
        while (!eof()) {
            Token t = get();
            int id = t.getType();
            if (id == Java8Lexer.AT) {
                Set<String> annotations = parseAnnotations();
                if (annotations.contains(BUSINESS_ANNOTATION) || annotations.contains(CHECK_PARAMS_ANNOTATION)) {
                    if (!eof()) {
                        MethodEntry entry = parseMethodHeader(lastComment, annotations);
                        parseMethodBody(entry == null ? identBeforeParen : entry.methodToCall);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                }
                lastComment = null;
                identBeforeParen = null;
                prevNonWhitespace = null;
                continue;
            } else if (id == Java8Lexer.RBRACE) {
                next();
                break;
            } else if (id == Java8Lexer.LBRACE) {
                next();
                parseMethodBody(identBeforeParen);
                lastComment = null;
                identBeforeParen = null;
                prevNonWhitespace = null;
                continue;
            } else if (id == Java8Lexer.LPAREN) {
                identBeforeParen = lastIdent;
            } else if (id == Java8Lexer.Identifier) {
                if (isClassToken(t, prevNonWhitespace)) {
                    next();
                    skipSpaces();
                    if (!eof()) {
                        Token name = get();
                        if (name.getType() == Java8Lexer.Identifier) {
                            Token lpar = skipTo(Java8Lexer.LPAREN);
                            if (lpar != null && !eof()) {
                                parseRecord();
                            }
                        }
                    }
                    prevNonWhitespace = null;
                    continue;
                }
                lastIdent = t.getText();
            } else if (id == Java8Lexer.COMMENT) {
                lastComment = t.getText();
            } else if (id == Java8Lexer.CLASS || id == Java8Lexer.INTERFACE) {
                if (isClassToken(t, prevNonWhitespace)) {
                    next();
                    skipSpaces();
                    if (!eof()) {
                        Token name = get();
                        if (name.getType() == Java8Lexer.Identifier) {
                            Token lbrace = skipTo(Java8Lexer.LBRACE);
                            if (lbrace != null && !eof()) {
                                parseClass();
                            }
                        }
                    }
                    prevNonWhitespace = null;
                    continue;
                }
            }
            if (!isWhitespace(id)) {
                prevNonWhitespace = t;
            }
            next();
        }
        return new ParseResult(file, text, header, entries, bindMap, parameters, fragments);
    }
}
