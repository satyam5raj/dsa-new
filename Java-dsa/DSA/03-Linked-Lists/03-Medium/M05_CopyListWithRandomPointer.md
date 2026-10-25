# M05 · Copy List with Random Pointer

| | |
|---|---|
| **LeetCode** | [#138 Copy List with Random Pointer](https://leetcode.com/problems/copy-list-with-random-pointer/) |
| **Difficulty** | Medium |
| **Pattern** | HashMap old → new, or interleaving (O(1) extra space) |
| **Code** | [M05_CopyListWithRandomPointer.java](M05_CopyListWithRandomPointer.java) |
| **Run** | `java M05_CopyListWithRandomPointer.java` |

---

## 1. The problem

Each node has `next` **and** `random` (pointing to **any** node in the list, or `null`). Make a **deep copy**: brand-new nodes, and none of the copy's pointers may point into the original list.

```
original:  7 → 13 → 11 → 10 → 1
random:    7→null, 13→7, 11→1, 10→11, 1→7
```

(The file describes a list as `[[val, randomIndex], ...]`: `[[7,null],[13,0],[11,4],[10,2],[1,0]]`.)

**Constraints:** `0 ≤ n ≤ 1000`.

---

## 2. How to think about it

### The difficulty
Copying `next` is easy: walk and create. But when copying node A, `A.random` may point to a node **we haven't copied yet** (or one that's far back). Which **new** node should `copyA.random` point to?

### Approach 1: a HashMap original → copy (two passes)
- **Pass 1:** create a copy of every node; `map.put(original, copy)`.
- **Pass 2:** `copy.next = map.get(orig.next)`, `copy.random = map.get(orig.random)`.

The map answers "which copy corresponds to this original node?" in O(1).

