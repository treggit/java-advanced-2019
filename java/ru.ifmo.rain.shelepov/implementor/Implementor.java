package ru.ifmo.rain.shelepov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Implementation of {@link JarImpler} interface
 * @author Anton Shelepov
 */

public class Implementor implements JarImpler {

    /**
     * Suffix, added to generated class name
     */
    private static final String IMPL = "Impl";

    /**
     * Java extension suffix
     */
    private static final String JAVA = ".java";

    /**
     * Class extension suffix
     */
    private static final String CLASS = ".class";

    /**
     * Jar extension suffix
     */
    private static final String JAR = ".jar";

    /**
     * Tab for generated class code
     */
    private static final String TAB = "    ";

    /**
     * Comma for generated class code
     */
    private static final String COMMA = ",";

    /**
     * Code lines separator for generated class code
     */
    private static final String EOL = ";" + System.lineSeparator();

    /**
     * Empty line for generated class code
     */
    private static final String EMPTY_LINE = System.lineSeparator();

    /**
     * Space for generated class code
     */
    private static final String SPACE = " ";

    /**
     * Java scope begin symbol for generated class code
     */
    private static final String BEGIN = "{" + System.lineSeparator();

    /**
     * Java scope end symbol for generated class code
     */
    private static final String END = "}" + System.lineSeparator();


    /**
     * Creates new instance of {@link Implementor}
     */
    public Implementor() {}

    /**
     * Returns absolute path of a given Java class
     * @param folder location of a class
     * @param pack class package name
     * @param filename name of corresponding file
     * @return {@link Path} corresponding to an absolute path of a given class
     */
    private Path getFullPath(Path folder, Package pack, String filename) {
        return folder.resolve(pack.getName()
                .replace('.', '/'))
                .resolve(filename);
    }

    /**
     * Checks whether it is possible to implement a given class
     * @param token token to be checked
     * @throws ImplerException in the following cases:
     * <ul>
     *     <li>
     *          if a given <code>class</code> is a primitive type or an array;
     *     </li>
     *      <li>
     *          if a given <code>class</code> is a final one;
     *      </li>
     *      <li>
     *          if a given <code>class</code> is {@link Enum}.
     *      </li>
     * </ul>
     */
    private void checkToken(Class<?> token) throws ImplerException {
        if (token.isPrimitive() || token.isArray()) {
            throw new ImplerException("target is primitive or an array");
        }

        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("can't extend final class");
        }

