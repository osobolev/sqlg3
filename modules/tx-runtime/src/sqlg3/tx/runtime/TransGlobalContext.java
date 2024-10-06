package sqlg3.tx.runtime;

import sqlg3.tx.api.Impl;
import sqlg3.tx.api.SQLGCallException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class TransGlobalContext {

    final ImplConstructor toImpl;

    private final ConcurrentMap<Class<?>, Class<?>> implCache = new ConcurrentHashMap<>();

    public TransGlobalContext(ImplConstructor toImpl) {
        this.toImpl = toImpl;
    }

    private static Class<?> getImplClass(Class<?> iface) {
        try {
            Impl sqlg = iface.getAnnotation(Impl.class);
            return iface.getClassLoader().loadClass(sqlg.value());
        } catch (ClassNotFoundException ex) {
            throw new SQLGCallException("Cannot find implementation for " + iface.getCanonicalName());
        }
    }

    Class<?> getImpl(Class<?> iface) {
        return implCache.computeIfAbsent(iface, i -> getImplClass(iface));
    }

    private final List<SessionListener> sessionListeners = new ArrayList<>();

    public void addSessionListener(SessionListener listener) {
        synchronized (sessionListeners) {
            sessionListeners.add(listener);
        }
    }

    public void removeSessionListener(SessionListener listener) {
        synchronized (sessionListeners) {
            sessionListeners.remove(listener);
        }
    }

    public void fireSessionListeners(Consumer<SessionListener> consumer) {
        synchronized (sessionListeners) {
            for (SessionListener listener : sessionListeners) {
                consumer.accept(listener);
            }
        }
    }
}
