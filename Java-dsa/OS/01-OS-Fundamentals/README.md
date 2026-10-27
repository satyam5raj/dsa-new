# OS 01 · Operating-system fundamentals

> The operating system is the program that runs your programs. It shares the CPU, memory, disks
> and network among many processes, keeps them from trampling each other, and gives them a simple
> interface: **system calls**. Every `read()`, `new Thread()` or socket in your code ends up there.

Run it: `java OsFundamentals.java` → **17/17 checks**, including **real** child processes killed by
signals and real system-call cost measurements.

---

## 1. What is an operating system?

Two jobs:

1. **Abstraction:** turn messy hardware into clean concepts.
   - CPU cores → **processes and threads**
   - RAM → **virtual address spaces**
   - disks → **files and directories**
   - network cards → **sockets**
2. **Resource management and protection:**
   - decide who runs when (**scheduling**);
   - who gets how much memory;
   - enforce isolation and permissions.

```
 ┌──────── user space ─────────────────────────────────────────┐
 │  your app (JVM, Node, Python)   libraries (libc, OpenSSL)    │
 └───────────────────── system call interface ─────────────────┘
 ┌──────── kernel space ───────────────────────────────────────┐
 │  scheduler · memory manager · file systems · network stack   │
 │  device drivers · security (permissions, namespaces, cgroups)│
 └─────────────────────────────────────────────────────────────┘
                   hardware (CPU, RAM, disk, NIC)
```

## 2. Kernel space vs user space; privilege levels

The CPU has **privilege levels** (rings on x86, exception levels on ARM):

| Mode | Who | Can do |
|---|---|---|
| **kernel mode** (ring 0 / EL1) | the OS kernel | everything: any memory, I/O instructions, page tables, interrupts |
| **user mode** (ring 3 / EL0) | your applications | only their own memory; no direct hardware access |

**Why can't an ordinary application directly access arbitrary physical memory?** (a senior
question)

- Every memory access goes through the **MMU** using the process's **page tables** (OS 05). A
  process can only reach pages the kernel mapped for it.
- Touching anything else causes a **page fault**, which the kernel turns into **SIGSEGV**.
- Changing page tables or doing I/O are **privileged instructions** that trap in user mode.

That's how one process can't read another's passwords or corrupt the kernel.

## 3. System calls

A system call is the **controlled doorway** from user mode into the kernel: `open`, `read`,
`write`, `mmap`, `fork`, `clone`, `socket`, `connect`, `epoll_wait` (~350 on Linux).

**What happens when an application invokes a system call?** (a senior question)

1. The libc wrapper puts the syscall **number** and arguments in registers.
2. It executes a special instruction (`syscall` on x86-64, `svc` on ARM). The CPU **switches to
   kernel mode** and jumps to the kernel's entry point.
3. The kernel saves user registers, **validates the arguments** (never trusting user pointers), and
   does the work: maybe it **blocks** the thread (e.g. waiting for disk or network) and schedules
   another one.
4. It writes the result (or `-errno`) back and returns to user mode (`sysret`).

**It's expensive compared with a function call:** a mode switch, register save/restore, cache and
TLB disruption, and security mitigations (Spectre/Meltdown). The run writes 200,000 single bytes:

| | Time | Syscalls |
|---|---|---|
| unbuffered `FileOutputStream.write(byte)` | **269.9 ms** | ~200,000 |
| `BufferedOutputStream` (8 KB buffer) | **3.6 ms** | ~25 |

That's **74× faster**, about **1.35 µs per write() syscall**. The lesson behind buffered I/O,
batching, `writev`, `sendfile`, io_uring and vDSO tricks (e.g. `clock_gettime` without entering the
kernel): **cross into the kernel less often**.

### Reading a syscall trace (`strace`)

```
openat(AT_FDCWD, "/etc/app/config.yaml", O_RDONLY|O_CLOEXEC) = 3    ← got file descriptor 3
fstat(3, {... st_size=2048 ...}) = 0
read(3, "server:\n  port: 8080\n"..., 4096) = 2048
read(3, "", 4096)                       = 0                         ← 0 = end of file
close(3)                                = 0
openat(AT_FDCWD, "/etc/app/secrets.yaml", O_RDONLY) = -1 ENOENT    ← the real root cause
write(2, "config error: secrets missing\n", 30) = 30                ← fd 2 = stderr
exit_group(1)
```

When an app's error message is vague, `strace -f -e trace=file,network -p <pid>` shows exactly what
the kernel was asked and what it answered. On macOS use `dtruss`.

## 4. Interrupts vs exceptions

| | **Interrupt** | **Exception** (trap / fault) |
|---|---|---|
| source | **external** hardware: timer, NIC packet arrived, disk I/O done, keyboard | **the instruction being executed**: divide by zero, page fault, invalid opcode, a `syscall` instruction |
| timing | **asynchronous**: between any two instructions | **synchronous**: caused by a specific instruction |
| handled by | an interrupt handler (top half quick, deferred bottom half / softirq) | the kernel's exception handler: fix it (page fault → load the page) or deliver a signal |

