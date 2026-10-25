# E02 · Contains Duplicate

| | |
|---|---|
| **LeetCode** | [#217 Contains Duplicate](https://leetcode.com/problems/contains-duplicate/) |
| **Difficulty** | Easy |
| **Pattern** | Hashing (seen-before set) / Sorting |
| **Code** | [E02_ContainsDuplicate.java](E02_ContainsDuplicate.java) |
| **Run** | `java E02_ContainsDuplicate.java` |

---

## 1. The problem

Given an integer array `nums`, return `true` if **any value appears at least twice**, and `false` if every element is distinct.

| nums | answer | why |
|---|---|---|
| `[1, 2, 3, 1]` | `true` | 1 appears twice |
| `[1, 2, 3, 4]` | `false` | all distinct |
| `[1,1,1,3,3,4,3,2,4,2]` | `true` | many repeats |
| `[]` | `false` | nothing to repeat |
| `[7]` | `false` | one element can't be a duplicate |

**Constraints (LeetCode):** `1 ≤ nums.length ≤ 10⁵`, `-10⁹ ≤ nums[i] ≤ 10⁹`.

---

## 2. How to think about it

The question is really: *"while walking the array, have I seen this value before?"*

1. **Brute force:** compare every pair. O(n²).
2. **Use order:** if the array is **sorted**, equal values end up **next to each other**, so you only compare neighbours. O(n log n).
3. **Use memory:** keep a **set** of seen values. "Have I seen x?" becomes an O(1) check. O(n).

All three are worth saying in an interview, because the real discussion is **time vs space**:
- HashSet = fastest, but uses O(n) extra memory.
- Sorting = slower, but uses almost no extra memory (if you're allowed to modify the input).

---

## 3. Java concepts used in this problem

### 3.1 `HashSet`: a collection of unique values

A **Set** holds each value **at most once**. A **HashSet** is a set built on top of a HashMap (internally it really is a `HashMap` whose values are unused), so it has the same O(1) average speed. See E01 for how hashing works.

```java
Set<Integer> seen = new HashSet<>();
```

- `Set<Integer>` = the interface type (the contract); `HashSet` = the implementation.
- `Integer`, not `int`: collections hold objects only. Autoboxing converts for you.

| Method | What it does | Cost |
|---|---|---|
| `add(x)` | adds `x`. **Returns `true` if it was new, `false` if it was already there** | O(1) avg |
| `contains(x)` | `true` if `x` is in the set | O(1) avg |
| `remove(x)` | removes `x` | O(1) avg |
| `size()` | how many distinct values | O(1) |

**The trick in this problem:** `add` *returns a boolean*. Most people write:

```java
if (seen.contains(x)) return true;
seen.add(x);
```

That's two hash lookups. Since `add` already tells you whether `x` was present, one call is enough:

```java
if (!seen.add(x)) return true;   // add() == false → x was already in the set
```

> `!` is logical NOT: `!false` is `true`. Read `!seen.add(x)` as "adding x did **not** succeed".

**HashSet vs TreeSet vs LinkedHashSet:**

| | order of elements | add / contains |
|---|---|---|
| `HashSet` | no particular order | O(1) avg |
| `LinkedHashSet` | insertion order | O(1) avg |
| `TreeSet` | sorted | O(log n) |

### 3.2 The for-each loop: `for (int x : nums)`

```java
for (int x : nums) { ... }     // read as: "for each int x in nums"
```

It's the same as `for (int i = 0; i < nums.length; i++) { int x = nums[i]; ... }`, but shorter. Use it when you need only the **values**, not the **indices**. You can't use it to modify the array (assigning to `x` changes only the local copy).

### 3.3 `nums.clone()`: copying an array

```java
int[] a = nums.clone();   // a NEW array with the same values
```

Why not `int[] a = nums;`? That copies only the **reference** (the address). `a` and `nums` would then point to the **same** array, and sorting `a` would also scramble the caller's `nums`.

```
int[] a = nums;          nums ──┐
                          a ────┴──► [3, 1, 2]     (one array, two names)

int[] a = nums.clone();  nums ───► [3, 1, 2]
                          a ─────► [3, 1, 2]       (two separate arrays)
```

For a 1D array of primitives, `clone()` is a full copy. For a 2D array (`int[][]`) it's **shallow**: it copies the outer array, but the inner rows are still shared. Other ways to copy: `Arrays.copyOf(nums, nums.length)`, `System.arraycopy(...)`.

### 3.4 `Arrays.sort(int[])`

```java
Arrays.sort(a);    // sorts a IN PLACE, in ascending order: [3,1,2,1] → [1,1,2,3]
```

- `Arrays` is a utility class in `java.util` full of static helpers: `sort`, `toString`, `fill`, `copyOf`, `equals`, `binarySearch`...
- For **primitive** arrays (`int[]`), Java uses **Dual-Pivot Quicksort**: O(n log n) on average, and it needs O(log n) stack space for its recursion.
- For **object** arrays (`Integer[]`, `String[]`), Java uses **TimSort** (a merge sort variant). It's **stable** (equal elements keep their relative order) and uses O(n) extra space.
- It returns nothing (`void`); the array itself is changed.

### 3.5 `import java.util.*;`

`HashSet`, `Set` and `Arrays` all live in the package `java.util`. The `*` imports every class in that package, which is fine for practice files.

---

## 4. Approach 1: Brute force (compare every pair)

The `.java` file describes this approach only in its header comment, so here is the code:

```java
static boolean containsDuplicateBrute(int[] nums) {
    for (int i = 0; i < nums.length; i++) {
        for (int j = i + 1; j < nums.length; j++) {
            if (nums[i] == nums[j]) return true;   // same value at two different positions
        }
    }
    return false;                                  // checked every pair, no match
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (int i = 0; ...)` | pick the first element of the pair |
| `for (int j = i + 1; ...)` | pick a **later** element. `j > i` means we never compare an element with itself, and never compare the same pair twice |
| `if (nums[i] == nums[j]) return true;` | two positions hold the same value → duplicate found, stop right away |
| `return false;` | every pair was different |

### Dry run: `nums = [1, 2, 3, 1]`

| i | j | nums[i] | nums[j] | equal? |
|---|---|---|---|---|
| 0 | 1 | 1 | 2 | no |
| 0 | 2 | 1 | 3 | no |
| 0 | 3 | 1 | 1 | **yes → return true** |

### Dry run: `nums = [1, 2, 3]` (no duplicate, so every pair is checked)

| i | j | compare | equal? |
|---|---|---|---|
| 0 | 1 | 1 vs 2 | no |
| 0 | 2 | 1 vs 3 | no |
| 1 | 2 | 2 vs 3 | no |
| — | — | loops end | **return false** |

### Complexity
- **Time O(n²):** n(n−1)/2 comparisons. For n = 10⁵ that's 5 × 10⁹, which **times out** on LeetCode.
- **Space O(1).**

---

## 5. Approach 2: Sort, then compare neighbours

### Idea
After sorting, equal values sit **next to each other**: `[1,2,3,1]` → `[1,1,2,3]`. So one pass that compares each element with the one before it is enough.

### Code ([E02_ContainsDuplicate.java:39-46](E02_ContainsDuplicate.java#L39-L46))

```java
static boolean containsDuplicateSort(int[] nums) {
    int[] a = nums.clone();                 // don't destroy the caller's array
    Arrays.sort(a);
    for (int i = 1; i < a.length; i++) {
        if (a[i] == a[i - 1]) return true;  // equal neighbours
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] a = nums.clone();` | work on a **copy**, so the caller's array isn't reordered behind their back. (If modifying the input is allowed, you can skip this and save O(n) space.) |
| `Arrays.sort(a);` | ascending order, so duplicates become neighbours |
| `for (int i = 1; i < a.length; i++)` | start at **1**, because we compare `a[i]` with `a[i-1]`, and `a[-1]` doesn't exist. For an empty or 1-element array the loop doesn't run at all, so we get `false` with no special case |
| `if (a[i] == a[i - 1]) return true;` | same value as the previous one → duplicate |
| `return false;` | no equal neighbours means no duplicates anywhere |

**Why are neighbours enough?** In a sorted array, if `x` appears twice, everything between the two copies must be ≥ x and ≤ x, which means it's also `x`. So at least two copies are adjacent.

### Dry run 1: `nums = [1, 2, 3, 1]`

After sort: `a = [1, 1, 2, 3]`

| i | a[i-1] | a[i] | equal? |
|---|---|---|---|
| 1 | 1 | 1 | **yes → return true** |

### Dry run 2: `nums = [4, 1, 3, 2]`

After sort: `a = [1, 2, 3, 4]`

| i | a[i-1] | a[i] | equal? |
|---|---|---|---|
| 1 | 1 | 2 | no |
| 2 | 2 | 3 | no |
| 3 | 3 | 4 | no |
| — | | | **return false** |

### Dry run 3: `nums = [7]` (edge case)
`a = [7]`. The loop condition `1 < 1` is false right away, so we **return false**.

### Complexity
- **Time O(n log n):** the sort dominates, and the scan is O(n).
- **Space:** O(n) for the clone, plus O(log n) for the sort's recursion. If you sort `nums` in place, it's just **O(log n)**. That's often called "O(1) extra" loosely.

---

## 6. Approach 3: HashSet (optimal time)

### Idea
Walk once. For each `x`, try to add it to a set of seen values. If it was already in the set, that's a duplicate.

### Code ([E02_ContainsDuplicate.java:48-54](E02_ContainsDuplicate.java#L48-L54))

```java
static boolean containsDuplicate(int[] nums) {
    Set<Integer> seen = new HashSet<>();
    for (int x : nums) {
        if (!seen.add(x)) return true;      // add() == false -> already there
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Set<Integer> seen = new HashSet<>();` | an empty set of values seen so far |
| `for (int x : nums)` | for-each over the values. We don't need indices here |
| `seen.add(x)` | tries to insert `x`. Returns `true` if `x` is new, `false` if it was already present |
| `if (!seen.add(x)) return true;` | adding failed → we've seen `x` before → duplicate. **Early exit**: stop as soon as the first duplicate appears |
| `return false;` | added every element successfully, so all are distinct |

### Dry run 1: `nums = [1, 2, 3, 1]`

| step | x | set before | `add(x)` returns | set after | action |
|---|---|---|---|---|---|
| 1 | 1 | `{}` | true | `{1}` | continue |
| 2 | 2 | `{1}` | true | `{1, 2}` | continue |
| 3 | 3 | `{1, 2}` | true | `{1, 2, 3}` | continue |
| 4 | 1 | `{1, 2, 3}` | **false** | `{1, 2, 3}` | **return true** |

### Dry run 2: `nums = [1, 2, 3, 4]`

| step | x | `add(x)` | set after |
|---|---|---|---|
| 1 | 1 | true | `{1}` |
| 2 | 2 | true | `{1,2}` |
| 3 | 3 | true | `{1,2,3}` |
| 4 | 4 | true | `{1,2,3,4}` |
| — | | loop ends | **return false** |

### Dry run 3: `nums = []`
The loop body never runs, so we **return false**.

### Complexity
- **Time O(n):** one pass, with O(1) average per `add`.
- **Space O(n):** if there's no duplicate, the set ends up holding all n values.

---

## 7. (Bonus) One-liner with streams

```java
static boolean containsDuplicateStream(int[] nums) {
    return Arrays.stream(nums).distinct().count() < nums.length;
}
```

- `Arrays.stream(nums)` turns the array into an `IntStream` (a pipeline of values).
- `.distinct()` drops repeats (using a set internally), and `.count()` counts what's left.
- If fewer distinct values than elements exist, something repeated.

It's short, but it **always processes the whole array** (no early exit) and is slower in practice. It's good to know, but use the loop version in interviews.

Another variant: `new HashSet<>(list).size() < list.size()`. Same idea, also no early exit.

---

## 8. Comparing the approaches

| | Brute force | Sort + neighbours | HashSet |
|---|---|---|---|
| Time | O(n²) | O(n log n) | **O(n)** |
| Extra space | **O(1)** | O(log n) in place (O(n) with clone) | O(n) |
| Modifies input? | no | yes, unless you clone | no |
| Early exit? | yes | only after the sort | yes |
| Best when | tiny arrays | memory is tight | **default choice** |

---

## 9. Edge cases and common mistakes

**Edge cases:** empty array, single element, all elements equal (`[5,5,5]`), duplicate at the very end, negative numbers, `Integer.MIN_VALUE` / `MAX_VALUE`.

**Common mistakes:**
1. Starting the sorted scan at `i = 0` and reading `a[i-1]`, which crashes with `ArrayIndexOutOfBoundsException`.
2. Writing `int[] a = nums;` and thinking it's a copy. Sorting it then reorders the caller's array.
3. Using `contains` + `add` (two lookups) when one `add` is enough. It isn't wrong, just slower.
4. Comparing `Integer` objects with `==`. Here `x` is an `int`, so `==` is safe. But `Integer a = 1000, b = 1000; a == b` is **false**, because `==` compares references for objects. Use `.equals()` for `Integer`s. (Java caches `Integer` values from −128 to 127, which is why the bug hides on small numbers.)

---

## 10. Interview questions and answers

**Q1. Give me three solutions and their trade-offs.**
> Brute force pairs: O(n²) time, O(1) space. Sort and compare neighbours: O(n log n) time, O(1)–O(log n) extra space, but it modifies the input. HashSet: O(n) time, O(n) space. HashSet is the default; sorting is the choice when memory is limited.

**Q2. Why does `seen.add(x)` return a boolean, and why use it?**
> The `Set` contract says `add` returns `true` if the set changed. Using that return value avoids a separate `contains` call, so it's one hash lookup instead of two.

**Q3. What's the worst case for the HashSet approach?**
> Time: when there's no duplicate, every element is inserted, so it's O(n) with O(n) memory. Hash collisions could degrade an operation, but Java 8+ turns crowded buckets into trees, so even a bad case is O(log n) per operation.

**Q4. The interviewer says: "You can't use extra memory."**
> Sort in place and compare neighbours. If I can't modify the array either, only brute force O(n²) is left.

**Q5. What if values are in a small range, e.g. 0 to 10⁴?**
> Use a `boolean[] seen = new boolean[10001]` instead of a HashSet. Index = value, no hashing, and it's much faster. For an even smaller memory footprint, use a `BitSet` (1 bit per value).

### Follow-up questions

**F1. Contains Duplicate II (LC 219): is there a duplicate within distance k, i.e. `|i − j| ≤ k`?**
> Use a sliding window: a HashSet holding only the last k elements. Before adding `nums[i]`, remove `nums[i−k−1]`. If `add` fails, there's a duplicate within distance k. O(n) time, O(k) space. Or use a `value → last index` map and check `i − lastIndex ≤ k`. Covered in [06-Hash-Table/E02_ContainsDuplicateII](../../06-Hash-Table/02-Easy/E02_ContainsDuplicateII.md).

**F2. Contains Duplicate III (LC 220): indices within k AND values within t, i.e. `|nums[i] − nums[j]| ≤ t`?**
> Use a sliding window of size k kept in a `TreeSet`. For each x, `ceiling(x − t)` gives the smallest value ≥ x − t; check whether it's ≤ x + t. O(n log k). A faster O(n) solution uses **buckets** of width t+1. See [06-Hash-Table/H01_ContainsDuplicateIII](../../06-Hash-Table/04-Hard/H01_ContainsDuplicateIII.md).

**F3. Return the duplicate values, not just true/false.**
> Use a second set for "reported" values. When `add` fails on `seen`, add the value to `dups`. O(n).

**F4. Count how many times each value appears.**
> Use a `HashMap<Integer, Integer>` with `map.merge(x, 1, Integer::sum)` or `map.put(x, map.getOrDefault(x, 0) + 1)`.

**F5. Values are in the range 1..n and n+1 numbers are given: find the duplicate in O(1) space without modifying the array.** (LC 287)
> Treat `nums[i]` as a "next pointer". That makes a linked list with a cycle, and Floyd's cycle detection (tortoise and hare) finds the entry point of the cycle, which is the duplicate. O(n) time, O(1) space.

**F6. The data is too big for one machine.**
> Hash-partition the values across machines (every copy of x goes to the same machine), then each machine checks its share with a local HashSet. If approximate answers are OK, a **Bloom filter** uses far less memory, with some false positives (see 12-Advanced).

---

## 11. Running the file

```bash
cd Java-dsa/DSA/01-Arrays/02-Easy
java E02_ContainsDuplicate.java
```

`main` runs 6 checks (the HashSet version on 5 inputs, plus the sort version once) and prints `6/6 tests passed`. The `check` / `fmt` / `summary` helper at the bottom is the same in every problem file; E01's notes explain how it works.

**One-line summary to remember:** *"Keep a set of what you've seen; if add() fails, it's a duplicate."*
