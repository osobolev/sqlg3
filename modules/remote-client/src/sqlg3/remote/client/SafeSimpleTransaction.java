package sqlg3.remote.client;

import sqlg3.core.IDBCommon;
import sqlg3.core.ISimpleTransaction;
import sqlg3.remote.common.RemoteException;

import java.sql.SQLException;

final class SafeSimpleTransaction implements ISimpleTransaction {

    private ISimpleTransaction trans = null;
    private int currentCounter = -1;
    private final SafeDBInterface db;

    SafeSimpleTransaction(SafeDBInterface db) {
        this.db = db;
    }

    private ISimpleTransaction getTrans() {
        synchronized (this) {
            int dbCounter = db.getResetCounter();
            if (dbCounter != currentCounter) {
                trans = null;
                currentCounter = dbCounter;
            }
            if (trans == null) {
                try {
                    trans = db.createSimpleTransaction();
                } catch (SQLException ex) {
                    throw new RemoteException(ex);
                }
            }
            return trans;
        }
    }

    public <T extends IDBCommon> T getInterface(Class<T> iface) {
        return db.wrap(iface, new SafeWrapper<>(iface, db, this));
    }

    <T extends IDBCommon> T createInterface(Class<T> iface) {
        return getTrans().getInterface(iface);
    }
}
