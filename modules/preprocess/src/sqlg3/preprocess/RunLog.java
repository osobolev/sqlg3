package sqlg3.preprocess;

import java.io.IOException;
import java.io.PrintStream;

final class RunLog implements AutoCloseable {

    interface LogSupplier {

        PrintStream getLog() throws IOException;
    }

    private final boolean close;
    private LogSupplier supplier;

    private PrintStream ps;

    RunLog(boolean close, LogSupplier supplier) {
        this.close = close;
        this.supplier = supplier;
    }

    PrintStream getLog() throws IOException {
        if (ps == null && supplier != null) {
            ps = supplier.getLog();
            supplier = null;
        }
        return ps;
    }

    @Override
    public void close() {
        if (close && ps != null) {
            ps.close();
        }
    }
}
