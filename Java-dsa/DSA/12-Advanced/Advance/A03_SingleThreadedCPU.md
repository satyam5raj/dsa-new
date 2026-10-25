# A03 · Single-Threaded CPU ⭐

| | |
|---|---|
| **LeetCode** | [#1834 Single-Threaded CPU](https://leetcode.com/problems/single-threaded-cpu/) |
| **Difficulty** | Medium |
| **Mix of structures** | **Sorting** (by arrival time) + **min-heap** (by processing time, then index) + a moving clock |
| **Code** | [A03_SingleThreadedCPU.java](A03_SingleThreadedCPU.java) |
| **Run** | `java A03_SingleThreadedCPU.java` |

⭐ = not in `leetcode.md`, added because it needs two data structures working together.

---

## 1. The problem

`tasks[i] = [enqueueTime, processingTime]`. There is one CPU:
- when it's idle, it picks, **among the tasks that have already arrived**, the one with the **shortest processing time**; ties go to the **smallest index**;
- it runs that task to the end (no interruptions);
- if no task has arrived yet, it waits.

Return the order in which tasks are processed.

```
tasks = [[1,2],[2,4],[3,2],[4,1]]   →   [0, 2, 3, 1]
tasks = [[7,10],[7,12],[7,5],[7,4],[7,2]]   →   [4, 3, 2, 0, 1]
```

---

## 2. How to think about it

### Two questions at every decision
Every time the CPU becomes free, it needs answers to:

| Question | Structure |
|---|---|
| 1. "Which tasks have **arrived** by now?" | the tasks **sorted by enqueue time** + a pointer that moves forward while `enqueueTime <= time` |
| 2. "Among the arrived ones, which is **shortest**?" | a **min-heap** ordered by `(processingTime, index)` |

Sorting alone can't answer question 2 (the arrived set keeps changing), and a heap alone can't answer question 1 (tasks that haven't arrived mustn't be picked). Together they make a **simulation**:

```
time = 0
repeat until every task is done:
    if the heap is empty: time = max(time, next task's arrival)        // idle: jump ahead
    push every task with enqueueTime <= time into the heap
    pop the best task, record it, time += its processing time
```

### Dry run: `[[1,2],[2,4],[3,2],[4,1]]`
Sorted by arrival: task 0 (t=1), 1 (t=2), 2 (t=3), 3 (t=4).

| time | heap empty? | pushed | heap (proc, idx) | run | new time |
|---|---|---|---|---|---|
| 0 | yes → jump to 1 | task 0 | (2,0) | **0** | 3 |
| 3 | | tasks 1, 2 | (2,2) (4,1) | **2** | 5 |
| 5 | | task 3 | (1,3) (4,1) | **3** | 6 |
| 6 | | – | (4,1) | **1** | 10 |

Order **[0, 2, 3, 1]** ✓

### Why `long` for the clock
Up to 10⁵ tasks × 10⁹ processing time each is 10¹⁴, far beyond `int` (about 2.1 × 10⁹).

---

## 3. Java concepts used in this problem

### 3.1 Sorting **indices** with a comparator: `Integer[]`, not `int[]`
```java
Integer[] byArrival = new Integer[n];
for (int i = 0; i < n; i++) byArrival[i] = i;
Arrays.sort(byArrival, Comparator.comparingInt(i -> tasks[i][0]));
```
We must remember each task's **original index** (it's the answer), so we sort the indices `0..n-1` by their task's arrival time instead of sorting the tasks themselves. `Arrays.sort(int[], comparator)` **doesn't exist**: comparators only work on object arrays, hence `Integer[]`.

`Comparator.comparingInt(f)` builds a comparator that compares `f(a)` with `f(b)`.

### 3.2 A heap with a two-level tie-break
```java
PriorityQueue<Integer> ready = new PriorityQueue<>((a, b) ->
        tasks[a][1] != tasks[b][1] ? Integer.compare(tasks[a][1], tasks[b][1]) : Integer.compare(a, b));
```
The heap holds **indices**. The comparator compares processing times first and, only if they're equal, the indices. The lambda reads `tasks` from the surrounding method (it "captures" it), which works because `tasks` is never reassigned.

### 3.3 `long time` mixed with `int`s
`time < tasks[...][0]` compares a `long` with an `int`: Java widens the `int` to `long` automatically. `time += tasks[t][1]` also stays a `long`.

---

## 4. Approach 1: Rescan every task at every step (the referee)

### Code ([A03_SingleThreadedCPU.java:40-61](A03_SingleThreadedCPU.java#L40-L61))
```java
static int[] getOrderBrute(int[][] tasks) {
    int n = tasks.length;
    boolean[] done = new boolean[n];
    int[] order = new int[n];
    long time = 0;
    for (int k = 0; k < n; k++) {
        int best = -1;
        for (int i = 0; i < n; i++)
            if (!done[i] && tasks[i][0] <= time && (best == -1 || tasks[i][1] < tasks[best][1])) best = i;
        if (best == -1) {                                                // idle: jump to the earliest arrival
            long next = Long.MAX_VALUE;
            for (int i = 0; i < n; i++) if (!done[i]) next = Math.min(next, tasks[i][0]);
            time = next;
            k--;                                                         // retry this slot at the new time
            continue;
        }
        done[best] = true;
        order[k] = best;
        time += tasks[best][1];
    }
    return order;
}
```

