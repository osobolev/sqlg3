package sqlg3.remote.common;

import sqlg3.core.IDBCommon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public abstract class BaseSerializer<I, O> {

    /**
     * If true, logger and onRead/onWrite are called only for remote method calls (and not for other queries like ping)
     */
    public boolean onlyMethods;
    public Consumer<String> logger;
    public LongConsumer onRead;
    public LongConsumer onWrite;
    public boolean zip = true;

    protected O writeData(OutputStream os) throws IOException {
        return write(zip ? new DeflaterOutputStream(os) : os);
    }

    protected abstract O write(OutputStream os) throws IOException;

    protected I readData(InputStream is) throws IOException {
        return read(zip ? new InflaterInputStream(is) : is);
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
