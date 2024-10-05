package sqlg3.tx.api;

import java.sql.SQLException;

final class TransactionRunContext implements AutoCloseable {

    final ITransaction trans;

    private boolean ok = false;

    TransactionRunContext(ITransaction trans) {
        this.trans = trans;
    }

    void setOk(boolean ok) {
        this.ok = ok;
    }

    @Override
    public void close() throws SQLException {
        if (ok) {
            try {
                trans.commit();
            } catch (SQLException ex) {
                try {
                    trans.rollback();
                } catch (SQLException ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
        } else {
            trans.rollback();
        }
    }
}
