# M02 · Design Circular Deque

| | |
|---|---|
| **LeetCode** | [#641 Design Circular Deque](https://leetcode.com/problems/design-circular-deque/) |
| **Difficulty** | Medium |
| **Pattern** | A ring buffer with both ends (`(i − 1 + k) % k` to step left) |
| **Code** | [M02_DesignCircularDeque.java](M02_DesignCircularDeque.java) |
| **Run** | `java M02_DesignCircularDeque.java` |

---

## 1. The problem

Design `MyCircularDeque(k)` with `insertFront`, `insertLast`, `deleteFront`, `deleteLast` (each returns true/false), `getFront`, `getRear` (−1 if empty), `isEmpty` and `isFull`.

LeetCode sequence (k = 3): `insertLast 1` ✓, `insertLast 2` ✓, `insertFront 3` ✓, `insertFront 4` ✗ (full), `getRear` → 2, `isFull` → true, `deleteLast` ✓, `insertFront 4` ✓, `getFront` → 4.

---

## 2. How to think about it

It's [M01 Circular Queue](M01_DesignCircularQueue.md) plus the ability to grow and shrink at the **front**:

| operation | how |
|---|---|
| `insertFront(v)` | `head = (head − 1 + k) % k; data[head] = v; size++` |
| `insertLast(v)` | `data[(head + size) % k] = v; size++` |
| `deleteFront()` | `head = (head + 1) % k; size−−` |
| `deleteLast()` | `size−−` (the last element is at `(head + size − 1) % k`) |

### The "+ k" detail
In Java, **`(−1) % 5 == −1`**, not 4. The `%` of a negative number stays negative. Adding k **before** the `%` keeps the index in `[0, k)`. (`Math.floorMod(head − 1, k)` does the same thing.)

This is exactly how `java.util.ArrayDeque` works internally (it also grows when full).

---

## 3. Java concepts used in this problem

### 3.1 `%` with negative numbers

```java
-1 % 5                 // -1  (Java's % keeps the sign of the dividend)
(-1 + 5) % 5           //  4
Math.floorMod(-1, 5)   //  4  (always in [0, 5))
```

Java's `%` is the **remainder** of truncating division, not the mathematical modulo. Whenever an index can go negative, add the modulus first or use `Math.floorMod`. The file even includes this as a test.

### 3.2 Deleting is just moving the boundaries

`deleteLast` only does `size−−`, and `deleteFront` moves head. The old values stay in the array but are **outside the logical range**, so they're ignored and later overwritten. There's no need to clear them for primitives (for objects you'd null them out to help garbage collection).

### 3.3 `Deque` methods in the random test

`offerFirst`, `offerLast`, `pollFirst`, `pollLast`, `peekFirst`, `peekLast`: the reference `ArrayDeque` supports both ends, so it's the perfect oracle.

---

## 4. Approach 1: A doubly linked list

```java
static class DequeLinked {
    private static class Node { int v; Node prev, next; Node(int v) { this.v = v; } }
    private final Node head = new Node(0), tail = new Node(0);   // sentinels
    private final int cap;
    private int size;
    DequeLinked(int k) { cap = k; head.next = tail; tail.prev = head; }
    private void insertAfter(Node p, int v) { Node n = new Node(v); n.prev = p; n.next = p.next; p.next.prev = n; p.next = n; size++; }
    private void unlink(Node n) { n.prev.next = n.next; n.next.prev = n.prev; size--; }
    boolean insertFront(int v) { if (size == cap) return false; insertAfter(head, v); return true; }
    boolean insertLast(int v)  { if (size == cap) return false; insertAfter(tail.prev, v); return true; }
    boolean deleteFront() { if (size == 0) return false; unlink(head.next); return true; }
    boolean deleteLast()  { if (size == 0) return false; unlink(tail.prev); return true; }
    int getFront() { return size == 0 ? -1 : head.next.v; }
    int getRear()  { return size == 0 ? -1 : tail.prev.v; }
}
```

It's also O(1) everywhere, but allocates a node per element (more memory, less cache-friendly). The ring buffer is the expected answer.

---

## 5. Approach 2: Ring buffer (the file's solution)

### Code ([M02_DesignCircularDeque.java:36-74](M02_DesignCircularDeque.java#L36-L74))

```java
static class MyCircularDeque {
    private final int[] data;
    private int head = 0, size = 0;

    MyCircularDeque(int k) { data = new int[k]; }

    public boolean insertFront(int value) {
        if (isFull()) return false;
        head = (head - 1 + data.length) % data.length;     // step left, wrapping
        data[head] = value;
        size++;
        return true;
    }

    public boolean insertLast(int value) {
        if (isFull()) return false;
        data[(head + size) % data.length] = value;
        size++;
        return true;
    }

    public boolean deleteFront() {
        if (isEmpty()) return false;
        head = (head + 1) % data.length;
        size--;
        return true;
    }

    public boolean deleteLast() {
        if (isEmpty()) return false;
        size--;
        return true;
    }

    public int getFront() { return isEmpty() ? -1 : data[head]; }
    public int getRear() { return isEmpty() ? -1 : data[(head + size - 1) % data.length]; }
    public boolean isEmpty() { return size == 0; }
    public boolean isFull() { return size == data.length; }
}
```

### Line by line

| Method | What it does |
|---|---|
| `insertFront` | move head one step **left** (wrapping), then write there |
| `insertLast` | write just after the last element (wrapping) |
| `deleteFront` | move head one step right |
| `deleteLast` | shrink the size; the last slot is now outside the range |
| `getFront` / `getRear` | `data[head]` / `data[(head + size − 1) % k]` |

### Dry run: k = 3

| operation | data | head | size | logical order | returns |
|---|---|---|---|---|---|
| insertLast 1 | [1, _, _] | 0 | 1 | 1 | true |
| insertLast 2 | [1, 2, _] | 0 | 2 | 1 2 | true |
| insertFront 3 | head = (0 − 1 + 3) % 3 = **2** → [1, 2, 3] | 2 | 3 | 3 1 2 | true |
| insertFront 4 | full | | | | **false** |
| getRear | data[(2 + 3 − 1) % 3] = data[1] | | | | **2** |
| isFull | | | | | **true** |
| deleteLast | (size → 2) | 2 | 2 | 3 1 | true |
| insertFront 4 | head = (2 − 1) % 3 = 1 → data[1] = 4 → [1, 4, 3] | 1 | 3 | 4 3 1 | true |
| getFront | data[1] | | | | **4** ✓ |

### Complexity
- **All O(1)**, **space O(k).**

---

## 6. Comparing the approaches

| | Doubly linked list | Ring buffer |
|---|---|---|
| Time | O(1) | O(1) |
| Memory | a node object per element | one fixed array |
| Cache-friendliness | poor | **good** |

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 1, inserting at the front when head is 0 (wrap to k − 1), deleting until empty, alternating the ends.

**Mistakes:**
1. `(head − 1) % k` without `+ k` → a negative index → `ArrayIndexOutOfBoundsException`.
2. Forgetting `% length` in `getRear`.
3. Mixing up which end each operation changes (front ops change `head`; back ops only change `size`).

---

## 8. Interview questions and answers

**Q1. Why `(head − 1 + k) % k`?**
> Java's `%` can return a negative value for a negative left operand. Adding k first keeps the value non-negative, so the index stays in range.

**Q2. How does `java.util.ArrayDeque` differ from this?**
> It uses the same ring-buffer idea, but grows (doubles its capacity) when full, uses power-of-two capacities with bit masking instead of `%`, and doesn't allow nulls.

### Follow-up questions

**F1. A sliding window maximum with a deque.** → [Arrays H02](../../01-Arrays/04-Hard/H02_SlidingWindowMaximum.md)

**F2. Make it growable.**
> When full, copy the elements in logical order into an array twice the size, with head = 0.

**F3. Power-of-two capacity with a mask.**
> If k is a power of two, `(i + 1) & (k − 1)` equals `(i + 1) % k` and is faster; `(i − 1) & (k − 1)` handles the negative step automatically.

---

## 9. Running the file

```bash
java M02_DesignCircularDeque.java      # 4/4 tests passed (including 20,000 random ops)
```

**One-line summary:** *"A ring buffer with head + size; insertFront steps head left with (head − 1 + k) % k; deleteLast just shrinks the size."*
