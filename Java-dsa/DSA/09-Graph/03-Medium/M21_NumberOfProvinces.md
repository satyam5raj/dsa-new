# M21 · Number of Provinces

| | |
|---|---|
| **LeetCode** | [#547 Number of Provinces](https://leetcode.com/problems/number-of-provinces/) |
| **Difficulty** | Medium |
| **Pattern** | Connected components on an **adjacency matrix** |
| **Code** | [M21_NumberOfProvinces.java](M21_NumberOfProvinces.java) |
| **Run** | `java M21_NumberOfProvinces.java` |

The same problem with an edge list is [M11](M11_NumberOfConnectedComponents.md) (which also explains Union-Find); the matrix representation is compared in [E01](../02-Easy/E01_FindIfPathExists.md#32-three-ways-to-store-a-graph).

---

## 1. The problem

`isConnected[i][j] = 1` if city i and city j are directly connected. A **province** is a group of directly or indirectly connected cities. Count the provinces.

| isConnected | answer |
|---|---|
| `[[1,1,0],[1,1,0],[0,0,1]]` | 2 ({0, 1} and {2}) |
| the identity matrix (3 × 3) | 3 (every city alone) |
| `[[1,1,0],[1,1,1],[0,1,1]]` | 1 (0–1 and 1–2: indirectly connected) |

---

## 2. How to think about it

It's the same as [M11](M11_NumberOfConnectedComponents.md) (connected components), but the graph comes as an **adjacency matrix**. The neighbours of i are the j with `isConnected[i][j] == 1`, found by scanning row i in O(n). So the DFS costs O(n²) in total, which is also the input size.

```
for each city: if unvisited → provinces++, then DFS to mark its whole province
```

Union-Find also works: union every (i, j) with a 1, then count the roots.

---

## 3. Java concepts used in this problem

### 3.1 Neighbours from a matrix row

```java
for (int j = 0; j < m.length; j++) if (m[i][j] == 1 && !seen[j]) dfs(m, j, seen);   // row scan = neighbours
```

There's no adjacency list: row i **is** the neighbour information.

### 3.2 Using only the upper triangle

```java
for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) if (m[i][j] == 1) { ... }
```

The matrix is symmetric (`m[i][j] == m[j][i]`) and the diagonal is always 1, so Union-Find only needs the pairs j > i.

---

## 4. Approach 1: DFS over the matrix (the file's main solution)

### Code ([M21_NumberOfProvinces.java:29-44](M21_NumberOfProvinces.java#L29-L44))

```java
static int findCircleNum(int[][] isConnected) {
    int n = isConnected.length, provinces = 0;
    boolean[] seen = new boolean[n];
    for (int i = 0; i < n; i++) {
        if (!seen[i]) {
            provinces++;
            dfs(isConnected, i, seen);
        }
    }
    return provinces;
}

static void dfs(int[][] m, int i, boolean[] seen) {
    seen[i] = true;
    for (int j = 0; j < m.length; j++) if (m[i][j] == 1 && !seen[j]) dfs(m, j, seen);   // row scan = neighbours
}
```

### Line by line

| Line | What it does |
|---|---|
| the outer loop | every city not yet in a counted province starts a new one |
| `dfs` | mark city i, then visit every unseen city in row i |

### Dry run: `[[1,1,0],[1,1,1],[0,1,1]]`

| step | action | seen |
|---|---|---|
| i = 0, unseen | provinces = 1; dfs(0): row 0 → j = 1 | {0} |
| dfs(1) | row 1 → j = 2 (0 is seen) | {0, 1} |
| dfs(2) | row 2: nothing new | {0, 1, 2} |
| i = 1, 2 | already seen | |

**1** ✓

### Complexity
- **Time O(n²).**
- **Space O(n)** (the recursion is at most n deep).

---

## 5. Approach 2: Union-Find

### Code ([M21_NumberOfProvinces.java:46-57](M21_NumberOfProvinces.java#L46-L57))

```java
static int unionFind(int[][] m) {
    int n = m.length, count = n;
    int[] p = new int[n];
    for (int i = 0; i < n; i++) p[i] = i;
    for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) if (m[i][j] == 1) {
        int a = find(p, i), b = find(p, j);
        if (a != b) { p[a] = b; count--; }
    }
    return count;
}

static int find(int[] p, int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }
```

Start with n provinces; every connection that joins two different groups merges them. **O(n² · α(n))**.

---

## 6. Comparing the approaches

| | DFS | Union-Find |
|---|---|---|
| Time | O(n²) | O(n² · α) |
| Space | O(n) | O(n) |

The file checks DFS against Union-Find on 500 random symmetric matrices.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1, the identity matrix (n provinces), all ones (1 province).

**Mistakes:**
1. Counting the 1s in the matrix instead of the components.
2. Building an adjacency list unnecessarily (fine, but extra code).
3. Treating the diagonal as a meaningful edge (it's always 1; harmless here because the city is already seen).

---

## 8. Interview questions and answers

**Q1. Why is the DFS O(n²) and not O(V + E)?**
> With an adjacency matrix, finding a vertex's neighbours costs O(n) regardless of how many there are. That's n vertices × O(n) = O(n²), which matches the input size anyway.

**Q2. When is a matrix the better representation?**
> Dense graphs, or when O(1) "is there an edge?" queries are needed.

### Follow-up questions

**F1. Number of Connected Components (an edge list).** → [M11](M11_NumberOfConnectedComponents.md)

**F2. The largest province's size.**
> Make the DFS count the cities it marks; take the max.

**F3. Redundant Connection.**
> Union-Find: the first edge whose ends are already connected.

---

## 9. Running the file

```bash
java M21_NumberOfProvinces.java      # 4/4 tests passed (including 500 random matrices: DFS == Union-Find)
```

**One-line summary:** *"Count the DFS starts from unvisited cities, where a city's neighbours are the 1s in its matrix row (or use Union-Find over the upper triangle)."*
