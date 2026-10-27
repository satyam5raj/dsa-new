# OS 13 · Interview question bank, priorities and study plan

> At senior level, interviewers want **mechanism → trade-off → production consequence**. Don't stop
> at "virtual memory maps virtual to physical addresses". Go on to "…so a container whose RSS creeps
> past `memory.max` gets SIGKILLed with exit 137, and here's how I'd prove it".

---

## 1. If time is short: the top 12

| # | Topic | Chapter |
|---|---|---|
| 1 | Processes vs threads | [02](../02-Processes-and-Threads/README.md) |
| 2 | Context switching and CPU scheduling | [03](../03-CPU-Scheduling/README.md) |
| 3 | Concurrency, race conditions, synchronisation | [04](../04-Concurrency-and-Synchronization/README.md) |
| 4 | Mutexes, semaphores, atomic operations | [04](../04-Concurrency-and-Synchronization/README.md) §3–4 |
| 5 | Deadlocks and their prevention | [04](../04-Concurrency-and-Synchronization/README.md) §6, [11](../11-Classic-Synchronization-Problems/README.md) |
| 6 | Virtual memory, paging, page faults | [05](../05-Memory-Management/README.md) |
| 7 | Stack, heap, memory allocation | [05](../05-Memory-Management/README.md) §10, [06](../06-Allocation-and-GC/README.md) |
| 8 | Garbage collection and memory leaks | [06](../06-Allocation-and-GC/README.md) |
| 9 | Blocking, non-blocking and asynchronous I/O | [08](../08-File-Systems-and-IO/README.md) §9, [09](../09-IO-Multiplexing-and-Event-Loops/README.md) |
| 10 | Event loops, epoll, I/O multiplexing | [09](../09-IO-Multiplexing-and-Event-Loops/README.md) |
| 11 | File descriptors, sockets, file systems | [08](../08-File-Systems-and-IO/README.md), [10](../10-IPC/README.md) |
| 12 | Linux troubleshooting, cgroups, container limits | [12](../12-Linux-and-Containers/README.md) |

---

## 2. Question bank (answer each out loud in 60–120 seconds)

### Fundamentals ([01](../01-OS-Fundamentals/README.md))

1. **What happens on a system call?** Registers + the `syscall` instruction → kernel mode → validate,
   do the work (maybe block) → return. About 1 µs (1.35 µs measured), so batch and buffer (74×
   faster).
2. **User vs kernel mode?** A CPU privilege level; privileged instructions and all memory belong to
   the kernel; transitions only via syscalls, exceptions and interrupts.
3. **Why can't an app touch arbitrary physical memory?** The MMU plus per-process page tables owned
   by the kernel; anything else faults → SIGSEGV.
4. **Interrupt vs exception?** Asynchronous hardware event vs a synchronous fault or trap caused by an
   instruction.

### Processes and threads ([02](../02-Processes-and-Threads/README.md))

5. **Process vs thread?** A resource container with an isolated address space vs an execution flow
   sharing it.
6. **What happens in a context switch?** Save registers/PC/SP → pick the next thread → restore → maybe
   switch page tables (TLB) → resume. The indirect cost is cold caches.
7. **Zombie process?** It exited, but the parent hasn't called `wait()`; only its PID and exit status
   remain (caught for real as state `Z`). PID 1 in containers must reap.
8. **Why do too many threads hurt?** Stack memory, context switches, contention, creation cost
   (60.8 ms per-thread vs 3.6 ms pooled).
9. **Process pool vs thread pool?** Isolation + serialisation vs shared memory + shared fate.
10. **Why doesn't a process crash take down others?** Separate address spaces and kernel-managed
    resources; the fault becomes a signal to that process only (exit 139).

### Scheduling ([03](../03-CPU-Scheduling/README.md))

11. **Preemptive vs non-preemptive?** The timer interrupt can take the CPU away vs running until the
    task blocks.
12. **Priority scheduling and starvation?** Constant high-priority work means low-priority tasks never
    run; aging fixes it (the job ran at t = 181).
13. **Priority inversion?** High waits on low's lock while medium runs; priority inheritance
    (t = 16 → 6).
14. **Too many context switches?** Switching cost plus cache and TLB thrash; runnable threads queue.
15. **CPU-bound vs I/O-bound?** Size pools to cores vs scale concurrency cheaply (async or virtual
    threads).

### Concurrency ([04](../04-Concurrency-and-Synchronization/README.md), [11](../11-Classic-Synchronization-Problems/README.md))

16. **Race condition example?** `count++` lost 1.36M of 1.6M updates; check-then-act.
17. **Mutex vs semaphore?** One owner vs N permits, no ownership, used for signalling.
18. **The four deadlock conditions?** Mutual exclusion, hold and wait, no preemption, circular wait;
    break one (lock ordering).
19. **Lock-free vs wait-free?** Some thread always progresses vs every thread finishes in bounded
    steps.
20. **How does contention hurt throughput?** Serialisation, parking, cache-line bouncing; shard,
    shorten critical sections, go lock-free.
21. **Diagnose a production deadlock?** Repeated thread dumps (`jstack`) → a cycle of BLOCKED
    threads → fix the lock order or add timeouts.

### Memory ([05](../05-Memory-Management/README.md), [06](../06-Allocation-and-GC/README.md), [07](../07-Page-Replacement/README.md))

22. **Explain virtual memory.** Private address spaces, MMU translation, the TLB, demand paging,
    sharing, COW, swap.
