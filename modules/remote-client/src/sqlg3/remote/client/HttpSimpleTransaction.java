package sqlg3.remote.client;

import sqlg3.core.IDBCommon;
import sqlg3.core.ISimpleTransaction;
import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;

import java.lang.reflect.Proxy;

class HttpSimpleTransaction implements ISimpleTransaction {

    protected final HttpRootObject rootObject;
    protected final HttpId id;
    private final HttpCommand command;

    HttpSimpleTransaction(HttpRootObject rootObject, HttpId id, HttpCommand command) {
        this.rootObject = rootObject;
        this.id = id;
        this.command = command;
    }

    public final <T extends IDBCommon> T getInterface(Class<T> iface) {
        return iface.cast(Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[] {iface},
            (proxy, method, args) -> rootObject.httpInvoke(method.getGenericReturnType(), command, id, iface, method.getName(), method.getParameterTypes(), args)
        ));
    }
}
