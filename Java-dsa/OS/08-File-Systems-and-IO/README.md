# OS 08 · File systems and I/O

> Files look simple: open, read, write, close. Underneath sit inodes, descriptors, a page cache that
> lies to you about durability, and rename tricks every database relies on. This chapter does it all
> **for real** on your disk.

Run it: `java FileSystems.java` → **24/24 checks**:

- real hard links and symlinks;
- a child JVM hitting **"Too many open files"**;
- POSIX permissions;
- real `fsync` timings;
- atomic rename vs torn in-place writes, under a concurrent reader;
- file locks;
- journaling;
- the textbook disk-scheduling numbers.

---

## 1. Files, directories and inodes

A **file** is an **inode** (metadata + pointers to data blocks) plus one or more **names**
(directory entries) that point at it.

| Inode holds | Directory entry holds |
|---|---|
| type, size, owner, permissions, timestamps, **link count**, block pointers | **name → inode number** |
| ❌ not the name | |

A **directory** is just a file whose content is a list of (name, inode) pairs.

## 2. Hard links vs symbolic links

The run creates `report.txt`, a hard link and a symlink to it:

| | Hard link | Symbolic link |
|---|---|---|
| what it is | **another name for the same inode** | a small file containing a **path** |
| same inode as the original? | ✅ (the link count became **2**) | ❌ its own inode |
| delete the original name | data **still there** ("quarterly numbers") | **dangles**: the path is gone |
| across file systems / to directories | ❌ / ❌ | ✅ / ✅ |

Data blocks are freed only when the **link count reaches 0 and no process has the file open**. That's
why deleting a huge log file that a process still has open **doesn't free the disk space**: `df`
says the disk is full while `du` can't find the space. `lsof +L1` finds such files; restart the
process, or truncate via `/proc/<pid>/fd/N`.

## 3. File descriptors

When a process opens a file (or a socket, pipe or epoll instance), the kernel returns a small integer,
a **file descriptor**: an index into the process's **fd table**.

```
fd table (per process)       open-file table (kernel)          inode table
0 stdin  ─┐                  ┌ offset, flags (O_APPEND...) ┐   ┌ metadata, blocks ┐
1 stdout ─┼────────────────▶ │  shared by fork / dup       │──▶│                  │
2 stderr ─┘                  └─────────────────────────────┘   └──────────────────┘
3 config.yaml, 4 socket to DB, 5 epoll ...
```

**What happens when an application opens a file?** `open()` → the kernel walks the path (each
directory lookup, using the dentry cache) → checks **permissions** → finds or loads the inode →
creates an open-file entry (offset 0, flags) → returns the **lowest free fd** (3 in the OS 01 trace).

**fd vs inode:** the inode is the file itself (one per file). An fd is a per-process **handle** to an
open instance, with its own offset; many fds (and processes) can refer to one inode.

### "Too many open files" (EMFILE)

Each process has a limit (`ulimit -n`, `LimitNOFILE`). The run starts a child JVM with `ulimit -n 256`
that keeps opening `/dev/null` without closing. It died after **252** opens with
`/dev/null (Too many open files)`; the JVM itself already held a few descriptors.

**Why might a service fail with `Too many open files`?** (a senior question)

- **Leaks:** connections or files not closed (missing try-with-resources / `finally`; pooled
  connections never returned; CLOSE_WAIT sockets, Networking 04).
- **Legitimately many connections:** a busy server or proxy where every socket is a descriptor.
- **Limit too low:** the default 1024 is common in containers and systemd units.

Check `ls /proc/<pid>/fd | wc -l` and `lsof -p <pid>` (what are they?), compare with
`/proc/<pid>/limits`. Fix leaks first, then raise the limit.

## 4. Permissions

`rwx` for **owner / group / others**, also written in octal:

| Mode | Meaning | Use |
|---|---|---|
| **600** `rw-------` | only the owner reads and writes | secrets, private keys (SSH refuses keys that are more open) |
| **644** `rw-r--r--` | everyone reads | configs, static files |
| **755** `rwxr-xr-x` | everyone reads and executes | scripts, binaries, directories |

On **directories**, `r` = list names, `w` = create or delete entries, `x` = traverse. Also: setuid,
setgid, the sticky bit (`/tmp`), ACLs, and `umask` (the default bits removed from new files).