23. **Accessing an unmapped address?** Page fault → no VMA → SIGSEGV (139) / NPE.
24. **Minor vs major fault?** No I/O (µs) vs a disk read (ms).
25. **Why does excessive paging hurt?** RAM accesses become disk accesses; thrashing (2.995 vs
    0.505 ms per access).
26. **Stack vs heap?** Per-thread LIFO frames vs a shared, dynamically managed heap.
27. **Copy-on-write?** Shared read-only pages copied on first write (256 of 25,600 copied).
28. **Investigate rising memory?** RSS vs heap vs cgroup; heap dumps; native memory; correlate with
    load.
29. **High memory without a leak?** Caches, allocation pressure, native buffers, fragmentation, a
    committed-but-unused heap.
30. **Allocated vs resident?** Address space vs pages actually in RAM.
31. **Why GC latency spikes?** Stop-the-world pauses, full GCs, allocation rate, CPU throttling.
32. **A Node heap growing steadily?** `memoryUsage` trends, heap-snapshot diffs, retainers, listeners
    and closures.
33. **RSS high after GC?** The heap stays committed, fragmentation, non-heap memory.
34. **FIFO vs LRU; Belady?** 15 vs 12 faults; FIFO can fault **more** with more frames (9 → 10).
35. **Thrashing on a server?** iowait, swap in/out, latency collapse; cut the footprint or
    concurrency.
36. **Locality and caching?** Small working sets → high hit rates (1,905 vs 16,841 faults); column
    loops 6.3× slower.

### Files and I/O ([08](../08-File-Systems-and-IO/README.md))

37. **Opening a file?** Path walk + permission checks → inode → open-file entry → the lowest free fd.
38. **What's a file descriptor?** A per-process handle to a kernel open-file object (files, sockets,
    pipes…).
39. **fd vs inode?** A per-process open instance with an offset vs the file's identity and metadata.
40. **"Too many open files"?** Leaks or a low `ulimit -n` (the child died after 252 opens).
41. **What does fsync guarantee and not?** Data durable for that file; not the directory entry, not
    lying disks, not after a failed fsync (798 vs 5 ms with group commit).
42. **The page cache?** RAM caching of file data; write-back; read-ahead.
43. **High disk latency?** `iostat await`, fsync patterns, swap, cloud IOPS throttling.

### Event loops ([09](../09-IO-Multiplexing-and-Event-Loops/README.md))

44. **Node with many connections on one thread?** Non-blocking sockets + epoll/kqueue in libuv (300
    clients, 1 thread, 22 wake-ups).
45. **epoll vs thread-per-connection?** Cost scales with activity vs with connection count.
46. **Readiness vs completion?** "You can read now" vs "your read finished".
47. **CPU work in Node?** It blocks every callback (292 ms lag) → offload (2 ms).
48. **Detect event-loop blocking?** `monitorEventLoopDelay`, CPU profiles, all endpoints slowing
    together.
49. **Workers vs processes?** CPU work with shared memory vs isolation and multi-core.

### IPC ([10](../10-IPC/README.md))

50. **How do processes communicate?** Pipes, FIFOs, sockets, shared memory, queues, signals.
51. **Pipe vs socket?** One-way local byte stream vs bidirectional, datagrams, network-capable, fd
    passing.
52. **Shared memory vs message passing?** Speed for bulk data vs safety, boundaries, distribution.
53. **Unix vs TCP sockets?** Path-addressed, local, faster (5.8 vs 14.1 µs), permissions and peer
    credentials.

---

## 3. Four-week plan (both courses)

At 60–90 minutes a day:

| Week | Networking | Operating systems |
|---|---|---|
| 1 | OSI/TCP-IP, IP addressing, subnetting, TCP/UDP ([01–04](../../Networking/README.md)) | OS fundamentals, processes, threads, scheduling ([01–03](../README.md)) |
| 2 | DNS, HTTP, TLS, HTTP versions (05–06) | synchronisation, race conditions, deadlocks (04, 11) |
| 3 | routing, NAT, load balancing, network security (07–08) | virtual memory, paging, allocation, garbage collection (05–07) |
| 4 | connection management, troubleshooting, distributed systems (09–10) | I/O, event loops, Linux internals, containers (08–10, 12) |

**Every day:**

1. **30 min:** learn the concepts (README).
2. **20 min:** answer the interview questions aloud.
3. **20 min:** run the chapter's program; change a parameter; predict; re-run; or practise a command
   (`ss`, `vmstat`, `strace`, `jstack`).
4. **10–20 min:** review notes and list gaps.

**End of each week:** a mock interview from this page and the
[networking question bank](../../Networking/11-Interview-Questions/README.md), without notes.

---

## 4. Final advice

At senior level, connect every concept to **a production incident or a design decision**:

- **Not** "TCP is reliable", **but** "retransmissions and slow start explain why our P99 rose under
  1 % packet loss, and why connection reuse fixed it".
- **Not** "virtual memory maps addresses", **but** "our container's RSS includes metaspace and direct
  buffers, so `-Xmx` at 75 % of the limit still got OOMKilled with exit 137".
- **Not** "deadlock has four conditions", **but** "two transfer endpoints locked accounts in opposite
  order; we enforced ordering by account ID and added lock timeouts".

**Learn the concept → explain the mechanism → state the trade-offs → apply it to a real scenario.**

---

**Back to:** [OS overview](../README.md) · [Networking course](../../Networking/README.md)
