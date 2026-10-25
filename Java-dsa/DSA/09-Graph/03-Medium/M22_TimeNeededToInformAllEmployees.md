# M22 · Time Needed to Inform All Employees

| | |
|---|---|
| **LeetCode** | [#1376 Time Needed to Inform All Employees](https://leetcode.com/problems/time-needed-to-inform-all-employees/) |
| **Difficulty** | Medium |
| **Pattern** | A tree given as a parent array → build the children lists, DFS for the max root-to-leaf time |
| **Code** | [M22_TimeNeededToInformAllEmployees.java](M22_TimeNeededToInformAllEmployees.java) |
| **Run** | `java M22_TimeNeededToInformAllEmployees.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

There are n employees; `manager[i]` is i's boss (the head has −1). Employee i needs `informTime[i]` minutes to tell **all** their direct subordinates (simultaneously). How many minutes until everyone knows the news?

```
n = 6, headID = 2, manager [2,2,−1,2,2,2], informTime [0,0,1,0,0,0]  →  1

          2 (1 min)
   ┌──┬──┼──┬──┐
   0  1  3  4  5
```

| example | answer |
|---|---|
| n = 1 | 0 |
| the star above | 1 |
| the chain 6 → 5 → … → 0, informTime [0,6,5,4,3,2,1] | 21 (1 + 2 + 3 + 4 + 5 + 6) |

---

## 2. How to think about it

### The company is a tree rooted at the head
The news reaches employee e after the **sum of informTime** along the path head → … → e's manager. Everyone knows once the **slowest** path finishes:

```
answer = max over the root-to-leaf paths of (the sum of informTime on the path)
```

### Steps
1. The input points **up** (child → manager); build children lists that point **down**.
2. Walk down from the head carrying the arrival time: `arrival[child] = arrival[u] + informTime[u]`; the answer is the max arrival.

### The alternative without building children
For each employee, walk **up** to the head summing the managers' informTimes, with memoisation. Also O(n).

---

## 3. Java concepts used in this problem

### 3.1 Inverting a parent array

```java
for (int i = 0; i < n; i++) if (manager[i] != -1) children.get(manager[i]).add(i);
```

A parent array is a compact tree representation (each node stores its parent). Traversing downward needs the reverse direction: the children lists.

### 3.2 Carrying state on an explicit stack

```java
stack.push(new int[]{headID, 0});                    // {employee, the time they learn the news}
```

Each stack entry stores the node and the time, the same thing a recursive call would hold in its parameters. The iterative version avoids deep recursion on a 10⁵-long chain of managers.

### 3.3 Memoised recursion upward (the walk-up version)

```java
return memo[i] = arrival(manager[i], ...) + informTime[manager[i]];
```

Each employee's arrival time is computed once and reused by everyone below them.

---

## 4. Approach 1: Walk up with memoisation

### Code ([M22_TimeNeededToInformAllEmployees.java:55-67](M22_TimeNeededToInformAllEmployees.java#L55-L67))

```java
static int walkUp(int n, int headID, int[] manager, int[] informTime) {
    int[] memo = new int[n];
    Arrays.fill(memo, -1);
    memo[headID] = 0;
    int best = 0;
    for (int i = 0; i < n; i++) best = Math.max(best, arrival(i, manager, informTime, memo));
    return best;
}

static int arrival(int i, int[] manager, int[] informTime, int[] memo) {
    if (memo[i] >= 0) return memo[i];
    return memo[i] = arrival(manager[i], manager, informTime, memo) + informTime[manager[i]];
}
```

| Line | What it does |
|---|---|
| `memo[headID] = 0` | the head knows at time 0 |
| `arrival(i)` | when i hears: when their manager heard + the manager's informTime |
| the memo | each employee is computed once |

**O(n)** time and space (the recursion can be n deep on a chain).

---

## 5. Approach 2: Down from the head with a stack (the file's main solution)

### Code ([M22_TimeNeededToInformAllEmployees.java:37-52](M22_TimeNeededToInformAllEmployees.java#L37-L52))

```java
static int numOfMinutes(int n, int headID, int[] manager, int[] informTime) {
    List<List<Integer>> children = new ArrayList<>();
    for (int i = 0; i < n; i++) children.add(new ArrayList<>());
    for (int i = 0; i < n; i++) if (manager[i] != -1) children.get(manager[i]).add(i);
    // iterative DFS carrying the arrival time (avoids deep recursion on a long chain)
    int best = 0;
    Deque<int[]> stack = new ArrayDeque<>();
    stack.push(new int[]{headID, 0});                    // {employee, time they learn the news}
    while (!stack.isEmpty()) {
        int[] top = stack.pop();
        int u = top[0], t = top[1];
        best = Math.max(best, t);
        for (int c : children.get(u)) stack.push(new int[]{c, t + informTime[u]});
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `children` | the down-pointing tree |
| `{headID, 0}` | the head learns at time 0 |
| `best = Math.max(best, t)` | the latest arrival so far |
| push the children | they learn `informTime[u]` minutes after u |

No `visited` array is needed: it's a tree, so each employee is reached exactly once.

### Dry run: the chain 6 → 5 → 4 → 3 → 2 → 1 → 0, informTime [0, 6, 5, 4, 3, 2, 1]

| employee | arrival |
|---|---|
| 6 (head) | 0 |
| 5 | 0 + 1 = 1 |
| 4 | 1 + 2 = 3 |
| 3 | 3 + 3 = 6 |
| 2 | 6 + 4 = 10 |
| 1 | 10 + 5 = 15 |
| 0 | 15 + 6 = **21** |

### Complexity
- **Time O(n).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Walk up + memo | Down from the head |
|---|---|---|
| Time | O(n) | O(n) |
| Builds the children lists? | no | yes |
| Deep recursion risk | yes | no (an explicit stack) |

The file checks both on 500 random companies.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single employee (0), a star (the head's time), a long chain (the sum).

**Mistakes:**
1. Summing **all** the informTimes (it's the max over paths, not the total).
2. Adding the employee's **own** informTime to their arrival (only the managers' times count).
3. Recursing on a 10⁵-deep chain.

---

## 8. Interview questions and answers

**Q1. Why the maximum over paths?**
> Different branches spread in parallel; the last employee to hear is at the end of the slowest root-to-leaf path.

**Q2. Up or down?**
> Down needs the children lists but is a single traversal; up uses the parent array directly, with memoisation.

### Follow-up questions

**F1. Each subordinate is told one at a time (sequentially).**
> Then the order matters: sort the children by their subtree times, descending (a greedy), like the "minimum time to broadcast" problems.

**F2. Return who hears last.**
> Track the employee with the maximum arrival.

**F3. Network Delay Time.** → [M08](M08_NetworkDelayTime.md)
> The same "the max over shortest arrival times", on a general weighted graph with Dijkstra.

---

## 9. Running the file

```bash
java M22_TimeNeededToInformAllEmployees.java      # 4/4 tests passed (including 500 random companies: DFS == walk-up)
```

**One-line summary:** *"Turn the manager array into children lists, then walk down from the head with arrival[child] = arrival[u] + informTime[u]; the answer is the latest arrival."*
