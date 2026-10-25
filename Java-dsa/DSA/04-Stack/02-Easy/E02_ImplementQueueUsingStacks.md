# E02 · Implement Queue using Stacks

| | |
|---|---|
| **LeetCode** | [#232 Implement Queue using Stacks](https://leetcode.com/problems/implement-queue-using-stacks/) |
| **Difficulty** | Easy |
| **Pattern** | Two stacks (in + out) with lazy transfer → amortised O(1) |
| **Code** | [E02_ImplementQueueUsingStacks.java](E02_ImplementQueueUsingStacks.java) |
| **Run** | `java E02_ImplementQueueUsingStacks.java` |

---

## 1. The problem

Build a **FIFO queue** (`push`, `pop`, `peek`, `empty`) using **only stack operations** (push to top, peek/pop from top, size, isEmpty).

```
push(1), push(2), peek() → 1, pop() → 1, push(3), pop() → 2, pop() → 3, empty() → true
```

Follow-up: *each operation in amortised O(1)*.

---

## 2. How to think about it

### Key fact: pouring a stack into another stack reverses the order
LIFO reversed gives **FIFO**!

```
in:  [1, 2, 3>        (3 on top)
pour → out: [3, 2, 1>  (1 on top: the OLDEST element, exactly what a queue serves first)
```

### Two stacks
- **`in`** receives every push.
- **`out`** serves pop and peek. When `out` is **empty**, pour **everything** from `in` into it.

```
push 1, 2, 3     in [1,2,3>    out []
pop              out is empty → pour: in []  out [3,2,1>  → pop 1
push 4           in [4>        out [3,2>
pop              out isn't empty → pop 2   (no pouring!)
```

### Why only pour when `out` is empty?
If `out` still has elements, they're **older** than anything in `in`. Pouring on top of them would bury them and break the order.

### Amortised O(1)
Each element is pushed to `in` once, moved to `out` once, and popped once: **3 operations over its lifetime**. A single pop can cost O(n) (a big pour), but that cost is spread over the n pushes that preceded it. That's the same reasoning as a dynamic array's occasional resize.

---

## 3. Java concepts used in this problem

### 3.1 A class with private fields (the design-problem format)

```java
static class MyQueue {
    private final Deque<Integer> in = new ArrayDeque<>();
    private final Deque<Integer> out = new ArrayDeque<>();
    public void push(int x) { ... }
    ...
}
```

`private final`: the two stacks are internal details (encapsulation), and the references never change. (See [Arrays E21](../../01-Arrays/02-Easy/E21_RangeSumQueryImmutable.md) for classes and constructors.)

### 3.2 Unboxing on return

`out.pop()` returns an `Integer`; the method's return type is `int`, so it's unboxed automatically. If the stack were empty, `pop()` would throw `NoSuchElementException`. LeetCode guarantees valid calls.

### 3.3 Amortised analysis

**Amortised cost** = the total cost of a sequence of operations ÷ the number of operations. Some operations are expensive, but the average over any sequence is small. It's different from **average case** (which is about random inputs): amortised bounds hold for **every** sequence.

### 3.4 Random tests against a real queue

10,000 random push/pop operations are compared with `ArrayDeque` used as a queue (`offer` / `poll`).

---

## 4. Approach 1: Transfer on every push (O(n) push)

```java
static class MyQueueEagerPush {
    private final Deque<Integer> s = new ArrayDeque<>(), tmp = new ArrayDeque<>();
    public void push(int x) {
        while (!s.isEmpty()) tmp.push(s.pop());   // move everything aside
        s.push(x);                                 // x goes to the BOTTOM
        while (!tmp.isEmpty()) s.push(tmp.pop());  // move everything back
    }
    public int pop()  { return s.pop(); }          // the oldest is always on top
    public int peek() { return s.peek(); }
    public boolean empty() { return s.isEmpty(); }
}
```

It keeps the oldest element on top at all times. **push O(n)**, pop/peek O(1). It's simple, but every push is expensive.

---

## 5. Approach 2: Two stacks with lazy transfer (the file's solution)

### Code ([E02_ImplementQueueUsingStacks.java:39-62](E02_ImplementQueueUsingStacks.java#L39-L62))

```java
static class MyQueue {
    private final Deque<Integer> in = new ArrayDeque<>();
    private final Deque<Integer> out = new ArrayDeque<>();

    public void push(int x) { in.push(x); }

    public int pop() {
        moveIfNeeded();
        return out.pop();
    }

    public int peek() {
        moveIfNeeded();
        return out.peek();
    }

    public boolean empty() { return in.isEmpty() && out.isEmpty(); }

    private void moveIfNeeded() {
        if (out.isEmpty()) {
            while (!in.isEmpty()) out.push(in.pop());   // reverse the order: oldest on top
        }
    }
}
```

### Line by line

| Method | What it does |
|---|---|
| `push(x)` | always O(1): onto `in` |
| `pop()` | make sure `out` has the oldest element on top, then pop it |
| `peek()` | the same, without removing |
| `empty()` | the queue is empty only if **both** stacks are |
| `moveIfNeeded()` | only when `out` is empty: pour all of `in` into it (this reverses the order) |

### Dry run: `push 1, push 2, peek, pop, push 3, pop, pop, empty`

| operation | in (top on the right) | out (top on the right) | result |
|---|---|---|---|
| push 1 | 1 | | |
| push 2 | 1 2 | | |
| peek | → pour | 2 1 | **1** |
| pop | | 2 | **1** |
| push 3 | 3 | 2 | |
| pop | 3 | (pop 2) | **2** |
| pop | → pour (out was empty) → out = 3 → pop | | **3** |
| empty | | | **true** |

### Complexity
- **push O(1)**; **pop/peek amortised O(1)** (worst case O(n) for a single call); **space O(n)**.

---

## 6. Comparing the approaches

| | Eager push | Lazy two stacks |
|---|---|---|
| push | O(n) | **O(1)** |
| pop / peek | O(1) | **O(1) amortised** |
| Worst case of a single call | push O(n) | pop O(n) |

---

## 7. Edge cases and common mistakes

**Edge cases:** interleaved pushes and pops, peek right after a pour, a long run of pushes then pops.

**Mistakes:**
1. Pouring `in` into `out` while `out` isn't empty, which breaks the FIFO order.
2. Pouring back from `out` to `in` after every pop (correct, but O(n) per operation).
3. `empty()` checking only one stack.

---

## 8. Interview questions and answers

**Q1. Why is pop amortised O(1)?**
> Every element is moved from `in` to `out` exactly once over its lifetime. Over any sequence of n operations the total work is O(n), so each operation is O(1) on average.

**Q2. When would the amortised bound be a problem?**
> In real-time systems that need a worst-case bound per operation. One pop could suddenly cost O(n). (There are de-amortised designs that pour incrementally.)

**Q3. Why must you only pour into an empty `out`?**
> The elements already in `out` are older than everything in `in`, and they must be served first. Pouring would put newer elements on top of them.

### Follow-up questions

**F1. Implement a stack using queues (LC 225).** → [05-Queue E01](../../05-Queue/02-Easy/E01_ImplementStackUsingQueues.md)
> One queue: after each push, rotate the previous n − 1 elements behind the new one, so the newest is at the front (push O(n), pop O(1)).

**F2. A queue with O(1) getMin.**
> Two stacks that each track the min (a MinStack, [M01](../03-Medium/M01_MinStack.md)); the queue min = the min of both stacks' mins.

**F3. Functional/persistent queues.**
> The same two-list trick (front list + reversed back list) is how immutable queues are implemented in functional languages.

---

## 9. Running the file

```bash
java E02_ImplementQueueUsingStacks.java      # 6/6 tests passed (including 10,000 random ops)
```

**One-line summary:** *"Push onto `in`; pop from `out`, refilling it from `in` (which reverses the order) only when it's empty."*
