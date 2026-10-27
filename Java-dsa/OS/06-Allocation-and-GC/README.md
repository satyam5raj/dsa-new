# OS 06 · Memory allocation and garbage collection

> Garbage collection lives in the language runtime, not the OS. But production memory incidents
> happen exactly where the two meet: heap vs RSS, GC pauses vs latency, "we freed it but the
> container still got OOM-killed".

Run it: `java GarbageCollection.java` → **18/18 checks**:

- a toy heap with reference counting, mark-sweep, mark-compact, copying and generational ideas;
- the **real JVM**: weak references, GC MXBeans, a real static-cache leak surviving full GCs,
  heap committed vs used.

---

## 1. Allocation basics

| | How | Speed | Freed by |
|---|---|---|---|
| **stack allocation** | move the stack pointer | fastest | function return |
| **heap allocation** (`malloc`/`new`) | find a free block (free lists, size classes) or bump a pointer | slower | `free`/`delete`, or GC |

**Allocators:**

- glibc malloc (per-thread **arenas**, free lists, `brk` for small and `mmap` for large blocks);
- **jemalloc** / **tcmalloc** (size classes, thread caches; better for fragmentation-prone,
  multi-threaded servers);
- the JVM's **TLABs** (thread-local allocation buffers: a pointer bump, nearly free).

Terms you should know:

- **Fragmentation:** OS 05 §8.
- **Memory leak:** memory that's no longer needed but still referenced (GC languages) or never freed
  (C/C++).
