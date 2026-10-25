# M06 · Map Sum Pairs ⭐

| | |
|---|---|
| **LeetCode** | [#677 Map Sum Pairs](https://leetcode.com/problems/map-sum-pairs/) |
| **Difficulty** | Medium |
| **Pattern** | Trie nodes store an aggregate (a prefix sum); updates push a **delta** |
| **Code** | [M06_MapSumPairs.java](M06_MapSumPairs.java) |
| **Run** | `java M06_MapSumPairs.java` |

⭐ = a bonus classic. Prefix counts in nodes are introduced in [E01](../02-Easy/E01_CountingWordsWithPrefix.md).

---

## 1. The problem

`insert(key, val)` (overwriting an existing key's value); `sum(prefix)` = the sum of the values of all keys that start with `prefix`.

```
insert("apple", 3);   sum("ap") → 3
insert("app", 2);     sum("ap") → 5
insert("apple", 5);   sum("ap") → 7     (apple's 3 is REPLACED by 5)
sum("b")              → 0
```

---

## 2. How to think about it

### Naive
A HashMap of keys; `sum(prefix)` scans all the keys: O(N · L) per query.

### A trie with sums
`node.sum` = the total value of all keys passing through the node. Then `sum(prefix)` is just the sum stored at the prefix's node: **O(L)**.

### The overwrite trap
Inserting "apple" again must **not** add 5 on top of the 3. Keep a HashMap key → current value; compute `delta = val − old` (0 if the key is new), and add the **delta** along the path. (The same idea as Fenwick tree updates: [Trees M16](../../07-Trees/03-Medium/M16_RangeSumQueryMutable.md).)

### Dry run

| operation | delta | sums along the path | node "ap" |
|---|---|---|---|
| insert apple 3 | +3 | a, p, p, l, e each +3 | 3 |
| insert app 2 | +2 | a, p, p each +2 | 5 |
| insert apple 5 | 5 − 3 = **+2** | a, p, p, l, e each +2 | **7** |

---

## 3. Java concepts used in this problem

### 3.1 Two structures working together

```java
private final Node root = new Node();                          // prefix sums
private final Map<String, Integer> values = new HashMap<>();   // each key's current value
```

The trie answers prefix questions; the map remembers the old values needed to compute deltas.

### 3.2 `getOrDefault` for new keys

```java
int delta = val - values.getOrDefault(key, 0);
```

A new key has an "old value" of 0, so its delta is just val.

### 3.3 Negative deltas

Lowering a value (e.g. from 5 to 1) pushes a negative delta, and the sums go down correctly. No special case needed.

---

## 4. Approach 1: Scan the map on every query

```java
static class MapSumScan {
    private final Map<String, Integer> values = new HashMap<>();
    public void insert(String key, int val) { values.put(key, val); }
    public int sum(String prefix) {
        int s = 0;
        for (Map.Entry<String, Integer> e : values.entrySet()) if (e.getKey().startsWith(prefix)) s += e.getValue();
        return s;
    }
}
```

The overwrite is handled naturally by `put`. `sum` is **O(N · L)**. (The file's random test uses this as its referee.)

---

## 5. Approach 2: Trie of prefix sums + a delta (the file's main solution)

### Code ([M06_MapSumPairs.java:38-62](M06_MapSumPairs.java#L38-L62))

```java
static class MapSum {
    private static class Node { Node[] next = new Node[26]; int sum; }
    private final Node root = new Node();
    private final Map<String, Integer> values = new HashMap<>();

    public void insert(String key, int val) {
        int delta = val - values.getOrDefault(key, 0);    // replace, don't add
        values.put(key, val);
        Node n = root;
        for (char c : key.toCharArray()) {
            if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
            n = n.next[c - 'a'];
            n.sum += delta;
        }
    }

    public int sum(String prefix) {
        Node n = root;
        for (char c : prefix.toCharArray()) {
            n = n.next[c - 'a'];
            if (n == null) return 0;
        }
        return n.sum;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `delta = val − old` | how much this key's contribution changes |
| `values.put(key, val)` | remember the new value for the next overwrite |
| the walk with `n.sum += delta` | every prefix of the key gains the delta |
| `sum`: the walk | a missing node → no key has this prefix → 0 |
| `return n.sum` | the precomputed total for this prefix |

### Complexity
- **insert O(L), sum O(L).**
- **Space O(total characters × 26)** plus the map.

---

## 6. Comparing the approaches

| | Scan the map | Trie with sums |
|---|---|---|
| insert | O(L) | O(L) |
| sum | O(N · L) | **O(L)** |

The file checks the trie against scanning a map on 10,000 random operations.

---

## 7. Edge cases and common mistakes

**Edge cases:** overwriting a key, a key that's a prefix of another, a prefix that matches nothing, a value lowered to 0.

**Mistakes:**
1. Adding `val` instead of the delta on an overwrite.
2. Updating the root's sum (harmless, but unused) or skipping the last node's update.
3. Forgetting to store the new value in the map.

---

## 8. Interview questions and answers

**Q1. Why push a delta instead of the new value?**
> Each node's sum is an aggregate over many keys. Replacing one key's value changes that aggregate by exactly the difference.

**Q2. How would you delete a key?**
> Push a delta of −old along its path and remove it from the map.

### Follow-up questions

**F1. Range Sum Query - Mutable.** → [Trees M16](../../07-Trees/03-Medium/M16_RangeSumQueryMutable.md)
> The same "push the difference" update.

**F2. The maximum value with a prefix.**
> Max isn't invertible: store each node's max and recompute it from the children (or keep a multiset) on updates.

**F3. Count the keys with a prefix.** → [E01](../02-Easy/E01_CountingWordsWithPrefix.md)

---

## 9. Running the file

```bash
java M06_MapSumPairs.java      # 5/5 tests passed (including 10,000 random ops vs scanning a map)
```

**One-line summary:** *"Each trie node holds the total value of the keys through it; insert pushes delta = new − old along the key's path (using a map of the current values), and sum reads the prefix node."*
