# E02 · Valid Palindrome

| | |
|---|---|
| **LeetCode** | [#125 Valid Palindrome](https://leetcode.com/problems/valid-palindrome/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers (skipping characters that don't count) |
| **Code** | [E02_ValidPalindrome.java](E02_ValidPalindrome.java) |
| **Run** | `java E02_ValidPalindrome.java` |

---

## 1. The problem

After **lowercasing** everything and **removing every non-alphanumeric character** (anything that isn't a letter or a digit), does the string read the same forwards and backwards?

| s | cleaned | answer |
|---|---|---|
| `"A man, a plan, a canal: Panama"` | `"amanaplanacanalpanama"` | true |
| `"race a car"` | `"raceacar"` | false |
| `" "` | `""` | true (an empty string is a palindrome) |
| `"0P"` | `"0p"` | false |
| `".,a,."` | `"a"` | true |

**Constraints:** `1 ≤ s.length ≤ 2 × 10⁵`, printable ASCII characters.

---

## 2. How to think about it

1. **Build, then compare:** create the cleaned string, then check it against its reverse. Simple, but it uses O(n) extra memory.
2. **Two pointers on the original string:** `i` from the left, `j` from the right.
   - skip `i` forward over characters that don't count
   - skip `j` backward over characters that don't count
   - compare the two characters ignoring case; a mismatch means it's not a palindrome
   - move both inward and repeat

No new string is ever built. That's O(1) extra space.

---

## 3. Java concepts used in this problem

### 3.1 `Character` helper methods

`Character` is the wrapper class for `char`, and it has many useful **static** methods:

| Method | Returns true / result for… |
|---|---|
| `Character.isLetterOrDigit(c)` | `'a'`, `'Z'`, `'7'` → true; `' '`, `','`, `':'` → false |
| `Character.isLetter(c)` | letters only |
| `Character.isDigit(c)` | `'0'..'9'` (and other Unicode digits) |
| `Character.isWhitespace(c)` | space, tab, newline |
| `Character.toLowerCase(c)` | `'A'` → `'a'`; non-letters are returned unchanged |
| `Character.isUpperCase(c)` / `toUpperCase(c)` | |

They also handle non-English letters correctly (`'é'` is a letter), which beats hand-written checks like `c >= 'a' && c <= 'z'`.

### 3.2 Comparing `char`s with `==`

`char` is a **primitive**, so `==` compares values directly. That's correct here (unlike `String`, where you need `.equals`).

### 3.3 Nested `while` loops with the same guard

```java
while (i < j && !Character.isLetterOrDigit(s.charAt(i))) i++;
```

The inner loops repeat the `i < j` check, so they can never run past each other. That matters for strings like `" "` or `".,."` that have no real characters at all.

### 3.4 Building a cleaned string with `StringBuilder` (approach 1)

```java
StringBuilder sb = new StringBuilder();
sb.append(c);                       // add a char
String fwd = sb.toString();
String rev = sb.reverse().toString();   // reverse() changes sb itself and returns it
```

Careful: `reverse()` reverses the builder **in place**, so call `toString()` for the forward version first.

---

## 4. Approach 1: Clean, then compare with the reverse

```java
static boolean isPalindromeClean(String s) {
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
        if (Character.isLetterOrDigit(c)) sb.append(Character.toLowerCase(c));
    }
    String cleaned = sb.toString();
    return cleaned.equals(sb.reverse().toString());
}
```

| Line | What it does |
|---|---|
| `if (Character.isLetterOrDigit(c)) sb.append(Character.toLowerCase(c));` | keep only letters/digits, lowercased |
| `String cleaned = sb.toString();` | snapshot of the forward version, taken **before** reversing |
| `cleaned.equals(sb.reverse().toString())` | compare it with the reversed version |

**Dry run** `"race a car"` → cleaned `"raceacar"`, reversed `"racaecar"` → not equal → **false**.

**O(n) time, O(n) space.** Easy to write, and a fine first answer.

---

## 5. Approach 2: Two pointers on the original string (the file's solution)

### Code ([E02_ValidPalindrome.java:38-48](E02_ValidPalindrome.java#L38-L48))

```java
static boolean isPalindrome(String s) {
    int i = 0, j = s.length() - 1;
    while (i < j) {
        while (i < j && !Character.isLetterOrDigit(s.charAt(i))) i++;   // skip junk on the left
        while (i < j && !Character.isLetterOrDigit(s.charAt(j))) j--;   // skip junk on the right
        if (Character.toLowerCase(s.charAt(i)) != Character.toLowerCase(s.charAt(j))) return false;
        i++;
        j--;
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int i = 0, j = s.length() - 1;` | the two ends |
| `while (i < j)` | there are still at least two positions left to compare |
| first inner `while` | move `i` right past spaces and punctuation |
| second inner `while` | move `j` left past spaces and punctuation |
| `if (toLowerCase(s[i]) != toLowerCase(s[j])) return false;` | a real mismatch (case is ignored) |
| `i++; j--;` | this pair matches, so move both inward |
| `return true;` | every pair matched |

### Dry run 1: `"race a car"` (indices: r0 a1 c2 e3 ␣4 a5 ␣6 c7 a8 r9)

| i (char) | j (char) | skips | compare | result |
|---|---|---|---|---|
| 0 (r) | 9 (r) | — | r = r | move |
| 1 (a) | 8 (a) | — | a = a | move |
| 2 (c) | 7 (c) | — | c = c | move |
| 3 (e) | 6 (␣) | j skips 6 → 5 (a) | e ≠ a | **false** |

### Dry run 2: `".,a,."`
| step | i | j | what happens |
|---|---|---|---|
| left skip | 0 → 1 → 2 | 4 | skips `.` and `,`, stops at `a` |
| right skip | 2 | 4 → 3 → 2 | skips `.` and `,`; stops when `i < j` fails |
| compare | 2 | 2 | `a` vs `a` (the same character) → equal |
| move | 3 | 1 | `3 < 1` is false → loop ends → **true** ✓ |

### Dry run 3: `" "` (a single space)
i = 0, j = 0: `0 < 0` is false, so the loop never runs → **true** ✓

### Dry run 4: `"0P"`
i = 0 ('0'), j = 1 ('P'). `'0'` vs `'p'` → different → **false** ✓ (digits count as real characters).

### Complexity
- **Time O(n):** each pointer moves across the string at most once.
- **Space O(1).**

---

## 6. Comparing the approaches

| | Clean + reverse | Two pointers |
|---|---|---|
| Time | O(n) | O(n) |
| Extra space | O(n) | **O(1)** |
| Early exit on a mismatch | no (it builds everything first) | **yes** |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty or all-punctuation string (true), mixed case (`"Aa"` → true), digits (`"0P"`), a single character.

**Mistakes:**
1. Forgetting the `i < j` check in the inner loops, which goes out of bounds on all-punctuation strings.
2. Treating digits as junk (`isLetter` instead of `isLetterOrDigit`).
3. Calling `sb.reverse()` before saving the forward string, so both versions are the reversed one.
4. Comparing without lowercasing.

---

## 8. Interview questions and answers

**Q1. Why are two pointers better than building the cleaned string?**
> Both are O(n) time, but two pointers use O(1) extra space and stop at the first mismatch.

**Q2. Why do the inner loops need `i < j` too?**
> If the remaining characters are all junk, the pointers would otherwise cross and read outside the string.

**Q3. How do you check alphanumerics without `Character` methods?**
> `(c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')`, and lowercase with `c + 32` for `'A'..'Z'`. Using `Character` is clearer, though.

### Follow-up questions

**F1. Valid Palindrome II: you may delete at most one character (LC 680).** → [E05](E05_ValidPalindromeII.md)
> On the first mismatch, try skipping either the left or the right character, and check whether the rest is a palindrome.

**F2. Palindrome Linked List (LC 234).** → [03-Linked-Lists E05](../../03-Linked-Lists/02-Easy/E05_PalindromeLinkedList.md)
> Find the middle with slow/fast pointers, reverse the second half, then compare.

**F3. Palindrome Number (LC 9) without converting to a string.**
> Reverse half of the digits (`rev = rev × 10 + x % 10`) until `rev ≥ x`, then compare.

**F4. Longest palindromic substring.** → [M10](../03-Medium/M10_LongestPalindromicSubstring.md)
> Expand around each centre (2n − 1 centres).

---

## 9. Running the file

```bash
java E02_ValidPalindrome.java      # 5/5 tests passed
```

**One-line summary:** *"Two pointers from both ends; skip non-alphanumerics; compare lowercased characters."*
