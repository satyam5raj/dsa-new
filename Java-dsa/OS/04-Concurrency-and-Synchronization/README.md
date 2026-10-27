# OS 04 · Concurrency and synchronization (essential)

> Shared memory makes threads fast, and it makes bugs that appear once a week in production. This
> chapter makes those bugs happen **for real**, then fixes each one with the right primitive.

Run it: `java Concurrency.java` → **18/18 checks**. Real threads throughout:

- a lost-update race;
- `synchronized` / `AtomicInteger` / `LongAdder` timings;
- CAS and a lock-free stack;
- a semaphore, a read-write lock, a condition variable, a barrier, `volatile`;
- a **real deadlock detected with `ThreadMXBean`**, then avoided two ways.

---

## 1. Concurrency vs parallelism

- **Concurrency:** *dealing* with many things at once. Tasks overlap in time, maybe interleaved on
  one core (an event loop, OS 09).
- **Parallelism:** *doing* many things at once, literally simultaneously on several cores.

Concurrency is about **structure**; parallelism is about **execution**. You can have either without
the other.

## 2. Race conditions and critical sections

A **race condition** happens when the result depends on the timing of threads touching shared data
without coordination.

`count++` looks atomic but is three steps: **read** count, **add** 1, **write** it back. Two
threads can both read 41, both write 42, and one increment is lost.

**Real result:** 8 threads × 200,000 increments = 1,600,000 expected; the unsafe counter ended at
**243,663**, so **1,356,337 updates were lost**.

A **critical section** is code that touches shared state and must not run in more than one thread at
a time. Requirements: **mutual exclusion**, **progress** (no needless blocking), **bounded waiting**
(no starvation).

Classic production races:

- check-then-act (`if (!map.containsKey(k)) map.put(k, v)`);
- lazy initialisation;
- read-modify-write on balances;
- unsynchronised `HashMap` resizing.

## 3. The synchronisation toolbox

