# OS 03 · CPU scheduling

> There are usually more runnable threads than CPU cores. The **scheduler** decides who runs next
> and for how long. Its choices show up directly in your service's latency, especially when CPUs
> are busy.

Run it: `java Scheduling.java` → **14/14 checks**:

- a simulator for FCFS, SJF, SRTF, round robin, priority + aging, MLFQ and priority inversion;
- a **real** measurement of oversubscribed CPUs.

---

## 1. Vocabulary

| Term | Meaning |
|---|---|
| **CPU burst / I/O burst** | programs alternate computing and waiting |
| **CPU-bound** | long bursts (encoding video, compressing, ML) |
| **I/O-bound** | short bursts, then waiting (web servers, DB clients) |
| **arrival / burst** | when a job becomes ready, and how much CPU it needs |
| **waiting time** | time spent ready but not running |
| **turnaround time** | completion − arrival |
| **response time** | first run − arrival (what interactive users feel) |
| **throughput** | jobs completed per unit time |
| **fairness** | does everyone get a reasonable share? |
| **starvation** | a ready job that never gets to run |
| **scheduling latency** | the delay between becoming runnable and actually running |

### Preemptive vs non-preemptive

- **Non-preemptive:** a job runs until it finishes or blocks.
- **Preemptive:** the scheduler can **take the CPU away**, using the **timer interrupt** (OS 01) or
  when a higher-priority job wakes.

All modern general-purpose OSes are preemptive; that's what keeps a busy loop from freezing the
machine.

## 2. FCFS, SJF, SRTF

The textbook workload (arrival/burst): **P1 0/8, P2 1/4, P3 2/9, P4 3/5**.

| Algorithm | Gantt (1 char = 1 unit) | Avg waiting |
|---|---|---|
| **FCFS** (first come, first served) | `11111111222233333333344444` | **8.75** |
| **SJF** (shortest job first, non-preemptive) | `11111111222244444333333333` | **7.75** |
| **SRTF** (shortest remaining time first, preemptive) | `12222444441111111333333333` | **6.50** |

- **SJF/SRTF are optimal** for average waiting time, but they need to **know burst lengths**. OSes
  estimate them with an exponential average of past bursts.
- They can **starve** long jobs.
- **The convoy effect (FCFS):** one 100-unit job in front of three 1-unit jobs gives an average
  wait of **75.75**, vs **1.50** with SJF. It's like one shopper with a full cart at the only
  checkout.

## 3. Round robin and the time quantum

Each job gets a **quantum**, then goes to the back of the queue.

| Quantum | Avg response | Avg waiting | Context switches |
|---|---|---|---|
| 1 | **0.75** | 12.50 | **23** |
| 4 | 4.50 | 11.75 | 7 |
| 100 (≫ any burst) | 8.75 | 8.75 | 3 |

- A **small quantum** gives great responsiveness but many context switches (overhead).
- A **huge quantum** is just FCFS.
- Real systems use ~1–100 ms slices. Linux's **CFS** doesn't use fixed quanta: it gives each task
  CPU time in proportion to its weight ("virtual runtime"), and **EEVDF** replaced CFS in Linux 6.6.

## 4. Priority scheduling, starvation, aging

Run the highest priority first. A low-priority batch job facing a **steady stream** of
high-priority work **never ran in 200 ticks: starvation**.

**Aging** raises a job's priority the longer it waits; with it, the batch job ran after **181
ticks**. Linux's `nice` values adjust weights (−20 to +19); real-time classes (`SCHED_FIFO`,
`SCHED_RR`) always beat normal tasks, which is dangerous if misused.

## 5. Multilevel queue and multilevel feedback queue (MLFQ)

**Multilevel queue:** separate queues (system, interactive, batch), each with its own algorithm,
with priority between queues.

**MLFQ** **learns** each job's behaviour:

- Every job starts in the top queue (short quantum).
- **Uses its whole quantum** → probably CPU-bound → **demoted**.
- **Blocks for I/O early** → interactive → **stays high**.

After 5 rounds: the **cpu-hog sank to queue 2**, while the **editor stayed in queue 0**, so it stays
snappy. A periodic **priority boost** lifts everyone back to the top so sunk jobs can't starve.
Windows and macOS schedulers use MLFQ ideas.

