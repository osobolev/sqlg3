package sqlg3.remote.client;

import sqlg3.tx.api.IDBCommon;

final class SafeWrapper<T extends IDBCommon> {

    private final Class<T> iface;
    private T instance = null;
    private int currentCounter = -1;
    private final SafeDBInterface db;
    private final SafeSimpleTransaction trans;

    SafeWrapper(Class<T> iface, SafeDBInterface db, SafeSimpleTransaction trans) {
        this.iface = iface;
        this.db = db;
        this.trans = trans;
    }

    T get() {
        synchronized (this) {
            int dbCounter = db.getResetCounter();
            if (dbCounter != currentCounter) {
                instance = null;
                currentCounter = dbCounter;
            }
            if (instance == null) {
                instance = trans.createInterface(iface);
            }
            return instance;
        }
    }
}
