package sqlg3.remote.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import sqlg3.remote.common.BaseSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public abstract class BaseKryoSerializer extends BaseSerializer<Input, Output> {

    private Consumer<Kryo> kryoCustomizer = BaseKryoSerializer::setupKryo;

    protected final ThreadLocal<Kryo> kryos = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryoCustomizer.accept(kryo);
        return kryo;
    });

    public static void setupKryo(Kryo kryo) {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
    }

    public Consumer<Kryo> getKryoCustomizer() {
        return kryoCustomizer;
    }

    public void setKryoCustomizer(Consumer<Kryo> kryoCustomizer) {
        this.kryoCustomizer = kryoCustomizer;
    }

    public Kryo getKryo() {
        return kryos.get();
    }

    @Override
    protected Output write(OutputStream os) throws IOException {
        return new Output(os);
    }

    @Override
    protected Input read(InputStream is) throws IOException {
        return new Input(is);
    }
}
