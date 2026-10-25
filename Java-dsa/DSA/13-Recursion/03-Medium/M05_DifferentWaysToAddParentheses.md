# M05 · Different Ways to Add Parentheses

| | |
|---|---|
| **LeetCode** | [#241 Different Ways to Add Parentheses](https://leetcode.com/problems/different-ways-to-add-parentheses/) |
| **Difficulty** | Medium |
| **Pattern** | Divide and conquer at **every operator**, plus memoization |
| **Code** | [M05_DifferentWaysToAddParentheses.java](M05_DifferentWaysToAddParentheses.java) |
| **Run** | `java M05_DifferentWaysToAddParentheses.java` |

Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

An expression of non-negative numbers and the operators `+`, `-`, `*`. Return **all** the results of all possible ways to fully parenthesize it, in any order, keeping duplicates.

| expression | results | why |
|---|---|---|
| "2-1-1" | [0, 2] | ((2−1)−1) = 0, (2−(1−1)) = 2 |
| "2*3-4*5" | [−34, −14, −10, −10, 10] | five parenthesizations |
| "11" | [11] | just a number |

---

## 2. How to think about it

### Some operator is applied last
In any full parenthesization, exactly one operator is evaluated **last**. Everything to its left is one sub-expression and everything to its right is another. So try every operator as the last one:

```
ways(expr):
    if expr is just a number → [number]
    for each operator at position i:
        left  = ways(expr[0..i))        all values of the left side
        right = ways(expr(i..])         all values of the right side
        for each l in left, r in right:  add (l op r)
```

Divide at the operator, conquer both sides, and **combine** by trying every pair.

### Memoization
The same substring shows up under many different splits. For "2*3-4*5", "3-4" is needed both inside "3-4*5" and inside "2*3-4". Caching substring → list of results avoids recomputing it. The number of results is a Catalan number (1, 1, 2, 5, 14, 42, …), so the **output** itself can be exponential; memoization just avoids doing that work more than once.

### Dry run: "2-1-1"

| last operator | left | right | results |
|---|---|---|---|
| the first `-` | ways("2") = [2] | ways("1-1") = [0] | 2 − 0 = **2** |
| the second `-` | ways("2-1") = [1] | ways("1") = [1] | 1 − 1 = **0** |

Result in the code's order: [2, 0].

### Dry run: "2*3-4*5"

| last operator | left values | right values | results |
|---|---|---|---|
| `*` (position 1) | [2] | ways("3-4*5") = [3 − 20, (3−4)·5] = [−17, −5] | −34, −10 |
| `-` (position 3) | ways("2*3") = [6] | ways("4*5") = [20] | −14 |
| `*` (position 5) | ways("2*3-4") = [2·(3−4), 6 − 4] = [−2, 2] | [5] | −10, 10 |

Result: [−34, −10, −14, −10, 10], which sorted is [−34, −14, −10, −10, 10] ✓. The memo returns "3-4" the second time it's needed.

---

## 3. Java concepts used in this problem

### 3.1 `substring(begin, end)` and `substring(begin)`

```java
expr.substring(0, i)        // characters 0..i−1 (end is exclusive)
expr.substring(i + 1)       // from i+1 to the end
```

Each call creates a new `String`. That's acceptable here because the inputs are short.

### 3.2 A `HashMap<String, List<Integer>>` memo

Strings make good keys: `equals` and `hashCode` compare the contents. The memo is created once in the wrapper and passed down, so every call shares it.

### 3.3 A chained ternary

```java
c == '+' ? l + r : c == '-' ? l - r : l * r
```

It reads as "if `+` then add, else if `-` then subtract, else multiply". It's fine for three cases; for more, use a `switch`.

### 3.4 "No operator found" as the base case

```java
if (result.isEmpty()) result.add(Integer.parseInt(expr));
```

Instead of checking for a number up front, the loop simply finds no operator, so `result` stays empty, and the whole string is parsed as a number. `Integer.parseInt("11")` handles multi-digit numbers.

### 3.5 `List.of(x)`

`List.of(nums.get(lo))` (in the referee) makes an immutable one-element list. It's fine to read from, but `add` would throw.

---

## 4. Approach 1: Enumerate the expression trees without memo (the referee)

### Code ([M05_DifferentWaysToAddParentheses.java:61-71](M05_DifferentWaysToAddParentheses.java#L61-L71))

```java
static List<Integer> brute(List<Integer> nums, List<Character> ops, int lo, int hi) {
    if (lo == hi) return List.of(nums.get(lo));
    List<Integer> out = new ArrayList<>();
    for (int k = lo; k < hi; k++) {
        for (int l : brute(nums, ops, lo, k)) for (int r : brute(nums, ops, k + 1, hi)) {
            char c = ops.get(k);
            out.add(c == '+' ? l + r : c == '-' ? l - r : l * r);
        }
    }
    return out;
}
```

| Line | What it does |
|---|---|
| `nums`, `ops` | the expression already split into tokens: n numbers and n − 1 operators |
| `lo == hi` | a single number |
| `k` | operator `ops[k]` joins numbers lo..k with numbers k+1..hi |
| the double loop | every pair of sub-results |

The same idea, on pre-parsed tokens and without a memo, so subexpressions are recomputed. It works as an independent check.

---

## 5. Approach 2: Split at every operator with memoization (the file's main solution)

### Code ([M05_DifferentWaysToAddParentheses.java:41-58](M05_DifferentWaysToAddParentheses.java#L41-L58))

```java
static List<Integer> diffWaysToCompute(String expression) {
    return ways(expression, new HashMap<>());
}

static List<Integer> ways(String expr, Map<String, List<Integer>> memo) {
    if (memo.containsKey(expr)) return memo.get(expr);
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < expr.length(); i++) {
        char c = expr.charAt(i);
        if (c != '+' && c != '-' && c != '*') continue;
        List<Integer> left = ways(expr.substring(0, i), memo);        // all values of the left part
        List<Integer> right = ways(expr.substring(i + 1), memo);      // all values of the right part
        for (int l : left) for (int r : right) result.add(c == '+' ? l + r : c == '-' ? l - r : l * r);
    }
    if (result.isEmpty()) result.add(Integer.parseInt(expr));          // no operator: a number
    memo.put(expr, result);
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| the memo check | this substring was already solved |
| the loop | try every operator position as the last operation |
| `continue` | digits aren't split points |
| `left`, `right` | every value each side can take |
| the double loop | combine every left value with every right value |
| the base case | no operator, so it's a single number |
| `memo.put` | remember the answer for this substring |

### Complexity
- **Time:** roughly O(Catalan(n) · n) for n operators, dominated by producing the output.
- **Space:** O(n²) memo keys (one per substring that starts and ends on a number), each holding its list of results.

---

## 6. Comparing the approaches

| | Tree enumeration (no memo) | Split + memo |
|---|---|---|
| Recomputes subexpressions? | yes | no |
| Input | pre-parsed tokens | the raw string |

The file compares the two (as sorted lists) on 300 random expressions with up to 6 numbers.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single number (including multi-digit), a single operator, repeated identical results (they must be kept).

**Mistakes:**
1. Treating each digit as a separate number (multi-digit numbers like "11").
2. Removing duplicate results (the problem keeps them).
3. Trying to evaluate with operator precedence (every grouping is considered, so precedence doesn't apply).
4. Creating the memo inside `ways` (then nothing is shared).

---

## 8. Interview questions and answers

**Q1. Why split at each operator?**
> The last operator applied splits the expression into two independent sub-expressions. Trying each one covers every parenthesization exactly once.

**Q2. How many results are there?**
> For n operators, Catalan(n) = (2n)! / ((n + 1)! · n!): the number of full binary trees with n + 1 leaves.

### Follow-up questions

**F1. Return the expressions as strings too.**
> Build "(" + l + op + r + ")" strings alongside the values.

**F2. Unique Binary Search Trees II (LC 95).**
> The same "choose the root, combine all left and right subtrees" recursion.

**F3. Burst Balloons / matrix-chain multiplication.**
> Interval DP with the same "choose the last operation in [i, j]" idea, but optimising a single value instead of listing every result.

---

## 9. Running the file

```bash
java M05_DifferentWaysToAddParentheses.java      # 4/4 tests passed (including 300 random expressions vs tree enumeration)
```

**One-line summary:** *"Try every operator as the one applied last, recursively get all values of its left and right sides, combine every pair, and memoize by substring."*
