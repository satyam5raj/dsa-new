# M17 · Is Graph Bipartite?

| | |
|---|---|
| **LeetCode** | [#785 Is Graph Bipartite?](https://leetcode.com/problems/is-graph-bipartite/) |
| **Difficulty** | Medium |
| **Pattern** | 2-colouring with BFS (on every component!) |
| **Code** | [M17_IsGraphBipartite.java](M17_IsGraphBipartite.java) |
| **Run** | `java M17_IsGraphBipartite.java` |

BFS basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#34-bfs-breadth-first-search-with-a-queue).

---

## 1. The problem

`graph[u]` lists u's neighbours (undirected, possibly **disconnected**). Can the vertices be split into two sets so that every edge goes **between** the sets?

```
[[1,2,3],[0,2],[0,1,3],[0,2]]  → false     0 ─ 1          (triangle 0-1-2)
                                           │ ╲ │
                                           3 ─ 2

[[1,3],[0,2],[1,3],[0,2]]      → true      0 ─ 1          (square: {0, 2} vs {1, 3})
                                           │   │
                                           3 ─ 2
```

| graph | answer |
|---|---|
| the triangle example | false |
| the square | true |
| `[[1],[0],[3,4],[2,4],[2,3]]` | false (an odd cycle in the 2nd component) |

---

## 2. How to think about it

### 2-colouring
Give a start vertex colour 0; every neighbour must get the **other** colour; continue with BFS. If an edge ever connects two vertices of the **same** colour → not bipartite.

**A graph is bipartite ⇔ it has no odd cycle.** (Going around an odd cycle, the alternating colours can't match up again.)

### The trap: disconnected graphs
Start a new BFS from **every** uncoloured vertex, or only the first component gets checked.

### Dry run: the triangle example (from the real run)
```
colour 0 = 0
from 0: colour 1 = 1, colour 2 = 1, colour 3 = 1
from 1: neighbour 2 has colour 1 = colour of 1  → conflict on edge 1–2 → false
```

[Possible Bipartition (M25)](M25_PossibleBipartition.md) is this problem with "dislike" edges.

---

## 3. Java concepts used in this problem

### 3.1 `-1` as "uncoloured"

```java
int[] color = new int[n];
Arrays.fill(color, -1);
```

Colours are 0 and 1, so −1 marks "not visited yet". The colour array doubles as the visited array.

### 3.2 Flipping between 0 and 1

```java
color[v] = 1 - color[u];
```

1 − 0 = 1 and 1 − 1 = 0. (`color[u] ^ 1` does the same.)

### 3.3 The input is already an adjacency list

`int[][] graph`: `graph[u]` is an array of u's neighbours, so no list needs to be built.

---

## 4. Approach 1: Try every colouring (the referee)

### Code ([M17_IsGraphBipartite.java:53-61](M17_IsGraphBipartite.java#L53-L61))

```java
static boolean brute(int[][] g) {
    int n = g.length;
    for (int mask = 0; mask < (1 << n); mask++) {
        boolean ok = true;
        for (int u = 0; u < n && ok; u++) for (int v : g[u]) if ((mask >> u & 1) == (mask >> v & 1)) { ok = false; break; }
        if (ok) return true;
    }
    return false;
}
```

Bit u of `mask` is u's side. A colouring is valid if no edge has both ends on the same side. **O(2ⁿ · E)**: tiny graphs only.

---

## 5. Approach 2: BFS 2-colouring (the file's solution)

### Code ([M17_IsGraphBipartite.java:33-50](M17_IsGraphBipartite.java#L33-L50))

```java
static boolean isBipartite(int[][] graph) {
    int n = graph.length;
    int[] color = new int[n];
    Arrays.fill(color, -1);
    for (int start = 0; start < n; start++) {            // every component
        if (color[start] != -1) continue;
        color[start] = 0;
        Queue<Integer> q = new ArrayDeque<>(List.of(start));
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : graph[u]) {
                if (color[v] == -1) { color[v] = 1 - color[u]; q.add(v); }
                else if (color[v] == color[u]) return false;   // conflict: same colour on an edge
            }
        }
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| the `start` loop | every component gets its own BFS |
| `color[start] = 0` | a component's first vertex can take either colour |
| `color[v] == -1` | uncoloured → give it the opposite colour and explore it |
| `color[v] == color[u]` | an edge inside one colour class → not bipartite |

### Dry run: `[[1],[0],[3,4],[2,4],[2,3]]`

| start | BFS | result |
|---|---|---|
| 0 | 0 → colour 0; 1 → colour 1 | component 1 is fine |
| 2 | 2 → 0; 3 → 1, 4 → 1; from 3: neighbour 4 has colour 1 = colour of 3 | **false** |

### Complexity
- **Time O(V + E).**
- **Space O(V).**

---

## 6. Approach 3: Union-Find

```java
static boolean isBipartiteUnionFind(int[][] graph) {
    int n = graph.length;
    int[] parent = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    for (int u = 0; u < n; u++) {
        for (int v : graph[u]) {
            if (find(parent, u) == find(parent, v)) return false;   // u and v forced to the same side
            parent[find(parent, v)] = find(parent, graph[u][0]);    // all of u's neighbours share a side
        }
    }
    return true;
}

static int find(int[] p, int x) {
    while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
    return x;
}
```

All of u's neighbours must be on the same side (the opposite of u), so union them together. If u ever ends up in the same set as one of its neighbours, it's a contradiction.

---

## 7. Comparing the approaches

| | Every colouring | BFS colouring | Union-Find |
|---|---|---|---|
| Time | O(2ⁿ · E) | O(V + E) | O(E · α) |
| Gives the two sets? | yes | **yes** (the colours) | no (not directly) |

The file checks BFS against every colouring on 1,000 random graphs.

---

## 8. Edge cases and common mistakes

**Edge cases:** no edges (bipartite), a single vertex, disconnected components, an even cycle (bipartite) vs an odd cycle (not).

**Mistakes:**
1. Checking only the component containing vertex 0.
2. Treating "already visited" as "same colour".
3. Using DFS recursion without handling the components the same way.

---

## 9. Interview questions and answers

**Q1. Why does an odd cycle make 2-colouring impossible?**
> Walking around a cycle, the colours alternate. After an odd number of steps you're back at the start with the opposite colour, which is a contradiction.

**Q2. DFS or BFS?**
> Either: the colouring logic is the same. BFS avoids deep recursion.

### Follow-up questions

**F1. Possible Bipartition.** → [M25](M25_PossibleBipartition.md)
> Build the graph from the "dislike" pairs, then 2-colour it.

**F2. Return the two groups.**
> Collect the vertices by colour after a successful colouring.

**F3. k-colouring for k ≥ 3.**
> NP-hard in general: backtracking ([14-Backtracking](../../14-Backtracking/README.md)).

---

## 10. Running the file

```bash
java M17_IsGraphBipartite.java      # 4/4 tests passed (including 1,000 random graphs vs all colourings)
```

**One-line summary:** *"BFS from every uncoloured vertex, giving each neighbour the opposite colour; an edge between two vertices of the same colour means the graph isn't bipartite."*
