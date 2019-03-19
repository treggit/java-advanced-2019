package info.kgeorgiy.java.advanced.implementor;

import java.nio.file.Path;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface JarImpler extends Impler {
    /**
     * Produces <code>.jar</code> file implementing class or interface specified by provided <code>token</code>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <code>Impl</code> suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target <code>.jar</code> file.
     * @throws ImplerException when implementation cannot be generated.
     */
    void implementJar(Class<?> token, Path jarFile) throws ImplerException;
}
