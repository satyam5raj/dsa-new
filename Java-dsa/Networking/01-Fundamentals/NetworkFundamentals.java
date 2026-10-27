/*
 * ============================================================================
 *  NETWORKING 01 : FUNDAMENTALS  (the numbers behind "the network is slow")
 * ============================================================================
 *  Run:  java NetworkFundamentals.java          (explanations: README.md in this folder)
 *
 *    1. delay components     transmission vs propagation vs queueing
 *    2. bandwidth vs latency why a fat pipe can still feel slow
 *    3. bandwidth-delay      how much data must be "in flight" to fill a link
 *    4. throughput vs loss   the Mathis formula: 1% loss can cut TCP throughput 10x
 *    5. jitter               variation in delay, measured like RFC 3550
 *    6. switching            circuit vs packet switching (statistical multiplexing)
 *    7. MTU + fragmentation  how a 4000-byte IPv4 packet is split at MTU 1500
 *    8. encapsulation        header overhead at each layer
 *    9. MAC addresses        structure: OUI, unicast/multicast, local/universal bits
 * ============================================================================
 */

import java.util.*;

public class NetworkFundamentals {

    // ---------------------------------------------------------------------
    // 1-2. delay
    // ---------------------------------------------------------------------
    /** time to push all bits onto the wire */
    static double transmissionMs(long bytes, double bitsPerSecond) { return bytes * 8 / bitsPerSecond * 1000; }

    /** time for a bit to travel the distance (light in fibre ~ 200,000 km/s) */
    static double propagationMs(double km) { return km / 200_000.0 * 1000; }

    static void delays() {
        section("1-2. Delay components and bandwidth vs latency");
        double tx = transmissionMs(1500, 100e6);
        check("transmission delay: 1500 B on 100 Mbit/s = 0.12 ms", "0.120", f3(tx));
        double prop = propagationMs(6000);                       // Mumbai -> London, roughly
        check("propagation delay over 6000 km of fibre = 30 ms", "30.000", f3(prop));

        // A 1 KB API response on a 10 Gbit/s link vs a 100 Mbit/s link, 6000 km away, 1 round trip
        double slowLink = transmissionMs(1024, 100e6) + 2 * prop;
        double fastLink = transmissionMs(1024, 10e9) + 2 * prop;
        show(String.format("1 KB request/response over 6000 km: 100 Mbit/s %.3f ms vs 10 Gbit/s %.3f ms", slowLink, fastLink));
        check("100x more bandwidth barely helps a small request (latency dominates)", true, slowLink / fastLink < 1.002);

        // a 1 GB download is the opposite: bandwidth dominates
        double bigSlow = transmissionMs(1L << 30, 100e6) + 2 * prop, bigFast = transmissionMs(1L << 30, 10e9) + 2 * prop;
        show(String.format("1 GB download: 100 Mbit/s %.1f s vs 10 Gbit/s %.2f s", bigSlow / 1000, bigFast / 1000));
        check("for bulk transfer bandwidth dominates (~100x faster)", true, bigSlow / bigFast > 90);

        // queueing delay: M/M/1 waiting time explodes as utilisation -> 1
        double service = 1.0;                                   // 1 ms per packet
        double[] util = {0.5, 0.8, 0.9, 0.99};
        StringBuilder sb = new StringBuilder("queueing delay (M/M/1, 1 ms service) at utilisation ");
        for (double u : util) sb.append(String.format("%.0f%% -> %.0f ms  ", u * 100, service * u / (1 - u)));
        show(sb.toString());
        check("at 99% utilisation the queue adds ~99x the service time", "99", String.format("%.0f", service * 0.99 / 0.01));
    }

