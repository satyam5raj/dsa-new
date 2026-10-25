# M04 · Satisfiability of Equality Equations

| | |
|---|---|
| **LeetCode** | [#990 Satisfiability of Equality Equations](https://leetcode.com/problems/satisfiability-of-equality-equations/) |
| **Difficulty** | Medium |
| **Pattern** | Two passes: first union all the "==", then check every "!=" |
| **Code** | [M04_SatisfiabilityOfEqualityEquations.java](M04_SatisfiabilityOfEqualityEquations.java) |
| **Run** | `java M04_SatisfiabilityOfEqualityEquations.java` |

Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Equations like "a==b" or "b!=c" over single-letter variables. Can integers be assigned so that all of them hold?

| equations | answer | why |
|---|---|---|
| ["a==b","b!=a"] | false | |
| ["a==b","b==c","a==c"] | true | |
| ["a==b","b!=c","c==a"] | false | a = b and c = a ⇒ b = c, contradicting b ≠ c |
| ["a!=a"] | false | |

---

## 2. How to think about it

Equality is an **equivalence relation** (reflexive, symmetric, transitive), which is exactly what Union-Find groups model. The inequalities are just constraints to check.

### Order matters
Process **all** the "==" first, then all the "!=". If you checked "b!=c" before seeing "c==a", it would wrongly pass (b and c weren't connected **yet**).

```
pass 1:  for "x==y":  union(x, y)
pass 2:  for "x!=y":  if find(x) == find(y) → false
```

"a!=a" is always false (the same root), and that's handled automatically.

With only 26 variables, a 26-element parent array is enough.

---

## 3. Java concepts used in this problem

### 3.1 Parsing a fixed-format string

```java
e.charAt(0)       // the first variable
e.charAt(1)       // '=' for "==", '!' for "!="
e.charAt(3)       // the second variable
```

Every equation has exactly 4 characters, so the positions are fixed.

### 3.2 A static parent array reused across calls

`static int[] parent = new int[26];` is reinitialised at the start of each call (`parent[i] = i`), so earlier calls don't leak into later ones.

### 3.3 Inline union

```java
parent[find(e.charAt(0) - 'a')] = find(e.charAt(3) - 'a');
```

It links one root to the other in a single statement (no union by size needed for 26 elements).

---

## 4. Approach 1: Try every assignment (the referee)

### Code ([M04_SatisfiabilityOfEqualityEquations.java:51-69](M04_SatisfiabilityOfEqualityEquations.java#L51-L69))

```java
static boolean brute(String[] eqs) {
    List<Character> vars = new ArrayList<>();
    for (String e : eqs) for (char c : new char[]{e.charAt(0), e.charAt(3)}) if (!vars.contains(c)) vars.add(c);
    int k = vars.size();
    int[] val = new int[k];
    return assign(eqs, vars, val, 0, k);
}

static boolean assign(String[] eqs, List<Character> vars, int[] val, int i, int k) {
    if (i == k) {
        for (String e : eqs) {
            int a = val[vars.indexOf(e.charAt(0))], b = val[vars.indexOf(e.charAt(3))];
            if ((e.charAt(1) == '=') != (a == b)) return false;
        }
        return true;
    }
    for (int v = 0; v < k; v++) { val[i] = v; if (assign(eqs, vars, val, i + 1, k)) return true; }
    return false;
}
```

With k variables, values 0..k−1 are always enough (each class can get its own value). It tries all kᵏ assignments: tiny inputs only.

`(e.charAt(1) == '=') != (a == b)`: the equation fails when "is it ==" doesn't match "are the values equal".

---

## 5. Approach 2: Union the equalities, then check the inequalities (the file's main solution)

### Code ([M04_SatisfiabilityOfEqualityEquations.java:34-48](M04_SatisfiabilityOfEqualityEquations.java#L34-L48))

```java
static int[] parent = new int[26];

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static boolean equationsPossible(String[] equations) {
    for (int i = 0; i < 26; i++) parent[i] = i;
    for (String e : equations)                                   // pass 1: all equalities
        if (e.charAt(1) == '=') parent[find(e.charAt(0) - 'a')] = find(e.charAt(3) - 'a');
    for (String e : equations)                                   // pass 2: check inequalities
        if (e.charAt(1) == '!' && find(e.charAt(0) - 'a') == find(e.charAt(3) - 'a')) return false;
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| the reset | 26 singleton groups |
| pass 1 | every "x==y" merges x's and y's groups |
| pass 2 | an "x!=y" with x and y in the same group is a contradiction |
| `return true` | assign one distinct value per group: everything holds |

### Dry run: ["a==b","b!=c","c==a"]

| pass | equation | action | groups |
|---|---|---|---|
| 1 | a==b | union | {a, b} |
| 1 | b!=c | (skipped in pass 1) | |
| 1 | c==a | union | {a, b, c} |
| 2 | b!=c | find(b) == find(c) | **false** |

### Complexity
- **Time O(n · α).**
- **Space O(26).**

---

## 6. Comparing the approaches

| | Every assignment | Two-pass Union-Find |
|---|---|---|
| Time | O(kᵏ · n) | **O(n)** |
| Space | O(k) | O(26) |

The file checks the Union-Find version against every assignment on 1,000 random systems.

---

## 7. Edge cases and common mistakes

**Edge cases:** "a!=a" (false), "a==a" (true), only inequalities (true unless one is "x!=x"), long chains of equalities.

**Mistakes:**
1. Processing the equations in input order in a single pass (the order trap).
2. Trying to "union" inequalities.
3. Forgetting to reset the static array between calls.

---

## 8. Interview questions and answers

**Q1. Why must all the equalities come first?**
> An inequality can only be checked against the **final** equivalence classes. A later equality might merge the two sides.

**Q2. Why is a satisfying assignment guaranteed once no inequality conflicts?**
> Give each group a distinct integer. Every equality holds (both sides are in the same group), and every inequality holds (the sides are in different groups).

### Follow-up questions

**F1. Inequalities like "a<b".**
> That's an ordering: build a directed graph and check for a cycle (topological sort), combined with the equality groups.

**F2. Return a valid assignment.**
> Map each root to a distinct number.

**F3. Lexicographically Smallest Equivalent String.** → [M07](M07_LexicographicallySmallestEquivalentString.md)
> Equivalence classes of letters again, with the smallest letter as the root.

---

## 9. Running the file

```bash
java M04_SatisfiabilityOfEqualityEquations.java      # 5/5 tests passed (including 1,000 random systems vs all assignments)
```

**One-line summary:** *"Union every 'x==y' first; then any 'x!=y' whose two variables share a root makes the system unsatisfiable."*
