package sqlg3.preprocess;

import java.io.IOException;
import java.io.Writer;

/**
 * Converts '\n' to system line separator in character stream.
 */
public final class EolnWriter extends Writer {

    private static final char EOLN = '\n';
    private static final char[] eoln = System.getProperty("line.separator").toCharArray();

    private final Writer out;

    public EolnWriter(Writer out) {
        this.out = out;
    }

    public void write(int c) throws IOException {
        if (c == EOLN) {
            for (char ch : eoln)
                out.write(ch);
        } else {
            out.write(c);
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = off, j = 0; j < len; i++, j++) {
            if (cbuf[i] == EOLN) {
                for (char ch : eoln)
                    out.write(ch);
            } else {
                out.write(cbuf[i]);
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
