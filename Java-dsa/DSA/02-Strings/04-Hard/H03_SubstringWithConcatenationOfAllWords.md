# H03 · Substring with Concatenation of All Words ⭐

| | |
|---|---|
| **LeetCode** | [#30 Substring with Concatenation of All Words](https://leetcode.com/problems/substring-with-concatenation-of-all-words/) |
| **Difficulty** | Hard |
| **Pattern** | Sliding window over **words** (word-sized steps, one pass per offset) |
| **Code** | [H03_SubstringWithConcatenationOfAllWords.java](H03_SubstringWithConcatenationOfAllWords.java) |
| **Run** | `java H03_SubstringWithConcatenationOfAllWords.java` |

⭐ = a bonus classic.

---

## 1. The problem

All the words have the **same length L**. Find every start index in `s` where a concatenation of **all** the words appears, with each word used **exactly once**, in any order.

| s | words | answer | matches |
|---|---|---|---|
| `"barfoothefoobarman"` | `[foo, bar]` | `[0, 9]` | `barfoo`, `foobar` |
| `"wordgoodgoodgoodbestword"` | `[word, good, best, word]` | `[]` | |
| `"barfoofoobarthefoobarman"` | `[bar, foo, the]` | `[6, 9, 12]` | `foobarthe`, `barthefoo`, `thefoobar` |
| `"aaaa"` | `[aa, aa]` | `[0]` | |

**Constraints:** `1 ≤ |s| ≤ 10⁴`, `1 ≤ k ≤ 5000` words, `1 ≤ L ≤ 30`.

---

## 2. How to think about it

### Brute force
For each start i, cut the next k chunks of length L and compare their counts with the words' counts. That's **O(n · k · L)**.

### Key insight: think in word-sized tokens
A valid window is made of **word-sized chunks**. If you start at an offset in `0..L−1` and jump L characters at a time, you see a sequence of **tokens**:

```
offset 0:  bar | foo | foo | bar | the | foo | bar | man
```

Now the problem is: **find the windows of exactly k consecutive tokens whose counts match the words**. That's the same counting sliding window as [Find All Anagrams](../03-Medium/M05_FindAllAnagrams.md), with **words** in place of characters.

```
for offset in 0..L−1:
    left = offset, seen = {}, count = 0
    for right = offset; right + L ≤ n; right += L:
        w = s[right .. right+L)
        if w isn't one of the words: reset (seen = {}, count = 0, left = right + L)
        else:
            seen[w]++, count++
            while seen[w] > need[w]: drop the leftmost token (left += L, count−−)
            if count == k: record left, then drop the leftmost token to keep sliding
```

### Why L offsets?
A match can start at **any** index. The tokens line up with a match only when we start at that match's index mod L. The L passes together cover every possible start.

---

## 3. Java concepts used in this problem

### 3.1 `Map<String, Integer>` counts of words

```java
need.merge(w, 1, Integer::sum);       // count each word (duplicates allowed!)
seen.merge(drop, -1, Integer::sum);   // remove one copy
```

Strings make good keys: `equals`/`hashCode` are based on content.

### 3.2 `Map.equals` (used in the brute force)

```java
got.equals(need)
```

Two maps are equal if they have the **same key-value pairs**. That's handy for comparing word counts in one call. (A key with value 0 is **not** the same as a missing key, which is why the fast version doesn't compare whole maps.)

### 3.3 `continue` to skip the rest of the iteration

When a token isn't one of the words, we reset and `continue` to the next token.

### 3.4 Comparing lists in the tests

`findSubstring(s, words).equals(brute(s, words))`: `List.equals` compares the elements in order. Both results are sorted, so that works.

---

## 4. Approach 1: Brute force (count the k chunks at each start)

### Code ([H03_SubstringWithConcatenationOfAllWords.java:84-95](H03_SubstringWithConcatenationOfAllWords.java#L84-L95))

```java
static List<Integer> brute(String s, String[] words) {
    List<Integer> res = new ArrayList<>();
    int L = words[0].length(), k = words.length;
    Map<String, Integer> need = new HashMap<>();
    for (String w : words) need.merge(w, 1, Integer::sum);
    for (int i = 0; i + L * k <= s.length(); i++) {
        Map<String, Integer> got = new HashMap<>();
        for (int j = 0; j < k; j++) got.merge(s.substring(i + j * L, i + (j + 1) * L), 1, Integer::sum);
        if (got.equals(need)) res.add(i);
    }
    return res;
}
```

| Line | What it does |
|---|---|
| `i + L * k <= s.length()` | the whole concatenation must fit |
| inner loop | cut chunk j: `s[i + jL, i + (j+1)L)` |
| `got.equals(need)` | the same multiset of words |

**O(n · k · L).**

---

## 5. Approach 2: A word-level sliding window per offset (the file's solution)

### Code ([H03_SubstringWithConcatenationOfAllWords.java:45-82](H03_SubstringWithConcatenationOfAllWords.java#L45-L82))

