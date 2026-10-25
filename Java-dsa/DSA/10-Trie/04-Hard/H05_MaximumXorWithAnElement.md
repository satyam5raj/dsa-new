# H05 · Maximum XOR With an Element From Array ⭐

| | |
|---|---|
| **LeetCode** | [#1707 Maximum XOR With an Element From Array](https://leetcode.com/problems/maximum-xor-with-an-element-from-array/) |
| **Difficulty** | Hard |
| **Pattern** | Offline queries (sorted by limit) + a binary trie built incrementally |
| **Code** | [H05_MaximumXorWithAnElement.java](H05_MaximumXorWithAnElement.java) |
| **Run** | `java H05_MaximumXorWithAnElement.java` |

⭐ = a bonus classic. The binary trie and the greedy XOR walk are explained in [M07](../03-Medium/M07_MaximumXorOfTwoNumbers.md).

---

## 1. The problem

`queries[i] = [x, m]`: the maximum of `x XOR nums[j]` over the `nums[j] ≤ m`, or −1 if no element is ≤ m.

| nums | queries | answer |
|---|---|---|
| [0,1,2,3,4] | [[3,1],[1,3],[5,6]] | [3, 3, 7] |
| [5,2,4,6,6,3] | [[12,4],[8,1],[6,3]] | [15, −1, 5] |

**Constraints:** up to 10⁵ numbers and 10⁵ queries.

---

## 2. How to think about it

[M07](../03-Medium/M07_MaximumXorOfTwoNumbers.md) (max XOR) uses a binary trie of **all** the numbers. Here each query may only use the numbers ≤ m: a different subset per query.

### The offline trick
We may answer the queries in **any** order (we just write each answer into its original slot). So:

```
sort nums ascending;  sort the query indices by m ascending
for each query (smallest m first):
    insert every not-yet-inserted num ≤ m into the trie      (a pointer sweep)
    if the trie is empty → −1,  else the greedy max-XOR walk (M07)
```

Each number is inserted once; each query is O(31).

**"Sort the queries so the data structure only grows"** is a powerful general idea (also used with Union-Find and Fenwick trees).

### The online alternative
Store the **minimum** value in each trie node's subtree, and refuse to enter subtrees whose min > m. That's also O(31) per query, with no sorting needed.

### Dry run: example 1

| query (in m order) | numbers inserted | best partner | answer |
|---|---|---|---|
| [3, 1] | 0, 1 | 3 ^ 0 = 3 (3 ^ 1 = 2) | 3 |
| [1, 3] | + 2, 3 | 1 ^ 2 = 3 | 3 |
| [5, 6] | + 4 | 5 ^ 2 = 7 | 7 |

→ `[3, 3, 7]` ✓

---

## 3. Java concepts used in this problem

### 3.1 Sorting query indices, not the queries

```java
Integer[] order = new Integer[queries.length];
for (int i = 0; i < order.length; i++) order[i] = i;
Arrays.sort(order, Comparator.comparingInt(i -> queries[i][1]));    // by limit m
```

Sorting the indices keeps the original positions, so each answer can be written back with `ans[qi] = …`.

### 3.2 The sweep pointer

```java
while (next < sorted.length && sorted[next] <= m) insert(root, sorted[next++]);
```

Because the limits only increase, the pointer only moves forward: n inserts in total over all the queries.

### 3.3 `next == 0` means "an empty trie"

If no number has been inserted yet, every number exceeds m → −1.

---

## 4. Approach 1: Scan every number per query (the referee)

### Code ([H05_MaximumXorWithAnElement.java:79-86](H05_MaximumXorWithAnElement.java#L79-L86))

```java
static int[] brute(int[] nums, int[][] qs) {
    int[] out = new int[qs.length];
    for (int i = 0; i < qs.length; i++) {
        out[i] = -1;
        for (int v : nums) if (v <= qs[i][1]) out[i] = Math.max(out[i], v ^ qs[i][0]);
    }
    return out;
}
```

**O(n · q)**: 10¹⁰ at the limits.

---

## 5. Approach 2: Online, with the minimum stored per node

```java
static class MinNode { MinNode[] child = new MinNode[2]; int min = Integer.MAX_VALUE; }

static int[] maximizeXorOnline(int[] nums, int[][] queries) {
    MinNode root = new MinNode();
    for (int x : nums) {
        MinNode n = root;
        n.min = Math.min(n.min, x);
        for (int b = 30; b >= 0; b--) {
            int bit = (x >> b) & 1;
            if (n.child[bit] == null) n.child[bit] = new MinNode();
            n = n.child[bit];
            n.min = Math.min(n.min, x);                       // the smallest number in this subtree
        }
    }
    int[] ans = new int[queries.length];
    for (int i = 0; i < queries.length; i++) {
        int x = queries[i][0], m = queries[i][1];
        if (root.min > m) { ans[i] = -1; continue; }           // every number exceeds m
        MinNode n = root;
        int res = 0;
        for (int b = 30; b >= 0; b--) {
            int bit = (x >> b) & 1;
            MinNode want = n.child[1 - bit];
            if (want != null && want.min <= m) { res |= 1 << b; n = want; }   // opposite bit, and still allowed
            else n = n.child[bit];
        }
        ans[i] = res;
    }
    return ans;
}
```

A subtree whose smallest number exceeds m contains no allowed numbers, so the walk only enters subtrees that still have one. The fallback child always contains the allowed number that got us here, so it's never null. **O(31 · (n + q))**, answering in the given order. (Stress-tested against the brute force on 3,000 random cases.)

---

## 6. Approach 3: Offline sweep + incremental trie (the file's main solution)

### Code ([H05_MaximumXorWithAnElement.java:40-77](H05_MaximumXorWithAnElement.java#L40-L77))

```java
static class Node { Node[] child = new Node[2]; }

static int[] maximizeXor(int[] nums, int[][] queries) {
    int[] sorted = nums.clone();
    Arrays.sort(sorted);
    Integer[] order = new Integer[queries.length];
    for (int i = 0; i < order.length; i++) order[i] = i;
    Arrays.sort(order, Comparator.comparingInt(i -> queries[i][1]));    // by limit m
    Node root = new Node();
    int[] ans = new int[queries.length];
    int next = 0;
    for (int qi : order) {
        int x = queries[qi][0], m = queries[qi][1];
        while (next < sorted.length && sorted[next] <= m) insert(root, sorted[next++]);   // grow the trie
        ans[qi] = next == 0 ? -1 : bestXor(root, x);
    }
    return ans;
}

static void insert(Node root, int x) {
    Node n = root;
    for (int b = 30; b >= 0; b--) {
        int bit = (x >> b) & 1;
        if (n.child[bit] == null) n.child[bit] = new Node();
        n = n.child[bit];
    }
}

static int bestXor(Node root, int x) {
    Node n = root;
    int res = 0;
    for (int b = 30; b >= 0; b--) {
        int bit = (x >> b) & 1;
        if (n.child[1 - bit] != null) { res |= 1 << b; n = n.child[1 - bit]; }
        else n = n.child[bit];
    }
    return res;
}
```

### Line by line

| Line | What it does |
|---|---|
| sort nums, sort the query order by m | both are swept in increasing order |
| the `while` | the trie now contains exactly the numbers ≤ m |
| `next == 0 ? -1` | no allowed number |
| `bestXor` | M07's greedy walk: take the opposite bit whenever possible |
| `ans[qi] = …` | write the answer in the query's original slot |

### Complexity
- **Time O(n log n + q log q + 31 · (n + q)).**
- **Space O(31 · n).**

---

## 7. Comparing the approaches

| | Scan per query | Online (min per node) | Offline sweep |
|---|---|---|---|
| Time | O(n · q) | O(31 · (n + q)) | O(sorts + 31 · (n + q)) |
| Needs all the queries up front? | no | no | **yes** |
| Extra per node | none | an int (min) | none |

The file checks the offline version against the brute force on 500 random cases.

---

## 8. Edge cases and common mistakes

**Edge cases:** m smaller than every number (−1), duplicate numbers, x = 0, m covering everything.

**Mistakes:**
1. Answering the queries in sorted order but forgetting to write back to the original index.
2. Inserting all the numbers at once (then the limit is ignored).
3. In the online version, entering a subtree without checking its min.

---

## 9. Interview questions and answers

**Q1. What makes the offline approach valid?**
> The answers don't depend on the order the queries are processed in. Sorting them by m lets the set of allowed numbers only grow, so a single incremental trie serves every query.

**Q2. When would you use the online version?**
> When the queries arrive one at a time (a stream), so they can't be sorted in advance.

### Follow-up questions

**F1. Numbers with a lower bound too (L ≤ num ≤ R).**
> Store the min and max per node, or use a persistent trie (versions by insertion order).

**F2. Maximum XOR of Two Numbers.** → [M07](../03-Medium/M07_MaximumXorOfTwoNumbers.md)

**F3. Other offline-sorted-query problems.**
> Distance-limited paths (LC 1697) with Union-Find: sort the edges and queries by weight limit.

---

## 10. Running the file

```bash
java H05_MaximumXorWithAnElement.java      # 3/3 tests passed (including 500 random cases vs brute force)
```

**One-line summary:** *"Sort the numbers and the queries by limit; sweep, inserting the numbers ≤ m into a binary trie, then answer each query with the greedy opposite-bit walk (−1 if the trie is empty)."*
