package sqlg3.preprocess;

import sqlg3.preprocess.ant.SQLGWarn;
import sqlg3.preprocess.checker.Oracle;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Options {

    public Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
    public boolean cleanup = true;
    public String classpath = null;
    public boolean checkTime = true;

    public Path srcRoot = Paths.get(".");

    public Path destRoot = Paths.get(".");
    public String ifacePack = "../dto";

    public Charset encoding = Charset.defaultCharset();
    public int tabSize = 4;

    public String driverClass = "oracle.jdbc.driver.OracleDriver"; // todo
    public String mapperClass = MapperImpl.class.getName();
    public String checkerClass = Oracle.class.getName(); // todo
    public String user = null;
    public String pass = null;
    public String url = null;
    public SQLGWarn warn = SQLGWarn.warn;
    public boolean log = false;
    public String runtimeMapperClass = null; // todo!!!

    public Options() {
    }

    public Options(Path tmpDir, boolean cleanup, String classpath, boolean checkTime, Path srcRoot, Path destRoot, String ifacePack, Charset encoding, int tabSize, String driverClass, String mapperClass, String checkerClass, String user, String pass, String url, SQLGWarn warn, boolean log, String runtimeMapperClass) {
        this.tmpDir = tmpDir;
        this.cleanup = cleanup;
        this.classpath = classpath;
        this.checkTime = checkTime;
        this.srcRoot = srcRoot;
        this.destRoot = destRoot;
        this.ifacePack = ifacePack;
        this.encoding = encoding;
        this.tabSize = tabSize;
        this.driverClass = driverClass;
        this.mapperClass = mapperClass;
        this.checkerClass = checkerClass;
        this.user = user;
        this.pass = pass;
        this.url = url;
        this.warn = warn;
        this.log = log;
        this.runtimeMapperClass = runtimeMapperClass;
    }

    public Options(Options o) {
        this(
            o.tmpDir, o.cleanup, o.classpath, o.checkTime,
            o.srcRoot, o.destRoot, o.ifacePack,
            o.encoding, o.tabSize, o.driverClass, o.mapperClass, o.checkerClass, o.user, o.pass, o.url, o.warn, o.log, o.runtimeMapperClass
        );
    }

    final String getTab() {
        StringBuilder buf = new StringBuilder(tabSize);
        for (int i = 0; i < tabSize; i++) {
            buf.append(' ');
        }
        return buf.toString();
    }
}
