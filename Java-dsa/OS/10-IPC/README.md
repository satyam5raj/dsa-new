# OS 10 · Inter-process communication (IPC)

> Processes are isolated on purpose (OS 02). When they **do** need to talk (a shell pipeline, nginx
> → your app, a database client → Postgres, a sidecar → its service), they use IPC. Each mechanism
> trades speed, structure and safety differently.

Run it: `java Ipc.java` → **14/14 checks**. Everything is real:

- pipes to child processes and a 4-process shell pipeline;
- a `mkfifo` named pipe;
- a **Unix domain socket** vs TCP loopback latency race;
- **shared memory between two processes**;
- a bounded message queue;
- signals with real handlers.

---

## The menu

| Mechanism | Between | Shape | Speed | Notes |
|---|---|---|---|---|
| **anonymous pipe** | related processes (parent/child) | one-way byte stream | fast | shell `|`, child stdin/stdout |
| **named pipe (FIFO)** | any processes on the host | one-way byte stream, has a path | fast | rendezvous by file name |
| **Unix domain socket** | any processes on the host | bidirectional stream or datagrams | **faster than TCP loopback** | file-path address, permissions, credential and fd passing |
| **TCP/UDP socket** | any processes, **any host** | stream / datagrams | network stack overhead | the universal option |
| **shared memory** | any processes on the host | raw shared bytes | **fastest** (no copy) | **you** must synchronise |
| **message queue** | processes, or threads | discrete messages | medium | POSIX/SysV queues; brokers across hosts |
| **signals** | any processes | a number, no data | — | notifications only |
| **RPC** | across hosts | function-call semantics over sockets | depends | gRPC, Thrift (Networking 09) |

## 1. Anonymous pipes

A pipe is a **one-way kernel buffer** with a write end and a read end. `fork()` gives both ends to the
child; each side closes the end it doesn't use.

From the run:

- writing `pear, apple, mango` into `sort`'s stdin pipe and reading its stdout gave
  **`apple,mango,pear`**;
- the shell pipeline `printf … | sort | uniq -c | sort -rn | head -1` (4 processes, 3 pipes) gave
  **`3 a`**.

Behaviour to know:

- **Closing the write end** signals **EOF** to the reader. Forgetting to close it makes the reader
  hang forever, a classic bug.
- The pipe buffer is ~64 KB on Linux. A **full** pipe **blocks the writer**: built-in
  **backpressure**, which is why `producer | slow_consumer` doesn't use unbounded memory.
- Writing to a pipe with no readers gives **SIGPIPE** / `EPIPE` (why `cmd | head` stops `cmd`).
- Writes ≤ `PIPE_BUF` (4 KB) are atomic, so they won't interleave with other writers.

## 2. Named pipes (FIFOs)

`mkfifo jobs.fifo` creates a **special file**: not a regular file, **size 0**, and its data never
touches disk. In the run, an **unrelated** shell process wrote `job-17 resize image` by path and we
read it. Opening blocks until both a reader and a writer exist. It's handy for simple local
producer/consumer setups and for feeding tools that expect file paths.

## 3. Pipe vs socket

| | Pipe | Socket |
|---|---|---|
| direction | one-way (two pipes for duplex) | **bidirectional** |
| scope | same host (FIFOs: any process; anonymous: related) | same host (Unix) **or across the network** (TCP/UDP) |
| addressing | inheritance or a file path | file path (UDS) or IP:port |
| message boundaries | stream only | stream (SOCK_STREAM) **or** datagrams (SOCK_DGRAM / SEQPACKET) |
| extras | — | credentials (`SO_PEERCRED`), **passing fds**, many clients per server, `accept()` |

## 4. Unix domain sockets vs TCP sockets

A **Unix domain socket (UDS)** is a socket whose address is a **file path** (`/var/run/docker.sock`,
`/var/run/postgresql/.s.PGSQL.5432`). The run created one, and the path appeared in the file system.

Real latency, 20,000 small round trips:

| | Per round trip |
|---|---|
| **Unix domain socket** | **5.8 µs** |
| TCP loopback (127.0.0.1) | **14.1 µs** |

UDS **skips the TCP/IP stack**: no checksums, segmentation, congestion control, routing, port
allocation or TIME_WAIT. It also gives:

- **file-permission access control** (who may connect = who may open the path);
- **peer credentials** (`SO_PEERCRED`: the client's uid and pid, which is how Postgres "peer" auth
  works);
- **fd passing** (`SCM_RIGHTS`: hand an open socket or file to another process; used by
  zero-downtime reloads and systemd socket activation).

