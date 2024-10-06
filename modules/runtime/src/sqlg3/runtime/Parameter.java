package sqlg3.runtime;

import sqlg3.types.SQLGException;

import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * This class represents query parameter (both IN or OUT). Parameter values are usually obtained by
 * {@link GBase#in} or {@link GBase#out} methods.
 */
public final class Parameter {

    private final Object inputValue;
    private final boolean input;
    /**
     * Array, always not null for OUT parameters
     */
    private final Object outputValue;
    private final boolean output;
    private final Class<Object> cls;

    @SuppressWarnings("unchecked")
    private Parameter(Object inputValue, boolean input,
                      Object outputValue, boolean output,
                      Class<?> cls) {
        this.inputValue = inputValue;
        this.input = input;
        this.outputValue = outputValue;
        this.output = output;
        this.cls = (Class<Object>) cls;
    }

    /**
     * Creates parameter with value and its type. Use this method only if you really need to, {@link GBase#in} is
     * more type safe.
     *
     * @param value parameter value (can be null)
     * @param cls parameter type (should be not null)
     */
    public static Parameter in(Object value, Class<?> cls) {
        return new Parameter(
            value, true,
            null, false,
            cls
        );
    }

    static Parameter out(Object output) {
        return new Parameter(
            null, false,
            output, true,
            output.getClass().getComponentType()
        );
    }

    /**
     * Creates IN/OUT parameter. Should be an array with at least one element. Its input value is array[0]
     * and output value is stored in array[0] as well.
     *
     * @param array an array with at least one element
     */
    public static Parameter inOut(Object array) {
        return new Parameter(
            Array.get(array, 0), true,
            array, true,
            array.getClass().getComponentType()
        );
    }

    public boolean isOut() {
        return output;
    }

    public String toString() {
        if (inputValue != null) {
            String str = inputValue.toString();
            if (str != null && str.length() > 1000) {
                return str.substring(0, 1000) + "...";
            }
            return str;
        } else if (cls != null) {
            return "type " + cls;
        } else {
            return "?";
        }
    }

    private TypeMapper<Object> getMapper(RuntimeMapper mappers) {
        return mappers.getMapper(cls);
    }

    private int set(PreparedStatement st, int index, TypeMapper<Object> mapper) throws SQLException {
        if (input) {
            mapper.set(st, index, inputValue);
        }
        if (isOut()) {
            if (st instanceof CallableStatement) {
                mapper.register((CallableStatement) st, index);
            } else {
                throw new SQLGException("You can pass OUT parameter only to CallableStatement");
            }
        }
        return mapper.getStatementParameters();
    }

    int set(RuntimeMapper mappers, PreparedStatement st, int index) throws SQLException {
        if (Parameter.class.equals(cls)) {
            Parameter nested = (Parameter) inputValue;
            return nested.set(mappers, st, index);
        } else {
            TypeMapper<Object> mapper = getMapper(mappers);
            return set(st, index, mapper);
        }
    }

    /**
     * Binds {@link PreparedStatement} parameters to values from array.
     * Can be used for {@link CallableStatement}, in this case it is possible
     * to use {@link Parameter} in array.
     *
     * @param in values array
     */
    static void setParameters(RuntimeMapper mappers, PreparedStatement st, List<Parameter> in) throws SQLException {
        if (in == null)
            return;
        int index = 1;
        for (Parameter param : in) {
            index += param.set(mappers, st, index);
        }
    }

    static void getOutParameters(RuntimeMapper mappers, CallableStatement cs, List<Parameter> params) throws SQLException {
        if (params == null)
            return;
        int index = 1;
        for (Parameter param : params) {
            TypeMapper<Object> mapper = param.getMapper(mappers);
            if (param.isOut()) {
                Object value = mapper.get(cs, index);
                if (param.outputValue != null) {
                    Array.set(param.outputValue, 0, value);
                }
            }
            index += mapper.getStatementParameters();
        }
    }
}
