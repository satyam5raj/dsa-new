/*
 * ============================================================================
 *  ADVANCE 02 : STOCK PRICE FLUCTUATION  ⭐ bonus   LeetCode #2034
 *  https://leetcode.com/problems/stock-price-fluctuation/
 *  Pattern: HashMap (timestamp -> price) + two heaps with LAZY DELETION
 * ============================================================================
 *
 *  PROBLEM
 *    Prices arrive as (timestamp, price) records, out of order, and a later record for
 *    the same timestamp CORRECTS the earlier one.
 *      update(timestamp, price)   current() = price at the latest timestamp
 *      maximum() / minimum()      over the CURRENT (corrected) prices
 *    update(1,10) update(2,5)  current 5  maximum 10
 *    update(1,3)  (correction) maximum 5
 *    update(4,2)               minimum 2
 *
 *  ---------------------------------------------------------------------------
 *  THE DIFFICULTY: a correction must REMOVE the old price from whatever finds the
 *  maximum. A PriorityQueue can't remove an arbitrary element quickly (O(n)).
 *
 *  IDEA 1 - TreeMap<price, count>: a correction decrements the old price's count and
 *  increments the new one. max = lastKey, min = firstKey. O(log n) everything.
 *
 *  IDEA 2 - LAZY DELETION (the main solution): never remove old entries from the heaps.
 *  Push (price, timestamp) on every update. When asked for the max, look at the top: if
 *  priceAt[timestamp] is no longer that price, the entry is STALE -> throw it away and
 *  look again. The HashMap is the "source of truth" the heaps are checked against.
 *  Each pushed entry is thrown away at most once, so the total work stays O(n log n).
 *
 *  DRY RUN (max-heap only)
 *    update(1,10) update(2,5)   heap [(10,t1) (5,t2)]     maximum: top (10,t1), priceAt[1]=10 -> 10
 *    update(1,3)                heap [(10,t1) (5,t2) (3,t1)]
 *    maximum: top (10,t1) but priceAt[1]=3 -> stale, poll;  top (5,t2), priceAt[2]=5 -> 5
 *
 *  COMPLEXITY   update O(log n)   max/min amortised O(log n)   current O(1)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A02_StockPriceFluctuation {

    // ---------------- Approach 1: scan every price (the referee) ----------------
    static class ScanStockPrice {
        private final Map<Integer, Integer> priceAt = new HashMap<>();
        private int latest = 0;
        public void update(int timestamp, int price) { priceAt.put(timestamp, price); latest = Math.max(latest, timestamp); }
        public int current() { return priceAt.get(latest); }
        public int maximum() { return Collections.max(priceAt.values()); }
        public int minimum() { return Collections.min(priceAt.values()); }
    }

    // ---------------- Approach 2: HashMap + TreeMap of price counts ----------------
    static class TreeMapStockPrice {
        private final Map<Integer, Integer> priceAt = new HashMap<>();
        private final TreeMap<Integer, Integer> countOfPrice = new TreeMap<>();
        private int latest = 0;

        public void update(int timestamp, int price) {
            Integer old = priceAt.put(timestamp, price);                 // put returns the previous value (or null)
            if (old != null && countOfPrice.merge(old, -1, Integer::sum) == 0) countOfPrice.remove(old);
            countOfPrice.merge(price, 1, Integer::sum);
            latest = Math.max(latest, timestamp);
        }
        public int current() { return priceAt.get(latest); }
        public int maximum() { return countOfPrice.lastKey(); }
        public int minimum() { return countOfPrice.firstKey(); }
    }

    // ---------------- Approach 3: HashMap + two heaps, lazy deletion (main solution) ----------------
    static class StockPrice {
        private final Map<Integer, Integer> priceAt = new HashMap<>();
        private int latest = 0;
        private final PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));   // {price, timestamp}
        private final PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));

        public void update(int timestamp, int price) {
            priceAt.put(timestamp, price);
            latest = Math.max(latest, timestamp);
            maxHeap.add(new int[]{price, timestamp});
            minHeap.add(new int[]{price, timestamp});
        }

        public int current() { return priceAt.get(latest); }
        public int maximum() { return validTop(maxHeap); }
        public int minimum() { return validTop(minHeap); }

        private int validTop(PriorityQueue<int[]> heap) {
            while (priceAt.get(heap.peek()[1]) != heap.peek()[0]) heap.poll();   // stale: that timestamp was corrected
            return heap.peek()[0];
        }
    }

    public static void main(String[] args) {
        StockPrice sp = new StockPrice();
        sp.update(1, 10); sp.update(2, 5);
        check("current", 5, sp.current());
        check("maximum", 10, sp.maximum());
        sp.update(1, 3);
        check("maximum after correcting t1", 5, sp.maximum());
        sp.update(4, 2);
        check("minimum", 2, sp.minimum());

        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            StockPrice main = new StockPrice();
            TreeMapStockPrice tree = new TreeMapStockPrice();
            ScanStockPrice ref = new ScanStockPrice();
            for (int i = 0; i < 300; i++) {
                int ts = 1 + rnd.nextInt(30), price = 1 + rnd.nextInt(50);    // few timestamps -> many corrections
                main.update(ts, price); tree.update(ts, price); ref.update(ts, price);
                int q = rnd.nextInt(3);
                int want = q == 0 ? ref.current() : q == 1 ? ref.maximum() : ref.minimum();
                int got1 = q == 0 ? main.current() : q == 1 ? main.maximum() : main.minimum();
                int got2 = q == 0 ? tree.current() : q == 1 ? tree.maximum() : tree.minimum();
                ok &= got1 == want && got2 == want;
            }
        }
        check("60,000 random updates: heaps == TreeMap == scan", true, ok);
        summary();
    }

    // ---------------- tiny test helper (same in every problem file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-34s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
