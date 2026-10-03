/*
 * ============================================================================
 *  HLD 12 : NOTIFICATION SYSTEM AT SCALE  - simulated
 * ============================================================================
 *  Run:  java NotificationHld.java          (the full design answer: README.md)
 *
 *     [queues]     a 1M-message marketing blast + steady OTP traffic through a provider limited
 *                  to 2,000 msg/s:
 *                    ONE FIFO queue      -> OTPs wait behind the blast (minutes!)
 *                    PRIORITY queues     -> OTP latency stays ~1 s, marketing drains later
 *     [campaign]   fanning a campaign out to 10M users: segment -> chunks -> workers, bounded
 *                  by provider rate limits -> how long it takes, and why chunking matters
 *     [caps]       per-user frequency caps (no more than N marketing pushes/day)
 *     [failover]   provider outage mid-campaign: circuit breaker + secondary provider
 *  (Service-level design - preferences, templates, retry/circuit-breaker decorators, dedupe:
 *   LLD/15-Notification-System.)
 * ============================================================================
 */

import java.util.*;

public class NotificationHld {

    record Note(long id, boolean otp, long enqueuedAt) {}

    record LatencyStats(double otpP99, double otpMax, long marketingDoneAt) {}

    // =====================================================================
    // [queues]
    // =====================================================================

    /** Discrete seconds. Each second: arrivals, then the provider sends up to `capacity` messages. */
    static LatencyStats simulate(boolean priority, int seconds, int capacity, int otpPerSecond, int blast) {
        Deque<Note> fifo = new ArrayDeque<>(), otpQ = new ArrayDeque<>(), mktQ = new ArrayDeque<>();
        List<Long> otpLat = new ArrayList<>();
        long id = 0, mktDone = -1, mktSent = 0;
        for (long t = 0; t < seconds; t++) {
            if (t == 0) for (int i = 0; i < blast; i++) { Note n = new Note(++id, false, 0); if (priority) mktQ.add(n); else fifo.add(n); }
            for (int i = 0; i < otpPerSecond; i++) { Note n = new Note(++id, true, t); if (priority) otpQ.add(n); else fifo.add(n); }
            for (int sent = 0; sent < capacity; sent++) {
                Note n = priority ? (!otpQ.isEmpty() ? otpQ.poll() : mktQ.poll()) : fifo.poll();
                if (n == null) break;
                if (n.otp()) otpLat.add(t - n.enqueuedAt() + 1); else if (++mktSent == blast) mktDone = t;
            }
        }
        Collections.sort(otpLat);
        return new LatencyStats(otpLat.isEmpty() ? -1 : otpLat.get((int) (otpLat.size() * 0.99)), otpLat.isEmpty() ? -1 : otpLat.get(otpLat.size() - 1), mktDone);
    }

    // =====================================================================
    // [campaign]
    // =====================================================================

    /** Time to deliver a campaign: limited by min(worker throughput, provider rate limit). */
    static double campaignSeconds(long users, int chunkSize, int workers, int perWorkerPerSec, int providerLimitPerSec) {
        long chunks = (users + chunkSize - 1) / chunkSize;
        double throughput = Math.min((double) workers * perWorkerPerSec, providerLimitPerSec);
        double fanOutPlanning = chunks * 0.001;                              // 1 ms to enqueue each chunk job
        return fanOutPlanning + users / throughput;
    }

    // =====================================================================
    // [caps]
    // =====================================================================

    static final class FrequencyCap {
        private final int maxPerDay;
        private final Map<String, Integer> counts = new HashMap<>();        // "user|day" -> sent (Redis INCR + EXPIRE)
        FrequencyCap(int maxPerDay) { this.maxPerDay = maxPerDay; }
        boolean allow(String user, long day) { return counts.merge(user + "|" + day, 1, Integer::sum) <= maxPerDay; }
    }

    // =====================================================================
    // [failover]
    // =====================================================================

    static final class Provider {
        final String name; boolean down; long sent;
        Provider(String name) { this.name = name; }
        boolean send() { if (down) return false; sent++; return true; }
    }

    static final class Breaker {
        final Provider primary, secondary; int consecutiveFailures; boolean open; long skipped;
        Breaker(Provider p, Provider s) { primary = p; secondary = s; }
        boolean send() {
            if (!open) {
                if (primary.send()) { consecutiveFailures = 0; return true; }
                if (++consecutiveFailures >= 5) open = true;                 // stop hammering the dead provider
            } else skipped++;
            return secondary.send();
        }
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. Marketing blast (1M) + OTPs (50/s) through a 2,000/s provider");
        LatencyStats fifo = simulate(false, 900, 2_000, 50, 1_000_000), prio = simulate(true, 900, 2_000, 50, 1_000_000);
        System.out.printf("   single FIFO : OTP p99 %.0f s (max %.0f s)%n", fifo.otpP99(), fifo.otpMax());
        System.out.printf("   priority    : OTP p99 %.0f s (max %.0f s); blast finished at t=%ds%n", prio.otpP99(), prio.otpMax(), prio.marketingDoneAt());
        check("FIFO: OTPs stuck behind the blast for minutes", true, fifo.otpP99() > 300);
        check("priority queues: OTPs delivered within 1 s", 1.0, prio.otpP99());
        check("the blast still completes (~1M / 1,950 per s)", true, prio.marketingDoneAt() > 500 && prio.marketingDoneAt() < 530);

        System.out.println("2. Campaign to 10M users");
        double tooFewWorkers = campaignSeconds(10_000_000, 1_000, 5, 1_000, 50_000);
        double enoughWorkers = campaignSeconds(10_000_000, 1_000, 60, 1_000, 50_000);
        System.out.printf("   5 workers: %.0f min; 60 workers: %.0f min (now capped by the 50K/s provider limit)%n", tooFewWorkers / 60, enoughWorkers / 60);
        check("scaling workers helps until the provider limit binds", true, enoughWorkers < tooFewWorkers / 5);
        check("provider-bound: 10M / 50K/s = 200 s", true, Math.abs(enoughWorkers - 210) < 15);

        System.out.println("3. Frequency caps: max 3 marketing pushes per user per day");
        FrequencyCap cap = new FrequencyCap(3);
        int allowed = 0;
        for (int campaign = 0; campaign < 7; campaign++) if (cap.allow("asha", 20_000)) allowed++;
        check("7 campaigns target asha today, 3 reach her", 3, allowed);
        check("tomorrow the cap resets", true, cap.allow("asha", 20_001));

        System.out.println("4. Provider outage mid-campaign");
        Provider fcm = new Provider("primary"), backup = new Provider("secondary");
        Breaker br = new Breaker(fcm, backup);
        int delivered = 0;
        for (int i = 0; i < 100_000; i++) {
            if (i == 40_000) fcm.down = true;                                // primary fails at 40%
            if (br.send()) delivered++;
        }
        check("every message delivered", 100_000, delivered);
        check("primary took 40K, secondary the rest", "40000/60000", fcm.sent + "/" + backup.sent);
        check("breaker opened: the dead provider was skipped ~60K times", true, br.skipped > 59_000);
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
