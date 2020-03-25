package sqlg3.remote.common;

import sqlg3.core.SQLGLogger;

import java.io.IOException;
import java.io.OutputStream;

public final class CountOutputStream extends OutputStream {

    private final OutputStream os;
    private final SQLGLogger logger;

    private long count = 0;
    private boolean closed = false;

    public CountOutputStream(OutputStream os, SQLGLogger logger) {
        this.os = os;
        this.logger = logger;
    }

    public void write(int b) throws IOException {
        os.write(b);
        count++;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
        count += len;
    }

    public void flush() throws IOException {
        os.flush();
    }

    public void close() throws IOException {
        if (!closed) {
            closed = true;
            if (logger != null) {
                logger.info("WRITE: " + count);
            }
        }
        os.close();
    }

    public long getCount() {
        return count;
    }
}