```java
static List<Integer> findSubstring(String s, String[] words) {
    List<Integer> result = new ArrayList<>();
    if (words.length == 0) return result;
    int L = words[0].length(), k = words.length, n = s.length();
    Map<String, Integer> need = new HashMap<>();
    for (String w : words) need.merge(w, 1, Integer::sum);

    for (int offset = 0; offset < L; offset++) {
        Map<String, Integer> seen = new HashMap<>();
        int left = offset, count = 0;
        for (int right = offset; right + L <= n; right += L) {
            String w = s.substring(right, right + L);
            if (!need.containsKey(w)) {                 // a token that can't be part of any match
                seen.clear();
                count = 0;
                left = right + L;
                continue;
            }
            seen.merge(w, 1, Integer::sum);
            count++;
            while (seen.get(w) > need.get(w)) {         // too many copies of w: shrink
                String drop = s.substring(left, left + L);
                seen.merge(drop, -1, Integer::sum);
                count--;
                left += L;
            }
            if (count == k) {                           // exactly the multiset of words
                result.add(left);
                String drop = s.substring(left, left + L);
                seen.merge(drop, -1, Integer::sum);
                count--;
                left += L;
            }
        }
    }
    Collections.sort(result);
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `need` | how many copies of each word are required |
| `for (int offset = 0; offset < L; offset++)` | one independent pass per alignment |
| `seen`, `left`, `count` | the window state: token counts, the window start, the number of tokens |
| `w = s.substring(right, right + L)` | the next token |
| `if (!need.containsKey(w)) { reset; continue; }` | no valid window can contain this token, so start over after it |
| `seen.merge(w, 1, ...); count++;` | **expand** by one token |
| `while (seen.get(w) > need.get(w)) {...}` | too many copies of w → drop tokens from the left until w is within its limit |
| `if (count == k) {...}` | the window has k tokens, none over its limit → it's exactly the multiset of words → record, then drop the leftmost token to keep sliding |
| `Collections.sort(result)` | the passes produce indices out of order; sort them for a clean answer |

**Why does `count == k` mean an exact match?** No word is over its limit (the while loop guarantees that), every token is one of the words, and there are k tokens in total, which is the total number of words. So every word appears exactly as many times as needed.

### Dry run: `s = "barfoofoobarthefoobarman"`, `words = [bar, foo, the]`, offset 0

| right | token | action | left | count | seen |
|---|---|---|---|---|---|
| 0 | bar | add | 0 | 1 | bar 1 |
| 3 | foo | add | 0 | 2 | bar 1, foo 1 |
| 6 | foo | add → foo is 2 > 1 → drop `bar`, drop `foo` | 6 | 1 | foo 1 |
| 9 | bar | add | 6 | 2 | bar 1, foo 1 |
| 12 | the | add → count 3 = k → **record 6**, drop `foo` | 9 | 2 | bar 1, the 1 |
| 15 | foo | add → **record 9**, drop `bar` | 12 | 2 | foo 1, the 1 |
| 18 | bar | add → **record 12**, drop `the` | 15 | 2 | bar 1, foo 1 |
| 21 | man | not a word → reset | 24 | 0 | {} |

Offsets 1 and 2 find nothing. **Result `[6, 9, 12]`** ✓

### Complexity
- **Time O(n · L):** L passes, each touching about n/L tokens a constant number of times, and each token costs O(L) to cut and hash.
- **Space O(k · L)** for the maps.

---

## 6. Comparing the approaches

| | Brute force | Word window per offset |
|---|---|---|
| Time | O(n · k · L) | **O(n · L)** |
| Space | O(k · L) | O(k · L) |

---

## 7. Edge cases and common mistakes

**Edge cases:** duplicate words (`[aa, aa]`), words that overlap in s, s shorter than the total word length, a non-word token in the middle.

**Mistakes:**
1. Treating the words as a **set** (duplicates must be counted).
2. Running only offset 0 (missing matches that start at other positions).
3. Not resetting when an unknown token appears.
4. Stepping `right` by 1 instead of by L within a pass.

---

## 8. Interview questions and answers

**Q1. Why loop over L offsets?**
> Tokens are only aligned with a match when the scan starts at the match's start index mod L. The L offsets cover every residue, and therefore every possible start.

**Q2. Why can we jump `left` past an unknown token?**
> Any window containing that token is invalid, so no window can start at or before it and still extend past it.

**Q3. What's the complexity improvement over brute force?**
> From O(n·k·L) to O(n·L): each token is added and removed a constant number of times per pass, instead of re-counting k tokens at every start.

### Follow-up questions

**F1. The words have different lengths.**
> The fixed-step tokenisation no longer works. Use backtracking or DP over positions with a trie of the words (like Word Break).

**F2. Find All Anagrams in a String.** → [M05](../03-Medium/M05_FindAllAnagrams.md)
> The same window, with characters as the tokens (L = 1).

**F3. Faster hashing.**
> Replace the `substring` keys with rolling-hash values of the tokens, which avoids creating strings.

---

## 9. Running the file

```bash
java H03_SubstringWithConcatenationOfAllWords.java      # 5/5 tests passed (including 400 random cross-checks)
```

**One-line summary:** *"For each offset 0..L−1, slide a window of word-sized tokens with counts; record when it holds exactly k tokens and no word is over its limit."*
