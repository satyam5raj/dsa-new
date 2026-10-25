# M03 · Next Greater Element II

| | |
|---|---|
| **LeetCode** | [#503 Next Greater Element II](https://leetcode.com/problems/next-greater-element-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Monotonic stack on a **circular** array (loop twice with `i % n`) |
| **Code** | [M03_NextGreaterElementII.java](M03_NextGreaterElementII.java) |
| **Run** | `java M03_NextGreaterElementII.java` |

---

## 1. The problem

The array is **circular**: after the last element comes the first. For each element, find the next greater value (searching **around the circle**), or −1.

| nums | answer | why |
|---|---|---|
| `[1, 2, 1]` | `[2, -1, 2]` | the last 1 wraps around and finds the 2 |
| `[1, 2, 3, 4, 3]` | `[2, 3, 4, -1, 4]` | the last 3 wraps around to 4 |
| `[5, 5, 5]` | `[-1, -1, -1]` | equal is not greater |

**Constraints:** `1 ≤ n ≤ 10⁴`; duplicates are allowed.

---

## 2. How to think about it

### The circular trick
Pretend the array is written **twice**: `[1, 2, 1, 1, 2, 1]`. Any element's "next greater around the circle" lies within the next n elements of this doubled array. You don't need to copy it: loop `i` from 0 to 2n − 1 and read `nums[i % n]`.

### The algorithm
```
answer = all −1
for i in 0 .. 2n−1:
    x = nums[i % n]
    while the stack isn't empty and nums[stack.top] < x:  answer[stack.pop()] = x
    if i < n: push i      // only the FIRST pass creates waiting entries;
                          // the second pass only RESOLVES them
```

Store **indices** (the values can repeat).

---

## 3. Java concepts used in this problem

### 3.1 `i % n` for circular indexing

`i % n` maps 0..2n−1 back onto 0..n−1, simulating a wrap-around without extra memory. It's the same tool as in a circular queue or rotation.

### 3.2 `Arrays.fill(ans, -1)`

It sets every entry to −1 at once, which is the default answer for "no greater element".

### 3.3 Why the push is only in the first pass

In the second pass, the indices `i % n` are the same elements again. Pushing them would create duplicate "waiting" entries. The second pass exists only so that the elements near the end can **see** the elements at the start.

---

## 4. Approach 1: Brute force around the circle

### Code ([M03_NextGreaterElementII.java:57-65](M03_NextGreaterElementII.java#L57-L65))

```java
static int[] brute(int[] a) {
    int n = a.length;
    int[] r = new int[n];
    Arrays.fill(r, -1);
    for (int i = 0; i < n; i++)
        for (int k = 1; k < n; k++)
            if (a[(i + k) % n] > a[i]) { r[i] = a[(i + k) % n]; break; }
    return r;
}
```

For each i, look at the next n − 1 positions around the circle. **O(n²).** It's the reference for the tests.

---

## 5. Approach 2: Monotonic stack, two passes (the file's solution)

### Code ([M03_NextGreaterElementII.java:44-55](M03_NextGreaterElementII.java#L44-L55))

```java
static int[] nextGreaterElements(int[] nums) {
    int n = nums.length;
    int[] ans = new int[n];
    Arrays.fill(ans, -1);
    Deque<Integer> stack = new ArrayDeque<>();
    for (int i = 0; i < 2 * n; i++) {
        int x = nums[i % n];
        while (!stack.isEmpty() && nums[stack.peek()] < x) ans[stack.pop()] = x;
        if (i < n) stack.push(i);
    }
    return ans;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Arrays.fill(ans, -1);` | the default |
| `for (int i = 0; i < 2 * n; i++)` | walk the circle twice |
| `int x = nums[i % n];` | the current element (wrapped) |
| `while (... nums[stack.peek()] < x) ans[stack.pop()] = x;` | x resolves every smaller waiting element |
| `if (i < n) stack.push(i);` | register each index once, in the first pass |

### Dry run: `[1, 2, 1]`

| i | i % n | x | pops (index: answer) | stack after (indices) |
|---|---|---|---|---|
| 0 | 0 | 1 | — | [0] |
| 1 | 1 | 2 | 0: 2 | [1] |
| 2 | 2 | 1 | — | [1, 2] |
| 3 | 0 | 1 | — (1 is not < 1) | [1, 2] |
| 4 | 1 | 2 | 2: 2 | [1] |
| 5 | 2 | 1 | — | [1] |

Index 1 (the maximum) stays −1 → **`[2, -1, 2]`** ✓

### Dry run 2: `[1, 2, 3, 4, 3]`
First pass: 1 → 2, 2 → 3, 3 → 4 resolved; the stack holds [3(4), 4(3)]. Second pass: x = 1, 2, 3: nothing smaller; x = 4 (i = 8): pops index 4 (value 3) → 4. **`[2, 3, 4, -1, 4]`** ✓

### Complexity
- **Time O(n):** 2n iterations; each index is pushed once and popped at most once.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Brute | Two-pass monotonic stack |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(1) | O(n) |

---

## 7. Edge cases and common mistakes

**Edge cases:** all equal (all −1), a single element (−1), the maximum appearing several times, strictly decreasing (only the wrap-around helps).

**Mistakes:**
1. Pushing indices in the second pass too (duplicates; it still works for values, but wastes work and can confuse).
2. Storing values instead of indices (it breaks with duplicates).
3. Copying the array to double its size (it works, but O(n) extra memory isn't needed).

---

## 8. Interview questions and answers

**Q1. Why are two passes enough?**
> An element's next greater element around the circle is at most n − 1 steps ahead. The second pass makes every element of the first pass "visible" to the indices near the end.

**Q2. Why push only during the first pass?**
> Each index should wait at most once. The second pass only resolves the elements that are still waiting.

### Follow-up questions

**F1. Next Greater Element III (LC 556): the next larger number with the same digits.**
> The next-permutation algorithm (a different technique): find the rightmost ascent, swap with the smallest larger digit to its right, reverse the suffix.

**F2. Circular daily temperatures (the distance around the circle).**
> The same two passes, with `answer[j] = (i − j)`, where i is the unwrapped index.

**F3. The previous greater element in a circular array.**
> Scan from right to left twice.

---

## 9. Running the file

```bash
java M03_NextGreaterElementII.java      # 4/4 tests passed (including 500 random arrays)
```

**One-line summary:** *"Walk the circle twice (i % n); push indices only in the first pass; pop and answer the smaller ones."*
