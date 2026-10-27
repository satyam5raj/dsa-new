/*
 * ============================================================================
 *  NETWORKING 05 : DNS  (a recursive resolver over a simulated hierarchy)
 * ============================================================================
 *  Run:  java Dns.java          (explanations: README.md in this folder)
 *
 *    1. the hierarchy          root -> TLD -> authoritative, iterative resolution by a recursive resolver
 *    2. caching and TTL        second lookup costs zero upstream queries; expiry; why changes "propagate" slowly
 *    3. record types           A, AAAA, CNAME chains, MX priority, TXT, NS, SRV
 *    4. negative caching       NXDOMAIN is cached too
 *    5. split-horizon          different answers inside and outside the network
 *    6. wire format            build and parse a real DNS query packet (no network needed)
 *    7. reverse DNS            in-addr.arpa names
 *    8. intermittent failures  one bad resolver in resolv.conf -> 5-second latency spikes
 * ============================================================================
 */

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class Dns {

    record Rec(String name, String type, String value, int ttl) { }

    /** A name server answers for its zone: either an answer, or a referral (NS + glue) to a more specific server. */
    static class NameServer {
        final String id;
        final List<Rec> records = new ArrayList<>();
        NameServer(String id) { this.id = id; }
        NameServer add(String n, String t, String v, int ttl) { records.add(new Rec(n, t, v, ttl)); return this; }
    }

    static final Map<String, NameServer> SERVERS = new HashMap<>();

    static void buildInternet() {
        SERVERS.put("root", new NameServer("root")
            .add("com.", "NS", "tld-com", 172800).add("org.", "NS", "tld-org", 172800));
        SERVERS.put("tld-com", new NameServer("tld-com")
            .add("example.com.", "NS", "ns1.example", 172800)
            .add("cdnprovider.com.", "NS", "ns.cdn", 172800));
        SERVERS.put("ns1.example", new NameServer("ns1.example")
            .add("example.com.", "A", "93.184.216.34", 300)
            .add("example.com.", "AAAA", "2606:2800:220:1::248", 300)
            .add("www.example.com.", "CNAME", "example.edgekey.cdnprovider.com.", 60)
            .add("example.com.", "MX", "20 backup-mail.example.com.", 3600)
            .add("example.com.", "MX", "10 mail.example.com.", 3600)
            .add("example.com.", "TXT", "v=spf1 include:_spf.google.com ~all", 3600)
            .add("_sip._tcp.example.com.", "SRV", "10 60 5060 sip1.example.com.", 3600)
            .add("api.example.com.", "A", "203.0.113.10", 30));
        SERVERS.put("ns.cdn", new NameServer("ns.cdn")
            .add("example.edgekey.cdnprovider.com.", "A", "23.45.67.89", 20));
    }

    /** A recursive resolver (like 8.8.8.8 or your VPC resolver): does iterative queries, caches everything. */
    static class Resolver {
        final Map<String, List<Rec>> cache = new HashMap<>();
        final Map<String, Long> expires = new HashMap<>();
        long now = 0;                                       // simulated clock (seconds)
        int upstreamQueries = 0;
        final List<String> trace = new ArrayList<>();

        List<Rec> resolve(String name, String type) {
            String key = name + "/" + type;
            if (cache.containsKey(key) && expires.get(key) > now) { trace.add("cache hit " + key); return cache.get(key); }
            String server = "root";
            while (true) {                                  // ITERATIVE: ask root, follow referrals down the tree
                upstreamQueries++;
                trace.add("ask " + server + " for " + key);
                NameServer ns = SERVERS.get(server);
                List<Rec> answer = ns.records.stream().filter(r -> r.name().equals(name) && r.type().equals(type)).toList();
                if (!answer.isEmpty()) return store(key, answer);
                Optional<Rec> cname = ns.records.stream().filter(r -> r.name().equals(name) && r.type().equals("CNAME")).findFirst();
                if (cname.isPresent()) {                    // follow the alias, starting again from the top
                    List<Rec> rest = resolve(cname.get().value(), type);
                    List<Rec> chain = new ArrayList<>(List.of(cname.get()));
                    chain.addAll(rest);
                    return store(key, chain);
                }
                Optional<Rec> referral = ns.records.stream().filter(r -> r.type().equals("NS") && name.endsWith(r.name())).findFirst();
                if (referral.isEmpty()) return store(key, List.of(new Rec(name, "NXDOMAIN", "", 300)));   // negative answer, cached too
                server = referral.get().value();
            }
        }

        List<Rec> store(String key, List<Rec> recs) {
            int ttl = recs.stream().mapToInt(Rec::ttl).min().orElse(0);     // a chain lives as long as its shortest TTL
            cache.put(key, recs);
            expires.put(key, now + ttl);
            return recs;
        }
    }

    static String values(List<Rec> recs) { return recs.stream().map(r -> r.type() + " " + r.value()).toList().toString(); }

    // ---------------------------------------------------------------------
    static void hierarchyAndCache() {
        section("1-2. Iterative resolution, caching, TTL");
        Resolver r = new Resolver();
        List<Rec> a = r.resolve("example.com.", "A");
        r.trace.forEach(Dns::show);
        check("example.com A", "[A 93.184.216.34]", values(a));
        check("cold cache: root -> .com TLD -> authoritative = 3 upstream queries", 3, r.upstreamQueries);
        r.trace.clear();
        r.resolve("example.com.", "A");
        check("second lookup within TTL: answered from cache, 0 new queries", 3, r.upstreamQueries);
        r.now += 301;
        r.resolve("example.com.", "A");
        check("after the 300 s TTL expires the resolver asks again", 6, r.upstreamQueries);
        show("a real resolver also caches the NS referrals (TTL 2 days), so the next query would skip root and TLD");
    }

    static void recordTypes() {
        section("3. Record types");
        Resolver r = new Resolver();
        List<Rec> www = r.resolve("www.example.com.", "A");
        show("www.example.com A -> " + values(www));
        check("CNAME chain: www -> CDN hostname -> CDN edge IP", "[CNAME example.edgekey.cdnprovider.com., A 23.45.67.89]", values(www));
        check("the cached chain expires with its SHORTEST TTL (CDN A record: 20 s)", 20L, r.expires.get("www.example.com./A") - r.now);
        check("AAAA = IPv6 address", "[AAAA 2606:2800:220:1::248]", values(r.resolve("example.com.", "AAAA")));
        List<Rec> mx = new ArrayList<>(r.resolve("example.com.", "MX"));
        mx.sort(Comparator.comparingInt(x -> Integer.parseInt(x.value().split(" ")[0])));
        check("MX: lowest preference number is tried first", "10 mail.example.com.", mx.get(0).value());
        check("TXT carries SPF/DKIM/domain-verification strings", true, r.resolve("example.com.", "TXT").get(0).value().startsWith("v=spf1"));
        String[] srv = r.resolve("_sip._tcp.example.com.", "SRV").get(0).value().split(" ");
        check("SRV gives priority, weight, PORT and host (service discovery)", "port 5060 on sip1.example.com.", "port " + srv[2] + " on " + srv[3]);
    }

    static void negative() {
        section("4. Negative caching (NXDOMAIN)");
        Resolver r = new Resolver();
        List<Rec> none = r.resolve("missing.example.com.", "A");
        int q = r.upstreamQueries;
        r.resolve("missing.example.com.", "A");
        check("non-existent name -> NXDOMAIN", "NXDOMAIN", none.get(0).type());
        check("...and the NXDOMAIN itself is cached (no new queries)", q, r.upstreamQueries);
        show("gotcha: query a name BEFORE creating it and resolvers may remember 'does not exist' for the SOA minimum TTL");
    }

    static void splitHorizon() {
        section("5. Split-horizon DNS");
        Map<String, String> internal = Map.of("db.corp.example.com.", "10.0.96.15");
        Map<String, String> external = Map.of();
        java.util.function.BiFunction<String, String, String> answer = (clientIp, name) ->
            (clientIp.startsWith("10.") ? internal : external).getOrDefault(name, "NXDOMAIN");
        check("query from inside the VPC gets the private IP", "10.0.96.15", answer.apply("10.0.1.5", "db.corp.example.com."));
        check("the same query from the Internet gets nothing", "NXDOMAIN", answer.apply("198.51.100.9", "db.corp.example.com."));
        show("why a name 'works on my laptop but not in the container': different resolver, different view, different cache");
    }

    // ---------------------------------------------------------------------
    // 6. wire format
    // ---------------------------------------------------------------------
    static byte[] buildQuery(int id, String name, int qtype) {
        ByteBuffer b = ByteBuffer.allocate(512);
        b.putShort((short) id);
        b.putShort((short) 0x0100);                 // flags: standard query, RD (recursion desired) = 1
        b.putShort((short) 1).putShort((short) 0).putShort((short) 0).putShort((short) 0);   // 1 question
        for (String label : name.split("\\.")) { b.put((byte) label.length()); b.put(label.getBytes()); }
        b.put((byte) 0);                            // root label ends the name
        b.putShort((short) qtype).putShort((short) 1);   // QTYPE (1 = A, 28 = AAAA), QCLASS IN
        return Arrays.copyOf(b.array(), b.position());
    }

    static String parseName(ByteBuffer b, int off) {
        StringBuilder s = new StringBuilder();
        int len;
        while ((len = b.get(off) & 0xff) != 0) { s.append(new String(b.array(), off + 1, len)).append('.'); off += len + 1; }
        return s.toString();
    }

    static void wire() {
        section("6. DNS wire format (UDP port 53)");
        byte[] q = buildQuery(0xBEEF, "example.com", 1);
        StringBuilder hex = new StringBuilder();
        for (byte x : q) hex.append(String.format("%02x ", x));
        show("query bytes: " + hex.toString().trim());
        check("12-byte header + name (1+7+1+3+1) + type + class = 29 bytes", 29, q.length);
        ByteBuffer b = ByteBuffer.wrap(q);
        check("transaction ID (matches answers to questions; random to resist spoofing)", "0xbeef", String.format("0x%04x", b.getShort(0) & 0xffff));
        check("labels are length-prefixed: 7'example' 3'com' 0", "example.com.", parseName(b, 12));
        check("RD flag set: 'please resolve recursively for me'", true, (b.getShort(2) & 0x0100) != 0);
        show("answers > 512 bytes (DNSSEC, many records) need EDNS0 or a retry over TCP");
    }

    static void reverse() throws Exception {
        section("7. Reverse DNS and the local resolver");
        String ip = "93.184.216.34";
        List<String> parts = new ArrayList<>(List.of(ip.split("\\.")));
        Collections.reverse(parts);
        check("PTR lookup name", "34.216.184.93.in-addr.arpa.", String.join(".", parts) + ".in-addr.arpa.");
        InetAddress local = InetAddress.getByName("localhost");
        check("the OS resolver maps 'localhost' via /etc/hosts (no DNS query)", true, local.isLoopbackAddress());
    }

    // ---------------------------------------------------------------------
    // 8. intermittent failures
    // ---------------------------------------------------------------------
    static void intermittent() {
        section("8. Intermittent DNS failures: one dead resolver in resolv.conf");
        // glibc tries nameservers in order; 'options rotate' spreads load; timeout 5 s, attempts 2
        Random rnd = new Random(7);
        int requests = 10_000, slow = 0;
        double totalMs = 0;
        for (int i = 0; i < requests; i++) {
            boolean first = rnd.nextBoolean();                       // with 'rotate', half the lookups start at the dead server
            double ms = first ? 5000 + 2 : 2;                        // wait for the 5 s timeout, then the healthy one answers
            if (ms > 1000) slow++;
            totalMs += ms;
        }
        show(String.format("2 nameservers (one dead), rotate on: %.0f%% of lookups take > 5 s; mean %.0f ms", 100.0 * slow / requests, totalMs / requests));
        check("~half of uncached lookups stall for the 5 s timeout -> 'random' slow requests", true, Math.abs(slow / (double) requests - 0.5) < 0.02);
        show("other classic causes: resolver rate limits (e.g. 1024 pps per ENI on AWS), UDP drops (conntrack full),");
        show("ndots:5 in Kubernetes (api.example.com is tried as 4 search-domain names first), missing caches in the app");
        int searchDomains = 3, ndots = 5;
        String name = "api.example.com";
        long dots = name.chars().filter(c -> c == '.').count();
        int queries = (dots < ndots ? searchDomains : 0) + 1;
        check("Kubernetes ndots:5 -> 4 lookups for one external name (x2 for A + AAAA)", 4, queries);
    }

    public static void main(String[] args) throws Exception {
        buildInternet();
        hierarchyAndCache();
        recordTypes();
        negative();
        splitHorizon();
        wire();
        reverse();
        intermittent();
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
