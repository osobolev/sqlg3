package sqlg3.remote.common;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public abstract class BaseJavaSerializer extends BaseSerializer<ObjectInputStream, ObjectOutputStream> {

    protected BaseJavaSerializer(boolean onlyMethods, Consumer<String> logger, LongConsumer onRead, LongConsumer onWrite) {
        super(onlyMethods, logger, onRead, onWrite);
    }

    protected BaseJavaSerializer() {
    }

    @Override
    protected ObjectOutputStream write(OutputStream os) throws IOException {
        return new ObjectOutputStream(os);
    }

    @Override
    protected ObjectInputStream read(InputStream is) throws IOException {
        return new ObjectInputStream(is);
    }
}
