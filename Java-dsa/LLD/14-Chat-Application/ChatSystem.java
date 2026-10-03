/*
 * ============================================================================
 *  LLD 14 : CHAT APPLICATION (WhatsApp-like)
 * ============================================================================
 *  Run:  java ChatSystem.java          (design notes: README.md)
 *
 *     [model]     User, Conversation (DIRECT / GROUP), Member (role), Message (server seq +
 *                 client message id), ReceiptState (SENT / DELIVERED / READ)
 *     [ordering]  per-conversation sequence numbers assigned by the server under the
 *                 conversation's lock -> ONE total order every member agrees on
 *     [dedupe]    clientMsgId: a retried send returns the SAME message (idempotent)
 *     [delivery]  online -> push to connected devices (Observer); offline -> stored, pulled on
 *                 reconnect via "give me everything after seq N" (cursor-based sync)
 *     [client]    ClientDevice: hold-back buffer that applies messages strictly in seq order,
 *                 even if pushes arrive out of order, and detects gaps
 *     [receipts]  per-member cursors (lastDelivered, lastRead) -> ticks computed from them
 *     [demo]      direct + group chats, retries, offline sync, receipts, permissions,
 *                 out-of-order delivery, 16 concurrent senders in one group
 *
 *  THE TWO HARD PROBLEMS
 *   1. ORDER: clocks on phones disagree, networks reorder packets. Never order by client time.
 *      The server stamps seq = 1, 2, 3 ... per conversation; clients display by seq.
 *   2. DELIVERY: phones disconnect constantly. Don't track "which messages did user X get" per
 *      message - track ONE CURSOR per (user, conversation): "delivered up to seq N".
 *      Sync = "send me seq > N". Receipts = compare cursors with the message's seq.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

public class ChatSystem {

    // =====================================================================
    // [model]
    // =====================================================================

    record User(String id, String name) {}

    enum ConversationType { DIRECT, GROUP }
    enum Role { ADMIN, MEMBER }
    enum Receipt { SENT, DELIVERED, READ }                    // single tick, double tick, blue ticks

    /** Immutable once stored. `seq` is the ONLY ordering truth. */
    record Message(String id, String conversationId, long seq, String senderId, String clientMsgId, String text, long serverTime, boolean deleted) {
        Message tombstone() { return new Message(id, conversationId, seq, senderId, clientMsgId, "", serverTime, true); }
    }

    /** Per-member delivery state: just two numbers, however many messages there are. */
    static class Member {
        final String userId;
        volatile Role role;
        final AtomicLong deliveredUpTo = new AtomicLong(), readUpTo = new AtomicLong();
        final long joinedAtSeq;                                 // new members don't see older history
        Member(String userId, Role role, long joinedAtSeq) { this.userId = userId; this.role = role; this.joinedAtSeq = joinedAtSeq; }
    }

    static class Conversation {
        final String id;
        final ConversationType type;
        final Map<String, Member> members = new ConcurrentHashMap<>();
        final List<Message> log = new ArrayList<>();            // index = seq - 1 ; guarded by lock
        final Map<String, Message> byClientId = new HashMap<>(); // dedupe ; guarded by lock
        final ReentrantLock lock = new ReentrantLock();
        Conversation(String id, ConversationType type) { this.id = id; this.type = type; }
        long lastSeq() { return log.size(); }
    }

    // =====================================================================
    // [delivery] what the server pushes to a connected device
    // =====================================================================

    @FunctionalInterface
    interface DeviceConnection { void push(Message m); }

    // =====================================================================
    // [service]
    // =====================================================================

    static class ChatException extends RuntimeException { ChatException(String m) { super(m); } }

    static class ChatService {
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
        private final Map<String, DeviceConnection> online = new ConcurrentHashMap<>();   // presence: userId -> connection
        private final LongSupplier clock;
        private final AtomicLong ids = new AtomicLong();

        ChatService(LongSupplier clock) { this.clock = clock; }

        void register(User u) { users.put(u.id(), u); }

        /** Direct chats get a deterministic id so A->B and B->A find the same conversation. */
        Conversation direct(String a, String b) {
            String id = "dm:" + (a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a);
            return conversations.computeIfAbsent(id, k -> {
                Conversation c = new Conversation(k, ConversationType.DIRECT);
                c.members.put(a, new Member(a, Role.MEMBER, 0));
                c.members.put(b, new Member(b, Role.MEMBER, 0));
                return c;
            });
        }

        Conversation group(String id, String creator, String... others) {
            Conversation c = new Conversation(id, ConversationType.GROUP);
            c.members.put(creator, new Member(creator, Role.ADMIN, 0));
            for (String o : others) c.members.put(o, new Member(o, Role.MEMBER, 0));
            if (conversations.putIfAbsent(id, c) != null) throw new ChatException("Group exists");
            return c;
        }

        void addMember(String convId, String byUser, String newUser) {
            Conversation c = conv(convId);
            c.lock.lock();
            try {
                requireAdmin(c, byUser);
                c.members.putIfAbsent(newUser, new Member(newUser, Role.MEMBER, c.lastSeq()));
            } finally { c.lock.unlock(); }
        }

        void removeMember(String convId, String byUser, String target) {
            Conversation c = conv(convId);
            c.lock.lock();
            try { requireAdmin(c, byUser); c.members.remove(target); } finally { c.lock.unlock(); }
        }

        private void requireAdmin(Conversation c, String u) {
            Member m = c.members.get(u);
            if (m == null || m.role != Role.ADMIN) throw new ChatException(u + " is not an admin");
        }

        /**
         * SEND: assign seq under the conversation lock (total order), dedupe by clientMsgId,
         * then fan out to ONLINE members outside the lock.
         */
        Message send(String convId, String sender, String clientMsgId, String text) {
            Conversation c = conv(convId);
            Message m;
            List<String> recipients;
            c.lock.lock();
            try {
                if (!c.members.containsKey(sender)) throw new ChatException(sender + " is not in " + convId);
                Message dup = c.byClientId.get(sender + "/" + clientMsgId);
                if (dup != null) return dup;                         // retry after a lost ack: same message, same seq
                m = new Message("M" + ids.incrementAndGet(), convId, c.lastSeq() + 1, sender, clientMsgId, text, clock.getAsLong(), false);
                c.log.add(m);
                c.byClientId.put(sender + "/" + clientMsgId, m);
                c.members.get(sender).deliveredUpTo.set(m.seq());   // the sender obviously has it
                c.members.get(sender).readUpTo.set(m.seq());
                recipients = new ArrayList<>(c.members.keySet());
                // push WHILE holding the lock = pushes leave in seq order; production hands off to
                // per-connection ordered queues instead of doing I/O under the lock
                for (String r : recipients) if (!r.equals(sender)) { DeviceConnection d = online.get(r); if (d != null) d.push(m); }
            } finally { c.lock.unlock(); }
            return m;
        }

        /** Delete for everyone: keep the seq slot (order must not shift), replace content with a tombstone. */
        void deleteForEveryone(String convId, String user, long seq) {
            Conversation c = conv(convId);
            c.lock.lock();
            try {
                Message m = c.log.get((int) seq - 1);
                if (!m.senderId().equals(user)) throw new ChatException("Only the sender can delete");
                c.log.set((int) seq - 1, m.tombstone());
            } finally { c.lock.unlock(); }
        }

        // ---- presence + sync ----

        void connect(String userId, DeviceConnection conn) { online.put(userId, conn); }
        void disconnect(String userId) { online.remove(userId); }

        /** Pull everything after `afterSeq` (what a reconnecting device does), respecting join time. */
        List<Message> sync(String convId, String userId, long afterSeq) {
            Conversation c = conv(convId);
            c.lock.lock();
            try {
                Member mem = c.members.get(userId);
                if (mem == null) throw new ChatException(userId + " is not a member");
                long from = Math.max(afterSeq, mem.joinedAtSeq);
                return List.copyOf(c.log.subList((int) Math.min(from, c.log.size()), c.log.size()));
            } finally { c.lock.unlock(); }
        }

        // ---- receipts: devices ack cursors, ticks are derived ----

        void ackDelivered(String convId, String userId, long upTo) { Member m = conv(convId).members.get(userId); if (m != null) m.deliveredUpTo.accumulateAndGet(upTo, Math::max); }
        void ackRead(String convId, String userId, long upTo) {
            Member m = conv(convId).members.get(userId);
            if (m == null) return;
            m.readUpTo.accumulateAndGet(upTo, Math::max);
            m.deliveredUpTo.accumulateAndGet(upTo, Math::max);      // read implies delivered
        }

        /** The sender's ticks: READ if every other member read it, DELIVERED if every one received it. */
        Receipt receipt(String convId, long seq) {
            Conversation c = conv(convId);
            String sender = c.log.get((int) seq - 1).senderId();
            boolean allRead = true, allDelivered = true;
            for (Member m : c.members.values()) {
                if (m.userId.equals(sender) || m.joinedAtSeq >= seq) continue;
                allRead &= m.readUpTo.get() >= seq;
                allDelivered &= m.deliveredUpTo.get() >= seq;
            }
            return allRead ? Receipt.READ : allDelivered ? Receipt.DELIVERED : Receipt.SENT;
        }

        long unread(String convId, String userId) { Conversation c = conv(convId); return c.lastSeq() - Math.max(c.members.get(userId).readUpTo.get(), c.members.get(userId).joinedAtSeq); }

        Conversation conv(String id) { Conversation c = conversations.get(id); if (c == null) throw new ChatException("No conversation " + id); return c; }
    }

    // =====================================================================
    // [client] ordering on the device
    // =====================================================================

    /**
     * Messages may arrive out of order (push + sync racing, retries). The device keeps a
     * HOLD-BACK buffer and applies seq N only after N-1. A gap that doesn't fill triggers a sync.
     */
    static class ClientDevice implements DeviceConnection {
        final String userId, convId;
        private final ChatService server;
        final List<Message> timeline = new ArrayList<>();      // what the UI shows, strictly ordered
        private final TreeMap<Long, Message> pending = new TreeMap<>();
        long applied = 0;
        int duplicatesIgnored = 0;

        ClientDevice(String userId, String convId, ChatService server) { this.userId = userId; this.convId = convId; this.server = server; }

        public synchronized void push(Message m) {
            if (!m.conversationId().equals(convId)) return;
            if (m.seq() <= applied || pending.containsKey(m.seq())) { duplicatesIgnored++; return; }
            pending.put(m.seq(), m);
            while (pending.containsKey(applied + 1)) timeline.add(pending.remove(++applied));   // drain in order
            server.ackDelivered(convId, userId, applied);
        }

        /** Called on reconnect or when a gap persists: fetch everything after what we have. */
        synchronized void syncFromServer() { for (Message m : server.sync(convId, userId, applied)) push(m); }
        synchronized boolean hasGap() { return !pending.isEmpty(); }
        synchronized void readAll() { server.ackRead(convId, userId, applied); }
        synchronized List<String> texts() { return timeline.stream().map(m -> m.deleted() ? "<deleted>" : m.text()).toList(); }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong();
        ChatService chat = new ChatService(clock::get);
        for (String u : List.of("alice", "bob", "carol", "dave")) chat.register(new User(u, u));

        System.out.println("1. Direct chat: ordering, idempotent retries");
        Conversation dm = chat.direct("alice", "bob");
        check("same conversation from both sides", dm.id, chat.direct("bob", "alice").id);
        ClientDevice bobPhone = new ClientDevice("bob", dm.id, chat);
        chat.connect("bob", bobPhone);
        Message m1 = chat.send(dm.id, "alice", "c-1", "hi");
        chat.send(dm.id, "alice", "c-2", "are you free?");
        Message retry = chat.send(dm.id, "alice", "c-1", "hi");                  // ack lost, app retries
        check("retry returns the same message", m1.id(), retry.id());
        check("bob sees each once, in order", "[hi, are you free?]", bobPhone.texts());
        check("seq is 1..2", 2L, dm.lastSeq());

        System.out.println("2. Receipts from cursors");
        check("delivered to bob (double tick)", Receipt.DELIVERED, chat.receipt(dm.id, 2));
        bobPhone.readAll();
        check("read by bob (blue ticks)", Receipt.READ, chat.receipt(dm.id, 2));

        System.out.println("3. Offline user syncs on reconnect");
        chat.disconnect("bob");
        chat.send(dm.id, "alice", "c-3", "call me");
        chat.send(dm.id, "alice", "c-4", "urgent");
        check("while offline: only SENT", Receipt.SENT, chat.receipt(dm.id, 4));
        check("bob has 2 unread", 2L, chat.unread(dm.id, "bob"));
        chat.connect("bob", bobPhone);
        bobPhone.syncFromServer();
        check("after reconnect bob caught up", "[hi, are you free?, call me, urgent]", bobPhone.texts());
        check("now DELIVERED", Receipt.DELIVERED, chat.receipt(dm.id, 4));

        System.out.println("4. Groups: fan-out, receipts need EVERY member, permissions");
        Conversation g = chat.group("trip", "alice", "bob", "carol");
        ClientDevice bobG = new ClientDevice("bob", "trip", chat), carolG = new ClientDevice("carol", "trip", chat);
        chat.connect("bob", m -> { bobG.push(m); bobPhone.push(m); });
        chat.connect("carol", carolG);
        chat.send("trip", "alice", "g-1", "goa?");
        chat.send("trip", "bob", "g-2", "yes!");
        check("carol got both", "[goa?, yes!]", carolG.texts());
        bobG.readAll();
        check("bob read, carol didn't -> DELIVERED", Receipt.DELIVERED, chat.receipt("trip", 1));
        carolG.readAll();
        check("everyone read -> READ", Receipt.READ, chat.receipt("trip", 1));
        expect("outsider can't post", () -> chat.send("trip", "dave", "x", "hello?"));
        expect("member can't add people", () -> chat.addMember("trip", "bob", "dave"));
        chat.addMember("trip", "alice", "dave");
        check("dave doesn't see history from before he joined", 0, chat.sync("trip", "dave", 0).size());
        chat.send("trip", "dave", "d-1", "hey all");
        check("dave sees his own and future messages", 1, chat.sync("trip", "dave", 0).size());
        chat.removeMember("trip", "alice", "carol");
        expect("removed member can't sync", () -> chat.sync("trip", "carol", 0));
        chat.deleteForEveryone("trip", "bob", 2);
        bobG.timeline.clear(); bobG.applied = 0; bobG.syncFromServer();
        check("delete keeps the slot, shows a tombstone", "[goa?, <deleted>, hey all]", bobG.texts());

        System.out.println("5. Out-of-order arrival: the client hold-back buffer");
        ChatService s2 = new ChatService(clock::get);
        for (String u : List.of("x", "y")) s2.register(new User(u, u));
        Conversation d2 = s2.direct("x", "y");
        List<Message> sent = new ArrayList<>();
        for (int i = 1; i <= 50; i++) sent.add(s2.send(d2.id, "x", "k" + i, "m" + i));
        ClientDevice y = new ClientDevice("y", d2.id, s2);
        List<Message> shuffled = new ArrayList<>(sent);
        shuffled.addAll(sent.subList(0, 10));                                      // some duplicates too
        Collections.shuffle(shuffled, new Random(3));
        shuffled.removeIf(m -> m.seq() == 25);                                     // one message lost in transit
        for (Message m : shuffled) y.push(m);
        check("stops at the gap (24 applied)", 24L, y.applied);
        check("gap detected", true, y.hasGap());
        y.syncFromServer();
        check("sync fills the gap, all 50 in order", true, y.texts().equals(sent.stream().map(Message::text).toList()));
        check("duplicates ignored", true, y.duplicatesIgnored >= 10);

        System.out.println("6. Concurrency: 16 members send 200 messages each into one group");
        ChatService s3 = new ChatService(clock::get);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 17; i++) { names.add("u" + i); s3.register(new User("u" + i, "U")); }
        s3.group("big", "u0", names.subList(1, 17).toArray(new String[0]));
        ClientDevice watcher = new ClientDevice("u0", "big", s3);
        s3.connect("u0", watcher);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch go = new CountDownLatch(1);
        for (int t = 1; t <= 16; t++) { String u = "u" + t; pool.submit(() -> { go.await(); for (int i = 0; i < 200; i++) s3.send("big", u, u + "-" + i, u + ":" + i); return null; }); }
        go.countDown();
        pool.shutdown(); pool.awaitTermination(20, TimeUnit.SECONDS);
        List<Message> all = s3.sync("big", "u0", 0);
        boolean contiguous = true;
        for (int i = 0; i < all.size(); i++) contiguous &= all.get(i).seq() == i + 1;
        check("3,200 messages, seq 1..3200 with no gaps", true, all.size() == 3200 && contiguous);
        check("the online watcher saw all of them in seq order", 3200L, watcher.applied);
        boolean perSenderOrder = true;
        Map<String, Integer> lastBySender = new HashMap<>();
        for (Message m : all) { int n = Integer.parseInt(m.text().split(":")[1]); Integer prev = lastBySender.put(m.senderId(), n); if (prev != null && prev + 1 != n) perSenderOrder = false; }
        check("each sender's messages keep their own order", true, perSenderOrder);
        summary();
    }

    static void expect(String label, Runnable r) {
        try { r.run(); check(label, "exception", "none"); }
        catch (ChatException e) { check(label, "exception", "exception"); }
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
