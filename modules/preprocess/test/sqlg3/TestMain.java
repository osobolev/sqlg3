package sqlg3;

import sqlg3.preprocess.Main;
import sqlg3.preprocess.OptionsBuilder;
import sqlg3.preprocess.checker.Postgres;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

final class TestMain {

    public static void main(String[] args) throws Throwable {
        OptionsBuilder o = new OptionsBuilder();
        o.cleanup = false;
        o.checkTime = false;
        o.tmpDir = Paths.get("C:/TEMP");
        o.classpath = "C:\\Users\\User\\.m2\\repository\\com\\github\\osobolev\\sqlg3\\sqlg3-preprocess\\1.0\\sqlg3-preprocess-1.0.jar;C:\\Users\\User\\.m2\\repository\\com\\github\\osobolev\\sqlg3\\sqlg3-runtime\\1.0\\sqlg3-runtime-1.0.jar;C:\\Users\\User\\.m2\\repository\\com\\github\\osobolev\\sqlg3\\sqlg3-core\\1.0\\sqlg3-core-1.0.jar";
        o.user = "sqlg2";
        o.pass = "sqlg2";
        o.checkerClass = Postgres.class.getName();
        o.driverClass = "org.postgresql.Driver";
        o.url = "jdbc:postgresql://localhost:5432/sqlg2";

        o.srcRoot = Paths.get("sample/daold/src");
        o.destRoot = Paths.get("sample/daold/src");
        List<Path> files = Collections.singletonList(o.srcRoot.resolve("daold/Example1.java"));

//        o.srcRoot = Paths.get("sample/danew/src");
//        o.destRoot = Paths.get("sample/dto/src");
//        o.ifacePack = "dto";
//        o.unpreprocess = true;
//        o.addInterface = true;
//        List<Path> files = Collections.singletonList(o.srcRoot.resolve("danew/Example2.java"));

        new Main(o.build()).workFiles(files, Collections.emptyList());
    }
}
