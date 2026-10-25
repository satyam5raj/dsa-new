# E01 · Fibonacci Number ⭐

| | |
|---|---|
| **LeetCode** | [#509 Fibonacci Number](https://leetcode.com/problems/fibonacci-number/) |
| **Difficulty** | Easy |
| **Pattern** | Tree recursion → memoization → bottom-up (the road to dynamic programming) |
| **Code** | [E01_FibonacciNumber.java](E01_FibonacciNumber.java) |
| **Run** | `java E01_FibonacciNumber.java` |

⭐ = not in `leetcode.md`, added for a key technique. This is the first note of the Recursion topic, so section 3 covers **how recursion works in Java**: base cases, the call stack, stack overflow, and memoization. (Recursion on trees was introduced in [Trees E01](../../07-Trees/02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).)

---

## 1. The problem

F(0) = 0, F(1) = 1, and F(n) = F(n − 1) + F(n − 2). Return F(n).

| n | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 10 | 30 |
|---|---|---|---|---|---|---|---|---|---|
| F(n) | 0 | 1 | 1 | 2 | 3 | 5 | 8 | 55 | 832040 |

---

## 2. How to think about it

The file has **four versions**. It's the same evolution you'll go through for every DP problem in topic 15:

1. **Plain recursion:** translate the definition directly. Simple, but exponential.
2. **Memoization (top-down):** the same recursion, but store each answer the first time and reuse it. O(n).
3. **Tabulation (bottom-up):** fill `dp[0..n]` in order with a loop. No recursion. O(n).
4. **Two variables:** each step only needs the previous two values. O(1) space.

### Why is plain recursion so slow?
The call tree recomputes the same values again and again:

```
                 F(5)
              /        \
           F(4)        F(3)          ← F(3) is computed twice
          /    \       /   \
        F(3)  F(2)   F(2)  F(1)      ← F(2) three times
       /   \   / \   /  \
     F(2) F(1) ...
```

The number of calls grows like φⁿ (φ ≈ 1.618, the golden ratio). Counted by running it:

| n | plain calls | memoized calls |
|---|---|---|
| 5 | 15 | 9 |
| 10 | 177 | 19 |
| 20 | 21,891 | 39 |
| 30 | 2,692,537 | 59 |
| 35 | 29,860,703 | 69 |

With memoization each F(k) is computed once, so there are only 2n − 1 calls.

### Dry run: memoized F(5)
```
memo(5) → memo(4) → memo(3) → memo(2) → memo(1) = 1, memo(0) = 0 → cache[2] = 1
                              memo(3) then calls memo(1) = 1          → cache[3] = 2
                    memo(4) then calls memo(2): cached, 1             → cache[4] = 3
          memo(5) then calls memo(3): cached, 2                       → cache[5] = 5
```

### Dry run: two variables, n = 6

| i | next = prev + cur | prev | cur |
|---|---|---|---|
| start | | 0 | 1 |
| 2 | 1 | 1 | 1 |
| 3 | 2 | 1 | 2 |
| 4 | 3 | 2 | 3 |
| 5 | 5 | 3 | 5 |
| 6 | 8 | 5 | 8 |

Return `cur` = **8** ✓

---

## 3. Java concepts used in this problem

### 3.1 The two parts of every recursive method

```java
static int fibPlain(int n) {
    if (n <= 1) return n;                          // 1. BASE CASE: answer directly
    return fibPlain(n - 1) + fibPlain(n - 2);      // 2. RECURSIVE CASE: smaller inputs
}
```

- **Base case:** a small input you can answer without recursing. Without one, the method calls itself forever.
- **Recursive case:** call the method on **smaller** inputs and combine the results. Every call must move toward a base case.
- **The leap of faith:** when writing the recursive case, *assume* the recursive calls return the correct answers for the smaller inputs, and just think about how to combine them. You don't trace the whole tree in your head.

### 3.2 The call stack

Each call gets its own **stack frame** holding its parameters and local variables. A call waits (its frame stays on the stack) until the calls it made return:

```
fibPlain(3)                       stack (top at the bottom):
  calls fibPlain(2)                 fibPlain(3)
    calls fibPlain(1) → 1           fibPlain(3), fibPlain(2), fibPlain(1)
    calls fibPlain(0) → 0           fibPlain(3), fibPlain(2), fibPlain(0)
    returns 1                       fibPlain(3)
  calls fibPlain(1) → 1             fibPlain(3), fibPlain(1)
  returns 2
```

The stack is only as **deep** as the longest chain of waiting calls (n here), even though the **total** number of calls is exponential. So plain Fibonacci uses O(n) stack space and O(φⁿ) time.

### 3.3 `StackOverflowError`

The JVM's stack has a fixed size (by default it fits roughly 10,000 to 20,000 simple frames). Recursing deeper throws `java.lang.StackOverflowError`. That's why deep recursions (like a linked list of 10⁶ nodes) are written as loops in Java. You can enlarge it with `java -Xss64m ...` or by running the work in a `new Thread(null, task, "name", stackSize)`.

### 3.4 `Integer[]` as a memo: `null` means "not computed yet"

```
static int fibMemo(int n) { return memo(n, new Integer[n + 1]); }

if (cache[n] != null) return cache[n];        // already solved
```

An `int[]` starts filled with 0, but 0 can be a real answer (F(0) = 0). An `Integer[]` starts filled with `null`, which can't be confused with any answer. Returning `cache[n]` unboxes it to an `int`.

### 3.5 Assignment is an expression

```
return cache[n] = memo(n - 1, cache) + memo(n - 2, cache);
```

`cache[n] = value` stores the value **and** evaluates to it, so this line saves the answer and returns it in one step.

### 3.6 Integer overflow

F(46) = 1,836,311,903 is the largest Fibonacci number that fits in an `int` (max 2,147,483,647). F(47) overflows and wraps to a negative number. LeetCode limits n to 30, so `int` is fine here; use `long` up to F(92).

### 3.7 Timing with `System.nanoTime()` and `printf`

```
long t0 = System.nanoTime();
fibPlain(35);
long plain = (System.nanoTime() - t0) / 1_000_000;       // nanoseconds → milliseconds
System.out.printf("   F(35): plain recursion %d ms, memoized %d us%n", plain, memo);
```

`nanoTime()` is a high-resolution clock for measuring durations. `printf` fills each `%d` with the next argument, and `%n` is a newline. Underscores in `1_000_000` are only for readability.

---

## 4. Approach 1: Plain recursion

### Code ([E01_FibonacciNumber.java:39-42](E01_FibonacciNumber.java#L39-L42))

```java
static int fibPlain(int n) {
    if (n <= 1) return n;
    return fibPlain(n - 1) + fibPlain(n - 2);
}
```

| Line | What it does |
|---|---|
| `if (n <= 1) return n` | F(0) = 0 and F(1) = 1 in one line |
| the return | the definition, word for word |

- **Time O(φⁿ)** ≈ O(1.618ⁿ).
- **Space O(n)** for the stack depth.

---

## 5. Approach 2: Memoization (top-down)

### Code ([E01_FibonacciNumber.java:44-50](E01_FibonacciNumber.java#L44-L50))

```java
static int fibMemo(int n) { return memo(n, new Integer[n + 1]); }

static int memo(int n, Integer[] cache) {
    if (n <= 1) return n;
    if (cache[n] != null) return cache[n];                  // already solved
    return cache[n] = memo(n - 1, cache) + memo(n - 2, cache);
}
```

| Line | What it does |
|---|---|
| `fibMemo` | creates the cache once and starts the recursion |
| the base case | the same as before |
| the cache check | return a stored answer without recursing |
| the last line | compute, store and return |

The cache is passed as a parameter so that every call shares the **same** array.

- **Time O(n)**: each value is computed once.
- **Space O(n)** for the cache and the stack.

---

## 6. Approach 3: Tabulation (bottom-up)

### Code ([E01_FibonacciNumber.java:52-58](E01_FibonacciNumber.java#L52-L58))

```java
static int fibTable(int n) {
    if (n <= 1) return n;
    int[] dp = new int[n + 1];
    dp[1] = 1;
    for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];
    return dp[n];
}
```

| Line | What it does |
|---|---|
| `dp[i]` | F(i) |
| `dp[1] = 1` | `dp[0]` is already 0 |
| the loop | each value from the two before it, smallest first |

- **Time O(n), space O(n)**, and no recursion, so no stack-overflow risk.

---

## 7. Approach 4: Two variables (the file's main solution)

### Code ([E01_FibonacciNumber.java:60-65](E01_FibonacciNumber.java#L60-L65))

```java
static int fib(int n) {
    int prev = 0, cur = 1;
    if (n == 0) return 0;
    for (int i = 2; i <= n; i++) { int next = prev + cur; prev = cur; cur = next; }
    return cur;
}
```

| Line | What it does |
|---|---|
| `prev, cur` | F(i − 2) and F(i − 1); they start as F(0) and F(1) |
| `n == 0` | the only case where the answer isn't `cur` |
| the loop | slide the window forward by one |

For n = 1 the loop doesn't run and `cur = 1` is returned. The dry run is in section 2.

- **Time O(n), space O(1).**

---

## 8. Comparing the approaches

| | Plain | Memo | Table | Two variables |
|---|---|---|---|---|
| Time | O(φⁿ) | O(n) | O(n) | O(n) |
| Space | O(n) stack | O(n) | O(n) | **O(1)** |
| Calls for n = 35 | 29.9 million | 69 | none | none |

The file checks that all four agree for n = 0..30, and times F(35): plain recursion takes milliseconds while memoized takes microseconds.

There's an even faster O(log n) method: matrix exponentiation, or the fast-doubling formulas F(2k) = F(k)·(2F(k + 1) − F(k)) and F(2k + 1) = F(k)² + F(k + 1)².

---

## 9. Edge cases and common mistakes

**Edge cases:** n = 0, n = 1, large n (overflow above 46 for `int`).

**Mistakes:**
1. A base case of only `n == 1` (then F(0) recurses to F(−1) forever).
2. Memoizing with an `int[]` and "0 means not computed" when 0 is a real answer.
3. Creating a new cache inside each recursive call (then nothing is shared).
4. Presenting plain recursion as the final answer in an interview.

---

## 10. Interview questions and answers

**Q1. What's the time complexity of plain recursive Fibonacci, and why?**
> O(φⁿ): each call branches into two, and the subproblems overlap heavily, so the same values are recomputed exponentially many times.

**Q2. What's the difference between memoization and tabulation?**
> Memoization keeps the recursion and caches results (top-down, computes only what's needed). Tabulation fills a table in dependency order with a loop (bottom-up, no recursion overhead or stack limit).

**Q3. What makes a problem a good fit for DP?**
> Overlapping subproblems (the same subproblem is solved many times) and optimal substructure (the answer is built from answers to subproblems). Fibonacci has the first; optimisation problems usually have both.

### Follow-up questions

**F1. Climbing Stairs (LC 70).**
> Ways(n) = Ways(n − 1) + Ways(n − 2): Fibonacci in disguise.

**F2. Compute F(n) mod 10⁹ + 7 for n up to 10¹⁸.**
> Matrix exponentiation of [[1, 1], [1, 0]] with fast power: O(log n). Fast power is in [M01 Pow(x, n)](../03-Medium/M01_PowXN.md).

**F3. Tribonacci (LC 1137).**
> The same with three variables.

---

## 11. Running the file

```bash
java E01_FibonacciNumber.java      # 5/5 tests passed; also prints the F(35) timing of plain vs memoized
```

**One-line summary:** *"Plain recursion recomputes subproblems exponentially; memoize or build bottom-up to get O(n), and keep only the last two values for O(1) space."*
