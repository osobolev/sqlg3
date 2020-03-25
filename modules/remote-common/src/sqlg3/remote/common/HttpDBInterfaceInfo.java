package sqlg3.remote.common;

import java.io.Serializable;

public final class HttpDBInterfaceInfo implements Serializable {

    public final HttpId id;
    public final String userLogin;
    public final String userHost;
    public final Object userObject;

    public HttpDBInterfaceInfo(HttpId id, String userLogin, String userHost, Object userObject) {
        this.id = id;
        this.userLogin = userLogin;
        this.userHost = userHost;
        this.userObject = userObject;
    }
}
