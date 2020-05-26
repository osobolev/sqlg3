package sqlg3.runtime;

public final class GContext implements AutoCloseable {

    final GlobalContext global;
    final SessionContext session;
    final TransactionContext transaction;
    final CallContext call;

    GContext(GlobalContext global, SessionContext session, TransactionContext transaction) {
        this.global = global;
        this.session = session;
        this.transaction = transaction;
        this.call = new CallContext(global);
    }

    void ok() {
        call.ok();
    }

    @Override
    public void close() {
        call.close();
    }
}
