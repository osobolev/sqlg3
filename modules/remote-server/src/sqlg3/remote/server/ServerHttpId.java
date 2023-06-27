package sqlg3.remote.server;

public final class ServerHttpId {

    public final String application;
    public final String sessionId;
    public final Long transactionId;

    public ServerHttpId(String application, String sessionId, Long transactionId) {
        this.application = application;
        this.sessionId = sessionId;
        this.transactionId = transactionId;
    }
}
