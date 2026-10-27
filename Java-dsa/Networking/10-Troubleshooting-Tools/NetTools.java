/*
 * ============================================================================
 *  NETWORKING 10 : TROUBLESHOOTING TOOLS  (what each tool tells you, made concrete)
 * ============================================================================
 *  Run:  java NetTools.java          (explanations: README.md in this folder)
 *
 *    1. nc -vz                port open vs connection refused (real sockets)
 *    2. timeouts              "connected but the app never answers" -> read timeout (real)
 *    3. curl -w timing        DNS / connect / time-to-first-byte / total against a slow server (real)
 *    4. iperf3-style          raw TCP throughput on localhost (real)
 *    5. traceroute            how TTL expiry reveals each hop; why some hops show * * *
 *    6. ss                    reading socket states: LISTEN, ESTAB, TIME_WAIT, CLOSE_WAIT
 *    7. dig                   status, answer, TTL; NXDOMAIN vs SERVFAIL
 *    8. tcpdump               spotting retransmissions and resets in a capture
 *    9. openssl s_client      certificate subject, issuer, expiry, verify result
 * ============================================================================
 */

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class NetTools {

    // ---------------------------------------------------------------------
    // 1. nc -vz host port
    // ---------------------------------------------------------------------
    static String nc(String host, int port, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return "succeeded";
        } catch (ConnectException e) {
            return "refused";                                            // RST: host up, nothing listening
        } catch (SocketTimeoutException e) {
            return "timed out";                                          // silence: firewall drop / host down / no route
        } catch (IOException e) {
            return "error: " + e.getMessage();
        }
    }

    static void portCheck() throws Exception {
        section("1. nc -vz: is the port reachable?");
        try (ServerSocket open = new ServerSocket(0)) {
            int closed;
            try (ServerSocket tmp = new ServerSocket(0)) { closed = tmp.getLocalPort(); }
            check("listening port -> 'succeeded'", "succeeded", nc("127.0.0.1", open.getLocalPort(), 1000));
            check("closed port -> 'refused' immediately (TCP RST)", "refused", nc("127.0.0.1", closed, 1000));
        }
        show("'timed out' instead means packets vanish: security group / firewall DROP, wrong route, host down");
    }

    // ---------------------------------------------------------------------
    // 2. connected, but no response
    // ---------------------------------------------------------------------
    static void readTimeout() throws Exception {
        section("2. Connected but hung: read timeout");
        try (ServerSocket hung = new ServerSocket(0)) {                   // accepts (kernel completes the handshake) but never replies
            String result;
            long t0 = System.nanoTime();
            try (Socket s = new Socket("127.0.0.1", hung.getLocalPort())) {
                s.setSoTimeout(300);
                s.getOutputStream().write("GET / HTTP/1.1\r\nHost: x\r\n\r\n".getBytes());
                s.getInputStream().read();
                result = "got data?!";
            } catch (SocketTimeoutException e) { result = "read timed out"; }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            show(String.format("%s after %d ms", result, ms));
            check("connect succeeds, but no byte arrives -> read timeout (app hung / overloaded / deadlocked)", true, result.equals("read timed out") && ms >= 290);
        }
        show("the kernel accepted the TCP connection into the backlog even though the app never called accept()");
    }

    // ---------------------------------------------------------------------
    // 3. curl -w timing breakdown
    // ---------------------------------------------------------------------
    static void curlTiming() throws Exception {
        section("3. curl -w style timing breakdown");
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService ex = Executors.newSingleThreadExecutor();
            ex.submit(() -> {
                try (Socket c = server.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    while (!in.readLine().isEmpty()) { }                  // read request headers
                    Thread.sleep(120);                                    // slow backend: 120 ms of "server processing"
                    OutputStream o = c.getOutputStream();
                    o.write("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\n".getBytes());
                    o.flush();
                    Thread.sleep(30);                                     // body trickles in later
                    o.write("hello".getBytes());
                    o.flush();
                } catch (Exception ignored) { }
                return null;
            });
            long t0 = System.nanoTime();
            InetAddress addr = InetAddress.getByName("localhost");
            long tDns = System.nanoTime();
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(addr, server.getLocalPort()), 1000);
                long tConnect = System.nanoTime();
                s.getOutputStream().write(("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes());
                InputStream in = s.getInputStream();
                in.read();
                long tFirstByte = System.nanoTime();
                in.readAllBytes();
                long tTotal = System.nanoTime();
                double dns = (tDns - t0) / 1e6, connect = (tConnect - t0) / 1e6, ttfb = (tFirstByte - t0) / 1e6, total = (tTotal - t0) / 1e6;
                show(String.format("time_namelookup %.1f ms | time_connect %.1f ms | time_starttransfer %.1f ms | time_total %.1f ms", dns, connect, ttfb, total));
                check("time to first byte includes the server's 120 ms of processing", true, ttfb - connect >= 115);
                check("time_total - time_starttransfer = body transfer (~30 ms here)", true, total - ttfb >= 25);
                check("connect on localhost is ~instant (cross-region it would be one RTT)", true, connect - dns < 50);
            }
            ex.shutdownNow();
        }
        show("real command: curl -o /dev/null -s -w '%{time_namelookup} %{time_connect} %{time_appconnect} %{time_starttransfer} %{time_total}\\n' URL");
    }

    // ---------------------------------------------------------------------
    // 4. throughput
    // ---------------------------------------------------------------------
    static void throughput() throws Exception {
        section("4. iperf3-style throughput test (localhost)");
        long bytes = 200L * 1024 * 1024;
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService ex = Executors.newSingleThreadExecutor();
            Future<Long> received = ex.submit(() -> {
                long n = 0;
                byte[] buf = new byte[1 << 16];
                try (Socket c = server.accept(); InputStream in = c.getInputStream()) { int r; while ((r = in.read(buf)) > 0) n += r; }
                return n;
            });
            long t0 = System.nanoTime();
            try (Socket s = new Socket("127.0.0.1", server.getLocalPort()); OutputStream out = s.getOutputStream()) {
                byte[] chunk = new byte[1 << 16];
                for (long sent = 0; sent < bytes; sent += chunk.length) out.write(chunk);
            }
            long got = received.get(30, TimeUnit.SECONDS);
            double secs = (System.nanoTime() - t0) / 1e9;
            double gbps = got * 8 / secs / 1e9;
            show(String.format("sent 200 MB in %.2f s = %.1f Gbit/s over loopback", secs, gbps));
            check("all bytes arrived", bytes, got);
            check("loopback is far faster than any real NIC path (> 1 Gbit/s): the bottleneck is elsewhere", true, gbps > 1);
            ex.shutdown();
        }
        show("real usage: 'iperf3 -s' on one host, 'iperf3 -c <host> -P 4 -t 30' on the other; compare with expected link speed");
    }

    // ---------------------------------------------------------------------
    // 5. traceroute
    // ---------------------------------------------------------------------
    record Hop(String ip, boolean answersIcmp, int rttMs) { }

    static void traceroute() {
        section("5. traceroute: TTL = 1, 2, 3 ... each router that drops a packet reports back");
        List<Hop> path = List.of(new Hop("192.168.1.1", true, 1), new Hop("100.64.0.1", true, 8), new Hop("203.0.113.9", false, 0),
                                 new Hop("198.51.100.20", true, 41), new Hop("93.184.216.34", true, 43));
        List<String> lines = new ArrayList<>();
        for (int ttl = 1; ttl <= path.size(); ttl++) {
            Hop h = path.get(ttl - 1);                                    // the packet dies at hop 'ttl' -> ICMP Time Exceeded from that router
            lines.add(ttl + "  " + (h.answersIcmp() ? h.ip() + "  " + h.rttMs() + " ms" : "* * *"));
        }
        lines.forEach(NetTools::show);
        check("a silent hop (ICMP rate-limited/filtered) shows * * * but later hops still answer -> NOT the failure point", "* * *", lines.get(2).substring(3));
        check("the big latency jump (8 -> 41 ms) marks the long-haul link", 33, path.get(3).rttMs() - path.get(1).rttMs());
        show("mtr = traceroute + ping per hop over time: loss at a middle hop that does NOT continue to the end is just ICMP de-prioritisation");
    }

    // ---------------------------------------------------------------------
    // 6. ss
    // ---------------------------------------------------------------------
    static final String SS_SAMPLE = """
        State      Recv-Q Send-Q Local Address:Port   Peer Address:Port
        LISTEN     0      4096   0.0.0.0:8080         0.0.0.0:*
        LISTEN     129    128    127.0.0.1:5432       0.0.0.0:*
        ESTAB      0      0      10.0.1.5:8080        10.0.0.9:51010
        ESTAB      0      87040  10.0.1.5:44120       10.0.3.7:6379
        CLOSE-WAIT 1      0      10.0.1.5:51322       10.0.2.4:443
        CLOSE-WAIT 1      0      10.0.1.5:51323       10.0.2.4:443
        CLOSE-WAIT 1      0      10.0.1.5:51324       10.0.2.4:443
        TIME-WAIT  0      0      10.0.1.5:51400       10.0.2.4:443
        TIME-WAIT  0      0      10.0.1.5:51401       10.0.2.4:443
        """;

    static void ss() {
        section("6. ss -tan: reading socket states");
        Map<String, Integer> states = new TreeMap<>();
        List<String> findings = new ArrayList<>();
        for (String line : SS_SAMPLE.strip().split("\n")) {
            String[] f = line.trim().split("\\s+");
            if (f[0].equals("State")) continue;
            states.merge(f[0], 1, Integer::sum);
            int recvQ = Integer.parseInt(f[1]), sendQ = Integer.parseInt(f[2]);
            if (f[0].equals("LISTEN") && recvQ > sendQ) findings.add("accept queue overflow on " + f[3]);   // LISTEN: Recv-Q = pending, Send-Q = backlog
            if (f[0].equals("ESTAB") && sendQ > 65536) findings.add("send queue backing up to " + f[4]);
        }
        show("state counts: " + states);
        findings.forEach(x -> show("finding: " + x));
        check("CLOSE-WAIT sockets = our app never closed connections the peer closed (leak)", 3, states.get("CLOSE-WAIT"));
        check("LISTEN with Recv-Q > Send-Q: accept queue full -> new SYNs dropped", true, findings.contains("accept queue overflow on 127.0.0.1:5432"));
        check("ESTAB with a large Send-Q: the peer (Redis here) isn't reading fast enough / network slow", true, findings.stream().anyMatch(x -> x.contains("10.0.3.7:6379")));
        show("useful: ss -ltnp (listeners + process), ss -tan state time-wait | wc -l, ss -ti (rtt, cwnd, retrans per socket)");
    }

    // ---------------------------------------------------------------------
    // 7. dig
    // ---------------------------------------------------------------------
    static final String DIG_OK = """
        ;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 4127
        ;; ANSWER SECTION:
        api.example.com.        42      IN      CNAME   api.lb.example.net.
        api.lb.example.net.     12      IN      A       203.0.113.10
        ;; Query time: 23 msec
        ;; SERVER: 10.0.0.2#53(10.0.0.2)
        """;
    static final String DIG_FAIL = ";; ->>HEADER<<- opcode: QUERY, status: SERVFAIL, id: 5512\n;; Query time: 5003 msec\n";

    static void dig() {
        section("7. dig: reading the answer");
        Matcher st = Pattern.compile("status: (\\w+)").matcher(DIG_OK);
        st.find();
        check("status NOERROR", "NOERROR", st.group(1));
        List<String> answers = new ArrayList<>();
        Matcher m = Pattern.compile("^(\\S+)\\s+(\\d+)\\s+IN\\s+(\\w+)\\s+(\\S+)$", Pattern.MULTILINE).matcher(DIG_OK);
        int minTtl = Integer.MAX_VALUE;
        while (m.find()) { answers.add(m.group(3) + " " + m.group(4)); minTtl = Math.min(minTtl, Integer.parseInt(m.group(2))); }
        check("CNAME chain to the load balancer's A record", "[CNAME api.lb.example.net., A 203.0.113.10]", answers.toString());
        check("remaining TTL (seconds until this resolver re-asks)", 12, minTtl);
        Matcher f = Pattern.compile("status: (\\w+)[\\s\\S]*Query time: (\\d+)").matcher(DIG_FAIL);
        f.find();
        check("SERVFAIL after ~5 s = upstream/authoritative problem or DNSSEC failure (not 'name missing')", "SERVFAIL 5003", f.group(1) + " " + f.group(2));
        show("compare: dig @10.0.0.2 name   vs   dig @8.8.8.8 name   vs   dig +trace name (walk from the root yourself)");
    }

    // ---------------------------------------------------------------------
    // 8. tcpdump
    // ---------------------------------------------------------------------
    static final String TCPDUMP = """
        10:00:00.000100 IP 10.0.1.5.51515 > 10.0.2.4.443: Flags [S], seq 1000, win 64240
        10:00:01.003200 IP 10.0.1.5.51515 > 10.0.2.4.443: Flags [S], seq 1000, win 64240
        10:00:03.007100 IP 10.0.1.5.51515 > 10.0.2.4.443: Flags [S], seq 1000, win 64240
        10:00:03.040300 IP 10.0.2.4.443 > 10.0.1.5.51515: Flags [S.], seq 7000, ack 1001, win 65160
        10:00:03.040400 IP 10.0.1.5.51515 > 10.0.2.4.443: Flags [.], ack 7001, win 502
        10:00:03.041000 IP 10.0.1.5.51515 > 10.0.2.4.443: Flags [P.], seq 1001:1518, ack 7001, length 517
        10:00:03.250900 IP 10.0.1.5.51515 > 10.0.2.4.443: Flags [P.], seq 1001:1518, ack 7001, length 517
        10:00:03.290000 IP 10.0.2.4.443 > 10.0.1.5.51515: Flags [R], seq 7001
        """;

    static void tcpdump() {
        section("8. tcpdump: retransmissions and resets");
        Map<String, Integer> seen = new HashMap<>();
        List<String> retrans = new ArrayList<>();
        int resets = 0;
        Pattern p = Pattern.compile("^(\\S+) IP (\\S+) > (\\S+): Flags \\[(\\S+)\\], seq ([^,\\s]+)");
        for (String line : TCPDUMP.strip().split("\n")) {
            Matcher m = p.matcher(line.trim());
            if (!m.find()) continue;
            if (m.group(4).equals("R")) resets++;
            String key = m.group(2) + ">" + m.group(3) + " " + m.group(4) + " " + m.group(5);
            if (seen.merge(key, 1, Integer::sum) > 1) retrans.add(m.group(1) + " " + m.group(4) + " seq " + m.group(5));
        }
        retrans.forEach(r -> show("retransmission: " + r));
        check("SYN retransmitted at +1 s and +3 s (exponential backoff): the classic 3-second connect", 2, retrans.stream().filter(r -> r.contains(" S seq")).count());
        check("a data segment retransmitted after ~210 ms (RTO): packet loss on the path", 1, retrans.stream().filter(r -> r.contains("P.")).count());
        check("the server then reset the connection (Flags [R])", 1, resets);
        show("capture: sudo tcpdump -ni eth0 'host 10.0.2.4 and port 443' -w cap.pcap ; open in Wireshark (Analyze > Expert Info)");
    }

    // ---------------------------------------------------------------------
    // 9. openssl s_client
    // ---------------------------------------------------------------------
    static final String OPENSSL = """
        CONNECTED(00000003)
        depth=2 C = US, O = Example Trust, CN = Example Root CA
        depth=1 C = US, O = Example Trust, CN = Example Issuing CA
        depth=0 CN = api.example.com
        ---
        subject=CN = api.example.com
        issuer=C = US, O = Example Trust, CN = Example Issuing CA
        notBefore=Sep  1 00:00:00 2026 GMT
        notAfter=Nov  5 23:59:59 2026 GMT
        ---
        Protocol  : TLSv1.3
        Verify return code: 0 (ok)
        """;

    static void openssl() {
        section("9. openssl s_client -connect host:443 -servername host -showcerts");
        String notAfter = OPENSSL.lines().filter(l -> l.trim().startsWith("notAfter=")).findFirst().get().trim().substring("notAfter=".length());
        LocalDateTime expiry = LocalDateTime.parse(notAfter.replaceAll("\\s+", " ").replace(" GMT", ""), DateTimeFormatter.ofPattern("MMM d HH:mm:ss yyyy", Locale.ENGLISH));
        long daysLeft = Duration.between(LocalDateTime.of(2026, 10, 27, 0, 0), expiry).toDays();
        check("chain depth 0..2: leaf, intermediate, root", 3, OPENSSL.lines().filter(l -> l.startsWith("depth=")).count());
        check("verify return code 0 = chain and hostname OK", true, OPENSSL.contains("Verify return code: 0 (ok)"));
        check("days until expiry (alert if < 14)", 9L, daysLeft);
        check("negotiated TLS 1.3", true, OPENSSL.contains("TLSv1.3"));
        show("always pass -servername (SNI): without it a shared IP may hand you the WRONG certificate");
    }

    public static void main(String[] args) throws Exception {
        portCheck();
        readTimeout();
        curlTiming();
        throughput();
        traceroute();
        ss();
        dig();
        tcpdump();
        openssl();
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
