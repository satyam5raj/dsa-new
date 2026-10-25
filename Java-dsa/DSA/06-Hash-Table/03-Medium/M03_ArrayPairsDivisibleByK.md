# M03 · Check If Array Pairs Are Divisible by k

| | |
|---|---|
| **LeetCode** | [#1497 Check If Array Pairs Are Divisible by k](https://leetcode.com/problems/check-if-array-pairs-are-divisible-by-k/) |
| **Difficulty** | Medium |
| **Pattern** | Count the remainders (hash by value mod k), then match complements |
| **Code** | [M03_ArrayPairsDivisibleByK.java](M03_ArrayPairsDivisibleByK.java) |
| **Run** | `java M03_ArrayPairsDivisibleByK.java` |

---

## 1. The problem

`arr` has even length n. Can it be split into n/2 pairs so that each pair's **sum** is divisible by k?

| arr | k | answer | pairs |
|---|---|---|---|
| `[1,2,3,4,5,10,6,7,8,9]` | 5 | true | (1,9) (2,8) (3,7) (4,6) (5,10) |
| `[1,2,3,4,5,6]` | 7 | true | (1,6) (2,5) (3,4) |
| `[1,2,3,4,5,6]` | 10 | false | |
| `[-1,1,-2,2,-3,3,-4,4]` | 3 | true | negatives! |
| `[2,2]` | 4 | true | 2 + 2 = 4 |

**Constraints:** `1 ≤ n ≤ 10⁵` (n even), `−10⁹ ≤ arr[i] ≤ 10⁹`, `1 ≤ k ≤ 10⁵`.

---

## 2. How to think about it

### Only the remainders matter
`(a + b) % k == 0` depends only on `a % k` and `b % k`. With remainders ra, rb in [0, k):
- `ra + rb == 0` (both 0), or
- `ra + rb == k`

So remainder **r must pair with remainder k − r**. This is Two Sum's "complement" idea ([Arrays E01](../../01-Arrays/02-Easy/E01_TwoSum.md)), applied to remainders.

### The counting conditions
Let `count[r]` = how many elements have remainder r.
- **r = 0** pairs with itself → `count[0]` must be **even**.
- **r and k − r** pair with each other → `count[r] == count[k − r]`.
- **r = k/2** (when k is even) pairs with itself → `count[k/2]` must be **even**. (The check `count[r] == count[k − r]` would be trivially true here, so test it explicitly.)

If all three hold, we can pair greedily; if any fails, some element is left without a partner.

```
[1,2,3,4,5,10,6,7,8,9], k = 5
remainders: 1 2 3 4 0 0 1 2 3 4    →   count = [2, 2, 2, 2, 2]
count[0] = 2 (even) ✓   count[1] = count[4] ✓   count[2] = count[3] ✓   → true
```

---

## 3. Java concepts used in this problem

### 3.1 `%` with negative numbers

In Java, `%` keeps the **sign of the dividend**:

```java
 7 % 5   →  2
-3 % 5   → -3      (not 2!)
-5 % 5   →  0
```

A remainder of −3 would be an invalid array index. Two fixes:

```java
((x % k) + k) % k        // shift into [0, k): -3 → (-3 + 5) % 5 = 2
Math.floorMod(x, k)      // the same result, built in
```

The outer `% k` matters for non-negative x: `(7 % 5 + 5) % 5 = 7 % 5 = 2`, whereas without it you'd get 7.

### 3.2 An array as a hash table for remainders

Remainders are always in `[0, k)`, so `int[] count = new int[k]` is a perfect "hash table": the key is the index, and no `HashMap` is needed.

### 3.3 `Math.floorMod` in the brute force

The backtracking checker uses `Math.floorMod(a[i] + a[j], k) == 0`. Since only "== 0" matters there, plain `%` would also work, but floorMod is the safe habit.

---

## 4. Approach 1: Backtracking (the file's test oracle)

### Code ([M03_ArrayPairsDivisibleByK.java:56-74](M03_ArrayPairsDivisibleByK.java#L56-L74))

```java
static boolean brute(int[] a, int k) {
    return pair(a, new boolean[a.length], k);
}

static boolean pair(int[] a, boolean[] used, int k) {
    int i = 0;
    while (i < a.length && used[i]) i++;
    if (i == a.length) return true;
    used[i] = true;
    for (int j = i + 1; j < a.length; j++) {
        if (!used[j] && Math.floorMod(a[i] + a[j], k) == 0) {
            used[j] = true;
            if (pair(a, used, k)) { used[i] = used[j] = false; return true; }
            used[j] = false;
        }
    }
    used[i] = false;
    return false;
}
```

| Line | What it does |
|---|---|
| the `while` | find the first unpaired element i |
| `if (i == a.length) return true;` | everyone is paired |
| the `for` loop | try every unpaired partner j whose sum with a[i] is divisible by k |
| `if (pair(...))` | recurse; on success, undo the marks (to leave `used` clean) and report true |
| `used[j] = false;` | that choice failed: try the next j |

Exponential in the worst case, so it's only for small random tests (backtracking gets its own topic: [14-Backtracking](../../14-Backtracking/README.md)).

---

## 5. Approach 2: Count the remainders (the file's solution)

### Code ([M03_ArrayPairsDivisibleByK.java:41-53](M03_ArrayPairsDivisibleByK.java#L41-L53))

```java
static boolean canArrange(int[] arr, int k) {
    int[] count = new int[k];
    for (int x : arr) count[((x % k) + k) % k]++;          // normalize negative remainders
    if (count[0] % 2 != 0) return false;
    for (int r = 1; r < k; r++) {
        if (2 * r == k) {
            if (count[r] % 2 != 0) return false;           // r pairs with itself
        } else if (count[r] != count[k - r]) {
            return false;
        }
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `count[((x % k) + k) % k]++` | bucket every element by its normalised remainder |
| `if (count[0] % 2 != 0)` | the remainder-0 elements pair among themselves, so there must be an even number |
| `if (2 * r == k)` | r = k/2: pairs with itself → must be even |
| `count[r] != count[k - r]` | every r needs an equal number of (k − r) partners |
| `return true` | everything can be matched |

(Each pair (r, k − r) is checked twice, once from each side. That's harmless; `r < k − r` would halve the loop.)

### Dry run 1: `[-1, 1, -2, 2, -3, 3, -4, 4]`, k = 3

| x | x % 3 | normalised |
|---|---|---|
| −1 | −1 | 2 |
| 1 | 1 | 1 |
| −2 | −2 | 1 |
| 2 | 2 | 2 |
| −3 | 0 | 0 |
| 3 | 0 | 0 |
| −4 | −1 | 2 |
| 4 | 1 | 1 |

count = [2, 3, 3]. count[0] = 2 even ✓; r = 1: count[1] = 3 = count[2] ✓; r = 2: count[2] = count[1] ✓ → **true**. (For example (−1, 1), (−2, 2), (−4, 4), (−3, 3).)

### Dry run 2: `[1, 2, 3, 4, 5, 6]`, k = 10
count[1..6] = 1 each, everything else 0. r = 1: count[1] = 1 ≠ count[9] = 0 → **false** ✓

### Dry run 3: `[2, 2]`, k = 4
count = [0, 0, 2, 0]. count[0] = 0 even; r = 1: count[1] = count[3] = 0; r = 2 = k/2: count[2] = 2, even ✓; r = 3: ok → **true** ✓

### Complexity
- **Time O(n + k).**
- **Space O(k).**

---

## 6. Comparing the approaches

| | Backtracking | Remainder counting |
|---|---|---|
| Time | exponential | **O(n + k)** |
| Space | O(n) | O(k) |

The file checks the counting version against backtracking on 500 random arrays (with negatives).

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 1 (everything works), negatives, even k with r = k/2, all elements divisible by k.

**Mistakes:**
1. Plain `x % k` with negative x → a negative index.
2. Forgetting the k/2 self-pairing case.
3. Forgetting that count[0] must be even.
4. Summing the pairs and checking `x + y` overflow: not needed here, since only the remainders are used.

---

## 8. Interview questions and answers

**Q1. Why is matching counts enough? Couldn't a greedy pairing fail?**
> Elements with the same remainder are interchangeable. If count[r] equals count[k − r] for every r (and the self-paired classes are even), we can pair the elements of class r with those of class k − r one-to-one, so a valid pairing exists. Conversely, any valid pairing forces exactly these equalities.

**Q2. How do you handle negative numbers?**
> Normalise the remainder into [0, k) with `((x % k) + k) % k` or `Math.floorMod(x, k)`.

### Follow-up questions

**F1. Count the pairs (i < j) with (a[i] + a[j]) % k == 0 (LC 1010-style).**
> One pass: for each x with remainder r, add `seen[(k − r) % k]` to the answer, then do `seen[r]++`.

**F2. Return the actual pairs.**
> Keep a list of the elements per remainder, then zip class r with class k − r (and pair class 0 and class k/2 within themselves).

**F3. Group into triples whose sum is divisible by k.**
> It's no longer a simple complement match; you'd need a DP or a search over the remainder counts.

---

## 9. Running the file

```bash
java M03_ArrayPairsDivisibleByK.java      # 6/6 tests passed (including 500 random arrays vs backtracking)
```

**One-line summary:** *"Count the elements by (normalised) remainder; remainder r must pair with k − r, and remainders 0 and k/2 must have even counts."*
