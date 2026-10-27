# OS 12 · Linux internals, troubleshooting and containers

> You don't need to be a kernel developer. You do need to walk up to a sick Linux box or container
> and read the evidence: load, memory, CPU throttling, OOM kills, signals, descriptors. This chapter
> is that field guide, with real-format samples to practise reading.

Run it: `java LinuxContainers.java` → **24/24 checks**:

- interpreting load average, `/proc/meminfo`, `/proc/<pid>/status` and `vmstat`;
- a **cgroup CPU-throttling** model;
- the memory cgroup and OOM-killer scoring;
- namespaces;
- **real** exit codes and SIGTERM handling;
- reading OOM lines in `dmesg`;
- real `ulimit`s.

---

## Part A · Processes and CPU

| Tool | What it shows | Look for |
|---|---|---|
| `ps -eo pid,ppid,stat,%cpu,%mem,rss,etime,cmd --sort=-%cpu` | snapshot of processes | states `R`, `D` (uninterruptible), `Z` (zombie, OS 02) |
| `top` / `htop` | live per-process CPU and memory; press `H` for threads | which **thread** burns CPU; load average |
| `pidstat 1` / `pidstat -w 1` / `pidstat -t -p PID 1` | per-process CPU, **context switches**, per-thread | `nvcswch/s` high = preempted (contention or throttling) |
| `vmstat 1` | system-wide run queue, memory, swap, I/O, CPU split | below |
| `mpstat -P ALL 1` | per-core utilisation | one core at 100 % = a single-threaded bottleneck (an event loop, OS 09) |
| `strace -f -p PID` | system calls (OS 01) | what it's stuck on: `futex` (lock), `epoll_wait` (idle), `read` (I/O) |
| `/proc/PID/*` | everything: `status`, `limits`, `fd/`, `maps`, `stack`, `sched` | |

### Load average

The exponentially damped 1/5/15-minute average of tasks **running + runnable + uninterruptible
(D)**. Compare it with the **CPU count**:

| Situation | Interpretation (from the run) |
|---|---|
| load 3.5 on 8 CPUs | **ok** (steady) |
| load 24 on 8 CPUs, low iowait | **overloaded, rising**: more runnable tasks than CPUs (CPU saturation) |
| load 24 on 8 CPUs, **60 % iowait** | **overloaded, rising: many tasks in D state**: I/O-bound (disk/NFS), not CPU |

On Linux, load ≠ CPU utilisation, because D-state tasks count. Compare 1-min vs 15-min to see the
trend.

### `/proc/<pid>/status`

```
Threads:        412
VmSize: 12481880 kB      ← virtual address space (reservations: max heap, stacks, mappings)
VmRSS:   2385344 kB      ← actually resident in RAM
voluntary_ctxt_switches:     1830233   ← blocked/slept (I/O, locks): normal
nonvoluntary_ctxt_switches:   928411   ← PREEMPTED: CPU contention or cgroup throttling
```

- VmSize is ~5× RSS. **Virtual size isn't memory use.**
- An involuntary/voluntary ratio of **0.51** = heavy preemption, a hint to check CPU limits.
- `PPid: 1` means it's the main process of its container or unit.

### `vmstat 1`

```
 r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
18  0      0 412716 201332 9120544  0    0     4    61 9012 48211 88 10  2  0  0   ← CPU saturated: run queue 18 on 8 CPUs
 2 14  81920  35120  10244  410512 812  930  9120  4410 2210  9811  4  6  5 85  0   ← swapping + 85% iowait: memory pressure
```

| Column | Meaning |
|---|---|
| `r` | runnable tasks |
| `b` | tasks blocked in D state |
| `si`/`so` | swap in/out (KB/s); **any sustained value = memory pressure** |
| `cs` | context switches/s |
| `us`/`sy` | user/kernel CPU |
| `wa` | iowait |
| **`st`** | **steal**: on a VM, the hypervisor gave your vCPU to someone else (a noisy neighbour; move or resize the instance) |

## Part B · Memory

### `free` and `/proc/meminfo`

```
MemTotal:  16303340 kB   MemFree: 412716 kB   MemAvailable: 9873120 kB   Cached: 9120544 kB
```

Only **2.5 % "free"**, but **60.6 % "available"**. Linux deliberately fills idle RAM with **page cache**
(OS 08), which is reclaimable on demand. **Alert on `MemAvailable` or PSI** (`/proc/pressure/memory`),
never on `MemFree`.

