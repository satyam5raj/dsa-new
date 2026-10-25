# M03 · Add Two Numbers

| | |
|---|---|
| **LeetCode** | [#2 Add Two Numbers](https://leetcode.com/problems/add-two-numbers/) |
| **Difficulty** | Medium |
| **Pattern** | Digit-by-digit addition with carry (+ a dummy head to build the result) |
| **Code** | [M03_AddTwoNumbers.java](M03_AddTwoNumbers.java) |
| **Run** | `java M03_AddTwoNumbers.java` |

---

## 1. The problem

Two non-negative numbers are stored as lists in **reverse order** (the ones digit first). Return their sum in the same format.

| l1 | l2 | numbers | result |
|---|---|---|---|
| `2 → 4 → 3` | `5 → 6 → 4` | 342 + 465 = 807 | `7 → 0 → 8` |
| `0` | `0` | 0 + 0 | `0` |
| `9→9→9→9→9→9→9` | `9→9→9→9` | 9999999 + 9999 = 10009998 | `8→9→9→9→0→0→0→1` |
| `5` | `5` | 10 | `0 → 1` (a final carry) |

**Constraints:** each list has 1–100 nodes; digits 0–9; no leading zeros (except the number 0).

---

## 2. How to think about it

- **Why not convert to `long`?** 100 digits overflow it (`long` holds about 19 digits).
- **School addition:** the reverse order is a **gift**. The ones digits come first, exactly where addition starts:

```
carry = 0
while l1 has digits OR l2 has digits OR carry > 0:
    sum = (l1's digit or 0) + (l2's digit or 0) + carry
    append (sum % 10) to the result
    carry = sum / 10
```

- **`OR carry > 0`** handles the final carry: `5 + 5 = 10` needs an extra node `1` at the end.
- Lists of different lengths are handled by treating a missing digit as 0.

This is [Plus One / Add Strings](../../01-Arrays/02-Easy/E12_PlusOne.md) on linked lists.

---

## 3. Java concepts used in this problem

### 3.1 `% 10` and `/ 10`

```java
sum % 10   // the last digit: 13 % 10 = 3
sum / 10   // the carry:      13 / 10 = 1  (integer division)
```

The sum of two digits plus a carry is at most 9 + 9 + 1 = 19, so the carry is always 0 or 1.

### 3.2 Building a new list with a dummy + tail

```java
ListNode dummy = new ListNode(0), tail = dummy;
tail.next = new ListNode(digit);
tail = tail.next;
...
return dummy.next;
```

This is the same "append at the tail" pattern as the `build` helper and [E02](../02-Easy/E02_MergeTwoSortedLists.md). Here we **create** new nodes, because the result is a new number.

### 3.3 `java.math.BigInteger` in the tests

```java
new java.math.BigInteger(numBits, rnd)   // a random big number with up to numBits bits
a.add(b)                                  // exact big-number addition
```

The file checks 300 random big numbers against `BigInteger`. The fully qualified name `java.math.BigInteger` is used instead of an `import`.

### 3.4 `s.chars().map(c -> c - '0').toArray()`

`String.chars()` gives an `IntStream` of the character codes; `map(c -> c - '0')` turns them into digits; `toArray()` gives an `int[]`. It's used by `fromNumber` to build a list from the digits.

---

## 4. Approach 1: Convert to numbers (fails for large inputs)

```java
static ListNode addAsLong(ListNode l1, ListNode l2) {
    long a = 0, b = 0, place = 1;
    for (ListNode c = l1; c != null; c = c.next, place *= 10) a += c.val * place;
    place = 1;
    for (ListNode c = l2; c != null; c = c.next, place *= 10) b += c.val * place;
    long s = a + b;
    ListNode dummy = new ListNode(0), t = dummy;
    do { t.next = new ListNode((int) (s % 10)); t = t.next; s /= 10; } while (s > 0);
    return dummy.next;
}
```

It works up to about 18 digits, then **overflows silently**. It's a good way to show why digit-by-digit addition is needed. (`do-while` makes sure the number 0 still produces one node.)

---

## 5. Approach 2: Digit-by-digit with carry (the file's solution)

### Code ([M03_AddTwoNumbers.java:40-52](M03_AddTwoNumbers.java#L40-L52))

```java
static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
    ListNode dummy = new ListNode(0), tail = dummy;
    int carry = 0;
    while (l1 != null || l2 != null || carry > 0) {
        int sum = carry;
        if (l1 != null) { sum += l1.val; l1 = l1.next; }
        if (l2 != null) { sum += l2.val; l2 = l2.next; }
        tail.next = new ListNode(sum % 10);
        tail = tail.next;
        carry = sum / 10;
    }
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode dummy = new ListNode(0), tail = dummy;` | the result list being built |
| `while (l1 != null \|\| l2 != null \|\| carry > 0)` | continue while there are digits left **or** a carry to place |
| `int sum = carry;` | start from the carry |
| `if (l1 != null) { sum += l1.val; l1 = l1.next; }` | add l1's digit if it has one (a missing digit counts as 0) |
| the same for `l2` | |
| `tail.next = new ListNode(sum % 10);` | this position's digit |
| `carry = sum / 10;` | carry to the next position |

### Dry run 1: 342 + 465 (`2 → 4 → 3` + `5 → 6 → 4`)

| position | l1 digit | l2 digit | carry in | sum | digit out | carry out |
|---|---|---|---|---|---|---|
| ones | 2 | 5 | 0 | 7 | 7 | 0 |
| tens | 4 | 6 | 0 | 10 | 0 | 1 |
| hundreds | 3 | 4 | 1 | 8 | 8 | 0 |

**Result `7 → 0 → 8`** (807) ✓

### Dry run 2: 5 + 5

| l1 | l2 | carry | sum | digit | carry out |
|---|---|---|---|---|---|
| 5 | 5 | 0 | 10 | 0 | 1 |
| — | — | 1 | 1 | 1 | 0 |

**Result `0 → 1`** (10) ✓. The second iteration runs **only** because of `carry > 0`.

### Dry run 3: 9999999 + 9999
The first 4 positions: 9 + 9 + carry → 8 (carry 1), then 9 each time (9 + 9 + 1 = 19). The next 3 positions: 9 + 0 + 1 = 10 → 0 (carry 1). Finally, the carry → 1. **Result `8 → 9 → 9 → 9 → 0 → 0 → 0 → 1`** ✓

### Complexity
- **Time O(max(m, n))**, **Space O(max(m, n))** for the result (no extra beyond the output).

---

## 6. Comparing the approaches

| | Convert to long | Digit by digit |
|---|---|---|
| Works for 100 digits | **no** (overflow) | **yes** |
| Time | O(m + n) | O(max(m, n)) |

---

## 7. Edge cases and common mistakes

**Edge cases:** lists of different lengths, a final carry, both zero, a long chain of carries (999 + 1).

**Mistakes:**
1. Forgetting the final carry (`5 + 5` → `0` instead of `0 → 1`).
2. Stopping when one list ends.
3. Converting to an int/long.
4. Reading `l1.val` without checking for null.

---

## 8. Interview questions and answers

**Q1. Why is the reversed order helpful?**
> Addition starts at the least significant digit, and that's exactly the head of each list, so we can add in one forward pass.

**Q2. How do you handle different lengths?**
> Treat a missing digit as 0, and keep looping while either list (or the carry) has something left.

### Follow-up questions

**F1. Add Two Numbers II (LC 445): the digits are in normal order (most significant first).**
> Push both lists onto **stacks** (or reverse them), add from the tops, and build the result by **prepending** nodes (`node.next = head; head = node`). O(m + n).

**F2. Multiply two numbers given as lists.**
> Grade-school multiplication into an array of digits, or convert to strings and use the string-multiplication algorithm (LC 43).

**F3. Subtract two numbers.**
> The same loop with a **borrow** instead of a carry; remove any leading zeros at the end.

**F4. Plus One Linked List (LC 369).**
> Find the last non-9 node; increment it and set everything after it to 0 (or recurse to the end and return the carry).

---

## 9. Running the file

```bash
java M03_AddTwoNumbers.java      # 5/5 tests passed (including 300 random BigInteger checks)
```

**One-line summary:** *"Walk both lists together: sum = digits + carry; write sum % 10, carry sum / 10; keep going while there's a carry."*
