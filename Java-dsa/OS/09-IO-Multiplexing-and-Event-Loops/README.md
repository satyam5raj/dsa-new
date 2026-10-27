# OS 09 · I/O multiplexing and event loops (essential for Node.js)

> How can **one thread** serve thousands of connections? By never waiting on any single one. The OS
> tells you which sockets are ready (`epoll`/`kqueue`), and an **event loop** handles only those.
> This chapter builds that server for real, then breaks it the way production code does.

Run it: `java EventLoop.java` → **11/11 checks**:

- a **real** `Selector` server (epoll on Linux, kqueue on macOS) echoing to **300 concurrent clients
  from one thread**;
- **real** completion-based I/O;
- a **real** event loop blocked by CPU work and then fixed;
- simulations of level vs edge triggering, select vs epoll, and backpressure.

---

## 1. The problem: blocking I/O and thread-per-connection

```java
while (true) {
    Socket s = server.accept();                  // blocks
    new Thread(() -> handle(s)).start();         // each connection gets a thread that mostly WAITS (read blocks)
}
```

It's simple and fine for hundreds of connections. At 10,000+ (the "C10K problem") it needs:

- 10,000 threads × ~1 MB stack reserve;
- heavy scheduler and context-switch overhead (OS 03);
- memory that grows with **connections** rather than **work**.

Fixes: thread pools (which limit concurrency), **virtual threads** (OS 02: 10,000 sleeping tasks in
130 ms), or **I/O multiplexing**.

## 2. Readiness-based multiplexing: `select`, `poll`, `epoll`, `kqueue`

Put every socket in **non-blocking** mode, then ask the kernel **"which of these are ready?"**:

```
loop:
    ready = epoll_wait(epfd)          # sleeps until at least one fd is readable/writable
    for fd in ready:
        if fd is the listening socket: accept ALL pending connections, register them
        elif readable: read what's there (non-blocking), handle it, write the response
```

The run implements exactly this with Java NIO's `Selector`:

- **300 clients** connected simultaneously; **peak open connections on the server: 300**;
- **every** client got its own message echoed back;
- **all by one thread** (`[event-loop]`), in only **22 selector wake-ups**: each wake-up returned a
  *batch* of ready channels.

> **A lesson the run taught for real:** my first version accepted **one** connection per wake-up. With
> 300 clients connecting at once, macOS's listen backlog (capped at 128 by `kern.ipc.somaxconn`)
> overflowed, and only **248** clients were served. **Always drain the accept queue** (`accept()`
> until it returns `null`/`EAGAIN`) and size the backlog (and `somaxconn`) for bursts.

| Mechanism | Per-call cost | Limits |
|---|---|---|
| **select** | pass **all** fds in, kernel scans **all**, you scan **all** | `FD_SETSIZE` = **1024** fds |
| **poll** | same O(n) scanning, no fixed limit | slow at large n |
| **epoll** (Linux) | register fds **once**; `epoll_wait` returns **only ready** fds | scales to millions |
| **kqueue** (BSD/macOS) | the same idea, more general (files, signals, timers) | |
| **IOCP** (Windows), **io_uring** (Linux) | **completion**-based (below) | |

With 10,000 connections of which 10 are active, select/poll touch **10,000** fds per call; epoll
touches **10**, a 1,000× difference. Cost scales with **activity**, not connection count.

## 3. Level-triggered vs edge-triggered

| | Level-triggered (LT, default) | Edge-triggered (ET, `EPOLLET`) |
|---|---|---|
| notifies when | the fd **is** ready (every `epoll_wait` while data remains) | the fd **becomes** ready (on a **change**) |
| run (100 bytes buffered, read 40 per wake-up) | read all **100** over **3** wake-ups | **one** wake-up → read **40**; **60 bytes sit unread forever** |
| rule | forgiving | you **must read/write until `EAGAIN`** each time |
| why use it | simplicity | fewer wake-ups, avoids thundering herds across threads |

## 4. Readiness vs completion

| | **Readiness** (epoll, kqueue, select) | **Completion** (IOCP, io_uring, Java `AsynchronousSocketChannel`) |
|---|---|---|
| the kernel tells you | "fd 7 is **readable**; now **you** call `read()`" | "your `read()` **finished**; here are the bytes" |
| buffers | you supply them when you read | supplied **up front** with the request |
| files | regular files are always "ready", so it doesn't help (hence Node's thread pool for `fs`) | works for files and sockets alike |
| syscalls | wait + read + write | batched submissions (io_uring: shared ring buffers, few syscalls) |

The run's `AsynchronousSocketChannel.read(buf, …, CompletionHandler)` got called back with the
**finished data** `"hello from server"`.

## 5. Event loops

An **event loop** = one thread running:

1. wait for events (I/O readiness, timers, completed tasks);
2. run each callback **to completion**;
3. repeat.

There are no locks for loop-owned state, it's cheap per connection, and it's predictable. This is
Node.js, nginx, Redis, Netty, Vert.x, Python asyncio, browsers.

### Node.js specifically

```
   ┌─► timers (setTimeout/setInterval) ──► pending callbacks ──► poll (I/O: epoll/kqueue/IOCP via libuv)
   │                                                                │
   └──────── close callbacks ◄──────── check (setImmediate) ◄───────┘
         (process.nextTick and promise microtasks run between every phase)
```

- **Network I/O:** non-blocking sockets plus epoll/kqueue in **libuv**: one JS thread, thousands of
  connections.
