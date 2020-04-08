package sqlg3.remote.server.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import sqlg3.core.IDBCommon;
import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.UnrecoverableRemoteException;
import sqlg3.remote.kryo.BaseKryoSerializer;
import sqlg3.remote.server.IServerSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class ServerKryoSerializer extends BaseKryoSerializer implements IServerSerializer {

    private static final HttpCommand[] COMMANDS = HttpCommand.values();

    public ServerKryoSerializer(boolean onlyMethods, Consumer<String> logger, LongConsumer onRead, LongConsumer onWrite) {
        super(onlyMethods, logger, onRead, onWrite);
    }

    public ServerKryoSerializer() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serverToClient(InputStream is, ServerCall call, OutputStream os) throws IOException {
        HttpId id;
        HttpCommand command;
        Class<? extends IDBCommon> iface;
        String method;
        Class<?>[] paramTypes;
        Object[] params;
        Kryo kryo = getKryo();
        try (Input input = readData(count(is, true))) {
            id = kryo.readObject(input, HttpId.class);
            int commandIndex = input.readByte();
            iface = kryo.readObjectOrNull(input, Class.class);
            method = kryo.readObjectOrNull(input, String.class);
            paramTypes = kryo.readObjectOrNull(input, Class[].class);
            params = kryo.readObjectOrNull(input, Object[].class);

            command = COMMANDS[commandIndex];
        } catch (KryoException ex) {
            throw new UnrecoverableRemoteException(ex);
        }

        Object result = null;
        Throwable error = null;
        try {
            result = call.call(id, command, iface, method, paramTypes, params);
        } catch (Throwable ex) {
            error = ex;
        }
        writeResponse(kryo, os, result, error, method);
    }

    private void writeResponse(Kryo kryo, OutputStream os, Object result, Throwable error, String method) throws IOException {
        boolean debug = isDebug(method);
        try (Output output = writeData(count(os, debug))) {
            kryo.writeClassAndObject(output, result);
            kryo.writeClassAndObject(output, error);
        } catch (KryoException ex) {
            throw new UnrecoverableRemoteException(ex);
        }
    }

    @Override
    public void sendError(OutputStream os, Throwable error) throws IOException {
        try {
            Kryo kryo = getKryo();
            writeResponse(kryo, os, null, error, null);
        } finally {
            os.close();
        }
    }
}
