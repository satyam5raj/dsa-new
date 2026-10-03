/*
 * ============================================================================
 *  TOPIC 3 : CLASS LOADING - loaders, delegation, load/link/init, custom loaders
 * ============================================================================
 *
 *  Run:  java ClassLoadingDemo.java
 *  Watch the JVM load classes:  java -Xlog:class+load:file=classload.log ClassLoadingDemo.java
 *
 *  Classes are loaded LAZILY: the first time they're actually needed,
 *  not all at startup.
 *
 *  THE 3 PHASES
 *  ------------
 *  1. LOADING   : find the .class bytes (file, jar, network, generated...),
 *                 create the Class object, store metadata in Metaspace.
 *  2. LINKING
 *       a. VERIFY   : the bytecode is safe and valid (no stack overflow tricks,
 *                     correct types). Throws VerifyError.
 *       b. PREPARE  : allocate static fields and set them to DEFAULT values (0/null/false).
 *       c. RESOLVE  : turn symbolic references ("java/util/List") into direct
 *                     references (often lazily, on first use).
 *  3. INITIALIZATION : run static initializers and static blocks, top to
 *                      bottom (the <clinit> method). Happens ONCE, thread-safely,
 *                      on first ACTIVE use:
 *         - new X(), calling a static method, reading/writing a non-constant static field
 *         - Class.forName("X") (initializes by default)
 *         - initializing a subclass (the parent is initialized first)
 *       NOT triggered by: X.class, declaring an array X[], reading a
 *       compile-time CONSTANT (static final int = 5 is inlined by javac).
 *
 *  CLASSLOADER HIERARCHY (Java 9+)
 *     Bootstrap  (native, shows as null)  -> java.base: java.lang, java.util...
 *        ^ parent
 *     Platform   (was "Extension" in Java 8) -> java.sql, java.xml, other JDK modules
 *        ^ parent
 *     Application / System -> your classpath (-cp) and modules
 *        ^ parent
 *     Custom loaders (Tomcat webapps, OSGi, plugin systems, hot reload)
 *
 *  PARENT-DELEGATION MODEL
 *    loadClass(name):
 *      1. already loaded by me? return it
 *      2. ask the PARENT first (recursively up to Bootstrap)
 *      3. only if the parent can't find it -> findClass(name) myself
 *    WHY: security and consistency. Nobody can sneak in a fake java.lang.String,
 *    because Bootstrap always wins for core classes.
 *    (Tomcat deliberately inverts this for webapp classes ("child-first"), so
 *     each app can use its own library versions.)
 *
 *  CLASS IDENTITY = (fully qualified name + DEFINING CLASSLOADER)
 *    The same class bytes loaded by two loaders = two DIFFERENT classes.
 *    Casting between them throws ClassCastException ("X cannot be cast to X").
 *    This is the basis of hot redeploy, and also of Metaspace leaks.
 *
 *  ERRORS
 *    ClassNotFoundException : explicit load by NAME failed (Class.forName, loadClass). Checked.
 *    NoClassDefFoundError   : a class that existed at COMPILE time is missing
 *                             at RUNTIME, or its static initializer failed earlier.
 *    ExceptionInInitializerError : the static initializer threw an exception.
 */

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;

public class ClassLoadingDemo {

    public static void main(String[] args) throws Exception {
        loaderHierarchy();
        lazyInitialization();
        initializationOrderWithInheritance();
        failingStaticInit();
        customClassLoader();
    }

    // =================================================================
    // 1. Who loaded what?
    // =================================================================
    static void loaderHierarchy() {
        System.out.println("===== 1. CLASSLOADER HIERARCHY =====");
        System.out.println("   String          loaded by: " + String.class.getClassLoader() + "  (null = Bootstrap)");
        System.out.println("   java.sql.Date   loaded by: " + java.sql.Date.class.getClassLoader().getName());
        ClassLoader mine = ClassLoadingDemo.class.getClassLoader();
        System.out.println("   ClassLoadingDemo loaded by: " + mine.getClass().getSimpleName()
                + "  (the single-file launcher's in-memory loader)");
        System.out.print("   parent chain: ");
        for (ClassLoader cl = mine; cl != null; cl = cl.getParent())
            System.out.print((cl.getName() != null ? cl.getName() : cl.getClass().getSimpleName()) + " -> ");
        System.out.println("bootstrap");
    }

    // =================================================================
    // 2. Lazy loading & what does / doesn't trigger initialization
    // =================================================================
    static class Config {
        static final int CONSTANT = 42;                       // compile-time constant: INLINED by javac
        static final String RUNTIME_VALUE = compute();        // NOT a constant: needs <clinit>
        static { System.out.println("      >> Config static block runs (class INITIALIZED)"); }
        static String compute() { return "computed-" + System.nanoTime() % 100; }
    }

    // Lazy, thread-safe singleton built on exactly these rules (Bill Pugh)
    static class Holder {
        static { System.out.println("      >> Holder initialized only now, on first access to INSTANCE"); }
        static final Object INSTANCE = new Object();
    }

    static void lazyInitialization() throws Exception {
        System.out.println("\n===== 2. LAZY INITIALIZATION =====");
        System.out.println("   reading Config.CONSTANT = " + Config.CONSTANT + "   (no init: the value was inlined)");
        Class<?> c = Config.class;
        System.out.println("   Config.class reference   = " + c.getSimpleName() + "   (no init)");
        Config[] arr = new Config[3];
        System.out.println("   new Config[3]            = length " + arr.length + " (no init)");
        System.out.println("   reading Config.RUNTIME_VALUE ->");
        System.out.println("   value = " + Config.RUNTIME_VALUE + "   (init happened exactly once, above)");

        // Class.forName(name, initialize=false, loader): load without initializing
        Class<?> h = Class.forName(Holder.class.getName(), false, ClassLoadingDemo.class.getClassLoader());
        System.out.println("   Class.forName(Holder, initialize=false) -> loaded " + h.getSimpleName() + ", still not initialized");
        System.out.println("   touching Holder.INSTANCE ->");
        Objects.requireNonNull(Holder.INSTANCE);
    }

