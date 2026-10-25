# M05 · Reveal Cards In Increasing Order ⭐

| | |
|---|---|
| **LeetCode** | [#950 Reveal Cards In Increasing Order](https://leetcode.com/problems/reveal-cards-in-increasing-order/) |
| **Difficulty** | Medium |
| **Pattern** | Simulate the process on **positions** (a queue of indices), then assign the values |
| **Code** | [M05_RevealCardsInIncreasingOrder.java](M05_RevealCardsInIncreasingOrder.java) |
| **Run** | `java M05_RevealCardsInIncreasingOrder.java` |

⭐ = a bonus classic.

---

## 1. The problem

The **reveal process**: take the top card and reveal it; then move the **next** top card to the **bottom**; repeat until no cards are left. Return an ordering of `deck` such that the cards are revealed in **increasing order**.

```
deck = [17, 13, 11, 2, 3, 5, 7]   →   [2, 13, 3, 11, 5, 17, 7]

revealing [2, 13, 3, 11, 5, 17, 7]:
  reveal 2,  move 13 to the bottom  → [3, 11, 5, 17, 7, 13]
  reveal 3,  move 11                → [5, 17, 7, 13, 11]
  reveal 5,  move 17                → [7, 13, 11, 17]
  reveal 7,  move 13                → [11, 17, 13]
  reveal 11, move 17                → [13, 17]
  reveal 13, move 17                → [17]
  reveal 17
revealed: 2, 3, 5, 7, 11, 13, 17 ✓
```

**Constraints:** `1 ≤ n ≤ 1000`, distinct values.

---

## 2. How to think about it

### The key idea
We don't know where each **value** should go, but we can simulate the process on the **positions** 0..n−1 to find out **in which order the positions get revealed**. Then put the smallest card at the first revealed position, the second smallest at the second, and so on.

```
the queue of positions: [0, 1, 2, 3, 4, 5, 6]
repeat:  pos = poll()            → this position gets the next smallest card
         if not empty: offer(poll())   (the next position goes to the bottom)

the reveal order of the positions:  0, 2, 4, 6, 3, 1, 5
the sorted cards:                   2, 3, 5, 7, 11, 13, 17
result[0]=2, result[2]=3, result[4]=5, result[6]=7, result[3]=11, result[1]=13, result[5]=17
→ [2, 13, 3, 11, 5, 17, 7]
```

"**Simulate on the indices, then assign the values**" is a reusable trick for any puzzle of the form "arrange the input so this process outputs a given order".

### Alternative: run the process backwards
Start from the largest card and undo each step: move the bottom card to the top, then put the next card on top. With a deque this also gives O(n log n).

---

## 3. Java concepts used in this problem

### 3.1 A queue of positions

```java
Deque<Integer> positions = new ArrayDeque<>();
for (int i = 0; i < n; i++) positions.offer(i);
result[positions.poll()] = card;                              // the position revealed next
if (!positions.isEmpty()) positions.offer(positions.poll());  // the next one goes to the bottom
```

The queue models the deck's **slots**, not the cards.

### 3.2 `deck.clone()` + `Arrays.sort`

Sort a copy, so the caller's array isn't reordered.

### 3.3 A verifier that runs the real process

`reveal(int[])` performs the actual reveal process on an ordering, which lets the tests check the **property** ("revealing gives sorted order") on 300 random decks.

---

## 4. Approach 1: Simulate on positions (the file's solution)

### Code ([M05_RevealCardsInIncreasingOrder.java:38-50](M05_RevealCardsInIncreasingOrder.java#L38-L50))

```java
static int[] deckRevealedIncreasing(int[] deck) {
    int n = deck.length;
    int[] sorted = deck.clone();
    Arrays.sort(sorted);
    Deque<Integer> positions = new ArrayDeque<>();
    for (int i = 0; i < n; i++) positions.offer(i);
    int[] result = new int[n];
    for (int card : sorted) {
        result[positions.poll()] = card;                 // this position is revealed next
        if (!positions.isEmpty()) positions.offer(positions.poll());   // next one goes to the bottom
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `sorted = deck.clone(); Arrays.sort(sorted);` | the cards in the order we want them revealed |
| `positions` = 0..n−1 | the slots of the final deck, from top to bottom |
| `for (int card : sorted)` | the next smallest card… |
| `result[positions.poll()] = card;` | …goes into the slot that gets revealed next |
| `positions.offer(positions.poll())` | simulate "move the next top card to the bottom" |

### Dry run: n = 7

| card | revealed position | queue after moving the next one to the bottom |
|---|---|---|
| 2 | 0 | [2, 3, 4, 5, 6, 1] |
| 3 | 2 | [4, 5, 6, 1, 3] |
| 5 | 4 | [6, 1, 3, 5] |
| 7 | 6 | [3, 5, 1] |
| 11 | 3 | [1, 5] |
| 13 | 1 | [5] |
| 17 | 5 | [] |

`result = [2, 13, 3, 11, 5, 17, 7]` ✓

### Complexity
- **Time O(n log n)** (the sort; the simulation is O(n)).
- **Space O(n).**

---

## 5. Approach 2: Reverse the process

```java
static int[] deckRevealedReverse(int[] deck) {
    int[] s = deck.clone();
    Arrays.sort(s);
    Deque<Integer> d = new ArrayDeque<>();
    for (int i = s.length - 1; i >= 0; i--) {
        if (!d.isEmpty()) d.offerFirst(d.pollLast());   // undo "move top to bottom": bottom → top
        d.offerFirst(s[i]);                             // undo "reveal": put the card back on top
    }
    return d.stream().mapToInt(Integer::intValue).toArray();
}
```

Building the deck from the **last** revealed card back to the first, each step undoes one reveal step in reverse order. It's the same complexity, and a nice way to show you understand the process.

**Dry run** (the last cards first): 17 → [17]; 13: move bottom to top (17), put 13 → [13, 17]; 11: move 17 to the top → [17, 13], put 11 → [11, 17, 13]; … → the final `[2, 13, 3, 11, 5, 17, 7]` ✓

---

## 6. Comparing the approaches

| | Simulate on positions | Reverse the process |
|---|---|---|
| Time | O(n log n) | O(n log n) |
| Space | O(n) | O(n) |
| Idea | forward on the indices | backward on the values |

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1, n = 2 (`[1, 1000]` → `[1, 1000]`), already sorted input.

**Mistakes:**
1. Simulating on the **values** instead of the positions.
2. Moving the card to the bottom even when the deck is empty after a reveal.
3. In the reverse version, doing the two undo steps in the wrong order.

---

## 8. Interview questions and answers

**Q1. Why simulate on indices?**
> The process doesn't depend on the card values, only on the positions. So the reveal order of the positions is fixed for a given n, and we fill those positions with the sorted cards.

**Q2. How would you verify the answer?**
> Run the real reveal process on the result and check that the output is sorted. The file does exactly that on 300 random decks.

### Follow-up questions

**F1. Reveal in decreasing order.**
> Assign the sorted cards in descending order to the same position order.

**F2. A different process (e.g. move two cards to the bottom each time).**
> The same technique: simulate the new process on the positions and assign the sorted values.

**F3. The Josephus problem (who survives the elimination?).** → [CircularLinkedList](../../03-Linked-Lists/01-Basics/CircularLinkedList.java)
> A related "queue rotation" process, with a known recursive formula.

---

## 9. Running the file

```bash
java M05_RevealCardsInIncreasingOrder.java      # 4/4 tests passed (including 300 random decks)
```

**One-line summary:** *"Simulate the reveal on positions with a queue; the k-th revealed position gets the k-th smallest card."*
