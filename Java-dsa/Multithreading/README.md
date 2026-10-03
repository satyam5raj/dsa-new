# Multithreading & Concurrency

Study the folders in order. Every file is self-contained and runs with `java FileName.java` (Java 21).
Each demo **shows the bug happening** (lost updates, a never-ending loop, a real deadlock) before showing the fix. Bugs that depend on timing may look slightly different on each run. That is the nature of concurrency bugs.

| # | Folder | Files | What you'll learn |
|---|--------|-------|-------------------|
| 1 | [01-Thread-Basics](01-Thread-Basics/) | `ThreadBasics` | Process vs thread, the lifecycle with **every state observed live**, Thread vs Runnable vs Callable, `start()` vs `run()`, `join`, cooperative interruption, daemon threads, uncaught exception handlers |
| 2 | [02-Synchronization-And-Locks](02-Synchronization-And-Locks/) | `SynchronizedAndWaitNotify`, `ExplicitLocks` | Race conditions (read-modify-write, check-then-act), `synchronized` methods and blocks, reentrancy, producer-consumer with wait/notify, the odd/even printer, ReentrantLock (tryLock, lockInterruptibly, fairness), Condition, ReadWriteLock, StampedLock |
| 3 | [03-Java-Memory-Model](03-Java-Memory-Model/) | `VolatileAndJMM` | Visibility (**a loop that never sees the write**), reordering, happens-before rules, `volatile` (and why it isn't atomic), safe publication, CAS and atomics, LongAdder vs AtomicLong, the ABA problem |
| 4 | [04-Executor-Framework](04-Executor-Framework/) | `ExecutorFramework` | execute vs submit, invokeAll/invokeAny, a **ThreadPoolExecutor walkthrough** (core → queue → max → reject), rejection policies and backpressure, scheduling, graceful shutdown, virtual threads vs platform pools |
| 5 | [05-Concurrent-Collections](05-Concurrent-Collections/) | `ConcurrentCollections` | The pitfall of synchronized wrappers, atomic operations on ConcurrentHashMap (merge, computeIfAbsent), bulk operations, CopyOnWriteArrayList, BlockingQueue producer-consumer with poison pills, DelayQueue, SynchronousQueue, ConcurrentSkipListMap |
| 6 | [06-Future-And-CompletableFuture](06-Future-And-CompletableFuture/) | `FutureAndCompletableFuture` | Limits of Future, CF pipelines, **thenApply vs thenCompose**, thenCombine/allOf/anyOf, exceptionally/handle/whenComplete, timeouts, a real-world parallel aggregator with fallbacks |
| 7 | [07-Deadlock-Livelock-Starvation](07-Deadlock-Livelock-Starvation/) | `ConcurrencyHazards` | A **real deadlock detected with ThreadMXBean**, the 4 Coffman conditions, fixes (lock ordering, tryLock with back-off), a livelock demo and its fix, starvation and fair locks |
| 8 | [08-Synchronizers](08-Synchronizers/) | `Synchronizers` | CountDownLatch (startup wait, starting gun), CyclicBarrier (phased work), Semaphore (limiting concurrency), Phaser, Exchanger |
| 9 | [09-Fork-Join](09-Fork-Join/) | `ForkJoinDemo` | Divide and conquer, work stealing, RecursiveTask/RecursiveAction, parallel merge sort, choosing thresholds, the link to parallel streams, and why parallel isn't always faster |

## Which tool should I use?

| Need | Use |
|---|---|
| A simple flag one thread writes and others read | `volatile boolean` |
| A counter or ID generator | `AtomicInteger` / `AtomicLong` (or `LongAdder` for metrics) |
| Several variables updated together | `synchronized` (or `ReentrantLock` if you need tryLock, timeouts or Conditions) |
| Many reads, few writes | `ReadWriteLock` / `StampedLock` / `CopyOnWriteArrayList` |
| A shared map | `ConcurrentHashMap` with `merge`/`compute` |
| Handing work between threads | `BlockingQueue` |
| Running tasks | `ExecutorService` (bounded `ThreadPoolExecutor` in production) |
| Lots of blocking I/O | Virtual threads |
| Async pipelines and combining results | `CompletableFuture` with your own executor |
| Waiting for N events | `CountDownLatch` |
| Limiting concurrent access to a resource | `Semaphore` |
| CPU-heavy divide and conquer | Fork/Join or parallel streams |

## Interview questions to test yourself

1. Walk through the thread lifecycle states. What moves a thread into BLOCKED vs WAITING? *(ThreadBasics)*
2. `start()` vs `run()`? Runnable vs Callable? *(ThreadBasics)*
3. How do you stop a thread correctly? Why must you not swallow InterruptedException? *(ThreadBasics)*
4. Why is `count++` not thread-safe? What is check-then-act? *(SynchronizedAndWaitNotify)*
5. `sleep()` vs `wait()`? Why must `wait()` be called in a while loop? *(SynchronizedAndWaitNotify)*
6. Write producer-consumer with wait/notify, then with BlockingQueue. *(02, 05)*
7. synchronized vs ReentrantLock: when would you choose each? *(ExplicitLocks)*
8. What does `volatile` guarantee and what doesn't it? Explain happens-before. *(VolatileAndJMM)*
9. How does CAS work? What is the ABA problem? *(VolatileAndJMM)*
10. Explain how ThreadPoolExecutor handles a new task. Why can `newFixedThreadPool` cause an OOM? *(ExecutorFramework)*
11. How would you size a thread pool for CPU-bound vs I/O-bound work? *(ExecutorFramework)*
12. When should you use virtual threads, and when shouldn't you? *(ExecutorFramework)*
13. How is ConcurrentHashMap thread-safe? Why is `get` then `put` still a bug? *(ConcurrentCollections)*
14. thenApply vs thenCompose? How do you handle errors and timeouts in CompletableFuture? *(FutureAndCompletableFuture)*
15. What are the 4 conditions for deadlock? How do you prevent and detect one? *(ConcurrencyHazards)*
16. Deadlock vs livelock vs starvation? *(ConcurrencyHazards)*
17. CountDownLatch vs CyclicBarrier vs Semaphore? *(Synchronizers)*
18. How does work stealing work in ForkJoinPool? *(ForkJoinDemo)*

Next topic: **JVM Internals**.
