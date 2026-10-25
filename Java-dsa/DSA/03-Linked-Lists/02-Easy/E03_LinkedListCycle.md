# E03 · Linked List Cycle

| | |
|---|---|
| **LeetCode** | [#141 Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) |
| **Difficulty** | Easy |
| **Pattern** | Fast & slow pointers (Floyd's cycle detection) |
| **Code** | [E03_LinkedListCycle.java](E03_LinkedListCycle.java) |
| **Run** | `java E03_LinkedListCycle.java` |

---

## 1. The problem

Does the list contain a **cycle**? That is, does some node's `next` point back to an earlier node, so that following `next` never reaches `null`?

```
3 → 2 → 0 → -4
    ↑        |
    └────────┘        → true
```

| list | answer |
|---|---|
| `3, 2, 0, -4`, the tail points to index 1 | true |
| `1, 2`, the tail points to index 0 | true |
| `1` (no cycle) | false |
| empty | false |
| a single node pointing to itself | true |

**Constraints:** `0 ≤ n ≤ 10⁴`. Follow-up: *O(1) memory?*

---

## 2. How to think about it

1. **Remember the visited nodes:** walk the list; if you ever reach a node you've already seen, there's a cycle. O(n) space.
2. **Floyd's tortoise and hare (O(1) space):** `slow` moves 1 step, `fast` moves 2 steps.
   - **No cycle:** `fast` reaches `null` → false.
   - **Cycle:** both pointers eventually enter the loop. Inside it, `fast` gains **one node** on `slow` with every step, so the gap shrinks by 1 each step and they **must meet**.

It's like two runners on a circular track: the faster one eventually laps the slower one. It can't "jump over" it, because it gains exactly one position per step.

---

## 3. Java concepts used in this problem

### 3.1 Comparing references with `==`

```java
if (slow == fast) return true;
```

For objects, `==` checks whether two variables point to the **same object**. That's exactly what we want here: "are the pointers on the same node?". Comparing **values** (`slow.val == fast.val`) would be wrong, because different nodes can hold the same value.

### 3.2 A `HashSet` of nodes (identity-based)

```java
Set<ListNode> seen = new HashSet<>();
```

`ListNode` doesn't override `equals`/`hashCode`, so it inherits `Object`'s versions, which are based on **identity**: two different node objects are never "equal", even with the same value. That's exactly right for "have I visited this node?". (If a class overrides `equals` by content, a HashSet would treat equal-content nodes as the same, which would be a bug here.)

### 3.3 The double null check for `fast`

```java
while (fast != null && fast.next != null) {
    fast = fast.next.next;
```

`fast.next.next` requires **both** `fast` and `fast.next` to be non-null. Short-circuit `&&` checks them in order, preventing an NPE.

### 3.4 The test helper `withCycle(pos, vals...)`

It builds a normal list and then points the last node at the node with index `pos` (−1 means no cycle). Printing a cyclic list would loop forever, which is why `ListNode.toString` has a 1000-node guard (see [E01](E01_ReverseLinkedList.md)).

---

## 4. Approach 1: HashSet of visited nodes (the file's second method)

### Code ([E03_LinkedListCycle.java:48-54](E03_LinkedListCycle.java#L48-L54))

```java
static boolean hasCycleWithSet(ListNode head) {
    Set<ListNode> seen = new HashSet<>();          // identity-based (ListNode doesn't override equals)
    for (ListNode c = head; c != null; c = c.next) {
        if (!seen.add(c)) return true;
    }
    return false;
}
```

| Line | What it does |
|---|---|
| `for (ListNode c = head; c != null; c = c.next)` | walk the list; with no cycle, it reaches null and stops |
| `if (!seen.add(c)) return true;` | `add` returns false if the node was already there, so we've come back to it: a cycle |
| `return false;` | reached the end: no cycle |

**Dry run** `3 → 2 → 0 → -4 → (back to 2)`: add 3, 2, 0, −4; the next node is 2 again → `add` fails → **true**.

**O(n) time, O(n) space.**

---

## 5. Approach 2: Floyd's fast & slow pointers (the file's main solution)

### Code ([E03_LinkedListCycle.java:38-46](E03_LinkedListCycle.java#L38-L46))

```java
static boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {   // fast checks two steps ahead
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) return true;
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode slow = head, fast = head;` | both start at the head |
| `while (fast != null && fast.next != null)` | fast can safely take two steps; if not, the list ends, so there's no cycle |
| `slow = slow.next;` | 1 step |
| `fast = fast.next.next;` | 2 steps |
| `if (slow == fast) return true;` | they're on the same node, which is only possible in a cycle. (Checking **after** moving avoids the trivial match at the start) |
| `return false;` | fast fell off the end |

### Dry run 1: `3 → 2 → 0 → -4 → (back to 2)`

| step | slow | fast | same node? |
|---|---|---|---|
| start | 3 | 3 | (not checked) |
| 1 | 2 | 0 | no |
| 2 | 0 | 2 (0 → −4 → 2) | no |
| 3 | −4 | −4 (2 → 0 → −4) | **yes → true** |

### Dry run 2: `1 → null` (no cycle)
`fast = 1`, `fast.next = null` → the loop doesn't run → **false** ✓

### Dry run 3: a single node pointing to itself
slow = 1.next = 1, fast = 1.next.next = 1 → equal → **true** ✓

### Why must they meet in a cycle? (the proof)
When `slow` enters the cycle, `fast` is already somewhere in it, some number d of steps **ahead** (going around). Each step, slow moves 1 and fast moves 2, so the distance from fast back to slow along the cycle **shrinks by 1**. It goes d, d − 1, …, 0, so they meet within one lap (at most the cycle length).

### Complexity
- **Time O(n):** slow walks at most the tail plus one lap.
- **Space O(1).**

---

## 6. Comparing the approaches

| | HashSet | Floyd |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(n) | **O(1)** |
| Finds the cycle start too | yes (the first repeated node) | yes, with a second phase (M01) |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty list, one node without a cycle, one node pointing to itself, a cycle back to the head, a long tail before the cycle.

**Mistakes:**
1. Checking `slow == fast` **before** moving (they're equal at the start, which gives a false positive).
2. Only checking `fast != null`, which crashes on `fast.next.next` when `fast.next` is null.
3. Comparing values instead of nodes.
4. Marking visited nodes by changing their values, which destroys the input.

---

## 8. Interview questions and answers

**Q1. Why does fast move exactly 2 steps?**
> Any speed difference works in principle, but with a difference of 1 the gap shrinks by exactly one node each step, so fast can never skip over slow. With larger jumps they could miss each other on some cycle lengths.

**Q2. What's the time complexity, more precisely?**
> If the tail before the cycle has μ nodes and the cycle has λ nodes, slow enters after μ steps and they meet within λ more steps: O(μ + λ) = O(n).

**Q3. How do you find where the cycle starts?** → [M01](../03-Medium/M01_LinkedListCycleII.md)
> After they meet, move one pointer back to the head; then advance both by 1 step. They meet at the cycle's start (the distance argument: the head and the meeting point are the same number of steps from the entry).

### Follow-up questions

**F1. The length of the cycle.**
> After meeting, keep one pointer fixed and walk the other around, counting steps until it comes back.

**F2. Find the duplicate number (LC 287).**
> Treat `i → nums[i]` as a linked list; the duplicate is the entry point of the cycle (Floyd + phase 2).

**F3. Happy Number (LC 202).** → [06-Hash-Table E01](../../06-Hash-Table/02-Easy/E01_HappyNumber.md)
> The sequence n → sum of the squares of its digits either reaches 1 or cycles. Floyd detects the cycle without a set.

**F4. Remove the cycle.**
> Find the start (M01), then walk around the cycle to the node whose next is the start, and set its next to null.

---

## 9. Running the file

```bash
java E03_LinkedListCycle.java      # 6/6 tests passed
```

**One-line summary:** *"Slow 1 step, fast 2 steps: they meet if there's a cycle; fast hits null if there isn't."*
