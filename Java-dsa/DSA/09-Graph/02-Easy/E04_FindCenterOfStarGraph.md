# E04 · Find Center of Star Graph ⭐

| | |
|---|---|
| **LeetCode** | [#1791 Find Center of Star Graph](https://leetcode.com/problems/find-center-of-star-graph/) |
| **Difficulty** | Easy |
| **Pattern** | Use the graph's structure: an O(1) observation beats a traversal |
| **Code** | [E04_FindCenterOfStarGraph.java](E04_FindCenterOfStarGraph.java) |
| **Run** | `java E04_FindCenterOfStarGraph.java` |

⭐ = not in `leetcode.md`, added for a key technique. Graph basics are explained in [E01](E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A **star graph** has one centre connected to every other vertex, and no other edges. Given its edges (vertices 1..n), return the centre.

```
[[1,2],[2,3],[4,2]]  →  2

      1
      |
  3 ─ 2 ─ 4
```

| edges | centre |
|---|---|
| `[[1,2],[2,3],[4,2]]` | 2 |
| `[[1,2],[5,1],[1,3],[1,4]]` | 1 |

---

## 2. How to think about it

### The general approach: count the degrees
The centre has degree n − 1, and every other vertex has degree 1. Count them: O(n).

### The observation: the centre is in **every** edge
So it's the vertex that the **first two edges** have in common. That's O(1)!

```
edges[0] = [a, b],  edges[1] = [c, d]   →   a if a == c or a == d, else b
```

### The lesson
Before writing a BFS, ask what the problem's structure already guarantees. (In the same spirit, a tree has exactly V − 1 edges: [M10 Graph Valid Tree](../03-Medium/M10_GraphValidTree.md).)

---

## 3. Java concepts used in this problem

### 3.1 A ternary for a two-way choice

```java
return (a == edges[1][0] || a == edges[1][1]) ? a : b;
```

If a appears in the second edge, it's the shared vertex; otherwise b must be (the two edges share exactly one vertex: the centre).

### 3.2 `n = edges.length + 1`

A star on n vertices has exactly n − 1 edges, so n can be derived from the edge count.

### 3.3 Building test data

The test plants a random centre, connects it to every other vertex in a random direction, and shuffles the edge list with `Collections.shuffle(list, rnd)`, so the O(1) method can't rely on any ordering.

---

## 4. Approach 1: Count the degrees

### Code ([E04_FindCenterOfStarGraph.java:36-42](E04_FindCenterOfStarGraph.java#L36-L42))

```java
static int byDegree(int[][] edges) {
    int n = edges.length + 1;
    int[] deg = new int[n + 1];
    for (int[] e : edges) { deg[e[0]]++; deg[e[1]]++; }
    for (int v = 1; v <= n; v++) if (deg[v] == n - 1) return v;
    return -1;
}
```

| Line | What it does |
|---|---|
| `deg[e[0]]++; deg[e[1]]++;` | an undirected edge adds 1 to both endpoints |
| `deg[v] == n − 1` | connected to everyone else → the centre |

**Dry run** `[[1,2],[2,3],[4,2]]` (n = 4): deg = [_, 1, 3, 1, 1] → deg[2] = 3 = n − 1 → **2**.

**O(n)** time and space. It works for any graph where you want "the vertex adjacent to everyone".

---

## 5. Approach 2: The first two edges (the file's solution)

### Code ([E04_FindCenterOfStarGraph.java:31-34](E04_FindCenterOfStarGraph.java#L31-L34))

```java
static int findCenter(int[][] edges) {
    int a = edges[0][0], b = edges[0][1];
    return (a == edges[1][0] || a == edges[1][1]) ? a : b;
}
```

### Line by line

| Line | What it does |
|---|---|
| `a, b` | the endpoints of the first edge; one of them is the centre |
| `a == edges[1][0] \|\| a == edges[1][1]` | does a also appear in the second edge? |
| `? a : b` | if yes, a is the centre; otherwise b is |

### Dry run 1: `[[1,2],[2,3],[4,2]]`
a = 1, b = 2; the second edge is [2, 3]: 1 isn't in it → **2** ✓

### Dry run 2: `[[1,2],[5,1],[1,3],[1,4]]`
a = 1, b = 2; the second edge is [5, 1]: 1 is in it → **1** ✓

### Complexity
- **Time O(1).**
- **Space O(1).**

---

## 6. Comparing the approaches

| | Count the degrees | The first two edges |
|---|---|---|
| Time | O(n) | **O(1)** |
| Space | O(n) | O(1) |
| Works if the input might not be a star? | it can verify | no (it trusts the guarantee) |

The file checks both on 500 random shuffled stars.

---

## 7. Edge cases and common mistakes

**Edge cases:** the smallest star (3 vertices, 2 edges), the centre written first or second in an edge.

**Mistakes:**
1. Assuming the centre is always `edges[0][0]` or always the larger label.
2. Building an adjacency list and running a BFS (it works, but it misses the point).

---

## 8. Interview questions and answers

**Q1. Why are two edges enough?**
> Every edge contains the centre, and two different edges of a star share exactly one vertex, so the shared vertex must be the centre.

**Q2. What if the graph might not be a star?**
> Count the degrees and verify: exactly one vertex has degree n − 1, every other has degree 1, and there are n − 1 edges.

### Follow-up questions

**F1. Find the Town Judge.** → [E03](E03_FindTheTownJudge.md)
> The directed analogue: in-degree n − 1 and out-degree 0.

**F2. Is the graph a tree?** → [M10](../03-Medium/M10_GraphValidTree.md)
> Exactly n − 1 edges and connected.

**F3. Maximum Star Sum (LC 2497).**
> For each vertex, take its value plus the best k positive neighbour values.

---

## 9. Running the file

```bash
java E04_FindCenterOfStarGraph.java      # 3/3 tests passed (including 500 random stars)
```

**One-line summary:** *"The centre is in every edge, so it's the vertex shared by the first two edges: O(1). Counting the degrees (centre = n − 1) is the general O(n) fallback."*
