# H04 · Maximum Frequency Stack ⭐

| | |
|---|---|
| **LeetCode** | [#895 Maximum Frequency Stack](https://leetcode.com/problems/maximum-frequency-stack/) |
| **Difficulty** | Hard |
| **Pattern** | One **stack per frequency level** + `maxFreq` |
| **Code** | [H04_MaximumFrequencyStack.java](H04_MaximumFrequencyStack.java) |
| **Run** | `java H04_MaximumFrequencyStack.java` |

⭐ = not in `leetcode.md`, added for a key technique. Design-problem basics are explained in [E01](../02-Easy/E01_LoggerRateLimiter.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Design `FreqStack`:
- `push(val)`;
- `pop()` removes and returns the **most frequent** element in the stack. On a tie, it returns the one closest to the top (pushed most recently).

```
push 5, 7, 5, 7, 4, 5          (the stack, bottom to top: 5 7 5 7 4 5)
pop → 5   (5 appears 3 times)
pop → 7   (5 and 7 both appear twice; the top-most of them is 7)
pop → 5   (5 is now the only one appearing twice)
pop → 4   (5, 7, 4 each appear once; 4 is the top-most)
```

---

## 2. How to think about it

### The heap idea
A max-heap ordered by (frequency, push time) works in O(log n): push `(freq after this push, time, value)`, and pop the top. It's correct, but not optimal.

### The O(1) idea: think of the stack as layers
```
freq:    val → how many copies are currently in the stack
group:   f → a stack of the values that reached frequency f, in push order
maxFreq
```

```
push(x):  f = ++freq[x];  group[f].push(x);  maxFreq = max(maxFreq, f)
pop():    x = group[maxFreq].pop();  freq[x]--;  if group[maxFreq] is empty: maxFreq--
```

### Why it works
- `group[maxFreq]` holds exactly the values at the top frequency, ordered by **when they reached** it. Its top is therefore the right tie-break: the most recent push among the most frequent values.
- A value with frequency 3 sits in `group[1]`, `group[2]` **and** `group[3]`. Popping it from `group[3]` leaves its copies in the lower groups, which correctly describe it at frequency 2.
- `maxFreq--` is enough: any value in `group[maxFreq]` also has a copy in `group[maxFreq − 1]`, so that group is non-empty.

### Dry run: push 5, 7, 5, 7, 4, 5

| push | freq after | group[f] after | maxFreq |
|---|---|---|---|
| 5 | 5:1 | g1 [5] | 1 |
| 7 | 7:1 | g1 [5, 7] | 1 |
| 5 | 5:2 | g2 [5] | 2 |
| 7 | 7:2 | g2 [5, 7] | 2 |
| 4 | 4:1 | g1 [5, 7, 4] | 2 |
| 5 | 5:3 | g3 [5] | 3 |

| pop | from | returns | after | maxFreq |
|---|---|---|---|---|
| 1 | g3 [5] | **5** | g3 empty | 3 → 2 |
| 2 | g2 [5, 7] | **7** | g2 [5] | 2 |
| 3 | g2 [5] | **5** | g2 empty | 2 → 1 |
| 4 | g1 [5, 7, 4] | **4** | g1 [5, 7] | 1 |

Output **"5 7 5 4"** ✓

---

## 3. Java concepts used in this problem

### 3.1 `merge` returning the new value

```java
int f = freq.merge(val, 1, Integer::sum);
```

`merge(key, 1, Integer::sum)` stores 1 if the key is absent, otherwise `old + 1`, and **returns the new value**. So one call both updates the count and gives us `f`. `Integer::sum` is a method reference meaning `(a, b) -> a + b`. `freq.merge(val, -1, Integer::sum)` decrements the same way.

### 3.2 `ArrayDeque` as a stack

`push` adds to the front and `pop` removes from the front, so the front is the top. Covered in [Stack E01](../../04-Stack/02-Easy/E01_ValidParentheses.md#3-java-concepts-used-in-this-problem).

### 3.3 `List.remove(int)` versus `remove(Object)` (in the referee)

```java
return stack.remove(i);     // i is an int → removes the element AT INDEX i and returns it
```

`List<Integer>` has two overloads: `remove(int index)` and `remove(Object o)`. With an `int` argument, Java picks the index version. To remove the **value** 5 you'd write `stack.remove(Integer.valueOf(5))`. This is a classic Java trap.

---

## 4. Approach 1: Scan the real stack on every pop (the referee)

### Code ([H04_MaximumFrequencyStack.java:62-72](H04_MaximumFrequencyStack.java#L62-L72))

```java
static class Brute {
    final List<Integer> stack = new ArrayList<>();
    void push(int v) { stack.add(v); }
    int pop() {
        Map<Integer, Integer> c = new HashMap<>();
        for (int v : stack) c.merge(v, 1, Integer::sum);
        int max = Collections.max(c.values());
        for (int i = stack.size() - 1; i >= 0; i--) if (c.get(stack.get(i)) == max) return stack.remove(i);
        throw new IllegalStateException();
    }
}
```

| Line | What it does |
|---|---|
| the count loop | the frequency of every value currently in the stack |
| `Collections.max` | the highest frequency |
| the backwards loop | the top-most element with that frequency; remove it by index |
| the `throw` | unreachable when the stack is non-empty |

**push O(1), pop O(n).** It follows the definition literally.

---

## 5. Approach 2: Frequency groups (the file's main solution)

### Code ([H04_MaximumFrequencyStack.java:42-59](H04_MaximumFrequencyStack.java#L42-L59))

```java
static class FreqStack {
    private final Map<Integer, Integer> freq = new HashMap<>();
    private final Map<Integer, Deque<Integer>> group = new HashMap<>();
    private int maxFreq = 0;

    public void push(int val) {
        int f = freq.merge(val, 1, Integer::sum);
        group.computeIfAbsent(f, k -> new ArrayDeque<>()).push(val);
        maxFreq = Math.max(maxFreq, f);
    }

    public int pop() {
        int val = group.get(maxFreq).pop();
        freq.merge(val, -1, Integer::sum);
        if (group.get(maxFreq).isEmpty()) maxFreq--;
        return val;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `push`: `merge` | the value's new frequency f |
| `push`: `group[f].push` | the value has now reached level f |
| `push`: `maxFreq` | a new highest level may have appeared |
| `pop`: `group[maxFreq].pop()` | the most recent value at the top frequency |
| `pop`: `merge(..., -1, ...)` | one fewer copy |
| `pop`: `maxFreq--` | the top level emptied; the next level is guaranteed non-empty |

### Complexity
- **O(1)** per `push` and `pop`.
- **Space O(n)**: each push adds exactly one entry to exactly one group.

---

## 6. Comparing the approaches

| | Scan on pop | Heap | Frequency groups |
|---|---|---|---|
| push | O(1) | O(log n) | **O(1)** |
| pop | O(n) | O(log n) | **O(1)** |

The file runs 100,000 random pushes and pops against the scanning version.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single value pushed many times, all values distinct (it behaves like a normal stack), alternating pushes and pops.

**Mistakes:**
1. Removing the value from **all** groups on pop (only the top group's copy goes).
2. Recomputing `maxFreq` by scanning instead of `maxFreq--`.
3. Breaking ties by the **first** push of a value instead of the push that reached the current frequency.
4. `list.remove(i)` versus `list.remove(Integer.valueOf(x))` confusion.

---

## 8. Interview questions and answers

**Q1. Why does each group keep the right tie-break order?**
> A value is pushed onto `group[f]` at the moment it reaches frequency f, so `group[f]`'s top is the value that most recently reached f, which is the top-most among the values at that frequency.

**Q2. Why is `maxFreq--` always correct?**
> Every value in `group[m]` also has an entry in `group[m − 1]` (it passed through that level). So if `group[m]` empties, `group[m − 1]` still has entries, unless the stack is empty.

**Q3. What about the heap version?**
> Push `(freq, time, value)` on every push and pop the top. Stale entries never occur, because every push adds one entry and every pop removes one. It's O(log n) and also correct.

### Follow-up questions

**F1. Peek without popping.**
> `group.get(maxFreq).peek()`.

**F2. Pop the least frequent instead.**
> Keep a `minFreq`, but it's harder: after a pop, the minimum can jump arbitrarily, so you need the bucket structures from [H03 All O`one](H03_AllOneDataStructure.md).

**F3. LFU Cache.** → [H01](H01_LFUCache.md)
> The same "group by frequency, break ties by recency" idea, for eviction.

---

## 9. Running the file

```bash
java H04_MaximumFrequencyStack.java      # 2/2 tests passed (example, 100,000 random ops vs scanning the stack)
```

**One-line summary:** *"Keep a stack for each frequency level, push a value onto the level it just reached, and pop from the highest level, lowering maxFreq when that level empties."*
