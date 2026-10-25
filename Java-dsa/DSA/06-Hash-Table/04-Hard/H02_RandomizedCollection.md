# H02 · Insert Delete GetRandom O(1), Duplicates Allowed ⭐

| | |
|---|---|
| **LeetCode** | [#381 Insert Delete GetRandom O(1) - Duplicates allowed](https://leetcode.com/problems/insert-delete-getrandom-o1-duplicates-allowed/) |
| **Difficulty** | Hard |
| **Pattern** | [M06](../03-Medium/M06_InsertDeleteGetRandom.md) with value → a **set of indices** |
| **Code** | [H02_RandomizedCollection.java](H02_RandomizedCollection.java) |
| **Run** | `java H02_RandomizedCollection.java` |

⭐ = a bonus classic.

---

## 1. The problem

Like M06's RandomizedSet, but it's a **multiset**: the same value may be inserted several times.
- `insert(val)`: always adds a copy; returns `true` if val was **not** present before
- `remove(val)`: removes **one** copy; returns `false` if none exist
- `getRandom()`: returns an element, with probability **proportional to its count**

```
insert(1) → true      [1]
insert(1) → false     [1, 1]       (it was already present)
insert(2) → true      [1, 1, 2]
getRandom() → 1 with probability 2/3, 2 with probability 1/3
remove(1) → true
remove(1) → true
remove(1) → false     (no copies left)
```

All operations in average O(1).

---

## 2. How to think about it

### What changes from M06
A value can now live at **several** list positions:

```
map:  value → Set<Integer> of indices   (a HashSet: O(1) add/remove)
```

`getRandom` doesn't change: a random list index is **automatically weighted** by the counts, because a value stored twice owns two slots.

### remove(val): swap with the last, but now with index sets

```
i    = ANY index of val (take one from its set)
L    = the last list index,  last = list[L]
1. list[i] = last;   in last's index set, replace L with i
2. remove i from val's set
3. drop the last list slot
```

### The tricky case: val == last
Then valSet and lastSet are **the same set object**. The order of the updates matters:

```
valSet.remove(i);       // val no longer at i
lastSet.add(i);         // last now at i
lastSet.remove(L);      // last no longer at L
```

- **i ≠ L** (val == last, but a different copy): the set loses L and keeps i. That's correct, because the copy from L moved to i.
- **i == L** (we're removing the very last slot): remove(i), add(i), remove(i) → i is gone. Correct.

```
list [1, 1, 2]     indices {1: {0, 1}, 2: {2}}

remove(1):  i = 0, L = 2, last = 2
   valSet {0, 1} → remove 0 → {1}
   lastSet {2}   → add 0 → {0, 2} → remove 2 → {0}
   list[0] = 2 → [2, 1, 2] → drop the last → [2, 1]
   indices {1: {1}, 2: {0}}  ✓
```

---

## 3. Java concepts used in this problem

### 3.1 `computeIfAbsent`: get the set, creating it if needed

```java
Set<Integer> set = indices.computeIfAbsent(val, k -> new HashSet<>());
```

If val has no entry, it creates `new HashSet<>()`, stores it and returns it. Otherwise it returns the existing set. Then `set.add(...)` modifies the set **inside the map** (it's the same object, not a copy).

### 3.2 Getting "any" element of a set: `iterator().next()`

```java
int i = valSet.iterator().next();
```

Sets have no `get(0)`. An iterator walks the elements; `next()` returns the first one it visits. For a HashSet this is O(1) on average.

### 3.3 Objects are shared by reference

When val == last, `indices.get(val)` and `indices.get(last)` return **the same `Set` object**. Changes through one variable are visible through the other, which is exactly why the update order matters.

### 3.4 Cleaning up empty entries

`if (valSet.isEmpty()) indices.remove(val);`: without this, the map would keep empty sets around, and "is val present?" would need `isEmpty()` checks everywhere. (`insert` handles both cases anyway, via `set.isEmpty()`.)

---

## 4. Approach 1: An ArrayList only

```java
static class RandomizedCollectionList {
    private final List<Integer> values = new ArrayList<>();
    private final Random rnd = new Random();
    public boolean insert(int val) {
        boolean absent = !values.contains(val);           // O(n)
        values.add(val);
        return absent;
    }
    public boolean remove(int val) {
        return values.remove(Integer.valueOf(val));       // O(n): removes one copy
    }
    public int getRandom() { return values.get(rnd.nextInt(values.size())); }
}
```

It's correct (and getRandom is naturally weighted), but insert and remove are **O(n)**.

---

## 5. Approach 2: A list + value → set of indices (the file's solution)

### Code ([H02_RandomizedCollection.java:39-73](H02_RandomizedCollection.java#L39-L73))

```java
static class RandomizedCollection {
    private final List<Integer> values = new ArrayList<>();
    private final Map<Integer, Set<Integer>> indices = new HashMap<>();
    private final Random rnd = new Random();

    public boolean insert(int val) {
        Set<Integer> set = indices.computeIfAbsent(val, k -> new HashSet<>());
        boolean wasAbsent = set.isEmpty();
        set.add(values.size());
        values.add(val);
        return wasAbsent;
    }

    public boolean remove(int val) {
        Set<Integer> valSet = indices.get(val);
        if (valSet == null || valSet.isEmpty()) return false;
        int i = valSet.iterator().next();                 // any position holding val
        int lastIndex = values.size() - 1;
        int last = values.get(lastIndex);

        valSet.remove(i);                                 // val no longer at i
        Set<Integer> lastSet = indices.get(last);
        lastSet.add(i);                                   // 'last' moves into slot i
        lastSet.remove(lastIndex);                        // ...and leaves the last slot
        values.set(i, last);
        values.remove(lastIndex);
        // If i == lastIndex, the add then the remove cancel out, leaving the sets consistent.
        if (valSet.isEmpty()) indices.remove(val);
        return true;
    }

    public int getRandom() { return values.get(rnd.nextInt(values.size())); }

    int size() { return values.size(); }
}
```

### Line by line

| Line | What it does |
|---|---|
| `computeIfAbsent(val, …)` | val's index set, created if new |
| `wasAbsent = set.isEmpty()` | the return value: was val absent before this insert? |
| `set.add(values.size()); values.add(val);` | the new copy lives at the end |
| `if (valSet == null \|\| valSet.isEmpty())` | no copies → false |
| `int i = valSet.iterator().next();` | pick any copy to remove |
| `lastIndex`, `last` | the element that will fill the hole |
| `valSet.remove(i)` | val leaves slot i |
| `lastSet.add(i); lastSet.remove(lastIndex);` | last moves from lastIndex to i |
| `values.set(i, last); values.remove(lastIndex);` | the same move in the list; dropping the last slot is O(1) |
| `if (valSet.isEmpty()) indices.remove(val);` | tidy up |

### Dry run: insert 1, 1, 2, then remove 1 twice, then remove 1 once more

| op | list | indices | returns |
|---|---|---|---|
| insert(1) | [1] | {1: {0}} | true |
| insert(1) | [1, 1] | {1: {0, 1}} | false |
| insert(2) | [1, 1, 2] | {1: {0, 1}, 2: {2}} | true |
| remove(1): i = 0, L = 2, last = 2 | [2, 1] | {1: {1}, 2: {0}} | true |
| remove(1): i = 1, L = 1, last = 1 (**val == last, i == L**) | [2] | {2: {0}} | true |
| remove(1) | [2] | (1 has no entry) | false |

In the second removal: valSet = lastSet = {1}. remove(1) → {}; add(1) → {1}; remove(1) → {}. The list sets slot 1 to 1 (no change) and drops it → [2]. valSet is empty → the entry for 1 is removed. ✓

### Complexity
- **All operations O(1) average.**
- **Space O(n).**

---

## 6. Comparing the approaches

| | List only | List + index sets |
|---|---|---|
| insert | O(n) (the presence check) | **O(1)** |
| remove | O(n) | **O(1)** |
| getRandom | O(1) | O(1) |

The file runs 50,000 random operations over only 6 distinct values (lots of duplicates, so the val == last case happens often) against a count map, and checks that P(getRandom = 1) ≈ 2/3 in the [1, 1, 2] example.

---

## 7. Edge cases and common mistakes

**Edge cases:** removing the last slot itself, val == last with i ≠ L, the only element, many copies of one value.

**Mistakes:**
1. Updating lastSet **before** removing i from valSet when they're the same set (this leaves a stale index or loses one).
2. Using a `List<Integer>` for the indices: removing an arbitrary index from a list is O(n) (and has the remove(int) vs remove(Object) trap).
3. Forgetting that insert must return whether val was **absent**, not whether it was added (it's always added).
4. Leaving empty sets in the map and then treating "has a key" as "present".

---

## 8. Interview questions and answers

**Q1. Why is getRandom weighted correctly without extra work?**
> Every copy occupies its own list slot, so picking a uniform random slot picks each value with probability count / size.

**Q2. Why a set of indices rather than a list?**
> When an element moves from L to i, we must delete L from its owner's collection by value. That's O(1) in a HashSet but O(n) in a list.

**Q3. Walk me through the val == last case.**
> Both variables refer to the same set. Removing i, then adding i, then removing L leaves the set correct whether or not i == L (see the dry run).

### Follow-up questions

**F1. getRandom returns each distinct value with equal probability (not weighted).**
> Also keep a RandomizedSet ([M06](../03-Medium/M06_InsertDeleteGetRandom.md)) of the distinct values, updated when a value's count goes 0 → 1 or 1 → 0.

**F2. Support `getCount(val)` in O(1).**
> It's `indices.get(val).size()` (or 0 if absent).

**F3. Make removal deterministic (remove the most recently inserted copy).**
> Use a structure that gives the largest index, such as a `TreeSet` per value (O(log n)) or a stack of indices with lazy cleanup.

---

## 9. Running the file

```bash
java H02_RandomizedCollection.java      # 8/8 tests passed (including 50,000 random ops with heavy duplicates)
```

**One-line summary:** *"M06's list + swap-with-last, with value → set of indices; update the sets in the order remove(i), add(i), remove(L) so the val == last case stays correct."*