The run shows CPU exceptions surfacing in Java:

- **Integer divide by zero:** the CPU raises a fault, the kernel sends SIGFPE, and the JVM's signal
  handler turns it into **`ArithmeticException: / by zero`**.
- **Null dereference:** an access near address 0 hits an **unmapped page**, giving a page fault and
  then SIGSEGV. The JVM converts it to **`NullPointerException`** (HotSpot uses this "implicit null
  check" trick to avoid explicit null tests).
- **Floating-point divide by zero does not trap:** IEEE 754 gives `Infinity`.

The **timer interrupt** is what lets the scheduler **preempt** a running thread (OS 03).

## 5. Processes, isolation, exit codes and signals

A **process** is a running program with its own virtual address space, open files, threads and
security identity (OS 02 goes deep). **Isolation** means one process's crash doesn't take others
down.

The run proves it with **real child processes**:

| Child does | Exit status the parent sees | Parent |
|---|---|---|
| `kill -SEGV $$` (crashes itself) | **139** = 128 + 11 (SIGSEGV) | still running ✔ |
| `exit 3` | 3 | ✔ |
| `kill -TERM $$` | **143** = 128 + 15 (SIGTERM) | ✔ |
| `kill -KILL $$` | **137** = 128 + 9 (SIGKILL; also what the **OOM killer** sends) | ✔ |
| `Process.destroy()` from Java | 143 (Java sends SIGTERM) | ✔ |

> **Memorise 137 and 143.** In Kubernetes, `exit code 137` / `OOMKilled` means the memory limit was
> hit (or a SIGKILL after the grace period). `143` means a normal SIGTERM shutdown.

## 6. Kernel architectures

| | **Monolithic** | **Microkernel** | Hybrid |
|---|---|---|---|
| design | file systems, drivers, network stack all **inside** the kernel | minimal kernel (IPC, scheduling, memory); services run as **user-space** processes | mostly monolithic with some microkernel ideas |
| examples | **Linux**, BSDs | seL4, QNX, MINIX 3 | Windows NT, macOS (XNU = Mach + BSD) |
| pro | fast: function calls inside the kernel | robust: a crashing driver restarts; small trusted base | |
| con | a driver bug can crash the whole kernel | slower: IPC between services | |

Linux mitigates the monolithic downsides with **loadable kernel modules** and, increasingly,
**eBPF** (safe, verified programs that run inside the kernel for observability and networking).

## 7. Hardware abstraction, devices, file systems

- **Device drivers** translate generic requests ("read block 42") into device-specific commands.
  The rest of the kernel stays hardware-agnostic.
- **"Everything is a file"** in Unix: regular files, devices (`/dev/sda`), pipes, sockets and
  `/proc` entries are all accessed through file descriptors and `read`/`write` (OS 08).
- **Resource management:** the CPU (scheduler, OS 03), memory (virtual memory, OS 05),
  I/O bandwidth (I/O schedulers), and limits per group of processes (**cgroups**, OS 12).

## 8. The boot process

1. **Firmware** (UEFI/BIOS): power-on self-test, finds a boot device.
2. **Bootloader** (GRUB, systemd-boot): loads the kernel image and the initial RAM disk
   (**initramfs**).
3. **Kernel** initialises the CPU, memory management and drivers; mounts the root file system
   (with the initramfs's help).
4. **PID 1 (init)** starts: **systemd** on most distros. It starts services, mounts and the network.
5. Login and your services. In a container, **your entrypoint is PID 1** (OS 02 and 12 explain why
   that matters).

---

## Senior-level questions

**1. What happens when an application invokes a system call?** The libc wrapper sets the syscall
number and arguments in registers and runs `syscall`/`svc`. The CPU switches to kernel mode at a
fixed entry point; the kernel saves state, validates the arguments, performs the operation (maybe
blocking and rescheduling), and returns the result or `-errno` to user mode. It costs about 1 µs
plus side effects (1.35 µs measured here), so batch and buffer.

**2. What is the difference between user mode and kernel mode?** A CPU privilege level. Kernel mode
can execute privileged instructions and access all memory and devices. User mode is restricted to
its own mapped pages, and privileged operations trap. Transitions happen only through syscalls,
exceptions and interrupts.

**3. Why can't an ordinary application directly access arbitrary physical memory?** All accesses go
through the MMU with per-process page tables controlled by the kernel. Unmapped or forbidden
addresses fault (SIGSEGV), and changing mappings is privileged. That gives isolation and security.

**4. What is the difference between an interrupt and an exception?** Interrupts are asynchronous
hardware signals (timer, NIC, disk). Exceptions are synchronous, caused by the current instruction
(divide by zero, page fault, syscall). Both enter the kernel through handlers; exceptions are often
turned into signals for the process.

---

**Next:** [02 · Processes and threads](../02-Processes-and-Threads/README.md)
