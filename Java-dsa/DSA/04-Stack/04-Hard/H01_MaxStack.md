# H01 · Max Stack

| | |
|---|---|
| **LeetCode** | [#716 Max Stack](https://leetcode.com/problems/max-stack/) (Premium) |
| **Difficulty** | Hard |
| **Pattern** | Doubly linked list + TreeMap (value → nodes): O(log n) for every operation |
| **Code** | [H01_MaxStack.java](H01_MaxStack.java) |
| **Run** | `java H01_MaxStack.java` |

---

## 1. The problem

Design a stack with `push(x)`, `pop()`, `top()`, `peekMax()` and **`popMax()`**. `popMax` removes the **maximum** element; if there are several, the one **closest to the top**.

```
push 5, push 1, push 5
top()     → 5
popMax()  → 5      (the TOP 5, not the bottom one)
top()     → 1
peekMax() → 5
pop()     → 1
top()     → 5
```

---

## 2. How to think about it

### Why it's harder than Min Stack ([M01](../03-Medium/M01_MinStack.md))
`peekMax` is easy (store the running max, as in M01). But **`popMax` removes an element from the middle** of the stack, which breaks M01's assumption that "everything below me is unchanged".

### Approach 1: two stacks (values + running max)
`popMax`: pop into a buffer until the top equals the max, drop it, then push the buffer back (which recomputes the running maxes). Simple, but **popMax is O(n)**. It's good to mention first.

### Approach 2: a doubly linked list + a TreeMap (all O(log n))
- A **doubly linked list (DLL)** holds the stack order (the tail = the top), so **any** node can be removed in O(1).
- A **`TreeMap<value, list of nodes with that value>`** finds the maximum value in O(log n). The **last** node in a value's list is the one closest to the top.

| operation | how |
|---|---|
| `push(x)` | append a node to the DLL; add it to `map[x]` |
| `pop()` | remove the DLL tail; remove it from `map[its value]` |
| `top()` | the DLL tail's value |
| `peekMax()` | `map.lastKey()` |
| `popMax()` | max = `map.lastKey()`; node = the last of `map[max]`; unlink it from the DLL |

This is the kind of "combine two structures" design that senior interviews love: **one structure for order, one for fast lookup, kept in sync.**

---

## 3. Java concepts used in this problem

### 3.1 `TreeMap`: a sorted map

```java
TreeMap<Integer, Deque<Node>> byValue = new TreeMap<>();
byValue.lastKey();      // the largest key, O(log n)
byValue.firstKey();     // the smallest key
byValue.floorKey(x);    // the largest key ≤ x
byValue.ceilingKey(x);  // the smallest key ≥ x
byValue.remove(k);
```

A `TreeMap` keeps its keys **sorted** (a red-black tree), so min/max/floor/ceiling are O(log n). A `HashMap` is O(1) for get/put, but has no order.

### 3.2 A doubly linked list with sentinels

```java
// ... inside class MaxStack ...
private final Node head = new Node(0), tail = new Node(0);   // fake nodes at both ends
MaxStack() { head.next = tail; tail.prev = head; }
```

The **sentinels** mean every real node always has a non-null `prev` and `next`, so inserting and unlinking need **no null checks**:

```java
n.prev.next = n.next;      // unlink n in O(1)
n.next.prev = n.prev;
```

(See [DoublyLinkedList](../../03-Linked-Lists/01-Basics/DoublyLinkedList.java).)

### 3.3 `computeIfAbsent` for the per-value lists

```java
byValue.computeIfAbsent(x, k -> new ArrayDeque<>()).addLast(n);
```

It creates the list for x the first time, then appends the node. Appending keeps the nodes with the same value **in push order**, so `peekLast()` is the most recent one (the closest to the top).

### 3.4 A private nested `Node` class

`private static class Node` is visible only inside `MaxStack`. Users see only the public stack API (encapsulation).

---

## 4. Approach 1: Two stacks (the file's second class)

### Code ([H01_MaxStack.java:92-112](H01_MaxStack.java#L92-L112))

```java
static class MaxStackSimple {
    private final Deque<Integer> values = new ArrayDeque<>(), maxes = new ArrayDeque<>();

    public void push(int x) {
        values.push(x);
        maxes.push(maxes.isEmpty() ? x : Math.max(x, maxes.peek()));
    }

    public int pop() { maxes.pop(); return values.pop(); }
    public int top() { return values.peek(); }
    public int peekMax() { return maxes.peek(); }

    public int popMax() {
        int max = peekMax();
        Deque<Integer> buffer = new ArrayDeque<>();
        while (top() != max) buffer.push(pop());                     // dig down to the max
        pop();                                                       // remove it
        while (!buffer.isEmpty()) push(buffer.pop());                // put the rest back
        return max;
    }
}
```

| Method | What it does |
|---|---|
| `push` | the value, plus the running max (as in Min Stack) |
| `pop` / `top` / `peekMax` | O(1) |
| `popMax` | pop everything above the topmost max into a buffer, remove the max, push the buffer back (the pushes recompute the maxes). **O(n)** |

**Dry run** `push 5, 1, 5; popMax`: the max is 5 and `top()` is 5 → no digging → pop it → `values = [5, 1]` ✓

---

## 5. Approach 2: DLL + TreeMap (the file's main class)

### Code ([H01_MaxStack.java:41-89](H01_MaxStack.java#L41-L89))

```java
static class MaxStack {
    private static class Node {
        int val;
        Node prev, next;
        Node(int val) { this.val = val; }
    }

    private final Node head = new Node(0), tail = new Node(0);       // sentinels
    private final TreeMap<Integer, Deque<Node>> byValue = new TreeMap<>();

    MaxStack() { head.next = tail; tail.prev = head; }

    public void push(int x) {
        Node n = new Node(x);
        n.prev = tail.prev; n.next = tail;                           // insert before the tail sentinel
        tail.prev.next = n; tail.prev = n;
        byValue.computeIfAbsent(x, k -> new ArrayDeque<>()).addLast(n);
    }

    public int pop() {
        Node n = tail.prev;
        unlink(n);
        removeFromMap(n.val);                                        // n is the newest node of its value
        return n.val;
    }

    public int top() { return tail.prev.val; }

    public int peekMax() { return byValue.lastKey(); }

    public int popMax() {
        int max = byValue.lastKey();
        Node n = byValue.get(max).peekLast();                        // the one nearest the top
        removeFromMap(max);
        unlink(n);
        return max;
    }

    private void unlink(Node n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
    }

    private void removeFromMap(int val) {
        Deque<Node> nodes = byValue.get(val);
        nodes.pollLast();
        if (nodes.isEmpty()) byValue.remove(val);
    }
}
```

### Line by line

| Method | What it does |
|---|---|
| `push` | link the new node just before the tail sentinel (it's the new top), and register it under its value |
| `pop` | the top is `tail.prev`; unlink it and remove it from its value's list. It's the **last** entry of that list, because it's the most recently pushed node of that value that's still present |
| `top` | `tail.prev.val` |
| `peekMax` | the largest key in the TreeMap |
| `popMax` | the largest value's most recent node; remove it from the map and from the DLL |
| `removeFromMap` | drop the last node for this value; remove the key when no nodes are left (so `lastKey` stays correct) |

**Why does `pop` remove the *last* node of its value's list?** That value's list is in push order. The current top is the most recently pushed of all the remaining nodes, so it's in particular the most recent among the nodes with its value.

### Dry run: `push 5, push 1, push 5, top, popMax, top, peekMax, pop, top`

| operation | DLL (bottom → top) | map | result |
|---|---|---|---|
| push 5 | 5a | {5: [5a]} | |
| push 1 | 5a 1 | {1: [1], 5: [5a]} | |
| push 5 | 5a 1 5b | {1: [1], 5: [5a, 5b]} | |
| top | | | **5** |
| popMax | 5a 1 | {1: [1], 5: [5a]} | **5** (it removed 5b) |
| top | | | **1** |
| peekMax | | | **5** |
| pop | 5a | {5: [5a]} | **1** |
| top | | | **5** ✓ |

### Complexity

| operation | DLL + TreeMap | two stacks |
|---|---|---|
| push | O(log n) | O(1) |
| pop | O(log n) | O(1) |
| top | O(1) | O(1) |
| peekMax | O(log n) | O(1) |
| popMax | **O(log n)** | O(n) |

Space O(n) for both. The file checks 20,000 random operations on both versions against a plain list.

---

## 6. (Bonus) Approach 3: Two heaps/sets with lazy deletion

Give each push an increasing id. Keep a max-heap ordered by (value, id) **and** a stack of (value, id). Removed ids go into a "deleted" set; when either structure's top has been deleted, pop it lazily. Each operation is amortised O(log n). It's a common alternative when a TreeMap of lists feels heavy.

---

## 7. Edge cases and common mistakes

**Edge cases:** several equal maxima (popMax must take the topmost), popMax when the max is the top, interleaved pop/popMax, a single element.

**Mistakes:**
1. popMax removing the **bottom-most** copy of the max.
2. Not removing empty value lists from the TreeMap (`lastKey` would return a value that's no longer present).
3. A DLL without sentinels, with many null-check bugs.
4. In approach 1, forgetting that the re-pushes must recompute the running maxes.

---

## 8. Interview questions and answers

**Q1. Why isn't the Min/Max Stack trick enough?**
> The stored running max only stays valid if elements leave from the top. popMax removes from the middle, invalidating the running maxima of everything above it.

**Q2. Why both a DLL and a TreeMap?**
> The DLL keeps the stack order and allows O(1) removal of any node; the TreeMap gives the maximum value in O(log n) and points to the right node. Each covers the other's weakness.

**Q3. How do you choose among the equal maxima?**
> Keep the nodes per value in push order and take the last one, which is the closest to the top.

### Follow-up questions

**F1. LRU Cache (LC 146).** → [12-Advanced M01](../../12-Advanced/03-Medium/M01_LRUCache.md)
> The same pairing: a HashMap for lookup + a DLL for order.

**F2. LFU Cache (LC 460).** → [12-Advanced H01](../../12-Advanced/04-Hard/H01_LFUCache.md)
> A HashMap + per-frequency DLLs + a minFreq pointer.

**F3. Maximum Frequency Stack (LC 895).** → [12-Advanced H04](../../12-Advanced/04-Hard/H04_MaximumFrequencyStack.md)
> A map of frequency → stack of values, popping from the highest frequency.

---

## 9. Running the file

```bash
java H01_MaxStack.java      # 7/7 tests passed (including 20,000 random ops on both versions)
```

**One-line summary:** *"A DLL for the order (O(1) removal anywhere) + a TreeMap value → nodes for the max; popMax takes the last node of the largest key."*
