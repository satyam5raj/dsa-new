# E01 · Binary Watch ⭐

| | |
|---|---|
| **LeetCode** | [#401 Binary Watch](https://leetcode.com/problems/binary-watch/) |
| **Difficulty** | Easy |
| **Pattern** | Choose k of the 10 LEDs (combinations) with pruning, or just loop over all 720 times |
| **Code** | [E01_BinaryWatch.java](E01_BinaryWatch.java) |
| **Run** | `java E01_BinaryWatch.java` |

⭐ = not in `leetcode.md`, added for a key technique. This is the first note of the Backtracking topic, so section 3 covers **the backtracking template**: choose, explore, un-choose; the `start` index; pruning; and recording a copy. Recursion itself is explained in [Recursion E01](../../13-Recursion/02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A binary watch has 4 LEDs for the hour (worth 8, 4, 2, 1) and 6 LEDs for the minutes (worth 32, 16, 8, 4, 2, 1). Given how many LEDs are on, list every valid time it could show, as `"h:mm"` with hour 0–11 and minute 0–59.

| turnedOn | answer |
|---|---|
| 1 | ["0:01", "0:02", "0:04", "0:08", "0:16", "0:32", "1:00", "2:00", "4:00", "8:00"] |
| 9 | [] (the most LEDs a valid time can use is 3 + 5 = 8, as in 11:59) |

---

## 2. How to think about it

### Backtracking: choose which LEDs are lit
Pick `turnedOn` of the 10 LED positions. It's a **combination** (the order you light them in doesn't matter), so each call only considers positions after the last one chosen (the `start` index). That way {8h, 1m} is generated once, not twice.

**Prune** as soon as the hour exceeds 11 or the minutes exceed 59: lighting more LEDs only makes them bigger, so nothing below that node can be valid.

### The simpler way (worth noticing)
There are only 12 · 60 = 720 possible times. Loop over all of them and keep those where `bitCount(h) + bitCount(m) == turnedOn`. Backtracking is the right tool when the search space is huge; for 720 states, plain enumeration wins. The file has both.

### The decision tree for turnedOn = 2 (part of it)

```
backtrack(left 2, start 0, h 0, m 0)
├─ light 8h → (left 1, start 1, h 8)
│   ├─ light 4h → h = 12  ✗ pruned (h > 11)
│   ├─ light 2h → h = 10, left 0 → "10:00" ✓
│   ├─ light 1h → h = 9           → "9:00" ✓
│   ├─ light 32m → m = 32         → "8:32" ✓
│   └─ ...
├─ light 4h → (left 1, start 2, h 4)
│   ├─ light 2h → "6:00" ✓
│   └─ ...
└─ ...
```

### Dry run: turnedOn = 1
`left` = 1 at the root, so each of the 10 branches lights exactly one LED and then records a time: 8:00, 4:00, 2:00, 1:00, 0:32, 0:16, 0:08, 0:04, 0:02, 0:01. Sorting gives the answer in the table. Nothing is pruned, because one LED can never exceed 11 hours or 59 minutes.

---

## 3. Java concepts used in this problem

### 3.1 The backtracking template

Backtracking is **recursion plus undo**. You build a solution one choice at a time, explore each choice depth-first, and then **undo** it so the next option starts from a clean state:

```
backtrack(state, path):
    if path is complete: record a COPY of path
    for each choice:
        if the choice isn't allowed: continue        // prune
        choose  →  backtrack(...)  →  un-choose
```

The classic version with a list as the path (from [01-Basics/BacktrackingTemplate.java](../01-Basics/BacktrackingTemplate.java)):

```java
static void subsets(int[] a, int start, List<Integer> path, List<List<Integer>> out) {
    out.add(new ArrayList<>(path));                 // every path is a subset
    for (int i = start; i < a.length; i++) {
        path.add(a[i]);                              // choose
        subsets(a, i + 1, path, out);                // explore (only later elements)
        path.remove(path.size() - 1);                // un-choose
    }
}
```

### 3.2 The three families

| family | records | how it avoids repeats | example |
|---|---|---|---|
| **subsets** | at every node | `start` index: only later elements | [M01 Subsets](../03-Medium/M01_Subsets.md) |
| **combinations** | at a fixed size or target | `start` index | this problem, [M03 Combinations](../03-Medium/M03_Combinations.md) |
| **permutations** | when every element is used | a `used[]` array (order matters) | [M04 Permutations](../03-Medium/M04_Permutations.md) |

### 3.3 Record a **copy** of the path

`out.add(new ArrayList<>(path))`, never `out.add(path)`. There's only **one** `path` list, which keeps changing as the search goes on. Adding the list itself stores a reference to it, so at the end every entry in `out` points to the same, now-empty list. This is the most common backtracking bug.

### 3.4 `path.remove(path.size() - 1)`

`List<Integer>` has two `remove` methods. With an `int` argument, `remove(int index)` is chosen, which removes the **last element**: exactly the un-choose step. `path.remove(Integer.valueOf(x))` would remove the first element **equal to** x instead.

### 3.5 Passing values instead of a path: nothing to undo

In this problem, the "path" is just two numbers, `h` and `m`. Passing `h + LED[i]` to the call creates a new value for the child, while the parent's `h` stays unchanged. So there's no explicit un-choose step: returning from the call **is** the undo.

### 3.6 Pruning

A check at the top of the call (`if (h > 11 || m > 59) return;`) cuts off a whole subtree. It's valid here because values only grow as more LEDs are lit: once a branch is invalid, every extension of it is too.

### 3.7 Formatting minutes with a leading zero

```java
h + ":" + (m < 10 ? "0" : "") + m          // 0:05, not 0:5
```

`String.format("%d:%02d", h, m)` does the same (`%02d` pads to two digits with zeros).

### 3.8 Sorting strings

`Collections.sort(out)` sorts **as strings**, so `"10:00"` comes before `"1:00"` (because `'0'` < `':'`). LeetCode accepts any order; the sort just makes the test's comparison deterministic.

---

## 4. Approach 1: Backtracking over the LEDs (the file's main solution)

### Code ([E01_BinaryWatch.java:31-47](E01_BinaryWatch.java#L31-L47))

```java
static final int[] LED = {8, 4, 2, 1, 32, 16, 8, 4, 2, 1};   // first 4 = hours, last 6 = minutes

static List<String> readBinaryWatch(int turnedOn) {
    List<String> out = new ArrayList<>();
    backtrack(turnedOn, 0, 0, 0, out);
    Collections.sort(out);
    return out;
}

static void backtrack(int left, int start, int h, int m, List<String> out) {
    if (h > 11 || m > 59) return;                            // prune: can only grow
    if (left == 0) { out.add(h + ":" + (m < 10 ? "0" : "") + m); return; }
    for (int i = start; i < LED.length; i++) {
        if (i < 4) backtrack(left - 1, i + 1, h + LED[i], m, out);   // turn on an hour LED
        else backtrack(left - 1, i + 1, h, m + LED[i], out);         // turn on a minute LED
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `LED` | the value of each of the 10 LEDs; indices 0–3 are hours |
| `left` | how many more LEDs to light |
| `start` | the first LED position this call may light (combinations, not permutations) |
| the prune | an invalid time can't become valid by lighting more |
| `left == 0` | a complete choice: record the formatted time |
| the loop | try lighting each remaining LED, then recurse with one fewer to go |

When `left` is larger than the number of LEDs remaining, the loop simply runs out without recording anything.

### Complexity
- **Time O(C(10, k))** at most (210 for k = 4 or 6), less with pruning.
- **Space O(k)** recursion depth, plus the output.

---

## 5. Approach 2: Enumerate all 720 times

### Code ([E01_BinaryWatch.java:49-56](E01_BinaryWatch.java#L49-L56))

```java
static List<String> enumerate(int turnedOn) {
    List<String> out = new ArrayList<>();
    for (int h = 0; h < 12; h++)
        for (int m = 0; m < 60; m++)
            if (Integer.bitCount(h) + Integer.bitCount(m) == turnedOn) out.add(h + ":" + (m < 10 ? "0" : "") + m);
    Collections.sort(out);
    return out;
}
```

The number of lit LEDs for a time is the number of 1 bits in the hour plus the number in the minute. `Integer.bitCount` counts them directly.

- **Time O(720)**, which is constant.

---

## 6. Comparing the approaches

| | Backtracking | Enumeration |
|---|---|---|
| Work | ≤ C(10, k) leaves, pruned | always 720 checks |
| Code | recursive | two loops |
| Lesson | the combinations template | sometimes brute force is simplest |

The file checks that both give the same list for every k from 0 to 10.

---

## 7. Edge cases and common mistakes

**Edge cases:** turnedOn = 0 (only "0:00"), turnedOn ≥ 9 (empty), the maximum valid totals (3 hour LEDs for 11, 5 minute LEDs for 59).

**Mistakes:**
1. Looping from 0 instead of `start` (the same set of LEDs is produced in many orders, giving duplicates).
2. Checking validity only at the leaves (it still works, but loses the pruning).
3. Printing minutes without the leading zero.
4. Allowing hour 12 (the watch only shows 0–11).

---

## 8. Interview questions and answers

**Q1. What makes this a combination rather than a permutation problem?**
> Which LEDs are lit matters, not the order they were lit in. Restricting each call to later positions (`start = i + 1`) generates each set exactly once.

**Q2. Why is the pruning valid?**
> LED values are positive, so lighting more LEDs never decreases the hour or the minutes. Once one is out of range, every extension is too.

**Q3. When would you prefer enumeration?**
> When the state space is small and easy to list, as here (720 states). Backtracking pays off when the space is exponential and pruning can cut most of it.

### Follow-up questions

**F1. Return the times sorted chronologically.**
> Generate in enumeration order (h from 0, m from 0), which is already chronological, or sort by h · 60 + m.

**F2. Combinations (LC 77).** → [M03](../03-Medium/M03_Combinations.md)
> The same `start`-index template for choosing k of n.

**F3. Letter Case Permutation (LC 784).**
> Another small "two choices per position" backtracking.

---

## 9. Running the file

```bash
java E01_BinaryWatch.java      # 3/3 tests passed (including k = 0..10: backtracking == enumeration)
```

**One-line summary:** *"Choose turnedOn of the 10 LEDs with a start index (pruning once the hour > 11 or the minutes > 59), or simply keep every h:mm whose bit counts add up to turnedOn."*
