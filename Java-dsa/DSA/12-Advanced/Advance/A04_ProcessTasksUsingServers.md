# A04 · Process Tasks Using Servers ⭐

| | |
|---|---|
| **LeetCode** | [#1882 Process Tasks Using Servers](https://leetcode.com/problems/process-tasks-using-servers/) |
| **Difficulty** | Medium |
| **Mix of structures** | **Two heaps**: free servers by (weight, index) + busy servers by free time |
| **Code** | [A04_ProcessTasksUsingServers.java](A04_ProcessTasksUsingServers.java) |
| **Run** | `java A04_ProcessTasksUsingServers.java` |

⭐ = not in `leetcode.md`, added because it needs two data structures working together.

---

## 1. The problem

- `servers[i]` is the weight of server i.
- `tasks[j]` is how long task j takes; task j **arrives at second j**.
- Tasks are assigned **in order**. A task goes to the **free** server with the smallest weight (ties: smallest index).
- If no server is free, the task (and every task behind it) waits until a server frees up.

Return `ans[j]` = the server that runs task j.

```
servers = [3,3,2], tasks = [1,2,3,2,1,2]          →  [2,2,0,2,1,2]
servers = [5,1,4,3,2], tasks = [2,1,2,4,5,2,1]    →  [1,4,1,4,1,3,2]
```

---

## 2. How to think about it

### Two different "smallest" questions → two heaps
| Question | Heap |
|---|---|
| "Which **free** server should take the task?" | `free`: min-heap by `(weight, index)` |
| "Which **busy** server finishes **first**?" | `busy`: min-heap by `freeTime` |

Servers move back and forth between the two heaps:

```
for each task j:
    time = max(time, j)                        // the task can't start before it arrives
    move every busy server with freeTime <= time into free
    if free is empty:                          // everyone is busy: wait
        time = busy.peek().freeTime
        move every busy server with freeTime <= time into free
    s = free.poll();   ans[j] = s;   busy.add(time + tasks[j], s)
```

**Why `time = max(time, j)` and not just `j`?** If tasks had to wait, the clock is already past `j`, and tasks keep their order, so a later task can never start earlier than the one before it.

**Why release *all* servers with `freeTime <= time`, not just one?** Several may become free at the same moment, and the task should go to the lightest of **all** of them.

### Dry run: `servers [3,3,2]`, `tasks [1,2,3,2,1,2]`
| j | time | released to free | free (weight, idx) | pick | busy until |
|---|---|---|---|---|---|
| 0 | 0 | – | (2,s2) (3,s0) (3,s1) | **s2** | 1 |
| 1 | 1 | s2 (free at 1) | (2,s2) (3,s0) (3,s1) | **s2** | 3 |
| 2 | 2 | – | (3,s0) (3,s1) | **s0** | 5 |
| 3 | 3 | s2 (free at 3) | (2,s2) (3,s1) | **s2** | 5 |
| 4 | 4 | – | (3,s1) | **s1** | 5 |
| 5 | 5 | s0, s1, s2 (all free at 5) | (2,s2) (3,s0) (3,s1) | **s2** | 7 |

Answer **[2, 2, 0, 2, 1, 2]** ✓

---

## 3. Java concepts used in this problem

### 3.1 A heap of indices with a comparator over another array
```java
PriorityQueue<Integer> free = new PriorityQueue<>((a, b) ->
        servers[a] != servers[b] ? Integer.compare(servers[a], servers[b]) : Integer.compare(a, b));
```
The heap stores server **indices**; the comparator looks up their weights. Weights never change, so the heap order stays valid.

### 3.2 `PriorityQueue<long[]>` for (freeTime, server)
```java
PriorityQueue<long[]> busy = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));   // {freeTime, server}
busy.add(new long[]{time + tasks[j], s});
free.add((int) busy.poll()[1]);
```
Free times can exceed `int` (2 × 10⁵ tasks each taking up to 2 × 10⁵ seconds is 4 × 10¹⁰), so they're stored as `long`. The server index is stored in the same `long[]` and cast back with `(int)`.

### 3.3 Long vs int arithmetic
`time = Math.max(time, j)`: `time` is a `long` and `j` an `int`; `Math.max(long, long)` is chosen after widening `j`.

---

## 4. Approach 1: Scan every server for every task (the referee)

### Code ([A04_ProcessTasksUsingServers.java:42-58](A04_ProcessTasksUsingServers.java#L42-L58))
```java
static int[] assignTasksBrute(int[] servers, int[] tasks) {
    long[] freeAt = new long[servers.length];
    int[] ans = new int[tasks.length];
    long time = 0;
    for (int j = 0; j < tasks.length; j++) {
        time = Math.max(time, j);
        long earliest = Long.MAX_VALUE;
        for (long f : freeAt) earliest = Math.min(earliest, f);
        time = Math.max(time, earliest);                                 // wait if nobody is free
        int best = -1;
        for (int i = 0; i < servers.length; i++)
            if (freeAt[i] <= time && (best == -1 || servers[i] < servers[best])) best = i;
        ans[j] = best;
        freeAt[best] = time + tasks[j];
    }
    return ans;
}
```

| Line | What it does |
|---|---|
| `freeAt[i]` | when server i is free (0 = free from the start) |
| `earliest` | the soonest any server is free; if that's later than `time`, everyone is busy, so wait |
| the `best` loop | the lightest free server; scanning in index order with `<` gives the smallest index on ties |
| `freeAt[best] = ...` | it's busy until the task finishes |

**Cost: O(n · m)** for n servers and m tasks.

---

## 5. Approach 2: Two heaps (main solution)

### Code ([A04_ProcessTasksUsingServers.java:61-81](A04_ProcessTasksUsingServers.java#L61-L81))
```java
static int[] assignTasks(int[] servers, int[] tasks) {
    PriorityQueue<Integer> free = new PriorityQueue<>((a, b) ->
            servers[a] != servers[b] ? Integer.compare(servers[a], servers[b]) : Integer.compare(a, b));
    PriorityQueue<long[]> busy = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));   // {freeTime, server}
    for (int i = 0; i < servers.length; i++) free.add(i);

    int[] ans = new int[tasks.length];
    long time = 0;
    for (int j = 0; j < tasks.length; j++) {
        time = Math.max(time, j);
        while (!busy.isEmpty() && busy.peek()[0] <= time) free.add((int) busy.poll()[1]);
        if (free.isEmpty()) {                                            // everyone busy: wait for the first one
            time = busy.peek()[0];
            while (!busy.isEmpty() && busy.peek()[0] <= time) free.add((int) busy.poll()[1]);
        }
        int s = free.poll();
        ans[j] = s;
        busy.add(new long[]{time + tasks[j], s});
    }
    return ans;
}
```

### Line by line
| Line | What it does |
|---|---|
| `free` holds all servers at the start | everyone is idle at time 0 |
| `time = max(time, j)` | task j arrives at j, but can't jump ahead of earlier waiting tasks |
| the first `while` | release servers that have finished by now |
| `if (free.isEmpty())` | nobody is free: fast-forward to the earliest finish and release everything finishing then |
| `free.poll()` | the lightest free server, smallest index on ties |
| `busy.add(...)` | busy until `time + tasks[j]` |

### Complexity
- Every server moves between the heaps at most once per task, each move O(log n).
- **O((n + m) log n)**, space O(n).

---

## 6. Comparing the approaches

| | Time |
|---|---|
| Scan servers | O(n · m) |
| **Two heaps** | **O((n + m) log n)** |

The file checks the two heaps against the scan on 3,000 random cases.

---

## 7. Edge cases and common mistakes

**Edge cases:** one server (tasks queue up), all servers busy, several servers freeing at the same moment, equal weights (smallest index wins).

**Mistakes:**
1. `time = j` every iteration, which forgets that waiting tasks push the clock forward.
2. Releasing only **one** server when several free up at the same time.
3. An `int` for free times (overflow).
4. Comparing only weights in `free` (the index tie-break goes missing).
5. Assigning a waiting task at time `j` instead of when the server actually frees up.

---

## 8. Interview questions and answers

**Q1. Why two heaps?**
> They answer different questions: the best free server (by weight and index), and the server that frees first (by time). A server is in exactly one heap at a time.

**Q2. How do you handle "all servers busy"?**
> Jump the clock to the smallest free time in the busy heap, release every server free by then, and assign from the free heap.

**Q3. Why is the clock `max(time, j)`?**
> Tasks are assigned in order. If an earlier task had to wait, the current task can't be assigned before it, so the clock never goes backwards.

### Follow-up questions

**F1. "Tasks can go to any free server out of order (no queue)."**
> Then each task can start at `max(j, earliest free time)` independently. It's the same heaps, but the order constraint disappears.

**F2. "Servers can fail."**
> Remove them lazily: mark them dead and skip them when they come out of either heap (lazy deletion, as in [A02](A02_StockPriceFluctuation.md)).

**F3. "Real systems?"**
> This is a load balancer's least-loaded, weighted assignment; the busy heap is a timer queue.

---

## 9. Running the file

```bash
java A04_ProcessTasksUsingServers.java     # 4/4 tests passed (2 examples, one server, 3,000 random cases)
```

**One-line summary:** *"Keep free servers in a min-heap by (weight, index) and busy servers in a min-heap by free time; for each task, move finished servers back, fast-forward the clock if none are free, and give the task to the best free server."*