    // ---------------------------------------------------------------------
    // 3-4. bandwidth-delay product and throughput under loss
    // ---------------------------------------------------------------------
    static void bdpAndLoss() {
        section("3-4. Bandwidth-delay product, throughput vs packet loss");
        double bw = 1e9, rttMs = 80;
        double bdpBytes = bw * rttMs / 1000 / 8;
        check("BDP of 1 Gbit/s x 80 ms RTT = 10 MB in flight", "10.0", String.format("%.1f", bdpBytes / 1e6));
        double window = 64 * 1024;                                // classic max window without window scaling
        double maxTput = window * 8 / (rttMs / 1000) / 1e6;
        show(String.format("with a 64 KB window (no window scaling) max throughput = %.2f Mbit/s on that 1 Gbit/s link", maxTput));
        check("a small window caps throughput at window / RTT, regardless of bandwidth", true, maxTput < 7);

        // Mathis et al.: throughput <= (MSS / RTT) * (1.22 / sqrt(p))
        double mss = 1460 * 8, rtt = 0.080;
        double t1 = mss / rtt * 1.22 / Math.sqrt(0.0001) / 1e6, t2 = mss / rtt * 1.22 / Math.sqrt(0.01) / 1e6;
        show(String.format("Mathis TCP throughput bound at 80 ms RTT: 0.01%% loss %.1f Mbit/s, 1%% loss %.2f Mbit/s", t1, t2));
        check("100x more loss -> 10x less throughput (1/sqrt(p))", "10.0", String.format("%.1f", t1 / t2));
        double t3 = mss / 0.010 * 1.22 / Math.sqrt(0.01) / 1e6;
        check("same loss with 8x shorter RTT -> 8x more throughput (why CDNs/edges help)", "8.0", String.format("%.1f", t3 / t2));
    }

    // ---------------------------------------------------------------------
    // 5. jitter
    // ---------------------------------------------------------------------
    static void jitter() {
        section("5. Latency vs jitter");
        double[] sendMs = {0, 20, 40, 60, 80, 100};
        double[] recvMs = {50, 72, 89, 115, 130, 152};
        double j = 0;                                              // RFC 3550 interarrival jitter: J += (|D| - J) / 16
        List<Double> transit = new ArrayList<>();
        for (int i = 0; i < sendMs.length; i++) transit.add(recvMs[i] - sendMs[i]);
        for (int i = 1; i < transit.size(); i++) j += (Math.abs(transit.get(i) - transit.get(i - 1)) - j) / 16;
        show("one-way delays (ms): " + transit);
        double mean = transit.stream().mapToDouble(d -> d).average().orElse(0);
        check("mean latency is 308/6 = 51.3 ms", "51.3", String.format("%.1f", mean));
        check("RFC 3550 smoothed jitter after 6 packets (diffs 2,3,6,5,2)", "1.00", String.format("%.2f", j));
        check("a jitter buffer must absorb max - min delay = 6 ms", "6.0",
              String.format("%.1f", Collections.max(transit) - Collections.min(transit)));
    }

    // ---------------------------------------------------------------------
    // 6. circuit vs packet switching
    // ---------------------------------------------------------------------
    static void switching() {
        section("6. Circuit switching vs packet switching");
        // 1 Mbit/s link; each user needs 100 kbit/s when active and is active 10% of the time
        int circuitUsers = 10;                                      // circuit switching reserves 100 kbit/s per user
        int n = 35;
        double pOver = 0;                                           // packet switching: P(more than 10 of 35 active at once)
        for (int k = 11; k <= n; k++) pOver += binom(n, k) * Math.pow(0.1, k) * Math.pow(0.9, n - k);
        show(String.format("circuit switching supports %d users; packet switching supports %d users with P(overload) = %.6f", circuitUsers, n, pOver));
        check("statistical multiplexing: 3.5x more users with < 0.05% overload risk", true, pOver < 0.0005);
    }

    static double binom(int n, int k) { double r = 1; for (int i = 1; i <= k; i++) r = r * (n - k + i) / i; return r; }

    // ---------------------------------------------------------------------
    // 7. MTU and IPv4 fragmentation
    // ---------------------------------------------------------------------
    record Fragment(int offsetUnits, int dataBytes, boolean moreFragments) { }

