package sqlg3.remote.common;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ISerializer {

    interface Writer extends Closeable {

        <T> void write(T obj, Class<T> cls) throws IOException;
    }

    interface Reader extends Closeable {

        <T> T read(Class<T> cls) throws IOException;
    }

    Writer newWriter(OutputStream os) throws IOException;

    Reader newReader(InputStream is) throws IOException;
}
