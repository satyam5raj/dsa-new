# OS 02 · Processes and threads (study deeply)

> A **process** is a container of resources: an address space, open files, an identity. A
> **thread** is a flow of execution inside it. Choosing between processes, threads, pools and
> virtual threads decides how your server scales, fails and uses memory.

Run it: `java ProcessesThreads.java` → **18/18 checks**. **Real** fork/exec and wait, a real
**zombie** caught with `ps`, process isolation, thread failure, real creation-cost timings,
10,000 **virtual threads**, stack depth, and thread-locals.

---

## Part A · Processes

### 1. What is a process?

A process is a program **in execution**:

- a **virtual address space**: code (text), data, heap, stack(s), memory-mapped files and
  libraries;
- **one or more threads** (each with registers, a program counter and a stack);
- **kernel resources**: open file descriptors (files, sockets, pipes), signal handlers, current
  directory, environment;
- **identity**: PID, parent PID (PPID), user/group IDs, capabilities, cgroup and namespaces.

The kernel keeps all of this in a **Process Control Block (PCB)** (Linux: `task_struct`): the
state, saved registers when not running, scheduling info, the memory map, the fd table, accounting.
That's what's saved and restored on a context switch.

### 2. Process states

```
          admit            dispatch
  NEW ───────────▶ READY ◀───────────▶ RUNNING ──── exit ───▶ TERMINATED (zombie until reaped)
                     ▲     preempt (timer)  │
                     │                      │ wait for I/O / lock / sleep
                     └───── event done ── BLOCKED (WAITING)
```

In Linux `ps`/`top`:

| Code | State |
|---|---|
| `R` | running or runnable |
| `S` | interruptible sleep (waiting on I/O or events, the usual state) |
| `D` | **uninterruptible sleep**: usually disk or NFS I/O. It can't be killed, and it **counts in load average** |
| `Z` | zombie |
| `T` | stopped |

### 3. Creating processes: `fork()` + `exec()` + `wait()`

```c
pid_t pid = fork();          // duplicate the calling process (copy-on-write, OS 05)
if (pid == 0) {              // in the child
    execve("/bin/sh", ...);  // replace the program image; keeps PID and open fds
} else {                     // in the parent
    waitpid(pid, &status, 0);// block until the child exits; collect its status
}
```

From the run, a real child launched with `ProcessBuilder`:

- it printed **`child pid=5648 parent=5646`**: a new PID whose parent is our JVM;
- we read its output through a **pipe** (its stdout connected to our input stream);
- `waitFor()` (≈ `waitpid`) collected **exit status 7**;
- a running child shows up in `ProcessHandle.current().children()`.

Why split fork and exec? Between them the child can rearrange its environment: redirect fds
(pipes, files), change directory, drop privileges, enter namespaces. That's how shells implement
`cmd > file | other`. (`posix_spawn` / `vfork` / `clone` are faster variants that the JVM uses.)

### 4. Process termination, zombies and orphans

- A process terminates by calling `exit()`, returning from main, or being killed by a **signal**.
- **Zombie:** the child has exited, but the **parent hasn't called `wait()`**. The kernel keeps a
  tiny entry (PID + exit status) so the parent can collect it later. The run caught a real one:
  `ps -o stat -p 5651 → 'Z'`.
  - Zombies use no memory or CPU, but they **hold a PID**. Thousands of them (a buggy parent that
    never waits) can exhaust the PID table, so new processes fail to start.
  - You **can't kill a zombie** (it's already dead). Fix or kill the **parent**.
- **Orphan:** the **parent died first**. The child is re-parented to **PID 1** (or a "subreaper"),
  which waits for it. When our zombie's parent exited, the zombie **disappeared**, because PID 1
  reaped it.

> **Containers:** your app is often **PID 1**. PID 1 must reap orphaned children and forward
> signals, and many apps do neither, so zombies accumulate and `docker stop` hangs for the grace
> period. Use `docker run --init` / **tini** / dumb-init, or a proper process manager.

