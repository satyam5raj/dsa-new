# M03 · Task Scheduler

| | |
|---|---|
| **LeetCode** | [#621 Task Scheduler](https://leetcode.com/problems/task-scheduler/) |
| **Difficulty** | Medium |
| **Pattern** | Greedy "most frequent first" (a max-heap + a cooldown queue) → then a formula |
| **Code** | [M03_TaskScheduler.java](M03_TaskScheduler.java) |
| **Run** | `java M03_TaskScheduler.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Tasks are letters A–Z, and each takes 1 unit of time. Two **identical** tasks must be at least **n** units apart (idling is allowed). What's the minimum total time?

| tasks | n | answer | a schedule |
|---|---|---|---|
| `AAABBB` | 2 | 8 | A B idle A B idle A B |
| `ACABDB` | 1 | 6 | no idling needed |
| `AAABBB` | 3 | 10 | A B _ _ A B _ _ A B |
| `AAABBB` | 0 | 6 | |

---

## 2. How to think about it

### Greedy: run the most frequent available task
At each time unit, run the **available** task with the **most** remaining copies. Why? It's the bottleneck: it needs the most cooldown gaps, so starting it early leaves the most room to fill those gaps with other tasks.

### Simulation (a heap + a cooldown queue)
- `ready`: the remaining counts of tasks that can run now (a max-heap)
- `cooling`: `{remaining count, the time it becomes ready again}`
- each tick: move a ready task from the queue back to the heap; run the top of the heap (count − 1, back into the queue with readyAt = time + n + 1); otherwise idle

O(total time · log 26).

### The formula (O(len), the "aha")
Let **maxFreq** = the highest count, and **countMax** = how many tasks have it. Picture the most frequent task as the frame:

```
A _ _ | A _ _ | A            (maxFreq − 1) blocks of size (n + 1), plus a last row
time = (maxFreq − 1) · (n + 1) + countMax
```

If there are **more** tasks than idle slots, nothing ever idles, and the time is just `tasks.length`. So:

```
answer = max(tasks.length, (maxFreq − 1) · (n + 1) + countMax)
```

`AAABBB`, n = 2: maxFreq 3, countMax 2 → (3 − 1) · 3 + 2 = 8; max(6, 8) = **8**.

---

## 3. Java concepts used in this problem

### 3.1 Counting letters with `int[26]`

```java
int[] freq = new int[26];
for (char t : tasks) freq[t - 'A']++;
```

`t - 'A'` maps 'A'..'Z' to 0..25 (char arithmetic, from the Strings topic).

### 3.2 `Arrays.stream(freq).max().getAsInt()`

An `IntStream`'s `max()` returns an `OptionalInt` (empty for an empty stream); `getAsInt()` unwraps it. Safe here because the array is never empty.

### 3.3 Storing pairs in a queue: `int[]{remaining, readyAt}`

The cooldown queue is FIFO, and tasks enter it in increasing `readyAt` order (one per tick), so the front is always the next one to become ready. At most one task becomes ready per tick, which is why an `if` (not a `while`) is enough when moving tasks back.

### 3.4 Why the heap only needs counts

Which **letter** runs doesn't matter, only how many copies remain. So the heap holds plain integers.

---

## 4. Approach 1: Greedy simulation

### Code ([M03_TaskScheduler.java:52-68](M03_TaskScheduler.java#L52-L68))

```java
static int simulate(char[] tasks, int n) {
    int[] freq = new int[26];
    for (char t : tasks) freq[t - 'A']++;
    PriorityQueue<Integer> ready = new PriorityQueue<>(Comparator.reverseOrder());
    for (int f : freq) if (f > 0) ready.offer(f);
    Queue<int[]> cooling = new ArrayDeque<>();                // {remaining, readyAt}
    int time = 0;
    while (!ready.isEmpty() || !cooling.isEmpty()) {
        if (!cooling.isEmpty() && cooling.peek()[1] <= time) ready.offer(cooling.poll()[0]);
        if (!ready.isEmpty()) {
            int remaining = ready.poll() - 1;                 // run the most frequent task
            if (remaining > 0) cooling.offer(new int[]{remaining, time + n + 1});
        }                                                     // else: idle this tick
        time++;
    }
    return time;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ready` (max-heap) | the counts of the tasks that can run now |
| `cooling.peek()[1] <= time` | a task's cooldown has ended → it's ready again |
| `ready.poll() - 1` | run one copy of the most frequent ready task |
| `time + n + 1` | it may run again n + 1 ticks later |
| (nothing ready) | an idle tick |
| `time++` | every tick counts, idle or not |

### Dry run: `AAABBB`, n = 2 (from the real run)

| t | event | ready after | cooling after ({count, readyAt}) |
|---|---|---|---|
| 0 | run A (3 → 2) | [3] | {2, 3} |
| 1 | run B (3 → 2) | [] | {2, 3} {2, 4} |
| 2 | **idle** | [] | {2, 3} {2, 4} |
| 3 | A is back; run it (2 → 1) | [] | {2, 4} {1, 6} |
| 4 | B is back; run it (2 → 1) | [] | {1, 6} {1, 7} |
| 5 | **idle** | [] | {1, 6} {1, 7} |
| 6 | A is back; run it (1 → 0) | [] | {1, 7} |
| 7 | B is back; run it (1 → 0) | [] | |

Total **8** ✓

### Complexity
- **Time O(T · log 26)**, where T is the answer.
- **Space O(26).**

---

## 5. Approach 2: The formula (the file's main solution)

### Code ([M03_TaskScheduler.java:43-50](M03_TaskScheduler.java#L43-L50))

```java
static int leastInterval(char[] tasks, int n) {
    int[] freq = new int[26];
    for (char t : tasks) freq[t - 'A']++;
    int maxFreq = Arrays.stream(freq).max().getAsInt();
    int countMax = 0;
    for (int f : freq) if (f == maxFreq) countMax++;
    return Math.max(tasks.length, (maxFreq - 1) * (n + 1) + countMax);
}
```

### Line by line

| Line | What it does |
|---|---|
| `freq` | the count of each letter |
| `maxFreq` | the bottleneck task's count |
| `countMax` | how many tasks tie for that count (they share the last row) |
| `(maxFreq − 1) · (n + 1) + countMax` | the frame's length |
| `Math.max(tasks.length, …)` | with enough other tasks, every slot is filled and nothing idles |

### Why the formula is right
- **Lower bound:** the most frequent task alone needs (maxFreq − 1) gaps of n slots between its copies, and every task tying at maxFreq must also appear in the last row. Also, the time is at least the number of tasks.
- **Achievable:** fill the frame column by column with the other tasks. If they overflow the frame, the extra tasks can be inserted into the blocks without breaking any cooldown (blocks just get longer), giving exactly `tasks.length`.

### Dry runs

| tasks | n | maxFreq | countMax | frame | length | answer |
|---|---|---|---|---|---|---|
| AAABBB | 2 | 3 | 2 | 2 · 3 + 2 = 8 | 6 | **8** |
| AAABBB | 3 | 3 | 2 | 2 · 4 + 2 = 10 | 6 | **10** |
| AAABBB | 0 | 3 | 2 | 2 · 1 + 2 = 4 | 6 | **6** |
| ACABDB | 1 | 2 | 2 (A, B) | 1 · 2 + 2 = 4 | 6 | **6** |

### Complexity
- **Time O(len)** (+ O(26)).
- **Space O(26).**

---

## 6. Comparing the approaches

| | Greedy simulation | Formula |
|---|---|---|
| Time | O(T log 26) | **O(len)** |
| Space | O(26) | O(26) |
| Produces the schedule? | yes (easy to record) | no |

The file checks that the formula equals the simulation on 2,000 random inputs.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 0 (no idling), a single task type (frame = (f − 1)(n + 1) + 1), many distinct tasks (no idling).

**Mistakes:**
1. Forgetting `countMax` (all the tasks tied at the max are in the last row).
2. Forgetting the `max` with `tasks.length`.
3. In the simulation, idling without incrementing time, or using readyAt = time + n (one tick too early).

---

## 8. Interview questions and answers

**Q1. Why schedule the most frequent task first?**
> It determines the minimum length: it needs the most separated slots. Delaying it can only create more idle time at the end.

**Q2. Explain the formula.**
> The most frequent task forms maxFreq − 1 blocks of length n + 1, plus a final row containing every task tied for the maximum. Other tasks fill the idle slots; if there are more of them than slots, there's no idling and the answer is the number of tasks.

### Follow-up questions

**F1. Return an actual schedule.**
> Run the simulation and record the letter (keep `{count, letter}` pairs in the heap).

**F2. The tasks must run in the given order (LC 2365).**
> Track each task type's last finish time in a HashMap: `day = max(day + 1, last[t] + n + 1)`.

**F3. Reorganize String.** → [M04](M04_ReorganizeString.md)
> The same "most frequent first" greedy with n = 1, and no idling allowed.

---

## 9. Running the file

```bash
java M03_TaskScheduler.java      # 6/6 tests passed (including 2,000 random: formula == greedy simulation)
```

**One-line summary:** *"answer = max(len, (maxFreq − 1) · (n + 1) + countMax); the greedy max-heap + cooldown queue simulation gives the same result."*
