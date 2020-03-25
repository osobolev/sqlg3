package sqlg3.remote.client;

import java.io.IOException;

public interface IHttpClientFactory {

    IHttpClient getClient() throws IOException;
}
