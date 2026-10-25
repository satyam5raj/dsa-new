# M01 · Linked List Cycle II

| | |
|---|---|
| **LeetCode** | [#142 Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Floyd's algorithm, phase 2 (find the cycle's **entry** node) |
| **Code** | [M01_LinkedListCycleII.java](M01_LinkedListCycleII.java) |
| **Run** | `java M01_LinkedListCycleII.java` |

---

## 1. The problem

Return the node where the cycle **begins**, or `null` if there's no cycle. Follow-up: **O(1) memory**.

```
3 → 2 → 0 → -4
    ↑        |
    └────────┘        → return the node with value 2 (index 1)
```

| list (and where the tail points) | answer |
|---|---|
| `3, 2, 0, -4` → index 1 | node at index 1 |
| `1, 2` → index 0 | node at index 0 |
| `1, 2, 3`, no cycle | null |

**Constraints:** `0 ≤ n ≤ 10⁴`. You must not modify the list.

---

## 2. How to think about it

1. **HashSet:** the first node you visit twice is the entry. O(n) space.
2. **Floyd, in two phases (O(1) space):**
   - **Phase 1:** slow (1 step) and fast (2 steps) until they meet **inside** the cycle ([E03](../02-Easy/E03_LinkedListCycle.md)).
   - **Phase 2:** put one pointer back at the **head** and keep the other at the **meeting point**. Move **both one step at a time**. Where they meet is the **cycle entry**.

### Why phase 2 works (the proof interviewers ask for)

Let:
- **a** = the distance from the head to the entry
- **b** = the distance from the entry to the meeting point (along the cycle)
- **c** = the cycle length

```
head ──a──► entry ──b──► meet
              ▲           │
              └── c − b ──┘
```

When they meet, slow has walked **a + b**, and fast has walked **a + b + k·c** (k whole extra laps). Fast walks twice as far:

```
2(a + b) = a + b + k·c   ⟹   a + b = k·c   ⟹   a = k·c − b = (c − b) + (k − 1)·c
```

**`(c − b) + (k−1)·c`** is exactly the distance from the **meeting point forward to the entry** (finish the current lap, c − b, plus some whole laps). So a walker starting at the head and a walker starting at the meeting point, both at speed 1, reach the entry **after exactly `a` steps, at the same time**.

---

## 3. Java concepts used in this problem

### 3.1 Reusing a pointer variable

After phase 1, `slow` is reused as the "meeting point walker" in phase 2. That's fine, as long as you're clear about what each variable means at each stage.

### 3.2 `do { } while (cond);` (in the bonus)

```java
do {
    slow = nums[slow];
    fast = nums[nums[fast]];
} while (slow != fast);
```

The body runs **at least once** before the check. That's needed because slow and fast **start equal**; a normal `while (slow != fast)` would never run.

### 3.3 `ok &= condition`

`ok &= x` is `ok = ok & x`: once any check fails, `ok` stays false. It's used in the exhaustive test (every n ≤ 30, every entry position).

### 3.4 `indexOf(head, node)` (test helper)

It converts a node back into its position, so the test can check "the entry is at index 1". It has a 10,000-step guard, because the list has a cycle.

---

## 4. Approach 1: HashSet

```java
static ListNode detectCycleSet(ListNode head) {
    Set<ListNode> seen = new HashSet<>();
    for (ListNode c = head; c != null; c = c.next) {
        if (!seen.add(c)) return c;         // the first node seen twice = the entry
    }
    return null;
}
```

The walk enters the cycle at the entry, goes around, and comes back to the entry first. **O(n) time, O(n) space.**

---

## 5. Approach 2: Floyd's two phases (the file's solution)

### Code ([M01_LinkedListCycleII.java:42-57](M01_LinkedListCycleII.java#L42-L57))

```java
static ListNode detectCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) {                       // phase 1 done: inside the cycle
            ListNode p = head;
            while (p != slow) {                   // phase 2: both at speed 1
                p = p.next;
                slow = slow.next;
            }
            return p;                             // the entry
        }
    }
    return null;                                  // fast hit the end: no cycle
}
```

### Line by line

| Line | What it does |
|---|---|
| phase 1 loop | the standard cycle detection |
| `if (slow == fast)` | they met inside the cycle |
| `ListNode p = head;` | the second walker starts at the head |
| `while (p != slow) { p = p.next; slow = slow.next; }` | both at speed 1; by the proof, they meet at the entry |
| `return p;` | the entry node |
| `return null;` | fast reached the end: no cycle |

### Dry run: `3 → 2 → 0 → -4 → (back to 2)`

Here a = 1 (3 → 2), the cycle is [2, 0, −4], so c = 3.

**Phase 1** (from E03): they meet at **−4**. So b = 2 (2 → 0 → −4), and indeed a + b = 3 = 1·c ✓.

**Phase 2:**

| step | p (from the head) | slow (from the meeting point) |
|---|---|---|
| start | 3 | −4 |
| 1 | **2** | **2** (−4 → 2) |

They meet at **2**, the entry ✓

### Dry run 2: `1 → 2 → (back to 1)` (the cycle starts at the head, a = 0)
Phase 1: slow = 2, fast = 1 → slow = 1, fast = 1 → meet at 1. Phase 2: p = head = 1 = slow → **return 1** immediately ✓

### Complexity
- **Time O(n):** phase 1 is O(a + c), phase 2 is O(a).
- **Space O(1).**

The file tests **every** list length up to 30 with **every** entry position: 465 cases, all correct.

---

## 6. Bonus: Find the Duplicate Number (LC 287) with the same algorithm

`nums` has n + 1 values in `[1, n]`, so some value repeats. Treat each index i as a node whose "next" is `nums[i]`. Two indices point to the duplicate value, so the duplicate is the **entry of a cycle**.

### Code ([M01_LinkedListCycleII.java:61-73](M01_LinkedListCycleII.java#L61-L73))

```java
static int findDuplicate(int[] nums) {
    int slow = nums[0], fast = nums[0];
    do {
        slow = nums[slow];
        fast = nums[nums[fast]];
    } while (slow != fast);
    int p = nums[0];
    while (p != slow) {
        p = nums[p];
        slow = nums[slow];
    }
    return p;
}
```

**Dry run** `[1, 3, 4, 2, 2]` (the "links": 0 → 1 → 3 → 2 → 4 → 2 …, so the cycle is 2 → 4 → 2):

| phase | slow | fast |
|---|---|---|
| start | 1 | 1 |
| 1 | 3 | 2 |
| 2 | 2 | 2 → meet |
| phase 2 | p = 1, slow = 2 | |
| | p = 3, slow = 4 | |
| | p = 2, slow = 2 → **duplicate 2** ✓ | |

**O(n) time, O(1) space, without modifying the array.** This is the classic surprising use of Floyd.

---

## 7. Comparing the approaches

| | HashSet | Floyd's two phases |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(n) | **O(1)** |
| Modifies the list | no | no |

---

## 8. Edge cases and common mistakes

**Edge cases:** no cycle, a cycle at the head, a self-loop on a single node, a long tail before a small cycle.

**Mistakes:**
1. Starting phase 2 with **both** pointers at the head (one must stay at the meeting point).
2. Moving fast 2 steps in phase 2 (both must move 1).
3. Returning the meeting point (it's generally **not** the entry).
4. In the array version, using `while` instead of `do-while` (the loop never runs, because slow == fast at the start).

---

## 9. Interview questions and answers

**Q1. Prove that phase 2 finds the entry.**
> At the meeting, `2(a + b) = a + b + kc`, so `a = kc − b`. Walking a steps from the meeting point is the same as walking `kc − b` steps, which finishes the lap back to the entry. A pointer from the head reaches the entry after a steps too, so they meet there.

**Q2. How do you get the cycle length?**
> From the meeting point, walk around until you return, counting the steps.

**Q3. Why does it work for "find the duplicate number"?**
> With values in 1..n and n + 1 indices, the function i → nums[i] has a duplicate value with two incoming "edges", which creates a cycle whose entry is that value. Index 0 isn't a value (values are ≥ 1), so it's outside the cycle and works as the head.

### Follow-up questions

**F1. Remove the cycle.**
> Find the entry, then walk from the entry around the cycle to the node whose `next` is the entry, and set that `next` to null.

**F2. Detect a cycle in a directed graph.** → [09-Graph M03 Course Schedule](../../09-Graph/03-Medium/M03_CourseSchedule.md)
> DFS with three colours (unvisited / visiting / done), or Kahn's topological sort.

**F3. Pollard's rho (integer factorisation).**
> It uses the same tortoise-and-hare cycle detection on the sequence x → x² + c mod n.

---

## 10. Running the file

```bash
java M01_LinkedListCycleII.java      # 6/6 tests passed (including all n ≤ 30 × all entry positions)
```

**One-line summary:** *"Meet with fast/slow, then restart one pointer at the head; both at speed 1 meet at the entry (a = kc − b)."*
