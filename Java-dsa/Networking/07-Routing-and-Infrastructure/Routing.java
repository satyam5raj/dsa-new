/*
 * ============================================================================
 *  NETWORKING 07 : ROUTING AND NETWORK INFRASTRUCTURE
 * ============================================================================
 *  Run:  java Routing.java          (explanations: README.md in this folder)
 *
 *    1. routing table        longest-prefix match (binary trie), default route
 *    2. switches + VLANs     MAC learning, flooding, VLAN isolation
 *    3. OSPF (link-state)    Dijkstra shortest paths; reconverge after a link failure
 *    4. BGP (path-vector)    AS-path selection, loop prevention, a more-specific-prefix hijack
 *    5. firewalls            stateless ACL vs stateful connection tracking
 *    6. proxies              forward vs reverse, X-Forwarded-For and who to trust
 *    7. L4 vs L7 balancing   5-tuple hashing vs path/header routing, sticky sessions
 *    8. health checks        shallow vs deep; "healthy" but can't serve
 *    9. anycast + CDN        nearest PoP; cache hit ratio and origin offload
 *   10. VPC debugging        "works inside the VPC, not from the Internet"
 * ============================================================================
 */

import java.util.*;

public class Routing {

    static long ip(String s) { long v = 0; for (String p : s.split("\\.")) v = (v << 8) | Integer.parseInt(p); return v; }

    // ---------------------------------------------------------------------
    // 1. longest-prefix match with a binary trie
    // ---------------------------------------------------------------------
    static class Trie {
        Trie zero, one;
        String nextHop;

        void insert(String cidr, String hop) {
            String[] p = cidr.split("/");
            long addr = ip(p[0]);
            int len = Integer.parseInt(p[1]);
            Trie n = this;
            for (int i = 31; i >= 32 - len; i--) {
                boolean bit = ((addr >> i) & 1) == 1;
                if (bit) { if (n.one == null) n.one = new Trie(); n = n.one; }
                else { if (n.zero == null) n.zero = new Trie(); n = n.zero; }
            }
            n.nextHop = hop;
        }

        String lookup(String dst) {
            long a = ip(dst);
            Trie n = this;
            String best = nextHop;                                    // /0 default route lives at the root
            for (int i = 31; i >= 0 && n != null; i--) {
                n = ((a >> i) & 1) == 1 ? n.one : n.zero;
                if (n != null && n.nextHop != null) best = n.nextHop;   // remember the most specific match so far
            }
            return best;
        }
    }

    static void routingTable() {
        section("1. Routing table: longest-prefix match");
        Trie t = new Trie();
        t.insert("0.0.0.0/0", "igw (default route -> Internet)");
        t.insert("10.0.0.0/16", "local (VPC)");
        t.insert("10.0.128.0/20", "pcx-db (peering to DB VPC)");
        t.insert("10.0.128.5/32", "eni-firewall (one host via inspection)");
        t.insert("192.168.0.0/16", "vgw (VPN to office)");
        check("10.0.5.9 -> /16 local", "local (VPC)", t.lookup("10.0.5.9"));
        check("10.0.130.1 matches /16 AND /20 -> the longer /20 wins", "pcx-db (peering to DB VPC)", t.lookup("10.0.130.1"));
        check("10.0.128.5 matches /16, /20 and /32 -> /32 wins", "eni-firewall (one host via inspection)", t.lookup("10.0.128.5"));
        check("8.8.8.8 matches only 0.0.0.0/0 -> default route", "igw (default route -> Internet)", t.lookup("8.8.8.8"));
        check("192.168.4.4 -> VPN", "vgw (VPN to office)", t.lookup("192.168.4.4"));
        Trie noDefault = new Trie();
        noDefault.insert("10.0.0.0/16", "local");
        check("no matching route and no default -> unreachable (ICMP net unreachable)", null, noDefault.lookup("8.8.8.8"));
    }

