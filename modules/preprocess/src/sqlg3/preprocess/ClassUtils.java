package sqlg3.preprocess;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
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

    private static List<String> tokenizePackage(String pack) {
        List<String> parts = new ArrayList<>();
        if (pack != null) {
            StringTokenizer tok = new StringTokenizer(pack, ".");
            while (tok.hasMoreTokens()) {
                String t = tok.nextToken();
                parts.add(t);
            }
        }
        return parts;
    }

    static Path packageDir(Path baseDir, String pack) {
        Path ret = baseDir;
        List<String> parts = tokenizePackage(pack);
        for (String part : parts) {
            ret = ret.resolve(part);
        }
        return ret;
    }

    static String resolvePackage(String base, String path) {
        if (!path.startsWith("."))
            return path;
        List<String> parts = tokenizePackage(base);
        ArrayDeque<String> deque = new ArrayDeque<>(parts);
        StringTokenizer tok = new StringTokenizer(path, "/\\");
        while (tok.hasMoreTokens()) {
            String t = tok.nextToken();
            if (".".equals(t))
                continue;
            if ("..".equals(t)) {
                deque.removeLast();
            } else {
                deque.addLast(t);
            }
        }
        if (deque.isEmpty()) {
            return null;
        } else {
            return String.join(".", deque);
        }
    }
}
