# H01 · Parsing a Boolean Expression ⭐

| | |
|---|---|
| **LeetCode** | [#1106 Parsing a Boolean Expression](https://leetcode.com/problems/parsing-a-boolean-expression/) |
| **Difficulty** | Hard |
| **Pattern** | **Recursive descent parsing**: the grammar is recursive, so the parser is too |
| **Code** | [H01_ParsingABooleanExpression.java](H01_ParsingABooleanExpression.java) |
| **Run** | `java H01_ParsingABooleanExpression.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Evaluate a boolean expression built from:

| form | meaning |
|---|---|
| `t`, `f` | true, false |
| `!(e)` | NOT e |
| `&(e1,e2,...)` | AND of one or more expressions |
| `\|(e1,e2,...)` | OR of one or more expressions |

| expression | value |
|---|---|
| `&(\|(f))` | false |
| `\|(f,f,f,t)` | true |
| `!(&(f,t))` | true |
| `t` | true |

---

## 2. How to think about it

### The grammar is recursive
An expression contains expressions. So write **one** function, `parse()`, that reads exactly one expression starting at a shared position `pos`, advances `pos` past it, and returns its value:

```
c = s[pos++]
't' → true;  'f' → false
'!' → skip '(';  v = parse();  skip ')';  return !v
'&' or '|' → skip '(';  then repeatedly:
                 v = parse();  combine it into the result
                 if the next char is ','  → skip it and continue
                 if it's ')'              → skip it and stop
