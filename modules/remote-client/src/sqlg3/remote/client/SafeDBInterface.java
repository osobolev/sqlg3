package sqlg3.remote.client;

import sqlg3.core.*;
import sqlg3.remote.common.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Wrapper for {@link IDBInterface} on the client side.
 * Caches transactions - creates only one transaction per thread.
 * Also runs activity notification thread to notify server about this client.
 */
public final class SafeDBInterface implements IRemoteDBInterface {

    private IRemoteDBInterface idb;
    private final ConnectionProducer producer;
    private int resetCounter = 0;
    private boolean unrecoverable = false;

    private final WatcherThread watcher;

    public SafeDBInterface(Consumer<Throwable> logger, ConnectionProducer producer) throws Exception {
        this(logger, producer.open(), producer);
    }

    /**
     * Constructor.
     *
     * @param idb DB connection
     */
    public SafeDBInterface(Consumer<Throwable> logger, IRemoteDBInterface idb) {
        this(logger, idb, null);
    }

    public SafeDBInterface(Consumer<Throwable> logger, IRemoteDBInterface idb, ConnectionProducer producer) {
        this.idb = idb;
        this.producer = producer;
        // pinging twice as frequent as server checks session activity
        this.watcher = new WatcherThread(2, () -> {
            try {
                ping();
            } catch (RemoteException ex) {
                logger.accept(ex);
            }
        });
        this.watcher.runThread();
    }

    private IRemoteDBInterface getDb() throws Exception {
        synchronized (this) {
            if (idb == null && producer != null) {
                if (unrecoverable)
                    throw new RemoteException("Unrecoverable error, please restart application");
                idb = producer.open();
            }
            return idb;
        }
    }

    ISimpleTransaction createSimpleTransaction() throws SQLException {
        try {
            return getDb().getSimpleTransaction();
        } catch (RemoteException | SQLException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RemoteException(ex);
        }
    }

    public ISimpleTransaction getSimpleTransaction() throws SQLException {
        if (producer == null) {
            return createSimpleTransaction();
        } else {
            return new SafeSimpleTransaction(this);
        }
    }

    public ITransaction getTransaction() throws SQLException {
        try {
            return getDb().getTransaction();
        } catch (RemoteException | SQLException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RemoteException(ex);
        }
    }

    public void ping() {
        try {
            getDb().ping();
        } catch (RemoteException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RemoteException(ex);
        }
    }

    public void close() throws SQLException {
        watcher.shutdown();
        synchronized (this) {
            if (idb != null) {
                idb.close();
                idb = null;
            }
        }
    }

    public Object getUserObject() {
        try {
            return getDb().getUserObject();
        } catch (RemoteException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RemoteException(ex);
        }
    }

    <T extends IDBCommon> T wrap(Class<T> iface, SafeWrapper<T> obj) {
        return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, (proxy, method, args) -> {
            try {
                return method.invoke(obj.get(), args);
            } catch (InvocationTargetException itex) {
                Throwable ex = itex.getTargetException();
                if (!(ex instanceof InformationException)) {
                    resetConnection(ex instanceof UnrecoverableRemoteException);
                }
                throw ex;
            }
        }));
    }

    private void resetConnection(boolean unrecoverable) {
        synchronized (this) {
            if (idb != null) {
                try {
                    idb.close();
                } catch (SQLException | RemoteException ex) {
                    // ignore
                }
                idb = null;
                resetCounter++;
                this.unrecoverable = unrecoverable;
            }
        }
    }

    int getResetCounter() {
        synchronized (this) {
            return resetCounter;
        }
    }
}
