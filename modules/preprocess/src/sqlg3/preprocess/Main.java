package sqlg3.preprocess;

import sqlg3.preprocess.ant.SQLGWarn;
import sqlg3.runtime.GBase;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;

public final class Main {

    private final Options o;

    public Main(Options o) {
        this.o = o;
    }

    private static final class Source {

        final ParseResult parsed;
        final Path interfaceFile;
        final String interfaceName;
        final String interfacePackage;

        Source(ParseResult parsed, Path interfaceFile, String interfaceName, String interfacePackage) {
            this.parsed = parsed;
            this.interfaceFile = interfaceFile;
            this.interfaceName = interfaceName;
            this.interfacePackage = interfacePackage;
        }
    }

    private static final class InputFile {

        final Path path;
        final String simpleClassName;
        final String fullClassName;
        final String pack;
        final Source src;

        InputFile(Path path, String simpleClassName, String fullClassName, String pack, Source src) {
            this.path = path;
            this.simpleClassName = simpleClassName;
            this.fullClassName = fullClassName;
            this.pack = pack;
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

    private static boolean isModified(Path src, Path target) throws IOException {
        if (Files.exists(target)) {
            FileTime srcLastModified = Files.getLastModifiedTime(src);
            FileTime targetLastModified = Files.getLastModifiedTime(target);
            return srcLastModified.compareTo(targetLastModified) > 0;
        } else {
            return true;
        }
    }

    private RowTypeInfo checkCompatibility(Class<?> rowType, List<RowTypeInfo> rowTypes) throws ParseException {
        return RowTypeInfo.checkCompatibility(rowType, rowTypes, warning -> {
            if (o.warn == SQLGWarn.error) {
                throw new ParseException(warning);
            } else if (o.warn == SQLGWarn.warn) {
                Options.getWarnLog().println("WARNING: " + warning);
            }
        });
    }

    public void workFiles(List<Path> in) throws Throwable {
        // 1. Parse & check modification time
        List<InputFile> inputs = new ArrayList<>();
        Map<String, RowTypeCutPaste> rowTypeMap = new HashMap<>();
        boolean isAnyModified = !o.checkTime;
        for (Path file : in) {
            String fileName = file.getFileName().toString();
            String java = ".java";
            if (!fileName.toLowerCase().endsWith(java))
                throw new IllegalArgumentException("File " + fileName + " should have " + java + " extension");
            String simpleClassName = fileName.substring(0, fileName.length() - java.length());
            String pack = getPackage(o.srcRoot, file);
            String fullClassName = pack == null ? simpleClassName : pack + "." + simpleClassName;
            String text = FileUtils.readFile(file, o.encoding);
            Parser parser = new Parser(text, simpleClassName, fullClassName, rowTypeMap);
            ParseResult parsed = parser.parseAll();
            Source src;
            if (parsed == null) {
                src = null;
            } else {
                String interfaceName = "I" + simpleClassName;
                String interfacePackage = ClassUtils.resolvePackage(pack, o.ifacePack);
                Path interfaceFile = ClassUtils.packageDir(o.destRoot, interfacePackage).resolve(interfaceName + java);
                src = new Source(parsed, interfaceFile, interfaceName, interfacePackage);
                if (o.checkTime) {
                    isAnyModified |= isModified(file, interfaceFile);
                }
            }
            inputs.add(new InputFile(file, simpleClassName, fullClassName, pack, src));
        }
        if (!isAnyModified || inputs.isEmpty())
            return;

        List<Path> srcRoots = new ArrayList<>();
        srcRoots.add(o.srcRoot);
        if (!Objects.equals(o.srcRoot, o.destRoot)) {
            srcRoots.add(o.destRoot);
        }
        List<RunResult> runResults = new ArrayList<>();
        Map<Class<?>, List<RowTypeInfo>> generatedIn;
        Map<Class<?>, List<RowTypeInfo>> generatedOut;
        try (RunGlobalContext runGlobal = new RunGlobalContext(o)) {
            // 2. Copy to temp
            Path tmpDir = runGlobal.getTmpDir();
            Path[] compFiles = new Path[inputs.size()];
            for (int i = 0; i < inputs.size(); i++) {
                InputFile input = inputs.get(i);
                Path dir = ClassUtils.packageDir(tmpDir, input.pack);
                Files.createDirectories(dir);
                compFiles[i] = dir.resolve(input.path.getFileName());
                if (input.src != null) {
                    ParseResult parsed = input.src.parsed;
                    String newText = parsed.doCutPaste();
                    FileUtils.writeFile(compFiles[i], newText, o.encoding);
                } else {
                    Files.copy(input.path, compFiles[i], StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 3. Run methods
            for (int i = 0; i < inputs.size(); i++) {
                InputFile input = inputs.get(i);
                Source src = input.src;
                if (src == null)
                    continue;
                ParseResult parsed = src.parsed;
                Class<?> cls = new ClassCompiler(tmpDir).compileAndLoad(srcRoots, compFiles[i], input.fullClassName, o.encoding, o.classpath);
                if (GBase.class.isAssignableFrom(cls)) {
                    MethodRunner runner = new MethodRunner(
                        runGlobal.getTest(), cls, input.simpleClassName, parsed.entries, o.getLog()
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
            String key = cls.getDeclaringClass().getName() + "." + cls.getSimpleName();
            RowTypeCutPaste cp = rowTypeMap.get(key);
            if (cp == null)
                throw new ParseException("Row type " + key + " definition not found");
            String body = rowType.generateRowTypeBody(tab, tab, cls);
            cp.replaceTo = body + tab;
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
            Source src = input.src;
            if (src == null)
                continue;
            ParseResult parsed = src.parsed;
            Files.createDirectories(src.interfaceFile.getParent());
            try (PrintWriter pw = FileUtils.open(src.interfaceFile, o.encoding)) {
                CodeGenerator g = new CodeGenerator(pw, tab, src.interfaceName, src.interfacePackage);
                g.start(rr.cls);
                for (RunMethod runMethod : rr.methods) {
                    MethodEntry entry = runMethod.entry;
                    if (!entry.publish)
                        continue;
                    g.addMethod(runMethod.method, entry.javadoc, entry.paramNames);
                }
                g.finish();
            }
            String newText = parsed.doCutPaste();
            FileUtils.writeFile(input.path, newText, o.encoding);
        }
    }
}
