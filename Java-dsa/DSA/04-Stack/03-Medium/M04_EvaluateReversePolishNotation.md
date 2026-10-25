# M04 · Evaluate Reverse Polish Notation

| | |
|---|---|
| **LeetCode** | [#150 Evaluate Reverse Polish Notation](https://leetcode.com/problems/evaluate-reverse-polish-notation/) |
| **Difficulty** | Medium |
| **Pattern** | Operand stack (postfix evaluation) |
| **Code** | [M04_EvaluateReversePolishNotation.java](M04_EvaluateReversePolishNotation.java) |
| **Run** | `java M04_EvaluateReversePolishNotation.java` |

---

## 1. The problem

Evaluate an expression in **Reverse Polish (postfix) notation**. The operators are `+ - * /`, and division **truncates toward zero**.

| tokens | infix meaning | answer |
|---|---|---|
| `["2","1","+","3","*"]` | (2 + 1) × 3 | 9 |
| `["4","13","5","/","+"]` | 4 + 13 / 5 | 6 |
| `["10","6","9","3","+","-11","*","/","*","17","+","5","+"]` | 10 × (6 / ((9 + 3) × −11)) + 17 + 5 | 22 |
| `["7","2","-"]` | 7 − 2 | 5 |
| `["-7","2","/"]` | −7 / 2 | −3 |

---

## 2. How to think about it

### Postfix notation
The operator comes **after** its two operands. No parentheses and no precedence rules are needed: the order of the tokens says everything. (Old HP calculators work this way, and so does the JVM's bytecode: **the JVM is a stack machine**.)

### The algorithm
- **number** → push it
- **operator** → `b = pop()`, `a = pop()`, `push(a OP b)`
  - **The order matters for `−` and `/`:** b was pushed **last**, so it's the **right** operand.
- The final answer is the only value left on the stack.

---

## 3. Java concepts used in this problem

### 3.1 `switch` on a `String` with arrows

```java
switch (t) {
    case "+" -> stack.push(stack.pop() + stack.pop());
    case "-" -> { int b = stack.pop(), a = stack.pop(); stack.push(a - b); }
    default  -> stack.push(Integer.parseInt(t));
}
```

- You can `switch` on Strings (Java 7+), and the comparison uses `.equals`.
- The arrow form (`->`, Java 14+) has **no fall-through**, so you don't need `break`. Use a block `{ }` for several statements.
- `default` catches everything else: the numbers.

### 3.2 Why `+` and `*` can pop in either order

Addition and multiplication are **commutative** (`a + b = b + a`), so `stack.pop() + stack.pop()` is fine even though the left operand of `+` is popped first. For `−` and `/` you must name them explicitly: `b` first, then `a`.

### 3.3 Integer division truncates toward zero

```java
 7 / 2  ==  3
-7 / 2  == -3      // toward zero (not floor, which would be −4)
```

That's exactly what the problem requires. (Use `Math.floorDiv` if you ever need flooring.)

### 3.4 `Integer.parseInt` handles negative numbers

`"-11"` parses to −11. A token like `"-11"` isn't an operator, because the switch matches the **whole** string, and only the exact `"-"` is subtraction.

---

## 4. Approach 1: Operand stack (the file's solution)

### Code ([M04_EvaluateReversePolishNotation.java:40-52](M04_EvaluateReversePolishNotation.java#L40-L52))

```java
static int evalRPN(String[] tokens) {
    Deque<Integer> stack = new ArrayDeque<>();
    for (String t : tokens) {
        switch (t) {
            case "+" -> stack.push(stack.pop() + stack.pop());
            case "*" -> stack.push(stack.pop() * stack.pop());
            case "-" -> { int b = stack.pop(), a = stack.pop(); stack.push(a - b); }
            case "/" -> { int b = stack.pop(), a = stack.pop(); stack.push(a / b); }
            default -> stack.push(Integer.parseInt(t));          // handles "-11" (negative numbers) too
        }
    }
    return stack.pop();
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (String t : tokens)` | process the tokens left to right |
| `case "+"` / `case "*"` | pop two values, push their sum/product (the order doesn't matter) |
| `case "-"` / `case "/"` | b = the right operand (popped first), a = the left; push `a − b` / `a / b` |
| `default` | a number: push it |
| `return stack.pop();` | the result |

### Dry run 1: `["4","13","5","/","+"]`

| token | action | stack (top on the right) |
|---|---|---|
| 4 | push | 4 |
| 13 | push | 4 13 |
| 5 | push | 4 13 5 |
| / | b = 5, a = 13 → 13 / 5 = 2 | 4 2 |
| + | 2 + 4 = 6 | 6 |

**Return 6** ✓

### Dry run 2: the LeetCode example 3

| token | stack after |
|---|---|
| 10, 6, 9, 3 | 10 6 9 3 |
| + | 10 6 12 |
| −11 | 10 6 12 −11 |
| * | 10 6 −132 |
| / | 10 0 (6 / −132 = 0, truncated) |
| * | 0 |
| 17, + | 17 |
| 5, + | 22 |

**Return 22** ✓

### Complexity
- **Time O(n)**, **Space O(n).**

---

## 5. (Bonus) Approach 2: Recursion from the end

Read the tokens **backwards**: the last token is the root operator; its right operand is the expression just before it, and its left operand comes before that.

```java
static int pos;
static int evalRPNRecursive(String[] tokens) {
    pos = tokens.length - 1;
    return evalFrom(tokens);
}
static int evalFrom(String[] tokens) {
    String t = tokens[pos--];
    switch (t) {
        case "+": case "-": case "*": case "/": {
            int b = evalFrom(tokens);          // the right operand is evaluated first (it's closer to the end)
            int a = evalFrom(tokens);
            return switch (t) { case "+" -> a + b; case "-" -> a - b; case "*" -> a * b; default -> a / b; };
        }
        default: return Integer.parseInt(t);
    }
}
```

It shows that a postfix expression is a **tree** written in post-order. The space is the O(depth) call stack.

---

## 6. Comparing the approaches

| | Operand stack | Recursion from the end |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(n) stack | O(depth) call stack |
| Clarity | **best** | educational |

---

## 7. Edge cases and common mistakes

**Edge cases:** a single number, negative numbers, division producing 0, division with negatives.

**Mistakes:**
1. Swapping the operands of `−` or `/`.
2. Treating `"-11"` as an operator (checking `t.charAt(0) == '-'`).
3. Using floor division for negatives.
4. Using `java.util.Stack` (old and synchronised).

---

## 8. Interview questions and answers

**Q1. Why is postfix easy to evaluate?**
> When an operator arrives, its operands are always the two most recent values, which are the top of a stack. There's no need for precedence or parentheses.

**Q2. How do you convert infix to postfix?**
> The **shunting-yard** algorithm (Dijkstra): an operator stack; output the numbers directly; pop the operators with higher or equal precedence before pushing a new one; parentheses push and pop the groups.

### Follow-up questions

**F1. Basic Calculator II (LC 227): infix with + − × /.** → [M06](M06_BasicCalculatorII.md)
> One stack: apply × and / immediately, push ± as signed terms, and sum at the end.

**F2. Basic Calculator (LC 224): + − and parentheses.** → [H03](../04-Hard/H03_BasicCalculator.md)
> A stack that saves (the result so far, the sign) at each `(`.

**F3. Build an expression tree from postfix.**
> The same stack, pushing **nodes**: an operator pops two nodes as its children.

---

## 9. Running the file

```bash
java M04_EvaluateReversePolishNotation.java      # 5/5 tests passed
```

**One-line summary:** *"Numbers are pushed; an operator pops b then a and pushes a OP b."*