### 5. Signals

Asynchronous notifications delivered to a process:

| Signal | # | Default | Typical use |
|---|---|---|---|
| SIGINT | 2 | terminate | Ctrl-C |
| SIGKILL | 9 | terminate, **uncatchable** | force kill; the OOM killer → exit **137** |
| SIGSEGV | 11 | core dump | invalid memory access → exit **139** |
| SIGTERM | 15 | terminate (**catchable**) | polite shutdown (`kill`, `docker stop`, Kubernetes) → **143** |
| SIGHUP | 1 | terminate | terminal closed; often "reload config" |
| SIGCHLD | 17 | ignore | a child changed state (time to `wait()`) |
| SIGSTOP / SIGCONT | 19 / 18 | stop / continue | job control |
| SIGUSR1/2 | | terminate | app-defined (e.g. dump stats) |

**Graceful shutdown:**

1. On SIGTERM, stop accepting new work (fail readiness).
2. Finish in-flight requests.
3. Close resources and exit before the grace period ends (Kubernetes default: 30 s), or you'll get
   SIGKILL.

### 6. Process isolation

Each process has its **own virtual address space**. In the run, a child process with *the same
program text* set its `counter` to **999**, while ours stayed **100**. A crash in one process
(OS 01: SIGSEGV, exit 139) doesn't touch the others. Isolation comes from page tables (memory), fd
tables (files), and credentials and namespaces (OS 12).

**Inter-process communication** (pipes, sockets, shared memory, signals) is covered in OS 10.

## Part B · Threads

### 7. What is a thread?

A **thread** is an independent flow of execution **inside** a process.

- **Its own:** program counter, registers, **stack**, thread-local storage, scheduling state.
- **Shared with the other threads of its process:** heap, globals, code, open files, sockets, the
  address space.

The run shows another thread's write (`shared[0] = 42`) is **visible** after `join()`. Shared memory
makes communication cheap, and it's exactly what makes **race conditions** possible (OS 04).

### 8. Process vs thread

| Property | Process | Thread |
|---|---|---|
| address space | isolated | **shared** with its process |
| resources | its own fds, signal handlers… | shared |
| communication | IPC (pipes, sockets, shared memory) | plain shared memory (needs synchronisation) |
| failure isolation | **strong**: a crash affects only itself | **weak**: a native crash, OOM or `System.exit` kills the whole process |
| creation cost | higher (new address space, page tables) | lower |
| context switch | may switch address spaces (TLB flush) | within a process: no address-space change |

These are general distinctions; real costs depend on the OS and workload. On Linux both are created
with `clone()` and differ only in what they share.

In the run, an uncaught exception **ended only that thread** (the handler saw `boom`) while the
process kept running. A **native** fault, an `OutOfMemoryError`, or `System.exit()` would take down
every thread.

### 9. Thread lifecycle and user-level vs kernel-level threads

Java thread states: `NEW → RUNNABLE → (BLOCKED | WAITING | TIMED_WAITING) → TERMINATED`.
`jstack` / thread dumps show them, which is the first thing to grab when a JVM hangs (OS 04).

| | **Kernel (1:1) threads** | **User-level / green threads (M:N)** |
|---|---|---|
| scheduled by | the OS | a runtime in user space, onto a few OS threads |
| examples | Java platform threads, pthreads | **Java 21 virtual threads**, Go goroutines, Erlang processes, Kotlin coroutines |
| blocking call | blocks only that thread | the runtime **parks** the user thread and runs others on the carrier |
| cost | ~1 MB stack reserve, a kernel object, OS scheduling | a few KB, cheap to create and switch |
| scale | thousands | **millions** |

### 10. Thread creation overhead, pools, virtual threads

Real timings from the run:

| 2,000 tiny tasks | Time |
|---|---|
| a new OS thread per task | **60.8 ms** |
| a fixed pool of 4 threads | **3.6 ms** |

