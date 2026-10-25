# M11 · Restore IP Addresses

| | |
|---|---|
| **LeetCode** | [#93 Restore IP Addresses](https://leetcode.com/problems/restore-ip-addresses/) |
| **Difficulty** | Medium |
| **Pattern** | **Partition** into exactly 4 pieces with validity rules + length pruning |
| **Code** | [M11_RestoreIPAddresses.java](M11_RestoreIPAddresses.java) |
| **Run** | `java M11_RestoreIPAddresses.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem); the partition template is in [M10](M10_PalindromePartitioning.md).

---

## 1. The problem

Insert 3 dots into a digit string to make every valid IPv4 address. Each of the 4 parts must be 0..255 with **no leading zeros** ("0" is fine, "01" is not).

| s | answer |
|---|---|
| "25525511135" | ["255.255.11.135", "255.255.111.35"] |
| "0000" | ["0.0.0.0"] |
| "101023" | ["1.0.10.23", "1.0.102.3", "10.1.0.23", "10.10.2.3", "101.0.2.3"] |

---

## 2. How to think about it

The partition template from [M10](M10_PalindromePartitioning.md), with:
- exactly **4** parts, each 1 to 3 characters long;
- `valid(part)`: no leading zero unless the part **is** "0", and a value ≤ 255;
- **length pruning:** with `left` parts still to place and `rem` characters remaining, we need `left ≤ rem ≤ 3 · left`. Otherwise stop right away.

The search space is tiny (at most 3⁴ = 81 splits), but getting the rules right is the point.

### Dry run: the first level of "25525511135" (11 characters)

| first part | rem | left | outcome |
|---|---|---|---|
| "2" | 10 | 3 | 10 > 3 · 3 = 9 → **pruned** at once |
| "25" | 9 | 3 | the rest must be three 3-character parts: "525" > 255 ✗ |
| "255" | 8 | 3 | continue → finds 255.255.11.135 and 255.255.111.35 |

### Dry run: "0000"

```
"0" → "0" → "0" → "0" → record 0.0.0.0
any part "00" or "000" → invalid (leading zero)
```

Only **"0.0.0.0"**.

---

## 3. Java concepts used in this problem

### 3.1 `String.join`

```java
String.join(".", parts)          // ["255","255","11","135"] → "255.255.11.135"
```

It concatenates the elements with the separator between them.

### 3.2 The leading-zero rule as a character check

```java
if (p.length() > 1 && p.charAt(0) == '0') return false;
```

`Integer.parseInt("01")` would happily return 1, so the leading zero has to be checked on the **string**.

### 3.3 `Integer.parseInt` on short strings

Each part has at most 3 digits, so parsing can never overflow. The loop condition `len <= 3` guarantees this.

### 3.4 A loop condition with two limits

```java
for (int len = 1; len <= 3 && start + len <= s.length(); len++)
```

The part can't be longer than 3 characters or run past the end of the string.

### 3.5 `TreeSet` to compare results (in the test)

`new TreeSet<>(list)` sorts and deduplicates, so two lists of addresses can be compared regardless of order.

---

## 4. Approach 1: Three nested loops over the dot positions (the referee)

### Code ([M11_RestoreIPAddresses.java:57-67](M11_RestoreIPAddresses.java#L57-L67))

```java
static List<String> brute(String s) {
    List<String> out = new ArrayList<>();
    int n = s.length();
    for (int a = 1; a < n; a++) for (int b = a + 1; b < n; b++) for (int c = b + 1; c < n; c++) {
        String[] p = {s.substring(0, a), s.substring(a, b), s.substring(b, c), s.substring(c)};
        boolean ok = true;
        for (String x : p) ok &= x.length() <= 3 && valid(x);
        if (ok) out.add(String.join(".", p));
    }
    return out;
}
```

With exactly 3 dots, three loops place them. Note the order inside `ok &= x.length() <= 3 && valid(x)`: the length check comes first so `valid` never parses a huge string.

- **Time O(n³)** splits (n ≤ 20 in LeetCode, so it's fine).

---

## 5. Approach 2: Partition backtracking (the file's main solution)

### Code ([M11_RestoreIPAddresses.java:32-54](M11_RestoreIPAddresses.java#L32-L54))

```java
static List<String> restoreIpAddresses(String s) {
    List<String> out = new ArrayList<>();
    bt(s, 0, new ArrayList<>(), out);
    return out;
}

static void bt(String s, int start, List<String> parts, List<String> out) {
    int left = 4 - parts.size(), rem = s.length() - start;
    if (rem < left || rem > 3 * left) return;               // can't finish with valid lengths
    if (left == 0) { out.add(String.join(".", parts)); return; }
    for (int len = 1; len <= 3 && start + len <= s.length(); len++) {
        String part = s.substring(start, start + len);
        if (!valid(part)) continue;
        parts.add(part);
        bt(s, start + len, parts, out);
        parts.remove(parts.size() - 1);
    }
}

static boolean valid(String p) {
    if (p.length() > 1 && p.charAt(0) == '0') return false; // leading zero
    return Integer.parseInt(p) <= 255;
}
```

### Line by line

| Line | What it does |
|---|---|
| `left`, `rem` | parts still needed, characters still unused |
| the pruning | too few or too many characters for the remaining parts |
| `left == 0` | 4 parts placed, and (by the pruning) all characters are used: record |
| the `len` loop | the next part is 1, 2 or 3 characters |
| `continue` | skip invalid parts |
| add / recurse / remove | the partition template |

When `left == 0`, the pruning line has already ensured `rem == 0` (since `rem > 3 · 0` returns), so every recorded address uses the whole string.

### Complexity
- **Time O(3⁴ · n)**, effectively constant for strings of length ≤ 12 (longer strings are pruned at the root).
- **Space O(1)** beyond the output (the depth is at most 4).

---

## 6. Comparing the approaches

| | Three loops | Backtracking |
|---|---|---|
| Splits examined | O(n³) | ≤ 81 |
| Generalises to k parts? | k − 1 loops | **just change 4** |

The file compares the two on 2,000 random digit strings (with extra zeros, to stress the leading-zero rule).

---

## 7. Edge cases and common mistakes

**Edge cases:** fewer than 4 or more than 12 digits (no answers), all zeros, parts exactly "255" or "256".

**Mistakes:**
1. Allowing "01" or "00" as parts.
2. Checking the value with `parseInt` before limiting the length (overflow on long inputs).
3. Recording 4 parts without checking that all characters were used.
4. Forgetting that "0" alone is valid.

---

## 8. Interview questions and answers

**Q1. Why prune on `left ≤ rem ≤ 3 · left`?**
> Each remaining part needs at least 1 and at most 3 characters. If the remaining characters fall outside that range, no completion exists.

**Q2. What's the maximum number of answers?**
> Small: at most 3³ = 27 ways to place the dots for a 4..12-character string, and usually far fewer survive the rules.

### Follow-up questions

**F1. IPv6 or other formats.**
> The same template with different part rules (hex digits, up to 4 characters, 8 parts).

**F2. Validate IP Address (LC 468).**
> Split on the separators and check each part with the same rules.

**F3. Palindrome Partitioning.** → [M10](M10_PalindromePartitioning.md)
> The same "choose where the next piece ends" template.

---

## 9. Running the file

```bash
java M11_RestoreIPAddresses.java      # 4/4 tests passed (including 2,000 random digit strings vs triple loop)
```

**One-line summary:** *"Partition the string into exactly 4 parts of 1–3 digits, skipping parts with a leading zero or a value above 255, and prune whenever the remaining length can't fit the remaining parts."*
