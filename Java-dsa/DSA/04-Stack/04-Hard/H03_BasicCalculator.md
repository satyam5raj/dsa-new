# H03 · Basic Calculator ⭐

| | |
|---|---|
| **LeetCode** | [#224 Basic Calculator](https://leetcode.com/problems/basic-calculator/) |
| **Difficulty** | Hard |
| **Pattern** | A stack of saved (result, sign) pairs at each `(` |
| **Code** | [H03_BasicCalculator.java](H03_BasicCalculator.java) |
| **Run** | `java H03_BasicCalculator.java` |

⭐ = a bonus classic (parentheses + unary minus).

---

## 1. The problem

Evaluate a string containing integers, `+`, `-`, `(`, `)` and spaces. `-` can be **unary** (`"-(2+3)"`, `"-1"`). There's no `*` or `/`. Don't use `eval`.

| s | answer |
|---|---|
| `"1 + 1"` | 2 |
| `" 2-1 + 2 "` | 3 |
| `"(1+(4+5+2)-3)+(6+8)"` | 23 |
| `"-(2 + 3)"` | −5 |
| `"1-(-2)"` | 3 |
| `"- (3 + (4 + 5))"` | −12 |

**Constraints:** `1 ≤ n ≤ 3 × 10⁵`; the expression is valid; the intermediate results fit in an int.

---

## 2. How to think about it

### Without parentheses it's a running sum
`result += sign × number`, where the sign comes from the `+` or `−` in front of the number.

### Parentheses are a nested sub-problem (the same idea as [Decode String](../03-Medium/M05_DecodeString.md))
- On **`(`**: **save** the outer state (the result so far, and the sign in front of the parenthesis) on the stack, then start fresh: `result = 0`, `sign = +1`.
- On **`)`**: the inner result is finished. Restore:
  ```
  result = savedResult + savedSign × innerResult
  ```

**The unary minus needs no special case:** in `"-("`, the `−` just sets `sign = −1` before the `(` is saved. A leading `"-1"` works because `result` starts at 0, so it's `0 − 1`.

---

## 3. Java concepts used in this problem

### 3.1 Pushing two values per level onto one stack

```java
stack.push(result);   // pushed first → popped second
stack.push(sign);     // pushed second → popped first
...
int savedSign = stack.pop(), savedResult = stack.pop();
```

LIFO: whatever is pushed last comes out first, so pop in the **reverse** order of pushing. (A stack of `int[]{result, sign}` pairs would be equally valid and harder to mix up.)

### 3.2 The sign as `+1` / `−1`

Multiplying by `sign` instead of branching on `+`/`−` keeps the code uniform: `result += sign * num`.

### 3.3 "Finish the pending number" at every operator and at `)`

Numbers can have several digits, so a number is only complete when a non-digit arrives. Each `+`, `−`, `)` and the end of the string first add the pending `sign * num`.

---

## 4. Approach 1: Recursion (one call per parenthesis level)

```java
static int idx;
static int calculateRecursive(String s) { idx = 0; return eval(s); }

static int eval(String s) {                      // evaluates until the matching ')' or the end
    int result = 0, sign = 1, num = 0;
    while (idx < s.length()) {
        char c = s.charAt(idx++);
        if (Character.isDigit(c)) num = num * 10 + (c - '0');
        else if (c == '+' || c == '-') { result += sign * num; num = 0; sign = c == '+' ? 1 : -1; }
        else if (c == '(') num = eval(s);         // the inner value behaves like a number
        else if (c == ')') break;
    }
    return result + sign * num;
}
```

`(` recursively evaluates the inner expression and treats its value as a **number** (so the sign before the `(` applies naturally). The call stack does the saving. **O(n) time, O(depth) stack.**

---

## 5. Approach 2: An explicit stack of (result, sign) (the file's solution)

### Code ([H03_BasicCalculator.java:43-68](H03_BasicCalculator.java#L43-L68))

```java
static int calculate(String s) {
    Deque<Integer> stack = new ArrayDeque<>();
    int result = 0, sign = 1, num = 0;
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (Character.isDigit(c)) {
            num = num * 10 + (c - '0');
        } else if (c == '+' || c == '-') {
            result += sign * num;                  // finish the previous number
            num = 0;
            sign = c == '+' ? 1 : -1;
        } else if (c == '(') {
            stack.push(result);                    // save the outer state
            stack.push(sign);
            result = 0;
            sign = 1;
        } else if (c == ')') {
            result += sign * num;                  // finish the last number inside
            num = 0;
            int savedSign = stack.pop(), savedResult = stack.pop();
            result = savedResult + savedSign * result;
        }
        // spaces are ignored
    }
    return result + sign * num;
}
```

### Line by line

| Branch | What it does |
|---|---|
| digit | build the multi-digit number |
| `+` / `−` | add the finished number with its sign; remember the sign for the next number |
| `(` | save (result, sign) for the outer level; the inner level starts at 0 with sign + |
| `)` | finish the inner number, then fold the inner result into the outer one: `outer + signBefore × inner` |
| (space) | ignored |
| `return result + sign * num;` | the last number, if the string ends with one |

### Dry run: `"(1+(4+5+2)-3)+(6+8)"`

| char | result | sign | num | stack (top first) | note |
|---|---|---|---|---|---|
| ( | 0 | + | 0 | [+1, 0] | save (0, +) |
| 1 | 0 | + | 1 | | |
| + | 1 | + | 0 | | |
| ( | 0 | + | 0 | [+1, 1, +1, 0] | save (1, +) |
| 4 + 5 + 2 | 9 | + | 2 | | |
| ) | 1 + 1·12 = **12** | | 0 | [+1, 0] | the inner 12 |
| − 3 | 12 | − | 3 | | |
| ) | 0 + 1·(12 − 3) = **9** | | 0 | [] | |
| + | 9 | + | 0 | | |
| ( | 0 | + | 0 | [+1, 9] | save (9, +) |
| 6 + 8 | 6 | + | 8 | | |
| ) | 9 + 1·14 = **23** | | 0 | [] | |

**Return 23** ✓

### Dry run 2: `"-(2 + 3)"`
`−` → result 0, sign −1; `(` → save (0, −1); inside: 2 + 3 = 5; `)` → 0 + (−1)·5 = **−5** ✓

### Dry run 3: `"1-(-2)"`
1, `−` → result 1, sign −1; `(` → save (1, −1); inside: `−` → sign −1, then 2 → at `)`: inner = 0 + (−1)·2 = −2 → result = 1 + (−1)(−2) = **3** ✓

### Complexity
- **Time O(n)**, **Space O(nesting depth).**

---

## 6. Comparing the approaches

| | Recursion | Explicit stack |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(depth) call stack | O(depth) heap stack |
| Stack-overflow risk | for very deep nesting | **none** |

---

## 7. Edge cases and common mistakes

**Edge cases:** a leading unary minus, nested unary minuses (`"-(-(1))"`), multi-digit numbers, spaces everywhere, a single number.

**Mistakes:**
1. Popping the sign and result in the wrong order.
2. Forgetting to finish the pending number before `)` or at the end.
3. Resetting `sign` incorrectly after `)` (it's harmless here, since `num` is reset to 0, but be careful in variants).
4. Overflow: `result` stays within int by the problem's guarantee; otherwise use long.

---

## 8. Interview questions and answers

**Q1. Why push both the result and the sign at `(`?**
> The inner expression is evaluated from scratch; when it closes, we need the value accumulated **before** the parenthesis and the sign that applies to the whole group.

**Q2. How is the unary minus handled?**
> It's just a sign before the next operand (a number or a parenthesised group). Since `result` starts at 0, `−x` becomes `0 − x`.

### Follow-up questions

**F1. Basic Calculator III (LC 772): + − × / with parentheses.**
> Recursion per parenthesis level, and inside each level the "terms" method from [M06](../03-Medium/M06_BasicCalculatorII.md). Or use shunting-yard → postfix → [M04](../03-Medium/M04_EvaluateReversePolishNotation.md).

**F2. Expressions with variables (LC 770).**
> Polynomials as maps from term → coefficient, with the same parsing structure.

**F3. Validate the expression first.**
> Track the expected token types (operand versus operator) and the parenthesis balance ([E01](../02-Easy/E01_ValidParentheses.md)).

---

## 9. Running the file

```bash
java H03_BasicCalculator.java      # 7/7 tests passed
```

**One-line summary:** *"A running sum with a sign; at `(` push (result, sign) and restart; at `)` set result = savedResult + savedSign × inner."*
