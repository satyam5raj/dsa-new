# H03 · All O`one Data Structure ⭐

| | |
|---|---|
| **LeetCode** | [#432 All O`one Data Structure](https://leetcode.com/problems/all-oone-data-structure/) |
| **Difficulty** | Hard |
| **Pattern** | A doubly linked list of **count buckets** (sorted by count) + a key → bucket map |
| **Code** | [H03_AllOneDataStructure.java](H03_AllOneDataStructure.java) |
| **Run** | `java H03_AllOneDataStructure.java` |

⭐ = not in `leetcode.md`, added for a key technique. Design-problem basics are explained in [E01](../02-Easy/E01_LoggerRateLimiter.md#3-java-concepts-used-in-this-problem); the bucket idea first appears in [H01 LFU Cache](H01_LFUCache.md).

---

## 1. The problem

Design a structure with **all** operations in O(1):
- `inc(key)`: add 1 to the key's count (a new key starts at 1);
- `dec(key)`: subtract 1 (remove the key when it reaches 0);
- `getMaxKey()` / `getMinKey()`: **any** key with the largest / smallest count, or `""` if empty.

| call | max | min |
|---|---|---|
| inc(hello), inc(hello) | hello | hello |
| inc(leet) | hello | leet |
| dec(hello), dec(hello) | leet | leet |

---

## 2. How to think about it

### Why not a heap or a TreeMap?
A heap gives the max **or** the min in O(1), but updates are O(log n). A `TreeMap` of counts is O(log n) too.

### Buckets in a sorted linked list
```
head ⇄ [count 1: {a, b}] ⇄ [count 3: {c}] ⇄ [count 7: {d}] ⇄ tail
```
- The buckets are sorted by count, and a bucket exists only while it has keys.
- `where`: key → its bucket.
- `getMinKey` = any key of `head.next`; `getMaxKey` = any key of `tail.prev`.

**inc(key):** the key moves from bucket c to c + 1. That bucket is either the **next** bucket (if its count is exactly c + 1) or a **new** bucket inserted right after bucket c. Then remove the key from bucket c, and delete bucket c if it's now empty.

**dec(key):** the mirror image. Bucket c − 1 is the previous bucket or a new one inserted before bucket c; at count 0 the key is dropped entirely.

**Why is this O(1)?** Counts only ever change by **1**, so the right bucket is always next to the current one. No searching.

### Treating the head as "count 0"
The head sentinel has count 0, so a new key behaves like a key in bucket 0: its target is `head.next` if that bucket has count 1, otherwise a new count-1 bucket after the head.

### Dry run: the example

| op | cur | target | list after |
|---|---|---|---|
| inc(hello) | head (0) | head.next is the tail, not count 1 → new [1] | head ⇄ [1: hello] ⇄ tail |
| inc(hello) | [1] | next is the tail → new [2] after [1]; [1] empties and is removed | head ⇄ [2: hello] ⇄ tail |
| inc(leet) | head (0) | head.next is [2], not 1 → new [1] after head | head ⇄ [1: leet] ⇄ [2: hello] ⇄ tail |
| getMax / getMin | | | tail.prev → hello; head.next → leet |
| dec(hello) | [2] | prev is [1] with count 1 → use it; [2] empties and is removed | head ⇄ [1: leet, hello] ⇄ tail |
| dec(hello) | [1] | count 1 → drop hello from `where` | head ⇄ [1: leet] ⇄ tail |
| getMax | | | leet |

---

## 3. Java concepts used in this problem

### 3.1 Sentinels with extreme counts

```java
private final Bucket head = new Bucket(0), tail = new Bucket(Integer.MAX_VALUE);
```

The head acts as "count 0" for new keys. The tail's count can never equal `c + 1` for a real count, so `cur.next.count == cur.count + 1` is never wrongly true at the end of the list.

### 3.2 `getOrDefault` returning a sentinel

```java
Bucket cur = where.getOrDefault(key, head);
```

A new key's "current bucket" is the head, so `inc` needs no separate branch for new keys (only `if (cur != head)` before removing it from its old bucket).

### 3.3 Inserting a bucket after another

```java
private Bucket insertAfter(Bucket b, int count) {
    Bucket n = new Bucket(count);
    n.prev = b; n.next = b.next;
    b.next.prev = n; b.next = n;
    return n;
}
```

The same pointer surgery as `addFirst` in [M01 LRU Cache](../03-Medium/M01_LRUCache.md#33-pointer-surgery), generalised to "after any bucket". `dec` uses `insertAfter(cur.prev, ...)` to insert **before** `cur`.

### 3.4 The ternary choosing between two buckets

```java
Bucket target = cur.next.count == cur.count + 1 ? cur.next : insertAfter(cur, cur.count + 1);
```

Only the chosen branch runs, so a new bucket is created only when it's needed.

---

## 4. Approach 1: A plain count map (the referee)

The test keeps `Map<String, Integer> ref` and, after every operation, computes the max and min by scanning all the counts ([H03_AllOneDataStructure.java:102-113](H03_AllOneDataStructure.java#L102-L113)):

```java
class Fragment {
    boolean check(Map<String, Integer> ref, String maxKey, String minKey) {
        int max = Collections.max(ref.values()), min = Collections.min(ref.values());
        return ref.get(maxKey) == max && ref.get(minKey) == min;   // ANY key with that count is fine
    }
}
```

- inc/dec O(1), but getMax/getMin are **O(n)**.
- Because the problem allows **any** key with the extreme count, the test checks the returned key's **count**, not the key itself.

---

## 5. Approach 2: The bucket list (the file's main solution)

### Code ([H03_AllOneDataStructure.java:38-86](H03_AllOneDataStructure.java#L38-L86))

```java
static class AllOne {
    private static class Bucket {
        int count;
        Set<String> keys = new LinkedHashSet<>();
        Bucket prev, next;
        Bucket(int count) { this.count = count; }
    }

    private final Bucket head = new Bucket(0), tail = new Bucket(Integer.MAX_VALUE);
    private final Map<String, Bucket> where = new HashMap<>();

    public AllOne() { head.next = tail; tail.prev = head; }

    private Bucket insertAfter(Bucket b, int count) {
        Bucket n = new Bucket(count);
        n.prev = b; n.next = b.next;
        b.next.prev = n; b.next = n;
        return n;
    }

    private void removeIfEmpty(Bucket b) {
        if (b != head && b != tail && b.keys.isEmpty()) { b.prev.next = b.next; b.next.prev = b.prev; }
    }

    public void inc(String key) {
        Bucket cur = where.getOrDefault(key, head);                  // head acts as "count 0"
        Bucket target = cur.next.count == cur.count + 1 ? cur.next : insertAfter(cur, cur.count + 1);
        target.keys.add(key);
        where.put(key, target);
        if (cur != head) { cur.keys.remove(key); removeIfEmpty(cur); }
    }

    public void dec(String key) {
        Bucket cur = where.get(key);
        if (cur == null) return;
        if (cur.count == 1) {
            where.remove(key);
        } else {
            Bucket target = cur.prev.count == cur.count - 1 ? cur.prev : insertAfter(cur.prev, cur.count - 1);
            target.keys.add(key);
            where.put(key, target);
        }
        cur.keys.remove(key);
        removeIfEmpty(cur);
    }

    public String getMaxKey() { return tail.prev == head ? "" : tail.prev.keys.iterator().next(); }
    public String getMinKey() { return head.next == tail ? "" : head.next.keys.iterator().next(); }
}
```

### Line by line

| Line | What it does |
|---|---|
| `Bucket` | one count, its keys, and its neighbours |
| `where` | key → the bucket holding it |
| `inc`: `target` | the neighbouring bucket with count + 1, created if missing |
| `inc`: the last line | leave the old bucket (unless it's the head) and delete it if empty |
| `dec`: count 1 | the key disappears entirely |
| `dec`: otherwise | the previous bucket with count − 1, created if missing |
| `removeIfEmpty` | never removes a sentinel |
| `getMaxKey` / `getMinKey` | the last and first real buckets; `""` when the list is empty |

**Order matters in `inc`:** the new bucket is inserted **before** the old one is removed, so `insertAfter(cur, ...)` still has a valid `cur` to attach to.

### Complexity
- **O(1)** for every operation (on average, for the hash operations).
- **Space O(number of keys).**

---

## 6. Comparing the approaches

| | Count map + scan | Bucket list |
|---|---|---|
| inc / dec | O(1) | O(1) |
| getMax / getMin | O(n) | **O(1)** |

The file runs 100,000 random operations against the count map, checking the counts of the returned keys after every step.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty structure (`""` for both), `dec` on a missing key (ignored), one key going up and down repeatedly, all keys with the same count.

**Mistakes:**
1. Removing the old bucket before inserting the new one next to it.
2. Leaving empty buckets in the list (then getMin/getMax can return a bucket with no keys).
3. Searching for the right bucket instead of using the neighbour.
4. Forgetting to update `where` after moving a key.

---

## 8. Interview questions and answers

**Q1. Why can every operation be O(1)?**
> Counts change by exactly 1, so the destination bucket is always adjacent. The list stays sorted without ever comparing counts beyond the neighbour.

**Q2. How does this compare with the LFU cache?**
> Both group keys into count buckets. LFU only needs the minimum, so a map of count → bucket plus `minFreq` is enough. Here we need both ends, so the buckets are kept as a sorted linked list.

**Q3. Why a `LinkedHashSet` for the keys?**
> O(1) add and remove, and `iterator().next()` returns some key in O(1). Any O(1) set with cheap "give me one element" works.

### Follow-up questions

**F1. Return all the keys with the max count.**
> Return `tail.prev.keys`, which is already grouped.

**F2. Increment by arbitrary amounts.**
> The neighbour trick breaks; you'd need a `TreeMap<count, bucket>` and O(log n) operations.

**F3. LFU Cache.** → [H01](H01_LFUCache.md)

---

## 9. Running the file

```bash
java H03_AllOneDataStructure.java      # 7/7 tests passed (including 100,000 random ops vs a count map)
```

**One-line summary:** *"Keep keys in buckets of equal count, linked in count order; since counts change by 1, inc and dec move a key to the neighbouring bucket (creating it if needed), and min and max are the first and last buckets."*
