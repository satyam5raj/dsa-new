/*
 * ============================================================================
 *  TOPIC 6 : Future & CompletableFuture - ASYNC PROGRAMMING
 * ============================================================================
 *
 *  Run:  java FutureAndCompletableFuture.java
 *
 *  Future<V> (Java 5) - a handle to a result that will exist LATER
 *    get()            : BLOCKS until done (or get(timeout))
 *    isDone(), cancel(mayInterrupt), isCancelled()
 *    LIMITATIONS: you can only block to get the result. No callbacks, no
 *    chaining, no combining two futures, no clean error handling, and you
 *    can't complete it manually.
 *
 *  CompletableFuture<V> (Java 8) - a Future + a CompletionStage (pipeline)
 *  Think of it as the Java version of a JavaScript Promise:
 *    JS:   fetch(url).then(r => r.json()).then(...).catch(...)
 *    Java: supplyAsync(() -> fetch(url)).thenApply(this::parse).thenAccept(...).exceptionally(...)
 *
 *  CHEAT SHEET
 *  ---------------------------------------------------------------------------
 *   Start         supplyAsync(supplier)  -> CF<T>     runAsync(runnable) -> CF<Void>
 *   Transform     thenApply(fn)          T -> U       (like Stream.map / Promise.then)
 *   Consume       thenAccept(consumer)   T -> void
 *   Run after     thenRun(runnable)      ignores the value
 *   Chain async   thenCompose(fn)        T -> CF<U>   (FLAT-map: avoids CF<CF<U>>)
 *   Combine 2     thenCombine(other, bifn)            (both results needed)
 *   Wait all      allOf(cf...)           -> CF<Void>  (like Promise.all)
 *   First done    anyOf(cf...)           -> CF<Object>(like Promise.race)
 *   Errors        exceptionally(fn)      recover from an exception
 *                 handle((v, ex) -> ..)  always called: success OR failure
 *                 whenComplete((v, ex))  side effect, doesn't change the result
 *   Timeouts      orTimeout(t, unit), completeOnTimeout(value, t, unit)   (Java 9)
 *   Get result    join() (unchecked CompletionException) / get() (checked exceptions)
 *  ---------------------------------------------------------------------------
 *
 *  THREADS: xxxAsync methods without an executor use ForkJoinPool.commonPool()
 *  (size = cores - 1). For BLOCKING I/O, ALWAYS pass your own executor, or
 *  you'll starve the common pool that parallel streams also use.
 *  Non-async thenApply runs on whichever thread completed the previous stage
 *  (or the caller's thread if it's already complete).
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class FutureAndCompletableFuture {

    static final ExecutorService IO_POOL = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "io-pool");
        t.setDaemon(true);
        return t;
    });

    public static void main(String[] args) throws Exception {
        plainFuture();
        basicPipeline();
        thenApplyVsThenCompose();
        combiningFutures();
        errorHandling();
        timeouts();
        realWorldAggregator();
        IO_POOL.shutdown();
    }

    // =================================================================
    // 1. Plain Future: blocking get, cancel
    // =================================================================
    static void plainFuture() throws Exception {
        System.out.println("===== 1. PLAIN FUTURE =====");
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<String> f = ex.submit(() -> { Thread.sleep(200); return "report ready"; });
        System.out.println("   isDone immediately? " + f.isDone());
        System.out.println("   get() blocks -> " + f.get());

        Future<?> slow = ex.submit(() -> { Thread.sleep(5_000); return null; });
        try {
            slow.get(100, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            slow.cancel(true);                              // interrupt the running task
            System.out.println("   timed out, cancelled = " + slow.isCancelled());
        }
        ex.shutdownNow();
    }

    // =================================================================
    // 2. A basic non-blocking pipeline
    // =================================================================
    static void basicPipeline() {
        System.out.println("\n===== 2. BASIC PIPELINE =====");
        CompletableFuture<Void> cf = CompletableFuture
                .supplyAsync(() -> fetchUserJson(42), IO_POOL)             // runs on io-pool
                .thenApply(json -> json.toUpperCase())                     // transform
                .thenApply(String::length)
                .thenAccept(len -> System.out.println("   json length = " + len))  // consume
                .thenRun(() -> System.out.println("   pipeline finished"));
        System.out.println("   main thread is free while the pipeline runs...");
        cf.join();                                                         // wait only for the demo
    }

    // =================================================================
    // 3. thenApply vs thenCompose (the most asked CF question)
    //    getUser returns CF<User>, getOrders(user) returns CF<List<Order>>
    //    thenApply(getOrders)   -> CF<CF<List<Order>>>   nested, awkward
    //    thenCompose(getOrders) -> CF<List<Order>>       flat (like Stream.flatMap)
    // =================================================================
    static CompletableFuture<String> getUser(int id) {
        return CompletableFuture.supplyAsync(() -> { sleep(100); return "user" + id; }, IO_POOL);
    }

    static CompletableFuture<List<String>> getOrders(String user) {
        return CompletableFuture.supplyAsync(() -> { sleep(100); return List.of(user + "-order1", user + "-order2"); }, IO_POOL);
    }

    static void thenApplyVsThenCompose() {
        System.out.println("\n===== 3. thenApply vs thenCompose =====");
        CompletableFuture<CompletableFuture<List<String>>> nested = getUser(1).thenApply(u -> getOrders(u));
        CompletableFuture<List<String>> flat = getUser(1).thenCompose(u -> getOrders(u));
        System.out.println("   thenApply   -> " + nested.join().join() + "  (needed join() twice)");
        System.out.println("   thenCompose -> " + flat.join());
    }

    // =================================================================
    // 4. Combining: two in parallel, many in parallel, first wins
    // =================================================================
    static void combiningFutures() {
        System.out.println("\n===== 4. COMBINING =====");
        long start = System.currentTimeMillis();
        CompletableFuture<Double> price = CompletableFuture.supplyAsync(() -> { sleep(300); return 999.0; }, IO_POOL);
        CompletableFuture<Double> discount = CompletableFuture.supplyAsync(() -> { sleep(300); return 0.1; }, IO_POOL);
        double finalPrice = price.thenCombine(discount, (p, d) -> p * (1 - d)).join();
        System.out.println("   thenCombine price = " + finalPrice + " in ~" + (System.currentTimeMillis() - start) + "ms (parallel, not 600)");

        // allOf: wait for N futures, then collect their results
        List<CompletableFuture<String>> calls = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> { sleep(100 * i); return "svc" + i; }, IO_POOL))
                .toList();
        CompletableFuture<List<String>> all = CompletableFuture
                .allOf(calls.toArray(new CompletableFuture[0]))             // CF<Void>
                .thenApply(v -> calls.stream().map(CompletableFuture::join).toList());   // safe: all are done
        System.out.println("   allOf -> " + all.join());

        // anyOf: the fastest replica wins
        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "replica-A"; }, IO_POOL),
                CompletableFuture.supplyAsync(() -> { sleep(50); return "replica-B"; }, IO_POOL));
        System.out.println("   anyOf -> " + fastest.join());
    }

    // =================================================================
    // 5. Error handling
    //    An exception in a stage skips the following thenApply/thenAccept
    //    stages and flows down to the first exceptionally/handle (like catch).
    // =================================================================
    static void errorHandling() {
        System.out.println("\n===== 5. ERROR HANDLING =====");
        CompletableFuture<String> recovered = CompletableFuture
                .supplyAsync(() -> { if (true) throw new IllegalStateException("payment service down"); return "paid"; }, IO_POOL)
                .thenApply(s -> { System.out.println("   (never printed)"); return s; })
                .exceptionally(ex -> "FALLBACK (" + ex.getCause().getMessage() + ")");
        System.out.println("   exceptionally -> " + recovered.join());

        CompletableFuture<String> handled = CompletableFuture
                .supplyAsync(() -> 10 / 0, IO_POOL)
                .handle((value, ex) -> ex == null ? "result " + value : "handled " + ex.getCause().getClass().getSimpleName());
        System.out.println("   handle        -> " + handled.join());

        CompletableFuture<Integer> logged = CompletableFuture
                .supplyAsync(() -> 7, IO_POOL)
                .whenComplete((v, ex) -> System.out.println("   whenComplete saw v=" + v + ", ex=" + ex));
        System.out.println("   result unchanged -> " + logged.join());

        try {
            CompletableFuture.supplyAsync(() -> { throw new RuntimeException("boom"); }, IO_POOL).join();
        } catch (CompletionException e) {
            System.out.println("   join() wraps it in CompletionException, cause = " + e.getCause().getMessage());
        }
    }

    // =================================================================
    // 6. Timeouts (Java 9+) and manual completion
    // =================================================================
    static void timeouts() {
        System.out.println("\n===== 6. TIMEOUTS & MANUAL COMPLETION =====");
        String withDefault = CompletableFuture
                .supplyAsync(() -> { sleep(1_000); return "live price"; }, IO_POOL)
                .completeOnTimeout("cached price", 100, TimeUnit.MILLISECONDS)
                .join();
        System.out.println("   completeOnTimeout -> " + withDefault);

        try {
            CompletableFuture.supplyAsync(() -> { sleep(1_000); return "x"; }, IO_POOL)
                    .orTimeout(100, TimeUnit.MILLISECONDS).join();
        } catch (CompletionException e) {
            System.out.println("   orTimeout -> " + e.getCause().getClass().getSimpleName());
        }

        // Manually completed future: bridges callback-style APIs into CF
        CompletableFuture<String> promise = new CompletableFuture<>();
        IO_POOL.submit(() -> { sleep(50); promise.complete("callback delivered"); });
        System.out.println("   manual complete -> " + promise.join());
    }

    // =================================================================
    // 7. Real-world: a product page aggregating 3 services in parallel,
    //    each with a timeout and a fallback (what a BFF/API gateway does)
    // =================================================================
    record ProductPage(String details, String reviews, String recommendations) {}

    static void realWorldAggregator() {
        System.out.println("\n===== 7. PARALLEL AGGREGATOR =====");
        long start = System.currentTimeMillis();

        CompletableFuture<String> details = callService("details", 150, false);
        CompletableFuture<String> reviews = callService("reviews", 200, true)         // fails
                .exceptionally(ex -> "reviews unavailable");
        CompletableFuture<String> recs = callService("recommendations", 2_000, false) // too slow
                .completeOnTimeout("popular items", 300, TimeUnit.MILLISECONDS);

        ProductPage page = details
                .thenCombine(reviews, (d, r) -> new String[]{d, r})
                .thenCombine(recs, (dr, rec) -> new ProductPage(dr[0], dr[1], rec))
                .join();
        System.out.println("   " + page);
        System.out.println("   built in ~" + (System.currentTimeMillis() - start) + "ms (bounded by the 300ms timeout)");
    }

    static CompletableFuture<String> callService(String name, long latencyMs, boolean fail) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(latencyMs);
            if (fail) throw new RuntimeException(name + " 503");
            return name + " OK";
        }, IO_POOL);
    }

    static String fetchUserJson(int id) {
        sleep(100);
        return "{\"id\":" + id + ",\"name\":\"satyam\"}";
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
