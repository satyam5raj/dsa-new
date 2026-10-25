# H04 · Concatenated Words ⭐

| | |
|---|---|
| **LeetCode** | [#472 Concatenated Words](https://leetcode.com/problems/concatenated-words/) |
| **Difficulty** | Hard |
| **Pattern** | Trie + DP over the split points ("word break" for every word) |
| **Code** | [H04_ConcatenatedWords.java](H04_ConcatenatedWords.java) |
| **Run** | `java H04_ConcatenatedWords.java` |

⭐ = a bonus classic. Trie basics are explained in [E01](../02-Easy/E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem); DP gets its own topic in [15-Dynamic-Programming](../../15-Dynamic-Programming/README.md).

---

## 1. The problem

Return every word that is made of **at least two** shorter words from the list.

```
["cat","cats","catsdogcats","dog","dogcatsdog","hippopotamuses","rat","ratcatdogcat"]
→ ["catsdogcats","dogcatsdog","ratcatdogcat"]
```

| words | answer |
|---|---|
| the example | catsdogcats, dogcatsdog, ratcatdogcat |
| ["cat","dog","catdog"] | ["catdog"] |
| ["a"] | [] (a word on its own doesn't count) |

---

## 2. How to think about it

### For one word, it's Word Break (a DP problem)
Can w be cut into dictionary words?

```
ok[i] = "w[0..i) can be built from words";   ok[0] = true
ok[j] = true  if  ok[i]  and  w[i..j) is a word,  for some i < j
```

### The trie makes the inner loop cheap
From position i, walk the trie along w[i], w[i+1], …; **every word end you pass is a valid j**. No substrings or hashing, and you stop as soon as you fall off the trie.

### "At least two words"
Process the words **shortest first**, and check each word against the trie **before** inserting it. Then the word can't use itself, so any successful split uses ≥ 2 smaller words. Then insert it (it may help longer words). Empty strings are skipped (they'd make everything "concatenated").

### Dry run: "catsdogcats" (the trie holds cat, cats, dog, …)
```
from 0: "cat" ends at 3, "cats" ends at 4      → ok[3], ok[4]
from 3: 's' — no word starts with 's'          → nothing
from 4: "dog" ends at 7                        → ok[7]
from 7: "cat" ends at 10, "cats" ends at 11    → ok[10], ok[11] = the end → yes
```

---

## 3. Java concepts used in this problem

### 3.1 Sorting strings by length

```java
Arrays.sort(sorted, Comparator.comparingInt(String::length));      // shortest first
```

`String::length` is a method reference used as the sort key.

### 3.2 A boolean DP array

```java
boolean[] ok = new boolean[n + 1];      // ok[i]: the first i characters can be built
ok[0] = true;                           // the empty prefix is trivially built
```

Indices 0..n represent positions **between** characters, which is why the array has n + 1 slots.

### 3.3 Early exits

`if (!ok[i]) continue;` skips unreachable starting points; `if (ok[n]) return true;` stops as soon as the whole word is covered.

---

## 4. Approach 1: Word break with a HashSet and recursion (the referee)

### Code ([H04_ConcatenatedWords.java:81-100](H04_ConcatenatedWords.java#L81-L100))

```java
static Set<String> brute(String[] words) {
    Set<String> dict = new HashSet<>(Arrays.asList(words));
    dict.remove("");
    Set<String> out = new HashSet<>();
    for (String w : dict) if (pieces(w, 0, dict, w) >= 2) out.add(w);
    return out;
}

// max number of pieces w[i..] can be split into using words other than `self` as a whole; -1 if impossible
static int pieces(String w, int i, Set<String> dict, String self) {
    if (i == w.length()) return 0;
    int best = -1;
    for (int j = i + 1; j <= w.length(); j++) {
        String part = w.substring(i, j);
        if (!dict.contains(part) || (i == 0 && j == w.length())) continue;   // not the whole word itself
        int rest = pieces(w, j, dict, self);
        if (rest >= 0) best = Math.max(best, rest + 1);
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `pieces(w, i, …)` | the most pieces that w[i..] can be split into, or −1 |
| `(i == 0 && j == w.length())` | forbid using the whole word as a single piece |
| `>= 2` | concatenated means at least two pieces |

Exponential without memoisation, so it's only a referee for small inputs.

---

## 5. Approach 2: Shortest first + trie word break (the file's main solution)

### Code ([H04_ConcatenatedWords.java:39-78](H04_ConcatenatedWords.java#L39-L78))

```java
static class Node { Node[] next = new Node[26]; boolean end; }

static List<String> findAllConcatenatedWordsInADict(String[] words) {
    String[] sorted = words.clone();
    Arrays.sort(sorted, Comparator.comparingInt(String::length));      // shortest first
    Node root = new Node();
    List<String> result = new ArrayList<>();
    for (String w : sorted) {
        if (w.isEmpty()) continue;
        if (canBuild(root, w)) result.add(w);                          // check BEFORE inserting itself
        insert(root, w);
    }
    return result;
}

static boolean canBuild(Node root, String w) {
    int n = w.length();
    boolean[] ok = new boolean[n + 1];
    ok[0] = true;
    for (int i = 0; i < n; i++) {
        if (!ok[i]) continue;                                          // can't even reach position i
        Node node = root;
        for (int j = i; j < n; j++) {
            node = node.next[w.charAt(j) - 'a'];
            if (node == null) break;                                   // no word continues: stop early
            if (node.end) ok[j + 1] = true;                            // w[i..j] is a word
        }
        if (ok[n]) return true;
    }
    return ok[n];
}
```

### Line by line

| Line | What it does |
|---|---|
| the sort by length | all the shorter words are in the trie before a longer word is checked |
| `canBuild` before `insert` | the word can't use itself as one piece |
| `ok[i]` | start a new piece only where the previous pieces end |
| the trie walk from i | every `end` node passed gives a valid next cut j + 1 |
| `break` on null | no dictionary word continues: stop this walk |
| `insert` | make this word available to longer ones |

**Why ≥ 2 pieces is guaranteed:** the trie only holds strictly shorter (or equal-length, different) words when w is checked, so no single piece can be all of w.

### Complexity
- **Time O(Σ L²)** (for each word, O(L) start positions × O(L) walk).
- **Space O(total characters).**

---

## 6. Comparing the approaches

| | Recursive word break (referee) | Trie + DP |
|---|---|---|
| Time | exponential without a memo | **O(Σ L²)** |
| The "not itself" rule | explicit check | the insertion order |

The file checks the trie version against the referee on 500 random dictionaries.

---

## 7. Edge cases and common mistakes

**Edge cases:** the empty string in the list (skip it), duplicate words, a word made of one repeated short word ("aaaa" from "a"), words of equal length.

**Mistakes:**
1. Inserting the word before checking it (every word would count as "built from itself").
2. Forgetting the empty-string case (`"" + w` makes everything concatenated).
3. A HashSet with `substring` in the inner loop (correct, but slower than walking the trie).

---

## 8. Interview questions and answers

**Q1. Why process the words shortest first?**
> A concatenated word is built from strictly shorter words. Sorting by length guarantees that all of them are already in the trie when the word is checked, and the word itself isn't.

**Q2. How does the trie speed up word break?**
> Instead of testing every substring w[i..j) in a hash set (O(L) each), one walk from i finds every valid j in a single pass, stopping when no word continues.

### Follow-up questions

**F1. Word Break (LC 139).**
> The single-word version, with a fixed dictionary.

**F2. Word Break II (LC 140): list all the splits.**
> Backtracking with memoisation from each position.

**F3. The minimum number of pieces.**
> `dp[j] = min(dp[i] + 1)` over the valid cuts.

---

## 9. Running the file

```bash
java H04_ConcatenatedWords.java      # 4/4 tests passed (including 500 random dictionaries vs word break)
```

**One-line summary:** *"Sort the words by length; for each one, run word break by walking a trie of the shorter words from every reachable cut, and insert the word only after checking it."*
