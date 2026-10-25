# M04 · Reorganize String

| | |
|---|---|
| **LeetCode** | [#767 Reorganize String](https://leetcode.com/problems/reorganize-string/) |
| **Difficulty** | Medium |
| **Pattern** | Greedy "most frequent first" with a max-heap, holding back the last letter |
| **Code** | [M04_ReorganizeString.java](M04_ReorganizeString.java) |
| **Run** | `java M04_ReorganizeString.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Rearrange s so that **no two adjacent characters are equal**; return `""` if that's impossible.

| s | answer |
|---|---|
| `"aab"` | `"aba"` |
| `"aaab"` | `""` |
| `"aaabbc"` | `"ababac"` (one valid answer) |

**Constraints:** `1 ≤ |s| ≤ 500`, lowercase letters.

---

## 2. How to think about it

### When is it possible?
The most frequent letter needs a different letter between every pair of its copies, so it can occupy at most every other slot:

```
maxFreq ≤ (len + 1) / 2          "aaab": 3 > (4 + 1)/2 = 2 → impossible
```

### Greedy with a heap
At every step, place the **most frequent** letter that isn't the one just placed. Why the most frequent? It's the hardest to fit. Using it early keeps the counts balanced, so we don't end up stuck with many copies of one letter.

**The implementation trick:** after placing a letter, **don't** put it back into the heap yet. "Hold" it for one step, so it can't be chosen twice in a row:

```
top = poll;  append top;  top.count−−
push the previously held letter back (if it has copies left)
held = top
```

If the heap runs empty while the held letter still has copies, we're stuck: the result is shorter than s → return `""`.

### O(n) alternative without a heap: even/odd slots
Write the most frequent letter at indices 0, 2, 4, …; then continue with the other letters on the remaining even indices, wrapping to the odd ones 1, 3, 5, … Copies of one letter always land 2 apart (or on different parities), so no two are adjacent.

---

## 3. Java concepts used in this problem

### 3.1 A heap of `{letter, remaining}` pairs with a tie-break

```java
new PriorityQueue<int[]>((x, y) -> x[1] != y[1] ? Integer.compare(y[1], x[1]) : Integer.compare(x[0], y[0]))
```

Most remaining first; on a tie, the smaller letter first. The tie-break makes the output deterministic (`"ababac"`), which keeps the tests stable.

### 3.2 Mutating an element that is **outside** the heap

```java
int[] top = heap.poll();
top[1]--;                       // safe: top is no longer inside the heap
```

**Never** change a field that the comparator uses while the element is inside a PriorityQueue: the heap wouldn't notice, and its order would break. Here `top` was polled first, and it's only re-offered later (as `held`), after the change.

### 3.3 `(char) ('a' + index)`

It converts a 0..25 index back into a letter. `'a' + index` is an `int`, so the cast is needed.

### 3.4 `count[letter]-- > 0`

It compares the **old** value with 0, then decrements: the loop body runs exactly `count[letter]` times.

---

## 4. Approach 1: Max-heap with a held letter (the file's main solution)

### Code ([M04_ReorganizeString.java:47-63](M04_ReorganizeString.java#L47-L63))

```java
static String reorganizeString(String s) {
    int[] count = new int[26];
    for (char c : s.toCharArray()) count[c - 'a']++;
    // entries {letter, remaining}; most remaining first, ties by letter
    PriorityQueue<int[]> heap = new PriorityQueue<>((x, y) -> x[1] != y[1] ? Integer.compare(y[1], x[1]) : Integer.compare(x[0], y[0]));
    for (int i = 0; i < 26; i++) if (count[i] > 0) heap.offer(new int[]{i, count[i]});
    StringBuilder sb = new StringBuilder();
    int[] held = null;                                       // the letter just placed
    while (!heap.isEmpty()) {
        int[] top = heap.poll();
        sb.append((char) ('a' + top[0]));
        top[1]--;
        if (held != null && held[1] > 0) heap.offer(held);   // allowed again after one step
        held = top;
    }
    return sb.length() == s.length() ? sb.toString() : "";   // stuck with copies left
}
```

### Line by line

| Line | What it does |
|---|---|
| `count` | the frequency of each letter |
| the heap | the letters available for the next position, most frequent first |
| `held` | the letter placed last: temporarily banned |
| `top = heap.poll(); sb.append(...)` | place the most frequent allowed letter |
| `top[1]--` | one fewer copy |
| `heap.offer(held)` | the previous letter is allowed again (if it has copies left) |
| `held = top` | now this one is banned for the next step |
| the length check | shorter than s → we got stuck → impossible |

There's no explicit "is it possible?" check: an impossible input simply gets stuck.

### Dry run: `"aaabbc"` (a3 b2 c1), from the real run

| step | placed | pushed back | heap after | held |
|---|---|---|---|---|
| 1 | a → "a" | | b2 c1 | a2 |
| 2 | b → "ab" | a2 | a2 c1 | b1 |
| 3 | a → "aba" | b1 | b1 c1 | a1 |
| 4 | b → "abab" | a1 | a1 c1 | b0 |
| 5 | a → "ababa" | (b0 has none left) | c1 | a0 |
| 6 | c → "ababac" | | | c0 |

Length 6 = |s| → **"ababac"** ✓

### Dry run: `"aaab"` (a3 b1)
a → "a" (held a2); b → "ab" (a2 back, held b0); a → "aba" (held a1); the heap is now empty, but a still has 1 copy → length 3 < 4 → **""** ✓

### Complexity
- **Time O(n log 26) = O(n).**
- **Space O(26)** plus the output.

---

## 5. Approach 2: Fill the even slots, then the odd ones

### Code ([M04_ReorganizeString.java:65-87](M04_ReorganizeString.java#L65-L87))

```java
static String reorganizeEvenOdd(String s) {
    int n = s.length();
    int[] count = new int[26];
    for (char c : s.toCharArray()) count[c - 'a']++;
    int maxLetter = 0;
    for (int i = 1; i < 26; i++) if (count[i] > count[maxLetter]) maxLetter = i;
    if (count[maxLetter] > (n + 1) / 2) return "";

    List<Integer> order = new ArrayList<>();
    order.add(maxLetter);                                    // the most frequent goes first
    for (int i = 0; i < 26; i++) if (i != maxLetter) order.add(i);

    char[] out = new char[n];
    int idx = 0;
    for (int letter : order) {
        while (count[letter]-- > 0) {
            if (idx >= n) idx = 1;                           // even slots full -> odd slots
            out[idx] = (char) ('a' + letter);
            idx += 2;
        }
    }
    return new String(out);
}
```

### Line by line

| Line | What it does |
|---|---|
| `maxLetter` | the most frequent letter |
| the `(n + 1) / 2` check | impossible → `""` immediately |
| `order` | the most frequent letter first, then the rest |
| `if (idx >= n) idx = 1;` | the even slots are used up → continue on the odd slots |
| `idx += 2` | every other slot |

**Why the most frequent must go first:** it needs up to ⌈n/2⌉ slots, which is exactly the number of even slots. Placed first, it never has to wrap around to the odd slots. Any other letter has fewer than ⌈n/2⌉ copies, so when it wraps, its copies are on different parities or 2 apart.

### Dry run: `"aaabbc"`

| letter | slots |
|---|---|
| a | 0, 2, 4 |
| b | (idx 6 ≥ 6 → wrap to 1) 1, 3 |
| c | 5 |

→ `a b a b a c` = **"ababac"** ✓

### Complexity
- **Time O(n).**
- **Space O(n)** for the output.

---

## 6. Comparing the approaches

| | Heap + held letter | Even/odd fill |
|---|---|---|
| Time | O(n log 26) | O(n) |
| Generalises to distance k? | yes (hold k letters in a queue) | no |
| Insight needed | the greedy | the parity argument |

The file checks both on 3,000 random strings: valid outputs when possible, `""` when not.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single character, all the same letter (possible only if the length is 1), exactly maxFreq = (n + 1)/2.

**Mistakes:**
1. Pushing the letter back immediately (the same letter can be picked again).
2. Changing a counter while its entry is still inside the heap.
3. In the even/odd version, not starting with the most frequent letter.

---

## 8. Interview questions and answers

**Q1. Why is maxFreq ≤ (n + 1)/2 necessary and sufficient?**
> Necessary: the copies of the most frequent letter need separators. Sufficient: the even/odd construction (or the greedy) always succeeds under that condition.

**Q2. Why hold the last letter instead of checking "is the top equal to the last one?"**
> Holding it keeps the heap's invariant simple: the heap only ever contains allowed letters, so its top is always a valid choice.

### Follow-up questions

**F1. Same letters at least k apart (LC 358).**
> Hold the last k placed letters in a FIFO queue; a letter re-enters the heap once it leaves the queue. Fail if the heap is empty while letters remain.

**F2. Task Scheduler.** → [M03](M03_TaskScheduler.md)
> The same greedy, but idling is allowed, so it never fails.

**F3. Distant Barcodes (LC 1054).**
> Exactly this problem on integers: the even/odd fill works directly.

---

## 9. Running the file

```bash
java M04_ReorganizeString.java      # 5/5 tests passed (including 3,000 random strings, both versions)
```

**One-line summary:** *"Repeatedly place the most frequent letter from a max-heap while holding back the one just placed; or fill the even slots with the most frequent letter, then everything else."*
