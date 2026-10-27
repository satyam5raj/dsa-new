# OS 05 · Memory management (study deeply)

> Every pointer in your program is a **virtual** address. The OS and the CPU's MMU translate it,
> page by page, into physical RAM, or into a page fault. Understanding this explains memory usage
> graphs, container OOM kills, `fork()` behaviour, and why some loops are 6× slower than others.

Run it: `java Memory.java` → **22/22 checks**:

- a two-level page-table MMU simulator with a TLB, page faults and protection;
- copy-on-write;
- a **real** cache-locality measurement;
- first-fit vs best-fit fragmentation;
- a **real** `mmap`;
- a child JVM hitting **real** `OutOfMemoryError`.

---

## 1. Physical vs virtual memory

- **Physical memory:** the actual RAM chips, addressed by frame.
- **Virtual memory:** each process gets its **own private address space** (e.g. 0 to 2⁴⁸ on x86-64)
  that **looks** contiguous and starts at the same addresses for every process.

Why bother?

1. **Isolation:** process A can't even *name* B's memory (OS 01).
2. **Simplicity:** every program links at the same addresses.
3. **Overcommit:** address space > RAM. Pages are only backed when touched, and can live on disk.
4. **Sharing:** the same physical page (libc code, a shared memory segment) is mapped into many
   processes.
5. **Tricks:** copy-on-write, memory-mapped files, guard pages, lazy allocation.

### Layout of a process's address space

```
high  ┌──────────────────────┐
      │ kernel (inaccessible)│
      │ stack (grows down) ↓ │  ← one per thread; guard page below
      │        ...           │
      │ memory-mapped region │  ← shared libraries, mmap'd files, big malloc blocks, Java heap
      │        ...           │
      │ heap (grows up) ↑    │  ← malloc/new (brk/mmap)
      │ BSS / data           │  ← globals
low   │ text (code, r-x)     │  ← read-only + executable
      └──────────────────────┘   page 0 unmapped (so NULL dereferences fault)
```

## 2. Paging and address translation

Memory is split into fixed-size **pages** (virtual) and **frames** (physical), typically **4 KB**
(huge pages: 2 MB or 1 GB). A virtual address = **page number + offset**.

```
32-bit VA 0x12345ABC  =  0001001000 | 1101000101 | 101010111100
                         dir idx 72   table idx 837   offset 0xABC (2748)
page 0x12345 → page table entry → frame 7   ⇒   PA = 7 × 4096 + 0xABC = 0x7ABC
```

A **page table entry** holds the frame number plus flags:

- **present** (in RAM?);
- **writable**;
- **user** (accessible from user mode?);
- accessed/dirty bits (used by replacement, OS 07);
- no-execute.

**Multi-level page tables:** a flat table for a 48-bit space would be enormous, so it's a tree
(2 levels in the toy; 4–5 on x86-64). Only the parts actually used get allocated.

## 3. The TLB (translation lookaside buffer)

Walking 4 levels on every access would quadruple memory traffic. The **TLB** caches recent
translations (tens to a few thousand entries).

| Access pattern (toy MMU, 64-entry TLB) | TLB result |
|---|---|
| sequential scan of 4 MB, 64-byte steps | **63,000 hits / 1,001 misses**: one miss per page, then 63 hits |
| random over 1,000 pages | **6.5 % hit rate**: the TLB thrashes |

Effective access time with 100 ns RAM: **~104 ns sequential vs ~287 ns random**. That's why
**huge pages** (2 MB: one TLB entry covers 512× more memory) speed up big-heap databases and JVMs
(`-XX:+UseLargePages`, transparent huge pages). A context switch to another process invalidates the
TLB (or tags it with PCID/ASID).

## 4. Page faults and demand paging

A **page fault** is the CPU saying "I can't translate this address right now", and it traps into
the kernel:

| Case | Kernel response | Cost |
|---|---|---|
| valid address, page **not yet in RAM, no I/O needed** (first touch of fresh memory, a page in the page cache, copy-on-write) | allocate or zero a frame, or map the cached page | **minor fault**, ~µs |
| valid address, page **on disk** (swapped out, or a file-backed page not cached) | read it from disk, then map it | **major fault**, ~ms (SSD 0.1 ms, HDD 10 ms) |
| **invalid** address or permission | send **SIGSEGV** | crash (or a Java NPE, OS 01) |

