package sqlg3.tx.runtime;

import java.sql.Connection;

public interface ImplConstructor {

    Object newInstance(Class<?> cls, Connection connection, Object userObject) throws Exception;
}
