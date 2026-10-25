# E08 · Remove Duplicates from Sorted List (+ #82 "remove all duplicates") ⭐

| | |
|---|---|
| **LeetCode** | [#83 Remove Duplicates from Sorted List](https://leetcode.com/problems/remove-duplicates-from-sorted-list/), variant [#82](https://leetcode.com/problems/remove-duplicates-from-sorted-list-ii/) |
| **Difficulty** | Easy (#82 is Medium) |
| **Pattern** | Compare with the next node (sorted ⇒ duplicates are adjacent); a dummy for #82 |
| **Code** | [E08_RemoveDuplicatesFromSortedList.java](E08_RemoveDuplicatesFromSortedList.java) |
| **Run** | `java E08_RemoveDuplicatesFromSortedList.java` |

⭐ = a bonus classic.

---

## 1. The problem

**#83:** the list is sorted. Delete the duplicates so that each value appears **once**.

| list | result |
|---|---|
| `1 → 1 → 2` | `1 → 2` |
| `1 → 1 → 2 → 3 → 3` | `1 → 2 → 3` |
| `1 → 1 → 1` | `1` |
| `[]` | `[]` |

**#82 (variant):** delete **all** nodes whose value has duplicates, keeping only the values that appear once.

| list | result |
|---|---|
| `1 → 2 → 3 → 3 → 4 → 4 → 5` | `1 → 2 → 5` |
| `1 → 1 → 1 → 2 → 3` | `2 → 3` (the head is removed!) |

**Constraints:** `0 ≤ n ≤ 300`, sorted ascending.

---

## 2. How to think about it

### #83
The list is sorted, so duplicates are **neighbours** (just like the array version, [Arrays E07](../../01-Arrays/02-Easy/E07_RemoveDuplicatesFromSortedArray.md)). Walk with `cur`:
- while `cur.next` has the **same value** → skip it (`cur.next = cur.next.next`)
- only move `cur` forward when the next value **differs**

The first copy of each value is always kept, so **the head never changes**, and no dummy is needed.

### #82
Now the **whole run** of a duplicated value must go, including its first copy, so the head itself may be deleted → use a **dummy**:
- `prev` = the last node we've decided to keep (it starts at the dummy)
- if `cur` starts a run of equal values (`cur.next.val == cur.val`), skip **the entire run** and link `prev.next` to the node after it
- otherwise keep `cur`: `prev = cur`

---

## 3. Java concepts used in this problem

### 3.1 Skipping nodes in place

`cur.next = cur.next.next` removes the next node without moving `cur`, the same move as in [E06](E06_RemoveLinkedListElements.md). It's done repeatedly while duplicates remain.

### 3.2 Saving a value before walking past it

```java
int dup = cur.val;
while (cur != null && cur.val == dup) cur = cur.next;
```

We store the duplicated value in a local variable because `cur` itself moves during the skipping loop.

### 3.3 When do you need a dummy?

Ask: **"Can the head be removed or replaced?"** If yes (as in #82, E06 and M02), use a dummy. If not (as in #83), you don't need one.

---

## 4. Approach 1 (#83): Compare with the next node (the file's solution)

### Code ([E08_RemoveDuplicatesFromSortedList.java:33-40](E08_RemoveDuplicatesFromSortedList.java#L33-L40))

```java
static ListNode deleteDuplicates(ListNode head) {
    ListNode cur = head;
    while (cur != null && cur.next != null) {
        if (cur.next.val == cur.val) cur.next = cur.next.next;   // skip the duplicate
        else cur = cur.next;
    }
    return head;
}
```

### Line by line

| Line | What it does |
|---|---|
| `while (cur != null && cur.next != null)` | there's a next node to compare with (`cur != null` handles the empty list) |
| `if (cur.next.val == cur.val) cur.next = cur.next.next;` | the next node is a duplicate → unlink it; stay on cur, because the new next may also be a duplicate |
| `else cur = cur.next;` | a new value → move on |
| `return head;` | the head is unchanged |

### Dry run: `1 → 1 → 2 → 3 → 3`

| cur | cur.next | equal? | action | list |
|---|---|---|---|---|
| 1 | 1 | yes | unlink | `1 → 2 → 3 → 3` |
| 1 | 2 | no | cur = 2 | |
| 2 | 3 | no | cur = 3 | |
| 3 | 3 | yes | unlink | `1 → 2 → 3` |
| 3 | null | — | stop | |

**Result `1 → 2 → 3`** ✓

### Dry run 2: `1 → 1 → 1`
Unlink, unlink (cur stays on the first 1), then `cur.next` is null → **`1`** ✓

**O(n) time, O(1) space.**

---

## 5. Approach 2 (#82): Dummy + skip whole runs (the file's second method)

### Code ([E08_RemoveDuplicatesFromSortedList.java:43-57](E08_RemoveDuplicatesFromSortedList.java#L43-L57))

```java
static ListNode deleteAllDuplicates(ListNode head) {
    ListNode dummy = new ListNode(0, head), prev = dummy;
    ListNode cur = head;
    while (cur != null) {
        if (cur.next != null && cur.next.val == cur.val) {
            int dup = cur.val;
            while (cur != null && cur.val == dup) cur = cur.next;   // skip the whole run
            prev.next = cur;
        } else {
            prev = cur;
            cur = cur.next;
        }
    }
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode dummy = new ListNode(0, head), prev = dummy;` | prev = the last kept node |
| `if (cur.next != null && cur.next.val == cur.val)` | cur starts a run of duplicates |
| `int dup = cur.val; while (... cur.val == dup) cur = cur.next;` | move cur past every copy |
| `prev.next = cur;` | cut out the whole run (prev doesn't move: cur might start **another** run) |
| `else { prev = cur; cur = cur.next; }` | cur is unique → keep it |
| `return dummy.next;` | the head may have changed |

### Dry run 1: `1 → 1 → 1 → 2 → 3`

| cur | starts a run? | action | list from the dummy | prev |
|---|---|---|---|---|
| 1 | yes (1 = 1) | skip all the 1s → cur = 2; `prev.next = 2` | D → 2 → 3 | D |
| 2 | no (3 ≠ 2) | keep | | 2 |
| 3 | no (next is null) | keep | | 3 |
| null | | stop | | |

**Result `2 → 3`** ✓

### Dry run 2: `1 → 2 → 3 → 3 → 4 → 4 → 5`

Keep 1, keep 2; the 3s form a run → skip to 4, `2.next = 4`; the 4s form a run → skip to 5, `2.next = 5`; keep 5. **Result `1 → 2 → 5`** ✓ (prev stayed on 2 through both runs.)

**O(n) time, O(1) space.**

---

## 6. Comparing #83 and #82

| | #83 keep one | #82 keep only the unique ones |
|---|---|---|
| Can the head change? | no | **yes** → a dummy |
| What we skip | the extra copies after the first | the **entire** run |
| Pointer that stays put | `cur` | `prev` |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty list, all equal values, duplicates at the start or end, no duplicates.

**Mistakes:**
1. #83: advancing `cur` after an unlink, which leaves triplicates behind (`1 → 1 → 1` → `1 → 1`).
2. #82: forgetting the dummy, so you can't remove a duplicated head.
3. #82: moving `prev` after cutting a run (a following run must also attach to prev).
4. Applying this to an **unsorted** list (the duplicates aren't adjacent; use a HashSet).

---

## 8. Interview questions and answers

**Q1. Why does #83 not need a dummy, but #82 does?**
> In #83 the first copy of each value is kept, so the head always stays. In #82 a duplicated head value must be removed completely, so the head can change.

**Q2. What if the list were unsorted?**
> Keep a `HashSet` of the values seen and unlink the nodes whose value is already in it: O(n) time, O(n) space. For #82 on an unsorted list: count the values in a first pass, then remove those with a count > 1.

### Follow-up questions

**F1. Remove Duplicates from an Unsorted List (LC 1836).**
> Two passes: count, then remove the values with a count > 1 (using a dummy).

**F2. Keep at most two copies of each value.**
> Track the run length; unlink when the run length exceeds 2 (the linked version of LC 80).

**F3. The array version.** → [Arrays E07](../../01-Arrays/02-Easy/E07_RemoveDuplicatesFromSortedArray.md)

---

## 9. Running the file

```bash
java E08_RemoveDuplicatesFromSortedList.java      # 6/6 tests passed
```

**One-line summary:** *"#83: skip next while equal to cur. #82: dummy + prev; if cur starts a run, cut out the whole run."*
