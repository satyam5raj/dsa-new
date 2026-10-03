/*
 * ============================================================================
 *  ARRAYS 1.2 : DYNAMIC ARRAY - BUILD YOUR OWN ArrayList FROM SCRATCH
 * ============================================================================
 *
 *  Run:  java DynamicArray.java
 *
 *  PROBLEM: a Java array can't grow. If you created new int[10] and need an
 *  11th slot, you're stuck.
 *
 *  IDEA: keep a bigger "backing" array and a 'size' counter. When it's FULL:
 *     1. allocate a NEW array of DOUBLE the capacity
 *     2. copy all elements across                         (O(n), but RARE)
 *     3. continue adding into the new array
 *
 *     capacity 4: [a][b][c][d]          <- full, add 'e'
 *     capacity 8: [a][b][c][d][e][ ][ ][ ]
 *
 *  WHY DOUBLE (and not +1 or +10)?
 *    Doubling makes the copies rare: over n adds the total copying is
 *    1 + 2 + 4 + ... + n < 2n  ->  O(1) AMORTIZED per add.
 *    Growing by a constant (+10) would copy ~n^2/20 elements in total -> O(n) per add.
 *    (Java's ArrayList grows by 1.5x. Same idea, less wasted memory.)
 *
 *  SHRINKING: if size drops to 1/4 of the capacity, halve the capacity.
 *  (1/4, not 1/2: shrinking at 1/2 could "thrash" add/remove/add/remove right at the
 *   boundary, copying the whole array on every operation.)
 *
 *  This class is generic (<T>) so it works for any type, like ArrayList<T>.
 */

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DynamicArray<T> implements Iterable<T> {

    private static final int DEFAULT_CAPACITY = 4;

    private T[] data;          // the backing array
    private int size;          // how many slots are USED (size <= data.length)
    private int resizeCount;   // just for the demo

    @SuppressWarnings("unchecked")
    public DynamicArray() {
        data = (T[]) new Object[DEFAULT_CAPACITY];   // can't do new T[] in Java (type erasure)
    }

    // ---------------------------------------------------------------
    // READ operations: O(1)
    // ---------------------------------------------------------------
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return data.length; }

    public T get(int index) {
        checkIndex(index);
        return data[index];
    }

    public void set(int index, T value) {
        checkIndex(index);
        data[index] = value;
    }

    // ---------------------------------------------------------------
    // add at the end: O(1) amortized
    // ---------------------------------------------------------------
    public void add(T value) {
        if (size == data.length) resize(data.length * 2);   // grow when full
        data[size++] = value;
    }

    // ---------------------------------------------------------------
    // insert at index: O(n) - shift the tail right
    // ---------------------------------------------------------------
    public void add(int index, T value) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        if (size == data.length) resize(data.length * 2);
        // System.arraycopy is a fast native copy that handles overlapping ranges correctly
        System.arraycopy(data, index, data, index + 1, size - index);
        data[index] = value;
        size++;
    }

    // ---------------------------------------------------------------
    // remove at index: O(n) - shift the tail left
    // ---------------------------------------------------------------
    public T remove(int index) {
        checkIndex(index);
        T removed = data[index];
        int moved = size - index - 1;
        if (moved > 0) System.arraycopy(data, index + 1, data, index, moved);
        data[--size] = null;            // IMPORTANT: clear the slot so the GC can free the object (avoid memory leak)
        if (size > 0 && size == data.length / 4 && data.length > DEFAULT_CAPACITY) {
            resize(data.length / 2);    // shrink when only 1/4 full
        }
        return removed;
    }

    public T removeLast() {
        if (isEmpty()) throw new NoSuchElementException("array is empty");
        return remove(size - 1);
    }

    // ---------------------------------------------------------------
    // search: O(n)
    // ---------------------------------------------------------------
    public int indexOf(T value) {
        for (int i = 0; i < size; i++) {
            if (value == null ? data[i] == null : value.equals(data[i])) return i;   // equals(), not ==
        }
        return -1;
    }

    public boolean contains(T value) { return indexOf(value) != -1; }

    // ---------------------------------------------------------------
    // the heart of a dynamic array
    // ---------------------------------------------------------------
    private void resize(int newCapacity) {
        data = Arrays.copyOf(data, newCapacity);    // new array + copy: O(n)
        resizeCount++;
    }

    private void checkIndex(int index) {
        // index must be < size (NOT < capacity): slots beyond size are not real elements
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    // Iterable lets us write: for (T x : myArray)
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int i = 0;
            public boolean hasNext() { return i < size; }
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return data[i++];
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(data[i]);
        }
        return sb.append("]").toString();
    }

    // =================================================================
    // DEMO
    // =================================================================
    public static void main(String[] args) {
        System.out.println("===== GROWING =====");
        DynamicArray<Integer> arr = new DynamicArray<>();
        for (int i = 1; i <= 9; i++) {
            int before = arr.capacity();
            arr.add(i * 10);
            String note = arr.capacity() != before ? "   <- full! resized " + before + " -> " + arr.capacity() : "";
            System.out.printf("   add(%2d) size=%d capacity=%d%s%n", i * 10, arr.size(), arr.capacity(), note);
        }

        System.out.println("\n===== INSERT / REMOVE / SEARCH =====");
        arr.add(0, 5);
        System.out.println("   add(0, 5)     -> " + arr);
        System.out.println("   remove(3)     -> removed " + arr.remove(3) + " -> " + arr);
        System.out.println("   get(2)        -> " + arr.get(2));
        arr.set(2, 999);
        System.out.println("   set(2, 999)   -> " + arr);
        System.out.println("   indexOf(70)   -> " + arr.indexOf(70) + ", contains(12345) -> " + arr.contains(12345));

        System.out.println("\n===== SHRINKING =====");
        while (arr.size() > 2) {
            int before = arr.capacity();
            arr.removeLast();
            if (arr.capacity() != before) System.out.println("   size=" + arr.size() + " -> shrank " + before + " -> " + arr.capacity());
        }
        System.out.println("   final: " + arr + " capacity=" + arr.capacity());

        System.out.println("\n===== ITERATION & GENERICS =====");
        DynamicArray<String> names = new DynamicArray<>();
        names.add("Ravi");
        names.add("Asha");
        for (String n : names) System.out.println("   for-each: " + n);

        System.out.println("\n===== ERRORS =====");
        try {
            names.get(5);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("   get(5) -> " + e.getMessage() + " (index must be < size, not < capacity)");
        }

        System.out.println("\n===== AMORTIZED COST =====");
        DynamicArray<Integer> big = new DynamicArray<>();
        for (int i = 0; i < 1_000_000; i++) big.add(i);
        System.out.println("   1,000,000 adds needed only " + big.resizeCount + " resizes (4 -> 8 -> ... -> " + big.capacity() + ")");
    }
}
