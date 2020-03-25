package sqlg3.preprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class RowTypeInfo {

    private final String displayEntryName;
    private final List<ColumnInfo> columns;
    private final boolean meta;

    RowTypeInfo(String displayEntryName, List<ColumnInfo> columns, boolean meta) {
        this.displayEntryName = displayEntryName;
        this.columns = columns;
        this.meta = meta;
    }

    String generateRowTypeBody(String start, String tab, Class<?> cls) {
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

    private static Class<?> unwrap(Class<?> cls) {
        if (Integer.class.equals(cls)) {
            return Integer.TYPE;
        } else if (Long.class.equals(cls)) {
            return Long.TYPE;
        } else if (Short.class.equals(cls)) {
            return Short.TYPE;
        } else if (Byte.class.equals(cls)) {
            return Byte.TYPE;
        } else if (Float.class.equals(cls)) {
            return Float.TYPE;
        } else if (Double.class.equals(cls)) {
            return Double.TYPE;
        } else if (Character.class.equals(cls)) {
            return Character.TYPE;
        } else if (Boolean.class.equals(cls)) {
            return Boolean.TYPE;
        } else {
            return null;
        }
    }

    private static int getIntegerSize(Class<?> primitive) {
        if (Integer.TYPE.equals(primitive)) {
            return 4;
        } else if (Long.TYPE.equals(primitive)) {
            return 8;
        } else if (Short.TYPE.equals(primitive)) {
            return 2;
        } else if (Byte.TYPE.equals(primitive)) {
            return 1;
        } else {
            return 0;
        }
    }

    private static int getFloatingSize(Class<?> primitive) {
        if (Double.TYPE.equals(primitive)) {
            return 8;
        } else if (Float.TYPE.equals(primitive)) {
            return 4;
        } else {
            return 0;
        }
    }

    private static Class<?> getWiderOf(Class<?> cls1, Class<?> cls2) {
        Class<?> primitive1;
        if (cls1.isPrimitive()) {
            primitive1 = cls1;
        } else {
            primitive1 = unwrap(cls1);
        }
        Class<?> primitive2;
        if (cls2.isPrimitive()) {
            primitive2 = cls2;
        } else {
            primitive2 = unwrap(cls2);
        }
        if (primitive1 == null || primitive2 == null)
            return null;
        if (Objects.equals(primitive1, primitive2)) {
            // Return non-primitive type as more permissive
            return !cls1.isPrimitive() ? cls1 : cls2;
        } else {
            int i1 = getIntegerSize(primitive1);
            int i2 = getIntegerSize(primitive2);
            if (i1 > 0 && i2 > 0) {
                return i1 > i2 ? cls1 : cls2;
            }
            int f1 = getFloatingSize(primitive1);
            int f2 = getFloatingSize(primitive2);
            if (f1 > 0 && f2 > 0) {
                return f1 > f2 ? cls1 : cls2;
            }
            if (i1 > 0 && f2 > 0)
                return cls2;
            if (f1 > 0 && i2 > 0)
                return cls1;
            return null;
        }
    }

    private static String vs(RowTypeInfo type0, RowTypeInfo type, Function<RowTypeInfo, Object> field) {
        return field.apply(type) + " in " + type.displayEntryName + " vs " +
               field.apply(type0) + " in " + type0.displayEntryName;
    }

    private static String warnTypeChange(Class<?> rowTypeClass, ColumnInfo column0,
                                         Class<?> wasType, RowTypeInfo was,
                                         Class<?> becameType, RowTypeInfo became) {
        return "Column " + column0.name + " of " + rowTypeClass.getSimpleName() +
               " had type " + wasType.getCanonicalName() + " in " + was.displayEntryName +
               ", but became type " + becameType.getCanonicalName() + " in " + became.displayEntryName;
    }

    interface WarningConsumer {

        void accept(String warning) throws ParseException;
    }

    static RowTypeInfo checkCompatibility(Class<?> rowTypeClass, List<RowTypeInfo> types, WarningConsumer warnings) throws ParseException {
        RowTypeInfo type0 = types.get(0);
        int columnCount = type0.columns.size();
        for (int i = 1; i < types.size(); i++) {
            RowTypeInfo type = types.get(i);
            if (type.meta != type0.meta) {
                throw new ParseException(
                    "Column meta differs for " + rowTypeClass.getSimpleName() + ": " +
                    type.displayEntryName + " vs " + type0.displayEntryName
                );
            }
            if (type.columns.size() != columnCount) {
                throw new ParseException(
                    "Column count differs for " + rowTypeClass.getSimpleName() + ": " +
                    vs(type0, type, t -> t.columns.size())
                );
            }
        }
        List<ColumnInfo> widenedColumn = new ArrayList<>();
        for (int j = 0; j < columnCount; j++) {
            int finalJ = j;
            ColumnInfo column0 = type0.columns.get(j);
            Class<?> widened = column0.type;
            RowTypeInfo widenedSource = type0;
            for (int i = 1; i < types.size(); i++) {
                RowTypeInfo type = types.get(i);
                ColumnInfo column = type.columns.get(j);
                if (!Objects.equals(column.name, column0.name)) {
                    throw new ParseException(
                        "Different names for column " + (j + 1) + " of " + rowTypeClass.getSimpleName() + ": " +
                        vs(type0, type, t -> t.columns.get(finalJ).name)
                    );
                }
                Class<?> cls = column.type;
                if (Objects.equals(widened, cls))
                    continue;
                Class<?> wider = getWiderOf(widened, cls);
                if (wider != null) {
                    String warning;
                    if (wider.equals(widened)) {
                        warning = warnTypeChange(
                            rowTypeClass, column0,
                            cls, type,
                            widened, widenedSource
                        );
                    } else {
                        warning = warnTypeChange(
                            rowTypeClass, column0,
                            widened, widenedSource,
                            cls, type
                        );
                        widened = wider;
                        widenedSource = type;
                    }
                    warnings.accept(warning);
                } else {
                    throw new ParseException(
                        "Different types for column " + column0.name + " of " + rowTypeClass.getSimpleName() + ": " +
                        vs(type0, type, t -> t.columns.get(finalJ).type.getCanonicalName())
                    );
                }
            }
            widenedColumn.add(new ColumnInfo(widened, column0.name));
        }
        return new RowTypeInfo(type0.displayEntryName, widenedColumn, type0.meta);
    }
}
