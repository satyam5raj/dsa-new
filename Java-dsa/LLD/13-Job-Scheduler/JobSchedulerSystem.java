/*
 * ============================================================================
 *  LLD 13 : DISTRIBUTED-STYLE JOB SCHEDULER  (cron / Quartz / Lambda-schedule / Celery beat)
 * ============================================================================
 *  Run:  java JobSchedulerSystem.java          (design notes: README.md)
 *
 *     [definition] JobDefinition (Builder): handler, Schedule, RetryPolicy, maxAttempts,
 *                  timeout, priority
 *     [schedule]   Schedule (Strategy): Once, FixedRate, FixedDelay, DailyAt
 *     [retry]      RetryPolicy (Strategy): NoRetry, FixedBackoff, ExponentialBackoff + jitter
 *     [runs]       JobRun = ONE execution attempt; RunStatus state machine;
 *                  claimed by CAS with a LEASE (worker crash -> lease expires -> re-run)
 *     [store]      RunStore: due-time priority queue + all runs (in production: a DB table
 *                  polled with SELECT ... FOR UPDATE SKIP LOCKED)
 *     [service]    Scheduler: submit / cancel / pollAndRun / recoverExpiredLeases,
 *                  dead-letter queue, listeners
 *     [demo]       schedules, retries + backoff, DLQ, timeout, cancel, lease recovery,
 *                  idempotent handlers, and 8 workers racing over 2,000 runs
 *
 *  THE GUARANTEE YOU CAN ACTUALLY GIVE: AT-LEAST-ONCE.
 *    A worker can finish the work and crash before recording success. The run's lease
 *    expires and another worker runs it AGAIN. "Exactly once" is impossible to promise for
 *    side effects; you get "effectively once" by making HANDLERS IDEMPOTENT
 *    (an idempotency key = the run's logical id, checked by the handler / downstream).
 *
 *  Time is injected (a fake clock) and execution can be synchronous, so every scenario
 *  below is deterministic - no Thread.sleep() based tests.
 * ============================================================================
 */

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class JobSchedulerSystem {

    // =====================================================================
    // [schedule]
    // =====================================================================

    /** When should the NEXT run be, given the previous scheduled time and when it finished? */
    interface Schedule {
        long first(long now);
        /** empty = no more runs (one-time job). */
        OptionalLong next(long prevScheduled, long finishedAt);
    }

    record Once(long at) implements Schedule {
        public long first(long now) { return Math.max(now, at); }
        public OptionalLong next(long p, long f) { return OptionalLong.empty(); }
    }

    /** Every `period` from the previous SCHEDULED time (keeps a steady cadence; can bunch up if late). */
    record FixedRate(long period) implements Schedule {
        public long first(long now) { return now; }
        public OptionalLong next(long prev, long finished) {
            long n = prev + period;
            while (n <= finished) n += period;          // MISFIRE: we were late -> skip missed slots, don't stampede
            return OptionalLong.of(n);
        }
    }

    /** `delay` after the previous run FINISHED (never overlaps; drifts). */
    record FixedDelay(long delay) implements Schedule {
        public long first(long now) { return now; }
        public OptionalLong next(long prev, long finished) { return OptionalLong.of(finished + delay); }
    }

    /** Every day at HH:MM UTC - a tiny "cron". Epoch ms clock. */
    record DailyAt(int hour, int minute) implements Schedule {
        public long first(long now) { return nextAfter(now - 1); }
        public OptionalLong next(long prev, long finished) { return OptionalLong.of(nextAfter(Math.max(prev, finished))); }
        long nextAfter(long t) {
            ZonedDateTime z = Instant.ofEpochMilli(t).atZone(ZoneOffset.UTC);
            ZonedDateTime cand = z.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            if (!cand.isAfter(z)) cand = cand.plusDays(1);
            return cand.toInstant().toEpochMilli();
        }
    }

    // =====================================================================
    // [retry]
    // =====================================================================

    interface RetryPolicy { long backoffMs(int failedAttempt); }

    record FixedBackoff(long ms) implements RetryPolicy { public long backoffMs(int a) { return ms; } }

    /**
     * base * 2^(attempt-1), capped, with "full jitter" (random in [0, cap]) so 10,000 jobs that
     * failed together because a DB blinked don't all retry at the same millisecond (thundering herd).
     */
    static class ExponentialBackoff implements RetryPolicy {
        private final long base, max;
        private final Random rnd;                      // injected -> deterministic tests
        ExponentialBackoff(long base, long max, Random rnd) { this.base = base; this.max = max; this.rnd = rnd; }
        public long backoffMs(int attempt) {
            long cap = Math.min(max, base << Math.min(attempt - 1, 30));
            return rnd == null ? cap : 1 + (long) (rnd.nextDouble() * cap);
        }
    }

    // =====================================================================
    // [definition] Builder
    // =====================================================================

    /** What the job DOES. Gets the context so it can use run.logicalId() as an idempotency key. */
    @FunctionalInterface
    interface JobHandler { void execute(RunContext ctx) throws Exception; }

    record RunContext(String jobId, String logicalId, int attempt) {}

    static final class JobDefinition {
        final String id;
        final JobHandler handler;
        final Schedule schedule;
        final RetryPolicy retry;
        final int maxAttempts, priority;
        final long timeoutMs;

        private JobDefinition(Builder b) {
            id = b.id; handler = b.handler; schedule = b.schedule; retry = b.retry;
            maxAttempts = b.maxAttempts; priority = b.priority; timeoutMs = b.timeoutMs;
        }

        static Builder builder(String id, JobHandler h) { return new Builder(id, h); }

        /** BUILDER: required fields in the constructor, sensible defaults, validated build(). */
        static final class Builder {
            private final String id;
            private final JobHandler handler;
            private Schedule schedule = new Once(0);
            private RetryPolicy retry = new FixedBackoff(1000);
            private int maxAttempts = 3, priority = 5;
            private long timeoutMs = 60_000;
            private Builder(String id, JobHandler h) { this.id = Objects.requireNonNull(id); this.handler = Objects.requireNonNull(h); }
            Builder schedule(Schedule s) { schedule = s; return this; }
            Builder retry(RetryPolicy r) { retry = r; return this; }
            Builder maxAttempts(int n) { maxAttempts = n; return this; }
            Builder priority(int p) { priority = p; return this; }        // lower number = more urgent
            Builder timeoutMs(long t) { timeoutMs = t; return this; }
            JobDefinition build() {
                if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts >= 1");
                if (timeoutMs <= 0) throw new IllegalArgumentException("timeout > 0");
                return new JobDefinition(this);
            }
        }
    }

    // =====================================================================
    // [runs]
    // =====================================================================

    enum RunStatus { SCHEDULED, RUNNING, SUCCEEDED, FAILED, DEAD, CANCELLED }

    /**
     * One attempt of one occurrence. logicalId identifies the OCCURRENCE (job + scheduled time)
     * and stays the same across retries -> it's the idempotency key handlers should use.
     */
    static final class JobRun {
        final String runId, jobId, logicalId;
        final long scheduledAt;
        final int attempt, priority;
        final AtomicReference<RunStatus> status = new AtomicReference<>(RunStatus.SCHEDULED);
        volatile String worker;
        volatile long leaseUntil;
        volatile String error;

        JobRun(String runId, String jobId, String logicalId, long scheduledAt, int attempt, int priority) {
            this.runId = runId; this.jobId = jobId; this.logicalId = logicalId; this.scheduledAt = scheduledAt; this.attempt = attempt; this.priority = priority;
        }

        /** The claim: only ONE worker can move SCHEDULED -> RUNNING. */
        boolean claim(String worker, long leaseUntil) {
            if (!status.compareAndSet(RunStatus.SCHEDULED, RunStatus.RUNNING)) return false;
            this.worker = worker; this.leaseUntil = leaseUntil;
            return true;
        }
    }

    // =====================================================================
    // [store]
    // =====================================================================

    /** Due runs ordered by (time, priority). A DB would index (status, scheduled_at, priority). */
    static class RunStore {
        private final PriorityBlockingQueue<JobRun> due = new PriorityBlockingQueue<>(64,
                Comparator.comparingLong((JobRun r) -> r.scheduledAt).thenComparingInt(r -> r.priority).thenComparing(r -> r.runId));
        final Map<String, JobRun> all = new ConcurrentHashMap<>();
        void add(JobRun r) { all.put(r.runId, r); due.add(r); }

        /** Remove and return up to `max` runs that are due now (skips cancelled ones). */
        List<JobRun> takeDue(long now, int max) {
            List<JobRun> out = new ArrayList<>();
            synchronized (due) {                          // peek+poll must be atomic across pollers
                while (out.size() < max) {
                    JobRun head = due.peek();
                    if (head == null || head.scheduledAt > now) break;
                    due.poll();
                    if (head.status.get() == RunStatus.SCHEDULED) out.add(head);
                }
            }
            return out;
        }
    }

    // =====================================================================
    // [service]
    // =====================================================================

    interface SchedulerListener {
        default void onSuccess(JobRun r) {}
        default void onRetry(JobRun failed, JobRun next) {}
        default void onDead(JobRun r) {}
    }

    static class Scheduler {
        private final Map<String, JobDefinition> jobs = new ConcurrentHashMap<>();
        private final Set<String> cancelledJobs = ConcurrentHashMap.newKeySet();
        final RunStore store = new RunStore();
        final List<JobRun> deadLetters = new CopyOnWriteArrayList<>();
        private final List<SchedulerListener> listeners = new CopyOnWriteArrayList<>();
        private final LongSupplier clock;
        private final long leaseMs;
        private final AtomicLong seq = new AtomicLong();

        Scheduler(LongSupplier clock, long leaseMs) { this.clock = clock; this.leaseMs = leaseMs; }
        void addListener(SchedulerListener l) { listeners.add(l); }

        void submit(JobDefinition d) {
            if (jobs.putIfAbsent(d.id, d) != null) throw new IllegalStateException("Job " + d.id + " exists");
            long first = d.schedule.first(clock.getAsLong());
            store.add(newRun(d, first, 1, d.id + "@" + first));
        }

        private JobRun newRun(JobDefinition d, long at, int attempt, String logicalId) {
            return new JobRun("R" + seq.incrementAndGet(), d.id, logicalId, at, attempt, d.priority);
        }

        /** Stops future occurrences and pending retries. A RUNNING attempt finishes (no thread kill). */
        void cancel(String jobId) {
            cancelledJobs.add(jobId);
            store.all.values().stream().filter(r -> r.jobId.equals(jobId)).forEach(r -> r.status.compareAndSet(RunStatus.SCHEDULED, RunStatus.CANCELLED));
        }

        /**
         * One poll of one worker: claim due runs and execute them. Many workers may call this
         * concurrently; claim() guarantees one executor per run.
         */
        int pollAndRun(String worker, int batch) {
            int ran = 0;
            for (JobRun r : store.takeDue(clock.getAsLong(), batch)) {
                if (!r.claim(worker, clock.getAsLong() + leaseMs)) continue;
                execute(r);
                ran++;
            }
            return ran;
        }

        private void execute(JobRun r) {
            JobDefinition d = jobs.get(r.jobId);
            long start = clock.getAsLong();
            try {
                d.handler.execute(new RunContext(r.jobId, r.logicalId, r.attempt));
                if (clock.getAsLong() - start > d.timeoutMs) throw new TimeoutException("took " + (clock.getAsLong() - start) + "ms > " + d.timeoutMs);
                if (!r.status.compareAndSet(RunStatus.RUNNING, RunStatus.SUCCEEDED)) return;   // lease was lost meanwhile
                listeners.forEach(l -> l.onSuccess(r));
                scheduleNextOccurrence(d, r);
            } catch (Exception e) {
                r.error = e.getClass().getSimpleName() + ": " + e.getMessage();
                if (!r.status.compareAndSet(RunStatus.RUNNING, RunStatus.FAILED)) return;
                handleFailure(d, r);
            }
        }

        private void handleFailure(JobDefinition d, JobRun r) {
            if (cancelledJobs.contains(d.id)) return;
            if (r.attempt < d.maxAttempts) {
                JobRun retry = newRun(d, clock.getAsLong() + d.retry.backoffMs(r.attempt), r.attempt + 1, r.logicalId);   // SAME logical id
                store.add(retry);
                listeners.forEach(l -> l.onRetry(r, retry));
            } else {
                r.status.set(RunStatus.DEAD);
                deadLetters.add(r);                           // park it for a human / a replay tool
                listeners.forEach(l -> l.onDead(r));
                scheduleNextOccurrence(d, r);                 // a recurring job keeps its cadence
            }
        }

        private void scheduleNextOccurrence(JobDefinition d, JobRun r) {
            if (cancelledJobs.contains(d.id)) return;
            d.schedule.next(occurrenceOf(r), clock.getAsLong())
                    .ifPresent(t -> store.add(newRun(d, t, 1, d.id + "@" + t)));
        }

        /**
         * The crash-recovery sweep: a RUNNING run whose lease expired belongs to a dead (or
         * stuck) worker. Mark it FAILED and retry it -> at-least-once execution.
         */
        int recoverExpiredLeases() {
            long now = clock.getAsLong();
            int n = 0;
            for (JobRun r : store.all.values())
                if (r.status.get() == RunStatus.RUNNING && r.leaseUntil <= now && r.status.compareAndSet(RunStatus.RUNNING, RunStatus.FAILED)) {
                    r.error = "lease expired (worker " + r.worker + " presumed dead)";
                    handleFailure(jobs.get(r.jobId), r);
                    n++;
                }
            return n;
        }

        List<JobRun> runsOf(String jobId) {
            return store.all.values().stream().filter(r -> r.jobId.equals(jobId)).sorted(Comparator.comparingLong((JobRun r) -> r.scheduledAt).thenComparing(r -> Long.parseLong(r.runId.substring(1)))).toList();
        }
    }

    // the occurrence time is encoded in the logical id ("job@time"), so a retried occurrence
    // computes its NEXT occurrence from the original slot, not from the retry time
    static long occurrenceOf(JobRun r) { return Long.parseLong(r.logicalId.substring(r.logicalId.lastIndexOf('@') + 1)); }

    // =====================================================================
    // [demo]
    // =====================================================================

    static class FakeClock implements LongSupplier { volatile long now; public long getAsLong() { return now; } void advance(long ms) { now += ms; } }

    public static void main(String[] args) throws Exception {
        FakeClock clock = new FakeClock();
        Scheduler s = new Scheduler(clock, 30_000);
        List<String> log = new CopyOnWriteArrayList<>();
        s.addListener(new SchedulerListener() {
            public void onRetry(JobRun f, JobRun n) { log.add("retry " + f.jobId + " #" + n.attempt + " at +" + (n.scheduledAt - clock.now)); }
            public void onDead(JobRun r) { log.add("DEAD " + r.jobId); }
        });

        System.out.println("1. Schedules");
        List<Long> rateTimes = new ArrayList<>(), delayTimes = new ArrayList<>();
        s.submit(JobDefinition.builder("report", ctx -> rateTimes.add(clock.now)).schedule(new FixedRate(1000)).build());
        s.submit(JobDefinition.builder("cleanup", ctx -> { delayTimes.add(clock.now); clock.advance(300); }).schedule(new FixedDelay(1000)).build());
        for (int i = 0; i < 40; i++) { s.pollAndRun("w1", 10); clock.advance(100); }
        check("fixed rate keeps the cadence", true, rateTimes.size() >= 3 && rateTimes.get(1) - rateTimes.get(0) == 1000);
        check("fixed delay = finish + 1000 (drifts)", true, delayTimes.size() >= 2 && delayTimes.get(1) - delayTimes.get(0) >= 1300);
        s.cancel("report"); s.cancel("cleanup");
        int before = rateTimes.size();
        for (int i = 0; i < 30; i++) { s.pollAndRun("w1", 10); clock.advance(100); }
        check("cancel stops future runs", before, rateTimes.size());
        long t = Instant.parse("2026-10-03T23:30:00Z").toEpochMilli();
        DailyAt nine = new DailyAt(9, 0);
        check("daily 09:00 after 23:30 -> next morning", "2026-10-04T09:00:00Z", Instant.ofEpochMilli(nine.first(t)).toString());
        FixedRate fr = new FixedRate(1000);
        check("misfire: 3.5 periods late -> next slot, no catch-up storm", 4000L, fr.next(0, 3500).getAsLong());

        System.out.println("2. Retries with backoff, then the dead-letter queue");
        AtomicInteger calls = new AtomicInteger();
        s.submit(JobDefinition.builder("flaky", ctx -> { if (calls.incrementAndGet() < 3) throw new RuntimeException("DB timeout"); })
                .schedule(new Once(clock.now)).retry(new ExponentialBackoff(1000, 60_000, null)).maxAttempts(5).build());
        for (int i = 0; i < 100; i++) { s.pollAndRun("w1", 10); clock.advance(100); }
        check("succeeded on attempt 3", "[FAILED, FAILED, SUCCEEDED]", s.runsOf("flaky").stream().map(r -> r.status.get()).toList());
        check("backoff 1s then 2s", "[retry flaky #2 at +1000, retry flaky #3 at +2000]", log);
        check("retries share one logical id (idempotency key)", 1L, s.runsOf("flaky").stream().map(r -> r.logicalId).distinct().count());
        s.submit(JobDefinition.builder("broken", ctx -> { throw new IllegalStateException("bad config"); }).schedule(new Once(clock.now)).maxAttempts(2).retry(new FixedBackoff(500)).build());
        for (int i = 0; i < 20; i++) { s.pollAndRun("w1", 10); clock.advance(100); }
        check("gave up after 2 attempts -> DLQ", "broken", s.deadLetters.get(0).jobId);
        ExponentialBackoff jitter = new ExponentialBackoff(1000, 30_000, new Random(1));
        boolean inRange = true;
        for (int a = 1; a <= 10; a++) { long b = jitter.backoffMs(a); inRange &= b >= 1 && b <= Math.min(30_000, 1000L << (a - 1)); }
        check("jittered backoff within [1, min(cap, base*2^n)]", true, inRange);

        System.out.println("3. Timeout counts as a failure");
        s.submit(JobDefinition.builder("slow", ctx -> clock.advance(5_000)).schedule(new Once(clock.now)).timeoutMs(2_000).maxAttempts(1).build());
        s.pollAndRun("w1", 10);
        check("timed out -> DEAD", RunStatus.DEAD, s.runsOf("slow").get(0).status.get());
        check("error recorded", true, s.runsOf("slow").get(0).error.startsWith("TimeoutException"));

        System.out.println("4. Worker crash: the lease expires and the run is retried (at-least-once)");
        Set<String> sideEffects = ConcurrentHashMap.newKeySet();     // e.g. "invoice emailed" keyed by logical id
        AtomicInteger executions = new AtomicInteger();
        JobHandler idempotent = ctx -> { executions.incrementAndGet(); sideEffects.add(ctx.logicalId()); };
        s.submit(JobDefinition.builder("invoice", idempotent).schedule(new Once(clock.now)).build());
        JobRun claimed = s.store.takeDue(clock.now, 10).get(0);
        claimed.claim("w-crashy", clock.now + 30_000);
        idempotent.execute(new RunContext(claimed.jobId, claimed.logicalId, 1));   // did the work...
        // ...and crashed before marking SUCCEEDED
        clock.advance(31_000);
        check("sweeper recovers 1 expired lease", 1, s.recoverExpiredLeases());
        clock.advance(1_000);                                                      // the retry's backoff
        s.pollAndRun("w2", 10);
        check("the handler ran twice (at-least-once)", 2, executions.get());
        check("but the side effect happened once (idempotent)", 1, sideEffects.size());

        System.out.println("5. Concurrency: 8 workers poll 2,000 due runs at once");
        FakeClock c2 = new FakeClock();
        Scheduler big = new Scheduler(c2, 60_000);
        ConcurrentHashMap<String, AtomicInteger> perJob = new ConcurrentHashMap<>();
        for (int i = 0; i < 2000; i++) {
            String id = "j" + i;
            big.submit(JobDefinition.builder(id, ctx -> perJob.computeIfAbsent(ctx.jobId(), k -> new AtomicInteger()).incrementAndGet())
                    .schedule(new Once(0)).priority(i % 10).build());
        }
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> fs = new ArrayList<>();
        for (int w = 0; w < 8; w++) { String name = "w" + w; fs.add(pool.submit(() -> { go.await(); int n = 0, k; while ((k = big.pollAndRun(name, 16)) > 0) n += k; return n; })); }
        go.countDown();
        int total = 0;
        for (Future<Integer> f : fs) total += f.get();
        pool.shutdown();
        check("2,000 runs executed in total", 2000, total);
        check("each job exactly once", true, perJob.size() == 2000 && perJob.values().stream().allMatch(a -> a.get() == 1));
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
