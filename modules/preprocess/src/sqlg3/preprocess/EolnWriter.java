package sqlg3.preprocess;

import java.io.IOException;
import java.io.Writer;

/**
 * Converts '\n' to system line separator in character stream.
 */
public final class EolnWriter extends Writer {

    private static final char EOLN_IN = '\n';
    private static final char[] EOLN_OUT = System.getProperty("line.separator").toCharArray();

    private final Writer out;

    public EolnWriter(Writer out) {
        this.out = out;
    }

    public void write(int c) throws IOException {
        if (c == EOLN_IN) {
            out.write(EOLN_OUT);
        } else {
            out.write(c);
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = off, j = 0; j < len; i++, j++) {
            char c = cbuf[i];
            if (c == EOLN_IN) {
                out.write(EOLN_OUT);
            } else {
                out.write(c);
            }
        }
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }
}
