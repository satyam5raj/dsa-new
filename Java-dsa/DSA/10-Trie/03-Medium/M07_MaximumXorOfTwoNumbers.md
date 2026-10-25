# M07 · Maximum XOR of Two Numbers in an Array ⭐

| | |
|---|---|
| **LeetCode** | [#421 Maximum XOR of Two Numbers in an Array](https://leetcode.com/problems/maximum-xor-of-two-numbers-in-an-array/) |
| **Difficulty** | Medium |
| **Pattern** | A binary trie: greedily take the **opposite** bit from the top |
| **Code** | [M07_MaximumXorOfTwoNumbers.java](M07_MaximumXorOfTwoNumbers.java) |
| **Run** | `java M07_MaximumXorOfTwoNumbers.java` |

⭐ = a bonus classic. Trie basics are explained in [E01](../02-Easy/E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return `max(nums[i] XOR nums[j])`.

| nums | answer |
|---|---|
| [3,10,5,25,2,8] | 28 (5 XOR 25 = 00101 ^ 11001 = 11100) |
| [14,70,53,83,49,91,36,80,92,51,66,70] | 127 |
| [0] | 0 |

**Constraints:** up to 2 · 10⁵ numbers in [0, 2³¹ − 1].

---

## 2. How to think about it

### Brute force
All pairs: O(n²). With n = 2 · 10⁵, that's 2 · 10¹⁰. Too slow.

### The key fact
In binary, a 1 in bit b is worth **more** than all the lower bits together (2ᵇ > 2ᵇ − 1). So maximise the XOR **greedily from the highest bit down**: at each bit, try to make the XOR bit 1, i.e. choose a partner whose bit is the **opposite** of ours, if any number with the prefix chosen so far has it.

### A binary trie
Each number is a 31-level path of bits (from high to low), with children [0] and [1]. For a number x, walk down choosing `child[1 − bit]` when it exists (that XOR bit becomes 1), otherwise `child[bit]`. That finds x's best partner in O(31).

Insert every number, then query every number → O(31 · n).

### Dry run: x = 5 (00101) with 25 (11001) in the trie (the top 5 bits shown)

| bit | x's bit | want | available | XOR bit |
|---|---|---|---|---|
| 4 | 0 | 1 | 1 (25) | **1** |
| 3 | 0 | 1 | 1 | **1** |
| 2 | 1 | 0 | 0 | **1** |
| 1 | 0 | 1 | only 0 | 0 |
| 0 | 1 | 0 | only 1 | 0 |

→ 11100 = **28**. (Running all of [3,10,5,25,2,8]: 25 is the best partner of every other number, and 5 ↔ 25 gives the maximum, 28.)

---

## 3. Java concepts used in this problem

### 3.1 Reading bit b: `(x >> b) & 1`

`x >> b` shifts bit b into position 0; `& 1` keeps only that bit. So it's 0 or 1.

### 3.2 Setting bit b: `xor |= 1 << b`

`1 << b` is a number with only bit b set; OR-ing it in turns that bit on.

### 3.3 Why 31 levels?

The inputs are non-negative ints, so bits 30..0 are enough (bit 31 is the sign bit, always 0 here).

### 3.4 A two-child node

```java
static class Node { Node[] child = new Node[2]; }
```

The same trie idea as letters, with an alphabet of {0, 1}.

### 3.5 Bit tricks in general

See [12-Advanced](../../12-Advanced/README.md) for more bit manipulation patterns.

---

## 4. Approach 1: All pairs (the referee)

### Code ([M07_MaximumXorOfTwoNumbers.java:66-70](M07_MaximumXorOfTwoNumbers.java#L66-L70))

```java
static int brute(int[] a) {
    int best = 0;
    for (int x : a) for (int y : a) best = Math.max(best, x ^ y);
    return best;
}
```

**O(n²).**

---

## 5. Approach 2: Bit-by-bit with a HashSet of prefixes

```java
static int findMaximumXORPrefixSet(int[] nums) {
    int best = 0, mask = 0;
    for (int b = 30; b >= 0; b--) {
        mask |= 1 << b;                                      // keep the top bits down to b
        Set<Integer> prefixes = new HashSet<>();
        for (int x : nums) prefixes.add(x & mask);
        int candidate = best | (1 << b);                     // can this bit be 1 too?
        for (int p : prefixes) {
            if (prefixes.contains(p ^ candidate)) { best = candidate; break; }   // a ^ b = c  ⇔  a ^ c = b
        }
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `mask` | the top (31 − b) bits |
| `prefixes` | every number's top bits |
| `candidate` | the best so far, with bit b also set |
| `p ^ candidate` in the set | two prefixes XOR to the candidate → it's achievable |

Greedy from the top bit, using the identity a ^ b = c ⇔ a ^ c = b. **O(31 · n)** with hashing. (Stress-tested against all pairs on 6,000 random arrays.)

---

## 6. Approach 3: A binary trie (the file's main solution)

### Code ([M07_MaximumXorOfTwoNumbers.java:40-64](M07_MaximumXorOfTwoNumbers.java#L40-L64))

```java
static class Node { Node[] child = new Node[2]; }

static int findMaximumXOR(int[] nums) {
    Node root = new Node();
    for (int x : nums) {
        Node n = root;
        for (int b = 30; b >= 0; b--) {
            int bit = (x >> b) & 1;
            if (n.child[bit] == null) n.child[bit] = new Node();
            n = n.child[bit];
        }
    }
    int best = 0;
    for (int x : nums) {
        Node n = root;
        int xor = 0;
        for (int b = 30; b >= 0; b--) {
            int bit = (x >> b) & 1;
            if (n.child[1 - bit] != null) { xor |= 1 << b; n = n.child[1 - bit]; }   // opposite bit: XOR bit = 1
            else n = n.child[bit];
        }
        best = Math.max(best, xor);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| the insert loop | each number becomes a root-to-leaf path of its 31 bits |
| `child[1 − bit] != null` | some number with the same higher bits differs here → take it, the XOR bit is 1 |
| `else child[bit]` | everyone in this subtree agrees at this bit → the XOR bit is 0 |
| `best = max(...)` | the best partner XOR for x |

Every query path exists all the way down because the trie contains at least x itself.

### Complexity
- **Time O(31 · n).**
- **Space O(31 · n)** nodes.

The file runs n = 200,000 in milliseconds.

---

## 7. Comparing the approaches

| | All pairs | Prefix HashSet | Binary trie |
|---|---|---|---|
| Time | O(n²) | O(31 · n) (hashing) | O(31 · n) |
| Space | O(1) | O(n) | O(31 · n) nodes |
| Extends to "XOR with a limit" queries? | | no | **yes** ([H05](../04-Hard/H05_MaximumXorWithAnElement.md)) |

The file checks the trie against all pairs on 500 random arrays.

---

## 8. Edge cases and common mistakes

**Edge cases:** a single number (0), all equal numbers (0), numbers using bit 30.

**Mistakes:**
1. Walking the bits from low to high (greedy from the top is what makes it correct).
2. Using fewer bits than the values need.
3. Forgetting that x ^ x = 0 is always available (so the answer is ≥ 0).

---

## 9. Interview questions and answers

**Q1. Why is the greedy choice per bit correct?**
> Setting a higher bit to 1 outweighs every possible combination of the lower bits, so the best XOR always takes the opposite bit when it's available.

**Q2. Why does the opposite child always lead to a full path?**
> Every node in the trie lies on some inserted number's path, so any child that exists can be followed down to a leaf.

### Follow-up questions

**F1. Maximum XOR With an Element From an Array (queries with a limit).** → [H05](../04-Hard/H05_MaximumXorWithAnElement.md)
> Offline: sort the queries by limit and insert numbers up to the limit.

**F2. The maximum XOR of a subarray.**
> Prefix XORs: XOR(i..j) = P[j] ^ P[i − 1]; insert the prefix XORs into the trie as you go.

**F3. Count the pairs with XOR < k.**
> A binary trie with subtree counts.

---

## 10. Running the file

```bash
java M07_MaximumXorOfTwoNumbers.java      # 5/5 tests passed (including 500 random arrays vs all pairs and n = 200,000 timed)
```

**One-line summary:** *"Insert every number into a 31-level binary trie; for each number, walk from the top bit preferring the opposite bit (setting that XOR bit), and take the best XOR found."*
