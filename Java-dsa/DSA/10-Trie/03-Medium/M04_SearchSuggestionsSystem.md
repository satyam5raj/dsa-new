# M04 · Search Suggestions System ⭐

| | |
|---|---|
| **LeetCode** | [#1268 Search Suggestions System](https://leetcode.com/problems/search-suggestions-system/) |
| **Difficulty** | Medium |
| **Pattern** | Autocomplete: store the top-3 suggestions **in** each trie node |
| **Code** | [M04_SearchSuggestionsSystem.java](M04_SearchSuggestionsSystem.java) |
| **Run** | `java M04_SearchSuggestionsSystem.java` |

⭐ = a bonus classic. Trie basics are explained in [E01](../02-Easy/E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem); storing answers in nodes was introduced in [E02](../02-Easy/E02_IsPrefixOfWordInSentence.md).

---

## 1. The problem

After each character of `searchWord` is typed, suggest up to 3 products that start with the typed prefix, lexicographically smallest first.

```
products ["mobile","mouse","moneypot","monitor","mousepad"], searchWord "mouse"

m     → [mobile, moneypot, monitor]
mo    → [mobile, moneypot, monitor]
mou   → [mouse, mousepad]
mous  → [mouse, mousepad]
mouse → [mouse, mousepad]
```

If a prefix matches nothing, its list is empty (and so are all the later ones).

---

## 2. How to think about it

### Approach 1: a trie with the suggestions cached in the nodes
**Sort** the products first, then insert them in that order. While inserting, add the product to **every node on its path** if that node holds fewer than 3. Because the products arrive sorted, each node keeps exactly its 3 smallest.

Query: walk the search word; at each step, output the node's list. Once you fall off the trie, every later answer is empty.

This is how real autocomplete works: precompute the top-k per prefix.

### Approach 2: sort + binary search (no trie, and also great)
In the sorted array, the words with a given prefix form a **contiguous block**. For each prefix, binary-search the first word ≥ the prefix, then take up to 3 following words that still start with it.

```
sorted: mobile, moneypot, monitor, mouse, mousepad
prefix "mou": the first word ≥ "mou" is "mouse" (index 3) → mouse, mousepad
```

---

## 3. Java concepts used in this problem

### 3.1 Sorting `String`s lexicographically

`Arrays.sort(String[])` uses String's natural order (`compareTo`): character by character, and a shorter string that's a prefix comes first ("mouse" < "mousepad").

### 3.2 A list inside every node

```java
static class Node {
    Node[] next = new Node[26];
    List<String> top3 = new ArrayList<>(3);
}
```

Each node holds at most 3 references to (shared) product strings. No strings are copied, only references.

### 3.3 `n == null ? null : n.next[c - 'a']`

Once the walk has fallen off the trie, `n` stays null, and every later prefix gets `List.of()` (an empty immutable list).

### 3.4 Lower-bound binary search with `compareTo`

```java
while (lo < hi) { int mid = (lo + hi) / 2; if (s[mid].compareTo(p) < 0) lo = mid + 1; else hi = mid; }
```

It finds the first index whose string is ≥ p (the standard "first position not less than the key").

---

## 4. Approach 1: Sort + binary search per prefix

### Code ([M04_SearchSuggestionsSystem.java:67-82](M04_SearchSuggestionsSystem.java#L67-L82))

```java
static List<List<String>> binarySearch(String[] products, String searchWord) {
    String[] s = products.clone();
    Arrays.sort(s);
    List<List<String>> result = new ArrayList<>();
    StringBuilder prefix = new StringBuilder();
    for (char c : searchWord.toCharArray()) {
        prefix.append(c);
        String p = prefix.toString();
        int lo = 0, hi = s.length;
        while (lo < hi) { int mid = (lo + hi) / 2; if (s[mid].compareTo(p) < 0) lo = mid + 1; else hi = mid; }   // first >= p
        List<String> sug = new ArrayList<>();
        for (int i = lo; i < s.length && sug.size() < 3 && s[i].startsWith(p); i++) sug.add(s[i]);
        result.add(sug);
    }
    return result;
}
```

| Line | What it does |
|---|---|
| sort | the words with a common prefix become contiguous, in order |
| the binary search | where the prefix's block starts |
| the collection loop | take up to 3 words while they still start with the prefix |

**O(n log n + L · (log n · L))** (each comparison costs up to L). It uses no extra memory beyond the sort, and it's a strong interview answer.

---

## 5. Approach 2: A trie with top-3 lists (the file's main solution)

### Code ([M04_SearchSuggestionsSystem.java:41-65](M04_SearchSuggestionsSystem.java#L41-L65))

```java
static class Node {
    Node[] next = new Node[26];
    List<String> top3 = new ArrayList<>(3);
}

static List<List<String>> suggestedProducts(String[] products, String searchWord) {
    String[] sorted = products.clone();
    Arrays.sort(sorted);                                     // insert in lexicographic order
    Node root = new Node();
    for (String p : sorted) {
        Node n = root;
        for (char c : p.toCharArray()) {
            if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
            n = n.next[c - 'a'];
            if (n.top3.size() < 3) n.top3.add(p);            // the first 3 to arrive are the smallest
        }
    }
    List<List<String>> result = new ArrayList<>();
    Node n = root;
    for (char c : searchWord.toCharArray()) {
        n = n == null ? null : n.next[c - 'a'];
        result.add(n == null ? List.of() : n.top3);
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| sort first | the products arrive in lexicographic order |
| `n.top3.size() < 3 → add` | every node keeps the first 3 products through it, which are its 3 smallest |
| the query walk | one step per typed character |
| `n == null` | the prefix matches nothing → an empty list from now on |

### Dry run: building (sorted: mobile, moneypot, monitor, mouse, mousepad)

| node (prefix) | top3 after all the inserts |
|---|---|
| m | [mobile, moneypot, monitor] |
| mo | [mobile, moneypot, monitor] |
| mou | [mouse, mousepad] |
| mous | [mouse, mousepad] |
| mouse | [mouse, mousepad] |

The query "mouse" reads these 5 lists in order ✓

### Dry run: "bagx" on [bags, baggage, banner, box, cloths]
b → [baggage, bags, banner]; ba → same; bag → [baggage, bags]; bagx → there's no child 'x' → **[]** ✓

### Complexity
- **Build O(total characters)** (+ the sort); **query O(|searchWord|)**.
- **Space O(total characters × 26)** for the nodes, plus up to 3 references each.

---

## 6. Comparing the approaches

| | Sort + binary search | Trie with top-3 |
|---|---|---|
| Build | O(n log n · L) (the sort) | the sort + O(total chars) |
| Per keystroke | O(log n · L) | **O(1)** (walk one step) |
| Memory | O(1) extra | the trie |
| Best for | one search word | many queries (real autocomplete) |

The file checks both on 500 random catalogs.

---

## 7. Edge cases and common mistakes

**Edge cases:** no product matches the first letter (all empty), duplicate products, a product equal to the search word, fewer than 3 matches.

**Mistakes:**
1. Inserting **without** sorting first (the top-3 lists wouldn't be the smallest).
2. Stopping the output early when the walk falls off (each typed character still needs an empty list).
3. Collecting the suggestions with a DFS for every prefix (correct, but slower).

---

## 8. Interview questions and answers

**Q1. Why does inserting in sorted order give the smallest 3 at each node?**
> Every product through a node is inserted in increasing order, and the node accepts only the first 3, so it keeps exactly the 3 smallest.

**Q2. Which approach would you pick?**
> For a single search word, sort + binary search: simple and memory-light. For a long-lived autocomplete service with many queries, the trie with cached top-k.

### Follow-up questions

**F1. Rank by popularity instead of alphabetically.**
> Keep each node's top-k by score (a small heap per node, or recompute on updates); the classic "design autocomplete" interview question (LC 642).

**F2. Products are added over time.**
> On insert, update the top-k lists along the new product's path.

**F3. Match anywhere in the product name, not just the prefix.**
> A trie of all suffixes (a suffix trie/tree), or an inverted index of substrings.

---

## 9. Running the file

```bash
java M04_SearchSuggestionsSystem.java      # 4/4 tests passed (including 500 random catalogs: trie == binary search)
```

**One-line summary:** *"Sort the products and insert them into a trie, letting each node keep the first 3 products through it; each typed character then just walks one node and returns its list."*
