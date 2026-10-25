# M06 · Rotate List

| | |
|---|---|
| **LeetCode** | [#61 Rotate List](https://leetcode.com/problems/rotate-list/) |
| **Difficulty** | Medium |
| **Pattern** | Make it a circle, then cut in the right place |
| **Code** | [M06_RotateList.java](M06_RotateList.java) |
| **Run** | `java M06_RotateList.java` |

---

## 1. The problem

Rotate the list to the **right** by k places.

| list | k | result |
|---|---|---|
| `1 → 2 → 3 → 4 → 5` | 2 | `4 → 5 → 1 → 2 → 3` |
| `0 → 1 → 2` | 4 | `2 → 0 → 1` (4 % 3 = 1) |
| `1 → 2 → 3` | 3 | `1 → 2 → 3` (a full turn) |
| `[]` | 5 | `[]` |

**Constraints:** `0 ≤ n ≤ 500`, `0 ≤ k ≤ 2 × 10⁹` (k can be **huge**).

---

## 2. How to think about it

### Observations
- Rotating by the length n gives back the same list, so **`k = k % n`**. With k up to 2 × 10⁹, simulating each step would be far too slow.
- A right rotation by k moves the **last k nodes to the front**, keeping their order.

### The circle trick
1. Find the length **n** and the **tail** (one pass).
2. Connect `tail → head`. It's now a **circular** list (see [CircularLinkedList](../01-Basics/CircularLinkedList.java)).
3. The **new tail** is at position `n − k − 1` (0-indexed) from the old head. Walk there; `newHead = newTail.next`; then cut: `newTail.next = null`.

Compare this with [Arrays M02 Rotate Array](../../01-Arrays/03-Medium/M02_RotateArray.md): arrays need the triple-reverse trick because the elements must physically move. A list needs only **two pointer changes**.

---

## 3. Java concepts used in this problem

### 3.1 `k %= n` with huge k

`k %= n` keeps k in `[0, n)`. Also, `int` can hold 2 × 10⁹ (the max is about 2.147 × 10⁹), so `k` fits.

### 3.2 Counting the length while finding the tail

```java
int n = 1;
ListNode tail = head;
while (tail.next != null) { tail = tail.next; n++; }
```

It starts at 1 because we're already standing on the head. It stops **on** the last node (not past it), so we keep a reference to the tail.

### 3.3 Exhaustive testing against an array rotation

The test tries every n ≤ 12 and every k ≤ 30, comparing with the array formula `exp[(i + k) % n] = v[i]`. With small inputs you can test **all** cases, not just random ones.

---

## 4. Approach 1: Rotate one step at a time (too slow for huge k)

```java
static ListNode rotateStepByStep(ListNode head, int k) {
    if (head == null || head.next == null) return head;
    int n = 0;
    for (ListNode c = head; c != null; c = c.next) n++;
    k %= n;                                        // without this, k = 2·10⁹ would never finish
    for (int s = 0; s < k; s++) {                  // move the last node to the front, k times
        ListNode prev = head;
        while (prev.next.next != null) prev = prev.next;   // find the second-to-last
        ListNode last = prev.next;
        prev.next = null;
        last.next = head;
        head = last;
    }
    return head;
}
```

**O(n · k) time** (even with `k %= n`, it's O(n²) in the worst case).

---

## 5. Approach 2: Circle and cut (the file's solution)

### Code ([M06_RotateList.java:39-52](M06_RotateList.java#L39-L52))

```java
static ListNode rotateRight(ListNode head, int k) {
    if (head == null || head.next == null) return head;
    int n = 1;
    ListNode tail = head;
    while (tail.next != null) { tail = tail.next; n++; }   // 1. length + tail
    k %= n;
    if (k == 0) return head;
    tail.next = head;                                      // 2. close the circle
    ListNode newTail = head;
    for (int i = 0; i < n - k - 1; i++) newTail = newTail.next;   // 3. find the cut point
    ListNode newHead = newTail.next;
    newTail.next = null;                                   //    cut
    return newHead;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (head == null \|\| head.next == null) return head;` | 0 or 1 nodes: a rotation changes nothing |
| the length loop | n = the length, `tail` = the last node |
| `k %= n; if (k == 0) return head;` | full turns do nothing |
| `tail.next = head;` | make it a circle |
| `for (i < n - k - 1) newTail = newTail.next;` | walk to the node that will become the tail |
| `newHead = newTail.next; newTail.next = null;` | the node after it is the new head; cut the circle there |

**Why `n − k − 1`?** After the rotation, the original indices `0..n−k−1` come **last** (in order), so the new tail is the original node at index `n − k − 1`.

### Dry run: `1 → 2 → 3 → 4 → 5`, k = 2

1. n = 5, tail = 5; k = 2 % 5 = 2.
2. Circle: `1 → 2 → 3 → 4 → 5 → (1)`.
3. Walk `n − k − 1 = 2` steps from 1: 1 → 2 → **3** = the new tail.
4. The new head = `3.next` = **4**; cut after 3.

**Result `4 → 5 → 1 → 2 → 3`** ✓

### Dry run 2: `0 → 1 → 2`, k = 4
n = 3, k = 4 % 3 = 1 → walk 3 − 1 − 1 = 1 step: newTail = 1 → newHead = 2 → **`2 → 0 → 1`** ✓

### Dry run 3: k = n
`k % n = 0` → return the head unchanged ✓

### Complexity
- **Time O(n):** one pass for the length, plus up to n steps to the cut point.
- **Space O(1).**

---

## 6. Comparing the approaches

| | Step by step | Circle + cut |
|---|---|---|
| Time | O(n · (k mod n)) | **O(n)** |
| Space | O(1) | O(1) |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty list, one node, k = 0, k a multiple of n, k much bigger than n.

**Mistakes:**
1. Forgetting `k %= n`, which means a huge walk (or index errors with `n − k − 1 < 0`).
2. Forgetting to cut the circle, which leaves an infinite loop.
3. Off by one in the walk (`n − k` instead of `n − k − 1`).
4. Rotating **left** by mistake (that's `k' = n − k` steps for a right rotation).

---

## 8. Interview questions and answers

**Q1. Why make the list circular?**
> Then the rotation is just choosing a new place to cut: one link added (tail → head) and one removed (newTail → null).

**Q2. Where is the new tail?**
> At index `n − k − 1` from the old head: the last k nodes become the front, so the node just before them becomes the tail.

### Follow-up questions

**F1. Rotate left by k.**
> The new tail is at index `k − 1` (after `k %= n`).

**F2. Rotate an array.** → [Arrays M02](../../01-Arrays/03-Medium/M02_RotateArray.md)
> The triple reverse.

**F3. Rotate a doubly linked list.**
> The same circle-and-cut, also updating the `prev` pointers at the cut.

**F4. Split a Linked List into k parts (LC 725).**
> Use the length: each part gets `n / k` nodes, and the first `n % k` parts get one extra.

---

## 9. Running the file

```bash
java M06_RotateList.java      # 5/5 tests passed (including all n ≤ 12 × k ≤ 30)
```

**One-line summary:** *"k %= n; join the tail to the head; walk n − k − 1 steps to the new tail; its next is the new head; cut."*
