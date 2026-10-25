# M01 · LRU Cache

| | |
|---|---|
| **LeetCode** | [#146 LRU Cache](https://leetcode.com/problems/lru-cache/) |
| **Difficulty** | Medium |
| **Pattern** | HashMap<key, Node> + a **doubly** linked list with dummy head and tail |
| **Code** | [M01_LRUCache.java](M01_LRUCache.java) |
| **Run** | `java M01_LRUCache.java` |

Design-problem basics (state in a class, `private final` fields) are explained in [E01](../02-Easy/E01_LoggerRateLimiter.md#3-java-concepts-used-in-this-problem). The full from-scratch walkthrough is in [01-Basics/LRUCacheBasics.java](../01-Basics/LRUCacheBasics.java); this is the interview-sized version.

---

## 1. The problem

Design `LRUCache(capacity)` with:
- `get(key)`: the value, or −1 if the key isn't present;
- `put(key, value)`: insert or update. When the cache is full, first evict the **least recently used** key.

Both must be **O(1)**. Using a key means a `get` or a `put` on it.

```
capacity 2:
put(1,1)  put(2,2)  get(1)=1  put(3,3) [evicts 2]  get(2)=-1
put(4,4) [evicts 1]  get(1)=-1  get(3)=3  get(4)=4
```

---

## 2. How to think about it

### What each operation needs
- Find a key's entry in O(1) → a **HashMap**.
- Keep the entries in **recency order**, move any entry to the front in O(1), and remove the oldest in O(1) → a **doubly linked list**.

Neither structure is enough alone: a map has no order, and a list can't find a key without scanning.

```
map:   key → node            (jump straight to any node)
list:  head ⇄ [most recent] ⇄ ... ⇄ [least recent] ⇄ tail
```

- **get:** look up the node; move it to the front; return its value.
- **put, existing key:** update the value; move it to the front.
- **put, new key:** if full, remove `tail.prev` from the list **and** from the map; add the new node at the front.

### Why doubly linked?
To unlink a node in O(1) you need its predecessor. A singly linked list would have to scan for it.

### Why dummy head and tail (sentinels)?
With sentinels, every real node always has a non-null `prev` and `next`, so `remove` and `addFirst` have no special cases for an empty list or the first or last node.

### Why does the node store its key?
On eviction we find the node from the list (`tail.prev`), and must then delete it from the map, which needs the key.

### Dry run: the example (the list is shown most recent first)

| op | action | list | map keys | output |
|---|---|---|---|---|
| put(1,1) | new | [1] | {1} | |
| put(2,2) | new | [2, 1] | {1, 2} | |
| get(1) | move 1 to the front | [1, 2] | | 1 |
| put(3,3) | full → evict tail.prev = 2 | [3, 1] | {1, 3} | |
| get(2) | missing | | | −1 |
| put(4,4) | full → evict tail.prev = 1 | [4, 3] | {3, 4} | |
| get(1) | missing | | | −1 |
| get(3) | move 3 to the front | [3, 4] | | 3 |
| get(4) | move 4 to the front | [4, 3] | | 4 |

Output: **"1 -1 -1 3 4"** ✓

---

## 3. Java concepts used in this problem

### 3.1 A private static nested `Node` class

```java
private static class Node {
    int key, value;
    Node prev, next;
    Node(int k, int v) { key = k; value = v; }
}
```

The node is an implementation detail of the cache, so it's `private` inside `LRUCache`. `static` means a `Node` doesn't carry a hidden reference to its cache. `Node(int k, int v)` is its constructor. Linked-list nodes were introduced in [Linked Lists E01](../../03-Linked-Lists/02-Easy/E01_ReverseLinkedList.md#3-java-concepts-used-in-this-problem).

### 3.2 A constructor with `this.capacity = capacity`

```java
public LRUCache(int capacity) {
    this.capacity = capacity;          // field = parameter (same name)
    head.next = tail;
    tail.prev = head;
}
```

The parameter has the same name as the field, so `this.capacity` means "the field". A `final` field without an initialiser must be set exactly once, in the constructor.

### 3.3 Pointer surgery

```java
private void remove(Node n) { n.prev.next = n.next; n.next.prev = n.prev; }

private void addFirst(Node n) {
    n.next = head.next;
    n.prev = head;
    head.next.prev = n;
    head.next = n;
}
```

- `remove` makes the neighbours skip over `n`.
- `addFirst` sets `n`'s own pointers first, then points the neighbours at it. The order matters: `head.next` must be read before it's overwritten on the last line.

### 3.4 `LinkedHashMap` in access order (the referee)

```java
static class Ref extends LinkedHashMap<Integer, Integer> {
    final int cap;
    Ref(int cap) { super(16, 0.75f, true); this.cap = cap; }
    @Override protected boolean removeEldestEntry(Map.Entry<Integer, Integer> e) { return size() > cap; }
}
```

- `extends LinkedHashMap<…>`: `Ref` **inherits** everything `LinkedHashMap` does.
- `super(16, 0.75f, true)` calls the parent constructor: initial capacity 16, load factor 0.75, and `accessOrder = true`, so iteration goes from least to most recently **accessed** (`get` counts as an access).
- `removeEldestEntry` is a hook that `LinkedHashMap` calls after every `put`. `@Override` says we're replacing the parent's version (which returns `false`). Returning `true` when the map is too big evicts the eldest entry, which is the LRU one.
- `protected` is the access level the parent declared; an override can't make it more restrictive.

That's a complete LRU cache in four lines. In an interview, mention it, but expect to be asked to build the real thing.

---

## 4. Approach 1: `LinkedHashMap` with access order (the referee)

The code is in section 3.4 ([M01_LRUCache.java:92-96](M01_LRUCache.java#L92-L96)). `get` is `ref.getOrDefault(k, -1)` and `put` is `ref.put(k, v)`.

- **Time O(1)** per operation.
- It's exactly the HashMap + doubly linked list design, already built into the JDK.

---

## 5. Approach 2: HashMap + doubly linked list by hand (the file's main solution)

### Code ([M01_LRUCache.java:38-89](M01_LRUCache.java#L38-L89))

```java
static class LRUCache {
    private static class Node {
        int key, value;
        Node prev, next;
        Node(int k, int v) { key = k; value = v; }
    }

    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();
    private final Node head = new Node(0, 0), tail = new Node(0, 0);

    public LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        Node n = map.get(key);
        if (n == null) return -1;
        remove(n);
        addFirst(n);
        return n.value;
    }

    public void put(int key, int value) {
        Node n = map.get(key);
        if (n != null) {
            n.value = value;
            remove(n);
            addFirst(n);
            return;
        }
        if (map.size() == capacity) {
            Node lru = tail.prev;
            remove(lru);
            map.remove(lru.key);
        }
        n = new Node(key, value);
        map.put(key, n);
        addFirst(n);
    }

    private void remove(Node n) { n.prev.next = n.next; n.next.prev = n.prev; }

    private void addFirst(Node n) {
        n.next = head.next;
        n.prev = head;
        head.next.prev = n;
        head.next = n;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `head`, `tail` | sentinels; the real nodes live between them |
| the constructor | an empty list is `head ⇄ tail` |
| `get`: `map.get(key)` | O(1) lookup; `null` means it's missing |
| `get`: `remove` + `addFirst` | mark it most recently used |
| `put`: existing | update the value and mark it used; `return` so we don't insert twice |
| `put`: full | `tail.prev` is the least recently used; unlink it **and** delete it from the map |
| `put`: new | create, register in the map, put it at the front |

### Complexity
- **Time O(1)** per `get` and `put`.
- **Space O(capacity).**

---

## 6. Comparing the approaches

| | LinkedHashMap | By hand |
|---|---|---|
| Time | O(1) | O(1) |
| Code | 4 lines | about 50 lines |
| What interviewers want | "do you know it exists?" | "can you build it?" |

The file checks the hand-built cache against `LinkedHashMap` on 200,000 random operations across 100 capacities.

---

## 7. Edge cases and common mistakes

**Edge cases:** capacity 1, `put` on an existing key when full (it's an update, so nothing is evicted), `get` on a missing key, the same key used over and over.

The interviewers' checklist (from the file):
1. Forgetting to move the node to the front on `get` (that's FIFO, not LRU).
2. Forgetting to remove the evicted key from the **map** (a memory leak, and `get` returns a stale node).
3. Not treating an update as a use (it must move to the front).
4. A node without its key (eviction can't find the map entry).
5. Checking "full" before handling the existing-key case (that wrongly evicts on an update).

---

## 8. Interview questions and answers

**Q1. Why not a single `ArrayList` or queue?**
> Moving an element from the middle to the front is O(n) in an array or a queue. The linked list does it in O(1) because the map gives us the node directly.

**Q2. Why must the list be doubly linked?**
> Unlinking needs the predecessor. With `prev` pointers we have it in O(1).

**Q3. What do the sentinels buy you?**
> No null checks: every real node always has both neighbours, so `remove` and `addFirst` are each two to four lines with no special cases.

**Q4. How would you make it thread-safe?**
> The simplest way is a lock around `get` and `put` (`synchronized`). For high concurrency, shard the cache into segments, each with its own lock, or use a library such as Caffeine.

### Follow-up questions

**F1. LFU Cache.** → [H01](../04-Hard/H01_LFUCache.md)
> Evict the least **frequently** used key, breaking ties by recency: a map of frequency → LRU list, plus a `minFreq` pointer.

**F2. Add a time-to-live per entry.**
> Store an expiry time in each node; treat expired entries as missing on `get`, and evict them lazily or with a heap ordered by expiry.

**F3. Logger Rate Limiter.** → [E01](../02-Easy/E01_LoggerRateLimiter.md)
> Bounded memory by time instead of by count.

---

## 9. Running the file

```bash
java M01_LRUCache.java      # 3/3 tests passed (example, cap 1 update, 200,000 random ops vs LinkedHashMap)
```

**One-line summary:** *"A HashMap finds the node in O(1), and a doubly linked list with sentinels keeps recency order: move a node to the front on every use, and evict `tail.prev` (from the list and the map) when full."*