- **Not everything is non-blocking:** file system calls, DNS `lookup` (`getaddrinfo`), crypto
  (`pbkdf2`, `scrypt`) and zlib run on **libuv's thread pool** (default **4** threads;
  `UV_THREADPOOL_SIZE`). Four slow `fs` or `dns.lookup` calls can stall all the others.
- **CPU-heavy JavaScript runs ON the event loop** and blocks everything.

## 6. Blocking the event loop (real)

A single-threaded loop runs a 10 ms timer (standing in for health checks, I/O callbacks, other
requests). Then one task does **300 ms of CPU work** (a huge `JSON.parse`, a regex with
backtracking, a synchronous bcrypt, an O(n²) loop):

| Where the CPU work runs | Max timer delay |
|---|---|
| **on the event loop** | **292 ms**: every request, timer and health check on that loop is frozen |
| **offloaded** to a worker pool | **2 ms** |

**Why can CPU-intensive work make an async Node.js server unresponsive?** The single JS thread can't
process any other callback (new requests, I/O completions, timers) until the CPU task returns. All
concurrency is cooperative.

**How to detect event-loop blocking:**

- `perf_hooks.monitorEventLoopDelay()` (histogram of loop lag, P99);
- `--cpu-prof` / clinic.js / 0x flame graphs;
- APM event-loop lag metrics;
- `--trace-sync-io`;
- the symptom: **all** endpoints slow down together, even trivial ones, while CPU sits at 100 % of
  one core.

**When to use worker threads or separate processes:**

| | Use for |
|---|---|
| **worker threads** (`worker_threads`) | CPU-heavy work that needs to share memory (`SharedArrayBuffer`) or transfer buffers cheaply: image processing, parsing, compression |
| **child processes / cluster / separate services** | isolation (a crash or leak can't take down the server), using **all cores** for request handling (`cluster`, PM2, or simply N containers), running other languages |
| **chunking** | split a long loop with `setImmediate` so other events interleave |
| **move it out** | queues + workers (BullMQ), or a dedicated service for heavy work |

## 7. Backpressure

A fast producer streams 1 MB per tick to a client that reads only 100 KB per tick:

| | Peak buffered memory |
|---|---|
| **ignoring backpressure** | **90 MB** and growing without bound, until OOM |
| **pausing when `write()` returns false** (above a 64 KB high-water mark) and resuming on `'drain'` | **972 KB** |

In Node, respect the `write()` return value or use `stream.pipeline()`. In Netty, check
`isWritable()`. In general, use bounded queues (Networking 09).

## 8. Connection scalability checklist

- epoll/kqueue-based servers (or virtual threads) instead of thread-per-connection;
- `ulimit -n` / `LimitNOFILE` high enough (OS 08);
- a big listen backlog plus `net.core.somaxconn`; drain `accept()`;
- ephemeral port and TIME_WAIT tuning for outbound connections (Networking 04);
- keepalive and idle timeouts (Networking 04);
- memory per connection (buffers) × connections fits in RAM;
- multiple event loops (one per core: nginx workers, Netty event-loop groups, Node `cluster`).

## Thread-per-connection vs event-driven

| | Thread-per-connection | Event-driven (epoll + loop) |
|---|---|---|
| programming model | simple sequential code | callbacks/promises/async-await; never block |
| memory per connection | a thread stack (~1 MB reserve) | a few KB of buffers |
| 10k idle connections | 10k threads | ~free |
| CPU-heavy work | fine (OS preempts) | **blocks everything**: offload it |
| failure mode under load | thread exhaustion, context switches | event-loop lag |
| modern middle ground | **virtual threads** (Java 21), goroutines: sequential code on an event-driven runtime | |

---

## Senior-level questions

**1. How can Node.js handle many concurrent connections with a JavaScript event loop?** Sockets are
non-blocking and registered with epoll/kqueue/IOCP through libuv. The single JS thread sleeps until
some are ready, then runs their callbacks. Waiting costs no thread, so thousands of idle connections
cost only memory. Blocking work (fs, dns.lookup, crypto) goes to libuv's thread pool.

**2. What is the difference between epoll and a thread-per-connection architecture?** epoll lets one
(or a few) threads watch many sockets and handle only the ready ones: memory ∝ buffers, cost ∝
activity. Thread-per-connection parks a whole thread per socket: simple code, but memory and
scheduling cost grow with connection count.

**3. What is the difference between readiness-based and completion-based I/O?** Readiness tells you
an fd can be read or written without blocking, and you do the syscall. Completion performs the
operation for you and notifies you when it's done, with the data (IOCP, io_uring). Completion also
works for regular files and allows batching.

**4. Why can CPU-intensive work make an asynchronous Node.js server unresponsive?** JS runs on one
thread; a long synchronous task prevents the loop from running any other callback (292 ms of lag in
the demo). Offload to workers, child processes or another service, or chunk the work.

**5. How would you detect event-loop blocking?** `monitorEventLoopDelay` / loop-lag metrics, CPU
profiles and flame graphs, `--trace-sync-io`, and the symptom of every endpoint slowing together with
one core pegged.

**6. When would you use worker threads or separate processes?** Worker threads for CPU-bound work
needing shared or transferred memory inside one service. Processes for isolation, using all cores,
crash containment, or other runtimes (§6).

---

**Next:** [10 · Inter-process communication](../10-IPC/README.md)
