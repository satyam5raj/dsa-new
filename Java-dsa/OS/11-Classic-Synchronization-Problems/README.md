# OS 11 · Classic synchronization problems

> These small puzzles are interview favourites because each one isolates a real failure mode:
> lost wake-ups, starvation, deadlock, unsafe resource allocation. For each: the problem, a correct
> solution with real threads, and **why** it's safe.

Run it: `java ClassicProblems.java` → **12/12 checks**:

- the bounded buffer with semaphores and as a monitor;
- readers–writers;
- dining philosophers (a **real deadlock**, then two fixes);
- the Banker's algorithm on the textbook instance;
- wait-for-graph deadlock detection.

---

## 1. Producer–consumer (bounded buffer), with semaphores

**Problem:** producers put items into a fixed-size buffer and consumers take them out. A producer
must wait when it's **full**, a consumer when it's **empty**, and the buffer itself must not be
corrupted.

```java
Semaphore empty = new Semaphore(N);   // free slots
Semaphore full  = new Semaphore(0);   // filled slots
Semaphore mutex = new Semaphore(1);   // protects the buffer indices

put(x):  empty.acquire(); mutex.acquire(); buf[in] = x; in = (in+1) % N; mutex.release(); full.release();
take():  full.acquire();  mutex.acquire(); x = buf[out]; out = (out+1) % N; mutex.release(); empty.release();
```

The run: 3 producers × 10,000 items, 2 consumers, a buffer of 8. **Every item was consumed exactly
once** (the sums match, 3,149,985,000), and the buffer never held more than **8**.

**Why it's safe:**

- `empty`/`full` **count** slots, so a producer can't overfill and a consumer can't take from
  nothing;
- `mutex` makes the index updates atomic.

**Order matters:** acquire `empty`/`full` **before** `mutex`. The other way round, a producer could
sleep on `empty` **while holding the mutex**, so no consumer could ever free a slot: deadlock.

## 2. Producer–consumer as a monitor

```java
put(x):  lock; while (q.size() == cap) notFull.await();  q.add(x);  notEmpty.signal(); unlock
take():  lock; while (q.isEmpty())     notEmpty.await(); x = q.poll(); notFull.signal(); unlock
```

1,000 items from one producer to one consumer arrived **all present and in FIFO order**. That's
essentially `ArrayBlockingQueue`.

**Why `while`, not `if`?** Spurious wake-ups happen, and another thread may grab the item between the
signal and our wake-up. Always re-check the condition.

## 3. Readers–writers

**Problem:** many readers may read simultaneously, but a writer needs exclusive access.

| Variant | Rule | Risk |
|---|---|---|
| **reader preference** (the classic first solution) | readers enter whenever no writer is **active** | **writers starve** under a continuous stream of overlapping readers |
| **writer preference** | once a writer **waits**, new readers queue | readers can starve |
| **fair** (FIFO) | arrival order | lower peak throughput |

The run hits a lock with **6 readers constantly overlapping**. With a **fair**
`ReentrantReadWriteLock(true)`, the writer got in after **3 ms**. A read-preferring lock could keep it
out indefinitely.

Real-world uses: config or route tables (read-mostly), caches. Alternatives: copy-on-write snapshots
(`CopyOnWriteArrayList`, atomic reference swaps), `StampedLock` optimistic reads, RCU in the kernel.

## 4. Dining philosophers

**Problem:** 5 philosophers sit around a table with 5 forks, one between each pair. Each needs
**both** neighbouring forks to eat.

| Strategy | Result |
|---|---|
| **naive:** everyone picks up the **left** fork, then the **right**, at the same moment | **DEADLOCK**: each holds one fork and waits for a neighbour's (a circular wait). The run confirms it: no meals completed |
| **resource ordering:** always pick up the **lower-numbered** fork first | the cycle is impossible (philosopher 4 reaches for fork 0 first, not fork 4): **10,000 meals** |
| **a waiter** (`Semaphore(4)`): at most 4 at the table | at least one philosopher can always get both forks: **10,000 meals** |
| (also) take both forks atomically, or put one back and retry with random back-off | breaks hold-and-wait, but can livelock without randomness |

