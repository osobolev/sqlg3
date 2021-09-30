package sqlg3.remote.common;

public class RemoteException extends RuntimeException {

    public RemoteException(String message) {
        super(message);
    }

    public RemoteException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
