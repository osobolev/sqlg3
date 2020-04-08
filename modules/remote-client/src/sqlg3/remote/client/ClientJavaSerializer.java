package sqlg3.remote.client;

import sqlg3.core.IDBCommon;
import sqlg3.remote.common.*;

import java.io.*;
import java.lang.reflect.Type;

public final class ClientJavaSerializer extends BaseJavaSerializer implements IClientSerializer {

    public HttpResult clientToServer(ReqRespProcessor processor, HttpId id, HttpCommand command,
                                     Class<? extends IDBCommon> iface, Type retType, String method, Class<?>[] paramTypes, Object[] params) throws IOException {
        boolean debug = isDebug(method);
        if (debug) {
            logClientCall(command, iface, method, retType);
        }
        return processor.process(new ReqRespConsumer() {

            public void writeToServer(OutputStream stream) throws IOException {
                try (ObjectOutputStream oos = writeData(count(stream, debug))) {
                    oos.writeObject(id);
                    oos.writeObject(command);
                    oos.writeObject(iface);
                    oos.writeObject(method);
                    oos.writeObject(paramTypes);
                    oos.writeObject(params);
                }
            }

            public HttpResult readFromServer(InputStream stream) throws IOException {
                try (ObjectInputStream ois = readData(count(stream, debug))) {
                    Object result = ois.readObject();
                    Throwable error = (Throwable) ois.readObject();
                    return new HttpResult(result, error);
                } catch (ClassNotFoundException | InvalidClassException ex) {
                    throw new UnrecoverableRemoteException(ex);
                }
            }
        });
    }
}
