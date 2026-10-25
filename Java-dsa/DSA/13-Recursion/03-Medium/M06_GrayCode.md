# M06 · Gray Code

| | |
|---|---|
| **LeetCode** | [#89 Gray Code](https://leetcode.com/problems/gray-code/) |
| **Difficulty** | Medium |
| **Pattern** | Build the answer for n from the answer for n − 1 ("reflect and prefix") |
| **Code** | [M06_GrayCode.java](M06_GrayCode.java) |
| **Run** | `java M06_GrayCode.java` |

Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem). (`leetcode.md` lists this under Backtracking; a DFS over bit flips also works, but the recursive reflection is the cleanest.)

---

## 1. The problem

Return an n-bit **Gray code**: a sequence of all 2ⁿ numbers from 0 to 2ⁿ − 1, starting at 0, where every two consecutive numbers (and also the last and the first) differ in exactly **one** bit.

| n | answer | in binary |
|---|---|---|
| 1 | [0, 1] | 0, 1 |
| 2 | [0, 1, 3, 2] | 00, 01, 11, 10 |
| 3 | [0, 1, 3, 2, 6, 7, 5, 4] | 000, 001, 011, 010, 110, 111, 101, 100 |

---

## 2. How to think about it

### The recursive construction (the reflected binary code)
```
G(0) = [0]
G(n) = G(n − 1)   followed by   REVERSE(G(n − 1)) with bit (n − 1) set
```

```
n = 1:  [0]                    + [1]                       → [0, 1]
n = 2:  [00, 01]               + [11, 10]                  → [0, 1, 3, 2]
n = 3:  [000, 001, 011, 010]   + [110, 111, 101, 100]      → [0, 1, 3, 2, 6, 7, 5, 4]
                         ↑ seam ↑
```

### Why it works
- **Inside each half**, neighbours differ by one bit, because G(n − 1) already has that property, and reversing it or setting the same top bit on every element doesn't change it.
- **At the seam**, the last element of the first half and the first element of the reflected half are the **same** (n − 1)-bit code; they differ only in the new top bit.
- **Wrapping around**, the last element is the top bit plus G(n − 1)'s first element, 0, so it's `100…0`, which differs from 0 in exactly one bit.

### The formula
The i-th Gray code is `i ^ (i >> 1)`: each output bit is the XOR of two neighbouring bits of i.

```
i = 5 = 101
i >> 1 = 010
XOR    = 111 = 7      (and indeed G(3)[5] = 7)
```

---

## 3. Java concepts used in this problem

### 3.1 Setting a bit with `|` and `<<`

```java
prev.get(i) | (1 << (n - 1))
```

`1 << (n − 1)` is a number with only bit n − 1 set. OR-ing it in sets that bit and leaves the others alone.

### 3.2 XOR `^` and shift `>>`

- `a ^ b` has a 1 wherever a and b **differ**. So `Integer.bitCount(a ^ b) == 1` means "a and b differ in exactly one bit", which is what the checker tests.
- `i >> 1` shifts right by one (drops the lowest bit).

### 3.3 Making a mutable list

```java
return new ArrayList<>(List.of(0));
```

