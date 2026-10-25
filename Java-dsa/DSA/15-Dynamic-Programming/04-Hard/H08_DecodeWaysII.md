# H08 · Decode Ways II

| | |
|---|---|
| **LeetCode** | [#639 Decode Ways II](https://leetcode.com/problems/decode-ways-ii/) |
| **Difficulty** | Hard |
| **Pattern** | [M03 Decode Ways](../03-Medium/M03_DecodeWays.md), but each step **multiplies** by "how many codes fit here" |
| **Code** | [H08_DecodeWaysII.java](H08_DecodeWaysII.java) |
| **Run** | `java H08_DecodeWaysII.java` |

The base problem is [M03](../03-Medium/M03_DecodeWays.md); modular arithmetic is explained in [Recursion M07](../../13-Recursion/03-Medium/M07_CountGoodNumbers.md#2-how-to-think-about-it).

---

## 1. The problem

Like Decode Ways, but `*` can be any digit from 1 to 9. Count the decodings modulo 10⁹ + 7.

| s | answer |
|---|---|
| "\*" | 9 |
| "1\*" | 18 |
| "2\*" | 15 |
| "\*\*" | 96 |
| "\*0" | 2 (only 10 and 20) |

---

## 2. How to think about it

The same recurrence as [M03](../03-Medium/M03_DecodeWays.md):

```
dp[i] = single(s[i−1]) · dp[i−1]  +  pair(s[i−2], s[i−1]) · dp[i−2]
```

where `single` and `pair` now **count** how many valid letters the last one or two characters can be (instead of being just 0 or 1):

| case | count | why |
|---|---|---|
| single(`*`) | 9 | 1..9 |
| single(`0`) | 0 | |
| single(digit) | 1 | |
| pair(`*`, `*`) | 15 | 11–19 (9) + 21–26 (6) |
| pair(`*`, d) | 2 if d ≤ 6, else 1 | "1d" and "2d", or only "1d" |
| pair(`1`, `*`) | 9 | 11–19 |
| pair(`2`, `*`) | 6 | 21–26 |
| pair(other digit, `*`) | 0 | |
| pair(d₁, d₂) | 1 if 10 ≤ d₁d₂ ≤ 26, else 0 | |

Everything is taken mod 10⁹ + 7, in `long` (single · dp can reach 9 · 10⁹).

### Dry runs

| s | dp[0] | dp[1] | dp[2] |
|---|---|---|---|
| "1\*" | 1 | 1 ('1') | 9 · 1 + 9 · 1 = **18** |
| "2\*" | 1 | 1 | 9 · 1 + 6 · 1 = **15** |
| "\*\*" | 1 | 9 | 9 · 9 + 15 · 1 = **96** |
| "\*0" | 1 | 9 | 0 · 9 + 2 · 1 = **2** |

---

## 3. Java concepts used in this problem

### 3.1 Character comparisons for digit ranges

```java
if (a == '*') return b <= '6' ? 2 : 1;
```

Digit characters are ordered '0' < '1' < … < '9', so `b <= '6'` checks the digit's value directly.

### 3.2 `long` intermediate, `int` result

```java
long cur = (single(...) * prev1 + pair(...) * prev2) % MOD;
```

Both products are below 15 · 10⁹ + 7, and their sum is still well within a `long`. Taking `% MOD` once per step keeps the values small.

### 3.3 Expanding stars with backtracking (in the referee)

```java
for (char d = '1'; d <= '9'; d++) { s[i] = d; total += brute(s, i + 1); }
s[i] = '*';
```

The referee replaces each `*` with every digit in turn (and restores it afterwards), then counts the plain decodings of each resulting string. With up to 3 stars, that's at most 729 strings.

---

## 4. Approach 1: Expand every `*` (the referee)

### Code ([H08_DecodeWaysII.java:60-78](H08_DecodeWaysII.java#L60-L78))

```java
static long brute(char[] s, int i) {
    if (i == s.length) return plain(new String(s));
    if (s[i] != '*') return brute(s, i + 1);
    long total = 0;
    for (char d = '1'; d <= '9'; d++) { s[i] = d; total += brute(s, i + 1); }
    s[i] = '*';
    return total;
}
```

`plain` is the [M03](../03-Medium/M03_DecodeWays.md#5-approach-2-bottom-up-with-two-variables-the-files-main-solution) DP. **O(9ᵏ · n)** for k stars.

---

## 5. Approach 2: Weighted Decode Ways (the file's main solution)

### Code ([H08_DecodeWaysII.java:37-57](H08_DecodeWaysII.java#L37-L57))

```java
static final int MOD = 1_000_000_007;

static int numDecodings(String s) {
    long prev2 = 1, prev1 = single(s.charAt(0));       // dp[0], dp[1]
    for (int i = 2; i <= s.length(); i++) {
        long cur = (single(s.charAt(i - 1)) * prev1 + pair(s.charAt(i - 2), s.charAt(i - 1)) * prev2) % MOD;
        prev2 = prev1;
        prev1 = cur;
    }
    return (int) prev1;
}

static int single(char c) { return c == '*' ? 9 : c == '0' ? 0 : 1; }

static int pair(char a, char b) {
    if (a == '*' && b == '*') return 15;
    if (a == '*') return b <= '6' ? 2 : 1;
    if (b == '*') return a == '1' ? 9 : a == '2' ? 6 : 0;
    int v = (a - '0') * 10 + (b - '0');
    return v >= 10 && v <= 26 ? 1 : 0;
}
```

| Line | What it does |
|---|---|
| `prev1 = single(s[0])` | dp[1]: how many letters the first character can be |
| the transition | each way of decoding the shorter prefix extends by every valid last letter |
| `single`, `pair` | the counts from the table in section 2 |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | Expand stars | Weighted DP |
|---|---|---|
| Time | O(9ᵏ · n) | **O(n)** |
| 100,000 stars | impossible | instant |

The file checks the DP against the expansion on 1,000 random strings (up to 3 stars) and runs a string of 100,000 stars.

---

## 7. Edge cases and common mistakes

**Edge cases:** "\*0" (2), "30" (0), a leading "0" (0), very long runs of stars.

**Mistakes:**
1. pair(`*`, `*`) = 17 or 26 (it's 15: 11–19 and 21–26 only).
2. pair(`*`, d) = 2 for every d (it's 1 when d ≥ 7: "27"+ aren't letters).
3. Forgetting `%` and overflowing.
4. Treating `*` as including 0.

---

## 8. Interview questions and answers

**Q1. How does the recurrence change from Decode Ways?**
> Each "can the last 1 or 2 characters be a letter?" check becomes "how many letters can they be?", and that count multiplies the number of ways for the shorter prefix.

**Q2. Where does 15 come from?**
> Two stars as one letter: the first must be 1 or 2. "1\*" gives 11–19 (9 options) and "2\*" gives 21–26 (6 options).

### Follow-up questions

**F1. Decode Ways.** → [M03](../03-Medium/M03_DecodeWays.md)

**F2. Count Vowels Permutation.** → [H10](H10_CountVowelsPermutation.md)
> Another counting DP mod 10⁹ + 7 with fixed transition rules.

**F3. Allow `*` to mean 0 as well.**
> Update the tables: single(`*`) stays 9 (0 alone is still invalid), but pair(`*`, `*`) becomes 17 (adding 10 and 20) and pair(`1`/`2`, `*`) each gain one.

---

## 9. Running the file

```bash
java H08_DecodeWaysII.java      # 7/7 tests passed (including 100,000 stars and 1000 random vs expanding every '*')
```

**One-line summary:** *"dp[i] = single(last char) · dp[i−1] + pair(last two chars) · dp[i−2] mod 10⁹+7, where single and pair count how many letters those characters can stand for ('*' = 1–9)."*
