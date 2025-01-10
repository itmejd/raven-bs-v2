package keystrokesmod.script;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

public class SecureClassLoader extends URLClassLoader {
    private List<String> restrictedClasses = Arrays.asList("java.nio", "java.net", "java.util.zip", "java.applet", "java.rmi", "java.security", "java.io.file", "java.lang.reflect", "java.lang.ref", "java.lang.thread", "java.io.buffer", "java.io.input", "java.io.read", "java.io.writer");
    public SecureClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        boolean isRestricted = restrictedClasses.stream().anyMatch(name.toLowerCase()::startsWith);
        boolean isExceptionClass = name.endsWith("Exception");

        if (isRestricted && !isExceptionClass) {
            throw new ClassNotFoundException("Unsafe class detected: " + name);
        }
        return super.loadClass(name, resolve);
    }
}
