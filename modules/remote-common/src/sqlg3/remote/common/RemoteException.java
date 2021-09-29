package sqlg3.remote.common;

public class RemoteException extends RuntimeException {

    public final boolean sessionClosed;

    public RemoteException(String message, boolean sessionClosed) {
        super(message);
        this.sessionClosed = sessionClosed;
    }

    public RemoteException(String message) {
        this(message, false);
    }

    public RemoteException(Throwable cause) {
        super(cause.getMessage(), cause);
        this.sessionClosed = false;
    }
}
