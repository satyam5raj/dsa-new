# H03 · Alien Dictionary

| | |
|---|---|
| **LeetCode** | [#269 Alien Dictionary](https://leetcode.com/problems/alien-dictionary/) (premium) |
| **Difficulty** | Hard |
| **Pattern** | Derive the edges from adjacent words, then topologically sort the letters |
| **Code** | [H03_AlienDictionary.java](H03_AlienDictionary.java) |
| **Run** | `java H03_AlienDictionary.java` |

Kahn's algorithm is introduced in [M03](../03-Medium/M03_CourseSchedule.md) and [M04](../03-Medium/M04_CourseScheduleII.md).

---

## 1. The problem

The words are sorted in an **unknown alphabet**. Return any letter order consistent with them, or `""` if none exists.

| words | answer |
|---|---|
| `["wrt","wrf","er","ett","rftt"]` | "wertf" |
| `["z","x"]` | "zx" |
| `["z","x","z"]` | "" (z < x and x < z: a cycle) |
| `["abc","ab"]` | "" (a word can't come after its own prefix) |

---

## 2. How to think about it

### Step 1: the edges
Only **adjacent** words give information (the order is transitive), and only at their **first differing position**:

```
"wrt" vs "wrf":   first difference t / f   →  t before f   (edge t → f)
"wrf" vs "er" :   w / e                    →  w → e
"er"  vs "ett":   r / t                    →  r → t
"ett" vs "rftt":  e / r                    →  e → r
```

Letters **after** the first difference tell us nothing.

### The invalid-prefix trap
`["abc", "ab"]` is impossible: a word can't come after its own prefix. If the later word is a prefix of the earlier, longer word → `""`.

### Step 2: Kahn's algorithm over every letter that appears
Letters with no edges still count (they can go anywhere).

```
edges: w → e → r → t → f       →   "wertf"
```

If not every letter gets output, there's a cycle → `""`.

---

## 3. Java concepts used in this problem

### 3.1 Graphs keyed by `Character`

```java
Map<Character, Set<Character>> adj = new TreeMap<>();
Map<Character, Integer> indeg = new TreeMap<>();
```

The vertices are letters. `TreeMap`/`TreeSet` make the output deterministic (letters with equal standing come out in normal alphabetical order), which keeps the tests stable. HashMaps would work too.

### 3.2 Counting each edge once

```java
if (adj.get(from).add(to)) indeg.merge(to, 1, Integer::sum);
```

`Set.add` returns false for a repeated edge, so the in-degree is increased only for **new** edges. Counting duplicates would break Kahn's algorithm (the in-degree would never reach 0).

### 3.3 `merge(nxt, -1, Integer::sum) == 0`

It decrements the in-degree and returns the new value in one call.

### 3.4 Checking an answer (in the test)

`sortedUnder(words, alphabet)` maps each letter to its rank and verifies that every adjacent pair is in order. The test accepts **any** valid alphabet, and only demands `""` when **no** permutation of the letters works (checked by brute force).

---

## 4. Approach 1: Try every permutation of the letters (the referee)

### Code ([H03_AlienDictionary.java:80-92](H03_AlienDictionary.java#L80-L92))

```java
static boolean anyOrderWorks(String[] words, List<Character> letters, int k) {
    if (k == letters.size()) {
        StringBuilder sb = new StringBuilder();
        for (char c : letters) sb.append(c);
        return sortedUnder(words, sb.toString());
    }
    for (int i = k; i < letters.size(); i++) {
        Collections.swap(letters, k, i);
        if (anyOrderWorks(words, letters, k + 1)) { Collections.swap(letters, k, i); return true; }
        Collections.swap(letters, k, i);
    }
    return false;
}
```

It generates the permutations by swapping (positions 0..k−1 are fixed; try every remaining letter at position k) and checks each one. **O(L! · total chars)** for L letters: only for tiny inputs.

---

## 5. Approach 2: Edges from adjacent words + Kahn (the file's solution)

### Code ([H03_AlienDictionary.java:37-63](H03_AlienDictionary.java#L37-L63))

```java
static String alienOrder(String[] words) {
    Map<Character, Set<Character>> adj = new TreeMap<>();
    Map<Character, Integer> indeg = new TreeMap<>();
    for (String w : words) for (char c : w.toCharArray()) { adj.putIfAbsent(c, new TreeSet<>()); indeg.putIfAbsent(c, 0); }

    for (int i = 0; i + 1 < words.length; i++) {
        String a = words[i], b = words[i + 1];
        int len = Math.min(a.length(), b.length()), j = 0;
        while (j < len && a.charAt(j) == b.charAt(j)) j++;
        if (j == len) {
            if (a.length() > b.length()) return "";            // "abc" before "ab": impossible
            continue;                                           // a is a prefix of b: no information
        }
        char from = a.charAt(j), to = b.charAt(j);
        if (adj.get(from).add(to)) indeg.merge(to, 1, Integer::sum);   // count each edge once
    }

    Queue<Character> q = new ArrayDeque<>();
    for (Map.Entry<Character, Integer> e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());
    StringBuilder order = new StringBuilder();
    while (!q.isEmpty()) {
        char c = q.poll();
        order.append(c);
        for (char nxt : adj.get(c)) if (indeg.merge(nxt, -1, Integer::sum) == 0) q.add(nxt);
    }
    return order.length() == indeg.size() ? order.toString() : "";   // leftover letters = cycle
}
```

### Line by line

| Line | What it does |
|---|---|
| the first loop | register **every** letter, even those without edges |
| the `while` on j | find the first differing position of the adjacent pair |
| `j == len` | one word is a prefix of the other |
| `a.length() > b.length()` | the longer word came first → invalid |
| `from → to` | the only information this pair gives |
| Kahn | output the letters in a valid order |
| the length check | letters left over → they're on a cycle |

### Dry run: `["wrt","wrf","er","ett","rftt"]`

Edges: t → f, w → e, r → t, e → r. In-degrees: e 1, f 1, r 1, t 1, w 0.

| poll | output | in-degree updates |
|---|---|---|
| w | "w" | e → 0 |
| e | "we" | r → 0 |
| r | "wer" | t → 0 |
| t | "wert" | f → 0 |
| f | "wertf" | |

All 5 letters output → **"wertf"** ✓

### Dry run: `["z","x","z"]`
Edges z → x and x → z → both in-degrees are 1 → nothing to start with → order "" (length 0 ≠ 2) → **""** ✓

### Complexity
- **Time O(total characters)** (plus the alphabet-sized Kahn).
- **Space O(26²)** at most for the edges.

---

## 6. Comparing the approaches

| | Every permutation | Edges + Kahn |
|---|---|---|
| Time | O(L! · chars) | **O(chars)** |
| Space | O(L) | O(26²) |

The file checks the Kahn version against every permutation on 1,000 random word lists: when an order exists it must be valid, and `""` only when none does.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single word (any order of its letters), identical adjacent words, the prefix trap, letters that appear but have no constraints.

**Mistakes:**
1. Comparing all pairs of words instead of adjacent ones (correct but wasteful).
2. Using characters after the first difference.
3. Forgetting the prefix trap.
4. Counting duplicate edges twice in the in-degrees.
5. Forgetting the letters with no edges.

---

## 8. Interview questions and answers

**Q1. Why are adjacent pairs enough?**
> The order is transitive: if w1 ≤ w2 ≤ w3, the constraint between w1 and w3 follows from the two adjacent constraints.

**Q2. Why only the first differing character?**
> The dictionary order is decided at the first difference; everything after it is unconstrained by that pair.

### Follow-up questions

**F1. Verifying an Alien Dictionary (LC 953): is the list sorted under a given order?**
> The `sortedUnder` check from the test.

**F2. Return all valid orders.**
> Backtracking over Kahn's choices (whenever several letters are ready, try each).

**F3. Is the order unique?**
> Only if the queue never holds more than one letter during Kahn's algorithm.

---

## 9. Running the file

```bash
java H03_AlienDictionary.java      # 5/5 tests passed (including 1,000 random lists vs all permutations)
```

**One-line summary:** *"Compare each adjacent word pair at its first difference to get letter edges (rejecting a longer word before its prefix), then topologically sort all the letters; a leftover letter means a cycle."*