    // =================================================================
    // 3. Parent initialized before child. Static init happens once per class.
    // =================================================================
    static class Base {
        static { System.out.println("      1. Base <clinit>"); }
        static int baseCounter = 0;
    }

    static class Derived extends Base {
        static { System.out.println("      2. Derived <clinit>"); }
        static void hello() { System.out.println("      3. Derived.hello()"); }
    }

    static void initializationOrderWithInheritance() {
        System.out.println("\n===== 3. PARENT FIRST =====");
        System.out.println("   accessing Derived.baseCounter (declared in Base) ->");
        System.out.println("   value " + Derived.baseCounter + "  (only Base was initialized: the field belongs to Base)");
        System.out.println("   calling Derived.hello() ->");
        Derived.hello();
    }

    // =================================================================
    // 4. A failing static initializer: ExceptionInInitializerError,
    //    and then NoClassDefFoundError on every later use
    // =================================================================
    static class BrokenConfig {
        static final int PORT = Integer.parseInt("not-a-number");   // throws during <clinit>
    }

    static void failingStaticInit() {
        System.out.println("\n===== 4. FAILED STATIC INIT =====");
        try {
            System.out.println(BrokenConfig.PORT);
        } catch (ExceptionInInitializerError e) {
            System.out.println("   1st access: ExceptionInInitializerError caused by " + e.getCause().getClass().getSimpleName());
        }
        try {
            System.out.println(BrokenConfig.PORT);
        } catch (NoClassDefFoundError e) {
            System.out.println("   2nd access: NoClassDefFoundError (the class is permanently unusable in this loader)");
        }
    }

    // =================================================================
    // 5. A custom ClassLoader + "same name, different class"
    //    We compile a tiny Greeter class IN MEMORY (javax.tools), then load
    //    the SAME bytes with two separate loaders: the essence of hot reload.
    // =================================================================
    static class BytesClassLoader extends ClassLoader {
        private final Map<String, byte[]> classes;

        BytesClassLoader(String name, Map<String, byte[]> classes) {
            super(name, ClassLoadingDemo.class.getClassLoader());   // parent = our app loader (delegation)
            this.classes = classes;
        }

        // Called only after the parent chain fails to find the class
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classes.get(name);
            if (bytes == null) throw new ClassNotFoundException(name);
            System.out.println("      [" + getName() + "] defineClass(" + name + ", " + bytes.length + " bytes)");
            return defineClass(name, bytes, 0, bytes.length);       // bytes -> Class object
        }
    }

    static void customClassLoader() throws Exception {
        System.out.println("\n===== 5. CUSTOM CLASSLOADER =====");
        String v1 = "public class Greeter { public String greet() { return \"Hello from version 1\"; } }";
        String v2 = "public class Greeter { public String greet() { return \"Hello from version 2 (hot reloaded)\"; } }";

        Class<?> g1 = new BytesClassLoader("loader-v1", compile("Greeter", v1)).loadClass("Greeter");
        Class<?> g2 = new BytesClassLoader("loader-v2", compile("Greeter", v2)).loadClass("Greeter");

        Object o1 = g1.getDeclaredConstructor().newInstance();
        Object o2 = g2.getDeclaredConstructor().newInstance();
        System.out.println("   v1: " + g1.getMethod("greet").invoke(o1));
        System.out.println("   v2: " + g2.getMethod("greet").invoke(o2));
        System.out.println("   same name? " + g1.getName().equals(g2.getName()) + ", same class? " + (g1 == g2));

        try {
            g1.cast(o2);                                              // Greeter(v2) -> Greeter(v1)
        } catch (ClassCastException e) {
            System.out.println("   casting across loaders -> ClassCastException: " + e.getMessage());
        }

        // Delegation in action: java.lang.String is NOT defined by our loader, the parent chain wins
        Class<?> s = new BytesClassLoader("loader-x", Map.of()).loadClass("java.lang.String");
        System.out.println("   loader-x.loadClass(\"java.lang.String\") came from: " + s.getClassLoader() + " (bootstrap)");
        System.out.println("   (A class is unloaded only when its LOADER becomes unreachable: hold on to");
        System.out.println("    loaders by accident, e.g. in a static map or a ThreadLocal, and Metaspace leaks.)");
    }

    // ---- compile Java source to bytecode entirely in memory ----
    static Map<String, byte[]> compile(String className, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Map<String, ByteArrayOutputStream> out = new HashMap<>();
        JavaFileManager fm = new ForwardingJavaFileManager<>(compiler.getStandardFileManager(null, null, null)) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location loc, String name, JavaFileObject.Kind kind, FileObject sibling) {
                return new SimpleJavaFileObject(URI.create("mem:///" + name + kind.extension), kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        out.put(name, bos);
                        return bos;
                    }
                };
            }
        };
        JavaFileObject src = new SimpleJavaFileObject(URI.create("string:///" + className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignore) { return source; }
        };
        if (!compiler.getTask(null, fm, null, null, null, List.of(src)).call()) throw new IllegalStateException("compile failed");
        Map<String, byte[]> result = new HashMap<>();
        out.forEach((k, v) -> result.put(k, v.toByteArray()));
        return result;
    }
}