```

Each call consumes exactly its own sub-expression, so when it returns, `pos` is right after it and the caller continues from there. This is **recursive descent**, the way compilers and JSON parsers are written. The Stack topic solved calculators with explicit stacks ([Basic Calculator](../../04-Stack/04-Hard/H03_BasicCalculator.md)); recursion uses the call stack instead.

### Note: no short-circuiting
The parser evaluates **all** children even after the answer is known (say, a `f` inside `&`), because it has to consume the input to find where the expression ends. Short-circuiting would need a separate "skip this sub-expression" routine that counts parentheses.

### Dry run: `!(&(f,t))`

```
index:  0 1 2 3 4 5 6 7 8
char:   ! ( & ( f , t ) )
```

| call | reads | pos after | action |
|---|---|---|---|
| parse #1 | `!` at 0 | 1 → skip `(` → 2 | NOT: call parse #2 |
| parse #2 | `&` at 2 | 3 → skip `(` → 4 | result = true (identity for AND) |
| parse #3 | `f` at 4 | 5 | returns false → result = false |
| back in #2 | `,` at 5 | 6 | continue |
| parse #4 | `t` at 6 | 7 | returns true → result = false & true = false |
| back in #2 | `)` at 7 | 8 | return **false** |
| back in #1 | skip `)` at 8 | 9 | return !false = **true** |

---

## 3. Java concepts used in this problem

### 3.1 A static field as the shared cursor

```java
static int pos;
```

Every recursive call reads and advances the **same** `pos`. A local variable wouldn't work, because each call would get its own copy and the caller wouldn't see how far its child read. (Alternatives: pass an `int[] pos` holder, or make the parser an object with a `pos` field.) The wrapper resets `pos = 0` before each new expression.

### 3.2 `charAt(pos++)`

It reads the character at `pos` and then advances `pos`: "consume one character".

### 3.3 Non-short-circuit `&` and `|` on booleans

```java
result = c == '&' ? result & v : result | v;
```

On `boolean`s, `&` and `|` are logical AND and OR that always evaluate both sides (unlike `&&` and `||`). Here both sides are plain variables, so either form gives the same value.

### 3.4 Identity elements

```java
boolean result = c == '&';      // AND starts at true, OR starts at false
```

`true` is the identity for AND (true AND x = x) and `false` is the identity for OR, so the loop can combine every child the same way, including the first.

### 3.5 `split`, `indexOf` and `lastIndexOf(ch, from)` (in the referee)

- `e.indexOf(')')`: the first `)`.
- `e.lastIndexOf('(', close)`: the last `(` at or before `close`, so the matching opener of the innermost group.
- `"f,t".split(",")`: `["f", "t"]`.

---

## 4. Approach 1: Rewrite the innermost group repeatedly (the referee)

### Code ([H01_ParsingABooleanExpression.java:67-79](H01_ParsingABooleanExpression.java#L67-L79))

```java
static boolean rewrite(String e) {
    while (e.length() > 1) {
        int close = e.indexOf(')'), open = e.lastIndexOf('(', close);
        char op = e.charAt(open - 1);
        String[] args = e.substring(open + 1, close).split(",");
        boolean v;
        if (op == '!') v = !args[0].equals("t");
        else if (op == '&') { v = true; for (String a : args) v &= a.equals("t"); }
        else { v = false; for (String a : args) v |= a.equals("t"); }
        e = e.substring(0, open - 1) + (v ? "t" : "f") + e.substring(close + 1);
    }
    return e.equals("t");
}
```

| Line | What it does |
|---|---|
| `close`, `open` | the first `)` and its matching `(`: an innermost group, whose arguments are all plain `t`/`f` |
| `op` | the operator just before `(` |
| `args` | the group's arguments |
| the three branches | evaluate NOT, AND or OR |
| the rebuild | replace `op(…)` with a single `t` or `f` |

**Dry run: `!(&(f,t))`** → the innermost group is `&(f,t)` = f → `!(f)` → t → **true**.

- **Time O(n²)**: up to n rewrites, each building a new string. Simple and independent, so it's a good referee.

---

## 5. Approach 2: Recursive descent (the file's main solution)

### Code ([H01_ParsingABooleanExpression.java:41-64](H01_ParsingABooleanExpression.java#L41-L64))

```java
static int pos;

static boolean parseBoolExpr(String expression) {
    pos = 0;
    return parse(expression);
}

static boolean parse(String s) {
    char c = s.charAt(pos++);
    if (c == 't') return true;
    if (c == 'f') return false;
    pos++;                                         // skip '('
    if (c == '!') {
        boolean v = parse(s);
        pos++;                                     // skip ')'
        return !v;
    }
    boolean result = c == '&';                     // identity: AND starts true, OR starts false
    while (true) {
        boolean v = parse(s);                      // one child expression
        result = c == '&' ? result & v : result | v;
        if (s.charAt(pos++) == ')') return result; // consumed ',' or ')'
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `pos = 0` | start a fresh parse |
| `c = s.charAt(pos++)` | the first character decides which kind of expression this is |
| `t` / `f` | a literal |
| `pos++` | every operator is followed by `(` |
| the `!` branch | parse exactly one child, skip `)`, negate |
| `result = c == '&'` | start from the identity |
| the loop | parse a child, combine it, then consume one separator |
| `== ')'` | the end of this expression's argument list |

The `while (true)` loop always ends through the `return`, because every `&(` or `|(` group in a valid expression is closed.

### Complexity
- **Time O(n):** every character is read once.
- **Space O(nesting depth)** for the recursion.

---

## 6. Comparing the approaches

| | Rewrite innermost | Recursive descent |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | new strings each round | O(depth) stack |
| Extends to real grammars? | no | yes |

The file checks the parser against the rewriter on 2,000 random expressions of depth up to 5.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single literal, deep nesting of `!`, an operator with one argument (`&(t)`), long argument lists.

**Mistakes:**
1. Using a local `pos` (the caller can't see how far the child read).
2. Forgetting to skip `(` or `)` (everything after shifts by one).
3. Short-circuiting the loop without consuming the remaining children.
4. Not resetting `pos` between calls.

---

## 8. Interview questions and answers

**Q1. What is recursive descent?**
> A parsing technique with one function per grammar rule. Each function consumes the input that matches its rule and calls the functions for the sub-rules, so nested structure maps directly onto nested calls.

**Q2. Could you solve this with a stack instead?**
> Yes: push characters until a `)` arrives, pop back to the matching `(`, evaluate the operator on the popped values, and push the result. That's the iterative version of the same idea.

**Q3. How would you short-circuit?**
> Once the AND result is false (or the OR result is true), call a `skip()` routine that advances `pos` past the rest of the arguments by counting parentheses, without evaluating them.

### Follow-up questions

**F1. Add operator precedence (an infix expression like `t&f|t`).**
> One function per precedence level: `parseOr` calls `parseAnd`, which calls `parseNot`, which calls `parseAtom`. This is the standard expression grammar.

**F2. Basic Calculator (LC 224).** → [Stack H03](../../04-Stack/04-Hard/H03_BasicCalculator.md)
> The same problem with numbers and `+`/`-`, solved with an explicit stack.

**F3. Decode String (LC 394).**
> Recursive descent again: `k[...]` contains strings that can contain more `k[...]`.

---

## 9. Running the file

```bash
java H01_ParsingABooleanExpression.java      # 5/5 tests passed (including 2,000 random expressions vs rewriting)
```

**One-line summary:** *"One recursive parse() reads exactly one expression from a shared cursor: a literal returns directly, ! wraps one child, and & or | fold their comma-separated children until the closing parenthesis."*