    static List<Fragment> fragment(int totalPacket, int mtu) {
        int header = 20, data = totalPacket - header;
        int perFrag = (mtu - header) / 8 * 8;                        // fragment data must be a multiple of 8 bytes
        List<Fragment> out = new ArrayList<>();
        for (int off = 0; off < data; off += perFrag) {
            int len = Math.min(perFrag, data - off);
            out.add(new Fragment(off / 8, len, off + len < data));
        }
        return out;
    }

    static void mtu() {
        section("7. MTU and fragmentation");
        List<Fragment> frags = fragment(4000, 1500);
        show("4000-byte IPv4 packet (3980 B data) over MTU 1500 -> " + frags);
        check("number of fragments", 3, frags.size());
        check("fragment offsets (in 8-byte units)", "[0, 185, 370]", frags.stream().map(Fragment::offsetUnits).toList().toString());
        check("only the last fragment has More-Fragments = 0", "[true, true, false]", frags.stream().map(Fragment::moreFragments).toList().toString());
        double lossOne = 0.01, lossAll = 1 - Math.pow(1 - lossOne, frags.size());
        check("lose ANY fragment -> the whole packet is lost: 1% per-fragment loss -> ~3% packet loss", "2.97", String.format("%.2f", lossAll * 100));
        int tunnelMtu = 1500 - 20 - 8 - 8 - 14 - 20;                  // e.g. outer IP + UDP + VXLAN-ish headers
        check("tunnels shrink the effective MTU (here 1430): a 1500-byte packet must fragment or be dropped", true, tunnelMtu < 1500);
        int mss = 1500 - 20 - 20;
        check("TCP MSS on a 1500 MTU path = 1500 - 20 (IP) - 20 (TCP)", 1460, mss);
    }

    // ---------------------------------------------------------------------
    // 8. encapsulation overhead
    // ---------------------------------------------------------------------
    static void encapsulation() {
        section("8. Encapsulation overhead");
        int payload = 100;                                          // a small JSON message
        int eth = 14 + 4, ip = 20, tcp = 20, tls = 5 + 16 + 8;      // Ethernet header+FCS, IPv4, TCP, TLS record header+tag(+nonce-ish)
        int onWire = payload + tls + tcp + ip + eth;
        show(String.format("100 B payload -> %d B on the wire (TLS %d, TCP %d, IP %d, Ethernet %d)", onWire, tls, tcp, ip, eth));
        check("efficiency for a 100 B message: 100/187", "53.5%", String.format("%.1f%%", 100.0 * payload / onWire));
        int big = 1400;
        check("efficiency for a 1400 B segment: 1400/1487", "94.1%", String.format("%.1f%%", 100.0 * big / (big + tls + tcp + ip + eth)));
    }

    // ---------------------------------------------------------------------
    // 9. MAC addresses
    // ---------------------------------------------------------------------
    static void mac() {
        section("9. MAC addresses");
        String[] macs = {"3c:22:fb:12:34:56", "01:00:5e:00:00:fb", "ff:ff:ff:ff:ff:ff", "02:42:ac:11:00:02"};
        List<String> kinds = new ArrayList<>();
        for (String m : macs) {
            int first = Integer.parseInt(m.substring(0, 2), 16);
            String cast = m.equals("ff:ff:ff:ff:ff:ff") ? "broadcast" : (first & 1) == 1 ? "multicast" : "unicast";
            String admin = (first & 2) == 2 ? "local" : "universal";
            kinds.add(cast + "/" + admin);
        }
        show("OUI (vendor) of 3c:22:fb:12:34:56 = 3c:22:fb; 02:42:... is what Docker assigns");
        check("bit 0 of the first byte = multicast, bit 1 = locally administered",
              "[unicast/universal, multicast/universal, broadcast/local, unicast/local]", kinds.toString());
    }

    // ---------------------------------------------------------------------
    public static void main(String[] args) {
        delays();
        bdpAndLoss();
        jitter();
        switching();
        mtu();
        encapsulation();
        mac();
        summary();
    }

    // =====================================================================
    // tiny test helpers
    // =====================================================================
    static int passed = 0, total = 0;
    static String f3(double v) { return String.format("%.3f", v); }
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
