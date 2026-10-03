/*
 * ============================================================================
 *  HLD 08 : CHAT SYSTEM AT SCALE (WhatsApp / Messenger)  - simulated
 * ============================================================================
 *  Run:  java ChatHld.java          (the full design answer: README.md)
 *
 *     [gateways]   users hold a WebSocket to ONE of many gateway servers; a session REGISTRY
 *                  (userId -> gateway) lets the chat service route a message to the right box
 *     [delivery]   online -> push through the gateway; offline -> stored + mobile push notification;
 *                  reconnect -> sync from the last seq (nothing lost)
 *     [failure]    a gateway crashes: its users reconnect elsewhere; messages sent meanwhile are
 *                  stored and synced, none lost, none duplicated
 *     [groups]     fan-out cost: deliveries per message grow with group size -> cap on push
 *                  fan-out, pull for huge groups/channels
 *     [storage]    partition = (conversation, month bucket) keeps partitions bounded
 *                  (Cassandra/HBase wide rows must not grow forever)
 *     [presence]   heartbeat + TTL instead of "online/offline" events to every friend
 *  (The per-conversation seq, cursors, receipts and client hold-back buffer: LLD/14-Chat-Application.)
 * ============================================================================
 */

import java.util.*;

public class ChatHld {

    record Msg(String conv, long seq, String from, String text) {}

    // =====================================================================
    // [gateways] + [delivery]
    // =====================================================================

    static final class Gateway {
        final String id;
        boolean alive = true;
        final Map<String, List<Msg>> sockets = new HashMap<>();             // userId -> messages pushed over its socket
        Gateway(String id) { this.id = id; }
    }

    static final class ChatCluster {
        final List<Gateway> gateways = new ArrayList<>();
        final Map<String, String> registry = new HashMap<>();               // userId -> gatewayId (Redis in production)
        final Map<String, List<Msg>> store = new HashMap<>();               // conv -> messages (Cassandra)
        final Map<String, Long> seqs = new HashMap<>();
        final Map<String, Set<String>> members = new HashMap<>();
        final Map<String, Long> cursor = new HashMap<>();                   // "user|conv" -> last delivered seq
        long pushNotifications, socketDeliveries;

        ChatCluster(int n) { for (int i = 0; i < n; i++) gateways.add(new Gateway("gw" + i)); }
        Gateway gw(String id) { return gateways.stream().filter(g -> g.id.equals(id)).findFirst().get(); }

        /** Connect via the load balancer to some live gateway; register; then SYNC missed messages. */
        void connect(String user, int pick) {
            List<Gateway> live = gateways.stream().filter(g -> g.alive).toList();
            Gateway g = live.get(Math.floorMod(pick, live.size()));
            g.sockets.put(user, new ArrayList<>());
            registry.put(user, g.id);
            for (var e : members.entrySet()) if (e.getValue().contains(user)) sync(user, e.getKey());
        }

        void disconnect(String user) { String g = registry.remove(user); if (g != null) gw(g).sockets.remove(user); }

        void crash(String gatewayId) {
            Gateway g = gw(gatewayId);
            g.alive = false;
            registry.values().removeIf(v -> v.equals(gatewayId));           // heartbeats stop -> registry entries expire
            g.sockets.clear();
        }

        Msg send(String conv, String from, String text) {
            long seq = seqs.merge(conv, 1L, Long::sum);                      // per-conversation order
            Msg m = new Msg(conv, seq, from, text);
            store.computeIfAbsent(conv, k -> new ArrayList<>()).add(m);      // persist FIRST, then deliver
            cursor.put(from + "|" + conv, seq);
            for (String u : members.get(conv)) {
                if (u.equals(from)) continue;
                String gid = registry.get(u);
                if (gid != null && gw(gid).alive) deliver(u, gw(gid), m);
                else pushNotifications++;                                   // APNs / FCM "you have a message"
            }
            return m;
        }

        void deliver(String user, Gateway g, Msg m) {
            String key = user + "|" + m.conv();
            if (m.seq() <= cursor.getOrDefault(key, 0L)) return;             // already have it (dedupe)
            g.sockets.get(user).add(m);
            cursor.put(key, m.seq());
            socketDeliveries++;
        }

        void sync(String user, String conv) {
            Gateway g = gw(registry.get(user));
            long from = cursor.getOrDefault(user + "|" + conv, 0L);
            for (Msg m : store.getOrDefault(conv, List.of())) if (m.seq() > from) deliver(user, g, m);
        }

        List<Msg> received(String user) { String g = registry.get(user); return g == null ? List.of() : gw(g).sockets.get(user); }
    }

    // =====================================================================
    // [storage]
    // =====================================================================

