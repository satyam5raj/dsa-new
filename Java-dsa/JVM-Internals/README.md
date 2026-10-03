# JVM Internals

Study the folders in order. Each file inspects the **JVM it is running in** (memory pools, GC counters, class loaders, JIT statistics, flag values), so the numbers you see are real numbers from your own machine.

| # | Folder | Files | What you'll learn |
|---|--------|-------|-------------------|
| 1 | [01-JVM-Architecture-And-Memory](01-JVM-Architecture-And-Memory/) | `JvmMemoryAreas` | JVM architecture, heap (young: Eden + survivors, old gen), metaspace, stacks and frames, code cache, direct memory, live memory pools, StackOverflowError depth, where each kind of value lives, object header sizes and the cost of boxing |
| 2 | [02-Garbage-Collection](02-Garbage-Collection/) | `GarbageCollectionDemo`, `GcWorkload`, `compare-gcs.sh` | GC roots and reachability (cycles get collected), mark/sweep/compact/copy, the generational hypothesis, Serial/Parallel/G1/ZGC/Shenandoah/Epsilon, G1 regions and mixed collections, strong/soft/weak/phantom references, WeakHashMap, Cleaner vs finalize, and a **side-by-side comparison of the collectors** |
| 3 | [03-Class-Loading](03-Class-Loading/) | `ClassLoadingDemo` | Load → link (verify, prepare, resolve) → initialize, what triggers initialization and what doesn't, the Bootstrap/Platform/App hierarchy, parent delegation, ClassNotFoundException vs NoClassDefFoundError, and a **custom ClassLoader hot-reloading a class compiled in memory** |
| 4 | [04-JIT-Compilation](04-JIT-Compilation/) | `JitDemo` | Interpreter vs JIT, tiered compilation (C1/C2), OSR, **measured warm-up**, inlining, **escape analysis (0 GCs vs 9)**, monomorphic vs megamorphic call sites, deoptimization, the dead-code benchmark trap and why to use JMH |
| 5 | [05-Memory-Leaks](05-Memory-Leaks/) | `MemoryLeaksDemo` | The kinds of OutOfMemoryError, **7 real leak patterns, each measured** (static caches, listeners, ThreadLocal on pools, inner classes, bad map keys...), a class histogram via `jcmd`, a programmatic heap dump, the MAT/JFR diagnosis playbook |
| 6 | [06-JVM-Tuning-Flags](06-JVM-Tuning-Flags/) | `JvmFlagsDemo`, `run-with-prod-flags.sh` | Heap, GC, observability and JIT flags; container-aware sizing; reading effective flag values and where they came from; a production baseline; a tuning method |

## Results from this machine (Apple Silicon, 10 cores, JDK 21)

These numbers came from running the files. Yours will differ, but the patterns should hold.

| Demo | Result |
|---|---|
| Default max heap | 6 GB = 25% of 24 GB RAM |
| Stack depth before StackOverflowError | ~47,000 frames (default stack) vs ~2.6 million (64 MB stack) |
| 2 million ints | `int[]` ≈ 8 MB vs `List<Integer>` ≈ 31 MB |
| GC comparison, worst app pause (256 MB heap) | Serial ~26 ms · Parallel ~7 ms · G1 ~7 ms · ZGC ~8 ms |
| JIT warm-up | first round ~2.8 ms → ~0.49 ms once compiled |
| Escape analysis, 100 million objects | non-escaping: 0 young GCs · escaping: 9 young GCs |
| Leaks | static map's live heap rises by ~20 MB per batch · ThreadLocal on 8 pool threads holds 64 MB · inner class retains 24 MB |

## Handy commands

```bash
java -XX:+PrintFlagsFinal -version | grep -i heap       # effective flag values
java -Xlog:gc GcWorkload.java                            # one line per GC event
java -XX:+PrintCompilation JitDemo.java                  # what the JIT compiles (% = OSR)
jcmd                                                     # list running JVMs
jcmd <pid> GC.class_histogram                            # what is filling the heap
jcmd <pid> GC.heap_dump /tmp/heap.hprof                  # heap dump for MAT / VisualVM
jcmd <pid> Thread.print                                  # thread dump (deadlocks show here)
jcmd <pid> VM.flags                                      # flags of a running JVM
jcmd <pid> JFR.start duration=60s filename=rec.jfr       # flight recording
```

## Interview questions to test yourself

1. Describe the JVM runtime data areas. Which are shared, and which are per thread? *(JvmMemoryAreas)*
2. What lives on the stack vs the heap? Where do static fields and the string pool live? *(JvmMemoryAreas)*
3. PermGen vs Metaspace: what changed and why? *(JvmMemoryAreas)*
4. How does the GC decide that an object is garbage? Can cycles leak? *(GarbageCollectionDemo)*
5. Explain minor vs major GC and the generational hypothesis. *(GarbageCollectionDemo)*
6. Compare Serial, Parallel, G1 and ZGC. Which would you choose for a low-latency API, and which for a batch job? *(GarbageCollectionDemo, compare-gcs.sh)*
7. Soft vs weak vs phantom references: give a use case for each. *(GarbageCollectionDemo)*
8. Why is `finalize()` deprecated? What replaces it? *(GarbageCollectionDemo)*
9. Walk through class loading phases. When exactly does static initialization run? *(ClassLoadingDemo)*
10. What is parent delegation, and why does it exist? When is it broken on purpose? *(ClassLoadingDemo)*
11. ClassNotFoundException vs NoClassDefFoundError? *(ClassLoadingDemo)*
12. How does the JIT work? What are C1, C2 and tiered compilation? *(JitDemo)*
13. What is escape analysis? What is deoptimization? *(JitDemo)*
14. Why do micro-benchmarks lie? *(JitDemo)*
15. How can Java leak memory with a GC? Name 5 causes. *(MemoryLeaksDemo)*
16. Production is throwing OutOfMemoryError. Walk me through how you diagnose it. *(MemoryLeaksDemo)*
17. Which JVM flags would you set for a service in Kubernetes, and why? *(JvmFlagsDemo)*
18. How would you approach GC tuning? *(JvmFlagsDemo)*

Next topic: **Spring / Spring Boot**.
