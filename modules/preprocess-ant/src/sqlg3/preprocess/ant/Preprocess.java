package sqlg3.preprocess.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import sqlg3.preprocess.Main;
import sqlg3.preprocess.OptionsBuilder;
import sqlg3.preprocess.PreprocessException;
import sqlg3.preprocess.SQLGWarn;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ANT task for SQLG preprocessor.
 */
public class Preprocess extends Task {

    private final List<FileSet> filesets = new ArrayList<>();

    private final OptionsBuilder options = new OptionsBuilder();
    private final List<JavacOption> javacOptions = new ArrayList<>();

    /**
     * Preprocessor temporary files folder
     */
    public void setTmpdir(File tmpdir) {
        options.tmpDir = tmpdir.toPath();
    }

    /**
     * Sets flag to clean up temporary files.
     */
    public void setCleanup(boolean on) {
        options.cleanup = on;
    }

    /**
     * CLASSPATH to be used during compilation of processed files.
     * Should include all required files referenced in processed files.
     * Usual CLASSPATH format.
     */
    public void setClasspath(String classpath) {
        options.classpath = classpath;
    }

    /**
     * @param unpreprocess true to delete generated interface files
     */
    public void setUnpreprocess(boolean unpreprocess) {
        options.unpreprocess = unpreprocess;
    }

    /**
     * @param addInterface if add interface to "implements" clause
     */
    public void setAddinterface(boolean addInterface) {
        options.addInterface = addInterface;
    }

    /**
     * @param txrpc if generate txrpc annotations for interfaces
     */
    public void setTxrpc(boolean txrpc) {
        options.txrpc = txrpc;
    }

    /**
     * Source files root. Package of class is determined relative to the source root.
     */
    public void setSrcroot(File srcroot) {
        options.srcRoot = srcroot.toPath();
    }

    /**
     * Destination files source root.
     */
    public void setDestroot(File destroot) {
        options.destRoot = destroot.toPath();
    }

    /**
     * Sets subpackage name for interface classes.
     */
    public void setIfacepack(String sub) {
        options.ifacePack = sub;
    }

    public static final class JavacOption {

        private String arg;

        public String getArg() {
            return arg;
        }

        public void setArg(String arg) {
            this.arg = arg;
        }
    }

    public JavacOption createJavacoption() {
        JavacOption jo = new JavacOption();
        javacOptions.add(jo);
        return jo;
    }

    /**
     * Sets source files encoding.
     */
    public void setEncoding(String encoding) {
        options.encoding = Charset.forName(encoding);
    }

    /**
     * Sets tab size (&lt;0 for tab character).
     */
    public void setTabsize(int size) {
        options.tabSize = size;
    }

    /**
     * JDBC driver class name to be used during preprocess.
     */
    public void setDriverclass(String driver) {
        options.driverClass = driver;
    }

    /**
     * {@link sqlg3.preprocess.Mapper} implementation class name.
     */
    public void setMapperclass(String cls) {
        options.mapperClass = cls;
    }

    /**
     * {@link sqlg3.runtime.DBSpecific} implementation class name.
     */
    public void setCheckerclass(String cls) {
        options.checkerClass = cls;
    }

    /**
     * JDBC URL to be used during preprocess.
     */
    public void setUrl(String url) {
        options.url = url;
    }

    /**
     * DB user name to be used during preprocess.
     */
    public void setUser(String user) {
        options.user = user;
    }

    /**
     * DB user password to be used during preprocess.
     */
    public void setPassword(String password) {
        options.pass = password;
    }

    /**
     * Warning output mode
     */
    public void setWarn(SQLGWarn warn) {
        options.warn = warn;
    }

    /**
     * Log method running
     */
    public void setLog(String log) {
        options.log = log;
    }

    public void setRuntimemapperclass(String cls) {
        options.runtimeMapperClass = cls;
    }

    /**
     * Adds files process.
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }

    /**
     * Runs ANT task.
     */
    public void execute() throws BuildException {
        List<Path> files = new ArrayList<>();
        for (FileSet fs : filesets) {
            Path dir = fs.getDir(getProject()).toPath();
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (String srcFile : srcFiles) {
                files.add(dir.resolve(srcFile));
            }
        }
        options.javacOptions = javacOptions.stream().map(JavacOption::getArg).filter(Objects::nonNull).collect(Collectors.toList());
        try {
            new Main(options.build()).processFiles(files);
        } catch (PreprocessException ex) {
            throw new BuildException(ex.getMessage(), ex);
        }
    }
}
