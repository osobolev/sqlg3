package sqlg3.tx.runtime;

import java.lang.reflect.Constructor;

final class ImplCache {

    final Class<?> dao;
    final Constructor<?> constructor;

    ImplCache(Class<?> dao, Constructor<?> constructor) {
        this.dao = dao;
        this.constructor = constructor;
    }
}
