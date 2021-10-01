package sqlg3.remote.server;

import sqlg3.core.IDBCommon;
import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.HttpResult;

import java.io.IOException;

public interface IHttpRequest {

    interface ServerCall {

        HttpResult call(HttpId id, HttpCommand command, Class<? extends IDBCommon> iface, String method, Class<?>[] paramTypes, Object[] params);
    }

    void perform(ServerCall call) throws IOException;

    default void writeError(Throwable error) throws IOException {
        perform((id, command, iface, method, paramTypes, params) -> new HttpResult(null, error));
    }
}
