# H02 · K-th Smallest in Lexicographical Order ⭐

| | |
|---|---|
| **LeetCode** | [#440 K-th Smallest in Lexicographical Order](https://leetcode.com/problems/k-th-smallest-in-lexicographical-order/) |
| **Difficulty** | Hard |
| **Pattern** | Walk a 10-ary "prefix tree" of numbers, **counting whole subtrees** so they can be skipped |
| **Code** | [H02_KthLexicographicalNumber.java](H02_KthLexicographicalNumber.java) |
| **Run** | `java H02_KthLexicographicalNumber.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem); tries are covered in [10-Trie](../../10-Trie/02-Easy/E01_CountingWordsWithPrefix.md).

---

## 1. The problem

Sort the numbers 1..n as **strings** (lexicographically) and return the k-th one. n and k can be up to 10⁹.

```
n = 13:  1, 10, 11, 12, 13, 2, 3, 4, 5, 6, 7, 8, 9
k = 2  →  10
```

| n | k | answer |
|---|---|---|
| 13 | 2 | 10 |
| 1 | 1 | 1 |
| 100 | 10 | 17 |
| 10⁹ | 10⁹ | 999999999 |

---

## 2. How to think about it

### Brute force
Generate all the numbers as strings and sort them: O(n log n) time and O(n) memory. For n = 10⁹ that's impossible.

### The tree
The numbers form a 10-ary tree by prefix: the root's children are 1..9, and node x has children 10x .. 10x + 9 (those that are ≤ n).

```
              (root)
     /    /   ...   \
    1     2   ...    9
  / | \
10 11 .. 19
 |
100 ...
```

Lexicographic order is exactly the **preorder** of this tree (like a trie): 1, 10, 100, …, then 11, … We need the k-th node of the preorder **without visiting k nodes**.

### Counting a subtree
How many numbers in [1, n] start with prefix p? Count level by level:

```
level 0:  [p, p + 1)
level 1:  [10p, 10(p + 1))
level 2:  [100p, 100(p + 1))
...
each level adds  min(n + 1, next) − cur,  for cur ≤ n
```

That's O(log n) per count.

### The walk
```
cur = 1, k −= 1                            (we're standing on the 1st number)
while k > 0:
    steps = count(cur)                     the size of cur's whole subtree
    if steps ≤ k:  SKIP the subtree  → cur = cur + 1 (next sibling),  k −= steps
    else:          GO DOWN           → cur = cur · 10 (first child),  k −= 1
```

Each step either skips a whole subtree or descends one level, so there are O(log n) steps per level and O(log n) levels: **O(log² n)** in total.

### Dry run: n = 100, k = 10 (from a real run)

| cur | k before | count(cur) | decision |
|---|---|---|---|
| 1 | 9 | 12 (1, 10–19, 100) | 12 > 9 → **down** to 10, k = 8 |
| 10 | 8 | 2 (10, 100) | 2 ≤ 8 → **skip** to 11, k = 6 |
| 11 | 6 | 1 | skip to 12, k = 5 |
| 12 | 5 | 1 | skip to 13, k = 4 |
| 13 | 4 | 1 | skip to 14, k = 3 |
| 14 | 3 | 1 | skip to 15, k = 2 |
| 15 | 2 | 1 | skip to 16, k = 1 |
| 16 | 1 | 1 | skip to 17, k = 0 |

Answer **17** ✓. The order is 1, 10, 100, 11, 12, 13, 14, 15, 16, **17**.

**count(1) for n = 100, step by step:** level [1, 2): 1 number; level [10, 20): 10 numbers; level [100, 200): min(101, 200) − 100 = 1 number; the next level starts at 1000 > 100, so stop. Total **12**.

---

## 3. Java concepts used in this problem

### 3.1 `long` to avoid overflow

```java
long cur = 1;
long count = 0, cur = prefix, next = prefix + 1;
```

`cur * 10` and `next * 10` can exceed `int` for n near 10⁹ (for example, next = 10⁹ · 10). Using `long` for the walk and the count keeps every intermediate value safe. The final answer is ≤ n, so `(int) cur` is safe.

### 3.2 Mixing `int` and `long`

`steps <= k` compares a `long` with an `int` (the `int` is widened), and `k -= steps` works because compound assignment casts the result back to `int` automatically. That's safe here because `steps ≤ k` in that branch.

### 3.3 `Math.min(n + 1, next) − cur`

The numbers on one level are [cur, next). Clipping the end to n + 1 counts only the ones ≤ n.

### 3.4 Sorting strings (in the referee)

`Arrays.sort(String[])` sorts lexicographically by character codes: `"10" < "2"` because `'1' < '2'`.

---

## 4. Approach 1: Sort the strings (the referee)

### Code ([H02_KthLexicographicalNumber.java:70-75](H02_KthLexicographicalNumber.java#L70-L75))

```java
static int brute(int n, int k) {
    String[] s = new String[n];
    for (int i = 1; i <= n; i++) s[i - 1] = String.valueOf(i);
    Arrays.sort(s);
    return Integer.parseInt(s[k - 1]);
}
```

- **Time O(n log n · digits), space O(n).** The test uses it for n ≤ 3,000.

---

## 5. Approach 2: Count subtrees and walk the prefix tree (the file's main solution)

### Code ([H02_KthLexicographicalNumber.java:43-68](H02_KthLexicographicalNumber.java#L43-L68))

```java
static int findKthNumber(int n, int k) {
    long cur = 1;
    k--;                                            // standing on "1" already
    while (k > 0) {
        long steps = countPrefix(n, cur);
        if (steps <= k) {                           // the answer is NOT in cur's subtree
            k -= steps;
            cur++;                                  // next sibling
        } else {
            k--;                                    // step onto the first child
            cur *= 10;
        }
    }
    return (int) cur;
}

// how many numbers in [1, n] have `prefix` as a prefix
static long countPrefix(long n, long prefix) {
    long count = 0, cur = prefix, next = prefix + 1;
    while (cur <= n) {
        count += Math.min(n + 1, next) - cur;       // this level's numbers inside [1, n]
        cur *= 10;
        next *= 10;
    }
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `cur = 1`, `k--` | the preorder starts at 1; k now counts the steps still to take |
| `steps` | how many numbers lie in cur's subtree (cur itself included) |
| `steps <= k` | the k-th number is after this whole subtree: jump to the next sibling |
| else | it's inside: move to the first child, which is one step further in preorder |
| `countPrefix` | sums the clipped size of each level of the subtree |

**Why `cur++` is always valid when skipping:** if cur ends in 9 (like 19), cur + 1 = 20 is not 19's sibling, but it **is** the next prefix in preorder after 19's subtree is finished and we've gone back up. That's exactly what the walk needs, because preorder continues with the next prefix.

Although the file uses a loop, it's the iterative form of a preorder DFS over the tree, where `countPrefix` lets us skip entire recursive calls at once.

### Complexity
- **Time O(log² n):** O(log n) steps per level, O(log n) levels, and each count is O(log n) — in practice at most about 10 skips per level.
- **Space O(1).**

---

## 6. Comparing the approaches

| | Sort strings | Count and walk |
|---|---|---|
| Time | O(n log n) | **O(log² n)** |
| Space | O(n) | O(1) |
| n = 10⁹ | impossible | instant |

The file checks 500 random (n, k) pairs against sorting and runs n = k = 10⁹ instantly.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1, k = 1 (the answer is 1), k = n (the largest in lexicographic order, such as 999999999 for n = 10⁹), n a power of 10.

**Mistakes:**
1. `int` for `cur` or `next` in the count (they overflow near 10⁹).
2. Forgetting `k--` at the start (off by one).
3. Counting the subtree without clipping to n.
4. Descending with `k −= steps` instead of `k −= 1`.

---

## 8. Interview questions and answers

**Q1. Why is lexicographic order the preorder of the prefix tree?**
> Comparing strings character by character means every number with prefix p comes right after p and before p's next sibling, which is exactly how preorder visits a node, then its whole subtree, then its sibling.

**Q2. Why is counting a subtree O(log n)?**
> The subtree has one level per extra digit, and there are at most about 10 digits.

### Follow-up questions

**F1. List all of 1..n in lexicographic order (LC 386).**
> A preorder DFS: from cur, go to cur · 10 if ≤ n; otherwise go to cur + 1, first going up (cur /= 10) while cur ends in 9 or cur + 1 > n. O(n) time, O(1) extra space.

**F2. The same question for words in a dictionary.**
> Build a trie with a subtree count in every node, and walk it the same way.

**F3. Kth Symbol in Grammar.** → [M02](../03-Medium/M02_KthSymbolInGrammar.md)
> Another "find the k-th element of a huge implicit tree without building it".

---

## 9. Running the file

```bash
java H02_KthLexicographicalNumber.java      # 5/5 tests passed (including n = k = 10^9 and 500 random cases vs sorting)
```

**One-line summary:** *"Treat 1..n as a 10-ary prefix tree whose preorder is lexicographic order; from cur = 1, skip whole subtrees when their size is ≤ k (cur + 1) and otherwise descend (cur · 10) until k reaches 0."*
