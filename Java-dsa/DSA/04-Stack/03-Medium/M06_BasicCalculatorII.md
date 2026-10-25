# M06 · Basic Calculator II

| | |
|---|---|
| **LeetCode** | [#227 Basic Calculator II](https://leetcode.com/problems/basic-calculator-ii/) |
| **Difficulty** | Medium |
| **Pattern** | A stack of terms (apply `*` and `/` immediately, defer `+` and `−`) |
| **Code** | [M06_BasicCalculatorII.java](M06_BasicCalculatorII.java) |
| **Run** | `java M06_BasicCalculatorII.java` |

---

## 1. The problem

Evaluate a string containing non-negative integers, `+ - * /` and spaces (**no parentheses**). Respect the operator precedence; division truncates toward zero. Don't use `eval`.

| s | answer |
|---|---|
| `"3+2*2"` | 7 |
| `" 3/2 "` | 1 |
| `" 3+5 / 2 "` | 5 |
| `"14-3*2/4+100"` | 113 |
| `"42"` | 42 |

---

## 2. How to think about it

### The precedence problem
In `"3+2*2"`, we can't add 3 + 2 as soon as we see the `*`.

### The idea: split into terms
Split the expression into **terms** joined by `+` and `−`:

```
3 + 2*2 − 8/4   =   (+3) + (+2*2) + (−8/4)
```

The answer is just the **sum of the terms**. While scanning:

| operator **before** the number | action |
|---|---|
| `+` | push `+num` (a new term) |
| `−` | push `−num` (a new negative term) |
| `*` | pop the last term, push `term × num` |
| `/` | pop the last term, push `term / num` |

`*` and `/` attach to the **last** term immediately (higher precedence), while `+` and `−` just start new terms. At the end, sum the stack.

**Implementation detail:** we act on a number only when we reach the **next** operator (or the end of the string), using the operator we saw **before** it (`lastOp`, which starts as `+`).

---

## 3. Java concepts used in this problem

### 3.1 Parsing numbers digit by digit

```java
if (Character.isDigit(c)) num = num * 10 + (c - '0');
```

### 3.2 The "act at the next operator or the end" condition

```java
if ((!Character.isDigit(c) && c != ' ') || i == s.length() - 1)
```

- `!isDigit && c != ' '` → c is an operator.
- `i == length − 1` → the last character: process the final number even though no operator follows.
- Spaces are neither digits nor operators, so they're simply skipped.

### 3.3 `switch` on a `char` with arrows

```java
switch (lastOp) {
    case '+' -> terms.push(num);
    case '-' -> terms.push(-num);
    case '*' -> terms.push(terms.pop() * num);
    case '/' -> terms.push(terms.pop() / num);
}
```

### 3.4 Truncation with negative terms

The terms carry their sign, so `−3 * 2 / 4` is computed as `(−6) / 4 = −1` (truncation toward zero), which matches `−(6 / 4) = −1`. Signed terms keep the truncation correct.

---

## 4. Approach 1: A stack of signed terms (the file's main solution)

### Code ([M06_BasicCalculatorII.java:47-68](M06_BasicCalculatorII.java#L47-L68))

```java
static int calculate(String s) {
    Deque<Integer> terms = new ArrayDeque<>();
    int num = 0;
    char lastOp = '+';
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (Character.isDigit(c)) num = num * 10 + (c - '0');
        if ((!Character.isDigit(c) && c != ' ') || i == s.length() - 1) {   // an operator, or the end
            switch (lastOp) {
                case '+' -> terms.push(num);
                case '-' -> terms.push(-num);
                case '*' -> terms.push(terms.pop() * num);
                case '/' -> terms.push(terms.pop() / num);
            }
            lastOp = c;
            num = 0;
        }
    }
    int sum = 0;
    for (int t : terms) sum += t;
    return sum;
}
```

### Line by line

| Line | What it does |
|---|---|
| `char lastOp = '+';` | pretend there's a `+` before the first number |
| `num = num * 10 + (c - '0')` | build the current number |
| the trigger condition | at an operator or at the end, the current number is complete |
| `switch (lastOp)` | combine the number with the stack according to the operator **before** it |
| `lastOp = c; num = 0;` | remember the new operator and reset the number |
| `for (int t : terms) sum += t;` | add up all the terms |

### Dry run 1: `"3+2*2"`

| read | trigger at | lastOp | action | terms |
|---|---|---|---|---|
| 3 | `+` | + | push 3 | [3] |
| 2 | `*` | + | push 2 | [3, 2] |
| 2 | the end | * | pop 2, push 2 × 2 | [3, 4] |

Sum = **7** ✓

### Dry run 2: `"14-3*2/4+100"`

| number | lastOp | action | terms |
|---|---|---|---|
| 14 | + | push 14 | [14] |
| 3 | − | push −3 | [14, −3] |
| 2 | * | −3 × 2 = −6 | [14, −6] |
| 4 | / | −6 / 4 = −1 | [14, −1] |
| 100 | + | push 100 | [14, −1, 100] |

Sum = **113** ✓

### Complexity
- **Time O(n)**, **Space O(n)** for the stack.

---

## 5. Approach 2: O(1) space (the file's second method)

Only the **last** term is ever modified, so keep it in a variable (`last`) plus a running `result`:

### Code ([M06_BasicCalculatorII.java:70-90](M06_BasicCalculatorII.java#L70-L90))

```java
static int calculateConstantSpace(String s) {
    int result = 0, last = 0, num = 0;
    char lastOp = '+';
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (Character.isDigit(c)) num = num * 10 + (c - '0');
        if ((!Character.isDigit(c) && c != ' ') || i == s.length() - 1) {
            if (lastOp == '+' || lastOp == '-') {
                result += last;                          // the previous term is final now
                last = lastOp == '+' ? num : -num;
            } else if (lastOp == '*') {
                last *= num;
            } else {
                last /= num;
            }
            lastOp = c;
            num = 0;
        }
    }
    return result + last;
}
```

| Line | What it does |
|---|---|
| `result += last;` | a `+`/`−` starts a new term, so the previous term can't change anymore: bank it |
| `last = ±num` | the new term |
| `last *= num` / `last /= num` | extend the current term |
| `return result + last;` | bank the final term |

**Dry run** `"14-3*2/4+100"`: last = 14 → (−) result 14, last −3 → ×2 → −6 → /4 → −1 → (+) result 13, last 100 → return **113** ✓

**O(n) time, O(1) space.**

---

## 6. Comparing the approaches

| | Stack of terms | Running result + last term |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(n) | **O(1)** |
| Extends to parentheses | needs nesting → [H03](../04-Hard/H03_BasicCalculator.md) | the same |

---

## 7. Edge cases and common mistakes

**Edge cases:** spaces everywhere, a single number, multi-digit numbers, a division that truncates (`3/2`), several `*` and `/` in a row.

**Mistakes:**
1. Applying `+` immediately when it's followed by `*`.
2. Forgetting to process the last number (the "end" trigger).
3. Treating a space as an operator (it would reset `lastOp` incorrectly).
4. Truncation mistakes from applying the sign **after** dividing a positive term (signed terms avoid this).

---

## 8. Interview questions and answers

**Q1. How do you handle precedence without building a tree?**
> Treat the expression as a sum of terms. `*` and `/` bind to the last term immediately; `+` and `−` start new terms. At the end, sum everything.

**Q2. Why is O(1) space possible?**
> Only the most recent term can still change (through `*` or `/`); every earlier term is final, so it can be added to a running result.

### Follow-up questions

**F1. Add parentheses (Basic Calculator III, LC 772).**
> Recursion: on `(`, evaluate the inner expression recursively and treat its value as a number; or keep a stack of (result, last, lastOp) contexts.

**F2. Only + − and parentheses (LC 224).** → [H03](../04-Hard/H03_BasicCalculator.md)
> A sign stack: at `(` push the current sign context.

**F3. Arbitrary precedence levels and operators.**
> The shunting-yard algorithm → postfix → evaluate with [M04](M04_EvaluateReversePolishNotation.md).

---

## 9. Running the file

```bash
java M06_BasicCalculatorII.java      # 6/6 tests passed
```

**One-line summary:** *"Push ±num for + and −; fold * and / into the last term; sum the terms (or keep only `last` + `result`)."*
