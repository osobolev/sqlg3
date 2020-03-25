package sqlg3.preprocess;

import sqlg3.core.SQLGException;
import sqlg3.runtime.GTest;
import sqlg3.runtime.Parameter;
import sqlg3.runtime.queries.QueryParser;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GTestImpl extends GTest {

    private final Map<String, Class<?>> paramTypeMap = new HashMap<>();
    private Map<String, List<ParamCutPaste>> bindMap;

    private String displayEntryName;
    private final Map<Statement, String> stmtMap = new HashMap<>();

    final Connection connection;
    final SqlChecker checker;
    final Mapper mapper;
    private final Map<Class<?>, List<RowTypeInfo>> generatedIn;
    private final Map<Class<?>, List<RowTypeInfo>> generatedOut;

    GTestImpl(Connection connection, SqlChecker checker, Mapper mapper,
              Map<Class<?>, List<RowTypeInfo>> generatedIn, Map<Class<?>, List<RowTypeInfo>> generatedOut) {
        this.connection = connection;
        this.checker = checker;
        this.mapper = mapper;
        this.generatedIn = generatedIn;
        this.generatedOut = generatedOut;
    }

    private Object getValue(Class<?> cls) {
        if (cls != null)
            return getTestObject(cls);
        return null;
    }

    @Override
    public <T> T getNullInterface(Class<T> iface) {
        Object ret = Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class[] {iface},
            (proxy, method, args) -> getValue(method.getReturnType())
        );
        return iface.cast(ret);
    }

    @Override
    public Object getTestObject(Class<?> paramType) {
        return mapper.getTestObject(paramType);
    }

    @Override
    public void getRowTypeFields(Class<?> rowType, ResultSet rs, boolean meta) throws SQLException {
        List<ColumnInfo> columns = mapper.getFields(rs.getMetaData(), meta);
        RowTypeInfo info = new RowTypeInfo(displayEntryName, columns, meta);

        Map<Class<?>, List<RowTypeInfo>> generated = rowType.getDeclaringClass() == null ? generatedOut : generatedIn;
        generated.computeIfAbsent(rowType, k -> new ArrayList<>()).add(info);
    }

    private ColumnInfo getOneColumn(ResultSet rs) throws SQLException {
        List<ColumnInfo> columns = mapper.getFields(rs.getMetaData(), false);
        if (columns.size() != 1) {
            throw new SQLException("More than one column in result set");
        }
        return columns.get(0);
    }

    @Override
    public void checkOneColumn(ResultSet rs, Class<?> cls) throws SQLException {
        ColumnInfo col1 = getOneColumn(rs);
        if (!cls.equals(col1.type))
            throw new SQLException("Column is of type " + col1.type.getCanonicalName() + ", but " + cls.getCanonicalName() + " required");
    }

    @Override
    public void checkSql(PreparedStatement stmt) throws SQLException {
        checker.checkStatement(stmt);
        String sql = stmtMap.get(stmt);
        if (sql == null)
            return;
        try {
            checker.checkSql(connection, sql);
        } catch (SQLException ex) {
            throw new SQLException("Invalid SQL: " + sql + "\n" + ex.getMessage(), ex);
        }
    }

    @Override
    public void statementCreated(Statement stmt, String sql) {
        if (sql != null) {
            stmtMap.put(stmt, sql);
        }
    }

    @Override
    public Class<?> setParamType(String paramId, Class<?> paramClass) {
        Class<?> cls = mapper.getParameterClass(paramClass);
        List<ParamCutPaste> params = bindMap.get(paramId);
        if (params != null) {
            String className = ClassUtils.getClassName(cls);
            for (ParamCutPaste param : params) {
                if (param.out) {
                    param.replaceTo = "outP(" + param.param + ")";
                } else {
                    param.replaceTo = "inP(" + param.param + ", " + className + ".class)";
                }
            }
        }
        Class<?> existingClass = paramTypeMap.get(paramId);
        if (existingClass != null) {
            if (!cls.equals(existingClass)) {
                throw new SQLGException("Parameter " + paramId + " has conflicting type definitions: was " + existingClass + ", became " + cls);
            }
        }
        paramTypeMap.put(paramId, cls);
        return cls;
    }

    private static String kindStr(boolean out) {
        return out ? "OUT" : "IN";
    }

    private static void checkStoredProcParameters(DatabaseMetaData meta,
                                                  String procNameToCall, Parameter[] parameters,
                                                  String foundCatalog, String foundSchema, String procName) throws SQLException {
        int paramCount;
        try (ResultSet rs = meta.getProcedureColumns(foundCatalog, foundSchema, procName, null)) {
            paramCount = 0;
            while (rs.next()) {
                int paramKind = rs.getShort(5);
                if (paramKind != DatabaseMetaData.procedureColumnUnknown) {
                    boolean needOut = paramKind == DatabaseMetaData.procedureColumnInOut || paramKind == DatabaseMetaData.procedureColumnOut;
                    boolean actualOut = parameters[paramCount].isOut();
                    if (needOut != actualOut) {
                        String paramName = rs.getString(4);
                        if (paramName == null) {
                            paramName = "_" + (paramCount + 1);
                        }
                        throw new SQLException("Parameter mismatch in call to " + procNameToCall + ": parameter " + paramName + " declared as " + kindStr(needOut) + " but passed as " + kindStr(actualOut));
                    }
                }
                paramCount++;
            }
        }
        if (paramCount != parameters.length)
            throw new SQLException("Parameter count does not match in call to " + procNameToCall + " (expected " + paramCount + " != actual " + parameters.length + ")");
    }

    @Override
    public void checkStoredProcName(String procNameToCall, Parameter[] parameters) throws SQLException {
        String[] parsed = QueryParser.parseIdent(procNameToCall);
        if (parsed == null)
            throw new SQLException("Invalid procedure name: " + procNameToCall);
        DatabaseMetaData meta = connection.getMetaData();
        String[] procSchemas;
        String[] procCatalogs;
        String procName;
        if (parsed.length == 1) {
            procSchemas = new String[] {checker.getCurrentSchema(meta)};
            procCatalogs = new String[] {null};
            procName = parsed[0];
        } else if (parsed.length == 2) {
            procSchemas = new String[] {checker.getCurrentSchema(meta), parsed[0]};
            procCatalogs = new String[] {parsed[0], null};
            procName = parsed[1];
        } else {
            procSchemas = new String[] {parsed[0]};
            procCatalogs = new String[] {parsed[1]};
            procName = parsed[2];
        }
        String procSchema;
        if (procSchemas.length > 1) {
            procSchema = null;
        } else {
            procSchema = procSchemas[0];
        }
        String procCatalog;
        if (procCatalogs.length > 1) {
            procCatalog = null;
        } else {
            procCatalog = procCatalogs[0];
        }
        String foundSchema = null;
        String foundCatalog = null;
        int foundCount = 0;
        Integer returnType = null;
        try (ResultSet rs = meta.getProcedures(procCatalog, procSchema, procName)) {
            while (rs.next()) {
                String catalog = rs.getString(1);
                String schema = rs.getString(2);
                boolean found = false;
                for (int j = 0; j < procSchemas.length; j++) {
                    boolean schemaEqual;
                    if (schema == null || schema.length() <= 0) {
                        schemaEqual = procSchemas[j] == null || procSchemas[j].length() <= 0;
                    } else {
                        schemaEqual = schema.equals(procSchemas[j]);
                    }
                    if (!schemaEqual)
                        continue;
                    boolean catalogEqual;
                    if (catalog == null || catalog.length() <= 0) {
                        catalogEqual = procCatalogs[j] == null;
                    } else {
                        catalogEqual = catalog.equals(procCatalogs[j]);
                    }
                    if (!catalogEqual)
                        continue;
                    found = true;
                    break;
                }
                if (found) {
                    foundSchema = schema;
                    foundCatalog = catalog;
                    returnType = rs.getInt(8);
                    foundCount++;
                }
            }
        }
        if (foundCount <= 0) {
            throw new SQLException("Procedure " + procNameToCall + " not found");
        }
        if (foundCount == 1) {
            if (returnType != null && returnType.intValue() == DatabaseMetaData.procedureReturnsResult)
                throw new SQLException(procNameToCall + " is a function, not a procedure");
            checkStoredProcParameters(meta, procNameToCall, parameters, foundCatalog, foundSchema, procName);
        }
    }

    @Override
    public void checkSequenceExists(String sequence) throws SQLException {
        checker.checkSequenceExists(connection, sequence);
    }

    void startClass(Map<String, List<ParamCutPaste>> bindMap) {
        this.bindMap = bindMap;
        this.paramTypeMap.clear();
    }

    Map<String, Class<?>> endClass() {
        return paramTypeMap;
    }

    void startCall(String displayEntryName) {
        this.displayEntryName = displayEntryName;
        this.stmtMap.clear();
    }
}
