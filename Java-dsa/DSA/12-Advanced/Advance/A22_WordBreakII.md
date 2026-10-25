# A22 · Word Break II ⭐

| | |
|---|---|
| **LeetCode** | [#140 Word Break II](https://leetcode.com/problems/word-break-ii/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Backtracking** + **memo** (HashMap start → sentences) + **HashSet** dictionary |
| **Code** | [A22_WordBreakII.java](A22_WordBreakII.java) |
| **Run** | `java A22_WordBreakII.java` |

⭐ = not in `leetcode.md`. It's the "list them all" sequel to [Word Break](../../15-Dynamic-Programming/03-Medium/M13_WordBreak.md).

---

## 1. The problem

Given a string `s` and a dictionary, return **all** the ways to split `s` into dictionary words, as space-separated sentences (in any order). Words can be reused.

```
s = "catsanddog", dict = [cat, cats, and, sand, dog]
→ ["cats and dog", "cat sand dog"]

s = "pineapplepenapple", dict = [apple, pen, applepen, pine, pineapple]
→ ["pine apple pen apple", "pineapple pen apple", "pine applepen apple"]

s = "catsandog", dict = [cats, dog, sand, and, cat]
→ []
```

---

## 2. How to think about it

### Word Break I vs II
Word Break I asks **"is there a split?"**, a yes/no DP. Here we must **list** every split, so we need to build sentences: backtracking.

### Backtracking
At position `start`, try every dictionary word that starts there and recurse on the rest:
```
split(start):
    if start == n: record the current sentence
    for each end: if s[start..end) is a word: choose it, split(end), un-choose it
```
It's correct, but the same suffix is solved again and again. For `s = "aaaa…ab"` with `[a, aa, aaa, aaaa]` there is **no** valid split, yet plain backtracking explores an exponential number of ways to cut the a's before discovering that the final `b` fails every time.

### The key observation: suffixes don't care how you got there
The sentences for the suffix starting at `i` are the same no matter which words came before. So compute them **once** per `i`:

```
solve(n) = [""]                            one way to split nothing
solve(i) = for each word w = s[i..j) in the dictionary:
               for each rest in solve(j):  w + (rest empty ? "" : " " + rest)
```
Cache `solve(i)` in a `HashMap<Integer, List<String>>`. A suffix that **can't** be split caches an **empty list**, so it's never explored again; that's what makes the `"aaa…ab"` case instant.

Two small speed-ups:
- a `HashSet` makes "is this a word?" O(1) (plus building the substring);
- the **longest word length** bounds how far `end` needs to go.

### The honest limit
The number of answers can itself be exponential: `"aaaa…a"` with `[a, aa]` has a Fibonacci number of splits. No algorithm can list them faster than writing them all out. Memo removes the **repeated** work, not the output.

### Dry run: "catsanddog"
| call | words found | result |
|---|---|---|
| solve(10) | | `[""]` |
| solve(7) | "dog" → solve(10) | `["dog"]` |
| solve(4) | "and" → solve(7) | `["and dog"]` |
| solve(3) | "sand" → solve(7) | `["sand dog"]` |
| solve(0) | "cat" → solve(3); "cats" → solve(4) | `["cat sand dog", "cats and dog"]` |

solve(7) is reached twice (via "sand" and via "and") but computed once.

---

## 3. Java concepts used in this problem

### 3.1 `HashMap<Integer, List<String>>` as a memo
Keys are start positions, values are the finished sentence lists. `memo.get(start)` returns `null` when nothing is cached yet; an **empty list** means "computed: no way". The difference between `null` and empty matters.

### 3.2 `List.of("")`
An immutable list holding one empty string: "there is exactly one way to split the empty suffix, using no words". It's the base case that makes the concatenation work.

### 3.3 `substring` and `String.join`
`s.substring(start, end)` copies characters `start..end−1` (O(length)). `String.join(" ", path)` glues the chosen words with spaces in the brute force.

### 3.4 Backtracking's choose / recurse / undo
```java
path.add(word);
backtrack(s, end, words, path, out);
path.remove(path.size() - 1);                                // undo the choice
```
The same list is reused for every branch, so each choice must be undone. See [Backtracking basics](../../14-Backtracking/01-Basics/BacktrackingTemplate.java).

---

## 4. Approach 1: Plain backtracking (the referee)

### Code ([A22_WordBreakII.java:48-63](A22_WordBreakII.java#L48-L63))
```java
static List<String> wordBreakBrute(String s, List<String> wordDict) {
    List<String> out = new ArrayList<>();
    backtrack(s, 0, new HashSet<>(wordDict), new ArrayList<>(), out);
    return out;
}

private static void backtrack(String s, int start, Set<String> words, List<String> path, List<String> out) {
    if (start == s.length()) { out.add(String.join(" ", path)); return; }
    for (int end = start + 1; end <= s.length(); end++) {
        String word = s.substring(start, end);
        if (!words.contains(word)) continue;
        path.add(word);
        backtrack(s, end, words, path, out);
        path.remove(path.size() - 1);                                // undo the choice
    }
}
```

It's correct, but suffixes are re-solved: **exponential** even when the answer is empty.

---

## 5. Approach 2: Backtracking + memo on the start index (main solution)

### Code ([A22_WordBreakII.java:66-85](A22_WordBreakII.java#L66-L85))
```java
static List<String> wordBreak(String s, List<String> wordDict) {
    Set<String> words = new HashSet<>(wordDict);
    int maxLen = 0;
    for (String w : wordDict) maxLen = Math.max(maxLen, w.length());
    return solve(s, 0, words, maxLen, new HashMap<>());
}

private static List<String> solve(String s, int start, Set<String> words, int maxLen, Map<Integer, List<String>> memo) {
    if (start == s.length()) return List.of("");                      // one way to split the empty suffix
    List<String> cached = memo.get(start);
    if (cached != null) return cached;
    List<String> out = new ArrayList<>();
    for (int end = start + 1; end <= Math.min(s.length(), start + maxLen); end++) {
        String word = s.substring(start, end);
        if (!words.contains(word)) continue;
        for (String rest : solve(s, end, words, maxLen, memo)) out.add(rest.isEmpty() ? word : word + " " + rest);
    }
    memo.put(start, out);                                             // an empty list means "can't be split"
    return out;
}
```

### Line by line
| Line | What it does |
|---|---|
| `words`, `maxLen` | O(1) lookups; never try substrings longer than any word |
| base case `List.of("")` | the end of the string: one (empty) way |
| `memo.get(start)` | already solved: reuse |
| the `end` loop | each possible first word |
| `for (String rest : solve(end))` | every way to finish after that word |
| `rest.isEmpty() ? word : word + " " + rest` | no trailing space when the word is the last one |
| `memo.put(start, out)` | cache, including empty results |

### Complexity
- O(n · L) substring checks across all positions (L = longest word), each costing O(L) to build: **O(n · L²)** for the search, plus **O(size of the output)** to build the sentences.
- Space: the memo holds every suffix's sentences.

---

## 6. Comparing the approaches

| | Time | When the answer is empty |
|---|---|---|
| Plain backtracking | exponential | still exponential |
| **Memo on start index** | O(n · L²) + output size | fast (empty lists are cached) |

The file checks both on 1,500 random cases, and runs the memo version on 150 a's followed by a b (instant; plain backtracking would never finish).

---

## 7. Edge cases and common mistakes

**Edge cases:** no split at all, the whole string being one word, words that are prefixes of other words (cat / cats), repeated use of a word, a dictionary word longer than `s`.

**Mistakes:**
1. Memoising only successful positions (treating a missing key and "no solution" the same), so dead ends are re-explored.
2. Adding a trailing space at the end of each sentence.
3. Returning a shared mutable list from the base case and modifying it later.
4. Checking every substring length up to n instead of up to the longest word.
5. Running Word Break I's boolean DP and thinking it lists the splits.

---

## 8. Interview questions and answers

**Q1. Why does memoisation work here?**
> The sentences for a suffix depend only on where the suffix starts, not on the words chosen before it. So each start index is solved once and its list reused.

**Q2. Why cache empty results?**
> They're the expensive part: a suffix with no split might be reached exponentially many times. Caching the empty list stops each one after its first visit.

**Q3. What's the complexity?**
> The search is polynomial: O(n · L) substring checks. But the output itself can be exponential, so the total time is "polynomial plus the size of the answer".

### Follow-up questions

**F1. "Only count the splits."**
> A pure DP: `ways[i] = sum of ways[j]` for each word `s[i..j)`. No strings needed.

**F2. "Huge dictionary."**
> Use a trie and walk it from `start` character by character: you stop as soon as no word continues, and every word ending is found in one pass.

**F3. "Prune before listing."**
> Run Word Break I first; if the whole string can't be split, return `[]` straight away. More generally, mark which suffixes are splittable and skip the others.

---

## 9. Running the file

```bash
java A22_WordBreakII.java     # 5/5 tests passed (3 LeetCode examples, 150 a's + b, 1,500 random cases)
```

**One-line summary:** *"Backtrack over the first word of each suffix, but memoise solve(start) → list of sentences in a HashMap (including empty lists for dead ends), with a HashSet for word lookups and the longest word length bounding each search."*
