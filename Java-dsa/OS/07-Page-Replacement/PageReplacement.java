/*
 * ============================================================================
 *  OS 07 : PAGE REPLACEMENT, WORKING SETS AND THRASHING
 * ============================================================================
 *  Run:  java PageReplacement.java          (explanations: README.md in this folder)
 *
 *    1. FIFO, LRU, OPT, Clock  page faults on the classic textbook reference string
 *    2. Belady's anomaly       FIFO with MORE frames causes MORE faults
 *    3. locality               real programs re-use a small working set -> LRU / Clock do well
 *    4. working set            the distinct pages touched in the last delta references
 *    5. thrashing              when the working set doesn't fit, faults explode and throughput collapses
 *    6. LRU cache              the same idea at application level (LinkedHashMap)
 * ============================================================================
 */

import java.util.*;

public class PageReplacement {

    static int fifo(int[] refs, int frames) {
        Deque<Integer> q = new ArrayDeque<>();
        Set<Integer> in = new HashSet<>();
        int faults = 0;
        for (int p : refs) {
            if (in.contains(p)) continue;
            faults++;
            if (q.size() == frames) in.remove(q.pollFirst());           // evict the OLDEST loaded page
            q.addLast(p);
            in.add(p);
        }
        return faults;
    }

    static int lru(int[] refs, int frames) {
        LinkedHashSet<Integer> order = new LinkedHashSet<>();            // iteration order = least -> most recently used
        int faults = 0;
        for (int p : refs) {
            if (order.remove(p)) { order.add(p); continue; }             // hit: move to most-recent
            faults++;
            if (order.size() == frames) { Integer victim = order.iterator().next(); order.remove(victim); }   // evict LEAST recently used
            order.add(p);
        }
        return faults;
    }

    static int opt(int[] refs, int frames) {
        Set<Integer> in = new HashSet<>();
        int faults = 0;
        for (int i = 0; i < refs.length; i++) {
            if (in.contains(refs[i])) continue;
            faults++;
            if (in.size() == frames) {                                   // evict the page used FARTHEST in the future (needs the future!)
                int victim = -1, farthest = -1;
                for (int p : in) {
                    int next = Integer.MAX_VALUE;
                    for (int j = i + 1; j < refs.length; j++) if (refs[j] == p) { next = j; break; }
                    if (next > farthest) { farthest = next; victim = p; }
                }
                in.remove(victim);
            }
            in.add(refs[i]);
        }
        return faults;
    }

    static int clock(int[] refs, int frames) {
        int[] page = new int[frames];
        boolean[] ref = new boolean[frames];
        Arrays.fill(page, -1);
        int hand = 0, faults = 0;
        for (int p : refs) {
            int slot = -1;
            for (int i = 0; i < frames; i++) if (page[i] == p) slot = i;
            if (slot >= 0) { ref[slot] = true; continue; }               // hit: the hardware sets the "accessed" bit
            faults++;
            while (ref[hand] && page[hand] != -1) { ref[hand] = false; hand = (hand + 1) % frames; }   // second chance: clear and move on
            page[hand] = p;
            ref[hand] = true;
            hand = (hand + 1) % frames;
        }
        return faults;
    }

