package sqlg3.runtime;

public final class GContext implements AutoCloseable {

    final GlobalContext global;
    final TransactionContext transaction;
    final CallContext call;

    GContext(GlobalContext global, TransactionContext transaction) {
        this.global = global;
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
