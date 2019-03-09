package ru.ifmo.rain.shelepov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Implementor implements Impler {

    private static final String IMPL = "Impl";
    private static final String JAVA = ".java";
    private static final String TAB = "    ";
    private static final String COMMA = ",";
    private static final String EOL = ";" + System.lineSeparator();
    private static final String EMPTY_LINE = System.lineSeparator();
    private static final String SPACE = " ";
    private static final String BEGIN = "{" + System.lineSeparator();
    private static final String END = "}" + System.lineSeparator();

    public Implementor() {}

    private Path getFullPath(Path root, Package p, String filename) {
        return root.resolve(p.getName()
                .replace('.', '/'))
                .resolve(filename + IMPL + JAVA);
    }

    private void checkToken(Class<?> token) throws ImplerException {
        if (token.isPrimitive() || token.isArray()) {
            throw new ImplerException("target is primitive or an array");
        }

        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("can't extend final class");
        }

        if (token.getSimpleName().equals("Enum")) {
            throw new ImplerException("can't extend enum class");
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkToken(token);

        Path path = getFullPath(root, token.getPackage(), token.getSimpleName());
        createOutFile(path);
        generateComponents(token, path);
    }

    private String getIndent(int cnt) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < cnt; i++) {
            res.append(TAB);
        }
        return res.toString();
    }

    private void generateHead(Class<?> token, BufferedWriter writer, String indent) throws IOException {
        String packageName = token.getPackageName();
        if (!packageName.isEmpty()) {
            writer.write(indent + "package" + SPACE + packageName + EOL + EMPTY_LINE);
        }

        String declaration = "public class" + SPACE + token.getSimpleName() + IMPL + SPACE
                + (token.isInterface() ? "implements" : "extends") + SPACE + token.getSimpleName() + SPACE + BEGIN;

        writer.write(indent + declaration);
    }

    private List<Constructor> getConstructors(Class<?> token) {
        return Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .collect(Collectors.toList());
    }

    private void generateConstructors(Class<?> token, BufferedWriter writer, String indent) throws IOException, ImplerException {
        if (token.isInterface()) {
            return;
        }
        List<Constructor> constructors = getConstructors(token);

        if (constructors.isEmpty()) {
            throw new ImplerException("class do not have public constructors");
        }
        for (Constructor constructor : constructors) {
            writer.write(generateExecutable(constructor, indent) + EMPTY_LINE);
        }

    }

    private boolean canOverride(Method method) {
        int mod = method.getModifiers();
        return Modifier.isAbstract(mod);
    }

    private class MethodWrapper {
        private Method method;

        private static final int MOD = (int) 1e7 + 3;
        private static final int BASE = 31;

        public MethodWrapper(Method method) {
            this.method = method;
        }

        public Method getMethod() {
            return method;
        }

        public int hashCode() {
            return ((Arrays.hashCode(method.getParameterTypes())
                    + method.getReturnType().hashCode() * BASE) % MOD
                    + method.getName().hashCode() * BASE * BASE) % MOD;
        }

        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }

            if (object instanceof MethodWrapper) {
                Method m = ((MethodWrapper) object).getMethod();
                return method.getReturnType().equals(m.getReturnType())
                        && method.getName().equals(m.getName())
                        && Arrays.equals(method.getParameterTypes(), m.getParameterTypes());
            }
            return false;
        }
    }

    private Set<MethodWrapper> convertToWrapper(Method[] methods) {
        return Arrays.stream(methods)
                .map(MethodWrapper::new)
                .collect(Collectors.toSet());
    }

    private List<Method> getMethods(Class<?> token) {
        Set<MethodWrapper> methods = convertToWrapper(token.getMethods());

        while (token != null) {
            methods.addAll(convertToWrapper(token.getDeclaredMethods()));
            token = token.getSuperclass();
        }

        return methods.stream()
                .map(MethodWrapper::getMethod)
                .collect(Collectors.toList());
    }

    private void generateMethods(Class<?> token, BufferedWriter writer, String indent) throws IOException {
        List<Method> methods = getMethods(token);

        for (Method method : methods) {
            if (canOverride(method)) {
                writer.write(generateExecutable(method, indent) + EMPTY_LINE);
            }
        }
    }

    private void generateComponents(Class<?> token, Path path) throws ImplerException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            generateHead(token, writer, getIndent(0));
            writer.write(EMPTY_LINE);

            generateConstructors(token, writer, getIndent(1));
            generateMethods(token, writer, getIndent(1));
            writer.write(END);
        } catch (IOException e) {
            throw new ImplerException("couldn't write to output file");
        }
    }

    private String generateExecutable(Executable executable, String indent) {
        String declaration = getDeclaration(executable, indent);
        String body = getBody(executable, indent);

        return declaration + body + indent + END;
    }

    private String getDeclaration(Executable executable, String indent) {
        StringBuilder declaration = (new StringBuilder(indent))
                .append(getModifiers(executable))
                .append(SPACE);

        if (executable instanceof Constructor) {
            declaration.append(getSimpleConstructorName(executable.getName()));
        } else {
            declaration.append(getReturnType(executable))
                    .append(SPACE)
                    .append(executable.getName());
        }

        declaration.append("(")
                .append(separated(getParameters(executable, true), COMMA + SPACE))
                .append(")");

        String exceptions = separated(getExceptions(executable), COMMA + SPACE);
        if (!exceptions.isEmpty()) {
            declaration.append(SPACE)
                    .append("throws")
                    .append(SPACE)
                    .append(exceptions);
        }

        declaration.append(SPACE).append(BEGIN);

        return declaration.toString();
    }

    private String getBody(Executable executable, String indent) {
        String res = indent + getIndent(1);
        if (executable instanceof Constructor) {
            res += "super(" + separated(getParameters(executable, false), COMMA + SPACE) + ")";
        } else {
            res += "return" + SPACE + getDefaultValue(((Method) executable).getReturnType());
        }
        return res + EOL;
    }

    private String getName(Parameter param, int ind) {
        if (param.isNamePresent()) {
            return param.getName();
        }
        return "var" + ind;
    }

    private List<String> getParameters(Executable executable, boolean withTypes) {
        List<String> res = new ArrayList<>();
        Parameter[] params = executable.getParameters();
        for (int i = 0; i < params.length; i++) {
            res.add((withTypes ? params[i].getType().getCanonicalName() + SPACE : "") + getName(params[i], i));
        }

        return res;
    }

    private List<String> getExceptions(Executable executable) {
        return Arrays.stream(executable.getExceptionTypes())
                .map(Class::getName)
                .collect(Collectors.toList());
    }

    private String separated(List<String> list, String separator) {
        return list.stream().reduce((String a, String b) -> (a + separator + b)).orElse("");
    }

    private String getDefaultValue(Class<?> c) {
        if (c.getSimpleName().equals("boolean")) {
            return "false";
        }

        if (c.getSimpleName().equals("void")) {
            return "";
        }

        if (c.isPrimitive()) {
            return "0";
        }

        return "null";
    }

    private String getModifiers(Executable executable) {
        return Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
    }

    private String getReturnType(Executable executable) {
        if (executable instanceof Constructor) {
            return "";
        } else {
            return ((Method)executable).getReturnType().getCanonicalName();
        }
    }

    private String getSimpleConstructorName(String name) {
        String[] parts = name.split("\\.");
        return parts[parts.length - 1] + IMPL;
    }

    private void createOutFile(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Couldn't create output file: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Two arguments were expected");
            return;
        }

        for (String arg : args) {
            if (arg == null) {
                System.out.println("Args cannot be null");
                return;
            }
        }

        Impler implementor = new Implementor();

        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ImplerException e) {
            System.out.println("Couldn't implement class: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Target class was not found: " + e.getMessage());
        }
    }
}
