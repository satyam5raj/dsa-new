# H02 · Word Ladder II

| | |
|---|---|
| **LeetCode** | [#126 Word Ladder II](https://leetcode.com/problems/word-ladder-ii/) |
| **Difficulty** | Hard |
| **Pattern** | BFS level by level, recording the parents, then DFS **back** from the end |
| **Code** | [H02_WordLadderII.java](H02_WordLadderII.java) |
| **Run** | `java H02_WordLadderII.java` |

The neighbour generation and BFS are explained in [H01 Word Ladder](H01_WordLadder.md).

---

## 1. The problem

Like [H01](H01_WordLadder.md), but return **all** the shortest transformation sequences.

```
hit → cog, wordList [hot, dot, dog, lot, log, cog]
→ [[hit, hot, dot, dog, cog],
   [hit, hot, lot, log, cog]]
```

---

## 2. How to think about it

### Naive
A DFS over all paths, keeping the shortest: exponential, it times out.

### Two phases
1. **BFS, level by level**, building a `parents` map: word → the words on the **previous** level that lead to it. Two subtleties:
   - a word can have **several** parents on the previous level: `cog` is reached from **both** `dog` and `log`. So don't block a word the moment it's first seen; remove the whole level from the dictionary only **after** the level is done (otherwise the path through `log` would be lost).
   - stop after the level where endWord appears (only the shortest paths matter).
2. **DFS from endWord back to beginWord** through the parents map, building each path in reverse. Every path found is shortest by construction.

**Going backwards from the end is the key:** it only walks edges that lie on some shortest path, so no work is wasted on dead ends.

### Dry run: the parents map, level by level (from the real run)

| level | words | parents added |
|---|---|---|
| 2 | {hot} | hot ← hit |
| 3 | {dot, lot} | dot ← hot, lot ← hot |
| 4 | {dog, log} | dog ← dot, log ← lot |
| 5 | {cog} | cog ← **dog, log** (two parents) → found, stop |

Backtracking from cog: cog ← dog ← dot ← hot ← hit, and cog ← log ← lot ← hot ← hit.

---

## 3. Java concepts used in this problem

### 3.1 A multimap: `Map<String, List<String>>`

```java
parents.computeIfAbsent(nb, k -> new ArrayList<>()).add(w);
```

Each word maps to a **list** of parents (there can be several).

### 3.2 `removeAll` after the level

```java
dict.removeAll(next);                                  // block this level only now
```

Words on the current level stay available while the level is being expanded, so a word can collect parents from several words on the same level. They're blocked before the next level, so no path ever goes back to an earlier level.

### 3.3 `LinkedList.addFirst` to build a reversed path

```java
path.addFirst(p);
backtrack(p, ...);
path.removeFirst();
```

The DFS walks from the end to the beginning, so prepending keeps the path in forward order. `addFirst`/`removeFirst` are O(1) on a LinkedList.

---

## 4. Approach 1: Distances from both ends (the referee)

### Code ([H02_WordLadderII.java:84-104](H02_WordLadderII.java#L84-L104))

```java
static Set<List<String>> referee(String b, String e, List<String> wl) {
    Set<String> all = new HashSet<>(wl);
    all.add(b);
    Map<String, Integer> dFrom = bfs(b, all), dTo = bfs(e, all);
    Set<List<String>> out = new HashSet<>();
    if (!new HashSet<>(wl).contains(e) || !dFrom.containsKey(e)) return out;
    int best = dFrom.get(e);
    enumerate(b, e, best, dFrom, dTo, all, new ArrayList<>(List.of(b)), out);
    return out;
}

static void enumerate(String w, String e, int best, Map<String, Integer> dFrom, Map<String, Integer> dTo, Set<String> all, List<String> path, Set<List<String>> out) {
    if (w.equals(e)) { out.add(new ArrayList<>(path)); return; }
    for (String nb : all) {
        if (oneApart(w, nb) && dFrom.getOrDefault(nb, -1) == dFrom.get(w) + 1 && dTo.containsKey(nb) && dFrom.get(nb) + dTo.get(nb) == best) {
            path.add(nb);
            enumerate(nb, e, best, dFrom, dTo, all, path, out);
            path.remove(path.size() - 1);
        }
    }
}
```

| Line | What it does |
|---|---|
| `dFrom`, `dTo` | BFS distances from the begin word and from the end word |
| `dFrom[nb] + dTo[nb] == best` | nb lies on **some** shortest path |
| `dFrom[nb] == dFrom[w] + 1` | moving one step further from the start |

A word lies on a shortest path iff its distances from both ends add up to the best length. It's a different (and slower, O(N²)-per-BFS) characterisation, used to cross-check.

---

## 5. Approach 2: Level BFS + parents + backtracking (the file's solution)

### Code ([H02_WordLadderII.java:37-81](H02_WordLadderII.java#L37-L81))

```java
static List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
    Set<String> dict = new HashSet<>(wordList);
    List<List<String>> result = new ArrayList<>();
    if (!dict.contains(endWord)) return result;
    Map<String, List<String>> parents = new HashMap<>();
    Set<String> level = new HashSet<>(List.of(beginWord));
    dict.remove(beginWord);
    boolean found = false;
    while (!level.isEmpty() && !found) {
        Set<String> next = new HashSet<>();
        for (String w : level) {
            char[] cs = w.toCharArray();
            for (int p = 0; p < cs.length; p++) {
                char orig = cs[p];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == orig) continue;
                    cs[p] = c;
                    String nb = new String(cs);
                    if (dict.contains(nb)) {                   // not used on an EARLIER level
                        next.add(nb);
                        parents.computeIfAbsent(nb, k -> new ArrayList<>()).add(w);
                        if (nb.equals(endWord)) found = true;
                    }
                }
                cs[p] = orig;
            }
        }
        dict.removeAll(next);                                  // block this level only now
        level = next;
    }
    if (found) {
        LinkedList<String> path = new LinkedList<>(List.of(endWord));
        backtrack(endWord, beginWord, parents, path, result);
    }
    return result;
}

static void backtrack(String word, String begin, Map<String, List<String>> parents, LinkedList<String> path, List<List<String>> result) {
    if (word.equals(begin)) { result.add(new ArrayList<>(path)); return; }
    for (String p : parents.getOrDefault(word, List.of())) {
        path.addFirst(p);
        backtrack(p, begin, parents, path, result);
        path.removeFirst();
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `level` / `next` | the BFS frontier as sets (no duplicates on a level) |
| `dict.contains(nb)` | nb wasn't used on an earlier level (it may be on this one) |
| `parents…add(w)` | record **every** way to reach nb from the current level |
| `found = true` | finish this level, then stop |
| `dict.removeAll(next)` | block this level's words for the later levels |
| `backtrack` | follow the parents from endWord back to beginWord, prepending |

### Complexity
- **BFS: O(N · 26 · L²)**, plus the backtracking proportional to the output size (which can be exponential).
- **Space O(N · L)** for the parents.

---

## 6. Comparing the approaches

| | Distances from both ends | Level BFS + parents |
|---|---|---|
| Neighbour finding | O(N) scan per word | 26 · L candidates per word |
| Wasted work | none on the dead ends | none on the dead ends |
| Code | longer | standard interview answer |

The file checks the solution against the distance referee on 300 random dictionaries.

---

## 7. Edge cases and common mistakes

**Edge cases:** endWord unreachable (empty result), several shortest paths sharing prefixes, beginWord not in the list.

**Mistakes:**
1. Removing each word from the dictionary as soon as it's seen (loses the second parent).
2. Not stopping after the level containing endWord (adds longer paths).
3. Building the paths forward with a DFS from beginWord (explores dead ends: very slow).
4. Forgetting to copy the path when recording it.

---

## 8. Interview questions and answers

**Q1. Why delay removing the level's words until after the level?**
> A word on the next level may be reachable from several words on the current level. Removing it at its first discovery would drop the others as parents, losing shortest paths.

**Q2. Why backtrack from the end rather than the start?**
> The parents map only contains edges on shortest paths into each word. Walking backwards from endWord, every branch leads to beginWord, with no dead ends.

### Follow-up questions

**F1. Only count the shortest paths.**
> Count the paths per word during the BFS: `ways[nb] += ways[w]`, no enumeration needed.

**F2. Word Ladder (only the length).** → [H01](H01_WordLadder.md)

**F3. Bidirectional version.**
> Grow from both ends recording directed parents, then join the two halves; it's faster but trickier to get right.

---

## 9. Running the file

```bash
java H02_WordLadderII.java      # 3/3 tests passed (including 300 random dictionaries vs distance referee)
```

**One-line summary:** *"BFS level by level, recording every parent from the previous level (block each level only after it's processed); stop at endWord's level, then backtrack the parents from endWord to beginWord."*
