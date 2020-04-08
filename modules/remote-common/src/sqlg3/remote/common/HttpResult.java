package sqlg3.remote.common;

public final class HttpResult {

    public final Object result;
    public final Throwable error;

    public HttpResult(Object result, Throwable error) {
        this.result = result;
        this.error = error;
    }
}
