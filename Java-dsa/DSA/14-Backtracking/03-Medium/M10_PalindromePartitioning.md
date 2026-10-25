# M10 · Palindrome Partitioning

| | |
|---|---|
| **LeetCode** | [#131 Palindrome Partitioning](https://leetcode.com/problems/palindrome-partitioning/) |
| **Difficulty** | Medium |
| **Pattern** | **Partition** backtracking: choose where the next piece ends (+ a palindrome table) |
| **Code** | [M10_PalindromePartitioning.java](M10_PalindromePartitioning.java) |
| **Run** | `java M10_PalindromePartitioning.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Split `s` into pieces so that **every** piece is a palindrome. Return all such splits.

| s | answer |
|---|---|
| "aab" | [["a","a","b"], ["aa","b"]] |
| "a" | [["a"]] |
| "aaaa" | 8 splits (every cut pattern works) |

---

## 2. How to think about it

### The partition template
At position `start`, the next piece is `s[start..end]` for some `end ≥ start`. Try every `end`; if the piece is valid, add it and recurse from `end + 1`.

```
bt(start):
    if start == n: record
    for end in start..n−1:
        if isPal(start, end):  add the piece;  bt(end + 1);  remove it
```

The same shape solves [Restore IP Addresses (M11)](M11_RestoreIPAddresses.md), Word Break II, splitting a string into increasing numbers, and more.

### Speed-up: a palindrome table
Checking each piece from scratch costs O(n). Instead, precompute

```
pal[i][j] = s[i] == s[j] && (j − i < 2 || pal[i+1][j−1])
```

in O(n²). It's a small DP: a string is a palindrome if its ends match and its inside is a palindrome. Because `pal[i][j]` depends on row i + 1, fill the rows from the bottom up. Then each check is O(1).

### The table for "aab"

| i \ j | 0 (a) | 1 (a) | 2 (b) |
|---|---|---|---|
| 0 | T | T ("aa") | F ("aab") |
| 1 | | T | F ("ab") |
| 2 | | | T |

### Dry run: "aab"

```
bt(0)
├─ end 0: "a" ✓ → bt(1)
│   ├─ end 1: "a" ✓ → bt(2)
│   │   └─ end 2: "b" ✓ → bt(3): record [a, a, b] ✓
│   └─ end 2: "ab" ✗ skip
├─ end 1: "aa" ✓ → bt(2)
│   └─ end 2: "b" ✓ → bt(3): record [aa, b] ✓
└─ end 2: "aab" ✗ skip
```

---

## 3. Java concepts used in this problem

### 3.1 A 2D boolean table filled in a specific order

```java
boolean[][] pal = new boolean[n][n];
for (int i = n - 1; i >= 0; i--)
    for (int j = i; j < n; j++)
        pal[i][j] = s.charAt(i) == s.charAt(j) && (j - i < 2 || pal[i + 1][j - 1]);
```

- `i` runs **backwards**, so `pal[i + 1][...]` is already computed when row i needs it.
- `j − i < 2` covers lengths 1 and 2, where matching ends are enough. It also short-circuits before `pal[i + 1][j − 1]` could be read with j − 1 < i + 1.

### 3.2 `substring(start, end + 1)`

`substring` excludes its end index, so the piece s[start..end] (inclusive) is `substring(start, end + 1)`.

### 3.3 A list of strings as the path

`List<String> path` holds the pieces chosen so far; `path.remove(path.size() − 1)` removes the last piece (by index).

### 3.4 Cut patterns as a bitmask (in the referee)

For a string of length n there are n − 1 places to cut. Bit i of `mask` says whether to cut after character i, so masks 0..2ⁿ⁻¹ − 1 cover every split. `Math.max(0, n − 1)` handles the empty string.

### 3.5 Reversing to test a palindrome

`new StringBuilder(p).reverse().toString().equals(p)` is the simplest palindrome check, used in the referee.

---

## 4. Approach 1: Every cut pattern (the referee)

### Code ([M10_PalindromePartitioning.java:57-69](M10_PalindromePartitioning.java#L57-L69))

```java
static Set<List<String>> brute(String s) {
    Set<List<String>> out = new HashSet<>();
    int n = s.length();
    for (int mask = 0; mask < (1 << Math.max(0, n - 1)); mask++) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < n; i++) if (i == n - 1 || (mask >> i & 1) == 1) { parts.add(s.substring(start, i + 1)); start = i + 1; }
        boolean ok = true;
        for (String p : parts) ok &= new StringBuilder(p).reverse().toString().equals(p);
        if (ok) out.add(parts);
    }
    return out;
}
```

| Line | What it does |
|---|---|
| the mask loop | every possible set of cuts |
| the inner loop | cut after i if bit i is set, and always after the last character |
| the palindrome check | keep the split only if every piece reads the same backwards |

- **Time O(n · 2ⁿ)** always.

---

## 5. Approach 2: Partition backtracking + palindrome table (the file's main solution)

### Code ([M10_PalindromePartitioning.java:35-54](M10_PalindromePartitioning.java#L35-L54))

```java
static List<List<String>> partition(String s) {
    int n = s.length();
    boolean[][] pal = new boolean[n][n];
    for (int i = n - 1; i >= 0; i--)                              // i from the end: pal[i+1] is ready
        for (int j = i; j < n; j++)
            pal[i][j] = s.charAt(i) == s.charAt(j) && (j - i < 2 || pal[i + 1][j - 1]);
    List<List<String>> out = new ArrayList<>();
    bt(s, 0, pal, new ArrayList<>(), out);
    return out;
}

