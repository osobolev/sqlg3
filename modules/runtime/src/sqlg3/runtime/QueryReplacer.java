package sqlg3.runtime;

/**
 * Utility for replacing substrings like &amp;identifier to substitution strings.
 */
public final class QueryReplacer {

    public interface Appender {

        void append(String buf, String ident);
    }

    public interface Replacer {

        String getReplacement(String ident);
    }

    public static String replace(String sql, Replacer replacer, boolean doEscaping) {
        return replace(sql, replacer, null, doEscaping);
    }

    public static String replace(String sql, Replacer replacer, Appender appender, boolean doEscaping) {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < sql.length()) {
            char ch = sql.charAt(i);
            if (ch == '&') {
                i++;
                boolean replace = false;
                int j = i + 1;
                if (i < sql.length()) {
                    ch = sql.charAt(i);
                    if (Character.isJavaIdentifierStart(ch)) {
                        while (j < sql.length()) {
                            ch = sql.charAt(j);
                            if (!Character.isJavaIdentifierPart(ch))
                                break;
                            j++;
                        }
                        String id = sql.substring(i, j);
                        if (j < sql.length() && sql.charAt(j) == '.') {
                            j++;
                        }
                        if (replacer != null) {
                            String replacement = replacer.getReplacement(id);
                            if (replacement != null) {
                                replace = true;
                                buf.append(replacement);
                            }
                        }
                        if (appender != null) {
                            replace = true;
                            appender.append(buf.toString(), id);
                            buf.setLength(0);
                        }
                    }
                }
                if (!replace) {
                    buf.append('&');
                } else {
                    i = j;
                }
/*
            } else if (ch == '\'' || ch == '"') {
                buf.append(ch);
                i++;
                char quote = ch;
                while (i < sql.length()) {
                    ch = sql.charAt(i);
                    buf.append(ch);
                    if (ch == quote) {
                        if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
                            buf.append(quote);
                            i++;
                        } else {
                            i++;
                            break;
                        }
                    }
                    i++;
                }
*/
            } else {
                if (doEscaping) {
                    escape(buf, ch);
                } else {
                    buf.append(ch);
                }
                i++;
            }
        }
        return buf.toString();
    }

    public static void escape(StringBuilder buf, char ch) {
        if (ch == '"') {
            buf.append("\\\"");
        } else if (ch == '\\') {
            buf.append("\\\\");
        } else if (ch == '\r') {
            buf.append("\\r");
        } else if (ch == '\n') {
            buf.append("\\n");
        } else if (ch == '\t') {
            buf.append("\\t");
        } else if (ch < ' ') {
            buf.append("\\" + Integer.toOctalString(ch));
        } else {
            buf.append(ch);
        }
    }

    public static String escape(String str) {
        StringBuilder buf = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            escape(buf, str.charAt(i));
        }
        return buf.toString();
    }
}
