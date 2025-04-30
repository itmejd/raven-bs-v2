package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.utility.Utils;
import net.minecraft.launchwrapper.Launch;

import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class Script {
    public String name;
    public Class clazz;
    public Object instance;
    public String scriptName;
    public String codeStr;
    public boolean error = false;
    public int STARTING_LINE;
    public ScriptEvents event;
    public File file;

    public Script(String name) {
        this.name = name;
        this.scriptName = "sc_" + name.replace(" ", "").replace(")", "_").replace("(", "_") + "_" + Utils.generateRandomString(5);
    }

    public boolean run() {
        try {
            if (this.scriptName == null || this.codeStr == null) {
                return false;
            }
            final File file = new File(Raven.scriptManager.COMPILED_DIR);
            if (!file.exists() || !file.isDirectory()) {
                file.mkdir();
            }
            if (Raven.scriptManager.compiler == null) {
                return false;
            }
            final ScriptDiagnosticListener bp = new ScriptDiagnosticListener();
            final StandardJavaFileManager fileManager = Raven.scriptManager.compiler.getStandardFileManager(bp, null, null);
            final ArrayList<String> compilationOptions = new ArrayList<>();
            compilationOptions.add("-d");
            compilationOptions.add(Raven.scriptManager.COMPILED_DIR);
            compilationOptions.add("-XDuseUnsharedTable");
            if (!(boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
                compilationOptions.add("-classpath");
                String s = Raven.scriptManager.jarPath;
                try {
                    s = URLDecoder.decode(s, "UTF-8");
                }
                catch (UnsupportedOperationException ex2) {}
                compilationOptions.add(s);
            }
            boolean success = Raven.scriptManager.compiler.getTask(null, fileManager, bp, compilationOptions, null, Arrays.asList(new JavaSourceFromString(this.scriptName, this.codeStr, this.STARTING_LINE))).call();
            if (!success) {
                this.error = true;
                return false;
            }
            try {
                final SecureClassLoader secureClassLoader = new SecureClassLoader(new URL[]{file.toURI().toURL()}, Launch.classLoader);
                this.clazz = secureClassLoader.loadClass(this.scriptName);
                this.instance = this.clazz.newInstance();
                secureClassLoader.close();
                fileManager.close();
            }
            catch (Throwable e) {
                e.printStackTrace();
                Utils.sendMessage("&7Script &b" + Utils.extractFileName(this.name) + " &7blocked, &cunsafe code&7 detected!");
                this.error = true;
                return false;
            }
            return true;
        }
        catch (Exception ex) {
            this.error = true;
            return !error;
        }
    }

    public int getBoolean(final String s, final Object... array) {
        if (this.clazz == null || this.instance == null) {
            return -1;
        }
        Method method = null;
        for (final Method method2 : this.clazz.getDeclaredMethods()) {
            if (method2.getName().equalsIgnoreCase(s) && method2.getParameterCount() == array.length && method2.getReturnType().equals(Boolean.TYPE)) {
                method = method2;
                break;
            }
        }
        if (method != null) {
            try {
                method.setAccessible(true);
                final Object invoke = method.invoke(this.instance, array);
                if (invoke instanceof Boolean) {
                    return ((boolean)invoke) ? 1 : 0;
                }
            }
            catch (IllegalAccessException | InvocationTargetException ex) {
                ReflectiveOperationException er = ex;
                Utils.sendMessage("&7Runtime error during script &b" + this.name);
                if (er.getCause() == null) {
                    Utils.sendMessage(" &7err: &cThrowable");
                }
                else {
                    Utils.sendMessage(" &7err: &c" + er.getCause().getClass().getSimpleName());
                    final StackTraceElement[] stArr = er.getCause().getStackTrace();
                    if (stArr.length > 0) {
                        StackTraceElement st = stArr[0];
                        for (final StackTraceElement element : er.getCause().getStackTrace()) {
                            if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                                st = element;
                                break;
                            }
                        }
                        Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - STARTING_LINE));
                        Utils.sendMessage(" &7src: &c" + st.getMethodName());
                    }
                }
            }
        }
        return -1;
    }

    public Float[] getFloatArray(String methodName, Object... args) {
        if (this.clazz == null || this.instance == null) {
            return null;
        }
        Method method = null;
        for (Method _method : this.clazz.getDeclaredMethods()) {
            if (_method.getName().equals(methodName) && _method.getReturnType().equals(Float[].class) && _method.getParameterCount() == args.length) {
                method = _method;
                break;
            }
        }
        if (method != null) {
            try {
                method.setAccessible(true);
                Object result = method.invoke(this.instance, args);
                if (result instanceof Float[]) {
                    return (Float[])result;
                }
            }
            catch (IllegalAccessException | InvocationTargetException ex) {
                ReflectiveOperationException er = ex;
                Utils.sendMessage("&7Runtime error during script &b" + this.name);
                if (er.getCause() == null) {
                    Utils.sendMessage(" &7err: &cThrowable");
                }
                else {
                    Utils.sendMessage(" &7err: &c" + er.getCause().getClass().getSimpleName());
                    StackTraceElement[] stArr = er.getCause().getStackTrace();
                    if (stArr.length > 0) {
                        StackTraceElement st = stArr[0];
                        for (StackTraceElement element : er.getCause().getStackTrace()) {
                            if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                                st = element;
                                break;
                            }
                        }
                        Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - STARTING_LINE));
                        Utils.sendMessage(" &7src: &c" + st.getMethodName());
                    }
                }
            }
        }
        return null;
    }

    public void delete() {
        this.clazz = null;
        this.instance = null;
        final File file = new File(Raven.scriptManager.COMPILED_DIR + File.separator + this.scriptName + ".class");
        if (file.exists()) {
            file.delete();
        }
    }

    public void setCode(String code) {
        STARTING_LINE = 0;
        StringBuilder fileCodeContents = new StringBuilder();
        Iterator<String> iterator = Raven.scriptManager.imports.iterator();
        while (iterator.hasNext()) {
            STARTING_LINE++;
            fileCodeContents.append("import ").append(iterator.next()).append(";\n");
        }
        fileCodeContents.append("import keystrokesmod.script.model.*;\n");
        fileCodeContents.append("import keystrokesmod.script.packet.clientbound.*;\n");
        fileCodeContents.append("import keystrokesmod.script.packet.serverbound.*;\n");
        String name = Utils.extractFileName(this.name);
        this.codeStr = fileCodeContents + "public class " + this.scriptName + " extends " + ScriptDefaults.class.getName() + " {public static final " + ScriptDefaults.modules.class.getName().replace("$", ".") + " modules = new " + ScriptDefaults.modules.class.getName().replace("$", ".") + "(\"" + name + "\");public static final String scriptName = \"" + name + "\";\n" + code + "\n}";
        STARTING_LINE += 4;
    }

    public boolean invoke(final String s, final Object... array) {
        if (this.clazz == null || this.instance == null) {
            return false;
        }
        Method method = null;
        for (final Method method2 : this.clazz.getDeclaredMethods()) {
            if (method2.getName().equalsIgnoreCase(s) && method2.getParameterCount() == array.length && method2.getReturnType().equals(Void.TYPE)) {
                method = method2;
                break;
            }
        }
        if (method != null) {
            try {
                method.setAccessible(true);
                method.invoke(this.instance, array);
                return true;
            }
            catch (IllegalAccessException | InvocationTargetException ex) {
                ReflectiveOperationException er = ex;
                Utils.sendMessage("&7Runtime error during script &b" + this.name);
                if (er.getCause() == null) {
                    Utils.sendMessage(" &7err: &cThrowable");
                }
                else {
                    Utils.sendMessage(" &7err: &c" + er.getCause().getClass().getSimpleName());
                    final StackTraceElement[] stArr = er.getCause().getStackTrace();
                    if (stArr.length > 0) {
                        StackTraceElement st = stArr[0];
                        for (final StackTraceElement element : er.getCause().getStackTrace()) {
                            if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                                st = element;
                                break;
                            }
                        }
                        Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - STARTING_LINE));
                        Utils.sendMessage(" &7src: &c" + st.getMethodName());
                    }
                }
            }
        }
        return false;
    }
}
