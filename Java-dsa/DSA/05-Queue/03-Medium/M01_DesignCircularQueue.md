# M01 · Design Circular Queue

| | |
|---|---|
| **LeetCode** | [#622 Design Circular Queue](https://leetcode.com/problems/design-circular-queue/) |
| **Difficulty** | Medium |
| **Pattern** | Fixed-capacity ring buffer (head + size, indices wrap with `%`) |
| **Code** | [M01_DesignCircularQueue.java](M01_DesignCircularQueue.java) |
| **Run** | `java M01_DesignCircularQueue.java` |

---

## 1. The problem

Design `MyCircularQueue(k)` with:

| method | behaviour |
|---|---|
| `enQueue(v)` | add at the back; `false` if full |
| `deQueue()` | remove from the front; `false` if empty |
| `Front()` / `Rear()` | the front / back value, or −1 if empty |
| `isEmpty()` / `isFull()` | |

LeetCode sequence (k = 3): `enQueue 1, 2, 3` → true ×3; `enQueue 4` → false; `Rear` → 3; `isFull` → true; `deQueue` → true; `enQueue 4` → true; `Rear` → 4.

---

## 2. How to think about it

### Why "circular"?
A plain array queue moves its head forward on every dequeue, so the space at the front is wasted. A **ring buffer** lets the indices **wrap around** to reuse that space:

```
capacity 3:   [ a | b | c ]   dequeue a → head moves to index 1
              [ _ | b | c ]   enqueue d → it goes into index 0 (wrapping around)
              [ d | b | c ]   logical order: b, c, d
```

### The classic pitfall: empty versus full
With only `head` and `tail` indices, an **empty** queue and a **full** queue look the same (`head == tail` in both cases). The fixes:
- **(a)** also track `size` ← used here, and the clearest
- **(b)** keep one slot always empty (capacity k + 1): full when `(tail + 1) % cap == head`

### With head + size
```
the index of the last element = (head + size − 1) % k
enQueue: data[(head + size) % k] = v;   size++
deQueue: head = (head + 1) % k;         size−−
Front = data[head],   Rear = data[(head + size − 1) % k]
```

---

## 3. Java concepts used in this problem

### 3.1 `%` for wrap-around indexing

`(head + size) % data.length` maps any position back into `[0, k)`. It's the core of every circular structure (Rotate Array, ring buffers, round robin).

### 3.2 A fixed-size `int[]` allocated once

`data = new int[k]` in the constructor, with no resizing and no per-element objects. That's **why** ring buffers are popular: fixed memory, no garbage collection pressure, O(1) everything. They're used in network drivers, audio, logging ("keep the last N lines"), and the LMAX Disruptor (a famous high-throughput Java queue).

### 3.3 Method names with capitals

`Front()` and `Rear()` break Java's camelCase convention only because LeetCode's API defines them that way.

### 3.4 Random testing against `ArrayDeque`

20,000 random operations are compared with a real `ArrayDeque` limited to 5 elements, checking the return values and the front/rear after each step.

---

## 4. Approach 1: An "empty slot" ring buffer (capacity k + 1)

```java
static class CircularQueueSpareSlot {
    private final int[] data;
    private int head = 0, tail = 0;                // tail = the next write position
    CircularQueueSpareSlot(int k) { data = new int[k + 1]; }
    boolean enQueue(int v) {
        if (isFull()) return false;
        data[tail] = v;
        tail = (tail + 1) % data.length;
        return true;
    }
    boolean deQueue() {
        if (isEmpty()) return false;
        head = (head + 1) % data.length;
        return true;
    }
    int Front() { return isEmpty() ? -1 : data[head]; }
    int Rear()  { return isEmpty() ? -1 : data[(tail - 1 + data.length) % data.length]; }
    boolean isEmpty() { return head == tail; }
    boolean isFull()  { return (tail + 1) % data.length == head; }
}
```

One slot is always kept empty so that `head == tail` can only mean "empty". `(tail − 1 + n) % n` steps left safely (see [M02](M02_DesignCircularDeque.md) for why the `+ n` is needed).

---

## 5. Approach 2: head + size (the file's solution)

### Code ([M01_DesignCircularQueue.java:40-64](M01_DesignCircularQueue.java#L40-L64))

```java
static class MyCircularQueue {
    private final int[] data;
    private int head = 0, size = 0;

    MyCircularQueue(int k) { data = new int[k]; }

    public boolean enQueue(int value) {
        if (isFull()) return false;
        data[(head + size) % data.length] = value;
        size++;
        return true;
    }

    public boolean deQueue() {
        if (isEmpty()) return false;
        head = (head + 1) % data.length;
        size--;
        return true;
    }

    public int Front() { return isEmpty() ? -1 : data[head]; }
    public int Rear() { return isEmpty() ? -1 : data[(head + size - 1) % data.length]; }
    public boolean isEmpty() { return size == 0; }
    public boolean isFull() { return size == data.length; }
}
```

### Line by line

| Method | What it does |
|---|---|
| `enQueue` | the next free slot is `head + size` (wrapped); write there, size++ |
| `deQueue` | just move head forward (the old value doesn't need to be cleared), size−− |
| `Front` | `data[head]` |
| `Rear` | the last element is at `head + size − 1` (wrapped) |
| `isEmpty` / `isFull` | `size` resolves the empty/full ambiguity |

### Dry run: k = 3

| operation | data | head | size | returns |
|---|---|---|---|---|
| enQueue 1 | [1, _, _] | 0 | 1 | true |
| enQueue 2 | [1, 2, _] | 0 | 2 | true |
| enQueue 3 | [1, 2, 3] | 0 | 3 | true |
| enQueue 4 | full | | | **false** |
| Rear | data[(0 + 3 − 1) % 3] = data[2] | | | **3** |
| isFull | | | | **true** |
| deQueue | [_, 2, 3] | 1 | 2 | true |
| enQueue 4 | slot (1 + 2) % 3 = **0** → [4, 2, 3] | 1 | 3 | true |
| Rear | data[(1 + 3 − 1) % 3] = data[0] | | | **4** |

The queue's logical order is now 2, 3, 4, stored with a wrap-around ✓

### Complexity
- **All operations O(1)**, **space O(k).**

---

## 6. Comparing the approaches

| | Spare slot (k + 1) | head + size |
|---|---|---|
| Extra state | none (one wasted slot) | a `size` counter |
| Empty/full check | `head == tail` / `(tail + 1) % n == head` | `size == 0` / `size == k` |
| Clarity | trickier | **clearer** |

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 1, alternating enqueue/dequeue across the wrap point, dequeue on empty, enqueue on full.

**Mistakes:**
1. Using `head == tail` for both empty and full.
2. Forgetting the `% length` in `Rear()`.
3. Shifting the elements on dequeue (O(n), which defeats the purpose).
4. Computing the rear as `tail − 1` without the wrap (negative index).

---

## 8. Interview questions and answers

**Q1. How do you distinguish empty from full?**
> Track the size explicitly, or always leave one slot empty so that head == tail can only mean empty.

**Q2. Why are ring buffers used in high-performance systems?**
> Fixed memory allocated once, O(1) operations, no per-element allocation, and good cache locality. With careful memory ordering, single-producer/single-consumer ring buffers can even be lock-free.

### Follow-up questions

**F1. Make it thread-safe.**
> Wrap the methods in a lock, or use `ArrayBlockingQueue` (a ring buffer with locks and conditions). For a single producer and single consumer: lock-free with volatile head/tail indices.

**F2. Grow when full.**
> Allocate double the capacity and copy the elements in **logical order** (from head, wrapping), then reset head = 0. That's what `ArrayDeque` does.

**F3. A circular deque (insert/delete at both ends).** → [M02](M02_DesignCircularDeque.md)

---

## 9. Running the file

```bash
java M01_DesignCircularQueue.java      # 2/2 tests passed (the LeetCode sequence + 20,000 random ops)
```

**One-line summary:** *"A fixed array + head + size: write at (head + size) % k, read at head, rear at (head + size − 1) % k."*
