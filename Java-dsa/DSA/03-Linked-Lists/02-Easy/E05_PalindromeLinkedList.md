# E05 · Palindrome Linked List

| | |
|---|---|
| **LeetCode** | [#234 Palindrome Linked List](https://leetcode.com/problems/palindrome-linked-list/) |
| **Difficulty** | Easy |
| **Pattern** | Find the middle (fast/slow) + reverse the second half + compare |
| **Code** | [E05_PalindromeLinkedList.java](E05_PalindromeLinkedList.java) |
| **Run** | `java E05_PalindromeLinkedList.java` |

---

## 1. The problem

Does the list read the same forwards and backwards? Follow-up: **O(n) time and O(1) space**.

| list | answer |
|---|---|
| `1 → 2 → 2 → 1` | true |
| `1 → 2` | false |
| `1 → 2 → 3 → 2 → 1` | true |
| `9` | true |
| `1 → 2 → 3` | false |

**Constraints:** `1 ≤ n ≤ 10⁵`.

---

## 2. How to think about it

The difficulty: in a **singly** linked list you can't walk **backwards** from the tail, so the two-pointer palindrome check from [Strings E02](../../02-Strings/02-Easy/E02_ValidPalindrome.md) doesn't work directly.

1. **Easy way:** copy the values into an array, then use two pointers. O(n) space.
2. **O(1) space:** make the second half walk backwards by **reversing it**:
   1. find the **middle** with fast/slow ([E07](E07_MiddleOfLinkedList.md))
   2. **reverse** the second half in place ([E01](E01_ReverseLinkedList.md))
   3. **compare** the first half with the reversed second half, node by node
   4. (good manners) **reverse it back**, so the caller's list is unchanged

This problem is a **combination of two basic patterns**, which is typical for linked-list problems.

---

## 3. Java concepts used in this problem

### 3.1 Combining helper methods

`isPalindrome` calls `reverse` twice (to reverse and then restore). Small, well-tested helpers combine into bigger solutions, so keep your `reverse` handy.

### 3.2 `break` with a result flag

```java
boolean result = true;
while (p2 != null) {
    if (p1.val != p2.val) { result = false; break; }
    ...
}
reverse(secondHalf);      // still runs after a break
return result;
```

We can't just `return false` inside the loop, because we want to **restore** the list first. So we record the result, `break` out, restore, and then return.

### 3.3 Side effects on the input

Reversing half of the caller's list modifies their data. The fourth step undoes it. The test `"input restored afterwards"` checks that. Always mention in an interview whether your solution modifies its input.

---

## 4. Approach 1: Copy into an array + two pointers

```java
static boolean isPalindromeArray(ListNode head) {
    List<Integer> vals = new ArrayList<>();
    for (ListNode c = head; c != null; c = c.next) vals.add(c.val);
    for (int i = 0, j = vals.size() - 1; i < j; i++, j--) {
        if (!vals.get(i).equals(vals.get(j))) return false;   // Integer objects: use equals
    }
    return true;
}
```

- `vals.get(i).equals(vals.get(j))`: these are `Integer` objects, so `==` would compare references and fail for values outside −128..127. **Use `.equals`** (or `intValue()`).
- **O(n) time, O(n) space.**

---

## 5. Approach 2: Reverse the second half (the file's solution)

### Code ([E05_PalindromeLinkedList.java:37-69](E05_PalindromeLinkedList.java#L37-L69))

```java
static boolean isPalindrome(ListNode head) {
    if (head == null || head.next == null) return true;
    // 1. middle
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    // 2. reverse the second half (starting at slow)
    ListNode secondHalf = reverse(slow);
    // 3. compare
    boolean result = true;
    ListNode p1 = head, p2 = secondHalf;
    while (p2 != null) {                       // the second half is the shorter or equal one
        if (p1.val != p2.val) { result = false; break; }
        p1 = p1.next;
        p2 = p2.next;
    }
    // 4. restore the list
    reverse(secondHalf);
    return result;
}

static ListNode reverse(ListNode head) {
    ListNode prev = null;
    while (head != null) {
        ListNode next = head.next;
        head.next = prev;
        prev = head;
        head = next;
    }
    return prev;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (head == null \|\| head.next == null) return true;` | 0 or 1 nodes is always a palindrome |
| fast/slow loop | slow ends at the middle: for odd n the exact middle, for even n the **second** middle |
| `ListNode secondHalf = reverse(slow);` | reverse from the middle to the end; `secondHalf` is the old tail |
| `while (p2 != null)` | compare as long as the reversed half has nodes |
| `if (p1.val != p2.val) { result = false; break; }` | a mismatch (`val` is an `int`, so `!=` is safe) |
| `reverse(secondHalf);` | restore the original order of the second half |

### Dry run 1: `1 → 2 → 3 → 2 → 1` (odd)

1. **Middle:** slow: 1 → 2 → **3**; fast: 1 → 3 → 1(last) → stop. slow = the node with value 3.
2. **Reverse from 3:** `3 → 2 → 1` becomes `1 → 2 → 3`. The first half is still `1 → 2 → 3` (node 2 still points to node 3, which is now the end of the reversed half).

```
first:    1 → 2 → 3 → null      (the 3 here is the same node as the reversed half's tail)
reversed: 1 → 2 → 3 → null
```

3. **Compare:** 1 = 1, 2 = 2, 3 = 3 → **true** ✓
4. Restore → `1 → 2 → 3 → 2 → 1` again.

### Dry run 2: `1 → 2 → 2 → 1` (even)

1. **Middle:** slow: 1 → 2 → **2 (the second 2)**; fast: 1 → 2(second) → null → stop.
2. **Reverse from the second 2:** `2 → 1` becomes `1 → 2`.
3. **Compare:** 1 = 1, 2 = 2 → p2 is null → **true** ✓

### Dry run 3: `1 → 2 → 3`
Middle = 2; reverse `2 → 3` → `3 → 2`; compare 1 vs 3 → **false** ✓

### Complexity
- **Time O(n):** about n/2 + n/2 + n/2 + n/2 steps.
- **Space O(1).**

---

## 6. (Bonus) Approach 3: Recursion with a front pointer

```java
static ListNode front;
static boolean isPalindromeRecursive(ListNode head) {
    front = head;
    return check(head);
}
static boolean check(ListNode node) {
    if (node == null) return true;
    if (!check(node.next)) return false;        // recurse to the end first
    boolean ok = node.val == front.val;          // node comes back from the tail; front goes forward
    front = front.next;
    return ok;
}
```

The recursion reaches the end first, so `node` visits the nodes **backwards** as the calls return, while `front` moves forwards. It's elegant, but uses **O(n) stack**, so it's not O(1) space.

---

## 7. Comparing the approaches

| | Array copy | Reverse the second half | Recursion |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) | **O(1)** | O(n) stack |
| Modifies the input | no | temporarily (restored) | no |

