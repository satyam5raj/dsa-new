# M16 · Evaluate Division

| | |
|---|---|
| **LeetCode** | [#399 Evaluate Division](https://leetcode.com/problems/evaluate-division/) |
| **Difficulty** | Medium |
| **Pattern** | A weighted graph of ratios; answer = the **product** of the weights along a path |
| **Code** | [M16_EvaluateDivision.java](M16_EvaluateDivision.java) |
| **Run** | `java M16_EvaluateDivision.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

`equations [["a","b"],["b","c"]]` with `values [2.0, 3.0]` mean a/b = 2 and b/c = 3. Answer queries like a/c, b/a, a/e and x/x (−1.0 if unknown).

| query | answer |
|---|---|
| a / c | 6.0 |
| b / a | 0.5 |
| a / e | −1.0 (e never appears) |
| a / a | 1.0 |
| x / x | −1.0 (x never appears, even divided by itself) |

---

## 2. How to think about it

### The model
Each variable is a vertex. `a / b = 2` gives **two** directed edges:

```
a ─(2)→ b          b ─(1/2)→ a
```

Then `a / c = (a / b) × (b / c)`: **multiply** the weights along any path from a to c. (Any path gives the same product if the equations are consistent.)

```
a ─2→ b ─3→ c     ⇒   a / c = 2 × 3 = 6
```

### Answering a query (x, y)
- x or y unknown → −1
- DFS/BFS from x, carrying the running product; reaching y → return the product
- unreachable (different components) → −1

Each query is O(V + E). With many queries, a **weighted Union-Find** (each node stores its ratio to its root) answers each in about O(1) ([11-Union-Find](../../11-Union-Find/README.md)).

---

## 3. Java concepts used in this problem

### 3.1 A graph keyed by strings: `Map<String, Map<String, Double>>`

```java
g.computeIfAbsent(a, k -> new HashMap<>()).put(b, values[i]);        // a / b
g.computeIfAbsent(b, k -> new HashMap<>()).put(a, 1.0 / values[i]);  // b / a
```

The vertices are names, so the adjacency "list" is a map from each name to a map of neighbour → weight.

### 3.2 `-1.0` as a "not found" signal

The DFS returns −1.0 for "no path". That's safe here because every ratio is positive, so a real answer can never be −1.0. (In general, a sentinel must lie outside the range of real answers.)

### 3.3 Comparing doubles in the test

```java
Math.abs(got - val[x] / val[y]) < 1e-9
```

Products of doubles may differ in their last bits depending on the path, so the test uses a tolerance.

### 3.4 A fresh `seen` set per query

`new HashSet<>()` for each query: a node visited in one query must be available again in the next.

---

## 4. Approach 1: DFS per query (the file's solution)

### Code ([M16_EvaluateDivision.java:36-60](M16_EvaluateDivision.java#L36-L60))

```java
static double[] calcEquation(List<List<String>> equations, double[] values, List<List<String>> queries) {
    Map<String, Map<String, Double>> g = new HashMap<>();
    for (int i = 0; i < equations.size(); i++) {
        String a = equations.get(i).get(0), b = equations.get(i).get(1);
        g.computeIfAbsent(a, k -> new HashMap<>()).put(b, values[i]);        // a / b
        g.computeIfAbsent(b, k -> new HashMap<>()).put(a, 1.0 / values[i]);  // b / a
    }
    double[] out = new double[queries.size()];
    for (int i = 0; i < queries.size(); i++) {
        String x = queries.get(i).get(0), y = queries.get(i).get(1);
        out[i] = (!g.containsKey(x) || !g.containsKey(y)) ? -1.0 : dfs(g, x, y, 1.0, new HashSet<>());
    }
    return out;
}

static double dfs(Map<String, Map<String, Double>> g, String cur, String target, double product, Set<String> seen) {
    if (cur.equals(target)) return product;
    seen.add(cur);
    for (Map.Entry<String, Double> e : g.get(cur).entrySet()) {
        if (seen.contains(e.getKey())) continue;
        double r = dfs(g, e.getKey(), target, product * e.getValue(), seen);
        if (r != -1.0) return r;
    }
    return -1.0;
}
```

### Line by line

| Line | What it does |
|---|---|
| the equation loop | two directed edges per equation: the ratio and its reciprocal |
| the unknown-variable check | a variable that never appeared → −1 (this also makes x/x = −1 for an unknown x) |
| `dfs(..., 1.0, ...)` | start with the product 1 (x/x = 1) |
| `cur.equals(target)` | reached y: the product is x/y |
| `product * e.getValue()` | extend the ratio along the edge |
| `if (r != -1.0) return r;` | stop at the first path found |

### Dry run: a / c

| call | product | action |
|---|---|---|
| dfs(a) | 1.0 | a ≠ c → neighbour b (weight 2) |
| dfs(b) | 2.0 | b ≠ c → neighbours: a (seen), c (weight 3) |
| dfs(c) | 6.0 | c == target → **6.0** |

### Dry run: b / a
dfs(b, 1.0) → neighbour a (weight 0.5) → reached → **0.5** ✓

### Complexity
- **Time O(Q · (V + E)).**
- **Space O(V + E).**

---

## 5. Approach 2: Weighted Union-Find

```java
static Map<String, String> parent = new HashMap<>();
static Map<String, Double> ratio = new HashMap<>();      // ratio.get(x) = x / parent(x)

static String find(String x) {
    if (!parent.get(x).equals(x)) {
        String p = parent.get(x);
        String root = find(p);
        ratio.put(x, ratio.get(x) * ratio.get(p));        // x/root = x/p × p/root
        parent.put(x, root);                              // path compression
    }
    return parent.get(x);
}

static void union(String a, String b, double value) {     // a / b = value
    parent.putIfAbsent(a, a); ratio.putIfAbsent(a, 1.0);
    parent.putIfAbsent(b, b); ratio.putIfAbsent(b, 1.0);
    String ra = find(a), rb = find(b);
    if (!ra.equals(rb)) {
        parent.put(ra, rb);
        ratio.put(ra, value * ratio.get(b) / ratio.get(a));   // ra/rb = (a/b) × (b/rb) / (a/ra)
    }
}

static double query(String x, String y) {
    if (!parent.containsKey(x) || !parent.containsKey(y)) return -1.0;
    if (!find(x).equals(find(y))) return -1.0;
    return ratio.get(x) / ratio.get(y);                   // (x/root) / (y/root)
}
```

Each node stores its ratio to its root; path compression multiplies the ratios along the way. A query is then `(x/root) / (y/root)`, almost O(1). It's worth knowing when there are many queries.

---

## 6. Comparing the approaches

| | DFS per query | Weighted Union-Find |
|---|---|---|
| Build | O(E) | O(E · α) |
| Per query | O(V + E) | ~O(1) |
| Code | simple | trickier ratio bookkeeping |

The file checks the DFS on 300 random consistent systems (each variable has a hidden value, and every equation is a true ratio).

---

## 7. Edge cases and common mistakes

**Edge cases:** x == y but unknown (−1), x == y and known (1), variables in different components (−1), multi-letter variable names.

**Mistakes:**
1. Adding only one direction (b/a becomes unreachable).
2. Returning 1.0 for an unknown x/x.
3. Reusing the `seen` set across queries.
4. Using `==` to compare Strings.

---

## 8. Interview questions and answers

**Q1. Why does any path give the same answer?**
> If the equations are consistent, each variable has a value (up to a common scale), and the product along any path from x to y telescopes to value(x)/value(y).

**Q2. How would you speed up many queries?**
> A weighted Union-Find, or precompute, per connected component, every variable's ratio to one root with a single BFS, then answer each query in O(1).

### Follow-up questions

**F1. Detect contradictory equations.**
> In the weighted Union-Find, when a and b are already connected, check that the stored ratio matches the new value.

**F2. Currency arbitrage (is there a cycle with a product > 1?).**
> Take −log of each rate and look for a negative cycle with Bellman-Ford.

**F3. Many equations and queries online.**
> The weighted Union-Find handles interleaved unions and queries.

---

## 9. Running the file

```bash
java M16_EvaluateDivision.java      # 3/3 tests passed (including 300 random consistent systems)
```

**One-line summary:** *"Build a graph with a/b → value and b/a → 1/value; for each query, DFS from x multiplying the weights until y is reached (−1 if unknown or unreachable)."*
