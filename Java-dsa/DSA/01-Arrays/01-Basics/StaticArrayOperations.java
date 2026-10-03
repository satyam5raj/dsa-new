/*
 * ============================================================================
 *  ARRAYS 1.1 : STATIC ARRAY - WHAT IT IS AND EVERY BASIC OPERATION
 * ============================================================================
 *
 *  Run:  java StaticArrayOperations.java
 *
 *  WHAT IS AN ARRAY?
 *  -----------------
 *  A FIXED-SIZE block of CONTIGUOUS (side-by-side) memory holding elements of
 *  ONE type.
 *
 *     index:     0     1     2     3     4
 *             +-----+-----+-----+-----+-----+
 *     value:  | 10  | 20  | 30  | 40  | 50  |
 *             +-----+-----+-----+-----+-----+
 *     address: 1000  1004  1008  1012  1016      (int = 4 bytes)
 *
 *  WHY IS ACCESS O(1)?
 *     address of arr[i] = base address + i * (size of one element)
 *     arr[3] = 1000 + 3*4 = 1012 -> ONE calculation, no matter how big the array is.
 *  That's the superpower of arrays: RANDOM ACCESS in constant time.
 *  Contiguous memory is also CPU-CACHE friendly, so loops over arrays are very fast.
 *
 *  THE WEAKNESS: the size is fixed, and inserting/deleting in the MIDDLE means
 *  SHIFTING all the elements after that position -> O(n).
 *
 *  A "static array" here = a Java array with a fixed capacity, plus a 'size'
 *  counter saying how many slots are actually used. This is exactly how
 *  ArrayList works inside (see DynamicArray.java).
 *
 *  OPERATION COSTS
 *  ------------------------------------------------------------
 *   access / update by index      O(1)
 *   insert / delete at the END    O(1)
 *   insert / delete at index i    O(n)   (shift elements)
 *   linear search (unsorted)      O(n)
 *   binary search (sorted)        O(log n)
 *   reverse / rotate              O(n), can be done in-place with O(1) space
 *  ------------------------------------------------------------
 */

import java.util.Arrays;

public class StaticArrayOperations {

    // Our "static array": fixed capacity + count of used slots
    static int[] data = new int[10];
    static int size = 0;

    public static void main(String[] args) {
        traversal();
        insertOperations();
        deleteOperations();
        searchOperations();
        reverseAndRotate();
        copyingAndComparing();
        twoDimensional();
    }

    // =================================================================
    // 1. Traversal: visit every element once -> O(n)
    // =================================================================
    static void traversal() {
        System.out.println("===== 1. TRAVERSAL =====");
        int[] arr = {5, 3, 8, 1, 9, 2};

        // forward with an index (when you need i)
        int sum = 0;
        for (int i = 0; i < arr.length; i++) sum += arr[i];

        // for-each (when you only need values)
        int max = Integer.MIN_VALUE;
        for (int x : arr) max = Math.max(max, x);

        // backward
        StringBuilder rev = new StringBuilder();
        for (int i = arr.length - 1; i >= 0; i--) rev.append(arr[i]).append(' ');

        // every second element (step 2)
        StringBuilder evens = new StringBuilder();
        for (int i = 0; i < arr.length; i += 2) evens.append(arr[i]).append(' ');

        System.out.println("   sum=" + sum + " max=" + max + " backward=[" + rev.toString().trim() + "] evenIdx=[" + evens.toString().trim() + "]");
    }

    // =================================================================
    // 2. Insert
    // =================================================================

    // At the END: O(1) - just write into the next free slot
    static void insertAtEnd(int value) {
        if (size == data.length) throw new IllegalStateException("array is full (static arrays can't grow)");
        data[size++] = value;
    }

