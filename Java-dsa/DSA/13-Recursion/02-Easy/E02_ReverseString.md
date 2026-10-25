# E02 · Reverse String ⭐

| | |
|---|---|
| **LeetCode** | [#344 Reverse String](https://leetcode.com/problems/reverse-string/) |
| **Difficulty** | Easy |
| **Pattern** | Recursion on **two indices**, shrinking the range from both ends |
| **Code** | [E02_ReverseString.java](E02_ReverseString.java) |
| **Run** | `java E02_ReverseString.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Reverse a `char[]` in place.

| input | output |
|---|---|
| ['h','e','l','l','o'] | ['o','l','l','e','h'] |
| ['H','a','n','n','a','h'] | ['h','a','n','n','a','H'] |

---

## 2. How to think about it

### Recursive thinking
"Reverse `s[lo..hi]`" is the same as:
1. swap the two ends, `s[lo]` and `s[hi]`;
2. reverse `s[lo+1..hi−1]` (a smaller version of the same problem).

**Base case:** `lo >= hi`. Zero or one characters are already reversed.

This is the same "two indices moving inward" shape as the two-pointer loop from [Arrays](../../01-Arrays/02-Easy/E09_SquaresOfSortedArray.md). Every such loop can be written as recursion and vice versa. The loop is better in practice: O(1) space instead of n/2 stack frames.

### Dry run: "hello"

| call | lo | hi | swap | array after |
|---|---|---|---|---|
| reverse(0, 4) | 0 | 4 | h ↔ o | o e l l h |
| reverse(1, 3) | 1 | 3 | e ↔ l | o l l e h |
| reverse(2, 2) | 2 | 2 | lo ≥ hi → return | o l l e h |

The calls then return in reverse order, with nothing left to do.

### Dry run: "Hannah" (even length)
reverse(0, 5): H ↔ h → "hannaH"; reverse(1, 4): a ↔ a; reverse(2, 3): n ↔ n; reverse(3, 2): lo > hi → stop. Result **"hannaH"**.

---

## 3. Java concepts used in this problem

### 3.1 A public wrapper and a private helper

```java
static void reverseString(char[] s) { reverse(s, 0, s.length - 1); }
static void reverse(char[] s, int lo, int hi) { ... }
```

The required signature only takes the array, but the recursion needs extra parameters (the current range). The usual pattern: a wrapper with the required signature calls a helper that carries the extra state.

### 3.2 Arrays are passed by reference value

`reverse(s, ...)` gives every call the **same** array object, so a swap made in one call is seen by all the others and by the caller. That's what makes "in place" work. (The `int` parameters, by contrast, are copied: each call has its own `lo` and `hi`.)

### 3.3 Tail recursion

The recursive call is the **last** thing `reverse` does. That's called *tail recursion*. Some languages turn it into a loop automatically; **Java does not**, so it still uses one stack frame per call.

### 3.4 Converting between `String` and `char[]`

```java
char[] a = "hello".toCharArray();     // String → a new char array
new String(a)                         // char array → a new String
```

Strings are immutable, so to change characters in place you work on a `char[]`.

---

## 4. Approach 1: Two-pointer loop (the referee idea)

The test compares against `StringBuilder.reverse()`, but the iterative version you'd write in an interview is:

```java
class Loop {
    static void reverseString(char[] s) {
        for (int lo = 0, hi = s.length - 1; lo < hi; lo++, hi--) {
            char t = s[lo]; s[lo] = s[hi]; s[hi] = t;
        }
    }
}
```

- **Time O(n), space O(1).**
- `StringBuilder` and its `reverse()` are explained in [Strings E03](../../02-Strings/02-Easy/E03_ReverseString.md#3-java-concepts-used-in-this-problem).

---

## 5. Approach 2: Recursion (the file's main solution)

### Code ([E02_ReverseString.java:30-36](E02_ReverseString.java#L30-L36))

```java
static void reverseString(char[] s) { reverse(s, 0, s.length - 1); }

static void reverse(char[] s, int lo, int hi) {
    if (lo >= hi) return;                         // base case
    char t = s[lo]; s[lo] = s[hi]; s[hi] = t;
    reverse(s, lo + 1, hi - 1);                   // the smaller problem
}
```

### Line by line

| Line | What it does |
|---|---|
| `reverseString` | start with the whole array |
| `if (lo >= hi) return` | an empty or single-character range is done |
| the swap | the classic three-step swap through a temporary |
| `reverse(s, lo + 1, hi − 1)` | the range shrinks by 2, toward the base case |

- **Time O(n).**
- **Space O(n)** for n/2 stack frames.

---

## 6. Comparing the approaches

| | Loop | Recursion |
|---|---|---|
| Time | O(n) | O(n) |
| Space | **O(1)** | O(n) stack |
| Risk | none | `StackOverflowError` for very long arrays |

The file checks the recursive version against `StringBuilder.reverse()` on 500 random strings.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty array (`hi = −1`, so the base case triggers at once), a single character, even and odd lengths.

**Mistakes:**
1. A base case of `lo == hi` only (on even lengths `lo` jumps past `hi` and the recursion never stops, until it swaps out of bounds).
2. Recursing with `lo + 1, hi` (only one end moves; the swaps are wrong).
3. Building a new reversed string (the problem requires in place).

---

## 8. Interview questions and answers

**Q1. Is the recursive version O(1) space?**
> No: each call adds a stack frame, so it's O(n). Java doesn't eliminate tail calls.

**Q2. Why `lo >= hi` rather than `lo == hi`?**
> For even lengths, the indices cross (`lo = hi + 1`) without ever being equal.

### Follow-up questions

**F1. Reverse a linked list recursively.** → [Linked Lists E01](../../03-Linked-Lists/02-Easy/E01_ReverseLinkedList.md)

**F2. Reverse the words in a string.**
> Reverse the whole string, then reverse each word (two passes of the same swap routine).

**F3. Check a palindrome recursively.**
> The same two-index shape: compare the ends, then recurse on the inside.

---

## 9. Running the file

```bash
java E02_ReverseString.java      # 4/4 tests passed (including 500 random strings vs StringBuilder.reverse)
```

**One-line summary:** *"Swap the two ends and recurse on the inside until the indices meet or cross; the loop version does the same in O(1) space."*
