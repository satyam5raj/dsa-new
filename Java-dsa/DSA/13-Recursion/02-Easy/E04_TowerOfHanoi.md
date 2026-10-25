# E04 · Tower of Hanoi

| | |
|---|---|
| **Source** | `leetcode.md` "Recursion Basics" #70 (no LeetCode problem number) |
| **Difficulty** | Easy |
| **Pattern** | Solve the problem by **assuming the smaller case works** (the purest recursion exercise) |
| **Code** | [E04_TowerOfHanoi.java](E04_TowerOfHanoi.java) |
| **Run** | `java E04_TowerOfHanoi.java` |

Recursion basics are explained in [E01](E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

n disks of different sizes sit on rod A, the largest at the bottom. Move them all to rod C, using rod B as a spare:
- move one disk at a time (the top disk of a rod);
- never put a bigger disk on a smaller one.

List the moves. Each move is written `disk:fromto`, so `1:AB` means "move disk 1 from A to B".

```
n = 2  →  [1:AB, 2:AC, 1:BC]
```

---

## 2. How to think about it

### Don't plan the moves; trust the recursion
Planning the moves directly is hopeless for large n. Think about the **biggest** disk instead.

The biggest disk must move from A to C at some point. For that to happen, all n − 1 smaller disks must be out of the way, stacked on B. So:

```
1. move the top n − 1 disks  A → B   (using C as the spare)     ← a smaller problem
2. move disk n               A → C
3. move the n − 1 disks      B → C   (using A as the spare)     ← a smaller problem
```

Base case: 0 disks, so there's nothing to do.

We never need to "see" how step 1 works. We trust it (the leap of faith from [E01](E01_FibonacciNumber.md#31-the-two-parts-of-every-recursive-method)), because it's the same problem with fewer disks and different rod names.

### The number of moves
M(n) = 2·M(n − 1) + 1 and M(0) = 0, so M(n) = 2ⁿ − 1. That's also the **minimum** possible. 64 disks at one move per second would take about 585 billion years.

### Dry run: n = 3, solve(3, A, C, B)

```
solve(3, A→C, spare B)
├─ solve(2, A→B, spare C)
│  ├─ solve(1, A→C, spare B)  → 1:AC
│  ├─ 2:AB
│  └─ solve(1, C→B, spare A)  → 1:CB
├─ 3:AC
└─ solve(2, B→C, spare A)
   ├─ solve(1, B→A, spare C)  → 1:BA
   ├─ 2:BC
   └─ solve(1, A→C, spare B)  → 1:AC
```

Moves: **1:AC, 2:AB, 1:CB, 3:AC, 1:BA, 2:BC, 1:AC**, which is 7 = 2³ − 1.

| after move | A | B | C |
|---|---|---|---|
| start | 3 2 1 | | |
| 1:AC | 3 2 | | 1 |
| 2:AB | 3 | 2 | 1 |
| 1:CB | 3 | 2 1 | |
| 3:AC | | 2 1 | 3 |
| 1:BA | 1 | 2 | 3 |
| 2:BC | 1 | | 3 2 |
| 1:AC | | | 3 2 1 ✓ |

---

## 3. Java concepts used in this problem

### 3.1 Recursion that swaps the roles of its parameters

```java
solve(n - 1, from, spare, to, moves);      // "to" and "spare" swap roles
solve(n - 1, spare, to, from, moves);      // "from" and "spare" swap roles
```

The rods are just parameters. Each recursive call solves the same problem with a different assignment of rods.

### 3.2 Collecting results in a shared list

`List<String> moves` is passed into every call, and each call appends to the **same** list, so the moves come out in order. This is the "accumulator parameter" style; the alternative is for each call to return a new list and concatenate them, which costs more.

### 3.3 String concatenation with `char`

```java
moves.add(n + ":" + from + to);
```

It's evaluated left to right: `n + ":"` makes a `String`, so the `char`s `from` and `to` are appended as characters. (Careful: `from + to` **on its own** would add their numeric codes, 'A' + 'C' = 132.)

### 3.4 Parsing a move back (in the checker)

```java
int disk = Integer.parseInt(m.substring(0, m.indexOf(':')));
char from = m.charAt(m.length() - 2), to = m.charAt(m.length() - 1);
```

`indexOf(':')` finds the separator, `substring(0, i)` takes the digits before it, and `Integer.parseInt` turns them into a number. The last two characters are the rods.

### 3.5 Comparing an `Integer` from a `Deque` with an `int`

`rods.get(from).peek() != disk`: `peek()` returns an `Integer` and `disk` is an `int`, so Java unboxes and compares the values. (Two `Integer`s compared with `!=` would compare references, as explained in [Union-Find M03](../../11-Union-Find/03-Medium/M03_MostStonesRemoved.md#33-integer-vs-int-comparisons).)

---

## 4. Approach 1: Recursive solution (the file's main solution)

### Code ([E04_TowerOfHanoi.java:35-40](E04_TowerOfHanoi.java#L35-L40))

```java
static void solve(int n, char from, char to, char spare, List<String> moves) {
    if (n == 0) return;
    solve(n - 1, from, spare, to, moves);           // clear the way
    moves.add(n + ":" + from + to);                  // move the biggest
    solve(n - 1, spare, to, from, moves);           // put the rest back on top
}
```

### Line by line

| Line | What it does |
|---|---|
| `n == 0` | no disks: nothing to move |
| the first call | park the n − 1 smaller disks on the spare rod |
| `moves.add(...)` | disk n is now free to move to its target |
| the second call | bring the n − 1 disks from the spare rod onto disk n |

### Complexity
- **Time O(2ⁿ)**: exactly 2ⁿ − 1 moves are produced.
- **Space O(n)** recursion depth, plus the output list.

---

## 5. Approach 2: Simulate and verify (the file's checker)

### Code ([E04_TowerOfHanoi.java:43-55](E04_TowerOfHanoi.java#L43-L55))

```java
static boolean valid(int n, List<String> moves) {
    Map<Character, Deque<Integer>> rods = new HashMap<>();
    for (char r : new char[]{'A', 'B', 'C'}) rods.put(r, new ArrayDeque<>());
    for (int d = n; d >= 1; d--) rods.get('A').push(d);
    for (String m : moves) {
        int disk = Integer.parseInt(m.substring(0, m.indexOf(':')));
        char from = m.charAt(m.length() - 2), to = m.charAt(m.length() - 1);
        if (rods.get(from).isEmpty() || rods.get(from).peek() != disk) return false;   // must be the top disk
        if (!rods.get(to).isEmpty() && rods.get(to).peek() < disk) return false;       // no big on small
        rods.get(to).push(rods.get(from).pop());
    }
    return rods.get('C').size() == n;
}
```

| Line | What it does |
|---|---|
| `rods` | three real stacks, keyed by the rod letter (autoboxed to `Character`) |
| the setup loop | push n, n − 1, …, 1 onto A, so 1 is on top |
| the first `if` | the moved disk must be the top of its rod |
| the second `if` | it can't land on a smaller disk |
| the final check | all n disks ended up on C |

It's not a different way to solve the puzzle; it checks every rule for the produced moves. The test runs it for n = 0..16 and also checks there are exactly 2ⁿ − 1 moves.

---

## 6. Iterative alternative (for the curious)

There's a non-recursive solution: on odd-numbered moves, move disk 1 one step around a cycle (A → C → B → A when n is odd, A → B → C → A when n is even); on even-numbered moves, make the only legal move that doesn't involve disk 1. It produces the same 2ⁿ − 1 moves, but it's much harder to see why it works than the recursive version.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 0 (no moves), n = 1 (one move, A → C).

**Mistakes:**
1. Mixing up the argument order in the two recursive calls.
2. A base case of `n == 1` that forgets to record the move.
3. Trying to reason about every move instead of trusting the recursion.

---

## 8. Interview questions and answers

**Q1. Why is 2ⁿ − 1 the minimum?**
> The biggest disk must move at least once, and before it does, the other n − 1 disks must all be on the third rod (at least M(n − 1) moves). Afterwards they must all be moved onto it (at least M(n − 1) more). So M(n) ≥ 2·M(n − 1) + 1.

**Q2. What's the recursion depth?**
> n: each call reduces n by 1 before recursing.

### Follow-up questions

**F1. Which disk moves on move number k?**
> The number of trailing zeros of k, plus 1 (disk 1 moves on every odd move). `Integer.numberOfTrailingZeros(k) + 1`.

**F2. Four rods (the Reve's puzzle).**
> The Frame–Stewart algorithm: move k disks to a spare using all 4 rods, the rest with 3 rods, then the k back. Much fewer moves.

**F3. Generate all subsets or permutations.** → [14-Backtracking](../../14-Backtracking/)
> The same "trust the smaller call" thinking, with choices.

---

## 9. Running the file

```bash
java E04_TowerOfHanoi.java      # 2/2 tests passed (2 disks, n = 0..16: 2^n − 1 legal moves ending on C)
```

**One-line summary:** *"Move n − 1 disks to the spare rod, move the biggest disk to the target, then move the n − 1 disks onto it; this gives 2ⁿ − 1 moves."*
