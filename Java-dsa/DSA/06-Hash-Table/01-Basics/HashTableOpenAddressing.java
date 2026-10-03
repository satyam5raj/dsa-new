/*
 * ============================================================================
 *  HASH TABLE 1.3 : OPEN ADDRESSING (linear probing) + TOMBSTONES
 * ============================================================================
 *
 *  Run:  java HashTableOpenAddressing.java
 *
 *  NO chains: every entry lives directly in the array. On a collision, PROBE
 *  for the next free slot:
 *      linear probing     : i, i+1, i+2, ...                  (used here)
 *      quadratic probing  : i, i+1, i+4, i+9, ...             (spreads clusters)
 *      double hashing     : i, i+h2, i+2*h2, ...              (a second hash sets the step)
 *
 *    insert "apple" -> hashes to 3, slot 3 taken by "kiwi" -> try 4: free -> store it there
 *    get("apple")   -> start at 3: "kiwi" != "apple" -> 4: found
 *                      a lookup stops at the first EMPTY slot ("the key can't be further")
 *
 *  THE DELETION PROBLEM: if we simply EMPTY the slot holding "kiwi", a later
 *  get("apple") stops at slot 3 (empty) and wrongly says "not found", because the
 *  probe chain is broken.
 *  FIX: TOMBSTONES. Mark the slot "deleted": lookups keep probing past it, and
 *  inserts may reuse it. Resizing cleans the tombstones out.
 *
 *  LOAD FACTOR MATTERS MORE: probe counts explode as the table fills up
 *  (clusters form and merge). Keep the load <= ~0.5-0.7. The demo measures it.
 *
 *  PROS: no pointers, contiguous memory -> CPU-cache friendly and fast.
 *        (Python's dict, Rust's HashMap and Google's SwissTable use open addressing.)
 *  CONS: deletion needs tombstones; very sensitive to the load factor and the hash quality.
 */

import java.util.*;

public class HashTableOpenAddressing {

    // int keys -> int values, to keep the probing logic in focus
    private int[] keys, values;
    private byte[] state;                       // 0 = EMPTY, 1 = OCCUPIED, 2 = DELETED (tombstone)
    private int size, tombstones;
    private long probes;                        // for the statistics

    private static final byte EMPTY = 0, OCCUPIED = 1, DELETED = 2;

    HashTableOpenAddressing(int capacity) {
        keys = new int[capacity];
        values = new int[capacity];
        state = new byte[capacity];
    }

    private int home(int key) {
        int h = key * 0x9E3779B9;               // multiplicative hashing: scrambles nearby integers
        return (h ^ (h >>> 16)) & (keys.length - 1);   // capacity is a power of 2
    }

    void put(int key, int value) {
        if ((size + tombstones + 1) * 10 > keys.length * 7) rehash(keys.length * 2);   // load factor 0.7
        int i = home(key), firstTombstone = -1;
        while (state[i] != EMPTY) {
            probes++;
            if (state[i] == OCCUPIED && keys[i] == key) { values[i] = value; return; }   // update
            if (state[i] == DELETED && firstTombstone < 0) firstTombstone = i;          // remember a reusable slot
            i = (i + 1) & (keys.length - 1);                                            // linear probe, wrapping
        }
        if (firstTombstone >= 0) { i = firstTombstone; tombstones--; }                  // reuse the tombstone
        keys[i] = key;
        values[i] = value;
        state[i] = OCCUPIED;
        size++;
    }

    Integer get(int key) {
        int i = find(key);
        return i < 0 ? null : values[i];
    }

    boolean remove(int key) {
        int i = find(key);
        if (i < 0) return false;
        state[i] = DELETED;                     // a tombstone: NOT empty, so probe chains stay intact
        size--;
        tombstones++;
        return true;
    }

    private int find(int key) {
        int i = home(key);
        while (state[i] != EMPTY) {             // only a truly EMPTY slot ends the search
            probes++;
            if (state[i] == OCCUPIED && keys[i] == key) return i;
            i = (i + 1) & (keys.length - 1);
        }
        return -1;
    }

    private void rehash(int newCapacity) {
        int[] oldKeys = keys, oldValues = values;
        byte[] oldState = state;
        keys = new int[newCapacity];
        values = new int[newCapacity];
        state = new byte[newCapacity];
        size = 0;
        tombstones = 0;                         // tombstones are dropped during the rehash
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldState[i] == OCCUPIED) put(oldKeys[i], oldValues[i]);
        }
    }

    int size() { return size; }

    public static void main(String[] args) {
        System.out.println("===== WHY TOMBSTONES: a naive delete breaks the probe chain =====");
        System.out.println("""
                   slot:   3        4
                         [kiwi] [apple]       apple's home is 3, but 3 was taken -> stored at 4
                   delete kiwi by EMPTYING slot 3:  [ ]  [apple]  -> get(apple) starts at 3, sees EMPTY,
                                                                     stops: "not found"  (WRONG)
                   delete kiwi with a TOMBSTONE:    [x]  [apple]  -> get(apple) probes past x -> found""");

        System.out.println("\n===== AVERAGE PROBES PER LOOKUP vs LOAD FACTOR (no resizing) =====");
        for (double load : new double[]{0.25, 0.5, 0.7, 0.9, 0.97}) {
            int cap = 1 << 16;
            HashTableOpenAddressing t = new HashTableOpenAddressing(cap);
            Random rnd = new Random(7);
            int n = (int) (cap * load);
            int[] inserted = new int[n];
            for (int i = 0; i < n; i++) {
                inserted[i] = rnd.nextInt();
                t.insertNoResize(inserted[i]);
            }
            t.probes = 0;
            for (int i = 0; i < n; i++) t.get(inserted[i]);
            System.out.printf("   load %.2f -> %.2f probes per successful lookup%n", load, (double) t.probes / n);
        }
        System.out.println("   -> it explodes near full: that's why tables resize long before they fill up");

        // self-check vs HashMap (with resizing and deletions)
        Random rnd = new Random(3);
        HashTableOpenAddressing mine = new HashTableOpenAddressing(8);
        Map<Integer, Integer> ref = new HashMap<>();
        boolean ok = true;
        for (int i = 0; i < 200_000; i++) {
            int k = rnd.nextInt(3000), v = rnd.nextInt();
            switch (rnd.nextInt(3)) {
                case 0 -> { mine.put(k, v); ref.put(k, v); }
                case 1 -> ok &= Objects.equals(mine.get(k), ref.get(k));
                default -> ok &= mine.remove(k) == (ref.remove(k) != null);
            }
            ok &= mine.size() == ref.size();
        }
        System.out.println("\n===== 200,000 random put/get/remove vs java.util.HashMap: " + (ok ? "all match" : "MISMATCH") + " =====");
        if (!ok) System.exit(1);
    }

    // used only by the load-factor experiment: insert without triggering a resize
    void insertNoResize(int key) {
        int i = home(key);
        while (state[i] == OCCUPIED) {
            if (keys[i] == key) return;
            i = (i + 1) & (keys.length - 1);
        }
        keys[i] = key;
        state[i] = OCCUPIED;
        size++;
    }
}