## 6. Priority inversion

**The setup:**

- a **low** task L holds a lock;
- a **high** task H needs that lock, so it blocks;
- a **medium**, CPU-bound task M preempts L (M > L).

Now **H waits for M**, a lower-priority task. This happened on the **Mars Pathfinder** (1997): the
high-priority task kept missing deadlines, the watchdog kept resetting the system.

| | H finishes at |
|---|---|
| no fix | **t = 16** |
| **priority inheritance** (L temporarily runs at H's priority while holding the lock) | **t = 6** |

Other fixes: priority ceiling protocols, avoiding locks shared across priority levels, lock-free
designs.

## 7. Context switches, affinity, multi-core

**A context switch** (OS 02 §Q2) saves and restores registers and switches the stack and maybe the
address space. That's a few µs directly, plus **cold caches and TLB** afterwards, which often
costs more.

**Why can too many context switches hurt performance?** CPU time goes to switching instead of work,
caches keep being refilled, and every request's latency includes queueing for a core. Watch
`vmstat` → `cs` (context switches/s) and `pidstat -w` (voluntary = blocking; involuntary =
preempted, i.e. too many runnable threads).

**Real measurement**, with 10 cores:

| Busy threads | Time per thread |
|---|---|
| 10 | **63 ms** |
| 40 (4× oversubscribed) | **143 ms** |

The total CPU didn't grow; each thread just got time-sliced. (The exact ratio varies run to run,
2.3–2.8× here.) For a latency-sensitive service, **more threads than cores for CPU-bound work = more
latency, not more throughput**.

- **CPU affinity:** pin threads to cores (`taskset`, `sched_setaffinity`) to keep caches warm,
  isolate noisy work, or match NUMA nodes (OS 05).
- **Load balancing across CPUs:** per-core run queues with periodic balancing and work stealing.
  Busy cores push or pull tasks to idle ones.

## 8. CPU-bound vs I/O-bound and your service

| | CPU-bound | I/O-bound |
|---|---|---|
| spends time | computing | waiting on network or disk |
| pool size | ≈ number of cores | cores × (1 + wait/compute), or async I/O / virtual threads |
| symptom under load | 100 % CPU, run-queue length > cores, rising latency | low CPU, many blocked threads, pool exhaustion |
| scheduler cares | fair share, cache affinity | **fast wake-up** when I/O completes |

**Containers:** a CPU **limit** (CFS quota) can **throttle** your process even when the host is idle.
For example, 200 ms of CPU allowed per 100 ms period with 8 busy threads means they burn the quota
in 25 ms and then **all wait 75 ms**. That gives mysterious P99 spikes (OS 12).

---

## Senior-level questions

**1. What is the difference between preemptive and non-preemptive scheduling?** Preemptive
scheduling can interrupt a running task (timer interrupt, higher-priority wake-up) and give the CPU
to another. Non-preemptive scheduling waits until the task blocks or finishes. Preemption gives
responsiveness and fairness at the cost of context switches and the need for synchronisation.

**2. How can priority scheduling cause starvation?** If higher-priority work keeps arriving,
low-priority tasks never run (shown above). Fix with aging, fair-share scheduling, or minimum
guarantees.

**3. What is priority inversion?** A high-priority task waits for a lock held by a low-priority
task, which is preempted by medium-priority tasks, so the high task effectively runs at the lowest
priority. Fix with priority inheritance or ceilings (t = 16 → t = 6 above).

**4. Why can too many context switches hurt performance?** Direct switching cost plus cache and TLB
pollution, and runnable threads queueing for cores. Throughput drops and tail latency grows. Size
pools to the cores for CPU work; use async or virtual threads for I/O.

**5. CPU-bound vs I/O-bound workloads?** CPU-bound work is limited by compute (scale with cores,
avoid oversubscription). I/O-bound work is limited by waiting (scale concurrency cheaply with async
I/O or virtual threads; the bottleneck is usually downstream pools or latency).

---

**Next:** [04 · Concurrency and synchronization](../04-Concurrency-and-Synchronization/README.md)
