package sqlg3.tx.runtime;

import sqlg3.core.SQLGException;
import sqlg3.runtime.GContext;
import sqlg3.runtime.GlobalContext;
import sqlg3.tx.api.Impl;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class TransGlobalContext {

    private final GlobalContext global;

    private final ConcurrentMap<Class<?>, ImplCache> implCache = new ConcurrentHashMap<>();

    public TransGlobalContext(GlobalContext global) {
        this.global = global;
    }

    public GlobalContext getGlobal() {
        return global;
    }

    private static ImplCache createImpl(Class<?> iface) {
        try {
            Impl sqlg = iface.getAnnotation(Impl.class);
            Class<?> dao = iface.getClassLoader().loadClass(sqlg.value());
            Constructor<?> constructor = dao.getConstructor(GContext.class);
            return new ImplCache(dao, constructor);
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            throw new SQLGException("Cannot find implementation for " + iface.getCanonicalName());
        }
    }

    ImplCache getImpl(Class<?> iface) {
        return implCache.computeIfAbsent(iface, i -> createImpl(iface));
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
