module io.github.osobolev.sqlg3.remote.server {
    exports sqlg3.remote.server;

    requires transitive io.github.osobolev.sqlg3.tx.runtime;
    requires transitive io.github.osobolev.sqlg3.remote.common;
}
