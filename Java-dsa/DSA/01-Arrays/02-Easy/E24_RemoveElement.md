# E24 · Remove Element

| | |
|---|---|
| **LeetCode** | [#27 Remove Element](https://leetcode.com/problems/remove-element/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers (slow/fast filter) / swap with the end |
| **Code** | [E24_RemoveElement.java](E24_RemoveElement.java) |
| **Run** | `java E24_RemoveElement.java` |

---

## 1. The problem

Remove all occurrences of `val` from `nums` **in place**. Return `k` = the number of elements that are **not** `val`. The first `k` slots must hold those elements; **any order is accepted**, and whatever is after position k doesn't matter.

| nums | val | k | first k slots (any order) |
|---|---|---|---|
| `[3, 2, 2, 3]` | 3 | 2 | `[2, 2]` |
| `[0,1,2,2,3,0,4,2]` | 2 | 5 | `[0, 1, 3, 0, 4]` |
| `[1, 1, 1]` | 1 | 0 | `[]` |

**Constraints:** `0 ≤ n ≤ 100`.

---

## 2. How to think about it

This is the third time you're seeing the **slow/fast write pointer** (after Move Zeroes E06 and Remove Duplicates E07):
- `i` (fast) reads every element.
- `k` (slow) is the next write position.
- Keep the elements that pass the test (`x != val`) by writing them at `k`.

Since order doesn't matter, there's also a second trick: **when you find `val`, overwrite it with the last element and shrink the array**. This does fewer writes when `val` is rare.

---

## 3. Java concepts used in this problem

### 3.1 Pre-decrement inside an index: `nums[--n]`

```java
nums[i] = nums[--n];
// is exactly:
n = n - 1;
nums[i] = nums[n];
```

`--n` **first** decreases `n`, **then** uses the new value. Compare `n--`, which uses the old value and then decreases. Here we want the **last valid** index, which is `n − 1`, and we also want `n` to shrink. `--n` does both in one step. (See E07 for `k++` versus `++k`.)

### 3.2 Testing when the order may differ

The two approaches leave the kept elements in different orders. The test helper `kept` copies the first k elements, **sorts** them and compares them as text, so any order passes. That's the same "check the property, not the exact layout" idea as E18.

---

## 4. Approach 1: Slow/fast filter (keeps the order)

### Code ([E24_RemoveElement.java:34-40](E24_RemoveElement.java#L34-L40))

```java
static int removeElement(int[] nums, int val) {
    int k = 0;
    for (int x : nums) {
        if (x != val) nums[k++] = x;
    }
    return k;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int k = 0;` | nothing kept yet |
| `for (int x : nums)` | read each element. Safe while writing, because `k ≤` the read position (we only overwrite slots already read) |
| `if (x != val) nums[k++] = x;` | keep the element: write it at k, then advance k |
| `return k;` | how many were kept |

### Dry run 1: `[3, 2, 2, 3]`, `val = 3`

| x | keep? | write | k after | array |
|---|---|---|---|---|
| 3 | no | — | 0 | `[3,2,2,3]` |
| 2 | yes | nums[0] = 2 | 1 | `[2,2,2,3]` |
| 2 | yes | nums[1] = 2 | 2 | `[2,2,2,3]` |
| 3 | no | — | 2 | |

**Return 2**; the first 2 slots are `[2, 2]` ✓

### Dry run 2: `[0, 1, 2, 2, 3, 0, 4, 2]`, `val = 2`

| x | keep? | k after | first k slots |
|---|---|---|---|
| 0 | yes | 1 | `[0]` |
| 1 | yes | 2 | `[0,1]` |
| 2 | no | 2 | |
| 2 | no | 2 | |
| 3 | yes | 3 | `[0,1,3]` |
| 0 | yes | 4 | `[0,1,3,0]` |
| 4 | yes | 5 | `[0,1,3,0,4]` |
| 2 | no | 5 | |

**Return 5** ✓

### Complexity
- **Time O(n)**, **Space O(1)**.
- **Writes:** one per kept element. If `val` is rare, that's almost n writes, even though almost nothing needed to move.

---

## 5. Approach 2: Swap with the end (fewer writes, order not kept)

### Code ([E24_RemoveElement.java:42-52](E24_RemoveElement.java#L42-L52))

```java
static int removeElementSwap(int[] nums, int val) {
    int i = 0, n = nums.length;
    while (i < n) {
        if (nums[i] == val) {
            nums[i] = nums[--n];       // bring the last element here, shrink the array
        } else {
            i++;
        }
    }
    return n;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int i = 0, n = nums.length;` | `i` scans; `n` is the current **logical length** (everything from n onward counts as removed) |
| `while (i < n)` | only scan the part that is still "in" the array |
| `if (nums[i] == val) nums[i] = nums[--n];` | drop `nums[i]` by overwriting it with the last element, and shrink the array by one |
| *(no `i++` in that branch)* | the element just moved into `i` **hasn't been checked yet**. It could also be `val` |
| `else i++;` | `nums[i]` is a keeper, so move on |
| `return n;` | the new length |

### Dry run 1: `[3, 2, 2, 3]`, `val = 3`

| step | i | n | nums[i] | action | array |
|---|---|---|---|---|---|
| 1 | 0 | 4 | 3 | == val → nums[0] = nums[3], n = 3 | `[3, 2, 2, 3]` |
| 2 | 0 | 3 | 3 | == val → nums[0] = nums[2], n = 2 | `[2, 2, 2, 3]` |
| 3 | 0 | 2 | 2 | keep, i = 1 | |
| 4 | 1 | 2 | 2 | keep, i = 2 | |
| — | 2 | 2 | | i < n fails → stop | |

**Return 2**; the first 2 slots are `[2, 2]` ✓. Step 2 shows why `i` doesn't advance: the swapped-in element was **another 3**.

### Dry run 2: `[0, 1, 2, 2, 3, 0, 4, 2]`, `val = 2`

| i | n | nums[i] | action | array (first n shown) |
|---|---|---|---|---|
| 0 | 8 | 0 | keep → i=1 | |
| 1 | 8 | 1 | keep → i=2 | |
| 2 | 8 | 2 | ← nums[7]=2, n=7 | `[0,1,2,2,3,0,4]` |
| 2 | 7 | 2 | ← nums[6]=4, n=6 | `[0,1,4,2,3,0]` |
| 2 | 6 | 4 | keep → i=3 | |
| 3 | 6 | 2 | ← nums[5]=0, n=5 | `[0,1,4,0,3]` |
| 3 | 5 | 0 | keep → i=4 | |
| 4 | 5 | 3 | keep → i=5 | |
| 5 | 5 | | stop | |

**Return 5**; the slots hold `[0, 1, 4, 0, 3]`, the same elements in a different order ✓

### Complexity
- **Time O(n)**, **Space O(1)**.
- **Writes:** one per **removed** element. That's much less than approach 1 when `val` is rare.

---

## 6. Comparing the approaches

| | Slow/fast | Swap with end |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(1) | O(1) |
| Keeps order? | **yes** | no |
| Writes | one per **kept** element | one per **removed** element |
| Best when | order matters / val is common | val is rare, order doesn't matter |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty array, all elements are `val`, none are `val`, `val` only at the end.

**Mistakes:**
1. Swap version: incrementing `i` after the swap, which skips checking the moved-in element.
2. Swap version: using `nums[n--]`, which reads index n (out of range, or a stale value).
3. Using `ArrayList.remove` in a loop: O(n²), plus index-shifting bugs.
4. Returning the array instead of `k`.

---

## 8. Interview questions and answers

**Q1. When would you choose the swap version?**
> When order doesn't matter and the value to remove is rare. It writes only once per removed element, while the filter version rewrites nearly every element.

**Q2. Why doesn't the swap version advance `i` after a swap?**
> The element copied from the end hasn't been checked. It might also equal `val`.

**Q3. The general pattern?**
> A "filter in place" with a write pointer: `for x in arr: if keep(x): arr[k++] = x; return k`. Move Zeroes, Remove Duplicates and Remove Element are all this with different `keep` rules.

### Follow-up questions

**F1. Remove all elements matching a predicate (e.g. negatives).**
> The same template with `keep(x) = x >= 0`. In Java collections: `list.removeIf(x -> x < 0)`. It's O(n) and uses the same idea internally.

**F2. Remove an element from a linked list (LC 203).** → [03-Linked-Lists E06](../../03-Linked-Lists/02-Easy/E06_RemoveLinkedListElements.md)
> Use a dummy head; for each node, unlink `cur.next` while it equals val.

**F3. Remove val, but report the removed count and keep the original order of the rest.**
> Filter version: removed = n − k.

**F4. Thread-safe removal while other threads read the array.**
> In-place compaction isn't safe for concurrent readers. Build a new array and publish it atomically (copy-on-write, like `CopyOnWriteArrayList`), or use locking.

---

## 9. Running the file

```bash
java E24_RemoveElement.java      # 4/4 tests passed
```

**One-line summary:** *"Filter with a write pointer (keeps order), or overwrite each val with the last element and shrink (fewer writes)."*