- **Ownership:** who is responsible for freeing (C++ RAII, `unique_ptr`; Rust's borrow checker).
- **Object lifetime:** how long things stay reachable.
- **Allocation pressure:** the allocation **rate** (MB/s). More of it means more frequent GCs, even
  without leaks.

## 2. GC roots and reachability

A **tracing** GC keeps everything **reachable from the roots** and frees the rest:

- **roots:** thread stacks (locals), static fields, CPU registers, JNI handles, class loaders;
- the toy heap: root → **A → B → C**; **D ↔ E** (a cycle, unreachable); **F** (unreferenced).
  Live set = **[A, B, C]**.

## 3. Reference counting vs tracing

**Reference counting:** each object counts incoming references and is freed when the count hits 0.

- ✅ Immediate, incremental reclamation; predictable.
- ❌ **Cycles leak:** D ↔ E keep each other at count **1/1** forever, though nothing reaches them.
- ❌ Count updates on every assignment (an atomic in multi-threaded code: slow).

Used by CPython (plus a cycle collector), Swift/Objective-C ARC, and C++ `shared_ptr` (break cycles
with `weak_ptr`).

**Tracing** (mark from the roots) handles cycles naturally. Every serious JVM, .NET, Go and V8 GC is
tracing.

## 4. Mark-and-sweep

1. **Mark:** traverse from the roots and mark everything reachable.
2. **Sweep:** walk the heap; unmarked objects go onto **free lists**.

In the toy, the sweep freed **D, E (the cycle) and F: 60 bytes**. Downsides: **fragmentation** (freed
objects are scattered), and allocation needs free-list searches.

## 5. Mark-and-compact

After marking, **slide live objects together**: the layout became **`A@0 B@10 D@30 E@60`**, leaving
**one free region 70..100**. Allocation is back to a cheap pointer bump, and there's no
fragmentation. The cost is moving objects and updating every reference (with pauses or clever
concurrent techniques).

## 6. Copying collectors

Split the space in two. Copy **only live objects** from the from-space to the to-space, then the
whole from-space is free.

- Work ∝ **live data** (40 bytes in the toy), **not** heap size (100): garbage costs nothing.
- The result is compact by construction.
- Half the space is reserved. That's ideal for regions where **most objects are dead**, which leads
  to…

## 7. Generational GC

**The weak generational hypothesis:** *most objects die young* (request objects, temporary strings,
iterators). In the toy, **~5 %** survived a minor GC, so a young collection copies only **4,993
survivors instead of tracing 100,000 objects (20× less work)**.

- **Young generation** (eden + survivor spaces): collected often with a fast copying collector
  (**minor GC**).
- Objects that survive several minor GCs are **promoted** to the **old generation**, collected
  rarely (**major/full GC**), with mark-sweep/compact or concurrent marking.
- It needs a **write barrier + card table**: when old code stores a pointer to a young object, the
  barrier records it, so a minor GC doesn't have to scan the whole old generation.

The real JVM in this run uses **G1** (`G1 Young Generation`, `G1 Concurrent GC`, `G1 Old
Generation`): a generational, **region-based**, mostly concurrent collector.

| JVM collector | Style | Pauses |
|---|---|---|
| Serial / Parallel | stop-the-world, generational | short heaps / max throughput |
| **G1** (default) | regions, concurrent marking, incremental compaction | target `-XX:MaxGCPauseMillis` (200 ms default) |
| **ZGC** / Shenandoah | concurrent compaction (load barriers) | **sub-millisecond**, even with huge heaps |

## 8. Stop-the-world, incremental and concurrent collection

- **Stop-the-world (STW):** all application threads pause while the GC works. That's simple and
  correct, but a **latency spike** for every request in flight.
- **Incremental:** do GC work in small slices between application work.
- **Concurrent:** GC threads mark (and sometimes compact) **while the application runs**, using
  barriers to track changes. Short STW phases remain (root scanning, final remark).

**Why can garbage collection cause latency spikes?** STW pauses freeze every request, so P99 jumps by
the pause length. Promotion failure or "to-space exhausted" forces a slow **full GC**. High
allocation rates trigger GCs more often. Concurrent collectors steal CPU, and in a container that
can cause **CPU throttling** (OS 12). Large heaps with STW collectors mean long pauses.

The fixes:

- allocate less (reuse buffers, avoid boxing, stream instead of building huge lists);
- right-size the heap;
- choose a low-pause collector (ZGC);
- leave CPU headroom;
- monitor GC pause time as an SLO.

## 9. The real JVM

| Experiment | Result |
|---|---|
| an object reachable **only** through a `WeakReference` | **collected** after GC |
| one that is also strongly reachable | **survives** |
| GC MXBeans | collectors `[G1 Young Generation, G1 Concurrent GC, G1 Old Generation]`, counts, total pause time |
| a **static `List` cache** filled with 50 MB and never evicted | heap used after full GC: **4 MB → 54 MB**, and it **survives** full GCs |
| setting the reference to `null` | the GC reclaims it |

**Weak, soft and phantom references** are used for caches (`WeakHashMap`), canonicalisation
mappings, and cleanup (`Cleaner`).

> **A leak in a GC language = unwanted objects that are still reachable.** Classic culprits:
>
> - unbounded static caches or maps;
> - listeners never unregistered;
> - `ThreadLocal`s in pools;
> - per-request data stored in long-lived objects;
> - growing queues (no backpressure, Networking 09).

**How can a service use lots of memory without a traditional leak?**

- **Caches** sized too large or unbounded.
- High **allocation pressure** with a big young generation.
- A **large heap ceiling**: the GC grows the heap if allowed.
- Retained data structures proportional to load (queues, in-flight requests).
- **Native memory:** direct buffers, threads (stacks), Netty arenas.
- **Fragmentation** in malloc arenas.
- Memory-mapped files.

## 10. Allocated vs resident memory; RSS after GC

| Metric | Meaning |
|---|---|
| **virtual size (VSZ)** | all mapped address space, including reserved-but-untouched (huge and mostly meaningless) |
| **RSS (resident set size)** | pages **actually in RAM** for this process (shared pages counted fully) |
| **PSS** | RSS with shared pages divided among their sharers |
| heap **used** | live + not-yet-collected objects |
| heap **committed** | memory the runtime has claimed from the OS for the heap |

The run reports heap **used 4 MB, committed 28 MB**, plus 21 MB of non-heap (metaspace, code cache).

**Why might process RSS stay high after objects have been garbage-collected?**

- The runtime **keeps freed heap committed** for future allocations instead of returning it to the
  OS (G1 uncommits only lazily, e.g. after periodic or full GCs; ZGC uncommits after a delay).
- malloc arenas are fragmented, so free chunks can't be returned.
- RSS also includes metaspace, code cache, thread stacks, direct buffers and mapped files.

That's why **container limits must cover the whole footprint**, not just `-Xmx`. Rule of thumb: set
the container limit at ~1.3–1.5× the heap, or use `-XX:MaxRAMPercentage`.

## 11. Node.js / V8 specifics

V8 has a generational heap:

- a **young generation** ("new space", Scavenge = copying);
- an **old generation** (mark-sweep-compact, mostly concurrent and incremental, the Orinoco
  project).

Old-space size is limited by `--max-old-space-size`. `Buffer`s live **outside** the V8 heap (native
memory), which shows up in RSS but not `heapUsed`.

**Investigating a steadily increasing Node.js heap:**

1. Watch `process.memoryUsage()` (`rss`, `heapUsed`, `external`, `arrayBuffers`) over time.
2. If `heapUsed` keeps growing **after GCs**: take **heap snapshots** (`--inspect` + Chrome DevTools,
   `heapdump`) a few minutes apart and compare. Look at **retainers** of growing object types.
3. Typical causes:
   - closures capturing large objects;
   - global maps or caches;
   - event listeners not removed (`MaxListenersExceededWarning`);
   - unresolved promises piling up;
   - unbounded queues.
4. If `rss` grows while `heapUsed` is flat: **Buffers / native addons**.

---

## Senior-level questions

**1. How can a service experience high memory usage without a traditional memory leak?** Big or
unbounded caches, a high allocation rate with a generous heap, load-proportional data structures,
native memory (buffers, stacks), malloc fragmentation, mapped files, and heaps committed but not
returned (§9–10).

**2. What is the difference between allocated memory and resident memory?** Allocated (virtual)
memory is address space handed out; resident (RSS) is the part physically in RAM now. Allocation is
lazy (pages become resident on first touch), and pages can be swapped out or shared.

**3. Why can garbage collection cause latency spikes?** Stop-the-world phases pause all request
threads; full GCs on large heaps are long; high allocation rates make GCs frequent; concurrent GCs
compete for CPU (and quota in containers) (§8).

**4. How would you investigate a steadily increasing Node.js heap?** §11: `memoryUsage` trends,
heap snapshots and diffing, retainer chains, common leak patterns, and heap vs external/RSS.

**5. Why might process RSS remain high after objects have been garbage-collected?** The runtime keeps
the committed heap; allocator fragmentation; non-heap memory; lazy return of memory to the OS
(§10).

---

**Next:** [07 · Page replacement](../07-Page-Replacement/README.md)
