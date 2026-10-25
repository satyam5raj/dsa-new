# E07 · Remove Duplicates from Sorted Array (+ #80 "at most twice")

| | |
|---|---|
| **LeetCode** | [#26 Remove Duplicates from Sorted Array](https://leetcode.com/problems/remove-duplicates-from-sorted-array/), follow-up [#80](https://leetcode.com/problems/remove-duplicates-from-sorted-array-ii/) |
| **Difficulty** | Easy (#80 is Medium) |
| **Pattern** | Two pointers (slow/fast) + "sorted ⇒ duplicates are adjacent" |
| **Code** | [E07_RemoveDuplicatesFromSortedArray.java](E07_RemoveDuplicatesFromSortedArray.java) |
| **Run** | `java E07_RemoveDuplicatesFromSortedArray.java` |

---

## 1. The problem

`nums` is sorted in ascending order. Remove the duplicates **in place** so that each value appears only once, and return `k` = the number of unique values.

- The **first `k` slots** must hold the unique values in order.
- What's left after position `k` doesn't matter (LeetCode only checks the first `k`).

| nums | k | first k slots |
|---|---|---|
| `[1, 1, 2]` | 2 | `[1, 2]` |
| `[0,0,1,1,1,2,2,3,3,4]` | 5 | `[0, 1, 2, 3, 4]` |
| `[1, 2, 3]` | 3 | `[1, 2, 3]` |
| `[5, 5, 5]` | 1 | `[5]` |

**Constraints:** `1 ≤ n ≤ 3 × 10⁴`, sorted in non-decreasing order.

**Why return a number?** Java arrays can't shrink, so "removing" really means **packing the kept values at the front** and telling the caller how many are valid.

---

## 2. How to think about it

1. The array is **sorted**, so equal values sit **next to each other**. A value is "new" exactly when it differs from the **last value we kept**.
2. Use the slow/fast write pointer from E06:
   - `k` = the length of the unique prefix. `nums[0..k-1]` are the kept values. It starts at **1**, because the first element is always unique.
   - `i` scans from index 1.
   - If `nums[i] != nums[k-1]` (differs from the last kept value), write it at `nums[k]` and do `k++`.

```
  [ unique, kept | already-read leftovers | not read yet ]
    0 ...... k-1   k ............... i-1    i ....... n-1
```

---

## 3. Java concepts used in this problem

### 3.1 Post-increment inside an index: `nums[k++] = nums[i];`

```java
nums[k++] = nums[i];
// is exactly:
nums[k] = nums[i];
k = k + 1;
```

- `k++` (**post-increment**): the expression's value is the **old** `k`, then `k` grows by one.
- `++k` (**pre-increment**): `k` grows first, and the expression's value is the **new** `k`.

```java
int k = 5;
int a = k++;   // a = 5, k = 6
int b = ++k;   // k = 7, b = 7
```

It's a very common idiom for "append at position k". Avoid using `k++` twice in one expression; it gets confusing.

### 3.2 `Arrays.copyOf(array, newLength)`

```java
int[] firstK = Arrays.copyOf(nums, k);   // a NEW array holding nums[0..k-1]
```

- If `newLength` is smaller than the array, it **cuts off** the end; if larger, it **pads** with zeros.
- The test helper `uniquePrefix` uses it to check only the first k elements.
- Related: `Arrays.copyOfRange(arr, from, to)` copies `arr[from..to-1]`.

### 3.3 Short-circuit `||` (used in the #80 version)

```java
if (k < 2 || x != nums[k - 2]) ...
```

`||` evaluates **left to right** and **stops as soon as the result is known**. If `k < 2` is true, the whole condition is true, and Java **never evaluates** `nums[k - 2]`. That matters, because `nums[-1]` or `nums[-2]` would crash. The same goes for `&&`: if the left side is false, the right side is skipped. This "guard on the left" pattern is everywhere in DSA code.

### 3.4 Guard clause: `if (nums.length == 0) return 0;`

Handle the special case first and leave. The main logic can then assume `nums[0]` exists.

---

## 4. Approach 1: Using a set (extra space, simple)

```java
static int removeDuplicatesSet(int[] nums) {
    Set<Integer> seen = new LinkedHashSet<>();   // remembers insertion order
    for (int x : nums) seen.add(x);
    int k = 0;
    for (int x : seen) nums[k++] = x;
    return k;
}
```

- `LinkedHashSet` keeps the values in the order they were first added (a plain `HashSet` doesn't), so the output stays sorted.
- **O(n) time, O(n) space.** It works even for **unsorted** input, but it ignores the fact that the array is sorted, and it breaks the in-place, O(1)-space spirit.

---

## 5. Approach 2: Two pointers (optimal)

### Code ([E07_RemoveDuplicatesFromSortedArray.java:44-53](E07_RemoveDuplicatesFromSortedArray.java#L44-L53))

```java
static int removeDuplicates(int[] nums) {
    if (nums.length == 0) return 0;
    int k = 1;
    for (int i = 1; i < nums.length; i++) {
        if (nums[i] != nums[k - 1]) {      // a value we haven't kept yet
            nums[k++] = nums[i];
        }
    }
    return k;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (nums.length == 0) return 0;` | empty array → no unique values. It also makes `nums[0]` safe below |
| `int k = 1;` | `nums[0]` is always kept, so the unique prefix starts with length 1 |
| `for (int i = 1; ...)` | scan everything after the first element |
| `if (nums[i] != nums[k - 1])` | compare with the **last kept value** (`nums[k-1]`), not with `nums[i-1]`. Both work for a sorted array, but `k-1` is the version that generalises to #80 |
| `nums[k++] = nums[i];` | new value → append it to the unique prefix |
| `return k;` | the number of unique values |

### Dry run 1: `[0, 0, 1, 1, 1, 2, 2, 3, 3, 4]`

| i | nums[i] | nums[k−1] (last kept) | new? | write | k after | first k slots |
|---|---|---|---|---|---|---|
| start | | | | | 1 | `[0]` |
| 1 | 0 | 0 | no | — | 1 | `[0]` |
| 2 | 1 | 0 | **yes** | nums[1] = 1 | 2 | `[0,1]` |
| 3 | 1 | 1 | no | — | 2 | |
| 4 | 1 | 1 | no | — | 2 | |
| 5 | 2 | 1 | **yes** | nums[2] = 2 | 3 | `[0,1,2]` |
| 6 | 2 | 2 | no | — | 3 | |
| 7 | 3 | 2 | **yes** | nums[3] = 3 | 4 | `[0,1,2,3]` |
| 8 | 3 | 3 | no | — | 4 | |
| 9 | 4 | 3 | **yes** | nums[4] = 4 | 5 | `[0,1,2,3,4]` |

Final array: `[0, 1, 2, 3, 4, 2, 2, 3, 3, 4]`, **return 5**. Only the first 5 matter.

### Dry run 2: `[5, 5, 5]`

| i | nums[i] | last kept | new? | k |
|---|---|---|---|---|
| 1 | 5 | 5 | no | 1 |
| 2 | 5 | 5 | no | 1 |

**Return 1**, prefix `[5]`.

### Complexity
- **Time O(n):** one pass.
- **Space O(1).**

---

## 6. Follow-up #80: keep each value at most twice

### Code ([E07_RemoveDuplicatesFromSortedArray.java:56-62](E07_RemoveDuplicatesFromSortedArray.java#L56-L62))

```java
static int removeDuplicatesAtMostTwice(int[] nums) {
    int k = 0;
    for (int x : nums) {
        if (k < 2 || x != nums[k - 2]) nums[k++] = x;   // allowed if not equal to the one TWO back
    }
    return k;
}
```

### Why compare with `nums[k-2]`?
The kept prefix is sorted. If `x` equals the value **two positions back** in the kept prefix, then the last two kept values are both `x` (everything between them is equal, since it's sorted). Adding `x` again would make three. If `x` differs from `nums[k-2]`, at most one copy of `x` is kept so far, so adding it is fine.

### Line by line

| Line | What it does |
|---|---|
| `int k = 0;` | this time start from 0 and let the condition handle the first elements |
| `for (int x : nums)` | for-each is safe here: we write only at `k ≤` the current read position, so `x` was read before it could be overwritten |
| `k < 2 \|\| ...` | the first two elements are always allowed. Short-circuiting stops `nums[k-2]` from being read when k < 2 |
| `x != nums[k - 2]` | not equal to the value two back → fewer than 2 copies kept so far |
| `nums[k++] = x;` | keep it |

### Dry run: `[1, 1, 1, 2, 2, 3]`

| x | k | k < 2? | nums[k−2] | keep? | k after | kept prefix |
|---|---|---|---|---|---|---|
| 1 | 0 | yes | — | ✓ | 1 | `[1]` |
| 1 | 1 | yes | — | ✓ | 2 | `[1,1]` |
| 1 | 2 | no | nums[0] = 1 | 1 == 1 → ✗ | 2 | `[1,1]` |
| 2 | 2 | no | nums[0] = 1 | ✓ | 3 | `[1,1,2]` |
| 2 | 3 | no | nums[1] = 1 | ✓ | 4 | `[1,1,2,2]` |
| 3 | 4 | no | nums[2] = 2 | ✓ | 5 | `[1,1,2,2,3]` |

**Return 5.**

**Generalisation:** to keep at most **m** copies, use `if (k < m || x != nums[k - m])`. With m = 1, this is exactly #26.

---

## 7. Comparing the approaches

| | LinkedHashSet | Two pointers |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(n) | **O(1)** |
| Needs sorted input? | no | **yes** |

---

## 8. Edge cases and common mistakes

**Edge cases:** empty array, a single element, all the same, all distinct, negative values.

**Mistakes:**
1. Starting `k` at 0 in #26 and then reading `nums[k-1]` → `nums[-1]` crash.
2. Returning the array instead of `k` (LeetCode's checker needs `k`).
3. In #80, comparing with `nums[i-2]` (the **original** array) instead of `nums[k-2]` (the **kept** prefix). After writes, `nums[i-2]` may already have been overwritten, which gives wrong results.
4. Trying to actually "delete" elements by shifting everything left each time: O(n²).

---

## 9. Interview questions and answers

**Q1. Why compare with `nums[k-1]` instead of `nums[i-1]`?**
> For #26 both work, since in a sorted array a new value differs from both. But `nums[k-1]` is the last value I actually *kept*, which is the right thing to compare against, and it generalises to "at most m copies" with `nums[k-m]`.

**Q2. Why is this O(1) space even though the array is "changed"?**
> We reuse the input array's own memory. "Extra space" counts memory beyond the input, and here that's just two integers.

**Q3. What if the array is not sorted?**
> Then duplicates aren't adjacent. Either sort first (O(n log n), but the order changes) or use a HashSet with the same write pointer: write `x` only if `seen.add(x)` returns true (O(n) time, O(n) space, keeps first occurrences in order).

### Follow-up questions

**F1. Keep at most m copies of each value.**
> `if (k < m || x != nums[k - m]) nums[k++] = x;`.

**F2. Remove **all** copies of values that appear more than once (keep only the values that were unique to begin with).** (like LC 82 on linked lists)
> Scan groups: for each run of equal values, measure its length and keep the value only if the length is 1.

**F3. Do it on a sorted linked list.** → [03-Linked-Lists E08](../../03-Linked-Lists/02-Easy/E08_RemoveDuplicatesFromSortedList.md)
> Walk with one pointer: while `cur.next != null && cur.next.val == cur.val`, unlink with `cur.next = cur.next.next`.

**F4. Count the distinct values without modifying the array.**
> Count the positions where `nums[i] != nums[i-1]`, plus 1.

---

## 10. Running the file

```bash
java E07_RemoveDuplicatesFromSortedArray.java      # 6/6 tests passed
```

The tests use `uniquePrefix`, which runs `removeDuplicates` and then `Arrays.copyOf(nums, k)`, so they can compare just the meaningful part.

**One-line summary:** *"Sorted ⇒ duplicates are neighbours; write a value only when it differs from the last one kept."*
