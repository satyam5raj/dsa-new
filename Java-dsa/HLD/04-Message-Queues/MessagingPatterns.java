/*
 * ============================================================================
 *  HLD 04 : MESSAGE QUEUES & LOGS - delivery semantics, ordering, groups, outbox
 * ============================================================================
 *  Run:  java MessagingPatterns.java          (concepts: README.md)
 *
 *     [log]        a mini Kafka: Topic = N append-only PARTITIONS; producer picks the partition
 *                  by KEY hash -> per-key ORDER; consumers track OFFSETS
 *     [groups]     consumer GROUPS: each group gets every message (pub-sub); inside a group,
 *                  partitions are split between members (competing consumers / point-to-point);
 *                  REBALANCE when members join/leave
 *     [semantics]  at-most-once (commit then process) vs at-least-once (process then commit),
 *                  measured with crashes; idempotent consumer -> effectively-once
 *     [outbox]     "write DB + publish event" as a dual write loses events on crash; the
 *                  TRANSACTIONAL OUTBOX doesn't
 *     [dlq]        retries with a limit + dead-letter topic so one poison message doesn't
 *                  block a partition forever
 *     [lag]        consumer lag and why partitions cap a group's parallelism
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class MessagingPatterns {

    // =====================================================================
    // [log]
    // =====================================================================

    record Message(String key, String value, int partition, long offset) {}

    static final class Topic {
        final String name;
        final List<List<Message>> partitions = new ArrayList<>();
        Topic(String name, int n) { this.name = name; for (int i = 0; i < n; i++) partitions.add(new ArrayList<>()); }

        /** Same key -> same partition -> consumed in production order. Null key -> round-robin. */
        private int rr;
        Message publish(String key, String value) {
            int p = key == null ? Math.floorMod(rr++, partitions.size()) : Math.floorMod(key.hashCode(), partitions.size());
            List<Message> log = partitions.get(p);
            Message m = new Message(key, value, p, log.size());
            log.add(m);
            return m;
        }
        long endOffset(int p) { return partitions.get(p).size(); }
    }

    // =====================================================================
    // [groups]
    // =====================================================================

    /** Committed offsets live per (group, partition) - in Kafka, the __consumer_offsets topic. */
    static final class ConsumerGroup {
        final String name;
        final Topic topic;
        final long[] committed;
        final List<String> members = new ArrayList<>();
        final Map<String, List<Integer>> assignment = new TreeMap<>();

        ConsumerGroup(String name, Topic topic) { this.name = name; this.topic = topic; this.committed = new long[topic.partitions.size()]; }

        void join(String member) { members.add(member); rebalance(); }
        void leave(String member) { members.remove(member); rebalance(); }

        /** Range assignment: partitions split evenly; extra members (beyond #partitions) sit idle. */
        void rebalance() {
            assignment.clear();
            for (String m : members) assignment.put(m, new ArrayList<>());
            for (int p = 0; p < topic.partitions.size(); p++)
                if (!members.isEmpty()) assignment.get(members.get(p % members.size())).add(p);
        }

        long lag() { long l = 0; for (int p = 0; p < committed.length; p++) l += topic.endOffset(p) - committed[p]; return l; }

        /** Poll up to `max` messages for one member, from its partitions, starting at the committed offsets. */
        List<Message> poll(String member, int max) {
            List<Message> out = new ArrayList<>();
            for (int p : assignment.getOrDefault(member, List.of())) {
                List<Message> log = topic.partitions.get(p);
                for (long o = committed[p]; o < log.size() && out.size() < max; o++) out.add(log.get((int) o));
            }
            return out;
        }

        void commit(Message m) { committed[m.partition()] = Math.max(committed[m.partition()], m.offset() + 1); }
    }

    // =====================================================================
    // [semantics]
    // =====================================================================

    enum CommitMode { BEFORE_PROCESSING, AFTER_PROCESSING }

    record Outcome(int processedTotal, int distinctProcessed, int lost) {}

    /**
     * One consumer processes the topic; it CRASHES with probability `crashRate` between the
     * commit and the processing (or vice versa). After a crash it restarts from the committed offset.
     *   commit BEFORE processing -> crash = message skipped    (AT-MOST-ONCE: loss, no dupes)
     *   commit AFTER processing  -> crash = message redone     (AT-LEAST-ONCE: dupes, no loss)
     */
    static Outcome run(Topic t, CommitMode mode, double crashRate, Random rnd, Set<String> sideEffects, boolean idempotent) {
        ConsumerGroup g = new ConsumerGroup("g-" + mode, t);
        g.join("c1");
        int processed = 0;
        Set<String> distinct = new HashSet<>();
        int guard = 0;
        while (g.lag() > 0 && guard++ < 1_000_000) {
            for (Message m : g.poll("c1", 1)) {
                boolean crash = rnd.nextDouble() < crashRate;
                if (mode == CommitMode.BEFORE_PROCESSING) {
                    g.commit(m);
                    if (crash) continue;                                         // died before doing the work
                    processed++; distinct.add(m.value()); apply(sideEffects, m, idempotent);
                } else {
                    processed++; distinct.add(m.value()); apply(sideEffects, m, idempotent);
                    if (crash) continue;                                         // died before committing -> redelivered
                    g.commit(m);
                }
            }
        }
        int total = t.partitions.stream().mapToInt(List::size).sum();
        return new Outcome(processed, distinct.size(), total - distinct.size());
    }

    /** The side effect, e.g. "credit loyalty points". Idempotent version: dedupe by message id. */
    static void apply(Set<String> effects, Message m, boolean idempotent) {
        String id = m.partition() + ":" + m.offset();
        if (idempotent) effects.add(id);                                         // a set: applying twice = once
        else effects.add(id + "#" + UUID.randomUUID());                          // non-idempotent: every apply counts
    }

    // =====================================================================
    // [outbox]
    // =====================================================================

    /** Order service: save the order AND tell the world. Crashes happen between steps. */
    static int[] dualWrite(int orders, double crashRate, Random rnd) {
        int saved = 0, published = 0;
        for (int i = 0; i < orders; i++) {
            saved++;                                                             // DB commit
            if (rnd.nextDouble() < crashRate) continue;                          // crash before publishing -> event lost forever
            published++;
        }
        return new int[]{saved, published};
    }

    /** Outbox: the order row and an outbox row commit in ONE DB transaction; a relay publishes outbox rows (retrying). */
    static int[] outbox(int orders, double crashRate, Random rnd) {
        int saved = 0, published = 0;
        Deque<Integer> outboxTable = new ArrayDeque<>();
        Set<Integer> publishedIds = new HashSet<>();
        for (int i = 0; i < orders; i++) { saved++; outboxTable.add(i); }        // atomic: both rows or neither
        while (!outboxTable.isEmpty()) {                                         // relay (poller or CDC)
            int id = outboxTable.peek();
            if (rnd.nextDouble() < crashRate) continue;                          // relay crashed: row still there, retry
            if (publishedIds.add(id)) published++;                               // may publish twice in reality -> consumers dedupe by id
            outboxTable.poll();                                                  // mark sent
        }
        return new int[]{saved, published};
    }

    // =====================================================================
    // [dlq]
    // =====================================================================

    static Map<String, Object> processWithDlq(Topic t, Predicate<Message> handler, int maxAttempts) {
        Topic dlq = new Topic(t.name + ".DLQ", 1);
        ConsumerGroup g = new ConsumerGroup("worker", t);
        g.join("w");
        int ok = 0, attempts = 0;
        while (g.lag() > 0) {
            for (Message m : g.poll("w", 1)) {
                boolean done = false;
                for (int a = 1; a <= maxAttempts && !done; a++) { attempts++; done = handler.test(m); }
                if (done) ok++; else dlq.publish(m.key(), m.value());            // park it, keep the partition moving
                g.commit(m);
            }
        }
        return Map.of("ok", ok, "dlq", dlq.partitions.get(0).size(), "attempts", attempts);
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. Partitioning by key keeps per-key order");
        Topic orders = new Topic("orders", 4);
        for (int i = 0; i < 100; i++) orders.publish("user" + (i % 10), "user" + (i % 10) + "-event" + (i / 10));
        boolean ordered = true;
        for (List<Message> p : orders.partitions) {
            Map<String, Integer> last = new HashMap<>();
            for (Message m : p) {
                int n = Integer.parseInt(m.value().substring(m.value().indexOf("event") + 5));
                Integer prev = last.put(m.key(), n);
                if (prev != null && prev + 1 != n) ordered = false;
            }
        }
        check("each user's events stay in order within its partition", true, ordered);
        check("one user always lands in one partition", 1L, orders.partitions.stream().filter(p -> p.stream().anyMatch(m -> m.key().equals("user3"))).count());

        System.out.println("2. Consumer groups: pub-sub between groups, work-sharing within a group");
        ConsumerGroup billing = new ConsumerGroup("billing", orders), email = new ConsumerGroup("email", orders);
        billing.join("b1"); billing.join("b2");
        email.join("e1");
        int billingGot = billing.poll("b1", 1000).size() + billing.poll("b2", 1000).size();
        check("each GROUP sees all 100 messages", "100/100", billingGot + "/" + email.poll("e1", 1000).size());
        check("b1 and b2 split the partitions", "{b1=[0, 2], b2=[1, 3]}", billing.assignment);
        billing.join("b3"); billing.join("b4"); billing.join("b5");
        check("5 members, 4 partitions -> one member idle", 1L, billing.assignment.values().stream().filter(List::isEmpty).count());
        billing.leave("b1");
        check("rebalance after b1 leaves", "{b2=[0], b3=[1], b4=[2], b5=[3]}", billing.assignment);

        System.out.println("3. Delivery semantics with 10% crashes (1,000 messages)");
        Topic t = new Topic("points", 1);
        for (int i = 0; i < 1000; i++) t.publish(null, "msg" + i);
        Outcome atMost = run(t, CommitMode.BEFORE_PROCESSING, 0.10, new Random(1), new HashSet<>(), false);
        Set<String> effectsNaive = new HashSet<>(), effectsIdem = new HashSet<>();
        Outcome atLeast = run(t, CommitMode.AFTER_PROCESSING, 0.10, new Random(1), effectsNaive, false);
        run(t, CommitMode.AFTER_PROCESSING, 0.10, new Random(1), effectsIdem, true);
        System.out.printf("   commit-then-process: %d lost, %d duplicates%n", atMost.lost(), atMost.processedTotal() - atMost.distinctProcessed());
        System.out.printf("   process-then-commit: %d lost, %d duplicates%n", atLeast.lost(), atLeast.processedTotal() - atLeast.distinctProcessed());
        check("at-most-once loses messages", true, atMost.lost() > 50);
        check("at-least-once loses nothing", 0, atLeast.lost());
        check("...but redelivers some", true, atLeast.processedTotal() > 1000);
        check("non-idempotent side effects are duplicated", true, effectsNaive.size() > 1000);
        check("idempotent consumer -> effectively once", 1000, effectsIdem.size());

        System.out.println("4. Transactional outbox vs dual write (10,000 orders, 1% crashes)");
        int[] dw = dualWrite(10_000, 0.01, new Random(2)), ob = outbox(10_000, 0.01, new Random(2));
        System.out.printf("   dual write: %d orders saved, %d events published%n", dw[0], dw[1]);
        check("dual write silently loses events", true, dw[1] < dw[0]);
        check("outbox publishes every saved order", ob[0], ob[1]);

        System.out.println("5. Retries + dead-letter queue: one poison message");
        Topic jobs = new Topic("jobs", 1);
        for (int i = 0; i < 50; i++) jobs.publish(null, i == 7 ? "POISON" : "job" + i);
        Map<String, Object> r = processWithDlq(jobs, m -> !m.value().equals("POISON"), 3);
        check("49 processed, poison parked in the DLQ after 3 tries", "49/1/52", r.get("ok") + "/" + r.get("dlq") + "/" + r.get("attempts"));

        System.out.println("6. Lag and parallelism");
        Topic events = new Topic("clicks", 3);
        for (int i = 0; i < 30_000; i++) events.publish("u" + i, "x");
        ConsumerGroup analytics = new ConsumerGroup("analytics", events);
        for (int i = 0; i < 6; i++) analytics.join("a" + i);
        long busy = analytics.assignment.values().stream().filter(l -> !l.isEmpty()).count();
        check("lag before consuming", 30_000L, analytics.lag());
        check("only 3 of 6 consumers can work (3 partitions)", 3L, busy);
        System.out.println("   -> choose partition count for PEAK consumer parallelism up front; re-partitioning breaks key order");
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
