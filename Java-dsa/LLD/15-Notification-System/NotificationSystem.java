/*
 * ============================================================================
 *  LLD 15 : NOTIFICATION SYSTEM  (multi-channel: email / SMS / push)
 * ============================================================================
 *  Run:  java NotificationSystem.java          (design notes: README.md)
 *
 *     [events]     DomainEventBus (Observer): services publish "OrderShipped"; the
 *                  notification service SUBSCRIBES - producers know nothing about email/SMS
 *     [model]      Channel, Priority, Category, NotificationRequest, UserProfile + Preferences
 *     [templates]  TemplateEngine: "{{name}}" placeholders, per channel, missing params fail loudly
 *     [senders]    NotificationSender (Strategy per channel) built by a SenderRegistry (Factory)
 *     [decorators] RetryingSender, CircuitBreakerSender (+ fallback provider), RateLimitedSender,
 *                  AuditingSender - stacked like Java I/O streams (Decorator)
 *     [service]    NotificationService: dedupe -> preferences -> quiet hours -> render ->
 *                  send with channel fallback -> delivery log
 *     [demo]       preferences, quiet hours, templates, retries, circuit breaker, rate limit,
 *                  fallback, dedupe, and a concurrency test
 *
 *  WHY DECORATORS: "send an SMS" is simple; production needs retry + rate-limit + circuit
 *  breaker + audit + metrics around it. Subclasses (RetryingRateLimitedAuditedSmsSender...)
 *  explode combinatorially. Decorators wrap ANY sender in ANY combination, each one
 *  doing one job.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;

public class NotificationSystem {

    // =====================================================================
    // [events]
    // =====================================================================

    record DomainEvent(String type, String userId, Map<String, String> data, String eventId) {}

    static class DomainEventBus {
        private final Map<String, List<Consumer<DomainEvent>>> subs = new ConcurrentHashMap<>();
        void subscribe(String type, Consumer<DomainEvent> c) { subs.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(c); }
        void publish(DomainEvent e) { subs.getOrDefault(e.type(), List.of()).forEach(c -> c.accept(e)); }
    }

    // =====================================================================
    // [model]
    // =====================================================================

    enum Channel { PUSH, SMS, EMAIL }
    enum Priority { CRITICAL, HIGH, LOW }                         // CRITICAL: OTP, fraud; LOW: marketing
    enum Category { SECURITY, TRANSACTIONAL, MARKETING }

    record NotificationRequest(String idempotencyKey, String userId, String templateId, Map<String, String> params,
                               Category category, Priority priority, List<Channel> channels) {}

    record Preferences(Set<Channel> enabledChannels, Set<Category> mutedCategories, int quietFromHour, int quietToHour) {
        boolean inQuietHours(int hour) {
            return quietFromHour <= quietToHour ? hour >= quietFromHour && hour < quietToHour
                                                : hour >= quietFromHour || hour < quietToHour;   // e.g. 22 -> 7 wraps midnight
        }
    }

    record UserProfile(String id, String name, String email, String phone, String pushToken, Preferences prefs) {
        String address(Channel c) { return switch (c) { case EMAIL -> email; case SMS -> phone; case PUSH -> pushToken; }; }
    }

    record Message(Channel channel, String to, String body) {}

    enum DeliveryStatus { SENT, FAILED, SKIPPED_PREFERENCE, DEFERRED_QUIET_HOURS, DUPLICATE }

    record DeliveryRecord(String key, Channel channel, DeliveryStatus status, String detail) {}

    // =====================================================================
    // [templates]
    // =====================================================================

    static class TemplateEngine {
        private static final Pattern VAR = Pattern.compile("\\{\\{(\\w+)}}");
        private final Map<String, String> templates = new ConcurrentHashMap<>();    // "templateId:CHANNEL" -> text

        void register(String templateId, Channel c, String text) { templates.put(templateId + ":" + c, text); }
        boolean has(String templateId, Channel c) { return templates.containsKey(templateId + ":" + c); }

        String render(String templateId, Channel c, Map<String, String> params) {
            String t = templates.get(templateId + ":" + c);
            if (t == null) throw new IllegalArgumentException("No template " + templateId + " for " + c);
            Matcher m = VAR.matcher(t);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String v = params.get(m.group(1));
                if (v == null) throw new IllegalArgumentException("Missing param '" + m.group(1) + "' for " + templateId);   // never send "Hi {{name}}"
                m.appendReplacement(sb, Matcher.quoteReplacement(v));
            }
            m.appendTail(sb);
            return sb.toString();
        }
    }

    // =====================================================================
    // [senders] Strategy per channel / provider
    // =====================================================================

    static class SendException extends Exception { SendException(String m) { super(m); } }

    interface NotificationSender { void send(Message m) throws SendException; }

    /** A fake provider: can be told to fail the next N calls or be "down". */
    static class FakeProvider implements NotificationSender {
        final String name;
        final List<Message> delivered = new CopyOnWriteArrayList<>();
        final AtomicInteger failNext = new AtomicInteger();
        volatile boolean down;
        final AtomicInteger calls = new AtomicInteger();
        FakeProvider(String name) { this.name = name; }
        public void send(Message m) throws SendException {
            calls.incrementAndGet();
            if (down) throw new SendException(name + " is down");
            if (failNext.getAndUpdate(n -> Math.max(0, n - 1)) > 0) throw new SendException(name + " transient error");
            delivered.add(m);
        }
    }

    // =====================================================================
    // [decorators]
    // =====================================================================

    /** Retries transient failures with backoff (sleep is injected so tests don't wait). */
    static class RetryingSender implements NotificationSender {
        private final NotificationSender inner; private final int attempts; private final LongConsumer sleeper; private final long baseMs;
        RetryingSender(NotificationSender inner, int attempts, long baseMs, LongConsumer sleeper) { this.inner = inner; this.attempts = attempts; this.baseMs = baseMs; this.sleeper = sleeper; }
        public void send(Message m) throws SendException {
            SendException last = null;
            for (int i = 1; i <= attempts; i++) {
                try { inner.send(m); return; }
                catch (SendException e) { last = e; if (i < attempts) sleeper.accept(baseMs << (i - 1)); }
            }
            throw last;
        }
    }

    /**
     * CIRCUIT BREAKER: after `threshold` consecutive failures the circuit OPENS and calls fail
     * fast (no waiting on a dead provider) - or go to a FALLBACK provider. After `coolDownMs`
     * one trial call is let through (HALF_OPEN); success closes the circuit again.
     */
    static class CircuitBreakerSender implements NotificationSender {
        enum State { CLOSED, OPEN, HALF_OPEN }
        private final NotificationSender primary, fallback;
        private final int threshold; private final long coolDownMs; private final LongSupplier clock;
        private State state = State.CLOSED;
        private int failures;
        private long openedAt;
        CircuitBreakerSender(NotificationSender primary, NotificationSender fallback, int threshold, long coolDownMs, LongSupplier clock) {
            this.primary = primary; this.fallback = fallback; this.threshold = threshold; this.coolDownMs = coolDownMs; this.clock = clock;
        }
        public void send(Message m) throws SendException {
            boolean tryPrimary;
            synchronized (this) {
                if (state == State.OPEN && clock.getAsLong() - openedAt >= coolDownMs) state = State.HALF_OPEN;
                tryPrimary = state != State.OPEN;
            }
            if (tryPrimary) {
                try { primary.send(m); synchronized (this) { state = State.CLOSED; failures = 0; } return; }
                catch (SendException e) {
                    synchronized (this) {
                        failures++;
                        if (state == State.HALF_OPEN || failures >= threshold) { state = State.OPEN; openedAt = clock.getAsLong(); }
                    }
                    if (fallback == null) throw e;
                }
            }
            if (fallback == null) throw new SendException("circuit open");
            fallback.send(m);
        }
        synchronized State state() { return state; }
    }

    /** Caps messages per recipient per window (no one wants 50 marketing SMS). Fixed window, see LLD 04. */
    static class RateLimitedSender implements NotificationSender {
        private final NotificationSender inner; private final int limit; private final long windowMs; private final LongSupplier clock;
        private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();   // to -> {windowStart, count}
        RateLimitedSender(NotificationSender inner, int limit, long windowMs, LongSupplier clock) { this.inner = inner; this.limit = limit; this.windowMs = windowMs; this.clock = clock; }
        public void send(Message m) throws SendException {
            long now = clock.getAsLong(), window = now - now % windowMs;
            long[] c = counters.computeIfAbsent(m.to(), k -> new long[]{window, 0});
            synchronized (c) {
                if (c[0] != window) { c[0] = window; c[1] = 0; }
                if (c[1] >= limit) throw new SendException("rate limited for " + m.to());
                c[1]++;
            }
            inner.send(m);
        }
    }

    static class AuditingSender implements NotificationSender {
        private final NotificationSender inner; final List<String> audit;
        AuditingSender(NotificationSender inner, List<String> audit) { this.inner = inner; this.audit = audit; }
        public void send(Message m) throws SendException {
            try { inner.send(m); audit.add("OK " + m.channel() + " " + m.to()); }
            catch (SendException e) { audit.add("ERR " + m.channel() + " " + m.to() + " " + e.getMessage()); throw e; }
        }
    }

    /** FACTORY / registry: channel -> fully decorated sender. */
    static class SenderRegistry {
        private final Map<Channel, NotificationSender> senders = new EnumMap<>(Channel.class);
        SenderRegistry register(Channel c, NotificationSender s) { senders.put(c, s); return this; }
        NotificationSender get(Channel c) { NotificationSender s = senders.get(c); if (s == null) throw new IllegalStateException("No sender for " + c); return s; }
    }

    // =====================================================================
    // [service]
    // =====================================================================

    static class NotificationService {
        private final Map<String, UserProfile> users = new ConcurrentHashMap<>();
        private final TemplateEngine templates;
        private final SenderRegistry senders;
        private final IntSupplier hourOfDay;                    // injected clock (user's local hour)
        private final Set<String> seenKeys = ConcurrentHashMap.newKeySet();
        final List<DeliveryRecord> log = new CopyOnWriteArrayList<>();
        final Queue<NotificationRequest> deferred = new ConcurrentLinkedQueue<>();

        NotificationService(TemplateEngine t, SenderRegistry s, IntSupplier hourOfDay) { templates = t; senders = s; this.hourOfDay = hourOfDay; }
        void addUser(UserProfile u) { users.put(u.id(), u); }

        /** Glue to the event bus: domain event -> notification request. */
        void subscribeTo(DomainEventBus bus, String eventType, String templateId, Category cat, Priority p, List<Channel> channels) {
            bus.subscribe(eventType, e -> notify(new NotificationRequest(e.eventId(), e.userId(), templateId, e.data(), cat, p, channels)));
        }

        /**
         * Pipeline: dedupe -> preferences -> quiet hours -> for each channel in order: render + send;
         * stop at the first success when the request lists FALLBACK channels (push, then SMS ...).
         */
        void notify(NotificationRequest r) {
            if (!seenKeys.add(r.idempotencyKey())) { log.add(new DeliveryRecord(r.idempotencyKey(), null, DeliveryStatus.DUPLICATE, "")); return; }
            UserProfile u = users.get(r.userId());
            if (u == null) throw new IllegalArgumentException("Unknown user " + r.userId());
            Preferences p = u.prefs();
            if (r.category() != Category.SECURITY && p.mutedCategories().contains(r.category())) {     // SECURITY can't be muted
                log.add(new DeliveryRecord(r.idempotencyKey(), null, DeliveryStatus.SKIPPED_PREFERENCE, "category muted"));
                return;
            }
            if (r.priority() == Priority.LOW && p.inQuietHours(hourOfDay.getAsInt())) {
                seenKeys.remove(r.idempotencyKey());          // allow the deferred replay through dedupe
                deferred.add(r);
                log.add(new DeliveryRecord(r.idempotencyKey(), null, DeliveryStatus.DEFERRED_QUIET_HOURS, ""));
                return;
            }
            Map<String, String> params = new HashMap<>(r.params());
            params.putIfAbsent("name", u.name());
            for (Channel c : r.channels()) {
                boolean allowed = r.category() == Category.SECURITY || p.enabledChannels().contains(c);
                if (!allowed || u.address(c) == null || !templates.has(r.templateId(), c)) {
                    log.add(new DeliveryRecord(r.idempotencyKey(), c, DeliveryStatus.SKIPPED_PREFERENCE, !allowed ? "channel off" : "no address/template"));
                    continue;
                }
                try {
                    senders.get(c).send(new Message(c, u.address(c), templates.render(r.templateId(), c, params)));
                    log.add(new DeliveryRecord(r.idempotencyKey(), c, DeliveryStatus.SENT, ""));
                    return;                                    // first successful channel wins (fallback order)
                } catch (SendException e) {
                    log.add(new DeliveryRecord(r.idempotencyKey(), c, DeliveryStatus.FAILED, e.getMessage()));
                }
            }
        }

        /** Called by a scheduler when quiet hours end. */
        int flushDeferred() { int n = 0; NotificationRequest r; while ((r = deferred.poll()) != null) { notify(r); n++; } return n; }

        List<String> statuses(String key) { return log.stream().filter(d -> d.key().equals(key)).map(d -> (d.channel() == null ? "-" : d.channel()) + ":" + d.status()).toList(); }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong();
        AtomicInteger hour = new AtomicInteger(12);
        List<Long> sleeps = new CopyOnWriteArrayList<>();
        List<String> audit = new CopyOnWriteArrayList<>();

        FakeProvider push = new FakeProvider("fcm"), sms = new FakeProvider("twilio"), smsBackup = new FakeProvider("msg91"), email = new FakeProvider("ses");
        CircuitBreakerSender smsBreaker = new CircuitBreakerSender(new RetryingSender(sms, 2, 100, sleeps::add), smsBackup, 3, 60_000, clock::get);
        SenderRegistry registry = new SenderRegistry()
                .register(Channel.PUSH, new AuditingSender(push, audit))
                .register(Channel.SMS, new AuditingSender(new RateLimitedSender(smsBreaker, 3, 3_600_000, clock::get), audit))
                .register(Channel.EMAIL, new AuditingSender(new RetryingSender(email, 3, 200, sleeps::add), audit));

        TemplateEngine t = new TemplateEngine();
        t.register("shipped", Channel.PUSH, "Hi {{name}}, order {{orderId}} is on the way");
        t.register("shipped", Channel.SMS, "Order {{orderId}} shipped. Track: {{link}}");
        t.register("shipped", Channel.EMAIL, "Dear {{name}}, your order {{orderId}} has shipped.");
        t.register("otp", Channel.SMS, "Your OTP is {{code}}");
        t.register("sale", Channel.EMAIL, "{{name}}, 50% off this weekend!");
        t.register("sale", Channel.SMS, "50% off this weekend!");

        NotificationService svc = new NotificationService(t, registry, hour::get);
        svc.addUser(new UserProfile("asha", "Asha", "asha@x.com", "+911", "tok-asha", new Preferences(EnumSet.allOf(Channel.class), EnumSet.noneOf(Category.class), 22, 7)));
        svc.addUser(new UserProfile("ravi", "Ravi", "ravi@x.com", "+912", null, new Preferences(EnumSet.of(Channel.EMAIL, Channel.SMS), EnumSet.of(Category.MARKETING), 23, 6)));

        System.out.println("1. Event bus -> notification (Observer); push first");
        DomainEventBus bus = new DomainEventBus();
        svc.subscribeTo(bus, "OrderShipped", "shipped", Category.TRANSACTIONAL, Priority.HIGH, List.of(Channel.PUSH, Channel.SMS, Channel.EMAIL));
        bus.publish(new DomainEvent("OrderShipped", "asha", Map.of("orderId", "A-17", "link", "x.co/1"), "evt-1"));
        check("sent once via push", "[PUSH:SENT]", svc.statuses("evt-1"));
        check("rendered with profile name", "Hi Asha, order A-17 is on the way", push.delivered.get(0).body());

        System.out.println("2. Channel fallback: ravi has no push token -> next channel");
        bus.publish(new DomainEvent("OrderShipped", "ravi", Map.of("orderId", "R-9", "link", "x.co/2"), "evt-2"));
        check("push skipped, SMS sent", "[PUSH:SKIPPED_PREFERENCE, SMS:SENT]", svc.statuses("evt-2"));

        System.out.println("3. Dedupe: the same event delivered twice by the bus");
        bus.publish(new DomainEvent("OrderShipped", "asha", Map.of("orderId", "A-17", "link", "x.co/1"), "evt-1"));
        check("second one marked DUPLICATE", "[PUSH:SENT, -:DUPLICATE]", svc.statuses("evt-1"));
        check("push delivered only once", 1, push.delivered.size());

        System.out.println("4. Preferences and quiet hours");
        svc.notify(new NotificationRequest("m-1", "ravi", "sale", Map.of(), Category.MARKETING, Priority.LOW, List.of(Channel.EMAIL)));
        check("ravi muted marketing", "[-:SKIPPED_PREFERENCE]", svc.statuses("m-1"));
        hour.set(23);
        svc.notify(new NotificationRequest("m-2", "asha", "sale", Map.of(), Category.MARKETING, Priority.LOW, List.of(Channel.EMAIL)));
        check("LOW at 23:00 deferred", "[-:DEFERRED_QUIET_HOURS]", svc.statuses("m-2"));
        svc.notify(new NotificationRequest("otp-1", "ravi", "otp", Map.of("code", "4821"), Category.SECURITY, Priority.CRITICAL, List.of(Channel.SMS)));
        check("CRITICAL OTP ignores quiet hours", "[SMS:SENT]", svc.statuses("otp-1"));
        hour.set(8);
        check("flushed after quiet hours", 1, svc.flushDeferred());
        check("deferred one now sent", "[-:DEFERRED_QUIET_HOURS, EMAIL:SENT]", svc.statuses("m-2"));

        System.out.println("5. Templates fail loudly on missing params");
        expect("missing {{orderId}}", () -> t.render("shipped", Channel.PUSH, Map.of("name", "X")));

        System.out.println("6. Retry decorator absorbs transient failures");
        email.failNext.set(2);
        svc.notify(new NotificationRequest("e-1", "asha", "shipped", Map.of("orderId", "A-18"), Category.TRANSACTIONAL, Priority.HIGH, List.of(Channel.EMAIL)));
        check("sent on the 3rd attempt", "[EMAIL:SENT]", svc.statuses("e-1"));
        check("backoff 200ms, 400ms", "[200, 400]", sleeps.subList(sleeps.size() - 2, sleeps.size()));

        System.out.println("7. Circuit breaker opens, traffic goes to the backup SMS provider");
        sms.down = true;
        for (int i = 1; i <= 8; i++)                                              // distinct numbers: keep the rate limiter out of this test
            svc.addUser(new UserProfile("u" + i, "U" + i, null, "+" + i, null, new Preferences(EnumSet.allOf(Channel.class), EnumSet.noneOf(Category.class), 0, 0)));
        for (int i = 0; i < 6; i++) svc.notify(new NotificationRequest("cb-" + i, "u" + (1 + i), "otp", Map.of("code", "" + i), Category.SECURITY, Priority.CRITICAL, List.of(Channel.SMS)));
        check("circuit OPEN after 3 failures", CircuitBreakerSender.State.OPEN, smsBreaker.state());
        check("all 6 OTPs delivered via the backup", 6, smsBackup.delivered.size());
        int callsWhileOpen = sms.calls.get();
        svc.notify(new NotificationRequest("cb-x", "u7", "otp", Map.of("code", "9"), Category.SECURITY, Priority.CRITICAL, List.of(Channel.SMS)));
        check("open circuit doesn't even call the dead provider", callsWhileOpen, sms.calls.get());
        sms.down = false;
        clock.addAndGet(60_000);
        svc.notify(new NotificationRequest("cb-y", "u8", "otp", Map.of("code", "8"), Category.SECURITY, Priority.CRITICAL, List.of(Channel.SMS)));
        check("half-open trial succeeded -> CLOSED", CircuitBreakerSender.State.CLOSED, smsBreaker.state());

        System.out.println("8. Rate limit: max 3 SMS per number per hour");
        clock.set(10 * 3_600_000L);
        for (int i = 0; i < 5; i++) svc.notify(new NotificationRequest("rl-" + i, "u3", "sale", Map.of(), Category.MARKETING, Priority.HIGH, List.of(Channel.SMS)));
        check("3 sent, 2 rate-limited", 3L, svc.log.stream().filter(d -> d.key().startsWith("rl-") && d.status() == DeliveryStatus.SENT).count());

        System.out.println("9. Concurrency: 8 threads, 4,000 requests, every key repeated");
        FakeProvider p2 = new FakeProvider("p2");
        NotificationService c = new NotificationService(t, new SenderRegistry().register(Channel.PUSH, p2), () -> 12);
        c.addUser(new UserProfile("z", "Z", null, null, "tok", new Preferences(EnumSet.allOf(Channel.class), EnumSet.noneOf(Category.class), 0, 0)));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        for (int th = 0; th < 8; th++) pool.submit(() -> { go.await(); for (int i = 0; i < 500; i++) c.notify(new NotificationRequest("k" + (i % 250) + "-" + (Thread.currentThread().threadId() % 4), "z", "shipped", Map.of("orderId", "o"), Category.TRANSACTIONAL, Priority.HIGH, List.of(Channel.PUSH))); return null; });
        go.countDown();
        pool.shutdown(); pool.awaitTermination(10, TimeUnit.SECONDS);
        long distinctKeys = c.log.stream().map(DeliveryRecord::key).distinct().count();
        check("each idempotency key delivered exactly once", distinctKeys, (long) p2.delivered.size());
        summary();
    }

    static void expect(String label, Runnable r) {
        try { r.run(); check(label, "exception", "none"); }
        catch (RuntimeException e) { check(label, "exception", "exception"); }
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
