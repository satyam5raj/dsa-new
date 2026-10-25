# H03 · Longest Duplicate Substring ⭐

| | |
|---|---|
| **LeetCode** | [#1044 Longest Duplicate Substring](https://leetcode.com/problems/longest-duplicate-substring/) |
| **Difficulty** | Hard |
| **Pattern** | Binary search on the answer + a rolling hash (Rabin–Karp) in a HashMap |
| **Code** | [H03_LongestDuplicateSubstring.java](H03_LongestDuplicateSubstring.java) |
| **Run** | `java H03_LongestDuplicateSubstring.java` |

⭐ = a bonus classic.

---

## 1. The problem

Return **any** longest substring that occurs **at least twice** in s (the occurrences may overlap), or `""` if there's none.

| s | answer |
|---|---|
| `"banana"` | `"ana"` (at indices 1 and 3; they overlap) |
| `"abcd"` | `""` |
| `"aaaaa"` | `"aaaa"` (at indices 0 and 1) |

**Constraints:** `2 ≤ n ≤ 3 · 10⁴`, lowercase letters.

---

## 2. How to think about it

### Brute force
For each length L, put all the substrings of length L into a HashSet. That's O(n) substrings per length, each costing up to O(n) to create and hash, for O(n) lengths: **O(n³)**. Impossible for n = 3 · 10⁴.

### Idea 1: binary search on the length
If a duplicate of length L exists, one of length L − 1 exists too (drop the last character of both copies). So "is there a duplicate of length L?" is **monotonic**:

```
L:          1     2     3     4     5
banana:   yes   yes   yes    no    no
                       ↑ the largest "yes"
```

Binary search finds the largest yes with **O(log n) checks**.

### Idea 2: check one length L in O(n) with a rolling hash
Hash every window of length L, **sliding** the hash in O(1) per step (Rabin–Karp, from [Strings 01-Basics](../../02-Strings/01-Basics/StringMatchingAlgorithms.java)), and store the hashes in a map. A hash seen twice means a candidate duplicate.

Two different strings can have the same hash (a **collision**), so **verify** each candidate by comparing the actual characters: store `hash → list of start positions`.

### Dry run on "banana"

```
lo = 1, hi = 5
mid = 3: windows ban, ana, nan, ana   → "ana" repeats (start 3) → yes, lo = 4
mid = 4: windows bana, anan, nana     → all different          → no,  hi = 3
lo > hi → the best length is 3, start 3 → "ana"
```

---

## 3. Java concepts used in this problem

### 3.1 The polynomial rolling hash

Treat a window as a number in base B, modulo a large prime M:

```
hash("ban") = ('b'·B² + 'a'·B + 'n') mod M
```

To slide from "ban" to "ana":

```
1. remove the left char:   hash − 'b' · B^(L−1)
2. shift and add the right: (hash · B + 'a')
```

Both steps are O(1). The file uses B = 131 and M = 1,000,000,007. Running the code confirms that rolling from "ban" gives exactly the direct hash of "ana" (1679124).

### 3.2 Modular arithmetic with `long`

```java
hash = (hash - s.charAt(start - 1) * power % MOD + MOD) % MOD;
```

- Values stay below M ≈ 10⁹, so a product like `hash * BASE` (about 1.3 · 10¹¹) fits in a `long` but **not** in an `int`.
- **`+ MOD` before the final `% MOD`**: the subtraction can go negative, and Java's `%` keeps the sign (as in [M03](../03-Medium/M03_ArrayPairsDivisibleByK.md)). Adding MOD brings it back into range.
- `char` values take part in the arithmetic as their numeric codes ('a' = 97).

### 3.3 `regionMatches`: comparing substrings without creating them

```java
s.regionMatches(other, s, start, L)    // do s[other .. other+L) and s[start .. start+L) match?
```

It compares in place, with no `substring` allocation. It's the collision check.

### 3.4 Binary search on the answer

```java
int mid = lo + (hi - lo) / 2;          // avoids (lo + hi) overflowing
if (feasible(mid)) { best = mid; lo = mid + 1; } else hi = mid - 1;
```

This template finds the largest feasible value when feasibility is monotonic. (Binary search is covered in [12-Advanced](../../12-Advanced/README.md).)

---

## 4. Approach 1: Brute force

### Code ([H03_LongestDuplicateSubstring.java:80-87](H03_LongestDuplicateSubstring.java#L80-L87))

```java
static int bruteLength(String s) {
    int best = 0;
    for (int L = 1; L < s.length(); L++) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i + L <= s.length(); i++) if (!set.add(s.substring(i, i + L))) { best = L; break; }
    }
    return best;
}
```

| Line | What it does |
|---|---|
| the outer loop | try every length |
| `set.add(s.substring(i, i + L))` | false → this substring appeared before → a duplicate of length L |
| `break` | one duplicate is enough for this L |

**O(n³)** time and up to O(n²) memory per length. It's the file's test oracle (on strings of length ≤ 25).

---

## 5. Approach 2: Binary search + Rabin–Karp (the file's solution)

### Code: the binary search ([H03_LongestDuplicateSubstring.java:43-57](H03_LongestDuplicateSubstring.java#L43-L57))

```java
static String longestDupSubstring(String s) {
    int lo = 1, hi = s.length() - 1, bestStart = -1, bestLen = 0;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        int start = findDuplicate(s, mid);
        if (start >= 0) {                 // a duplicate of length mid exists -> try longer
            bestStart = start;
            bestLen = mid;
            lo = mid + 1;
        } else {
            hi = mid - 1;                 // none -> try shorter
        }
    }
    return bestStart < 0 ? "" : s.substring(bestStart, bestStart + bestLen);
}
```

| Line | What it does |
|---|---|
| `hi = s.length() - 1` | a duplicate can't be as long as the whole string |
| `findDuplicate(s, mid)` | the start of a repeated substring of length mid, or −1 |
| found → `lo = mid + 1` | remember it and try longer |
| not found → `hi = mid - 1` | try shorter |
| the return | `""` if no length worked |

### Code: checking one length ([H03_LongestDuplicateSubstring.java:60-78](H03_LongestDuplicateSubstring.java#L60-L78))

```java
static int findDuplicate(String s, int L) {
    long hash = 0, power = 1;
    for (int i = 0; i < L; i++) {
        hash = (hash * BASE + s.charAt(i)) % MOD;
        if (i > 0) power = power * BASE % MOD;          // BASE^(L-1)
    }
    Map<Long, List<Integer>> seen = new HashMap<>();
    seen.computeIfAbsent(hash, k -> new ArrayList<>()).add(0);
    for (int start = 1; start + L <= s.length(); start++) {
        hash = (hash - s.charAt(start - 1) * power % MOD + MOD) % MOD;   // drop the left char
        hash = (hash * BASE + s.charAt(start + L - 1)) % MOD;             // add the right char
        List<Integer> candidates = seen.computeIfAbsent(hash, k -> new ArrayList<>());
        for (int other : candidates) {
            if (s.regionMatches(other, s, start, L)) return start;       // verify (rule out a collision)
        }
        candidates.add(start);
    }
    return -1;
}
```

| Line | What it does |
|---|---|
| the first loop | the hash of the first window, and `power` = B^(L−1) (the weight of the leftmost character) |
| `seen…add(0)` | window 0's hash → its start position |
| `hash − charAt(start−1) · power` | remove the character that just left the window |
| `hash · BASE + charAt(start+L−1)` | shift and append the new character |
| `candidates` | the earlier windows with the same hash |
| `regionMatches` | a real match (not just a hash collision) → return this start |
| `candidates.add(start)` | remember this window |

### Dry run: `findDuplicate("banana", 3)`

| start | window | hash new? | action |
|---|---|---|---|
| 0 | ban | (initial) | seen {h(ban): [0]} |
| 1 | ana | new | add [1] |
| 2 | nan | new | add [2] |
| 3 | ana | **same as start 1** | `regionMatches(1, s, 3, 3)`: "ana" = "ana" → **return 3** |

And `findDuplicate("banana", 4)`: bana, anan, nana are all different → −1.

### Dry run: `"aaaaa"`
lo = 1, hi = 4 → mid = 2 ("aa" at 0 and 1) yes, lo = 3 → mid = 3 yes, lo = 4 → mid = 4 ("aaaa" at 0 and 1, overlapping) yes, lo = 5 > hi → **"aaaa"** ✓

### Complexity
- **Time O(n log n) on average:** O(log n) lengths × O(n) windows. Collisions with M ≈ 10⁹ are rare, so the verification adds little.
- **Space O(n)** for the map.

The file runs a 30,000-character random string in milliseconds.

---

## 6. Comparing the approaches

| | Brute force | Binary search + rolling hash | Suffix array / automaton |
|---|---|---|---|
| Time | O(n³) | **O(n log n)** average | O(n log n) or O(n) |
| Space | O(n²) | O(n) | O(n) |
| Difficulty | easy | medium | hard to code in an interview |

---

## 7. Edge cases and common mistakes

**Edge cases:** no duplicate (`""`), all the same character (overlapping copies), the duplicate at the very end, n = 2.

**Mistakes:**
1. `int` overflow in the hash arithmetic (use `long`).
2. A negative hash after the subtraction (`+ MOD` is missing).
3. Trusting the hash without verifying (a collision gives a wrong answer).
4. `power` = B^L instead of B^(L−1).
5. Binary searching with `hi = n` (a duplicate can't have length n).

---

## 8. Interview questions and answers

**Q1. Why is binary search valid?**
> Feasibility is monotonic: a duplicate of length L contains duplicates of every shorter length (its prefixes). So the "yes" lengths form a prefix 1..best.

**Q2. How do you handle hash collisions?**
> Verify the candidate with an actual character comparison. Alternatively, use **double hashing** (two different moduli) so that a false match is astronomically unlikely, at the cost of a small chance of being wrong if you skip verification.

**Q3. Why use a large prime modulus?**
> It spreads the hashes and keeps collisions rare (about 1 in 10⁹ per pair). The base should be larger than the alphabet size.

### Follow-up questions

**F1. The longest substring that appears at least k times.**
> The same binary search; in the check, count the verified occurrences per hash and test for ≥ k.

**F2. The longest common substring of two strings.**
> Binary search the length; hash all the windows of s1 into a set, then slide over s2 looking for a verified match.

**F3. The suffix-array solution.**
> Build the suffix array and the LCP array; the answer is the maximum LCP value between adjacent suffixes. That's O(n log n) and deterministic, but much more code.

---

## 9. Running the file

```bash
java H03_LongestDuplicateSubstring.java      # 4/4 tests passed (including 500 random strings vs brute force, and n = 30,000 timed)
```

**One-line summary:** *"Binary search the length (feasibility is monotonic); for each length, slide a rolling hash over all windows, store hash → starts, and verify matches with regionMatches."*
