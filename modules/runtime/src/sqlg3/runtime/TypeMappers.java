package sqlg3.runtime;

import sqlg3.core.SQLGException;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class defines custom Java/SQL mapping.
 */
public final class TypeMappers {

    private final Map<Class<?>, TypeMapper<?>> mappers = new ConcurrentHashMap<>();

    public <T> void register(TypeMapper<T> mapper) {
        mappers.put(mapper.cls, mapper);
    }

    /**
     * Returns mapping for class {@code cls}
     *
     * @param cls class for which to define mapping
     * @return mapping for the class {@code cls}
     */
    @SuppressWarnings("unchecked")
    <T> TypeMapper<T> getMapper(Class<T> cls) {
        TypeMapper<?> mapper = mappers.get(cls);
        if (mapper == null) {
            throw new SQLGException("Cannot find mapping for class " + cls.getCanonicalName());
        }
        return (TypeMapper<T>) mapper;
    }

    public interface PSSetter<T> {

        void set(PreparedStatement stmt, int index, T value) throws SQLException;
    }

    public interface RSGetter<T> {

        T get(ResultSet rs, int index) throws SQLException;
    }

    public interface CSGetter<T> {

        T get(CallableStatement cs, int index) throws SQLException;
    }

    public static final class PrimitiveMapper<T> extends TypeMapper<T> {

        private final int jdbcType;
        private final PSSetter<T> setter;
        private final RSGetter<T> rsGetter;
        private final CSGetter<T> csGetter;

        public PrimitiveMapper(Class<T> cls, int jdbcType, PSSetter<T> setter, RSGetter<T> rsGetter, CSGetter<T> csGetter) {
            super(cls);
            this.jdbcType = jdbcType;
            this.setter = setter;
            this.rsGetter = rsGetter;
            this.csGetter = csGetter;
        }

        @Override
        public T fetch(ResultSet rs, int index) throws SQLException {
            T value = rsGetter.get(rs, index);
            if (rs.wasNull())
                return null;
            return value;
        }

        @Override
        public void set(PreparedStatement stmt, int index, T value) throws SQLException {
            if (value == null) {
                stmt.setNull(index, jdbcType);
            } else {
                setter.set(stmt, index, value);
            }
        }

        @Override
        public void register(CallableStatement cs, int index) throws SQLException {
            cs.registerOutParameter(index, jdbcType);
        }

        @Override
        public T get(CallableStatement cs, int index) throws SQLException {
            T value = csGetter.get(cs, index);
            if (cs.wasNull())
                return null;
            return value;
        }
    }

    public void registerDefault() {
        register(new PrimitiveMapper<>(
            Boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean, ResultSet::getBoolean, CallableStatement::getBoolean
        ));
        register(new PrimitiveMapper<>(
            Integer.class, Types.INTEGER, PreparedStatement::setInt, ResultSet::getInt, CallableStatement::getInt
        ));
        register(new PrimitiveMapper<>(
            Long.class, Types.BIGINT, PreparedStatement::setLong, ResultSet::getLong, CallableStatement::getLong
        ));
        register(new PrimitiveMapper<>(
            Double.class, Types.DOUBLE, PreparedStatement::setDouble, ResultSet::getDouble, CallableStatement::getDouble
        ));
        register(new PrimitiveMapper<>(
            String.class, Types.VARCHAR, PreparedStatement::setString, ResultSet::getString, CallableStatement::getString
        ));
        register(new PrimitiveMapper<>(
            byte[].class, Types.VARBINARY, PreparedStatement::setBytes, ResultSet::getBytes, CallableStatement::getBytes
        ));
        register(new TypeMappers.PrimitiveMapper<>(
            Timestamp.class, Types.TIMESTAMP, PreparedStatement::setTimestamp, ResultSet::getTimestamp, CallableStatement::getTimestamp
        ));
        register(new TypeMappers.PrimitiveMapper<>(
            Date.class, Types.DATE, PreparedStatement::setDate, ResultSet::getDate, CallableStatement::getDate
        ));
        register(new TypeMappers.PrimitiveMapper<>(
            Time.class, Types.TIME, PreparedStatement::setTime, ResultSet::getTime, CallableStatement::getTime
        ));
    }
}
