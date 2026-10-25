# M02 · Clone Graph

| | |
|---|---|
| **LeetCode** | [#133 Clone Graph](https://leetcode.com/problems/clone-graph/) |
| **Difficulty** | Medium |
| **Pattern** | DFS/BFS + a HashMap original → copy (the map is also the visited set) |
| **Code** | [M02_CloneGraph.java](M02_CloneGraph.java) |
| **Run** | `java M02_CloneGraph.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Deep-copy a **connected undirected** graph, given one of its nodes. Each `Node` has a `val` and a list of `neighbors`.

```
1 ── 2
|    |          adjacency: 1: [2, 4],  2: [1, 3],  3: [2, 4],  4: [1, 3]
4 ── 3
```

The copy must have the same structure and values, but **no node object in common** with the original.

---

## 2. How to think about it

### The difficulty: cycles and sharing
A naive recursive copy of 1 copies 2, which copies its neighbour 1 again, which copies 2… forever. And even without cycles, a node reachable in two ways must be copied **once** (shared), not duplicated.

### The fix: a map original → its copy
```
clone(node):
    if node is in the map → return the existing copy      (handles cycles and sharing)
    copy = new Node(node.val);  map.put(node, copy)       ← BEFORE recursing!
    for each neighbour: copy.neighbors.add(clone(neighbour))
```

Putting the copy into the map **before** visiting the neighbours is what breaks the cycle: when the recursion comes back around to this node, it finds the copy already registered.

It's the same idea as Copy List with Random Pointer ([Linked Lists](../../03-Linked-Lists/README.md)): an "old → new" map.

---

## 3. Java concepts used in this problem

### 3.1 A node class with a neighbour list

```java
static class Node {
    int val;
    List<Node> neighbors = new ArrayList<>();
    Node(int val) { this.val = val; }
}
```

The graph is stored as objects pointing at each other (rather than an adjacency list of ints).

### 3.2 `HashMap<Node, Node>` keyed by identity

Node doesn't override `equals`/`hashCode`, so the map uses **object identity**: two different nodes with the same `val` are different keys. That's exactly right for "which original node does this copy belong to?"

### 3.3 Shallow vs deep copy

- **Shallow:** a new node object whose `neighbors` list still points to the **original** nodes.
- **Deep:** every node reachable from the start is copied, and the copies point only to copies.

The test checks deep-ness by collecting every node object reachable from both graphs and confirming that the intersection is empty (`a.retainAll(b)` keeps only the common elements).

---

## 4. Approach 1: DFS with the copy map (the file's solution)

### Code ([M02_CloneGraph.java:42-54](M02_CloneGraph.java#L42-L54))

```java
static Node cloneGraph(Node node) {
    return clone(node, new HashMap<>());
}

static Node clone(Node node, Map<Node, Node> copies) {
    if (node == null) return null;
    Node existing = copies.get(node);
    if (existing != null) return existing;           // already copied (cycle or shared node)
    Node copy = new Node(node.val);
    copies.put(node, copy);                          // register BEFORE recursing
    for (Node nb : node.neighbors) copy.neighbors.add(clone(nb, copies));
    return copy;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (node == null) return null;` | an empty graph |
| `copies.get(node)` | has this node been copied already? |
| `return existing` | reuse the copy (this is what stops the infinite loop) |
| `new Node(node.val)` | create this node's copy, with no neighbours yet |
| `copies.put(node, copy)` | register it **before** recursing |
| the neighbour loop | copy (or fetch) each neighbour and link it |

### Dry run: the 4-cycle, starting at 1

| call | in the map? | action | map after |
|---|---|---|---|
| clone(1) | no | create 1′, register | {1: 1′} |
| → clone(2) | no | create 2′ | {1, 2} |
| → → clone(1) | **yes** | return 1′ → 2′.neighbors = [1′] | |
| → → clone(3) | no | create 3′ | {1, 2, 3} |
| → → → clone(2) | yes → 2′ | 3′.neighbors = [2′] | |
| → → → clone(4) | no | create 4′ | {1, 2, 3, 4} |
| → → → → clone(1) | yes → 1′; clone(3) yes → 3′ | 4′.neighbors = [1′, 3′] | |
| | | 3′.neighbors = [2′, 4′]; 2′.neighbors = [1′, 3′] | |
| → clone(4) | yes → 4′ | 1′.neighbors = [2′, 4′] | |

Every node is created once, and every copy points only at copies ✓

### Complexity
- **Time O(V + E):** each node is created once, and each neighbour entry is processed once.
- **Space O(V)** for the map and the recursion.

---

## 5. Approach 2: BFS with the copy map

```java
static Node cloneGraphBfs(Node node) {
    if (node == null) return null;
    Map<Node, Node> copies = new HashMap<>();
    copies.put(node, new Node(node.val));
    Queue<Node> q = new ArrayDeque<>();
    q.add(node);
    while (!q.isEmpty()) {
        Node u = q.poll();
        for (Node v : u.neighbors) {
            if (!copies.containsKey(v)) {                 // first time we see v: copy and queue it
                copies.put(v, new Node(v.val));
                q.add(v);
            }
            copies.get(u).neighbors.add(copies.get(v));   // link the copies
        }
    }
    return copies.get(node);
}
```

The same map, filled level by level instead of depth-first. A node is copied the first time it's **seen** (exactly like marking visited when pushing), and every edge u → v is mirrored as u′ → v′. No recursion, so no stack overflow on long chains.

---

## 6. Comparing the approaches

| | DFS + map | BFS + map |
|---|---|---|
| Time | O(V + E) | O(V + E) |
| Space | O(V) (map + recursion) | O(V) (map + queue) |
| Deep chains | recursion depth V | fine |

The file clones 300 random connected graphs and checks both the structure and that no node object is shared.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty graph (null), a single node with no neighbours, self-loops, very dense graphs.

**Mistakes:**
1. Registering the copy **after** recursing (an infinite loop on cycles).
2. A `Set<Node>` of visited nodes **without** the map (you can't find the existing copy to link to).
3. Copying the neighbours list by reference (a shallow copy).
4. Keying the map by `val` when values might repeat.

---

## 8. Interview questions and answers

**Q1. Why must the copy be registered before visiting the neighbours?**
> Because of cycles: a neighbour's recursion may lead back to this node. If the copy is already in the map, that call returns it immediately instead of starting another copy.

**Q2. How do you test that the clone is deep?**
> Collect every node object reachable from the original and from the clone; the two sets must be disjoint, while the serialised structures must be identical.

### Follow-up questions

**F1. Copy List with Random Pointer (LC 138).**
> The same old → new map, on a list with an extra pointer (or the interleaving trick for O(1) extra space).

**F2. Clone a directed graph, or one that may be disconnected.**
> The same code works for directed graphs. For a disconnected one, start a clone from every node not yet in the map.

**F3. Clone a binary tree with random pointers (LC 1485).**
> The same map from original to copy nodes.

---

## 9. Running the file

```bash
java M02_CloneGraph.java      # 5/5 tests passed (including 300 random graphs cloned)
```

**One-line summary:** *"DFS (or BFS) with a map original → copy: return the existing copy if there is one, otherwise create it, register it before recursing, then link the cloned neighbours."*
