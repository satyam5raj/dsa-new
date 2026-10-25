# M07 · Encode and Decode Strings

| | |
|---|---|
| **LeetCode** | [#271 Encode and Decode Strings](https://leetcode.com/problems/encode-and-decode-strings/) (Premium) |
| **Difficulty** | Medium |
| **Pattern** | Length-prefix encoding (how real network protocols frame messages) |
| **Code** | [M07_EncodeDecodeStrings.java](M07_EncodeDecodeStrings.java) |
| **Run** | `java M07_EncodeDecodeStrings.java` |

---

## 1. The problem

Design two functions:
- `encode(List<String>) → String`
- `decode(String) → List<String>`

such that `decode(encode(x))` equals `x` for **any** list, including strings that contain **any** character (commas, `#`, digits, empty strings…).

| list | one possible encoding |
|---|---|
| `["hello", "world"]` | `"5#hello5#world"` |
| `["hello", "", "a#b"]` | `"5#hello0#3#a#b"` |
| `[]` | `""` |
| `[""]` | `"0#"` |

---

## 2. How to think about it

### Why "join with a delimiter" fails
`String.join(",", list)` turns `["a,b", "c"]` and `["a", "b", "c"]` into the **same** string `"a,b,c"`, so decoding is ambiguous. **Any** delimiter character can appear inside a string.

### Fix 1: escaping
Escape the delimiter inside strings (like CSV quoting or `\,`). It works, but it's fiddly: you have to escape the escape character too.

### Fix 2: a length prefix (simple and robust)
Encode each string as **`<length>#<string>`**:

```
["hello", "", "a#b"]  →  "5#hello" + "0#" + "3#a#b"  =  "5#hello0#3#a#b"
```

**Decoding:** read digits up to the next `#`; that number is the length L. Then take **exactly** the next L characters, whatever they contain, and repeat. The `#` only ends the **number**; inside a string it's never interpreted, because we already know how many characters to take.

This is how real protocols frame messages: HTTP `Content-Length`, Protobuf length-delimited fields, Redis RESP (`$5\r\nhello`).

---

## 3. Java concepts used in this problem

### 3.1 `StringBuilder.append` with different types

```java
sb.append(s.length()).append('#').append(s);   // int, char, String: all supported, chained
```

`append` is overloaded for every type and returns the builder itself, so calls chain. An `int` is appended as its decimal digits.

### 3.2 `indexOf(char, fromIndex)`

```java
int hash = encoded.indexOf('#', i);   // the first '#' at or after position i
```

It searches from `i` onward, so it finds the `#` that ends the **current** length field. A `#` inside an earlier string is already behind us.

### 3.3 `Integer.parseInt(String)`

```java
Integer.parseInt("12")   // 12
Integer.parseInt("x")    // NumberFormatException
```

It converts the digit text into an `int`.

### 3.4 `substring(start, end)`

It takes exactly `end − start` characters (end exclusive). With `end = start + len`, we get exactly the string's characters.

### 3.5 Comparing lists in the tests

`check("round trip", a, decode(encode(a)))`: the helper compares the **text** of the two lists (`[hello, world]`), which is the same as element-by-element equality here. In real code, `list1.equals(list2)` compares lists by content.

---

## 4. Approach 1: A delimiter (broken), for contrast

```java
static String encodeJoin(List<String> strs) { return String.join(",", strs); }
static List<String> decodeSplit(String s) { return Arrays.asList(s.split(",", -1)); }
```

- `String.join(sep, list)` glues the elements with `sep`.
- `split(",", -1)`: the `-1` limit **keeps trailing empty strings** (by default `split` drops them).
- `["a,b", "c"]` → `"a,b,c"` → decoded as `["a", "b", "c"]` ✗. And an empty list and `[""]` both encode to `""`. This is the bug that the length prefix fixes.

---

## 5. Approach 2: Length prefix (the file's solution)

### Code ([M07_EncodeDecodeStrings.java:39-56](M07_EncodeDecodeStrings.java#L39-L56))

```java
static String encode(List<String> strs) {
    StringBuilder sb = new StringBuilder();
    for (String s : strs) sb.append(s.length()).append('#').append(s);
    return sb.toString();
}

static List<String> decode(String encoded) {
    List<String> out = new ArrayList<>();
    int i = 0;
    while (i < encoded.length()) {
        int hash = encoded.indexOf('#', i);                // end of the length field
        int len = Integer.parseInt(encoded.substring(i, hash));
        int start = hash + 1;
        out.add(encoded.substring(start, start + len));    // take exactly len chars
        i = start + len;
    }
    return out;
}
```

### Line by line: `encode`

| Line | What it does |
|---|---|
| `for (String s : strs)` | each string |
| `sb.append(s.length()).append('#').append(s);` | write its length, a `#`, then the raw string |
| `return sb.toString();` | one string |

### Line by line: `decode`

| Line | What it does |
|---|---|
| `int i = 0;` | the current read position, always at the start of a length field |
| `int hash = encoded.indexOf('#', i);` | where the length field ends |
| `int len = Integer.parseInt(encoded.substring(i, hash));` | the length L |
| `int start = hash + 1;` | the string's first character |
| `out.add(encoded.substring(start, start + len));` | exactly L characters, whatever they are |
| `i = start + len;` | jump to the next length field |

### Dry run: decode `"5#hello0#3#a#b"`

| i | `#` found at | length text | len | string taken | next i |
|---|---|---|---|---|---|
| 0 | 1 | `"5"` | 5 | `hello` (2..6) | 7 |
| 7 | 8 | `"0"` | 0 | `""` | 9 |
| 9 | 10 | `"3"` | 3 | `a#b` (11..13) | 14 = end |

**Result `["hello", "", "a#b"]`** ✓. The `#` inside `"a#b"` is never looked at as a separator, because we take exactly 3 characters.

### Dry run: tricky strings `["12#", "#"]`
Encoded: `"3#12#" + "1##"` = `"3#12#1##"`. Decode: i=0 → `#` at 1 → len 3 → `"12#"` → i = 5 → `#` at 6 → len 1 → `"#"` → i = 8 → done ✓

### Complexity
- **Time O(total length)** for both encode and decode.
- **Space O(total length)** for the output.

---

## 6. (Bonus) Approach 3: A fixed-width length header

Instead of `<digits>#`, write each length as exactly **4 characters** (e.g. `String.format("%04d", len)`), or as raw bytes. Decoding reads 4 characters, parses them, then takes that many. There's no separator at all, and it's how binary protocols work (e.g. a 4-byte big-endian length). The downside is a maximum length (9999 for 4 decimal digits).

```java
static String encodeFixed(List<String> strs) {
    StringBuilder sb = new StringBuilder();
    for (String s : strs) sb.append(String.format("%04d", s.length())).append(s);
    return sb.toString();
}
```

`String.format("%04d", 7)` gives `"0007"`: a width of 4, padded with zeros.

---

## 7. Comparing the approaches

| | Delimiter | Escaping | Length prefix | Fixed-width header |
|---|---|---|---|---|
| Correct for any content | **no** | yes | **yes** | yes (up to the max length) |
| Complexity of the code | trivial | fiddly | **simple** | simple |
| Used in real protocols | CSV (with quoting) | JSON strings | HTTP, Redis | TCP framing, Protobuf |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty list (`""`), a list with one empty string (`"0#"`), strings containing `#` or digits, very long strings (multi-digit lengths).

**Mistakes:**
1. Using `split("#")` to decode, which breaks on strings that contain `#`.
2. Searching for `#` from the start of the string instead of from `i`.
3. Assuming the length is a single digit.

---

## 9. Interview questions and answers

**Q1. Why does the length prefix work for any content?**
> The decoder never interprets the string's characters. It reads a number and then copies exactly that many characters, so nothing inside the string can confuse it.

**Q2. Could a `#` inside a string be mistaken for the end of the length field?**
> No. `indexOf('#', i)` is only called at the **start** of a length field, and the length field consists only of digits, so the first `#` after them is always the separator.

**Q3. How do real systems do this?**
> HTTP's Content-Length header, gRPC/Protobuf length-delimited messages, Redis's RESP (`$<len>\r\n<data>`), and TCP framing with fixed-size length headers.

### Follow-up questions

**F1. Serialize and Deserialize a Binary Tree (LC 297).** → [07-Trees H01](../../07-Trees/04-Hard/H01_SerializeDeserializeBinaryTree.md)
> The same "design a reversible encoding" problem for a tree: preorder with null markers.

**F2. Encode nested lists.**
> Use length prefixes recursively (a list's length, then its encoded elements), or a type tag per element.

**F3. Make it more compact.**
> Use binary lengths (varints, as in Protobuf) instead of decimal text.

---

## 10. Running the file

```bash
java M07_EncodeDecodeStrings.java      # 5/5 tests passed
```

**One-line summary:** *"Encode as length#string; decode by reading the length, then taking exactly that many characters."*
