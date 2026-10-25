# M25 · Possible Bipartition

| | |
|---|---|
| **LeetCode** | [#886 Possible Bipartition](https://leetcode.com/problems/possible-bipartition/) |
| **Difficulty** | Medium |
| **Pattern** | [Is Graph Bipartite](M17_IsGraphBipartite.md): build the graph from the "dislikes" first |
| **Code** | [M25_PossibleBipartition.java](M25_PossibleBipartition.java) |
| **Run** | `java M25_PossibleBipartition.java` |

The 2-colouring technique is explained in [M17](M17_IsGraphBipartite.md).

---

## 1. The problem

n people (1..n); `dislikes[i] = [a, b]` means a and b refuse to be in the same group. Can everyone be split into **two** groups?

| n | dislikes | answer |
|---|---|---|
| 4 | [[1,2],[1,3],[2,4]] | true ({1, 4} and {2, 3}) |
| 3 | [[1,2],[1,3],[2,3]] | false (a triangle of dislikes) |
| 5 | [[1,2],[2,3],[3,4],[4,5],[1,5]] | false (an odd cycle of 5) |

---

## 2. How to think about it

Each "dislike" is an edge whose two ends need **different** groups = different colours. So:
1. build the undirected graph
2. 2-colour it (BFS/DFS), on **every** component
3. a conflict (an edge with the same colour at both ends) → false

This is a word problem around [M17](M17_IsGraphBipartite.md); the work is spotting the bipartite check.

(The Union-Find alternative: for each person, union all their enemies together; if a person ends up in the same set as one of their enemies → false.)

```
n = 4: 1 – 2, 1 – 3, 2 – 4
group 1 → group A;  2, 3 → B;  4 (an enemy of 2) → A
groups: A = {1, 4}, B = {2, 3}  ✓
```

---

## 3. Java concepts used in this problem

### 3.1 Groups 1 and 2, and `3 − g`

```java
int[] group = new int[n + 1];                         // 0 = unassigned, 1 or 2
group[v] = 3 - group[u];                              // the other group
```

With the groups numbered 1 and 2, `3 − g` flips between them (3 − 1 = 2, 3 − 2 = 1). Using 0 for "unassigned" lets the default array value mean "not visited".

### 3.2 1-based people

The arrays and lists have n + 1 slots, so the people's labels are used directly as indices.

---

## 4. Approach 1: Try every split (the referee)

### Code ([M25_PossibleBipartition.java:51-58](M25_PossibleBipartition.java#L51-L58))

```java
static boolean brute(int n, int[][] d) {
    for (int mask = 0; mask < (1 << n); mask++) {
        boolean ok = true;
        for (int[] e : d) if ((mask >> (e[0] - 1) & 1) == (mask >> (e[1] - 1) & 1)) { ok = false; break; }
        if (ok) return true;
    }
    return false;
}
```

Bit i − 1 of the mask is person i's group; a split is valid if every dislike pair is in different groups. **O(2ⁿ · D)**.

---

## 5. Approach 2: Build the graph, then BFS 2-colouring (the file's solution)

### Code ([M25_PossibleBipartition.java:31-49](M25_PossibleBipartition.java#L31-L49))

```java
static boolean possibleBipartition(int n, int[][] dislikes) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());
    for (int[] d : dislikes) { adj.get(d[0]).add(d[1]); adj.get(d[1]).add(d[0]); }
    int[] group = new int[n + 1];                         // 0 = unassigned, 1 or 2
    for (int s = 1; s <= n; s++) {
        if (group[s] != 0) continue;
        group[s] = 1;
        Queue<Integer> q = new ArrayDeque<>(List.of(s));
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : adj.get(u)) {
                if (group[v] == 0) { group[v] = 3 - group[u]; q.add(v); }   // the other group
                else if (group[v] == group[u]) return false;
            }
        }
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| the graph | an undirected edge per dislike |
| the outer loop | every component (people with no dislikes form their own) |
| `group[s] = 1` | a component's first person goes in group 1 |
| `3 − group[u]` | every enemy goes in the other group |
| `group[v] == group[u]` | two enemies in the same group → impossible |

### Dry run: the odd cycle 1 – 2 – 3 – 4 – 5 – 1

| poll | assignments | conflict? |
|---|---|---|
| 1 (group 1) | 2 → 2, 5 → 2 | |
| 2 | 3 → 1 | |
| 5 | 4 → 1 | |
| 3 | its neighbour 4 is in group 1 = 3's group | **yes → false** |

### Complexity
- **Time O(n + D).**
- **Space O(n + D).**

---

## 6. Comparing the approaches

| | Every split | BFS 2-colouring |
|---|---|---|
| Time | O(2ⁿ · D) | **O(n + D)** |
| Space | O(1) | O(n + D) |

The file checks the BFS against every split on 1,000 random dislike lists.

---

## 7. Edge cases and common mistakes

**Edge cases:** no dislikes (true), duplicate dislike pairs, people with no dislikes, odd vs even cycles.

**Mistakes:**
1. Only colouring person 1's component.
2. Greedily placing people pair by pair in input order without propagating. With 1–2, 3–4, 1–3, it puts 1 and 3 both in group A from the first two pairs, then sees a "conflict", although {1, 4} / {2, 3} works.
3. 0-based arrays with 1-based people.

---

## 8. Interview questions and answers

**Q1. Why is "two groups" the same as "bipartite"?**
> The groups are the two colour classes; every dislike edge must cross between them, which is exactly the definition of a bipartite graph.

**Q2. When is it impossible?**
> Exactly when the dislike graph contains an odd cycle.

### Follow-up questions

**F1. Return the two groups.**
> Collect the people by their final `group` value.

**F2. Three groups.**
> 3-colouring is NP-complete: backtracking for small n.

**F3. Is Graph Bipartite.** → [M17](M17_IsGraphBipartite.md)

---

## 9. Running the file

```bash
java M25_PossibleBipartition.java      # 4/4 tests passed (including 1,000 random dislike lists vs all splits)
```

**One-line summary:** *"Turn each dislike into an undirected edge and 2-colour every component with BFS (group 3 − g for enemies); a same-group edge means it's impossible."*
