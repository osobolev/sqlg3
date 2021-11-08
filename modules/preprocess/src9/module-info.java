module io.github.osobolev.sqlg3.preprocess {
    exports sqlg3.preprocess;
    exports sqlg3.preprocess.checker;

    requires java.compiler;
    requires org.antlr.antlr4.runtime;
    requires transitive io.github.osobolev.sqlg3.runtime;
}
