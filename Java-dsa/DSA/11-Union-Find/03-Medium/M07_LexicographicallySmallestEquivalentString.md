# M07 · Lexicographically Smallest Equivalent String ⭐

| | |
|---|---|
| **LeetCode** | [#1061 Lexicographically Smallest Equivalent String](https://leetcode.com/problems/lexicographically-smallest-equivalent-string/) |
| **Difficulty** | Medium |
| **Pattern** | Union-Find where the root is always the **smallest** element of its set |
| **Code** | [M07_LexicographicallySmallestEquivalentString.java](M07_LexicographicallySmallestEquivalentString.java) |
| **Run** | `java M07_LexicographicallySmallestEquivalentString.java` |

⭐ = not in `leetcode.md`, added for a key technique. Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

`s1[i]` and `s2[i]` are equivalent characters (equivalence is reflexive, symmetric and transitive). Rewrite `baseStr`, replacing each character by the **smallest** character in its equivalence class.

```
s1 "parker", s2 "morris", base "parser"  →  "makkek"
classes: {m, p}, {a, o}, {k, r, s}, {e, i}
p → m,  a → a,  r → k,  s → k,  e → e
```

| s1 / s2 / base | answer |
|---|---|
| parker / morris / parser | makkek |
| hello / world / hold | hdld |
| leetcode / programs / sourcecode | aauaaaaada |

---

## 2. How to think about it

Equivalence classes → **Union-Find over the 26 letters**.

### The twist: we need the smallest member of each class
Instead of union by size, **always make the smaller root the parent**:

```
union(a, b):  ra = find(a), rb = find(b);   parent[max(ra, rb)] = min(ra, rb)
```

Then `find(x)` **is** the answer for x. (With only 26 elements, giving up union by size costs nothing.)

"Choose the root deliberately so that it carries the answer" is a common idea: the smallest or largest member, or a representative with a special meaning.

### Dry run: parker / morris
```
p–m → root m      a–o → root a      r–r (same)
k–r → root k      e–i → root e      r–s → find(r) = k, s → root k
base "parser": p→m, a→a, r→k, s→k, e→e, r→k  →  "makkek"
```

---

## 3. Java concepts used in this problem

### 3.1 Letters as indices: `c − 'a'` and back

```java
int a = find(s1.charAt(i) - 'a');                  // letter → 0..25
sb.append((char) ('a' + find(c - 'a')));            // 0..25 → letter
```

### 3.2 `Math.max` / `Math.min` to choose the parent

```java
if (a != b) parent[Math.max(a, b)] = Math.min(a, b);
```

The larger root is hung under the smaller one, so each set's root is always its smallest letter (an invariant that holds after every union).

---

## 4. Approach 1: Transitive closure (the referee)

### Code ([M07_LexicographicallySmallestEquivalentString.java:52-60](M07_LexicographicallySmallestEquivalentString.java#L52-L60))

```java
static String brute(String s1, String s2, String base) {
    boolean[][] eq = new boolean[26][26];
    for (int i = 0; i < 26; i++) eq[i][i] = true;
    for (int i = 0; i < s1.length(); i++) { int a = s1.charAt(i) - 'a', b = s2.charAt(i) - 'a'; eq[a][b] = eq[b][a] = true; }
    for (int k = 0; k < 26; k++) for (int i = 0; i < 26; i++) for (int j = 0; j < 26; j++) eq[i][j] |= eq[i][k] && eq[k][j];
    StringBuilder sb = new StringBuilder();
    for (char c : base.toCharArray()) { int x = c - 'a', m = x; for (int j = 0; j < 26; j++) if (eq[x][j]) { m = j; break; } sb.append((char) ('a' + m)); }
    return sb.toString();
}
```

| Line | What it does |
|---|---|
| `eq` | the direct equivalences, plus reflexivity |
| the triple loop | Floyd–Warshall-style closure: i ~ j if i ~ k and k ~ j |
| the scan | the smallest j equivalent to x |

**O(26³ + |s1| + |base| · 26).** It's correct by definition.

---

## 5. Approach 2: Union-Find with the smallest root (the file's main solution)

### Code ([M07_LexicographicallySmallestEquivalentString.java:33-49](M07_LexicographicallySmallestEquivalentString.java#L33-L49))

```java
static int[] parent = new int[26];

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static String smallestEquivalentString(String s1, String s2, String baseStr) {
    for (int i = 0; i < 26; i++) parent[i] = i;
    for (int i = 0; i < s1.length(); i++) {
        int a = find(s1.charAt(i) - 'a'), b = find(s2.charAt(i) - 'a');
        if (a != b) parent[Math.max(a, b)] = Math.min(a, b);       // the smaller letter stays root
    }
    StringBuilder sb = new StringBuilder();
    for (char c : baseStr.toCharArray()) sb.append((char) ('a' + find(c - 'a')));
    return sb.toString();
}
```

### Line by line

| Line | What it does |
|---|---|
| the reset | each letter alone |
| `a = find(...), b = find(...)` | the current roots (the smallest letters) of both classes |
| `parent[max] = min` | merge, keeping the smaller root |
| the rewrite | each base letter becomes its class's root |

### Dry run: hello / world / hold

| pair | roots | merge | classes |
|---|---|---|---|
| h–w | h, w | w → h | {h, w} |
| e–o | e, o | o → e | {e, o} |
| l–r | l, r | r → l | {l, r} |
| l–l | same | | |
| o–d | find(o) = e, d | e → d | {d, e, o} |

base "hold": h → h, o → d, l → l, d → d → **"hdld"** ✓

### Complexity
- **Time O((|s1| + |base|) · α).**
- **Space O(26).**

---

## 6. Comparing the approaches

| | Transitive closure | Union-Find (min root) |
|---|---|---|
| Time | O(26³ + …) | O(n · α) |
| Generalises to large alphabets? | no (cubic) | yes |

The file checks the Union-Find version against the closure on 1,000 random cases.

---

## 7. Edge cases and common mistakes

**Edge cases:** a letter equivalent only to itself, long chains linking many letters, base letters that never appear in s1/s2 (unchanged).

**Mistakes:**
1. Union by size (then the root isn't the smallest letter).
2. Setting `parent[a] = b` on the letters instead of on their roots.
3. Forgetting to reset the static array.

---

## 8. Interview questions and answers

**Q1. Why does "the smaller root becomes the parent" give the minimum?**
> Each union keeps the minimum of the two roots, and each root was its own set's minimum. By induction, every set's root is its smallest element.

**Q2. Doesn't dropping union by size make it slow?**
> The trees can get taller, but path compression keeps it fast, and with 26 elements it doesn't matter at all.

### Follow-up questions

**F1. Return the largest equivalent letter instead.**
> Keep the larger root.

**F2. Satisfiability of Equality Equations.** → [M04](M04_SatisfiabilityOfEqualityEquations.md)

**F3. Smallest String With Swaps (LC 1202).**
> Union the swappable indices; within each group, sort the characters and place them back in sorted index order.

---

## 9. Running the file

```bash
java M07_LexicographicallySmallestEquivalentString.java      # 4/4 tests passed (including 1,000 random cases vs transitive closure)
```

**One-line summary:** *"Union-Find over 26 letters where every union hangs the larger root under the smaller, so find(c) is the smallest equivalent letter; map baseStr through it."*
