/*
 * ============================================================================
 *  PATTERN 1 : SINGLETON  (Creational)
 * ============================================================================
 *
 *  Run:  java P1_Singleton.java
 *
 *  INTENT : Ensure a class has ONLY ONE instance and provide a global
 *           access point to it.
 *
 *  USE WHEN : exactly one object must coordinate something shared:
 *             configuration, a connection pool, a cache, a logger, a registry.
 *
 *  INGREDIENTS
 *    1. private constructor  -> nobody else can call 'new'
 *    2. private static field holding the one instance
 *    3. public static accessor (getInstance)
 *
 *  IMPLEMENTATIONS (know all 5 and their trade-offs)
 *    a) Eager                 - simple, thread-safe, created even if never used
 *    b) Lazy (naive)          - NOT thread-safe
 *    c) synchronized method   - thread-safe, but locks on EVERY call (slow)
 *    d) Double-checked locking with 'volatile'  - lazy + fast
 *    e) Bill Pugh holder class - lazy + thread-safe with NO locking (JVM class-loading guarantee)
 *    f) Enum                  - Effective Java's recommendation: thread-safe,
 *                               serialization-safe, reflection-safe
 *
 *  WHY IS SINGLETON OFTEN CALLED AN ANTI-PATTERN? (senior interview point)
 *    - It's GLOBAL MUTABLE STATE in disguise: hidden dependencies, because a
 *      method can secretly call X.getInstance().
 *    - HARD TO TEST: you can't swap in a fake, and state leaks between tests.
 *    - Violates SRP: the class manages its business logic AND its own lifecycle.
 *    - Tight coupling to a concrete class (violates DIP).
 *    BETTER: make it a "singleton by configuration". Create ONE instance at
 *    startup and INJECT it where needed. Spring beans are singletons by
 *    default without any of these problems.
 */

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;

public class P1_Singleton {

    // a) EAGER: created when the class loads. The JVM guarantees class init is thread-safe.
    static class EagerConfig {
        private static final EagerConfig INSTANCE = new EagerConfig();
        private EagerConfig() {}
        static EagerConfig getInstance() { return INSTANCE; }
    }

    // b) LAZY, NOT THREAD-SAFE: two threads can both see null and create two instances
    static class NaiveLazy {
        private static NaiveLazy instance;
        static int created = 0;
        private NaiveLazy() {
            created++;
            try { Thread.sleep(5); } catch (InterruptedException ignored) {}   // widen the race window
        }
        static NaiveLazy getInstance() {
            if (instance == null) instance = new NaiveLazy();   // check-then-act race
            return instance;
        }
    }

    // c) SYNCHRONIZED METHOD: correct, but every call takes a lock
    static class SyncLazy {
        private static SyncLazy instance;
        private SyncLazy() {}
        static synchronized SyncLazy getInstance() {
            if (instance == null) instance = new SyncLazy();
            return instance;
        }
    }

    // d) DOUBLE-CHECKED LOCKING
    //    'volatile' is ESSENTIAL: 'instance = new X()' is really 3 steps
    //    (allocate, construct, assign). Without volatile, the CPU/JIT may
    //    reorder them so another thread sees a non-null but HALF-CONSTRUCTED object.
    static class DclLazy {
        private static volatile DclLazy instance;
        private DclLazy() {}
        static DclLazy getInstance() {
            DclLazy local = instance;                 // one volatile read (small optimisation)
            if (local == null) {                      // 1st check: no lock (fast path)
                synchronized (DclLazy.class) {
                    local = instance;
                    if (local == null) {              // 2nd check: inside the lock
                        instance = local = new DclLazy();
                    }
                }
            }
            return local;
        }
    }

    // e) BILL PUGH / HOLDER: Holder isn't loaded until getInstance() is first called.
    //    Class loading is lazy and thread-safe, so no explicit locking is needed.
    static class HolderSingleton {
        private HolderSingleton() {}
        private static class Holder {
            static final HolderSingleton INSTANCE = new HolderSingleton();
        }
        static HolderSingleton getInstance() { return Holder.INSTANCE; }
    }

    // f) ENUM: the JVM guarantees exactly one instance per constant, even
    //    across serialization and reflection.
    enum AppRegistry {
        INSTANCE;
        private final Map<String, String> settings = new ConcurrentHashMap<>();
        void set(String k, String v) { settings.put(k, v); }
        String get(String k) { return settings.get(k); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===== a) EAGER =====");
        System.out.println("   same instance? " + (EagerConfig.getInstance() == EagerConfig.getInstance()));

        System.out.println("\n===== b) NAIVE LAZY UNDER CONCURRENCY =====");
        raceTest();

        System.out.println("\n===== c/d/e) THREAD-SAFE LAZY VARIANTS =====");
        System.out.println("   synchronized same? " + (SyncLazy.getInstance() == SyncLazy.getInstance()));
        System.out.println("   DCL same?          " + (DclLazy.getInstance() == DclLazy.getInstance()));
        System.out.println("   Holder same?       " + (HolderSingleton.getInstance() == HolderSingleton.getInstance()));

        System.out.println("\n===== f) ENUM =====");
        AppRegistry.INSTANCE.set("env", "prod");
        System.out.println("   env = " + AppRegistry.INSTANCE.get("env"));

        System.out.println("\n===== BREAKING A SINGLETON WITH REFLECTION =====");
        Constructor<HolderSingleton> ctor = HolderSingleton.class.getDeclaredConstructor();
        ctor.setAccessible(true);                     // bypasses 'private'
        HolderSingleton sneaky = ctor.newInstance();
        System.out.println("   reflection made a 2nd instance? " + (sneaky != HolderSingleton.getInstance()));
        System.out.println("   (defence: throw in the constructor if INSTANCE exists, or use an enum;");
        System.out.println("    enums can't be created via reflection at all)");
        // Serialization can also create a 2nd instance -> fix with readResolve()
        // (see Core-Java/09-Serialization)
    }

    static void raceTest() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Set<Integer> identities = ConcurrentHashMap.newKeySet();
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 8; i++) {
            pool.submit(() -> {
                start.await();                               // release all threads together
                identities.add(System.identityHashCode(NaiveLazy.getInstance()));
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("   constructor ran " + NaiveLazy.created + " time(s), distinct instances seen = "
                + identities.size() + " (should be 1)");
    }
}
