# M28 · Stone Game

| | |
|---|---|
| **LeetCode** | [#877 Stone Game](https://leetcode.com/problems/stone-game/) |
| **Difficulty** | Medium |
| **Pattern** | **Game DP** on intervals: store the score **difference** for the player to move |
| **Code** | [M28_StoneGame.java](M28_StoneGame.java) |
| **Run** | `java M28_StoneGame.java` |

The interval-DP loop order is explained in [M24](M24_PalindromicSubstringsDP.md#31-the-interval-dp-loop-order).

---

## 1. The problem

There's an even number of piles in a row, and the total number of stones is odd. Alice and Bob take turns taking a whole pile from **either end**; both play optimally. Does Alice (who goes first) win?

| piles | answer |
|---|---|
| [5, 3, 4, 5] | true |
| [3, 7, 2, 3] | true |

---

## 2. How to think about it

### The minimax trick: one number instead of two scores
Instead of tracking both players' totals, track

```
diff[i][j] = (my stones − my opponent's stones) when piles i..j remain and it's MY turn
```

If I take pile i, my opponent then faces i + 1..j and gets diff[i + 1][j] **in their favour**, so my margin is a[i] − diff[i + 1][j]. The same holds for the right end:

| step | for this problem |
|---|---|
| **State** | diff[i][j] as above |
| **Transition** | diff[i][j] = max(a[i] − diff[i + 1][j], a[j] − diff[i][j − 1]) |
| **Base** | diff[i][i] = a[i] |
| **Answer** | Alice wins iff diff[0][n − 1] > 0 |

This "my score minus yours" formulation solves most two-player optimal games. [Predict the Winner (LC 486)](https://leetcode.com/problems/predict-the-winner/) is the same code without the even/odd guarantee.

### The math answer: always true
With an **even** number of piles, Alice can take all the even-indexed piles or all the odd-indexed ones: whichever end she takes, Bob is forced onto the other parity. One parity has the larger sum, and since the total is odd there are no ties, so Alice always wins. The DP is still the point of this problem: it gives the exact margin and handles variants without the guarantee.

### Dry run: [5, 3, 4, 5]

| length | intervals |
|---|---|
| 1 | 5, 3, 4, 5 |
| 2 | [5,3] = max(5 − 3, 3 − 5) = 2; [3,4] = max(3 − 4, 4 − 3) = 1; [4,5] = 1 |
| 3 | [5,3,4] = max(5 − 1, 4 − 2) = 4; [3,4,5] = max(3 − 1, 5 − 1) = 4 |
| 4 | [5,3,4,5] = max(5 − 4, 5 − 4) = **1** |

Alice wins by 1.

---

## 3. Java concepts used in this problem

### 3.1 A 2-D interval table in one row

```java
int[] diff = new int[n];                          // diff[j] for the current start i
for (int i = n - 1; i >= 0; i--) {
    diff[i] = a[i];
    for (int j = i + 1; j < n; j++)
        diff[j] = Math.max(a[i] - diff[j], a[j] - diff[j - 1]);   // diff[j] = old row (i+1), diff[j-1] = this row
}
```

With i going down and j going up, before the update `diff[j]` still holds diff[i + 1][j] (the previous row), and `diff[j − 1]` has just been set to diff[i][j − 1]. So one array is enough.

### 3.2 Checking a theorem in the test

```java
if (a.length % 2 == 0 && Arrays.stream(a).sum() % 2 == 1) alwaysWins &= stoneGame(a);
```

For every random input that meets the problem's conditions, the test confirms Alice wins, which is the math argument above.

---

## 4. Approach 1: Recursive minimax (the referee)

### Code ([M28_StoneGame.java:57-60](M28_StoneGame.java#L57-L60))

```java
static int bruteMargin(int[] a, int i, int j) {
    if (i > j) return 0;
    return Math.max(a[i] - bruteMargin(a, i + 1, j), a[j] - bruteMargin(a, i, j - 1));
}
```

The same recurrence top-down, without memo: **O(2ⁿ).**

---

## 5. Approach 2: Interval DP in one row (the file's main solution)

### Code ([M28_StoneGame.java:43-54](M28_StoneGame.java#L43-L54))

```java
static boolean stoneGame(int[] piles) { return margin(piles) > 0; }

static int margin(int[] a) {
    int n = a.length;
    int[] diff = new int[n];                          // diff[j] for the current start i
    for (int i = n - 1; i >= 0; i--) {
        diff[i] = a[i];
        for (int j = i + 1; j < n; j++)
            diff[j] = Math.max(a[i] - diff[j], a[j] - diff[j - 1]);   // diff[j] = old row (i+1), diff[j-1] = this row
    }
    return diff[n - 1];
}
```

| Line | What it does |
|---|---|
| `diff[i] = a[i]` | one pile left: take it |
| the inner loop | longer intervals starting at i |
| `a[i] − diff[j]` | take the left pile; the opponent's best margin on i+1..j counts against me |
| `a[j] − diff[j − 1]` | take the right pile |
| the return | Alice's margin on the whole row |

### Complexity
- **Time O(n²), space O(n).**

---

## 6. Comparing the approaches

| | Recursion | Interval DP | Math |
|---|---|---|---|
| Time | O(2ⁿ) | O(n²) | **O(1)** |
| Gives the margin | yes | **yes** | no |
| Works without the even/odd guarantee | yes | **yes** | no |

The file checks the DP margin against the recursion on 500 random arrays (any length), and checks that Alice always wins when the problem's conditions hold.

---

## 7. Edge cases and common mistakes

**Edge cases:** two piles (Alice takes the larger), all piles equal (margin 0 if allowed, which the odd total rules out).

**Mistakes:**
1. Tracking two separate scores and mixing up whose turn it is.
2. `a[i] + diff[i + 1][j]` (the opponent's margin must be **subtracted**).
3. The wrong loop order for the one-row version.

---

## 8. Interview questions and answers

**Q1. Why subtract the opponent's diff?**
> After my move, the opponent plays optimally on what remains, gaining a margin of diff over me. My total margin is my pile minus that.

**Q2. Why does Alice always win here?**
> With an even number of piles she can force herself onto either all even indices or all odd indices; she chooses the parity with more stones, and the odd total rules out a tie.

### Follow-up questions

**F1. Predict the Winner (LC 486).**
> The same DP with any number of piles: return diff ≥ 0.

**F2. Stone Game II (LC 1140).**
> Players take from the front only, up to 2M piles; DP over (start, M) with suffix sums.

**F3. Nim.**
> A different kind of game: XOR the pile sizes; the first player wins iff the XOR is non-zero.

---

## 9. Running the file

```bash
java M28_StoneGame.java      # 5/5 tests passed (including 500 random: margin == minimax, and Alice always wins)
```

**One-line summary:** *"diff[i][j] = max(a[i] − diff[i+1][j], a[j] − diff[i][j−1]) is the mover's best margin on piles i..j; Alice wins iff diff[0][n−1] > 0 (which, with even piles and an odd total, is always)."*
