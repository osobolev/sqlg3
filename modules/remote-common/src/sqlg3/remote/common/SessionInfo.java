package sqlg3.remote.common;

import java.io.Serializable;

/**
 * Client session descriptor.
 */
public final class SessionInfo implements Serializable {

    /**
     * User login.
     */
    public final String user;
    /**
     * User host name (null for local connection).
     */
    public final String host;
    /**
     * Unique session identifier.
     */
    public final long sessionOrderId;
    /**
     * Secure unique session identifier.
     */
    public final String sessionLongId;
    /**
     * Session working time.
     */
    public final long workingTime;
    /**
     * User identifier object.
     */
    public final Object userObject;
    /**
     * true for background process.
     */
    public final boolean background;

    public SessionInfo(String user, String host, long sessionOrderId, String sessionLongId, long workingTime, Object userObject, boolean background) {
        this.user = user;
        this.host = host;
        this.sessionOrderId = sessionOrderId;
        this.sessionLongId = sessionLongId;
        this.workingTime = workingTime;
        this.userObject = userObject;
        this.background = background;
    }
}
