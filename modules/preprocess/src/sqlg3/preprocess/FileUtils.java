package sqlg3.preprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class FileUtils {

    // todo: remove this crap
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

    // todo: remove this crap
    static void writeFile(Path file, String text, Charset encoding) throws IOException {
        try (Writer wr = open(file, encoding)) {
            wr.write(text);
        }
    }

    // todo: remove this crap
    static void copyFile(Path in, Path out) throws IOException {
        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
    }

    // todo: remove this crap
    static PrintWriter open(Path file, Charset encoding) throws IOException {
        Writer writer = Files.newBufferedWriter(file, encoding);
        return new PrintWriter(new EolnWriter(writer));
    }
}
