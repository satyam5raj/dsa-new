# E11 · Longest Common Prefix ⭐

| | |
|---|---|
| **LeetCode** | [#14 Longest Common Prefix](https://leetcode.com/problems/longest-common-prefix/) |
| **Difficulty** | Easy |
| **Pattern** | Vertical scan (compare column by column) |
| **Code** | [E11_LongestCommonPrefix.java](E11_LongestCommonPrefix.java) |
| **Run** | `java E11_LongestCommonPrefix.java` |

⭐ = a classic added as a bonus (it isn't in `leetcode.md`).

---

## 1. The problem

Return the longest prefix shared by **all** strings in the array (or `""`).

| strs | answer |
|---|---|
| `["flower", "flow", "flight"]` | `"fl"` |
| `["dog", "racecar", "car"]` | `""` |
| `["ab", "a"]` | `"a"` |
| `["same", "same"]` | `"same"` |

**Constraints:** `1 ≤ strs.length ≤ 200`, `0 ≤ strs[i].length ≤ 200`.

---

## 2. How to think about it

### Vertical scan
Line the strings up and read **column by column** (index i = 0, 1, 2, …). Stop at the first column where some string **has ended** or has a **different character**:

```
f l o w e r
f l o w
f l i g h t
✓ ✓ ✗          → "fl"
```

The prefix can't be longer than any string, and in particular not longer than the first one, so we loop over the first string's columns.

### Other approaches worth mentioning
- **Horizontal scan:** prefix = strs[0]; shrink it until each next string starts with it.
- **Sort trick:** after sorting, the LCP of the whole array = the LCP of the **first and last** strings (the most different pair in sorted order).
- **Trie:** insert all the words and walk down while there's exactly one child (useful for many queries).
- **Binary search** on the prefix length.

---

## 3. Java concepts used in this problem

### 3.1 `substring(begin, end)`

```java
"flower".substring(0, 2)   // "fl": from index 0 up to but NOT including 2
"flower".substring(2)      // "ower": to the end
"flower".substring(0, 0)   // "": an empty string
```

The end index is **exclusive**, so `substring(0, i)` has exactly i characters. Since Java 7u6, `substring` **copies** the characters (O(length)); older versions shared the array.

### 3.2 `startsWith`

`"flower".startsWith("fl")` → `true`. It's used in the horizontal-scan approach.

### 3.3 Sorting strings: lexicographic order

`Arrays.sort(String[])` orders strings like a dictionary (by `compareTo`, character by character using char codes). Note that uppercase sorts before lowercase (`'Z'` = 90 < `'a'` = 97), and a prefix comes before longer strings (`"flow"` < `"flower"`).

### 3.4 `strs.clone()`

It copies the **array of references**, so the caller's array order is preserved. The String objects themselves are shared, which is fine because strings are immutable.

---

## 4. Approach 1: Horizontal scan

```java
static String lcpHorizontal(String[] strs) {
    String prefix = strs[0];
    for (int k = 1; k < strs.length; k++) {
        while (!strs[k].startsWith(prefix)) {
            prefix = prefix.substring(0, prefix.length() - 1);   // drop the last character
            if (prefix.isEmpty()) return "";
        }
    }
    return prefix;
}
```

**Dry run** `["flower","flow","flight"]`: prefix `"flower"` → "flow" doesn't start with it → shrink to `"flowe"` → `"flow"` ✓ → "flight": shrink `"flo"` → `"fl"` ✓ → **"fl"**.

**O(S) time** (S = the total number of characters) in the worst case, plus the substring copies.

---

## 5. Approach 2: Vertical scan (the file's solution)

### Code ([E11_LongestCommonPrefix.java:36-48](E11_LongestCommonPrefix.java#L36-L48))

```java
static String longestCommonPrefix(String[] strs) {
    if (strs.length == 0) return "";
    String first = strs[0];
    for (int i = 0; i < first.length(); i++) {             // column i
        char c = first.charAt(i);
        for (int k = 1; k < strs.length; k++) {
            if (i == strs[k].length() || strs[k].charAt(i) != c) {
                return first.substring(0, i);             // this column breaks
            }
        }
    }
    return first;                                         // the first string itself is the prefix
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (strs.length == 0) return "";` | there are no strings |
| `String first = strs[0];` | the reference string; the prefix can't be longer than it |
| `for (int i = 0; i < first.length(); i++)` | column i |
| `char c = first.charAt(i);` | the character every string must have in this column |
| `if (i == strs[k].length() \|\| strs[k].charAt(i) != c)` | string k has ended (check this **first**, so `charAt` is safe) or differs |
| `return first.substring(0, i);` | the columns 0..i−1 were common |
| `return first;` | every column of the first string matched, so the whole first string is the prefix |

### Dry run 1: `["flower", "flow", "flight"]`

| i | c | "flow" | "flight" | column ok? |
|---|---|---|---|---|
| 0 | f | f ✓ | f ✓ | yes |
| 1 | l | l ✓ | l ✓ | yes |
| 2 | o | o ✓ | **i** ✗ | **no → return "fl"** |

### Dry run 2: `["ab", "a"]`

| i | c | "a" | |
|---|---|---|---|
| 0 | a | a ✓ | ok |
| 1 | b | i == length 1 → ended | **return "a"** |

### Dry run 3: `["dog", "racecar", "car"]`
i = 0: 'd' vs 'r' → **return ""** ✓

### Complexity
- **Time O(S)** in the worst case (all strings equal), but it stops at the first bad column, so often far less: about O(n × LCP length).
- **Space O(1)** (besides the returned substring).

---

## 6. Approach 3: Sort, then compare the first and last (the file's second method)

### Code ([E11_LongestCommonPrefix.java:50-57](E11_LongestCommonPrefix.java#L50-L57))

```java
static String longestCommonPrefixSorted(String[] strs) {
    String[] a = strs.clone();
    Arrays.sort(a);
    String lo = a[0], hi = a[a.length - 1];
    int i = 0;
    while (i < lo.length() && i < hi.length() && lo.charAt(i) == hi.charAt(i)) i++;
    return lo.substring(0, i);
}
```

**Why only the first and last?** In sorted order, any prefix shared by the first and last strings is shared by everything in between (each string in between is "sandwiched" between them in dictionary order). And any prefix shared by all strings is obviously shared by those two.

**Dry run:** sorted `["flight", "flow", "flower"]` → lo = "flight", hi = "flower" → f = f, l = l, i ≠ o → **"fl"** ✓

**O(S log n)** for the sort (each comparison can cost up to the string length). It's elegant, but slower than the vertical scan.

---

## 7. Comparing the approaches

| | Horizontal | Vertical | Sort first/last | Trie |
|---|---|---|---|---|
| Time | O(S) | **O(S)**, early stop | O(S log n) | O(S) to build |
| Extra space | O(1) + copies | **O(1)** | O(n) clone | O(S) |
| Best when | — | **default** | a one-liner | many prefix queries |

---

## 8. Edge cases and common mistakes

**Edge cases:** one string (it's the answer), an empty string in the array (answer `""`), all strings identical, a shorter string ending first.

**Mistakes:**
1. Calling `strs[k].charAt(i)` before checking `i < strs[k].length()` → `StringIndexOutOfBoundsException`.
2. Returning `substring(0, i + 1)` (off by one).
3. Forgetting to return `first` when the loop finishes without a mismatch.

---

## 9. Interview questions and answers

**Q1. What's the time complexity of the vertical scan?**
> O(S) in the worst case (every character is compared once), but it stops at the first column that breaks, so it's O(n × LCP) in practice.

**Q2. Why does the sort trick work?**
> In lexicographic order, the strings between the first and last share every prefix that those two share. So the LCP of the extremes is the LCP of everything.

**Q3. When would you use a trie?**
> When you have many queries against the same set of words, such as "the LCP of a subset" or autocomplete. Build once, and each walk is O(prefix length).

### Follow-up questions

**F1. Longest common suffix.**
> Compare from the ends (or reverse the strings).

**F2. Longest common substring of two strings (not a prefix).**
> DP: `dp[i][j] = dp[i−1][j−1] + 1` if the characters match, else 0. O(nm). Or suffix structures / binary search + rolling hash.

**F3. Search suggestions / autocomplete.** → [10-Trie M04](../../10-Trie/03-Medium/M04_SearchSuggestionsSystem.md)

---

## 10. Running the file

```bash
java E11_LongestCommonPrefix.java      # 5/5 tests passed
```

**One-line summary:** *"Scan column by column over the first string; stop when any string ends or differs."*
