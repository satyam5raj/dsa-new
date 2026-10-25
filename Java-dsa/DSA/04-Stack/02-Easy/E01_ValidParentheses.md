# E01 · Valid Parentheses

| | |
|---|---|
| **LeetCode** | [#20 Valid Parentheses](https://leetcode.com/problems/valid-parentheses/) |
| **Difficulty** | Easy |
| **Pattern** | Matching with a stack |
| **Code** | [E01_ValidParentheses.java](E01_ValidParentheses.java) |
| **Run** | `java E01_ValidParentheses.java` |

---

## 1. The problem

`s` contains only the characters `()[]{}`. It's **valid** if every opener is closed by the **same type**, in the **correct order**.

| s | answer | why |
|---|---|---|
| `"()[]{}"` | true | three closed pairs |
| `"(]"` | false | the wrong type of closer |
| `"([)]"` | false | the wrong order: `)` closes while `[` is still open |
| `"{[]}"` | true | properly nested |
| `")"` | false | a closer with nothing open |
| `"(("` | false | left unclosed |

**Constraints:** `1 ≤ n ≤ 10⁴`.

---

## 2. How to think about it

### Why counting isn't enough
`"([)]"` has one of each bracket, so the counts balance, but the **order** is wrong. A closer must match the **most recently opened bracket that's still open**.

"The most recent unresolved thing" is exactly what a **stack's top** gives you. That's the signature of a stack problem.

### The algorithm
- **opener** → push the **expected closer** (a neat trick: afterwards we only compare characters)
- **closer** → the stack must be non-empty **and** its top must equal this closer (pop it)
- **at the end** → the stack must be **empty** (no unclosed openers left)

**Quick reject:** an odd-length string can't be balanced.

---

## 3. Java concepts used in this problem

This is the first stack problem, so here are the basics.

### 3.1 A stack: LIFO

A **stack** is a pile: you can only add to the **top** (`push`), look at the top (`peek`), or remove the top (`pop`). **Last in, first out.** All three are O(1).

```
push 1, push 2, push 3        top → [3]
                                     [2]
                                     [1]
pop → 3,  pop → 2,  pop → 1
```

### 3.2 `Deque<Character> stack = new ArrayDeque<>()`, not `java.util.Stack`

```java
Deque<Character> stack = new ArrayDeque<>();
stack.push(x);      // add on top
stack.peek();       // look at the top (null if empty)
stack.pop();        // remove and return the top (throws NoSuchElementException if empty)
stack.isEmpty();
```

- **Why not `java.util.Stack`?** It's an old class (from Java 1.0) that extends `Vector`: every method is **synchronised** (slower), and it exposes index access, which breaks the stack abstraction. The Java docs themselves recommend `Deque`/`ArrayDeque`.
- `ArrayDeque` is a resizable circular array, so push and pop are amortised O(1).
- It doesn't allow `null` elements (which also catches bugs).

### 3.3 `Character` versus `char` when comparing

```java
stack.pop() != c       // pop() returns a Character; c is a char → unboxed, compared by value ✓
```

When one side is a primitive, Java **unboxes** the wrapper, so this is a value comparison. Comparing two `Character` objects with `!=` would compare references: a classic bug (it works only for cached values ≤ 127).

### 3.4 The `if / else if` chain with a final combined condition

```java
else if (stack.isEmpty() || stack.pop() != c) return false;
```

Short-circuit `||`: if the stack is empty, `pop()` is **never called** (it would throw). This is the same guard-first pattern as everywhere else.

---

## 4. Approach 1: Repeatedly remove matched pairs (naive)

```java
static boolean isValidReplace(String s) {
    String prev;
    do {
        prev = s;
        s = s.replace("()", "").replace("[]", "").replace("{}", "");
    } while (!s.equals(prev));          // until nothing changes
    return s.isEmpty();
}
```

- `String.replace(target, replacement)` replaces **all** occurrences and returns a new string.
- Each pass removes the innermost pairs; a valid string eventually becomes empty.
- **O(n²) time** (up to n/2 passes, each O(n)). It's correct, but slow.

**Dry run** `"{[]}"` → remove `[]` → `"{}"` → remove `{}` → `""` → **true**.

---

## 5. Approach 2: Stack of expected closers (the file's solution)

### Code ([E01_ValidParentheses.java:37-47](E01_ValidParentheses.java#L37-L47))

```java
static boolean isValid(String s) {
    if (s.length() % 2 == 1) return false;             // quick reject
    Deque<Character> stack = new ArrayDeque<>();
    for (char c : s.toCharArray()) {
        if (c == '(') stack.push(')');
        else if (c == '[') stack.push(']');
        else if (c == '{') stack.push('}');
        else if (stack.isEmpty() || stack.pop() != c) return false;   // wrong or missing opener
    }
    return stack.isEmpty();
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (s.length() % 2 == 1) return false;` | an odd length can't pair up |
| `Deque<Character> stack = new ArrayDeque<>();` | the closers we're waiting for, the most recent on top |
| `if (c == '(') stack.push(')');` (and `[`, `{`) | an opener: remember which closer must come |
| `else if (stack.isEmpty() \|\| stack.pop() != c) return false;` | a closer: there must be an open bracket, and it must be the matching type |
| `return stack.isEmpty();` | anything left means unclosed openers |

### Dry run 1: `"([)]"`

| char | action | stack (top on the right) |
|---|---|---|
| `(` | push `)` | `)` |
| `[` | push `]` | `) ]` |
| `)` | pop `]` ≠ `)` → **false** | |

### Dry run 2: `"{[]}"`

| char | action | stack |
|---|---|---|
| `{` | push `}` | `}` |
| `[` | push `]` | `} ]` |
| `]` | pop `]` = `]` ✓ | `}` |
| `}` | pop `}` = `}` ✓ | (empty) |

The stack is empty → **true** ✓

### Dry run 3: `"(("`
Push `)`, push `)`. The end comes with 2 items left → **false** ✓

### Complexity
- **Time O(n)**, **Space O(n)** (all openers, in the worst case).

---

## 6. Comparing the approaches

| | Repeated replace | Stack |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(n) (new strings) | O(n) |
| Early exit on an error | no | **yes** |

---

## 7. Edge cases and common mistakes

**Edge cases:** a closer first, only openers, odd length, deep nesting, an empty string (true).

**Mistakes:**
1. Only counting brackets (misses `"([)]"`).
2. Popping without checking `isEmpty()` → `NoSuchElementException` on `")"`.
3. Forgetting the final `isEmpty()` check (returns true for `"(("`).
4. Using `java.util.Stack` (works, but outdated and slower).
5. Comparing two `Character` objects with `==`.

---

## 8. Interview questions and answers

**Q1. Why a stack?**
> Every closer must match the most recently opened, still-unclosed bracket, and that's the top of a stack. Brackets close in reverse order of opening (LIFO).

**Q2. Why push the expected closer instead of the opener?**
> Then a closer only needs one comparison with the top, with no mapping from closer to opener.

**Q3. Can you do it in O(1) space?**
> With only **one** bracket type, yes: a counter (+1 for `(`, −1 for `)`, never negative, ending at 0). With several types you need the stack to remember the order of types.

### Follow-up questions

**F1. Minimum Remove to Make Valid Parentheses (LC 1249).**
> A stack of the **indices** of unmatched `(`; unmatched `)` are marked immediately; remove all the marked indices.

**F2. Longest Valid Parentheses (LC 32).** → [H04](../04-Hard/H04_LongestValidParentheses.md)
> A stack of indices with a sentinel −1; the length = i − the index on top after popping.

**F3. Generate all valid parentheses strings (LC 22).** → [14-Backtracking M09](../../14-Backtracking/03-Medium/M09_GenerateParentheses.md)
> Backtracking with open/close counts.

**F4. Score of Parentheses (LC 856).** → [M10](../03-Medium/M10_ScoreOfParentheses.md)
> A stack of partial scores, or a depth counter.

**F5. Check HTML/XML tags.**
> The same algorithm with tag names as the "bracket types".

---

## 9. Running the file

```bash
cd Java-dsa/DSA/04-Stack/02-Easy
java E01_ValidParentheses.java      # 6/6 tests passed
```

**One-line summary:** *"Push the expected closer for each opener; each closer must match the top; the stack must end empty."*
