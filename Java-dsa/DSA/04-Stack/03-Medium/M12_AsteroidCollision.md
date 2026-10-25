# M12 · Asteroid Collision

| | |
|---|---|
| **LeetCode** | [#735 Asteroid Collision](https://leetcode.com/problems/asteroid-collision/) |
| **Difficulty** | Medium |
| **Pattern** | Stack simulation (a new element fights the top, possibly repeatedly) |
| **Code** | [M12_AsteroidCollision.java](M12_AsteroidCollision.java) |
| **Run** | `java M12_AsteroidCollision.java` |

---

## 1. The problem

Each asteroid's `|value|` is its size and its sign is its direction (`+` moves right, `−` moves left); all move at the same speed. When two meet, the **smaller** one explodes; if they're **equal**, both explode. Return the asteroids that survive.

| asteroids | survivors | why |
|---|---|---|
| `[5, 10, -5]` | `[5, 10]` | −5 hits 10 and explodes |
| `[8, -8]` | `[]` | equal: both explode |
| `[10, 2, -5]` | `[10]` | −5 destroys 2, then dies against 10 |
| `[-2, -1, 1, 2]` | `[-2, -1, 1, 2]` | they move apart: no collisions |
| `[1, -2, -2, -2]` | `[-2, -2, -2]` | the first −2 destroys the 1; the others move left freely |

**Constraints:** `2 ≤ n ≤ 10⁴`, the values are non-zero.

---

## 2. How to think about it

### When do two asteroids collide?
**Only** when a **right-mover (+)** is followed by a **left-mover (−)**. A `−` followed by a `+` move apart; two asteroids moving the same way never meet.

### Why a stack?
A new left-mover may destroy **several** right-movers in a row: the **most recent** one first (the one directly to its left). "The most recent unresolved thing" is a stack.

```
for each asteroid a:
    alive = true
    while alive and a < 0 and the stack's top > 0:      // a collision is happening
        if top < −a:  pop (the top explodes), keep fighting
        if top == −a: pop, alive = false (both explode)
        if top > −a:  alive = false (a explodes)
    if alive: push a
```

---

## 3. Java concepts used in this problem

### 3.1 A `while` loop with a flag

```java
boolean alive = true;
while (alive && a < 0 && !stack.isEmpty() && stack.peek() > 0) { ... }
if (alive) stack.push(a);
```

The flag `alive` ends the fight loop when the incoming asteroid dies, and decides afterwards whether it gets pushed.

### 3.2 Turning a Deque stack into an array in the right order

```java
int[] result = new int[stack.size()];
for (int i = result.length - 1; i >= 0; i--) result[i] = stack.pop();   // the top is the rightmost asteroid
```

Popping gives the elements from right to left, so we fill the array **from the end**.

### 3.3 `-a` for the size of a negative asteroid

For a < 0, `−a` is its size. Comparing `top` (a positive size) with `−a` decides the collision.

---

## 4. Approach 1: Repeated passes (naive)

```java
static int[] asteroidsNaive(int[] asteroids) {
    List<Integer> list = new ArrayList<>();
    for (int a : asteroids) list.add(a);
    boolean changed = true;
    while (changed) {
        changed = false;
        for (int i = 0; i + 1 < list.size(); i++) {
            int l = list.get(i), r = list.get(i + 1);
            if (l > 0 && r < 0) {                          // a colliding pair
                if (l > -r) list.remove(i + 1);
                else if (l < -r) list.remove(i);
                else { list.remove(i + 1); list.remove(i); }
                changed = true;
                break;
            }
        }
    }
    return list.stream().mapToInt(Integer::intValue).toArray();
}
```

Resolve one collision at a time and rescan: **O(n²)**.

---

## 5. Approach 2: Stack simulation (the file's solution)

### Code ([M12_AsteroidCollision.java:44-64](M12_AsteroidCollision.java#L44-L64))

```java
static int[] asteroidCollision(int[] asteroids) {
    Deque<Integer> stack = new ArrayDeque<>();
    for (int a : asteroids) {
        boolean alive = true;
        while (alive && a < 0 && !stack.isEmpty() && stack.peek() > 0) {
            int top = stack.peek();
            if (top < -a) {
                stack.pop();                 // the right-mover explodes; a keeps going
            } else if (top == -a) {
                stack.pop();                 // both explode
                alive = false;
            } else {
                alive = false;               // a explodes
            }
        }
        if (alive) stack.push(a);
    }
    int[] result = new int[stack.size()];
    for (int i = result.length - 1; i >= 0; i--) result[i] = stack.pop();   // the stack top is the rightmost
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `while (alive && a < 0 && ... stack.peek() > 0)` | a collision happens only between a left-moving `a` and a right-moving top |
| `if (top < -a) stack.pop();` | the top is smaller → it explodes; `a` continues left and may hit the next one |
| `else if (top == -a) { stack.pop(); alive = false; }` | the same size → both are gone |
| `else alive = false;` | the top is bigger → `a` is destroyed |
| `if (alive) stack.push(a);` | a survivor: a right-mover, or a left-mover with nothing left to hit |
| the final loop | turn the stack into the left-to-right result |

### Dry run 1: `[10, 2, -5]`

| a | fights | stack |
|---|---|---|
| 10 | none (positive) | [10] |
| 2 | none | [10, 2] |
| −5 | top 2 < 5 → pop; top 10 > 5 → −5 explodes | [10] |

**Result `[10]`** ✓

### Dry run 2: `[1, -2, -2, -2]`

| a | fights | stack |
|---|---|---|
| 1 | — | [1] |
| −2 | top 1 < 2 → pop; the stack is empty → push −2 | [−2] |
| −2 | the top is −2 (not > 0) → no fight → push | [−2, −2] |
| −2 | no fight → push | [−2, −2, −2] |

**Result `[-2, -2, -2]`** ✓

### Dry run 3: `[8, -8]`
top 8 == 8 → pop, `a` dies → **`[]`** ✓

### Complexity
- **Time O(n)**: each asteroid is pushed once and popped at most once.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Repeated passes | Stack |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(n) | O(n) |

---

## 7. Edge cases and common mistakes

**Edge cases:** everything moving left, everything moving right, `−` followed by `+` (no collision), chain destructions, equal sizes.

**Mistakes:**
1. Treating `[-2, 1]` as a collision (they move apart).
2. Stopping after one collision when the incoming asteroid survives (it must keep fighting).
3. Building the result by popping into an array from the front (it comes out reversed).

---

## 8. Interview questions and answers

**Q1. When do asteroids collide?**
> Only a right-mover with a left-mover to its right; everything else separates or moves in parallel.

**Q2. Why is it O(n) with a nested while loop?**
> Every asteroid enters the stack at most once and leaves at most once, so the total number of pops is at most n.

### Follow-up questions

**F1. Robot Collisions (LC 2751): robots with health; the winner loses 1 health.**
> The same stack, decrementing the survivor's health instead of keeping it intact.

**F2. Remove All Adjacent Duplicates.** → [E04](../02-Easy/E04_RemoveAllAdjacentDuplicates.md)
> The same "the new element interacts with the top, chain reactions follow" idea.

**F3. Count the collisions.**
> Increment a counter on every pop or explosion.

---

## 9. Running the file

```bash
java M12_AsteroidCollision.java      # 5/5 tests passed
```

**One-line summary:** *"Push right-movers; a left-mover fights the positive tops: smaller explodes, equal both, and it's pushed if it survives."*
