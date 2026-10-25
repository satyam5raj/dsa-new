# M09 · Generate Parentheses

| | |
|---|---|
| **LeetCode** | [#22 Generate Parentheses](https://leetcode.com/problems/generate-parentheses/) |
| **Difficulty** | Medium |
| **Pattern** | Build character by character, only making choices that **keep it valid** |
| **Code** | [M09_GenerateParentheses.java](M09_GenerateParentheses.java) |
| **Run** | `java M09_GenerateParentheses.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return all well-formed strings with n pairs of parentheses.

| n | answer |
|---|---|
| 1 | ["()"] |
| 3 | ["((()))", "(()())", "(())()", "()(())", "()()()"] |

---

## 2. How to think about it

### Brute force
Generate all 2²ⁿ strings of `(` and `)` and keep the valid ones. For n = 8 that's 65,536 strings for only 1,430 answers.

### Backtracking with constraints
Track `open` (how many `(` are used) and `close` (how many `)`):

```
add '(' if open < n          there are still some to place
add ')' if close < open      only close something that's open
length 2n → record
```

Every partial string we build can still be completed, so **every leaf is an answer**: no branch is ever wasted. The number of answers is the n-th **Catalan number**: 1, 1, 2, 5, 14, 42, 132, 429, 1430, 4862, …

### The tree for n = 2

```
                 ""
                 |              (only '(' is allowed: close < open fails)
                "("
             /       \
          "(("       "()"
            |          |
         "(()"       "()("
            |          |
        "(())" ✓    "()()" ✓
```

### Dry run: n = 2

| string | open | close | can add `(`? | can add `)`? |
|---|---|---|---|---|
| "" | 0 | 0 | yes | no (0 < 0 is false) |
| "(" | 1 | 0 | yes | yes |
| "((" | 2 | 0 | no (open == n) | yes |
| "(()" | 2 | 1 | no | yes |
| "(())" | 2 | 2 | length 4 → record ✓ | |
| "()" | 1 | 1 | yes | no |
| "()(" | 2 | 1 | no | yes |
| "()()" | 2 | 2 | record ✓ | |

---

## 3. Java concepts used in this problem

### 3.1 Two independent `if`s instead of a loop

```java
if (open < n)     { sb.append('(');  bt(...);  sb.deleteCharAt(...); }
if (close < open) { sb.append(')');  bt(...);  sb.deleteCharAt(...); }
```

There are only two possible choices, so they're written out directly. Each `if` is a separate branch; both can run at the same node.

### 3.2 Why the output is sorted

The `(` branch is always tried before the `)` branch, and `'('` (40) < `')'` (41) in character codes. So the results come out in lexicographic order, which is why the test can compare them directly with the brute force's list.

### 3.3 Reading a bitmask from the top bit (in the referee)

```java
for (int i = 2 * n - 1; i >= 0; i--) {
    boolean openP = (mask >> i & 1) == 0;
    ...
}
```

Each mask from 0 to 2²ⁿ − 1 is one string: a 0 bit means `(` and a 1 bit means `)`. Reading from the **highest** bit down makes the strings come out in lexicographic order too, since increasing masks give increasing strings.

### 3.4 The balance check

`bal += openP ? 1 : -1;` tracks open minus close. A string is valid exactly when `bal` never goes negative and ends at 0.

---

## 4. Approach 1: All strings, filtered (the referee)

### Code ([M09_GenerateParentheses.java:54-69](M09_GenerateParentheses.java#L54-L69))

```java
static List<String> brute(int n) {
    List<String> out = new ArrayList<>();
    for (int mask = 0; mask < (1 << 2 * n); mask++) {
        StringBuilder sb = new StringBuilder();
        int bal = 0;
        boolean ok = true;
        for (int i = 2 * n - 1; i >= 0; i--) {
            boolean openP = (mask >> i & 1) == 0;
            sb.append(openP ? '(' : ')');
            bal += openP ? 1 : -1;
            if (bal < 0) { ok = false; break; }
        }
        if (ok && bal == 0) out.add(sb.toString());
    }
    return out;
}
```

`1 << 2 * n` is `1 << (2 * n)`, because `*` binds tighter than `<<`.

- **Time O(2²ⁿ · n) = O(4ⁿ · n).**

---

## 5. Approach 2: Backtracking with open/close counts (the file's main solution)

### Code ([M09_GenerateParentheses.java:34-52](M09_GenerateParentheses.java#L34-L52))

```java
static List<String> generateParenthesis(int n) {
    List<String> out = new ArrayList<>();
    bt(n, 0, 0, new StringBuilder(), out);
    return out;
}

static void bt(int n, int open, int close, StringBuilder sb, List<String> out) {
    if (sb.length() == 2 * n) { out.add(sb.toString()); return; }
    if (open < n) {
        sb.append('(');
        bt(n, open + 1, close, sb, out);
        sb.deleteCharAt(sb.length() - 1);
    }
    if (close < open) {
        sb.append(')');
        bt(n, open, close + 1, sb, out);
        sb.deleteCharAt(sb.length() - 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `sb.length() == 2 * n` | all n pairs are placed, and it's valid by construction |
| `open < n` | another `(` is still available |
| `close < open` | there's an unmatched `(` to close |
| append / recurse / delete | choose, explore, un-choose |

### Complexity
- **Time O(4ⁿ / √n):** the Catalan number Cₙ ≈ 4ⁿ / (n^1.5 √π) results, each of length 2n.
- **Space O(n)** recursion depth and builder.

---

## 6. Comparing the approaches

| | All strings + filter | Constrained backtracking |
|---|---|---|
| Strings examined for n = 8 | 65,536 | 1,430 (only answers) |
| Wasted branches | most of them | none |

The file checks for n = 1..9 that the count is the Catalan number and the list equals the filtered brute force.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1, larger n (the output grows like 4ⁿ).

**Mistakes:**
1. `close < n` instead of `close < open` (it creates invalid strings like `)(`).
2. `else if` for the second branch (it loses all the strings where both choices were possible).
3. Building strings with `+` at every node (works, but creates many temporary strings).

---

## 8. Interview questions and answers

**Q1. Why is every leaf valid?**
> The constraints keep the string a valid prefix at all times (never more `)` than `(`), and `open < n` guarantees we never use too many `(`. A valid prefix of length 2n with n of each is a complete valid string.

**Q2. How many results are there?**
> The n-th Catalan number, C(2n, n) / (n + 1), which grows like 4ⁿ / n^1.5.

### Follow-up questions

**F1. Valid Parentheses (checking, not generating).** → [Stack E01](../../04-Stack/02-Easy/E01_ValidParentheses.md)

**F2. Remove Invalid Parentheses (LC 301).**
> BFS by the number of removals, or backtracking that removes the minimum number of `(` and `)` with duplicate skipping.

**F3. Several kinds of brackets.**
> Keep a stack of the open bracket types; a closing bracket must match the top.

---

## 9. Running the file

```bash
java M09_GenerateParentheses.java      # 3/3 tests passed (including n = 1..9: Catalan counts, == filtered brute force)
```

**One-line summary:** *"Build the string one character at a time, adding '(' while open < n and ')' while close < open, so every completed string of length 2n is valid."*
