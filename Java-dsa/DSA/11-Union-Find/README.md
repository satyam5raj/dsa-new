# 11 · Union-Find (Disjoint Set Union)

Union-Find keeps elements split into **disjoint groups**. It supports `union(a, b)` to merge two groups and `find(x)` to get a group's representative. With **union by size** and **path compression**, each operation costs O(α(n)), which is effectively constant. Reach for it when connections **arrive over time**, when you're grouping **equivalent** things, for Kruskal's MST, and for cycle detection while adding undirected edges. It cannot split groups or find paths.

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

**Study notes:** every problem file has a companion `.md` with the same name (e.g. `02-Easy/E01_FindIfPathExistsUnionFind.md`). Each note covers the problem, how to think about it, the Java concepts it uses (E01 starts with the parent array, find, union by size, path halving and why it's nearly O(1)), a line-by-line walkthrough of every approach, dry-run tables, edge cases, and interview questions with follow-ups.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `UnionFindBasics` | the forest representation; naive union vs **union by size** vs **+ path compression**, **measured** (chain: 134 million hops → 16k; doubling build: depth capped at log₂n); component counting; checked against BFS after every single edge |
| `UnionFindVariants` | DSU over **strings** (HashMaps), **weighted DSU** (ratios, e.g. a/b = 2), grid ids and virtual nodes, **offline** sorting |

## 02-Easy (1)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | ⭐ Find if Path Exists (Union-Find way) | 1971 | Union every edge, compare roots (the template) |

## 03-Medium (7)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Redundant Connection | 684 | The first edge whose ends are already connected |
| M02 | Accounts Merge | 721 | DSU over **emails** |
| M03 | Most Stones Removed | 947 | stones − components; union **rows with columns** |
| M04 | Satisfiability of Equality Equations | 990 | All `==` first, then check `!=` |
| M05 | Regions Cut by Slashes | 959 | Each cell → **4 triangles** |
| M06 | Number of Operations to Make Network Connected | 1319 | components − 1 (after the n−1 cable check) |
| M07 | ⭐ Lexicographically Smallest Equivalent String | 1061 | Make the **smallest** letter the root |

## 04-Hard (4)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | ⭐ Number of Islands II | 305 | **Online**: +1 per land cell, −1 per successful union |
| H02 | ⭐ Redundant Connection II | 685 | Directed: two-parent candidates + cycle check |
| H03 | ⭐ Edge Length Limited Paths | 1697 | **Offline**: sort edges and queries, the DSU only grows |
| H04 | ⭐ Remove Max Edges to Keep Graph Traversable | 1579 | **Two DSUs**, shared edges first |

**Solved elsewhere with Union-Find as an alternative:** Number of Connected Components → [Graph M11](../09-Graph/03-Medium/M11_NumberOfConnectedComponents.java), Number of Provinces → [Graph M21](../09-Graph/03-Medium/M21_NumberOfProvinces.java), Kruskal's MST → [Graph Basics MinimumSpanningTree](../09-Graph/01-Basics/MinimumSpanningTree.java) and [Graph M12](../09-Graph/03-Medium/M12_MinCostToConnectAllPoints.java), Evaluate Division (graph DFS there, weighted DSU in `UnionFindVariants`) → [Graph M16](../09-Graph/03-Medium/M16_EvaluateDivision.java).

## Union-Find or BFS/DFS?

| Situation | Use |
|---|---|
| edges/cells arrive one by one, answer after each | **Union-Find** |
| "are x and y in the same group?" asked many times | **Union-Find** |
| group equivalent items (emails, synonyms, equations) | **Union-Find** |
| queries with a growing threshold (offline) | **sort + Union-Find** |
| need the path itself, its length, or directed reachability | BFS / DFS |
| edges get **removed** over time | reverse time (process backwards) or a different structure |

## Interview questions

1. What do union by size/rank and path compression each guarantee alone, and together?
2. What is α(n), and why do people call Union-Find "O(1)"?
3. How do you detect a cycle in an undirected graph with Union-Find? Why doesn't this work for directed graphs?
4. How would you support elements that are strings or coordinates?
5. Why can't Union-Find delete an edge, and how do offline / reverse-time tricks work around that?
6. Kruskal vs Prim: where does Union-Find come in?
