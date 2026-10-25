# E01 · Implement Stack using Queues

| | |
|---|---|
| **LeetCode** | [#225 Implement Stack using Queues](https://leetcode.com/problems/implement-stack-using-queues/) |
| **Difficulty** | Easy |
| **Pattern** | Rotate the queue after each push (the mirror of [Stack E02](../../04-Stack/02-Easy/E02_ImplementQueueUsingStacks.md)) |
| **Code** | [E01_ImplementStackUsingQueues.java](E01_ImplementStackUsingQueues.java) |
| **Run** | `java E01_ImplementStackUsingQueues.java` |

---

## 1. The problem

Build a **LIFO stack** (`push`, `pop`, `top`, `empty`) using **only queue operations** (offer at the back, poll/peek at the front, size, isEmpty).

```
push(1), push(2)
top()   → 2
pop()   → 2
empty() → false
pop()   → 1
empty() → true
```

---

## 2. How to think about it

### The challenge
A queue hands back the **oldest** element; a stack must hand back the **newest**.

### One queue with a costly push (the cleanest version)
- `push(x)`: offer x at the back, then **rotate**: move the `size − 1` elements in front of it to the back, one by one. Now x is at the **front**.
- `pop` / `top`: just poll/peek the front → O(1).

```
push 1:  [1]
push 2:  offer → [1, 2],   rotate 1 → [2, 1]
push 3:  offer → [2, 1, 3], rotate 2 → [3, 2, 1]      front = 3 = the newest
```

The queue is always kept in **stack order** (newest at the front).

### Alternative: a cheap push and a costly pop
Push normally; for pop, move n − 1 elements to a second queue to reach the last one. Pick whichever operation is less frequent to be the expensive one.

### Compare with "Queue using Stacks"
That one is **amortised O(1)**, because reversing twice restores the order. Here, every push really costs O(n): you can't get an amortised O(1) stack out of plain queues, because a queue's order can't be reversed cheaply.

---

## 3. Java concepts used in this problem

This is the first queue problem, so here are the basics.

### 3.1 The `Queue` interface: FIFO

```java
Queue<Integer> q = new ArrayDeque<>();
q.offer(x);     // add at the back        (add(x) throws if a capacity-limited queue is full)
q.poll();       // remove the front       (returns null if empty; remove() throws)
q.peek();       // look at the front      (returns null if empty; element() throws)
q.size(); q.isEmpty();
```

| Operation | returns a special value | throws an exception |
|---|---|---|
| insert | `offer` (false) | `add` |
| remove | `poll` (null) | `remove` |
| examine | `peek` (null) | `element` |

Prefer the left column (`offer`/`poll`/`peek`) when "empty" is a normal situation.

### 3.2 `ArrayDeque` versus `LinkedList` as a queue

Both implement `Queue`. `ArrayDeque` (a circular array) is faster and uses less memory than `LinkedList` (a node object per element, with poor cache locality). Use `ArrayDeque` unless you need `null` elements (ArrayDeque forbids them) or `List` operations.

### 3.3 Declaring with the interface type

`Queue<Integer> q = new ArrayDeque<>()` restricts the variable to queue operations, which documents the intent: we promise to use it **only** as a queue (the problem's rule).

### 3.4 `q.offer(q.poll())`: rotating

Take the front out and put it at the back: one rotation step. Repeating it `size − 1` times brings the last element to the front.

---

## 4. Approach 1: One queue, rotate on push (the file's solution)

### Code ([E01_ImplementStackUsingQueues.java:38-49](E01_ImplementStackUsingQueues.java#L38-L49))

```java
static class MyStack {
    private final Queue<Integer> q = new ArrayDeque<>();

    public void push(int x) {
        q.offer(x);
        for (int i = 0; i < q.size() - 1; i++) q.offer(q.poll());   // rotate x to the front
    }

    public int pop() { return q.poll(); }
    public int top() { return q.peek(); }
    public boolean empty() { return q.isEmpty(); }
}
```

### Line by line

| Line | What it does |
|---|---|
| `q.offer(x);` | x goes to the back |
| `for (i < q.size() - 1) q.offer(q.poll());` | move every element that was before x to behind x; then x is at the front, followed by the old stack order. (`q.size()` doesn't change during the rotation: one poll and one offer per step) |
| `pop()` | the front is the newest → O(1) |
| `top()` | peek at the front |
| `empty()` | |

### Dry run: `push 1, push 2, push 3, pop, top`

| operation | queue (front → back) |
|---|---|
| push 1 | [1] (no rotation) |
| push 2 | [1, 2] → rotate 1 → [2, 1] |
| push 3 | [2, 1, 3] → rotate 2 → [1, 3, 2] → [3, 2, 1] |
| pop → **3** | [2, 1] |
| top → **2** | [2, 1] |

### Complexity
- **push O(n)**, **pop / top / empty O(1)**, **space O(n).**

---

## 5. Approach 2: Two queues, costly pop

```java
static class MyStackTwoQueues {
    private Queue<Integer> main = new ArrayDeque<>(), helper = new ArrayDeque<>();
    public void push(int x) { main.offer(x); }                    // O(1)
    public int pop() {
        while (main.size() > 1) helper.offer(main.poll());        // move all but the last
        int top = main.poll();                                    // the newest
        Queue<Integer> t = main; main = helper; helper = t;       // swap the roles
        return top;
    }
    public int top() {
        int x = pop();
        push(x);                                                  // put it back
        return x;
    }
    public boolean empty() { return main.isEmpty(); }
}
```

- `push` is O(1); `pop` and `top` are O(n).
- Swapping the two queue **references** avoids copying them back.

---

## 6. Comparing the approaches

| | Rotate on push | Two queues, costly pop |
|---|---|---|
| push | O(n) | **O(1)** |
| pop / top | **O(1)** | O(n) |
| Queues used | 1 | 2 |

---

## 7. Edge cases and common mistakes

**Edge cases:** a single element, alternating push and pop, many pushes followed by many pops.

**Mistakes:**
1. Rotating `size` times instead of `size − 1` (it puts x back at the end).
2. Using `Deque` stack methods (`push`/`pop`) on the ArrayDeque, which is cheating the problem's rule.
3. In the two-queue version, forgetting to swap the roles after a pop.

---

## 8. Interview questions and answers

**Q1. Why can't this be amortised O(1) like the queue-from-stacks?**
> Moving elements from one queue to another keeps their order, so you can't flip FIFO into LIFO in bulk. Each push (or pop) has to rotate past the existing elements.

**Q2. Which version would you choose?**
> It depends on the workload: rotate on push if pops and tops dominate; use the costly pop if pushes dominate.

### Follow-up questions

**F1. Implement a queue using stacks.** → [Stack E02](../../04-Stack/02-Easy/E02_ImplementQueueUsingStacks.md)
> Two stacks with lazy transfer: amortised O(1).

**F2. Implement a stack with only one queue and O(1) pop, where push can be slow.**
> That's exactly approach 1.

**F3. A deque (double-ended queue) from two stacks.**
> Two stacks holding the front and back halves; rebalance (split in half) when one side runs out. Amortised O(1).

---

## 9. Running the file

```bash
cd Java-dsa/DSA/05-Queue/02-Easy
java E01_ImplementStackUsingQueues.java      # 6/6 tests passed (including 3,000 random ops)
```

**One-line summary:** *"Push = offer, then rotate the size − 1 older elements behind it, so the newest is always at the front."*
