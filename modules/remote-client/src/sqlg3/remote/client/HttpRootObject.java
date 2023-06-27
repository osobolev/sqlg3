package sqlg3.remote.client;

import sqlg3.core.IDBCommon;
import sqlg3.remote.common.*;

import java.io.IOException;
import java.lang.reflect.Type;

final class HttpRootObject {

    private final IHttpClient client;

    HttpRootObject(IHttpClient client) {
        this.client = client;
    }

    Object newContext() {
        return client.newContext();
    }

    @SuppressWarnings("unchecked")
    <T> T httpInvoke(Class<T> retType, Object clientContext, HttpCommand command, HttpId id, Object... params) throws Throwable {
        return (T) httpInvoke(retType, clientContext, command, id, null, null, null, params);
    }

    Object httpInvoke(Type retType, Object clientContext, HttpCommand command, HttpId id, Class<? extends IDBCommon> iface, String method,
                      Class<?>[] paramTypes, Object[] params) throws Throwable {
        HttpRequest request = new HttpRequest(id, command, iface, method, paramTypes, params);
        HttpResult result = client.call(retType, clientContext, request);
        Throwable error = result.error;
        if (error != null) {
            serverException(error);
            return null;
        } else {
            return result.result;
        }
    }

    private static void serverException(Throwable error) throws Throwable {
        StackTraceElement[] serverST = error.getStackTrace();
        StackTraceElement[] clientST = new Throwable().getStackTrace();
        StackTraceElement[] allST = new StackTraceElement[serverST.length + clientST.length];
        System.arraycopy(serverST, 0, allST, 0, serverST.length);
        System.arraycopy(clientST, 0, allST, serverST.length, clientST.length);
        error.setStackTrace(allST);
        throw error;
    }
}
