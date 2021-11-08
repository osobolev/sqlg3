module io.github.osobolev.sqlg3.remote.kryo {
    exports sqlg3.remote.kryo;

    requires transitive com.esotericsoftware.kryo;
    requires transitive io.github.osobolev.sqlg3.remote.common;
}
