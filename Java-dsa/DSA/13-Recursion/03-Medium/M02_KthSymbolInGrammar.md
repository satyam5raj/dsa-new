# M02 · K-th Symbol in Grammar ⭐

| | |
|---|---|
| **LeetCode** | [#779 K-th Symbol in Grammar](https://leetcode.com/problems/k-th-symbol-in-grammar/) |
| **Difficulty** | Medium |
| **Pattern** | Recurse on the **parent**; never build the huge row |
| **Code** | [M02_KthSymbolInGrammar.java](M02_KthSymbolInGrammar.java) |
| **Run** | `java M02_KthSymbolInGrammar.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Row 1 is `0`. Each next row replaces every `0` with `01` and every `1` with `10`:

```
row 1: 0
row 2: 01
row 3: 0110
row 4: 01101001
```

Return the k-th symbol (1-indexed) of row n.

| n | k | answer |
|---|---|---|
| 1 | 1 | 0 |
| 2 | 2 | 1 |
| 4 | 6 | 0 |

---

## 2. How to think about it

### Brute force
Build row n. It has 2ⁿ⁻¹ symbols, so for n = 30 that's about 500 million characters. Too much.

### Every symbol has one parent
Each symbol in row n comes from exactly **one** symbol in row n − 1:
- positions 1 and 2 come from parent 1, positions 3 and 4 from parent 2, and so on: **parent = (k + 1) / 2**;
- a parent `0` makes `01` and a parent `1` makes `10`, so:
  - an **odd** k (the left child) is the **same** as its parent;
  - an **even** k (the right child) is the **opposite** of its parent.

```
f(1, 1) = 0
f(n, k) = f(n − 1, (k + 1) / 2)         if k is odd
f(n, k) = 1 − f(n − 1, (k + 1) / 2)     if k is even
```

It's like walking up a binary tree from a leaf to the root: n levels, never building a row.

```
            0                  row 1
         /     \
        0       1              row 2
       / \     / \
      0   1   1   0            row 3
     / \ / \ / \ / \
     0 1 1 0 1 0 0 1           row 4     (k = 6 is the 0 under the right-hand 1 of row 3)
```

### Dry run: n = 4, k = 6

| call | k parity | parent call | result |
|---|---|---|---|
| f(4, 6) | even → flip | f(3, 3) | 1 − 1 = **0** |
| f(3, 3) | odd → same | f(2, 2) | 1 |
| f(2, 2) | even → flip | f(1, 1) | 1 − 0 = 1 |
| f(1, 1) | base | | 0 |

### The bit view (O(1))
Write k − 1 in binary with n − 1 bits. Read from the top, it's the path from the root: 0 = go left (keep the value), 1 = go right (flip it). The root is 0, so the answer is the **parity of the number of 1 bits in k − 1**.

n = 4, k = 6: k − 1 = 5 = `101`, which has two 1 bits → even → **0** ✓

---

## 3. Java concepts used in this problem

### 3.1 Integer division rounding up: `(k + 1) / 2`

`k / 2` rounds down, but the parent of k is ⌈k / 2⌉. Adding 1 first turns integer division into a round-up for positive k: (5 + 1) / 2 = 3 and (6 + 1) / 2 = 3.

### 3.2 `1 − x` to flip a bit

For x ∈ {0, 1}, `1 − x` swaps 0 and 1. (`x ^ 1` does the same.)

### 3.3 `Integer.bitCount`

`Integer.bitCount(v)` returns the number of 1 bits in v (the "popcount"), usually as a single CPU instruction. `% 2` gives its parity.

### 3.4 `char` − `'0'`

`row.charAt(k − 1) - '0'` turns the character `'0'` or `'1'` into the number 0 or 1, because digit characters have consecutive codes.

---

## 4. Approach 1: Build the rows (the referee)

### Code ([M02_KthSymbolInGrammar.java:51-59](M02_KthSymbolInGrammar.java#L51-L59))

```java
static String buildRow(int n) {
    String row = "0";
    for (int i = 1; i < n; i++) {
        StringBuilder sb = new StringBuilder();
        for (char c : row.toCharArray()) sb.append(c == '0' ? "01" : "10");
        row = sb.toString();
    }
    return row;
}
```

| Line | What it does |
|---|---|
| `row = "0"` | row 1 |
| the outer loop | build rows 2..n |
| the inner loop | replace each symbol with its two children |

- **Time and space O(2ⁿ).** The test only uses it for n ≤ 14.

---

## 5. Approach 2: Recurse on the parent (the file's main solution)

### Code ([M02_KthSymbolInGrammar.java:43-47](M02_KthSymbolInGrammar.java#L43-L47))

```java
static int kthGrammar(int n, int k) {
    if (n == 1) return 0;
    int parent = kthGrammar(n - 1, (k + 1) / 2);
    return k % 2 == 1 ? parent : 1 - parent;
}
```

### Line by line

| Line | What it does |
|---|---|
| `n == 1` | row 1 is just `0` |
| `parent = ...` | the symbol one row up that produced position k |
| the return | the left child copies the parent, the right child flips it |

- **Time O(n), space O(n)** for the recursion.

---

## 6. Approach 3: The bit trick

### Code ([M02_KthSymbolInGrammar.java:49](M02_KthSymbolInGrammar.java#L49))

```java
static int bitTrick(int n, int k) { return Integer.bitCount(k - 1) % 2; }
```

Each 1 bit in k − 1 is one "right child" step from the root, and each such step flips the value. n isn't even needed, as long as k is a valid position in row n.

- **Time O(1), space O(1).**

---

## 7. Comparing the approaches

| | Build the rows | Recurse on the parent | Bit count |
|---|---|---|---|
| Time | O(2ⁿ) | O(n) | **O(1)** |
| Space | O(2ⁿ) | O(n) | O(1) |

The file builds rows 1..14 and checks every position against both fast versions, and runs n = 30, k = 2²⁹ without building anything.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1, k = 1 (always 0), k = 2ⁿ⁻¹ (the last symbol), n = 30.

**Mistakes:**
1. Building the row.
2. Using `k / 2` as the parent (wrong for odd k).
3. Mixing up 0-indexed and 1-indexed positions in the bit trick (it's k − 1).

---

## 9. Interview questions and answers

**Q1. Why doesn't the recursion need to know the row's contents?**
> Each symbol depends only on its parent and on whether it's a left or right child. That's a single path up the tree.

**Q2. Explain the bit trick.**
> k − 1 in binary encodes the left/right path from the root. Going right flips the bit and going left keeps it, so the answer is the parity of the number of right moves, which is the number of 1 bits.

### Follow-up questions

**F1. What's this sequence?**
> The Thue–Morse sequence: t(i) = popcount(i) mod 2.

**F2. Count the 1s in the first k symbols of row n.**
> Recurse on halves: the first half of row n is row n − 1, and the second half is its complement.

**F3. Tower of Hanoi.** → [E04](../02-Easy/E04_TowerOfHanoi.md)
> Another problem where a binary structure makes the answer for move k directly computable.

---

## 10. Running the file

```bash
java M02_KthSymbolInGrammar.java      # 5/5 tests passed (including n = 30 without building, and rows 1..14 compared)
```

**One-line summary:** *"The k-th symbol equals its parent ((k + 1) / 2 in the row above) if k is odd and its complement if k is even; equivalently, it's the parity of the 1 bits in k − 1."*
