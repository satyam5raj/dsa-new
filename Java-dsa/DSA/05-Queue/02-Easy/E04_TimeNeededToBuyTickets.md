# E04 · Time Needed to Buy Tickets ⭐

| | |
|---|---|
| **LeetCode** | [#2073 Time Needed to Buy Tickets](https://leetcode.com/problems/time-needed-to-buy-tickets/) |
| **Difficulty** | Easy |
| **Pattern** | Queue simulation → then a one-pass formula |
| **Code** | [E04_TimeNeededToBuyTickets.java](E04_TimeNeededToBuyTickets.java) |
| **Run** | `java E04_TimeNeededToBuyTickets.java` |

⭐ = a bonus classic.

---

## 1. The problem

People stand in a line; `tickets[i]` = how many tickets person i wants. Each second, the person at the front buys **one** ticket, then goes to the **back** of the line if they still need more (or leaves if they're done). How many seconds until person **k** has all of their tickets?

| tickets | k | answer |
|---|---|---|
| `[2, 3, 2]` | 2 | 6 |
| `[5, 1, 1, 1]` | 0 | 8 |

**Constraints:** `1 ≤ n ≤ 100`, `1 ≤ tickets[i] ≤ 100`.

---

## 2. How to think about it

### Approach 1: simulate with a queue of indices
This is a **round-robin** scheduler: poll i, buy one ticket, `time++`; if i == k and they're done → return the time; otherwise, if i still needs tickets, offer i again. It's O(sum of the tickets): fine for small inputs, and it **matches the story exactly**.

### Approach 2: count directly (O(n))
Person k needs `t = tickets[k]` rounds. During that time:
- someone **ahead** of k (i ≤ k) buys `min(tickets[i], t)` tickets: they get a turn **before** k in every round, including k's final one;
- someone **behind** k (i > k) buys `min(tickets[i], t − 1)`: in k's last round, k finishes **before** they get their turn.

`answer = the sum of those amounts` (each ticket bought takes one second).

"Simulate first, then find the formula" is a great way to approach these problems, and the simulation becomes your test oracle.

---

## 3. Java concepts used in this problem

### 3.1 A queue of indices for round robin

```java
Deque<Integer> q = new ArrayDeque<>();
for (int i = 0; i < n; i++) q.offer(i);
int i = q.poll();       // the person at the front
q.offer(i);             // back to the end of the line
```

Store **indices**, not values, so you can update `t[i]` and recognise person k.

### 3.2 `tickets.clone()` before simulating

The simulation decrements the counts, and cloning keeps the caller's array intact (see Arrays E02).

### 3.3 `while (true)` with a `return` inside

The loop ends only when person k finishes. The problem guarantees that it will.

### 3.4 The ternary inside `Math.min`

```java
time += Math.min(tickets[i], i <= k ? need : need - 1);
```

It chooses the cap depending on whether person i is ahead of k or behind.

---

## 4. Approach 1: Queue simulation (the file's first method)

### Code ([E04_TimeNeededToBuyTickets.java:41-53](E04_TimeNeededToBuyTickets.java#L41-L53))

```java
static int timeRequiredToBuySimulate(int[] tickets, int k) {
    int[] t = tickets.clone();
    Deque<Integer> q = new ArrayDeque<>();
    for (int i = 0; i < t.length; i++) q.offer(i);
    int time = 0;
    while (true) {
        int i = q.poll();
        t[i]--;
        time++;
        if (i == k && t[i] == 0) return time;
        if (t[i] > 0) q.offer(i);                 // back of the line
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `q.offer(i)` for each person | the initial line |
| `int i = q.poll(); t[i]--; time++;` | the front person buys one ticket, taking one second |
| `if (i == k && t[i] == 0) return time;` | person k just finished |
| `if (t[i] > 0) q.offer(i);` | still needs more → back of the line; otherwise they leave |

### Dry run: `[2, 3, 2]`, k = 2

| second | person | tickets after | line after |
|---|---|---|---|
| 1 | 0 | [1, 3, 2] | 1 2 0 |
| 2 | 1 | [1, 2, 2] | 2 0 1 |
| 3 | 2 | [1, 2, 1] | 0 1 2 |
| 4 | 0 | [0, 2, 1] | 1 2 (person 0 leaves) |
| 5 | 1 | [0, 1, 1] | 2 1 |
| 6 | 2 | [0, 1, 0] | → **k is done at 6** |

**Return 6** ✓

**O(sum of the tickets) time, O(n) space.**

---

## 5. Approach 2: Direct count (the file's main method)

### Code ([E04_TimeNeededToBuyTickets.java:55-61](E04_TimeNeededToBuyTickets.java#L55-L61))

```java
static int timeRequiredToBuy(int[] tickets, int k) {
    int time = 0, need = tickets[k];
    for (int i = 0; i < tickets.length; i++) {
        time += Math.min(tickets[i], i <= k ? need : need - 1);
    }
    return time;
}
```

### Line by line

| Line | What it does |
|---|---|
| `need = tickets[k]` | the number of rounds person k needs |
| `i <= k ? need : need - 1` | people at or before k get up to `need` turns before k finishes; people after k get up to `need − 1` |
| `Math.min(tickets[i], ...)` | nobody buys more tickets than they want |
| `time +=` | each ticket takes one second |

### Dry run 1: `[2, 3, 2]`, k = 2, need = 2

| i | ahead/behind | cap | min(tickets[i], cap) |
|---|---|---|---|
| 0 | ahead | 2 | 2 |
| 1 | ahead | 2 | 2 |
| 2 | k itself | 2 | 2 |

Total **6** ✓

### Dry run 2: `[5, 1, 1, 1]`, k = 0, need = 5

| i | cap | min |
|---|---|---|
| 0 (k) | 5 | 5 |
| 1 (behind) | 4 | 1 |
| 2 | 4 | 1 |
| 3 | 4 | 1 |

Total **8** ✓

**O(n) time, O(1) space.** The file checks that the formula equals the simulation on 500 random lines.

---

## 6. Comparing the approaches

| | Simulation | Formula |
|---|---|---|
| Time | O(Σ tickets) | **O(n)** |
| Space | O(n) | **O(1)** |
| Matches the story directly | **yes** | needs reasoning |

---

## 7. Edge cases and common mistakes

**Edge cases:** k at the front (k = 0), k at the end, k needing just 1 ticket, everyone needing 1 ticket.

**Mistakes:**
1. Using `need` for the people **behind** k (they don't get k's final round).
2. Forgetting to cap at `tickets[i]` (people leave once they're done).
3. Simulating without a clone, which modifies the input.

---

## 8. Interview questions and answers

**Q1. Why do the people behind k only get `need − 1` turns?**
> In each of k's first need − 1 rounds, everyone behind k also gets a turn. In the final round, k buys their last ticket before the people behind k get their turn, so the time stops.

**Q2. Why simulate first?**
> It's obviously correct and easy to write. It then serves as a test oracle for the clever formula.

### Follow-up questions

**F1. The order in which people finish.**
> Simulate, recording each person when their count reaches 0. Or sort by (rounds needed, position).

**F2. Each person buys up to m tickets per turn.**
> The rounds for person i = ⌈tickets[i] / m⌉; the same ahead/behind reasoning with ceilings.

**F3. Round-robin CPU scheduling with a time quantum.**
> The same simulation with a queue of processes, each running for at most q units per turn.

---

## 9. Running the file

```bash
java E04_TimeNeededToBuyTickets.java      # 4/4 tests passed (including 500 random lines)
```

**One-line summary:** *"Simulate the round robin with a queue of indices, or sum min(tickets[i], need) for i ≤ k and min(tickets[i], need − 1) for i > k."*
