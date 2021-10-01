package sqlg3.remote.client;

import sqlg3.remote.common.HttpRequest;
import sqlg3.remote.common.HttpResult;
import sqlg3.remote.common.ISerializer;
import sqlg3.remote.common.JavaSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.*;

public final class DefaultHttpClient implements IHttpClient {

    private final URL url;
    private final Proxy proxy;
    private final int connectTimeout;
    private final ISerializer serializer;

    public static final class Builder {

        private final URL url;
        private int connectTimeout = 3000;
        private ISerializer serializer = null;
        private Proxy proxy = Proxy.NO_PROXY;

        public Builder(URL url) {
            this.url = url;
        }

        public Builder setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder setSerializer(ISerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder setProxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public DefaultHttpClient build() {
            return new DefaultHttpClient(url, proxy, connectTimeout, serializer == null ? new JavaSerializer() : serializer);
        }
    }

    public static Builder builder(URL url) {
        return new Builder(url);
    }

    public static Builder builder(String url) throws URISyntaxException, MalformedURLException {
        return builder(new URI(url).normalize().toURL());
    }

    public DefaultHttpClient(URL url, Proxy proxy, int connectTimeout, ISerializer serializer) {
        this.url = url;
        this.proxy = proxy;
        this.connectTimeout = connectTimeout;
        this.serializer = serializer;
    }

    public static HttpURLConnection open(URL url, Proxy proxy, int connectTimeout) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(connectTimeout);
        conn.setUseCaches(false);
        conn.setAllowUserInteraction(false);
        return conn;
    }

    @Override
    public HttpResult call(Type retType, HttpRequest request) throws IOException {
        HttpURLConnection conn = open(url, proxy, connectTimeout);
        conn.connect();
        try {
            try (ISerializer.Writer toServer = serializer.newWriter(conn.getOutputStream())) {
                toServer.write(request, HttpRequest.class);
            }
            try (ISerializer.Reader fromServer = serializer.newReader(conn.getInputStream())) {
                return fromServer.read(HttpResult.class);
            }
        } finally {
            conn.disconnect();
        }
    }
}
