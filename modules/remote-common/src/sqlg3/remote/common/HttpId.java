package sqlg3.remote.common;

import java.io.Serializable;

public final class HttpId implements Serializable {

    public final String application;
    public final String sessionId;
    public final Long transactionId;

    public HttpId(String application) {
        this(application, null, null);
    }

    public HttpId(String application, String sessionId, Long transactionId) {
        this.application = application;
        this.sessionId = sessionId;
        this.transactionId = transactionId;
    }
}
