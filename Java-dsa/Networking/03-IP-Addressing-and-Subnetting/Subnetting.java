/*
 * ============================================================================
 *  NETWORKING 03 : IP ADDRESSING AND SUBNETTING
 * ============================================================================
 *  Run:  java Subnetting.java          (explanations: README.md in this folder)
 *
 *    1. CIDR calculator         network, broadcast, host range, usable hosts, mask
 *    2. same subnet?            the AND-with-mask test every host does before sending
 *    3. address classes         private (RFC 1918), CGNAT, loopback, link-local, multicast, public
 *    4. VPC planning            carve 10.0.0.0/16 into per-AZ subnets; detect overlapping CIDRs
 *    5. host routing decision   on-link vs via gateway; what a wrong gateway breaks
 *    6. NAT / PAT               many private hosts behind one public IP (a translation table)
 *    7. DHCP and ARP            DORA lease handshake; ARP cache resolution
 *    8. IPv6                    parse, compress (::), expand, link-local, why v4 works but v6 doesn't
 * ============================================================================
 */

import java.util.*;

public class Subnetting {

    // ---------------------------------------------------------------------
    // IPv4 as a 32-bit integer
    // ---------------------------------------------------------------------
    static long toInt(String ip) {
        String[] p = ip.split("\\.");
        long v = 0;
        for (String s : p) v = (v << 8) | Integer.parseInt(s);
        return v;
    }

    static String toIp(long v) { return ((v >> 24) & 255) + "." + ((v >> 16) & 255) + "." + ((v >> 8) & 255) + "." + (v & 255); }
    static long mask(int prefix) { return prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL; }

    record Subnet(String network, String broadcast, String firstHost, String lastHost, long addresses, long usable, String netmask) { }

    static Subnet calc(String cidr) {
        String[] parts = cidr.split("/");
        int prefix = Integer.parseInt(parts[1]);
        long ip = toInt(parts[0]), m = mask(prefix);
        long net = ip & m, bcast = net | (~m & 0xFFFFFFFFL);
        long size = 1L << (32 - prefix);
        if (prefix >= 31)                                         // /31 point-to-point links (RFC 3021) and /32 single hosts
            return new Subnet(toIp(net), "n/a", toIp(net), toIp(bcast), size, size, toIp(m));
        return new Subnet(toIp(net), toIp(bcast), toIp(net + 1), toIp(bcast - 1), size, size - 2, toIp(m));
    }

    static boolean sameSubnet(String a, String b, int prefix) { return (toInt(a) & mask(prefix)) == (toInt(b) & mask(prefix)); }

    static void cidr() {
        section("1-2. CIDR calculator and the same-subnet test");
        Subnet s24 = calc("192.168.1.0/24"), s26 = calc("192.168.1.77/26");
        show("192.168.1.0/24  -> " + s24);
        show("192.168.1.77/26 -> " + s26);
        check("/24: 256 addresses, 254 usable hosts", "256/254", s24.addresses() + "/" + s24.usable());
        check("/24 netmask", "255.255.255.0", s24.netmask());
        check("/26: 64 addresses (2^(32-26)), 62 usable", "64/62", s26.addresses() + "/" + s26.usable());
        check("192.168.1.77/26 lives in network .64, broadcast .127", "192.168.1.64 - 192.168.1.127", s26.network() + " - " + s26.broadcast());
        check("its usable host range", "192.168.1.65 - 192.168.1.126", s26.firstHost() + " - " + s26.lastHost());
        check("a /24 splits into four /26 subnets", 4, s24.addresses() / s26.addresses());
        check("/31 (point-to-point link): both addresses usable", 2, calc("10.0.0.0/31").usable());
        check("/32 is a single host (e.g. a route to one IP)", 1, calc("10.0.0.7/32").addresses());
        check("10.0.1.200 and 10.0.1.5 are in the same /24", true, sameSubnet("10.0.1.200", "10.0.1.5", 24));
        check("...but NOT in the same /25 (.0-.127 vs .128-.255)", false, sameSubnet("10.0.1.200", "10.0.1.5", 25));
        check("netmask 255.255.240.0 = /20", "255.255.240.0", toIp(mask(20)));
    }

