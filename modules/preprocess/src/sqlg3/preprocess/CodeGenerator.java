package sqlg3.preprocess;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import sqlg3.preprocess.lexer.Java8Lexer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
    private final boolean txrpc;

    CodeGenerator(String tab, String interfaceName, String interfacePackage, boolean txrpc) {
        this.tab = tab;
        this.interfaceName = interfaceName;
        this.interfacePackage = interfacePackage;
        this.txrpc = txrpc;
    }

    void start(Class<?> cls, String javadoc) {
        if (interfacePackage != null) {
            buf.append("package " + interfacePackage + ";\n");
            buf.append("\n");
        }
        Class<?>[] ifaces = cls.getInterfaces();
        StringBuilder addIface = new StringBuilder();
        if (txrpc) {
            addIface.append(" extends txrpc.api.IDBCommon");
        }
        for (Class<?> iface : ifaces) {
            if (buf.length() > 0) {
                addIface.append(", ");
            } else {
                addIface.append(" extends ");
            }
            addIface.append(iface.getName());
        }
        buf.append(GENERATED_WARNING + "\n");
        if (javadoc != null) {
            buf.append(javadoc).append("\n");
        }
        buf.append("@SuppressWarnings(\"UnnecessaryFullyQualifiedName\")\n");
        if (txrpc) {
            buf.append("@txrpc.api.Impl(\"" + cls.getName() + "\")\n");
        }
        buf.append("public interface " + interfaceName + addIface + " {\n");
    }

    void addMethod(Method method, String javadoc) {
        if (javadoc != null) {
            buf.append("\n" + tab + javadoc);
        }
        buf.append("\n" + tab);
        buf.append(ClassUtils.getClassName(method.getGenericReturnType()) + " " + method.getName() + "(");
        Type[] parameterTypes = method.getGenericParameterTypes();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
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

    static Path getSourceFile(Class<?> cls, List<Path> srcRoots) throws ParseException {
        String fullName = cls.getName();
        String packName;
        if (fullName.equals(cls.getSimpleName())) {
            packName = null;
        } else {
            packName = fullName.substring(0, fullName.length() - cls.getSimpleName().length() - 1);
        }
        for (Path srcRoot : srcRoots) {
            Path dir = ClassUtils.packageDir(srcRoot, packName);
            Path testFile = dir.resolve(cls.getSimpleName() + ".java");
            if (Files.exists(testFile)) {
                return testFile;
            }
        }
        throw new ParseException("Cannot find source file for class " + cls);
    }

    static void generateImplOut(List<Path> srcRoots, Charset encoding, Class<?> rowType, String body) throws ParseException, IOException {
        Path file = getSourceFile(rowType, srcRoots);
        String text = FileUtils.readFile(file, encoding);
        Java8Lexer lexer = new Java8Lexer(CharStreams.fromString(text));
        int startBody;
        int endBody;
        if (ClassUtils.isRecord(rowType)) {
            startBody = Java8Lexer.LPAREN;
            endBody = Java8Lexer.RPAREN;
        } else {
            startBody = Java8Lexer.LBRACE;
            endBody = Java8Lexer.RBRACE;
        }
        Token start = null;
        Token end = null;
        Token prevNonWhitespace = null;
        boolean wasClass = false;
        while (true) {
            Token t = lexer.nextToken();
            int id = t.getType();
            if (id == Java8Lexer.EOF)
                break;
            if (id == startBody) {
                if (start == null && wasClass) {
                    start = t;
                }
            } else if (id == endBody) {
                end = t;
            } else {
                if (Parser.isClassToken(t, prevNonWhitespace)) {
                    wasClass = true;
                }
            }
            if (!Parser.isWhitespace(id)) {
                prevNonWhitespace = t;
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
