# M05 · 3Sum

| | |
|---|---|
| **LeetCode** | [#15 3Sum](https://leetcode.com/problems/3sum/) |
| **Difficulty** | Medium |
| **Pattern** | Sort + fix one + two pointers (with careful duplicate skipping) |
| **Code** | [M05_ThreeSum.java](M05_ThreeSum.java) |
| **Run** | `java M05_ThreeSum.java` |

---

## 1. The problem

Return **all unique triplets** `[a, b, c]` from the array (at three different indices) with `a + b + c == 0`. The answer must not contain **duplicate triplets** (the same three values, in any order, count once).

| nums | answer |
|---|---|
| `[-1, 0, 1, 2, -1, -4]` | `[[-1, -1, 2], [-1, 0, 1]]` |
| `[0, 1, 1]` | `[]` |
| `[0, 0, 0, 0]` | `[[0, 0, 0]]` (only once, even though there are 4 zeros) |
| `[-2, 0, 1, 1, 2]` | `[[-2, 0, 2], [-2, 1, 1]]` |

**Constraints:** `3 ≤ n ≤ 3000`, `-10⁵ ≤ nums[i] ≤ 10⁵`.

---

## 2. How to think about it

1. **Brute force:** three nested loops + a `Set` to remove duplicates. O(n³), about 4.5 × 10⁹ for n = 3000, which is too slow.
2. **Reduce to a problem you already know:** if you **fix** the first number `a = nums[i]`, you need two numbers in the rest of the array that sum to `−a`. That's **Two Sum**.
3. **Sort first.** Sorting gives two big wins:
   - the inner Two Sum can use **two pointers** ([M03](M03_TwoSumII_SortedInput.md)): O(n), O(1) space
   - **equal values become neighbours**, so skipping duplicates is easy
4. Total: n choices of i × an O(n) two-pointer scan = **O(n²)**.

### Avoiding duplicate triplets (the tricky part)
- **(a) Skip a repeated first number:** if `nums[i] == nums[i−1]`, every triplet starting with this value was already found, so skip it.
- **(b) After finding a triplet,** move `left` past **all** copies of `nums[left]` and `right` past all copies of `nums[right]`. Otherwise the same pair would be found again.

### Early exit
The array is sorted, so if `nums[i] > 0`, everything after it is positive too, and no zero sum is possible. Stop.

---

## 3. Java concepts used in this problem

### 3.1 `List.of(...)`: a small immutable list

```java
result.add(List.of(nums[i], nums[left], nums[right]));
```

- `List.of(a, b, c)` (Java 9+) creates a list with those elements in one call. It's shorter than `new ArrayList<>()` plus three `add`s.
- It's **immutable**: calling `add`/`set`/`remove` on it throws `UnsupportedOperationException`. That's perfect for answer triplets that should never change.
- It doesn't allow `null` elements.
- Related: `Arrays.asList(a, b, c)` gives a fixed-size list that you **can** `set` but not `add` to.

### 3.2 Sorting the input changes the caller's array

`Arrays.sort(nums)` reorders the array passed in (see E03). LeetCode allows it. In production code, sort a copy if the caller still needs the original order.

### 3.3 `break` versus `continue` in the same loop

```java
if (nums[i] > 0) break;                          // leave the loop completely
if (i > 0 && nums[i] == nums[i - 1]) continue;   // skip only this i
```

### 3.4 `i > 0 &&` guards `nums[i - 1]`

Short-circuit `&&` makes sure `nums[-1]` is never read when i = 0 (see E07).

### 3.5 Set of lists for deduplication (used in brute force)

```java
Set<List<Integer>> seen = new HashSet<>();
seen.add(List.of(-1, -1, 2));
seen.add(List.of(-1, -1, 2));   // ignored: List.equals compares the elements
```

Lists implement `equals`/`hashCode` **by content**, so two lists with the same elements in the same order count as equal in a `HashSet`. That's why we sort each triplet before adding it: `[2, -1, -1]` and `[-1, -1, 2]` are different lists.

---

## 4. Approach 1: Brute force + set

```java
static List<List<Integer>> threeSumBrute(int[] nums) {
    Set<List<Integer>> set = new HashSet<>();
    int n = nums.length;
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++)
            for (int k = j + 1; k < n; k++)
                if (nums[i] + nums[j] + nums[k] == 0) {
                    int[] t = {nums[i], nums[j], nums[k]};
                    Arrays.sort(t);                      // a canonical order → duplicates look the same
                    set.add(List.of(t[0], t[1], t[2]));
                }
    return new ArrayList<>(set);
}
```

**O(n³) time.** `new ArrayList<>(set)` copies the set into a list (the order is arbitrary).

---

## 5. Approach 2: HashSet for the third number, O(n²) without two pointers

```java
static List<List<Integer>> threeSumHash(int[] nums) {
    Arrays.sort(nums);
    List<List<Integer>> res = new ArrayList<>();
    for (int i = 0; i < nums.length; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;
        Set<Integer> seen = new HashSet<>();
        for (int j = i + 1; j < nums.length; j++) {
            int need = -nums[i] - nums[j];
            if (seen.contains(need)) {
                res.add(List.of(nums[i], need, nums[j]));
                while (j + 1 < nums.length && nums[j] == nums[j + 1]) j++;   // skip duplicate partners
            }
            seen.add(nums[j]);
        }
    }
    return res;
}
```

This is Two Sum I (a hash set) inside the loop. **O(n²) time, O(n) space.** It works, but the two-pointer version is simpler and uses O(1) extra space.

---

## 6. Approach 3: Sort + fix one + two pointers (the file's solution)

### Code ([M05_ThreeSum.java:44-70](M05_ThreeSum.java#L44-L70))

```java
static List<List<Integer>> threeSum(int[] nums) {
    Arrays.sort(nums);
    List<List<Integer>> result = new ArrayList<>();
    int n = nums.length;

    for (int i = 0; i < n - 2; i++) {
        if (nums[i] > 0) break;                              // early exit
        if (i > 0 && nums[i] == nums[i - 1]) continue;       // (a) skip duplicate first numbers

        int left = i + 1, right = n - 1;
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];
            if (sum < 0) {
                left++;
            } else if (sum > 0) {
                right--;
            } else {
                result.add(List.of(nums[i], nums[left], nums[right]));
                while (left < right && nums[left] == nums[left + 1]) left++;     // (b) skip copies
                while (left < right && nums[right] == nums[right - 1]) right--;
                left++;
                right--;
            }
        }
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Arrays.sort(nums);` | enables two pointers and makes duplicates adjacent |
| `for (int i = 0; i < n - 2; i++)` | i = the first element; at least two elements must remain after it |
| `if (nums[i] > 0) break;` | sorted: the smallest of the three is positive → no more zero sums |
| `if (i > 0 && nums[i] == nums[i - 1]) continue;` | the same first value as before → its triplets are already found |
| `int left = i + 1, right = n - 1;` | two pointers over the rest |
| `sum < 0 → left++` | we need a larger sum |
| `sum > 0 → right--` | we need a smaller sum |
| `else { result.add(...); ... }` | found a triplet |
| `while (left < right && nums[left] == nums[left + 1]) left++;` | move `left` to the **last** copy of its value |
| `while (... nums[right] == nums[right - 1]) right--;` | move `right` to the **first** copy of its value |
| `left++; right--;` | step past those last copies to brand-new values |

**Why can both pointers move after a match?** With `nums[i]` and `nums[left]` fixed, the only partner that works is `nums[right]`'s value. Any new triplet needs a **different** `left` value **and** a different `right` value, so moving just one pointer can't find a new triplet.

### Dry run 1: `[-1, 0, 1, 2, -1, -4]`

Sorted: `[-4, -1, -1, 0, 1, 2]` (indices 0..5)

**i = 0 (−4):** we need 4 from the rest.

| L (val) | R (val) | sum | action |
|---|---|---|---|
| 1 (−1) | 5 (2) | −3 | L++ |
| 2 (−1) | 5 (2) | −3 | L++ |
| 3 (0) | 5 (2) | −2 | L++ |
| 4 (1) | 5 (2) | −1 | L++ |
| 5 | 5 | | stop: nothing found |

**i = 1 (−1):** we need 1.

| L (val) | R (val) | sum | action |
|---|---|---|---|
| 2 (−1) | 5 (2) | 0 | **found [−1, −1, 2]**; no copies to skip; L=3, R=4 |
| 3 (0) | 4 (1) | 0 | **found [−1, 0, 1]**; L=4, R=3 |
| | | | stop |

**i = 2 (−1):** `nums[2] == nums[1]` → **skip**. (It would find `[-1, 0, 1]` again.)

**i = 3 (0):** L=4 (1), R=5 (2): sum 3 > 0 → R-- → stop.

The loop ends at i = 3 (since i < n − 2 = 4).

**Result:** `[[-1, -1, 2], [-1, 0, 1]]` ✓

### Dry run 2: `[0, 0, 0, 0]` (duplicate skipping in action)

**i = 0 (0):** L = 1, R = 3, sum = 0 → **found [0, 0, 0]**. Then:

| step | check | result |
|---|---|---|
| skip left copies | L=1: `1 < 3` and nums[1] == nums[2] | L = 2 |
| | L=2: `2 < 3` and nums[2] == nums[3] | L = 3 |
| | L=3: `3 < 3` is false | stop |
| skip right copies | `3 < 3` is false | nothing to do |
| step past | L++ → 4, R-- → 2 | `4 < 2` is false → the inner loop ends |

**i = 1:** `nums[1] == nums[0]` → skip. The outer loop ends (i must be < n − 2 = 2).

**Result:** `[[0, 0, 0]]`, exactly once ✓

### Dry run 3: `[-2, 0, 1, 1, 2]`

Sorted already. **i = 0 (−2):** we need 2.

| L (val) | R (val) | sum | action |
|---|---|---|---|
| 1 (0) | 4 (2) | 0 | **found [−2, 0, 2]**; L=2, R=3 |
| 2 (1) | 3 (1) | 0 | **found [−2, 1, 1]**; L=3, R=2 |

**i = 1 (0):** L=2 (1), R=4 (2): sum 3 → R--; L=2, R=3: sum 2 → R-- → stop.
**i = 2 (1)** > 0 → break.

**Result:** `[[-2, 0, 2], [-2, 1, 1]]` ✓

### Complexity
- **Time O(n²):** the sort is O(n log n); the outer loop is n, and each inner two-pointer scan is O(n).
- **Space O(1) extra**, apart from the output and the sort's O(log n) stack.

---

## 7. Comparing the approaches

| | Brute + set | Sort + hash set | Sort + two pointers |
|---|---|---|---|
| Time | O(n³) | O(n²) | **O(n²)** |
| Extra space | O(answers) | O(n) | **O(1)** |
| Duplicate handling | the set does it | skipping | skipping |

**Can 3Sum be faster than O(n²)?** Not by any known simple method. It's a famous conjecture that roughly O(n²) is needed; "3SUM-hard" problems are named after it.

---

## 8. Edge cases and common mistakes

**Edge cases:** all zeros, no answer, many duplicates (`[-1,-1,-1,2,2]`), all positive or all negative, exactly 3 elements.

**Mistakes:**
1. Skipping duplicates of `i` with `nums[i] == nums[i+1]` (looking forward). That skips the **first** occurrence and loses triplets like `[-1, -1, 2]`. Always compare with the **previous** element.
2. Forgetting to move both pointers after a match → an infinite loop.
3. Skipping duplicates **before** recording the triplet.
4. Using a Set to deduplicate in the O(n²) version: it works, but it hides the reasoning interviewers want to see.

---

## 9. Interview questions and answers

**Q1. Why sort?**
> Two reasons: it enables the two-pointer Two Sum inside the loop (O(1) space), and it puts duplicates next to each other, so skipping them is a simple comparison with the neighbour.

**Q2. Why compare `nums[i]` with `nums[i−1]` and not `nums[i+1]`?**
> Comparing with the previous element skips the **later** copies, after the first copy has already been used (with the other copies available as partners). Comparing with the next element would skip the first copy, missing triplets that need two equal values.

**Q3. What's the time complexity, and could hashing make it better?**
> O(n²). Hashing doesn't beat O(n²); it just swaps the two pointers for a set. The sort's O(n log n) is dominated by O(n²).

### Follow-up questions

**F1. 3Sum Closest (LC 16): the triplet sum closest to the target.**
> The same loop; track `best` by `|sum − target|`, and move the pointers by comparing the sum with the target. No duplicate skipping is needed for correctness.

**F2. 4Sum (LC 18).**
> Fix two numbers (two loops with duplicate skipping), then two pointers: O(n³). Use `long` for the sum (values up to 10⁹ overflow int when four are added).

**F3. kSum in general.**
> Recursion: `kSum(k, start, target)` fixes one number and calls `kSum(k−1, ...)`; the base case k = 2 is two pointers. O(n^(k−1)).

**F4. Count the triplets with sum < target (LC 259).**
> For a fixed i, if `nums[i] + nums[l] + nums[r] < target`, then all `r − l` pairs (l, l+1..r) work: add `r − l`, then `l++`; otherwise `r--`. O(n²).

**F5. Valid Triangle Number (LC 611): count the triplets that can form a triangle.**
> Sort, fix the **largest** side `c = nums[k]`, two pointers on [0, k): if `nums[l] + nums[r] > c`, then every l' in [l, r) works: add `r − l` and do `r--`; else `l++`.

**F6. The input is huge and doesn't fit in memory.**
> External sort, then for each fixed value run two pointers over the sorted file (two read streams). Or partition by value ranges, since a triplet summing to zero needs values from specific ranges.

---

## 10. Running the file

```bash
java M05_ThreeSum.java      # 4/4 tests passed
```

**One-line summary:** *"Sort; fix nums[i] (skip repeats); two-pointer Two Sum for −nums[i]; after a hit, skip the copies on both sides."*
