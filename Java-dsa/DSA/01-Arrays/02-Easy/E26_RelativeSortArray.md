# E26 · Relative Sort Array

| | |
|---|---|
| **LeetCode** | [#1122 Relative Sort Array](https://leetcode.com/problems/relative-sort-array/) |
| **Difficulty** | Easy |
| **Pattern** | Counting sort (values in a small range) |
| **Code** | [E26_RelativeSortArray.java](E26_RelativeSortArray.java) |
| **Run** | `java E26_RelativeSortArray.java` |

---

## 1. The problem

Sort `arr1` so that its elements follow **the order in which they appear in `arr2`**. Elements that are **not** in `arr2` go at the end, in **ascending** order.

- All of `arr2`'s elements are **distinct**, and each of them appears in `arr1`.
- Values are in **0..1000**.

```
arr1 = [2,3,1,3,2,4,6,7,9,2,19]
arr2 = [2,1,4,3,9,6]
→      [2,2,2,1,4,3,3,9,6,7,19]
        └ arr2's order ┘ └ rest ┘
                         ascending
```

| arr1 | arr2 | answer |
|---|---|---|
| `[2,3,1,3,2,4,6,7,9,2,19]` | `[2,1,4,3,9,6]` | `[2,2,2,1,4,3,3,9,6,7,19]` |
| `[28,6,22,8,44,17]` | `[22,28,8,6]` | `[22,28,8,6,17,44]` |
| `[3,1,2]` | `[]` | `[1,2,3]` |

**Constraints:** `1 ≤ arr1.length, arr2.length ≤ 1000`, `0 ≤ values ≤ 1000`.

---

## 2. How to think about it

1. **Custom comparator:** sort `arr1` by "position in arr2". Elements not in arr2 get a large key (1000 + value), so they go last, in ascending order. O(n log n).
2. **Notice the clue: the values are small (0..1000).** When the value range `R` is small, **counting sort** beats any comparison sort:
   - `count[v]` = how many times v appears in arr1
   - for each v in arr2, **in order**: output v `count[v]` times, then mark it used up
   - for v = 0..1000 **ascending**: output the leftovers `count[v]` times

**Counting sort** in general: O(n + R) time and O(R) space. It's great when R is small, and impossible when values go up to 10⁹ (a 4 GB count array!).

---

## 3. Java concepts used in this problem

### 3.1 A count array (a "frequency table")

```java
int[] count = new int[1001];     // indices 0..1000, one slot per possible value
for (int x : arr1) count[x]++;   // the value IS the index
```

This is the array version of a `HashMap<Integer, Integer>` counter, and it's much faster (no hashing, no boxing). It only works when the values are small, non-negative integers.

### 3.2 The tricky loop: `while (count[v]-- > 0) result[w++] = v;`

Read it carefully. It's compact, but subtle:

```java
while (count[v]-- > 0)   // 1. compare the CURRENT count[v] with 0
                         // 2. THEN decrease count[v] by 1 (post-decrement)
    result[w++] = v;     // write v at w, then w++
```

With `count[v] = 3`:

| check | value compared | true? | count after | writes |
|---|---|---|---|---|
| 1 | 3 | yes | 2 | 1st v |
| 2 | 2 | yes | 1 | 2nd v |
| 3 | 1 | yes | 0 | 3rd v |
| 4 | 0 | **no** | **−1** | stop |

So v is written exactly 3 times ✓, but **`count[v]` ends at −1, not 0**. That's why the second loop (the leftovers) doesn't write arr2's values again: for them, `count[v]` is −1, so `−1 > 0` is false right away (it becomes −2, which is harmless).

> The header comment says "set count[v] = 0". The code gets the same effect with −1. A clearer (if longer) version:
> ```java
> for (int c = 0; c < count[v]; c++) result[w++] = v;
> count[v] = 0;
> ```

### 3.3 Custom sort order with a Comparator (used in approach 1)

```java
Integer[] boxed = ...;
Arrays.sort(boxed, (a, b) -> Integer.compare(key(a), key(b)));
```

- `Arrays.sort(int[])` can't take a comparator; it only sorts primitives in their natural order. To sort with your own rule you need an **object** array (`Integer[]`).
- `(a, b) -> ...` is a **lambda** implementing `Comparator<Integer>`. It returns a negative number if a comes first, positive if b comes first, and 0 if they're equal.
- `Integer.compare(x, y)` is the safe way to compare. Don't use `x − y`, which can overflow for large values.

---

## 4. Approach 1: Custom comparator

```java
static int[] relativeSortComparator(int[] arr1, int[] arr2) {
    Map<Integer, Integer> rank = new HashMap<>();
    for (int i = 0; i < arr2.length; i++) rank.put(arr2[i], i);   // value → position in arr2
    Integer[] boxed = Arrays.stream(arr1).boxed().toArray(Integer[]::new);
    Arrays.sort(boxed, (a, b) -> {
        int ka = rank.getOrDefault(a, 1000 + a);   // not in arr2 → after every arr2 value, ascending
        int kb = rank.getOrDefault(b, 1000 + b);
        return Integer.compare(ka, kb);
    });
    return Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();
}
```

| Line | What it does |
|---|---|
| `rank.put(arr2[i], i)` | the sort key for arr2's values = their index (0..999) |
| `Arrays.stream(arr1).boxed().toArray(Integer[]::new)` | `int[]` → `Integer[]` so we can use a comparator. `Integer[]::new` is a method reference to the array constructor |
| `rank.getOrDefault(a, 1000 + a)` | values not in arr2 get keys 1000..2000, larger than any index, and ordered by value |
| `Integer.compare(ka, kb)` | sort by key |

**Dry run** (example 2): keys are 22→0, 28→1, 8→2, 6→3, 17→1017, 44→1044 → sorted: `[22, 28, 8, 6, 17, 44]` ✓

**O(n log n + m) time, O(n + m) space.** It works for **any** value range, which makes it the general solution.

---

## 5. Approach 2: Counting sort (the file's solution)

### Code ([E26_RelativeSortArray.java:37-50](E26_RelativeSortArray.java#L37-L50))

```java
static int[] relativeSortArray(int[] arr1, int[] arr2) {
    int[] count = new int[1001];
    for (int x : arr1) count[x]++;

    int[] result = new int[arr1.length];
    int w = 0;
    for (int v : arr2) {                    // 1) in arr2's order
        while (count[v]-- > 0) result[w++] = v;
    }
    for (int v = 0; v < count.length; v++) { // 2) leftovers in ascending order
        while (count[v]-- > 0) result[w++] = v;
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] count = new int[1001];` | one counter for each possible value 0..1000 |
| `for (int x : arr1) count[x]++;` | build the frequency table |
| `int[] result = new int[arr1.length]; int w = 0;` | the output and its write pointer |
| `for (int v : arr2) while (count[v]-- > 0) result[w++] = v;` | phase 1: for each arr2 value in order, write all its copies. Afterwards its count is −1 (used up) |
| `for (int v = 0; v < 1001; v++) while (...)` | phase 2: scan all values in ascending order; only those **not** in arr2 still have a positive count |
| `return result;` | |

### Dry run: example 1

`arr1 = [2,3,1,3,2,4,6,7,9,2,19]`, `arr2 = [2,1,4,3,9,6]`

**Counts** (non-zero only): `1:1, 2:3, 3:2, 4:1, 6:1, 7:1, 9:1, 19:1`

**Phase 1** (arr2 order):

| v | count[v] | writes | result so far | count after |
|---|---|---|---|---|
| 2 | 3 | 2, 2, 2 | `[2,2,2]` | −1 |
| 1 | 1 | 1 | `[2,2,2,1]` | −1 |
| 4 | 1 | 4 | `[2,2,2,1,4]` | −1 |
| 3 | 2 | 3, 3 | `[2,2,2,1,4,3,3]` | −1 |
| 9 | 1 | 9 | `[2,2,2,1,4,3,3,9]` | −1 |
| 6 | 1 | 6 | `[2,2,2,1,4,3,3,9,6]` | −1 |

**Phase 2** (v = 0..1000): the only positive counts left are 7 (1) and 19 (1):

| v | count[v] | writes | result |
|---|---|---|---|
| 0..6 | 0 or −1 | none | |
| 7 | 1 | 7 | `[...,6,7]` |
| 8..18 | 0 | none | |
| 19 | 1 | 19 | `[2,2,2,1,4,3,3,9,6,7,19]` ✓ |
| 20..1000 | 0 | none | |

### Dry run: `arr1 = [3,1,2]`, `arr2 = []`
Phase 1 does nothing. Phase 2 writes 1, 2, 3 → `[1, 2, 3]`: a plain counting sort ✓

### Complexity
- **Time O(n + m + R)** where R = 1001: one pass to count, phase 1 over arr2, phase 2 over the range. Total writes = n.
- **Space O(R)** for `count`, plus the output.

---

## 6. Comparing the approaches

| | Comparator | Counting sort |
|---|---|---|
| Time | O(n log n) | **O(n + R)** |
| Extra space | O(n + m) (boxing + map) | O(R) |
| Works for any value range? | **yes** | only small R |

---

## 7. Edge cases and common mistakes

**Edge cases:** arr2 empty (a plain sort), every arr1 value in arr2 (no leftovers), duplicates, the value 0, the value 1000 (the array needs size **1001**).

**Mistakes:**
1. `new int[1000]`: the value 1000 then crashes with `ArrayIndexOutOfBoundsException`.
2. Not marking arr2's values as used, so they get written again in phase 2. (The `count[v]--` leaving −1 handles this here.)
3. Sorting with `(a, b) -> a − b` for keys that could overflow (not here, but a bad habit).
4. Trying `Arrays.sort(int[], comparator)`: it doesn't exist. Box the values first.

---

## 8. Interview questions and answers

**Q1. When is counting sort better than comparison sorting?**
> When the value range R is small compared with n log n, e.g. ages, grades, or small IDs. It's O(n + R) and beats the O(n log n) lower bound because it doesn't compare elements, it indexes by value.

**Q2. Why does `while (count[v]-- > 0)` stop correctly?**
> Post-decrement compares the old value, then decrements. It runs exactly `count[v]` times and leaves the count at −1, which is also why phase 2 skips arr2's values.

**Q3. Is counting sort stable?**
> The version that just writes values isn't really about stability, since equal ints are indistinguishable. The "real" stable counting sort (for sorting records by key) uses prefix sums over the counts to find each key's output position and places records in input order. That's the basis of **radix sort**.

### Follow-up questions

**F1. Values range up to 10⁹.**
> Use the comparator approach, or a `TreeMap<Integer, Integer>` of counts for the leftovers (sorted keys), with a HashMap for arr2's order.

**F2. Custom Sort String (LC 791): order the characters of s by the order of `order`.**
> The same counting idea with `int[26]`: write `order`'s characters by count, then the rest.

**F3. Sort Colors (only 0, 1, 2) in one pass.**
> Counting sort works (two passes). The Dutch National Flag does it in one pass.

**F4. Sort a huge array of ages (0..150).**
> Counting sort: O(n) and memory-trivial. Also parallelisable: count per chunk, then add the counts.

**F5. Radix sort.**
> Sort numbers digit by digit (least significant first), each pass a **stable** counting sort on a base-10 (or base-256) digit. O(d·(n + base)), handy for fixed-width integers.

---

## 9. Running the file

```bash
java E26_RelativeSortArray.java      # 3/3 tests passed
```

**One-line summary:** *"Count every value; write arr2's values in arr2's order, then the leftovers in ascending order."*
