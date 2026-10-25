# M05 · Decode String

| | |
|---|---|
| **LeetCode** | [#394 Decode String](https://leetcode.com/problems/decode-string/) |
| **Difficulty** | Medium |
| **Pattern** | Nested structure: save the context on `[` and restore it on `]` |
| **Code** | [M05_DecodeString.java](M05_DecodeString.java) |
| **Run** | `java M05_DecodeString.java` |

---

## 1. The problem

`k[encoded]` means "repeat `encoded` k times". Brackets can **nest**.

| s | decoded |
|---|---|
| `"3[a]2[bc]"` | `"aaabcbc"` |
| `"3[a2[c]]"` | `"accaccacc"` |
| `"2[abc]3[cd]ef"` | `"abcabccdcdcdef"` |
| `"10[a]"` | `"aaaaaaaaaa"` (a multi-digit k) |

**Constraints:** `1 ≤ s.length ≤ 30`, `1 ≤ k ≤ 300`; the input is always valid.

---

## 2. How to think about it

### Why a stack
When we reach an inner `[`, we must **pause** the outer string we were building, decode the inner part completely, then **resume** the outer one. Pausing and resuming nested work is a **stack**, which is exactly what recursion does with the call stack.

### The state while scanning
- `cur` = the string being built at the current nesting level
- `k` = the number being read (it can have several digits, as in `"12[a]"`)

### On each character
| character | action |
|---|---|
| digit | `k = k·10 + digit` |
| `[` | push `(cur, k)`; start fresh: `cur = ""`, `k = 0` |
| `]` | pop `(prevString, times)`; `cur = prevString + cur × times` |
| letter | `cur += letter` |

---

## 3. Java concepts used in this problem

### 3.1 Two parallel stacks

```java
Deque<Integer> counts = new ArrayDeque<>();          // the repeat counts
Deque<StringBuilder> outer = new ArrayDeque<>();     // the paused outer strings
```

They're always pushed and popped together, so they stay aligned. A single stack of pairs would work too.

### 3.2 Building multi-digit numbers

```java
k = k * 10 + (c - '0');      // "12" → 1 → 12
```

`c − '0'` turns a digit character into its value. Multiplying by 10 shifts the previous digits left.

### 3.3 `String.repeat(n)` and `StringBuilder.append`

```java
cur = outer.pop().append(inner.repeat(counts.pop()));
```

`append` returns the same `StringBuilder`, so the popped outer builder (with the repeated inner string added) becomes the new `cur`.

### 3.4 A static field as the recursion's cursor

```java
static int pos;     // shared by all recursive calls: how far we've read
```

In the recursive version, every call reads from the same position in the string, so the cursor is shared (a field, or an `int[]` holder) instead of being passed by value.

---

## 4. Approach 1: Two stacks (the file's main solution)

### Code ([M05_DecodeString.java:47-68](M05_DecodeString.java#L47-L68))

```java
static String decodeString(String s) {
    Deque<Integer> counts = new ArrayDeque<>();
    Deque<StringBuilder> outer = new ArrayDeque<>();
    StringBuilder cur = new StringBuilder();
    int k = 0;
    for (char c : s.toCharArray()) {
        if (Character.isDigit(c)) {
            k = k * 10 + (c - '0');
        } else if (c == '[') {
            counts.push(k);
            outer.push(cur);                 // pause the outer level
            cur = new StringBuilder();
            k = 0;
        } else if (c == ']') {
            String inner = cur.toString();
            cur = outer.pop().append(inner.repeat(counts.pop()));   // resume + combine
        } else {
            cur.append(c);
        }
    }
    return cur.toString();
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (Character.isDigit(c)) k = k * 10 + (c - '0');` | accumulate the repeat count |
| `counts.push(k); outer.push(cur);` | at `[`: save how many times to repeat, and the outer string built so far |
| `cur = new StringBuilder(); k = 0;` | start the inner level with an empty string |
| `inner = cur.toString(); cur = outer.pop().append(inner.repeat(counts.pop()));` | at `]`: the inner level is finished; attach it, repeated, to the outer string |
| `else cur.append(c);` | a letter belongs to the current level |
| `return cur.toString();` | the top level is the answer |

### Dry run: `"3[a2[c]]"`

| char | action | cur | stacks (counts / outer) |
|---|---|---|---|
| 3 | k = 3 | "" | |
| [ | push (3, "") | "" | [3] / [""] |
| a | append | "a" | |
| 2 | k = 2 | "a" | |
| [ | push (2, "a") | "" | [3, 2] / ["", "a"] |
| c | append | "c" | |
| ] | pop (2, "a"): "a" + "cc" | "acc" | [3] / [""] |
| ] | pop (3, ""): "" + "acc" × 3 | "accaccacc" | [] / [] |

**Result `"accaccacc"`** ✓

### Dry run 2: `"2[abc]3[cd]ef"`
`2[abc]` → "abcabc"; `3[cd]` → appended: "abcabccdcdcd"; then `e`, `f` at the top level → **`"abcabccdcdcdef"`** ✓

### Complexity
- **Time O(output length)** (every output character is written; the repeats can make the output much longer than the input).
- **Space O(output length).**

---

## 5. Approach 2: Recursion (the file's second method)

### Code ([M05_DecodeString.java:71-95](M05_DecodeString.java#L71-L95))

```java
static int pos;

static String decodeRecursive(String s) {
    pos = 0;
    return parse(s);
}

static String parse(String s) {
    StringBuilder sb = new StringBuilder();
    while (pos < s.length() && s.charAt(pos) != ']') {
        char c = s.charAt(pos);
        if (Character.isDigit(c)) {
            int k = 0;
            while (Character.isDigit(s.charAt(pos))) k = k * 10 + (s.charAt(pos++) - '0');
            pos++;                                   // skip '['
            String inner = parse(s);                 // decode the nested part
            pos++;                                   // skip ']'
            sb.append(inner.repeat(k));
        } else {
            sb.append(c);
            pos++;
        }
    }
    return sb.toString();
}
```

| Line | What it does |
|---|---|
| `while (pos < n && s.charAt(pos) != ']')` | decode until the end of this level (a `]` or the end of the input) |
| digit branch | read k, skip `[`, **recursively** decode the inside, skip `]`, append it repeated |
| letter branch | append the letter |
| `return sb.toString();` | this level's decoded string |

Each recursive call is one nesting level. The call stack stores the paused outer `sb`, exactly like the explicit stack in approach 1.

**Dry run** `"3[a2[c]]"`: parse level 0 reads `3[` → parse level 1 reads `a`, then `2[` → parse level 2 reads `c`, stops at `]` → returns "c" → level 1 appends "cc" → "acc", stops at `]` → level 0 appends "acc" × 3. **"accaccacc"** ✓

---

## 6. Comparing the approaches

| | Explicit stacks | Recursion |
|---|---|---|
| Time | O(output) | O(output) |
| Space | O(output) + O(depth) | O(output) + O(depth) call stack |
| Code style | iterative state machine | mirrors the grammar |

---

## 7. Edge cases and common mistakes

**Edge cases:** multi-digit counts (`"100[a]"`), letters outside any brackets, deep nesting, adjacent groups.

**Mistakes:**
1. Reading only a single digit for k.
2. Forgetting to reset `k = 0` after `[`.
3. Building strings with `+=` in loops (O(n²)); use StringBuilder.
4. Popping the counts and strings in the wrong order.

---

## 8. Interview questions and answers

**Q1. Why is a stack natural here?**
> Brackets nest. When an inner group starts, the outer group's partial result and repeat count must be saved and restored after the inner group ends, which is LIFO.

**Q2. What's the complexity?**
> It's proportional to the size of the output, which can be exponential in the nesting depth (e.g. `2[2[2[a]]]`), so a "Time O(n)" claim would be wrong.

### Follow-up questions

**F1. Encode a string into the shortest k[...] form (LC 471, Hard).**
> Interval DP over substrings, checking for repetition with `(s + s).indexOf(s, 1)`.

**F2. Number of Atoms (LC 726): a chemistry formula with nested groups and counts.**
> The same "save context at `(`, multiply the counts at `)`" stack, with maps of atom counts.

**F3. Parse a nested list / mini JSON.**
> A stack of containers; `[` pushes a new list, `]` pops and attaches it to the parent.

---

## 9. Running the file

```bash
java M05_DecodeString.java      # 6/6 tests passed
```

**One-line summary:** *"At `[`, push (the current string, k) and start fresh; at `]`, pop and set cur = saved + cur × k."*
