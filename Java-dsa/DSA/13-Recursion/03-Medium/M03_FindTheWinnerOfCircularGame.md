# M03 · Find the Winner of the Circular Game (Josephus) ⭐

| | |
|---|---|
| **LeetCode** | [#1823 Find the Winner of the Circular Game](https://leetcode.com/problems/find-the-winner-of-the-circular-game/) |
| **Difficulty** | Medium |
| **Pattern** | Recurse on n − 1 and **shift** the answer back (renumbering) |
| **Code** | [M03_FindTheWinnerOfCircularGame.java](M03_FindTheWinnerOfCircularGame.java) |
| **Run** | `java M03_FindTheWinnerOfCircularGame.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

n friends sit in a circle, numbered 1..n. Starting at friend 1, count k friends clockwise (including the one you start on). That friend leaves the circle. Counting restarts at the next friend. The last one left wins.

| n | k | winner | order of leaving |
|---|---|---|---|
| 5 | 2 | 3 | 2, 4, 1, 5 |
| 6 | 5 | 1 | |
| 1 | 7 | 1 | |

---

## 2. How to think about it

### Simulation
Put everyone in a queue. Move k − 1 people from the front to the back, then remove the front person (the k-th). Repeat until one is left. That's O(n · k): fine for small inputs, and the file's referee.

### The Josephus recurrence (0-indexed positions)
```
J(1) = 0                      one person: they win
J(n) = (J(n − 1) + k) % n
```

**Why it works:**
1. In a circle of n people at positions 0..n − 1, the first to leave is at position (k − 1) % n.
2. The game then continues with n − 1 people, starting from the next position, k % n.
3. **Renumber** those survivors 0..n − 2, starting at old position k. Now it's exactly the n − 1 game.
4. If the winner of that smaller game is at new position w, its old position is (w + k) % n.

The recursion solves the smaller game; we only have to shift its answer back. The final answer is J(n) + 1, because friends are numbered from 1.

### Dry run: n = 5, k = 2

| m | J(m) = (J(m − 1) + 2) % m |
|---|---|
| 1 | 0 |
| 2 | (0 + 2) % 2 = 0 |
| 3 | (0 + 2) % 3 = 2 |
| 4 | (2 + 2) % 4 = 0 |
| 5 | (0 + 2) % 5 = 2 |

J(5) = 2, so the winner is friend **3** ✓

### Dry run: the simulation for n = 5, k = 2

| queue (front first) | rotate 1 | leaves |
|---|---|---|
| 1 2 3 4 5 | 2 3 4 5 1 | 2 |
| 3 4 5 1 | 4 5 1 3 | 4 |
| 5 1 3 | 1 3 5 | 1 |
| 3 5 | 5 3 | 5 |
| 3 | | winner **3** |

---

## 3. Java concepts used in this problem

### 3.1 `%` for wrapping around a circle

`(w + k) % n` keeps a position inside 0..n − 1, wrapping past the end back to the start. Using 0-indexed positions is what makes `%` work cleanly; that's why the recursion is 0-indexed and the wrapper adds 1.

### 3.2 A `Deque` as a rotating circle

```java
circle.add(circle.poll());      // take the front person and put them at the back
```

`poll()` removes from the front and `add()` appends to the back. Rotating like this simulates walking around the circle. Queue methods are covered in [Queue E01](../../05-Queue/02-Easy/E01_ImplementStackUsingQueues.md#3-java-concepts-used-in-this-problem).

### 3.3 Unboxing on return

`return circle.peek();` returns an `Integer`, which is unboxed automatically to the method's `int` return type.

---

## 4. Approach 1: Simulate with a queue (the referee)

### Code ([M03_FindTheWinnerOfCircularGame.java:45-53](M03_FindTheWinnerOfCircularGame.java#L45-L53))

```java
static int simulate(int n, int k) {
    Deque<Integer> circle = new ArrayDeque<>();
    for (int i = 1; i <= n; i++) circle.add(i);
    while (circle.size() > 1) {
        for (int i = 0; i < k - 1; i++) circle.add(circle.poll());   // skip k-1 friends
        circle.poll();                                                 // the k-th leaves
    }
    return circle.peek();
}
```

| Line | What it does |
|---|---|
| the setup | friends 1..n in order |
| the inner loop | the k − 1 friends who are counted but stay move to the back |
| `circle.poll()` | the k-th friend leaves |
| `peek()` | the last one standing |

- **Time O(n · k), space O(n).**

---

## 5. Approach 2: The Josephus recurrence (the file's main solution)

### Code ([M03_FindTheWinnerOfCircularGame.java:38-43](M03_FindTheWinnerOfCircularGame.java#L38-L43))

```java
static int findTheWinner(int n, int k) { return josephus(n, k) + 1; }

static int josephus(int n, int k) {
    if (n == 1) return 0;
    return (josephus(n - 1, k) + k) % n;           // shift the smaller game's winner back
}
```

### Line by line

| Line | What it does |
|---|---|
| `findTheWinner` | convert the 0-indexed answer to a friend number |
| `n == 1` | the lone survivor is at position 0 |
| the return | solve the n − 1 game, then shift its winner by k around a circle of n |

- **Time O(n), space O(n)** for the recursion.

---

## 6. Approach 3: The recurrence as a loop

The recursion only goes downward and then builds the answer up from J(1), so a loop can do the same, starting from J(1) = 0. This version isn't in the `.java` file; it was checked against the recursion for every n and k from 1 to 200.

```java
class Loop {
    static int findTheWinner(int n, int k) {
        int w = 0;                              // J(1)
        for (int m = 2; m <= n; m++) w = (w + k) % m;
        return w + 1;
    }
}
```

Dry run n = 6, k = 5: w goes 1, 0, 1, 1, 0 for m = 2..6, so the winner is friend **1** ✓

- **Time O(n), space O(1).** There's no stack-overflow risk even for n = 10⁶.

---

## 7. Comparing the approaches

| | Simulation | Recursion | Loop |
|---|---|---|---|
| Time | O(n · k) | O(n) | O(n) |
| Space | O(n) | O(n) stack | **O(1)** |

The file checks the recursion against the simulation for every n and k from 1 to 60.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1 (friend 1 wins), k = 1 (friend n wins: people leave in order), k larger than n (the `%` handles it).

**Mistakes:**
1. Using 1-indexed positions in the recurrence (`%` then gives 0 for the last person).
2. `% (n − 1)` instead of `% n` (the shift happens in the circle of n people).
3. Forgetting the final `+ 1`.

---

## 9. Interview questions and answers

**Q1. Explain the recurrence.**
> After the first person leaves, the remaining n − 1 people form the same game, starting k positions later. If we know where the winner sits in that smaller game, adding k (mod n) converts it back to the original numbering.

**Q2. Can it be faster than O(n)?**
> For k = 2 there's a closed form: write n = 2ᵐ + l with 0 ≤ l < 2ᵐ, and the winner is 2l + 1. For general small k there's an O(k log n) method that removes several people per step.

### Follow-up questions

**F1. Return the full elimination order.**
> Simulate (O(n · k)), or use an order-statistics tree to find each next victim in O(log n).

**F2. The winner when counting starts at friend s.**
> Shift the answer: ((J(n) + s − 1) % n) + 1.

**F3. Elimination Game (LC 390).**
> A similar "solve the smaller game and map back" recursion.

---

## 10. Running the file

```bash
java M03_FindTheWinnerOfCircularGame.java      # 4/4 tests passed (including all n, k in 1..60 vs simulation)
```

**One-line summary:** *"With 0-indexed positions, J(1) = 0 and J(n) = (J(n − 1) + k) % n: solve the game with one fewer person and shift its winner back; the answer is J(n) + 1."*
