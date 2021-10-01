package sqlg3.remote.common;

import java.io.*;

public final class JavaSerializer implements ISerializer {

    public static final class JavaWriter implements Writer {

        private final ObjectOutputStream oos;

        public JavaWriter(ObjectOutputStream oos) {
            this.oos = oos;
        }

        @Override
        public <T> void write(T obj, Class<T> cls) throws IOException {
            oos.writeObject(obj);
        }

        @Override
        public void close() throws IOException {
            oos.close();
        }
    }

    public static final class JavaReader implements Reader {

        private final ObjectInputStream ois;

        public JavaReader(ObjectInputStream ois) {
            this.ois = ois;
        }

        @Override
        public <T> T read(Class<T> cls) throws IOException {
            try {
                return cls.cast(ois.readObject());
            } catch (ClassNotFoundException | InvalidClassException ex) {
                throw new UnrecoverableRemoteException(ex);
            }
        }

        @Override
        public void close() throws IOException {
            ois.close();
        }
    }

    @Override
    public Writer newWriter(OutputStream os) throws IOException {
        return new JavaWriter(new ObjectOutputStream(os));
    }

    @Override
    public Reader newReader(InputStream is) throws IOException {
        return new JavaReader(new ObjectInputStream(is));
    }
}