### Approach 2: interleave, so the list itself is the map (O(1) extra)
1. Insert each copy **right after** its original: `A → A' → B → B' → C → C'`. Now **the copy of X is simply `X.next`**.
2. Set the randoms: `X'.random = (X.random == null) ? null : X.random.next`. (The copy of X's random target is the node right after that target.)
3. **Unweave:** restore the original list and extract the copies.

---

## 3. Java concepts used in this problem

### 3.1 Objects as `HashMap` keys (identity semantics)

```java
Map<Node, Node> copyOf = new HashMap<>();
copyOf.put(original, copy);
```

`Node` doesn't override `equals`/`hashCode`, so each node is its **own** key (identity), even if two nodes have the same value. That's exactly what we need: we're mapping **node objects**, not values.

### 3.2 `map.get(null)` returns `null`

`HashMap` allows a `null` key. `copyOf.get(c.next)` when `c.next == null` returns `null` (there's no such key), which is exactly the right value for the copy's `next`. That avoids a special case.

### 3.3 `IdentityHashMap` and `Collections.newSetFromMap` (in the test)

```java
Set<Node> orig = Collections.newSetFromMap(new IdentityHashMap<>());
```

`IdentityHashMap` compares keys with `==` **even if** `equals` is overridden. `newSetFromMap` turns any map into a `Set` view. The test uses it to make sure **no node of the copy is a node of the original**, which is the definition of a deep copy.

### 3.4 Deep copy versus shallow copy

| | shallow copy | deep copy |
|---|---|---|
| new top-level object | yes | yes |
| inner references | **shared** with the original | **new objects** too |

`array.clone()` on a 2D array is shallow (see Arrays M08). This problem asks for the deep version of a linked structure.

---

## 4. Approach 1: HashMap (the file's first method)

### Code ([M05_CopyListWithRandomPointer.java:44-52](M05_CopyListWithRandomPointer.java#L44-L52))

```java
static Node copyRandomList(Node head) {
    Map<Node, Node> copyOf = new HashMap<>();
    for (Node c = head; c != null; c = c.next) copyOf.put(c, new Node(c.val));
    for (Node c = head; c != null; c = c.next) {
        copyOf.get(c).next = copyOf.get(c.next);        // get(null) -> null, conveniently
        copyOf.get(c).random = copyOf.get(c.random);
    }
    return copyOf.get(head);
}
```

### Line by line

| Line | What it does |
|---|---|
| first loop | create every copy (only values so far) and remember original → copy |
| `copyOf.get(c).next = copyOf.get(c.next);` | the copy's next = the copy of the original's next |
| `copyOf.get(c).random = copyOf.get(c.random);` | the copy's random = the copy of the original's random target |
| `return copyOf.get(head);` | the copy of the head (null for an empty list, since `get(null)` is null) |

### Dry run: `7 → 13 → 11 → 10 → 1`

**Pass 1:** map = {7 → 7', 13 → 13', 11 → 11', 10 → 10', 1 → 1'}

**Pass 2:**

| original | next → copy | random → copy |
|---|---|---|
| 7 | 13' | null |
| 13 | 11' | 7' |
| 11 | 10' | 1' |
| 10 | 1' | 11' |
| 1 | null | 7' |

The copy has the same structure, and every pointer goes to a primed (new) node ✓

**O(n) time, O(n) space.**

---

## 5. Approach 2: Interleaving (the file's second method)

### Code ([M05_CopyListWithRandomPointer.java:55-72](M05_CopyListWithRandomPointer.java#L55-L72))

```java
static Node copyRandomListInterleave(Node head) {
    if (head == null) return null;
    for (Node c = head; c != null; c = c.next.next) {          // 1. A -> A' -> B -> B'
        Node copy = new Node(c.val);
        copy.next = c.next;
        c.next = copy;
    }
    for (Node c = head; c != null; c = c.next.next) {          // 2. randoms
        c.next.random = (c.random == null) ? null : c.random.next;
    }
    Node copyHead = head.next;
    for (Node c = head; c != null; c = c.next) {               // 3. unweave
        Node copy = c.next;
        c.next = copy.next;
        copy.next = (copy.next == null) ? null : copy.next.next;
    }
    return copyHead;
}
```

### Line by line

| Line | What it does |
|---|---|
| **phase 1** `copy.next = c.next; c.next = copy;` | insert the copy right after c. The update `c = c.next.next` jumps over the copy to the next original |
| **phase 2** `c.next.random = c.random == null ? null : c.random.next;` | c.next is c's copy; c.random.next is the copy of c's random target |
| `Node copyHead = head.next;` | the first copy |
| **phase 3** `c.next = copy.next;` | restore the original's next (skip its copy) |
| `copy.next = copy.next == null ? null : copy.next.next;` | the copy's next = the next copy (skip the next original) |

### Dry run: `A(7) → B(13) → C(11)`, with random: A→null, B→A, C→A (a smaller example)

**Phase 1** (interleave): `A → A' → B → B' → C → C'`

**Phase 2** (randoms):

| original | original's random | copy's random = random.next |
|---|---|---|
| A | null | null |
| B | A | A.next = **A'** |
| C | A | **A'** |

**Phase 3** (unweave):

| c | copy | restore c.next | copy.next |
|---|---|---|---|
| A | A' | A → B | A' → B' |
| B | B' | B → C | B' → C' |
| C | C' | C → null | C' → null |

Result: the original `A → B → C` is restored, and the copy is `A' → B' → C'` with randoms B'→A', C'→A' ✓

### Complexity
- **Time O(n):** three passes.
- **Space O(1) extra** (the new nodes are the output itself).

---

## 6. (Bonus) Approach 3: Recursive with memoisation

```java
static Map<Node, Node> memo = new HashMap<>();
static Node copyRec(Node node) {
    if (node == null) return null;
    if (memo.containsKey(node)) return memo.get(node);
    Node copy = new Node(node.val);
    memo.put(node, copy);                     // register BEFORE recursing (the random pointers may loop back)
    copy.next = copyRec(node.next);
    copy.random = copyRec(node.random);
    return copy;
}
```

It's the same idea as Clone Graph ([09-Graph M02](../../09-Graph/03-Medium/M02_CloneGraph.md)): a DFS with a visited/copy map. **O(n) space**, plus the recursion depth.

---

## 7. Comparing the approaches

| | HashMap | Interleave | Recursive + memo |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) | **O(1)** | O(n) + stack |
| Modifies the original temporarily | no | yes (restored) | no |
| Generalises to graphs | **yes** | no | **yes** |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty list, random pointing to itself, all randoms null, random pointing forward or backward.

**Mistakes:**
1. Copying `random` straight from the original (`copy.random = orig.random`), which points into the original list: a shallow copy.
2. Interleave: not restoring the original's `next` pointers.
3. Interleave: forgetting the null check for `copy.next.next` at the tail.
4. Recursive: putting into the memo **after** recursing, which leads to infinite recursion on cycles of randoms.

---

## 9. Interview questions and answers

**Q1. What does the HashMap solve?**
> It maps every original node to its copy, so any pointer in the original (next or random) can be translated to the corresponding pointer in the copy in O(1).

**Q2. How does interleaving avoid the map?**
> By placing each copy right after its original, "the copy of X" is always `X.next`, so the list structure itself acts as the map.

**Q3. Which would you pick in production?**
> The HashMap version: it's simpler and doesn't mutate the input (important if other threads might read the list).

### Follow-up questions

**F1. Clone Graph (LC 133).** → [09-Graph M02](../../09-Graph/03-Medium/M02_CloneGraph.md)
> BFS/DFS with an original → copy map.

**F2. Clone a binary tree with random pointers (LC 1485).**
> A map from original → copy plus a tree traversal.

**F3. Serialise a list with random pointers.**
> Assign each node an index, then store `[value, randomIndex]` pairs (exactly like this file's test helpers).

---

## 10. Running the file

```bash
java M05_CopyListWithRandomPointer.java      # 6/6 tests passed
```

**One-line summary:** *"Map every original to its copy and translate next/random through the map; or interleave the copies so X's copy is X.next."*