And **10,000 virtual threads**, each sleeping 100 ms, finished in **130 ms** total: they ran
concurrently on a handful of carrier OS threads. (10,000 platform threads would need ~10 GB of
reserved stack and heavy scheduling.)

**Why can creating too many threads reduce performance?**

- **Memory:** each platform thread reserves a stack (1 MB default). Thousands means GBs of virtual
  memory, plus kernel structures.
- **Context switching:** with more runnable threads than cores, the scheduler constantly switches.
  Each switch saves and restores registers and pollutes caches and TLBs (OS 03).
- **Contention:** more threads fighting over the same locks and cache lines.
- **Creation and teardown cost** per task.

**Thread pool vs process pool:**

| | **Thread pool** | **Process pool** |
|---|---|---|
| shares memory | yes: cheap data sharing | no: data is serialised between workers |
| isolation | a crash or leak can affect all | a crash affects one worker |
| use | I/O-bound servers, most JVM work | CPU-bound work in languages with a GIL (Python), untrusted or unstable native code, isolation (PHP-FPM, Gunicorn workers, Node `cluster`) |
| sizing | CPU-bound: ≈ number of cores; I/O-bound: cores × (1 + wait/compute) | ≈ number of cores |

### 11. Thread stacks

Each thread has its own stack for frames and locals. A **guard page** below it catches overflow.
From the run:

| Stack size | Max recursion depth before `StackOverflowError` |
|---|---|
| 256 KB | **1,475** frames |
| 8 MB | **206,702** frames |

Set the size with `-Xss` or the `Thread` constructor. Deep recursion (recursive parsers, naive DFS)
needs bigger stacks or iteration.

### 12. Thread-local storage

Per-thread variables. Two threads setting `REQUEST_ID` each saw only their own value
(`{A=req-A, B=req-B}`); `main` saw `none`. They're used for request context, transactions,
`SimpleDateFormat` instances, MDC logging.

> **Pitfall:** thread **pools reuse threads**. Forget `remove()` and the next request inherits stale
> context (or you leak memory). With virtual threads, prefer **scoped values**.

---

## Senior-level questions

**1. Explain the difference between a process and a thread.** A process owns resources (address
space, fds, identity) and is isolated from other processes. Threads are execution flows inside a
process that share its memory and resources. That makes communication cheap but needs
synchronisation, and gives weak failure isolation (§8).

**2. What happens during a context switch?** A timer interrupt or a blocking call enters the kernel.
The scheduler saves the current thread's registers, PC and stack pointer into its kernel structure,
picks the next runnable thread, restores that thread's registers, and — if it belongs to another
process — switches page tables (the TLB is flushed, or tagged by PCID/ASID). Then it returns to user
mode in the new thread. The cost is microseconds directly, plus cold caches and TLB afterwards
(OS 03).

**3. What is a zombie process?** A child that has exited but whose parent hasn't called `wait()`.
Only its PID and exit status remain. It can't be killed; reap it by fixing the parent (or the
parent dies and PID 1 reaps it). Many zombies exhaust PIDs. In containers, run a proper init as
PID 1.

**4. Why might creating too many threads reduce performance?** Stack memory, kernel overhead,
context-switch and cache thrash when runnable threads exceed cores, lock contention, and
creation/teardown cost. Use pools sized to the workload, or virtual threads / async I/O for massive
concurrency.

**5. What is the difference between a process pool and a thread pool?** Thread pools share memory
(cheap, but a crash or leak spreads). Process pools isolate workers (robust, but data must be
serialised, and they cost more memory). Use process pools for CPU-bound work under a GIL or for
isolation.

**6. Why can a process crash without crashing every other process?** Separate virtual address spaces
and kernel-managed resources: a fault is delivered as a signal to that process only, and the kernel
cleans up its resources. Shared state lives only in explicit IPC channels.

---

**Next:** [03 · CPU scheduling](../03-CPU-Scheduling/README.md)
