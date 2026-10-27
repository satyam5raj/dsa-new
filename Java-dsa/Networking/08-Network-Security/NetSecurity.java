/*
 * ============================================================================
 *  NETWORKING 08 : NETWORK SECURITY
 * ============================================================================
 *  Run:  java NetSecurity.java          (explanations: README.md in this folder)
 *
 *    1. authN vs authZ        a signed token proves WHO; a policy decides WHAT -- TLS does neither alone
 *    2. request signing       HMAC + timestamp + nonce: integrity, identity, replay protection
 *    3. SYN flood             backlog exhaustion vs SYN cookies (real HMAC cookies)
 *    4. rate limiting         token bucket per client: absorbs bursts, stops floods
 *    5. amplification         why open DNS/NTP/memcached reflectors are DDoS weapons
 *    6. DNS spoofing          odds of a forged answer: txid only vs txid + random source port
 *    7. ARP spoofing          a poisoned cache, and detecting the flip
 *    8. MITM + pinning        a rogue-but-trusted certificate vs a pinned public key; rotation trade-off
 *    9. allowlists            CIDR allowlists, and why the source IP must be the real one
 *   10. zero trust            per-request decisions on identity + device + context, not network location
 *   11. segmentation          which tier may talk to which
 * ============================================================================
 */

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class NetSecurity {

    static byte[] hmac(byte[] key, String msg) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(key, "HmacSHA256"));
        return m.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    static String b64(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }

    // ---------------------------------------------------------------------
    // 1. authentication vs authorization
    // ---------------------------------------------------------------------
    static final byte[] TOKEN_KEY = "server-side-secret-key-32-bytes!".getBytes();

    static String issueToken(String user, String role, long exp) throws Exception {
        String payload = b64(("{\"sub\":\"" + user + "\",\"role\":\"" + role + "\",\"exp\":" + exp + "}").getBytes());
        return payload + "." + b64(hmac(TOKEN_KEY, payload));
    }

    static String authenticate(String token, long now) throws Exception {
        String[] p = token.split("\\.");
        if (p.length != 2 || !MessageDigest.isEqual(hmac(TOKEN_KEY, p[0]), Base64.getUrlDecoder().decode(p[1]))) return null;   // constant-time compare
        String json = new String(Base64.getUrlDecoder().decode(p[0]));
        long exp = Long.parseLong(json.replaceAll(".*\"exp\":(\\d+).*", "$1"));
        return exp < now ? null : json;
    }

    static boolean authorize(String claims, String action) {
        Map<String, Set<String>> rbac = Map.of("viewer", Set.of("orders:read"), "admin", Set.of("orders:read", "orders:refund"));
        String role = claims.replaceAll(".*\"role\":\"(\\w+)\".*", "$1");
        return rbac.getOrDefault(role, Set.of()).contains(action);
    }

    static void authnAuthz() throws Exception {
        section("1. Authentication vs authorization");
        String alice = issueToken("alice", "viewer", 2000);
        String claims = authenticate(alice, 1000);
        check("authentication: the signature proves the token is ours -> alice", true, claims != null && claims.contains("alice"));
        check("authorization: viewer may read orders", true, authorize(claims, "orders:read"));
        check("...but may NOT refund (403, though perfectly authenticated over TLS)", false, authorize(claims, "orders:refund"));
        String tampered = b64(("{\"sub\":\"alice\",\"role\":\"admin\",\"exp\":2000}").getBytes()) + "." + alice.split("\\.")[1];
        check("changing role to admin breaks the signature -> rejected", null, authenticate(tampered, 1000));
        check("expired token -> rejected", null, authenticate(alice, 3000));
    }

    // ---------------------------------------------------------------------
    // 2. HMAC request signing with replay protection
    // ---------------------------------------------------------------------
    static final Set<String> SEEN_NONCES = new HashSet<>();

    static String verifyRequest(Map<String, String> req, byte[] key, long now) throws Exception {
        String canonical = req.get("method") + "\n" + req.get("path") + "\n" + req.get("ts") + "\n" + req.get("nonce") + "\n" + req.get("body");
        if (!MessageDigest.isEqual(hmac(key, canonical), Base64.getUrlDecoder().decode(req.get("sig")))) return "401 bad signature";
        if (Math.abs(now - Long.parseLong(req.get("ts"))) > 300) return "401 stale timestamp";
        if (!SEEN_NONCES.add(req.get("nonce"))) return "401 replayed";
        return "200";
    }

    static void signing() throws Exception {
        section("2. HMAC request signing (like AWS SigV4 / webhook signatures)");
        byte[] key = "partner-shared-secret".getBytes();
        Map<String, String> req = new HashMap<>(Map.of("method", "POST", "path", "/payments", "ts", "1000", "nonce", "n-7f3a", "body", "{\"amount\":100}"));
        req.put("sig", b64(hmac(key, "POST\n/payments\n1000\nn-7f3a\n{\"amount\":100}")));
        check("valid signed request", "200", verifyRequest(req, key, 1010));
        check("the exact same request replayed by an attacker", "401 replayed", verifyRequest(req, key, 1020));
        Map<String, String> changed = new HashMap<>(req);
        changed.put("body", "{\"amount\":100000}");
        changed.put("nonce", "n-other");
        check("body modified in transit", "401 bad signature", verifyRequest(changed, key, 1030));
        Map<String, String> old = new HashMap<>(req);
        old.put("nonce", "n-new");
        old.put("sig", b64(hmac(key, "POST\n/payments\n1000\nn-new\n{\"amount\":100}")));
        check("a validly signed but old request (outside the 5-minute window)", "401 stale timestamp", verifyRequest(old, key, 2000));
    }

    // ---------------------------------------------------------------------
    // 3. SYN flood and SYN cookies
    // ---------------------------------------------------------------------
    static long synCookie(byte[] secret, String fourTuple, long minute) throws Exception {
        byte[] h = hmac(secret, fourTuple + "|" + minute);
        return ((h[0] & 0xffL) << 24 | (h[1] & 0xffL) << 16 | (h[2] & 0xffL) << 8 | (h[3] & 0xffL));   // becomes the server's ISN
    }

    static void synFlood() throws Exception {
        section("3. SYN flood vs SYN cookies");
        int backlog = 256, attackerSynsPerTick = 2000, legitPerTick = 20, ticks = 10;
        int halfOpenTimeout = 30;                                          // ticks a half-open entry stays (no final ACK arrives)
        int queue = 0, legitAccepted = 0;
        for (int t = 0; t < ticks; t++) {
            int space = Math.max(0, backlog - queue);
            int arrivals = attackerSynsPerTick + legitPerTick;
            double legitShare = legitPerTick / (double) arrivals;
            int admitted = Math.min(space, arrivals);
            legitAccepted += (int) Math.round(admitted * legitShare);
            queue += admitted;                                             // spoofed SYNs never complete; they sit until timeout
            if (t >= halfOpenTimeout) queue = 0;
        }
        double without = legitAccepted / (double) (legitPerTick * ticks);
        check("without SYN cookies the backlog fills with spoofed half-open entries", true, without < 0.05);

        byte[] secret = new byte[16];
        new SecureRandom().nextBytes(secret);
        String tuple = "198.51.100.7:51000->10.0.1.5:443";
        long isn = synCookie(secret, tuple, 1000);                         // server keeps NO state, just sends SYN-ACK with this ISN
        long ackFromRealClient = isn + 1;
        check("SYN cookie: the final ACK proves the client got our SYN-ACK (stateless check)", true, synCookie(secret, tuple, 1000) == ackFromRealClient - 1);
        check("a spoofed ACK for another tuple fails the cookie check", false, synCookie(secret, "203.0.113.9:4444->10.0.1.5:443", 1000) == ackFromRealClient - 1);
        show(String.format("with cookies no backlog is consumed: legit success ~100%% (without: %.1f%%)", without * 100));
    }

    // ---------------------------------------------------------------------
    // 4. rate limiting
    // ---------------------------------------------------------------------
    static class TokenBucket {
        final double capacity, refillPerSec;
        double tokens, last;
        TokenBucket(double capacity, double refillPerSec) { this.capacity = capacity; this.refillPerSec = refillPerSec; tokens = capacity; }
        boolean allow(double now) {
            tokens = Math.min(capacity, tokens + (now - last) * refillPerSec);
            last = now;
            if (tokens >= 1) { tokens -= 1; return true; }
            return false;                                                  // -> HTTP 429 Too Many Requests + Retry-After
        }
    }

    static void rateLimiting() {
        section("4. Rate limiting (token bucket per client)");
        Map<String, TokenBucket> buckets = new HashMap<>();
        int legitOk = 0, legitTotal = 0, floodOk = 0, floodTotal = 0;
        for (int ms = 0; ms < 10_000; ms++) {
            double now = ms / 1000.0;
            if (ms % 200 == 0) { legitTotal++; if (buckets.computeIfAbsent("good", k -> new TokenBucket(20, 10)).allow(now)) legitOk++; }   // 5 req/s
            for (int k = 0; k < 2; k++) { floodTotal++; if (buckets.computeIfAbsent("bot", x -> new TokenBucket(20, 10)).allow(now)) floodOk++; }  // 2000 req/s
        }
        show(String.format("limit 10 req/s (burst 20): legit %d/%d allowed, flood %d/%d allowed", legitOk, legitTotal, floodOk, floodTotal));
        check("a well-behaved client is never limited", legitTotal, legitOk);
        check("the flooder gets only burst + rate x time (20 + 10/s x 10 s)", true, Math.abs(floodOk - 120) <= 2);
    }

    // ---------------------------------------------------------------------
    // 5-6. amplification and DNS spoofing
    // ---------------------------------------------------------------------
    static void ddosAndDns() {
        section("5-6. Reflection/amplification and DNS spoofing odds");
        int query = 60, response = 3000;                                   // small spoofed query -> big answer to the victim
        check("DNS amplification factor (ANY/DNSSEC answer vs query)", 50, response / query);
        double attackerGbps = 2;
        check("2 Gbit/s of spoofed queries -> 100 Gbit/s at the victim", 100.0, attackerGbps * response / query);

        double perPacketTxid = 1.0 / 65536, perPacketTxidPort = 1.0 / (65536.0 * 28232);
        int forged = 1000;                                                // forged answers squeezed in before the real one
        double p1 = 1 - Math.pow(1 - perPacketTxid, forged), p2 = 1 - Math.pow(1 - perPacketTxidPort, forged);
        show(String.format("1000 forged answers per race: success %.2f%% (16-bit txid only) vs %.7f%% (txid + random source port)", p1 * 100, p2 * 100));
        check("source-port randomisation (post-Kaminsky) makes poisoning ~28,000x harder", true, p1 / p2 > 20000);
    }

    // ---------------------------------------------------------------------
    // 7. ARP spoofing
    // ---------------------------------------------------------------------
    static void arpSpoofing() {
        section("7. ARP spoofing (LAN man-in-the-middle)");
        Map<String, String> arp = new HashMap<>(Map.of("10.0.1.1", "aa:bb:cc:00:00:01"));
        List<String> alerts = new ArrayList<>();
        java.util.function.BiConsumer<String, String> gratuitousReply = (ip, mac) -> {   // ARP has no authentication: hosts accept replies
            String old = arp.put(ip, mac);
            if (old != null && !old.equals(mac)) alerts.add(ip + " moved " + old + " -> " + mac);
        };
        gratuitousReply.accept("10.0.1.1", "de:ad:be:ef:00:66");          // attacker claims to be the gateway
        check("the victim now sends gateway traffic to the attacker's MAC", "de:ad:be:ef:00:66", arp.get("10.0.1.1"));
        check("detection: the gateway's MAC suddenly changed (arpwatch-style alert)", 1, alerts.size());
        show("defences: dynamic ARP inspection on switches, static ARP for gateways, and TLS so a MITM sees only ciphertext");
    }

    // ---------------------------------------------------------------------
    // 8. MITM and certificate pinning
    // ---------------------------------------------------------------------
    static String spkiPin(PublicKey k) throws Exception { return b64(MessageDigest.getInstance("SHA-256").digest(k.getEncoded())); }

    static void pinning() throws Exception {
        section("8. MITM with a rogue certificate vs public-key pinning");
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(256);
        PublicKey realKey = g.generateKeyPair().getPublic(), backupKey = g.generateKeyPair().getPublic(), mitmKey = g.generateKeyPair().getPublic();
        Set<String> pins = Set.of(spkiPin(realKey), spkiPin(backupKey));   // pin the current AND a backup key
        show("scenario: the attacker's certificate chains to a TRUSTED CA (a compromised CA, or a TLS-inspection root) -> normal validation passes");
        check("...but its public key is not pinned -> connection refused", false, pins.contains(spkiPin(mitmKey)));
        check("the real server key matches a pin", true, pins.contains(spkiPin(realKey)));
        check("rotation to the pre-pinned backup key keeps old clients working", true, pins.contains(spkiPin(backupKey)));
        show("trade-off: pin only what you control; a rotation without a backup pin bricks every installed app");
    }

    // ---------------------------------------------------------------------
    // 9-11. allowlists, zero trust, segmentation
    // ---------------------------------------------------------------------
    static boolean inCidr(String ip, String cidr) {
        String[] p = cidr.split("/");
        int len = Integer.parseInt(p[1]);
        long mask = len == 0 ? 0 : (0xFFFFFFFFL << (32 - len)) & 0xFFFFFFFFL;
        return (toLong(ip) & mask) == (toLong(p[0]) & mask);
    }

    static long toLong(String ip) { long v = 0; for (String s : ip.split("\\.")) v = (v << 8) | Integer.parseInt(s); return v; }

    record Ctx(String identity, boolean mtlsVerified, boolean deviceCompliant, String action, String networkZone) { }

    static String zeroTrust(Ctx c) {
        if (!c.mtlsVerified()) return "deny: no verified workload identity";
        if (!c.deviceCompliant()) return "deny: device posture";
        Map<String, Set<String>> policy = Map.of("spiffe://prod/checkout", Set.of("payments:charge"), "spiffe://prod/reporting", Set.of("payments:read"));
        return policy.getOrDefault(c.identity(), Set.of()).contains(c.action()) ? "allow" : "deny: not authorized for " + c.action();
    }

    static void zeroTrustAndSegments() {
        section("9-11. Allowlists, zero trust, segmentation");
        List<String> allow = List.of("203.0.113.0/24", "198.51.100.17/32");
        check("partner office IP inside the allowlisted /24", true, allow.stream().anyMatch(c -> inCidr("203.0.113.88", c)));
        check("anything else is rejected", false, allow.stream().anyMatch(c -> inCidr("203.0.114.1", c)));
        show("allowlists need the REAL source IP: behind proxies use the trusted XFF hop (chapter 07), never a client-supplied header");

        check("zero trust: being 'inside the network' grants nothing",
              "deny: no verified workload identity", zeroTrust(new Ctx("spiffe://prod/checkout", false, true, "payments:charge", "internal")));
        check("verified identity + compliant device + allowed action", "allow", zeroTrust(new Ctx("spiffe://prod/checkout", true, true, "payments:charge", "internet")));
        check("verified, but this service may only read", "deny: not authorized for payments:charge", zeroTrust(new Ctx("spiffe://prod/reporting", true, true, "payments:charge", "internal")));

        Map<String, Set<String>> sgAllows = Map.of("lb", Set.of("web"), "web", Set.of("app"), "app", Set.of("db", "cache"));
        java.util.function.BiPredicate<String, String> canTalk = (from, to) -> sgAllows.getOrDefault(from, Set.of()).contains(to);
        check("segmentation: web tier cannot reach the database directly", false, canTalk.test("web", "db"));
        check("only the app tier can", true, canTalk.test("app", "db"));
    }

    public static void main(String[] args) throws Exception {
        authnAuthz();
        signing();
        synFlood();
        rateLimiting();
        ddosAndDns();
        arpSpoofing();
        pinning();
        zeroTrustAndSegments();
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
