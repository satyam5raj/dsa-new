# OS 07 · Page replacement, working sets and thrashing

> When RAM is full and a new page is needed, which page should go? The answer decides whether your
> machine hums along or grinds to a halt. The same algorithms reappear in every cache you build:
> CPU caches, buffer pools, Redis, CDNs.

Run it: `java PageReplacement.java` → **13/13 checks**: FIFO, LRU, OPT and Clock on the classic
textbook reference string, **Belady's anomaly**, locality, working sets, a **thrashing** curve,
and an LRU cache in 5 lines of Java.

---

## 1. The algorithms

The reference string is `7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1` with **3 frames** (the classic
textbook example; the program reproduces the textbook answers):

| Algorithm | Evicts | Faults |
|---|---|---|
| **FIFO** | the page loaded **longest ago** | **15** |
| **LRU** (least recently used) | the page **not used for the longest time** | **12** |
| **OPT** / Belady's MIN | the page used **farthest in the future** | **9**: the minimum possible |
| **Clock** (second chance) | sweep a "hand"; skip pages with the **accessed bit** set (clearing it) | 14 |

- **OPT is a benchmark, not an algorithm:** it needs to know future references. It tells you how
  close a real policy gets.
- **LRU** is excellent but expensive to implement exactly in hardware (every access would have to
  update an ordering).
- **Clock** approximates LRU cheaply using the **accessed bit** the MMU sets on every touch. Linux
  uses a refinement: **active/inactive lists**, plus multi-generational LRU in newer kernels.

## 2. Belady's anomaly

The reference string is `1 2 3 4 1 2 5 1 2 3 4 5`:

| | 3 frames | 4 frames |
|---|---|---|
| **FIFO** | **9** faults | **10** faults: **more memory, more faults!** |
| LRU | 10 | 8 |

FIFO isn't a **stack algorithm**: the set of pages it holds with *n* frames isn't always a subset of
what it holds with *n + 1*. LRU and OPT are stack algorithms, so adding frames **never** increases
their faults.

## 3. Locality explains why caching works

Programs spend phases working on a small set of pages (a function's code, a hot loop's data). The
program's trace has 90 % of references inside an 8-page working set that shifts every 500
references, over 100 pages total, with **16 frames**:

| Trace | Faults |
|---|---|
| with locality, LRU | **1,905** of 20,000 (< 10 %) |
| with locality, FIFO | 2,958 |
| **uniform random** (no locality), LRU | **16,841** (~84 %: only 16 of 100 pages fit) |

With no locality, no replacement policy can help. That's the same reason caches fail on random
access patterns (and why OS 05's column-major loop was 6× slower).

## 4. Working sets

**Working set W(t, Δ)** = the distinct pages referenced in the last Δ references. At t = 10,000 with
Δ = 200 it was **18 pages**, far smaller than the 100-page address space.

**Working-set model:** give each process enough frames for its working set. If the sum of the
working sets exceeds RAM, **suspend** (swap out) some processes instead of letting everyone thrash.

**Page-fault frequency (PFF)** is the practical control loop: if a process faults too often, give it
more frames; if rarely, take some away.

**Local vs global replacement:**

| | Local | Global |
|---|---|---|
| evicts from | the faulting process's **own** frames | **any** process |
| effect | isolation: a hog only hurts itself | better overall utilisation, but one process can steal from others |
| Linux | global page reclaim, plus **cgroup memory limits** for per-container local pressure | |

## 5. Thrashing

LRU faults vs frames on the locality trace:

| Frames | 4 | 6 | 8 | 12 | 16 | 32 | 64 |
|---|---|---|---|---|---|---|---|
| Faults (of 20,000) | **11,978** | 8,342 | 5,154 | 2,348 | 2,019 | 1,614 | 900 |

- **Below the working-set size, faults explode.** That's thrashing: the system spends its time
  moving pages instead of computing.
- With a 5 ms major fault, the **effective access time** is 0.505 ms per reference with 16 frames vs
  **2.995 ms** with 4 frames.
- Once the working set fits, adding memory gives **diminishing returns**.

**What thrashing looks like on a server:**

- load average up, but **CPU mostly in iowait** (or system time);
- `vmstat` `si/so` (swap in/out) non-zero and rising;
- major faults spiking;
- latency through the roof;
- the box barely responds to SSH.

In containers, near the memory limit the kernel reclaims the page cache aggressively (re-reading
files) before the OOM killer finally fires. **Fixes:** reduce the memory footprint, add RAM or raise
limits, reduce concurrency (fewer simultaneous working sets), or fail fast (no swap, so an OOM
restart instead of a 30-minute brownout).

## 6. The same ideas in your code

An LRU cache in Java:

```java
Map<String, String> cache = new LinkedHashMap<>(16, 0.75f, true) {     // accessOrder = true
    protected boolean removeEldestEntry(Map.Entry<String, String> e) { return size() > 3; }
};
// put a, b, c; get a; put d  →  b (least recently used) is evicted  →  [c, a, d]
```

| Caching policy | Where |
|---|---|
| LRU / approximate LRU | Redis `allkeys-lru` (sampled), Memcached, database buffer pools (with midpoint insertion against scans) |
| LFU / TinyLFU | Caffeine (W-TinyLFU), Redis `allkeys-lfu`: better hit rates for skewed, frequency-heavy traffic |
| FIFO / random | simple, sometimes good enough; susceptible to Belady-style surprises |
| CLOCK variants | OS page caches, PostgreSQL's buffer manager |

---

## Interview questions

**1. Compare FIFO and LRU.** FIFO evicts the oldest-loaded page regardless of use: simple, but it can
evict hot pages and suffers Belady's anomaly (15 faults vs LRU's 12 on the textbook string). LRU
evicts the least recently used page: it exploits temporal locality, is a stack algorithm (more
memory never hurts), but is costly to track exactly, so Clock approximates it.

**2. What is Belady's anomaly?** For some algorithms (FIFO), increasing the number of frames can
**increase** page faults (9 → 10 above), because they aren't stack algorithms.

**3. What is thrashing, and how can it affect a server?** When the active working sets exceed RAM,
almost every access faults and goes to disk. CPU sits in iowait, throughput collapses and latency
explodes (2.995 vs 0.505 ms per access above). Fix by reducing the footprint or concurrency, adding
memory, or failing fast with OOM instead of swapping.

**4. How does locality of reference help explain caching and page replacement?** Programs reuse
recent and nearby data, so a small working set captures most accesses. LRU-like policies keep that
set resident (1,905 faults vs 16,841 for a random trace). Without locality, caches can't help.

---

**Next:** [08 · File systems and I/O](../08-File-Systems-and-IO/README.md)
