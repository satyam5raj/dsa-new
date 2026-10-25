# H02 · Design Skiplist ⭐

| | |
|---|---|
| **LeetCode** | [#1206 Design Skiplist](https://leetcode.com/problems/design-skiplist/) |
| **Difficulty** | Hard |
| **Pattern** | A skip list **with duplicates** (the version in 01-Basics was a set) |
| **Code** | [H02_DesignSkiplist.java](H02_DesignSkiplist.java) |
| **Run** | `java H02_DesignSkiplist.java` |

⭐ = not in `leetcode.md`, added for a key technique. The full skip-list walkthrough (levels, search cost, the `update[]` array) is in [01-Basics/SkipListBasics.java](../01-Basics/SkipListBasics.java).

---

## 1. The problem

Implement `search(target)`, `add(num)` and `erase(num)` without library structures, each in expected O(log n). **Duplicates are allowed**: `erase` removes **one** copy and returns whether anything was erased.

| call | result |
|---|---|
| add 1, add 2, add 3 | |
| search(0) | false |
| add 4 | |
| search(1) | true |
| erase(0) | false |
| erase(1) | true |
| search(1) | false |

---

## 2. How to think about it

### A skip list in one picture
Level 0 is the full sorted linked list. Each node is also promoted to level 1 with probability ½, to level 2 with probability ¼, and so on. The higher levels are "express lanes":

```
L3: H ──────────────→ 1 ──────────────────→ 4
L2: H ──────────────→ 1 ──────────────────→ 4
L1: H ──────────────→ 1 ──────────────────→ 4
L0: H ──────────────→ 1 ──→ 2 ──→ 3 ──→ 4
```

**Search:** start at the top level of the head; move **right** while the next value is < target, otherwise drop **down** a level. At level 0, the next node is the first one ≥ target. With about log₂ n levels and an expected two steps per level, that's expected O(log n).

### Differences from the set version
- **add:** don't reject an existing value; just insert another node. The search stops **before** the first node ≥ num, so the new copy goes in front of the existing copies. That's fine: the order among equal values doesn't matter.
- **erase:** walk to the **first** node with value == num, and unlink exactly that node on every level it occupies.

### The careful part of `erase`
On level i, unlink only if `update[i].next[i]` **is** the target node. If the target doesn't reach level i, a different copy of the same value might sit there instead, and it must not be removed. Since a node's levels are contiguous (0 up to its height − 1), the first level where the check fails means all higher levels fail too, so the loop can `break`.

### Dry run: the example (real heights from the file's fixed random seed)

After add 1, 2, 3, 4 (node 1 got height 4, node 4 got height 5, nodes 2 and 3 got height 1):

```
L4: H → 4
L3: H → 1 → 4
L2: H → 1 → 4
L1: H → 1 → 4
L0: H → 1 → 2 → 3 → 4
```

**search(3):** L4: 4 < 3? no, drop. L3: 1 < 3 → move to 1; 4 < 3? no, drop. L2, L1: 4 < 3? no, drop. L0: 2 < 3 → move to 2; 3 < 3? no. The next node is 3 → **true**.

**erase(1):** `update[i]` is the head on every level, and the target is node 1. Unlink it on L0–L3. On L4, `head.next[4]` is node 4, not the target → stop.

```
L4: H → 4
L3: H → 4   ...   L0: H → 2 → 3 → 4
```

### Dry run: duplicates
`add(5)` three times: the first copy got height 4 and the next two got height 1. Each new copy is inserted **in front** on level 0:

```
L3..L1: H → 5(tall)
L0:     H → 5(c) → 5(b) → 5(tall)
```

`erase(5)`: the target is the first copy at level 0, 5(c), with height 1. Unlink it on L0. On L1, `head.next[1]` is 5(tall), which is **not** the target → `break`. Without that check, the tall copy would be wrongly unlinked from L1–L3. The result is [5, 5] ✓.

---

## 3. Java concepts used in this problem

### 3.1 A node with an array of next pointers

```java
private static class Node {
    int val;
    Node[] next;                                    // next[i] = the next node on level i
    Node(int val, int levels) { this.val = val; next = new Node[levels]; }
}
```

The node's height is the length of its `next` array. A new `Node[]` is filled with `null`, meaning "nothing after me on this level".

### 3.2 A head sentinel with every level

`private final Node head = new Node(-1, MAX);` has all 16 levels, so every search can start at the head on any level. Its value is never compared.

### 3.3 `private static final int MAX = 16`

A class constant: `static` (one copy, shared), `final` (never changes). Sixteen levels comfortably cover 2¹⁶ = 65,536 elements and much more in practice.

### 3.4 A seeded `Random` for the coin flips

```java
class Coins {
    static final int MAX = 16;
    private final Random rnd = new Random(7);

    int height() {
        int h = 1;
        while (h < MAX && rnd.nextBoolean()) h++;      // keep flipping heads
        return h;
    }
}
```

`new Random(7)` gives the same sequence every run, so the test results (and the dry run above) are reproducible. `nextBoolean()` is a fair coin: the height is h with probability ½ʰ.

### 3.5 The `update[]` array

`update[i]` is the last node visited on level i: the node **before** the insertion or deletion point on that level. Splicing at level i then only touches `update[i].next[i]`.

---

## 4. Approach 1: A `TreeMap` multiset (the referee)

The test keeps `TreeMap<Integer, Integer> counts` (value → number of copies) ([H02_DesignSkiplist.java:110-126](H02_DesignSkiplist.java#L110-L126)):
- add: `counts.merge(k, 1, Integer::sum)`;
- erase: succeeds iff the key is present; decrement, and remove at 0;
- search: `containsKey`.

At the end, the skip list's level 0 must equal every key repeated by its count, in sorted order. `merge(k, 1, Integer::sum)` means "put 1 if absent, otherwise add 1 to the current value".

- **O(log n)** per operation (a red-black tree).

---

## 5. Approach 2: Skip list with duplicates (the file's main solution)

### Code ([H02_DesignSkiplist.java:36-92](H02_DesignSkiplist.java#L36-L92))

```java
static class Skiplist {
    private static final int MAX = 16;
    private static class Node {
        int val;
        Node[] next;
        Node(int val, int levels) { this.val = val; next = new Node[levels]; }
    }

    private final Node head = new Node(-1, MAX);
    private int level = 1;
    private final Random rnd = new Random(7);

    public boolean search(int target) {
        Node x = head;
        for (int i = level - 1; i >= 0; i--) while (x.next[i] != null && x.next[i].val < target) x = x.next[i];
        x = x.next[0];
        return x != null && x.val == target;
    }

    public void add(int num) {
        Node[] update = new Node[MAX];
        Node x = head;
        for (int i = level - 1; i >= 0; i--) {
            while (x.next[i] != null && x.next[i].val < num) x = x.next[i];
            update[i] = x;
        }
        int h = 1;
        while (h < MAX && rnd.nextBoolean()) h++;
        for (int i = level; i < h; i++) update[i] = head;
        level = Math.max(level, h);
        Node n = new Node(num, h);
        for (int i = 0; i < h; i++) { n.next[i] = update[i].next[i]; update[i].next[i] = n; }
    }

    public boolean erase(int num) {
        Node[] update = new Node[MAX];
        Node x = head;
        for (int i = level - 1; i >= 0; i--) {
            while (x.next[i] != null && x.next[i].val < num) x = x.next[i];
            update[i] = x;
        }
        Node target = x.next[0];
        if (target == null || target.val != num) return false;
        for (int i = 0; i < level; i++) {
            if (update[i].next[i] != target) break;          // target doesn't reach this level
            update[i].next[i] = target.next[i];
        }
        while (level > 1 && head.next[level - 1] == null) level--;
        return true;
    }

    List<Integer> toList() {
        List<Integer> out = new ArrayList<>();
        for (Node n = head.next[0]; n != null; n = n.next[0]) out.add(n.val);
        return out;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `level` | how many levels are currently in use (searches start at `level − 1`) |
| `search`: the loop | right while the next value is smaller, then down |
| `search`: `x.next[0]` | the first node ≥ target; is it equal? |
| `add`: `update[i] = x` | remember the predecessor on each level |
| `add`: the coin flips | choose the height h |
| `add`: `update[i] = head` for new levels | above the old top level, the predecessor is the head |
| `add`: the splice | on each of the h levels, insert after `update[i]` |
| `erase`: `target` | the first node with this value, or a miss |
| `erase`: the unlink loop | skip over the target on each level it occupies; stop at the first level it doesn't |
| `erase`: `level--` | shrink the level count if the top levels are now empty |
| `toList` | walk level 0 (used by the tests) |

### Complexity
- **Expected O(log n)** for each of search, add and erase.
- **Space O(n) expected**: about 2 pointers per node on average (1 + ½ + ¼ + …).

---

## 6. Comparing the approaches

| | TreeMap multiset | Skip list |
|---|---|---|
| Time | O(log n) worst case | O(log n) expected |
| Balancing | rotations (complex) | coin flips (simple) |
| Concurrency | hard to lock finely | easier: Java's `ConcurrentSkipListMap` uses one |

The file runs 100,000 random operations against the `TreeMap` multiset and also compares the full contents at the end.

---

## 7. Edge cases and common mistakes

**Edge cases:** erasing from an empty list, erasing a value that isn't there, many copies of one value, erasing the tallest node (the level count shrinks).

**Mistakes:**
1. Rejecting duplicates in `add` (copied from the set version).
2. Unlinking on every level without the `update[i].next[i] == target` check (that removes a different copy).
3. Forgetting `update[i] = head` for levels above the current `level`.
4. Moving right while `<=` instead of `<` (then `x.next[0]` is past the target).

---

## 8. Interview questions and answers

**Q1. Why is a skip list O(log n) expected?**
> Each level has about half the nodes of the one below, so there are about log₂ n levels, and on each level the search moves right an expected constant number of times before dropping down.

**Q2. Why use a skip list over a balanced BST?**
> It's far simpler to implement (no rotations), supports ordered iteration like a BST, and is easier to make concurrent. Redis sorted sets and Java's `ConcurrentSkipListMap` use them.

**Q3. Where does a new duplicate go, and does it matter?**
> In front of the existing copies, because the search stops before the first node ≥ num. Order among equal values doesn't matter for search or erase.

### Follow-up questions

**F1. Find the k-th smallest value.**
> Store a "span" on each pointer (how many level-0 nodes it skips), as Redis does; then walk while the accumulated span is < k.

**F2. Range query [a, b].**
> Search for a, then walk level 0 until the values exceed b.

**F3. Why not always use height log n?**
> Fixed heights need rebalancing on every insert. Random heights keep the structure balanced in expectation with no rebalancing at all.

---

## 9. Running the file

```bash
java H02_DesignSkiplist.java      # 7/7 tests passed (including duplicates and 100,000 random ops vs a TreeMap multiset)
```

**One-line summary:** *"A skip list where add always inserts a new node (in front of equal ones) and erase unlinks the first node with that value only on the levels where it really is the successor."*
