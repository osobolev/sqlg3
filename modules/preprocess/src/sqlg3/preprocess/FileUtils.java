package sqlg3.preprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

final class FileUtils {

    static final String JAVA_EXTENSION = ".java";

    static String readFile(Path file, Charset encoding) throws IOException {
        try (BufferedReader rdr = Files.newBufferedReader(file, encoding)) {
            StringBuilder buf = new StringBuilder();
            while (true) {
                String line = rdr.readLine();
                if (line == null)
                    break;
                buf.append(line).append('\n');
            }
            return buf.toString();
        }
    }

    // todo: remove this in Java 11:
    static void writeFile(Path file, String text, Charset encoding) throws IOException {
        try (Writer wr = open(file, encoding)) {
            wr.write(text);
        }
    }

    static PrintWriter open(Path file, Charset encoding) throws IOException {
        Writer writer = Files.newBufferedWriter(file, encoding);
        return new PrintWriter(new EolnWriter(writer));
    }

    static boolean isModified(Path src, Path target) throws IOException {
        if (Files.exists(target)) {
            FileTime srcLastModified = Files.getLastModifiedTime(src);
            FileTime targetLastModified = Files.getLastModifiedTime(target);
            return srcLastModified.compareTo(targetLastModified) > 0;
        } else {
            return true;
        }
    }

    static void deleteRecursively(Path path) {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                for (Path child : children) {
                    deleteRecursively(child);
                }
            } catch (IOException ex) {
                // ignore
            }
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // ignore
        }
    }

    static String getJavaClassName(Path file) {
        String fileName = file.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(JAVA_EXTENSION))
            return null;
        return fileName.substring(0, fileName.length() - JAVA_EXTENSION.length());
    }

    static void listAllJavaFiles(Path path, List<Path> javaFiles) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                for (Path child : children) {
                    listAllJavaFiles(child, javaFiles);
                }
            }
        } else {
            if (getJavaClassName(path) != null) {
                javaFiles.add(path);
            }
        }
    }
}
