# E01 · Two Sum

| | |
|---|---|
| **LeetCode** | [#1 Two Sum](https://leetcode.com/problems/two-sum/) |
| **Difficulty** | Easy |
| **Pattern** | Hashing (complement lookup) |
| **Code** | [E01_TwoSum.java](E01_TwoSum.java) |
| **Run** | `java E01_TwoSum.java` |

---

## 1. The problem

You get an integer array `nums` and an integer `target`. Return the **indices** of the two numbers that add up to `target`.

- Exactly one valid answer exists.
- You may **not** use the same element twice.
- You can return the two indices in any order (our code returns the smaller index first).

| nums | target | answer | why |
|---|---|---|---|
| `[2, 7, 11, 15]` | 9 | `[0, 1]` | 2 + 7 = 9 |
| `[3, 2, 4]` | 6 | `[1, 2]` | 2 + 4 = 6. **Not** `[0, 0]`: you can't use the 3 twice |
| `[3, 3]` | 6 | `[0, 1]` | two *different* 3s, so this is allowed |

**Constraints (LeetCode):** `2 ≤ nums.length ≤ 10⁴`, values and target fit in an `int`, exactly one answer.

> Watch out: the answer is **indices**, not values. Returning `[2, 7]` for the first example is wrong.

---

## 2. How to think about it

1. **Simplest idea:** try every pair `(i, j)` and check `nums[i] + nums[j] == target`. That's the brute force.
2. **Find the bottleneck:** for each number `x`, the inner loop is really asking one question: *"is the value `target − x` somewhere in the array?"* That search takes O(n) time.
3. **Insight:** "Is this value present, and where?" is exactly what a **HashMap** answers in O(1). So store each value with its index as you go, and ask the map instead of scanning.
4. **The pair we need:** for `x`, the partner is `complement = target − x`.

This "**brute force → bottleneck → right data structure**" chain is how you should talk in an interview.

---

## 3. Java concepts used in this problem

### 3.1 Arrays: `int[]`, `nums.length`, `nums[i]`

```java
int[] nums = {2, 7, 11, 15};
nums.length   // 4. It's a field, NOT a method, so no ()  (String uses .length())
nums[0]       // 2. Indices go from 0 to length-1
nums[4]       // crash: ArrayIndexOutOfBoundsException
```

An array is a **fixed-size** block of memory where the elements sit next to each other. Reading `nums[i]` is O(1) because Java can compute the address directly: `start + i × 4 bytes`.

### 3.2 Creating and returning a small array: `new int[]{i, j}` and `new int[0]`

```java
return new int[]{i, j};   // creates an array of length 2 holding i and j, and returns it
return new int[0];        // an EMPTY array (length 0). Means "no answer"
```

- `new int[]{...}` is an **array literal**: Java counts the elements and builds the array.
- The short form `{i, j}` (without `new int[]`) only works when you declare a variable (`int[] a = {i, j};`). In a `return` statement you need the full `new int[]{i, j}`.
- Returning `new int[0]` instead of `null` is a safe habit: the caller can still call `.length` without a `NullPointerException`.

### 3.3 `HashMap`: the star of this problem

A **HashMap** stores **key → value** pairs and finds a key in about **O(1)** time, no matter how many entries it holds.

```java
Map<Integer, Integer> indexOf = new HashMap<>();
```

Reading this declaration piece by piece:

| Piece | Meaning |
|---|---|
| `Map<Integer, Integer>` | the **type of the variable**: a map whose keys are `Integer` and whose values are `Integer` |
| `indexOf` | the variable name. Reads naturally: `indexOf.get(7)` means "the index of 7" |
| `new HashMap<>()` | creates the actual object. `<>` (the "diamond") tells Java to work out the types from the left side |

**Why `Map` on the left and `HashMap` on the right?** `Map` is an **interface** (a contract: "I support put/get/containsKey"). `HashMap` is one **implementation** of it. Declaring the variable as `Map` is good practice, because you could switch to `TreeMap` later without changing any other code.

**Why `Integer` and not `int`?** Java collections can only hold **objects**, not primitives. `Integer` is the object "wrapper" around `int`. Java converts between them automatically:
- **Autoboxing:** `indexOf.put(2, 0)` turns the `int` values 2 and 0 into `Integer` objects.
- **Unboxing:** `int idx = indexOf.get(2);` turns the `Integer` back into an `int`.

**How it works inside (simplified):**
1. A HashMap holds an internal array of "buckets" (16 by default).
2. To store key `7`, it computes `7.hashCode()` (for an Integer that's just 7) and picks a bucket from that number.
3. To look up `7` later, it does the same calculation, goes straight to that bucket and checks the few entries there. No scanning of the whole map.
4. When the map gets about 75% full, it **doubles** its bucket array and redistributes the entries. That's why operations are *average* (amortized) O(1), like `StringBuilder.append`.

**Methods used here:**

| Method | What it does | Cost |
|---|---|---|
| `put(key, value)` | store (or overwrite) `key → value` | O(1) average |
| `containsKey(key)` | `true` if the key is present | O(1) average |
| `get(key)` | the value for `key`, or **`null`** if missing | O(1) average |

Other handy ones you'll meet soon: `getOrDefault(key, def)`, `remove(key)`, `size()`, `merge(key, 1, Integer::sum)` (counting).

> **Pitfall:** `int idx = map.get(k);` when `k` is missing → `get` returns `null`, unboxing `null` throws a **NullPointerException**. That's why the code checks `containsKey` first.

### 3.4 `static` methods

`static int[] twoSum(...)` belongs to the **class**, not to an object. So `main` (also static) can call `twoSum(...)` directly without writing `new E01_TwoSum()`. Every problem file in this course uses this style.

---

## 4. Approach 1: Brute force (try every pair)

### Idea
Check every pair `(i, j)` with `i < j`. The first pair whose sum equals `target` is the answer.

### Code ([E01_TwoSum.java:50-57](E01_TwoSum.java#L50-L57))

```java
static int[] twoSumBrute(int[] nums, int target) {
    for (int i = 0; i < nums.length; i++) {
        for (int j = i + 1; j < nums.length; j++) {   // j starts after i: no reuse, no repeats
            if (nums[i] + nums[j] == target) return new int[]{i, j};
        }
    }
    return new int[0];
}
```

### Line by line

| Line | What it does |
|---|---|
| `static int[] twoSumBrute(int[] nums, int target)` | a method that takes the array and target and returns an `int[]` holding two indices |
| `for (int i = 0; i < nums.length; i++)` | **outer loop**: `i` chooses the **first** element of the pair, going through every index |
| `for (int j = i + 1; j < nums.length; j++)` | **inner loop**: `j` chooses the **second** element. It starts at `i + 1` for two reasons: (1) `j` is never equal to `i`, so no element is used twice; (2) we never check `(1,0)` after already checking `(0,1)` |
| `if (nums[i] + nums[j] == target)` | does this pair add up to the target? |
| `return new int[]{i, j};` | yes: build `[i, j]` and return. `return` leaves **both** loops at once |
| `return new int[0];` | only reached if no pair matched. The problem promises an answer, but Java requires a return value on every path |

### Dry run 1: `nums = [3, 2, 4]`, `target = 6`

```
index:  0  1  2
nums:  [3, 2, 4]
```

| step | i | j | nums[i] | nums[j] | sum | == 6? |
|---|---|---|---|---|---|---|
| 1 | 0 | 1 | 3 | 2 | 5 | no |
| 2 | 0 | 2 | 3 | 4 | 7 | no |
| 3 | 1 | 2 | 2 | 4 | **6** | **yes → return `[1, 2]`** |

Notice `(0, 0)`, where 3 + 3 = 6, is **never tested**. `j = i + 1` is what protects us from the wrong answer `[0, 0]`.

### Dry run 2: `nums = [2, 7, 11, 15]`, `target = 9`

| step | i | j | sum | == 9? |
|---|---|---|---|---|
| 1 | 0 | 1 | 2 + 7 = 9 | **yes → return `[0, 1]`** |

It finished after one check. That's the best case. The worst case is when the pair is at the very end, giving about n²/2 checks.

### Complexity
- **Time O(n²):** the number of pairs is n(n−1)/2. For n = 10⁴ that's about 50 million checks: slow, but it fits LeetCode's limit. For n = 10⁶ it would be about 5 × 10¹¹, which is hopeless.
- **Space O(1):** only the variables `i` and `j`.

---

## 5. Approach 2: One-pass HashMap (optimal)

### Idea
Walk through the array once. For each number `x` at index `i`:
1. Work out `complement = target − x`.
2. If the complement was **already seen**, the answer is `[index of complement, i]`.
3. Otherwise remember `x → i` in the map and move on.

### Code ([E01_TwoSum.java:60-70](E01_TwoSum.java#L60-L70))

```java
static int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> indexOf = new HashMap<>();      // value -> index where we saw it
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (indexOf.containsKey(complement)) {
            return new int[]{indexOf.get(complement), i}; // the earlier index first
        }
        indexOf.put(nums[i], i);                          // store AFTER checking
    }
    return new int[0];                                    // never happens per the problem
}
```

### Line by line

| Line | What it does |
|---|---|
| `Map<Integer, Integer> indexOf = new HashMap<>();` | empty map from **value → index**. The key is the value because that's what we **look up**. The stored item is the index because that's what we **return** |
| `for (int i = 0; i < nums.length; i++)` | a **single** pass over the array |
| `int complement = target - nums[i];` | the partner this number needs. target 9, current 2 → we need 7 |
| `if (indexOf.containsKey(complement))` | "have I seen the partner **earlier**?" O(1) |
| `return new int[]{indexOf.get(complement), i};` | found it. `get` gives the earlier index, `i` is the current one, so the smaller index comes first |
| `indexOf.put(nums[i], i);` | no partner yet, so remember this number. Some **later** number may need it |
| `return new int[0];` | unreachable for valid input |

### Why "check first, then store" matters
If we stored first, an element could match **itself**:

```
nums = [3, 2, 4], target = 6, i = 0
WRONG order: put(3 → 0), then complement = 3 → containsKey(3) is true → returns [0, 0]  ✗
RIGHT order: complement = 3 → map is empty → not found → then put(3 → 0)            ✓
```

Checking first guarantees the map only ever holds elements **before** index `i`.

### Dry run 1: `nums = [2, 7, 11, 15]`, `target = 9`

| i | nums[i] | complement = 9 − nums[i] | map before | complement in map? | action | map after |
|---|---|---|---|---|---|---|
| 0 | 2 | 7 | `{}` | no | put 2 → 0 | `{2:0}` |
| 1 | 7 | 2 | `{2:0}` | **yes, index 0** | **return `[0, 1]`** | — |

### Dry run 2: `nums = [3, 2, 4]`, `target = 6` (the self-reuse trap)

| i | nums[i] | complement = 6 − nums[i] | map before | in map? | action | map after |
|---|---|---|---|---|---|---|
| 0 | 3 | 3 | `{}` | no (the 3 is not stored yet) | put 3 → 0 | `{3:0}` |
| 1 | 2 | 4 | `{3:0}` | no | put 2 → 1 | `{3:0, 2:1}` |
| 2 | 4 | 2 | `{3:0, 2:1}` | **yes, index 1** | **return `[1, 2]`** | — |

### Dry run 3: duplicates, `nums = [3, 3]`, `target = 6`

| i | nums[i] | complement | map before | in map? | action |
|---|---|---|---|---|---|
| 0 | 3 | 3 | `{}` | no | put 3 → 0 → `{3:0}` |
| 1 | 3 | 3 | `{3:0}` | **yes, index 0** | **return `[0, 1]`** |

Here pairing a 3 with a 3 is correct, because they are different elements at different indices.

### Dry run 4: zeros, `nums = [0, 4, 3, 0]`, `target = 0`

| i | nums[i] | complement | map before | in map? | action |
|---|---|---|---|---|---|
| 0 | 0 | 0 | `{}` | no | put 0 → 0 |
| 1 | 4 | −4 | `{0:0}` | no | put 4 → 1 |
| 2 | 3 | −3 | `{0:0, 4:1}` | no | put 3 → 2 |
| 3 | 0 | 0 | `{0:0, 4:1, 3:2}` | **yes, index 0** | **return `[0, 3]`** |

**Your turn:** dry-run `nums = [-3, 4, 3, 90]`, `target = 0` on paper. Expected `[0, 2]`.

### Complexity
- **Time O(n):** one pass, and each `containsKey` / `get` / `put` is O(1) on average.
- **Space O(n):** in the worst case (answer at the very end) the map holds about n entries.

We **trade memory for speed**. That trade-off is the most common theme in DSA.

---

## 6. (Bonus) Approach 3: Sort + two pointers

Interviewers often ask *"Can you do it without extra hash memory?"* Here's that answer, and its catch.

```java
static int[] twoSumSorted(int[] nums, int target) {
    int n = nums.length;
    int[][] pairs = new int[n][2];             // [value, originalIndex]
    for (int i = 0; i < n; i++) pairs[i] = new int[]{nums[i], i};
    Arrays.sort(pairs, (a, b) -> Integer.compare(a[0], b[0]));   // sort by value

    int left = 0, right = n - 1;
    while (left < right) {
        int sum = pairs[left][0] + pairs[right][0];
        if (sum == target) {
            int a = pairs[left][1], b = pairs[right][1];
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        if (sum < target) left++;   // need a bigger sum  -> move the small end up
        else right--;               // need a smaller sum -> move the big end down
    }
    return new int[0];
}
```

- We must remember the **original indices** before sorting, because sorting moves the elements around. That's why we sort `[value, index]` pairs.
- `(a, b) -> Integer.compare(a[0], b[0])` is a **lambda** (a short unnamed function) that tells `sort` to order the pairs by value.
- **Two pointers** work because the array is sorted: if the sum is too small, the only way to grow it is to move `left` right; if it's too big, move `right` left.
- **Time O(n log n)** (the sort), **Space O(n)** (the pairs array). It isn't better than the HashMap here. It shines in **Two Sum II** (input already sorted, [M03](../03-Medium/M03_TwoSumII_SortedInput.md)), where the space is O(1).

---

## 7. Comparing the approaches

| | Brute force | HashMap (one pass) | Sort + two pointers |
|---|---|---|---|
| Time | O(n²) | **O(n)** | O(n log n) |
| Extra space | **O(1)** | O(n) | O(n) (to keep indices) |
| Prevents self-pairing by | `j = i + 1` | checking before `put` | `left < right` |
| When to use | tiny n, or as a first idea | **the default answer** | input already sorted (Two Sum II) |

---

## 8. Edge cases and common mistakes

**Edge cases to test:**
- Duplicates that form the answer: `[3,3]`, target 6 → `[0,1]`
- Zeros: `[0,4,3,0]`, target 0 → `[0,3]`
- Negatives: `[-3,4,3,90]`, target 0 → `[0,2]`
- The answer at the very end (worst case for both approaches)
- Minimum length 2: `[1,2]`, target 3 → `[0,1]`

**Common mistakes:**
1. Returning **values** instead of **indices**.
2. Calling `put` **before** checking, which pairs an element with itself.
3. Starting the inner loop at `j = 0` instead of `j = i + 1`.
4. Building the whole map first, then looking up. This still works if you add a `map.get(c) != i` check, but the one-pass version is cleaner.
5. Sorting `nums` directly and then returning indices from the **sorted** array. Those indices are wrong.

---

## 9. Interview questions and answers

**Q1. Walk me through your approach.**
> Brute force checks every pair in O(n²). The inner loop is just searching for `target − x`, so I replace the search with a HashMap lookup. One pass: for each `x`, if `target − x` is in the map I return both indices; otherwise I store `x → index`. O(n) time, O(n) space.

**Q2. Why do you check the map before inserting the current element?**
> So an element can't pair with itself. If I inserted first, then for `x = target/2` the lookup would find the element I just added.

**Q3. What happens with duplicate values? `put` overwrites the old index.**
> That's fine. If two copies of `x` form the answer (`x + x = target`), the second copy finds the first during the check, *before* the overwrite. If they don't form the answer, any one index for `x` is enough.

**Q4. Is HashMap really O(1)?**
> On average, yes. In the worst case, many keys could land in the same bucket. Since Java 8, a crowded bucket turns into a balanced tree, so the worst case is O(log n) per operation, not O(n). For integer keys this basically never happens in practice.

**Q5. Can you solve it in O(1) extra space?**
> Not in O(n) time for an unsorted array. O(1) space means brute force (O(n²)). Sorting in place would lose the original indices, and keeping them costs O(n). If the array is already sorted (Two Sum II), two pointers give O(n) time and O(1) space.

### Follow-up questions

**F1. Return all pairs instead of one. (values may repeat)**
> Use the map to count how many times each value has appeared so far (`value → count`). For each `x`, add `count[target − x]` to the answer, then increment `count[x]`. To list index pairs, store a list of indices per value. The output can be O(n²) in size (e.g. all elements equal), so time is O(n + output size).

**F2. The input is already sorted.** → Two Sum II
> Two pointers from both ends: O(n) time, O(1) space. See [M03_TwoSumII_SortedInput](../03-Medium/M03_TwoSumII_SortedInput.md).

**F3. Three numbers that sum to target / zero.** → 3Sum
> Sort, fix one element `nums[i]`, then run two-pointer Two Sum on the rest for `target − nums[i]`. O(n²). See [M05_ThreeSum](../03-Medium/M05_ThreeSum.md).

**F4. Count pairs whose sum is ≤ target / < target.**
> Sort, then use two pointers: if `nums[l] + nums[r] ≤ target`, every element between `l` and `r` pairs with `l`, so add `r − l` and move `l` right; otherwise move `r` left. O(n log n).

**F5. A stream of numbers arrives: design `add(x)` and `find(target)`.** (LeetCode 170)
> Keep a `value → count` map. `add` is O(1). `find` loops over the map's keys and checks whether `target − key` exists (it needs count ≥ 2 if it's the same key). That's O(n) per find. If finds are far more common than adds, you could store all pair sums instead, which makes `find` O(1) and `add` O(n).

**F6. The array is huge and doesn't fit in memory.**
> Sort it externally (external merge sort) and use two pointers, streaming from disk. Or split the values by hash into partitions, so that `x` and `target − x` always land in the same pair of partitions, and then solve each partition in memory.

**F7. Why not use `int[]` as a direct lookup table instead of HashMap?**
> That works only if the values are in a small known range (e.g. 0..1000): index = value, O(1) lookup with no hashing. Values here go up to ±10⁹, so the table would be far too big.

---

## 10. Running the file and how the tests work

```bash
cd Java-dsa/DSA/01-Arrays/02-Easy
java E01_TwoSum.java
```

Expected output:

```
   PASS  [2,7,11,15] t=9                  -> [0, 1]
   ...
   6/6 tests passed
```

`main` calls `check(label, expected, actual)` for each test case:
- The parameters are of type `Object`, so any value fits (`int[]`, `boolean`, `Integer`...).
- `fmt` converts the value to text. `o instanceof int[] x` means "if `o` is an `int[]`, call it `x`" (**pattern matching**, Java 16+). `Arrays.toString(x)` prints `[0, 1]`.
- It compares the **text** of expected and actual, prints PASS/FAIL, and counts the results.
- `summary()` prints the totals and exits with code 1 if anything failed. `run-all.sh` uses that exit code.

**One-line summary to remember:** *"For each x, look for target − x among the elements already seen, using a HashMap."*
