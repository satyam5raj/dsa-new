# E04 · Design HashMap

| | |
|---|---|
| **LeetCode** | [#706 Design HashMap](https://leetcode.com/problems/design-hashmap/) |
| **Difficulty** | Easy |
| **Pattern** | Buckets of (key, value) nodes with chaining: [E03](E03_DesignHashSet.md) with values |
| **Code** | [E04_DesignHashMap.java](E04_DesignHashMap.java) |
| **Run** | `java E04_DesignHashMap.java` |

---

## 1. The problem

Implement a map from int keys to int values, both in `[0, 10⁶]`, **without** built-in hash tables:
- `put(key, value)`: insert, or **update** if the key exists
- `get(key)`: the value, or **−1** if missing
- `remove(key)`: delete if present

```
put(1, 1), put(2, 2)
get(1) → 1
get(3) → −1
put(2, 1)          (update)
get(2) → 1
remove(2)
get(2) → −1
```

---

## 2. How to think about it

The same structure as Design HashSet, but each chain node stores **(key, value)**:
- **put:** walk the bucket. If the key exists, **update** its value; otherwise add a new node.
- **get:** walk the bucket; return the value, or −1.
- **remove:** walk the bucket with a `prev` pointer and unlink the node.

Here the chain is a **hand-written singly linked list with a dummy head** per bucket (the [Linked Lists](../../03-Linked-Lists/README.md) trick), so removing the first real node needs no special case.

```
BUCKETS = 2069 (prime)
put(1, 10), put(2070, 20)        both hash to bucket 1  (2070 % 2069 = 1)

bucket 1:  dummy → [1:10] → [2070:20] → null
remove(1): findPrev returns dummy → dummy.next = dummy.next.next
bucket 1:  dummy → [2070:20] → null
```

### One helper does all the walking
`findPrev(dummy, key)` returns the node **before** the one holding key (or the last node if key is absent). All three operations use it:
- `prev.next != null` → the key exists, at `prev.next`
- `prev.next == null` → absent, and `prev` is the tail (exactly where put should append)

### The "update, don't duplicate" bug
If put forgets to check for an existing key, one key gets two nodes and get returns whichever comes first. The file's 50,000-operation random test would catch it.

---

## 3. Java concepts used in this problem

### 3.1 A private static nested `Node` class

```java
private static class Node {
    int key, value;
    Node next;
    Node(int key, int value, Node next) { this.key = key; this.value = value; this.next = next; }
}
```

The same shape as the `ListNode` in the Linked Lists topic, with an extra field. `private` hides it inside `MyHashMap`; `static` means a Node doesn't carry a hidden reference to a MyHashMap instance.

### 3.2 Arrays of objects start as `null`

`new Node[BUCKETS]` creates 2069 **null** slots, not 2069 nodes. The file creates each bucket's dummy **lazily**, the first time a key maps there. That avoids allocating thousands of unused objects.

### 3.3 A sentinel (dummy) node

The dummy holds `(-1, -1)`, values that are never used. Its only job is to be a `prev` for the first real node, so insert and remove work the same way everywhere in the chain.

### 3.4 Why not `java.util.LinkedList` here?

With a `LinkedList<int[]>`, update would need a search and then a set; remove by key would need an iterator. A hand-written list gives direct access to `prev.next`, which makes all three operations a few lines each.

---

## 4. Approach 1: A direct address table

```java
static class MyHashMapArray {
    private final int[] values = new int[1_000_001];
    MyHashMapArray() { Arrays.fill(values, -1); }      // -1 = "absent"
    public void put(int key, int value) { values[key] = value; }
    public int get(int key)             { return values[key]; }
    public void remove(int key)         { values[key] = -1; }
}
```

O(1) worst case and about 4 MB. It works only because the keys are small and −1 is never a real value. `Arrays.fill` sets every slot (an int array starts as 0, which would mean "value 0").

---

## 5. Approach 2: Buckets with dummy-headed chains (the file's solution)

### Code ([E04_DesignHashMap.java:36-74](E04_DesignHashMap.java#L36-L74))

```java
static class MyHashMap {
    private static class Node {
        int key, value;
        Node next;
        Node(int key, int value, Node next) { this.key = key; this.value = value; this.next = next; }
    }

    private static final int BUCKETS = 2069;                  // prime
    private final Node[] buckets = new Node[BUCKETS];          // each slot: a dummy head (created lazily)

    private Node dummy(int key) {
        int i = key % BUCKETS;
        if (buckets[i] == null) buckets[i] = new Node(-1, -1, null);
        return buckets[i];
    }

    // returns the node BEFORE the one holding key (or the last node if absent)
    private Node findPrev(Node dummy, int key) {
        Node prev = dummy;
        while (prev.next != null && prev.next.key != key) prev = prev.next;
        return prev;
    }

    public void put(int key, int value) {
        Node prev = findPrev(dummy(key), key);
        if (prev.next != null) prev.next.value = value;         // UPDATE an existing key
        else prev.next = new Node(key, value, null);            // insert at the end
    }

    public int get(int key) {
        Node prev = findPrev(dummy(key), key);
        return prev.next == null ? -1 : prev.next.value;
    }

    public void remove(int key) {
        Node prev = findPrev(dummy(key), key);
        if (prev.next != null) prev.next = prev.next.next;      // unlink
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `dummy(key)` | the bucket index `key % 2069`; create that bucket's dummy on first use |
| `findPrev`'s loop | advance while the **next** node exists and isn't the key |
| `put`: `prev.next != null` | the key exists → overwrite its value |
| `put`: else | the key is absent; prev is the tail → append |
| `get` | absent → −1, else the stored value |
| `remove` | unlink by skipping over the node; Java's garbage collector frees it |

### Dry run: keys 1 and 2070 collide in bucket 1

| call | bucket 1 after | returns |
|---|---|---|
| put(1, 10) | dummy → [1:10] | |
| put(2070, 20) | dummy → [1:10] → [2070:20] | |
| get(2070) | (findPrev stops at [1:10]) | 20 |
| put(1, 99) | dummy → [1:**99**] → [2070:20] (updated, not duplicated) | |
| remove(1) | dummy → [2070:20] (findPrev returned the dummy) | |
| get(1) | (findPrev reaches the tail [2070:20]; next is null) | −1 |

### Dry run: the file's example
`put(1,1)` → bucket 1: [1:1]. `put(2,2)` → bucket 2: [2:2]. `get(1)` → 1. `get(3)` → bucket 3 has only a dummy → −1. `put(2,1)` updates → [2:1]. `get(2)` → 1. `remove(2)` → bucket 2 empty. `get(2)` → −1 ✓

### Complexity
- **Time:** O(1 + chain length): O(1) average, O(n) worst case.
- **Space:** O(BUCKETS + number of keys).

A small side effect: `get` and `remove` on an empty bucket also create its dummy. That's harmless, but a stricter version would return early when `buckets[i] == null`.

---

## 6. Comparing the approaches

| | `int[]` table | Buckets + chains |
|---|---|---|
| Time | O(1) worst case | O(1) average |
| Space | 4 MB fixed | O(buckets + n) |
| Arbitrary keys? | no | yes (with hashCode) |

---

## 7. Edge cases and common mistakes

**Edge cases:** updating an existing key, removing a missing key, value 0 (a valid value!), keys that collide, removing the first node of a chain.

**Mistakes:**
1. Not updating an existing key, which creates duplicates.
2. Using 0 as "absent" (0 is a legal value; that's why get returns −1).
3. Without a dummy: forgetting the special case for removing the head of a chain.
4. Walking with `cur` instead of `prev`, which leaves you unable to unlink.

---

## 8. Interview questions and answers

**Q1. How would you add resizing?**
> Track `size`. When `size / buckets > 0.75`, allocate twice as many buckets and re-insert every node (each key's index changes because the modulus changed). The occasional O(n) resize is amortised O(1) per operation. See [HashTableChaining](../01-Basics/HashTableChaining.java).

**Q2. How does Java's `HashMap` pick a bucket?**
> It takes `key.hashCode()`, mixes the high bits into the low ones (`h ^ (h >>> 16)`), and uses `hash & (capacity − 1)`. The capacity is always a power of two, so that `&` acts as a fast modulo.

**Q3. Why prime bucket counts here, but powers of two in Java?**
> With plain `%`, a prime spreads patterned keys better. Java uses powers of two for the fast `&`, and fixes the spreading problem with the bit-mixing step instead.

### Follow-up questions

**F1. Generic keys and values.**
> `Node<K, V>`, `Math.floorMod(key.hashCode(), n)` for the index, and `equals` for comparisons.

**F2. Keep insertion order (like `LinkedHashMap`).**
> Also thread every node into a doubly linked list in insertion order. That's the core of LRU Cache ([Linked Lists](../../03-Linked-Lists/README.md)).

**F3. Open addressing instead of chains.**
> Store the keys and values in parallel arrays and probe linearly; deletes leave tombstones ([HashTableOpenAddressing](../01-Basics/HashTableOpenAddressing.java)).

---

## 9. Running the file

```bash
java E04_DesignHashMap.java      # 5/5 tests passed (including 50,000 random ops vs java.util.HashMap)
```

**One-line summary:** *"2069 prime buckets, each a dummy-headed linked list of (key, value) nodes; one `findPrev` helper gives update-or-append, get and unlink."*
