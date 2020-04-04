package sqlg3.remote.common;

import java.io.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class BaseJavaSerializer {

    protected final Consumer<String> logger;
    protected final boolean onlyMethods;

    protected BaseJavaSerializer(Consumer<String> logger, boolean onlyMethods) {
        this.logger = logger;
        this.onlyMethods = onlyMethods;
    }

    protected BaseJavaSerializer() {
        this(null, false);
    }

    protected static ObjectOutputStream writeData(OutputStream os) throws IOException {
        return new ObjectOutputStream(new GZIPOutputStream(os));
    }

    protected static ObjectInputStream readData(InputStream is) throws IOException {
        return new ObjectInputStream(new GZIPInputStream(is));
    }

    protected InputStream count(InputStream is, boolean debug) throws IOException {
        if (logger != null && debug) {
            return new CountInputStream(is, logger);
        } else {
            return is;
        }
    }

    protected OutputStream count(OutputStream os, boolean debug) throws IOException {
        if (logger != null && debug) {
            return new CountOutputStream(os, logger);
        } else {
            return os;
        }
    }
}
