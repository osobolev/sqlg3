package sqlg3.runtime;

import sqlg3.core.IDBCommon;
import sqlg3.core.Impl;
import sqlg3.core.InformationException;
import sqlg3.core.SQLGException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

final class TransactionContext {

    private final GlobalContext global;
    private final ConnectionManager cman;

    private final Object connLock = new Object();
    private Connection connection = null;

    TransactionContext(GlobalContext global, ConnectionManager cman) {
        this.global = global;
        this.cman = cman;
    }

    Connection getConnection() throws SQLException {
        synchronized (connLock) {
            if (connection == null) {
                connection = cman.allocConnection();
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
                // todo: cache class/constructor???
                Impl sqlg = iface.getAnnotation(Impl.class);
                Class<?> dao = classLoader.loadClass(sqlg.value());
                Constructor<?> constructor = dao.getConstructor(GContext.class);
                Method daoMethod = dao.getMethod(method.getName(), method.getParameterTypes());
                try (GContext ctx = new GContext(global, this)) {
                    Object instance = constructor.newInstance(ctx);
                    result = daoMethod.invoke(instance, args);
                    ctx.ok();
                }
                success = true;
            } catch (InvocationTargetException ex) {
                Throwable target = ex.getTargetException();
                if (target instanceof InformationException) {
                    InformationException informationException = (InformationException) target;
                    success = !informationException.error;
                }
                error = target;
            } catch (ReflectiveOperationException ex) {
                error = new SQLGException("Error calling DAO method", ex);
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
            cman.releaseConnection(connection);
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
