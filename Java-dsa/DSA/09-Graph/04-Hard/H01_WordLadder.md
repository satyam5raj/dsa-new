# H01 · Word Ladder

| | |
|---|---|
| **LeetCode** | [#127 Word Ladder](https://leetcode.com/problems/word-ladder/) |
| **Difficulty** | Hard |
| **Pattern** | BFS on an implicit graph (words differing by one letter) + bidirectional BFS |
| **Code** | [H01_WordLadder.java](H01_WordLadder.java) |
| **Run** | `java H01_WordLadder.java` |

BFS basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#34-bfs-breadth-first-search-with-a-queue).

---

## 1. The problem

Transform `beginWord` into `endWord`, changing **one letter** at a time; every intermediate word must be in `wordList`. Return the number of **words** in the shortest sequence (0 if it's impossible).

```
hit → hot → dot → dog → cog          = 5 words
wordList = [hot, dot, dog, lot, log, cog]
```

| example | answer |
|---|---|
| hit → cog with the list above | 5 |
| hit → cog without "cog" in the list | 0 |
| a → c with [a, b, c] | 2 |

**Constraints:** up to 5000 words of length ≤ 10.

---

## 2. How to think about it

### The graph
Vertices = words; an edge joins two words that differ in exactly one position. All edges cost 1 → **BFS**.

```
hit ─ hot ─ dot ─ dog ─ cog
       │     │     │     │
       └─ lot ─── log ───┘
```

### Generating the neighbours (the real problem)
- Comparing against every word: O(N · L) per word → O(N² · L) in total. Slow for N = 5000.
- **Better:** for each position, try all 26 letters and look the result up in a HashSet: 26 · L candidates per word, each an O(L) hash → O(N · 26 · L²).
- **Remove** a word from the set when it's enqueued: that's the visited marker.

### Bidirectional BFS
Grow from **both** ends, always expanding the **smaller** frontier, and stop when they meet. If each word has b neighbours and the answer is d, one-sided BFS explores about b^d words, two-sided about 2 · b^(d/2): a huge win.

### Dry run (one-sided)
```
level 1: {hit}
level 2: {hot}
level 3: {dot, lot}
level 4: {dog, log}
level 5: {cog} = endWord → 5
```

---

## 3. Java concepts used in this problem

### 3.1 Mutating a `char[]` to generate the neighbours

```java
char[] cs = w.toCharArray();
for (int p = 0; p < cs.length; p++) {
    char orig = cs[p];
    for (char c = 'a'; c <= 'z'; c++) { cs[p] = c; String nb = new String(cs); ... }
    cs[p] = orig;                        // restore before moving to the next position
}
```

Strings are immutable, so the candidates are built in a `char[]` and turned into a new String each time. Forgetting to restore `cs[p]` corrupts every later candidate.

### 3.2 `set.remove(x)` as "check + mark visited"

```java
if (unvisited.remove(nb)) q.add(nb);      // in the list and not seen yet
```

`remove` returns true only if the word was present, which means it's in the dictionary **and** hasn't been visited. One hash operation does both checks.

### 3.3 A `char` loop variable

`for (char c = 'a'; c <= 'z'; c++)` works because chars are numeric (16-bit unsigned).

### 3.4 Swapping two references

```java
if (front.size() > back.size()) { Set<String> t = front; front = back; back = t; }
```

The bidirectional search always expands the smaller set; swapping the variables (not the contents) is O(1).

---

## 4. Approach 1: One-sided BFS (the file's main solution)

### Code ([H01_WordLadder.java:39-64](H01_WordLadder.java#L39-L64))

```java
static int ladderLength(String beginWord, String endWord, List<String> wordList) {
    Set<String> unvisited = new HashSet<>(wordList);
    if (!unvisited.contains(endWord)) return 0;
    Queue<String> q = new ArrayDeque<>(List.of(beginWord));
    unvisited.remove(beginWord);
    int words = 1;
    while (!q.isEmpty()) {
        for (int i = q.size(); i > 0; i--) {
            String w = q.poll();
            if (w.equals(endWord)) return words;
            char[] cs = w.toCharArray();
            for (int p = 0; p < cs.length; p++) {
                char orig = cs[p];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == orig) continue;
                    cs[p] = c;
                    String nb = new String(cs);
                    if (unvisited.remove(nb)) q.add(nb);      // in the list and not seen yet
                }
                cs[p] = orig;
            }
        }
        words++;
    }
    return 0;
}
```

### Line by line

| Line | What it does |
|---|---|
| `unvisited` | the dictionary, minus the words already reached |
| the endWord check | not in the list → impossible |
| `words = 1` | the sequence length counts words (beginWord is the first) |
| the level loop | each level adds one word to the sequence |
| `w.equals(endWord)` | reached → the current level's length |
| the 26 × L loop | every one-letter variation of w |
| `unvisited.remove(nb)` | a dictionary word not yet visited → queue it |

### Complexity
- **Time O(N · 26 · L²).**
- **Space O(N · L).**

---

## 5. Approach 2: Bidirectional BFS

### Code ([H01_WordLadder.java:66-94](H01_WordLadder.java#L66-L94))

```java
static int bidirectional(String beginWord, String endWord, List<String> wordList) {
    Set<String> dict = new HashSet<>(wordList);
    if (!dict.contains(endWord)) return 0;
    Set<String> front = new HashSet<>(List.of(beginWord)), back = new HashSet<>(List.of(endWord));
    dict.remove(beginWord);
    dict.remove(endWord);
    int words = 1;
    while (!front.isEmpty() && !back.isEmpty()) {
        if (front.size() > back.size()) { Set<String> t = front; front = back; back = t; }   // expand the smaller side
        Set<String> next = new HashSet<>();
        for (String w : front) {
            char[] cs = w.toCharArray();
            for (int p = 0; p < cs.length; p++) {
                char orig = cs[p];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == orig) continue;
                    cs[p] = c;
                    String nb = new String(cs);
                    if (back.contains(nb)) return words + 1;      // the two searches meet
                    if (dict.remove(nb)) next.add(nb);
                }
                cs[p] = orig;
            }
        }
        front = next;
        words++;
    }
    return 0;
}
```

### Line by line

| Line | What it does |
|---|---|
| `front`, `back` | the two frontiers, starting at each end |
| the swap | always expand the smaller frontier (it's cheaper) |
| `back.contains(nb)` | a neighbour of the front is already on the other frontier → the searches meet |
| `words + 1` | the meeting word adds one to the count |
| `dict.remove(nb)` | visited by **either** search |

### Dry run: hit → cog (from the real run)

| words | expanded set | other set | result |
|---|---|---|---|
| 1 | {hit} | {cog} | front becomes {hot} |
| 2 | {hot} | {cog} | front becomes {dot, lot} |
| 3 | {cog} (**swapped**: it's smaller than {dot, lot}) | {dot, lot} | this side becomes {dog, log} |
| 4 | {dog, log} (equal sizes: no swap) | {dot, lot} | `log` → `lot` is in the other set → **meet** → return 4 + 1 = **5** ✓ |

### Complexity
- **Worst case** the same as one-sided; **typically** far fewer words explored (about 2 · b^(d/2) vs b^d).

---

## 6. Comparing the approaches

| | One-sided BFS | Bidirectional BFS |
|---|---|---|
| Worst-case time | O(N · 26 · L²) | O(N · 26 · L²) |
| Typical explored words | b^d | 2 · b^(d/2) |
| Code | simpler | a bit more |

The file checks both on 500 random dictionaries over a 3-letter alphabet.

---

## 7. Edge cases and common mistakes

**Edge cases:** endWord not in the list (0), beginWord not in the list (allowed), beginWord one step from endWord (2), unreachable (0).

**Mistakes:**
1. Comparing every pair of words (too slow for N = 5000).
2. Not restoring `cs[p]` after trying the 26 letters.
3. Returning the number of transformations (edges) instead of words.
4. Marking a word visited only when it's polled (duplicates in the queue).

---

## 8. Interview questions and answers

**Q1. Why is generating 26 · L candidates better than comparing with every word?**
> When N is large (5000) and L is small (≤ 10), 26 · L = 260 hash lookups per word beats comparing against N words.

**Q2. Why does bidirectional BFS help so much?**
> The number of explored nodes grows exponentially with the depth. Two searches of half the depth each explore roughly the square root of what one full-depth search would.

### Follow-up questions

**F1. Return all the shortest sequences.** → [H02 Word Ladder II](H02_WordLadderII.md)

**F2. Minimum Genetic Mutation (LC 433).**
> The same BFS with the alphabet {A, C, G, T}.

**F3. Precomputing patterns.**
> Map each pattern like `h*t` → the list of words matching it; the neighbours are the words sharing a pattern.

---

## 9. Running the file

```bash
java H01_WordLadder.java      # 5/5 tests passed (including 500 random dictionaries: BFS == bidirectional)
```

**One-line summary:** *"BFS over the words, generating the neighbours by trying 26 letters at each position against a HashSet (removing = visited); bidirectional BFS from both ends is much faster in practice."*