    static final int[] TEXTBOOK = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};

    static void classic() {
        section("1. FIFO vs LRU vs OPT vs Clock (reference string of 20, 3 frames)");
        int f = fifo(TEXTBOOK, 3), l = lru(TEXTBOOK, 3), o = opt(TEXTBOOK, 3), c = clock(TEXTBOOK, 3);
        show(String.format("faults: FIFO %d, LRU %d, OPT %d, Clock %d", f, l, o, c));
        check("FIFO (the classic textbook answer)", 15, f);
        check("LRU", 12, l);
        check("OPT: the theoretical minimum (Belady's MIN)", 9, o);
        check("OPT <= LRU <= FIFO here; Clock approximates LRU cheaply", true, o <= l && l <= f && c <= f);
    }

    static void belady() {
        section("2. Belady's anomaly");
        int[] refs = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};
        int f3 = fifo(refs, 3), f4 = fifo(refs, 4), l3 = lru(refs, 3), l4 = lru(refs, 4);
        show(String.format("FIFO: 3 frames -> %d faults, 4 frames -> %d faults;  LRU: 3 -> %d, 4 -> %d", f3, f4, l3, l4));
        check("FIFO: more memory, MORE faults (9 -> 10)", "9 -> 10", f3 + " -> " + f4);
        check("LRU is a stack algorithm: more frames never hurt", true, l4 <= l3);
    }

    static int[] localityTrace(int n, long seed) {
        Random r = new Random(seed);
        int[] refs = new int[n];
        int base = 0;
        for (int i = 0; i < n; i++) {
            if (i % 500 == 0) base = r.nextInt(90);                      // the program moves to a new phase / function
            refs[i] = r.nextDouble() < 0.9 ? base + r.nextInt(8) : r.nextInt(100);   // 90% of refs within an 8-page working set
        }
        return refs;
    }

    static void locality() {
        section("3. Locality: real-ish access patterns");
        int[] refs = localityTrace(20_000, 4);
        Random rnd = new Random(5);
        int[] random = new int[20_000];
        for (int i = 0; i < random.length; i++) random[i] = rnd.nextInt(100);
        int lruLocal = lru(refs, 16), fifoLocal = fifo(refs, 16), lruRandom = lru(random, 16);
        show(String.format("16 frames, 100 pages: locality trace LRU %d faults, FIFO %d; uniform random LRU %d", lruLocal, fifoLocal, lruRandom));
        check("with locality LRU faults on < 15% of references", true, lruLocal < 0.15 * refs.length);
        check("with no locality any policy faults on ~84% (only 16/100 fit)", true, Math.abs(lruRandom / 20000.0 - 0.84) < 0.02);
    }

    static void workingSetAndThrashing() {
        section("4-5. Working set and thrashing");
        int[] refs = localityTrace(20_000, 6);
        int delta = 200;
        Set<Integer> ws = new HashSet<>();
        for (int i = 10_000 - delta; i < 10_000; i++) ws.add(refs[i]);
        show("working set W(t=10000, delta=200) = " + ws.size() + " distinct pages");
        check("the working set is far smaller than the 100-page address space", true, ws.size() < 40);
        Map<Integer, Integer> faultsByFrames = new TreeMap<>();
        for (int fr : new int[]{4, 6, 8, 12, 16, 32, 64}) faultsByFrames.put(fr, lru(refs, fr));
        show("LRU faults vs frames: " + faultsByFrames);
        check("below the working-set size, faults EXPLODE (thrashing)", true, faultsByFrames.get(4) > 4 * faultsByFrames.get(16));
        double diskMs = 5, memNs = 100;
        double fRate4 = faultsByFrames.get(4) / 20000.0, fRate16 = faultsByFrames.get(16) / 20000.0;
        double eat4 = (1 - fRate4) * memNs / 1e6 + fRate4 * diskMs, eat16 = (1 - fRate16) * memNs / 1e6 + fRate16 * diskMs;
        show(String.format("effective access time with a 5 ms major fault: 16 frames %.3f ms, 4 frames %.3f ms per reference", eat16, eat4));
        check("a few % more faults -> each access orders of magnitude slower: the CPU waits on disk", true, eat4 > 3 * eat16);
        check("diminishing returns once the working set fits", true, faultsByFrames.get(32) - faultsByFrames.get(64) < faultsByFrames.get(4) - faultsByFrames.get(8));
    }

    static void lruCache() {
        section("6. The same idea in your code: an LRU cache");
        Map<String, String> cache = new LinkedHashMap<>(16, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, String> e) { return size() > 3; }
        };
        cache.put("a", "1"); cache.put("b", "2"); cache.put("c", "3");
        cache.get("a");                                                  // a becomes most recently used
        cache.put("d", "4");                                             // evicts the least recently used: b
        check("LinkedHashMap(accessOrder=true) + removeEldestEntry = an LRU cache", "[c, a, d]", cache.keySet().toString());
    }

    public static void main(String[] args) {
        classic();
        belady();
        locality();
        workingSetAndThrashing();
        lruCache();
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
