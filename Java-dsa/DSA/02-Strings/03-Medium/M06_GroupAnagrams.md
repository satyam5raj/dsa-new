# M06 · Group Anagrams

| | |
|---|---|
| **LeetCode** | [#49 Group Anagrams](https://leetcode.com/problems/group-anagrams/) |
| **Difficulty** | Medium |
| **Pattern** | Canonical key + HashMap grouping |
| **Code** | [M06_GroupAnagrams.java](M06_GroupAnagrams.java) |
| **Run** | `java M06_GroupAnagrams.java` |

---

## 1. The problem

Group the words that are anagrams of each other. Any order of groups and of words within a group is fine.

```
["eat","tea","tan","ate","nat","bat"]  →  [["eat","tea","ate"], ["tan","nat"], ["bat"]]
```

| input | output (one valid order) |
|---|---|
| `["eat","tea","tan","ate","nat","bat"]` | `[[eat,tea,ate],[tan,nat],[bat]]` |
| `[""]` | `[[""]]` |
| `["a"]` | `[["a"]]` |

**Constraints:** `1 ≤ n ≤ 10⁴` words, each of length `k ≤ 100`, lowercase letters.

---

## 2. How to think about it

1. **Brute force:** compare every pair with an anagram check: O(n²·k). Too slow.
2. **Canonical key:** give every word a **key that is identical for all of its anagrams** (and different for non-anagrams). Then group with a `Map<key, List<word>>`. Anagrams land in the same bucket automatically.

**Key option 1: the sorted word.** `"eat"`, `"tea"`, `"ate"` → `"aet"`. O(k log k) per word.

**Key option 2: the letter counts.** `"eat"` → `"a1e1t1"`. O(k) per word.

"Map each item to a canonical form, then group by it" is a pattern that appears everywhere: grouping isomorphic strings, shifted strings, equivalent shapes, and more.

---

## 3. Java concepts used in this problem

### 3.1 `computeIfAbsent`: the grouping idiom

```java
groups.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
```

It means: *"get the list for `key`; if there isn't one, create an empty list with the lambda, store it, and return it."* Then `.add(word)` puts the word into that list. The long version:

```java
List<String> list = groups.get(key);
if (list == null) {
    list = new ArrayList<>();
    groups.put(key, list);
}
list.add(word);
```

The lambda's parameter `k` is the key (unused here). It's named `k` only by convention.

### 3.2 `new String(char[])`: turning sorted characters back into a String

```java
char[] c = word.toCharArray();
Arrays.sort(c);
String key = new String(c);   // or String.valueOf(c)
```

**Don't** use `c.toString()`: arrays don't override `toString`, so you'd get something like `"[C@1b6d3586"` (the type plus a hash code), which is different for every array.

### 3.3 Why the key must be a `String`, not an `int[]`

A `HashMap` uses `hashCode()` and `equals()` to find keys. For arrays, those are **identity-based**: two arrays with the same contents are **different keys**. Strings (and `List`s) compare by **content**. So we turn the counts into a String (or use `Arrays.toString(count)`, or `List.of(...)`).

### 3.4 `map.values()` → a list of groups

```java
return new ArrayList<>(groups.values());
```

`values()` is a **view** collection of all the lists; wrapping it in `new ArrayList<>(...)` copies it into a real `List<List<String>>`.

### 3.5 Normalising unordered output for tests

The file's `normalise` sorts the words inside each group and then sorts the groups, so different but equivalent outputs compare equal. This is the same "check the property, not the exact layout" idea used before.

---

## 4. Approach 1: Brute force with a visited flag

```java
static List<List<String>> groupAnagramsBrute(String[] strs) {
    List<List<String>> res = new ArrayList<>();
    boolean[] used = new boolean[strs.length];
    for (int i = 0; i < strs.length; i++) {
        if (used[i]) continue;
        List<String> g = new ArrayList<>(List.of(strs[i]));
        used[i] = true;
        for (int j = i + 1; j < strs.length; j++) {
            if (!used[j] && isAnagram(strs[i], strs[j])) { g.add(strs[j]); used[j] = true; }
        }
        res.add(g);
    }
    return res;
}
static boolean isAnagram(String a, String b) {
    if (a.length() != b.length()) return false;
    int[] c = new int[26];
    for (int i = 0; i < a.length(); i++) { c[a.charAt(i) - 'a']++; c[b.charAt(i) - 'a']--; }
    for (int x : c) if (x != 0) return false;
    return true;
}
```

**O(n² · k) time.** With n = 10⁴, that's about 10⁸ × k, which is too slow.

---

## 5. Approach 2: The sorted word as the key (the file's main solution)

### Code ([M06_GroupAnagrams.java:36-45](M06_GroupAnagrams.java#L36-L45))

```java
static List<List<String>> groupAnagrams(String[] strs) {
    Map<String, List<String>> groups = new HashMap<>();
    for (String word : strs) {
        char[] c = word.toCharArray();
        Arrays.sort(c);
        String key = new String(c);                      // canonical form
        groups.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
    }
    return new ArrayList<>(groups.values());
}
```

### Line by line

| Line | What it does |
|---|---|
| `Map<String, List<String>> groups` | key → all the words with that key |
| `char[] c = word.toCharArray(); Arrays.sort(c);` | the word's letters in sorted order |
| `String key = new String(c);` | the canonical form, as a String (a proper HashMap key) |
| `computeIfAbsent(...).add(word)` | put the word into its group, creating the group on first use |
| `return new ArrayList<>(groups.values());` | the groups themselves |

### Dry run: `["eat","tea","tan","ate","nat","bat"]`

| word | sorted key | map after |
|---|---|---|
| eat | aet | aet=[eat] |
| tea | aet | aet=[eat, tea] |
| tan | ant | aet=[eat, tea], ant=[tan] |
| ate | aet | aet=[eat, tea, ate], ant=[tan] |
| nat | ant | …, ant=[tan, nat] |
| bat | abt | …, abt=[bat] |

**Groups:** `[eat, tea, ate]`, `[tan, nat]`, `[bat]` ✓ (the order of the groups depends on the HashMap)

### Dry run: `[""]`
The empty word's sorted key is `""` → one group `[""]` ✓

### Complexity
- **Time O(n · k log k)** (sorting each word).
- **Space O(n · k)** (the keys and the groups).

---

## 6. Approach 3: Letter counts as the key (the file's second method)

### Code ([M06_GroupAnagrams.java:47-59](M06_GroupAnagrams.java#L47-L59))

```java
static List<List<String>> groupAnagramsByCount(String[] strs) {
    Map<String, List<String>> groups = new HashMap<>();
    for (String word : strs) {
        int[] count = new int[26];
        for (char ch : word.toCharArray()) count[ch - 'a']++;
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            if (count[i] > 0) key.append((char) ('a' + i)).append(count[i]);   // e.g. "a1e1t1"
        }
        groups.computeIfAbsent(key.toString(), k -> new ArrayList<>()).add(word);
    }
    return new ArrayList<>(groups.values());
}
```

### Line by line

| Line | What it does |
|---|---|
| `count[ch - 'a']++` | count the letters, O(k) |
| `key.append((char) ('a' + i)).append(count[i])` | write each present letter followed by its count: `"a1e1t1"`. Including the letter avoids ambiguity: counts alone, like `"11"`, could mean different things |
| `computeIfAbsent(key.toString(), ...)` | group by the count signature |

**Dry run:** `"eat"` → counts a:1, e:1, t:1 → key `"a1e1t1"`; `"tea"` → the same key; `"bat"` → `"a1b1t1"`.

**Why include the letter in the key?** Without separators or letters, different count arrays could collide: counts `[1, 11]` and `[11, 1]` would both become `"111"`. Writing the letter before each count (or using a separator like `#`) makes the key unambiguous.

### Complexity
- **Time O(n · (k + 26))** = **O(n · k)**.
- **Space O(n · k).**

---

## 7. Comparing the approaches

| | Brute | Sorted key | Count key |
|---|---|---|---|
| Time | O(n²·k) | O(n·k log k) | **O(n·k)** |
| Space | O(n·k) | O(n·k) | O(n·k) |
| Simplicity | low | **highest** | high |

For short words (k ≤ 100), the sorted key is just as fast in practice and simpler. Mention the count key as the asymptotically better option.

---

## 8. Edge cases and common mistakes

**Edge cases:** empty strings, single words, all the words being anagrams, no anagrams at all.

**Mistakes:**
1. Using `char[]` or `int[]` directly as the map key (identity-based `equals`/`hashCode`).
2. Calling `c.toString()` on a char array.
3. An ambiguous count key without letters or separators.
4. Returning `groups.values()` directly when a `List` is required (it's a `Collection` view).

---

## 9. Interview questions and answers

**Q1. What makes a good canonical key?**
> It's identical for every equivalent item, different for non-equivalent ones, and cheap to compute. Here: the sorted letters, or the letter counts.

**Q2. Why can't the key be an `int[]`?**
> Arrays use identity-based `equals`/`hashCode`, so two equal-content arrays would be different keys. Convert to a String (or a List) first.

**Q3. Complexity of the two key choices?**
> The sorted key is O(k log k) per word; the count key is O(k + 26). Both are dominated by reading the input when k is small.

### Follow-up questions

**F1. Group Shifted Strings (LC 249): "abc", "bcd", "xyz" belong together.**
> Key = the differences between consecutive letters, mod 26 (e.g. `"1,1"`).

**F2. Group the words by the same set of letters (ignoring counts).**
> Key = a 26-bit mask of the letters present.

**F3. Very large input (distributed).**
> Map step: emit `(key, word)`; reduce step: group by key. That's MapReduce, with the canonical key as the shuffle key.

**F4. Find the largest anagram group.**
> Track the max list size while grouping.

---

## 10. Running the file

```bash
java M06_GroupAnagrams.java      # 4/4 tests passed
```

**One-line summary:** *"Map every word to a canonical key (its sorted letters or its counts) and group with computeIfAbsent."*
