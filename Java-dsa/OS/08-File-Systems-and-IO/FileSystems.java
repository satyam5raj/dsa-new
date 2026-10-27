/*
 * ============================================================================
 *  OS 08 : FILE SYSTEMS AND I/O  (real files, links, descriptors, fsync, atomic rename)
 * ============================================================================
 *  Run:  java FileSystems.java          (explanations: README.md in this folder)
 *
 *    1. inodes and links      hard link = another name for the same inode; symlink = a path that can dangle (real)
 *    2. file descriptors      a per-process table of open files; hitting the limit -> "Too many open files" (real, child JVM)
 *    3. permissions           rwx bits, octal modes, umask-style defaults (real)
 *    4. fsync                 data in the page cache vs data on disk; the real cost of force() (real)
 *    5. atomic replacement    write temp + fsync + rename: readers never see a torn file (real)
 *    6. file locking          exclusive vs shared locks (real FileLock)
 *    7. journaling            crash in the middle of a metadata update, with and without a journal
 *    8. disk scheduling       FCFS, SSTF, SCAN, C-SCAN head movement on the textbook queue
 * ============================================================================
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class FileSystems {

    static Path dir;

    // ---------------------------------------------------------------------
    // 1. inodes, hard links, symlinks
    // ---------------------------------------------------------------------
    static void links() throws Exception {
        section("1. Inodes, hard links and symbolic links (real files)");
        Path orig = dir.resolve("report.txt");
        Files.writeString(orig, "quarterly numbers");
        Path hard = dir.resolve("report-hardlink.txt"), soft = dir.resolve("report-symlink.txt");
        Files.createLink(hard, orig);
        Files.createSymbolicLink(soft, orig);
        Object inoOrig = Files.getAttribute(orig, "unix:ino"), inoHard = Files.getAttribute(hard, "unix:ino");
        Object inoSoftItself = Files.getAttribute(soft, "unix:ino", LinkOption.NOFOLLOW_LINKS);
        check("hard link and original share ONE inode (same file, two names)", true, inoOrig.equals(inoHard));
        check("the inode's link count is now 2", 2, Files.getAttribute(orig, "unix:nlink"));
        check("a symlink is its own small file (its own inode) containing a path", true, !inoSoftItself.equals(inoOrig));
        Files.delete(orig);                                                    // unlink one NAME
        check("after deleting the original name, the hard link still has the data", "quarterly numbers", Files.readString(hard));
        check("...and the symlink now dangles (points at a missing path)", false, Files.exists(soft));
        check("the dangling symlink itself still exists", true, Files.exists(soft, LinkOption.NOFOLLOW_LINKS));
        show("data blocks are freed only when the link count AND the number of open descriptors both reach 0");
    }

    // ---------------------------------------------------------------------
    // 2. file descriptors and their limit
    // ---------------------------------------------------------------------
    static void descriptors() throws Exception {
        section("2. File descriptors and 'Too many open files'");
        Path src = dir.resolve("Leak.java");
        Files.writeString(src, """
            import java.io.*; import java.util.*;
            public class Leak { public static void main(String[] a) throws Exception {
              List<FileInputStream> open = new ArrayList<>();
              try { while (true) open.add(new FileInputStream("/dev/null")); }   // never closed: a descriptor leak
              catch (IOException e) { System.out.println(open.size() + " " + e.getMessage()); } } }
            """);
        String java = ProcessHandle.current().info().command().orElse("java");
        Process p = new ProcessBuilder("sh", "-c", "ulimit -n 256 && exec '" + java + "' '" + src + "'").redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        p.waitFor();
        String last = out.lines().reduce((x, y) -> y).orElse("");
        show("child JVM with 'ulimit -n 256' leaking FileInputStreams: " + last);
        check("the leak dies with 'Too many open files' (EMFILE)", true, last.contains("Too many open files"));
        int leaked = Integer.parseInt(last.split(" ")[0]);
        check("it managed fewer than 256 opens (the JVM itself already uses some descriptors)", true, leaked > 100 && leaked < 256);
        show("every socket is a descriptor too: a server leaking connections hits the same wall (check: ls /proc/<pid>/fd | wc -l)");
    }

    // ---------------------------------------------------------------------
    // 3. permissions
    // ---------------------------------------------------------------------
    static String octal(Set<PosixFilePermission> perms) {
        int v = 0;
        for (PosixFilePermission p : perms) v |= 1 << (8 - p.ordinal());     // OWNER_READ is ordinal 0 -> bit 8
        return Integer.toOctalString(v);
    }

    static void permissions() throws Exception {
        section("3. Permissions");
        Path secret = dir.resolve("db-password");
        Files.writeString(secret, "s3cr3t");
        Files.setPosixFilePermissions(secret, PosixFilePermissions.fromString("rw-------"));
        check("chmod 600: owner read+write, nobody else", "600", octal(Files.getPosixFilePermissions(secret)));
        Path script = dir.resolve("deploy.sh");
        Files.writeString(script, "#!/bin/sh\necho ok\n");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        check("chmod 755: owner rwx, group and others r-x", "755", octal(Files.getPosixFilePermissions(script)));
        check("the execute bit lets the kernel run it", true, Files.isExecutable(script));
        show("directories: r = list names, w = create/delete entries, x = traverse (cd / access files inside)");
    }

    // ---------------------------------------------------------------------
    // 4. fsync
    // ---------------------------------------------------------------------
    static void fsync() throws Exception {
        section("4. Buffered writes vs fsync (real timings)");
        Path f = dir.resolve("wal.log");
        int records = 200;
        byte[] rec = "INSERT order=42 amount=100\n".getBytes(StandardCharsets.UTF_8);
        long t0 = System.nanoTime();
        try (FileChannel ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < records; i++) ch.write(ByteBuffer.wrap(rec));          // lands in the OS page cache: fast, NOT durable yet
        }
        double noSync = (System.nanoTime() - t0) / 1e6;
        t0 = System.nanoTime();
        try (FileChannel ch = FileChannel.open(f, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < records; i++) { ch.write(ByteBuffer.wrap(rec)); ch.force(false); }   // fdatasync after every record
        }
        double syncEach = (System.nanoTime() - t0) / 1e6;
        t0 = System.nanoTime();
        try (FileChannel ch = FileChannel.open(f, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < records; i++) ch.write(ByteBuffer.wrap(rec));
            ch.force(false);                                                              // group commit: one fsync for the batch
        }
        double groupCommit = (System.nanoTime() - t0) / 1e6;
        show(String.format("%d log records: no fsync %.2f ms, fsync each %.1f ms, one fsync per batch %.2f ms", records, noSync, syncEach, groupCommit));
        check("fsync per write is much slower: it waits for the device", true, syncEach > 5 * noSync);
        check("group commit amortises the fsync cost (how databases get throughput AND durability)", true, groupCommit < syncEach / 5);
        check("all records are in the file", (long) records * rec.length, Files.size(f));
    }

    // ---------------------------------------------------------------------
    // 5. atomic file replacement
    // ---------------------------------------------------------------------
    static void atomicReplace() throws Exception {
        section("5. Atomic file replacement (write temp + fsync + rename)");
        Path config = dir.resolve("config.json");
        String v1 = "{\"version\":1,\"payload\":\"" + "a".repeat(200_000) + "\"}";
        String v2 = "{\"version\":2,\"payload\":\"" + "b".repeat(200_000) + "\"}";
        Files.writeString(config, v1);
        AtomicBoolean stop = new AtomicBoolean();
        AtomicInteger torn = new AtomicInteger(), reads = new AtomicInteger();
        Thread reader = new Thread(() -> {
            while (!stop.get()) {
                try {
                    String s = Files.readString(config);
                    reads.incrementAndGet();
                    if (!(s.equals(v1) || s.equals(v2))) torn.incrementAndGet();      // partial / mixed content
                } catch (IOException ignored) { }
            }
        });
        reader.start();
        for (int i = 0; i < 40; i++) {
            Path tmp = dir.resolve("config.json.tmp");
            try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ch.write(ByteBuffer.wrap((i % 2 == 0 ? v2 : v1).getBytes(StandardCharsets.UTF_8)));
                ch.force(true);                                                         // data durable BEFORE it becomes visible
            }
            Files.move(tmp, config, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);   // rename(2): atomic swap of the name
        }
        stop.set(true);
        reader.join();
        show(String.format("%d concurrent reads during 40 replacements", reads.get()));
        check("with rename, readers see the complete old file or the complete new file, never a mix", 0, torn.get());

        AtomicInteger tornInPlace = new AtomicInteger();
        AtomicBoolean stop2 = new AtomicBoolean();
        Thread reader2 = new Thread(() -> {
            while (!stop2.get()) {
                try { String s = Files.readString(config); if (!(s.equals(v1) || s.equals(v2))) tornInPlace.incrementAndGet(); } catch (IOException ignored) { }
            }
        });
        reader2.start();
        for (int i = 0; i < 400; i++) Files.writeString(config, i % 2 == 0 ? v2 : v1);   // truncate + write in place
        stop2.set(true);
        reader2.join();
        show(String.format("overwriting in place instead: %d torn/empty reads observed", tornInPlace.get()));
        check("in-place overwrite exposes truncated / half-written files to readers", true, tornInPlace.get() > 0);
    }

    // ---------------------------------------------------------------------
    // 6. file locking
    // ---------------------------------------------------------------------
    static void locking() throws Exception {
        section("6. File locking");
        Path f = dir.resolve("job.lock");
        try (FileChannel a = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = a.lock()) {                                         // exclusive lock (fcntl/flock under the hood)
            String second;
            try (FileChannel b = FileChannel.open(f, StandardOpenOption.WRITE)) {
                b.tryLock();
                second = "acquired?!";
            } catch (OverlappingFileLockException e) { second = "refused: already locked"; }
            check("a second exclusive lock on the same file is refused", "refused: already locked", second);
            check("the first lock is valid and exclusive", "true/false", lock.isValid() + "/" + lock.isShared());
        }
        show("POSIX locks are ADVISORY: they only stop processes that also ask for the lock; they vanish if the process dies");
    }

    // ---------------------------------------------------------------------
    // 7. journaling
    // ---------------------------------------------------------------------
    static void journaling() {
        section("7. Journaling: surviving a crash mid-update");
        // creating a file touches 3 metadata structures: inode bitmap, inode table, directory entry
        List<String> steps = List.of("bitmap: mark inode 42 used", "inode 42: size=0, owner=app", "dir /data: add 'orders.db' -> 42");
        Set<String> disk = new HashSet<>();
        for (int i = 0; i < 2; i++) disk.add(steps.get(i));                       // CRASH after 2 of 3 writes
        check("without a journal: inode allocated but no directory entry -> a leaked inode (needs fsck)", false, disk.contains(steps.get(2)));
        List<String> journal = new ArrayList<>(steps);
        journal.add("COMMIT");                                                    // write intent + commit record first
        Set<String> disk2 = new HashSet<>();
        disk2.add(steps.get(0));                                                  // crash during checkpointing
        if (journal.get(journal.size() - 1).equals("COMMIT")) disk2.addAll(steps.subList(0, 3));   // replay committed transactions on boot
        check("with a journal: replay the committed transaction -> all 3 updates applied", 3, disk2.size());
        show("uncommitted journal entries are discarded: all-or-nothing; ext4 'ordered' mode journals metadata, writes data first");
    }

    // ---------------------------------------------------------------------
    // 8. disk scheduling
    // ---------------------------------------------------------------------
    static int movement(int start, List<Integer> order) {
        int pos = start, total = 0;
        for (int c : order) { total += Math.abs(c - pos); pos = c; }
        return total;
    }

    static void diskScheduling() {
        section("8. Disk-arm scheduling (head at 53, cylinders 0-199)");
        List<Integer> q = List.of(98, 183, 37, 122, 14, 124, 65, 67);
        int fcfs = movement(53, q);
        List<Integer> remaining = new ArrayList<>(q), sstf = new ArrayList<>();
        int pos = 53;
        while (!remaining.isEmpty()) { final int p = pos; int next = remaining.stream().min(Comparator.comparingInt(c -> Math.abs(c - p))).get(); sstf.add(next); remaining.remove(Integer.valueOf(next)); pos = next; }
        List<Integer> sorted = new ArrayList<>(q);
        Collections.sort(sorted);
        List<Integer> scan = new ArrayList<>();                                   // elevator: go down to 0, then up
        for (int i = sorted.size() - 1; i >= 0; i--) if (sorted.get(i) < 53) scan.add(sorted.get(i));
        scan.add(0);
        for (int c : sorted) if (c > 53) scan.add(c);
        List<Integer> cscan = new ArrayList<>();                                  // circular: up to the end, jump to 0, continue up
        for (int c : sorted) if (c > 53) cscan.add(c);
        cscan.add(199);
        cscan.add(0);
        for (int c : sorted) if (c < 53) cscan.add(c);
        show("SSTF order " + sstf + ", SCAN order " + scan);
        check("FCFS total head movement (textbook: 640)", 640, fcfs);
        check("SSTF (shortest seek first, textbook: 236) -- can starve far requests", 236, movement(53, sstf));
        check("SCAN / elevator toward 0 (textbook: 236)", 236, movement(53, scan));
        check("C-SCAN incl. the return sweep: uniform waiting time", 382, movement(53, cscan));
        show("SSDs have no arm: Linux uses 'none'/'mq-deadline' for NVMe; ordering still matters for fairness and merging");
    }

    public static void main(String[] args) throws Exception {
        dir = Files.createTempDirectory("fs-demo");
        try {
            links();
            descriptors();
            permissions();
            fsync();
            atomicReplace();
            locking();
            journaling();
            diskScheduling();
        } finally {
            try (var s = Files.walk(dir)) { s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
        }
        summary();
    }

    // =====================================================================
    static int passed = 0, total = 0;
    static void section(String s) { System.out.println(s); }
    static void show(String s) { System.out.println("      " + s); }

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-62s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
