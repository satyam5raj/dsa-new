# M01 · Min Stack

| | |
|---|---|
| **LeetCode** | [#155 Min Stack](https://leetcode.com/problems/min-stack/) |
| **Difficulty** | Medium |
| **Pattern** | Augmented stack (store the running minimum with each element) |
| **Code** | [M01_MinStack.java](M01_MinStack.java) |
| **Run** | `java M01_MinStack.java` |

---

## 1. The problem

Design a stack that supports `push`, `pop`, `top` **and** `getMin()`, **all in O(1)**.

```
push(-2), push(0), push(-3)
getMin() → -3
pop()
top()    → 0
getMin() → -2      ← it must "remember" the old minimum
```

---

## 2. How to think about it

### Why a single `min` variable isn't enough
`push 5`, `push 2` (min = 2), `pop` → what's the min now? We **forgot** that it was 5 before the 2 arrived. Recomputing it by scanning would be O(n).

### Key insight
A stack only changes **at the top**. So "the minimum of everything **below and including** this element" **never changes** while that element is in the stack. Store it **with** each element!

```
push 5 → [5, min 5]
push 2 → [2, min 2]
push 7 → [7, min 2]
getMin = 2;  pop (7);  pop (2);  getMin = 5   ← remembered
```

### Variant: two stacks
A separate `mins` stack only receives a value when it's **≤** the current minimum (this saves space when the minimum rarely changes). Pop from `mins` only when the popped value **equals** its top. Use `≤` so that duplicate minimums are tracked correctly.

---

## 3. Java concepts used in this problem

### 3.1 Storing pairs as `int[]` in a Deque

```java
Deque<int[]> stack = new ArrayDeque<>();
stack.push(new int[]{val, min});
stack.peek()[0]   // the value
stack.peek()[1]   // the minimum up to here
```

A small array acts as a tuple. (A `record Entry(int val, int min)` would be more readable.)

### 3.2 `.equals()` for `Integer` objects

```java
if (values.pop().equals(mins.peek())) mins.pop();     // both sides are Integer objects!
```

Here **both** sides are `Integer`. Using `==` would compare **references**. Java caches the Integers from −128 to 127, so `==` would *seem* to work in small tests and then fail for, say, 1000. That's one of the most famous Java bugs. Always use `.equals()` (or unbox one side) for wrapper objects.

### 3.3 Testing with `Collections.min`

The random test keeps an `ArrayList` mirror and checks `getMin()` against `Collections.min(ref)` (an O(n) scan, slow but obviously correct) after every operation.

---

## 4. Approach 1: Scan for the min (not O(1))

```java
static class MinStackScan {
    private final Deque<Integer> s = new ArrayDeque<>();
    public void push(int v) { s.push(v); }
    public void pop() { s.pop(); }
    public int top() { return s.peek(); }
    public int getMin() {
        int m = Integer.MAX_VALUE;
        for (int x : s) m = Math.min(m, x);   // O(n) every call
        return m;
    }
}
```

`getMin` is **O(n)**, which fails the requirement. It's here only for contrast.

---

## 5. Approach 2: A stack of [value, minSoFar] (the file's main solution)

### Code ([M01_MinStack.java:40-51](M01_MinStack.java#L40-L51))

```java
static class MinStack {
    private final Deque<int[]> stack = new ArrayDeque<>();     // [value, min of everything up to here]

    public void push(int val) {
        int min = stack.isEmpty() ? val : Math.min(val, stack.peek()[1]);
        stack.push(new int[]{val, min});
    }

    public void pop() { stack.pop(); }
    public int top() { return stack.peek()[0]; }
    public int getMin() { return stack.peek()[1]; }
}
```

### Line by line

| Line | What it does |
|---|---|
| `int min = stack.isEmpty() ? val : Math.min(val, stack.peek()[1]);` | the new element's "min so far" = the smaller of itself and the min below it |
| `stack.push(new int[]{val, min});` | store both together |
| `pop()` | removes the pair; the min below is restored automatically |
| `top()` | the value part of the top pair |
| `getMin()` | the min part of the top pair: O(1) |

### Dry run: `push -2, push 0, push -3, getMin, pop, top, getMin`

| operation | stack (top on the right) as [val, min] | result |
|---|---|---|
| push −2 | [−2, −2] | |
| push 0 | [−2, −2] [0, −2] | |
| push −3 | [−2, −2] [0, −2] [−3, −3] | |
| getMin | | **−3** |
| pop | [−2, −2] [0, −2] | |
| top | | **0** |
| getMin | | **−2** ✓ |

### Complexity
- **All operations O(1)**; **space O(n)** (two ints per element).

---

## 6. Approach 3: Two stacks (the file's second version)

### Code ([M01_MinStack.java:53-68](M01_MinStack.java#L53-L68))

```java
static class MinStackTwoStacks {
    private final Deque<Integer> values = new ArrayDeque<>();
    private final Deque<Integer> mins = new ArrayDeque<>();

    public void push(int val) {
        values.push(val);
        if (mins.isEmpty() || val <= mins.peek()) mins.push(val);   // <= keeps duplicate minimums
    }

    public void pop() {
        if (values.pop().equals(mins.peek())) mins.pop();          // equals: Integer objects!
    }

    public int top() { return values.peek(); }
    public int getMin() { return mins.peek(); }
}
```

### Line by line

| Line | What it does |
|---|---|
| `values.push(val);` | the normal stack |
| `if (mins.isEmpty() \|\| val <= mins.peek()) mins.push(val);` | record a new minimum (or a tie with the current one) |
| `if (values.pop().equals(mins.peek())) mins.pop();` | if we're removing the current minimum, drop it from `mins` too |
| `getMin()` | the top of `mins` |

### Why `<=` and not `<`?
`push 1, push 1, pop`: with `<`, `mins` would hold only one 1; popping the first 1 would also pop `mins`, leaving it **empty** although a 1 is still in the stack. With `<=`, both 1s are recorded.

### Dry run: `push 5, push 2, push 2, push 7, pop, pop, getMin`

| operation | values | mins | getMin |
|---|---|---|---|
| push 5 | 5 | 5 | |
| push 2 | 5 2 | 5 2 | |
| push 2 | 5 2 2 | 5 2 2 | |
| push 7 | 5 2 2 7 | 5 2 2 | |
| pop (7) | 5 2 2 | 5 2 2 (7 ≠ 2) | |
| pop (2) | 5 2 | 5 2 | |
| getMin | | | **2** ✓ (the other 2 is still there) |

**All O(1)**; the space is O(n) in the worst case (decreasing pushes), but often much less.

---

## 7. Comparing the approaches

| | Scan | Pairs | Two stacks |
|---|---|---|---|
| getMin | O(n) | **O(1)** | **O(1)** |
| Extra space | none | n extra ints | ≤ n extra (often far less) |
| Pitfall | — | none | `<=` and `.equals` |

---

## 8. Edge cases and common mistakes

**Edge cases:** duplicate minimums, negative numbers, values like `Integer.MIN_VALUE`, alternating push/pop.

**Mistakes:**
1. One global `min` variable (it can't be restored after a pop).
2. Two stacks with `<` instead of `<=` (duplicate minimums are lost).
3. Comparing `Integer`s with `==` (it breaks for values outside −128..127).

---

## 9. Interview questions and answers

**Q1. Why does storing the min with each element work?**
> Elements below the top never change while they're in the stack, so the minimum of the prefix up to each element is fixed. Popping simply reveals the previous prefix's min.

**Q2. What's the trade-off between the two designs?**
> Pairs: simple, always 2× memory. Two stacks: less memory when the minimum rarely changes, but you have to handle duplicates (`<=`) and use `.equals`.

**Q3. Can you do it with O(1) extra space?**
> There's a trick that stores `2·val − min` (an encoded difference) when a new minimum arrives, and decodes it on pop. It's clever, but risky with overflow (use long). Mention it as a curiosity.

### Follow-up questions

**F1. Max Stack with popMax (LC 716).** → [H01](../04-Hard/H01_MaxStack.md)
> popMax removes an arbitrary element, so a stack alone isn't enough: use a doubly linked list + a TreeMap from value to nodes.

**F2. A queue with getMin.**
> Two stacks with mins (like [E02](../02-Easy/E02_ImplementQueueUsingStacks.md)); the queue min = the min of both stacks' mins. Or a monotonic deque.

**F3. Get the min of a sliding window.** → [Arrays H02](../../01-Arrays/04-Hard/H02_SlidingWindowMaximum.md)
> A monotonic deque.

---

## 10. Running the file

```bash
java M01_MinStack.java      # 4/4 tests passed (including 10,000 random ops on both versions)
```

**One-line summary:** *"Store the minimum so far with each element; popping automatically restores the previous minimum."*
