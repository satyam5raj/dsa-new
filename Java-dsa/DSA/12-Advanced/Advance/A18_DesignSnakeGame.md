# A18 · Design Snake Game ⭐

| | |
|---|---|
| **LeetCode** | [#353 Design Snake Game](https://leetcode.com/problems/design-snake-game/) |
| **Difficulty** | Medium |
| **Mix of structures** | **Deque** (the body, head at the front) + **HashSet** (occupied cells) |
| **Code** | [A18_DesignSnakeGame.java](A18_DesignSnakeGame.java) |
| **Run** | `java A18_DesignSnakeGame.java` |

⭐ = not in `leetcode.md`, added because it's the textbook "deque + set" design problem.

---

## 1. The problem

The classic Nokia snake on a `width × height` board:
- the snake starts at the top-left `(0,0)` with length 1;
- `food[]` is a list of positions; only the **next** piece is on the board at any time;
- `move(direction)` with `"U"`, `"D"`, `"L"` or `"R"` moves the head one cell and returns the **score** (food eaten so far), or **−1** if the game is over (it hit a wall or its own body);
- eating food makes the snake one cell longer.

```
SnakeGame(width 3, height 2, food [[1,2],[0,1]])
move R → 0     move D → 0     move R → 1 (eats (1,2))
move U → 1     move L → 2 (eats (0,1))     move U → −1 (wall)
```

---

## 2. How to think about it

### What each move needs
| Operation | Needs |
|---|---|
| add a new head, drop the tail | O(1) at **both ends** → a **deque** |
| "is the new head on the body?" | O(1) membership → a **hash set** of occupied cells |

A list alone makes the collision check O(length). A set alone doesn't know which cell is the tail. So use both, and keep them in sync.

Each cell is encoded as a single int, `row * width + col`, so it fits in a `Set<Integer>`.

### The subtle rule: the tail moves first
In one step the head moves forward **and** the tail moves away. So the head is allowed to move into the cell the tail is **leaving**, unless the snake eats this turn (then the tail stays where it is). That fixes the order of operations:

```
1. compute the new head; outside the board → −1
2. does it land on the food?  yes → don't move the tail     no → remove the tail first
3. now, if the new head is on the body → −1
4. add the head
```

Checking collision **before** removing the tail would wrongly end the game when the snake chases its own tail.

### Dry run: the example (cells numbered r·3 + c)
| move | new head | food? | body after (head first) | score |
|---|---|---|---|---|
| start | | | [0] | |
| R | (0,1) = 1 | no → drop 0 | [1] | 0 |
| D | (1,1) = 4 | no → drop 1 | [4] | 0 |
| R | (1,2) = 5 | **yes** | [5, 4] | 1 |
| U | (0,2) = 2 | no → drop 4 | [2, 5] | 1 |
| L | (0,1) = 1 | **yes** | [1, 2, 5] | 2 |
| U | (−1,1) | | wall | **−1** |

---

## 3. Java concepts used in this problem

### 3.1 `ArrayDeque` as a double-ended queue
| Method | Meaning |
|---|---|
| `addFirst(x)` | new head |
| `peekFirst()` | current head |
| `pollLast()` | remove and return the tail |
| `size()` | snake length |

All are O(1). `ArrayDeque` is a circular array, faster than `LinkedList` for this.

### 3.2 `Set.add` returns false for duplicates
```java
if (!occupied.add(next)) return -1;
```
`add` both inserts and reports whether the element was **new**. One call does the check and the update.

### 3.3 Encoding a cell as one int
`cell = r * width + c`, and back: `r = cell / width`, `c = cell % width`. This avoids creating `int[]` or record objects for every cell, and `Integer` has correct `equals`/`hashCode` for sets.

### 3.4 The arrow-form `switch`
```java
switch (direction) { case "U" -> r--; case "D" -> r++; case "L" -> c--; default -> c++; }
```
Java's `switch` on strings, with arrows: no `break` needed, and no fall-through.

---

## 4. Approach 1: A list of cells, collision by scanning (the referee)

### Code ([A18_DesignSnakeGame.java:45-64](A18_DesignSnakeGame.java#L45-L64))
```java
static class ListSnakeGame {
    private final int width, height;
    private final int[][] food;
    private int foodIndex = 0;
    private final List<int[]> snake = new ArrayList<>();               // index 0 = head

    ListSnakeGame(int width, int height, int[][] food) { this.width = width; this.height = height; this.food = food; snake.add(new int[]{0, 0}); }

    public int move(String direction) {
        int r = snake.get(0)[0], c = snake.get(0)[1];
        switch (direction) { case "U" -> r--; case "D" -> r++; case "L" -> c--; default -> c++; }
        if (r < 0 || r >= height || c < 0 || c >= width) return -1;
        boolean eats = foodIndex < food.length && food[foodIndex][0] == r && food[foodIndex][1] == c;
        if (eats) foodIndex++;
        else snake.remove(snake.size() - 1);
        for (int[] cell : snake) if (cell[0] == r && cell[1] == c) return -1;
        snake.add(0, new int[]{r, c});
        return snake.size() - 1;
    }
}
```

It uses the same rules, with a plain list: the collision scan and `add(0, …)` are both **O(length)** per move.

---

## 5. Approach 2: Deque + hash set (main solution)

### Code ([A18_DesignSnakeGame.java:67-92](A18_DesignSnakeGame.java#L67-L92))
```java
static class SnakeGame {
    private final int width, height;
    private final int[][] food;
    private int foodIndex = 0;
    private final Deque<Integer> body = new ArrayDeque<>();            // head at the front; cell = r * width + c
    private final Set<Integer> occupied = new HashSet<>();

    SnakeGame(int width, int height, int[][] food) {
        this.width = width; this.height = height; this.food = food;
        body.addFirst(0);
        occupied.add(0);
    }

    public int move(String direction) {
        int head = body.peekFirst(), r = head / width, c = head % width;
        switch (direction) { case "U" -> r--; case "D" -> r++; case "L" -> c--; default -> c++; }
        if (r < 0 || r >= height || c < 0 || c >= width) return -1;      // hit a wall
        int next = r * width + c;
        boolean eats = foodIndex < food.length && food[foodIndex][0] == r && food[foodIndex][1] == c;
        if (eats) foodIndex++;
        else occupied.remove(body.pollLast());                           // the tail moves out of the way first
        if (!occupied.add(next)) return -1;                              // ran into its own body
        body.addFirst(next);
        return body.size() - 1;                                          // score = food eaten = length - 1
    }
}
```

### Line by line
| Line | What it does |
|---|---|
| decode `head` | current head row and column |
| `switch` | one step in the chosen direction |
| bounds check | off the board → game over |
| `eats` | the next piece of food is exactly here (`foodIndex < food.length` guards against running out of food) |
| `pollLast()` + `remove` | not eating: the tail leaves, from **both** structures |
| `occupied.add(next)` false | the new head is on the body → game over |
| `addFirst(next)` | the new head |
| `size() − 1` | length 1 + food eaten |

### Complexity
**O(1)** per move (amortised hash and deque operations). Space **O(length + food)**.

---

## 6. Comparing the approaches

| | move | Notes |
|---|---|---|
| List + scan | O(length) | simple, slow for long snakes |
| **Deque + HashSet** | **O(1)** | the two structures must be updated together |

The file plays 3,000 random games on random boards and checks every move against the list version.

---

## 7. Edge cases and common mistakes

**Edge cases:** a snake that fills the whole board and follows its own tail (allowed), a length-2 snake reversing into its tail (allowed: the tail moves away), a length-3 snake turning back (it hits itself), running out of food, food in a corner.

**Mistakes:**
1. Checking collision before removing the tail (wrongly ends the game when following the tail).
2. Removing the tail even when eating.
3. Updating the deque but not the set (or the other way round).
4. Mixing up width and height (rows = height, columns = width).
5. Showing all food at once instead of one piece at a time.

---

## 8. Interview questions and answers

**Q1. Why a deque and a set?**
> The deque gives O(1) access to both ends (new head, old tail). The set answers "is this cell part of the body?" in O(1). Each covers what the other can't.

**Q2. Can the head move into the tail's cell?**
> Yes, if the snake isn't eating that turn, because the tail moves away in the same step. That's why the tail is removed before the collision check.

**Q3. What's the score?**
> The number of food pieces eaten, which equals the length minus 1.

### Follow-up questions

**F1. "Obstacles on the board."**
> Put obstacle cells in another set and check it with the wall test.

**F2. "Multiple snakes (multiplayer)."**
> One deque per snake and one shared occupied map from cell to snake id; resolve head-on collisions after computing all new heads.

**F3. "Food appears at random empty cells."**
> Keep the empty cells in an indexable set (HashMap + ArrayList, as in [Insert Delete GetRandom](../../06-Hash-Table/03-Medium/M06_InsertDeleteGetRandom.md)) to pick one uniformly in O(1).

---

## 9. Running the file

```bash
java A18_DesignSnakeGame.java     # 5/5 tests passed (example, full board, self-hit, tail-following, 3,000 random games)
```

**One-line summary:** *"Keep the body in a deque (head at the front) and the occupied cells in a HashSet. Each move computes the new head, removes the tail unless it eats, then fails on a wall or a cell still in the set, and otherwise adds the new head."*