    // ---------------------------------------------------------------------
    // 2. learning switch + VLANs
    // ---------------------------------------------------------------------
    static void switching() {
        section("2. Layer-2 switch: MAC learning, flooding, VLANs");
        Map<String, Integer> macTable = new HashMap<>();
        Map<Integer, Integer> portVlan = Map.of(1, 10, 2, 10, 3, 10, 4, 20);   // ports 1-3 VLAN 10, port 4 VLAN 20
        List<String> log = new ArrayList<>();
        java.util.function.BiFunction<Integer, String[], List<Integer>> frame = (inPort, srcDst) -> {
            macTable.put(srcDst[0], inPort);                                     // learn: the source lives on this port
            int vlan = portVlan.get(inPort);
            Integer out = macTable.get(srcDst[1]);
            List<Integer> ports;
            if (out != null && portVlan.get(out) == vlan) ports = List.of(out);   // known: forward to one port
            else ports = portVlan.entrySet().stream().filter(e -> e.getValue() == vlan && e.getKey() != inPort).map(Map.Entry::getKey).sorted().toList(); // unknown: flood the VLAN
            log.add(srcDst[0] + "->" + srcDst[1] + " out " + ports);
            return ports;
        };
        List<Integer> first = frame.apply(1, new String[]{"A", "B"});
        List<Integer> reply = frame.apply(2, new String[]{"B", "A"});
        List<Integer> again = frame.apply(1, new String[]{"A", "B"});
        log.forEach(Routing::show);
        check("unknown destination -> flood to all other ports in the VLAN", "[2, 3]", first.toString());
        check("after learning, frames go to exactly one port", "[1]", reply.toString());
        check("...in both directions", "[2]", again.toString());
        check("flooding never crosses into another VLAN (port 4 is VLAN 20)", false, first.contains(4));
        check("a router (L3) is needed to go between VLAN 10 and VLAN 20", "[]", frame.apply(4, new String[]{"D", "A"}).toString());
    }

    // ---------------------------------------------------------------------
    // 3. OSPF-style link state: Dijkstra
    // ---------------------------------------------------------------------
    static Map<String, String> dijkstra(Map<String, Map<String, Integer>> g, String src, Map<String, Integer> distOut) {
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(Map.Entry.comparingByValue());
        dist.put(src, 0);
        pq.add(Map.entry(src, 0));
        while (!pq.isEmpty()) {
            var cur = pq.poll();
            if (cur.getValue() > dist.get(cur.getKey())) continue;
            for (var e : g.getOrDefault(cur.getKey(), Map.of()).entrySet()) {
                int nd = cur.getValue() + e.getValue();
                if (nd < dist.getOrDefault(e.getKey(), Integer.MAX_VALUE)) { dist.put(e.getKey(), nd); prev.put(e.getKey(), cur.getKey()); pq.add(Map.entry(e.getKey(), nd)); }
            }
        }
        distOut.putAll(dist);
        return prev;
    }

    static String path(Map<String, String> prev, String dst) {
        LinkedList<String> p = new LinkedList<>();
        for (String n = dst; n != null; n = prev.get(n)) p.addFirst(n);
        return String.join(">", p);
    }

    static void link(Map<String, Map<String, Integer>> g, String a, String b, int cost) {
        g.computeIfAbsent(a, k -> new HashMap<>()).put(b, cost);
        g.computeIfAbsent(b, k -> new HashMap<>()).put(a, cost);
    }

    static void ospf() {
        section("3. OSPF (link-state): every router runs Dijkstra on the same map");
        Map<String, Map<String, Integer>> g = new HashMap<>();
        link(g, "R1", "R2", 10); link(g, "R2", "R4", 10); link(g, "R1", "R3", 5); link(g, "R3", "R4", 30); link(g, "R2", "R3", 3);
        Map<String, Integer> d = new HashMap<>();
        Map<String, String> prev = dijkstra(g, "R1", d);
        check("cheapest R1 -> R4 path (cost = sum of link costs)", "R1>R3>R2>R4 cost 18", path(prev, "R4") + " cost " + d.get("R4"));
        g.get("R2").remove("R4"); g.get("R4").remove("R2");                  // link R2-R4 fails; LSAs flood; all recompute
        d.clear();
        prev = dijkstra(g, "R1", d);
        check("after the R2-R4 link fails the network reconverges", "R1>R3>R4 cost 35", path(prev, "R4") + " cost " + d.get("R4"));
    }

    // ---------------------------------------------------------------------
    // 4. BGP path-vector
    // ---------------------------------------------------------------------
    record Route(String prefix, List<Integer> asPath) { }

    static Optional<Route> bestBgp(int myAs, List<Route> received) {
        return received.stream()
            .filter(r -> !r.asPath().contains(myAs))                       // loop prevention: my own AS in the path -> reject
            .min(Comparator.comparingInt(r -> r.asPath().size()));         // (simplified) prefer the shortest AS path
    }

    static void bgp() {
        section("4. BGP (path-vector between autonomous systems)");
        List<Route> received = List.of(
            new Route("203.0.113.0/24", List.of(64500, 64510, 15169)),
            new Route("203.0.113.0/24", List.of(64501, 15169)),
            new Route("203.0.113.0/24", List.of(64502, 65001, 64999)));       // contains OUR AS (65001): a loop
        check("best path = shortest AS path", "[64501, 15169]", bestBgp(65001, received).get().asPath().toString());
        check("a route that already passed through us is rejected", 2, received.stream().filter(r -> !r.asPath().contains(65001)).count());
        Trie fib = new Trie();
        fib.insert("203.0.113.0/24", "legit origin AS15169");
        fib.insert("203.0.113.0/25", "hijacker AS666");                     // someone announces a MORE SPECIFIC prefix
        check("BGP hijack: a more-specific /25 wins longest-prefix match everywhere", "hijacker AS666", fib.lookup("203.0.113.10"));
        show("defences: RPKI route-origin validation, prefix filters, max-prefix limits, monitoring (BGPStream)");
    }

