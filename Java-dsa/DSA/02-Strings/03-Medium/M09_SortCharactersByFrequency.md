# M09 · Sort Characters By Frequency

| | |
|---|---|
| **LeetCode** | [#451 Sort Characters By Frequency](https://leetcode.com/problems/sort-characters-by-frequency/) |
| **Difficulty** | Medium |
| **Pattern** | Count + bucket sort (by frequency) |
| **Code** | [M09_SortCharactersByFrequency.java](M09_SortCharactersByFrequency.java) |
| **Run** | `java M09_SortCharactersByFrequency.java` |

---

## 1. The problem

Rearrange the characters in **decreasing order of frequency**. Characters with equal frequency can come in any order, but each character's copies must be together.

| s | one valid answer |
|---|---|
| `"tree"` | `"eert"` (or `"eetr"`) |
| `"cccaaa"` | `"cccaaa"` (or `"aaaccc"`) |
| `"Aabb"` | `"bbAa"` (or `"bbaA"`): `'A'` and `'a'` are different characters |

**Constraints:** `1 ≤ n ≤ 5 × 10⁵`, letters and digits.

---

## 2. How to think about it

1. **Count** every character (`int[128]`).
2. Order the characters by count, from the highest:
   - **Option A, sort:** sort the k distinct characters by count. O(k log k), and k ≤ 62 here, so it's effectively O(n).
   - **Option B, bucket sort:** a frequency is always between 1 and n, so make `buckets[freq]` = the list of characters with that frequency. Walk the buckets from n down to 1, appending each character `freq` times. O(n).

**Bucket sort idea:** when the values you sort by are **small integers in a known range**, use them as **indices** instead of comparing. The same trick solves "Top K Frequent Elements" in O(n).

---

## 3. Java concepts used in this problem

### 3.1 `String.repeat(n)` (Java 11+)

```java
"e".repeat(3)            // "eee"
String.valueOf('e')      // "e": a char turned into a String
```

### 3.2 A list of lists as buckets

```java
List<List<Character>> buckets = new ArrayList<>();
for (int i = 0; i <= n; i++) buckets.add(new ArrayList<>());   // buckets 0..n, each empty
buckets.get(freq).add(c);
```

You can't create a generic array like `new List<Character>[n]` without a warning (Java generics are erased at runtime), so a list of lists is the clean alternative.

### 3.3 `(char) c` when looping over codes

`for (int c = 0; c < 128; c++)` loops over character **codes** as ints; `(char) c` turns a code back into the character.

### 3.4 A `PriorityQueue` with a comparator (approach 2)

```java
PriorityQueue<Character> pq = new PriorityQueue<>((a, b) -> count[b] - count[a]);   // max-heap by count
```

The comparator puts higher counts first. (`count[b] − count[a]` is safe here, because counts are small and non-negative.) Heaps are covered fully in 08-Heap.

### 3.5 Testing a property

Several outputs are valid, so the file's `valid` method checks the rules: the output has the same characters, each character's copies are grouped, and the group sizes never increase.

---

## 4. Approach 1: Count, then sort the characters by count

```java
static String frequencySortSort(String s) {
    int[] count = new int[128];
    for (char c : s.toCharArray()) count[c]++;
    List<Character> chars = new ArrayList<>();
    for (int c = 0; c < 128; c++) if (count[c] > 0) chars.add((char) c);
    chars.sort((a, b) -> count[b] - count[a]);              // the highest frequency first
    StringBuilder sb = new StringBuilder();
    for (char c : chars) sb.append(String.valueOf(c).repeat(count[c]));
    return sb.toString();
}
```

- `list.sort(comparator)` sorts a `List` in place.
- A lambda can read `count` because it's **effectively final** (it's never reassigned, although its contents change).
- **O(n + k log k)**.

---

## 5. Approach 2: A max-heap of characters

```java
static String frequencySortHeap(String s) {
    int[] count = new int[128];
    for (char c : s.toCharArray()) count[c]++;
    PriorityQueue<Character> pq = new PriorityQueue<>((a, b) -> count[b] - count[a]);
    for (int c = 0; c < 128; c++) if (count[c] > 0) pq.offer((char) c);
    StringBuilder sb = new StringBuilder();
    while (!pq.isEmpty()) {
        char c = pq.poll();
        sb.append(String.valueOf(c).repeat(count[c]));
    }
    return sb.toString();
}
```

