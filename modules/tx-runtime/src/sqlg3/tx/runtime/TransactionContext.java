package sqlg3.tx.runtime;

import sqlg3.tx.api.IDBCommon;
import sqlg3.tx.api.TxCallException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

final class TransactionContext {

    private final TransGlobalContext global;
    private final SessionContext session;

    private final Object connLock = new Object();
    private Connection connection = null;

    TransactionContext(TransGlobalContext global, SessionContext session) {
        this.global = global;
        this.session = session;
    }

    private Connection getConnection() throws SQLException {
        synchronized (connLock) {
            if (connection == null) {
                connection = session.cman.allocConnection();
            }
            return connection;
        }
    }

    @SuppressWarnings("unchecked")
    <T extends IDBCommon> T getInterface(Class<T> iface, boolean commitCalls) {
        ClassLoader classLoader = iface.getClassLoader();
        return (T) Proxy.newProxyInstance(classLoader, new Class[] {iface}, (proxy, method, args) -> {
            boolean success = false;
            Object result = null;
            Throwable error = null;
            try {
                Class<?> implClass = global.getImpl(iface);
                Method daoMethod = implClass.getMethod(method.getName(), method.getParameterTypes());
                if (session.beforeCall != null) {
                    session.beforeCall.check(daoMethod, args);
                }
                Object instance = global.toImpl.newInstance(implClass, getConnection(), session.getUserObject());
                result = daoMethod.invoke(instance, args);
                success = true;
            } catch (InvocationTargetException ex) {
                error = ex.getTargetException();
            } catch (ReflectiveOperationException ex) {
                error = new TxCallException("Error calling DAO method " + iface.getCanonicalName() + "." + method.getName(), ex);
            } catch (Throwable ex) {
                error = ex;
            }
            if (commitCalls) {
                try {
                    if (success) {
                        commit();
                    } else {
                        rollback();
                    }
                } catch (SQLException ex) {
                    if (error != null) {
                        error.addSuppressed(ex);
                    } else {
                        error = ex;
                    }
                }
            }
            if (error != null) {
                throw error;
            }
            return result;
        });
    }

    private void releaseConnection(SQLException error) throws SQLException {
        try {
            session.cman.releaseConnection(connection);
        } catch (SQLException ex) {
            if (error != null) {
                error.addSuppressed(ex);
            } else {
                error = ex;
            }
        } finally {
            connection = null;
        }
        if (error != null) {
            throw error;
        }
    }

    void commit() throws SQLException {
        synchronized (connLock) {
            if (connection == null)
                return;
            SQLException error = null;
            try {
                connection.commit();
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException ex2) {
                    ex.addSuppressed(ex2);
                }
                error = ex;
            } finally {
                releaseConnection(error);
            }
        }
    }

    void rollback() throws SQLException {
        synchronized (connLock) {
            if (connection == null)
                return;
            SQLException error = null;
            try {
                connection.rollback();
            } catch (SQLException ex) {
                error = ex;
            } finally {
                releaseConnection(error);
            }
        }
    }
}