| Concept | Meaning |
|---|---|
| **RSS vs virtual** | RSS = resident pages; VSZ = mapped address space (OS 05, 06) |
| **swap usage** | non-zero `si/so` over time = the working set doesn't fit (thrashing, OS 07) |
| **page faults** | `ps -o min_flt,maj_flt`; major faults = disk reads (OS 05) |
| **memory pressure** | PSI `some`/`full` percentages: time stalled waiting for memory |
| **OOM killer** | picks a victim when memory can't be reclaimed (below) |
| **cgroup limits** | per-container `memory.max`; `memory.current` **includes page cache** |

## Part C · Files and networking

| Tool | Use |
|---|---|
| `lsof -p PID`, `lsof -i :8080`, `lsof +L1` | open files and sockets; deleted-but-open files eating disk (OS 08) |
| `df -h`, `df -i` | free space; **free inodes** |
| `du -sh * \| sort -h` | what uses the space |
| `iostat -x 1` | disk latency (`await`) and saturation (OS 08 §11) |
| `ss -tanp`, `ip addr`, `ip route` | sockets and network config (Networking 10) |
| `journalctl -u svc --since -1h`, `journalctl -k` | service and kernel logs |
| `dmesg -T` | kernel ring buffer: **OOM kills**, disk errors, NIC resets, segfaults |

## Part D · Containers = namespaces + cgroups

A container is **an ordinary Linux process** with:

**Namespaces: what it can *see*.**

| Namespace | Isolates |
|---|---|
| `pid` | its own PID numbering: the app sees itself as **PID 1** |
| `net` | its own interfaces, IPs, routes, ports, iptables |
| `mnt` | its own mount table / root filesystem (the image layers) |
| `uts` | its own hostname |
| `ipc` | its own System V IPC / POSIX message queues |
| `user` | UID 0 inside can map to an unprivileged UID outside |
| `cgroup` | its own view of the cgroup tree |
| (`time`) | its own clocks, since Linux 5.6 |

**cgroups: how much it can *use*.** CPU (`cpu.max`, `cpu.weight`), memory (`memory.max`), I/O
(`io.max`), PIDs (`pids.max`).

**Capabilities, seccomp and LSMs** (AppArmor/SELinux): what it may *do*.

All containers on a host share **one kernel**: a kernel exploit escapes. For stronger isolation use
VMs, gVisor or Firecracker microVMs.

### CPU limits and throttling (the hidden P99 killer)

`cpu.max = "200000 100000"` means **200 ms of CPU per 100 ms period**, i.e. 2 CPUs. Suppose a burst of
**8 busy threads** (a request spike plus GC threads on an 8-core node):

- the quota is used up after **25 ms**;
- then **every thread in the container is frozen for 75 ms** of each 100 ms period;
- a 30 ms request that starts the burst takes **> 100 ms**;
- yet the dashboard shows **40 % average CPU**, because averages over a minute hide 100 ms freezes.

**Detect:** `/sys/fs/cgroup/cpu.stat` → `nr_throttled`, `throttled_usec`; `container_cpu_cfs_throttled_*`
metrics; a high involuntary context-switch ratio.

**Fix:**

- raise or remove CPU **limits** (keep **requests** for scheduling and fairness);
- size thread pools and GC threads to the **limit**, not the node (`-XX:ActiveProcessorCount`,
  `GOMAXPROCS`; modern JVMs read cgroup limits);
- avoid bursty parallelism.

### Memory limits and OOM kills

The memory cgroup enforces `memory.max`. When it can't reclaim enough (page cache first), the kernel
**OOM-kills** a process in that cgroup: **SIGKILL, exit 137**, no cleanup, no stack trace in your
logs.

**The JVM math that bites everyone:**

```
container limit 2048 MB
-Xmx1536m heap + 180 metaspace + 250 thread stacks + 200 direct buffers + 60 code cache = 2226 MB  > limit → OOMKilled
```

Rule: the heap is only part of RSS (OS 06). Use `-XX:MaxRAMPercentage=60–75` and Native Memory
Tracking to measure the rest.

**Who gets killed?** `oom_score` ≈ (memory share × 1000) + `oom_score_adj`:

| Processes | Victim |
|---|---|
| app-java 1900 MB, log-agent 300 MB, sidecar 120 MB, all with adj 0 | **app-java** (score 463) |
| same, but the sidecar has `oom_score_adj = +500` | **sidecar** (29 + 500 = **529** > 463): the adjustment outweighs size |
| sshd with adj **−1000** (even at 3000 MB) | **never** chosen: immune |

Kubernetes sets `oom_score_adj` by QoS class: BestEffort pods die first, Guaranteed last.

### Signals, PID 1 and graceful shutdown

Real child processes from the run: `kill -KILL` → **137**; a handled SIGTERM → **exit 0**.

