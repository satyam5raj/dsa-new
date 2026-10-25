# E01 · Happy Number

| | |
|---|---|
| **LeetCode** | [#202 Happy Number](https://leetcode.com/problems/happy-number/) |
| **Difficulty** | Easy |
| **Pattern** | A HashSet to detect a cycle (or Floyd's fast/slow pointers, O(1) space) |
| **Code** | [E01_HappyNumber.java](E01_HappyNumber.java) |
| **Run** | `java E01_HappyNumber.java` |

This is the first note of the Hash Table topic, so section 3 covers **`HashSet` and `HashMap` from the ground up**. Later notes give only short recaps.

---

## 1. The problem

Repeatedly replace n with the **sum of the squares of its digits**. If you reach 1, n is **happy**. Otherwise the sequence loops forever.

```
19 → 1² + 9² = 82 → 8² + 2² = 68 → 6² + 8² = 100 → 1² + 0² + 0² = 1            happy ✓
 7 → 49 → 97 → 130 → 10 → 1                                                   happy ✓
 2 → 4 → 16 → 37 → 58 → 89 → 145 → 42 → 20 → 4 → …   (4 again: a loop!)     not happy ✗
```

**Constraints:** `1 ≤ n ≤ 2³¹ − 1`.

---

## 2. How to think about it

### Why must it end in 1 or a loop?
The next number is **small**. A number with d digits gives at most `d · 81` (every digit is 9). Even the largest int (10 digits) gives at most 810, and a 3-digit number gives at most 3 · 81 = 243. So after a step or two the sequence lives inside 1..810, a **finite** range. A sequence in a finite range must eventually **repeat** a value, and once a value repeats, everything after it repeats too: a **cycle**. (1 is a cycle of length 1: next(1) = 1.)

So the question becomes: **do we reach 1, or do we see a number for the second time?**

### Approach 1: remember what you've seen
Keep a `HashSet` of the visited numbers. Seeing a number twice means a cycle, so not happy.

### Approach 2: it's a linked list!
"Each value determines the next one" **is** a linked list: n → next(n) → next(next(n)) → … Detecting a cycle in a linked list is **Floyd's algorithm** ([Linked Lists E03](../../03-Linked-Lists/02-Easy/E03_LinkedListCycle.md)): slow moves 1 step and fast moves 2 steps. They must meet inside the cycle. The number is happy iff the cycle they meet in is the 1 → 1 loop. O(1) memory.

**The lesson:** recognise structure. A sequence where each value determines the next is a linked list, so linked-list tricks apply.

---

## 3. Java concepts used in this problem

### 3.1 What a hash table is (in one picture)

A hash table stores keys in an **array**, using a **hash function** to turn each key into an array index:

```
key ──hashCode()──▶ a big int ──compress──▶ index in [0, capacity)

  "cat".hashCode() = 98262   →  98262 & 15 = 6   →  bucket 6   (simplified: the real HashMap
                                                                first mixes in the high bits)
  table:  [0] [1] [2] [3] [4] [5] [6]→"cat" [7] ... [15]
```

Looking a key up repeats the same computation and jumps straight to its bucket, so `add`, `contains` and `remove` are **O(1) on average**, with no scanning. When two keys land in the same bucket (a **collision**), Java keeps a small list in that bucket. (The [01-Basics](../01-Basics/HashTableChaining.java) files build one from scratch; [E03](E03_DesignHashSet.md) and [E04](E04_DesignHashMap.md) are the interview versions.)

### 3.2 `HashSet<E>`: a set of unique elements

```java
Set<Integer> seen = new HashSet<>();
seen.add(5);          // true: 5 was new
seen.add(5);          // false: 5 was already there (the set doesn't change)
seen.contains(5);     // true
seen.remove(5);       // true: it was there and is now gone
seen.size();          // 0
```

| Method | Returns | Average cost |
|---|---|---|
| `add(x)` | `true` if x was **new**, `false` if it was already present | O(1) |
| `contains(x)` | whether x is present | O(1) |
| `remove(x)` | `true` if x was present | O(1) |
| `size()`, `isEmpty()` | | O(1) |

- **No duplicates, no order.** Iterating a HashSet gives the elements in an unpredictable order. Use `LinkedHashSet` for insertion order and `TreeSet` for sorted order (O(log n) operations).
- **The boolean from `add` is very useful:** "add, and tell me whether it was already there" in **one** lookup. This file's main loop is built on that.

### 3.3 `HashMap<K, V>`: a key → value table

Most problems in this topic use a map rather than a set. Here's the full toolkit:

```java
Map<String, Integer> count = new HashMap<>();
count.put("a", 1);                        // insert or overwrite; returns the OLD value (or null)
count.get("a");                           // 1;   get("zzz") → null (not 0!)
count.getOrDefault("zzz", 0);             // 0
count.containsKey("a");                   // true
count.merge("a", 1, Integer::sum);        // "a" → 2   (insert 1 if absent, else old + 1)
count.put("b", count.getOrDefault("b", 0) + 1);   // the same counting idiom, spelled out
count.computeIfAbsent("c", k -> 0);       // insert a default only if absent
count.remove("a");                        // returns the removed value (2)
for (Map.Entry<String, Integer> e : count.entrySet()) { /* e.getKey(), e.getValue() */ }
```

| Method | Meaning | Average cost |
|---|---|---|
| `put(k, v)` | set k → v; returns the previous value or `null` | O(1) |
| `get(k)` | the value, or **`null`** if absent | O(1) |
| `getOrDefault(k, d)` | the value, or d | O(1) |
| `containsKey(k)` | is k present? | O(1) |
| `merge(k, v, f)` | absent → put v; present → put f(old, v) | O(1) |
| `remove(k)` | delete; returns the old value | O(1) |
| `keySet()`, `values()`, `entrySet()` | views for iteration | O(n) to iterate |

**Traps:**
- `get` returns `null` for a missing key. Unboxing that into an `int` (`int c = map.get(k);`) throws a **NullPointerException**. Use `getOrDefault`.
- `HashSet` is really a `HashMap` whose values are ignored. Same costs, same rules.

### 3.4 Keys need `equals` and `hashCode`

A hash table finds a key by its `hashCode()`, then confirms the match with `equals()`. `Integer`, `String`, `Long` and `Character` already implement both correctly, which is why they're the usual keys. For your **own** class as a key you must override both (or use a `record`, which does it for you). Details are in [HashFunctions](../01-Basics/HashFunctions.java).

**Never use an `int[]` as a key**: arrays use identity equality, so two arrays with the same contents are different keys. Convert to a `String` or a `List<Integer>` instead.

### 3.5 Autoboxing in collections

Collections hold **objects**, so `seen.add(n)` with an `int n` silently boxes it into an `Integer`. It's fine, but each box costs a small allocation, which is why problems with small int keys sometimes use a plain array instead (`boolean[]`, `int[26]`).

### 3.6 Extracting digits: `% 10` and `/ 10`

```java
int d = n % 10;     // the last digit
n /= 10;            // drop the last digit
```

The same loop as in the Basics digit problems: it visits the digits from right to left.

---

## 4. The helper: `next(n)`

### Code ([E01_HappyNumber.java:35-43](E01_HappyNumber.java#L35-L43))

```java
static int next(int n) {
    int sum = 0;
    while (n > 0) {
        int d = n % 10;
        sum += d * d;
        n /= 10;
    }
    return sum;
}
```

| Line | What it does |
|---|---|
| `int d = n % 10;` | the last digit |
| `sum += d * d;` | add its square |
| `n /= 10;` | drop that digit |
| `return sum;` | the sum of the squared digits |

**Dry run** `next(19)`: d = 9 → sum 81, n = 1; d = 1 → sum 82, n = 0 → **82**.

O(number of digits) = O(log n).

---

## 5. Approach 1: A HashSet of seen numbers (the file's main solution)

### Code ([E01_HappyNumber.java:45-49](E01_HappyNumber.java#L45-L49))

```java
static boolean isHappy(int n) {
    Set<Integer> seen = new HashSet<>();
    while (n != 1 && seen.add(n)) n = next(n);   // add() is false when n was seen before -> cycle
    return n == 1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Set<Integer> seen = new HashSet<>();` | the numbers visited so far |
| `while (n != 1 && seen.add(n))` | keep going while we haven't reached 1 **and** n is new. `add` both records n and tells us whether it was new |
| `n = next(n);` | step forward |
| `return n == 1;` | the loop ends either at 1 (happy) or on a repeat (a cycle) |

`&&` short-circuits: when `n == 1`, `seen.add(n)` isn't even called.

### Dry run 1: n = 19

| n | `n != 1` | `seen.add(n)` | seen after | next |
|---|---|---|---|---|
| 19 | ✓ | true | {19} | 82 |
| 82 | ✓ | true | {19, 82} | 68 |
| 68 | ✓ | true | {19, 82, 68} | 100 |
| 100 | ✓ | true | {19, 82, 68, 100} | 1 |
| 1 | ✗ → stop | (not called) | | |

`return 1 == 1` → **true** ✓

### Dry run 2: n = 2

| n | `seen.add(n)` | seen size after | next |
|---|---|---|---|
| 2 | true | 1 | 4 |
| 4 | true | 2 | 16 |
| 16 | true | 3 | 37 |
| 37 | true | 4 | 58 |
| 58 | true | 5 | 89 |
| 89 | true | 6 | 145 |
| 145 | true | 7 | 42 |
| 42 | true | 8 | 20 |
| 20 | true | 9 | 4 |
| 4 | **false** (already seen) → stop | | |

`return 4 == 1` → **false** ✓

### Dry run 3: n = 1
`n != 1` fails immediately → return **true**.

### Complexity
- **Time:** O(log n) for the first step, then a bounded number of steps over numbers ≤ 810. In practice it's effectively O(log n).
- **Space:** the size of the set, which is also bounded (at most a few hundred entries).

---

## 6. Approach 2: Floyd's fast and slow pointers (O(1) space)

### Code ([E01_HappyNumber.java:51-58](E01_HappyNumber.java#L51-L58))

```java
static boolean isHappyFloyd(int n) {
    int slow = n, fast = next(n);
    while (fast != 1 && slow != fast) {
        slow = next(slow);
        fast = next(next(fast));
    }
    return fast == 1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int slow = n, fast = next(n);` | fast starts one step ahead (otherwise `slow != fast` would be false immediately) |
| `while (fast != 1 && slow != fast)` | stop when fast reaches 1, or when they meet (a cycle) |
| `slow = next(slow);` | 1 step |
| `fast = next(next(fast));` | 2 steps |
| `return fast == 1;` | reaching 1 means happy. (Since 1 → 1, if the cycle they met in is 1 itself, fast is 1.) |

Fast is the one that reaches 1 first, so checking only `fast` is enough.

### Dry run 1: n = 19

| step | slow | fast |
|---|---|---|
| start | 19 | 82 |
| 1 | 82 | 100 |
| 2 | 68 | **1** → stop |

**true** ✓

### Dry run 2: n = 2 (the cycle 4 → 16 → 37 → 58 → 89 → 145 → 42 → 20 → 4)

| step | slow | fast |
|---|---|---|
| start | 2 | 4 |
| 1 | 4 | 37 |
| 2 | 16 | 89 |
| 3 | 37 | 42 |
| 4 | 58 | 4 |
| 5 | 89 | 37 |
| 6 | 145 | 89 |
| 7 | **42** | **42** → they meet |

fast = 42 ≠ 1 → **false** ✓. (These traces were produced by running the code.)

### Complexity
- **Time:** the same bounded number of steps (a few times more `next` calls).
- **Space: O(1).**

---

## 7. Comparing the approaches

| | HashSet | Floyd |
|---|---|---|
| Time | O(log n) effectively | O(log n) effectively |
| Space | O(number of steps), bounded | **O(1)** |
| Simplicity | very simple | needs the cycle insight |

The file's tests check that both agree on every n from 1 to 10,000.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1 (already happy), n = 7 (happy, a common surprise), n = 2³¹ − 1 (the first step brings it down to at most 810).

**Mistakes:**
1. Looping until 1 with no cycle check → an infinite loop for unhappy numbers.
2. In Floyd, starting slow and fast at the **same** value, so the loop exits immediately.
3. Calling `contains` and then `add` (two lookups) when `add`'s return value already tells you.
4. Squaring with `Math.pow` (it returns a double; `d * d` is simpler and exact).

---

## 9. Interview questions and answers

**Q1. Why doesn't the sequence grow forever?**
> A d-digit number maps to at most 81·d, which is much smaller than the number once d ≥ 4. So after a few steps every value is at most 810, and in a finite range the sequence must repeat.

**Q2. How is this a linked-list problem?**
> Each number points to exactly one next number, so the sequence is a linked list that either ends in the 1 → 1 self-loop or enters another cycle. Floyd's cycle detection then finds which.

**Q3. HashSet or Floyd: which would you write?**
> I'd write the HashSet version first (clear and obviously correct), then mention Floyd for O(1) space.

### Follow-up questions

**F1. Is there a simpler test?**
> It's known that every unhappy number eventually enters the cycle containing 4. So `while (n != 1 && n != 4) n = next(n); return n == 1;` works. It relies on that fact, so state it as a known result.

**F2. Replace squares with cubes (or any power).**
> The same algorithm. The bound changes (9³ · d), but the sequence still stays in a finite range.

**F3. Find the start of the cycle.**
> Floyd phase 2: reset one pointer to the start and move both 1 step at a time; they meet at the cycle entry ([Linked Lists M01 Cycle II](../../03-Linked-Lists/03-Medium/M01_LinkedListCycleII.md)).

**F4. Count the happy numbers up to N quickly.**
> Memoise: every number maps into 1..810 after one step, so precompute happiness for 1..810 once, then each query is one `next` call plus a lookup.

---

## 10. Running the file

```bash
java E01_HappyNumber.java      # 5/5 tests passed (including Floyd == HashSet for 1..10,000)
```

**One-line summary:** *"The digit-square sequence must reach 1 or repeat; detect the repeat with a HashSet (`add` returns false) or with Floyd's fast/slow pointers."*