| Line | What it does |
|---|---|
| the inner `for` | among unfinished tasks that have arrived, the shortest. Scanning in index order with a strict `<` keeps the smallest index on ties |
| `best == -1` | nothing has arrived: jump to the earliest unfinished arrival |
| `k--; continue` | this slot is still unfilled, so try again at the new time |
| the last three lines | run the task and advance the clock |

**Cost: O(n²)**: fine as a referee, too slow for 10⁵ tasks.

---

## 5. Approach 2: Sort + min-heap (main solution)

### Code ([A03_SingleThreadedCPU.java:64-84](A03_SingleThreadedCPU.java#L64-L84))
```java
static int[] getOrder(int[][] tasks) {
    int n = tasks.length;
    Integer[] byArrival = new Integer[n];
    for (int i = 0; i < n; i++) byArrival[i] = i;
    Arrays.sort(byArrival, Comparator.comparingInt(i -> tasks[i][0]));

    PriorityQueue<Integer> ready = new PriorityQueue<>((a, b) ->
            tasks[a][1] != tasks[b][1] ? Integer.compare(tasks[a][1], tasks[b][1]) : Integer.compare(a, b));

    int[] order = new int[n];
    int k = 0, next = 0;
    long time = 0;
    while (k < n) {
        if (ready.isEmpty() && time < tasks[byArrival[next]][0]) time = tasks[byArrival[next]][0];   // idle: jump
        while (next < n && tasks[byArrival[next]][0] <= time) ready.add(byArrival[next++]);
        int t = ready.poll();
        order[k++] = t;
        time += tasks[t][1];
    }
    return order;
}
```

### Line by line
| Line | What it does |
|---|---|
| `byArrival` sorted | indices in arrival order: question 1 |
| `ready` | indices of arrived tasks, shortest first: question 2 |
| `k` | how many tasks are done; `next` = the first task not yet pushed |
| the idle jump | nothing is ready and the next task is in the future, so the CPU sits idle until it arrives. `next < n` is guaranteed here: if the heap is empty and tasks remain, some haven't been pushed yet |
| the inner `while` | push everything that has arrived by `time` |
| `poll` | the shortest (smallest index on ties) |
| `time += ...` | the CPU is busy until then |

### Complexity
- Sorting: O(n log n). Each task is pushed and popped once: O(n log n).
- **Total O(n log n)**, space O(n).

---

## 6. Comparing the approaches

| | Time | Space |
|---|---|---|
| Rescan | O(n²) | O(n) |
| **Sort + heap** | **O(n log n)** | O(n) |

The file checks the heap version against the rescan on 2,000 random task sets, plus an idle gap and a case with huge processing times (where the clock must be a `long`).

---

## 7. Edge cases and common mistakes

**Edge cases:** all tasks arrive together (pure shortest-job-first), a gap where the CPU is idle, equal processing times (smallest index wins), huge times.

**Mistakes:**
1. `int time`: overflow on big inputs.
2. Forgetting the idle jump, so the code polls an empty heap and gets `null` → `NullPointerException`.
3. Jumping the clock to the next arrival **even when the heap isn't empty** (the CPU should run what's ready).
4. Sorting the `tasks` array itself and losing the original indices.
5. Breaking ties by enqueue time instead of index.

---

## 8. Interview questions and answers

**Q1. Why do you need both sorting and a heap?**
> Sorting by arrival lets a pointer release tasks exactly when they arrive; the heap picks the shortest among the released ones. The released set keeps changing, which is what heaps are for, while the arrival order never changes, so one sort is enough.

**Q2. What happens when the CPU is idle?**
> If the heap is empty, the clock jumps straight to the next arrival instead of ticking one unit at a time; otherwise a gap of 10⁹ would take 10⁹ steps.

**Q3. Where does the tie-break go?**
> Into the heap's comparator: processing time first, then index.

### Follow-up questions

**F1. "Two CPUs."**
> Add a second heap of busy CPUs keyed by when they become free, exactly like [A04 Process Tasks Using Servers](A04_ProcessTasksUsingServers.md).

**F2. "Pre-emption: a shorter task interrupts the current one."**
> That's shortest-remaining-time-first. Process events at every arrival, and keep remaining times in the heap.

**F3. "This pattern elsewhere?"**
> Meeting Rooms II, IPO, Course Schedule III: sort by one key, then use a heap on another key as you sweep.

---

## 9. Running the file

```bash
java A03_SingleThreadedCPU.java     # 5/5 tests passed (examples, idle gap, huge times, 2,000 random sets vs rescanning)
```

**One-line summary:** *"Sort the task indices by arrival, advance a clock, push every arrived task into a min-heap keyed by (processing time, index), jump the clock forward when the heap is empty, and repeatedly run the heap's top."*
