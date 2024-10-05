package sqlg3.preprocess.checker;

import sqlg3.preprocess.SqlChecker;
import sqlg3.runtime.DBSpecific;
import sqlg3.runtime.Parameter;
import sqlg3.runtime.queries.QueryParser;

import java.sql.*;

/**
 * Base SQL checker for generic JDBC database - does not support sequences.
 */
public class Generic implements SqlChecker {

    private final DBSpecific specific;

    public Generic(DBSpecific specific) {
        this.specific = specific;
    }

    public Generic() {
        this(new sqlg3.runtime.specific.Generic());
    }

    @Override
    public DBSpecific getSpecific() {
        return specific;
    }

    private static String getCurrentSchema(DatabaseMetaData meta) throws SQLException {
        return meta.getUserName();
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
                    if (paramCount >= parameters.length)
                        throw new SQLException("Too few parameters in call to " + procNameToCall + ": " + parameters.length);
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
    public void checkStoredProcName(Connection conn, String procNameToCall, Parameter[] parameters) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        String[] parsed = QueryParser.parseIdent(procNameToCall, meta);
        if (parsed == null)
            throw new SQLException("Invalid procedure name: " + procNameToCall);
        String[] procSchemas;
        String[] procCatalogs;
        String procName;
        if (parsed.length == 1) {
            procSchemas = new String[] {getCurrentSchema(meta)};
            procCatalogs = new String[] {null};
            procName = parsed[0];
        } else if (parsed.length == 2) {
            procSchemas = new String[] {getCurrentSchema(meta), parsed[0]};
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
    public void checkSql(Connection conn, PreparedStatement stmt, String sql) throws SQLException {
        stmt.getParameterMetaData();
    }
}
