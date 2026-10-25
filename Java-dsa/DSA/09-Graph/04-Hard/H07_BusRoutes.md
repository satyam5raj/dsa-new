# H07 · Bus Routes

| | |
|---|---|
| **LeetCode** | [#815 Bus Routes](https://leetcode.com/problems/bus-routes/) |
| **Difficulty** | Hard |
| **Pattern** | BFS where the "vertices" are **routes** (buses), not stops |
| **Code** | [H07_BusRoutes.java](H07_BusRoutes.java) |
| **Run** | `java H07_BusRoutes.java` |

BFS basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#34-bfs-breadth-first-search-with-a-queue).

---

## 1. The problem

`routes[i]` = the stops bus i loops through. What's the fewest number of **buses** to get from the source stop to the target stop? (−1 if impossible, 0 if source == target.)

```
routes [[1,2,7],[3,6,7]], 1 → 6  →  2     (bus 0 to stop 7, change to bus 1)
```

| routes | source → target | answer |
|---|---|---|
| [[1,2,7],[3,6,7]] | 1 → 6 | 2 |
| [[7,12],[4,5,15],[6],[15,19],[9,12,13]] | 15 → 12 | −1 |
| [[1,2]] | 3 → 3 | 0 |

**Constraints:** up to 500 routes, up to 10⁵ stops per route.

---

## 2. How to think about it

### What do we count?
**Buses**, not stops. Riding bus 0 from stop 1 to stop 7 costs 1, however many stops it passes.

### The naive graph on stops
Connecting every pair of stops on the same route means a route with k stops creates k² edges. With 10⁵ stops per route, that's far too many.

### BFS over routes
```
stopToRoutes: stop → the routes passing through it (a HashMap)
level 1 = all the routes through the source stop
for each route in the current level:
    if it contains the target → answer = level
    for each stop on it: every unvisited route through that stop → the next level
```

Mark **routes** as visited, and stops too, so a busy stop isn't re-scanned. Each route and each stop is processed once → O(the total number of stops on all routes).

### Dry run
```
routes through stop 1: {0}  (level 1)
route 0 = 1, 2, 7: no 6.  Stop 7 → route 1 (level 2)
route 1 = 3, 6, 7: contains 6 → 2
```

---

## 3. Java concepts used in this problem

### 3.1 An inverted index: `stop → routes`

```java
for (int r = 0; r < routes.length; r++) for (int s : routes[r]) stopToRoutes.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
```

The input maps routes → stops; the BFS needs the reverse ("which buses can I board here?"). A HashMap is used because stop numbers can be as large as 10⁶ and are sparse.

### 3.2 Two visited structures

- `boolean[] routeSeen`: never board the same bus twice
- `Set<Integer> stopSeen`: never re-scan the routes of a stop already handled

The second one matters for performance: a hub stop on many routes would otherwise be re-expanded from every route that passes through it.

### 3.3 `stopSeen.add(s)` returning false = "already handled"

The same check-and-mark idiom as in earlier notes.

---

## 4. Approach 1: BFS on stops (the referee)

### Code ([H07_BusRoutes.java:68-82](H07_BusRoutes.java#L68-L82))

```java
static int stopBfs(int[][] routes, int source, int target) {
    if (source == target) return 0;
    Map<Integer, Integer> dist = new HashMap<>(Map.of(source, 0));
    Queue<Integer> q = new ArrayDeque<>(List.of(source));
    while (!q.isEmpty()) {
        int s = q.poll();
        for (int[] route : routes) {
            boolean has = false;
            for (int x : route) if (x == s) { has = true; break; }
            if (!has) continue;
            for (int x : route) if (!dist.containsKey(x)) { dist.put(x, dist.get(s) + 1); q.add(x); }
        }
    }
    return dist.getOrDefault(target, -1);
}
```

| Line | What it does |
|---|---|
| `dist[stop]` | the number of buses needed to reach that stop |
| for each route containing s | one more bus reaches every stop on it |

It scans every route for every stop: **O(stops × total route length)**. It's correct, but slow.

---

## 5. Approach 2: BFS over routes (the file's main solution)

### Code ([H07_BusRoutes.java:40-65](H07_BusRoutes.java#L40-L65))

```java
static int numBusesToDestination(int[][] routes, int source, int target) {
    if (source == target) return 0;
    Map<Integer, List<Integer>> stopToRoutes = new HashMap<>();
    for (int r = 0; r < routes.length; r++) for (int s : routes[r]) stopToRoutes.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
    if (!stopToRoutes.containsKey(source) || !stopToRoutes.containsKey(target)) return -1;

    boolean[] routeSeen = new boolean[routes.length];
    Set<Integer> stopSeen = new HashSet<>(List.of(source));
    Queue<Integer> q = new ArrayDeque<>();
    for (int r : stopToRoutes.get(source)) { routeSeen[r] = true; q.add(r); }
    int buses = 1;
    while (!q.isEmpty()) {
        for (int i = q.size(); i > 0; i--) {
            int r = q.poll();
            for (int s : routes[r]) {
                if (s == target) return buses;
                if (!stopSeen.add(s)) continue;               // this stop's routes were already queued
                for (int nr : stopToRoutes.get(s)) {
                    if (!routeSeen[nr]) { routeSeen[nr] = true; q.add(nr); }
                }
            }
        }
        buses++;
    }
    return -1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `source == target` | no bus needed |
| `stopToRoutes` | which buses stop where |
| the missing-stop check | if no bus serves the source or the target → −1 |
| the initial queue | every bus you can board at the source = 1 bus |
| `s == target` | this bus reaches the target → the current number of buses |
| `stopSeen.add(s)` | handle each transfer stop once |
| the inner loop | transfer to every new bus at stop s (the next level = one more bus) |

### Dry run: example 2 (15 → 12)

| level | routes | stops scanned | result |
|---|---|---|---|
| 1 | {1 (4, 5, 15), 3 (15, 19)} | 4, 5, 15, 19 → no new routes | |
| — | queue empty | | **−1** ✓ (12 is only on routes 0 and 4, which never connect) |

### Complexity
- **Time O(Σ route lengths).**
- **Space O(Σ route lengths)** for the index.

---

## 6. Comparing the approaches

| | BFS on stops (scan routes) | BFS over routes |
|---|---|---|
| Time | O(stops × Σ lengths) | **O(Σ lengths)** |
| Space | O(stops) | O(Σ lengths) |

The file checks the route BFS against the stop BFS on 1,000 random networks.

---

## 7. Edge cases and common mistakes

**Edge cases:** source == target (0, even if no bus serves it), the target not on any route, a route containing both the source and the target (1).

**Mistakes:**
1. Counting stops instead of buses.
2. Building the stop-to-stop graph (quadratic per route).
3. Not marking the stops as seen (re-scanning a hub stop many times).
4. Returning −1 for source == target when it isn't on any route (it should be 0).

---

## 8. Interview questions and answers

**Q1. Why make the routes the vertices?**
> The cost is the number of buses, so a "step" is boarding a new route. Two routes are adjacent if they share a stop. That graph has at most 500 vertices, instead of a huge stop graph.

**Q2. What's the role of `stopSeen`?**
> It guarantees each stop's route list is scanned once. Without it, a stop shared by many routes would be expanded once per route, adding up to quadratic work.

### Follow-up questions

**F1. Minimise the number of stops travelled instead.**
> A different graph: stops as vertices, consecutive stops on a route as edges, BFS by stops.

**F2. Each bus has a fare; minimise the cost.**
> Dijkstra over the routes, with each route's fare as the cost of boarding it.

**F3. Multiple sources or targets.**
> Seed the BFS with all the source routes; stop when any target is found.

---

## 9. Running the file

```bash
java H07_BusRoutes.java      # 4/4 tests passed (including 1,000 random networks vs stop-level BFS)
```

**One-line summary:** *"Index stop → routes, then BFS over the routes (each level = one more bus), scanning each route's stops once and each transfer stop once, until a route containing the target is found."*
