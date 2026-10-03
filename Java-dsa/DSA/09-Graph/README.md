# 09 · Graph

A graph is a set of vertices joined by edges, which can be directed or undirected, weighted or unweighted. Trees, grids and linked lists are all special cases. The differences from trees: graphs can have **cycles** (so you need a `visited` set) and **several components** (so you loop over every vertex as a possible start). In interviews you almost always store a graph as an **adjacency list**.

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

## 01-Basics: start here (in this order)

| File | What you learn |
|---|---|
| `GraphRepresentation` | vocabulary; edge list vs **adjacency matrix** vs **adjacency list**; named vertices (`Map<String, List<String>>`); grids as **implicit graphs** |
| `GraphTraversal` | **BFS** (shortest unweighted path + rebuilding the path), **DFS** recursive/iterative, connected components, **cycle detection** (undirected: parent check; directed: **3 colours**), **bipartite** check |
| `TopologicalSort` | **Kahn's algorithm** (in-degrees), DFS reversed postorder, smallest order with a heap, "rounds" = parallel semesters |
| `ShortestPaths` | **Dijkstra**, **Bellman-Ford** (negative edges, negative cycles), **Floyd-Warshall**, **0-1 BFS**: when to use which, all checked against each other on random graphs |
| `MinimumSpanningTree` | **Prim** (heap, and the O(V²) version for dense graphs) and **Kruskal** (sort + mini Union-Find), the cut property |
| `BridgesAndEulerPaths` | **Tarjan's bridges** (disc/low values) and **Hierholzer's Euler path**: the two "advanced DFS" algorithms |

## 02-Easy (5)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Find if Path Exists in Graph | 1971 | Build the adjacency list + BFS (the template) |
| E02 | ⭐ Flood Fill | 733 | Grid DFS (and the same-colour infinite-loop trap) |
| E03 | ⭐ Find the Town Judge | 997 | In-degree − out-degree |
| E04 | ⭐ Find Center of Star Graph | 1791 | Structure: O(1), no traversal |
| E05 | ⭐ Island Perimeter | 463 | 4·cells − 2·shared edges |

## 03-Medium (26)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Number of Islands | 200 | Count components in a grid |
| M02 | Clone Graph | 133 | DFS + old→new map (register before recursing) |
| M03 | Course Schedule | 207 | Cycle detection (Kahn / 3 colours) |
| M04 | Course Schedule II | 210 | Topological order |
| M05 | Pacific Atlantic Water Flow | 417 | **Reverse**: climb from the oceans |
| M06 | Surrounded Regions | 130 | Protect border regions first |
| M07 | Rotting Oranges | 994 | **Multi-source BFS** (levels = minutes) |
| M08 | Network Delay Time | 743 | **Dijkstra**, max of distances |
| M09 | Cheapest Flights Within K Stops | 787 | **Bellman-Ford with k+1 rounds** (relax from a copy) |
| M10 | Graph Valid Tree | 261 | n−1 edges + connected |
| M11 | Number of Connected Components | 323 | DFS starts / Union-Find |
| M12 | Min Cost to Connect All Points | 1584 | **MST**: O(n²) Prim on a complete graph |
| M13 | Shortest Path in Binary Matrix | 1091 | Grid BFS, 8 directions |
| M14 | Path With Minimum Effort | 1631 | **Minimax Dijkstra** (max instead of sum) |
| M15 | Find the City With the Smallest Number of Neighbors | 1334 | **Floyd-Warshall** |
| M16 | Evaluate Division | 399 | Weighted graph, multiply along the path |
| M17 | Is Graph Bipartite? | 785 | 2-colouring, every component |
| M18 | Find Eventual Safe States | 802 | 3 colours / Kahn on the reversed graph |
| M19 | Minimum Height Trees | 310 | **Peel leaves** to the centre |
| M20 | Parallel Courses | 1136 | Kahn level by level = longest path |
| M21 | Number of Provinces | 547 | Components on an adjacency matrix |
| M22 | Time Needed to Inform All Employees | 1376 | Parent array → children → max path |
| M23 | All Paths From Source to Target | 797 | DFS + backtracking on a DAG |
| M24 | Keys and Rooms | 841 | Reachability (keys are edges) |
| M25 | Possible Bipartition | 886 | Bipartite check on "dislike" edges |
| M26 | Snakes and Ladders | 909 | BFS over squares + zig-zag index math |

## 04-Hard (8)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Word Ladder | 127 | BFS on an implicit graph + **bidirectional BFS** |
| H02 | Word Ladder II | 126 | BFS with parents per level → DFS back from the end |
| H03 | Alien Dictionary | 269 | Edges from adjacent words → topological sort (+ prefix trap) |
| H04 | Reconstruct Itinerary | 332 | **Euler path** (Hierholzer) with min-heaps |
| H05 | Critical Connections in a Network | 1192 | **Bridges** (Tarjan low-link) |
| H06 | Swim in Rising Water | 778 | Minimax Dijkstra on cell weights |
| H07 | Bus Routes | 815 | BFS over **routes**, not stops |
| H08 | Jump Game IV | 1345 | BFS + clear each value group after one use |

**From `leetcode.md`, solved in 11-Union-Find** (Union-Find is the intended tool): Redundant Connection, Accounts Merge, Most Stones Removed with Same Row or Column, Satisfiability of Equality Equations, Regions Cut by Slashes, Number of Operations to Make Network Connected. Course Schedule III → [Heap H08](../08-Heap/04-Hard/H08_CourseScheduleIII.java) (it's a greedy + heap problem, not a graph one).

## Which algorithm? The cheat sheet

| The question… | Use |
|---|---|
| can A reach B / how many groups? | BFS or DFS (or Union-Find if edges arrive one by one) |
| fewest steps, all moves cost the same | **BFS** (multi-source if there are several starts) |
| cheapest path, non-negative weights | **Dijkstra** |
| weights 0 or 1 | 0-1 BFS |
| negative weights / at most k edges | **Bellman-Ford** |
| distances between all pairs, V ≤ ~400 | **Floyd-Warshall** |
| the path's cost is its worst edge or cell | minimax Dijkstra / binary search + BFS / Union-Find |
| order with prerequisites, or detect a directed cycle | **topological sort** (Kahn) |
| connect everything at minimum total cost | **MST** (Prim / Kruskal) |
| split into two groups with constraints | **bipartite** 2-colouring |
| use every edge exactly once | **Euler path** (Hierholzer) |
| edge whose removal disconnects | **bridges** (Tarjan) |
| it's a grid | implicit graph: 4 or 8 directions, mark cells visited in place |

## Interview questions

1. Adjacency list vs matrix: space, edge lookup, iterating neighbours. When is a matrix better?
2. Why must BFS mark a vertex visited when it's **enqueued**, not when it's dequeued?
3. Why does Dijkstra fail with negative edges, and what do you use instead?
4. How do you detect a cycle in a directed vs an undirected graph? Why isn't a plain `visited` enough for directed graphs?
5. Kahn's algorithm vs DFS topological sort: how does each detect a cycle?
6. Prim vs Kruskal: which is better for dense vs sparse graphs?
7. BFS vs DFS: memory use on a wide vs a deep graph. Which finds shortest paths?
