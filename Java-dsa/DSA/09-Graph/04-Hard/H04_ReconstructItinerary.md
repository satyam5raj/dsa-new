# H04 · Reconstruct Itinerary

| | |
|---|---|
| **LeetCode** | [#332 Reconstruct Itinerary](https://leetcode.com/problems/reconstruct-itinerary/) |
| **Difficulty** | Hard |
| **Pattern** | An Euler path (use every ticket = every edge once) via Hierholzer's algorithm |
| **Code** | [H04_ReconstructItinerary.java](H04_ReconstructItinerary.java) |
| **Run** | `java H04_ReconstructItinerary.java` |

Euler paths are built from scratch in [01-Basics/BridgesAndEulerPaths](../01-Basics/BridgesAndEulerPaths.java). PriorityQueue basics are in [Heap E01](../../08-Heap/02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

`tickets[i] = [from, to]`. Use **all** the tickets exactly once, starting at "JFK". If several itineraries work, return the **lexicographically smallest**.

| tickets | answer |
|---|---|
| MUC→LHR, JFK→MUC, SFO→SJC, LHR→SFO | [JFK, MUC, LHR, SFO, SJC] |
| JFK→SFO, JFK→ATL, SFO→ATL, ATL→JFK, ATL→SFO | [JFK, ATL, JFK, SFO, ATL, SFO] |
| JFK→KUL, JFK→NRT, NRT→JFK | [JFK, NRT, JFK, KUL] |

A valid itinerary is guaranteed to exist.

---

## 2. How to think about it

### Every edge exactly once = an Euler path

### The naive greedy fails
Always flying to the smallest airport can lead into a **dead end** before all the tickets are used:

```
JFK → KUL (dead end),  JFK → NRT → JFK
greedy takes KUL first (K < N) and gets stuck with tickets left over
```

Backtracking fixes it, but it can be exponential.

### Hierholzer's algorithm (O(E log E))
```
keep each airport's destinations in a MIN-HEAP (smallest first)
dfs(a):  while a has unused tickets:  dfs(poll the smallest destination)
         then PREPEND a to the route        (postorder)
```

A dead end **finishes first**, so it lands at the **end** of the route; the remaining loop gets spliced in **before** it. Greedy-smallest + postorder = the smallest valid itinerary.

### Dry run: the dead-end trap
```
dfs(JFK): take KUL → dfs(KUL): no tickets → route [KUL]
          take NRT → dfs(NRT): take JFK → dfs(JFK): no tickets left → [JFK, KUL]
                     NRT done → [NRT, JFK, KUL]
          JFK done → [JFK, NRT, JFK, KUL]
```

---

## 3. Java concepts used in this problem

### 3.1 `Map<String, PriorityQueue<String>>`

```java
flights.computeIfAbsent(t.get(0), k -> new PriorityQueue<>()).add(t.get(1));
```

Each airport's outgoing tickets are kept in a min-heap of Strings, ordered **lexicographically** (Strings' natural order). `poll()` uses up the smallest ticket.

### 3.2 Postorder with `addFirst`

```java
route.addFirst(airport);          // postorder, prepended
```

An airport is added only after all its outgoing tickets have been used, and prepending turns the reverse finishing order into the forward route. A `LinkedList` makes `addFirst` O(1).

### 3.3 Skipping duplicate tickets in backtracking (the referee)

```java
if (i > 0 && !used[i - 1] && t.get(i).equals(t.get(i - 1))) continue;
```

With the tickets sorted, identical tickets are adjacent; trying both copies at the same step would explore the same itinerary twice. The usual "skip duplicates at the same level" trick from backtracking.

---

## 4. Approach 1: Backtracking in sorted order (the referee)

### Code ([H04_ReconstructItinerary.java:58-78](H04_ReconstructItinerary.java#L58-L78))

```java
static List<String> backtracking(List<List<String>> tickets) {
    List<List<String>> sorted = new ArrayList<>(tickets);
    sorted.sort(Comparator.comparing((List<String> t) -> t.get(1)));
    List<String> route = new ArrayList<>(List.of("JFK"));
    return bt(sorted, new boolean[sorted.size()], route) ? route : null;
}

static boolean bt(List<List<String>> t, boolean[] used, List<String> route) {
    if (route.size() == t.size() + 1) return true;
    String at = route.get(route.size() - 1);
    for (int i = 0; i < t.size(); i++) {
        if (used[i] || !t.get(i).get(0).equals(at)) continue;
        if (i > 0 && !used[i - 1] && t.get(i).equals(t.get(i - 1))) continue;   // skip duplicate tickets
        used[i] = true;
        route.add(t.get(i).get(1));
        if (bt(t, used, route)) return true;
        route.remove(route.size() - 1);
        used[i] = false;
    }
    return false;
}
```

| Line | What it does |
|---|---|
| sort by destination | try the smallest destinations first |
| `route.size() == t.size() + 1` | every ticket used → done |
| the loop | each unused ticket leaving the current airport |
| choose / recurse / un-choose | standard backtracking |

The **first** complete route found is the smallest (the destinations were tried in order). It's exponential in the worst case.

---

## 5. Approach 2: Hierholzer (the file's main solution)

### Code ([H04_ReconstructItinerary.java:43-55](H04_ReconstructItinerary.java#L43-L55))

```java
static List<String> findItinerary(List<List<String>> tickets) {
    Map<String, PriorityQueue<String>> flights = new HashMap<>();
    for (List<String> t : tickets) flights.computeIfAbsent(t.get(0), k -> new PriorityQueue<>()).add(t.get(1));
    LinkedList<String> route = new LinkedList<>();
    visit("JFK", flights, route);
    return route;
}

static void visit(String airport, Map<String, PriorityQueue<String>> flights, LinkedList<String> route) {
    PriorityQueue<String> dests = flights.get(airport);
    while (dests != null && !dests.isEmpty()) visit(dests.poll(), flights, route);   // use each ticket once
    route.addFirst(airport);                                                         // postorder, prepended
}
```

### Line by line

| Line | What it does |
|---|---|
| the map of heaps | each airport's remaining tickets, smallest destination first |
| `dests.poll()` | use up the smallest remaining ticket |
| `visit(...)` | fly there and keep going |
| the `while` | come back and use any remaining tickets from this airport (that's what splices in the loops) |
| `addFirst` | this airport is finished: it goes in front of everything that comes after it |

### Dry run: example 2 (from the real run)

```
visit JFK [ATL, SFO]
  fly JFK→ATL → visit ATL [JFK, SFO]
    fly ATL→JFK → visit JFK [SFO]
      fly JFK→SFO → visit SFO [ATL]
        fly SFO→ATL → visit ATL [SFO]
          fly ATL→SFO → visit SFO []      done → [SFO]
        done ATL → [ATL, SFO]
      done SFO → [SFO, ATL, SFO]
    done JFK → [JFK, SFO, ATL, SFO]
  done ATL → [ATL, JFK, SFO, ATL, SFO]
done JFK → [JFK, ATL, JFK, SFO, ATL, SFO] ✓
```

### Complexity
- **Time O(E log E)** (each ticket goes into and out of a heap once).
- **Space O(E).**

---

## 6. Comparing the approaches

| | Backtracking | Hierholzer |
|---|---|---|
| Time | exponential worst case | **O(E log E)** |
| Space | O(E) | O(E) |
| Dead ends | backtracks out of them | splices them in automatically |

The file checks Hierholzer against backtracking on 500 random ticket sets (each generated from a random walk, so a valid itinerary always exists).

---

## 7. Edge cases and common mistakes

**Edge cases:** duplicate tickets, a single ticket, dead ends, an airport visited many times.

**Mistakes:**
1. Greedy "always take the smallest" without postorder (gets stuck in a dead end).
2. Appending in preorder (that gives the wrong route when there are dead ends).
3. A sorted list with `remove(0)` per ticket (O(E) each).
4. Forgetting that an airport with no outgoing tickets has no map entry (`dests == null`).

---

## 8. Interview questions and answers

**Q1. Why does postorder fix the dead-end problem?**
> A dead end is where the path must end, since no tickets leave it. Postorder records it first, so after reversing, it ends up last, and any cycle explored afterwards from earlier airports is inserted before it.

**Q2. Why is the result lexicographically smallest?**
> At every airport, the smallest destination is explored first, and Hierholzer only defers a choice when it leads to the forced end. So the earliest positions of the route get the smallest possible airports.

### Follow-up questions

**F1. Valid Arrangement of Pairs (LC 2097).**
> An Euler path where the start is the node with out-degree − in-degree = 1 (or any node if they're balanced).

**F2. Cracking the Safe (LC 753).**
> An Euler circuit on a de Bruijn graph.

**F3. Does an Euler path exist?**
> In a directed graph: at most one node with out − in = 1, at most one with in − out = 1, all others balanced, and the edges connected.

---

## 9. Running the file

```bash
java H04_ReconstructItinerary.java      # 4/4 tests passed (including 500 random ticket sets vs backtracking)
```

**One-line summary:** *"Hierholzer: keep each airport's destinations in a min-heap, DFS by always flying to the smallest remaining one, and prepend each airport when it runs out of tickets (postorder)."*