    // ---------------------------------------------------------------------
    // 3. classifying addresses
    // ---------------------------------------------------------------------
    static boolean in(String ip, String cidr) { String[] p = cidr.split("/"); return sameSubnet(ip, p[0], Integer.parseInt(p[1])); }

    static String classify(String ip) {
        if (in(ip, "127.0.0.0/8")) return "loopback";
        if (in(ip, "10.0.0.0/8") || in(ip, "172.16.0.0/12") || in(ip, "192.168.0.0/16")) return "private (RFC 1918)";
        if (in(ip, "100.64.0.0/10")) return "carrier-grade NAT (RFC 6598)";
        if (in(ip, "169.254.0.0/16")) return "link-local (no DHCP answer / cloud metadata)";
        if (in(ip, "224.0.0.0/4")) return "multicast";
        if (ip.equals("255.255.255.255")) return "limited broadcast";
        return "public";
    }

    static void classes() {
        section("3. Address types");
        String[] ips = {"10.4.2.1", "172.20.0.5", "172.32.0.5", "192.168.0.10", "100.72.1.1", "169.254.169.254", "127.0.0.1", "224.0.0.251", "8.8.8.8"};
        for (String ip : ips) show(String.format("%-16s %s", ip, classify(ip)));
        check("172.20.0.5 is private (inside 172.16.0.0/12)", "private (RFC 1918)", classify("172.20.0.5"));
        check("172.32.0.5 is PUBLIC (just outside the /12)", "public", classify("172.32.0.5"));
        check("169.254.169.254 = link-local (the cloud metadata service)", true, classify("169.254.169.254").startsWith("link-local"));
        check("the whole 127.0.0.0/8 is loopback, not just 127.0.0.1", "loopback", classify("127.8.8.8"));
    }

    // ---------------------------------------------------------------------
    // 4. VPC planning
    // ---------------------------------------------------------------------
    static List<String> split(String cidr, int newPrefix) {
        String[] p = cidr.split("/");
        long base = toInt(p[0]) & mask(Integer.parseInt(p[1]));
        int count = 1 << (newPrefix - Integer.parseInt(p[1]));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(toIp(base + ((long) i << (32 - newPrefix))) + "/" + newPrefix);
        return out;
    }

    static boolean overlaps(String a, String b) {
        String[] pa = a.split("/"), pb = b.split("/");
        int pre = Math.min(Integer.parseInt(pa[1]), Integer.parseInt(pb[1]));
        return sameSubnet(pa[0], pb[0], pre);
    }

    static void vpcPlanning() {
        section("4. VPC planning");
        List<String> subnets = split("10.0.0.0/16", 20);
        show("10.0.0.0/16 split into /20s (4,096 addresses each): " + subnets.subList(0, 6) + " ...");
        check("a /16 holds sixteen /20 subnets", 16, subnets.size());
        Map<String, String> plan = new LinkedHashMap<>();
        String[] tiers = {"public", "private-app", "private-db"};
        String[] azs = {"az-a", "az-b", "az-c"};
        int k = 0;
        for (String t : tiers) for (String az : azs) plan.put(t + "/" + az, subnets.get(k++));
        show("tier x AZ plan: " + plan);
        check("private-db in az-c", "10.0.128.0/20", plan.get("private-db/az-c"));
        check("7 /20s left for growth (plan big: resizing a VPC later is painful)", 7, subnets.size() - plan.size());
        check("AWS reserves 5 addresses per subnet: usable in a /20", 4091, (1 << 12) - 5);
        check("VPC 10.0.0.0/16 overlaps an office network 10.0.64.0/18 -> peering/VPN routing breaks", true, overlaps("10.0.0.0/16", "10.0.64.0/18"));
        check("10.1.0.0/16 does not overlap 10.0.0.0/16", false, overlaps("10.0.0.0/16", "10.1.0.0/16"));
        check("Docker's default bridge 172.17.0.0/16 overlaps a VPC 172.16.0.0/12 -> containers can't reach it", true, overlaps("172.17.0.0/16", "172.16.0.0/12"));
    }