        if (token.equals(Enum.class)) {
            throw new ImplerException("can't extend enum class");
        }
    }

    /**
     * Adds suffix to a given filename
     * @param path path of a file
     * @param suf suffix to add
     * @return {@link Path} corresponding to a path with added suffix
     */
    private Path addSuffix(Path path, String suf) {
        return path.getParent().resolve(path.getFileName() + suf);
    }

    /**
     * Produces code implementing class or interface specified by provided <code>token</code>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <code>Impl</code> suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * <code>root</code> directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to <code>$root/java/util/ListImpl.java</code>
     *
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException in the following cases:
     * <ul>
     *     <li>
     *         it is not possible to extend a given class;
     *     </li>
     *     <li>
     *         problems occurred while interacting with filesystem or input/output streams.
     *     </li>
     * </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkToken(token);
        Path path = addSuffix(getFullPath(root, token.getPackage(), token.getSimpleName()), IMPL + JAVA);
        createOutFile(path);
        generateComponents(token, path);
    }

    /**
     * Removes content of a given <code>dir</code>
     * @param dir directory to be cleaned
     */
    private void cleanDirectory(Path dir) {
        try {
            Files.walkFileTree(dir, new Cleaner());
        } catch (IOException e) {
            System.out.println("Couldn't delete: " + e.getMessage());
        }
    }

    /**
     * Class designed to clean folders recursively
     */
    private class Cleaner extends SimpleFileVisitor<Path> {

        /**
         * Deletes a file corresponding to <code>file</code>
         * @param file a file to be deleted
         * @param attrs attribute of a file
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if couldn't delete a file
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Deletes a directory corresponding to <code>dir</code>.
         * The directory is empty, as {@link Cleaner#visitFile(Path, BasicFileAttributes)} was invoked for every file, contained in it
         * @param dir a file to be deleted
         * @param exc occurred exceptions
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if couldn't delete a directory
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }


    /**
     * Produces <code>.jar</code> file implementing class or interface specified by provided <code>token</code>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <code>Impl</code> suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target <code>.jar</code> file.
     * @throws ImplerException when implementation cannot be generated. The reasons might be the same as for {@link Implementor#implement(Class, Path)}
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path tmpDir = jarFile.getParent().resolve("tmp");
        implement(token, tmpDir);
        Path filepath = addSuffix(getFullPath(tmpDir, token.getPackage(), token.getSimpleName()), IMPL + JAVA);

        try {
            compileClass(filepath, tmpDir, getClassPath(token));
            Path compiledClassPath = addSuffix(getFullPath(tmpDir, token.getPackage(), token.getSimpleName()), IMPL + CLASS);
            writeToJar(jarFile, compiledClassPath, token.getName().replace(".", "/") + IMPL + CLASS);
        } finally {
            cleanDirectory(tmpDir);
        }
    }

    /**
     * Returns a classpath of a given <code>token</code>
     * @param token token to get classpath
     * @return {@link String} corresponding to a classpath of a given <code>token</code>
     */
    private String getClassPath(Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Compiles a class with a given <code>filepath</code> and saves a produced class to <code>dir</code>
     * @param filepath path of a file to be compiled
     * @param dir directory, containing a class to be compiled. It also contains a result
     * @param classpath classpath parameter for compilation
     * @throws ImplerException if couldn't compile class
     */
    private void compileClass(Path filepath, Path dir, String classpath) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String[] args = {"-cp", dir.toString() + File.pathSeparator + classpath, filepath.toString()};
        System.err.println(args[1]);
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("couldn't compile class");
        }
    }

    /**
     * Packs a file with a given <code>classname</code> to a jar file with corresponding <code>jarPath</code>
     * @param jarPath path of a jar file, where file is going to be packed
     * @param filepath path of a file to be packed
     * @param classname name of a file to be packed
     * @throws ImplerException if couldn't write to a jar file
     */
    private void writeToJar(Path jarPath, Path filepath, String classname) throws ImplerException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Anton Shelepov");

        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            writer.putNextEntry(new ZipEntry(classname));
            Files.copy(filepath, writer);
        } catch (IOException e) {
            throw new ImplerException("couldn't write to jar file");
        }
    }

    /**
     * Returns an indent with corresponding number of {@link Implementor#TAB}
     * @param cnt number of {@link Implementor#TAB}
     * @return {@link String} consisting of <code>cnt</code> tabs
     */
    private String getIndent(int cnt) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < cnt; i++) {
            res.append(TAB);
        }
        return res.toString();
    }

    /**
     * Generates code, which contains package name of a class and its declaration, and puts it into the output stream
     * @param token class to be extended
     * @param writer output facility
     * @param indent generated code indent
     * @throws IOException if couldn't write a generated code
     */
    private void generateHead(Class<?> token, BufferedWriter writer, String indent) throws IOException {
        String packageName = token.getPackageName();
        if (!packageName.isEmpty()) {
            writer.write(indent + "package" + SPACE + packageName + EOL + EMPTY_LINE);
        }

        String declaration = "public class" + SPACE + token.getSimpleName() + IMPL + SPACE
                + (token.isInterface() ? "implements" : "extends") + SPACE + token.getSimpleName() + SPACE + BEGIN;

        writer.write(indent + declaration);
    }

    /**
     * Returns a list of non-private constructors of a given <code>token</code>
     * @param token class, which constructors are extracted
     * @return {@link java.util.List}, consisting of corresponding constructors
     */
    private List<Constructor> getConstructors(Class<?> token) {
        return Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .collect(Collectors.toList());
    }

    /**
     * Generates declarations and default body of non-private constructors of a given <code>token</code> and puts the code into output stream
     * @param token class, which constructed are to be generated
     * @param writer output facility
     * @param indent generated code indent
     * @throws IOException if couldn't write a generated code
     * @throws ImplerException if a given class has no public constructors
     */
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

    /**
     * Checks, whether it is possible to override a given <code>method</code>
     * @param method method to be checked
     * @return true, if it is possible to override a method; false otherwise
     */
    private boolean canOverride(Method method) {
        int mod = method.getModifiers();
        return Modifier.isAbstract(mod);
    }

    /**
     * Wrapper class of a {@link Method} class with custom {@link MethodWrapper#hashCode()} and {@link MethodWrapper#equals(Object)} methods
     */
    private class MethodWrapper {
        /**
         * Wrapped method
         */
        private Method method;

        /**
         * Mod for hash calculation
         */
        private static final int MOD = (int) 1e7 + 3;

        /**
         * Base for hash calculating
         */
        private static final int BASE = 31;

        /**
         * Creates new instance of {@link MethodWrapper}
         * @param method method to be wrapped
         */
        public MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Returns wrapped method
         * @return {@link Method} corresponding to wrapped method
         */
        public Method getMethod() {
            return method;
        }

        /**
         * Calculates hash
         * @return hash value
         */
        public int hashCode() {
            return ((Arrays.hashCode(method.getParameterTypes())
                    + method.getReturnType().hashCode() * BASE) % MOD
                    + method.getName().hashCode() * BASE * BASE) % MOD;
        }

        /**
         * Check whether this class is equal to a given <code>object</code>
         * @param object object to compare
         * @return true, if this class is equal to <code>object</code>; false otherwise
         */
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

    /**
     * Wraps an array of <code>methods</code> to {@link MethodWrapper}
     * @param methods methods to be wrapped
     * @return {@link Set} consisting of wraps of given <code>methods</code>
     */
    private Set<MethodWrapper> convertToWrapper(Method[] methods) {
        return Arrays.stream(methods)
                .map(MethodWrapper::new)
                .collect(Collectors.toSet());
    }

    /**
     * Returns methods of a given <code>token</code>, which can be overridden. All methods are distinct
     * @param token token, which methods are extracted
     * @return {@link List} consisting of {@link Method} classes, which corresponds to required methods
     */
    private List<Method> getMethods(Class<?> token) {
        Set<MethodWrapper> methods = convertToWrapper(token.getMethods());

        while (token != null) {
            methods.addAll(convertToWrapper(token.getDeclaredMethods()));
            token = token.getSuperclass();
        }

        return methods.stream()
                .map(MethodWrapper::getMethod)
                .filter(this::canOverride)
                .collect(Collectors.toList());
    }

    /**
     * Generates methods declarations and default bodies of a given <code>token</code> and puts them to output stream
     * @param token token, which methods are to be generated
     * @param writer output facility
     * @param indent generated code indent
     * @throws IOException if couldn't write a generated code
     */
    private void generateMethods(Class<?> token, BufferedWriter writer, String indent) throws IOException {
        List<Method> methods = getMethods(token);

        for (Method method : methods) {
            writer.write(generateExecutable(method, indent) + EMPTY_LINE);
        }
    }

    /**
     * Generates code of a class, extending given <code>token</code>, and writes it to the file with a given <code>path</code>
     * @param token class to be extended
     * @param path output path
     * @throws ImplerException if couldn't write to <code>path</code>
     */
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

    /**
     * Generates declaration and default body of given <code>executable</code>
     * @param executable {@link Executable} to be generated. This might be an instance of {@link Method} or {@link Constructor}
     * @param indent indent for generated code
     * @return {@link String} representing a code of a given <code>executable</code>
     */
    private String generateExecutable(Executable executable, String indent) {
        String declaration = getDeclaration(executable, indent);
        String body = getBody(executable, indent);

        return declaration + body + indent + END;
    }

    /**
     * Generates declaration of given <code>executable</code>
     * @param executable {@link Executable} to be generated. This might be an instance of {@link Method} or {@link Constructor}
     * @param indent indent for generated code
     * @return {@link String} representing a code of a given <code>executable</code> declaration
     */
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

    /**
     * Generates default body of given <code>executable</code>
     * @param executable {@link Executable} to be generated. This might be an instance of {@link Method} or {@link Constructor}
     * @param indent indent for generated code
     * @return {@link String} representing a code of a given <code>executable</code> body
     */
    private String getBody(Executable executable, String indent) {
        String res = indent + getIndent(1);
        if (executable instanceof Constructor) {
            res += "super(" + separated(getParameters(executable, false), COMMA + SPACE) + ")";
        } else {
            res += "return" + SPACE + getDefaultValue(((Method) executable).getReturnType());
        }
        return res + EOL;
    }

    /**
     * Generates name for a given <code>param</code>
     * @param param {@link Parameter} to generate name
     * @param ind number of a given <code>param</code> in a method signature
     * @return {@link String} representing a name of a given <code>param</code>
     */
    private String getName(Parameter param, int ind) {
        if (param.isNamePresent()) {
            return param.getName();
        }
        return "var" + ind;
    }

    /**
     * Returns a list of parameters of a given <code>executable</code>
     * @param executable {@link Executable}, which parameters are extracted. This might be an instance of {@link Method} or {@link Constructor}
     * @param withTypes specifies, whether types of parameters are required
     * @return {@link List} of {@link String}, representing parameters names of a given <code>executable</code>
     */
    private List<String> getParameters(Executable executable, boolean withTypes) {
        List<String> res = new ArrayList<>();
        Parameter[] params = executable.getParameters();
        for (int i = 0; i < params.length; i++) {
            res.add((withTypes ? params[i].getType().getCanonicalName() + SPACE : "") + getName(params[i], i));
        }

        return res;
    }

    /**
     * Returns a list of exceptions, which a given <code>executable</code> might throw
     * @param executable {@link Executable}, which exceptions are extracted. This might be an instance of {@link Method} or {@link Constructor}
     * @return {@link List} of {@link String}, representing types of exceptions
     */
    private List<String> getExceptions(Executable executable) {
        return Arrays.stream(executable.getExceptionTypes())
                .map(Class::getName)
                .collect(Collectors.toList());
    }

    /**
     * Concatenates string from a given <code>list</code>, using given <code>separator</code>
     * @param list {@link List} of {@link String} to be concatenated
     * @param separator separator between two list entries
     * @return {@link String}, representing required concatenation
     */
    private String separated(List<String> list, String separator) {
        return list.stream().reduce((String a, String b) -> (a + separator + b)).orElse("");
    }

    /**
     * Returns default value of a given <code>token</code>
     * @param token class, which default value is queried
     * @return {@link String}, representing <code>token</code> default value
     */
    private String getDefaultValue(Class<?> token) {
        if (token.getSimpleName().equals("boolean")) {
            return "false";
        }

        if (token.getSimpleName().equals("void")) {
            return "";
        }

        if (token.isPrimitive()) {
            return "0";
        }

        return "null";
    }

    /**
     * Returns modifiers of a given <code>executable</code>
     * @param executable {@link Executable}, which modifiers are extracted. This might be an instance of {@link Method} or {@link Constructor}
     * @return {@link String}, representing queried modifiers
     */
    private String getModifiers(Executable executable) {
        return Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
    }

    /**
     * Returns return type of a given <code>exectu</code>
     * @param executable {@link Executable}, which return type is extracted. This might be an instance of {@link Method} or {@link Constructor}
     * @return {@link String}, representing queried return type
     */
    private String getReturnType(Executable executable) {
        if (executable instanceof Constructor) {
            return "";
        } else {
            return ((Method)executable).getReturnType().getCanonicalName();
        }
    }

    /**
     * Returns simple constructor name
     * @param name constructor canonical name
     * @return {@link String}, representing queried name
     */
    private String getSimpleConstructorName(String name) {
        String[] parts = name.split("\\.");
        return parts[parts.length - 1] + IMPL;
    }

    /**
     * Creates file with corresponding <code>path</code> and all necessary directories, which are a part of <code>path</code>
     * @param path {@link Path} of a file to be created
     * @throws ImplerException if couldn't create output file or necessary directories
     */
    private void createOutFile(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Couldn't create output file: " + e.getMessage());
            }
        }
    }

    /**
     * Function invokes {@link Implementor#implement(Class, Path)} or {@link Implementor#implementJar(Class, Path)}, depending on its argumants
     * Usage:
     * <ul>
     *     <li>
     *         to invoke {@link Implementor#implement(Class, Path)}: <code>className root</code>
     *     </li>
     *     <li>
     *         to invoke {@link Implementor#implementJar(Class, Path)}: <code>-jar className rootJar</code>
     *     </li>
     * </ul>
     * Aborts, if args are invalid or doesn't fit the usage
     * @param args arguments for execution
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Two or three arguments were expected");
            return;
        }

        for (String arg : args) {
            if (arg == null) {
                System.out.println("Args cannot be null");
                return;
            }
        }

        JarImpler implementor = new Implementor();
        try {
            if (args.length == 3) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (ImplerException e) {
            System.out.println("Couldn't implement class: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Target class was not found: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("Couldnt't get access to private members: " + e.getMessage());
        }
    }
}
