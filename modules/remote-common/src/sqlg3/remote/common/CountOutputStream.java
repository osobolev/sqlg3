package sqlg3.remote.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public final class CountOutputStream extends OutputStream {

    private final OutputStream os;
    private final Consumer<String> logger;

    private long count = 0;
    private boolean closed = false;

    public CountOutputStream(OutputStream os, Consumer<String> logger) {
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
                logger.accept("WRITE: " + count);
            }
        }
        os.close();
    }

    public long getCount() {
        return count;
    }
}