## 5. Buffered vs unbuffered, page cache, and `fsync`

**Buffered I/O** collects small writes in user space (OS 01: 74× fewer syscalls). Below that, the
kernel's **page cache** holds file data in RAM:

- **reads** hit RAM if cached (repeat reads are fast; "free" memory is mostly cache, OS 12);
- **writes** just dirty pages in RAM and return immediately. The kernel flushes them **later**
  (seconds).

So **`write()` returning ≠ data on disk**. If the machine loses power, recent writes vanish.

**`fsync(fd)`** blocks until the file's data and metadata reach **stable storage**; `fdatasync`
skips non-essential metadata. Real timings, 200 log records:

| Strategy | Time |
|---|---|
| no fsync (page cache only) | **1.01 ms**: fast and **not durable** |
| fsync after **every** record | **798 ms** |
| one fsync per **batch** (group commit) | **5.05 ms** |

**Group commit** is how databases get both throughput and durability: many transactions share one
WAL fsync.

**What `fsync` does not guarantee by itself:**

- **A new file's existence:** fsync the file **and its parent directory**, or the directory entry may
  be lost.
- **Disks that lie:** volatile write caches without power-loss protection (enterprise SSDs and
  battery-backed controllers are fine).
- **Errors are sticky and confusing:** a failed `fsync` may have dropped the dirty pages. Calling it
  again can report success with the data gone (the "fsyncgate" bug that hit PostgreSQL). Treat fsync
  errors as fatal.
- **Ordering across files.**
- **Replication:** it protects one machine only.

## 6. Atomic file replacement

**The problem:** overwriting a config file in place (`truncate` + `write`) lets a concurrent reader see
an **empty or half-written** file, and a crash mid-write leaves it corrupt.

**The pattern:** write a temp file **in the same directory** → `fsync` it → **`rename()`** over the
target (atomic on POSIX) → `fsync` the directory.

The run replaces a 200 KB file repeatedly while another thread keeps reading it:

| Method | Torn or partial reads seen |
|---|---|
| **temp + fsync + atomic rename**, 40 replacements, 2,881 concurrent reads | **0** |
| **in-place overwrite**, 400 replacements | **525** |

Used by editors, package managers, Kubernetes ConfigMap updates (symlink swap), databases' manifest
files, and every "safe save".

## 7. File locking

`FileChannel.lock()` → `fcntl`/`flock`. In the run, a second exclusive lock was **refused** while the
first was held.

- **Shared** (read) locks can coexist; **exclusive** (write) locks can't.
- POSIX locks are **advisory**: they only bind processes that also ask for them. They're released
  automatically when the process dies (good for "only one cron job runs").
- Mandatory locking exists on Windows and is deprecated on Linux.
- Over NFS, locking is fragile. For distributed coordination use etcd/ZooKeeper leases or database
  row locks.

## 8. Journaling

Creating a file updates several structures (inode bitmap, inode table, directory entry). A crash
between them leaves the file system **inconsistent**: in the run, an inode is allocated with **no
directory entry**, a leaked inode that needs a full `fsck` scan.

**Journaling:**

1. write the intended changes to a **journal** plus a **COMMIT** record;
2. then apply them to their real locations (checkpoint).

After a crash, **replay committed** transactions (the run applied all 3 updates) and **discard
uncommitted** ones: all-or-nothing. That's a write-ahead log, the same idea as database WALs.

ext4's default is **ordered** mode: it journals metadata and writes data blocks before committing the
metadata. Copy-on-write file systems (ZFS, btrfs) never overwrite in place.

## 9. Synchronous vs asynchronous; blocking vs non-blocking

These are **two different axes**:

| | Meaning |
|---|---|
| **blocking** | the call **waits** until it can complete (data available, space in the buffer) |
| **non-blocking** | the call **returns immediately**, maybe with `EAGAIN` ("not ready, try later") |
| **synchronous** | you get the result **as the call's return value**; your code drives the operation |
| **asynchronous** | you start the operation and are **notified when it completes** (callback, future, completion event) |

Examples:

