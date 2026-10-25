# M06 · Number of Operations to Make Network Connected

| | |
|---|---|
| **LeetCode** | [#1319 Number of Operations to Make Network Connected](https://leetcode.com/problems/number-of-operations-to-make-network-connected/) |
| **Difficulty** | Medium |
| **Pattern** | Count the components (and the spare cables) with one Union-Find pass |
| **Code** | [M06_NumberOfOperationsToMakeNetworkConnected.java](M06_NumberOfOperationsToMakeNetworkConnected.java) |
| **Run** | `java M06_NumberOfOperationsToMakeNetworkConnected.java` |

Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

n computers; `connections[i]` is a cable. One operation = unplug any cable and plug it between two other computers. What's the minimum number of operations to connect everything, or −1 if it's impossible?

| n | connections | answer |
|---|---|---|
| 4 | [[0,1],[0,2],[1,2]] | 1 (move 1–2 to connect 3) |
| 6 | [[0,1],[0,2],[0,3],[1,2],[1,3]] | 2 |
| 6 | [[0,1],[0,2],[0,3],[1,2]] | −1 (only 4 cables; 5 are needed) |
| 3 | [[0,1],[1,2]] | 0 (already connected) |

---

## 2. How to think about it

### Two facts
1. Connecting n computers needs at least **n − 1** cables. Fewer → −1 right away.
2. With enough cables, joining c components takes exactly **c − 1** moves, and there are always enough spare cables: every cable that closes a cycle (the union fails) is redundant and can be moved.

So: **answer = components − 1** (after the n − 1 check).

### Why are there always enough spare cables?
With E ≥ n − 1 cables and c components, the cables that aren't redundant form spanning trees of the components: n − c cables. So the number of spare cables is E − (n − c) ≥ (n − 1) − (n − c) = c − 1, exactly what's needed.

### Dry run: n = 4
```
0–1 (3 components), 0–2 (2 components), 1–2 redundant → 2 components → 1 move
```

---

## 3. Java concepts used in this problem

### 3.1 The early return on the cable count

```java
if (connections.length < n - 1) return -1;
```

That's the whole impossibility condition: no graph search is needed to detect it.

### 3.2 Counting the components inside the union loop

```java
int components = n;
if (a != b) { parent[a] = b; components--; }       // else: a spare cable
```

The same counter as in [Graph M11](../../09-Graph/03-Medium/M11_NumberOfConnectedComponents.md).

---

## 4. Approach 1: DFS components (the referee)

### Code ([M06_NumberOfOperationsToMakeNetworkConnected.java:55-70](M06_NumberOfOperationsToMakeNetworkConnected.java#L55-L70))

```java
static int brute(int n, int[][] cs) {
    if (cs.length < n - 1) return -1;
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] c : cs) { adj.get(c[0]).add(c[1]); adj.get(c[1]).add(c[0]); }
    boolean[] seen = new boolean[n];
    int comps = 0;
    for (int s = 0; s < n; s++) {
        if (seen[s]) continue;
        comps++;
        Deque<Integer> st = new ArrayDeque<>(List.of(s));
        seen[s] = true;
        while (!st.isEmpty()) for (int v : adj.get(st.pop())) if (!seen[v]) { seen[v] = true; st.push(v); }
    }
    return comps - 1;
}
```

The same reasoning, with the components counted by DFS: **O(V + E)**.

---

## 5. Approach 2: Union-Find (the file's main solution)

### Code ([M06_NumberOfOperationsToMakeNetworkConnected.java:35-52](M06_NumberOfOperationsToMakeNetworkConnected.java#L35-L52))

```java
static int[] parent;

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static int makeConnected(int n, int[][] connections) {
    if (connections.length < n - 1) return -1;           // not enough cables in total
    parent = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    int components = n;
    for (int[] c : connections) {
        int a = find(c[0]), b = find(c[1]);
        if (a != b) { parent[a] = b; components--; }       // else: a spare cable
    }
    return components - 1;
}
```

### Line by line

| Line | What it does |
|---|---|
| the cable-count check | fewer than n − 1 cables can never connect n computers |
| `components = n` | everyone alone |
| a successful union | two components merge |
| (a failed union) | the cable is spare, available to move |
| `components − 1` | the bridges needed between the components |

### Dry run: n = 6, [[0,1],[0,2],[0,3],[1,2],[1,3]]

| cable | effect | components |
|---|---|---|
| | | 6 |
| 0–1, 0–2, 0–3 | merges | 3 ({0, 1, 2, 3}, {4}, {5}) |
| 1–2, 1–3 | spare | 3 |

5 cables ≥ 5 → answer 3 − 1 = **2** ✓

### Complexity
- **Time O(E · α).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | DFS | Union-Find |
|---|---|---|
| Time | O(V + E) | O(E · α) |
| Space | O(V + E) | **O(V)** |

The file checks Union-Find against the DFS on 1,000 random networks.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 (0), exactly n − 1 cables forming a tree (0), many isolated computers with lots of spare cables.

**Mistakes:**
1. Counting the spare cables and comparing them, instead of using the simple n − 1 check.
2. Returning the number of components instead of components − 1.
3. Forgetting that isolated computers are components too.

---

## 8. Interview questions and answers

**Q1. Why is the n − 1 check sufficient for feasibility?**
> If there are at least n − 1 cables, the spare cables number at least components − 1 (see section 2), which is exactly enough to link the components.

**Q2. Which cables should be moved?**
> Any cable whose union failed (it closes a cycle). Removing it doesn't disconnect anything.

### Follow-up questions

**F1. List which cables to move and where.**
> Collect the spare cables; for each component other than the first, re-plug one spare cable between it and the first component.

**F2. Number of Connected Components.** → [Graph M11](../../09-Graph/03-Medium/M11_NumberOfConnectedComponents.md)

**F3. The minimum cost to connect everything.** → [Graph M12](../../09-Graph/03-Medium/M12_MinCostToConnectAllPoints.md)
> A minimum spanning tree.

---

## 9. Running the file

```bash
java M06_NumberOfOperationsToMakeNetworkConnected.java      # 5/5 tests passed (including 1,000 random networks vs DFS components)
```

**One-line summary:** *"If there are fewer than n − 1 cables it's impossible; otherwise count the components with Union-Find, and the answer is components − 1."*
