package sqlg3.preprocess;

import sqlg3.preprocess.ant.SQLGWarn;
import sqlg3.runtime.GBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class Main {

    private final Options o;

    public Main(Options o) {
        this.o = o;
    }

    private static final class JavaClassFile {

        final Path path;
        final String simpleClassName;
        final String fullClassName;
        final String pack;

        JavaClassFile(Path path, String simpleClassName, String pack) {
            this.path = path;
            this.simpleClassName = simpleClassName;
            this.fullClassName = pack == null ? simpleClassName : pack + "." + simpleClassName;
            this.pack = pack;
        }
    }

    private static final class ToProcess {

        final ParseResult parsed;
        final JavaClassFile iface;

        ToProcess(ParseResult parsed, JavaClassFile iface) {
            this.parsed = parsed;
            this.iface = iface;
        }
    }

    private static final class InputFile {

        final JavaClassFile file;
        final ToProcess src;

        InputFile(JavaClassFile file, ToProcess src) {
            this.file = file;
            this.src = src;
        }
    }

    private static final class RunResult {

        final InputFile input;
        final Class<?> cls;
        final List<RunMethod> methods;

        RunResult(InputFile input, Class<?> cls, List<RunMethod> methods) {
            this.input = input;
            this.cls = cls;
            this.methods = methods;
        }
    }

    private static String getPackage(Path srcRoot, Path file) {
        Path relative = srcRoot.relativize(file.getParent());
        if (relative.getNameCount() <= 0)
            return null;
        StringBuilder buf = new StringBuilder();
        for (Path sub : relative) {
            if (buf.length() > 0)
                buf.append('.');
            buf.append(sub.toString());
        }
        return buf.toString();
    }

    private RowTypeInfo checkCompatibility(Class<?> rowType, List<RowTypeInfo> rowTypes) throws ParseException {
        return RowTypeInfo.checkCompatibility(rowType.getSimpleName(), rowTypes, warning -> {
            if (o.warn == SQLGWarn.error) {
                throw new ParseException(warning);
            } else if (o.warn == SQLGWarn.warn) {
                Options.getWarnLog().println("WARNING: " + warning);
            }
        });
    }

    private JavaClassFile getJavaClass(Path file) {
        String simpleClassName = FileUtils.getJavaClassName(file);
        if (simpleClassName == null)
            throw new IllegalArgumentException("File " + file + " should have " + FileUtils.JAVA_EXTENSION + " extension");
        String pack = getPackage(o.srcRoot, file);
        return new JavaClassFile(file, simpleClassName, pack);
    }

    private JavaClassFile getInterface(JavaClassFile src) {
        String interfaceName = "I" + src.simpleClassName;
        String interfacePackage = ClassUtils.resolvePackage(src.pack, o.ifacePack);
        Path interfaceFile = ClassUtils.packageDir(o.destRoot, interfacePackage).resolve(interfaceName + FileUtils.JAVA_EXTENSION);
        return new JavaClassFile(interfaceFile, interfaceName, interfacePackage);
    }

    private List<InputFile> getInputs(List<Path> inputFiles, Map<ClassName, RowTypeCutPaste> rowTypeMap) throws IOException, ParseException {
        List<Path> in;
        if (inputFiles.isEmpty()) {
            in = new ArrayList<>();
            FileUtils.listAllJavaFiles(o.srcRoot, in);
        } else {
            in = inputFiles;
        }

        List<JavaClassFile> javaFiles = new ArrayList<>(in.size());
        for (Path path : in) {
            javaFiles.add(getJavaClass(path));
        }

        if (!o.unpreprocess && o.checkTime) {
            Set<Path> candidateInterfaces = new HashSet<>(javaFiles.size());
            Map<Path, Path> classToIface = new LinkedHashMap<>(javaFiles.size());
            for (JavaClassFile file : javaFiles) {
                JavaClassFile iface = getInterface(file);
                candidateInterfaces.add(iface.path);
                classToIface.put(file.path, iface.path);
            }
            classToIface.keySet().removeAll(candidateInterfaces);
            boolean isAnyModified = FileUtils.isAnyModified(
                classToIface.entrySet().stream(),
                Map.Entry::getKey,
                Map.Entry::getValue
            );
            if (!isAnyModified)
                return Collections.emptyList();
        }

        List<InputFile> inputs = new ArrayList<>(javaFiles.size());
        for (JavaClassFile file : javaFiles) {
            String text = FileUtils.readFile(file.path, o.encoding);
            Parser parser = new Parser(text, file.simpleClassName, file.fullClassName, rowTypeMap);
            ParseResult parsed;
            boolean addSrc;
            if (o.unpreprocess) {
                parsed = null;
                addSrc = parser.parseHeader();
            } else {
                parsed = parser.parseAll();
                addSrc = parsed != null;
            }
            ToProcess src;
            if (addSrc) {
                JavaClassFile iface = getInterface(file);
                src = new ToProcess(parsed, iface);
            } else {
                src = null;
            }
            inputs.add(new InputFile(file, src));
        }
        return inputs;
    }

    public void workFiles(List<Path> inputFiles, List<String> javacOptions) throws Throwable {
        // 1. Parse & check modification time
        Map<ClassName, RowTypeCutPaste> rowTypeMap = new HashMap<>();
        List<InputFile> inputs = getInputs(inputFiles, rowTypeMap);
        if (inputs.isEmpty())
            return;
        if (o.unpreprocess) {
            for (InputFile input : inputs) {
                ToProcess src = input.src;
                if (src == null)
                    continue;
                Files.deleteIfExists(src.iface.path);
            }
            return;
        }
        if (o.checkTime) {
            boolean isAnyModified = FileUtils.isAnyModified(
                inputs.stream().filter(input -> input.src != null),
                input -> input.file.path,
                input -> input.src.iface.path
            );
            if (!isAnyModified)
                return;
        }

        List<Path> srcRoots = new ArrayList<>();
        srcRoots.add(o.srcRoot);
        if (!Objects.equals(o.srcRoot, o.destRoot)) {
            srcRoots.add(o.destRoot);
        }
        List<RunResult> runResults = new ArrayList<>();
        Map<Class<?>, List<RowTypeInfo>> generatedIn;
        Map<Class<?>, List<RowTypeInfo>> generatedOut;
        try (RunGlobalContext runGlobal = new RunGlobalContext(o);
             RunLog log = o.getLog()) {
            // 2. Copy to temp
            Path tmpDir = runGlobal.getTmpDir();
            Path[] compFiles = new Path[inputs.size()];
            for (int i = 0; i < inputs.size(); i++) {
                InputFile input = inputs.get(i);
                Path dir = ClassUtils.packageDir(tmpDir, input.file.pack);
                Files.createDirectories(dir);
                compFiles[i] = dir.resolve(input.file.path.getFileName());
                if (input.src != null) {
                    ParseResult parsed = input.src.parsed;
                    String newText = parsed.doCutPaste();
                    FileUtils.writeFile(compFiles[i], newText, o.encoding);
                } else {
                    Files.copy(input.file.path, compFiles[i], StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 3. Run methods
            for (int i = 0; i < inputs.size(); i++) {
                InputFile input = inputs.get(i);
                ToProcess src = input.src;
                if (src == null)
                    continue;
                ParseResult parsed = src.parsed;
                Class<?> cls = new ClassCompiler(tmpDir).compileAndLoad(
                    srcRoots, compFiles[i], input.file.fullClassName, o.encoding, o.classpath, javacOptions
                );
                if (GBase.class.isAssignableFrom(cls)) {
                    MethodRunner runner = new MethodRunner(
                        runGlobal.getTest(), cls, input.file.simpleClassName, parsed.entries, log
                    );
                    List<RunMethod> runMethods = runner.checkEntries(parsed.bindMap, parsed.parameters);
                    runResults.add(new RunResult(input, cls, runMethods));
                }
            }
            generatedIn = runGlobal.generatedIn;
            generatedOut = runGlobal.generatedOut;
        }

        // 4. Generate row types
        String tab = o.getTab();
        for (Map.Entry<Class<?>, List<RowTypeInfo>> entry : generatedIn.entrySet()) {
            Class<?> cls = entry.getKey();
            RowTypeInfo rowType = checkCompatibility(cls, entry.getValue());
            ClassName key = ClassName.nested(cls.getDeclaringClass().getName(), cls.getSimpleName());
            RowTypeCutPaste cp = rowTypeMap.get(key);
            if (cp == null)
                throw new ParseException("Row type " + key + " definition not found");
            cp.replaceTo = rowType.generateRowTypeBody(tab, tab, cls);
        }
        for (Map.Entry<Class<?>, List<RowTypeInfo>> entry : generatedOut.entrySet()) {
            Class<?> cls = entry.getKey();
            RowTypeInfo rowType = checkCompatibility(cls, entry.getValue());
            String body = rowType.generateRowTypeBody("", tab, cls);
            CodeGenerator.generateImplOut(srcRoots, o.encoding, cls, body);
        }

        // 5. Generate interfaces & write back sources
        for (RunResult rr : runResults) {
            InputFile input = rr.input;
            ToProcess src = input.src;
            String ifaceText;
            {
                CodeGenerator g = new CodeGenerator(tab, src.iface.simpleClassName, src.iface.pack);
                g.start(rr.cls);
                for (RunMethod runMethod : rr.methods) {
                    MethodEntry entry = runMethod.entry;
                    if (!entry.publish)
                        continue;
                    g.addMethod(runMethod.method, entry.javadoc, entry.paramNames);
                }
                ifaceText = g.finish();
            }

            String newText = src.parsed.doCutPaste();
            FileUtils.writeFile(input.file.path, newText, o.encoding);

            Files.createDirectories(src.iface.path.getParent());
            FileUtils.writeFile(src.iface.path, ifaceText, o.encoding);
        }
    }
}
