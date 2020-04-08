package sqlg3.remote.common;

import java.io.*;

public abstract class BaseJavaSerializer extends BaseSerializer<ObjectInputStream, ObjectOutputStream> {

    @Override
    protected ObjectOutputStream write(OutputStream os) throws IOException {
        return new ObjectOutputStream(os);
    }

    @Override
    protected ObjectInputStream read(InputStream is) throws IOException {
        return new ObjectInputStream(is);
    }
}