Use UDS for same-host traffic: nginx → app server (uWSGI/gunicorn/php-fpm), app → local database,
sidecars, the Docker and containerd APIs. Use TCP when you need to cross hosts, or for simplicity in
container networks.

## 5. Shared memory

Two processes map the **same physical pages** (`shm_open` + `mmap(MAP_SHARED)`, or a shared file
mapping). In the run, a **child JVM** wrote `price=101.25 from pid 6190` into the mapped region and set
a ready flag **last**. The parent, polling the flag, read the message **directly from its own
memory**: no syscall per message, no copying through the kernel.

**When to choose shared memory over message passing:**

- very **high-volume or low-latency** data between processes on one host: market-data feeds,
  video frames, ML tensors (PyTorch multiprocessing, Apache Arrow/Plasma), database buffer pools
  (PostgreSQL's `shared_buffers` shared by its backend processes), Chrome's renderer ↔ GPU process;
- large data that would be expensive to serialise and copy.

**Costs:**

- **Synchronisation is your job:** flags with proper memory ordering, futexes, process-shared mutexes
  or semaphores; the race conditions of OS 04, now across processes.
- **No message boundaries or framing**; you design a layout (ring buffers).
- **Weaker isolation:** a buggy writer can corrupt the reader's view; a crash can leave a lock held.
- Same host only.

**Message passing** (pipes, sockets, queues) copies data but gives **clear ownership, boundaries,
backpressure and isolation**, and it extends naturally across machines. Default to message passing;
use shared memory when profiling proves the copies matter.

## 6. Message queues

A **bounded queue** passes discrete messages, **whole and in order**. In the run `msg-0..msg-19`
arrived intact, and a **full queue blocked the producer**: natural backpressure.

- On one host: POSIX message queues (`mq_open`), System V queues, Unix datagram sockets.
- Within a process: `BlockingQueue` / channels (Go).
- Across machines: brokers like Kafka, RabbitMQ, SQS (durability, fan-out, replay; Networking 09).

## 7. Signals

The smallest IPC: "event N happened", no payload. In the run, a child shell trapped:

- **`SIGUSR1`** → ran its handler (`got-USR1`): typical "reload config" or "dump stats";
- **`SIGTERM`** → ran its graceful-shutdown handler and **exited 0** (not 143). That's what your
  service should do on deploys (OS 02, OS 12).

> A detail the run surfaced: Java's `Process.destroy()` sends SIGTERM **and closes our ends of the
> child's pipes**, so you can't read its last words afterwards. The demo sends `kill -TERM` instead.

Signals are asynchronous and only async-signal-safe functions may run in a handler, so the usual
pattern is "set a flag / write to a self-pipe, and let the main loop act".

## 8. IPC performance and security trade-offs

| Concern | Guidance |
|---|---|
| speed | shared memory > UDS > pipes ≈ TCP loopback > cross-host TCP |
| safety and isolation | message passing (copies, boundaries) > shared memory |
| access control | UDS/FIFO: file permissions + peer credentials; TCP: network policy + authN (TLS/mTLS) |
| exposure | **never** expose a powerful local socket (e.g. `docker.sock`, which is root-equivalent) to containers or the network |
| crash behaviour | sockets and pipes report EOF/EPIPE to the peer; shared-memory locks can be left held |

---

## Senior-level questions

**1. How do processes communicate with each other?** Pipes and FIFOs (byte streams), sockets (Unix
domain locally, TCP/UDP across hosts), shared memory (mapped pages + synchronisation), message queues,
signals (notifications), plus higher-level RPC and brokers built on sockets.

**2. What is the difference between a pipe and a socket?** Pipes are one-way byte streams between
local processes. Sockets are bidirectional, support datagrams as well as streams, work locally (UDS)
or across the network, support many clients per server, and can pass credentials and fds (UDS).

**3. When would you choose shared memory over message passing?** For very high-throughput,
low-latency bulk data between processes on one host, where copying and serialisation dominate, and
only if you can handle synchronisation, a layout protocol and reduced isolation. Otherwise prefer
message passing (§5).

**4. How do Unix domain sockets differ from TCP sockets?** UDS are addressed by file paths, stay on
one host, skip the TCP/IP stack (5.8 vs 14.1 µs round trips here), use file permissions, expose peer
credentials and can pass file descriptors. TCP works across hosts and needs ports and network
security.

---

**Next:** [11 · Classic synchronization problems](../11-Classic-Synchronization-Problems/README.md)
