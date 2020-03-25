package sqlg3.preprocess;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.StringTokenizer;

final class ClassUtils {

    private static final String JAVA_LANG_PREFIX = "java.lang.";

    static String getClassName(Type type) {
        int count = 0;
        String name;
        if (type instanceof Class<?>) {
            Class<?> cls = (Class<?>) type;
            while (cls.isArray()) {
                cls = cls.getComponentType();
                count++;
            }
            name = cls.getName();
            if (name.startsWith(JAVA_LANG_PREFIX)) {
                name = name.substring(JAVA_LANG_PREFIX.length());
            }
        } else {
            name = type.toString();
        }
        name = name.replace('$', '.');
        StringBuilder buf = new StringBuilder(name);
        for (int i = 0; i < count; i++) {
            buf.append("[]");
        }
        return buf.toString();
    }

    static Path packageDir(Path baseDir, String pack) {
        Path ret = baseDir;
        if (pack != null) {
            StringTokenizer tok = new StringTokenizer(pack, ".");
            while (tok.hasMoreTokens()) {
                String t = tok.nextToken();
                ret = ret.resolve(t);
            }
        }
        return ret;
    }
}