| Primitive | Main purpose | In the run |
|---|---|---|
| **mutex / lock** (`synchronized`, `ReentrantLock`) | exclusive access to a critical section | exact 1,600,000 |
| **atomic operation** (`AtomicInteger`) | a single-variable update with **no partial state**, using hardware CAS | exact 1,600,000 |
| **striped counter** (`LongAdder`) | spread a hot counter over cells, sum on read | exact 1,600,000 |
| **semaphore** | at most N concurrent holders (a resource pool), or signalling | never more than **3** of 12 inside |
| **read-write lock** | many readers **or** one writer | readers overlapped; a writer **never** overlapped a reader |
| **condition variable** | sleep until a condition holds | the consumer is `WAITING` (no CPU), then wakes to `payload` |
| **monitor** | lock + condition variables bundled (Java's `synchronized` + `wait/notify`) | |
| **barrier** | everyone waits until all arrive | `AAAA|BBBB|`: no phase B before every A |
| **spinlock** | busy-wait for a lock | (kernel / very short sections only) |

**Timings for 1.6M contended increments in this run:** `synchronized` 86 ms, `AtomicInteger` 71 ms,
`LongAdder` **39 ms**. One hot atomic makes every core fight over the same **cache line**; LongAdder
spreads the updates over several cells. It usually wins under heavy contention, but timings vary by
run and machine.

### Mutex vs semaphore

| | Mutex | Semaphore |
|---|---|---|
| holders | **1** | up to **N** (a counting semaphore) |
| ownership | the thread that locked it must unlock it | any thread can release (used for **signalling**) |
| use | protect a critical section | limit concurrency (connection pools, rate of work); producer–consumer signalling (OS 11) |

A binary semaphore (N = 1) behaves like a lock without ownership. That's useful for signalling,
risky as a mutex.

### Spinlocks

They busy-wait instead of sleeping. Good only when the wait is **shorter than a context switch**,
with **no preemption** while holding the lock (kernels disable preemption or interrupts). In user
space, prefer adaptive locks (spin briefly, then park). Java's locks already do this.

## 4. Atomic operations and compare-and-swap (CAS)

**CAS(address, expected, new):** atomically, *if the value equals `expected`, set it to `new`*;
report success. One CPU instruction (`cmpxchg` on x86, LL/SC on ARM).

```
x = 5:  CAS(5 → 6) succeeds;  CAS(5 → 7) fails (the value is now 6)
```

**Lock-free algorithms** retry CAS in a loop. The Treiber stack's `push`:

```java
while (true) {
    Node old = head.get();
    if (head.compareAndSet(old, new Node(v, old))) return;   // fails if someone pushed meanwhile → retry
}
```

160,000 concurrent pushes, **none lost or duplicated**, with **332,110 CAS retries**. Threads never
block; they retry.

| | Guarantee |
|---|---|
| **blocking** (locks) | a thread holding the lock can stall everyone (preempted, page-faulted, GC'd) |
| **lock-free** | **some** thread always makes progress (system-wide progress); individual threads may retry forever in theory |
| **wait-free** | **every** thread finishes in a bounded number of steps (strongest, hardest; e.g. some wait-free queues, `AtomicInteger.getAndAdd` on x86) |

Pitfalls of lock-free code:

- **ABA** (a value changes A → B → A, so the CAS wrongly succeeds; fix with version stamps,
  `AtomicStampedReference`);
- memory reclamation in non-GC languages;
- it's very hard to get right. Use library structures (`ConcurrentHashMap`,
  `ConcurrentLinkedQueue`).

## 5. Memory visibility, reordering and memory models

Without synchronisation, a thread may **never see** another thread's write:

- the CPU keeps it in a store buffer or cache;
- the compiler or JIT keeps the value in a register, or hoists the read out of a loop;
- both CPUs and compilers **reorder** independent instructions.

**Memory models** (Java JMM, C++11) define when writes become visible through **happens-before**
edges:

- `volatile` write → a later `volatile` read of the same variable;
- `unlock` → a later `lock` of the same monitor;
- `Thread.start()` → the started thread's actions; a thread's actions → `join()` returning;
- `executor.submit()` → the task's execution; `Future.get()` after the task.

In the run, a worker loops `while (!stop)`. Because `stop` is **`volatile`**, the main thread's write
is seen and the loop exits. Without `volatile` the JIT may hoist the read and spin forever: a real,
intermittent, works-in-debug-mode bug. The same family includes broken double-checked locking (fix
it with `volatile`).

## 6. Deadlock

### The four necessary conditions (all must hold)

1. **Mutual exclusion:** resources can't be shared.
2. **Hold and wait:** a thread holds one resource while waiting for another.
3. **No preemption:** resources can't be forcibly taken away.
4. **Circular wait:** T1 waits for T2 waits for … waits for T1.

### A real one

```
thread transfer-A-to-B:  lock(A) … lock(B)
thread transfer-B-to-A:  lock(B) … lock(A)     ← each holds one, waits for the other: forever
```

`ThreadMXBean.findDeadlockedThreads()` (what `jstack` uses to print "Found one Java-level
deadlock") returned **`[transfer-A-to-B, transfer-B-to-A]`**.

### Handling deadlock

| Strategy | How | In the run |
|---|---|---|
| **prevention:** break a condition | **global lock ordering** (always A then B) breaks *circular wait*; acquiring all locks at once breaks *hold and wait*; `tryLock` + release breaks *no preemption* | ordering: **20,000** successful iterations; `tryLock(10 ms)` + jittered back-off with **opposite** orders: **4,000/4,000** |
| **avoidance** | grant a request only if the system stays in a **safe state** (the Banker's algorithm, OS 11) | |
| **detection + recovery** | build the wait-for graph, find cycles, kill or roll back a victim | databases do this: one transaction gets "deadlock detected" and must retry |
| **ignore** | hope (common in practice!) plus timeouts | |

**Livelock:** threads keep reacting to each other without progress (two people stepping aside in a
corridor forever). Randomised back-off breaks the symmetry; that's why the `tryLock` fix sleeps a
**random** 0–1 ms.

**Starvation:** a thread never gets the lock (unfair locks, constant newcomers). `new
ReentrantLock(true)` is fair, at a throughput cost.

### Diagnosing a deadlock in production

1. **Symptom:** throughput drops to zero for some endpoints; threads are stuck; CPU is idle (unlike
   a livelock).
2. **Thread dump:** `jstack <pid>` or `jcmd <pid> Thread.print` (or `kill -3`). Look for
   "Found one Java-level deadlock", or threads `BLOCKED` waiting on locks held by each other. Take
   2–3 dumps a few seconds apart.
3. Other runtimes: `py-spy dump`; `gdb thread apply all bt` for native code; database deadlock
   graphs (`SHOW ENGINE INNODB STATUS`, `pg_locks`).
4. **Fix:** a consistent lock order, smaller lock scope (no calls to external services while
   holding a lock), timeouts, or a lock-free or single-writer design.

### Lock contention

Many threads wanting the same lock means they **queue**: throughput **flattens or drops** as threads
increase, CPU goes to parking and unparking, and latency rises. Find it with thread dumps (many
`BLOCKED` on the same monitor), JFR lock profiling, or async-profiler `-e lock`.

Fixes:

- shorten critical sections;
- **shard** the lock (striping, `ConcurrentHashMap` bins, `LongAdder`);
- read-write locks for read-heavy data;
- immutable or copy-on-write data;
- per-thread or batched work, then merge.

---

## Coding practice (common interview asks)

1. **A thread-safe counter:** `AtomicLong` / `LongAdder`, or `synchronized` (§3).
2. **A producer–consumer queue:** a bounded buffer with a lock + two conditions, or semaphores
   (OS 11 implements both).
3. **A bounded resource pool:** a `Semaphore(N)` plus a concurrent queue of resources; `acquire`
   with a timeout, `release` in `finally` (§3).

---

## Senior-level questions

**1. What is a race condition? Give an example.** The outcome depends on thread timing over shared
unsynchronised state. For example, `count++` from 8 threads lost 1.36M of 1.6M updates; or
check-then-act on a map; or two requests both reading a balance of 100 and both withdrawing 80.

**2. How do mutexes and semaphores differ?** A mutex gives exclusive ownership (one holder, released
by the owner). A semaphore is a counter allowing N holders, releasable by any thread, used for
limiting concurrency and signalling.

**3. Explain the four necessary conditions for deadlock.** Mutual exclusion, hold and wait, no
preemption, circular wait. Break any one (lock ordering is the usual fix) to prevent deadlock.

**4. What is the difference between lock-free and wait-free algorithms?** Lock-free guarantees system-
wide progress (some thread always completes; others may retry). Wait-free guarantees every thread
completes in bounded steps. Both avoid blocking on preempted lock holders.

**5. How can lock contention degrade throughput?** Threads serialise on the lock, queue and
park/unpark (context switches), and cache lines bounce between cores. Adding threads adds waiting,
not throughput. Reduce critical sections, shard, or go lock-free.

**6. How would you diagnose a deadlock in a production application?** Several thread dumps (jstack/
jcmd); look for the deadlock report or a cycle of BLOCKED threads and their held locks; correlate
with the code paths; fix the lock order or remove nested locking; add lock timeouts and metrics.

---

**Next:** [05 · Memory management](../05-Memory-Management/README.md)