    /** Partition key options for a chat that lives for 5 years at 200 messages/day. */
    static Map<String, Integer> partitionSizes(boolean bucketByMonth) {
        Map<String, Integer> sizes = new TreeMap<>();
        for (int day = 0; day < 5 * 365; day++) {
            String key = bucketByMonth ? "conv42|" + (2020 + day / 365) + "-" + String.format("%02d", (day % 365) / 31 + 1) : "conv42";
            sizes.merge(key, 200, Integer::sum);
        }
        return sizes;
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. Routing through gateways via the session registry");
        ChatCluster c = new ChatCluster(4);
        c.members.put("dm:a:b", new HashSet<>(Set.of("alice", "bob")));
        c.connect("alice", 0); c.connect("bob", 3);
        check("alice and bob on different gateways", "gw0/gw3", c.registry.get("alice") + "/" + c.registry.get("bob"));
        c.send("dm:a:b", "alice", "hi bob");
        check("delivered over bob's socket on gw3", "[hi bob]", c.received("bob").stream().map(Msg::text).toList());

        System.out.println("2. Offline user: stored + push notification, synced on reconnect");
        c.disconnect("bob");
        c.send("dm:a:b", "alice", "call me");
        c.send("dm:a:b", "alice", "urgent!");
        check("2 push notifications sent", 2L, c.pushNotifications);
        c.connect("bob", 1);
        check("reconnected bob synced both, in order", "[call me, urgent!]", c.received("bob").stream().map(Msg::text).toList());

        System.out.println("3. Gateway crash: no loss, no duplicates");
        ChatCluster big = new ChatCluster(10);
        Random rnd = new Random(1);
        List<String> users = new ArrayList<>();
        for (int i = 0; i < 200; i++) users.add("u" + i);
        for (int i = 0; i < 200; i += 2) big.members.put("dm" + i, new HashSet<>(Set.of(users.get(i), users.get(i + 1))));
        for (String u : users) big.connect(u, rnd.nextInt(10));
        for (int i = 0; i < 1000; i++) { int p = rnd.nextInt(100) * 2; big.send("dm" + p, users.get(p + rnd.nextInt(2)), "m" + i); }
        big.crash("gw3");
        List<String> orphaned = users.stream().filter(u -> !big.registry.containsKey(u)).toList();
        for (int i = 0; i < 1000; i++) { int p = rnd.nextInt(100) * 2; big.send("dm" + p, users.get(p + rnd.nextInt(2)), "x" + i); }   // while they're away
        for (String u : orphaned) big.connect(u, rnd.nextInt(9));          // clients reconnect to surviving gateways
        boolean complete = true;
        for (String u : users) {
            String conv = big.members.entrySet().stream().filter(e -> e.getValue().contains(u)).findFirst().get().getKey();
            long expected = big.store.get(conv) == null ? 0 : big.store.get(conv).stream().filter(m -> !m.from().equals(u)).count();
            if (orphaned.contains(u)) {
                // their socket history restarted on the new gateway: everything they got = synced after reconnect
                List<Msg> got = big.received(u);
                Set<Long> seqs = new HashSet<>();
                for (Msg m : got) if (!seqs.add(m.seq())) complete = false;      // duplicates?
            }
            long cur = big.cursor.getOrDefault(u + "|" + conv, 0L);
            if (cur != big.seqs.getOrDefault(conv, 0L) && expected > 0) complete = false;   // every user caught up to the last seq
        }
        System.out.println("   " + orphaned.size() + " users lost their gateway and reconnected");
        check("every user caught up to the latest seq, no duplicates", true, complete);

        System.out.println("4. Group fan-out cost");
        for (int size : new int[]{2, 50, 1000, 100_000}) {
            double online = 0.3;
            System.out.printf("   group of %,7d: 1 message -> %,8d socket pushes + %,8d notifications%n", size, (long) ((size - 1) * online), (long) ((size - 1) * (1 - online)));
        }
        System.out.println("   -> cap push fan-out (e.g. 1,000 members); larger groups/channels: notify 'new activity', clients PULL");

        System.out.println("5. Storage partitioning for a 5-year conversation");
        Map<String, Integer> one = partitionSizes(false), monthly = partitionSizes(true);
        check("single partition: 365,000 rows in ONE wide row", 365_000, one.get("conv42"));
        check("monthly buckets: each partition ~6K rows", true, monthly.values().stream().allMatch(n -> n <= 6_400));
        System.out.println("   " + monthly.size() + " partitions; 'load older' pages walk buckets backwards");

        System.out.println("6. Presence: heartbeats, not broadcasts");
        long friends = 300, users2 = 100_000_000L, changesPerDay = 20;
        long broadcastEvents = users2 * changesPerDay * friends;
        System.out.printf("   broadcasting every online/offline change to all friends: %,d events/day (~%,d/s)%n", broadcastEvents, broadcastEvents / 86_400);
        check("too expensive -> fetch presence lazily for the open chat / visible contacts", true, broadcastEvents / 86_400 > 1_000_000);
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
