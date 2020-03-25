package sqlg3.preprocess;

import sqlg3.preprocess.ant.SQLGWarn;
import sqlg3.preprocess.checker.Generic;
import sqlg3.runtime.RuntimeMapperImpl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class OptionsBuilder {

    public Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
    public boolean cleanup = true;
    public String classpath = null;
    public boolean checkTime = true;

    public Path srcRoot = Paths.get(".");

    public Path destRoot;
    public String ifacePack = ".";
    public final List<String> javacOptions = new ArrayList<>();

    public Charset encoding = StandardCharsets.UTF_8;
    public int tabSize = 4;

    public String driverClass;
    public String mapperClass = MapperImpl.class.getName();
    public String checkerClass = Generic.class.getName();
    public String url;
    public String user;
    public String pass;
    public SQLGWarn warn = SQLGWarn.warn;
    public boolean log = false;
    public String runtimeMapperClass = RuntimeMapperImpl.class.getName();

    public Options build() throws ParseException {
        if (driverClass == null) {
            throw new ParseException("Property 'driverClass' not set");
        }
        if (url == null) {
            throw new ParseException("Property 'url' not set");
        }
        Path destRoot;
        if (this.destRoot == null) {
            destRoot = srcRoot;
        } else {
            destRoot = this.destRoot;
        }
        return new Options(
            tmpDir, cleanup, classpath, checkTime, srcRoot, destRoot, ifacePack, javacOptions,
            encoding, tabSize, driverClass, mapperClass, checkerClass, url, user, pass, warn, log, runtimeMapperClass
        );
    }
}
