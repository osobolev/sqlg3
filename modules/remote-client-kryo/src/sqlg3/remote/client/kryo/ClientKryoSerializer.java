package sqlg3.remote.client.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import sqlg3.core.IDBCommon;
import sqlg3.remote.client.IClientSerializer;
import sqlg3.remote.common.HttpCommand;
import sqlg3.remote.common.HttpId;
import sqlg3.remote.common.HttpResult;
import sqlg3.remote.common.UnrecoverableRemoteException;
import sqlg3.remote.kryo.BaseKryoSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class ClientKryoSerializer extends BaseKryoSerializer implements IClientSerializer {

    @Override
    public HttpResult clientToServer(ReqRespProcessor processor, HttpId id, HttpCommand command,
                                     Class<? extends IDBCommon> iface, Type retType, String method, Class<?>[] paramTypes,
                                     Object[] params) throws IOException {
        boolean debug = isDebug(method);
        if (debug) {
            logClientCall(command, iface, method, retType);
        }
        return processor.process(new ReqRespConsumer() {

            @Override
            public void writeToServer(OutputStream stream) throws IOException {
                try (Output output = writeData(count(stream, debug))) {
                    Kryo kryo = getKryo();

                    kryo.writeObject(output, id);
                    output.writeByte(command.ordinal());
                    kryo.writeObjectOrNull(output, iface, Class.class);
                    kryo.writeObjectOrNull(output, method, String.class);
                    kryo.writeObjectOrNull(output, paramTypes, Class[].class);
                    kryo.writeObjectOrNull(output, params, Object[].class);
                } catch (KryoException ex) {
                    throw new UnrecoverableRemoteException(ex);
                }
            }

            @Override
            public HttpResult readFromServer(InputStream stream) throws IOException {
                try (Input input = readData(count(stream, debug))) {
                    Kryo kryo = getKryo();
                    Object result = kryo.readClassAndObject(input);
                    Throwable error = (Throwable) kryo.readClassAndObject(input);
                    return new HttpResult(result, error);
                } catch (KryoException ex) {
                    throw new UnrecoverableRemoteException(ex);
                }
            }
        });
    }
}
