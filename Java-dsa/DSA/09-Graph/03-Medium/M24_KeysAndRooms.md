# M24 · Keys and Rooms

| | |
|---|---|
| **LeetCode** | [#841 Keys and Rooms](https://leetcode.com/problems/keys-and-rooms/) |
| **Difficulty** | Medium |
| **Pattern** | Reachability from one source (the keys **are** the edges) |
| **Code** | [M24_KeysAndRooms.java](M24_KeysAndRooms.java) |
| **Run** | `java M24_KeysAndRooms.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Rooms 0..n−1; only room 0 is unlocked. `rooms[i]` = the keys found in room i. Can you visit every room?

| rooms | answer |
|---|---|
| `[[1],[2],[3],[]]` | true (0 → 1 → 2 → 3) |
| `[[1,3],[3,0,1],[2],[0]]` | false (the key to room 2 is inside room 2) |

---

## 2. How to think about it

### The model
Room i → room k for every key k in room i: a **directed** graph. "Visit every room" = "every vertex is reachable from 0". So one DFS/BFS from 0 with `visited[]`, counting the visited rooms.

### The skill: recognising the graph
No edges are given explicitly, but **"having X lets you reach Y" is always an edge X → Y**.

```
[[1,3],[3,0,1],[2],[0]]
0 → 1, 0 → 3, 1 → 3, 1 → 0, 1 → 1, 2 → 2, 3 → 0
from 0: {0, 1, 3}  — room 2 only has an edge from itself → unreachable
```

---

## 3. Java concepts used in this problem

### 3.1 `List<List<Integer>>` input used directly as the adjacency list

`rooms.get(r)` is the list of r's out-neighbours; nothing needs to be built.

### 3.2 `changed |= open.add(k)` (in the referee)

`|=` ORs the right side into the boolean: `changed` becomes true as soon as any `add` returns true (a newly opened room). Note that `|=` on booleans does **not** short-circuit, so every `add` still runs.

### 3.3 Iterating over a copy while modifying

```java
for (int r : new ArrayList<>(open)) for (int k : rooms.get(r)) changed |= open.add(k);
```

Adding to a HashSet while iterating over it would throw `ConcurrentModificationException`, so the referee iterates over a snapshot.

---

## 4. Approach 1: Repeat until nothing changes (the referee)

### Code ([M24_KeysAndRooms.java:44-52](M24_KeysAndRooms.java#L44-L52))

```java
static boolean fixpoint(List<List<Integer>> rooms) {
    Set<Integer> open = new HashSet<>(List.of(0));
    boolean changed = true;
    while (changed) {
        changed = false;
        for (int r : new ArrayList<>(open)) for (int k : rooms.get(r)) changed |= open.add(k);
    }
    return open.size() == rooms.size();
}
```

Repeatedly open every room you have a key for, until a full pass opens nothing new (a "fixpoint"). Up to n passes of O(keys): **O(n · keys)**.

---

## 5. Approach 2: One DFS from room 0 (the file's main solution)

### Code ([M24_KeysAndRooms.java:30-41](M24_KeysAndRooms.java#L30-L41))

```java
static boolean canVisitAllRooms(List<List<Integer>> rooms) {
    boolean[] visited = new boolean[rooms.size()];
    Deque<Integer> stack = new ArrayDeque<>(List.of(0));
    visited[0] = true;
    int count = 1;
    while (!stack.isEmpty()) {
        for (int key : rooms.get(stack.pop())) {
            if (!visited[key]) { visited[key] = true; count++; stack.push(key); }
        }
    }
    return count == rooms.size();
}
```

### Line by line

| Line | What it does |
|---|---|
| `visited[0] = true; count = 1` | room 0 is open |
| `rooms.get(stack.pop())` | the keys in the room just entered |
| `!visited[key]` | a room we can now open for the first time |
| `count == rooms.size()` | every room was reached |

### Dry run: `[[1,3],[3,0,1],[2],[0]]`

| pop | keys | newly opened | stack after | count |
|---|---|---|---|---|
| 0 | 1, 3 | 1, 3 | [3, 1] | 3 |
| 3 | 0 | — | [1] | 3 |
| 1 | 3, 0, 1 | — | [] | 3 |

3 ≠ 4 → **false** ✓

### Complexity
- **Time O(rooms + keys).**
- **Space O(rooms).**

---

## 6. Comparing the approaches

| | Fixpoint | DFS |
|---|---|---|
| Time | O(n · keys) | **O(n + keys)** |
| Space | O(n) | O(n) |

The file checks the DFS against the fixpoint on 1,000 random buildings.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single room (true), keys to already-open rooms, a key to a room's own lock, duplicate keys.

**Mistakes:**
1. Treating the graph as undirected (a key in room 3 for room 0 doesn't open room 3 from room 0).
2. Forgetting to count room 0.
3. No visited array (an infinite loop on cycles of keys).

---

## 8. Interview questions and answers

**Q1. Why is this a graph problem?**
> Each key is a directed edge "from the room where it lies to the room it opens". Visiting all rooms is reachability of every vertex from vertex 0.

**Q2. Which rooms are unreachable?**
> Those still `false` in `visited` after the search.

### Follow-up questions

**F1. Minimum keys to add so that every room is reachable.**
> Count the unreachable "source" components (strongly connected components with no incoming edges from outside).

**F2. Find if Path Exists.** → [E01](../02-Easy/E01_FindIfPathExists.md)
> The same reachability, undirected.

**F3. Course Schedule.** → [M03](M03_CourseSchedule.md)
> Another word problem hiding a directed graph.

---

## 9. Running the file

```bash
java M24_KeysAndRooms.java      # 3/3 tests passed (including 1,000 random buildings vs fixpoint)
```

**One-line summary:** *"The keys are directed edges; DFS from room 0 counting the newly opened rooms, and check that the count equals n."*
