/*
 * ============================================================================
 *  OS 05 : MEMORY MANAGEMENT  (virtual memory, paging, TLB, faults, fragmentation)
 * ============================================================================
 *  Run:  java Memory.java          (explanations: README.md in this folder)
 *
 *    1. address translation   virtual address -> (page, offset) -> two-level page table -> physical address
 *    2. TLB                   hit rates for sequential vs random access; effective access time
 *    3. page faults           demand paging: minor (page in RAM) vs major (from disk) faults
 *    4. protection            unmapped / read-only / kernel-only pages -> segmentation fault
 *    5. copy-on-write         fork shares pages until someone writes
 *    6. locality (real)       row-major vs column-major traversal of a big 2-D array
 *    7. fragmentation         external (first-fit vs best-fit holes) and internal (rounding to blocks)
 *    8. memory-mapped file    map a real file, write through memory, read it back via the file API
 *    9. stack vs heap         what lives where; StackOverflowError vs OutOfMemoryError
 *   10. NUMA                  local vs remote memory access cost
 * ============================================================================
 */

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Memory {

    // ---------------------------------------------------------------------
    // 1-4. a tiny MMU: 32-bit addresses, 4 KB pages, two-level page table, TLB
    // ---------------------------------------------------------------------
    static final int PAGE = 4096;

    record Pte(int frame, boolean present, boolean writable, boolean user, boolean onDisk) { }

    static class Mmu {
        final Map<Integer, Map<Integer, Pte>> directory = new HashMap<>();       // level 1 index (10 bits) -> level 2 table
        final LinkedHashMap<Integer, Integer> tlb = new LinkedHashMap<>(16, 0.75f, true) {   // vpn -> frame, LRU, 64 entries
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> e) { return size() > 64; }
        };
        int tlbHits, tlbMisses, minorFaults, majorFaults, nextFrame = 100;

        void map(int vpn, Pte pte) { directory.computeIfAbsent(vpn >>> 10, k -> new HashMap<>()).put(vpn & 0x3FF, pte); }

        /** Returns the physical address, or throws a "segfault". */
        long translate(long vaddr, boolean write, boolean userMode) {
            int vpn = (int) (vaddr >>> 12), offset = (int) (vaddr & 0xFFF);
            Integer frame = tlb.get(vpn);
            if (frame != null) { tlbHits++; return (long) frame * PAGE + offset; }
            tlbMisses++;                                                  // page walk: 2 memory reads (directory, table)
            Pte pte = Optional.ofNullable(directory.get(vpn >>> 10)).map(t -> t.get(vpn & 0x3FF)).orElse(null);
            if (pte == null) throw new IllegalStateException("SIGSEGV: unmapped address 0x" + Long.toHexString(vaddr));
            if (write && !pte.writable()) throw new IllegalStateException("SIGSEGV: write to read-only page 0x" + Long.toHexString(vaddr));
            if (userMode && !pte.user()) throw new IllegalStateException("SIGSEGV: user access to kernel page 0x" + Long.toHexString(vaddr));
            if (!pte.present()) {                                         // PAGE FAULT: valid address, not in RAM yet
                if (pte.onDisk()) majorFaults++; else minorFaults++;       // major: read from disk/swap; minor: just allocate/map
                pte = new Pte(nextFrame++, true, pte.writable(), pte.user(), false);
                map(vpn, pte);
            }
            tlb.put(vpn, pte.frame());
            return (long) pte.frame() * PAGE + offset;
        }
    }

    static void translation() {
        section("1-4. Address translation, TLB, page faults, protection");
        Mmu m = new Mmu();
        m.map(0x12345, new Pte(7, true, true, true, false));
        long va = 0x12345ABCL;
        long pa = m.translate(va, false, true);
        check("0x12345ABC -> page 0x12345 + offset 0xABC -> frame 7 -> 0x7ABC", "0x7abc", "0x" + Long.toHexString(pa));
        check("level-1 index = top 10 bits, level-2 index = next 10, offset = low 12", "72/837/2748", ((va >>> 22)) + "/" + ((va >>> 12) & 0x3FF) + "/" + (va & 0xFFF));

        for (int p = 0; p < 1000; p++) m.map(0x20000 + p, new Pte(-1, false, true, true, p % 10 == 0));   // 1000 pages, not yet in RAM; every 10th was swapped out
        for (long a = 0x20000000L; a < 0x20000000L + 1000L * PAGE; a += 64) m.translate(a, true, true);   // sequential, 64-byte cache lines
        show(String.format("sequential scan of 4 MB: TLB hits %,d, misses %,d; minor faults %d, major faults %d", m.tlbHits, m.tlbMisses, m.minorFaults, m.majorFaults));
        check("sequential access: one TLB miss per page, then 63 hits", true, m.tlbHits > 60 * m.tlbMisses);
        check("first touch of each page faults (demand paging): 900 minor + 100 major", "900/100", m.minorFaults + "/" + m.majorFaults);

        Mmu r = new Mmu();
        for (int p = 0; p < 1000; p++) r.map(0x20000 + p, new Pte(p, true, true, true, false));
        Random rnd = new Random(1);
        for (int i = 0; i < 64_000; i++) r.translate(0x20000000L + rnd.nextInt(1000 * PAGE), false, true);
        double hitRate = r.tlbHits / (double) (r.tlbHits + r.tlbMisses);
        show(String.format("random access over 1000 pages with a 64-entry TLB: hit rate %.1f%%", hitRate * 100));
        check("random access thrashes a small TLB (~64/1000 hit rate) -> why huge pages help", true, hitRate < 0.1);
        double mem = 100, eatSeq = 0.98 * mem + 0.02 * 3 * mem, eatRand = hitRate * mem + (1 - hitRate) * 3 * mem;
        show(String.format("effective access time (100 ns RAM, 2-level walk): sequential ~%.0f ns, random ~%.0f ns", eatSeq, eatRand));

        String err1 = trySegv(() -> m.translate(0x0L, false, true));
        String err2 = trySegv(() -> { m.map(0x400, new Pte(3, true, false, true, false)); m.translate(0x400000L, true, true); });
        String err3 = trySegv(() -> { m.map(0xC0000, new Pte(4, true, true, false, false)); m.translate(0xC0000000L, false, true); });
        check("dereferencing NULL (page 0 is never mapped)", "SIGSEGV: unmapped address 0x0", err1);
        check("writing to a read-only page (e.g. code / string literals)", true, err2.startsWith("SIGSEGV: write to read-only"));
        check("user code touching a kernel-only page", true, err3.startsWith("SIGSEGV: user access to kernel"));
    }

    static String trySegv(Runnable r) { try { r.run(); return "no fault"; } catch (IllegalStateException e) { return e.getMessage(); } }

    // ---------------------------------------------------------------------
    // 5. copy-on-write
    // ---------------------------------------------------------------------
    static void copyOnWrite() {
        section("5. Copy-on-write after fork");
        int pages = 25_600;                                              // a 100 MB process
        int[] parent = new int[pages], child = new int[pages];
        Map<Integer, Integer> refCount = new HashMap<>();
        for (int p = 0; p < pages; p++) { parent[p] = p; child[p] = p; refCount.put(p, 2); }   // fork: share every frame, mark read-only
        int copies = 0, nextFrame = pages;
        for (int p = 0; p < 256; p++) {                                  // the child writes to 1 MB of its memory
            if (refCount.get(child[p]) > 1) { refCount.merge(child[p], -1, Integer::sum); child[p] = nextFrame++; copies++; }   // fault -> private copy
        }
        long distinctFrames = Arrays.stream(parent).distinct().count() + copies;
        show("fork itself copied 0 pages: parent and child share all 25,600 frames, marked read-only");
        check("only the 256 pages the child WROTE got copied", 256, copies);
        show(String.format("memory used: %d frames (%.1f MB) instead of %d (%.0f MB) for a full copy", distinctFrames, distinctFrames * 4 / 1024.0, 2 * pages, 2 * pages * 4 / 1024.0));
        check("parent still sees its original page", 0, parent[0]);
        show("Redis BGSAVE relies on this; heavy writes during the save -> memory can nearly double");
    }

    // ---------------------------------------------------------------------
    // 6. locality, measured for real
    // ---------------------------------------------------------------------
    static void locality() {
        section("6. Locality of reference (real timing)");
        int n = 4096;
        int[][] a = new int[n][n];                                        // 64 MB
        for (int[] row : a) Arrays.fill(row, 1);
        long best1 = Long.MAX_VALUE, best2 = Long.MAX_VALUE, sum = 0;
        for (int rep = 0; rep < 3; rep++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) sum += a[i][j];    // row by row: consecutive addresses
            best1 = Math.min(best1, System.nanoTime() - t0);
            t0 = System.nanoTime();
            for (int j = 0; j < n; j++) for (int i = 0; i < n; i++) sum += a[i][j];    // column by column: a new cache line (and often page) each step
            best2 = Math.min(best2, System.nanoTime() - t0);
        }
        show(String.format("sum 16M ints: row-major %.0f ms, column-major %.0f ms (%.1fx slower)", best1 / 1e6, best2 / 1e6, (double) best2 / best1));
        check("same work, same answer", 6L * n * n, sum);
        check("striding across rows defeats caches, prefetchers and the TLB (> 3x slower)", true, best2 > 3 * best1);
    }

    // ---------------------------------------------------------------------
    // 7. fragmentation
    // ---------------------------------------------------------------------
    static int allocate(List<int[]> holes, int size, boolean bestFit) {
        int chosen = -1;
        for (int i = 0; i < holes.size(); i++) {
            if (holes.get(i)[1] < size) continue;
            if (chosen < 0 || (bestFit ? holes.get(i)[1] < holes.get(chosen)[1] : false)) chosen = i;
            if (!bestFit && chosen >= 0) break;                          // first fit: take the first one big enough
        }
        if (chosen < 0) return -1;
        int[] h = holes.get(chosen);
        int start = h[0];
        h[0] += size;
        h[1] -= size;
        if (h[1] == 0) holes.remove(chosen);
        return start;
    }

    static void fragmentation() {
        section("7. Fragmentation");
        List<int[]> holes = new ArrayList<>(List.of(new int[]{0, 100}, new int[]{200, 30}, new int[]{300, 50}));   // free: 100 + 30 + 50 = 180
        int free = holes.stream().mapToInt(h -> h[1]).sum();
        check("180 bytes free in total, but the largest hole is 100", "180 / 100", free + " / " + holes.stream().mapToInt(h -> h[1]).max().getAsInt());
        check("EXTERNAL fragmentation: a 120-byte request fails although 180 are free", -1, allocate(new ArrayList<>(holes.stream().map(int[]::clone).toList()), 120, false));
        List<int[]> ff = new ArrayList<>(holes.stream().map(int[]::clone).toList()), bf = new ArrayList<>(holes.stream().map(int[]::clone).toList());
        int[] requests = {25, 90, 45};
        String ffRes = "", bfRes = "";
        for (int req : requests) { ffRes += allocate(ff, req, false) + " "; bfRes += allocate(bf, req, true) + " "; }
        show("requests 25, 90, 45 -> first fit at [" + ffRes.trim() + "], best fit at [" + bfRes.trim() + "]");
        check("first fit carves the big hole for 25 -> later 90 fails", true, ffRes.trim().contains("-1"));
        check("best fit puts 25 in the 30-hole and keeps the big hole for 90", true, !bfRes.trim().contains("-1"));
        int blockSize = 64, request = 65;
        int wasted = ((request + blockSize - 1) / blockSize) * blockSize - request;
        check("INTERNAL fragmentation: 65 bytes in 64-byte blocks wastes 63 bytes", 63, wasted);
        show("fixes: compaction (moving GCs), size-class slabs/arenas (jemalloc, tcmalloc), paging (no external fragmentation of RAM)");
    }

    // ---------------------------------------------------------------------
    // 8. memory-mapped file (real)
    // ---------------------------------------------------------------------
    static void mmap() throws Exception {
        section("8. Memory-mapped file (real mmap)");
        Path f = Files.createTempFile("mapped", ".bin");
        try (RandomAccessFile raf = new RandomAccessFile(f.toFile(), "rw"); FileChannel ch = raf.getChannel()) {
            MappedByteBuffer buf = ch.map(FileChannel.MapMode.READ_WRITE, 0, PAGE * 4L);   // 4 pages of the file now look like memory
            buf.put(0, "hello via memory".getBytes(StandardCharsets.UTF_8));
            buf.putInt(PAGE * 3, 0xCAFEBABE);                              // touch the 4th page: a page fault maps it in
            buf.force();                                                   // like msync(): flush dirty pages to the file
        }
        byte[] disk = Files.readAllBytes(f);
        check("bytes written through memory are in the file", "hello via memory", new String(disk, 0, 16, StandardCharsets.UTF_8));
        check("file size = mapped length (4 pages)", 16384L, Files.size(f));
        check("int written at offset 12288", "cafebabe", Integer.toHexString(java.nio.ByteBuffer.wrap(disk, PAGE * 3, 4).getInt()));
        Files.delete(f);
        show("used by databases (LMDB, MongoDB's old engine), Kafka index files, shared libraries, and zero-copy reads");
    }

    // ---------------------------------------------------------------------
    // 9-10. stack vs heap, NUMA
    // ---------------------------------------------------------------------
    static String runChildJvm(String flag, String source) {
        try {
            Path dir = Files.createTempDirectory("hog");
            Path src = dir.resolve("Hog.java");
            Files.writeString(src, source);
            String java = ProcessHandle.current().info().command().orElse("java");
            Process p = new ProcessBuilder(java, flag, src.toString()).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor();
            Files.delete(src);
            Files.delete(dir);
            return out;
        } catch (Exception e) { return e.toString(); }
    }

    static void stackHeapNuma() {
        section("9-10. Stack vs heap, NUMA");
        String oom = runChildJvm("-Xmx64m",                               // a separate small JVM, so this demo can't eat the machine's RAM
            "public class Hog { public static void main(String[] a) { java.util.List<long[]> l = new java.util.ArrayList<>(); while (true) l.add(new long[1 << 20]); } }");
        check("heap exhausted -> OutOfMemoryError (the JVM's -Xmx limit, not the OS's)", true, oom.contains("OutOfMemoryError: Java heap space"));
        show("stack: frames + locals, per thread, LIFO, freed automatically; heap: objects, shared, managed by malloc / GC");
        double local = 80, remote = 140;
        double mixed = 0.5 * local + 0.5 * remote;
        check("NUMA: a thread whose memory is half remote pays ~38% more per access", "37.5", String.format("%.1f", (mixed - local) / local * 100));
    }

    public static void main(String[] args) throws Exception {
        translation();
        copyOnWrite();
        locality();
        fragmentation();
        mmap();
        stackHeapNuma();
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
