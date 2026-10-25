# E03 · Counting Bits ⭐

| | |
|---|---|
| **LeetCode** | [#338 Counting Bits](https://leetcode.com/problems/counting-bits/) |
| **Difficulty** | Easy |
| **Pattern** | DP where the "smaller subproblem" is a smaller **number** (`i >> 1`, `i & (i − 1)`) |
| **Code** | [E03_CountingBits.java](E03_CountingBits.java) |
| **Run** | `java E03_CountingBits.java` |

⭐ = not in `leetcode.md`, added for a key technique. The DP recipe is explained in [E01](E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

For every i from 0 to n, return the number of 1 bits in i.

| n | answer |
|---|---|
| 2 | [0, 1, 1] |
| 5 | [0, 1, 1, 2, 1, 2] |
| 0 | [0] |

---

## 2. How to think about it

### Brute force
Count the bits of each i separately: O(n log n).

### Every number is a smaller number plus at most one bit
And the smaller number's answer is already in the table.

| version | relation | transition |
|---|---|---|
| **shift** | i is (i >> 1) followed by its last bit | dp[i] = dp[i >> 1] + (i & 1) |
| **lowest bit** | i & (i − 1) is i with its lowest 1 bit removed | dp[i] = dp[i & (i − 1)] + 1 |

Both read a strictly **smaller** index, so a simple forward loop always has the input ready.

| step | shift version |
|---|---|
| **State** | dp[i] = the number of 1 bits in i |
| **Transition** | dp[i] = dp[i >> 1] + (i & 1) |
| **Base** | dp[0] = 0 |
| **Order** | i from 1 up to n |
| **Answer** | the whole array |

### Dry run: the shift version, i = 1..12 (from a real run)

| i | binary | i >> 1 | i & 1 | dp[i] |
|---|---|---|---|---|
| 1 | 1 | 0 | 1 | 0 + 1 = 1 |
| 2 | 10 | 1 | 0 | 1 + 0 = 1 |
| 3 | 11 | 1 | 1 | 1 + 1 = 2 |
| 4 | 100 | 2 | 0 | 1 |
| 5 | 101 | 2 | 1 | 1 + 1 = 2 |
| 6 | 110 | 3 | 0 | 2 + 0 = 2 |
| 7 | 111 | 3 | 1 | 3 |
| 8 | 1000 | 4 | 0 | 1 |
| 11 | 1011 | 5 | 1 | 2 + 1 = 3 |
| 12 | 1100 | 6 | 0 | 2 |

### The lowest-bit version for i = 12
12 = `1100`, 12 & 11 = `1100` & `1011` = `1000` = 8, so dp[12] = dp[8] + 1 = 1 + 1 = 2 ✓

---

## 3. Java concepts used in this problem

### 3.1 `>>` and `& 1`

- `i >> 1` shifts right by one bit: it drops the last bit (the same as `i / 2` for non-negative i).
- `i & 1` is the last bit itself (1 if i is odd).

### 3.2 `i & (i − 1)`

Subtracting 1 flips the lowest 1 bit and every 0 below it, so AND-ing with i clears exactly that lowest 1 bit. It was explained in [Recursion E03](../../13-Recursion/02-Easy/E03_PowerOfTwo.md#32-bitwise-and-).

### 3.3 Parentheses around `i & 1`

`dp[i >> 1] + (i & 1)` needs the parentheses: `+` binds tighter than `&`, so without them it would mean `(dp[i >> 1] + i) & 1`.

### 3.4 `Integer.bitCount` as the referee

The test compares every entry from 0 to 100,000 with `Integer.bitCount(i)`, the library popcount.

---

## 4. Approach 1: Drop the last bit (the file's main solution)

### Code ([E03_CountingBits.java:31-35](E03_CountingBits.java#L31-L35))

```java
static int[] countBits(int n) {
    int[] dp = new int[n + 1];
    for (int i = 1; i <= n; i++) dp[i] = dp[i >> 1] + (i & 1);     // drop the last bit, add it back
    return dp;
}
```

| Line | What it does |
|---|---|
| `new int[n + 1]` | dp[0] = 0 automatically |
| the loop | each value from a smaller one that's already computed |

- **Time O(n), space O(1)** besides the output.

---

## 5. Approach 2: Remove the lowest set bit

### Code ([E03_CountingBits.java:37-41](E03_CountingBits.java#L37-L41))

```java
static int[] countBitsLowbit(int n) {
    int[] dp = new int[n + 1];
    for (int i = 1; i <= n; i++) dp[i] = dp[i & (i - 1)] + 1;      // remove the lowest set bit
    return dp;
}
```

i & (i − 1) has exactly one fewer 1 bit than i, so its count plus one is i's count.

- **Time O(n), space O(1)** besides the output.

---

## 6. Comparing the approaches

| | Popcount each | Shift | Lowest bit |
|---|---|---|---|
| Time | O(n log n) | O(n) | O(n) |
| Reads | | dp[i / 2] | dp[i without its lowest bit] |

The file checks both against `Integer.bitCount` for every i up to 100,000.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 0 ([0]), powers of two (exactly one bit), 2ᵏ − 1 (all ones).

**Mistakes:**
1. Missing parentheses around `i & 1`.
2. Starting the loop at 0 with the lowest-bit version (0 & −1 = 0, so dp[0] would read itself and become 1).

---

## 8. Interview questions and answers

**Q1. What makes this a DP?**
> Each answer is built from the answer for a smaller number that's already in the table, so each value is computed once in O(1).

**Q2. Why must the subproblem index be smaller?**
> The table is filled from 0 upward, so only smaller indices are guaranteed to be ready.

### Follow-up questions

**F1. Another transition.**
> dp[i] = dp[i − highestPowerOfTwo(i)] + 1, tracking the current power of two as i grows.

**F2. Number of 1 Bits (LC 191) for a single number.**
> Kernighan's loop: `while (n != 0) { n &= n − 1; count++; }`.

**F3. Number of Digit One.** → [Recursion H03](../../13-Recursion/04-Hard/H03_NumberOfDigitOne.md)
> Counting digits across a range, without listing each number.

---

## 9. Running the file

```bash
java E03_CountingBits.java      # 4/4 tests passed (including 0..100000 vs Integer.bitCount)
```

**One-line summary:** *"dp[i] = dp[i >> 1] + (i & 1), or dp[i & (i − 1)] + 1: each count comes from a smaller number already in the table."*