The same O(n + k log k). It's the natural choice if you only need the **top few** characters.

---

## 6. Approach 3: Bucket sort (the file's solution)

### Code ([M09_SortCharactersByFrequency.java:38-55](M09_SortCharactersByFrequency.java#L38-L55))

```java
static String frequencySort(String s) {
    int[] count = new int[128];
    for (char c : s.toCharArray()) count[c]++;

    List<List<Character>> buckets = new ArrayList<>();
    for (int i = 0; i <= s.length(); i++) buckets.add(new ArrayList<>());
    for (int c = 0; c < 128; c++) {
        if (count[c] > 0) buckets.get(count[c]).add((char) c);
    }

    StringBuilder sb = new StringBuilder();
    for (int freq = s.length(); freq >= 1; freq--) {          // highest frequency first
        for (char c : buckets.get(freq)) {
            sb.append(String.valueOf(c).repeat(freq));
        }
    }
    return sb.toString();
}
```

### Line by line

| Line | What it does |
|---|---|
| `count[c]++` | the frequency of each character |
| `for (i = 0; i <= n; i++) buckets.add(...)` | buckets for frequencies 0..n (0 is unused) |
| `buckets.get(count[c]).add((char) c);` | put each present character into the bucket for its frequency |
| `for (int freq = n; freq >= 1; freq--)` | from the highest possible frequency down |
| `sb.append(String.valueOf(c).repeat(freq));` | write the character `freq` times |

### Dry run: `"tree"`

Counts: `e:2, r:1, t:1`. Characters are visited by code (`e` = 101, `r` = 114, `t` = 116), so:

| bucket (freq) | characters |
|---|---|
| 2 | [e] |
| 1 | [r, t] |

Walk freq = 4, 3 (empty), 2 → `"ee"`, 1 → `"r"`, `"t"` → **`"eert"`** ✓

### Dry run: `"Aabb"`
Counts: `A:1` (65), `a:1` (97), `b:2` → buckets: 2: [b], 1: [A, a] → **`"bbAa"`** ✓

### Complexity
- **Time O(n + 128)** = **O(n)**: no comparison sorting at all.
- **Space O(n)** for the buckets and the output.

---

## 7. Comparing the approaches

| | Sort by count | Max-heap | Bucket sort |
|---|---|---|---|
| Time | O(n + k log k) | O(n + k log k) | **O(n)** |
| Space | O(k) | O(k) | O(n) buckets |
| Best when | k is small (any) | you need only the top m | the general O(n) answer |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty string, a single character, all characters equally frequent, uppercase vs lowercase.

**Mistakes:**
1. Treating `'A'` and `'a'` as the same character.
2. Appending a character once instead of `freq` times.
3. Creating only n buckets (0..n−1): a character appearing n times then needs index n, which crashes.
4. Building the result with `+=` in a loop (O(n²)); use a `StringBuilder`.

---

## 9. Interview questions and answers

**Q1. Why does bucket sort apply here?**
> Frequencies are integers between 1 and n, so they can be used directly as indices. No comparisons are needed, which gives O(n).

**Q2. How would you get only the top k characters?**
> A min-heap of size k (O(n + k' log k)), or the bucket walk, stopping after k characters.

### Follow-up questions

**F1. Top K Frequent Elements (LC 347).** → [06-Hash-Table M01](../../06-Hash-Table/03-Medium/M01_TopKFrequentElements.md)
> Count + buckets by frequency, then collect from the highest bucket until you have k elements. O(n).

**F2. Top K Frequent Words (LC 692): ties broken alphabetically.**
> A heap with the comparator "count descending, then word ascending". O(n log k).

**F3. Rearrange a string so that no two equal characters are adjacent (LC 767).** → [08-Heap M04](../../08-Heap/03-Medium/M04_ReorganizeString.md)
> A max-heap by count: repeatedly place the two most frequent different characters.

---

## 10. Running the file

```bash
java M09_SortCharactersByFrequency.java      # 6/6 tests passed
```

**One-line summary:** *"Count, then bucket the characters by frequency and read the buckets from high to low."*
