# M03 · Decode Ways

| | |
|---|---|
| **LeetCode** | [#91 Decode Ways](https://leetcode.com/problems/decode-ways/) |
| **Difficulty** | Medium |
| **Pattern** | 1-D counting DP with **validity conditions** (Climbing Stairs with rules) |
| **Code** | [M03_DecodeWays.java](M03_DecodeWays.java) |
| **Run** | `java M03_DecodeWays.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

Letters are encoded as 'A' = 1, 'B' = 2, …, 'Z' = 26. Count the ways to decode a digit string.

| s | answer | decodings |
|---|---|---|
| "12" | 2 | "AB" (1, 2), "L" (12) |
| "226" | 3 | "BZ" (2, 26), "VF" (22, 6), "BBF" (2, 2, 6) |
| "06" | 0 | "06" isn't "6": no leading zeros |
| "10" | 1 | "J" |
| "100" | 0 | the second 0 can't be decoded |
| "11106" | 2 | (1, 1, 10, 6), (11, 10, 6) |

---

## 2. How to think about it

### Brute force
A recursion that tries 1 or 2 digits at each position: O(2ⁿ).

### Look at the last letter
The last letter used either **1 digit** or **2 digits**, like the last move in [Climbing Stairs](../02-Easy/E01_ClimbingStairs.md), except a step is only allowed if it forms a valid code:

| step | for this problem |
|---|---|
| **State** | dp[i] = the number of ways to decode the first i characters |
| **Transition** | dp[i] = (s[i−1] ≠ '0' ? dp[i−1] : 0) + (s[i−2..i−1] is 10..26 ? dp[i−2] : 0) |
| **Base** | dp[0] = 1 (the empty string has one decoding: nothing) |
| **Answer** | dp[n] |

**The zeros are the whole difficulty:** a '0' can never be decoded on its own; it can only be the second digit of 10 or 20. And a two-digit code must be 10..26, which rules out a leading 0 ("06").

### Dry run: "226"

| i | char | one digit? | two digits? | dp[i] |
|---|---|---|---|---|
| 0 | | | | 1 |
| 1 | '2' | yes → dp[0] = 1 | | 1 |
| 2 | '2' | yes → dp[1] = 1 | "22" → dp[0] = 1 | 2 |
| 3 | '6' | yes → dp[2] = 2 | "26" → dp[1] = 1 | **3** |

### Dry run: "11106"

| i | char | one digit | two digits | dp[i] |
|---|---|---|---|---|
| 0 | | | | 1 |
| 1 | '1' | dp[0] = 1 | | 1 |
| 2 | '1' | dp[1] = 1 | "11" → dp[0] = 1 | 2 |
| 3 | '1' | dp[2] = 2 | "11" → dp[1] = 1 | 3 |
| 4 | '0' | ✗ ('0' alone) | "10" → dp[2] = 2 | 2 |
| 5 | '6' | dp[4] = 2 | "06" ✗ | **2** |

---

## 3. Java concepts used in this problem

### 3.1 Turning two characters into a number

```java
int two = (s.charAt(i - 2) - '0') * 10 + (s.charAt(i - 1) - '0');
```

Each `charAt(...) − '0'` is a digit 0..9; the first is multiplied by 10. This avoids creating a substring and parsing it.

### 3.2 `10 <= two && two <= 26`

The lower bound 10 is what rejects a leading zero: "06" gives 6, which is below 10.

### 3.3 The initial `prev1` uses a ternary

```java
int prev2 = 1, prev1 = s.charAt(0) != '0' ? 1 : 0;      // dp[0], dp[1]
```

dp[1] is 1 unless the string starts with '0', in which case nothing can be decoded at all.

### 3.4 `Integer.parseInt(s.substring(i, i + 2))` (in the referee)

The referee does it the simple way: take the two-character substring and parse it. Since it only calls this when `s[i]` isn't '0', the parsed value is automatically ≥ 10.

---

## 4. Approach 1: Recursion over the string (the referee)

### Code ([M03_DecodeWays.java:52-58](M03_DecodeWays.java#L52-L58))

```java
static int brute(String s, int i) {
    if (i == s.length()) return 1;
    if (s.charAt(i) == '0') return 0;
    int ways = brute(s, i + 1);
    if (i + 1 < s.length() && Integer.parseInt(s.substring(i, i + 2)) <= 26) ways += brute(s, i + 2);
    return ways;
}
```

| Line | What it does |
|---|---|
| `i == s.length()` | the whole string was decoded: one way |
| `s.charAt(i) == '0'` | nothing can start with 0 |
| `brute(s, i + 1)` | decode one digit |
| the two-digit branch | if s[i..i+1] is ≤ 26 (and ≥ 10, since s[i] ≠ '0') |

- **Time O(2ⁿ).** Memoizing on i gives O(n).

---

## 5. Approach 2: Bottom-up with two variables (the file's main solution)

### Code ([M03_DecodeWays.java:39-50](M03_DecodeWays.java#L39-L50))

```java
static int numDecodings(String s) {
    int prev2 = 1, prev1 = s.charAt(0) != '0' ? 1 : 0;      // dp[0], dp[1]
    for (int i = 2; i <= s.length(); i++) {
        int cur = 0;
        if (s.charAt(i - 1) != '0') cur += prev1;              // last letter = 1 digit
        int two = (s.charAt(i - 2) - '0') * 10 + (s.charAt(i - 1) - '0');
        if (two >= 10 && two <= 26) cur += prev2;              // last letter = 2 digits (10..26: no leading 0)
        prev2 = prev1;
        prev1 = cur;
    }
    return prev1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `prev2, prev1` | dp[i − 2] and dp[i − 1] |
| the one-digit check | the last character alone is a letter (1..9) |
| `two` | the last two characters as a number |
| the two-digit check | 10..26 is a letter |
| the shift | move the window |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | Recursion | Two variables |
|---|---|---|
| Time | O(2ⁿ) | **O(n)** |
| Space | O(n) stack | O(1) |

The file checks the DP against the recursion on 1,000 random strings biased toward 0, 1 and 2 (the tricky digits).

---

## 7. Edge cases and common mistakes

**Edge cases:** a leading '0' (0 ways), "10" and "20" (1 way), "30" or "00" (0 ways), long runs of 1s and 2s (Fibonacci-many ways).

**Mistakes:**
1. Accepting "0" as a one-digit code.
2. Accepting "05" as a two-digit code (it's below 10).
3. dp[0] = 0 (then everything comes out 0).
4. Returning early when a '0' appears (it may still be valid as part of 10 or 20).

---

## 8. Interview questions and answers

**Q1. How is this related to Climbing Stairs?**
> It's the same recurrence (the last step is 1 or 2), but each kind of step is only allowed when the corresponding digits form a valid letter.

**Q2. Why is dp[0] = 1?**
> The empty prefix has exactly one decoding (the empty one). It makes the two-digit case at i = 2 count correctly: "12" gets dp[0] = 1 from decoding "12" as a single letter.

### Follow-up questions

**F1. Decode Ways II (with '*' wildcards).** → [H08](../04-Hard/H08_DecodeWaysII.md)
> The same DP, where each '*' contributes several possibilities, counted modulo 10⁹ + 7.

**F2. List all decodings.**
> Backtracking with the same two choices (exponential output).

**F3. Climbing Stairs.** → [E01](../02-Easy/E01_ClimbingStairs.md)

---

## 9. Running the file

```bash
java M03_DecodeWays.java      # 8/8 tests passed (including 1000 random strings vs recursion)
```

**One-line summary:** *"dp[i] adds dp[i − 1] when the last digit is 1–9 and dp[i − 2] when the last two digits form 10–26, with dp[0] = 1; zeros only survive as the second digit of 10 or 20."*