    // ---------------------------------------------------------------------
    // 5. stateless vs stateful firewall
    // ---------------------------------------------------------------------
    record Pkt(String src, int sport, String dst, int dport, String dir) { }

    static void firewalls() {
        section("5. Stateless ACL vs stateful firewall");
        // policy: our server 10.0.1.5 may call out to 93.184.216.34:443; nothing may connect in
        Pkt out = new Pkt("10.0.1.5", 51515, "93.184.216.34", 443, "out");
        Pkt reply = new Pkt("93.184.216.34", 443, "10.0.1.5", 51515, "in");
        Pkt attack = new Pkt("198.51.100.66", 4444, "10.0.1.5", 51999, "in");

        // stateless (e.g. AWS NACL): must open the whole ephemeral range inbound to let replies in
        java.util.function.Predicate<Pkt> stateless = p -> p.dir().equals("out") ? p.dport() == 443 : (p.dport() >= 1024 && p.dport() <= 65535);
        check("stateless: the reply is allowed only because 1024-65535 is open inbound", true, stateless.test(reply));
        check("...which also lets an unsolicited packet to a high port through", true, stateless.test(attack));

        Set<String> conntrack = new HashSet<>();                              // stateful (e.g. security groups, iptables -m state)
        java.util.function.Predicate<Pkt> stateful = p -> {
            if (p.dir().equals("out")) { if (p.dport() == 443) { conntrack.add(p.dst() + ":" + p.dport() + ">" + p.src() + ":" + p.sport()); return true; } return false; }
            return conntrack.contains(p.src() + ":" + p.sport() + ">" + p.dst() + ":" + p.dport());   // reply to a tracked flow?
        };
        check("stateful: outbound allowed and the flow is recorded", true, stateful.test(out));
        check("the reply matches the tracked connection -> allowed automatically", true, stateful.test(reply));
        check("the unsolicited packet matches nothing -> dropped", false, stateful.test(attack));
    }

    // ---------------------------------------------------------------------
    // 6. proxies and X-Forwarded-For
    // ---------------------------------------------------------------------
    static String clientIp(List<String> xff, String peer, Set<String> trustedProxies) {
        List<String> chain = new ArrayList<>(xff);
        chain.add(peer);                                                     // the TCP peer is the last hop
        for (int i = chain.size() - 1; i >= 0; i--) if (!trustedProxies.contains(chain.get(i))) return chain.get(i);
        return chain.get(0);
    }

    static void proxies() {
        section("6. Forward vs reverse proxies, X-Forwarded-For");
        Set<String> trusted = Set.of("10.0.0.10", "10.0.0.20");               // our CDN edge -> our load balancer
        check("real client = right-most address that is NOT one of our proxies", "203.0.113.50",
              clientIp(List.of("203.0.113.50", "10.0.0.10"), "10.0.0.20", trusted));
        check("a client can FORGE the left-most XFF entry; trusting it would accept '1.2.3.4'", "203.0.113.50",
              clientIp(List.of("1.2.3.4", "203.0.113.50", "10.0.0.10"), "10.0.0.20", trusted));
        check("without a proxy the TCP peer address is the client", "198.51.100.7", clientIp(List.of(), "198.51.100.7", trusted));
    }

    // ---------------------------------------------------------------------
    // 7. L4 vs L7 load balancing
    // ---------------------------------------------------------------------
    static void l4l7() {
        section("7. L4 vs L7 load balancing");
        List<String> backends = List.of("b1", "b2", "b3");
        Map<String, Integer> perBackend = new TreeMap<>();
        boolean stable = true;
        for (int port = 40000; port < 40300; port++) {                       // 300 client connections
            String tuple = "198.51.100.7:" + port + "->10.0.1.5:443/TCP";
            String chosen = backends.get(Math.floorMod(tuple.hashCode() * 0x9E3779B1, backends.size()));
            for (int pkt = 0; pkt < 5; pkt++)                                // every packet of the connection hashes the same
                stable &= chosen.equals(backends.get(Math.floorMod(tuple.hashCode() * 0x9E3779B1, backends.size())));
            perBackend.merge(chosen, 1, Integer::sum);
        }
        show("300 connections hashed by 5-tuple: " + perBackend);
        check("L4: every packet of a connection goes to the same backend", true, stable);
        check("...and connections spread across all backends (each gets > 60 of 300)", true, perBackend.size() == 3 && perBackend.values().stream().allMatch(v -> v > 60));
        Map<String, String> l7Routes = new LinkedHashMap<>();
        l7Routes.put("/api/", "api-pool"); l7Routes.put("/static/", "cdn-origin-pool"); l7Routes.put("/", "web-pool");
        java.util.function.Function<String, String> l7 = path -> l7Routes.entrySet().stream().filter(e -> path.startsWith(e.getKey())).findFirst().get().getValue();
        check("L7 reads the HTTP request: /api/orders -> api-pool", "api-pool", l7.apply("/api/orders"));
        check("L7: /static/app.js -> cdn-origin-pool", "cdn-origin-pool", l7.apply("/static/app.js"));
        Map<String, String> sticky = new HashMap<>();
        String b = sticky.computeIfAbsent("session=abc123", k -> "b2");
        check("L7 sticky sessions by cookie: same cookie -> same backend", b, sticky.computeIfAbsent("session=abc123", k -> "b3"));
        show("L7 can also terminate TLS, retry, rewrite headers, rate limit, do canary splits -- at the cost of parsing every request");
    }

