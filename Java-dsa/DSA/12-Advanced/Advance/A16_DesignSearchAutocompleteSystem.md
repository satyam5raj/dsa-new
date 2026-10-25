# A16 · Design Search Autocomplete System ⭐

| | |
|---|---|
| **LeetCode** | [#642 Design Search Autocomplete System](https://leetcode.com/problems/design-search-autocomplete-system/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Trie** (follow the typed prefix one character at a time) + a **size-3 heap** for the top 3 + a **HashMap** of children |
| **Code** | [A16_DesignSearchAutocompleteSystem.java](A16_DesignSearchAutocompleteSystem.java) |
| **Run** | `java A16_DesignSearchAutocompleteSystem.java` |

⭐ = not in `leetcode.md`, added because it combines a trie with a heap (the search-box suggestion problem).

---

## 1. The problem

The system is built from past `sentences` and `times[i]`, how often each was typed (its **hot degree**). Then the user types one character at a time:

- `input(c)` with `c == '#'`: the sentence is finished. Save it (hot degree + 1), reset the typed text, and return `[]`.
- `input(c)` otherwise: return the **top 3** past sentences that start with everything typed so far, ranked by hot degree (highest first), with ties broken by **ASCII order** (`' '` comes before letters).

```
sentences = ["i love you", "island", "iroman", "i love leetcode"],  times = [5, 3, 2, 2]

input('i') → ["i love you", "island", "i love leetcode"]
input(' ') → ["i love you", "i love leetcode"]
input('a') → []
input('#') → []                 ("i a" is saved with hot degree 1)
```

---

## 2. How to think about it

### Brute force
Keep `sentence → count` in a HashMap. On every keystroke, filter every sentence with `startsWith(typed)`, sort, and take 3. That's **O(N · L)** per keystroke for N sentences of length L, even when the prefix matches almost nothing.

### A trie remembers the prefix
Every sentence is a path of characters in a **trie**; the node at the end of a sentence stores the sentence and its count. Two ideas make typing fast:

1. **Keep a pointer** `cur` to the trie node for the text typed so far. A new character is **one step down**, `cur = cur.children.get(c)`, with no re-walk from the root.
2. If that child doesn't exist, `cur = null`: nothing starts with this prefix. Every later keystroke returns `[]` immediately, until `#`.

### Top 3 with a small heap
The sentences matching the prefix are exactly those in `cur`'s **subtree**. Visit them and keep the best 3 in a heap where the **worst of the three is on top**; when a fourth arrives, drop the worst. This is the standard "top K" trick ([Heap E01](../../08-Heap/02-Easy/E01_KthLargestInStream.md)): O(S log 3) for S matching sentences, instead of sorting all of them.

### The ranking rule, written once
```
better = higher count;  on a tie, smaller string (ASCII)
```
`RANK` sorts better first. The heap uses `RANK.reversed()`, so the **worst** is at the top, ready to be dropped.

### Dry run: `input('i')`
`cur` = the node for "i". Its subtree holds:

| sentence | count |
|---|---|
| i love you | 5 |
| island | 3 |
| iroman | 2 |
| i love leetcode | 2 |

The heap keeps 3. Between the two with count 2, `"i love leetcode" < "iroman"` because `' '` (32) < `'r'` (114), so **iroman** is dropped. Sorted with `RANK`: **[i love you, island, i love leetcode]** ✓

Then `input(' ')` moves `cur` to "i " (only the two "i love…" sentences remain), `input('a')` finds no child (`cur = null` → `[]`), and `input('#')` inserts "i a" with count 1 and resets `cur` to the root.

---

## 3. Java concepts used in this problem

### 3.1 A reusable `Comparator` constant
```java
static final Comparator<Map.Entry<String, Integer>> RANK =             // better first
        (a, b) -> !a.getValue().equals(b.getValue()) ? Integer.compare(b.getValue(), a.getValue()) : a.getKey().compareTo(b.getKey());
```
- `Integer.compare(b, a)` puts the **higher** count first.
- `a.getKey().compareTo(b.getKey())` is `String`'s natural order, which compares character codes (ASCII for these inputs).
- `!a.getValue().equals(b.getValue())`: the counts are `Integer` objects, so use `equals`, **not** `!=`, which would compare object identity (see [A02 §3.3](A02_StockPriceFluctuation.md#3-java-concepts-used-in-this-problem)).

### 3.2 `Comparator.reversed()`
`RANK.reversed()` flips the order, so the **worst** entry is the heap's top. One comparator, used both ways.

### 3.3 `Map.entry(k, v)`
It creates an immutable key-value pair: a quick way to bundle a sentence and its count without writing a class.

### 3.4 `StringBuilder` for the typed text
`typed.append(c)` per keystroke; `typed.setLength(0)` clears it on `#` (reusing the same buffer).

### 3.5 An explicit stack for the subtree walk
```java
Deque<Node> stack = new ArrayDeque<>();
stack.push(cur);
while (!stack.isEmpty()) { Node node = stack.pop(); ... for (Node child : node.children.values()) stack.push(child); }
```
It visits every node under `cur`. The visiting order doesn't matter, because the heap does the ranking.

---

## 4. Approach 1: HashMap + filter + sort (the referee)

### Code ([A16_DesignSearchAutocompleteSystem.java:52-69](A16_DesignSearchAutocompleteSystem.java#L52-L69))
```java
static class MapAutocomplete {
    private final Map<String, Integer> count = new HashMap<>();
    private final StringBuilder typed = new StringBuilder();

    MapAutocomplete(String[] sentences, int[] times) { for (int i = 0; i < sentences.length; i++) count.merge(sentences[i], times[i], Integer::sum); }

    public List<String> input(char c) {
        if (c == '#') { count.merge(typed.toString(), 1, Integer::sum); typed.setLength(0); return List.of(); }
        typed.append(c);
        String prefix = typed.toString();
        List<Map.Entry<String, Integer>> matches = new ArrayList<>();
        for (Map.Entry<String, Integer> e : count.entrySet()) if (e.getKey().startsWith(prefix)) matches.add(e);
        matches.sort(RANK);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(3, matches.size()); i++) out.add(matches.get(i).getKey());
        return out;
    }
}
```

| Line | What it does |
|---|---|
| the constructor | sentence → total hot degree (`merge` adds, in case a sentence appears twice) |
| `#` | save the typed sentence (+1), reset |
| otherwise | filter by prefix, sort by `RANK`, take up to 3 |

**Cost:** O(N · L + M log M) per keystroke, for M matches.

---

## 5. Approach 2: Trie + size-3 heap (main solution)

### Code ([A16_DesignSearchAutocompleteSystem.java:72-120](A16_DesignSearchAutocompleteSystem.java#L72-L120))
```java
static class AutocompleteSystem {
    private static class Node {
        final Map<Character, Node> children = new HashMap<>();
        String sentence;                                             // non-null at the end of a sentence
        int count;
    }

    private final Node root = new Node();
    private Node cur = root;                                         // node of the prefix typed so far (null = no match)
    private final StringBuilder typed = new StringBuilder();

    AutocompleteSystem(String[] sentences, int[] times) { for (int i = 0; i < sentences.length; i++) insert(sentences[i], times[i]); }

    private void insert(String s, int times) {
        Node node = root;
        for (char ch : s.toCharArray()) node = node.children.computeIfAbsent(ch, k -> new Node());
        node.sentence = s;
        node.count += times;
    }

    public List<String> input(char c) {
        if (c == '#') {
            insert(typed.toString(), 1);
            typed.setLength(0);
            cur = root;
            return List.of();
        }
        typed.append(c);
        if (cur != null) cur = cur.children.get(c);                  // one step down the trie
        if (cur == null) return List.of();

        PriorityQueue<Map.Entry<String, Integer>> best = new PriorityQueue<>(RANK.reversed());   // worst of the 3 on top
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(cur);
        while (!stack.isEmpty()) {                                   // every sentence under this prefix
            Node node = stack.pop();
            if (node.sentence != null) {
                best.add(Map.entry(node.sentence, node.count));
                if (best.size() > 3) best.poll();                    // drop the worst
            }
            for (Node child : node.children.values()) stack.push(child);
        }
        List<Map.Entry<String, Integer>> top = new ArrayList<>(best);
        top.sort(RANK);
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : top) out.add(e.getKey());
        return out;
    }
}
```

### Line by line
| Line | What it does |
|---|---|
| `Node.children` | `HashMap<Character, Node>`: the alphabet includes spaces, so a map is simpler than a 27-slot array |
| `insert` | walk or create the path; `count += times` (adding, so re-typing a sentence raises its degree) |
| `cur` | the node for everything typed since the last `#` |
| `#` | save the sentence, clear the text, go back to the root |
| `cur = cur.children.get(c)` | O(1) step per keystroke |
| `cur == null → []` | no sentence has this prefix (and none will until `#`) |
| the stack loop | every sentence node in the subtree goes through the heap |
| `best.size() > 3 → poll()` | keep only the best 3 (the top is the worst of them) |
| `top.sort(RANK)` | the heap's iteration order isn't sorted, so sort the 3 for output |

### Complexity
- A keystroke: O(1) to move `cur`, plus **O(S · log 3)** to scan the S nodes in the subtree.
- `#`: **O(L)** to insert.
- Space: **O(total characters)** in the trie.

---

## 6. Comparing the approaches

| | Keystroke | Notes |
|---|---|---|
| HashMap + filter + sort | O(N · L + M log M) | re-checks every sentence each time |
| **Trie + size-3 heap** | O(1) + O(subtree size) | only looks at sentences with the prefix |

The file runs 24,000 random keystrokes over random sentences (with spaces and `#`) and checks the trie against the HashMap version.

---

## 7. Edge cases and common mistakes

**Edge cases:** a prefix with no matches (and the keystrokes after it), fewer than 3 matches, ties on count (ASCII order, with spaces first), a new sentence becoming a suggestion right after `#`, re-typing an existing sentence (its count goes up).

**Mistakes:**
1. Re-walking the trie from the root on every keystroke (works, but wastes O(L)).
2. Forgetting to reset `cur` (and the typed text) on `#`.
3. Comparing counts with `!=` on `Integer`s.
4. A min-heap with the normal order, which keeps the 3 **worst** instead of the best.
5. Returning the heap's contents without sorting them.
6. After `cur` becomes `null`, calling `cur.children` again (`NullPointerException`); the `if (cur != null)` guard handles it.

---

## 8. Interview questions and answers

**Q1. Why a trie?**
> Every keystroke extends the prefix by one character. Keeping a pointer to the prefix's node makes each step O(1), and the matching sentences are exactly that node's subtree.

**Q2. Why a heap of size 3 instead of sorting?**
> We only need the top 3. A size-3 heap costs O(log 3) per candidate and holds 3 items, instead of sorting every match.

**Q3. How do you handle "no matches"?**
> Once the child for a character is missing, set `cur = null` and return `[]` for the rest of the sentence; `#` resets it.

### Follow-up questions

**F1. "Make each keystroke O(1) even for huge subtrees."**
> Store the top 3 at **every** trie node. On insert or an increased count, update the top-3 list of each node along the sentence's path (O(L) per `#`). Then a keystroke just reads `cur.top3`.

**F2. "Millions of users (real search suggestions)."**
> Precompute the top suggestions per prefix offline (from query logs), shard by prefix, and cache popular prefixes. A trie with top-k per node is still the core idea.

**F3. "Fuzzy matching (typos)."**
> Search the trie allowing up to k edits (a DFS carrying a row of edit distances), or use n-gram indexes.

---

## 9. Running the file

```bash
java A16_DesignSearchAutocompleteSystem.java     # 7/7 tests passed (LeetCode example, the saved sentence suggested, 24,000 random keystrokes)
```

**One-line summary:** *"Put the sentences in a trie with counts at the end nodes, keep a pointer to the typed prefix's node so each keystroke is one step, and collect the subtree's sentences through a size-3 heap ordered by (count desc, string asc). '#' inserts the typed sentence with +1 and resets."*
