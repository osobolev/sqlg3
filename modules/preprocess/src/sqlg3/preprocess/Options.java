package sqlg3.preprocess;

import sqlg3.preprocess.ant.SQLGWarn;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class Options {

    public final Path tmpDir;
    public final boolean cleanup;
    public final String classpath;
    public final boolean checkTime;
    public final boolean unpreprocess;

    public final Path srcRoot;

    public final Path destRoot;
    public final String ifacePack;

    public final Charset encoding;
    public final int tabSize;

    public final String driverClass;
    public final String mapperClass;
    public final String checkerClass;
    public final String url;
    public final String user;
    public final String pass;
    public final SQLGWarn warn;
    public final boolean log;
    public final String runtimeMapperClass;

    Options(Path tmpDir, boolean cleanup, String classpath, boolean checkTime, boolean unpreprocess, Path srcRoot, Path destRoot, String ifacePack, Charset encoding, int tabSize, String driverClass, String mapperClass, String checkerClass, String url, String user, String pass, SQLGWarn warn, boolean log, String runtimeMapperClass) {
        this.tmpDir = tmpDir;
        this.cleanup = cleanup;
        this.classpath = classpath;
        this.checkTime = checkTime;
        this.unpreprocess = unpreprocess;
        this.srcRoot = srcRoot;
        this.destRoot = destRoot;
        this.ifacePack = ifacePack;
        this.encoding = encoding;
        this.tabSize = tabSize;
        this.driverClass = driverClass;
        this.mapperClass = mapperClass;
        this.checkerClass = checkerClass;
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.warn = warn;
        this.log = log;
        this.runtimeMapperClass = runtimeMapperClass;
    }

    String getTab() {
        StringBuilder buf = new StringBuilder(tabSize);
        for (int i = 0; i < tabSize; i++) {
            buf.append(' ');
        }
        return buf.toString();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    PrintStream getLog() {
        return log ? System.out : null;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static PrintStream getWarnLog() {
        return System.err;
    }
}
