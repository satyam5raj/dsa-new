# E06 · First Unique Character in a String

| | |
|---|---|
| **LeetCode** | [#387 First Unique Character in a String](https://leetcode.com/problems/first-unique-character-in-a-string/) |
| **Difficulty** | Easy |
| **Pattern** | Frequency array + a second pass |
| **Code** | [E06_FirstUniqueCharacter.java](E06_FirstUniqueCharacter.java) |
| **Run** | `java E06_FirstUniqueCharacter.java` |

---

## 1. The problem

Return the **index** of the first character that appears **exactly once**, or `-1` if there's none.

| s | answer | why |
|---|---|---|
| `"leetcode"` | 0 | `l` appears once |
| `"loveleetcode"` | 2 | `l` and `o` repeat; `v` is the first unique one |
| `"aabb"` | −1 | everything repeats |
| `"z"` | 0 | |

**Constraints:** `1 ≤ n ≤ 10⁵`, lowercase letters.

---

## 2. How to think about it

1. **Brute force:** for each character, scan the whole string to count it: O(n²).
2. **Two passes:**
   - pass 1: count every character (`int[26]`)
   - pass 2: walk the **string** again, in order; the first character with count 1 wins

**Why walk the string and not the count array?** The count array is in **alphabetical** order, but we need the first unique character in **string** order. In `"zab..."` where both `z` and `a` are unique, the answer is `z`, which the count array would never tell you first.

### Follow-up: a stream
If characters arrive one at a time and you must report "the first unique character so far" after each one, you can't do a second pass. Keep counts plus a **queue of candidates** in arrival order, and drop the front while its count is > 1.

---

## 3. Java concepts used in this problem

### 3.1 Counting with `int[26]` (recap)

```java
count[s.charAt(i) - 'a']++;      // see E01 for c − 'a'
```

### 3.2 `Deque` used as a queue (FIFO)

```java
Deque<Character> q = new ArrayDeque<>();
q.addLast(c);        // enqueue at the back
q.peekFirst();       // look at the front
q.pollFirst();       // dequeue the front
```

A **queue** is first-in, first-out, like a line at a shop. `ArrayDeque` is the standard choice for both queues and stacks.

### 3.3 Pre-increment inside a condition: `++count[c - 'a'] == 1`

```java
if (++count[c - 'a'] == 1) q.addLast(c);
```

`++x` increments **first**, then the comparison uses the **new** value. So the condition is true exactly when this is the **first** time we see c. (With `x++ == 0` you'd get the same test written with the old value.)

### 3.4 `char` arithmetic with a `Character`

`count[q.peekFirst() - 'a']`: `peekFirst()` returns a `Character`, which is unboxed to `char` for the subtraction, giving an `int` index.

### 3.5 `LinkedHashMap`: an alternative for keeping insertion order

```java
Map<Character, Integer> m = new LinkedHashMap<>();   // iterates in insertion order
```

A `LinkedHashMap` is a HashMap that also remembers the **order in which keys were first inserted**. Iterating over it after counting gives the characters in first-appearance order, so the first one with count 1 is the answer. It's handy for Unicode input.

---

## 4. Approach 1: Brute force

```java
static int firstUniqCharBrute(String s) {
    for (int i = 0; i < s.length(); i++) {
        boolean unique = true;
        for (int j = 0; j < s.length(); j++) {
            if (i != j && s.charAt(i) == s.charAt(j)) { unique = false; break; }
        }
        if (unique) return i;
    }
    return -1;
}
```

**O(n²) time.** It finds the right answer (it checks positions in order), but too slowly.

---

## 5. Approach 2: Count, then a second pass (the file's solution)

### Code ([E06_FirstUniqueCharacter.java:37-44](E06_FirstUniqueCharacter.java#L37-L44))

```java
static int firstUniqChar(String s) {
    int[] count = new int[26];
    for (int i = 0; i < s.length(); i++) count[s.charAt(i) - 'a']++;
    for (int i = 0; i < s.length(); i++) {
        if (count[s.charAt(i) - 'a'] == 1) return i;
    }
    return -1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] count = new int[26];` | one counter per letter |
| first loop | pass 1: count everything |
| second loop | pass 2: in **string order**, check each character's total count |
| `if (count[...] == 1) return i;` | the first unique character → return its index |
| `return -1;` | there's no unique character |

### Dry run 1: `"loveleetcode"`

**Pass 1** counts: `l:2, o:2, v:1, e:4, t:1, c:1, d:1`

**Pass 2:**

| i | char | count | unique? |
|---|---|---|---|
| 0 | l | 2 | no |
| 1 | o | 2 | no |
| 2 | v | 1 | **yes → return 2** |

### Dry run 2: `"aabb"`
Counts `a:2, b:2`. No character has count 1 → **return −1** ✓

### Complexity
- **Time O(n):** two passes.
- **Space O(1):** 26 counters.

---

## 6. Follow-up: first unique character in a stream (the file's second method)

### Code ([E06_FirstUniqueCharacter.java:47-57](E06_FirstUniqueCharacter.java#L47-L57))

```java
static String firstUniqueInStream(String stream) {
    int[] count = new int[26];
    Deque<Character> candidates = new ArrayDeque<>();
    StringBuilder out = new StringBuilder();
    for (char c : stream.toCharArray()) {
        if (++count[c - 'a'] == 1) candidates.addLast(c);
        while (!candidates.isEmpty() && count[candidates.peekFirst() - 'a'] > 1) candidates.pollFirst();
        out.append(candidates.isEmpty() ? '#' : candidates.peekFirst());
    }
    return out.toString();
}
```

### Line by line

| Line | What it does |
|---|---|
| `Deque<Character> candidates` | characters seen exactly once so far, in arrival order (possibly with some stale ones at the front) |
| `if (++count[c - 'a'] == 1) candidates.addLast(c);` | a first sighting → it's a new candidate at the back |
| `while (... count[front] > 1) candidates.pollFirst();` | drop the front candidates that have since repeated. **Lazy removal**: we only clean the front, because only the front is ever reported |
| `out.append(candidates.isEmpty() ? '#' : candidates.peekFirst());` | the answer after this character (`#` = none) |

### Dry run: `"aabc"`

| c | count after | queue after cleanup | output |
|---|---|---|---|
| a | a:1 | [a] | a |
| a | a:2 | [a] → a is repeated → [] | # |
| b | b:1 | [b] | b |
| c | c:1 | [b, c] | b |

**Output `"a#bb"`** ✓

**Complexity:** each character enters and leaves the queue at most once → **O(1) amortised per character**, O(26) space.

---

## 7. Comparing the approaches

| | Brute | Count + 2nd pass | Stream (queue) |
|---|---|---|---|
| Time | O(n²) | **O(n)** | O(1) amortised per char |
| Space | O(1) | O(1) | O(1) (≤ 26 in the queue) |
| Needs the whole string | yes | yes | **no** |

---

## 8. Edge cases and common mistakes

**Edge cases:** no unique character (−1), a single character, the unique character at the very end, all characters unique (answer 0).

**Mistakes:**
1. Scanning the **count array** for the first count of 1, which gives the alphabetically first unique letter, not the first by position.
2. Returning the character instead of the index.
3. Using `int[26]` with uppercase or Unicode input; use `int[128]` or a map.

---

## 9. Interview questions and answers

**Q1. Why two passes?**
> A character's uniqueness depends on the whole string (it might repeat at the very end), so you need all the counts first; then a second pass in order finds the first unique one.

**Q2. How would you handle a stream?**
> Counts plus a queue of candidates in arrival order. After each character, pop the front while it has repeated; the front is the answer. Each character is enqueued and dequeued once, so it's amortised O(1).

**Q3. What if the alphabet is all of Unicode?**
> Use a `LinkedHashMap<Character, Integer>` (it keeps first-insertion order), or a `HashMap` plus the second pass over the string.

### Follow-up questions

**F1. First non-repeating character in a stream, as a class (LC 1429 First Unique Number).**
> A count map + a `LinkedHashSet` of the currently unique values: add on the first sighting, remove on the second. The first element of the set is the answer.

**F2. Return the first character that repeats.**
> One pass with a seen set: the first `c` for which `add` fails.

**F3. Sort characters by frequency (LC 451).** → [M09](../03-Medium/M09_SortCharactersByFrequency.md)
> Count, then bucket-sort the characters by their counts.

---

## 10. Running the file

```bash
java E06_FirstUniqueCharacter.java      # 5/5 tests passed
```

**One-line summary:** *"Count everything, then walk the string in order and return the first character with count 1."*
