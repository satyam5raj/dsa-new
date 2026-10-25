# M04 · Dota2 Senate ⭐

| | |
|---|---|
| **LeetCode** | [#649 Dota2 Senate](https://leetcode.com/problems/dota2-senate/) |
| **Difficulty** | Medium |
| **Pattern** | Two queues of turn order (a greedy round-robin simulation) |
| **Code** | [M04_Dota2Senate.java](M04_Dota2Senate.java) |
| **Run** | `java M04_Dota2Senate.java` |

⭐ = a bonus classic.

---

## 1. The problem

Senators from two parties, **Radiant (`R`)** and **Dire (`D`)**, act in order, round after round. On their turn, a senator may **ban** one opposing senator (who loses all future turns). (They could also announce victory if only their party is left.) Every senator plays optimally. Which party wins?

| senate | winner | why |
|---|---|---|
| `"RD"` | Radiant | R acts first and bans D |
| `"RDD"` | Dire | R bans the first D; the second D bans R |
| `"DDRRR"` | Dire | |
| `"RRDDD"` | Radiant | |
| `"R"` | Radiant | |

**Constraints:** `1 ≤ n ≤ 10⁴`.

---

## 2. How to think about it

### The greedy choice
On your turn, ban the opponent **who would act next** (the earliest upcoming opponent). That's the one about to hurt your party soonest; banning anyone later would let that one act first and ban one of yours.

### Two queues of turn indices
Keep, for each party, a queue of the **turn indices** of its living senators.
- Compare the two fronts: the **smaller index acts first**.
- The one who acts **bans the other front** (poll it, gone forever).
- The one who acts goes to the **back** of its own queue with index **+ n**, because it acts again in the next round.
- When one queue is empty, the other party wins.

### Why + n?
It keeps the global order correct: everyone in round 1 (indices < n) acts before anyone's round 2 (indices ≥ n), and round 2 keeps the original relative order.

---

## 3. Java concepts used in this problem

### 3.1 Two queues as competing timelines

```java
Deque<Integer> radiant = new ArrayDeque<>(), dire = new ArrayDeque<>();
int r = radiant.poll(), d = dire.poll();
if (r < d) radiant.offer(r + n); else dire.offer(d + n);
```

Both fronts are polled; only the winner of the comparison is re-offered. The loser simply isn't put back, which is the "ban".

### 3.2 Encoding "the next round" in the value

Instead of tracking round numbers separately, adding n to the index makes later rounds naturally compare as later. It's a handy trick for round-robin processes.

---

## 4. Approach 1: Direct simulation with ban counters

```java
static String predictWithCounters(String senate) {
    char[] s = senate.toCharArray();
    boolean[] banned = new boolean[s.length];
    int pendingBanR = 0, pendingBanD = 0, aliveR = 0, aliveD = 0;
    for (char c : s) if (c == 'R') aliveR++; else aliveD++;
    while (aliveR > 0 && aliveD > 0) {
        for (int i = 0; i < s.length && aliveR > 0 && aliveD > 0; i++) {
            if (banned[i]) continue;
            if (s[i] == 'R') {
                if (pendingBanR > 0) { pendingBanR--; banned[i] = true; aliveR--; }   // this R was banned earlier
                else { pendingBanD++; }                                              // R bans the next D to act
            } else {
                if (pendingBanD > 0) { pendingBanD--; banned[i] = true; aliveD--; }
                else { pendingBanR++; }
            }
        }
    }
    return aliveR > 0 ? "Radiant" : "Dire";
}
```

"Pending bans" are applied to the next senator of that party who reaches their turn, which is exactly the greedy rule. It's O(n · rounds), O(n²) in the worst case.

---

## 5. Approach 2: Two queues (the file's solution)

### Code ([M04_Dota2Senate.java:39-51](M04_Dota2Senate.java#L39-L51))

```java
static String predictPartyVictory(String senate) {
    int n = senate.length();
    Deque<Integer> radiant = new ArrayDeque<>(), dire = new ArrayDeque<>();
    for (int i = 0; i < n; i++) {
        if (senate.charAt(i) == 'R') radiant.offer(i); else dire.offer(i);
    }
    while (!radiant.isEmpty() && !dire.isEmpty()) {
        int r = radiant.poll(), d = dire.poll();
        if (r < d) radiant.offer(r + n);    // R acts first: bans d, and comes back next round
        else dire.offer(d + n);             // D acts first: bans r
    }
    return radiant.isEmpty() ? "Dire" : "Radiant";
}
```

### Line by line

| Line | What it does |
|---|---|
| the first loop | each party's senators, in turn order |
| `while (both non-empty)` | the fight continues |
| `int r = radiant.poll(), d = dire.poll();` | the next senator of each party |
| `if (r < d) radiant.offer(r + n);` | R acts first: D (just polled) is banned; R returns for the next round |
| `else dire.offer(d + n);` | D acts first: R is banned |
| the return | whoever still has senators |

### Dry run 1: `"RDD"` (n = 3)

| step | R queue | D queue | compare | result |
|---|---|---|---|---|
| start | [0] | [1, 2] | | |
| 1 | [3] | [2] | R0 < D1 → R bans D1, returns as 3 | |
| 2 | [] | [5] | D2 < R3 → D bans R3, returns as 5 | |
| — | empty | | | **"Dire"** |

### Dry run 2: `"DDRRR"` (n = 5)

| step | R | D | compare |
|---|---|---|---|
| start | [2, 3, 4] | [0, 1] | |
| 1 | [3, 4] | [1, 5] | D0 < R2 → bans R2 |
| 2 | [4] | [5, 6] | D1 < R3 → bans R3 |
| 3 | [9] | [6] | R4 < D5 → bans D5 |
| 4 | [] | [11] | D6 < R9 → bans R9 |

**"Dire"** ✓

### Complexity
- **Time O(n):** each comparison removes one senator for good.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Rounds with pending bans | Two queues |
|---|---|---|
| Time | O(n²) worst case | **O(n)** |
| Space | O(n) | O(n) |

---

## 7. Edge cases and common mistakes

**Edge cases:** a single senator, one party only, alternating `RDRD…`, a long run of one party followed by the other.

**Mistakes:**
1. Banning a random opponent or the last one (not optimal).
2. Re-offering with the same index (without + n), which breaks the round order.
3. Treating it as a simple count comparison. The order matters: `"DDRRR"` has 3 R and only 2 D, yet Dire wins because the Ds act first.

---

## 8. Interview questions and answers

**Q1. Why ban the next opponent to act?**
> Every opponent will eventually ban one of yours when their turn comes. Removing the one who acts soonest prevents the earliest damage; any other choice lets that opponent act first. An exchange argument shows this is never worse.

**Q2. Why does + n preserve the order?**
> All the round-1 indices are below n, and all the round-2 indices are at least n, in the same relative order. So comparing the indices compares "who acts first" correctly, across rounds too.

### Follow-up questions

**F1. More than two parties.**
> You'd need to decide whom to ban (the next acting senator of **any** other party is the natural greedy choice): a priority queue of the turn indices of all the senators.

**F2. Each senator can ban k opponents per turn.**
> Ban the k earliest upcoming opponents: poll k from the other queue.

**F3. Round-robin simulations in general.** → [E04 Time Needed to Buy Tickets](../02-Easy/E04_TimeNeededToBuyTickets.md)

---

## 9. Running the file

```bash
java M04_Dota2Senate.java      # 5/5 tests passed
```

**One-line summary:** *"A queue of turn indices per party; the smaller front acts, bans the other front, and re-queues itself with index + n."*