    // At INDEX: O(n) - shift everything from index to the right by one, from the BACK
    //   insert 99 at index 1 in [10,20,30]:
    //     [10,20,30,_] -> shift 30 -> [10,20,30,30] -> shift 20 -> [10,20,20,30] -> write -> [10,99,20,30]
    //   Why from the back? Shifting from the front would overwrite values before moving them.
    static void insertAt(int index, int value) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException("index " + index);
        if (size == data.length) throw new IllegalStateException("array is full");
        for (int i = size; i > index; i--) {
            data[i] = data[i - 1];
        }
        data[index] = value;
        size++;
    }

    static void insertOperations() {
        System.out.println("\n===== 2. INSERT =====");
        insertAtEnd(10);
        insertAtEnd(20);
        insertAtEnd(30);
        System.out.println("   after insertAtEnd 10,20,30 : " + show());
        insertAt(1, 99);
        System.out.println("   after insertAt(1, 99)       : " + show() + "  <- 20,30 shifted right");
        insertAt(0, 5);
        System.out.println("   after insertAt(0, 5)        : " + show() + "  <- worst case: shift EVERYTHING");
    }

    // =================================================================
    // 3. Delete
    // =================================================================

    // From the END: O(1)
    static int deleteLast() {
        if (size == 0) throw new IllegalStateException("empty");
        return data[--size];               // the slot is simply "forgotten" (size shrinks)
    }

    // At INDEX: O(n) - shift everything after it to the LEFT, from the FRONT
    static int deleteAt(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index " + index);
        int removed = data[index];
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
        }
        size--;
        return removed;
    }

    static void deleteOperations() {
        System.out.println("\n===== 3. DELETE =====");
        System.out.println("   deleteLast() removed " + deleteLast() + " -> " + show());
        System.out.println("   deleteAt(1)  removed " + deleteAt(1) + " -> " + show() + "  <- elements shifted left");
    }

    // =================================================================
    // 4. Search
    // =================================================================

    // LINEAR search: check one by one. Works on ANY array. O(n)
    static int linearSearch(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) return i;
        }
        return -1;                                   // convention: -1 means "not found"
    }

    // BINARY search: only on a SORTED array. Compare with the middle, discard half. O(log n)
    static int binarySearch(int[] sorted, int target) {
        int lo = 0, hi = sorted.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (sorted[mid] == target) return mid;
            if (sorted[mid] < target) lo = mid + 1;  // target is in the right half
            else hi = mid - 1;                       // target is in the left half
        }
        return -1;
    }

    static void searchOperations() {
        System.out.println("\n===== 4. SEARCH =====");
        int[] arr = {7, 2, 9, 4, 1};
        int[] sorted = {1, 2, 4, 7, 9};
        System.out.println("   linearSearch(9)  = index " + linearSearch(arr, 9));
        System.out.println("   linearSearch(5)  = " + linearSearch(arr, 5) + " (not found)");
        System.out.println("   binarySearch(7)  = index " + binarySearch(sorted, 7));
    }

    // =================================================================
    // 5. Reverse & rotate IN-PLACE (O(1) extra space)
    // =================================================================

    // Two pointers: swap the ends and walk inward
    static void reverse(int[] arr, int from, int to) {        // inclusive range
        while (from < to) {
            int tmp = arr[from];
            arr[from] = arr[to];
            arr[to] = tmp;
            from++;
            to--;
        }
    }

    // Rotate RIGHT by k with the "triple reverse" trick:
    //   [1,2,3,4,5,6,7], k=3
    //   1. reverse all          -> [7,6,5,4,3,2,1]
    //   2. reverse first k      -> [5,6,7,4,3,2,1]
    //   3. reverse the rest     -> [5,6,7,1,2,3,4]   done!
    static void rotateRight(int[] arr, int k) {
        int n = arr.length;
        k %= n;                                   // rotating by n is the same as rotating by 0
        reverse(arr, 0, n - 1);
        reverse(arr, 0, k - 1);
        reverse(arr, k, n - 1);
    }

    static void reverseAndRotate() {
        System.out.println("\n===== 5. REVERSE & ROTATE (in-place) =====");
        int[] a = {1, 2, 3, 4, 5};
        reverse(a, 0, a.length - 1);
        System.out.println("   reversed        : " + Arrays.toString(a));
        int[] b = {1, 2, 3, 4, 5, 6, 7};
        rotateRight(b, 3);
        System.out.println("   rotateRight(3)  : " + Arrays.toString(b));
    }

    // =================================================================
    // 6. Copying & comparing (common bugs live here)
    // =================================================================
    static void copyingAndComparing() {
        System.out.println("\n===== 6. COPYING & COMPARING =====");
        int[] a = {1, 2, 3};
        int[] alias = a;                      // NOT a copy: both names point to the SAME array
        int[] copy = a.clone();               // a real copy (also: Arrays.copyOf(a, a.length))
        alias[0] = 100;
        System.out.println("   a after alias[0]=100 : " + Arrays.toString(a) + "  (alias changed a!)");
        System.out.println("   copy is unaffected   : " + Arrays.toString(copy));
        System.out.println("   a == copy ? " + (a == copy) + "   Arrays.equals(...) ? " + Arrays.equals(new int[]{1, 2}, new int[]{1, 2}));
        int[] bigger = Arrays.copyOf(copy, 5);           // grow: extra slots are 0
        System.out.println("   Arrays.copyOf(copy,5): " + Arrays.toString(bigger));
    }

    // =================================================================
    // 7. 2D arrays (matrices): an array of arrays
    //    Row-major traversal (row by row) is cache-friendly.
    // =================================================================
    static void twoDimensional() {
        System.out.println("\n===== 7. 2D ARRAYS =====");
        int[][] m = {
                {1, 2, 3},
                {4, 5, 6}
        };
        int rows = m.length, cols = m[0].length;
        int total = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) total += m[r][c];
        }
        System.out.println("   " + rows + "x" + cols + " matrix, sum = " + total + ", m[1][2] = " + m[1][2]);

        // Flattening: a 2D cell (r, c) <-> 1D index r * cols + c  (used in many grid problems)
        int r = 1, c = 2;
        int flat = r * cols + c;
        System.out.println("   (1,2) -> flat index " + flat + " -> back to (" + flat / cols + "," + flat % cols + ")");
    }

    static String show() {
        return Arrays.toString(Arrays.copyOf(data, size)) + " size=" + size + " capacity=" + data.length;
    }
}
