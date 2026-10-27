/*
 * ============================================================================
 *  NETWORKING 02 : OSI AND TCP/IP MODELS  (build a real frame, byte by byte)
 * ============================================================================
 *  Run:  java Layers.java          (explanations: README.md in this folder)
 *
 *    1. encapsulation     HTTP text -> TCP segment -> IPv4 packet -> Ethernet frame (real byte layouts)
 *    2. IPv4 checksum     the one's-complement checksum every router verifies
 *    3. decapsulation     parse the frame back, layer by layer
 *    4. PDU names         data / segment / packet / frame / bits
 *    5. layer mapping     which layer each protocol and device lives at
 *    6. troubleshooting   which layer is broken, from the symptom
 * ============================================================================
 */

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Layers {

    // ---------------------------------------------------------------------
    // building headers
    // ---------------------------------------------------------------------
    static byte[] tcpSegment(int srcPort, int dstPort, long seq, long ack, int flags, int window, byte[] payload) {
        ByteBuffer b = ByteBuffer.allocate(20 + payload.length);
        b.putShort((short) srcPort).putShort((short) dstPort);
        b.putInt((int) seq).putInt((int) ack);
        b.put((byte) (5 << 4));                       // data offset = 5 words (20 bytes), no options
        b.put((byte) flags);                          // CWR ECE URG ACK PSH RST SYN FIN
        b.putShort((short) window);
        b.putShort((short) 0).putShort((short) 0);    // checksum (left 0 here), urgent pointer
        b.put(payload);
        return b.array();
    }

    static byte[] ipv4Packet(byte[] src, byte[] dst, int protocol, int ttl, int id, byte[] payload) {
        ByteBuffer b = ByteBuffer.allocate(20 + payload.length);
        b.put((byte) 0x45);                           // version 4, IHL 5 (20 bytes)
        b.put((byte) 0);                              // DSCP/ECN
        b.putShort((short) (20 + payload.length));    // total length
        b.putShort((short) id);
        b.putShort((short) 0x4000);                   // flags: Don't Fragment, offset 0
        b.put((byte) ttl).put((byte) protocol);       // protocol 6 = TCP, 17 = UDP
        b.putShort((short) 0);                        // checksum placeholder
        b.put(src).put(dst);
        byte[] header = Arrays.copyOf(b.array(), 20);
        int c = checksum(header);
        b.putShort(10, (short) c);
        b.position(20);
        b.put(payload);
        return b.array();
    }

    static byte[] ethernetFrame(byte[] dstMac, byte[] srcMac, int etherType, byte[] payload) {
        ByteBuffer b = ByteBuffer.allocate(14 + payload.length);
        b.put(dstMac).put(srcMac).putShort((short) etherType);   // 0x0800 = IPv4, 0x86DD = IPv6, 0x0806 = ARP
        b.put(payload);
        return b.array();                             // (the 4-byte FCS/CRC is added by the NIC)
    }

    /** Internet checksum: one's-complement sum of 16-bit words, then inverted. */
    static int checksum(byte[] h) {
        long sum = 0;
        for (int i = 0; i < h.length; i += 2) sum += ((h[i] & 0xff) << 8) | (h[i + 1] & 0xff);
        while ((sum >> 16) != 0) sum = (sum & 0xffff) + (sum >> 16);      // fold carries back in
        return (int) (~sum & 0xffff);
    }

    static byte[] ip(String s) { byte[] r = new byte[4]; String[] p = s.split("\\."); for (int i = 0; i < 4; i++) r[i] = (byte) Integer.parseInt(p[i]); return r; }
    static byte[] mac(String s) { byte[] r = new byte[6]; String[] p = s.split(":"); for (int i = 0; i < 6; i++) r[i] = (byte) Integer.parseInt(p[i], 16); return r; }
    static String ipStr(ByteBuffer b, int off) { return (b.get(off) & 0xff) + "." + (b.get(off + 1) & 0xff) + "." + (b.get(off + 2) & 0xff) + "." + (b.get(off + 3) & 0xff); }
    static String hex(byte[] b, int from, int to) { StringBuilder s = new StringBuilder(); for (int i = from; i < to; i++) s.append(String.format("%02x ", b[i])); return s.toString().trim(); }

    // ---------------------------------------------------------------------
    static void encapsulation() {
        section("1-3. Encapsulate an HTTP request, then decapsulate it");
        byte[] http = "GET /health HTTP/1.1\r\nHost: api.local\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
        int ACK = 0x10, PSH = 0x08;
        byte[] seg = tcpSegment(51514, 80, 1001, 5001, ACK | PSH, 65535, http);
        byte[] pkt = ipv4Packet(ip("10.0.0.5"), ip("10.0.1.20"), 6, 64, 0x1c46, seg);
        byte[] frame = ethernetFrame(mac("aa:bb:cc:00:00:01"), mac("02:42:ac:11:00:02"), 0x0800, pkt);
        show("application data : " + http.length + " bytes");
        show("TCP segment      : " + seg.length + " bytes (20-byte header)");
        show("IPv4 packet      : " + pkt.length + " bytes (20-byte header)");
        show("Ethernet frame   : " + frame.length + " bytes (14-byte header; NIC adds 4-byte FCS)");
        show("first 34 bytes on the wire: " + hex(frame, 0, 34));
        check("each layer adds its header: 41 + 20 + 20 + 14", 95, frame.length);

        // the receiving side: peel layers off
        ByteBuffer f = ByteBuffer.wrap(frame);
        int etherType = f.getShort(12) & 0xffff;
        check("L2: EtherType 0x0800 says 'the payload is IPv4'", "0x0800", String.format("0x%04x", etherType));
        ByteBuffer p = ByteBuffer.wrap(Arrays.copyOfRange(frame, 14, frame.length));
        int ihl = (p.get(0) & 0x0f) * 4;
        check("L3: version 4, header length 20", "4/20", ((p.get(0) >> 4) & 0xf) + "/" + ihl);
        check("L3: source -> destination IP", "10.0.0.5 -> 10.0.1.20", ipStr(p, 12) + " -> " + ipStr(p, 16));
        check("L3: protocol 6 says 'the payload is TCP'", 6, p.get(9) & 0xff);
        check("L3: TTL starts at 64 (each router decrements; 0 -> dropped, ICMP Time Exceeded)", 64, p.get(8) & 0xff);
        check("L3: header checksum verifies (sum of header incl. checksum = 0xffff -> complement 0)", 0, checksum(Arrays.copyOf(p.array(), 20)));
        byte[] corrupted = Arrays.copyOf(p.array(), 20);
        corrupted[8] = 63;                                      // a router decremented TTL without fixing the checksum
        check("...and detects corruption (routers must recompute it after TTL--)", true, checksum(corrupted) != 0);
        ByteBuffer t = ByteBuffer.wrap(Arrays.copyOfRange(p.array(), ihl, p.array().length));
        check("L4: ports identify the processes: 51514 -> 80", "51514 -> 80", (t.getShort(0) & 0xffff) + " -> " + (t.getShort(2) & 0xffff));
        int flags = t.get(13) & 0xff;
        check("L4: flags ACK+PSH", "ACK PSH", ((flags & 0x10) != 0 ? "ACK " : "") + ((flags & 0x08) != 0 ? "PSH" : ""));
        String app = new String(Arrays.copyOfRange(t.array(), 20, t.array().length), StandardCharsets.US_ASCII);
        check("L7: the application gets back exactly what was sent", true, app.startsWith("GET /health HTTP/1.1"));
    }

    // ---------------------------------------------------------------------
    static void pduAndMapping() {
        section("4-5. PDU names and layer mapping");
        Map<Integer, String> pdu = new LinkedHashMap<>();
        pdu.put(7, "data/message"); pdu.put(4, "segment (TCP) / datagram (UDP)"); pdu.put(3, "packet"); pdu.put(2, "frame"); pdu.put(1, "bits/symbols");
        show("PDU by OSI layer: " + pdu);
        check("PDU at layer 3", "packet", pdu.get(3));
        check("PDU at layer 2", "frame", pdu.get(2));

        Map<String, Integer> osi = new LinkedHashMap<>();
        for (String s : List.of("HTTP", "DNS", "SMTP", "SSH")) osi.put(s, 7);
        osi.put("TCP", 4); osi.put("UDP", 4); osi.put("IP", 3); osi.put("ICMP", 3); osi.put("Ethernet", 2); osi.put("ARP", 2);
        osi.put("switch", 2); osi.put("router", 3); osi.put("L4 load balancer", 4); osi.put("L7 load balancer / API gateway", 7);
        Map<Integer, String> tcpip = Map.of(7, "Application", 6, "Application", 5, "Application", 4, "Transport", 3, "Internet", 2, "Link", 1, "Link");
        show("OSI -> TCP/IP: 7,6,5 -> Application; 4 -> Transport; 3 -> Internet; 2,1 -> Link");
        check("a router works at OSI layer 3 (TCP/IP 'Internet')", "3 Internet", osi.get("router") + " " + tcpip.get(osi.get("router")));
        check("an L7 load balancer reads HTTP (paths, headers, cookies)", 7, osi.get("L7 load balancer / API gateway"));
        check("TCP/IP collapses OSI's 7 layers into 4", 4, new HashSet<>(tcpip.values()).size());
    }

    // ---------------------------------------------------------------------

    static String diagnose(boolean linkUp, boolean pingGateway, boolean dnsResolves, boolean tcpConnects, boolean tlsOk, int httpStatus) {
        if (!linkUp) return "L1/L2: interface down / cable / Wi-Fi / VLAN";
        if (!pingGateway) return "L3: IP config, default gateway, route, ARP";
        if (!dnsResolves) return "L7 (DNS): resolver, record, cache";
        if (!tcpConnects) return "L4: port closed (RST) / firewall (timeout) / service down";
        if (!tlsOk) return "L5-6 (TLS): certificate, SNI, protocol version, clock";
        if (httpStatus >= 500) return "L7: application / upstream error";
        if (httpStatus >= 400) return "L7: client request / auth error";
        return "healthy";
    }

    static void troubleshooting() {
        section("6. Which layer is broken? (bottom-up)");
        check("can't reach the gateway", "L3: IP config, default gateway, route, ARP", diagnose(true, false, false, false, false, 0));
        check("name doesn't resolve but IPs work", "L7 (DNS): resolver, record, cache", diagnose(true, true, false, false, false, 0));
        check("TCP connect fails", "L4: port closed (RST) / firewall (timeout) / service down", diagnose(true, true, true, false, false, 0));
        check("TCP works, handshake errors in TLS", "L5-6 (TLS): certificate, SNI, protocol version, clock", diagnose(true, true, true, true, false, 0));
        check("everything connects, server returns 503", "L7: application / upstream error", diagnose(true, true, true, true, true, 503));
    }

    public static void main(String[] args) {
        encapsulation();
        pduAndMapping();
        troubleshooting();
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
