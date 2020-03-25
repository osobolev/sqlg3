package sqlg3.remote.client;

import sqlg3.core.IDBCommon;
import sqlg3.remote.common.*;

import java.io.*;
import java.lang.reflect.Type;

public final class ClientJavaSerializer extends BaseJavaSerializer implements IClientSerializer {

    public ClientJavaSerializer(SQLGLogger logger, boolean onlyMethods) {
        super(logger, onlyMethods);
    }

    public ClientJavaSerializer() {
    }

    public HttpResult clientToServer(ReqRespProcessor processor, HttpId id, HttpCommand command,
                                     Class<? extends IDBCommon> iface, Type retType, String method, Class<?>[] paramTypes, Object[] params) throws IOException {
        boolean debug = onlyMethods ? method != null : true;
        if (logger != null && debug) {
            if (method != null) {
                logger.info(iface + "." + method + ": " + retType);
            } else {
                logger.info(command + ": " + retType);
            }
        }
        return processor.process(new ReqRespConsumer() {

            public void writeToServer(OutputStream stream) throws IOException {
                OutputStream os = count(stream, debug);
                try (ObjectOutputStream oos = writeData(os)) {
                    oos.writeObject(id);
                    oos.writeObject(command);
                    oos.writeObject(iface);
                    oos.writeObject(method);
                    oos.writeObject(paramTypes);
                    oos.writeObject(params);
                }
            }

            public HttpResult readFromServer(InputStream stream) throws IOException {
                InputStream is = count(stream, debug);
                try {
                    try (ObjectInputStream ois = readData(is)) {
                        Object result = ois.readObject();
                        Throwable error = (Throwable) ois.readObject();
                        return new HttpResult(result, error);
                    }
                } catch (ClassNotFoundException | InvalidClassException ex) {
                    throw new UnrecoverableRemoteException(ex);
                }
            }
        });
    }
}
