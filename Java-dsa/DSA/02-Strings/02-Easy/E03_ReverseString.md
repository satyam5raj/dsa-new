# E03 · Reverse String

| | |
|---|---|
| **LeetCode** | [#344 Reverse String](https://leetcode.com/problems/reverse-string/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers (swap from both ends) |
| **Code** | [E03_ReverseString.java](E03_ReverseString.java) |
| **Run** | `java E03_ReverseString.java` |

---

## 1. The problem

Reverse a **`char[]`** **in place**, using O(1) extra memory.

| input | output |
|---|---|
| `['h','e','l','l','o']` | `['o','l','l','e','h']` |
| `['H','a','n','n','a','h']` | `['h','a','n','n','a','H']` |
| `['a']` | `['a']` |
| `['a','b']` | `['b','a']` |

**Constraints:** `1 ≤ n ≤ 10⁵`, printable ASCII.

Note that the input is a `char[]`, not a `String`. A String can't be reversed in place, because it's immutable.

---

## 2. How to think about it

Swap the first and last characters, then the second and second-to-last, and so on, moving inward until the pointers meet:

```
h e l l o      swap(0,4)  →  o e l l h
^       ^
o e l l h      swap(1,3)  →  o l l e h
  ^   ^
o l l e h      left = right = 2 → stop (the middle stays put)
    ^
```

Only **n/2 swaps** are needed.

---

## 3. Java concepts used in this problem

### 3.1 `char[]` vs `String`

| | `String` | `char[]` |
|---|---|---|
| can change characters? | **no** (immutable) | **yes** |
| length | `s.length()` | `a.length` |
| read | `s.charAt(i)` | `a[i]` |
| convert | `s.toCharArray()` | `new String(a)` or `String.valueOf(a)` |

When a problem says "in place", it hands you a `char[]`. To "edit" a String you always end up making a copy.

### 3.2 Swap with post-increment and post-decrement

```java
char t = s[left];
s[left++] = s[right];    // write at left, THEN left moves right
s[right--] = t;          // write at right, THEN right moves left
```

The same as the usual three-line swap followed by `left++; right--;`, written more compactly (see Arrays E07 for `k++`).

### 3.3 Recursion and the call stack

```java
static void reverseRecursive(char[] s, int left, int right) {
    if (left >= right) return;          // base case
    // ...swap s[left] and s[right]...
    reverseRecursive(s, left + 1, right - 1);   // a smaller problem
}
```

- Every recursive method needs a **base case** that stops it; otherwise it calls itself forever and crashes with a `StackOverflowError`.
- Each call is placed on the **call stack** (its parameters and local variables), so n/2 nested calls use **O(n) stack memory**, even though the code never creates an array. "Recursion isn't free."
- Java doesn't do **tail-call optimisation**, so even this "tail-recursive" form (the recursive call is the last thing it does) still uses stack space.

### 3.4 Built-in: `new StringBuilder(s).reverse()`

For a **String**, `new StringBuilder(s).reverse().toString()` gives the reversed copy in O(n). Good for interviews when "in place" isn't required.

---

## 4. Approach 1: Copy into a new array (not in place)

```java
static char[] reverseCopy(char[] s) {
    int n = s.length;
    char[] out = new char[n];
    for (int i = 0; i < n; i++) out[i] = s[n - 1 - i];   // the mirror position
    return out;
}
```

`s[n − 1 − i]` is the **mirror index** of i. **O(n) time, O(n) space**, which breaks the in-place rule.

---

## 5. Approach 2: Two pointers (the file's solution)

### Code ([E03_ReverseString.java:32-39](E03_ReverseString.java#L32-L39))

```java
static void reverseString(char[] s) {
    int left = 0, right = s.length - 1;
    while (left < right) {
        char t = s[left];
        s[left++] = s[right];
        s[right--] = t;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `int left = 0, right = s.length - 1;` | the two ends |
| `while (left < right)` | stop when they meet (odd length: the middle stays) or cross (even length) |
| `char t = s[left];` | save the left character |
| `s[left++] = s[right];` | copy the right character to the left, then move left inward |
| `s[right--] = t;` | put the saved character on the right, then move right inward |

### Dry run 1: `hello`

| step | left | right | swap | array |
|---|---|---|---|---|
| 1 | 0 | 4 | h ↔ o | `o e l l h` |
| 2 | 1 | 3 | e ↔ l | `o l l e h` |
| — | 2 | 2 | `2 < 2` false → stop | `o l l e h` ✓ |

### Dry run 2: `ab` (even length)

| step | left | right | swap | array |
|---|---|---|---|---|
| 1 | 0 | 1 | a ↔ b | `b a` |
| — | 1 | 0 | stop | ✓ |

### Complexity
- **Time O(n):** n/2 swaps.
- **Space O(1).**

---

## 6. Approach 3: Recursion (the file's second version)

### Code ([E03_ReverseString.java:41-45](E03_ReverseString.java#L41-L45))

```java
static void reverseRecursive(char[] s, int left, int right) {
    if (left >= right) return;                     // base case: crossed or met
    char t = s[left]; s[left] = s[right]; s[right] = t;
    reverseRecursive(s, left + 1, right - 1);
}
```

| Line | What it does |
|---|---|
| `if (left >= right) return;` | nothing left to swap |
| swap | the same swap as the loop |
| `reverseRecursive(s, left + 1, right - 1);` | "reverse the inside" is the same problem, just smaller |

**Dry run** `abcde`: call(0,4) swaps a/e → call(1,3) swaps b/d → call(2,2) returns. The stack depth is 3, and the result is `edcba` ✓

**O(n) time, O(n) stack space.** For n = 10⁵, 5 × 10⁴ nested calls can overflow the default stack. Prefer the loop.

---

## 7. Comparing the approaches

| | Copy | Two pointers | Recursion |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) | **O(1)** | O(n) stack |
| In place? | no | **yes** | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** a single character, even vs odd length, palindromes (unchanged).

**Mistakes:**
1. `while (left <= right)`: harmless (the middle swaps with itself), but unnecessary.
2. Looping i from 0 to n and swapping `s[i]` with `s[n−1−i]`: every pair gets swapped **twice**, so the array ends up unchanged. Only go to n/2.
3. Trying to reverse a `String` in place (impossible; strings are immutable).

---

## 9. Interview questions and answers

**Q1. Why only n/2 swaps?**
> Each swap fixes two positions at once (both ends). After n/2 swaps every position has been placed.

**Q2. Is the recursive version O(1) space?**
> No. Each call adds a stack frame, so it's O(n) space, and Java doesn't optimise tail calls.

**Q3. How would you reverse a String?**
> `new StringBuilder(s).reverse().toString()`, or `toCharArray()`, two-pointer swap, then `new String(chars)`.

### Follow-up questions

**F1. Reverse only the vowels (LC 345).**
> Two pointers that skip non-vowels (like E02 skips punctuation) and swap vowels.

**F2. Reverse String II (LC 541): reverse the first k characters of every 2k block.**
> For each block start `i = 0, 2k, 4k, …`, reverse `[i, min(i + k, n) − 1]`.

**F3. Reverse the words in a sentence (LC 151 / 186).** → [M12](../03-Medium/M12_ReverseWordsInString.md)
> Reverse the whole array, then reverse each word in place.

**F4. Reverse Only Letters (LC 917).**
> The same two pointers, skipping non-letters.

---

## 10. Running the file

```bash
java E03_ReverseString.java      # 5/5 tests passed
```

**One-line summary:** *"Swap the two ends and move inward: n/2 swaps, O(1) space."*
