# E11 · Intersection of Two Arrays (+ #350 Intersection II)

| | |
|---|---|
| **LeetCode** | [#349 Intersection of Two Arrays](https://leetcode.com/problems/intersection-of-two-arrays/), variant [#350 Intersection of Two Arrays II](https://leetcode.com/problems/intersection-of-two-arrays-ii/) |
| **Difficulty** | Easy |
| **Pattern** | Hashing / sort + two pointers / counting |
| **Code** | [E11_IntersectionOfTwoArrays.java](E11_IntersectionOfTwoArrays.java) |
| **Run** | `java E11_IntersectionOfTwoArrays.java` |

---

## 1. The problem

**#349:** return the **unique** values that appear in **both** arrays, in any order.

| nums1 | nums2 | answer |
|---|---|---|
| `[1, 2, 2, 1]` | `[2, 2]` | `[2]` |
| `[4, 9, 5]` | `[9, 4, 9, 8, 4]` | `[4, 9]` (or `[9, 4]`) |
| `[1, 2]` | `[3, 4]` | `[]` |

**#350 (variant):** keep **duplicates**, i.e. each value appears as many times as it appears in **both** arrays (the smaller of the two counts).

| nums1 | nums2 | #350 answer |
|---|---|---|
| `[1, 2, 2, 1]` | `[2, 2]` | `[2, 2]` |
| `[4, 9, 5]` | `[9, 4, 9, 8, 4]` | `[4, 9]` |

**Constraints:** `1 ≤ n, m ≤ 1000`, `0 ≤ values ≤ 1000`.

---

## 2. How to think about it

- **Brute force:** for each `x` in nums1, scan nums2 for it. O(n × m).
- **Bottleneck:** "is x in nums2?" is a membership question, so use a **set** (O(1)).
- **Set approach:** put nums1 in a set; walk nums2; keep the values found in the set. Collect them in **another set**, so each common value appears once.
- **Sorted approach:** sort both and walk with two pointers, like merging. Good when the input is already sorted or memory is tight.
- **#350:** "how many times" means a **count map** instead of a set.

---

## 3. Java concepts used in this problem

### 3.1 `TreeSet`: a sorted set

```java
Set<Integer> common = new TreeSet<>();
```

A `TreeSet` keeps its elements **sorted** (internally a red-black tree, a self-balancing binary search tree). Operations are O(log n) instead of a HashSet's O(1). The code uses it **only** so the output order is predictable for the tests (`[4, 9]`, never `[9, 4]`). In an interview, a `HashSet` is fine because "any order" is allowed.

TreeSet extras: `first()`, `last()`, `floor(x)` (the largest ≤ x), `ceiling(x)` (the smallest ≥ x), `headSet`, `tailSet`.

### 3.2 `ArrayList` / `List`

```java
List<Integer> out = new ArrayList<>();
out.add(5);             // append at the end, amortized O(1)
out.get(0);             // read by index, O(1)
out.size();             // number of elements
out.isEmpty();          // size() == 0
out.get(out.size() - 1) // the last element
```

An `ArrayList` is a **resizable array** (you built one in [DynamicArray](../01-Basics/DynamicArray.java)). Use it when you don't know the output size in advance, then convert it to `int[]` at the end.

### 3.3 Converting a collection to `int[]`: `stream().mapToInt(Integer::intValue).toArray()`

```java
return common.stream().mapToInt(Integer::intValue).toArray();
```

| Piece | Meaning |
|---|---|
| `common.stream()` | turn the collection into a **Stream<Integer>** (a pipeline of elements) |
| `.mapToInt(Integer::intValue)` | convert each `Integer` object to a primitive `int` → an **IntStream** |
| `Integer::intValue` | a **method reference**: short for the lambda `x -> x.intValue()` |
| `.toArray()` | collect the IntStream into an `int[]` |

Why so much ceremony? Java generics can't hold primitives, so `List<Integer>` and `int[]` are different things, and there's no direct `toArray()` to `int[]`. A manual loop works too:

```java
int[] res = new int[out.size()];
for (int k = 0; k < res.length; k++) res[k] = out.get(k);
```

### 3.4 `Map.merge` and `getOrDefault`: counting

```java
count.merge(x, 1, Integer::sum);
```
means: *"if `x` isn't in the map, put 1; otherwise replace the value with `oldValue + 1`."* `Integer::sum` is a method reference to `(a, b) -> a + b`. It's the shortest way to count in Java. The long version:

```java
count.put(x, count.getOrDefault(x, 0) + 1);
```

`getOrDefault(key, def)` returns the value, or `def` when the key is missing. That avoids the `null` you'd get from `get`.

### 3.5 Comparing `Integer` with `int`: `out.get(...) != a[i]`

`out.get(...)` returns an `Integer`, and `a[i]` is an `int`. When one side is a primitive, Java **unboxes** the Integer and compares **values**, so this is correct. If **both** sides were `Integer`, `!=` would compare **references**, which is wrong for values outside −128..127. In that case use `.equals()`.

### 3.6 `else if` chains

```java
if (a[i] < b[j]) i++;
else if (a[i] > b[j]) j++;
else { ... }        // the only remaining case: equal
```

Exactly one branch runs.

---

## 4. Approach 1: Brute force

```java
static int[] intersectionBrute(int[] nums1, int[] nums2) {
    Set<Integer> res = new TreeSet<>();
    for (int x : nums1) {
        for (int y : nums2) {
            if (x == y) { res.add(x); break; }   // found x in nums2, stop scanning
        }
    }
    return res.stream().mapToInt(Integer::intValue).toArray();
}
```

`break` exits the **inner** loop only. **O(n × m)** time.

---

## 5. Approach 2: Two sets (#349)

### Code ([E11_IntersectionOfTwoArrays.java:35-43](E11_IntersectionOfTwoArrays.java#L35-L43))

```java
static int[] intersection(int[] nums1, int[] nums2) {
    Set<Integer> first = new HashSet<>();
    for (int x : nums1) first.add(x);
    Set<Integer> common = new TreeSet<>();          // TreeSet only to make the output order predictable
    for (int x : nums2) {
        if (first.contains(x)) common.add(x);
    }
    return common.stream().mapToInt(Integer::intValue).toArray();
}
```

### Line by line

| Line | What it does |
|---|---|
| `Set<Integer> first = new HashSet<>(); for (...) first.add(x);` | all distinct values of nums1, for O(1) lookups |
| `Set<Integer> common = new TreeSet<>();` | the result. A **set**, so a value found several times in nums2 is stored once |
| `if (first.contains(x)) common.add(x);` | x is in both arrays → part of the answer |
| `return common.stream()...toArray();` | convert `Set<Integer>` to `int[]` |

### Dry run: `nums1 = [4, 9, 5]`, `nums2 = [9, 4, 9, 8, 4]`

`first = {4, 9, 5}`

| x (from nums2) | in first? | common after |
|---|---|---|
| 9 | yes | {9} |
| 4 | yes | {4, 9} |
| 9 | yes | {4, 9} (already there) |
| 8 | no | {4, 9} |
| 4 | yes | {4, 9} |

**Return `[4, 9]`** (sorted, because it's a TreeSet).

### Dry run: `[1,2,2,1]` & `[2,2]`
`first = {1, 2}` → x=2 ✓ → {2}; x=2 ✓ → {2}. **Return `[2]`**.

### Complexity
- **Time O(n + m)** with a HashSet result (O(n + m log k) with a TreeSet, where k is the result size).
- **Space O(n)** for `first`, plus the result.

**Optimisation:** build the set from the **smaller** array to save memory.

---

## 6. Approach 3: Sort both + two pointers (#349)

### Code ([E11_IntersectionOfTwoArrays.java:46-62](E11_IntersectionOfTwoArrays.java#L46-L62))

```java
static int[] intersectionSorted(int[] nums1, int[] nums2) {
    int[] a = nums1.clone(), b = nums2.clone();
    Arrays.sort(a);
    Arrays.sort(b);
    List<Integer> out = new ArrayList<>();
    int i = 0, j = 0;
    while (i < a.length && j < b.length) {
        if (a[i] < b[j]) i++;
        else if (a[i] > b[j]) j++;
        else {
            if (out.isEmpty() || out.get(out.size() - 1) != a[i]) out.add(a[i]);   // skip repeats
            i++;
            j++;
        }
    }
    return out.stream().mapToInt(Integer::intValue).toArray();
}
```

### Line by line

| Line | What it does |
|---|---|
| `a = nums1.clone(), b = nums2.clone()` + sort | sorted copies (we don't mess up the caller's arrays) |
| `while (i < a.length && j < b.length)` | stop when either array runs out. Nothing after that can be common |
| `if (a[i] < b[j]) i++;` | a[i] is too small to be in b (everything in b from j on is bigger), so skip it |
| `else if (a[i] > b[j]) j++;` | the same reasoning for b[j] |
| `else` | equal → a common value |
| `if (out.isEmpty() \|\| last != a[i]) out.add(a[i]);` | add it only if it isn't the same as the last one we added (uniqueness). `isEmpty()` is checked first, so `get(-1)` is never called |
| `i++; j++;` | move both forward |

### Dry run: `nums1 = [4, 9, 5]`, `nums2 = [9, 4, 9, 8, 4]`

Sorted: `a = [4, 5, 9]`, `b = [4, 4, 8, 9, 9]`

| i (a[i]) | j (b[j]) | compare | action | out |
|---|---|---|---|---|
| 0 (4) | 0 (4) | equal | add 4, i++, j++ | [4] |
| 1 (5) | 1 (4) | 5 > 4 | j++ | [4] |
| 1 (5) | 2 (8) | 5 < 8 | i++ | [4] |
| 2 (9) | 2 (8) | 9 > 8 | j++ | [4] |
| 2 (9) | 3 (9) | equal | add 9, i++, j++ | [4, 9] |
| 3 | 4 | i = a.length → stop | | |

**Return `[4, 9]`**.

### Complexity
- **Time O(n log n + m log m)** (the sorts); the walk is O(n + m).
- **Space** O(1) extra besides the sorted copies and the output (if sorting the inputs in place is allowed).

---

## 7. Approach 4: Count map (#350, keep duplicates)

### Code ([E11_IntersectionOfTwoArrays.java:65-77](E11_IntersectionOfTwoArrays.java#L65-L77))

```java
static int[] intersectWithDuplicates(int[] nums1, int[] nums2) {
    Map<Integer, Integer> count = new HashMap<>();
    for (int x : nums1) count.merge(x, 1, Integer::sum);
    List<Integer> out = new ArrayList<>();
    for (int x : nums2) {
        int c = count.getOrDefault(x, 0);
        if (c > 0) {
            out.add(x);
            count.put(x, c - 1);                   // use one copy up
        }
    }
    return out.stream().mapToInt(Integer::intValue).toArray();
}
```

### Line by line

| Line | What it does |
|---|---|
| `count.merge(x, 1, Integer::sum)` | how many copies of each value nums1 has |
| `int c = count.getOrDefault(x, 0);` | copies of x still available from nums1 (0 if x is not in nums1) |
| `if (c > 0) { out.add(x); count.put(x, c - 1); }` | match one copy from nums2 with one copy from nums1, and use that copy up |

### Dry run: `nums1 = [1, 2, 2, 1]`, `nums2 = [2, 2]`

`count = {1:2, 2:2}`

| x | c | match? | out | count after |
|---|---|---|---|---|
| 2 | 2 | yes | [2] | {1:2, 2:1} |
| 2 | 1 | yes | [2, 2] | {1:2, 2:0} |

**Return `[2, 2]`**.

### Dry run: `nums1 = [2, 2]`, `nums2 = [1, 2, 2, 2]`
`count = {2:2}` → x=1: c=0, skip → x=2: yes [2] → x=2: yes [2,2] → x=2: c=0, skip. **Return `[2, 2]`**. That's min(2, 3) copies.

**Time O(n + m), Space O(n).**

---

## 8. Comparing the approaches

| | Brute | Two sets | Sort + pointers | Count map (#350) |
|---|---|---|---|---|
| Time | O(nm) | **O(n+m)** | O(n log n + m log m) | O(n+m) |
| Extra space | O(k) | O(n) | O(1)* | O(n) |
| Handles #350? | — | no | yes (drop the uniqueness check) | **yes** |
| Best when | tiny | default | already sorted / low memory | duplicates matter |

\* not counting sorting the inputs in place.

---

## 9. Edge cases and common mistakes

**Edge cases:** no overlap → `[]`, one array a subset of the other, all elements the same, duplicates in only one array, empty arrays.

**Mistakes:**
1. Using a `List` for the result in #349, which gives duplicate outputs like `[2, 2]`.
2. In #350, not decrementing the count, which outputs too many copies.
3. Comparing two `Integer` objects with `==` / `!=`.
4. Calling `out.get(out.size() - 1)` on an empty list → `IndexOutOfBoundsException`. That's why `isEmpty()` comes first.

---

## 10. Interview questions and answers

**Q1. What's the difference between #349 and #350 in code?**
> #349 wants unique values, so I use sets. #350 keeps multiplicity, so I use a count map and decrement it on each match. With sorted input, the only difference is whether I skip repeated matches.

**Q2. Which array do you put into the hash structure?**
> The smaller one, so memory is O(min(n, m)). Then I stream the larger one past it.

**Q3. Is `retainAll` a valid answer?**
> `Set<Integer> s1 = ...; s1.retainAll(s2);` keeps only the elements that are also in s2. That's correct for #349 and concise, but explain that it's still O(n + m) with hash sets.

### Follow-up questions (from LeetCode's #350 page)

**F1. What if both arrays are already sorted?**
> Two pointers, O(n + m) time and O(1) extra space. No hashing needed.

**F2. What if nums1 is much smaller than nums2?**
> Hash the small array (tiny memory) and stream the big one. Or, if the big one is sorted, binary search each small element in it: O(n log m), which beats O(m) when n is tiny.

**F3. nums2 is on disk and too big for memory, and you can only load chunks.**
> Put nums1 in a count map (it fits in memory). Stream nums2 chunk by chunk, matching against the map. If neither fits: external sort both files, then stream both with two pointers.

**F4. Intersection of k arrays.**
> Count in how many arrays each value appears (use a set per array, so duplicates inside one array count once). The values with count == k are the answer. Or intersect pairwise, smallest first.

**F5. Union or difference instead of intersection.**
> Union: add everything to one set (`addAll`). Difference A − B: `setA.removeAll(setB)`. Sorted-merge versions work too.

---

## 11. Running the file

```bash
java E11_IntersectionOfTwoArrays.java      # 5/5 tests passed
```

**One-line summary:** *"Set for membership (#349), count map for multiplicity (#350), two pointers when sorted."*
