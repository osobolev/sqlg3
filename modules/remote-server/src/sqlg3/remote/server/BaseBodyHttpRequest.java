package sqlg3.remote.server;

import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.HttpRequest;
import sqlg3.remote.common.HttpResult;
import sqlg3.remote.common.ISerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BaseBodyHttpRequest implements IHttpRequest {

    private final ISerializer serializer;
    private final String hostName;
    private final InputStream in;
    private final OutputStream out;

    protected BaseBodyHttpRequest(ISerializer serializer, String hostName, InputStream in, OutputStream out) {
        this.serializer = serializer;
        this.hostName = hostName;
        this.in = in;
        this.out = out;
    }

    @Override
    public final String hostName() {
        return hostName;
    }

    protected abstract ServerHttpId serverId(HttpId id);

    @Override
    public final ServerHttpRequest requestData() throws IOException {
        HttpRequest request;
        try (ISerializer.Reader fromClient = serializer.newReader(in)) {
            request = fromClient.read(HttpRequest.class);
        }
        return new ServerHttpRequest(
            serverId(request.id), request.getCommand(),
            request.iface, request.method, request.paramTypes, request.params
        );
    }

    @Override
    public final void write(HttpResult result) throws IOException {
        try (ISerializer.Writer toClient = serializer.newWriter(out)) {
            toClient.write(result, HttpResult.class);
        }
    }
}
