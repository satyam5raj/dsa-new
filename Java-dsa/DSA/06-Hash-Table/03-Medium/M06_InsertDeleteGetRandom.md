# M06 · Insert Delete GetRandom O(1) ⭐

| | |
|---|---|
| **LeetCode** | [#380 Insert Delete GetRandom O(1)](https://leetcode.com/problems/insert-delete-getrandom-o1/) |
| **Difficulty** | Medium |
| **Pattern** | A HashMap (value → index) + an ArrayList, with swap-with-last deletion |
| **Code** | [M06_InsertDeleteGetRandom.java](M06_InsertDeleteGetRandom.java) |
| **Run** | `java M06_InsertDeleteGetRandom.java` |

⭐ = a bonus classic (a very common design question).

---

## 1. The problem

Design a set with all three operations in **average O(1)**:
- `insert(val)`: add it; return `false` if it was already present
- `remove(val)`: delete it; return `false` if it was absent
- `getRandom()`: return a random element, **each equally likely**

```
insert(1) → true
remove(2) → false
insert(2) → true
getRandom() → 1 or 2 (50/50)
remove(1) → true
insert(2) → false
getRandom() → 2
```

---

## 2. How to think about it

### Why one structure isn't enough

| | insert | remove | getRandom |
|---|---|---|---|
| `HashSet` | O(1) | O(1) | ✗ (no random index access) |
| `ArrayList` | O(1) (append) | ✗ O(n) search + O(n) shift | O(1): `list.get(random index)` |

### Combine them
- **list:** the values, for random access
- **map:** value → its index in the list, so we can find it in O(1)

### The trick for O(1) remove
Removing from the **middle** of an ArrayList shifts everything after it. Instead:
1. Move the **last** element into the hole (overwrite the removed value's slot).
2. Update the moved element's index in the map.
3. Remove the last slot: O(1), nothing shifts.

A set has no order, so moving elements around is allowed.

```
list [5, 8, 3]    map {5:0, 8:1, 3:2}        remove(5)

  i = map[5] = 0,  last = 3
  1. list[0] = 3            → [3, 8, 3]
  2. map[3] = 0             → {5:0, 8:1, 3:0}
  3. drop the last slot     → [3, 8]
     remove 5 from the map  → {8:1, 3:0}
```

---

## 3. Java concepts used in this problem

### 3.1 `ArrayList` operations and their costs

| Call | Cost | Note |
|---|---|---|
| `add(x)` | O(1) amortised | appends at the end |
| `get(i)`, `set(i, x)` | O(1) | random access by index |
| `remove(size() − 1)` | **O(1)** | removing the last element shifts nothing |
| `remove(i)` in the middle | O(n) | shifts everything after i |

### 3.2 `java.util.Random`

```java
private final Random rnd = new Random();
// rnd.nextInt(bound)  →  a uniform random int in [0, bound)
```

`values.get(rnd.nextInt(values.size()))` picks each element with probability 1/size. (`new Random(seed)` gives a repeatable sequence; the tests use seeds.)

### 3.3 `remove(int index)` on a `List<Integer>`

`values.remove(values.size() - 1)` passes an **int**, so it's the **index** overload: exactly what we want here. Compare with [E03](../02-Easy/E03_DesignHashSet.md), where removing by value needed `Integer.valueOf`.

### 3.4 `containsKey` vs `get` for presence

`insert` uses `containsKey`; `remove` uses `get` and checks for null, because it needs the index anyway. One lookup either way.

---

## 4. Approach 1: An ArrayList only

```java
static class RandomizedSetList {
    private final List<Integer> values = new ArrayList<>();
    private final Random rnd = new Random();
    public boolean insert(int val) {
        if (values.contains(val)) return false;      // O(n) search
        values.add(val);
        return true;
    }
    public boolean remove(int val) {
        return values.remove(Integer.valueOf(val));  // O(n) search + O(n) shift
    }
    public int getRandom() { return values.get(rnd.nextInt(values.size())); }
}
```

It's correct, but insert and remove are **O(n)**. (A HashSet alone fails the other way: getRandom would need O(n) iteration to reach a random position.)

---

## 5. Approach 2: Map + list with swap-with-last (the file's solution)

### Code ([M06_InsertDeleteGetRandom.java:42-70](M06_InsertDeleteGetRandom.java#L42-L70))

```java
static class RandomizedSet {
    private final List<Integer> values = new ArrayList<>();
    private final Map<Integer, Integer> indexOf = new HashMap<>();
    private final Random rnd = new Random();

    public boolean insert(int val) {
        if (indexOf.containsKey(val)) return false;
        indexOf.put(val, values.size());
        values.add(val);
        return true;
    }

    public boolean remove(int val) {
        Integer i = indexOf.get(val);
        if (i == null) return false;
        int last = values.get(values.size() - 1);
        values.set(i, last);                       // 1. move the last element into the hole
        indexOf.put(last, i);                      // 2. fix its index
        values.remove(values.size() - 1);          // 3. drop the last slot: O(1)
        indexOf.remove(val);                       //    (do this AFTER step 2, in case val == last)
        return true;
    }

    public int getRandom() {
        return values.get(rnd.nextInt(values.size()));
    }

    int size() { return values.size(); }
}
```

### Line by line

| Line | What it does |
|---|---|
| `insert`: `containsKey` | already present → false |
| `indexOf.put(val, values.size());` | the new value's index is the current size (where it's about to be appended) |
| `values.add(val);` | append |
| `remove`: `Integer i = indexOf.get(val);` | where val lives (null → absent) |
| `int last = values.get(values.size() - 1);` | the element that will fill the hole |
| `values.set(i, last);` | overwrite val's slot with last |
| `indexOf.put(last, i);` | last now lives at i |
| `values.remove(values.size() - 1);` | drop the duplicate at the end, O(1) |
| `indexOf.remove(val);` | val is gone |
| `getRandom` | a uniform random index |

### Why `indexOf.remove(val)` must come last
If val **is** the last element, then `last == val`. Step 2 does `indexOf.put(val, i)`. If we had removed val from the map **before** that, step 2 would put it back, leaving a stale entry. Removing it **after** step 2 cleans up correctly in both cases.

### Dry run 1: remove from the middle

| op | list | map | returns |
|---|---|---|---|
| insert(5) | [5] | {5:0} | true |
| insert(8) | [5, 8] | {5:0, 8:1} | true |
| insert(3) | [5, 8, 3] | {5:0, 8:1, 3:2} | true |
| remove(5): i = 0, last = 3 | set → [3, 8, 3] | put 3→0 → {5:0, 8:1, 3:0} | |
| | drop last → [3, 8] | remove 5 → {8:1, 3:0} | true |

### Dry run 2: remove the last element (val == last)

list [3, 8], map {8:1, 3:0}; **remove(8)**:
- i = 1, last = 8
- `values.set(1, 8)` → [3, 8] (no change)
- `indexOf.put(8, 1)` → no change
- drop the last slot → [3]
- `indexOf.remove(8)` → {3:0} ✓

### Dry run 3: the file's example
insert(1) → [1]; remove(2) → absent, false; insert(2) → [1, 2]; insert(2) → false; remove(1): i = 0, last = 2 → [2], {2:0}; getRandom → **2** ✓

### Complexity
- **insert / remove / getRandom: O(1) average** (the hash map is average O(1); the ArrayList append is amortised O(1)).
- **Space O(n).**

---

## 6. Comparing the approaches

| | List only | HashSet only | Map + list |
|---|---|---|---|
| insert | O(n) | O(1) | **O(1)** |
| remove | O(n) | O(1) | **O(1)** |
| getRandom | O(1) | O(n) | **O(1)** |

The file checks 50,000 random operations against a `HashSet` and verifies uniformity: 40,000 draws over 4 values come out at about 10,000 each.

---

## 7. Edge cases and common mistakes

**Edge cases:** removing the only element, removing the last element (val == last), inserting a duplicate, removing an absent value.

**Mistakes:**
1. Removing val from the map **before** fixing last's index (breaks the val == last case).
2. Forgetting to update the moved element's index in the map.
3. `values.remove(Integer.valueOf(val))`, which is O(n) and defeats the purpose.
4. Storing the indices in a `HashSet`, so you can't find an element's position.

---

## 8. Interview questions and answers

**Q1. Why can we reorder the list?**
> It represents a set, so order carries no meaning. Only membership and uniform random access matter.

**Q2. Is getRandom truly uniform?**
> Yes. The list holds each element exactly once, and `nextInt(size)` picks every index with probability 1/size.

**Q3. What's the worst case?**
> The hash map's worst case is O(n) (or O(log n) with Java's treeified buckets), and an ArrayList resize is O(n) occasionally. Both are O(1) amortised/average, which is what the problem asks for.

### Follow-up questions

**F1. Allow duplicates (LC 381).** → [H02](../04-Hard/H02_RandomizedCollection.md)
> The map becomes value → a **set** of indices. On removal, take any index of val, and move the last element there, updating **its** index set.

**F2. getRandom weighted by a value's frequency.**
> That's exactly what the duplicates version gives: each copy occupies a slot in the list.

**F3. Weighted random by arbitrary weights (LC 528).**
> Prefix sums of the weights plus a binary search for a random number in [0, total).

---

## 9. Running the file

```bash
java M06_InsertDeleteGetRandom.java      # 8/8 tests passed (including 50,000 random ops and a uniformity check)
```

**One-line summary:** *"An ArrayList for O(1) random access plus a map of value → index; delete by moving the last element into the hole and fixing its index."*
