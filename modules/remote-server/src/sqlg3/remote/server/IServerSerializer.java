package sqlg3.remote.server;

import sqlg3.core.IDBCommon;
import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IServerSerializer {

    interface ServerCall {

        Object call(HttpId id, HttpCommand command, Class<? extends IDBCommon> iface, String method, Class<?>[] paramTypes, Object[] params) throws Throwable;
    }

    void serverToClient(InputStream is, ServerCall call,
                        OutputStream os) throws IOException;

    void sendError(OutputStream os, Throwable error) throws IOException;
}
