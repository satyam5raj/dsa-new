# M08 · Bulls and Cows

| | |
|---|---|
| **LeetCode** | [#299 Bulls and Cows](https://leetcode.com/problems/bulls-and-cows/) |
| **Difficulty** | Medium |
| **Pattern** | One pass with a signed count array |
| **Code** | [M08_BullsAndCows.java](M08_BullsAndCows.java) |
| **Run** | `java M08_BullsAndCows.java` |

---

## 1. The problem

`secret` and `guess` are digit strings of equal length.
- **Bulls** = digits in the **right position**.
- **Cows** = digits that exist in secret but are in the **wrong position**. Each secret digit can be matched **at most once**.

Return `"xAyB"` (x bulls, y cows).

| secret | guess | answer | why |
|---|---|---|---|
| `"1807"` | `"7810"` | `"1A3B"` | 8 is a bull; 1, 0, 7 are cows |
| `"1123"` | `"0111"` | `"1A1B"` | the 1 at index 1 is a bull; one more 1 is a cow |
| `"1234"` | `"1234"` | `"4A0B"` | |
| `"1122"` | `"2211"` | `"0A4B"` | |

**Constraints:** `1 ≤ n ≤ 1000`, digits only.

---

## 2. How to think about it

### Two passes (the clearest)
1. Count the **bulls** (same position, same digit).
2. For the **non-bull** positions, count each digit in secret and in guess separately.
3. Cows = Σ over digits d of `min(secretCount[d], guessCount[d])`: each unmatched secret digit can pair with at most one unmatched guess digit.

### One pass (elegant)
Use **one** signed array `count[10]` over the non-bull positions: a **secret** digit adds **+1**, a **guess** digit adds **−1**.
- When we see secret digit s: if `count[s] < 0`, an earlier **guess** digit s is waiting for it → **cow**.
- When we see guess digit g: if `count[g] > 0`, an earlier **secret** digit g is waiting → **cow**.
- Then update: `count[s]++`, `count[g]--`.

A positive count means "unmatched secret copies waiting"; a negative count means "unmatched guess copies waiting".

---

## 3. Java concepts used in this problem

### 3.1 Digit characters to numbers: `c - '0'`

```java
int s = secret.charAt(i) - '0';   // '7' → 7
```

The characters `'0'..'9'` have consecutive codes (48..57), so subtracting `'0'` gives the digit's value.

### 3.2 Building the result with `+`

```java
return bulls + "A" + cows + "B";   // 1 + "A" + 3 + "B" = "1A3B"
```

Once a String is involved, `+` means concatenation, evaluated left to right. Careful: `1 + 2 + "A"` is `"3A"` (the numbers are added first), but `"A" + 1 + 2` is `"A12"`. For a single expression like this, `+` is fine; inside loops, use a `StringBuilder`.

---

## 4. Approach 1: Two passes with two count arrays

```java
static String getHintTwoPass(String secret, String guess) {
    int bulls = 0;
    int[] sc = new int[10], gc = new int[10];
    for (int i = 0; i < secret.length(); i++) {
        char s = secret.charAt(i), g = guess.charAt(i);
        if (s == g) bulls++;
        else { sc[s - '0']++; gc[g - '0']++; }      // only the unmatched positions
    }
    int cows = 0;
    for (int d = 0; d < 10; d++) cows += Math.min(sc[d], gc[d]);
    return bulls + "A" + cows + "B";
}
```

**Dry run** `"1123"` / `"0111"`:
- i=1 is a bull (1 = 1).
- Unmatched secret digits: 1, 2, 3 → sc[1]=1, sc[2]=1, sc[3]=1.
- Unmatched guess digits: 0, 1, 1 → gc[0]=1, gc[1]=2.
- Cows = min(1, 2) for digit 1 = **1** → `"1A1B"` ✓

**O(n) time, O(1) space.**

---

## 5. Approach 2: One pass with a signed count (the file's solution)

### Code ([M08_BullsAndCows.java:44-59](M08_BullsAndCows.java#L44-L59))

```java
static String getHint(String secret, String guess) {
    int[] count = new int[10];
    int bulls = 0, cows = 0;
    for (int i = 0; i < secret.length(); i++) {
        int s = secret.charAt(i) - '0', g = guess.charAt(i) - '0';
        if (s == g) {
            bulls++;
        } else {
            if (count[s] < 0) cows++;     // a guess digit was waiting for this secret digit
            if (count[g] > 0) cows++;     // a secret digit was waiting for this guess digit
            count[s]++;
            count[g]--;
        }
    }
    return bulls + "A" + cows + "B";
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] count = new int[10];` | > 0: unmatched secret copies; < 0: unmatched guess copies |
| `if (s == g) bulls++;` | the right digit in the right place; it doesn't touch the counts |
| `if (count[s] < 0) cows++;` | this secret digit pairs with a waiting guess digit |
| `if (count[g] > 0) cows++;` | this guess digit pairs with a waiting secret digit |
| `count[s]++; count[g]--;` | record them (a paired digit cancels the waiting one; an unpaired one starts waiting) |
| `return bulls + "A" + cows + "B";` | |

**Why check before updating?** At a single position, s ≠ g, so the two checks are about **different** digits and can't match each other. Checking first makes sure each digit only pairs with **earlier** unmatched digits.

### Dry run 1: `"1807"` / `"7810"`

| i | s | g | checks | count after (non-zero) | bulls | cows |
|---|---|---|---|---|---|---|
| 0 | 1 | 7 | none waiting | 1:+1, 7:−1 | 0 | 0 |
| 1 | 8 | 8 | **bull** | (unchanged) | 1 | 0 |
| 2 | 0 | 1 | count[1] = +1 > 0 → **cow** (the guess 1 meets the secret 1) | 0:+1, 7:−1 | 1 | 1 |
| 3 | 7 | 0 | count[7] = −1 < 0 → **cow**; count[0] = +1 > 0 → **cow** | all 0 | 1 | 3 |

**Return `"1A3B"`** ✓

### Dry run 2: `"1123"` / `"0111"`

| i | s | g | checks | count after (non-zero) | bulls | cows |
|---|---|---|---|---|---|---|
| 0 | 1 | 0 | none | 0:−1, 1:+1 | 0 | 0 |
| 1 | 1 | 1 | **bull** | | 1 | 0 |
| 2 | 2 | 1 | count[1] = +1 → **cow** | 0:−1, 2:+1 | 1 | 1 |
| 3 | 3 | 1 | count[3] = 0, count[1] = 0 → nothing | 0:−1, 1:−1, 2:+1, 3:+1 | 1 | 1 |

**Return `"1A1B"`** ✓. The third guess 1 finds no secret 1 left (the first one was already used as a cow).

### Complexity
- **Time O(n)**, **Space O(1).**

---

## 6. Comparing the approaches

| | Two passes | One signed pass |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(20) | O(10) |
| Clarity | **very clear** | elegant |

---

## 7. Edge cases and common mistakes

**Edge cases:** all bulls, all cows, no matches at all, repeated digits on one side only.

**Mistakes:**
1. Counting bull positions as cows too (bull digits must be excluded from the cow counting).
2. Counting cows as `min` over **all** positions (including bulls).
3. Counting a secret digit as a cow several times (the "used up" rule).

---

## 8. Interview questions and answers

**Q1. Why min(secretCount, guessCount) for cows?**
> Each unmatched secret copy can be matched with at most one unmatched guess copy of the same digit, so the number of pairs per digit is the smaller of the two counts.

**Q2. Explain the signed count.**
> It's a running balance per digit: a secret digit adds +1 and a guess digit adds −1. If a secret digit arrives while the balance is negative, a guess copy was waiting: that's a cow. And vice versa.

### Follow-up questions

**F1. Letters instead of digits, or longer strings.**
> Use `int[26]` / `int[128]` or a map; the logic is the same.

**F2. Mastermind solver: pick the next guess.**
> Knuth's minimax algorithm: choose the guess that minimises the worst-case number of remaining candidates. That's game-tree search, beyond this problem.

**F3. Return the positions of the bulls.**
> Collect the indices where `s == g`.

---

## 9. Running the file

```bash
java M08_BullsAndCows.java      # 5/5 tests passed
```

**One-line summary:** *"Bulls: the same position. Cows: a signed digit balance; a digit pairs with a waiting copy from the other string."*
