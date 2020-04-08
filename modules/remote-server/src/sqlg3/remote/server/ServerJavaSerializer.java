package sqlg3.remote.server;

import sqlg3.core.IDBCommon;
import sqlg3.remote.common.BaseJavaSerializer;
import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.UnrecoverableRemoteException;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class ServerJavaSerializer extends BaseJavaSerializer implements IServerSerializer {

    public ServerJavaSerializer(boolean onlyMethods, Consumer<String> logger, LongConsumer onRead, LongConsumer onWrite) {
        super(onlyMethods, logger, onRead, onWrite);
    }

    public ServerJavaSerializer() {
    }

    @SuppressWarnings("unchecked")
    public void serverToClient(InputStream is, ServerCall call, OutputStream os) throws IOException {
        HttpId id;
        HttpCommand command;
        Class<? extends IDBCommon> iface;
        String method;
        Class<?>[] paramTypes;
        Object[] params;
        try (ObjectInputStream ois = readData(count(is, true))) {
            id = (HttpId) ois.readObject();
            command = (HttpCommand) ois.readObject();
            iface = (Class<? extends IDBCommon>) ois.readObject();
            method = (String) ois.readObject();
            paramTypes = (Class<?>[]) ois.readObject();
            params = (Object[]) ois.readObject();
        } catch (ClassNotFoundException | InvalidClassException ex) {
            throw new UnrecoverableRemoteException(ex);
        }
        Object result = null;
        Throwable error = null;
        try {
            result = call.call(id, command, iface, method, paramTypes, params);
        } catch (Throwable ex) {
            error = ex;
        }
        writeResponse(os, result, error, method);
    }

    private void writeResponse(OutputStream os, Object result, Throwable error, String method) throws IOException {
        boolean debug = isDebug(method);
        try (ObjectOutputStream oos = writeData(count(os, debug))) {
            oos.writeObject(result);
            oos.writeObject(error);
        }
    }

    public void sendError(OutputStream os, Throwable error) throws IOException {
        try {
            writeResponse(os, null, error, null);
        } finally {
            os.close();
        }
    }
}
