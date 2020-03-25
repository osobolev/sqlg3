package sqlg3.preprocess;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

final class ClassCompiler {

    private final Path tmpDir;

    ClassCompiler(Path tmpDir) {
        this.tmpDir = tmpDir;
    }

    private void compile(List<Path> srcRoots, Charset encoding, String classpath, Path file) throws ParseException {
        List<String> params = new ArrayList<>(Arrays.asList(
            "-sourcepath", srcRoots.stream().map(p -> p.toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator))
        ));
        if (classpath != null) {
            params.addAll(Arrays.asList(
                "-classpath", classpath
            ));
        }
        params.addAll(Arrays.asList(
            "-d", tmpDir.toAbsolutePath().toString(),
            file.toAbsolutePath().toString()
        ));
        if (encoding != null) {
            params.addAll(0, Arrays.asList("-encoding", encoding.name()));
        }

        String[] arguments = params.toArray(new String[0]);
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            if (compiler.run(null, buf, buf, arguments) != 0) {
                throw new ParseException(buf.toString());
            }
        } catch (ParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ParseException(ex.getMessage());
        }
    }

    private static boolean isFromSameSource(String className, String name) {
        if (name.equals(className))
            return true;
        if (name.startsWith(className)) {
            return name.substring(className.length()).startsWith("$");
        } else {
            return false;
        }
    }

    Class<?> compileAndLoad(List<Path> srcRoots, Path file, String className, Charset encoding, String classpath) throws MalformedURLException, ClassNotFoundException, ParseException {
        compile(srcRoots, encoding, classpath, file);

        StringTokenizer tok = new StringTokenizer(classpath, File.pathSeparator);
        URL[] urls = new URL[1 + tok.countTokens()];
        urls[0] = tmpDir.toUri().toURL();
        int i = 1;
        while (tok.hasMoreTokens()) {
            String t = tok.nextToken();
            urls[i] = Paths.get(t).toUri().toURL();
            i++;
        }
        URLClassLoader cll = new URLClassLoader(urls, getClass().getClassLoader()) {
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (isFromSameSource(className, name)) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        c = findClass(name);
                    }
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                } else {
                    return super.loadClass(name, resolve);
                }
            }
        };
        return cll.loadClass(className);
    }
}
