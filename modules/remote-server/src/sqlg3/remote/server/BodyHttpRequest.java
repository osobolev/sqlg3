package sqlg3.remote.server;

import sqlg3.remote.common.HttpRequest;
import sqlg3.remote.common.HttpResult;
import sqlg3.remote.common.ISerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class BodyHttpRequest implements IHttpRequest {

    private final ISerializer serializer;
    private final InputStream in;
    private final OutputStream out;

    public BodyHttpRequest(ISerializer serializer, InputStream in, OutputStream out) {
        this.serializer = serializer;
        this.in = in;
        this.out = out;
    }

    @Override
    public void perform(ServerCall call) throws IOException {
        HttpRequest request;
        try (ISerializer.Reader fromClient = serializer.newReader(in)) {
            request = fromClient.read(HttpRequest.class);
        }
        HttpResult result = call.call(
            request.id, request.getCommand(),
            request.iface, request.method, request.paramTypes, request.params
        );
        try (ISerializer.Writer toClient = serializer.newWriter(out)) {
            toClient.write(result, HttpResult.class);
        }
    }
}
