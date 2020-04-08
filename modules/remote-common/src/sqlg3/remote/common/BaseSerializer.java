package sqlg3.remote.common;

import sqlg3.core.IDBCommon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class BaseSerializer<I, O> {

    protected final boolean onlyMethods;
    protected final Consumer<String> logger;
    protected final LongConsumer onRead;
    protected final LongConsumer onWrite;

    protected BaseSerializer(boolean onlyMethods, Consumer<String> logger, LongConsumer onRead, LongConsumer onWrite) {
        this.onlyMethods = onlyMethods;
        this.logger = logger;
        this.onRead = onRead;
        this.onWrite = onWrite;
    }

    protected BaseSerializer() {
        this(false, null, null, null);
    }

    protected O writeData(OutputStream os) throws IOException {
        return write(new GZIPOutputStream(os));
    }

    protected abstract O write(OutputStream os) throws IOException;

    protected I readData(InputStream is) throws IOException {
        return read(new GZIPInputStream(is));
    }

    protected abstract I read(InputStream is) throws IOException;

    protected InputStream count(InputStream is, boolean debug) throws IOException {
        if (onRead != null && debug) {
            return new CountInputStream(is, onRead);
        } else {
            return is;
        }
    }

    protected OutputStream count(OutputStream os, boolean debug) throws IOException {
        if (onWrite != null && debug) {
            return new CountOutputStream(os, onWrite);
        } else {
            return os;
        }
    }

    protected boolean isDebug(String method) {
        return onlyMethods ? method != null : true;
    }

    protected void logClientCall(HttpCommand command, Class<? extends IDBCommon> iface, String method, Type retType) {
        if (logger != null) {
            if (method != null) {
                logger.accept(iface + "." + method + ": " + retType);
            } else {
                logger.accept(command + ": " + retType);
            }
        }
    }
}