`List.of(0)` is immutable. Wrapping it in `new ArrayList<>(...)` makes a copy that can be added to. (Here the base case's list is copied again by the caller, so it would also work without, but returning a mutable list is the safer habit.)

### 3.4 Copying a list: `new ArrayList<>(prev)`

It creates a new list with the same elements in the same order, so we can append to `result` without changing `prev` while we're still reading it backwards.

### 3.5 Checking uniqueness with a `HashSet`

`new HashSet<>(g).size() != g.size()` is true if there are duplicates: a set keeps only one copy of each value.

---

## 4. Approach 1: Recursive reflection (the file's main solution)

### Code ([M06_GrayCode.java:39-45](M06_GrayCode.java#L39-L45))

```java
static List<Integer> grayCode(int n) {
    if (n == 0) return new ArrayList<>(List.of(0));
    List<Integer> prev = grayCode(n - 1);
    List<Integer> result = new ArrayList<>(prev);
    for (int i = prev.size() - 1; i >= 0; i--) result.add(prev.get(i) | (1 << (n - 1)));   // reflect + set top bit
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `n == 0` | the 0-bit Gray code is just [0] |
| `prev` | the (n − 1)-bit code |
| `result = copy of prev` | the first half |
| the backwards loop | the second half: prev in reverse, with the new top bit set |

### Dry run: n = 3
`prev` = G(2) = [0, 1, 3, 2]. Walking it backwards (2, 3, 1, 0) and OR-ing with 4 gives 6, 7, 5, 4. Result: **[0, 1, 3, 2, 6, 7, 5, 4]** ✓

### Complexity
- **Time O(2ⁿ):** the levels produce 1 + 2 + 4 + … + 2ⁿ elements, about 2ⁿ⁺¹ in total.
- **Space O(2ⁿ)** for the output, plus O(n) recursion depth.

---

## 5. Approach 2: The formula `i ^ (i >> 1)`

The test checks the recursive output against this formula for every n from 1 to 16 ([M06_GrayCode.java:65](M06_GrayCode.java#L65)). As a standalone solution:

```java
class Formula {
    static java.util.List<Integer> grayCode(int n) {
        java.util.List<Integer> g = new java.util.ArrayList<>();
        for (int i = 0; i < (1 << n); i++) g.add(i ^ (i >> 1));
        return g;
    }
}
```

**Why consecutive values differ in one bit:** going from i to i + 1 flips a run of trailing 1s and one 0 (for example, 0111 → 1000). In `i ^ (i >> 1)`, every flipped pair of neighbouring bits cancels out except at the top of that run, so exactly one output bit changes.

- **Time O(2ⁿ), O(1) per element.**

---

## 6. The checker

### Code ([M06_GrayCode.java:47-55](M06_GrayCode.java#L47-L55))

```java
static boolean valid(int n, List<Integer> g) {
    if (g.size() != 1 << n || g.get(0) != 0 || new HashSet<>(g).size() != g.size()) return false;
    for (int i = 0; i < g.size(); i++) {
        int a = g.get(i), b = g.get((i + 1) % g.size());
        if (a < 0 || a >= (1 << n)) return false;
        if (g.size() > 1 && Integer.bitCount(a ^ b) != 1) return false;
    }
    return true;
}
```

| Check | Meaning |
|---|---|
| size 2ⁿ, no duplicates, in range | it's a permutation of 0..2ⁿ − 1 |
| starts at 0 | as required |
| `(i + 1) % size` | also compares the last element with the first |
| `bitCount(a ^ b) == 1` | neighbours differ in exactly one bit |

`g.get(0) != 0` compares an `Integer` with an `int`, so it unboxes and compares values.

---

## 7. Comparing the approaches

| | Recursive reflection | Formula |
|---|---|---|
| Time | O(2ⁿ) | O(2ⁿ) |
| Extra work | copies each level | none |
| Insight | how Gray codes are built | a neat bit identity |

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 0 ([0]), n = 1 ([0, 1]).

**Mistakes:**
1. Appending the second half in forward order (the seam then differs in more than one bit).
2. Setting bit n instead of bit n − 1.
3. Forgetting that the sequence must also wrap around (last vs first).

---

## 9. Interview questions and answers

**Q1. Why reflect the previous code?**
> Reflection makes the last element of the first half equal (in its lower bits) to the first element of the second half, so the seam differs only in the new top bit.

**Q2. Where are Gray codes used?**
> Rotary encoders and other hardware where several bits changing at once would give glitchy intermediate readings, Karnaugh maps, and error-resistant counters.

### Follow-up questions

**F1. Convert a Gray code back to binary.**
> b = g; then repeatedly `g >>= 1; b ^= g;` until g is 0 (a prefix XOR of the bits).

**F2. Circular Permutation in Binary Representation (LC 1238).**
> A Gray code that starts at a given value: XOR every element of the standard code with that start value.

**F3. Generate it with backtracking.**
> DFS from 0, flipping one bit at a time to an unvisited number; it always finds a full sequence for this problem.

---

## 10. Running the file

```bash
java M06_GrayCode.java      # 4/4 tests passed (including n = 1..16 valid and == i ^ (i >> 1))
```

**One-line summary:** *"G(n) is G(n − 1) followed by its reverse with the top bit set; equivalently, the i-th code is i ^ (i >> 1)."*
