# M12 · Reverse Words in a String ⭐

| | |
|---|---|
| **LeetCode** | [#151 Reverse Words in a String](https://leetcode.com/problems/reverse-words-in-a-string/) |
| **Difficulty** | Medium |
| **Pattern** | Split + reverse, or reverse everything and then each word (in place on a `char[]`) |
| **Code** | [M12_ReverseWordsInString.java](M12_ReverseWordsInString.java) |
| **Run** | `java M12_ReverseWordsInString.java` |

⭐ = a bonus classic.

---

## 1. The problem

Reverse the **order of the words**. The output must have words separated by **single** spaces, with **no** leading or trailing spaces.

| s | answer |
|---|---|
| `"the sky is blue"` | `"blue is sky the"` |
| `"  hello world  "` | `"world hello"` |
| `"a good   example"` | `"example good a"` |

**Constraints:** `1 ≤ n ≤ 10⁴`; letters, digits and spaces; at least one word.

---

## 2. How to think about it

### Approach 1: split + join backwards
`words = s.trim().split("\\s+")`, then append them from last to first. Clear, O(n), and perfectly fine in an interview (just mention it uses O(n) extra space).

### Approach 2: the double reverse (in place on a `char[]`)
1. **Reverse the whole string:** `"the sky"` → `"yks eht"`. The words are now in the right **order** but each one is backwards.
2. **Reverse each word:** `"yks eht"` → `"sky the"`.
3. **Clean up the spaces** with a slow/fast pass: copy characters forward, keeping a single space between words.

It's the same idea as rotating an array with three reversals ([Arrays M02](../../01-Arrays/03-Medium/M02_RotateArray.md)): `reverse(AB) = BᴿAᴿ`, then reversing each block gives `BA`.

---

## 3. Java concepts used in this problem

### 3.1 `split("\\s+")` and `trim()`

See [E04](../02-Easy/E04_LengthOfLastWord.md). `"\\s+"` is the regex "one or more whitespace characters". Trimming first avoids an empty first element.

### 3.2 Alternatives: `Collections.reverse` + `String.join`

```java
List<String> words = Arrays.asList(s.trim().split("\\s+"));
Collections.reverse(words);          // reverses the list in place
return String.join(" ", words);      // glue them with single spaces
```

`Arrays.asList` wraps the array as a fixed-size list (changes write through to the array). `String.join(delimiter, items)` is the inverse of `split`.

### 3.3 `new String(char[], offset, count)`

```java
return new String(a, 0, w);   // a String from the first w characters of the array
```

It lets the in-place version return only the "used" part of the array after the spaces are squeezed out.

### 3.4 Why Java can't be truly in place here

`String` is immutable, so we must copy it into a `char[]` first (O(n)). In languages with mutable strings (C++), approach 2 is O(1) extra space. LeetCode's follow-up asks exactly that.

---

## 4. Approach 1: Split + join backwards (the file's main solution)

### Code ([M12_ReverseWordsInString.java:39-47](M12_ReverseWordsInString.java#L39-L47))

```java
static String reverseWords(String s) {
    String[] words = s.trim().split("\\s+");
    StringBuilder sb = new StringBuilder();
    for (int i = words.length - 1; i >= 0; i--) {
        sb.append(words[i]);
        if (i > 0) sb.append(' ');
    }
    return sb.toString();
}
```

### Line by line

| Line | What it does |
|---|---|
| `s.trim().split("\\s+")` | the words, with no empty strings (multiple spaces collapse) |
| `for (int i = words.length - 1; i >= 0; i--)` | from the last word to the first |
| `sb.append(words[i]);` | add the word |
| `if (i > 0) sb.append(' ');` | a space **between** words, but not after the last one |

### Dry run: `"  hello world  "`

- trim → `"hello world"` → split → `["hello", "world"]`
- i = 1: `"world"` + `" "` → i = 0: `"hello"`
- **Result `"world hello"`** ✓

**O(n) time, O(n) space.**

---

## 5. Approach 2: Reverse all, reverse each word, squeeze the spaces (the file's second method)

### Code ([M12_ReverseWordsInString.java:49-73](M12_ReverseWordsInString.java#L49-L73))

```java
static String reverseWordsInPlace(String s) {
    char[] a = s.toCharArray();
    int n = a.length;
    reverse(a, 0, n - 1);                                   // 1. whole string
    for (int i = 0; i < n; ) {                              // 2. each word
        while (i < n && a[i] == ' ') i++;
        int start = i;
        while (i < n && a[i] != ' ') i++;
        reverse(a, start, i - 1);
    }
    int w = 0;                                              // 3. squeeze spaces (slow/fast)
    for (int i = 0; i < n; i++) {
        if (a[i] != ' ') {
            a[w++] = a[i];
        } else if (w > 0 && a[w - 1] != ' ') {
            a[w++] = ' ';                                   // keep a single space after a word
        }
    }
    if (w > 0 && a[w - 1] == ' ') w--;                      // drop the trailing space
    return new String(a, 0, w);
}

static void reverse(char[] a, int i, int j) {
    while (i < j) { char t = a[i]; a[i++] = a[j]; a[j--] = t; }
}
```

### Line by line

| Line | What it does |
|---|---|
| `reverse(a, 0, n - 1);` | step 1: the whole array |
| `while (i < n && a[i] == ' ') i++;` | step 2: skip the spaces to the start of the next word |
| `while (i < n && a[i] != ' ') i++;` | find the end of the word |
| `reverse(a, start, i - 1);` | un-reverse that word. (If i == start, there was no word, and `reverse` does nothing) |
| `w` (the slow pointer) | step 3: where to write the cleaned output |
| `if (a[i] != ' ') a[w++] = a[i];` | always keep letters |
| `else if (w > 0 && a[w - 1] != ' ') a[w++] = ' ';` | keep a space only if something was written and the last written character isn't already a space (so leading and repeated spaces are dropped) |
| `if (w > 0 && a[w - 1] == ' ') w--;` | remove a single trailing space |
| `new String(a, 0, w)` | the cleaned result |

### Dry run: `"  hello world  "`

| step | array content |
|---|---|
| start | `␣␣hello␣world␣␣` |
| 1. reverse all | `␣␣dlrow␣olleh␣␣` |
| 2. reverse each word | `␣␣world␣hello␣␣` |
| 3. squeeze | `world␣hello` (w = 11) |

During step 3: the two leading spaces are skipped (w = 0); `world` is copied; one space kept; `hello` copied; the first trailing space is kept (the last written was `o`), the second dropped; then the final check removes the trailing space.

**Result `"world hello"`** ✓

### Complexity
- **Time O(n):** three linear passes.
- **Space O(n)** in Java (the char array copy); O(1) extra in languages with mutable strings.

---

## 6. Comparing the approaches

| | Split + join | Double reverse |
|---|---|---|
| Time | O(n) | O(n) |
| Extra space | O(n) (words array) | O(1) beyond the char[] |
| Code | short | longer, error-prone |
| Handles messy spacing | `trim` + `\\s+` | the squeeze step |

---

## 7. Edge cases and common mistakes

**Edge cases:** leading/trailing spaces, multiple spaces between words, a single word, a single word surrounded by spaces (`"   hi "`).

**Mistakes:**
1. `split(" ")`, which produces empty strings for repeated spaces.
2. Adding a trailing space after the last word.
3. In the in-place version, forgetting step 3 (cleaning the spaces) or mishandling the trailing space.
4. Reversing the **characters** of the whole string only (that also reverses each word's letters).

---

## 8. Interview questions and answers

**Q1. How would you do it in O(1) extra space?**
> On a mutable character array: reverse the whole array, reverse each word, then compact the spaces with a slow/fast pointer. In Java, the char[] copy itself is O(n), because Strings are immutable.

**Q2. Why does the double reverse work?**
> Reversing the whole string puts the words in reverse order but reverses each word's letters too. Reversing each word individually undoes the letter reversal while keeping the new word order.

### Follow-up questions

**F1. Reverse Words in a String II (LC 186): the input is a char[] with single spaces only.**
> Steps 1 and 2 only (no cleanup needed).

**F2. Reverse Words in a String III (LC 557): reverse each word's letters, keeping the word order.**
> Step 2 only.

**F3. Rotate a string by k positions.**
> The three-reversal trick: reverse the parts and then the whole.

---

## 9. Running the file

```bash
java M12_ReverseWordsInString.java      # 6/6 tests passed
```

**One-line summary:** *"Split + join backwards; or reverse everything, reverse each word, and squeeze the spaces."*
