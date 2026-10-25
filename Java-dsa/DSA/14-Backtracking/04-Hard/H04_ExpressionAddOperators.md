# H04 · Expression Add Operators

| | |
|---|---|
| **LeetCode** | [#282 Expression Add Operators](https://leetcode.com/problems/expression-add-operators/) |
| **Difficulty** | Hard |
| **Pattern** | Backtracking over operand splits and operators, **evaluating on the fly** |
| **Code** | [H04_ExpressionAddOperators.java](H04_ExpressionAddOperators.java) |
| **Run** | `java H04_ExpressionAddOperators.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem); the partition template is in [M10](../03-Medium/M10_PalindromePartitioning.md).

---

## 1. The problem

Insert `+`, `-` or `*` between the digits of `num` (or nothing, which joins digits into one number) so that the expression equals `target`. Numbers can't have leading zeros. Return every such expression.

| num | target | answer |
|---|---|---|
| "123" | 6 | ["1*2*3", "1+2+3"] |
| "232" | 8 | ["2*3+2", "2+3*2"] |
| "105" | 5 | ["1*0+5", "10-5"] |
| "3456237490" | 9191 | [] |

---

## 2. How to think about it

### The choices
At each position, choose the next operand `num[pos..i]` (any length), and an operator in front of it. Building every string and evaluating it at the end is slow, and `*` precedence makes evaluation fiddly.

### Evaluate as you go: the `*` precedence trick
Carry two numbers down the recursion:
- `value`: the value of the expression so far;
- `last`: the value of the **last term** added, with its sign.

| operator | new value | new last |
|---|---|---|
| `+ x` | value + x | x |
| `- x` | value − x | −x |
| `* x` | value − last + last · x | last · x |

For `*`, **undo** the last term and add it back multiplied. That's exactly how precedence works: `*` binds to the last term only.

```
2 + 3         value 5, last 3
2 + 3 * 2     value 5 − 3 + 3·2 = 8, last 6     (= 2 + 6) ✓
2 * 3         value 2 − 2 + 2·3 = 6, last 6
2 * 3 + 2     value 6 + 2 = 8, last 2           ✓
```

### The rules
- An operand longer than one digit can't start with `0`, so once the first digit is `0`, **break** the length loop after the single-digit "0".
- Use `long`: a 10-digit operand overflows `int`.
- The first operand has no operator in front.

### Dry run: "105", target 5

| path | value | last | outcome |
|---|---|---|---|
| "1" | 1 | 1 | |
| "1+0" → "1+0+5" / "1+0-5" / "1+0*5" | 6 / −4 / 1 | | ✗ |
| "1-0" → … | −4 / 6 / 1 … | | ✗ |
| "1*0" | 1 − 1 + 0 = 0 | 0 | |
| "1*0+5" | 5 | 5 | **✓** |
| "1*05", "1+05", … | | | operand "05" → break |
| "10" | 10 | 10 | |
| "10-5" | 5 | −5 | **✓** |
| "105" | 105 | | ✗ |

---

## 3. Java concepts used in this problem

### 3.1 `StringBuilder.setLength` to undo

```java
int len = expr.length();             // remember the length before this level's changes
expr.append('+').append(operand);
bt(...);
expr.setLength(len);                 // cut back to where we were
```

Each choice appends an operator and a multi-character operand, so `deleteCharAt` isn't enough. `setLength(len)` truncates back to the saved length in one call.

### 3.2 `Long.parseLong` and `long` arithmetic

Operands can have up to 10 digits (up to 9,999,999,999), which doesn't fit in an `int`. The values `value` and `last` are `long` too, because products of operands get large.

### 3.3 `break` for leading zeros

```java
if (i > pos && num.charAt(pos) == '0') break;
```

If the operand starts with '0', only the single-digit operand "0" is allowed. Every longer operand from this `pos` would also start with '0', so `break` (not `continue`) is correct.

### 3.4 A nullable `Long` (in the referee)

`static Long eval(String e)` returns `null` for an expression with a leading-zero operand. The return type is the **wrapper** `Long`, because a primitive `long` can't be `null`. `v != null && v == target` checks for null first, then unboxes.

### 3.5 Base-4 codes for the gaps (in the referee)

`ops[c % 4]` and `c /= 4` pick one of {"", "+", "-", "*"} for each gap between digits, as in [M14](../03-Medium/M14_MatchsticksToSquare.md#33-base-4-counting-in-the-referee).

---

## 4. Approach 1: Every operator pattern, parsed (the referee)

### Code ([H04_ExpressionAddOperators.java:77-106](H04_ExpressionAddOperators.java#L77-L106))

```java
static Set<String> brute(String num, int target) {
    Set<String> out = new HashSet<>();
    int gaps = num.length() - 1;
    String[] ops = {"", "+", "-", "*"};
    for (int code = 0; code < Math.pow(4, gaps); code++) {
        StringBuilder sb = new StringBuilder().append(num.charAt(0));
        int c = code;
        for (int g = 0; g < gaps; g++) { sb.append(ops[c % 4]).append(num.charAt(g + 1)); c /= 4; }
        String e = sb.toString();
        Long v = eval(e);
        if (v != null && v == target) out.add(e);
    }
    return out;
}

static Long eval(String e) {                       // null if an operand has a leading zero
    long total = 0, term = 1;
    char sign = '+';
    int i = 0;
    while (i < e.length()) {
        int j = i;
        while (j < e.length() && Character.isDigit(e.charAt(j))) j++;
        String operand = e.substring(i, j);
        if (operand.length() > 1 && operand.charAt(0) == '0') return null;
        term *= Long.parseLong(operand);
        if (j == e.length() || e.charAt(j) != '*') { total += sign == '+' ? term : -term; term = 1; if (j < e.length()) sign = e.charAt(j); }
        i = j + 1;
    }
    return total;
}
```

| Part | What it does |
|---|---|
| the code loop | each of the 4^gaps ways to fill the gaps |
| `eval` | reads operands; multiplies them into `term` while the next operator is `*`; at `+`, `-` or the end, adds the signed term to `total` |
| `null` | rejects leading zeros |

- **Time O(4ⁿ · n).** It's slow, but it evaluates with an independent mini-parser, so it's a trustworthy check.

---

## 5. Approach 2: Backtracking with on-the-fly evaluation (the file's main solution)

### Code ([H04_ExpressionAddOperators.java:42-74](H04_ExpressionAddOperators.java#L42-L74))

```java
static List<String> addOperators(String num, int target) {
    List<String> out = new ArrayList<>();
    bt(num, target, 0, 0, 0, new StringBuilder(), out);
    return out;
}

static void bt(String num, int target, int pos, long value, long last, StringBuilder expr, List<String> out) {
    if (pos == num.length()) {
        if (value == target) out.add(expr.toString());
        return;
    }
    int len = expr.length();
    for (int i = pos; i < num.length(); i++) {
        if (i > pos && num.charAt(pos) == '0') break;               // no leading zeros
        String operand = num.substring(pos, i + 1);
        long x = Long.parseLong(operand);
        if (pos == 0) {
            expr.append(operand);
            bt(num, target, i + 1, x, x, expr, out);                  // first operand: no operator
            expr.setLength(len);
            continue;
        }
        expr.append('+').append(operand);
        bt(num, target, i + 1, value + x, x, expr, out);
        expr.setLength(len);
        expr.append('-').append(operand);
        bt(num, target, i + 1, value - x, -x, expr, out);
        expr.setLength(len);
        expr.append('*').append(operand);
        bt(num, target, i + 1, value - last + last * x, last * x, expr, out);   // undo last term, multiply it
        expr.setLength(len);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `pos == num.length()` | all digits are used: keep the expression if it hits the target |
| `len` | the builder's length before this level, for undoing |
| the leading-zero `break` | only "0" itself may start with '0' |
| `operand`, `x` | the next number, num[pos..i] |
| `pos == 0` | the first operand starts the expression with value = last = x |
| the three branches | `+`, `-` and `*`, each with the value/last update from section 2 |
| `setLength(len)` | undo each branch's text |

### Complexity
- **Time O(4ⁿ · n):** each of the n − 1 gaps has 4 choices, and building each answer costs O(n).
- **Space O(n)** recursion depth and builder.

---

## 6. Comparing the approaches

| | All patterns + parse | On-the-fly backtracking |
|---|---|---|
| Time | O(4ⁿ · n) | O(4ⁿ · n), with a smaller constant |
| Evaluation | parse every full string | O(1) per step |
| Leading zeros | rejected after building | never built |

The file compares the two on 300 random digit strings (with extra zeros) and random targets from −20 to 20.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single digit, zeros (like "00" → "0+0", "0-0", "0*0" for target 0), large operands that need `long`, a negative target.

**Mistakes:**
1. Getting `*` precedence wrong (for example, using `value * x`).
2. Allowing operands like "05".
3. Using `int` (overflow on 10-digit operands).
4. Adding an operator before the first operand.
5. Undoing with `deleteCharAt` (the appended text is longer than one character).

---

## 8. Interview questions and answers

**Q1. Explain the `last` variable.**
> Multiplication only applies to the most recent term. Keeping that term's signed value lets us remove it from the total and add back its product with the new operand, which gives correct precedence without parsing.

**Q2. Why `break` and not `continue` on a leading zero?**
> Every longer operand starting at the same position also begins with '0', so none of them are valid.

### Follow-up questions

**F1. Add division.**
> Track `last` the same way, with `value − last + last / x`, and handle division by zero and truncation rules.

**F2. Only count the expressions.**
> The same recursion returning counts. Memoization is hard because (pos, value, last) has too many states.

**F3. Different Ways to Add Parentheses.** → [Recursion M05](../../13-Recursion/03-Medium/M05_DifferentWaysToAddParentheses.md)
> The operators are fixed and the grouping varies, instead of the other way around.

---

## 9. Running the file

```bash
java H04_ExpressionAddOperators.java      # 5/5 tests passed (including 300 random cases vs all 4^gaps expressions)
```

**One-line summary:** *"Choose each next operand (no leading zeros) and an operator, carrying the running value and the last signed term so that '*' can undo and re-multiply the last term; record the expressions that use every digit and hit the target."*
