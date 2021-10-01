package sqlg3.remote.client;

import sqlg3.core.IDBCommon;
import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.HttpRequest;
import sqlg3.remote.common.HttpResult;

import java.lang.reflect.Type;

final class HttpRootObject {

    private final IHttpClient client;

    HttpRootObject(IHttpClient client) {
        this.client = client;
    }

    @SuppressWarnings("unchecked")
    <T> T httpInvoke(Class<T> retType, HttpCommand command, HttpId id, Object... params) throws Throwable {
        return (T) httpInvoke(retType, command, id, null, null, null, params);
    }

    Object httpInvoke(Type retType, HttpCommand command, HttpId id, Class<? extends IDBCommon> iface, String method, Class<?>[] paramTypes, Object[] params) throws Throwable {
        HttpRequest request = new HttpRequest(id, command, iface, method, paramTypes, params);
        HttpResult result = client.call(retType, request);
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
