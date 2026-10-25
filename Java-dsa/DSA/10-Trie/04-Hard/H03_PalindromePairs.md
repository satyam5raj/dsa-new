# H03 · Palindrome Pairs ⭐

| | |
|---|---|
| **LeetCode** | [#336 Palindrome Pairs](https://leetcode.com/problems/palindrome-pairs/) |
| **Difficulty** | Hard |
| **Pattern** | For each word, look up the reversed pieces: split into prefix/suffix cases |
| **Code** | [H03_PalindromePairs.java](H03_PalindromePairs.java) |
| **Run** | `java H03_PalindromePairs.java` |

⭐ = a bonus classic. Palindrome checks are covered in [Strings E02](../../02-Strings/02-Easy/E02_ValidPalindrome.md).

---

## 1. The problem

The words are unique. Return all the pairs (i, j), i ≠ j, where `words[i] + words[j]` is a palindrome.

```
["abcd","dcba","lls","s","sssll"]  →  [[0,1],[1,0],[3,2],[2,4]]
  abcd + dcba,  dcba + abcd,  s + lls = "slls",  lls + sssll = "llssssll"
```

| words | pairs |
|---|---|
| the example | 4 pairs |
| ["a",""] | [[0,1],[1,0]] ("a" + "" and "" + "a") |

**Constraints:** up to 5000 words of length ≤ 300.

---

## 2. How to think about it

### Brute force
Every pair, checking the concatenation: O(n² · L). With n = 5000, too slow.

### When is A + B a palindrome? Split the longer word at some point k
- **Case 1 (A is at least as long):** `A = P + S` where **S is a palindrome** and **B == reverse(P)**.
  ```
  A = "abcdd" = "abc" + "dd",  B = "cba":   "abc | dd | cba"  is a palindrome
  (the palindromic middle S sits between P and its mirror B)
  ```
- **Case 2 (B is longer):** `B = P + S` where **P is a palindrome** and **A == reverse(S)**.
  ```
  B = "sssll" = "ss" + "sll",  A = "lls":   "lls | ss | sll" = "llssssll"
  ```

So, for each word w and every split k (0..len), with `prefix = w[0..k)` and `suffix = w[k..)`:
- if the suffix is a palindrome and reverse(prefix) is another word x → the pair **(w, x)**
- if the prefix is a palindrome and reverse(suffix) is another word x → the pair **(x, w)**

The lookups go into a HashMap word → index: O(n · L²) in total.

**Duplicates:** at k = 0 the prefix is "" (a palindrome) and the suffix is the whole word; at k = len it's the reverse. The second case is skipped at k = 0 (`k > 0`), so a pair isn't added twice when both pieces are empty or whole.

### Dry run: the example (from the real run)

| w | k | case | condition | pair |
|---|---|---|---|---|
| abcd | 4 | 1 | suffix "" is a palindrome; reverse("abcd") = "dcba" is word 1 | [0, 1] |
| dcba | 4 | 1 | suffix ""; reverse = "abcd" = word 0 | [1, 0] |
| lls | 2 | 2 | prefix "ll" is a palindrome; reverse(suffix "s") = "s" = word 3 | [3, 2] |
| sssll | 2 | 2 | prefix "ss" is a palindrome; reverse(suffix "sll") = "lls" = word 2 | [2, 4] |

---

## 3. Java concepts used in this problem

### 3.1 A HashMap from string to index

```java
Map<String, Integer> index = new HashMap<>();
Integer j = index.get(reversed);
if (j != null && j != i) ...
```

`j != i` prevents pairing a word with itself (e.g. a palindromic word reversed is itself).

### 3.2 `j != i` with `Integer` vs `int`

`j` is an `Integer` and `i` an `int`, so `j != i` unboxes j and compares the values. (Comparing two `Integer` objects with `!=` would compare references, a classic bug.)

### 3.3 Reversing a piece

`new StringBuilder(prefix).reverse().toString()`: O(L) per piece.

### 3.4 A two-pointer palindrome check

```java
for (int i = 0, j = s.length() - 1; i < j; i++, j--) if (s.charAt(i) != s.charAt(j)) return false;
```

---

## 4. Approach 1: All pairs (the referee)

### Code ([H03_PalindromePairs.java:75-79](H03_PalindromePairs.java#L75-L79))

```java
static Set<List<Integer>> brute(String[] w) {
    Set<List<Integer>> out = new HashSet<>();
    for (int i = 0; i < w.length; i++) for (int j = 0; j < w.length; j++) if (i != j && isPalindrome(w[i] + w[j])) out.add(List.of(i, j));
    return out;
}
```

**O(n² · L).** Obviously correct.

---

## 5. Approach 2: Split each word and look up the reversed pieces (the file's main solution)

### Code ([H03_PalindromePairs.java:49-73](H03_PalindromePairs.java#L49-L73))

```java
static List<List<Integer>> palindromePairs(String[] words) {
    Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < words.length; i++) index.put(words[i], i);
    List<List<Integer>> result = new ArrayList<>();
    for (int i = 0; i < words.length; i++) {
        String w = words[i];
        for (int k = 0; k <= w.length(); k++) {
            String prefix = w.substring(0, k), suffix = w.substring(k);
            if (isPalindrome(suffix)) {                                // w + x with x = reverse(prefix)
                Integer j = index.get(new StringBuilder(prefix).reverse().toString());
                if (j != null && j != i) result.add(List.of(i, j));
            }
            if (k > 0 && isPalindrome(prefix)) {                       // x + w with x = reverse(suffix)
                Integer j = index.get(new StringBuilder(suffix).reverse().toString());   // k > 0 avoids a duplicate
                if (j != null && j != i) result.add(List.of(j, i));
            }
        }
    }
    return result;
}

static boolean isPalindrome(String s) {
    for (int i = 0, j = s.length() - 1; i < j; i++, j--) if (s.charAt(i) != s.charAt(j)) return false;
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `index` | find any word by its exact text in O(L) |
| `k = 0..len` | every split point, including "nothing" and "everything" |
| case 1 | the suffix is a palindromic middle; the partner after w is reverse(prefix) |
| case 2 (k > 0) | the prefix is a palindromic middle; the partner before w is reverse(suffix) |
| `j != i` | never pair a word with itself |

### Dry run: ["a", ""]
- w = "a", k = 0: suffix "a" is a palindrome, reverse("") = "" is word 1 → **[0, 1]**. k = 1: suffix "" → reverse("a") = "a" = itself → skip; prefix "a" is a palindrome → reverse("") = "" = word 1 → **[1, 0]**.
- w = "", k = 0: suffix "" → reverse("") = "" = itself → skip.

→ `[[0,1],[1,0]]` ✓

### Complexity
- **Time O(n · L²)** (L + 1 splits per word, each with O(L) palindrome checks, reversals and hashing).
- **Space O(n · L).**

---

## 6. Approach 3: The trie version (outline)

Store every word **reversed** in a trie, marking at each node the words whose remaining part (below that node) is a palindrome. Then walk each word down the trie, collecting:
- the words ending along the path whose leftover part of w is a palindrome (case 1)
- the words below the final node whose remaining reversed part is a palindrome (case 2)

Same O(n · L²) complexity, but much more code; the HashMap version is what most people write in interviews.

---

## 7. Comparing the approaches

| | All pairs | Split + HashMap | Trie |
|---|---|---|---|
| Time | O(n² · L) | **O(n · L²)** | O(n · L²) |
| Code | trivial | moderate | long |
| Better when | n is tiny | n is large, L small | the same, with a lot more code |

The file checks the split version against all pairs on 1,000 random word sets (with the empty string), and also checks that there are no duplicate pairs.

---

## 8. Edge cases and common mistakes

**Edge cases:** the empty string (pairs with every palindrome, in both orders), palindromic words, a word and its reverse, single letters.

**Mistakes:**
1. Producing duplicate pairs at k = 0 and k = len.
2. Pairing a palindrome with itself.
3. Getting the order (i, j) vs (j, i) wrong in case 2.
4. Comparing `Integer`s with `==`.

---

## 9. Interview questions and answers

**Q1. Why do the two cases cover every palindrome pair?**
> In A + B, either |A| ≥ |B| or |B| > |A|. In the first case, B's mirror is A's prefix and the rest of A must be a palindrome; in the second, A's mirror is B's suffix and the rest of B must be a palindrome.

**Q2. Why skip the second check at k = 0?**
> At k = 0 the second case looks up reverse(whole word) with an empty palindromic prefix, which is exactly what the first case does at k = len. Skipping it avoids adding the same pair twice.

### Follow-up questions

**F1. Count the pairs only.**
> The same loops, counting instead of collecting.

**F2. Longest Palindrome by Concatenating Two-Letter Words (LC 2131).**
> A simpler version: count "ab"/"ba" pairs, plus one self-palindrome "aa" in the middle.

**F3. Shortest Palindrome.** → [Strings H02](../../02-Strings/04-Hard/H02_ShortestPalindrome.md)
> Add characters in front, using KMP on s + "#" + reverse(s).

---

## 10. Running the file

```bash
java H03_PalindromePairs.java      # 4/4 tests passed (including 1,000 random word sets vs all pairs)
```

**One-line summary:** *"For every split of every word: if the suffix is a palindrome, pair with reverse(prefix) after it; if the prefix is a palindrome (k > 0), pair with reverse(suffix) before it, using a word → index HashMap."*
