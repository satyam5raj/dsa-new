# M13 · Exclusive Time of Functions

| | |
|---|---|
| **LeetCode** | [#636 Exclusive Time of Functions](https://leetcode.com/problems/exclusive-time-of-functions/) |
| **Difficulty** | Medium |
| **Pattern** | Simulate the call stack (a CPU profiler in miniature) |
| **Code** | [M13_ExclusiveTimeOfFunctions.java](M13_ExclusiveTimeOfFunctions.java) |
| **Run** | `java M13_ExclusiveTimeOfFunctions.java` |

---

## 1. The problem

A **single-threaded** CPU runs n functions. The logs look like `"id:start:time"` or `"id:end:time"`.
- A **start** at time t begins **at the start** of unit t.
- An **end** at time t ends **at the end** of unit t (so `"0:start:0"` + `"0:end:0"` ran for **1** unit).

**Exclusive time** = the time spent in the function itself, **not** in the functions it called.

```
n = 2, logs = ["0:start:0", "1:start:2", "1:end:5", "0:end:6"]   →   [3, 4]

time:  0  1  2  3  4  5  6
f0:    █  █              █      → 3 units
f1:          █  █  █  █         → 4 units
```

| logs | answer |
|---|---|
| the example above | `[3, 4]` |
| `0:start:0, 0:start:2, 0:end:5, 0:start:6, 0:end:6, 0:end:7` (recursion) | `[8]` |
| `0:start:0, 0:end:0` | `[1]` |

---

## 2. How to think about it

The function **currently running** is always the **top of the call stack**: a start pushes, an end pops. That's exactly how a real CPU's call stack works.

Keep `prev` = **the start of the current, not-yet-charged time slice**:

| log | what to charge | then |
|---|---|---|
| **start** at t | the function on top ran from prev to t − 1 → charge it **t − prev** | push the new id; prev = t |
| **end** at t | the top function ran from prev to t **inclusive** → charge **t − prev + 1** | pop it; **prev = t + 1** (the caller resumes at the next unit) |

The `+1` and the `t + 1` come from the "ends at the end of t" rule. Getting this right is the whole problem.

---

## 3. Java concepts used in this problem

### 3.1 Parsing a log line with `split`

```java
String[] p = log.split(":");                // "1:start:2" → ["1", "start", "2"]
int id = Integer.parseInt(p[0]), t = Integer.parseInt(p[2]);
if (p[1].equals("start")) ...
```

`.equals` for comparing the string `"start"`, as always.

### 3.2 `List.of(...)` for the test input

The method takes a `List<String>`; the tests create one with `List.of("0:start:0", ...)` (an immutable list).

### 3.3 A stack of ids models nested calls

Recursion (the same id twice on the stack) needs no special handling: each push and pop is matched, and the time is charged to whichever call is on top.

---

## 4. Approach 1: A stack of (id, start time), charging the time on end

An alternative bookkeeping: push `[id, startTime]`; on end, compute the **total** duration and subtract the time used by the children.

```java
static int[] exclusiveTimeChildren(int n, List<String> logs) {
    int[] res = new int[n];
    Deque<int[]> st = new ArrayDeque<>();            // [id, start, childTime]
    for (String log : logs) {
        String[] p = log.split(":");
        int id = Integer.parseInt(p[0]), t = Integer.parseInt(p[2]);
        if (p[1].equals("start")) {
            st.push(new int[]{id, t, 0});
        } else {
            int[] f = st.pop();
            int total = t - f[1] + 1;                // the whole duration, inclusive
            res[f[0]] += total - f[2];               // minus the time spent in its children
            if (!st.isEmpty()) st.peek()[2] += total; // the parent's children took `total`
        }
    }
    return res;
}
```

It's O(L) for L logs too, but with more state per frame. The file's "prev" approach is simpler.

---

## 5. Approach 2: A stack of ids + the `prev` time pointer (the file's solution)

### Code ([M13_ExclusiveTimeOfFunctions.java:40-57](M13_ExclusiveTimeOfFunctions.java#L40-L57))

```java
static int[] exclusiveTime(int n, List<String> logs) {
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>();
    int prev = 0;
    for (String log : logs) {
        String[] p = log.split(":");
        int id = Integer.parseInt(p[0]), t = Integer.parseInt(p[2]);
        if (p[1].equals("start")) {
            if (!stack.isEmpty()) result[stack.peek()] += t - prev;   // pause the caller
            stack.push(id);
            prev = t;
        } else {
            result[stack.pop()] += t - prev + 1;                      // end is inclusive
            prev = t + 1;
        }
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int prev = 0;` | the start of the uncharged slice |
| start: `result[stack.peek()] += t - prev;` | the caller ran from prev up to (not including) t; charge it before it's paused |
| `stack.push(id); prev = t;` | the new function starts running at t |
| end: `result[stack.pop()] += t - prev + 1;` | the top function ran from prev through t inclusive |
| `prev = t + 1;` | the caller resumes at the next unit |

### Dry run: `["0:start:0", "1:start:2", "1:end:5", "0:end:6"]`

| log | charge | stack after | prev after | result |
|---|---|---|---|---|
| 0:start:0 | (empty stack) | [0] | 0 | [0, 0] |
| 1:start:2 | f0 += 2 − 0 = 2 | [0, 1] | 2 | [2, 0] |
| 1:end:5 | f1 += 5 − 2 + 1 = 4 | [0] | 6 | [2, 4] |
| 0:end:6 | f0 += 6 − 6 + 1 = 1 | [] | 7 | [3, 4] |

**Result `[3, 4]`** ✓

### Dry run 2: recursion, `0:start:0, 0:start:2, 0:end:5, 0:start:6, 0:end:6, 0:end:7`

| log | charge to f0 | prev |
|---|---|---|
| start 0 | — | 0 |
| start 2 | 2 − 0 = 2 | 2 |
| end 5 | 5 − 2 + 1 = 4 | 6 |
| start 6 | 6 − 6 = 0 | 6 |
| end 6 | 6 − 6 + 1 = 1 | 7 |
| end 7 | 7 − 7 + 1 = 1 | 8 |

Total **8** ✓ (every unit 0..7 is f0, at some depth of the recursion).

### Complexity
- **Time O(L)** for L logs, **Space O(n + depth).**

---

## 6. Comparing the approaches

| | (id, start, childTime) frames | Stack of ids + prev |
|---|---|---|
| Time | O(L) | O(L) |
| State | 3 ints per frame | 1 int per frame + 1 variable |
| Ease | more bookkeeping | **simpler** |

---

## 7. Edge cases and common mistakes

**Edge cases:** recursion (the same id nested), back-to-back calls, a function that runs for a single unit, deep nesting.

**Mistakes:**
1. Off by one: forgetting the `+1` on an end, or setting `prev = t` instead of `t + 1` after an end.
2. Not charging the caller **before** pushing a new function.
3. Comparing `p[1] == "start"` with `==`.

---

## 8. Interview questions and answers

**Q1. Why is a stack the right model?**
> A single-threaded CPU's calls are perfectly nested: the most recently started function that hasn't ended yet is the one running. That's the top of a stack.

**Q2. Why `+1` on end and `t + 1` for prev?**
> An end timestamp means the end **of** that unit, so the unit t itself belongs to the ending function, and the caller resumes at t + 1.

### Follow-up questions

**F1. Inclusive time (including the children).**
> Store the start time with each frame; on end, add `t − start + 1` to the function (don't subtract the children).

**F2. A multi-threaded profiler.**
> One call stack per thread; the logs carry thread ids.

**F3. Build a flame graph.**
> Aggregate (call path → time) by keeping the whole stack content as the key when charging.

---

## 9. Running the file

```bash
java M13_ExclusiveTimeOfFunctions.java      # 4/4 tests passed
```

**One-line summary:** *"The stack top is the running function; on start charge the top t − prev, on end charge t − prev + 1 and set prev = t + 1."*
