# E03 · Next Greater Element I

| | |
|---|---|
| **LeetCode** | [#496 Next Greater Element I](https://leetcode.com/problems/next-greater-element-i/) |
| **Difficulty** | Easy |
| **Pattern** | Monotonic stack + HashMap (your first monotonic stack problem) |
| **Code** | [E03_NextGreaterElementI.java](E03_NextGreaterElementI.java) |
| **Run** | `java E03_NextGreaterElementI.java` |

---

## 1. The problem

`nums1` is a subset of `nums2` (all the values are distinct). For each x in `nums1`, find the **first element to the right of x in nums2 that is greater than x**, or `-1`.

```
nums1 = [4, 1, 2],  nums2 = [1, 3, 4, 2]   →   [-1, 3, -1]
  4: nothing greater after it in nums2   → -1
  1: the next greater is 3
  2: nothing after it                    → -1
```

| nums1 | nums2 | answer |
|---|---|---|
| `[4,1,2]` | `[1,3,4,2]` | `[-1,3,-1]` |
| `[2,4]` | `[1,2,3,4]` | `[3,-1]` |
| `[3,1]` | `[3,2,1]` | `[-1,-1]` |

**Constraints:** `1 ≤ m ≤ n ≤ 1000`, distinct values. Follow-up: *O(m + n)*.

---

## 2. How to think about it

1. **Brute force:** for each x, find it in nums2 and scan to the right: O(m · n).
2. **Monotonic stack:** compute the next greater element for **every** value of nums2 in **one** pass, store it in a map, then answer nums1 by lookup.

### How the monotonic stack works
The stack holds the values that are **still waiting for their answer**. They're always in **decreasing** order from bottom to top (a smaller value on top of a larger one).

For each new x:
- while the top is **smaller** than x: x is the top's next greater element → record it and pop
- push x (it waits for its own answer)

Anything still in the stack at the end has no greater element → −1.

**Why does it stay decreasing?** We only push x after popping everything smaller than it, so x sits on top of something bigger (or on an empty stack).

---

## 3. Java concepts used in this problem

### 3.1 A monotonic stack

A **monotonic stack** keeps its elements in sorted order by popping the elements that would break the order **before** pushing. The moment an element is popped is the moment you learn its answer:

| stack order (bottom → top) | pop when the new x is… | answers |
|---|---|---|
| **decreasing** | greater | **next greater** element |
| **increasing** | smaller | **next smaller** element |

You'll use it in Daily Temperatures, Stock Span, Largest Rectangle, Trapping Rain Water, Sum of Subarray Minimums, and more.

### 3.2 `HashMap` for answers keyed by value

`nextGreater.put(value, answer)` works because the values are **distinct**. Afterwards, `getOrDefault(x, -1)` handles the values that never got an answer.

### 3.3 `stack.peek() < x` compares `Integer` with `int`

`peek()` returns an `Integer`, which is unboxed for the comparison, so this is safe.

### 3.4 Amortised O(n) again

Each value is pushed once and popped at most once, so the inner `while` loop runs at most n times **in total** across the whole scan (as in [Arrays H02](../../01-Arrays/04-Hard/H02_SlidingWindowMaximum.md)).

---

## 4. Approach 1: Brute force

```java
static int[] nextGreaterBrute(int[] nums1, int[] nums2) {
    int[] ans = new int[nums1.length];
    for (int i = 0; i < nums1.length; i++) {
        ans[i] = -1;
        int j = 0;
        while (nums2[j] != nums1[i]) j++;            // find x in nums2
        for (int k = j + 1; k < nums2.length; k++) { // scan to the right
            if (nums2[k] > nums1[i]) { ans[i] = nums2[k]; break; }
        }
    }
    return ans;
}
```

**O(m · n) time, O(1) extra space.**

---

## 5. Approach 2: Monotonic stack + map (the file's solution)

### Code ([E03_NextGreaterElementI.java:44-56](E03_NextGreaterElementI.java#L44-L56))

```java
static int[] nextGreaterElement(int[] nums1, int[] nums2) {
    Map<Integer, Integer> nextGreater = new HashMap<>();
    Deque<Integer> stack = new ArrayDeque<>();
    for (int x : nums2) {
        while (!stack.isEmpty() && stack.peek() < x) {
            nextGreater.put(stack.pop(), x);
        }
        stack.push(x);
    }
    int[] ans = new int[nums1.length];
    for (int i = 0; i < nums1.length; i++) ans[i] = nextGreater.getOrDefault(nums1[i], -1);
    return ans;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Map<Integer, Integer> nextGreater` | value → its next greater element |
| `Deque<Integer> stack` | the values waiting for an answer, decreasing from bottom to top |
| `while (!stack.isEmpty() && stack.peek() < x)` | x is greater than the waiting top |
| `nextGreater.put(stack.pop(), x);` | so x is the top's answer; resolve and remove it |
| `stack.push(x);` | x now waits for something greater |
| `getOrDefault(nums1[i], -1)` | the values never resolved have no greater element |

### Dry run: `nums2 = [1, 3, 4, 2]`

| x | pops (value → answer) | stack after (bottom → top) |
|---|---|---|
| 1 | — | [1] |
| 3 | 1 → 3 | [3] |
| 4 | 3 → 4 | [4] |
| 2 | — (4 > 2) | [4, 2] |

The map is `{1: 3, 3: 4}`; 4 and 2 are left over (−1). For `nums1 = [4, 1, 2]` → **`[-1, 3, -1]`** ✓

### Dry run 2: `nums2 = [1, 3, 5, 2, 4]`

| x | pops | stack after |
|---|---|---|
| 1 | — | [1] |
| 3 | 1 → 3 | [3] |
| 5 | 3 → 5 | [5] |
| 2 | — | [5, 2] |
| 4 | 2 → 4 | [5, 4] |

Map `{1:3, 3:5, 2:4}` → for nums1 = nums2: **`[3, 5, -1, 4, -1]`** ✓

### Complexity
- **Time O(n + m)**: one pass over nums2 (amortised), one over nums1.
- **Space O(n)** for the map and the stack.

---

## 6. Comparing the approaches

| | Brute force | Monotonic stack |
|---|---|---|
| Time | O(m · n) | **O(m + n)** |
| Space | O(1) | O(n) |

---

## 7. Edge cases and common mistakes

**Edge cases:** nums2 strictly decreasing (all −1), strictly increasing (each answer is the next element), a single element.

**Mistakes:**
1. Using `<=` with duplicates (they don't occur here, but in the general version equal values are **not** "greater").
2. Iterating over nums1 to build the stack (the stack must run over nums2).
3. Forgetting the default −1 for the values left in the stack.

---

## 8. Interview questions and answers

**Q1. Explain the monotonic stack in one sentence.**
> Keep a decreasing stack of the values still waiting for a greater element; each new value pops (and answers) every smaller value on top, then waits itself.

**Q2. Why is it O(n) with a nested while loop?**
> Each element is pushed once and popped at most once, so the total number of pops over the whole loop is at most n.

**Q3. How would you find the next **smaller** element?**
> Keep an increasing stack and pop while the top is **greater** than the new value.

### Follow-up questions

**F1. Next Greater Element II (LC 503): a circular array.** → [M03](../03-Medium/M03_NextGreaterElementII.md)
> Loop over indices `0..2n−1` using `i % n`, and store **indices** in the stack.

**F2. Daily Temperatures (LC 739): how many days until a warmer day?** → [M02](../03-Medium/M02_DailyTemperatures.md)
> The same stack, storing **indices**, with the answer `i − popped index`.

**F3. Previous greater element.**
> Scan from left to right; after popping the smaller elements, the top (if any) is the previous greater element.

**F4. Next greater element with duplicates.**
> Store indices instead of values (the map from values breaks with duplicates).

---

## 9. Running the file

```bash
java E03_NextGreaterElementI.java      # 4/4 tests passed
```

**One-line summary:** *"Decreasing stack over nums2: each new x pops (and answers) the smaller values; look the answers up for nums1."*
