package sqlg3.remote.server;

import sqlg3.core.IDBCommon;
import sqlg3.core.ISimpleTransaction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

final class AsyncTransaction implements ISimpleTransaction {

    private final DBInterface db;
    private final Consumer<Throwable> logger;

    AsyncTransaction(DBInterface db, Consumer<Throwable> logger) {
        this.db = db;
        this.logger = logger;
    }

    public <T extends IDBCommon> T getInterface(Class<T> iface) {
        return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class[] {iface}, (proxy, method, args) -> {
            Thread thread = new Thread(() -> {
                try {
                    try (DBInterface background = db.createBackground()) {
                        T target = background.getSimpleTransaction().getInterface(iface);
                        method.invoke(target, args);
                    }
                } catch (InvocationTargetException ex) {
                    logger.accept(ex.getTargetException());
                } catch (Exception ex) {
                    logger.accept(ex);
                }
            });
            thread.start();
            return null;
        }));
    }
}