| Exit code | Meaning |
|---|---|
| **0** | success |
| **1–125** | application error |
| **137** = 128 + 9 | **SIGKILL**: OOM-killed, or killed after the termination grace period |
| **139** = 128 + 11 | **SIGSEGV**: native crash |
| **143** = 128 + 15 | **SIGTERM**: asked to stop (deploy, scale-down, `docker stop`) and didn't handle it |

**PID 1 is special.** The kernel gives it **no default signal handlers**, so SIGTERM is ignored
unless the app installs one. `docker stop` then waits 10 s (Kubernetes: 30 s) and sends SIGKILL.
That's slow deploys and dropped requests.

- `ENTRYPOINT sh -c "java -jar app.jar"`: **the shell** is PID 1 and may not forward signals to
  Java. Use `exec java …`, the exec-form `ENTRYPOINT ["java", …]`, or `--init` / **tini**.
- PID 1 must also **reap zombies** (OS 02).

**Graceful shutdown:** on SIGTERM → fail readiness → stop accepting → drain in-flight requests →
close pools → exit before the grace period (and add a `preStop` sleep so load balancers notice).

### Per-process limits (`ulimit`)

This machine reports `ulimit -n` = **1048576** open files and `ulimit -u` = **4000** processes.
Servers need high `nofile` (every connection is an fd, OS 08); set it via systemd `LimitNOFILE=`,
`docker run --ulimit`, or Kubernetes node config.

---

## Senior-level scenarios

### Scenario 1: CPU is at 100 %

1. **Which process and thread?** `top` → `H` for threads, or `pidstat -t`. Map the native thread id
   (hex) to a Java thread in `jstack` (`nid=0x…`).
2. **User or system time?** High `sy` → syscall storms, lock contention in the kernel, interrupts. High
   `us` → application code.
3. **Is it doing useful work?** Profile with async-profiler, `perf top`, `py-spy`, `node --cpu-prof`:
   a tight loop, regex backtracking, JSON (de)serialisation, **GC** (check GC logs: a nearly full
   heap means back-to-back GCs burn CPU), or a hot lock (spinning).
4. **Context-switch rate** (`vmstat cs`, `pidstat -w`): thousands of threads thrashing (OS 03)?
5. In containers: **throttling** (§D) can look like slowness without 100 % CPU.
6. Compare with the traffic graph: legitimate load (scale out) vs regression (roll back).

### Scenario 2: memory usage keeps increasing

1. **Which number?** RSS vs heap used vs cgroup `memory.current` (which includes page cache).
2. **Heap after full GC rising** → a leak or unbounded cache: compare heap dumps (dominator tree), or
   Node heap-snapshot diffs (OS 06).
3. **Heap flat, RSS rising** → native: direct/NIO buffers, thread count growth (`Threads:`), JNI or
   native libraries, malloc fragmentation (glibc arenas; try `MALLOC_ARENA_MAX` or jemalloc), mmap'd
   files.
4. **Cache growth** vs an expected **workload increase** (more connections, bigger payloads):
   correlate with traffic.
5. **Fragmentation**: allocator stats.
6. Act before the container limit: OOM kills are abrupt.

### Scenario 3: requests become slow under load

Work through the resources with the **USE method** (Utilisation, Saturation, Errors):

1. **CPU saturation:** run queue > cores, throttling.
2. **Event-loop delay** (Node) or **thread-pool saturation** (queue length, active = max).
3. **Disk I/O:** `iostat await`, fsync-heavy logging.
4. **Network latency and retransmits** (Networking 04, 10).
5. **Connection pools** (Little's law, Networking 09).
6. **Downstream dependencies** (traces).
7. **GC pauses** (OS 06).

### Scenario 4: a service suddenly exits

1. **Exit code / signal:** `kubectl describe pod` (`Last State: Terminated, Reason: OOMKilled, Exit
   Code: 137`), `docker inspect --format '{{.State.ExitCode}} {{.State.OOMKilled}}'`, `systemctl status`.
2. **137** → check `dmesg -T | grep -i "killed process"` / `journalctl -k`. The run parses
   `Memory cgroup out of memory: Killed process 4123 (java) … anon-rss:2091520kB`: it was the
   **container** limit, and the process held **2042 MB**.
3. **143** → who sent SIGTERM? A deploy, the autoscaler, a failed liveness probe, node drain.
4. **139 / core dumps** → a native crash: `hs_err_pid*.log` for the JVM, core files.
5. **0 or 1 with no signal** → the application decided to exit: read its last logs (config errors,
   unhandled exceptions, `process.exit`).
6. **Container events** (`kubectl get events`): evictions (node memory or disk pressure), preemption,
   probe failures.

---

**Next:** [13 · Interview questions and study plan](../13-Interview-Questions/README.md)
