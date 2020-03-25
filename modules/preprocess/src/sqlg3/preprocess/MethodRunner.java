package sqlg3.preprocess;

import sqlg3.runtime.GContext;
import sqlg3.runtime.GTest;
import sqlg3.runtime.TypeMappers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

final class MethodRunner {

    private final GTestImpl test = GTestImpl.INSTANCE;
    private final TypeMappers mappers;
    private final Class<?> cls;
    private final String displayClassName;
    private final List<MethodEntry> entries;
    private final boolean log;

    private final Map<String, List<RowTypeInfo>> generatedIn;
    private final Map<String, List<RowTypeInfo>> generatedOut;

    MethodRunner(TypeMappers mappers, Class<?> cls, String displayClassName,
                 List<MethodEntry> entries, boolean log,
                 Map<String, List<RowTypeInfo>> generatedIn, Map<String, List<RowTypeInfo>> generatedOut) {
        this.mappers = mappers;
        this.cls = cls;
        this.displayClassName = displayClassName;
        this.entries = entries;
        this.log = log;
        this.generatedIn = generatedIn;
        this.generatedOut = generatedOut;
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

    List<RunMethod> checkEntries(Map<String, List<ParamCutPaste>> bindMap, List<String> allParameters) throws Throwable {
        Map<String, List<Method>> methodMap = Arrays.stream(cls.getDeclaredMethods()).collect(Collectors.groupingBy(Method::getName));
        GTest.setTest(test);
        if (log) {
            System.out.println(cls.getCanonicalName());
        }
        test.paramTypeMap.clear();
        test.bindMap = bindMap;
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
            if (log) {
                System.out.println(toCall.getName());
            }
            test.startCall();
            Constructor<?> cons = cls.getConstructor(GContext.class);
            try (GContext ctx = GTest.testContext(test.connection, test.checker.getSpecific(), mappers)) {
                Object inst = cons.newInstance(ctx);
                try {
                    toCall.invoke(inst, getTestParams(toCall));
                } catch (InvocationTargetException itex) {
                    throw itex.getTargetException();
                }
            } finally {
                test.connection.rollback(); // todo: overrides existing exception!!!
            }
            Class<?> rowTypeClass = test.returnClass;
            if (rowTypeClass != null) {
                saveRowTypeInfo(
                    rowTypeClass.getDeclaringClass() == null ? generatedOut : generatedIn,
                    displayEntryName, rowTypeClass, test.meta
                );
            }
        }
        checkParamTypes(allParameters);

        return entryMethods;
    }

    private void saveRowTypeInfo(Map<String, List<RowTypeInfo>> generated, String displayEntryName, Class<?> rowType, boolean meta) throws ParseException {
        List<ColumnInfo> columns = test.columns;
        if (columns == null)
            throw new ParseException("Method " + displayEntryName + " should perform SELECT for " + rowType.getSimpleName());
        String key = rowType.getName();
        RowTypeInfo newData = new RowTypeInfo(rowType, displayEntryName, columns, meta);
        generated.computeIfAbsent(key, k -> new ArrayList<>()).add(newData);
    }

    private void checkParamTypes(List<String> allParameters) throws ParseException {
        Set<String> missingParams = new HashSet<>(allParameters);
        missingParams.removeAll(test.paramTypeMap.keySet());
        if (missingParams.size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (String param : allParameters) {
                if (missingParams.contains(param)) {
                    if (buf.length() > 0) {
                        buf.append(", ");
                    }
                    buf.append(param);
                }
            }
            throw new ParseException("Type is not known for parameters: " + buf);
        }
    }
}
