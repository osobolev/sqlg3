package sqlg3.runtime.queries;

final class SelectTokenizer extends NameParser {

    private String param;
    private String value;

    SelectTokenizer(String str) {
        super(str);
    }

    TokenKind getToken() {
        if (pos < sourceLength) {
            char ch = source.charAt(pos);

            switch (ch) {
            case '\'':
                {
                    StringBuilder buf = new StringBuilder();
                    buf.append(ch);
                    pos++;
                    while (pos < sourceLength) {
                        ch = source.charAt(pos++);
                        buf.append(ch);
                        if (ch == '\'') {
                            if (pos < sourceLength) {
                                ch = source.charAt(pos);
                                if (ch == '\'') {
                                    buf.append(ch);
                                    pos++;
                                    continue;
                                }
                            }
                            break;
                        }
                    }
                    value = buf.toString();
                    return TokenKind.R_QUOTE;
                }
            case '"':
                {
                    readIdent();
                    return TokenKind.R_ID;
                }
            case ':':
                {
                    if (pos > 0 && source.charAt(pos - 1) == ':') {
                        pos++;
                        value = ":";
                        return TokenKind.R_CHAR;
                    }
                    int pos1 = ++pos;
                    skipSpaces();
                    if (pos >= sourceLength) {
                        pos = pos1;
                        value = ":";
                        return TokenKind.R_CHAR;
                    }
                    ch = source.charAt(pos);
                    if (isIdentChar(ch)) {
                        param = getIdent();
                        value = "?";
                        return TokenKind.R_PARAM;
                    } else if (ch == '{') {
                        int posb = pos;
                        int pose = sourceLength;
                        while (pos < sourceLength) {
                            if (source.charAt(pos) == '}') {
                                pose = pos;
                                pos++;
                                break;
                            }
                            pos++;
                        }
                        param = source.substring(posb + 1, pose);
                        value = "?";
                        return TokenKind.R_PARAM;
                    } else if (ch == '>' && pos + 1 < sourceLength && isIdentChar(source.charAt(pos + 1))) {
                        pos++;
                        param = ">" + getIdent();
                        value = "?";
                        return TokenKind.R_PARAM;
                    } else {
                        pos = pos1;
                        value = ":";
                        return TokenKind.R_CHAR;
                    }
                }
            default:
                if (isIdentChar(ch)) {
                    readIdent();
                    return TokenKind.R_ID;
                } else if (isWhiteSpace(ch)) {
                    boolean containsEoln = skipSpaces();
                    value = containsEoln ? "\n" : " ";
                    return TokenKind.R_WS;
                } else {
                    value = String.valueOf(ch);
                    pos++;
                    return TokenKind.R_CHAR;
                }
            }
        }
        return TokenKind.R_THEEND;
    }

    String getParam() {
        return param;
    }

    String getValue(TokenKind id) {
        if (id == TokenKind.R_ID) {
            return getIdentString();
        } else {
            return value;
        }
    }

    String getValue() {
        return value;
    }

/*
    public static void main(String[] args) {
        SelectTokenizer tok = new SelectTokenizer(
            "SELECT \"X.Y\".\"Z\" FROM \"X.Y\" WHERE T='''ABBA''' AND P=:{x+1}"
        );
        while (true) {
            int id = tok.getToken();
            StringBuilder buf = new StringBuilder();
            tok.getValue(id, buf);
            if (id == R_THEEND)
                break;
            System.out.println(id + ": <" + buf + ">");
        }
    }
*/
}
