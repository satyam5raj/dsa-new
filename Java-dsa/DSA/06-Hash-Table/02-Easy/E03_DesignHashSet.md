# E03 · Design HashSet

| | |
|---|---|
| **LeetCode** | [#705 Design HashSet](https://leetcode.com/problems/design-hashset/) |
| **Difficulty** | Easy |
| **Pattern** | Buckets + chaining (no built-in hash tables allowed) |
| **Code** | [E03_DesignHashSet.java](E03_DesignHashSet.java) |
| **Run** | `java E03_DesignHashSet.java` |

---

## 1. The problem

Implement a set of int keys in `[0, 10⁶]` **without** `java.util`'s hash collections:

```
add(1), add(2)
contains(1) → true
contains(3) → false
add(2)                  (already there: no change)
contains(2) → true
remove(2)
contains(2) → false
```

**Constraints:** at most 10⁴ calls.

---

## 2. How to think about it

### Option A: a `boolean[1_000_001]` (a "direct address table")
`present[key] = true / false`. Everything is O(1), using about 1 MB. It's perfectly valid when the keys are small integers, but it isn't really a hash set and doesn't work for big or arbitrary keys. Mention it, then build the real thing.

### Option B: a real hash set (what the interviewer wants to see)
- An array of **buckets**, each a small list (**chaining**).
- Bucket index = `key % BUCKETS`.
- `add`: append the key to its bucket's list if it isn't already there.
- `remove` / `contains`: search **one** bucket's list.

```
BUCKETS = 769
add(1):    1 % 769 = 1      → bucket 1: [1]
add(770):  770 % 769 = 1    → bucket 1: [1, 770]       ← a collision: both live in bucket 1
add(5):    5 % 769 = 5      → bucket 5: [5]
contains(770): only bucket 1 is searched → found
```

With about 10⁴ keys spread over 769 buckets, the average chain has about 13 elements.

### Why a prime bucket count?
Real keys often have patterns (all even, multiples of 10, …). With 1000 buckets, the multiples of 10 would use only 100 of them. A prime has no common factor with such patterns, so keys spread out evenly.

---

## 3. Java concepts used in this problem

### 3.1 A static nested class

```java
static class MyHashSet { ... }
```

A class defined inside another class. `static` means it doesn't need an instance of the outer class, so `main` can just do `new MyHashSet()`. (On LeetCode it would be a top-level class.)

### 3.2 An array of generic lists, and `@SuppressWarnings("unchecked")`

```java
static class Demo {
    private final List<Integer>[] buckets;
    @SuppressWarnings("unchecked")
    Demo() {
        buckets = new List[769];                // not "new List<Integer>[769]": a compile error
    }
}
```

Java **doesn't allow creating arrays of a generic type** (`new List<Integer>[n]` is a compile error), because generic types are erased at runtime and the array couldn't check its elements. The workaround is to create a raw `List[]` and assign it to the generic array variable. That compiles with an "unchecked" warning, which the annotation silences. (An alternative is `List<List<Integer>>`, an `ArrayList` of lists, with no warning.)

### 3.3 `final` fields

`private final List<Integer>[] buckets;` must be assigned exactly once, in the constructor. The **array reference** can't change, but its contents can.

### 3.4 `List.remove(int)` vs `List.remove(Object)`: a classic trap

```java
List<Integer> b = new LinkedList<>(List.of(5, 0, 7));
b.remove(0);                       // remove(int index): removes the element AT INDEX 0 → [0, 7]
b.remove(Integer.valueOf(0));      // remove(Object):     removes the VALUE 0       → [7]
```

With an `int` argument, Java picks the **index** overload (no boxing needed beats boxing). So to remove a key **by value** you must pass an `Integer`: `Integer.valueOf(key)`. The file does exactly this, with a comment.

### 3.5 `contains` on a list is O(length)

`List.contains` scans the list and compares with `equals`, so each operation costs O(chain length). Short chains are what make the whole structure O(1) on average.

---

## 4. Approach 1: A direct address table

```java
static class MyHashSetArray {
    private final boolean[] present = new boolean[1_000_001];
    public void add(int key)           { present[key] = true; }
    public void remove(int key)        { present[key] = false; }
    public boolean contains(int key)   { return present[key]; }
}
```

O(1) worst case, 1 MB of memory, valid only because keys ≤ 10⁶. (`1_000_001`: underscores in number literals are just for readability.)

---

## 5. Approach 2: Buckets + chaining (the file's solution)

### Code ([E03_DesignHashSet.java:37-61](E03_DesignHashSet.java#L37-L61))

```java
static class MyHashSet {
    private static final int BUCKETS = 769;                // a prime
    private final List<Integer>[] buckets;

    @SuppressWarnings("unchecked")
    MyHashSet() {
        buckets = new List[BUCKETS];
        for (int i = 0; i < BUCKETS; i++) buckets[i] = new LinkedList<>();
    }

    private List<Integer> bucket(int key) { return buckets[key % BUCKETS]; }

    public void add(int key) {
        List<Integer> b = bucket(key);
        if (!b.contains(key)) b.add(key);
    }

    public void remove(int key) {
        bucket(key).remove(Integer.valueOf(key));          // remove by VALUE, not by index!
    }

    public boolean contains(int key) {
        return bucket(key).contains(key);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `private static final int BUCKETS = 769;` | a constant shared by all instances |
| `buckets = new List[BUCKETS];` | an array of 769 empty slots (all null) |
| the `for` loop | put an empty list in every slot, so no null checks are needed later |
| `bucket(key)` | the hash function plus compression: `key % 769`. (Keys are non-negative here; for negative keys you'd use `Math.floorMod`) |
| `add` | only append if the key isn't already in this bucket (a set has no duplicates) |
| `remove` | remove the key **by value** from its bucket; does nothing if it's absent |
| `contains` | search only this key's bucket |

### Dry run: `add(1), add(770), add(2), contains(770), remove(1), contains(1), contains(770)`

| call | bucket index | bucket 1 after | bucket 2 after | returns |
|---|---|---|---|---|
| add(1) | 1 % 769 = 1 | [1] | [] | |
| add(770) | 770 % 769 = 1 | [1, 770] | [] | |
| add(2) | 2 | [1, 770] | [2] | |
| contains(770) | 1 | scan: 1, **770** | | true |
| remove(1) | 1 | [770] | | |
| contains(1) | 1 | scan: 770 | | false |
| contains(770) | 1 | scan: **770** | | true |

The collision between 1 and 770 is handled by the chain; neither key affects any other bucket.

### Complexity
- **Time:** O(1 + chain length) per operation. That's O(1) average with good spreading, and O(n) worst case if every key lands in one bucket.
- **Space:** O(BUCKETS + number of keys).

---

## 6. Comparing the approaches

| | `boolean[]` | Buckets + chaining |
|---|---|---|
| Time | O(1) worst case | O(1) average |
| Space | O(key range) = 1 MB | O(buckets + n) |
| Works for big or arbitrary keys? | no | yes (with a hash function) |

---

## 7. Edge cases and common mistakes

**Edge cases:** removing a key that isn't there, adding a duplicate, key 0, key 10⁶, keys that collide.

**Mistakes:**
1. `bucket.remove(key)` with an int → removes **by index** (wrong element, or an `IndexOutOfBoundsException`).
2. Forgetting the duplicate check in `add`, so the set holds two copies and `remove` deletes only one.
3. Not initialising the buckets (a NullPointerException on first use), or allocating 10⁶ lists eagerly.
4. A non-prime or tiny bucket count that makes the chains long.

---

## 8. Interview questions and answers

**Q1. What happens when there are many more keys than buckets?**
> The chains get long and operations slow down. Real hash tables track the **load factor** (keys / buckets) and **resize**: double the buckets and rehash every key when the load passes about 0.75 ([HashTableChaining](../01-Basics/HashTableChaining.java)).

**Q2. Chaining vs open addressing?**
> Chaining keeps a list per bucket: simple, and deletes are easy. Open addressing keeps all keys in the array itself and probes for the next free slot on a collision: better cache behaviour, but deletes need tombstones and performance drops sharply at high load ([HashTableOpenAddressing](../01-Basics/HashTableOpenAddressing.java)).

**Q3. How does Java's `HashMap` handle a bucket with many collisions?**
> Since Java 8, a bucket whose chain grows past 8 entries (in a table with at least 64 buckets) is converted into a red-black tree, so the worst case per bucket becomes O(log n) instead of O(n).

### Follow-up questions

**F1. Support arbitrary objects.**
> Use `key.hashCode()`, compress it with `Math.floorMod(h, BUCKETS)` (hashCode can be negative), and compare with `equals`.

**F2. Make it thread-safe.**
> The simplest way is to make every method `synchronized`. Better is lock striping (one lock per group of buckets), the idea behind `ConcurrentHashMap`.

**F3. Design HashMap.** → [E04](E04_DesignHashMap.md)
> The same structure, but each chain node stores a key **and** a value.

---

## 9. Running the file

```bash
java E03_DesignHashSet.java      # 5/5 tests passed (including 50,000 random ops vs java.util.HashSet)
```

**One-line summary:** *"769 (prime) buckets of linked lists; key % 769 picks the bucket, and every operation searches only that short chain. Remove by `Integer.valueOf(key)`, not by index."*
