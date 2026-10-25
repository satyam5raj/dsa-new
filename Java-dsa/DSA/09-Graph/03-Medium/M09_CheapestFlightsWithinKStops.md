# M09 · Cheapest Flights Within K Stops

| | |
|---|---|
| **LeetCode** | [#787 Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/) |
| **Difficulty** | Medium |
| **Pattern** | Bellman-Ford limited to k + 1 rounds (edges), relaxing from a **copy** |
| **Code** | [M09_CheapestFlightsWithinKStops.java](M09_CheapestFlightsWithinKStops.java) |
| **Run** | `java M09_CheapestFlightsWithinKStops.java` |

Dijkstra and Bellman-Ford are introduced in [M08 Network Delay Time](M08_NetworkDelayTime.md).

---

## 1. The problem

`flights[i] = [from, to, price]`. Find the cheapest price from src to dst using **at most k stops** (= at most k + 1 flights), or −1.

```
n = 4, flights [[0,1,100],[1,2,100],[2,0,100],[1,3,600],[2,3,200]], 0 → 3, k = 1  →  700

0 ─100→ 1 ─100→ 2 ─200→ 3        0 → 1 → 2 → 3 costs 400 but has 2 stops (too many)
        └──────600──────→ 3      0 → 1 → 3 costs 700 with 1 stop
```

| k | answer |
|---|---|
| 1 | 700 |
| 2 | 400 (the 2-stop route is allowed) |

---

## 2. How to think about it

### Why plain Dijkstra fails
Dijkstra keeps only the **cheapest** way to reach each city. But a pricier route with **fewer stops** may be the only one that can still continue within the limit. (City 2 is cheapest via 0 → 1 → 2, but that already uses 2 flights.)

### Bellman-Ford with a round limit
After round i, `cost[v]` = the cheapest way to reach v using **at most i flights**. Run exactly k + 1 rounds.

**The catch:** within one round, relax from the **previous** round's values (a copy). Otherwise an edge relaxed earlier in the same round could be chained with another one, and two flights would be counted as one round.

```
for round in 1..k+1:
    next = copy of cost
    for each flight (u, v, p):  if cost[u] is finite:  next[v] = min(next[v], cost[u] + p)
    cost = next
```

### Dry run: k = 1 (2 rounds), src 0

| after | cost (0, 1, 2, 3) | |
|---|---|---|
| start | [0, ∞, ∞, ∞] | |
| round 1 (from start) | [0, 100, ∞, ∞] | 1 = 100 |
| round 2 (from round 1) | [0, 100, 200, 700] | 2 = 200, 3 = 700 (via 1); 0 stays 0 |

**700**: the 400 route would need round 3.

### Alternative
BFS (or Dijkstra) over the states (city, flights used) also works. The BFS-by-stops version is shown below.

---

## 3. Java concepts used in this problem

### 3.1 `cost.clone()` for a round snapshot

```java
int[] next = cost.clone();                         // relax from the PREVIOUS round only
```

Reading from `cost` and writing into `next` keeps the rounds separate. `clone()` on an `int[]` makes a full copy.

### 3.2 Guarding against `MAX_VALUE + price`

```java
if (cost[f[0]] != INF && cost[f[0]] + f[2] < next[f[1]]) ...
```

Without the `!= INF` check, `Integer.MAX_VALUE + price` would overflow to a negative number and look "cheap".

### 3.3 `int[] best = {Integer.MAX_VALUE}` in the referee

A one-element array is a mutable box that the recursive DFS can update (an `int` parameter would be a copy).

---

## 4. Approach 1: DFS over every route (the referee)

### Code ([M09_CheapestFlightsWithinKStops.java:61-71](M09_CheapestFlightsWithinKStops.java#L61-L71))

```java
static int brute(int n, int[][] flights, int src, int dst, int k) {
    int[] best = {Integer.MAX_VALUE};
    dfs(flights, src, dst, k + 1, 0, best);
    return best[0] == Integer.MAX_VALUE ? -1 : best[0];
}

static void dfs(int[][] flights, int u, int dst, int flightsLeft, int spent, int[] best) {
    if (u == dst) best[0] = Math.min(best[0], spent);
    if (flightsLeft == 0) return;
    for (int[] f : flights) if (f[0] == u) dfs(flights, f[1], dst, flightsLeft - 1, spent + f[2], best);
}
```

| Line | What it does |
|---|---|
| `flightsLeft = k + 1` | the budget of flights |
| `u == dst` | record the cost (but keep exploring: revisiting dst later might be cheaper) |
| the loop | take every outgoing flight |

Exponential: only for tiny tests.

---

## 5. Approach 2: BFS by stops

```java
static int bfsStops(int n, int[][] flights, int src, int dst, int k) {
    List<List<int[]>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] f : flights) adj.get(f[0]).add(new int[]{f[1], f[2]});
    int[] best = new int[n];
    Arrays.fill(best, Integer.MAX_VALUE);
    best[src] = 0;
    Queue<int[]> q = new ArrayDeque<>();
    q.add(new int[]{src, 0});                                   // {city, cost so far}
    for (int stops = 0; stops <= k && !q.isEmpty(); stops++) {  // each level = one more flight
        for (int i = q.size(); i > 0; i--) {
            int[] cur = q.poll();
            for (int[] e : adj.get(cur[0])) {
                int c = cur[1] + e[1];
                if (c < best[e[0]]) { best[e[0]] = c; q.add(new int[]{e[0], c}); }
            }
        }
    }
    return best[dst] == Integer.MAX_VALUE ? -1 : best[dst];
}
```

Each BFS level adds one flight; a state is only queued if it improves the best known cost to that city. That's at most k + 1 levels. It's equivalent to the round-limited Bellman-Ford (stress-tested against it on 5,000 random cases).

---

## 6. Approach 3: Round-limited Bellman-Ford (the file's main solution)

### Code ([M09_CheapestFlightsWithinKStops.java:45-58](M09_CheapestFlightsWithinKStops.java#L45-L58))

```java
static int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
    int INF = Integer.MAX_VALUE;
    int[] cost = new int[n];
    Arrays.fill(cost, INF);
    cost[src] = 0;
    for (int round = 0; round <= k; round++) {             // k stops = k + 1 flights
        int[] next = cost.clone();                         // relax from the PREVIOUS round only
        for (int[] f : flights) {
            if (cost[f[0]] != INF && cost[f[0]] + f[2] < next[f[1]]) next[f[1]] = cost[f[0]] + f[2];
        }
        cost = next;
    }
    return cost[dst] == INF ? -1 : cost[dst];
}
```

### Line by line

| Line | What it does |
|---|---|
| `round = 0..k` | k + 1 rounds = at most k + 1 flights |
| `next = cost.clone()` | start from the previous round's best (staying put is allowed) |
| the relaxation | extend every reachable city's best route by one flight |
| `cost = next` | the end of the round |
| the return | −1 if dst was never reached within the limit |

### Dry run: `[[0,1,100],[1,2,100],[0,2,500]]`, 0 → 2

| k | rounds | cost[2] |
|---|---|---|
| 0 | 1 (direct flights only) | 500 |
| 1 | 2 | min(500, 100 + 100) = **200** |

### Complexity
- **Time O(k · E).**
- **Space O(n).**

---

## 7. Comparing the approaches

| | All routes | BFS by stops | Bellman-Ford, k + 1 rounds |
|---|---|---|---|
| Time | exponential | O(k · E) | O(k · E) |
| Space | O(k) recursion | O(n + E) | O(n) |

The file checks Bellman-Ford against all routes on 1,000 random cases.

---

## 8. Edge cases and common mistakes

**Edge cases:** src == dst (0), k = 0 (direct flights only), cycles in the flight graph, unreachable dst.

**Mistakes:**
1. Relaxing in place (without the copy), which allows too many flights per round.
2. Off-by-one: k stops means k + 1 flights (rounds).
3. Plain Dijkstra keyed only by city.
4. Overflow when adding to infinity.

---

## 9. Interview questions and answers

**Q1. Why does the in-place update break the stop limit?**
> If u → v is relaxed and then v → w in the same round, w gets a route with two more flights after just one round, exceeding the limit.

**Q2. Can Dijkstra be adapted?**
> Yes, over the states (city, stops used): pop by cost, and skip a state if it exceeds k stops or if this city was already reached with fewer stops at a lower cost. It's more involved than Bellman-Ford here.

### Follow-up questions

**F1. Return the actual route.**
> Store a parent per (round, city), or reconstruct it from the BFS states.

**F2. Minimise the number of stops first, then the cost.**
> BFS by stops; within the first level that reaches dst, take the minimum cost.

**F3. Network Delay Time.** → [M08](M08_NetworkDelayTime.md)
> No stop limit: plain Dijkstra.

---

## 10. Running the file

```bash
java M09_CheapestFlightsWithinKStops.java      # 5/5 tests passed (including 1,000 random cases vs all routes)
```

**One-line summary:** *"Run Bellman-Ford for exactly k + 1 rounds, each round relaxing every flight from a copy of the previous round's costs, so no round chains two flights."*
