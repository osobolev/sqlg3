package sqlg3.remote.client;

import java.io.Closeable;
import java.io.IOException;

public interface IHttpClient extends Closeable {

    IClientSerializer.ReqRespProcessor getProcessor() throws IOException;
}
