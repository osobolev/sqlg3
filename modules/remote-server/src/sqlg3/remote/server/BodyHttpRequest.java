package sqlg3.remote.server;

import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.ISerializer;

import java.io.InputStream;
import java.io.OutputStream;

public final class BodyHttpRequest extends BaseBodyHttpRequest {

    public BodyHttpRequest(ISerializer serializer, String hostName, InputStream in, OutputStream out) {
        super(serializer, hostName, in, out);
    }

    @Override
    protected ServerHttpId serverId(HttpId id) {
        return new ServerHttpId(id.application, id.sessionId, id.transactionId);
    }

    @Override
    public String newSessionId() {
        return null;
    }

    @Override
    public HttpId session(ServerHttpId id, String sessionId) {
        return new HttpId(id.application, sessionId, null);
    }

    @Override
    public HttpId transaction(ServerHttpId id, long transactionId) {
        return new HttpId(id.application, id.sessionId, transactionId);
    }
}
