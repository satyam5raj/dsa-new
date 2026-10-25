# M02 · Rotate Array

| | |
|---|---|
| **LeetCode** | [#189 Rotate Array](https://leetcode.com/problems/rotate-array/) |
| **Difficulty** | Medium |
| **Pattern** | Reversal trick (in place) |
| **Code** | [M02_RotateArray.java](M02_RotateArray.java) |
| **Run** | `java M02_RotateArray.java` |

---

## 1. The problem

Rotate the array to the **right** by `k` steps, **in place**. Each step moves every element one position right, and the last element wraps around to the front.

| nums | k | answer |
|---|---|---|
| `[1, 2, 3, 4, 5, 6, 7]` | 3 | `[5, 6, 7, 1, 2, 3, 4]` |
| `[-1, -100, 3, 99]` | 2 | `[3, 99, -1, -100]` |
| `[1, 2, 3]` | 10 | `[3, 1, 2]` (10 % 3 = 1) |
| `[1, 2, 3]` | 3 | `[1, 2, 3]` (a full turn) |

**Constraints:** `1 ≤ n ≤ 10⁵`, `0 ≤ k ≤ 10⁵`. LeetCode's follow-up: *at least three ways, one of them in O(1) extra space*.

---

## 2. How to think about it

**Step 0: reduce k.** Rotating by `n` gives the original array, so only `k % n` matters. Without this, k > n breaks the index logic.

Then three ideas:
1. **Rotate by one, k times.** Each step moves all n elements: O(n·k). Too slow when n and k are both 10⁵.
2. **Extra array.** The element at index `i` goes to `(i + k) % n`. O(n) time, O(n) space.
3. **Triple reverse** (the trick). A right rotation by k moves the **last k elements to the front**, keeping their order:

```
[1 2 3 4 | 5 6 7]   →   [5 6 7 | 1 2 3 4]
   A          B              B       A
```

   - Reverse **everything**: `[7 6 5 | 4 3 2 1]`. Now block B is at the front and A at the back, but both are backwards.
   - Reverse the first k: `[5 6 7 | 4 3 2 1]`, which fixes B.
   - Reverse the rest: `[5 6 7 | 1 2 3 4]`, which fixes A. Done, in O(n) time with O(1) space.

In math notation: `rotate(AB) = BA = (AᴿBᴿ)ᴿ`, where ᴿ means reversed.

---

## 3. Java concepts used in this problem

### 3.1 Modulo `%` for wrap-around

```java
k %= n;                  // k = k % n: 10 % 3 = 1
out[(i + k) % n] = ...;  // indices past the end wrap back to the start
```

`%` is how you handle **circular** structures: ring buffers, clock arithmetic, circular queues (05-Queue). With i = 5, k = 3, n = 7: `(5 + 3) % 7 = 1`, so the element at index 5 lands at index 1.

### 3.2 A helper method with index bounds

```java
static void reverse(int[] a, int i, int j) {   // reverses a[i..j] in place
    while (i < j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
        i++; j--;
    }
}
```

- It works on any **sub-range**, which is what makes it reusable three times.
- `i` and `j` are **copies** of the caller's values (primitives are passed by value), so moving them inside the method doesn't affect the caller. But the array **contents** are changed, because the array is shared (see E03).
- Several statements on one line (`i++; j--;`) is fine for tiny, related steps.

### 3.3 Early return for "nothing to do"

`if (k == 0) return;` handles k being a multiple of n. It isn't strictly needed (reversing twice would restore the array), but it's clean and saves work. Without it, `reverse(nums, 0, k − 1)` with k = 0 would be `reverse(0, −1)`, which does nothing because `0 < −1` is false. Still safe, but the guard makes the intent clear.

---

## 4. Approach 1: Rotate by one step, k times

```java
static void rotateOneByOne(int[] nums, int k) {
    int n = nums.length;
    k %= n;
    for (int step = 0; step < k; step++) {
        int last = nums[n - 1];
        for (int i = n - 1; i > 0; i--) nums[i] = nums[i - 1];   // shift right by one
        nums[0] = last;
    }
}
```

**Dry run** `[1,2,3]`, k = 1: last = 3 → shift → `[1,1,2]` → `nums[0] = 3` → `[3,1,2]` ✓
**O(n·k) time** (up to 10¹⁰, which times out), **O(1) space.**

---

## 5. Approach 2: Extra array

### Code ([M02_RotateArray.java:55-60](M02_RotateArray.java#L55-L60))

```java
static int[] rotateWithExtraArray(int[] nums, int k) {
    int n = nums.length;
    int[] out = new int[n];
    for (int i = 0; i < n; i++) out[(i + k) % n] = nums[i];
    return out;
}
```

| Line | What it does |
|---|---|
| `int[] out = new int[n];` | the destination |
| `out[(i + k) % n] = nums[i];` | the element at i moves k steps right, wrapping around |

(This version returns a new array for testing. To satisfy "in place", finish with `System.arraycopy(out, 0, nums, 0, n)`.) Note that `(i + k) % n` handles k > n by itself, so no `k %= n` is needed here.

**Dry run** `[1..7]`, k = 3:

| i | nums[i] | (i+3) % 7 | out |
|---|---|---|---|
| 0 | 1 | 3 | `[_,_,_,1,_,_,_]` |
| 1 | 2 | 4 | `[_,_,_,1,2,_,_]` |
| 2 | 3 | 5 | `[_,_,_,1,2,3,_]` |
| 3 | 4 | 6 | `[_,_,_,1,2,3,4]` |
| 4 | 5 | 0 | `[5,_,_,1,2,3,4]` |
| 5 | 6 | 1 | `[5,6,_,1,2,3,4]` |
| 6 | 7 | 2 | `[5,6,7,1,2,3,4]` ✓ |

**O(n) time, O(n) space.**

---

## 6. Approach 3: Triple reverse (the file's solution)

### Code ([M02_RotateArray.java:38-52](M02_RotateArray.java#L38-L52))

```java
static void rotate(int[] nums, int k) {
    int n = nums.length;
    k %= n;
    if (k == 0) return;
    reverse(nums, 0, n - 1);
    reverse(nums, 0, k - 1);
    reverse(nums, k, n - 1);
}

static void reverse(int[] a, int i, int j) {
    while (i < j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
        i++; j--;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `k %= n;` | ignore full turns |
| `if (k == 0) return;` | nothing to rotate |
| `reverse(nums, 0, n - 1);` | the whole array: the last k elements are now at the front (backwards) |
| `reverse(nums, 0, k - 1);` | fix the order of the first k |
| `reverse(nums, k, n - 1);` | fix the order of the remaining n − k |
| `reverse(...)`: `while (i < j)` swap the ends and move inward | the classic in-place reversal: ⌊len/2⌋ swaps |

### Dry run 1: `[1, 2, 3, 4, 5, 6, 7]`, k = 3

| step | call | array after |
|---|---|---|
| start | | `[1, 2, 3, 4, 5, 6, 7]` |
| 1 | reverse(0, 6) | `[7, 6, 5, 4, 3, 2, 1]` |
| 2 | reverse(0, 2) | `[5, 6, 7, 4, 3, 2, 1]` |
| 3 | reverse(3, 6) | `[5, 6, 7, 1, 2, 3, 4]` ✓ |

Inside step 1, `reverse(0, 6)`: swap(0,6) → swap(1,5) → swap(2,4) → i = j = 3, stop. That's 3 swaps.

### Dry run 2: `[-1, -100, 3, 99]`, k = 2

| call | array |
|---|---|
| reverse(0, 3) | `[99, 3, -100, -1]` |
| reverse(0, 1) | `[3, 99, -100, -1]` |
| reverse(2, 3) | `[3, 99, -1, -100]` ✓ |

### Dry run 3: `[1, 2, 3]`, k = 10
k = 10 % 3 = 1 → reverse all `[3,2,1]` → reverse(0,0): nothing → reverse(1,2): `[3,1,2]` ✓

### Complexity
- **Time O(n):** each element is swapped about twice in total (n/2 + k/2 + (n−k)/2 = n swaps).
- **Space O(1).**

---

## 7. (Bonus) Approach 4: Cyclic replacements (also O(1) space)

Move each element directly to its final place, carrying the displaced element along, until you return to the start of the cycle. There are `gcd(n, k)` cycles.

```java
static void rotateCyclic(int[] nums, int k) {
    int n = nums.length;
    k %= n;
    int moved = 0;
    for (int start = 0; moved < n; start++) {
        int cur = start, carry = nums[start];
        do {
            int next = (cur + k) % n;
            int tmp = nums[next];
            nums[next] = carry;            // drop the carried value into its home
            carry = tmp;                   // pick up the displaced one
            cur = next;
            moved++;
        } while (cur != start);            // the cycle closed
    }
}
```

- A `do { } while (cond);` loop runs the body **at least once** and checks the condition at the end. Here we must move at least one element before checking "are we back at the start?".
- Each element is written exactly once: n writes in total, the minimum possible.
- It's trickier to get right in an interview; triple reverse is the safer choice to present.

**Dry run** `[1,2,3,4,5,6]`, k = 2 (gcd = 2, so two cycles): cycle from 0: 0→2→4→0 places 1, 3, 5; cycle from 1: 1→3→5→1 places 2, 4, 6. Result `[5,6,1,2,3,4]` ✓

---

## 8. Comparing the approaches

| | One by one | Extra array | Triple reverse | Cyclic |
|---|---|---|---|---|
| Time | O(n·k) | O(n) | **O(n)** | O(n) |
| Extra space | O(1) | O(n) | **O(1)** | O(1) |
| Writes | n·k | 2n | 2n | **n** |
| Interview risk | — | low | **low** | higher |

---

## 9. Edge cases and common mistakes

**Edge cases:** k = 0, k = n, k > n, n = 1, k = n − 1.

**Mistakes:**
1. Forgetting `k %= n`, so `reverse(0, k − 1)` goes out of bounds when k > n.
2. Mixing up the left rotation order: for a **left** rotation by k, reverse the first k, then the rest, then all (or right-rotate by n − k).
3. Writing `out[i] = nums[(i + k) % n]`, which is a **left** rotation. Ask yourself which direction you want.
4. Cyclic version: counting cycles wrongly (looping until `cur != start` without a `moved` counter misses cycles when gcd > 1).

---

## 10. Interview questions and answers

**Q1. Why does the triple reverse work?**
> Write the array as AB, where B is the last k elements. We want BA. Reversing the whole thing gives BᴿAᴿ, so the blocks are in the right places but each is internally reversed. Reversing each block restores its order: BA.

**Q2. Why reduce k modulo n?**
> A rotation by n is the identity, so only k mod n matters. It also keeps the indices valid.

**Q3. How would you do a left rotation?**
> Left-rotate by k = right-rotate by n − k. Or reverse the first k, then the last n − k, then the whole array.

### Follow-up questions

**F1. Rotate a linked list by k (LC 61).** → [03-Linked-Lists M06](../../03-Linked-Lists/03-Medium/M06_RotateList.md)
> Find the length n and make the list circular; the new tail is at position n − k%n − 1. Break the ring there.

**F2. Rotate a matrix 90° (LC 48).** → [M08 Rotate Image](M08_RotateImage.md)
> Transpose, then reverse each row. It's the same "a rotation is a composition of reflections" idea in 2D.

**F3. Reverse the words in a string (LC 151).** → [02-Strings M12](../../02-Strings/03-Medium/M12_ReverseWordsInString.md)
> Reverse the whole string, then reverse each word: the same trick on characters.

**F4. Check whether one array is a rotation of another / string rotation (LC 796).**
> `B` is a rotation of `A` iff `len(A) == len(B)` and `B` is a substring of `A + A`.

**F5. Search in a rotated sorted array (LC 33).**
> Modified binary search: at each step, one half is sorted; check whether the target lies in that half.

---

## 11. Running the file

```bash
java M02_RotateArray.java      # 5/5 tests passed
```

**One-line summary:** *"k %= n; reverse all, reverse the first k, reverse the rest."*