- `read()` on a blocking socket: synchronous **and** blocking.
- `read()` on a non-blocking socket after epoll says it's readable: synchronous, non-blocking.
- `io_uring`, Windows IOCP, Java's `AsynchronousFileChannel`: asynchronous.
- Node's `fs.readFile`: asynchronous **API**, implemented with **blocking** reads on the libuv thread
  pool (regular files don't support epoll readiness).

OS 09 builds on this. **Direct I/O** (`O_DIRECT`) bypasses the page cache: databases with their own
buffer pools use it to avoid double caching.

## 10. Disk scheduling (for spinning disks)

The head is at cylinder 53; requests are `98, 183, 37, 122, 14, 124, 65, 67` (the textbook example):

| Algorithm | Order | Head movement |
|---|---|---|
| **FCFS** | as they arrived | **640** |
| **SSTF** (shortest seek first) | 65, 67, 37, 14, 98, 122, 124, 183 | **236**, but it can **starve** far requests |
| **SCAN** (elevator, toward 0 first) | 37, 14, 0, 65, 67, 98, 122, 124, 183 | **236** |
| **C-SCAN** (sweep up, jump back, sweep up) | 65 … 183, 199, 0, 14, 37 | **382** including the return; more **uniform** waits |

SSDs have no seek time, so Linux uses `none` or `mq-deadline` for NVMe. Scheduling still matters for
fairness between workloads (`bfq`, cgroup `io.max` / `io.weight`) and for **merging** adjacent
requests.

## 11. Disk-full conditions and I/O bottlenecks

- **Disk full** → `ENOSPC`. Writes fail, databases stop accepting writes, logs stop (so you lose the
  evidence). It can also be **inode exhaustion** (`df -i`) with free bytes left: millions of tiny
  files.
- Keep logs on a separate volume, rotate them (`logrotate`), alert at 80 %, and handle `ENOSPC`
  explicitly.

**Troubleshooting a service with high disk latency:**

1. `iostat -x 1`: `await` (ms per I/O), `%util` (near 100 % = saturated on HDDs; less meaningful on
   NVMe), `aqu-sz` (queue depth), r/s vs w/s.
2. Who's doing it? `iotop`, `pidstat -d`.
3. Is it **fsync-heavy** (WAL, logging with sync), lots of small random I/O, swap (`vmstat si/so`),
   or the page cache being evicted under memory pressure (OS 07)?
4. Cloud volumes have **IOPS and throughput quotas and burst credits**. Throttling looks like a
   sudden `await` jump at a fixed IOPS ceiling.
5. Fixes: batch and group-commit, async logging, bigger or provisioned-IOPS volumes, local NVMe for
   hot data, caching, moving temp files off the data volume.

---

## Senior-level questions

**1. What happens when an application opens a file?** Path resolution (dentry cache, per-component
permission checks) → inode lookup or load → an open-file object (offset, flags) → the lowest free fd
in the process's table → return. Reads then go through the page cache.

**2. What is a file descriptor?** A small per-process integer referring to a kernel open-file object:
a file, socket, pipe, eventfd or epoll instance. It's inherited across fork, closed on exec with
`O_CLOEXEC`, and limited by `ulimit -n`.

**3. How do file descriptors differ from inodes?** An inode is the file's identity, metadata and data
location (one per file, shared by all its names). An fd is a per-process handle to an *open instance*
with its own offset and flags. Many fds can point to one inode, and an inode can exist with no fd.

**4. Why might a service fail with `Too many open files`?** Descriptor leaks (unclosed files or
sockets, unreturned pool connections, CLOSE_WAIT), legitimately high connection counts, or too-low
limits. Diagnose with `/proc/<pid>/fd`, `lsof` and `limits` (§3).

**5. What does `fsync()` provide, and what does it not guarantee by itself?** It flushes that file's
dirty data and metadata to stable storage before returning. It doesn't persist the directory entry of
a new file (fsync the directory too), doesn't help if the disk cache lies, can lose data after a
failed fsync, doesn't order writes across files, and doesn't replicate (§5).

**6. How can the page cache improve I/O performance?** Repeat reads are served from RAM, writes return
after dirtying memory (write-back, coalescing, reordering), read-ahead prefetches sequential data,
and mmap shares cached pages between processes. The trade-offs are durability and memory accounting
(§5).

**7. How would you troubleshoot a service with high disk latency?** §11: `iostat`/`iotop`, the fsync
pattern, swap and cache pressure, cloud IOPS throttling, then batching, async writes or better
storage.

---

**Next:** [09 · I/O multiplexing and event loops](../09-IO-Multiplexing-and-Event-Loops/README.md)
