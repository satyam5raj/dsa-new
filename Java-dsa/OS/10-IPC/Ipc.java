/*
 * ============================================================================
 *  OS 10 : INTER-PROCESS COMMUNICATION  (real pipes, FIFOs, Unix sockets, shared memory)
 * ============================================================================
 *  Run:  java Ipc.java          (explanations: README.md in this folder)
 *
 *    1. anonymous pipe         parent <-> child through stdin/stdout; a shell pipeline (real)
 *    2. named pipe (FIFO)      a pipe with a name in the file system (real mkfifo)
 *    3. Unix domain socket     a socket addressed by a file path; vs TCP loopback latency (real)
 *    4. shared memory          two processes map the same file and see each other's writes (real)
 *    5. message queue          producer/consumer message passing with a bounded queue
 *    6. signals                the smallest IPC: "something happened" (real SIGTERM / SIGUSR1 to a child)
 * ============================================================================
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Ipc {

    static Path dir;
    static String java;

    // ---------------------------------------------------------------------
    // 1. anonymous pipes
    // ---------------------------------------------------------------------
    static void pipes() throws Exception {
        section("1. Anonymous pipes");
        Process sort = new ProcessBuilder("sort").start();                      // the child's stdin and stdout are pipes to us
        try (Writer w = new OutputStreamWriter(sort.getOutputStream(), StandardCharsets.UTF_8)) { w.write("pear\napple\nmango\n"); }   // close = EOF for the child
        String sorted = new String(sort.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim().replace("\n", ",");
        sort.waitFor();
        check("parent writes into the child's stdin pipe, reads its stdout pipe", "apple,mango,pear", sorted);
        Process pipeline = new ProcessBuilder("sh", "-c", "printf 'a\\nb\\na\\nc\\na\\n' | sort | uniq -c | sort -rn | head -1").start();
        String top = new String(pipeline.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim().replaceAll("\\s+", " ");
        pipeline.waitFor();
        check("a shell pipeline: 4 processes connected by 3 pipes", "3 a", top);
        show("pipes are one-way byte streams between RELATED processes; the kernel buffers ~64 KB and blocks a full writer (backpressure)");
    }

    // ---------------------------------------------------------------------
    // 2. named pipes
    // ---------------------------------------------------------------------
    static void fifo() throws Exception {
        section("2. Named pipe (FIFO)");
        Path fifo = dir.resolve("jobs.fifo");
        Process mk = new ProcessBuilder("mkfifo", fifo.toString()).start();
        mk.waitFor();
        Process writer = new ProcessBuilder("sh", "-c", "echo 'job-17 resize image' > '" + fifo + "'").start();   // an UNRELATED process writes by name
        String got = Files.readString(fifo).trim();                               // opening blocks until both ends are open
        writer.waitFor();
        check("an unrelated process communicated through a path in the file system", "job-17 resize image", got);
        check("the FIFO is a special file, not a regular one", false, Files.isRegularFile(fifo));
        check("...and its data never touches the disk (size 0)", 0L, Files.size(fifo));
    }

    // ---------------------------------------------------------------------
    // 3. Unix domain sockets vs TCP loopback
    // ---------------------------------------------------------------------
    static double roundTrips(SocketAddress addr, ServerSocketChannel server, int n) throws Exception {
        Thread echo = new Thread(() -> {
            try (SocketChannel c = server.accept()) {
                ByteBuffer b = ByteBuffer.allocate(64);
                while (c.read(b) > 0) { b.flip(); c.write(b); b.clear(); }
            } catch (IOException ignored) { }
        });
        echo.start();
        long t0;
        try (SocketChannel client = SocketChannel.open(addr)) {
            ByteBuffer out = ByteBuffer.allocate(8), in = ByteBuffer.allocate(8);
            for (int i = 0; i < 200; i++) { out.clear(); out.putLong(i).flip(); client.write(out); in.clear(); while (in.hasRemaining()) client.read(in); }   // warm-up
            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                out.clear(); out.putLong(i).flip(); client.write(out);
                in.clear(); while (in.hasRemaining()) client.read(in);
            }
        }
        double us = (System.nanoTime() - t0) / 1e3 / n;
        echo.join();
        return us;
    }

    static void unixSockets() throws Exception {
        section("3. Unix domain sockets vs TCP loopback (real)");
        Path sock = dir.resolve("app.sock");
        UnixDomainSocketAddress uds = UnixDomainSocketAddress.of(sock);
        double udsUs, tcpUs;
        try (ServerSocketChannel s = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
            s.bind(uds);
            check("a Unix socket is addressed by a FILE PATH (permissions apply)", true, Files.exists(sock));
            udsUs = roundTrips(uds, s, 20_000);
        }
        try (ServerSocketChannel s = ServerSocketChannel.open()) {
            s.bind(new InetSocketAddress("127.0.0.1", 0));
            tcpUs = roundTrips(s.getLocalAddress(), s, 20_000);
        }
        show(String.format("20,000 round trips: Unix domain socket %.1f us each, TCP loopback %.1f us each", udsUs, tcpUs));
        check("both work as full-duplex sockets", true, udsUs > 0 && tcpUs > 0);
        show("UDS skips the TCP/IP stack (no checksums, routing, ports): typically faster -- used by Docker's API, PostgreSQL, nginx -> app servers");
        show("also: SO_PEERCRED tells the server the client's uid/pid, and UDS can pass open file descriptors between processes");
    }

    // ---------------------------------------------------------------------
    // 4. shared memory between processes
    // ---------------------------------------------------------------------
    static void sharedMemory() throws Exception {
        section("4. Shared memory between two processes (real, via mmap)");
        Path shm = dir.resolve("shared.mem");
        try (FileChannel ch = FileChannel.open(shm, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            MappedByteBuffer mem = ch.map(FileChannel.MapMode.READ_WRITE, 0, 4096);
            mem.putInt(0, 0);                                                    // [0] = ready flag, [4..] = message
            Path childSrc = dir.resolve("Writer.java");
            Files.writeString(childSrc, """
                import java.nio.*; import java.nio.channels.*; import java.nio.file.*;
                public class Writer { public static void main(String[] a) throws Exception {
                  try (FileChannel ch = FileChannel.open(Path.of(a[0]), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                    MappedByteBuffer m = ch.map(FileChannel.MapMode.READ_WRITE, 0, 4096);
                    byte[] msg = "price=101.25 from pid ".concat(String.valueOf(ProcessHandle.current().pid())).getBytes();
                    m.putInt(4, msg.length); m.put(8, msg);
                    m.putInt(0, 1);                                // publish: set the flag LAST
                  } } }
                """);
            Process child = new ProcessBuilder(java, childSrc.toString(), shm.toString()).inheritIO().start();
            long deadline = System.currentTimeMillis() + 15_000;
            while (mem.getInt(0) != 1 && System.currentTimeMillis() < deadline) Thread.onSpinWait();   // poll the shared flag
            child.waitFor();
            byte[] msg = new byte[mem.getInt(4)];
            mem.get(8, msg);
            String text = new String(msg, StandardCharsets.UTF_8);
            show("parent read from shared memory: '" + text + "'");
            check("the CHILD's write appeared in the PARENT's memory, with no copy through the kernel", true, text.startsWith("price=101.25 from pid " + child.pid()));
        }
        show("fastest IPC, but YOU must synchronise (flags + memory barriers, futexes, or semaphores) -- no message boundaries, no safety");
    }

    // ---------------------------------------------------------------------
    // 5. message queue
    // ---------------------------------------------------------------------
    static void messageQueue() throws Exception {
        section("5. Message passing with a bounded queue");
        BlockingQueue<String> q = new ArrayBlockingQueue<>(4);
        List<String> received = Collections.synchronizedList(new ArrayList<>());
        Thread consumer = new Thread(() -> {
            try { while (true) { String m = q.take(); if (m.equals("STOP")) return; received.add(m); Thread.sleep(2); } } catch (InterruptedException ignored) { }
        });
        consumer.start();
        long blockedMs = 0;
        for (int i = 0; i < 20; i++) { long t0 = System.nanoTime(); q.put("msg-" + i); blockedMs += (System.nanoTime() - t0) / 1_000_000; }
        q.put("STOP");
        consumer.join();
        check("messages arrive whole and in order (boundaries preserved)", "msg-0..msg-19", received.get(0) + ".." + received.get(received.size() - 1));
        check("a full queue blocks the producer: natural backpressure", true, blockedMs > 0);
        show("cross-process equivalents: POSIX/System V message queues, Unix datagram sockets, and brokers (Kafka, RabbitMQ) across machines");
    }

    // ---------------------------------------------------------------------
    // 6. signals
    // ---------------------------------------------------------------------
    static void signals() throws Exception {
        section("6. Signals as IPC");
        Process child = new ProcessBuilder("sh", "-c", "trap 'echo got-USR1' USR1; trap 'echo graceful-shutdown; exit 0' TERM; echo ready; while true; do sleep 0.05; done").start();
        BufferedReader out = new BufferedReader(new InputStreamReader(child.getInputStream()));
        check("child installed its signal handlers", "ready", out.readLine());
        new ProcessBuilder("kill", "-USR1", String.valueOf(child.pid())).start().waitFor();
        check("SIGUSR1 ran the child's handler (e.g. 'reload config' / 'dump stats')", "got-USR1", out.readLine());
        new ProcessBuilder("kill", "-TERM", String.valueOf(child.pid())).start().waitFor();   // (Process.destroy() would also close our end of its pipes)
        check("SIGTERM handler ran: graceful shutdown", "graceful-shutdown", out.readLine());
        check("...and the child exited cleanly with 0 (not 143)", 0, child.waitFor());
    }

    public static void main(String[] args) throws Exception {
        dir = Files.createTempDirectory("ipc");
        java = ProcessHandle.current().info().command().orElse("java");
        try {
            pipes();
            fifo();
            unixSockets();
            sharedMemory();
            messageQueue();
            signals();
        } finally {
            try (var s = Files.walk(dir)) { s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
        }
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
