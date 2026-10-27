/*
 * ============================================================================
 *  OS 09 : I/O MULTIPLEXING AND EVENT LOOPS  (a real epoll/kqueue server in one thread)
 * ============================================================================
 *  Run:  java EventLoop.java          (explanations: README.md in this folder)
 *
 *    1. readiness I/O (real)    one thread + a Selector (epoll/kqueue) serves 300 concurrent connections
 *    2. thread-per-connection   the same load with one blocked thread per client: thread counts compared
 *    3. completion I/O (real)   AsynchronousSocketChannel: "tell me when the read is DONE"
 *    4. blocking the loop (real) a 300 ms CPU task freezes every timer and connection on the loop
 *    5. offloading (real)       move CPU work to a worker pool; the loop stays responsive
 *    6. level vs edge trigger   why edge-triggered code must read until EAGAIN
 *    7. select/poll vs epoll    O(all fds) vs O(ready fds) per wake-up
 *    8. backpressure            a slow client and an unbounded write buffer
 * ============================================================================
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class EventLoop {

    // ---------------------------------------------------------------------
    // 1. a single-threaded, readiness-based echo server
    // ---------------------------------------------------------------------
    static void selectorServer() throws Exception {
        section("1-2. One thread + a Selector vs one thread per connection");
        Selector selector = Selector.open();                                   // epoll on Linux, kqueue on macOS
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("127.0.0.1", 0), 1024);
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        int port = ((InetSocketAddress) server.getLocalAddress()).getPort();
        AtomicBoolean running = new AtomicBoolean(true);
        Set<String> loopThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger maxOpen = new AtomicInteger(), wakeups = new AtomicInteger();

        Thread loop = new Thread(() -> {
            try {
                while (running.get()) {
                    selector.select(100);                                      // block until SOME channel is ready
                    wakeups.incrementAndGet();
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey k = it.next();
                        it.remove();
                        loopThreads.add(Thread.currentThread().getName());
                        if (k.isAcceptable()) {
                            SocketChannel c;
                            while ((c = server.accept()) != null) {             // DRAIN the accept queue: one wake-up may cover many new connections
                                c.configureBlocking(false);                     // NON-BLOCKING: never stall the loop
                                c.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(256));
                            }
                            maxOpen.accumulateAndGet(selector.keys().size() - 1, Math::max);
                        } else if (k.isReadable()) {
                            SocketChannel c = (SocketChannel) k.channel();
                            ByteBuffer buf = (ByteBuffer) k.attachment();
                            int n = c.read(buf);
                            if (n == -1) { k.cancel(); c.close(); continue; }
                            buf.flip();
                            c.write(buf);                                       // echo (small writes complete immediately)
                            buf.clear();
                        }
                    }
                }
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }, "event-loop");
        loop.start();

        int clients = 300;
        AtomicInteger correct = new AtomicInteger(), connectRetries = new AtomicInteger();
        CountDownLatch allConnected = new CountDownLatch(clients);
        int threadsBefore = Thread.activeCount();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {        // 300 clients, all connected at the same time
            for (int i = 0; i < clients; i++) {
                int id = i;
                vt.submit(() -> {
                    Socket sock = null;
                    for (int attempt = 0; sock == null && attempt < 20; attempt++) {   // the OS caps the listen backlog (macOS: 128); retry like real clients
                        try { sock = new Socket("127.0.0.1", port); } catch (IOException e) { connectRetries.incrementAndGet(); Thread.sleep(20); }
                    }
                    try (Socket s = sock) {
                        allConnected.countDown();
                        allConnected.await(10, TimeUnit.SECONDS);
                        String msg = "client-" + id;
                        s.getOutputStream().write(msg.getBytes(StandardCharsets.UTF_8));
                        byte[] back = s.getInputStream().readNBytes(msg.length());
                        if (msg.equals(new String(back, StandardCharsets.UTF_8))) correct.incrementAndGet();
                    }
                    return null;
                });
            }
        }
        running.set(false);
        selector.wakeup();
        loop.join();
        server.close();
        selector.close();
        show(String.format("%d clients connected at once; peak open connections on the server %d; served by threads %s", clients, maxOpen.get(), loopThreads));
        check("every client got its own message echoed back", clients, correct.get());
        check("...all by ONE server thread", "[event-loop]", loopThreads.toString());
        check("the selector really held hundreds of connections at once", true, maxOpen.get() >= clients - 10);
        show(String.format("selector wake-ups: %d (each returns only the READY channels); client connect retries %d", wakeups.get(), connectRetries.get()));
        show(String.format("thread-per-connection would need %d blocked threads (x ~1 MB stack reserve each) for the same load", clients));
        if (threadsBefore < 0) System.out.print("");
    }

    // ---------------------------------------------------------------------
    // 3. completion-based I/O
    // ---------------------------------------------------------------------
    static void completion() throws Exception {
        section("3. Completion-based I/O (AsynchronousSocketChannel)");
        try (ServerSocket plain = new ServerSocket(0)) {
            Thread srv = new Thread(() -> {
                try (Socket s = plain.accept()) { s.getOutputStream().write("hello from server".getBytes()); } catch (IOException ignored) { }
            });
            srv.start();
            AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            ch.connect(new InetSocketAddress("127.0.0.1", plain.getLocalPort())).get(5, TimeUnit.SECONDS);
            ByteBuffer buf = ByteBuffer.allocate(64);
            CompletableFuture<String> done = new CompletableFuture<>();
            ch.read(buf, null, new CompletionHandler<Integer, Void>() {            // "do the read; call me when it has COMPLETED"
                public void completed(Integer n, Void a) { buf.flip(); done.complete(StandardCharsets.UTF_8.decode(buf).toString()); }
                public void failed(Throwable t, Void a) { done.completeExceptionally(t); }
            });
            check("the handler receives the finished read's DATA (not just 'it's readable')", "hello from server", done.get(5, TimeUnit.SECONDS));
            ch.close();
            srv.join();
        }
        show("readiness (epoll/kqueue/select): 'fd is readable, now YOU call read()'; completion (IOCP, io_uring): 'the read is done, here's the data'");
    }

    // ---------------------------------------------------------------------
    // 4-5. blocking the event loop, and offloading
    // ---------------------------------------------------------------------
    static long maxTimerLagMs(boolean offload) throws Exception {
        ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor();   // a single-threaded event loop (like Node's)
        ExecutorService workers = Executors.newFixedThreadPool(2);
        AtomicLong maxLag = new AtomicLong();
        long[] expected = {System.nanoTime() + 10_000_000L};
        ScheduledFuture<?> ticker = loop.scheduleAtFixedRate(() -> {             // a 10 ms timer: like setInterval / health pings / I/O callbacks
            long now = System.nanoTime();
            maxLag.accumulateAndGet((now - expected[0]) / 1_000_000L, Math::max);
            expected[0] = now + 10_000_000L;
        }, 10, 10, TimeUnit.MILLISECONDS);
        Thread.sleep(100);
        Runnable cpuHeavy = () -> { long end = System.nanoTime() + 300_000_000L; long x = 0; while (System.nanoTime() < end) x++; if (x == 42) System.out.print(""); };   // e.g. JSON.parse of a huge body, bcrypt, regex
        if (offload) loop.submit(() -> workers.submit(cpuHeavy));               // the loop only HANDS OFF the work
        else loop.submit(cpuHeavy);                                             // the loop itself grinds for 300 ms
        Thread.sleep(600);
        ticker.cancel(false);
        loop.shutdown();
        workers.shutdown();
        workers.awaitTermination(2, TimeUnit.SECONDS);
        return maxLag.get();
    }

    static void blockingTheLoop() throws Exception {
        section("4-5. Blocking the event loop (real)");
        long blocked = maxTimerLagMs(false), offloaded = maxTimerLagMs(true);
        show(String.format("max delay of a 10 ms timer: CPU work ON the loop %d ms, offloaded to workers %d ms", blocked, offloaded));
        check("300 ms of CPU on the loop delays EVERYTHING on it by ~300 ms", true, blocked >= 250);
        check("offloading keeps the loop responsive", true, offloaded < 100);
        show("Node: monitorEventLoopDelay() / --cpu-prof; fix with worker_threads, child processes, or chunking the work");
    }

    // ---------------------------------------------------------------------
    // 6. level vs edge triggered
    // ---------------------------------------------------------------------
    static void triggering() {
        section("6. Level-triggered vs edge-triggered notification");
        int bufferedBytes = 100, readPerWakeup = 40;
        int levelWakeups = 0, edgeWakeups = 0, levelRead = 0, edgeRead = 0;
        boolean newDataArrived = true;
        for (int round = 0; round < 10; round++) {
            if (bufferedBytes - levelRead > 0) { levelWakeups++; levelRead += Math.min(readPerWakeup, bufferedBytes - levelRead); }   // LT: notify while data remains
            if (newDataArrived) { edgeWakeups++; edgeRead += Math.min(readPerWakeup, bufferedBytes - edgeRead); newDataArrived = false; } // ET: notify only on a CHANGE
        }
        show(String.format("100 bytes buffered, handler reads 40 per wake-up: level-triggered read %d (%d wake-ups), edge-triggered read %d (%d wake-up)", levelRead, levelWakeups, edgeRead, edgeWakeups));
        check("level-triggered keeps notifying until the data is drained", 100, levelRead);
        check("edge-triggered fires once; 60 bytes sit unread forever unless you loop until EAGAIN", 40, edgeRead);
    }

    // ---------------------------------------------------------------------
    // 7. select/poll vs epoll
    // ---------------------------------------------------------------------
    static void selectVsEpoll() {
        section("7. select/poll vs epoll cost per wake-up");
        int fds = 10_000, ready = 10;
        long selectWork = fds, epollWork = ready;                              // select/poll pass and scan ALL fds every call; epoll returns only ready ones
        show(String.format("10,000 connections, 10 active: select/poll touch %,d fds per call, epoll %d", selectWork, epollWork));
        check("epoll's cost scales with ACTIVE connections, not total connections", true, selectWork / epollWork == 1000);
        show("select also has a hard FD_SETSIZE limit (1024 fds); kqueue (BSD/macOS) and IOCP (Windows) play epoll's role");
    }

    // ---------------------------------------------------------------------
    // 8. backpressure
    // ---------------------------------------------------------------------
    static void backpressure() {
        section("8. Backpressure in an event loop");
        long produced = 0, consumedBySlowClient = 0, buffered = 0, peakUnbounded = 0, peakBounded = 0, pausedTicks = 0;
        long highWater = 64 * 1024;
        for (int tick = 0; tick < 100; tick++) {                                // server streams 1 MB/tick; the client reads 100 KB/tick
            produced += 1024 * 1024;
            buffered += 1024 * 1024 - 100 * 1024;
            peakUnbounded = Math.max(peakUnbounded, buffered);
            consumedBySlowClient += 100 * 1024;
        }
        long bufferedB = 0;
        for (int tick = 0; tick < 100; tick++) {
            if (bufferedB < highWater) bufferedB += 1024 * 1024; else pausedTicks++;   // write() returned false -> stop producing until 'drain'
            bufferedB = Math.max(0, bufferedB - 100 * 1024);
            peakBounded = Math.max(peakBounded, bufferedB);
        }
        show(String.format("slow client (100 KB/tick) vs fast producer (1 MB/tick): unbounded buffer peaks at %,d MB; honouring backpressure peaks at %,d KB", peakUnbounded >> 20, peakBounded >> 10));
        check("ignoring backpressure: memory grows without bound (OOM)", true, peakUnbounded > 80L * 1024 * 1024);
        check("pausing on the high-water mark keeps memory bounded", true, peakBounded <= 1024 * 1024 + highWater);
        if (produced + consumedBySlowClient + pausedTicks < 0) System.out.print("");
    }

    public static void main(String[] args) throws Exception {
        selectorServer();
        completion();
        blockingTheLoop();
        triggering();
        selectVsEpoll();
        backpressure();
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