    // ---------------------------------------------------------------------
    // 5. on-link or via the default gateway?
    // ---------------------------------------------------------------------
    static String nextHop(String myIp, int prefix, String gateway, String dst) {
        if (sameSubnet(myIp, dst, prefix)) return "direct (ARP for " + dst + ")";
        if (gateway == null || !sameSubnet(myIp, gateway, prefix)) return "unreachable (no valid gateway)";
        return "via gateway " + gateway + " (ARP for " + gateway + ")";
    }

    static void gateway() {
        section("5. The host's routing decision and the default gateway");
        check("same subnet -> deliver directly", "direct (ARP for 10.0.1.9)", nextHop("10.0.1.5", 24, "10.0.1.1", "10.0.1.9"));
        check("other network -> send to the default gateway", "via gateway 10.0.1.1 (ARP for 10.0.1.1)", nextHop("10.0.1.5", 24, "10.0.1.1", "93.184.216.34"));
        check("wrong gateway (not on our subnet) -> local works, everything else fails", "unreachable (no valid gateway)", nextHop("10.0.1.5", 24, "10.0.2.1", "93.184.216.34"));
        check("...but same-subnet traffic still works with a wrong gateway", true, nextHop("10.0.1.5", 24, "10.0.2.1", "10.0.1.9").startsWith("direct"));
        check("wrong PREFIX (/16 instead of /24): 10.0.2.7 looks local, ARP gets no answer", true, nextHop("10.0.1.5", 16, "10.0.1.1", "10.0.2.7").startsWith("direct"));
    }

    // ---------------------------------------------------------------------
    // 6. NAT / PAT
    // ---------------------------------------------------------------------
    static class Pat {
        final String publicIp;
        int nextPort = 40000;
        final Map<String, Integer> out = new HashMap<>();          // "privIp:privPort->dst" -> public port
        final Map<Integer, String> back = new HashMap<>();        // public port -> "privIp:privPort"

        Pat(String publicIp) { this.publicIp = publicIp; }

        String outbound(String privIp, int privPort, String dst) {
            String key = privIp + ":" + privPort + "->" + dst;
            int port = out.computeIfAbsent(key, k -> { int p = nextPort++; back.put(p, privIp + ":" + privPort); return p; });
            return publicIp + ":" + port;
        }

        String inbound(int publicPort) { return back.getOrDefault(publicPort, "DROP (no mapping: unsolicited inbound)"); }
    }

    static void nat() {
        section("6. NAT / PAT (port address translation)");
        Pat nat = new Pat("203.0.113.7");
        String a = nat.outbound("192.168.1.10", 51000, "93.184.216.34:443");
        String b = nat.outbound("192.168.1.11", 51000, "93.184.216.34:443");   // same private port on another host
        String c = nat.outbound("192.168.1.10", 51000, "93.184.216.34:443");   // same flow again
        show("192.168.1.10:51000 -> " + a + ",  192.168.1.11:51000 -> " + b);
        check("two hosts share one public IP, told apart by the translated PORT", true, !a.equals(b) && a.startsWith("203.0.113.7") && b.startsWith("203.0.113.7"));
        check("the same flow keeps the same mapping", a, c);
        check("replies to public port 40001 go back to the right private host", "192.168.1.11:51000", nat.inbound(40001));
        check("unsolicited inbound traffic has no mapping -> dropped (why servers behind NAT need port forwarding)", true, nat.inbound(45000).startsWith("DROP"));
        show("limit: one public IP has at most ~64k source ports per (destination IP, port) -> 'SNAT port exhaustion' under heavy egress");
    }

