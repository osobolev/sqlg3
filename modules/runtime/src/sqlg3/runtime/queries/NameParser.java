package sqlg3.runtime.queries;

class NameParser {

    protected String source;
    protected int sourceLength;
    protected int pos;

    private String name;
    private boolean isQuote;

    protected NameParser() {
    }

    protected NameParser(String str) {
        init(str);
    }

    private void init(String str) {
        this.source = str;
        this.pos = 0;
        this.sourceLength = str.length();
    }

    protected final String getIdent() {
        int pos1 = pos;
        while (pos < sourceLength) {
            char ch = source.charAt(pos);
            if (!isIdentChar(ch)) {
                break;
            }
            pos++;
        }
        return source.substring(pos1, pos);
    }

    protected final String getQuoteIdent() {
        int pos1 = ++pos;
        while (pos < sourceLength) {
            char ch = source.charAt(pos);
            if (ch == '"') {
                return source.substring(pos1, pos++);
            }
            pos++;
        }
        return source.substring(pos1, pos);
    }

    protected final boolean skipSpaces() {
        boolean containsEoln = false;
        while (pos < sourceLength) {
            char ch = source.charAt(pos);
            if (!isWhiteSpace(ch))
                break;
            if (ch == '\r' || ch == '\n')
                containsEoln = true;
            pos++;
        }
        return containsEoln;
    }

    protected final boolean readIdent() {
        if (pos >= sourceLength)
            return false;
        char ch = source.charAt(pos);
        if (ch == '"') {
            name = getQuoteIdent();
            isQuote = true;
            return true;
        } else if (isIdentChar(ch)) {
            name = getIdent();
            isQuote = false;
            return true;
        } else {
            return false;
        }
    }

    static boolean isIdentChar(char ch) {
        return ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z'
               || ch >= 'a' && ch <= 'z' || ch == '_' || ch == '#'
               || ch == '$';
    }

    static boolean isWhiteSpace(char ch) {
        return ch <= ' ';
    }

    protected final String getIdentString() {
        if (isQuote) {
            return '"' + name + '"';
        } else {
            return name;
        }
    }

    protected final String getCanonicIdent() {
        if (isQuote) {
            return name;
        } else {
            return name.toUpperCase();
        }
    }
}