Map it to the four deadlock conditions (OS 04 §6): ordering breaks **circular wait**; the waiter
breaks circular wait by limiting participants; `tryLock` + release breaks **hold and wait**.

## 5. Critical-section requirements

Any correct solution to the critical-section problem must provide:

1. **Mutual exclusion:** at most one thread in the critical section.
2. **Progress:** if nobody is inside, one of the waiting threads must get in (no indefinite
   postponement by threads not even competing).
3. **Bounded waiting:** a limit on how many times others can enter before a waiting thread does (no
   starvation).

Peterson's algorithm satisfies all three for two threads, but only with sequential consistency.
Modern CPUs reorder memory, so real code uses hardware atomics (CAS) and memory fences (OS 04 §5).

## 6. Deadlock avoidance: the Banker's algorithm

Each process declares its **maximum** claim up front. A request is granted only if the resulting
state is **safe**: some order exists in which every process can finish.

The classic textbook instance (resources A, B, C; available = 3, 3, 2):

| Process | Allocation | Max | Need = Max − Alloc |
|---|---|---|---|
| P0 | 0 1 0 | 7 5 3 | 7 4 3 |
| P1 | 2 0 0 | 3 2 2 | 1 2 2 |
| P2 | 3 0 2 | 9 0 2 | 6 0 0 |
| P3 | 2 1 1 | 2 2 2 | 0 1 1 |
| P4 | 0 0 2 | 4 3 3 | 4 3 1 |

**The safety algorithm:** `work = available`. Repeatedly find a process whose `need ≤ work`; pretend
it finishes, so `work += its allocation`. If every process finishes, the state is safe.

| Question | Answer from the run |
|---|---|
| Is the initial state safe? | **Yes**: the sequence **⟨P1, P3, P4, P0, P2⟩** |
| P1 requests (1, 0, 2) | the state stays safe → **grant** |
| **Then** P0 requests (0, 2, 0) | the resources **are available** (2, 3, 0), but the resulting state is **unsafe** → **P0 must wait** |

"Unsafe" ≠ "deadlocked": an unsafe state *may* lead to deadlock, and the Banker's algorithm refuses
to take the risk. It's rarely used in general-purpose OSes because processes can't declare maxima,
but the idea survives as **admission control** (don't accept work you can't finish:
Kubernetes resource requests, connection-pool limits, database admission).

## 7. Deadlock detection: resource-allocation / wait-for graphs

Draw an edge **Ti → Tj** when Ti waits for a resource Tj holds. **A cycle means a deadlock** (for
single-instance resources).

- The run finds **`[T1, T2, T3, T1]`**. T4 waits on T2 but isn't in the cycle: it's blocked
  *because of* the deadlock, not part of it.
- **Recovery:** abort or roll back a victim (cheapest, youngest, least work done). After removing
  T3's wait, **no cycle remains**.

Databases do exactly this. PostgreSQL checks for cycles after `deadlock_timeout` (1 s) and aborts one
transaction with "deadlock detected"; the application should **retry** it.

> **Implementation note:** my first cycle-reconstruction code walked the DFS path in the wrong
> direction and printed `[T1, T1]`. The check caught it. When reconstructing a cycle from a DFS
> stack, walk **from the repeated node forward** to the top of the stack.

---

## Practice

Implement each from memory with your language's primitives, then explain **why it's safe**:

1. a bounded blocking queue (both versions above);
2. a fair read-write lock (with a writer-waiting flag);
3. deadlock-free dining philosophers (ordering, and a waiter);
4. the Banker's safety check;
5. a wait-for-graph cycle detector.

---

**Next:** [12 · Linux internals and containers](../12-Linux-and-Containers/README.md)
