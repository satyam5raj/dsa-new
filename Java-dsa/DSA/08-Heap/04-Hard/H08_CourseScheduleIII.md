# H08 · Course Schedule III

| | |
|---|---|
| **LeetCode** | [#630 Course Schedule III](https://leetcode.com/problems/course-schedule-iii/) |
| **Difficulty** | Hard |
| **Pattern** | Sort by deadline + a max-heap of durations ("swap out the longest") |
| **Code** | [H08_CourseScheduleIII.java](H08_CourseScheduleIII.java) |
| **Run** | `java H08_CourseScheduleIII.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem). A similar "regret" greedy: [M05 Furthest Building](../03-Medium/M05_FurthestBuildingYouCanReach.md).

---

## 1. The problem

Course i takes `duration[i]` days and must **finish by** `lastDay[i]`. You take the courses one at a time, starting on day 1. What's the maximum number of courses you can take?

| courses [duration, lastDay] | answer |
|---|---|
| `[[100,200],[200,1300],[1000,1250],[2000,3200]]` | 3 |
| `[[1,2]]` | 1 |
| `[[3,2],[4,3]]` | 0 (none fit) |
| `[[5,5],[4,6],[2,6]]` | 2 (swap out the 5) |

---

## 2. How to think about it

### Step 1: the best order for a fixed set is earliest deadline first
If you take a set of courses, scheduling them by **earliest deadline** is optimal. Exchange argument: swapping two adjacent courses that are out of deadline order never makes things worse. So sort by lastDay and decide course by course.

### Step 2: the greedy
Take every course, tracking `time` = the total duration of the courses taken. If `time > lastDay` of the current course, we must **drop** one course. Drop the **longest** one taken so far (possibly the current one): the count drops by the same 1 either way, but this frees the most time for the future. A **max-heap of durations** finds it.

```
for (d, last) sorted by last:
    heap.offer(d);  time += d
    if time > last:  time −= heap.poll()     // drop the longest
answer = heap.size()
```

**Why dropping one course is always enough:** before adding the current course, the schedule was feasible. The longest duration is ≥ the current one, so after dropping it, time ≤ the previous time ≤ the previous deadline ≤ the current deadline.

### Dry run: sorted by deadline [100,200] [1000,1250] [200,1300] [2000,3200]

| course | time | ≤ deadline? | heap |
|---|---|---|---|
| +100 | 100 | ≤ 200 ✓ | {100} |
| +1000 | 1100 | ≤ 1250 ✓ | {1000, 100} |
| +200 | 1300 | ≤ 1300 ✓ | {1000, 200, 100} |
| +2000 | 3300 | > 3200 → drop 2000 → 1300 | {1000, 200, 100} |

→ **3 courses**

---

## 3. Java concepts used in this problem

### 3.1 Sorting `int[][]` by the second column

```java
Arrays.sort(c, Comparator.comparingInt(x -> x[1]));       // earliest deadline first
```

`Arrays.sort` on objects is **stable**, so courses with equal deadlines keep their input order (it doesn't affect the answer here).

### 3.2 A max-heap of durations

`new PriorityQueue<>(Comparator.reverseOrder())`: the longest course taken so far is always at the root, ready to be dropped.

### 3.3 The answer is the heap's size

The heap contains exactly the courses currently taken, so no separate counter is needed.

---

## 4. Approach 1: Every subset, checked in deadline order (the referee)

### Code ([H08_CourseScheduleIII.java:58-74](H08_CourseScheduleIII.java#L58-L74))

```java
static int brute(int[][] courses) {
    int n = courses.length, best = 0;
    int[][] c = courses.clone();
    Arrays.sort(c, Comparator.comparingInt(x -> x[1]));
    for (int mask = 0; mask < (1 << n); mask++) {
        int time = 0, count = 0;
        boolean ok = true;
        for (int i = 0; i < n && ok; i++) {
            if ((mask >> i & 1) == 0) continue;
            time += c[i][0];
            count++;
            if (time > c[i][1]) ok = false;
        }
        if (ok) best = Math.max(best, count);
    }
    return best;
}
```

| Line | What it does |
|---|---|
| sort by deadline | the optimal order for any subset (step 1) |
| the mask loop | every subset |
| `time > c[i][1]` | a course finishes after its deadline → infeasible |

**O(2ⁿ · n).** Relies on step 1, but nothing else.

---

## 5. Approach 2: Sort by deadline + max-heap of durations (the file's solution)

### Code ([H08_CourseScheduleIII.java:44-55](H08_CourseScheduleIII.java#L44-L55))

```java
static int scheduleCourse(int[][] courses) {
    int[][] c = courses.clone();
    Arrays.sort(c, Comparator.comparingInt(x -> x[1]));                 // earliest deadline first
    PriorityQueue<Integer> taken = new PriorityQueue<>(Comparator.reverseOrder());   // durations, max-heap
    int time = 0;
    for (int[] course : c) {
        taken.offer(course[0]);
        time += course[0];
        if (time > course[1]) time -= taken.poll();                     // drop the longest course
    }
    return taken.size();
}
```

### Line by line

| Line | What it does |
|---|---|
| sort by lastDay | decide the courses in deadline order |
| `taken.offer(course[0]); time += course[0];` | tentatively take this course |
| `time > course[1]` | the schedule now misses this deadline |
| `time -= taken.poll()` | drop the longest course taken (it could be this one) |
| `taken.size()` | the number of courses kept |

### Dry run: `[[5,5],[4,6],[2,6]]`

| course | time after adding | deadline | action | heap |
|---|---|---|---|---|
| [5, 5] | 5 | 5 | ok | {5} |
| [4, 6] | 9 | 6 | 9 > 6 → drop 5 → time 4 | {4} |
| [2, 6] | 6 | 6 | ok | {4, 2} |

→ **2** ✓ (dropping the 5 made room for both shorter courses).

### Dry run: `[[3,2],[4,3]]`
[3, 2]: time 3 > 2 → drop 3 → time 0, heap {}. [4, 3]: time 4 > 3 → drop 4 → heap {} → **0** ✓

### Complexity
- **Time O(n log n).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Every subset | Sort + max-heap |
|---|---|---|
| Time | O(2ⁿ · n) | **O(n log n)** |
| Space | O(1) | O(n) |

The file checks the greedy against every subset on 1,000 random cases.

---

## 7. Edge cases and common mistakes

**Edge cases:** a course longer than its own deadline (never taken), equal deadlines, all courses fit, none fit.

**Mistakes:**
1. Sorting by duration instead of deadline.
2. Skipping a course that doesn't fit, instead of swapping it for a longer one.
3. Dropping the **shortest** course (that frees the least time).
4. Using `>=` instead of `>` (finishing exactly on the last day is allowed).

---

## 8. Interview questions and answers

**Q1. Why is earliest-deadline-first the right order?**
> For a fixed set, if two adjacent courses are out of deadline order, swapping them makes the later-deadline course finish when the earlier one used to, which is still within its (later) deadline. So the sorted order is always feasible when any order is.

**Q2. Why drop the longest course rather than skip the current one?**
> Either way, the count is the same. Dropping the longest leaves the smallest total time, which can only help the courses that come later.

### Follow-up questions

**F1. Maximise the total duration instead of the count.**
> It becomes a knapsack-like DP over the deadline-sorted courses.

**F2. Courses have prerequisites (Course Schedule I/II).**
> A different problem: a topological sort on a graph ([09-Graph](../../09-Graph/README.md)).

**F3. Other "regret" greedies.**
> Furthest Building ([M05](../03-Medium/M05_FurthestBuildingYouCanReach.md)), and the job scheduling problem with deadlines and profits (sort by deadline, a min-heap of profits).

---

## 9. Running the file

```bash
java H08_CourseScheduleIII.java      # 5/5 tests passed (including 1,000 random cases vs all subsets)
```

**One-line summary:** *"Sort by deadline, take every course, and whenever the time passes the current deadline, drop the longest course taken so far (a max-heap); the heap size is the answer."*
