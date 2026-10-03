/*
 * ============================================================================
 *  TOPIC 9 : FORK/JOIN FRAMEWORK
 * ============================================================================
 *
 *  Run:  java ForkJoinDemo.java
 *
 *  IDEA: DIVIDE AND CONQUER in parallel.
 *      solve(problem):
 *          if problem is small enough -> solve it directly (sequentially)
 *          else:
 *              split into subproblems
 *              FORK the subtasks (run them asynchronously)
 *              JOIN their results and combine
 *
 *  CLASSES
 *    ForkJoinPool          : a special executor; one worker per core by default
 *    RecursiveTask<V>      : a task that RETURNS a result     (compute() returns V)
 *    RecursiveAction       : a task with NO result            (compute() is void)
 *    ForkJoinPool.commonPool() : a shared pool. Parallel streams and
 *                                CompletableFuture.*Async use it too.
 *
 *  WORK STEALING (why it's fast)
 *    Each worker has its OWN double-ended queue (deque).
 *      - A worker pushes/pops its own tasks at the HEAD (LIFO, cache-friendly).
 *      - An IDLE worker STEALS from the TAIL of a busy worker's deque (FIFO).
 *        Tail tasks are usually the BIGGEST chunks, so one steal brings a lot of work.
 *    This balances load automatically with little contention.
 *
 *  THE IDIOM
 *      left.fork();                    // run the left half asynchronously
 *      long r = right.compute();       // compute the right half IN THIS THREAD
 *      long l = left.join();           // then wait for the left half
 *      return l + r;
 *  (Calling fork() on both and then join() on both wastes the current thread.
 *   invokeAll(left, right) is also fine.)
 *
 *  GUIDELINES
 *    - Pick a sensible THRESHOLD: too small = overhead of millions of tiny
 *      tasks, too large = poor parallelism. Aim for a few thousand
 *      operations per leaf, with total tasks ~ 10-100x the number of cores.
 *    - Tasks should be CPU-bound and INDEPENDENT. No blocking I/O, no locks.
 *    - Exceptions thrown in compute() are rethrown on join().
 *    - For most use cases, parallel streams give you fork/join for free.
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.LongStream;

public class ForkJoinDemo {

    public static void main(String[] args) {
        long[] data = LongStream.rangeClosed(1, 20_000_000).toArray();

        sumWithRecursiveTask(data);
        parallelMergeSort();
        recursiveActionDemo();
        compareWithParallelStream(data);
    }

    // =================================================================
    // 1. RecursiveTask: parallel sum of an array
    // =================================================================
    static class SumTask extends RecursiveTask<Long> {
        static final int THRESHOLD = 10_000;
        private final long[] arr;
        private final int lo, hi;                 // [lo, hi)

        SumTask(long[] arr, int lo, int hi) { this.arr = arr; this.lo = lo; this.hi = hi; }

        @Override
        protected Long compute() {
            if (hi - lo <= THRESHOLD) {           // BASE CASE: small enough, do it sequentially
                long sum = 0;
                for (int i = lo; i < hi; i++) sum += arr[i];
                return sum;
            }
            int mid = lo + (hi - lo) / 2;
            SumTask left = new SumTask(arr, lo, mid);
            SumTask right = new SumTask(arr, mid, hi);
            left.fork();                          // push left onto this worker's deque (may be stolen)
            long rightResult = right.compute();   // do the right half ourselves
            long leftResult = left.join();        // wait for (or help finish) the left half
            return leftResult + rightResult;
        }
    }

    static void sumWithRecursiveTask(long[] data) {
        System.out.println("===== 1. RecursiveTask: PARALLEL SUM =====");
        long start = System.nanoTime();
        long seq = 0;
        for (long x : data) seq += x;
        long seqMs = (System.nanoTime() - start) / 1_000_000;

        ForkJoinPool pool = ForkJoinPool.commonPool();
        start = System.nanoTime();
        long par = pool.invoke(new SumTask(data, 0, data.length));
        long parMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("   sequential = " + seq + " in " + seqMs + " ms");
        System.out.println("   fork/join  = " + par + " in " + parMs + " ms (parallelism " + pool.getParallelism() + ")");
        System.out.println("   steals so far in common pool = " + pool.getStealCount());
        // Don't be surprised if fork/join is SLOWER here: a plain sum does almost no
        // work per element (memory-bandwidth bound), the JIT vectorizes the simple
        // loop, and the first invoke pays to start the pool's threads. Parallelism
        // pays off when each element needs real CPU work, like the sort below.
        // LESSON: always MEASURE, never assume "parallel = faster".
    }

    // =================================================================
    // 2. Parallel merge sort: the textbook fork/join use case
    // =================================================================
    static class MergeSortTask extends RecursiveAction {
        static final int THRESHOLD = 8_192;
        private final int[] arr, tmp;
        private final int lo, hi;

        MergeSortTask(int[] arr, int[] tmp, int lo, int hi) { this.arr = arr; this.tmp = tmp; this.lo = lo; this.hi = hi; }

        @Override
        protected void compute() {
            if (hi - lo <= THRESHOLD) {
                Arrays.sort(arr, lo, hi);                         // small: sequential sort
                return;
            }
            int mid = lo + (hi - lo) / 2;
            invokeAll(new MergeSortTask(arr, tmp, lo, mid),       // fork both, wait for both
                      new MergeSortTask(arr, tmp, mid, hi));
            merge(mid);
        }

        private void merge(int mid) {
            System.arraycopy(arr, lo, tmp, lo, hi - lo);
            int i = lo, j = mid, k = lo;
            while (i < mid && j < hi) arr[k++] = tmp[i] <= tmp[j] ? tmp[i++] : tmp[j++];
            while (i < mid) arr[k++] = tmp[i++];
            while (j < hi) arr[k++] = tmp[j++];
        }
    }

    static void parallelMergeSort() {
        System.out.println("\n===== 2. RecursiveAction: PARALLEL MERGE SORT =====");
        int n = 4_000_000;
        int[] a = new Random(42).ints(n).toArray();
        int[] b = a.clone();

        long start = System.nanoTime();
        Arrays.sort(b);
        long seqMs = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        ForkJoinPool.commonPool().invoke(new MergeSortTask(a, new int[n], 0, n));
        long parMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("   Arrays.sort (sequential) : " + seqMs + " ms");
        System.out.println("   fork/join merge sort     : " + parMs + " ms, correct = " + Arrays.equals(a, b));
        // The JDK has this built in: Arrays.parallelSort(array)
    }

    // =================================================================
    // 3. RecursiveAction (no result): transform an array in place,
    //    with a custom pool size
    // =================================================================
    static class SquareRootAction extends RecursiveAction {
        private final double[] arr;
        private final int lo, hi;
        SquareRootAction(double[] arr, int lo, int hi) { this.arr = arr; this.lo = lo; this.hi = hi; }

        @Override
        protected void compute() {
            if (hi - lo <= 50_000) {
                for (int i = lo; i < hi; i++) arr[i] = Math.sqrt(Math.sin(arr[i]) * Math.sin(arr[i]) + 1);  // CPU work
                return;
            }
            int mid = (lo + hi) >>> 1;
            invokeAll(new SquareRootAction(arr, lo, mid), new SquareRootAction(arr, mid, hi));
        }
    }

    static void recursiveActionDemo() {
        System.out.println("\n===== 3. RecursiveAction WITH A CUSTOM POOL =====");
        double[] values = new double[5_000_000];
        Arrays.fill(values, 2.0);
        try (ForkJoinPool custom = new ForkJoinPool(4)) {             // isolate from the common pool
            long start = System.nanoTime();
            custom.invoke(new SquareRootAction(values, 0, values.length));
            System.out.printf("   transformed %,d values on 4 workers in %d ms, sample=%.4f%n",
                    values.length, (System.nanoTime() - start) / 1_000_000, values[123]);
        }
    }

    // =================================================================
    // 4. Parallel streams use the same machinery under the hood
    // =================================================================
    static void compareWithParallelStream(long[] data) {
        System.out.println("\n===== 4. PARALLEL STREAM = FORK/JOIN FOR FREE =====");
        long start = System.nanoTime();
        long sum = Arrays.stream(data).parallel().sum();
        System.out.println("   parallel stream sum = " + sum + " in " + (System.nanoTime() - start) / 1_000_000 + " ms");
        System.out.println("   (runs on ForkJoinPool.commonPool, split by the stream's Spliterator)");
    }
}