    // ---------------------------------------------------------------------
    // 7. DHCP and ARP
    // ---------------------------------------------------------------------
    static void dhcpArp() {
        section("7. DHCP (DORA) and ARP");
        List<String> dora = List.of(
            "DISCOVER  client 0.0.0.0:68 -> 255.255.255.255:67 (broadcast: 'any DHCP server?')",
            "OFFER     server -> client: 'take 10.0.1.57/24, gateway 10.0.1.1, DNS 10.0.0.2, lease 1h'",
            "REQUEST   client -> broadcast: 'I accept 10.0.1.57 from this server'",
            "ACK       server -> client: lease confirmed; renew at 50% of lease (T1)");
        dora.forEach(Subnetting::show);
        check("DHCP is 4 messages: Discover, Offer, Request, Ack", "DORA", String.join("", dora.stream().map(s -> s.substring(0, 1)).toList()));

        Map<String, String> arpCache = new HashMap<>();
        Map<String, String> lan = Map.of("10.0.1.1", "aa:bb:cc:00:00:01", "10.0.1.9", "aa:bb:cc:00:00:09");
        int broadcasts = 0;
        for (String ip : List.of("10.0.1.1", "10.0.1.9", "10.0.1.1", "10.0.1.1")) {
            if (!arpCache.containsKey(ip)) { broadcasts++; arpCache.put(ip, lan.get(ip)); }   // "Who has 10.0.1.1? Tell 10.0.1.5"
        }
        check("ARP broadcasts only on a cache miss (2 for 4 sends)", 2, broadcasts);
        check("cached mapping for the gateway", "aa:bb:cc:00:00:01", arpCache.get("10.0.1.1"));
    }

    // ---------------------------------------------------------------------
    // 8. IPv6
    // ---------------------------------------------------------------------
    static String expand6(String a) {
        String[] halves = a.split("::", -1);
        List<String> left = halves[0].isEmpty() ? List.of() : List.of(halves[0].split(":"));
        List<String> right = halves.length > 1 && !halves[1].isEmpty() ? List.of(halves[1].split(":")) : List.of();
        List<String> groups = new ArrayList<>(left);
        for (int i = 0; i < 8 - left.size() - right.size(); i++) groups.add("0");
        groups.addAll(right);
        return String.join(":", groups.stream().map(g -> String.format("%4s", g).replace(' ', '0')).toList());
    }

    static String compress6(String full) {
        String[] g = full.split(":");
        for (int i = 0; i < 8; i++) g[i] = g[i].replaceFirst("^0+(?=.)", "");      // drop leading zeros in each group
        int bestStart = -1, bestLen = 1;                                           // longest run of >= 2 zero groups -> ::
        for (int i = 0; i < 8; ) {
            if (g[i].equals("0")) { int j = i; while (j < 8 && g[j].equals("0")) j++; if (j - i > bestLen) { bestLen = j - i; bestStart = i; } i = j; }
            else i++;
        }
        if (bestStart < 0) return String.join(":", g);
        String left = String.join(":", Arrays.copyOfRange(g, 0, bestStart));
        String right = String.join(":", Arrays.copyOfRange(g, bestStart + bestLen, 8));
        return left + "::" + right;
    }

    static void ipv6() {
        section("8. IPv6");
        String full = expand6("2001:db8::8a2e:370:7334");
        check("expand 2001:db8::8a2e:370:7334", "2001:0db8:0000:0000:0000:8a2e:0370:7334", full);
        check("...and compress it back (zeros dropped, longest zero run -> ::)", "2001:db8::8a2e:370:7334", compress6(full));
        check("loopback ::1", "::1", compress6(expand6("::1")));
        check("IPv6 has 2^128 addresses; a single /64 subnet holds 2^64 (no subnet maths for hosts)", "18446744073709551616", new java.math.BigInteger("2").pow(64).toString());
        check("fe80::/10 = link-local (every interface has one, used by Neighbor Discovery)", true, expand6("fe80::1").startsWith("fe80"));
        Map<String, Boolean> checklist = new LinkedHashMap<>();
        checklist.put("AAAA record published", true);
        checklist.put("server listens on [::]:443 (not only 0.0.0.0)", false);
        checklist.put("security group / firewall allows IPv6", true);
        checklist.put("route to ::/0 via IPv6 gateway", true);
        String broken = checklist.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).findFirst().orElse("none");
        check("'works on IPv4, fails on IPv6': first failing item", "server listens on [::]:443 (not only 0.0.0.0)", broken);
    }

    public static void main(String[] args) {
        cidr();
        classes();
        vpcPlanning();
        gateway();
        nat();
        dhcpArp();
        ipv6();
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
