module io.github.osobolev.sqlg3.runtime {
    exports sqlg3.annotations;
    exports sqlg3.runtime;
    exports sqlg3.runtime.specific;

    exports sqlg3.runtime.queries to io.github.osobolev.sqlg3.preprocess;

    requires transitive io.github.osobolev.sqlg3.types;
}
