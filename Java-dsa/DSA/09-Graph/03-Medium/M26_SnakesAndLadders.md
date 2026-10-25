# M26 · Snakes and Ladders

| | |
|---|---|
| **LeetCode** | [#909 Snakes and Ladders](https://leetcode.com/problems/snakes-and-ladders/) |
| **Difficulty** | Medium |
| **Pattern** | BFS over the squares (each die roll = an edge) + the boustrophedon index maths |
| **Code** | [M26_SnakesAndLadders.java](M26_SnakesAndLadders.java) |
| **Run** | `java M26_SnakesAndLadders.java` |

BFS basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#34-bfs-breadth-first-search-with-a-queue).

---

## 1. The problem

An n × n board, with squares numbered 1..n² starting at the **bottom-left** and zig-zagging ("boustrophedon": left → right, then right → left on the next row up, …). `board[r][c] != -1` means a snake or ladder **to** that square. From square s you roll 1..6 and move to s + 1..s + 6; if you land on a snake or ladder you **must** take it (once only: you don't chain). What's the fewest number of rolls to reach n², or −1?

```
6 × 6 board, numbering:            the example's snakes/ladders:
36 35 34 33 32 31                  square 2  → 15  (ladder)
25 26 27 28 29 30                  square 14 → 35  (ladder)
24 23 22 21 20 19                  square 17 → 13  (snake)
13 14 15 16 17 18
12 11 10  9  8  7
 1  2  3  4  5  6                  answer 4
```

---

## 2. How to think about it

### The graph
Square s → up to 6 neighbours (after applying the snake/ladder). Every roll costs 1 → **BFS** gives the minimum number of rolls.

### The tricky part: square number → (row, col)
```
k = s − 1                         (0-based)
rowFromBottom = k / n,   col = k % n
row = n − 1 − rowFromBottom
if rowFromBottom is ODD, the row runs right-to-left:  col = n − 1 − col
```

For n = 6: square 1 → (5, 0), square 6 → (5, 5), square 7 → (4, 5), square 12 → (4, 0).

### Rules to get right
- mark visited on the **destination** after the snake/ladder (where you actually end up)
- a ladder's end square is **not** re-checked for another ladder (no chaining)

### Dry run: the example (from the real run)

| rolls | squares reachable |
|---|---|
| 0 | 1 |
| 1 | 3, 4, 5, 6, 7, **15** (rolling 1 lands on 2 → ladder to 15) |
| 2 | 8–12, **13** (rolling from 15 to 17 → snake to 13), 16, 18–21 |
| 3 | 22–27, **35** (rolling from 13 to 14 → ladder to 35) |
| 4 | 28–33, **36** ✓ |

The path of landing squares: 1 → 15 → 13 → 35 → 36. Going **down** a snake (17 → 13) is part of the optimal route, because 13 is next to the big ladder at 14.

---

## 3. Java concepts used in this problem

### 3.1 Integer division and modulo for 2D coordinates

```java
int k = s - 1, fromBottom = k / n, col = k % n;
```

`/` gives the row (how many full rows come before), `%` the position within the row. That's the standard way to map a 1D index onto a grid.

### 3.2 Returning two values: `int[]{row, col}`

`cell` returns a 2-element array, a lightweight pair.

### 3.3 `dist[]` as both the visited marker and the answer

`dist[s] == -1` means unvisited; otherwise it's the minimum number of rolls. The BFS queue stores just the square numbers.

### 3.4 `Math.min(s + 6, target)`

It caps the rolls at the last square (you can't go past n²).

---

## 4. Approach 1: Relaxation until stable (the referee)

### Code ([M26_SnakesAndLadders.java:67-85](M26_SnakesAndLadders.java#L67-L85))

```java
static int relax(int[][] board) {
    int n = board.length, T = n * n, INF = 1_000_000;
    int[] d = new int[T + 1];
    Arrays.fill(d, INF);
    d[1] = 0;
    boolean changed = true;
    while (changed) {
        changed = false;
        for (int s = 1; s < T; s++) {
            if (d[s] == INF) continue;
            for (int nx = s + 1; nx <= Math.min(s + 6, T); nx++) {
                int[] rc = cell(nx, n);
                int dest = board[rc[0]][rc[1]] == -1 ? nx : board[rc[0]][rc[1]];
                if (d[s] + 1 < d[dest]) { d[dest] = d[s] + 1; changed = true; }
            }
        }
    }
    return d[T] == INF ? -1 : d[T];
}
```

Bellman-Ford on the squares: keep sweeping until no distance improves. Snakes create "backward" edges, so several sweeps may be needed. **O(n² · sweeps)**.

---

## 5. Approach 2: BFS over the squares (the file's main solution)

### Code ([M26_SnakesAndLadders.java:38-64](M26_SnakesAndLadders.java#L38-L64))

```java
static int snakesAndLadders(int[][] board) {
    int n = board.length, target = n * n;
    int[] dist = new int[target + 1];
    Arrays.fill(dist, -1);
    dist[1] = 0;
    Queue<Integer> q = new ArrayDeque<>(List.of(1));
    while (!q.isEmpty()) {
        int s = q.poll();
        if (s == target) return dist[s];
        for (int next = s + 1; next <= Math.min(s + 6, target); next++) {
            int[] rc = cell(next, n);
            int dest = board[rc[0]][rc[1]] == -1 ? next : board[rc[0]][rc[1]];   // take the snake/ladder
            if (dist[dest] == -1) {
                dist[dest] = dist[s] + 1;
                q.add(dest);
            }
        }
    }
    return -1;
}

// square number (1-based) -> {row, col} on the zig-zag board
static int[] cell(int s, int n) {
    int k = s - 1, fromBottom = k / n, col = k % n;
    if (fromBottom % 2 == 1) col = n - 1 - col;          // odd rows run right-to-left
    return new int[]{n - 1 - fromBottom, col};
}
```

### Line by line

| Line | What it does |
|---|---|
| `dist[1] = 0` | the game starts on square 1 |
| `if (s == target)` | the first time n² is polled, its distance is minimal |
| `next = s + 1 .. s + 6` | the six die outcomes |
| `cell(next, n)` | where that square is on the board |
| `dest = …` | you end up at the snake/ladder's destination, if there is one |
| `dist[dest] == -1` | an unvisited **landing** square → record it and queue it |
| `cell` | the boustrophedon mapping |

### Complexity
- **Time O(n²)** (each square is queued at most once, with 6 rolls each).
- **Space O(n²).**

---

## 6. Comparing the approaches

| | Relaxation until stable | BFS |
|---|---|---|
| Time | O(n² · sweeps) | **O(n²)** |
| Space | O(n²) | O(n²) |

The file checks BFS against relaxation on 300 random boards, and tests the `cell` mapping directly.

---

## 7. Edge cases and common mistakes

**Edge cases:** a 2 × 2 board, a ladder straight to n², unreachable n² (e.g. 6 snakes in a row), snakes that loop back.

**Mistakes:**
1. Wrong coordinates on the odd (right-to-left) rows.
2. Chaining snakes/ladders (following a second one from the destination).
3. Marking the **rolled** square as visited instead of the landing square.
4. Forgetting to cap the roll at n².

---

## 8. Interview questions and answers

**Q1. Why BFS rather than DP from left to right?**
> Snakes send you backwards, so the squares don't form a DAG in numeric order. BFS handles arbitrary edges with uniform cost.

**Q2. Why mark the destination, not the rolled square?**
> The rolled square with a snake/ladder isn't where you stand: you immediately move. Two different rolls landing on the same destination are the same state.

### Follow-up questions

**F1. Return the sequence of rolls.**
> Store each landing square's parent and the roll used; walk back from n².

**F2. Expected number of rolls with a random die.**
> A Markov chain: solve a linear system, or DP from the end if there are no snakes.

**F3. Minimum Jumps to Reach Home (LC 1654).**
> BFS over the states (position, last move direction).

---

## 9. Running the file

```bash
java M26_SnakesAndLadders.java      # 5/5 tests passed (including 300 random boards vs relaxation)
```

**One-line summary:** *"BFS over the squares where each of the 6 rolls is an edge to the square's snake/ladder destination; map square numbers to the zig-zag (row, col) carefully."*
