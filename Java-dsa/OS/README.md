# Operating systems for senior software engineers

> How your code actually runs: processes and threads, scheduling, locks, virtual memory, garbage
> collection, files, event loops, IPC, and the Linux and container machinery underneath. Every
> concept is tied to a production symptom, and almost every one is **demonstrated for real** in Java.

Every chapter has:

- a **README**: intuition, diagrams, the mechanism, production consequences, the senior interview
  questions **answered**;
- a **self-checking Java program**: run it with `java File.java` (JDK 21, no dependencies). The
  real demos include:
  - child processes killed by signals (exit 137/139/143) and a **zombie** caught by `ps`;
  - 10,000 virtual threads; a lost-update race; a **real deadlock detected by `ThreadMXBean`**;
  - row- vs column-major cache effects; real `mmap`; a child JVM hitting `OutOfMemoryError`;
  - a static-cache leak surviving full GCs; "Too many open files"; real `fsync` costs; torn writes
    vs atomic rename;
  - **one-thread epoll/kqueue server for 300 clients**; event-loop blocking;
  - Unix domain sockets vs TCP; shared memory between processes; dining philosophers deadlocking.

```bash
cd Java-dsa/OS
cd 04-Concurrency-and-Synchronization && java Concurrency.java   # one chapter
./run-all.sh                                                      # everything (~1 minute)
```

## Chapters

| # | Chapter | Highlights | Checks |
|---|---|---|---|
| 01 | [OS fundamentals](01-OS-Fundamentals/README.md) | kernel vs user mode, syscall cost (74× buffering win), interrupts vs exceptions, isolation, exit codes, kernel architectures, boot, `strace` | 17 |
| 02 | [Processes and threads](02-Processes-and-Threads/README.md) | PCB, states, fork/exec/wait, a real zombie, orphans, signals, isolation, thread failure, pools vs per-task threads, virtual threads, stacks, thread-locals | 18 |
| 03 | [CPU scheduling](03-CPU-Scheduling/README.md) | FCFS/SJF/SRTF (textbook 8.75/7.75/6.50), round robin quanta, priority + aging, MLFQ, priority inversion, oversubscription measured | 14 |
| 04 | [Concurrency and synchronization](04-Concurrency-and-Synchronization/README.md) | races, mutex/atomic/LongAdder, CAS, a lock-free stack, semaphores, RW locks, conditions, barriers, visibility, a real deadlock + fixes | 18 |
| 05 | [Memory management](05-Memory-Management/README.md) | paging, 2-level page tables, TLB, minor/major faults, protection, COW, locality, fragmentation, mmap, stack vs heap, NUMA | 22 |
| 06 | [Allocation and GC](06-Allocation-and-GC/README.md) | refcounting cycles, mark-sweep, mark-compact, copying, generational; real weak refs, G1 stats, a leak, heap vs RSS; V8 | 18 |
| 07 | [Page replacement](07-Page-Replacement/README.md) | FIFO/LRU/OPT/Clock (15/12/9/14), Belady's anomaly (9 → 10), working sets, thrashing curve, LRU cache | 13 |
| 08 | [File systems and I/O](08-File-Systems-and-IO/README.md) | inodes, hard vs soft links, fds and EMFILE, permissions, page cache and fsync, atomic rename, locks, journaling, disk scheduling | 24 |
| 09 | [I/O multiplexing and event loops](09-IO-Multiplexing-and-Event-Loops/README.md) | select/poll/epoll/kqueue, a real single-thread server, LT vs ET, readiness vs completion, Node's loop, blocking and offloading, backpressure | 11 |
| 10 | [IPC](10-IPC/README.md) | pipes, FIFOs, Unix domain vs TCP sockets, shared memory, message queues, signals | 14 |
| 11 | [Classic synchronization problems](11-Classic-Synchronization-Problems/README.md) | bounded buffer (semaphores, monitor), readers-writers, dining philosophers, Banker's algorithm, wait-for-graph detection | 12 |
| 12 | [Linux and containers](12-Linux-and-Containers/README.md) | load average, `/proc`, vmstat, namespaces, cgroups, **CPU throttling**, OOM killer, PID 1 and signals, the 4 senior scenarios | 24 |
| 13 | [Interview questions and plan](13-Interview-Questions/README.md) | top-12 priorities, 53 senior questions with answers, the 4-week plan for both courses, final advice | — |

Total: **205 self-checks** across 12 programs.

## How to study

1. Read a chapter. Each ends with its senior questions answered.
2. Run the program; match every `PASS` line to the code that produced it.
3. Break something on purpose (remove `volatile`, reverse a lock order, shrink a pool, drop a
   `close()`), predict, re-run.
4. Answer the questions aloud, then compare with the README.

Pair this with [Networking](../Networking/README.md) (sockets, TCP, load balancing),
[Multithreading](../Multithreading/) (Java concurrency APIs) and [JVM-Internals](../JVM-Internals/).
