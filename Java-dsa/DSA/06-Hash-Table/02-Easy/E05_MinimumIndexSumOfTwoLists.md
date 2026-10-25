# E05 · Minimum Index Sum of Two Lists

| | |
|---|---|
| **LeetCode** | [#599 Minimum Index Sum of Two Lists](https://leetcode.com/problems/minimum-index-sum-of-two-lists/) |
| **Difficulty** | Easy |
| **Pattern** | A HashMap of value → index (index one list, scan the other) |
| **Code** | [E05_MinimumIndexSumOfTwoLists.java](E05_MinimumIndexSumOfTwoLists.java) |
| **Run** | `java E05_MinimumIndexSumOfTwoLists.java` |

---

## 1. The problem

Two people each have a list of favourite restaurants (no duplicates within a list). Among the **common** restaurants, find those with the smallest **index sum** (index in list1 + index in list2). Return **all** ties, in any order.

| list1 | list2 | answer |
|---|---|---|
| `["Shogun","Tapioca Express","Burger King","KFC"]` | `["KFC","Shogun","Burger King"]` | `["Shogun"]` (0 + 1 = 1) |
| `["happy","sad","good"]` | `["sad","happy","good"]` | `["sad","happy"]` (both sum to 1) |
| `["a","b","z"]` | `["x","y","z"]` | `["z"]` |

**Constraints:** up to 1000 strings each; at least one common string.

---

## 2. How to think about it

### Brute force
Compare every pair of strings: O(n · m · L), where L is the string length.

### Index one list, scan the other
"For each name in list2, where is it in list1?" is a lookup, so build a `HashMap` of **name → index** for list1. Then walk list2 with index j:
- the name is in the map at index i → `sum = i + j`
- `sum < best` → a new best: clear the answer list and add this name
- `sum == best` → a **tie**: add it too

### Early exit
Once `j > best`, every later name has a sum of at least j > best, so nothing better (or equal) can come. Stop.

---

## 3. Java concepts used in this problem

### 3.1 Strings as HashMap keys

`String` implements `hashCode()` and `equals()` based on its **characters**, so two different String objects with the same text find the same entry. Hashing a string costs O(L) the first time (Java caches a String's hash code afterwards).

### 3.2 `get` returning `Integer` (possibly null)

```java
Integer i = index1.get(list2[j]);
if (i == null) continue;
```

`continue` skips to the next loop iteration. Using `Integer` (not `int`) is required: `int i = index1.get(...)` would throw a NullPointerException for a missing name.

### 3.3 `List.clear()` and `toArray(new String[0])`

- `answer.clear()` empties the list (O(size)), used when a strictly better sum appears.
- `answer.toArray(new String[0])` converts the `List<String>` to a `String[]`. The `new String[0]` tells Java the array **type** to create; Java allocates one of the right size.

### 3.4 Two conditions in a `for` header

```java
for (int j = 0; j < list2.length && j <= best; j++)
```

The loop stops as soon as **either** condition fails. That's where the early exit lives.

---

## 4. Approach 1: Brute force

```java
static String[] findRestaurantBrute(String[] list1, String[] list2) {
    List<String> answer = new ArrayList<>();
    int best = Integer.MAX_VALUE;
    for (int i = 0; i < list1.length; i++)
        for (int j = 0; j < list2.length; j++)
            if (list1[i].equals(list2[j])) {
                if (i + j < best) { best = i + j; answer.clear(); }
                if (i + j == best) answer.add(list1[i]);
            }
    return answer.toArray(new String[0]);
}
```

Strings must be compared with `.equals`, not `==` (that compares references; see [Strings E01](../../02-Strings/02-Easy/E01_ValidAnagram.md)). **O(n · m · L).**

---

## 5. Approach 2: A HashMap of list1 (the file's solution)

### Code ([E05_MinimumIndexSumOfTwoLists.java:39-57](E05_MinimumIndexSumOfTwoLists.java#L39-L57))

```java
static String[] findRestaurant(String[] list1, String[] list2) {
    Map<String, Integer> index1 = new HashMap<>();
    for (int i = 0; i < list1.length; i++) index1.put(list1[i], i);
    List<String> answer = new ArrayList<>();
    int best = Integer.MAX_VALUE;
    for (int j = 0; j < list2.length && j <= best; j++) {      // early exit
        Integer i = index1.get(list2[j]);
        if (i == null) continue;
        int sum = i + j;
        if (sum < best) {
            best = sum;
            answer.clear();
            answer.add(list2[j]);
        } else if (sum == best) {
            answer.add(list2[j]);
        }
    }
    return answer.toArray(new String[0]);
}
```

### Line by line

| Line | What it does |
|---|---|
| the first loop | name → its index in list1 |
| `int best = Integer.MAX_VALUE;` | no common name found yet |
| `j <= best` | the early exit: past this point no sum can be ≤ best |
| `Integer i = index1.get(list2[j]);` | where is this name in list1? |
| `if (i == null) continue;` | not common: skip |
| `sum < best` | a strictly better sum: restart the answer list |
| `sum == best` | a tie: keep both |

### Dry run 1: `["Shogun","Tapioca Express","Burger King","KFC"]` vs `["KFC","Shogun","Burger King"]`

map = {Shogun: 0, Tapioca Express: 1, Burger King: 2, KFC: 3}

| j | list2[j] | i | sum | action | best | answer |
|---|---|---|---|---|---|---|
| 0 | KFC | 3 | 3 | 3 < ∞: new best | 3 | [KFC] |
| 1 | Shogun | 0 | 1 | 1 < 3: new best | 1 | [Shogun] |
| 2 | | | | `2 <= 1` fails → **stop** | | |

**["Shogun"]** ✓ (Burger King is never even looked up.)

### Dry run 2: `["happy","sad","good"]` vs `["sad","happy","good"]`

map = {happy: 0, sad: 1, good: 2}

| j | name | i | sum | action | best | answer |
|---|---|---|---|---|---|---|
| 0 | sad | 1 | 1 | new best | 1 | [sad] |
| 1 | happy | 0 | 1 | tie | 1 | [sad, happy] |
| 2 | | | | `2 <= 1` fails → stop | | |

**["sad", "happy"]** ✓

### Complexity
- **Time O((n + m) · L):** building the map, then one lookup per name of list2.
- **Space O(n · L)** for the map.

---

## 6. Comparing the approaches

| | Brute force | HashMap |
|---|---|---|
| Time | O(n · m · L) | **O((n + m) · L)** |
| Space | O(1) extra | O(n) entries |

**Tip:** build the map from the **shorter** list to save memory.

---

## 7. Edge cases and common mistakes

**Edge cases:** only the last items match, several ties, a single common name, identical lists.

**Mistakes:**
1. Comparing strings with `==`.
2. Forgetting ties (returning only the first).
3. Not clearing the answer list when a strictly better sum appears.
4. An `int` instead of `Integer` for the `get` result → NullPointerException.

---

## 8. Interview questions and answers

**Q1. Why is the early exit correct?**
> Every index in list1 is at least 0, so the sum for position j is at least j. Once j > best, no later name can tie or beat best.

**Q2. Which list should go into the map?**
> The shorter one: less memory, and the scan over the longer one is O(1) per lookup either way.

### Follow-up questions

**F1. k people's lists: minimise the total index sum.**
> A map of name → (count of lists containing it, sum of indices). Keep only the names whose count is k, then take the min.

**F2. Return the single best name, breaking ties alphabetically.**
> Track the best name, and replace it on a tie only if `name.compareTo(bestName) < 0`.

**F3. The lists are huge and stored on disk.**
> Load the smaller list as a map (or a sorted file you can binary-search), then stream the bigger one.

---

## 9. Running the file

```bash
java E05_MinimumIndexSumOfTwoLists.java      # 3/3 tests passed
```

**One-line summary:** *"Map list1's names to their indices, scan list2 keeping the minimum i + j and all ties, and stop once j exceeds the best sum."*
