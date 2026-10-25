# M09 · Remove K Digits

| | |
|---|---|
| **LeetCode** | [#402 Remove K Digits](https://leetcode.com/problems/remove-k-digits/) |
| **Difficulty** | Medium |
| **Pattern** | Greedy + monotonic (increasing) stack |
| **Code** | [M09_RemoveKDigits.java](M09_RemoveKDigits.java) |
| **Run** | `java M09_RemoveKDigits.java` |

---

## 1. The problem

Remove **exactly k digits** from the number string so the result is as **small** as possible. The answer has no leading zeros (`"0"` if nothing is left).

| num | k | answer | how |
|---|---|---|---|
| `"1432219"` | 3 | `"1219"` | remove 4, 3, 2 |
| `"10200"` | 1 | `"200"` | remove 1 → `"0200"` → strip the zero |
| `"10"` | 2 | `"0"` | everything removed |
| `"12345"` | 2 | `"123"` | already increasing → cut the end |

**Constraints:** `1 ≤ k ≤ n ≤ 10⁵`.

---

## 2. How to think about it

### Greedy insight
The **leftmost** digits matter most (they're the most significant). Scanning left to right, if a digit is **bigger than the digit after it**, removing it makes the number smaller: `"14…"` → `"1…"` with the 4 replaced by the smaller next digit. So we want the kept digits to be **as increasing as possible**, as early as possible.

### Monotonic increasing stack
```
for each digit d:
    while k > 0 and the stack's top > d:  pop (remove that bigger digit), k−−
    push d
if k is still > 0: remove from the END (the stack is increasing, so the biggest digits are at the end)
strip the leading zeros; empty → "0"
```

### Why is the greedy choice right?
Compare two numbers of the same length: the first position where they differ decides which is smaller. Removing a digit that's larger than its right neighbour makes the number smaller at the earliest possible position, and no other removal can achieve a smaller digit there. (The file also checks this against an exhaustive search on 400 random cases.)

---

## 3. Java concepts used in this problem

### 3.1 A `StringBuilder` as a stack of digits

`append` = push, `charAt(len − 1)` = peek, `deleteCharAt(len − 1)` = pop (O(1) at the end).

### 3.2 Comparing digit characters directly

```java
stack.charAt(stack.length() - 1) > d
```

`'0'..'9'` have increasing codes, so comparing the **characters** is the same as comparing the digits. No conversion is needed.

### 3.3 `setLength` to truncate

```java
stack.setLength(Math.max(0, stack.length() - k));   // drop the last k characters
```

`setLength(n)` cuts a StringBuilder to n characters (or pads with `'\0'` if n is larger). `Math.max(0, …)` protects against a negative length.

### 3.4 The brute force with bitmasks (in the test)

```java
for (int mask = 0; mask < (1 << n); mask++)
    if (Integer.bitCount(mask) != k) continue;     // the masks with exactly k bits = which positions to remove
```

- `1 << n` is 2ⁿ, the number of subsets of n positions.
- `Integer.bitCount(mask)` counts the 1-bits.
- `(mask & (1 << i)) == 0` → position i is kept.

This tries **every** way to remove k digits. It's exponential, but fine for n ≤ 10, and it gives a trustworthy reference.

---

## 4. Approach 1: Exhaustive search (the file's test reference)

### Code ([M09_RemoveKDigits.java:62-73](M09_RemoveKDigits.java#L62-L73))

```java
static String brute(String num, int k) {
    int n = num.length();
    java.math.BigInteger best = null;
    for (int mask = 0; mask < (1 << n); mask++) {
        if (Integer.bitCount(mask) != k) continue;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) if ((mask & (1 << i)) == 0) sb.append(num.charAt(i));
        java.math.BigInteger v = sb.length() == 0 ? java.math.BigInteger.ZERO : new java.math.BigInteger(sb.toString());
        if (best == null || v.compareTo(best) < 0) best = v;
    }
    return best.toString();
}
```

`BigInteger` handles both the leading zeros and arbitrary length when comparing. **O(2ⁿ · n)**: only for tiny inputs.

---

## 5. Approach 2: Remove one peak at a time (O(n · k))

```java
static String removeKdigitsSlow(String num, int k) {
    StringBuilder sb = new StringBuilder(num);
    for (int r = 0; r < k; r++) {
        int i = 0;
        while (i + 1 < sb.length() && sb.charAt(i) <= sb.charAt(i + 1)) i++;   // the first "peak"
        sb.deleteCharAt(i);                                                    // remove it
    }
    int z = 0;
    while (z < sb.length() - 1 && sb.charAt(z) == '0') z++;
    String res = sb.substring(z);
    return res.isEmpty() ? "0" : res;
}
```

The same greedy idea, done one removal at a time: find the first digit that's bigger than its successor (or the last digit) and delete it. **O(n · k)**. The stack version does all k removals in one pass.

---

## 6. Approach 3: Monotonic increasing stack (the file's solution)

### Code ([M09_RemoveKDigits.java:45-59](M09_RemoveKDigits.java#L45-L59))

```java
static String removeKdigits(String num, int k) {
    StringBuilder stack = new StringBuilder();
    for (char d : num.toCharArray()) {
        while (k > 0 && stack.length() > 0 && stack.charAt(stack.length() - 1) > d) {
            stack.deleteCharAt(stack.length() - 1);
            k--;
        }
        stack.append(d);
    }
    stack.setLength(Math.max(0, stack.length() - k));      // still need removals: cut from the end
    int i = 0;
    while (i < stack.length() - 1 && stack.charAt(i) == '0') i++;   // leading zeros (keep at least one digit)
    String result = stack.substring(i);
    return result.isEmpty() ? "0" : result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `while (k > 0 && ... top > d)` | a bigger digit sits before a smaller one → remove the bigger one (as long as removals remain) |
| `stack.append(d);` | keep d (for now) |
| `stack.setLength(... - k)` | if fewer than k removals happened, the stack is increasing, so the largest digits are at the end: cut them |
| the leading-zero loop | skip the zeros, but keep at least one character (`i < length − 1`) |
| `result.isEmpty() ? "0" : result` | everything removed → "0" |

### Dry run 1: `"1432219"`, k = 3

| digit | pops (k after) | stack |
|---|---|---|
| 1 | — | 1 |
| 4 | — | 14 |
| 3 | 4 (k = 2) | 13 |
| 2 | 3 (k = 1) | 12 |
| 2 | — | 122 |
| 1 | 2 (k = 0) | 121 |
| 9 | — (k = 0) | 1219 |

k = 0, so nothing more to cut; there are no leading zeros → **`"1219"`** ✓

### Dry run 2: `"10200"`, k = 1
1 → `1`; 0: 1 > 0 → pop (k = 0) → `0`; then 2, 0, 0 → `0200` → strip the leading zero → **`"200"`** ✓

### Dry run 3: `"12345"`, k = 2
It's increasing, so there are no pops → `12345` → cut 2 from the end → **`"123"`** ✓

### Complexity
- **Time O(n)**: each digit is pushed once and popped at most once.
- **Space O(n).**

---

## 7. Comparing the approaches

| | Exhaustive | Peak removal | Monotonic stack |
|---|---|---|---|
| Time | O(2ⁿ · n) | O(n · k) | **O(n)** |
| Space | O(n) | O(n) | O(n) |

---

## 8. Edge cases and common mistakes

**Edge cases:** k = n (answer "0"), an increasing number (cut from the end), many zeros after the removals, all digits equal.

**Mistakes:**
1. Forgetting to remove the remaining k from the **end**.
2. Stripping **all** zeros, including a single `"0"` result (leading to `""`).
3. Removing the smallest digits instead of the "peaks".
4. Using `int`/`long` conversion (n up to 10⁵ digits!).

---

## 9. Interview questions and answers

**Q1. Why is removing a bigger digit before a smaller one optimal?**
> The earliest position where two candidates differ decides the comparison. Removing a digit that's greater than its successor lowers the digit at that position, and no other single removal can produce a smaller digit there.

**Q2. Why cut from the end if k remains?**
> Then the stack is non-decreasing, so the largest digits are the last ones, and removing them hurts the least significant positions.

### Follow-up questions

**F1. Remove Duplicate Letters / Smallest Subsequence of Distinct Characters (LC 316 / 1081).**
> A monotonic stack with an extra condition: pop a bigger char only if it appears again later; skip chars already in the stack.

**F2. Create Maximum Number (LC 321).**
> For each split i + j = k, take the max subsequence of length i from nums1 and of length j from nums2 (a monotonic **decreasing** stack), then merge them greedily.

**F3. Build the largest number by removing k digits.**
> Mirror it: pop while the top is **smaller** than d (a decreasing stack).

---

## 10. Running the file

```bash
java M09_RemoveKDigits.java      # 5/5 tests passed (including 400 random cases vs exhaustive search)
```

**One-line summary:** *"Increasing stack: pop bigger digits while k > 0; cut the rest from the end; strip the leading zeros."*