**Demand paging:** memory isn't loaded until first touched. Scanning 1,000 fresh pages caused **900
minor + 100 major faults** in the toy (every 10th page had been swapped out). On Linux check
`ps -o min_flt,maj_flt -p <pid>`, `/proc/<pid>/stat`, `vmstat` (`si`/`so` = swap in/out).

**Protection, checked in the toy MMU:**

- reading `0x0` → **SIGSEGV: unmapped** (why NULL dereferences crash);
- writing a read-only page (code, string literals) → SIGSEGV;
- user code touching a kernel page → SIGSEGV.

**What happens when a process accesses an unmapped address?** The MMU finds no valid PTE and raises a
page fault. The kernel checks its record of the process's mappings (VMAs). Not found, or permission
denied → it delivers SIGSEGV; the default action kills the process with a core dump (exit 139).

## 5. Swapping and excessive paging

When RAM runs short, the kernel evicts pages: clean file pages are just dropped (re-read later);
dirty anonymous pages go to **swap**.

**Why can excessive paging degrade performance?** Every major fault turns a ~100 ns memory access
into a ~ms disk access, roughly 10⁴–10⁵× slower. When the working set doesn't fit, the system
**thrashes**: it spends its time paging instead of computing (OS 07 measures this).

Servers often run with little or no swap and rely on cgroup limits plus the **OOM killer** for
predictable latency (OS 12).

## 6. Copy-on-write

After `fork()`, parent and child **share every physical page**, marked read-only. The first
**write** to a page faults, and only then does the kernel copy **that one page**.

| 100 MB process (25,600 pages), child writes 1 MB | |
|---|---|
| pages copied at fork | **0** |
| pages copied on write | **256** |
| memory used | **101 MB** instead of 200 MB |

That's why `fork()` + `exec()` is cheap. **Redis `BGSAVE`** forks to snapshot; if the parent keeps
writing heavily during the save, many pages get copied and memory can nearly **double**, a classic
OOM cause.

## 7. Locality of reference (and caches)

Programs reuse recently accessed data (**temporal locality**) and nearby data (**spatial
locality**). Caches, TLBs, prefetchers and page replacement all depend on it.

**Real measurement:** summing a 4096 × 4096 `int` array (64 MB):

| Order | Time |
|---|---|
| row by row (consecutive addresses) | **4 ms** |
| column by column (each access jumps 16 KB: a new cache line, often a new page) | **26 ms**: **6.3× slower** |

Same work, same answer, just a different memory access pattern. This is why arrays beat linked
lists, columnar databases beat row stores for analytics, and data-oriented design matters.

## 8. Fragmentation

| Kind | Meaning | Example from the run |
|---|---|---|
| **external** | enough free memory **in total**, but not **contiguous** | holes of 100 + 30 + 50 = 180 free, yet a **120-byte request fails** |
| **internal** | wasted space **inside** an allocated block, from rounding | 65 bytes in 64-byte blocks → **63 bytes wasted** |

**Allocation strategies** (requests 25, 90, 45 into holes 100, 30, 50):

- **First fit** `[0, −1, 25]`: puts 25 into the big hole, so **90 fails**.
- **Best fit** `[200, 0, 300]`: puts 25 into the 30-hole, so everything fits.
- (Worst fit, next fit and buddy allocators trade speed against fragmentation differently.)

**Fixes:**

- **paging** (physical RAM never fragments externally: any free frame works);
- **compaction** (moving garbage collectors, OS 06);
- **size-class allocators** (jemalloc, tcmalloc and glibc's arenas keep per-size free lists).

Fragmented `malloc` heaps are a common reason a process's RSS stays high after freeing (OS 06).

## 9. Memory-mapped files and shared memory

`mmap` maps a file's pages into the address space: reads and writes become memory accesses, faults
load pages on demand, and the **page cache** is shared with other processes.

In the run, writing `"hello via memory"` into a `MappedByteBuffer`, then `force()` (≈ `msync`),
made the bytes appear in the file through the normal file API; the 4th page faulted in on first
touch.

It's used by databases (LMDB, older MongoDB, SQLite's mmap mode), Kafka index files, shared
libraries, and zero-copy I/O.

