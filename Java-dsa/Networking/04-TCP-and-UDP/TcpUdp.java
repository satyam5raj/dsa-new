/*
 * ============================================================================
 *  NETWORKING 04 : TCP AND UDP  (real sockets + simulators for what you can't see)
 * ============================================================================
 *  Run:  java TcpUdp.java          (explanations: README.md in this folder)
 *
 *    1. real TCP vs UDP          a byte STREAM (no message boundaries) vs DATAGRAMS (boundaries kept)
 *    2. framing                  length-prefixed messages: how protocols rebuild boundaries on TCP
 *    3. real RST                 "connection refused" = the kernel answered with a reset
 *    4. real connection cost     new connection per request vs one reused connection
 *    5. state machine            handshake, data, FIN teardown, TIME_WAIT, RST
 *    6. sequence numbers         reorder, de-duplicate, cumulative ACKs
 *    7. reliable delivery        Go-Back-N vs Selective Repeat over a lossy channel
 *    8. congestion control       slow start, congestion avoidance, fast recovery, timeout (Reno)
 *    9. flow control             receive window: a slow reader throttles the sender (zero window)
 *   10. Nagle + delayed ACK      the classic 40 ms stall, and TCP_NODELAY
 *   11. keepalive vs NAT         idle connections silently dropped; half-open connections
 *   12. head-of-line blocking    one lost segment stalls everything behind it
 * ============================================================================
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class TcpUdp {

    // ---------------------------------------------------------------------
    // 1-2. real TCP stream vs real UDP datagrams, and framing
    // ---------------------------------------------------------------------
    static void streamVsDatagram() throws Exception {
        section("1-2. Real sockets: TCP stream vs UDP datagrams, and framing");
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService ex = Executors.newSingleThreadExecutor();
            Future<byte[]> received = ex.submit(() -> {
                try (Socket s = server.accept()) { return s.getInputStream().readAllBytes(); }
            });
            try (Socket c = new Socket("127.0.0.1", server.getLocalPort())) {
                OutputStream out = c.getOutputStream();
                for (String m : List.of("hello", "world", "!")) out.write(m.getBytes(StandardCharsets.UTF_8));   // 3 separate writes
            }
            String got = new String(received.get(5, TimeUnit.SECONDS), StandardCharsets.UTF_8);
            check("TCP: three writes arrive as ONE stream of bytes (no boundaries)", "helloworld!", got);
            ex.shutdown();
        }

        try (DatagramSocket server = new DatagramSocket(0, InetAddress.getLoopbackAddress());
             DatagramSocket client = new DatagramSocket()) {
            server.setSoTimeout(2000);
            for (String m : List.of("hello", "world", "!")) {
                byte[] b = m.getBytes(StandardCharsets.UTF_8);
                client.send(new DatagramPacket(b, b.length, InetAddress.getLoopbackAddress(), server.getLocalPort()));
            }
            List<String> msgs = new ArrayList<>();
            byte[] buf = new byte[1500];
            for (int i = 0; i < 3; i++) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                server.receive(p);
                msgs.add(new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8));
            }
            check("UDP: each send is one datagram, boundaries preserved", "[hello, world, !]", msgs.toString());
        }

        // framing on TCP: 4-byte length prefix, like HTTP/2 frames, gRPC, Kafka, Postgres
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(wire);
        for (String m : List.of("hello", "world", "!")) { byte[] b = m.getBytes(StandardCharsets.UTF_8); dos.writeInt(b.length); dos.write(b); }
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(wire.toByteArray()));
        List<String> back = new ArrayList<>();
        while (dis.available() > 0) { byte[] b = new byte[dis.readInt()]; dis.readFully(b); back.add(new String(b, StandardCharsets.UTF_8)); }
        check("length-prefix framing rebuilds message boundaries on a stream", "[hello, world, !]", back.toString());
    }

    // ---------------------------------------------------------------------
    // 3. connection refused (RST)
    // ---------------------------------------------------------------------
    static void refused() throws Exception {
        section("3. Real RST: connecting to a port where nothing listens");
        int port;
        try (ServerSocket tmp = new ServerSocket(0)) { port = tmp.getLocalPort(); }    // grab a free port, then close it
        String result;
        long t0 = System.nanoTime();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 2000);
            result = "connected?!";
        } catch (ConnectException e) {
            result = "ConnectException: " + e.getMessage();
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        show(String.format("%s after %.2f ms", result, ms));
        check("the kernel answers SYN with RST -> 'Connection refused', immediately", true, result.contains("refused") && ms < 1000);
    }

    // ---------------------------------------------------------------------
    // 4. new connection per request vs a reused connection
    // ---------------------------------------------------------------------
    static void connectionCost() throws Exception {
        section("4. Real cost of connection setup: new connection per request vs reuse");
        int n = 300;
        try (ServerSocket server = new ServerSocket(0, 512)) {
            ExecutorService pool = Executors.newCachedThreadPool();
            pool.submit(() -> {
                while (!server.isClosed()) {
                    try {
                        Socket s = server.accept();
                        pool.submit(() -> {                           // echo one byte per request until the client closes
                            try (s; InputStream in = s.getInputStream(); OutputStream out = s.getOutputStream()) {
                                int b;
                                while ((b = in.read()) != -1) { out.write(b); out.flush(); }
                            } catch (IOException ignored) { }
                        });
                    } catch (IOException e) { return; }
                }
            });
            int port = server.getLocalPort();
            for (int i = 0; i < 50; i++) try (Socket s = new Socket("127.0.0.1", port)) { s.getOutputStream().write(1); s.getInputStream().read(); }   // warm-up

            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                try (Socket s = new Socket("127.0.0.1", port)) {     // handshake + request + teardown every time
                    s.setTcpNoDelay(true);
                    s.getOutputStream().write(1);
                    s.getInputStream().read();
                }
            }
            double fresh = (System.nanoTime() - t0) / 1e6;
            t0 = System.nanoTime();
            try (Socket s = new Socket("127.0.0.1", port)) {           // one connection, n requests (keep-alive / pooling)
                s.setTcpNoDelay(true);
                OutputStream out = s.getOutputStream();
                InputStream in = s.getInputStream();
                for (int i = 0; i < n; i++) { out.write(1); in.read(); }
            }
            double reused = (System.nanoTime() - t0) / 1e6;
            show(String.format("%d requests on localhost: new connection each time %.1f ms, one reused connection %.1f ms (%.1fx)", n, fresh, reused, fresh / reused));
            check("reusing a connection is faster even on localhost (RTT ~0!)", true, reused < fresh);
            show("over a real network each new connection also costs 1 RTT (TCP) + 1-2 RTT (TLS): 60-200 ms across regions");
            server.close();
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------------
    // 5. state machine
    // ---------------------------------------------------------------------
    static void stateMachine() {
        section("5. TCP state machine: handshake, teardown, TIME_WAIT, RST");
        List<String> log = new ArrayList<>();
        String client = "CLOSED", server = "LISTEN";
        client = "SYN_SENT";      log.add("C->S SYN seq=x            client " + client);
        server = "SYN_RECEIVED";  log.add("S->C SYN+ACK seq=y ack=x+1  server " + server);
        client = "ESTABLISHED";   log.add("C->S ACK ack=y+1          client " + client);
        server = "ESTABLISHED";   log.add("                          server " + server);
        check("3-way handshake ends with both sides ESTABLISHED", "ESTABLISHED/ESTABLISHED", client + "/" + server);
        client = "FIN_WAIT_1";    log.add("C->S FIN (client closes first: the ACTIVE closer)  " + client);
        server = "CLOSE_WAIT";    log.add("S->C ACK                  server " + server + " (app must call close()!)");
        client = "FIN_WAIT_2";
        server = "LAST_ACK";      log.add("S->C FIN                  server " + server);
        client = "TIME_WAIT";     log.add("C->S ACK                  client " + client + " for 2*MSL (60 s on Linux)");
        server = "CLOSED";
        log.forEach(TcpUdp::show);
        check("the side that closes FIRST ends in TIME_WAIT", "TIME_WAIT", client);
        long teardownSegments = log.stream().skip(4).filter(l -> l.startsWith("C->S") || l.startsWith("S->C")).count();
        check("teardown uses 4 segments (FIN, ACK, FIN, ACK)", 4, teardownSegments);
        // a buggy server app that never calls close(): after the client's FIN it stays in CLOSE_WAIT forever
        String buggyServer = "ESTABLISHED";
        boolean appCallsClose = false;
        buggyServer = "CLOSE_WAIT";                                   // kernel ACKs the peer's FIN
        if (appCallsClose) buggyServer = "LAST_ACK";
        check("app never calls close() -> the socket stays in CLOSE_WAIT (a descriptor leak)", "CLOSE_WAIT", buggyServer);
        int perSecond = 500, timeWaitSeconds = 60;
        show(String.format("a client opening %d short connections/s to one server keeps %,d sockets in TIME_WAIT", perSecond, perSecond * timeWaitSeconds));
        check("ephemeral ports in use by TIME_WAIT (Linux range 32768-60999 has 28,232)", true, perSecond * timeWaitSeconds > 28232 - 1000);
    }

    // ---------------------------------------------------------------------
    // 6. sequence numbers: reordering and duplicates
    // ---------------------------------------------------------------------
    static void sequenceNumbers() {
        section("6. Sequence numbers: reorder, de-duplicate, cumulative ACK");
        record Seg(int seq, String data) { }
        List<Seg> arrivals = List.of(new Seg(0, "HEL"), new Seg(6, "WOR"), new Seg(3, "LO "), new Seg(3, "LO "), new Seg(9, "LD"));
        TreeMap<Integer, String> outOfOrder = new TreeMap<>();
        StringBuilder delivered = new StringBuilder();
        int expected = 0;
        List<Integer> acks = new ArrayList<>();
        int dups = 0;
        for (Seg s : arrivals) {
            if (s.seq() < expected || outOfOrder.containsKey(s.seq())) { dups++; acks.add(expected); continue; }   // duplicate: re-ACK
            outOfOrder.put(s.seq(), s.data());
            while (outOfOrder.containsKey(expected)) { String d = outOfOrder.remove(expected); delivered.append(d); expected += d.length(); }
            acks.add(expected);                                     // cumulative ACK = next byte expected
        }
        show("arrival order (seq): 0, 6, 3, 3(dup), 9;  ACKs sent: " + acks);
        check("the app receives bytes in order despite reordering", "HELLO WORLD", delivered.toString());
        check("the duplicate is discarded", 1, dups);
        check("cumulative ACKs: segment 6 arriving early gets a DUPLICATE ack (3)", "[3, 3, 9, 9, 11]", acks.toString());
    }

    // ---------------------------------------------------------------------
    // 7. Go-Back-N vs Selective Repeat
    // ---------------------------------------------------------------------
    static int transmissions(String mode, int packets, int window, double loss, long seed) {
        Random r = new Random(seed);
        boolean[] acked = new boolean[packets];
        int base = 0, sent = 0;
        while (base < packets) {
            int end = Math.min(base + window, packets);
            int firstLost = -1;
            for (int i = base; i < end; i++) {
                if (mode.equals("SR") && acked[i]) continue;          // selective repeat only resends what's missing
                sent++;
                if (r.nextDouble() < loss) { if (firstLost < 0) firstLost = i; }
                else if (mode.equals("SR") || firstLost < 0) acked[i] = true;   // GBN receiver discards anything after a gap
            }
            while (base < packets && acked[base]) base++;
        }
        return sent;
    }

    static void reliability() {
        section("7. Reliable delivery over a lossy channel");
        int gbn = 0, sr = 0;
        for (long s = 1; s <= 20; s++) { gbn += transmissions("GBN", 1000, 16, 0.05, s); sr += transmissions("SR", 1000, 16, 0.05, s); }
        gbn /= 20; sr /= 20;
        show(String.format("1000 packets, window 16, 5%% loss: Go-Back-N sends %d, Selective Repeat sends %d (ideal ~%d)", gbn, sr, (int) (1000 / 0.95)));
        check("selective repeat (TCP SACK) wastes far less bandwidth than go-back-N", true, sr < gbn * 0.8);
        check("selective repeat is close to the minimum 1000 / (1 - loss)", true, Math.abs(sr - 1000 / 0.95) < 30);
    }

    // ---------------------------------------------------------------------
    // 8. congestion control (Reno)
    // ---------------------------------------------------------------------
    static void congestion() {
        section("8. Congestion control (TCP Reno): cwnd per RTT");
        double cwnd = 1, ssthresh = 64;
        Set<Integer> tripleDupAck = Set.of(12), timeout = Set.of(20);
        List<String> trace = new ArrayList<>();
        List<Double> series = new ArrayList<>();
        for (int rtt = 0; rtt < 26; rtt++) {
            series.add(cwnd);
            trace.add(String.valueOf((int) cwnd));
            if (tripleDupAck.contains(rtt)) { ssthresh = cwnd / 2; cwnd = ssthresh; }       // fast retransmit + fast recovery: halve
            else if (timeout.contains(rtt)) { ssthresh = cwnd / 2; cwnd = 1; }             // timeout: back to 1 (slow start)
            else if (cwnd < ssthresh) cwnd = Math.min(cwnd * 2, ssthresh);                 // slow start: double per RTT
            else cwnd += 1;                                                                // congestion avoidance: +1 MSS per RTT
        }
        show("cwnd (segments) per RTT: " + String.join(" ", trace));
        check("slow start doubles: 1 2 4 8 16 32 64", "[1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0]", series.subList(0, 7).toString());
        check("then linear growth (+1 per RTT) above ssthresh", 69.0, series.get(11));
        check("3 duplicate ACKs (mild loss) -> halve, no slow start (fast recovery)", 35.0, series.get(13));
        check("timeout (severe) -> cwnd 1, slow start again", 1.0, series.get(21));
        check("the 'sawtooth': AIMD = additive increase, multiplicative decrease", true, series.get(19) > series.get(13));
        show("modern Linux default is CUBIC; Google's BBR models bandwidth and RTT instead of reacting to loss");
    }

    // ---------------------------------------------------------------------
    // 9. flow control
    // ---------------------------------------------------------------------
    static void flowControl() {
        section("9. Flow control: the receive window");
        int buffer = 64, inBuffer = 0, sent = 0;
        List<Integer> windows = new ArrayList<>();
        int senderRate = 32, appReadRate = 8;                         // KB per tick
        for (int tick = 0; tick < 20; tick++) {
            int window = buffer - inBuffer;                           // advertised rwnd
            windows.add(window);
            int send = Math.min(senderRate, window);
            inBuffer += send;
            sent += send;
            boolean appPaused = tick >= 5 && tick < 10;               // e.g. a GC pause or a blocked consumer thread
            if (!appPaused) inBuffer -= Math.min(appReadRate, inBuffer);
        }
        show("advertised window per tick (KB): " + windows);
        show(String.format("sender wants 32 KB/tick, app reads 8 KB/tick (paused ticks 5-9), 64 KB buffer: sent %d KB in 20 ticks", sent));
        check("while the reader is paused the window hits ZERO and the sender must stop", true, windows.subList(5, 11).contains(0));
        check("throughput is capped by the RECEIVER's reading rate, not the sender's 32 KB/tick", true, sent <= 64 + 15 * 8);
        int rwnd = 64, cwndAfterLoss = 10, rttMs = 50;
        check("effective window = min(rwnd, cwnd): after a loss the NETWORK limits it", 10, Math.min(rwnd, cwndAfterLoss));
        check("...throughput = window / RTT = 10 KB / 50 ms = 200 KB/s", 200, Math.min(rwnd, cwndAfterLoss) * 1000 / rttMs);
    }

    // ---------------------------------------------------------------------
    // 10. Nagle + delayed ACK
    // ---------------------------------------------------------------------
    static double requestLatencyMs(boolean nagle, boolean delayedAck, double rttMs) {
        // client sends a request as two writes (header, then body) and waits for the reply
        double t = 0;
        // first write goes out immediately
        if (nagle) {
            // Nagle: hold the 2nd small write until the 1st is ACKed.
            // Delayed ACK: the server waits up to 40 ms hoping to piggyback the ACK on a response
            // -- but it can't respond until it has the body: deadlock until the delayed-ACK timer fires.
            double ackArrives = delayedAck ? 40 + rttMs / 2 : rttMs;
            t = ackArrives + rttMs / 2;                               // then the body goes out
        } else {
            t = rttMs / 2;                                            // both writes go immediately
        }
        return t + rttMs / 2;                                         // response travels back
    }

    static void nagle() throws Exception {
        section("10. Nagle's algorithm + delayed ACK");
        double bad = requestLatencyMs(true, true, 1), fixed = requestLatencyMs(false, true, 1);
        show(String.format("write-write-read pattern, 1 ms RTT: Nagle + delayed ACK %.1f ms, TCP_NODELAY %.1f ms", bad, fixed));
        check("the classic stall: ~40 ms added to a 1 ms request", true, bad > 40 && fixed < 2);
        try (Socket s = new Socket()) {
            s.setTcpNoDelay(true);
            check("Java sockets expose TCP_NODELAY (most RPC/DB clients enable it)", true, s.getTcpNoDelay());
        }
        show("better fix: build the whole request in a buffer and write it ONCE");
    }

    // ---------------------------------------------------------------------
    // 11. keepalive, NAT idle timeouts, half-open connections
    // ---------------------------------------------------------------------
    static void keepalive() throws Exception {
        section("11. Keepalive, NAT idle timeouts, half-open connections");
        int natIdleTimeout = 350;                                     // seconds (e.g. a cloud NAT gateway)
        int linuxKeepaliveStart = 7200;                               // default tcp_keepalive_time = 2 hours!
        int appKeepalive = 60;
        check("default OS keepalive (2 h) fires long after the NAT forgot the flow (350 s)", true, linuxKeepaliveStart > natIdleTimeout);
        check("an app/pool keepalive of 60 s keeps the NAT mapping alive", true, appKeepalive < natIdleTimeout);
        show("after the NAT drops state, the next request on the pooled connection gets RST or silently times out");
        show("half-open: one side crashed/rebooted; the other still thinks the connection is ESTABLISHED until it sends");
        try (Socket s = new Socket()) {
            s.setKeepAlive(true);
            check("Java can enable SO_KEEPALIVE on a socket", true, s.getKeepAlive());
        }
    }

    // ---------------------------------------------------------------------
    // 12. head-of-line blocking
    // ---------------------------------------------------------------------
    static void holBlocking() {
        section("12. Head-of-line blocking");
        // 4 independent streams (e.g. 4 HTTP/2 requests) share one TCP connection; segment 2 (stream B) is lost
        String[] segs = {"A1", "B1", "C1", "D1", "A2", "B2", "C2", "D2"};
        int lost = 1, retransmitAt = 10;                              // arrives late, at t = 10
        Map<String, Integer> tcpDone = new TreeMap<>(), quicDone = new TreeMap<>();
        for (int i = 0; i < segs.length; i++) {
            int arrival = i == lost ? retransmitAt : i;
            String stream = segs[i].substring(0, 1);
            int tcpDeliver = i > lost ? Math.max(arrival, retransmitAt) : arrival;   // TCP: in-order byte stream, all wait
            tcpDone.merge(stream, tcpDeliver, Math::max);
            quicDone.merge(stream, arrival, Math::max);                              // QUIC: per-stream ordering only
        }
        show("time each stream completes  TCP (HTTP/2): " + tcpDone + "   QUIC (HTTP/3): " + quicDone);
        check("on TCP, ONE lost packet delays every stream", "{A=10, B=10, C=10, D=10}", tcpDone.toString());
        check("on QUIC only stream B waits for the retransmission", "{A=4, B=10, C=6, D=7}", quicDone.toString());
    }

    public static void main(String[] args) throws Exception {
        streamVsDatagram();
        refused();
        connectionCost();
        stateMachine();
        sequenceNumbers();
        reliability();
        congestion();
        flowControl();
        nagle();
        keepalive();
        holBlocking();
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