static void bt(String s, int start, boolean[][] pal, List<String> path, List<List<String>> out) {
    if (start == s.length()) { out.add(new ArrayList<>(path)); return; }
    for (int end = start; end < s.length(); end++) {
        if (!pal[start][end]) continue;                          // this piece isn't a palindrome
        path.add(s.substring(start, end + 1));
        bt(s, end + 1, pal, path, out);
        path.remove(path.size() - 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the table | O(1) palindrome checks later |
| `start == s.length()` | the whole string is covered by palindromes: record |
| the `end` loop | every possible length for the next piece |
| `continue` | prune: a non-palindrome piece can never be part of an answer |
| add / recurse / remove | choose the piece, partition the rest, un-choose |

### Complexity
- **Time O(n · 2ⁿ)** in the worst case ("aaaa…", where every one of the 2ⁿ⁻¹ splits works).
- **Space O(n²)** for the table, plus O(n) recursion.

---

## 6. Comparing the approaches

| | Every cut pattern | Partition backtracking |
|---|---|---|
| Time | O(n · 2ⁿ) always | O(n · 2ⁿ) worst, much less when few pieces are palindromes |
| Palindrome check | O(length) each | O(1) with the table |

The file compares the two on 500 random strings of a's and b's (lots of palindromes) and checks that no split is produced twice.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single character, all characters equal (2ⁿ⁻¹ splits), no palindromes longer than 1 (exactly one split: all single characters).

**Mistakes:**
1. Filling the table with i going forward (`pal[i + 1]` isn't ready).
2. `substring(start, end)` (drops the last character of the piece).
3. Recursing from `end` instead of `end + 1`.

---

## 8. Interview questions and answers

**Q1. Why precompute the table?**
> The same substring is checked many times across different branches. The table makes each check O(1) for an O(n²) one-time cost.

**Q2. What's the worst case?**
> A string of one repeated character: every piece is a palindrome, so all 2ⁿ⁻¹ ways to cut are valid answers.

### Follow-up questions

**F1. Palindrome Partitioning II (LC 132): the minimum number of cuts.**
> DP: cuts[j] = min over i ≤ j with pal[i][j] of cuts[i − 1] + 1. O(n²), no backtracking.

**F2. Restore IP Addresses.** → [M11](M11_RestoreIPAddresses.md)
> The same partition template with "a valid IP part" as the piece check, and exactly 4 pieces.

**F3. Word Break II (LC 140).**
> Pieces must be dictionary words; memoize the results for each `start` to avoid recomputing suffixes.

---

## 9. Running the file

```bash
java M10_PalindromePartitioning.java      # 4/4 tests passed (including 500 random strings vs all cut patterns)
```

**One-line summary:** *"Precompute pal[i][j] bottom-up, then from each start try every end whose piece is a palindrome, add it, and recurse on the rest; record when the whole string is used."*
