package sqlg3.preprocess;

import sqlg3.runtime.GContext;
import sqlg3.runtime.GTest;
import sqlg3.runtime.TypeMappers;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

final class MethodRunner {

    private final GTestImpl test;
    private final TypeMappers mappers;
    private final Class<?> cls;
    private final String displayClassName;
    private final List<MethodEntry> entries;
    private final PrintStream log;

    MethodRunner(GTestImpl test, TypeMappers mappers, Class<?> cls, String displayClassName,
                 List<MethodEntry> entries, PrintStream log) {
        this.test = test;
        this.mappers = mappers;
        this.cls = cls;
        this.displayClassName = displayClassName;
        this.entries = entries;
        this.log = log;
    }

    private Object[] getTestParams(Method method) throws ParseException {
        Class<?>[] types = method.getParameterTypes();
        Object[] ret = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            ret[i] = test.getTestObject(types[i]);
            if (ret[i] == null) {
                throw new ParseException("Non-standard type in entry params: " + types[i]);
            }
        }
        return ret;
    }

    private static final class CallContext implements AutoCloseable {

        private final Connection connection;

        CallContext(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void close() throws SQLException {
            connection.rollback();
        }
    }

    List<RunMethod> checkEntries(Map<String, List<ParamCutPaste>> bindMap, List<String> allParameters) throws Throwable {
        Map<String, List<Method>> methodMap = Arrays.stream(cls.getDeclaredMethods()).collect(Collectors.groupingBy(Method::getName));
        if (log != null) {
            log.println(cls.getCanonicalName());
        }
        test.startClass(bindMap);
        List<RunMethod> entryMethods = new ArrayList<>();
        for (MethodEntry entry : entries) {
            String displayEntryName = displayClassName + "." + entry.methodToCall;
            List<Method> methods = methodMap.get(entry.methodToCall);
            if (methods == null) {
                throw new ParseException("Entry " + displayEntryName + " not found");
            } else if (methods.size() != 1) {
                throw new ParseException("Entry " + displayEntryName + " occured more than once");
            }
            Method toCall = methods.get(0);
            int modifiers = toCall.getModifiers();
            if ((modifiers & Modifier.STATIC) != 0) {
                throw new ParseException("Business method " + displayEntryName + " cannot be static");
            }
            if (entry.publish) {
                if ((modifiers & Modifier.PUBLIC) == 0) {
                    throw new ParseException("Business method " + displayEntryName + " should be public");
                }
            } else {
                toCall.setAccessible(true);
            }
            entryMethods.add(new RunMethod(entry, toCall));
            if (log != null) {
                log.println(toCall.getName());
            }
            test.startCall(displayEntryName);
            Constructor<?> constructor = cls.getConstructor(GContext.class);
            try (CallContext call = new CallContext(test.connection);
                 GContext ctx = GTest.testContext(call.connection, test.checker.getSpecific(), mappers)) {
                try {
                    Object instance = constructor.newInstance(ctx);
                    toCall.invoke(instance, getTestParams(toCall));
                } catch (InvocationTargetException itex) {
                    throw itex.getTargetException();
                }
            }
        }
        Map<String, Class<?>> paramTypeMap = test.endClass();
        checkParamTypes(allParameters, paramTypeMap);

        return entryMethods;
    }

    private static void checkParamTypes(List<String> allParameters, Map<String, Class<?>> paramTypeMap) throws ParseException {
        Set<String> missingParams = new LinkedHashSet<>(allParameters);
        missingParams.removeAll(paramTypeMap.keySet());
        if (!missingParams.isEmpty()) {
            throw new ParseException("Type is not known for parameters: " + String.join(", ", missingParams));
        }
    }
}
