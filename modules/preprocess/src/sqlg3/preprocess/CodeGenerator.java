package sqlg3.preprocess;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import sqlg3.preprocess.lexer.Java8Lexer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class CodeGenerator {

    private static final String GENERATED_WARNING = "// THIS FILE IS MACHINE-GENERATED, DO NOT EDIT";

    private final StringBuilder buf = new StringBuilder();
    private final String tab;
    private final String interfaceName;
    private final String interfacePackage;

    CodeGenerator(String tab, String interfaceName, String interfacePackage) {
        this.tab = tab;
        this.interfaceName = interfaceName;
        this.interfacePackage = interfacePackage;
    }

    void start(Class<?> cls) {
        if (interfacePackage != null) {
            buf.append("package " + interfacePackage + ";\n");
            buf.append("\n");
        }
        Class<?>[] ifaces = cls.getInterfaces();
        StringBuilder buf = new StringBuilder();
        for (Class<?> iface : ifaces) {
            buf.append(", ");
            buf.append(iface.getName());
        }
        String addIface = buf.toString();
        buf.append(GENERATED_WARNING + "\n");
        buf.append("@SuppressWarnings({\"UnnecessaryInterfaceModifier\", \"UnnecessaryFullyQualifiedName\", \"RedundantSuppression\"})\n");
        buf.append("@sqlg3.core.Impl(\"" + cls.getName() + "\")\n");
        buf.append("public interface " + interfaceName + " extends sqlg3.core.IDBCommon" + addIface + " {\n");
    }

    void addMethod(Method method, String javadoc, List<String> paramNames) throws ParseException {
        if (javadoc != null) {
            buf.append("\n" + tab + javadoc);
        }
        buf.append("\n" + tab);
        buf.append(ClassUtils.getClassName(method.getGenericReturnType()) + " " + method.getName() + "(");
        Type[] parameterTypes = method.getGenericParameterTypes();
        if (parameterTypes.length != paramNames.size())
            throw new ParseException("Parameter list length mismatch: " + parameterTypes.length + " vs " + paramNames.size());
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(ClassUtils.getClassName(parameterTypes[i]) + " " + paramName);
        }
        List<Type> excs = new ArrayList<>(Arrays.asList(method.getGenericExceptionTypes()));
        Class<SQLException> sqle = SQLException.class;
        if (!excs.contains(sqle)) {
            excs.add(sqle);
        }
        buf.append(") throws " + excs.stream().map(ClassUtils::getClassName).collect(Collectors.joining(", ")));
        buf.append(";\n");
    }

    String finish() {
        buf.append("}\n");
        return buf.toString();
    }

    static void generateImplOut(List<Path> srcRoots, Charset encoding, Class<?> rowType, String body) throws ParseException, IOException {
        String fullName = rowType.getName();
        String packName;
        if (fullName.equals(rowType.getSimpleName())) {
            packName = null;
        } else {
            packName = fullName.substring(0, fullName.length() - rowType.getSimpleName().length() - 1);
        }
        Path file = null;
        for (Path srcRoot : srcRoots) {
            Path dir = ClassUtils.packageDir(srcRoot, packName);
            Path testFile = dir.resolve(rowType.getSimpleName() + ".java");
            if (Files.exists(testFile)) {
                file = testFile;
                break;
            }
        }
        if (file == null)
            throw new ParseException("Cannot find file for class " + rowType);
        String text = FileUtils.readFile(file, encoding);
        Java8Lexer lexer = new Java8Lexer(CharStreams.fromString(text));
        Token start = null;
        Token end = null;
        // todo: special case for records: replace inside parentheses
        while (true) {
            Token token = lexer.nextToken();
            int type = token.getType();
            if (type == Java8Lexer.EOF)
                break;
            if (type == Java8Lexer.LBRACE) {
                if (start == null) {
                    start = token;
                }
            } else if (type == Java8Lexer.RBRACE) {
                end = token;
            }
        }
        if (start == null || end == null)
            return;
        CutPaste cp = new SimpleCutPaste(start.getStartIndex() + 1, end.getStartIndex(), body);
        StringBuilder textBuf = new StringBuilder(text);
        cp.cutPaste(textBuf);
        FileUtils.writeFile(file, textBuf.toString(), encoding);
    }
}
