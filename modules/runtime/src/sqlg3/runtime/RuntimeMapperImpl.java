package sqlg3.runtime;

import sqlg3.core.SQLGException;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeMapperImpl implements RuntimeMapper {

    private final Map<Class<?>, TypeMapper<?>> mappers = new ConcurrentHashMap<>();

    public final <T> void register(TypeMapper<T> mapper) {
        mappers.put(mapper.cls, mapper);
    }

    /**
     * Returns mapping for class {@code cls}
     *
     * @param cls class for which to define mapping
     * @return mapping for the class {@code cls}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeMapper<T> getMapper(Class<T> cls) {
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

    public abstract static class AbstractBasicMapper<T> extends TypeMapper<T> {

        private final int jdbcType;
        private final PSSetter<T> setter;

        protected AbstractBasicMapper(Class<T> cls, int jdbcType, PSSetter<T> setter) {
            super(cls);
            this.jdbcType = jdbcType;
            this.setter = setter;
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
    }

    public static final class BasicMapper<T> extends AbstractBasicMapper<T> {

        private final RSGetter<T> rsGetter;
        private final CSGetter<T> csGetter;

        public BasicMapper(Class<T> cls, int jdbcType, PSSetter<T> setter, RSGetter<T> rsGetter, CSGetter<T> csGetter) {
            super(cls, jdbcType, setter);
            this.rsGetter = rsGetter;
            this.csGetter = csGetter;
        }

        @Override
        public T fetch(ResultSet rs, int index) throws SQLException {
            return rsGetter.get(rs, index);
        }

        @Override
        public T get(CallableStatement cs, int index) throws SQLException {
            return csGetter.get(cs, index);
        }
    }

    public static final class WrapperMapper<T> extends AbstractBasicMapper<T> {

        private final RSGetter<T> rsGetter;
        private final CSGetter<T> csGetter;

        public WrapperMapper(Class<T> cls, int jdbcType, PSSetter<T> setter, RSGetter<T> rsGetter, CSGetter<T> csGetter) {
            super(cls, jdbcType, setter);
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
        public T get(CallableStatement cs, int index) throws SQLException {
            T value = csGetter.get(cs, index);
            if (cs.wasNull())
                return null;
            return value;
        }
    }

    public static final class EnumMapper<T extends Enum<T>> extends TypeMapper<T> {

        public EnumMapper(Class<T> cls) {
            super(cls);
        }

        private T convert(String name) {
            return name == null ? null : Enum.valueOf(cls, name);
        }

        @Override
        public T fetch(ResultSet rs, int index) throws SQLException {
            String name = rs.getString(index);
            return convert(name);
        }

        @Override
        public void set(PreparedStatement stmt, int index, T value) throws SQLException {
            stmt.setString(index, value == null ? null : value.name());
        }

        @Override
        public void register(CallableStatement cs, int index) throws SQLException {
            cs.registerOutParameter(index, Types.VARCHAR);
        }

        @Override
        public T get(CallableStatement cs, int index) throws SQLException {
            String name = cs.getString(index);
            return convert(name);
        }
    }

    public RuntimeMapperImpl() {
        registerDefault();
    }

    public void registerDefault() {
        register(new BasicMapper<>(
            Boolean.TYPE, Types.BOOLEAN, PreparedStatement::setBoolean, ResultSet::getBoolean, CallableStatement::getBoolean
        ));
        register(new WrapperMapper<>(
            Boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean, ResultSet::getBoolean, CallableStatement::getBoolean
        ));

        register(new BasicMapper<>(
            Integer.TYPE, Types.INTEGER, PreparedStatement::setInt, ResultSet::getInt, CallableStatement::getInt
        ));
        register(new WrapperMapper<>(
            Integer.class, Types.INTEGER, PreparedStatement::setInt, ResultSet::getInt, CallableStatement::getInt
        ));

        register(new BasicMapper<>(
            Long.TYPE, Types.BIGINT, PreparedStatement::setLong, ResultSet::getLong, CallableStatement::getLong
        ));
        register(new WrapperMapper<>(
            Long.class, Types.BIGINT, PreparedStatement::setLong, ResultSet::getLong, CallableStatement::getLong
        ));

        register(new BasicMapper<>(
            Double.TYPE, Types.DOUBLE, PreparedStatement::setDouble, ResultSet::getDouble, CallableStatement::getDouble
        ));
        register(new WrapperMapper<>(
            Double.class, Types.DOUBLE, PreparedStatement::setDouble, ResultSet::getDouble, CallableStatement::getDouble
        ));

        register(new BasicMapper<>(
            String.class, Types.VARCHAR, PreparedStatement::setString, ResultSet::getString, CallableStatement::getString
        ));
        register(new BasicMapper<>(
            byte[].class, Types.VARBINARY, PreparedStatement::setBytes, ResultSet::getBytes, CallableStatement::getBytes
        ));
        register(new BasicMapper<>(
            Timestamp.class, Types.TIMESTAMP, PreparedStatement::setTimestamp, ResultSet::getTimestamp, CallableStatement::getTimestamp
        ));
        register(new BasicMapper<>(
            Date.class, Types.DATE, PreparedStatement::setDate, ResultSet::getDate, CallableStatement::getDate
        ));
        register(new BasicMapper<>(
            Time.class, Types.TIME, PreparedStatement::setTime, ResultSet::getTime, CallableStatement::getTime
        ));
    }
}
