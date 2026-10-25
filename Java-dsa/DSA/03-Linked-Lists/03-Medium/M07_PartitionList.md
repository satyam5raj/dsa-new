# M07 · Partition List

| | |
|---|---|
| **LeetCode** | [#86 Partition List](https://leetcode.com/problems/partition-list/) |
| **Difficulty** | Medium |
| **Pattern** | Split into two dummy-headed lists, then join (a stable partition) |
| **Code** | [M07_PartitionList.java](M07_PartitionList.java) |
| **Run** | `java M07_PartitionList.java` |

---

## 1. The problem

Put all the nodes with value **< x** before the nodes with value **≥ x**, keeping the **original relative order** inside each group (a **stable** partition).

| list | x | result |
|---|---|---|
| `1 → 4 → 3 → 2 → 5 → 2` | 3 | `1 → 2 → 2 → 4 → 3 → 5` |
| `2 → 1` | 2 | `1 → 2` |
| `1 → 2` | 9 | `1 → 2` (all smaller) |
| `5 → 6` | 0 | `5 → 6` (all bigger) |

**Constraints:** `0 ≤ n ≤ 200`.

---

## 2. How to think about it

With **arrays**, a stable partition needs extra space or O(n²) shifting (see [Arrays E18](../../01-Arrays/02-Easy/E18_SortArrayByParity.md), where the fast in-place version isn't stable). With a **linked list** it's easy: just **relink** the nodes into two new chains as you walk.

```
less chain: dummy1 → (nodes < x, in order)
more chain: dummy2 → (nodes ≥ x, in order)
then:  lessTail.next = dummy2.next;   moreTail.next = null
```

### The bug everyone hits once: forgetting `moreTail.next = null`
The last node of the "more" chain still points to its **old successor**, which may now sit **earlier** in the result. For `1 → 4 → 3 → 2 → 5 → 2` the last "≥ 3" node is 5, and its old next is the final 2, which is in the "less" chain. Without the cut: `… → 5 → 2 → 4 → 3 → 5 → 2 → …` **forever** (a cycle).

---

## 3. Java concepts used in this problem

### 3.1 Two dummies at once

```java
ListNode lessDummy = new ListNode(0), less = lessDummy;
ListNode moreDummy = new ListNode(0), more = moreDummy;
```

Each chain gets its own dummy, so appending is uniform, even when one chain stays empty. When "less" is empty, `lessDummy.next` ends up being the "more" chain, which is correct.

### 3.2 Stale pointers

When you relink a node into a new chain, its `next` still points wherever it pointed **before** until you overwrite it. Each appended node's `next` is overwritten by the **next** append, except for the **last** node of each chain. That's why both tails need attention: the less tail gets the join, and the more tail gets `null`.

### 3.3 The test compares with a stable array partition

It builds the expected order with two simple loops (first `< x`, then `≥ x`) and compares on 400 random lists.

---

## 4. Approach 1: Collect the values and rebuild

```java
static ListNode partitionValues(ListNode head, int x) {
    List<Integer> small = new ArrayList<>(), big = new ArrayList<>();
    for (ListNode c = head; c != null; c = c.next) (c.val < x ? small : big).add(c.val);
    small.addAll(big);
    ListNode c = head;
    for (int v : small) { c.val = v; c = c.next; }        // overwrite the values in place
    return head;
}
```

`(c.val < x ? small : big).add(...)`: a ternary can choose **which list** to add to. **O(n) time, O(n) space**, and it changes the values instead of relinking the nodes.

---

## 5. Approach 2: Two chains with dummies (the file's solution)

### Code ([M07_PartitionList.java:38-53](M07_PartitionList.java#L38-L53))

```java
static ListNode partition(ListNode head, int x) {
    ListNode lessDummy = new ListNode(0), less = lessDummy;
    ListNode moreDummy = new ListNode(0), more = moreDummy;
    for (ListNode cur = head; cur != null; cur = cur.next) {
        if (cur.val < x) {
            less.next = cur;
            less = cur;
        } else {
            more.next = cur;
            more = cur;
        }
    }
    more.next = null;                  // cut the stale link (avoids a cycle)
    less.next = moreDummy.next;        // join
    return lessDummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| two dummies and two tails | the chains being built |
| `for (ListNode cur = head; ...)` | walk the original list. `cur = cur.next` reads the **old** next, which is still intact, because we only overwrite `less.next` / `more.next` (the **previous** tail's next, not cur's) |
| `less.next = cur; less = cur;` | append to the less chain |
| `more.next = cur; more = cur;` | append to the more chain |
| `more.next = null;` | the more chain's last node must end the list |
| `less.next = moreDummy.next;` | glue: less chain → more chain |
| `return lessDummy.next;` | the head of the result |

### Dry run: `1 → 4 → 3 → 2 → 5 → 2`, x = 3

| cur | < 3? | less chain | more chain |
|---|---|---|---|
| 1 | yes | 1 | |
| 4 | no | 1 | 4 |
| 3 | no | 1 | 4 → 3 |
| 2 | yes | 1 → 2 | 4 → 3 |
| 5 | no | 1 → 2 | 4 → 3 → 5 |
| 2 | yes | 1 → 2 → 2 | 4 → 3 → 5 |

Cut `5.next = null` (it pointed to the last 2!), then join → **`1 → 2 → 2 → 4 → 3 → 5`** ✓

### Complexity
- **Time O(n)**, **Space O(1)** (two dummy nodes).

---

## 6. Comparing the approaches

| | Values + rebuild | Two chains |
|---|---|---|
| Time | O(n) | O(n) |
| Extra space | O(n) | **O(1)** |
| Stable | yes | **yes** |
| Relinks the nodes | no | **yes** |

---

## 7. Edge cases and common mistakes

**Edge cases:** all < x, all ≥ x, an empty list, x equal to some values (they go to the "more" side).

**Mistakes:**
1. Forgetting `more.next = null`, which creates a cycle (an infinite print / timeout).
2. Joining before cutting: harmless here, but think about which tail needs what.
3. Using `<=` instead of `<` (values equal to x must go to the second group).

---

## 8. Interview questions and answers

**Q1. Why is this easier on lists than on arrays?**
> A stable partition on an array needs extra space or shifting. On a list, you can move nodes into new chains in O(1) each, without disturbing the others.

**Q2. Why must the more chain's tail be set to null?**
> It still points to its original successor, which might be a node in the less chain, so joining would create a cycle.

### Follow-up questions

**F1. Three-way partition (< x, = x, > x).**
> Three chains with three dummies, joined in order.

**F2. Odd Even Linked List (LC 328).** → [M12](M12_OddEvenLinkedList.md)
> The same split-and-join, by position parity instead of value.

**F3. Quicksort on a linked list.**
> Use this partition (or a three-way one) as the partition step, recurse on the parts, and concatenate.

---

## 9. Running the file

```bash
java M07_PartitionList.java      # 5/5 tests passed (including 400 random checks)
```

**One-line summary:** *"Two dummy-headed chains (< x and ≥ x); cut the second chain's tail; join less → more."*
