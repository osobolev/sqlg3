package sqlg3.preprocess;

import java.util.List;

final class RowTypeInfo {

    final Class<?> cls;
    private final String displayEntryName;
    private final List<ColumnInfo> columns;
    private final boolean meta;

    RowTypeInfo(Class<?> cls, String displayEntryName, List<ColumnInfo> columns, boolean meta) {
        this.cls = cls;
        this.displayEntryName = displayEntryName;
        this.columns = columns;
        this.meta = meta;
    }

    String generateRowTypeBody(String start, String tab) {
        StringBuilder fields = new StringBuilder();

        fields.append("\n\n");

        StringBuilder constructor = new StringBuilder();
        constructor.append('\n');
        constructor.append(start).append(tab).append("public " + cls.getSimpleName() + "(");

        StringBuilder getters = new StringBuilder();
        getters.append('\n');

        for (int j = 0; j < columns.size(); j++) {
            ColumnInfo column = columns.get(j);
            String type = meta ? "sqlg3.core.MetaColumn" : ClassUtils.getClassName(column.type);

            fields.append(start).append(tab).append("private final ");
            fields.append(type).append(' ').append(column.name).append(";\n");

            if (j > 0) {
                constructor.append(", ");
            }
            constructor.append(type).append(' ').append(column.name);

            getters.append(start).append(tab).append("public ");
            getters.append(type).append(' ').append(column.name);
            getters.append("() { return ").append(column.name).append("; }\n");
        }

        constructor.append(") {\n");
        for (ColumnInfo col : columns) {
            String field = col.name;
            constructor.append(start).append(tab).append(tab).append("this." + field + " = " + field + ";\n");
        }
        constructor.append(start).append(tab).append("}\n");

        return fields.toString() + constructor + getters;
    }

    static void checkCompatibility(Class<?> rowTypeClass, RowTypeInfo cols1, RowTypeInfo cols2) throws ParseException {
        if (cols1.meta != cols2.meta) {
            throw new ParseException(
                "Column meta differs for " + rowTypeClass.getSimpleName() + ": " +
                cols1.displayEntryName + " vs " + cols2.displayEntryName
            );
        }
        if (cols1.columns.size() != cols2.columns.size()) {
            throw new ParseException(
                "Column count differs for " + rowTypeClass.getSimpleName() + ": " +
                cols1.columns.size() + " in " + cols1.displayEntryName + ", " +
                cols2.columns.size() + " in " + cols2.displayEntryName
            );
        }
        for (int i = 0; i < cols1.columns.size(); i++) {
            ColumnInfo col1 = cols1.columns.get(i);
            ColumnInfo col2 = cols2.columns.get(i);
            boolean differentType = !col1.type.getName().equals(col2.type.getName());
            if (differentType) {
                String error =
                    "Different types for " + col1.name +
                    " in " + cols1.displayEntryName + " (" + col1.type.getCanonicalName() + ")" +
                    " and " + cols2.displayEntryName + " (" + col2.type.getCanonicalName() + ")";
                // todo: allow to print only warning if difference is primitive vs wrapper!!!
                throw new ParseException(error);
            }
        }
    }
}
