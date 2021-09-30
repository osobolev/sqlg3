package sqlg3.remote.common;

import java.io.Serializable;

public final class HttpDBInterfaceInfo implements Serializable {

    public final HttpId id;
    public final Object userObject;

    public HttpDBInterfaceInfo(HttpId id, Object userObject) {
        this.id = id;
        this.userObject = userObject;
    }
}
