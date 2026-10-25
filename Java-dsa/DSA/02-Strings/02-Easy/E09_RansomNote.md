# E09 · Ransom Note

| | |
|---|---|
| **LeetCode** | [#383 Ransom Note](https://leetcode.com/problems/ransom-note/) |
| **Difficulty** | Easy |
| **Pattern** | Frequency array (supply vs demand) |
| **Code** | [E09_RansomNote.java](E09_RansomNote.java) |
| **Run** | `java E09_RansomNote.java` |

---

## 1. The problem

Can `ransomNote` be built from the letters of `magazine`? Each magazine letter can be used **only once**.

| ransomNote | magazine | answer | why |
|---|---|---|---|
| `"a"` | `"b"` | false | no `a` available |
| `"aa"` | `"ab"` | false | only one `a` |
| `"aa"` | `"aab"` | true | two `a`s available |
| `""` | `"abc"` | true | an empty note needs nothing |
| `"abcd"` | `"abc"` | false | the note is longer than the magazine |

**Constraints:** `1 ≤ lengths ≤ 10⁵`, lowercase letters.

---

## 2. How to think about it

Think **supply and demand**:
1. Count the **supply**: the letters available in the magazine.
2. For each letter the note **demands**, use one from the supply. If the supply of a letter drops **below 0**, it's impossible.

**Compared with Valid Anagram ([E01](E01_ValidAnagram.md)):** an anagram needs **equal** counts. Here the magazine only needs **at least** as many of each letter, so extra magazine letters are fine.

**Early exit:** a note longer than the magazine can never work.

---

## 3. Java concepts used in this problem

### 3.1 Pre-decrement inside a condition

```java
if (--supply[c - 'a'] < 0) return false;
```

`--x` decreases **first**; the comparison then uses the **new** value. "Use one letter, then check whether we went negative" happens in one line. (`x-- < 1` would be the post-decrement equivalent, but it's harder to read.)

### 3.2 Two strings with two loops

Unlike E01, the two strings can have different lengths, so we count the magazine in one loop and spend the note in another.

---

## 4. Approach 1: Remove letters from a mutable copy (naive)

```java
static boolean canConstructNaive(String ransomNote, String magazine) {
    StringBuilder mag = new StringBuilder(magazine);
    for (char c : ransomNote.toCharArray()) {
        int idx = mag.indexOf(String.valueOf(c));   // find the letter somewhere in the magazine
        if (idx < 0) return false;
        mag.deleteCharAt(idx);                       // use it up
    }
    return true;
}
```

- `StringBuilder.indexOf(String)` searches for the letter (O(m)); `deleteCharAt` shifts everything after it (O(m)).
- `String.valueOf(c)` turns a `char` into a one-character `String`.
- **O(n·m) time.** It's correct but slow, and it shows why counting wins.

---

## 5. Approach 2: Supply counts (the file's solution)

### Code ([E09_RansomNote.java:37-45](E09_RansomNote.java#L37-L45))

```java
static boolean canConstruct(String ransomNote, String magazine) {
    if (ransomNote.length() > magazine.length()) return false;
    int[] supply = new int[26];
    for (int i = 0; i < magazine.length(); i++) supply[magazine.charAt(i) - 'a']++;
    for (int i = 0; i < ransomNote.length(); i++) {
        if (--supply[ransomNote.charAt(i) - 'a'] < 0) return false;   // demand exceeds supply
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (ransomNote.length() > magazine.length()) return false;` | a quick reject |
| `int[] supply = new int[26];` | the letters available |
| first loop | count the magazine's letters |
| `--supply[...] < 0` | use one letter for the note; if none was left, it's impossible |
| `return true;` | every demand was met |

### Dry run 1: note `"aa"`, magazine `"ab"`

Supply: `a:1, b:1`

| note char | supply of it before | after | < 0? |
|---|---|---|---|
| a | 1 | 0 | no |
| a | 0 | **−1** | **yes → false** |

### Dry run 2: note `"aa"`, magazine `"aab"`

Supply: `a:2, b:1` → a: 2 → 1, a: 1 → 0 → never negative → **true** ✓

### Dry run 3: note `""`, magazine `"abc"`
The second loop doesn't run → **true** ✓

### Complexity
- **Time O(m + n).**
- **Space O(1)** (26 counters).

---

## 6. Comparing the approaches

| | Delete from a copy | Supply counts |
|---|---|---|
| Time | O(n·m) | **O(n + m)** |
| Space | O(m) | **O(1)** |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty note, a note longer than the magazine, the same strings, a letter that's missing entirely.

**Mistakes:**
1. Checking only **whether** each letter exists (a set), not **how many**: `"aa"` / `"ab"` would wrongly be true.
2. Requiring equal counts (that's the anagram problem).
3. Counting the note and the magazine in the wrong roles.

---

## 8. Interview questions and answers

**Q1. How does this differ from Valid Anagram?**
> An anagram needs the counts to be equal. Here the magazine's counts only need to be ≥ the note's, per letter.

**Q2. Can you stop early?**
> Yes: return false as soon as some letter's supply goes negative. Also reject immediately if the note is longer than the magazine.

**Q3. What if the magazine is a huge stream and the note is small?**
> Count the note's **demand** instead, then stream the magazine, decrementing the demand; stop as soon as the total remaining demand hits 0. That's O(note) memory and can finish early.

### Follow-up questions

**F1. How many copies of the note can be built?**
> `min over letters c in the note of supply[c] / demand[c]` (integer division). E.g. "Maximum Number of Balloons" (LC 1189).

**F2. Unicode letters.**
> Use a `HashMap<Character, Integer>` for the supply.

**F3. The note must use the magazine's letters in order (a subsequence).**
> A different problem: two pointers over both strings (LC 392 Is Subsequence).

---

## 9. Running the file

```bash
java E09_RansomNote.java      # 5/5 tests passed
```

**One-line summary:** *"Count the magazine's supply; spend one per note letter; negative means impossible."*
