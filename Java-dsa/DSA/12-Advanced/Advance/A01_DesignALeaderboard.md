# A01 · Design A Leaderboard ⭐

| | |
|---|---|
| **LeetCode** | [#1244 Design A Leaderboard](https://leetcode.com/problems/design-a-leaderboard/) |
| **Difficulty** | Medium |
| **Mix of structures** | **HashMap** (player → score) + **TreeMap** (score → how many players) |
| **Code** | [A01_DesignALeaderboard.java](A01_DesignALeaderboard.java) |
| **Run** | `java A01_DesignALeaderboard.java` |

⭐ = not in `leetcode.md`, added because it needs two data structures working together. This folder's problems all mix structures; see the [topic README](../README.md#advance-problems-that-mix-data-structures).

---

## 1. The problem

Design a `Leaderboard`:
- `addScore(playerId, score)` adds `score` to the player's total. A new player starts at 0.
- `top(K)` returns the **sum** of the K highest totals.
- `reset(playerId)` sets the player's total back to 0, which takes them off the board.

```
addScore(1,73) addScore(2,56) addScore(3,39) addScore(4,51) addScore(5,4)
top(1)  → 73
reset(1)  reset(2)  addScore(2,51)
top(3)  → 51 + 51 + 39 = 141
```

Constraints: `K` is at most the number of players on the board, and `reset` is only called for players who exist.

---

## 2. How to think about it

### Why one structure isn't enough
We need two kinds of question:

| Question | What answers it fast |
|---|---|
| "What is player 7's score?" (needed by `addScore` and `reset`) | a **HashMap**, O(1) |
| "What are the highest scores?" (needed by `top`) | a **sorted** structure |

A HashMap has no order, and a sorted structure can't find a player by id. So **keep both, and update them together.**

### Why store counts instead of players
Two players can have the same total (in the example, players 2 and 4 both end on 51). A `TreeMap<score, playerId>` would overwrite one of them. Instead:

```
countByScore:  score → how many players have exactly this score
```

`top(K)` then walks from the highest score down, taking up to `count` players from each score until it has K.

### The update rule
When a player's score changes from `old` to `new`:
1. decrease `countByScore[old]` (and remove the key if it reaches 0);
2. increase `countByScore[new]`.

### Dry run: the example
| Step | `score` | `countByScore` (highest first) |
|---|---|---|
| 5 × addScore | {1:73, 2:56, 3:39, 4:51, 5:4} | {73:1, 56:1, 51:1, 39:1, 4:1} |
| `top(1)` | | take 1 from 73 → **73** |
| `reset(1)` | {2:56, 3:39, 4:51, 5:4} | {56:1, 51:1, 39:1, 4:1} |
| `reset(2)` | {3:39, 4:51, 5:4} | {51:1, 39:1, 4:1} |
| `addScore(2,51)` | {2:51, 3:39, 4:51, 5:4} | {51:**2**, 39:1, 4:1} |
| `top(3)` | | 51 × 2 → sum 102, K = 1; 39 × 1 → sum 141, K = 0 → **141** |

---

## 3. Java concepts used in this problem

### 3.1 `TreeMap` with `Comparator.reverseOrder()`
```java
TreeMap<Integer, Integer> countByScore = new TreeMap<>(Comparator.reverseOrder());
```
A `TreeMap` keeps its keys **sorted** (it's a red-black tree inside), so `put`, `get` and `remove` cost O(log n). By default the order is ascending; passing `Comparator.reverseOrder()` flips it, so iterating `entrySet()` starts at the **highest** score. That's exactly the order `top` wants.

| Method | Cost | Meaning |
|---|---|---|
| `put / get / remove` | O(log n) | like a HashMap, but sorted |
| `firstKey() / lastKey()` | O(log n) | smallest / largest key (in the map's order) |
| `entrySet()` iteration | O(1) per step | key-value pairs in sorted order |

### 3.2 `merge` for counters, and the "remove at zero" trick
```java
countByScore.merge(now, 1, Integer::sum);                              // +1, creating the key if absent
if (countByScore.merge(s, -1, Integer::sum) == 0) countByScore.remove(s);
```
`merge` returns the **new** value. Removing keys that reach 0 matters here: a stale `51 → 0` entry would make `top` waste time on it, and could make `firstKey`-style queries wrong.

### 3.3 `Integer` vs `int`: null means "not there"
```java
Integer old = score.get(playerId);         // null if the player is new
int now = (old == null ? 0 : old) + s;
```
`get` returns `null` for a missing key. Writing `int old = score.get(id)` would throw a `NullPointerException` while unboxing `null`. Keep the `Integer` and check it first.

### 3.4 `PriorityQueue` as a size-K min-heap (Approach 2)
Java's `PriorityQueue` is a **min-heap**: `poll()` removes the smallest. To keep the K **largest** values, add each value and, whenever the size goes past K, `poll()` away the smallest. See [Heap E01](../../08-Heap/02-Easy/E01_KthLargestInStream.md) for the full explanation.

---

## 4. Approach 1: Sort everything on `top()` (the referee)

### Code ([A01_DesignALeaderboard.java:42-53](A01_DesignALeaderboard.java#L42-L53))
```java
static class SortLeaderboard {
    private final Map<Integer, Integer> score = new HashMap<>();
    public void addScore(int playerId, int s) { score.merge(playerId, s, Integer::sum); }
    public int top(int K) {
        List<Integer> all = new ArrayList<>(score.values());
        all.sort(Collections.reverseOrder());
        int sum = 0;
        for (int i = 0; i < K; i++) sum += all.get(i);
        return sum;
    }
    public void reset(int playerId) { score.remove(playerId); }
}
```

| Line | What it does |
|---|---|
| `merge(playerId, s, Integer::sum)` | adds `s`, starting from 0 for a new player |
| `new ArrayList<>(score.values())` | copies every total into a list |
| `sort(Collections.reverseOrder())` | highest first |
| the loop | adds the first K |
| `remove` | reset = drop the player |

**Cost:** `addScore` and `reset` are O(1), but `top` is **O(n log n)** on every call.

---

## 5. Approach 2: Min-heap of size K on `top()`

### Code ([A01_DesignALeaderboard.java:56-70](A01_DesignALeaderboard.java#L56-L70))
```java
static class HeapLeaderboard {
    private final Map<Integer, Integer> score = new HashMap<>();
    public void addScore(int playerId, int s) { score.merge(playerId, s, Integer::sum); }
    public int top(int K) {
        PriorityQueue<Integer> best = new PriorityQueue<>();      // min-heap: the smallest of the best K on top
        for (int s : score.values()) {
            best.add(s);
            if (best.size() > K) best.poll();                    // drop the smallest, keep the K largest
        }
        int sum = 0;
        for (int s : best) sum += s;
        return sum;
    }
    public void reset(int playerId) { score.remove(playerId); }
}
```

| Line | What it does |
|---|---|
| `new PriorityQueue<>()` | a min-heap |
| `add` then `poll` when size > K | the heap always holds the K largest seen so far; its top is the smallest of them |
| `for (int s : best)` | the order of iteration doesn't matter for a sum |

**Cost:** `top` is **O(n log K)**, which is better than sorting when K is small, but still touches every player on each call.

---

## 6. Approach 3: HashMap + TreeMap of counts (main solution)

### Code ([A01_DesignALeaderboard.java:73-104](A01_DesignALeaderboard.java#L73-L104))
```java
static class Leaderboard {
    private final Map<Integer, Integer> score = new HashMap<>();
    private final TreeMap<Integer, Integer> countByScore = new TreeMap<>(Comparator.reverseOrder());

    public void addScore(int playerId, int s) {
        Integer old = score.get(playerId);
        if (old != null) decrement(old);
        int now = (old == null ? 0 : old) + s;
        score.put(playerId, now);
        countByScore.merge(now, 1, Integer::sum);
    }

    public int top(int K) {
        int sum = 0;
        for (Map.Entry<Integer, Integer> e : countByScore.entrySet()) {
            int take = Math.min(K, e.getValue());
            sum += take * e.getKey();
            K -= take;
            if (K == 0) break;
        }
        return sum;
    }

    public void reset(int playerId) {
        Integer old = score.remove(playerId);
        if (old != null) decrement(old);
    }

    private void decrement(int s) {
        if (countByScore.merge(s, -1, Integer::sum) == 0) countByScore.remove(s);
    }
}
```

### Line by line
| Line | What it does |
|---|---|
| `score.get(playerId)` | the old total, or `null` for a new player |
| `if (old != null) decrement(old)` | the player no longer has the old total |
| `now = old + s` | the new total |
| `score.put` + `merge(now, 1, ...)` | update **both** structures |
| `top`: the loop over `entrySet()` | highest score first (reverse order) |
| `take = min(K, count)` | take every player at this score, or just as many as we still need |
| `sum += take * score` | they all have the same score |
| `if (K == 0) break` | stop early: don't walk the whole map |
| `reset`: `score.remove` | returns the removed value, so one lookup does both jobs |
| `decrement` | −1, and delete the key when nobody has that score any more |

### Complexity
- `addScore`, `reset`: **O(log n)** (a TreeMap update).
- `top(K)`: **O(K)** in the worst case (each distinct score visited holds at least one player), often much less.
- Space: **O(n)**.

---

## 7. Comparing the approaches

| | addScore | reset | top(K) |
|---|---|---|---|
| Sort on top | O(1) | O(1) | O(n log n) |
| Heap on top | O(1) | O(1) | O(n log K) |
| **HashMap + TreeMap** | O(log n) | O(log n) | **O(K)** |

Which is best depends on the mix of calls: if `top` is rare, sorting is fine. The TreeMap version keeps every operation cheap. The file runs 60,000 random operations and checks that all three agree.

---

## 8. Edge cases and common mistakes

**Edge cases:** two players with the same score, a player who is reset and then added again, `top(K)` with K = the number of players, a score added to an existing player (the old total must disappear).

**Mistakes:**
1. `TreeMap<score, playerId>`: equal scores overwrite each other.
2. Forgetting to remove the **old** score from the TreeMap when a player's score changes, so their old total is counted too.
3. Leaving keys with count 0 in the TreeMap.
4. `int old = score.get(id)`: a `NullPointerException` for a new player.
5. Sorting ascending and taking the first K (those are the lowest).

---

## 9. Interview questions and answers

**Q1. Why do you need two maps?**
> The HashMap finds a player's current score in O(1), so we know what to remove from the sorted structure. The TreeMap keeps scores ordered, so `top` can read the highest ones directly. Neither can do both jobs.

**Q2. Why count players per score instead of storing ids?**
> Several players can share a score, and `top` only needs the sum, not who they are. A count per score handles duplicates and keeps the map small.

**Q3. What's the cost of `top(K)`?**
> O(K) in the worst case, because each TreeMap entry we visit contributes at least one player. With many ties it can be much less.

### Follow-up questions

**F1. "Return the top K player ids, not the sum."**
> Store `score → set of playerIds` (a `TreeMap<Integer, Set<Integer>>`) instead of counts, and walk it the same way.

**F2. "What rank is player X?"**
> With counts, rank = 1 + (number of players with a higher score), which means summing counts above X's score: O(number of distinct higher scores). For O(log n) ranks, use a Fenwick tree over scores, or an order-statistic tree.

**F3. "Millions of players, many servers?"**
> Use Redis sorted sets (`ZINCRBY`, `ZREVRANGE`), which are a skip list plus a hash map: the same two-structure idea. See [Design Skiplist](../04-Hard/H02_DesignSkiplist.md).

---

## 10. Running the file

```bash
java A01_DesignALeaderboard.java     # 3/3 tests passed (example, 60,000 random ops: TreeMap == heap == sort)
```

**One-line summary:** *"A HashMap gives each player's current score; a reverse-ordered TreeMap of score → count keeps the scores sorted. Each update moves one count from the old score to the new one, and top(K) walks from the highest score taking up to K players."*
