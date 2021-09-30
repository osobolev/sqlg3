package sqlg3.remote.client;

import sqlg3.core.IDBCommon;
import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.HttpResult;
import sqlg3.remote.common.RemoteException;

import java.lang.reflect.Type;

final class HttpRootObject {

    private final IHttpClientFactory clientFactory;
    private IClientSerializer serializer = new ClientJavaSerializer();

    HttpRootObject(IHttpClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    void setSerializer(IClientSerializer serializer) {
        this.serializer = serializer;
    }

    @SuppressWarnings("unchecked")
    <T> T httpInvoke(Class<T> retType, HttpCommand command, HttpId id, Object... params) throws Throwable {
        return (T) httpInvoke(retType, command, id, null, null, null, params);
    }

    Object httpInvoke(Type retType, HttpCommand command, HttpId id, Class<? extends IDBCommon> iface, String method, Class<?>[] paramTypes, Object[] params) throws Throwable {
        Object result;
        Throwable error;
        try (IHttpClient conn = clientFactory.getClient()) {
            IClientSerializer.ReqRespProcessor processor = conn.getProcessor();
            HttpResult httpResult = serializer.clientToServer(processor, id, command, iface, retType, method, paramTypes, params);
            result = httpResult.result;
            error = httpResult.error;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RemoteException(ex);
        }
        if (error != null) {
            serverException(error);
            return null;
        } else {
            return result;
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
