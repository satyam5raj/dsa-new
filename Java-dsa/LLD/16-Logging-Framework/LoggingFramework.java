/*
 * ============================================================================
 *  LLD 16 : LOGGING FRAMEWORK  (a mini Log4j / Logback)
 * ============================================================================
 *  Run:  java LoggingFramework.java          (design notes: README.md)
 *
 *     [core]       Level, LogRecord (immutable), MDC (per-thread context, e.g. requestId)
 *     [loggers]    Logger with a dotted-name HIERARCHY ("com.app.db" inherits from "com.app",
 *                  then ROOT), effective level, ADDITIVITY; LoggerRepository caches loggers
 *     [format]     Formatter (Strategy): PatternFormatter ("%d [%t] %p %c - %m%n"), JsonFormatter
 *     [filters]    Filter chain (Chain of Responsibility): ACCEPT / DENY / NEUTRAL
 *     [appenders]  Appender (Observer of log records): Console, InMemory, RollingFile (by size);
 *                  AsyncAppender (Decorator) with a bounded queue and an overflow policy
 *     [config]     LoggingConfig.Builder - configure everything fluently, apply atomically
 *     [demo]       levels, hierarchy, laziness, formats, filters, additivity, rolling, MDC,
 *                  async ordering + drops, and 8 threads logging concurrently
 *
 *  THE FAST PATH MATTERS MOST: most log calls are DISABLED (debug in production). They must
 *  cost ~nothing: one volatile read + compare, NO string building. Hence:
 *     log.debug(() -> "user " + expensiveToString())     // Supplier: only evaluated if enabled
 * ============================================================================
 */

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class LoggingFramework {

    // =====================================================================
    // [core]
    // =====================================================================

    enum Level { TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF;
        boolean atLeast(Level threshold) { return this.ordinal() >= threshold.ordinal(); }
    }

    /** Immutable event: everything an appender might need, captured at the call site. */
    record LogRecord(long timeMs, Level level, String logger, String thread, String message, Throwable error, Map<String, String> mdc) {}

    /** Mapped Diagnostic Context: per-thread key/values automatically attached to every record. */
    static final class MDC {
        private static final ThreadLocal<Map<String, String>> CTX = ThreadLocal.withInitial(LinkedHashMap::new);
        static void put(String k, String v) { CTX.get().put(k, v); }
        static void clear() { CTX.get().clear(); }                  // ALWAYS clear in finally: thread pools reuse threads!
        static Map<String, String> snapshot() { return Map.copyOf(CTX.get()); }
    }

    // =====================================================================
    // [format]
    // =====================================================================

    interface Formatter { String format(LogRecord r); }

    /** %d time  %t thread  %p level  %c logger  %m message  %X{key} MDC value  %n newline */
    static class PatternFormatter implements Formatter {
        private final String pattern;
        PatternFormatter(String pattern) { this.pattern = pattern; }
        public String format(LogRecord r) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pattern.length(); i++) {
                char ch = pattern.charAt(i);
                if (ch != '%' || i + 1 >= pattern.length()) { sb.append(ch); continue; }
                char k = pattern.charAt(++i);
                switch (k) {
                    case 'd' -> sb.append(Instant.ofEpochMilli(r.timeMs()));
                    case 't' -> sb.append(r.thread());
                    case 'p' -> sb.append(String.format("%-5s", r.level()));
                    case 'c' -> sb.append(r.logger());
                    case 'm' -> sb.append(r.message());
                    case 'n' -> sb.append('\n');
                    case 'X' -> {                                   // %X{requestId}
                        int end = pattern.indexOf('}', i);
                        String key = pattern.substring(i + 2, end);
                        sb.append(r.mdc().getOrDefault(key, "-"));
                        i = end;
                    }
                    default -> sb.append('%').append(k);
                }
            }
            if (r.error() != null) sb.append(" | ").append(r.error());
            return sb.toString();
        }
    }

    /** One JSON object per line - what log shippers (Fluent Bit, Logstash) and ELK want. */
    static class JsonFormatter implements Formatter {
        public String format(LogRecord r) {
            StringBuilder sb = new StringBuilder("{");
            field(sb, "ts", Instant.ofEpochMilli(r.timeMs()).toString()).append(',');
            field(sb, "level", r.level().name()).append(',');
            field(sb, "logger", r.logger()).append(',');
            field(sb, "thread", r.thread()).append(',');
            field(sb, "msg", r.message());
            for (var e : r.mdc().entrySet()) { sb.append(','); field(sb, e.getKey(), e.getValue()); }
            if (r.error() != null) { sb.append(','); field(sb, "error", r.error().toString()); }
            return sb.append('}').toString();
        }
        private static StringBuilder field(StringBuilder sb, String k, String v) { return sb.append('"').append(esc(k)).append("\":\"").append(esc(v)).append('"'); }
        static String esc(String s) {                           // an unescaped quote or newline breaks every parser downstream
            StringBuilder o = new StringBuilder();
            for (char c : s.toCharArray()) switch (c) {
                case '"' -> o.append("\\\""); case '\\' -> o.append("\\\\"); case '\n' -> o.append("\\n"); case '\t' -> o.append("\\t");
                default -> { if (c < 0x20) o.append(String.format("\\u%04x", (int) c)); else o.append(c); }
            }
            return o.toString();
        }
    }

    // =====================================================================
    // [filters] chain of responsibility
    // =====================================================================

    enum FilterResult { ACCEPT, DENY, NEUTRAL }

    interface Filter { FilterResult decide(LogRecord r); }

    record ThresholdFilter(Level min) implements Filter {
        public FilterResult decide(LogRecord r) { return r.level().atLeast(min) ? FilterResult.NEUTRAL : FilterResult.DENY; }
    }

    /** Drop noisy messages, e.g. load-balancer health checks. */
    record ContainsDenyFilter(String text) implements Filter {
        public FilterResult decide(LogRecord r) { return r.message().contains(text) ? FilterResult.DENY : FilterResult.NEUTRAL; }
    }

    /** Always keep records for a given MDC value (e.g. a user being debugged), whatever later filters say. */
    record MdcAcceptFilter(String key, String value) implements Filter {
        public FilterResult decide(LogRecord r) { return value.equals(r.mdc().get(key)) ? FilterResult.ACCEPT : FilterResult.NEUTRAL; }
    }

    /** Walk the chain: the first ACCEPT or DENY decides; all NEUTRAL means accept. */
    static boolean passes(List<Filter> chain, LogRecord r) {
        for (Filter f : chain) {
            FilterResult res = f.decide(r);
            if (res != FilterResult.NEUTRAL) return res == FilterResult.ACCEPT;
        }
        return true;
    }

    // =====================================================================
    // [appenders]
    // =====================================================================

    interface Appender {
        String name();
        void append(LogRecord r);
        default void close() {}
    }

    /** Shared base: filter chain + formatter + a lock so lines never interleave. */
    static abstract class AbstractAppender implements Appender {
        private final String name;
        protected final Formatter formatter;
        private final List<Filter> filters;
        AbstractAppender(String name, Formatter f, List<Filter> filters) { this.name = name; this.formatter = f; this.filters = List.copyOf(filters); }
        public String name() { return name; }
        public final void append(LogRecord r) {                  // TEMPLATE: filter -> format -> write
            if (!passes(filters, r)) return;
            String line = formatter.format(r);                    // format OUTSIDE the lock (it's the slow part)
            synchronized (this) { write(line); }
        }
        protected abstract void write(String line);
    }

    static class ConsoleAppender extends AbstractAppender {
        ConsoleAppender(Formatter f, List<Filter> fs) { super("console", f, fs); }
        protected void write(String line) { System.out.println("      | " + line.stripTrailing()); }
    }

    static class InMemoryAppender extends AbstractAppender {
        final List<String> lines = new ArrayList<>();
        InMemoryAppender(String name, Formatter f, List<Filter> fs) { super(name, f, fs); }
        protected void write(String line) { lines.add(line); }
        synchronized List<String> snapshot() { return List.copyOf(lines); }
    }

    /** Rolls to a new "file" when the current one exceeds maxBytes; keeps maxFiles (oldest deleted). */
    static class RollingFileAppender extends AbstractAppender {
        private final long maxBytes; private final int maxFiles;
        final Deque<StringBuilder> files = new ArrayDeque<>();   // last = current (in-memory stand-in for disk)
        RollingFileAppender(Formatter f, long maxBytes, int maxFiles) { super("file", f, List.of()); this.maxBytes = maxBytes; this.maxFiles = maxFiles; files.add(new StringBuilder()); }
        protected void write(String line) {
            if (files.getLast().length() + line.length() + 1 > maxBytes && files.getLast().length() > 0) {
                files.addLast(new StringBuilder());               // roll: app.log -> app.1.log ...
                while (files.size() > maxFiles) files.removeFirst();   // retention
            }
            files.getLast().append(line).append('\n');
        }
    }

    /**
     * DECORATOR: makes ANY appender asynchronous. The caller only enqueues (microseconds); a
     * background thread does the slow I/O. A BOUNDED queue protects memory; when full we
     * either BLOCK the caller (lossless) or DROP (protect latency) - and count the drops.
     */
    static class AsyncAppender implements Appender {
        enum Overflow { BLOCK, DROP }
        private final Appender inner;
        private final BlockingQueue<LogRecord> queue;
        private final Overflow overflow;
        final AtomicLong dropped = new AtomicLong();
        private final Thread worker;
        private volatile boolean running = true;
        private static final LogRecord POISON = new LogRecord(0, Level.OFF, "", "", "", null, Map.of());

        AsyncAppender(Appender inner, int capacity, Overflow overflow) {
            this.inner = inner; this.queue = new ArrayBlockingQueue<>(capacity); this.overflow = overflow;
            worker = new Thread(() -> {
                try {
                    while (true) { LogRecord r = queue.take(); if (r == POISON) return; inner.append(r); }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "async-log-" + inner.name());
            worker.setDaemon(true);
            worker.start();
        }
        public String name() { return "async(" + inner.name() + ")"; }
        public void append(LogRecord r) {
            if (!running) return;
            if (overflow == Overflow.BLOCK) { try { queue.put(r); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
            else if (!queue.offer(r)) dropped.incrementAndGet();
        }
        /** Drain everything before exit - otherwise the last (often most important) logs are lost. */
        public void close() {
            running = false;
            try { queue.put(POISON); worker.join(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            inner.close();
        }
    }

    // =====================================================================
    // [loggers]
    // =====================================================================

    static final class Logger {
        final String name;
        final Logger parent;
        private volatile Level level;                           // null = inherit
        private volatile boolean additive = true;
        private final List<Appender> appenders = new CopyOnWriteArrayList<>();   // read on every log call, rarely written
        private final LoggerRepository repo;

        Logger(String name, Logger parent, LoggerRepository repo) { this.name = name; this.parent = parent; this.repo = repo; }

        /** Walk up until a configured level is found. ROOT always has one. */
        Level effectiveLevel() { for (Logger l = this; l != null; l = l.parent) if (l.level != null) return l.level; return Level.INFO; }
        boolean isEnabled(Level lv) { return lv != Level.OFF && lv.atLeast(effectiveLevel()); }

        void trace(Supplier<String> m) { log(Level.TRACE, m, null); }
        void debug(Supplier<String> m) { log(Level.DEBUG, m, null); }
        void info(String m) { log(Level.INFO, () -> m, null); }
        void warn(String m) { log(Level.WARN, () -> m, null); }
        void error(String m, Throwable t) { log(Level.ERROR, () -> m, t); }

        void log(Level lv, Supplier<String> msg, Throwable t) {
            if (!isEnabled(lv)) return;                          // FAST PATH: nothing allocated, supplier never called
            LogRecord r = new LogRecord(repo.clock.getAsLong(), lv, name, Thread.currentThread().getName(), msg.get(), t, MDC.snapshot());
            // send to my appenders, then my parent's, ... until a non-additive logger stops it
            for (Logger l = this; l != null; l = l.parent) {
                for (Appender a : l.appenders) a.append(r);
                if (!l.additive) break;
            }
        }
    }

    /** Factory + cache: getLogger("a.b.c") always returns the same instance, creating parents as needed. */
    static final class LoggerRepository {
        final Logger root;
        private final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();
        final LongSupplier clock;
        LoggerRepository(LongSupplier clock) { this.clock = clock; root = new Logger("ROOT", null, this); root.level = Level.INFO; }

        Logger getLogger(String name) {
            if (name.isEmpty()) return root;
            Logger existing = loggers.get(name);
            if (existing != null) return existing;
            int dot = name.lastIndexOf('.');
            Logger parent = dot < 0 ? root : getLogger(name.substring(0, dot));
            return loggers.computeIfAbsent(name, n -> new Logger(n, parent, this));
        }
        Logger getLogger(Class<?> c) { return getLogger(c.getName()); }
    }

    // =====================================================================
    // [config] builder
    // =====================================================================

    static final class LoggingConfig {
        private final Map<String, Level> levels; private final Map<String, List<Appender>> appenders; private final Set<String> nonAdditive;
        private LoggingConfig(Builder b) { levels = Map.copyOf(b.levels); appenders = Map.copyOf(b.appenders); nonAdditive = Set.copyOf(b.nonAdditive); }
        static Builder builder() { return new Builder(); }

        void applyTo(LoggerRepository repo) {
            levels.forEach((n, lv) -> repo.getLogger(n).level = lv);
            appenders.forEach((n, list) -> { Logger l = repo.getLogger(n); l.appenders.clear(); l.appenders.addAll(list); });
            nonAdditive.forEach(n -> repo.getLogger(n).additive = false);
        }

        static final class Builder {
            private final Map<String, Level> levels = new HashMap<>();
            private final Map<String, List<Appender>> appenders = new HashMap<>();
            private final Set<String> nonAdditive = new HashSet<>();
            Builder root(Level lv, Appender... as) { levels.put("", lv); appenders.put("", List.of(as)); return this; }
            Builder logger(String name, Level lv) { levels.put(name, lv); return this; }
            Builder appender(String logger, Appender a) { appenders.computeIfAbsent(logger, k -> new ArrayList<>()).add(a); return this; }
            Builder additivity(String logger, boolean additive) { if (!additive) nonAdditive.add(logger); return this; }
            LoggingConfig build() {
                if (!levels.containsKey("")) throw new IllegalStateException("root level required");
                return new LoggingConfig(this);
            }
        }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong(Instant.parse("2026-10-04T10:00:00Z").toEpochMilli());
        LoggerRepository repo = new LoggerRepository(clock::get);
        Formatter pattern = new PatternFormatter("%d [%t] %p %c - %m [req=%X{requestId}]");
        InMemoryAppender main = new InMemoryAppender("main", pattern, List.of());
        InMemoryAppender errors = new InMemoryAppender("errors", new JsonFormatter(), List.of(new ThresholdFilter(Level.ERROR)));
        LoggingConfig.builder().root(Level.INFO, main).logger("com.shop.db", Level.DEBUG).appender("com.shop", errors).build().applyTo(repo);

        Logger svc = repo.getLogger("com.shop.OrderService"), db = repo.getLogger("com.shop.db.Pool"), lib = repo.getLogger("org.lib.Http");

        System.out.println("1. Levels and the logger hierarchy");
        check("same instance for the same name", true, svc == repo.getLogger("com.shop.OrderService"));
        check("OrderService inherits INFO from ROOT", Level.INFO, svc.effectiveLevel());
        check("db.Pool inherits DEBUG from com.shop.db", Level.DEBUG, db.effectiveLevel());
        svc.debug(() -> "hidden");
        db.debug(() -> "pool size 10");
        check("debug only where enabled", 1, main.snapshot().size());

        System.out.println("2. Lazy messages: disabled calls cost (almost) nothing");
        AtomicInteger built = new AtomicInteger();
        for (int i = 0; i < 100_000; i++) svc.debug(() -> { built.incrementAndGet(); return "expensive " + Arrays.toString(new int[100]); });
        check("supplier never evaluated at INFO", 0, built.get());

        System.out.println("3. Pattern + MDC");
        MDC.put("requestId", "r-42");
        try { svc.info("order placed"); } finally { MDC.clear(); }
        check("pattern line", "2026-10-04T10:00:00Z [main] INFO  com.shop.OrderService - order placed [req=r-42]", main.snapshot().get(1));
        svc.info("no context");
        check("missing MDC key -> '-'", true, main.snapshot().get(2).endsWith("[req=-]"));

        System.out.println("4. Errors go to the JSON appender too (additivity + threshold filter)");
        svc.error("payment failed: card \"declined\"\nretrying", new IllegalStateException("gateway 502"));
        check("one JSON error line", 1, errors.snapshot().size());
        check("JSON escaped quotes/newlines", true, errors.snapshot().get(0).contains("card \\\"declined\\\"\\nretrying"));
        check("and the main appender (additive to ROOT)", 4, main.snapshot().size());
        lib.error("not ours", null);
        check("org.lib errors don't reach the com.shop appender", 1, errors.snapshot().size());

        System.out.println("5. Filter chain: deny health checks, but always keep a debugged user");
        InMemoryAppender filtered = new InMemoryAppender("filtered", pattern, List.of(new MdcAcceptFilter("user", "vip"), new ContainsDenyFilter("/health")));
        Logger web = repo.getLogger("com.web");
        web.appenders.add(filtered);
        web.additive = false;                                        // keep these out of ROOT
        web.info("GET /health 200");
        web.info("GET /orders 200");
        MDC.put("user", "vip");
        try { web.info("GET /health 200"); } finally { MDC.clear(); }
        check("health dropped, orders kept, vip health kept", 2, filtered.snapshot().size());
        check("non-additive: nothing leaked to ROOT", 5, main.snapshot().size());

        System.out.println("6. Rolling file appender");
        RollingFileAppender file = new RollingFileAppender(new PatternFormatter("%p %m"), 50, 3);
        Logger roll = repo.getLogger("roll");
        roll.appenders.add(file);
        roll.additive = false;
        for (int i = 0; i < 20; i++) roll.info("line number " + i);
        check("kept at most 3 files", 3, file.files.size());
        check("each file within 50 bytes", true, file.files.stream().allMatch(f -> f.length() <= 50));
        check("newest line in the current file", true, file.files.getLast().toString().contains("line number 19"));

        System.out.println("7. Async appender: order kept per thread, drained on close; DROP policy counts losses");
        InMemoryAppender sink = new InMemoryAppender("sink", new PatternFormatter("%m"), List.of());
        AsyncAppender async = new AsyncAppender(sink, 1024, AsyncAppender.Overflow.BLOCK);
        Logger a = repo.getLogger("async");
        a.appenders.add(async); a.additive = false;
        for (int i = 0; i < 5000; i++) a.info("m" + i);
        async.close();
        List<String> got = sink.snapshot();
        boolean ordered = got.size() == 5000;
        for (int i = 0; i < got.size() && ordered; i++) ordered = got.get(i).equals("m" + i);
        check("BLOCK: all 5,000 in order after close()", true, ordered);
        CountDownLatch gate = new CountDownLatch(1);
        Appender slow = new Appender() { public String name() { return "slow"; } public void append(LogRecord r) { try { gate.await(); } catch (InterruptedException e) { } } };
        AsyncAppender lossy = new AsyncAppender(slow, 10, AsyncAppender.Overflow.DROP);
        for (int i = 0; i < 100; i++) lossy.append(new LogRecord(0, Level.INFO, "x", "t", "m", null, Map.of()));
        check("DROP: queue full -> drops counted, caller never blocked", true, lossy.dropped.get() >= 89);
        gate.countDown(); lossy.close();

        System.out.println("8. Concurrency: 8 threads x 1,000 lines, each with its own MDC");
        InMemoryAppender shared = new InMemoryAppender("shared", new PatternFormatter("%X{tid}|%m"), List.of());
        Logger c = repo.getLogger("conc");
        c.appenders.add(shared); c.additive = false;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int t = 0; t < 8; t++) { int tid = t; pool.submit(() -> { MDC.put("tid", "" + tid); try { for (int i = 0; i < 1000; i++) c.info("t" + tid + "-" + i); } finally { MDC.clear(); } }); }
        pool.shutdown(); pool.awaitTermination(10, TimeUnit.SECONDS);
        List<String> lines = shared.snapshot();
        boolean wellFormed = lines.stream().allMatch(l -> { String[] p = l.split("\\|"); return p.length == 2 && p[1].startsWith("t" + p[0] + "-"); });
        check("8,000 lines", 8000, lines.size());
        check("no interleaving, MDC never leaked across threads", true, wellFormed);

        System.out.println("9. Console appender with a JSON formatter (what you'd ship to ELK)");
        Logger out = repo.getLogger("demo.console");
        out.appenders.add(new ConsoleAppender(new JsonFormatter(), List.of()));
        out.additive = false;
        MDC.put("requestId", "r-7");
        try { out.warn("disk 91% full"); } finally { MDC.clear(); }
        summary();
    }

    // ---------------- tiny test helper (same in every problem file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-34s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
