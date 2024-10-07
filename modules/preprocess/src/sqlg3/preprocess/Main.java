package sqlg3.preprocess;

import sqlg3.runtime.GBase;

import java.io.IOException;
import java.nio.charset.Charset;
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

        final IfaceCutPaste ifaceCP;
        final ParseResult parsed;
        final JavaClassFile iface;

        ToProcess(IfaceCutPaste ifaceCP, ParseResult parsed, JavaClassFile iface) {
            this.ifaceCP = ifaceCP;
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

    private static final class ParseContext {

        final Charset encoding;

        ParseContext(Charset encoding) {
            this.encoding = encoding;
        }

        Parser newParser(Path file, String simpleClassName, String fullClassName) throws IOException {
            String text = FileUtils.readFile(file, encoding);
            return new Parser(file, text, simpleClassName, fullClassName);
        }
    }

    private List<InputFile> getInputs(List<Path> inputFiles, ParseContext pctx) throws IOException, ParseException {
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

        if (!o.unpreprocess && o.checkTime != ModifiedCheck.all_always) {
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
            Parser parser = pctx.newParser(file.path, file.simpleClassName, file.fullClassName);
            HeaderResult header;
            ParseResult parsed;
            if (o.unpreprocess) {
                parsed = null;
                header = parser.parseHeader(true);
            } else {
                parsed = parser.parseAll(true);
                header = parsed == null ? null : parsed.header;
            }
            ToProcess src;
            if (header != null) {
                JavaClassFile iface = getInterface(file);
                IfaceCutPaste ifaceCP;
                if (o.addInterface) {
                    ifaceCP = IfaceCutPaste.create(parser.text, header, iface.fullClassName);
                    if (parsed != null) {
                        parsed.insertIfaceFragment(ifaceCP);
                    }
                } else {
                    ifaceCP = null;
                }
                src = new ToProcess(ifaceCP, parsed, iface);
            } else {
                src = null;
            }
            inputs.add(new InputFile(file, src));
        }
        return inputs;
    }

    public void workFiles(List<Path> inputFiles, List<String> javacOptions) throws Throwable {
        // 1. Parse & check modification time
        ParseContext pctx = new ParseContext(o.encoding);
        List<InputFile> inputs0 = getInputs(inputFiles, pctx);
        if (inputs0.isEmpty())
            return;
        if (o.unpreprocess) {
            for (InputFile input : inputs0) {
                ToProcess src = input.src;
                if (src == null)
                    continue;
                Files.deleteIfExists(src.iface.path);
                if (src.ifaceCP != null) {
                    FileUtils.writeFile(input.file.path, src.ifaceCP.removeIface(), o.encoding);
                }
            }
            return;
        }
        List<InputFile> inputs;
        switch (o.checkTime) {
        case all_always:
            inputs = inputs0;
            break;
        case all_if_any_changed:
            boolean isAnyModified = FileUtils.isAnyModified(
                inputs0.stream().filter(input -> input.src != null),
                input -> input.file.path,
                input -> input.src.iface.path
            );
            if (isAnyModified) {
                inputs = inputs0;
            } else {
                inputs = Collections.emptyList();
            }
            break;
        case only_changed:
            List<InputFile> toProcess = new ArrayList<>(inputs0.size());
            boolean anyModified = false;
            for (InputFile input : inputs0) {
                if (input.src != null) {
                    if (FileUtils.isModified(input.file.path, input.src.iface.path)) {
                        toProcess.add(input);
                        anyModified = true;
                    }
                } else {
                    toProcess.add(input);
                }
            }
            if (anyModified) {
                inputs = toProcess;
            } else {
                inputs = Collections.emptyList();
            }
            break;
        default:
            throw new IllegalArgumentException();
        }
        if (inputs.isEmpty())
            return;

        List<Path> srcRoots = new ArrayList<>();
        srcRoots.add(o.srcRoot);
        if (!Objects.equals(o.srcRoot, o.destRoot)) {
            srcRoots.add(o.destRoot);
        }
        try (RunGlobalContext runGlobal = new RunGlobalContext(o); RunLog log = o.getLog()) {
            // 2. Copy to temp
            Path tmpDir = runGlobal.getTmpDir();
            Path[] compFiles = new Path[inputs.size()];
            for (int i = 0; i < inputs.size(); i++) {
                InputFile input = inputs.get(i);
                ToProcess src = input.src;
                Path dir = ClassUtils.packageDir(tmpDir, input.file.pack);
                Files.createDirectories(dir);
                compFiles[i] = dir.resolve(input.file.path.getFileName());
                if (src != null) {
                    ParseResult parsed = src.parsed;
                    String newText = parsed.doCutPaste();
                    FileUtils.writeFile(compFiles[i], newText, o.encoding);
                } else {
                    Files.copy(input.file.path, compFiles[i], StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 3. Run methods
            List<RunResult> runResults = new ArrayList<>();
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

            // 4. Generate row types
            String tab = o.getTab();
            for (Map.Entry<Class<?>, List<RowTypeInfo>> entry : runGlobal.generated.entrySet()) {
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
                    CodeGenerator g = new CodeGenerator(tab, src.iface.simpleClassName, src.iface.pack, o.txrpc);
                    g.start(rr.cls, src.parsed.header.javadoc);
                    for (RunMethod runMethod : rr.methods) {
                        MethodEntry entry = runMethod.entry;
                        if (!entry.publish)
                            continue;
                        g.addMethod(runMethod.method, entry.javadoc);
                    }
                    ifaceText = g.finish();
                }

                if (o.addInterface) {
                    Class<?>[] ifaces = rr.cls.getInterfaces();
                    boolean alreadyHasInterface = Arrays.stream(ifaces)
                        .anyMatch(c -> c.getName().equals(src.iface.fullClassName));
                    if (!alreadyHasInterface) {
                        src.ifaceCP.replaceTo = IfaceCutPaste.getImplements(src.iface.fullClassName, ifaces.length > 0) + " ";
                    }
                }
                String newText = src.parsed.doCutPaste();
                FileUtils.writeFile(input.file.path, newText, o.encoding);

                Files.createDirectories(src.iface.path.getParent());
                FileUtils.writeFile(src.iface.path, ifaceText, o.encoding);
            }
        }
    }
}
