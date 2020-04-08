package sqlg3.remote.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.LongConsumer;

public final class CountOutputStream extends OutputStream {

    private final OutputStream os;
    private final LongConsumer countConsumer;

    private long count = 0;
    private boolean closed = false;

    public CountOutputStream(OutputStream os, LongConsumer countConsumer) {
        this.os = os;
        this.countConsumer = countConsumer;
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
            if (countConsumer != null) {
                countConsumer.accept(count);
            }
        }
        os.close();
    }

    public long getCount() {
        return count;
    }
}