**Shared memory** (`shm_open` / `mmap(MAP_SHARED)`) maps the same physical pages into several
processes: the fastest IPC, but you must synchronise yourself (OS 10).

**Memory-mapped I/O** (device registers mapped into the address space) is how drivers talk to
hardware.

## 10. Stack vs heap

| | Stack | Heap |
|---|---|---|
| holds | call frames, locals, return addresses | objects with dynamic lifetime |
| per | **thread** | process (shared by its threads) |
| allocation | bump a pointer: very fast, freed automatically on return | `malloc`/`new`: slower; freed by `free` or GC |
| size | small, fixed (Java 512 KB–1 MB default, `-Xss`) | large (`-Xmx`, or system memory) |
| overflow | **StackOverflowError** / SIGSEGV on the guard page (deep recursion; OS 02 measured 1,475 frames at 256 KB) | **OutOfMemoryError** / `malloc` returns NULL / the OOM killer |

The run starts a **separate JVM with `-Xmx64m`** that keeps allocating. It dies with
**`OutOfMemoryError: Java heap space`**. That's the JVM's limit, enforced long before the OS's.

**Out-of-memory conditions at each level:**

- the language runtime limit (`-Xmx`, V8's `--max-old-space-size`);
- the process limit (`ulimit -v`);
- the **cgroup / container limit**, enforced by the **OOM killer: SIGKILL, exit 137**, with no
  chance to clean up;
- the whole machine out of RAM + swap, so the global OOM killer picks a victim by `oom_score`.

## 11. NUMA

On multi-socket servers each CPU socket has **local** RAM. Accessing another socket's RAM crosses an
interconnect and is slower. In the toy, half-remote memory costs **37.5 % more per access**.

The OS tries to allocate memory on the node where the thread runs ("first touch"). For big
databases or JVMs, pin processes to nodes (`numactl`), use NUMA-aware GC
(`-XX:+UseNUMA`), or interleave memory. Check with `numastat`.

---

## Senior-level questions

**1. Explain virtual memory.** Each process gets a private address space translated to physical
frames by the MMU using OS-managed page tables, cached in the TLB. It gives isolation, a simple
programming model, lazy allocation, sharing (libraries, COW), file mapping, and overcommit with
swap.

**2. What happens when a process accesses an unmapped memory address?** The MMU raises a page fault;
the kernel finds no valid mapping (or a permission violation) and sends SIGSEGV. By default the
process dies with a core dump (139); the JVM converts some cases into NPEs.

**3. What is a page fault?** A trap when a translation isn't currently valid. It's resolved by the
kernel (allocate, map, read from disk) or turned into SIGSEGV if the access is illegal.

**4. Minor vs major page faults on Linux?** Minor: resolved without I/O (fresh zero page, page
already in the page cache, COW copy), ~µs. Major: requires disk I/O (swap-in or uncached file page),
~ms. Major-fault spikes mean memory pressure.

**5. Why can excessive paging degrade performance?** Memory accesses turn into disk accesses
(10⁴–10⁵× slower), and with working sets larger than RAM the system thrashes. CPUs idle on I/O,
latency explodes.

**6. Stack vs heap memory?** §10.

**7. What is copy-on-write?** Sharing pages read-only after fork (or between snapshots) and copying a
page only on its first write: cheap forks, but memory can grow later (Redis BGSAVE).

**8. How would you investigate increasing memory usage in a production service?**

1. Which metric? RSS vs heap vs cgroup `memory.current` (which includes page cache) (OS 06, 12).
2. Is the runtime heap growing after full GCs? Then it's a leak or cache growth: take heap dumps
   and compare dominators.
3. If the heap is flat but RSS grows: native memory (direct buffers, thread stacks, JNI, glibc
   arena fragmentation; use NMT `-XX:NativeMemoryTracking`, `pmap`, jemalloc profiling).
4. Major faults and swap (`vmstat`).
5. Correlate with traffic and deploys.
6. Check the container limit vs the real footprint.

---

**Next:** [06 · Memory allocation and garbage collection](../06-Allocation-and-GC/README.md)
