# M14 · Design Browser History

| | |
|---|---|
| **LeetCode** | [#1472 Design Browser History](https://leetcode.com/problems/design-browser-history/) |
| **Difficulty** | Medium |
| **Pattern** | Two stacks (back / forward), or an array + a cursor |
| **Code** | [M14_DesignBrowserHistory.java](M14_DesignBrowserHistory.java) |
| **Run** | `java M14_DesignBrowserHistory.java` |

---

## 1. The problem

Design `BrowserHistory(homepage)` with:
- `visit(url)`: go to url. **This clears all the forward history** (like a real browser).
- `back(steps)`: go back up to `steps` pages (as far as possible) and return the current URL.
- `forward(steps)`: go forward up to `steps` pages and return the current URL.

The LeetCode sequence:

```
start at leetcode.com
visit google, facebook, youtube
back(1)    → facebook.com
back(1)    → google.com
forward(1) → facebook.com
visit linkedin.com            (the forward history [youtube] is cleared)
forward(2) → linkedin.com     (nothing ahead)
back(2)    → google.com
back(7)    → leetcode.com     (it can't go further back than the homepage)
```

---

## 2. How to think about it

### Approach 1: two stacks (the classic undo/redo design)
- **back stack** = the pages behind us (top = the most recent)
- **forward stack** = the pages ahead of us (filled when we go back)
- **current** = the page we're on

| operation | action |
|---|---|
| `visit(url)` | push current onto back; current = url; **clear forward** |
| `back(k)` | up to k times: push current onto forward, current = back.pop() |
| `forward(k)` | the mirror image |

Text editors implement undo/redo exactly like this.

### Approach 2: an array + a cursor (O(1) per call, even for big jumps)
- the `history` list, the `cur` index, and `last` = the last valid index
- `visit`: `cur++`, overwrite (or append) `history[cur]`, `last = cur`. The forward pages are "forgotten" without deleting anything.
- `back(k)`: `cur = max(0, cur − k)`
- `forward(k)`: `cur = min(last, cur + k)`

Jumping k steps is O(1) here, versus O(k) with stacks.

---

## 3. Java concepts used in this problem

### 3.1 A design class with a constructor

```java
static class BrowserHistory {
    private final Deque<String> back = new ArrayDeque<>(), forward = new ArrayDeque<>();
    private String current;
    BrowserHistory(String homepage) { current = homepage; }
```

The constructor sets the initial state. The fields are private (encapsulation), and the two stacks are `final` (the references never change; their contents do).

### 3.2 `deque.clear()`

It empties the deque in O(size). In the array version, clearing the forward history is just `last = cur`, an O(1) "logical delete".

### 3.3 `while (steps-- > 0 && !back.isEmpty())`

Post-decrement in the condition: it compares the current `steps`, then decreases it. The loop runs at most `steps` times, and stops early when there's nothing left to go back to.

### 3.4 `List.set` versus `add`

```java
if (cur < history.size()) history.set(cur, url); else history.add(url);
```

`set` overwrites an existing slot (a stale forward page); `add` appends when we're at the end. That reuses memory instead of growing forever.

---

## 4. Approach 1: Two stacks (the file's first class)

### Code ([M14_DesignBrowserHistory.java:38-65](M14_DesignBrowserHistory.java#L38-L65))

```java
static class BrowserHistory {                          // two stacks
    private final Deque<String> back = new ArrayDeque<>(), forward = new ArrayDeque<>();
    private String current;

    BrowserHistory(String homepage) { current = homepage; }

    void visit(String url) {
        back.push(current);
        current = url;
        forward.clear();                                // new page: the forward history is gone
    }

    String back(int steps) {
        while (steps-- > 0 && !back.isEmpty()) {
            forward.push(current);
            current = back.pop();
        }
        return current;
    }

    String forward(int steps) {
        while (steps-- > 0 && !forward.isEmpty()) {
            back.push(current);
            current = forward.pop();
        }
        return current;
    }
}
```

### Dry run (the LeetCode sequence)

| operation | back (top on the right) | current | forward (top on the right) | returns |
|---|---|---|---|---|
| init | | leetcode | | |
| visit google | leetcode | google | | |
| visit facebook | leetcode google | facebook | | |
| visit youtube | leetcode google facebook | youtube | | |
| back(1) | leetcode google | facebook | youtube | facebook |
| back(1) | leetcode | google | youtube facebook | google |
| forward(1) | leetcode google | facebook | youtube | facebook |
| visit linkedin | leetcode google facebook | linkedin | **(cleared)** | |
| forward(2) | | linkedin | (empty) | linkedin |
| back(2) | leetcode google | facebook → **google** | linkedin facebook | google |
| back(7) | (empty) | leetcode | … | leetcode |

**Matches `[facebook, google, facebook, linkedin, google, leetcode]`** ✓

**visit O(1) (clear is O(forward size), amortised O(1)), back/forward O(steps).**

---

## 5. Approach 2: Array + cursor (the file's second class)

### Code ([M14_DesignBrowserHistory.java:67-81](M14_DesignBrowserHistory.java#L67-L81))

```java
static class BrowserHistoryArray {                      // array + cursor
    private final List<String> history = new ArrayList<>();
    private int cur = 0, last = 0;

    BrowserHistoryArray(String homepage) { history.add(homepage); }

    void visit(String url) {
        cur++;
        if (cur < history.size()) history.set(cur, url); else history.add(url);
        last = cur;                                     // everything after cur is now invalid
    }

    String back(int steps) { cur = Math.max(0, cur - steps); return history.get(cur); }
    String forward(int steps) { cur = Math.min(last, cur + steps); return history.get(cur); }
}
```

### Line by line

| Method | What it does |
|---|---|
| `visit` | move the cursor forward, write the url there (overwriting a stale page or appending), and mark it as the new end |
| `back` | clamp the cursor at 0 |
| `forward` | clamp the cursor at `last` (it can't go into the forgotten pages beyond it) |

### Dry run (the key moments)
After the three visits: `history = [leetcode, google, facebook, youtube]`, cur = last = 3. `back(1)` → 2; `back(1)` → 1; `forward(1)` → 2 (facebook). `visit(linkedin)`: cur = 3, `set(3, linkedin)` (overwriting youtube), **last = 3**. `forward(2)`: min(3, 5) = 3 → linkedin. `back(2)` → 1 → google. `back(7)` → 0 → leetcode ✓

**All operations O(1).**

---

## 6. Comparing the approaches

| | Two stacks | Array + cursor |
|---|---|---|
| visit | O(1) amortised (the clear) | **O(1)** |
| back/forward(k) | O(k) | **O(1)** |
| Memory | only the reachable pages | can keep stale entries (overwritten later) |

---

## 7. Edge cases and common mistakes

**Edge cases:** going back past the homepage, forward with nothing ahead, a visit after going back (the forward history must clear), large step counts.

**Mistakes:**
1. Forgetting to clear the forward history on visit.
2. In the array version, not tracking `last`, so `forward` could reach stale pages.
3. Moving `steps` times without checking for an empty stack.

---

## 8. Interview questions and answers

**Q1. Why does visit clear the forward history?**
> A new visit creates a new branch; the old forward pages belong to a branch we've left. Browsers (and undo systems) discard them.

**Q2. Which design is better?**
> The array + cursor: all O(1), even for big jumps. The two-stack version maps directly onto undo/redo and only keeps the reachable pages.

### Follow-up questions

**F1. Undo/redo in a text editor.**
> The same two stacks, holding commands (or inverse commands) instead of URLs.

**F2. Limit the history size.**
> Use a bounded deque (drop the oldest from the bottom of the back stack), or a circular buffer for the array version.

**F3. Keep the forward history as a tree (branching history).**
> Each page node keeps a list of children; back = parent, forward = the most recently used child.

---

## 9. Running the file

```bash
java M14_DesignBrowserHistory.java      # 2/2 tests passed (both versions on the LeetCode sequence)
```

**One-line summary:** *"Two stacks (back/forward, clear forward on visit), or an array + cursor + last index for O(1) jumps."*