---

## 8. Edge cases and common mistakes

**Edge cases:** one node, two equal nodes, two different nodes, odd vs even length.

**Mistakes:**
1. Comparing until `p1 != null` instead of `p2 != null` (with the half-reversed structure, p1 can run longer).
2. Returning early without restoring the list.
3. Comparing `Integer` objects with `==` in the array version.
4. Getting the middle wrong for even lengths (here either middle works, as long as the comparison loop is driven by the reversed half).

---

## 9. Interview questions and answers

**Q1. How do you get O(1) space?**
> Find the middle with fast/slow, reverse the second half in place, compare it with the first half, then reverse it back.

**Q2. Is modifying the list acceptable?**
> Only temporarily, and I restore it. In a concurrent setting, though, other threads could observe the broken list. Then I'd use the O(n)-space array approach.

**Q3. What's the complexity?**
> O(n) time: a constant number of passes over halves of the list. O(1) extra space.

### Follow-up questions

**F1. Reorder List (LC 143): L0 → Ln → L1 → Ln−1 …** → [M04](../03-Medium/M04_ReorderList.md)
> The same three steps: middle, reverse the second half, then **interleave** instead of comparing.

**F2. Maximum Twin Sum of a Linked List (LC 2130).**
> Middle + reverse the second half, then walk both halves, summing the pairs.

**F3. Palindrome check on a doubly linked list.**
> Easy: two pointers from the head and the tail, moving inward.

---

## 10. Running the file

```bash
java E05_PalindromeLinkedList.java      # 6/6 tests passed
```

**One-line summary:** *"Middle by fast/slow, reverse the second half, compare, reverse it back."*
