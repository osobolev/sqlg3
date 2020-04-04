package sqlg3.core;

import java.sql.SQLException;

/**
 * Adds transaction control methods to {@link ISimpleTransaction} - {@link #commit()}
 * and {@link #rollback()}. Business method calls on objects, obtained from
 * {@link ITransaction#getInterface(Class)} are not automatically committed or rolled back.
 * ITransaction holds resources (DB connection) until explicit call to {@link #commit()}
 * or {@link #rollback()}, so you must call one of them once you start transaction
 * (else resource leak may occur).
 * If single-connection pool is used, then you cannot call something from other transactions
 * (even from simple transactions) during active transaction - they have to wait until single
 * DB connection is released by the current transaction. In most cases violation of this rule
 * leads to deadlock, so if you are using ITransaction then all data access interfaces should
 * by obtained from it, not from other transactions.
 */
public interface ITransaction extends ISimpleTransaction {

    /**
     * Rolls back transaction.
     */
    void rollback() throws SQLException;

    /**
     * Commits transaction.
     */
    void commit() throws SQLException;

    interface Action {

        void run(ISimpleTransaction t) throws SQLException;
    }

    static void runAction(IDBInterface db, Action action) throws SQLException {
        try (TransactionRunContext ctx = new TransactionRunContext(db.getTransaction())) {
            action.run(ctx.trans);
            ctx.setOk(true);
        }
    }

    interface Call<T> {

        T run(ISimpleTransaction t) throws SQLException;
    }

    static <T> T runCall(IDBInterface db, Call<T> call) throws SQLException {
        try (TransactionRunContext ctx = new TransactionRunContext(db.getTransaction())) {
            T result = call.run(ctx.trans);
            ctx.setOk(true);
            return result;
        }
    }
}
