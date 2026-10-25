# E04 · Intersection of Two Linked Lists

| | |
|---|---|
| **LeetCode** | [#160 Intersection of Two Linked Lists](https://leetcode.com/problems/intersection-of-two-linked-lists/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers that switch lists (to equalise the path lengths) |
| **Code** | [E04_IntersectionOfTwoLinkedLists.java](E04_IntersectionOfTwoLinkedLists.java) |
| **Run** | `java E04_IntersectionOfTwoLinkedLists.java` |

---

## 1. The problem

Two lists may **merge** at some node and share their tail from there on (a Y shape). Return that first shared **node**, or `null`.

```
A:      a1 → a2 ↘
                  c1 → c2 → c3
B: b1 → b2 → b3 ↗                 → return c1
```

"Shared" means the **same node objects**, not just equal values. Lists that merely have equal values do not intersect.

**Constraints:** `1 ≤ m, n ≤ 3 × 10⁴`, no cycles; the lists must keep their structure. Follow-up: *O(m + n) time and O(1) memory*.

---

## 2. How to think about it

The difficulty is that the parts **before** the intersection have different lengths, so two pointers walking in step don't arrive at c1 at the same time.

1. **HashSet:** store every node of A, then walk B until a node is in the set. O(n) space.
2. **Length difference:** compute both lengths, advance the longer list's pointer by the difference, then walk both together until they're equal. O(1) space.
3. **Switching pointers (elegant):** `pA` walks A and then continues on B; `pB` walks B and then continues on A. Both travel **lenA + lenB** steps in total, so they line up:

```
pA: a-part + shared + b-part → arrives at c1
pB: b-part + shared + a-part → arrives at c1
```

Each pointer covers (a-part + b-part + shared) before it reaches c1 the second time, the **same distance**, so they reach it at the same moment. With no intersection, both reach **null** at the same time (after lenA + lenB steps), and the loop ends with null.

---

## 3. Java concepts used in this problem

### 3.1 `==` on nodes: identity

`while (pA != pB)` compares **references**. The intersection is where both point to the **same node object**. Value equality isn't enough (see [E03](E03_LinkedListCycle.md)).

### 3.2 Building lists that share a tail (in the tests)

```java
ListNode shared = build(8, 4, 5);
ListNode a = build(4, 1); a.next.next = shared;   // a: 4 → 1 → [8 → 4 → 5]
ListNode b = build(5, 6, 1); b.next.next.next = shared;
```

Setting the last node's `next` to the **same** `shared` object creates a real intersection. The test then checks `result == shared` (identity), not the printed values.

### 3.3 Ternaries that switch lists

```java
pA = (pA == null) ? headB : pA.next;
```

When a pointer falls off the end of its list (becomes null), it jumps to the **other** list's head. That one "null step" is part of the equal-length argument.

---

## 4. Approach 1: HashSet of A's nodes

```java
static ListNode intersectionSet(ListNode headA, ListNode headB) {
    Set<ListNode> seen = new HashSet<>();
    for (ListNode c = headA; c != null; c = c.next) seen.add(c);
    for (ListNode c = headB; c != null; c = c.next) if (seen.contains(c)) return c;
    return null;
}
```

The first node of B that's also in A is the intersection. **O(m + n) time, O(m) space.**

---

## 5. Approach 2: Equalise by the length difference

```java
static ListNode intersectionByLength(ListNode headA, ListNode headB) {
    int lenA = length(headA), lenB = length(headB);
    ListNode a = headA, b = headB;
    while (lenA > lenB) { a = a.next; lenA--; }      // skip A's extra prefix
    while (lenB > lenA) { b = b.next; lenB--; }      // or B's
    while (a != b) { a = a.next; b = b.next; }       // now the same distance from the end
    return a;                                         // the intersection, or null
}
static int length(ListNode h) {
    int n = 0;
    for (ListNode c = h; c != null; c = c.next) n++;
    return n;
}
```

After skipping the extra nodes, both pointers are the **same distance from the end**, so they reach the shared part together. **O(m + n) time, O(1) space.** Very clear; it's a great answer too.

---

## 6. Approach 3: Switching pointers (the file's solution)

### Code ([E04_IntersectionOfTwoLinkedLists.java:43-50](E04_IntersectionOfTwoLinkedLists.java#L43-L50))

```java
static ListNode getIntersectionNode(ListNode headA, ListNode headB) {
    ListNode pA = headA, pB = headB;
    while (pA != pB) {                                   // compares NODES
        pA = (pA == null) ? headB : pA.next;            // switch to the other list at the end
        pB = (pB == null) ? headA : pB.next;
    }
    return pA;                                           // the intersection, or null
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode pA = headA, pB = headB;` | one pointer per list |
| `while (pA != pB)` | until they're on the same node (or both null) |
| `pA = (pA == null) ? headB : pA.next;` | step forward; after A's end, continue at B's head |
| `pB = (pB == null) ? headA : pB.next;` | the same, the other way round |
| `return pA;` | the shared node, or null if there's none |

### Dry run 1: `A = 4 → 1 → [8 → 4 → 5]`, `B = 5 → 6 → 1 → [8 → 4 → 5]`

(The values in brackets are the shared nodes. lenA = 5, lenB = 6.)

| step | pA | pB |
|---|---|---|
| 0 | 4 (A) | 5 (B) |
| 1 | 1 | 6 |
| 2 | **8** | 1 (B) |
| 3 | 4 | **8** |
| 4 | 5 | 4 |
| 5 | null | 5 |
| 6 | 5 (B's head) | null |
| 7 | 6 | 4 (A's head) |
| 8 | 1 (B) | 1 (A) |
| 9 | **8** | **8** ← the same node → stop |

**Return the shared 8** ✓. Both travelled 9 steps: pA = 5 (A) + 1 (null) + 3 (B's own part); pB = 6 (B) + 1 (null) + 2 (A's own part).

### Dry run 2: no intersection, `A = 2 → 6 → 4`, `B = 1 → 5`
pA: 2, 6, 4, null, 1, 5, **null**; pB: 1, 5, null, 2, 6, 4, **null**. After 6 steps both are null → `pA == pB` → **return null** ✓

### Complexity
- **Time O(m + n):** each pointer walks at most m + n + 1 steps.
- **Space O(1).**

---

## 7. Comparing the approaches

| | HashSet | Length difference | Switching pointers |
|---|---|---|---|
| Time | O(m + n) | O(m + n) | O(m + n) |
| Space | O(m) | **O(1)** | **O(1)** |
| Code | simple | clear | shortest |

---

## 8. Edge cases and common mistakes

**Edge cases:** no intersection, the intersection at the first node (the same list), one list much longer, the intersection at the last node.

**Mistakes:**
1. Comparing values instead of node references.
2. Switching to the other list's **next** instead of its head, or skipping the null step. That desynchronises the pointers when there's no intersection and can loop forever.
3. Modifying the lists (e.g. temporarily making a cycle) and forgetting to restore them.

---

## 9. Interview questions and answers

**Q1. Why do the switching pointers meet at the intersection?**
> Both walk (A's own part + B's own part + the shared part) before reaching the shared start a second time, the same total distance, so they arrive at the same moment.

**Q2. Why doesn't it loop forever when there's no intersection?**
> After lenA + lenB steps both pointers are null at the same time, and `null == null` ends the loop.

**Q3. Could the lists have cycles?**
> Not in this problem. With cycles you'd need Floyd's algorithm first, to find each list's cycle entry, and then reason about whether the cycles are the same.

### Follow-up questions

**F1. Return the length of the shared part.**
> Walk from the intersection to the end, counting.

**F2. Lowest Common Ancestor with parent pointers (LC 1650).**
> It's the same problem: each node's path to the root is a list, and the LCA is where the two paths intersect. Use switching pointers.

**F3. Intersection of two **arrays**.** → [Arrays E11](../../01-Arrays/02-Easy/E11_IntersectionOfTwoArrays.md)
> A different meaning (common values), solved with hashing.

---

## 10. Running the file

```bash
java E04_IntersectionOfTwoLinkedLists.java      # 4/4 tests passed
```

**One-line summary:** *"Each pointer walks its own list and then the other one; after lenA + lenB steps they meet at the shared node (or both at null)."*