    // ---------------------------------------------------------------------
    // 8. health checks
    // ---------------------------------------------------------------------
    static void healthChecks() {
        section("8. Health checks and failover");
        boolean processUp = true, dbReachable = false, threadPoolFull = true;
        boolean shallow = processUp;                                         // GET /health returns 200 if the process answers
        boolean deep = processUp && dbReachable && !threadPoolFull;          // checks what real requests need
        check("shallow check passes while real requests fail (DB down, pool full)", "shallow=true deep=false", "shallow=" + shallow + " deep=" + deep);
        show("time to detect a dead backend = check interval x unhealthy threshold, e.g. 10 s x 3 = 30 s of errors");
        boolean sharedDbUp = false;                                          // all 3 instances depend on the same database
        List<Boolean> deepResults = new ArrayList<>(), shallowResults = new ArrayList<>();
        for (int inst = 0; inst < 3; inst++) { deepResults.add(sharedDbUp); shallowResults.add(true); }
        check("danger: a deep check on a SHARED dependency marks EVERY instance unhealthy at once", "[false, false, false]", deepResults.toString());
        check("...while shallow checks keep them in rotation (errors, but no total outage)", "[true, true, true]", shallowResults.toString());
        show("so: liveness = 'restart me?' (shallow); readiness = 'send me traffic?' (deps, warmed up); fail open if all are unhealthy");
    }

    // ---------------------------------------------------------------------
    // 9. anycast + CDN
    // ---------------------------------------------------------------------
    static void anycastCdn() {
        section("9. Anycast and CDNs");
        Map<String, Integer> rttFromMumbai = Map.of("mumbai-pop", 4, "singapore-pop", 38, "frankfurt-pop", 120);
        String nearest = rttFromMumbai.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey();
        check("anycast: the same IP is announced everywhere; BGP delivers you to the nearest PoP", "mumbai-pop", nearest);
        double hitRatio = 0.92, originRtt = 180, edgeRtt = 4;
        double avg = hitRatio * edgeRtt + (1 - hitRatio) * (edgeRtt + originRtt);
        show(String.format("92%% CDN hit ratio: average fetch %.1f ms instead of %.0f ms; origin serves only 8%% of requests", avg, originRtt));
        check("CDN cuts average latency ~10x and origin load 12.5x", true, originRtt / avg > 9 && 1 / (1 - hitRatio) > 12);
    }

    // ---------------------------------------------------------------------
    // 10. "works inside the VPC, not from the Internet"
    // ---------------------------------------------------------------------
    static void vpcDebug() {
        section("10. Works inside the VPC, inaccessible from the Internet");
        LinkedHashMap<String, Boolean> path = new LinkedHashMap<>();
        path.put("DNS resolves to the PUBLIC address / load balancer (not a private record)", true);
        path.put("public subnet route table has 0.0.0.0/0 -> Internet gateway", true);
        path.put("load balancer is internet-facing with a public IP", true);
        path.put("security group allows inbound 443 from 0.0.0.0/0", false);
        path.put("network ACL allows inbound 443 AND outbound ephemeral ports", true);
        path.put("targets healthy in the target group", true);
        path.put("app listens on 0.0.0.0, not 127.0.0.1", true);
        String first = path.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).findFirst().orElse("all ok");
        check("walk the path from outside in; first broken hop", "security group allows inbound 443 from 0.0.0.0/0", first);
        show("inside the VPC the SG allowed the VPC CIDR, so internal tests passed -- they never exercised the Internet path");
    }

    public static void main(String[] args) {
        routingTable();
        switching();
        ospf();
        bgp();
        firewalls();
        proxies();
        l4l7();
        healthChecks();
        anycastCdn();
        vpcDebug();
        summary();
    }

    // =====================================================================
    static int passed = 0, total = 0;
    static void section(String s) { System.out.println(s); }
    static void show(String s) { System.out.println("      " + s); }

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-62s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
