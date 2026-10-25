# E05 · Valid Palindrome II

| | |
|---|---|
| **LeetCode** | [#680 Valid Palindrome II](https://leetcode.com/problems/valid-palindrome-ii/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers + one "skip" (branch once at the first mismatch) |
| **Code** | [E05_ValidPalindromeII.java](E05_ValidPalindromeII.java) |
| **Run** | `java E05_ValidPalindromeII.java` |

---

## 1. The problem

Can `s` become a palindrome by deleting **at most one** character?

| s | answer | why |
|---|---|---|
| `"aba"` | true | already a palindrome (0 deletions) |
| `"abca"` | true | delete `b` → `"aca"` (or delete `c` → `"aba"`) |
| `"abc"` | false | no single deletion works |
| `"deeee"` | true | delete `d` |

**Constraints:** `1 ≤ s.length ≤ 10⁵`, lowercase letters.

---

## 2. How to think about it

1. **Brute force:** try deleting each character (n options) and check whether what remains is a palindrome (O(n) each): **O(n²)**.
2. **Insight:** walk inward with two pointers, like E02. While the characters match, nothing needs deleting. At the **first mismatch** `s[i] != s[j]`, everything outside `[i, j]` already matched, so **the deleted character must be either `s[i]` or `s[j]`**. Deleting anything else would leave this mismatched pair in place.

   So there are only **two** options to check:
   - delete `s[i]` → is `s[i+1..j]` a palindrome?
   - delete `s[j]` → is `s[i..j−1]` a palindrome?

   We've now used our one deletion, so these are **plain** palindrome checks with no more skipping.

---

## 3. Java concepts used in this problem

### 3.1 A helper that works on a range

```java
static boolean isPalindrome(String s, int i, int j) { ... }   // checks s[i..j] only
```

Passing indices instead of calling `s.substring(i, j + 1)` avoids **copying** the string. `substring` creates a new String in O(length) time and memory. Range parameters are a habit worth building in string problems.

### 3.2 `s.charAt(i++) != s.charAt(j--)`

It compares the characters at the **current** `i` and `j`, then moves both inward (post-increment and post-decrement), all in one expression. It's compact, but make sure you read it as "compare first, then move".

### 3.3 Short-circuit `||` between two checks

```java
return isPalindrome(s, i + 1, j) || isPalindrome(s, i, j - 1);
```

If the first option succeeds, the second check **never runs**.

---

## 4. Approach 1: Brute force (try every deletion)

```java
static boolean validPalindromeBrute(String s) {
    if (isPalindrome(s, 0, s.length() - 1)) return true;
    for (int del = 0; del < s.length(); del++) {
        String t = s.substring(0, del) + s.substring(del + 1);   // s without index del
        if (isPalindrome(t, 0, t.length() - 1)) return true;
    }
    return false;
}
```

- `s.substring(a, b)` returns the characters in `[a, b)`: start **inclusive**, end **exclusive**. `s.substring(a)` goes to the end.
- **O(n²) time and O(n) extra space per attempt.** It times out for n = 10⁵.

---

## 5. Approach 2: Two pointers with one skip (the file's solution)

### Code ([E05_ValidPalindromeII.java:38-55](E05_ValidPalindromeII.java#L38-L55))

```java
static boolean validPalindrome(String s) {
    int i = 0, j = s.length() - 1;
    while (i < j) {
        if (s.charAt(i) != s.charAt(j)) {
            return isPalindrome(s, i + 1, j) || isPalindrome(s, i, j - 1);   // our ONE deletion
        }
        i++;
        j--;
    }
    return true;                                      // no deletion needed
}

static boolean isPalindrome(String s, int i, int j) {
    while (i < j) {
        if (s.charAt(i++) != s.charAt(j--)) return false;
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `while (i < j)` | walk inward like a normal palindrome check |
| `if (s.charAt(i) != s.charAt(j))` | the first mismatch, where we must spend our deletion |
| `return isPalindrome(s, i + 1, j) \|\| isPalindrome(s, i, j - 1);` | try deleting the left character, or the right one. The answer is decided **right here**, either way |
| `i++; j--;` | the characters matched, so move inward |
| `return true;` | no mismatch at all: already a palindrome |
| `isPalindrome(s, i, j)` | a plain check of the range `[i, j]` |

### Dry run 1: `"abca"`

| i | j | s[i] | s[j] | action |
|---|---|---|---|---|
| 0 | 3 | a | a | match, move |
| 1 | 2 | b | c | **mismatch** |

- Option 1, delete `b`: is `s[2..2]` = `"c"` a palindrome? **yes** → **return true** ✓ (option 2 is never checked)

### Dry run 2: `"abc"`

| i | j | s[i] | s[j] | action |
|---|---|---|---|---|
| 0 | 2 | a | c | **mismatch** |

- Option 1: `s[1..2]` = `"bc"` → no.
- Option 2: `s[0..1]` = `"ab"` → no.
- **Return false** ✓

### Dry run 3: `"deeee"`

| i | j | s[i] | s[j] | action |
|---|---|---|---|---|
| 0 | 4 | d | e | **mismatch** |

- Option 1: `s[1..4]` = `"eeee"` → palindrome → **true** ✓

### Why checking only these two options is enough
The outer part already matches pair by pair: `s[0..i−1]` is the mirror image of `s[j+1..n−1]`. So **the outer part can't be the problem**; the trouble is inside `[i, j]`, whose two ends `s[i]` and `s[j]` disagree. For the inner part to become a palindrome after one deletion, one of those two disagreeing ends has to go. (A deletion outside `[i, j]` only shifts characters that already match their mirrors. Whenever it works, deleting `s[i]` or `s[j]` works too.)

This was also checked by comparing the algorithm with brute force on 200,000 random strings over the alphabet {a, b, c}: zero mismatches. A tricky example where only the **right** deletion works is `"cbbcc"`: at the mismatch (b, c), option 1 gives `"bc"` (no), and option 2 gives `"bb"` (yes).

### Complexity
- **Time O(n):** the main scan plus at most two more partial scans, each O(n).
- **Space O(1).**

---

## 6. Comparing the approaches

| | Brute force | Two pointers + one skip |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(n) per attempt | **O(1)** |

---

## 7. Edge cases and common mistakes

**Edge cases:** already a palindrome, a single character, two characters (`"ab"` → true: delete either), the mismatch at the very start or middle.

**Mistakes:**
1. Checking only **one** option (e.g. always deleting the left character). `"cbbcc"` needs the right one deleted.
2. Continuing the main loop after a mismatch (and trying to skip again later), which allows more than one deletion.
3. Using `substring` inside the checks, which adds O(n) copies.

---

## 8. Interview questions and answers

**Q1. Why do only two options need checking at the first mismatch?**
> Everything outside `[i, j]` already matches in pairs. The mismatched pair `s[i]`, `s[j]` must be broken up, so one of them has to be deleted. That leaves two choices.

**Q2. Why are the follow-up checks plain palindrome checks?**
> The one allowed deletion has been used, so the remaining range must already be a palindrome.

**Q3. What's the complexity in the worst case?**
> O(n): the first scan, plus at most two scans of the remaining middle part.

### Follow-up questions

**F1. At most k deletions (LC 1216, Valid Palindrome III).**
> Branching on every mismatch blows up to O(2ᵏ). Use DP instead: the string is a k-palindrome iff `n − LPS(s) ≤ k`, where LPS is the longest palindromic subsequence (O(n²) DP, see 15-DP).

**F2. Return which index to delete.**
> Return `i` if option 1 works, `j` if option 2 works, or −1 if the string is already a palindrome.

**F3. Minimum insertions to make a palindrome (LC 1312).**
> `n − LPS(s)`, the same DP.

**F4. Ignore case and non-alphanumerics, as in E02.**
> Combine both skips: skip junk as in E02, and allow one real deletion as here.

---

## 9. Running the file

```bash
java E05_ValidPalindromeII.java      # 6/6 tests passed
```

**One-line summary:** *"Two pointers; at the first mismatch, the answer is palindrome(i+1..j) or palindrome(i..j−1)."*
