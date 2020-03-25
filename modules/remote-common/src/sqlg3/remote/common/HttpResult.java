package sqlg3.remote.common;

import java.io.Serializable;

public final class HttpResult implements Serializable {

    public final Object result;
    public final Throwable error;

    public HttpResult(Object result, Throwable error) {
        this.result = result;
        this.error = error;
    }
}
