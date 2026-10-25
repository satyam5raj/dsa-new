# E03 · Find the Town Judge ⭐

| | |
|---|---|
| **LeetCode** | [#997 Find the Town Judge](https://leetcode.com/problems/find-the-town-judge/) |
| **Difficulty** | Easy |
| **Pattern** | In-degree and out-degree counting (no traversal needed) |
| **Code** | [E03_FindTheTownJudge.java](E03_FindTheTownJudge.java) |
| **Run** | `java E03_FindTheTownJudge.java` |

⭐ = not in `leetcode.md`, added for a key technique. Graph basics are explained in [E01](E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

People are labelled 1..n. `trust[i] = [a, b]` means a trusts b. The **judge** trusts **nobody** and is trusted by **everybody else** (n − 1 people). Return the judge, or −1.

| n | trust | answer |
|---|---|---|
| 2 | [[1,2]] | 2 |
| 3 | [[1,3],[2,3]] | 3 |
| 3 | [[1,3],[2,3],[3,1]] | −1 (3 trusts 1) |
| 1 | [] | 1 (alone, trivially the judge) |

The pairs are distinct, and nobody trusts themselves.

---

## 2. How to think about it

### As a directed graph
Draw an edge a → b for "a trusts b". Then the judge is the vertex with:

```
out-degree 0   AND   in-degree n − 1
```

### One counter is enough
Use `score = in-degree − out-degree`. Only the judge can reach **n − 1**: n − 1 is the maximum possible in-degree (everyone else), and any out-edge lowers the score below it.

```
n = 3, [[1,3],[2,3]]
score: 1 → −1,  2 → −1,  3 → +2 = n − 1  → judge 3
```

### Degree questions
Many graph questions are really **degree** questions, with no traversal needed: the centre of a star graph ([E04](E04_FindCenterOfStarGraph.md)), sources and sinks in a DAG (Kahn's algorithm, [M03](../03-Medium/M03_CourseSchedule.md)), Euler paths.

---

## 3. Java concepts used in this problem

### 3.1 An array indexed by label (1-based)

```java
int[] score = new int[n + 1];          // index 0 unused, people are 1..n
```

Allocating n + 1 slots lets the labels be used directly as indices.

### 3.2 In-degree and out-degree

For a directed edge a → b: a's **out**-degree increases, b's **in**-degree increases. The file merges both into one `score` array.

### 3.3 Building test data with streams

```java
pairs.stream().map(s -> Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new)
```

The test stores the pairs as strings like "1,3" in a `LinkedHashSet` (no duplicates, insertion order kept), then converts each one back into an `int[]`.

---

## 4. Approach 1: Check every person (brute force)

### Code ([E03_FindTheTownJudge.java:43-54](E03_FindTheTownJudge.java#L43-L54))

```java
static int brute(int n, int[][] trust) {
    for (int p = 1; p <= n; p++) {
        boolean trustsNobody = true;
        Set<Integer> trustedBy = new HashSet<>();
        for (int[] t : trust) {
            if (t[0] == p) trustsNobody = false;
            if (t[1] == p) trustedBy.add(t[0]);
        }
        if (trustsNobody && trustedBy.size() == n - 1) return p;
    }
    return -1;
}
```

For each person, scan every trust pair: **O(n · T)** (T = the number of pairs).

---

## 5. Approach 2: Two degree arrays

```java
static int findJudgeTwoArrays(int n, int[][] trust) {
    int[] in = new int[n + 1], out = new int[n + 1];
    for (int[] t : trust) { out[t[0]]++; in[t[1]]++; }
    for (int p = 1; p <= n; p++) if (out[p] == 0 && in[p] == n - 1) return p;
    return -1;
}
```

It states the definition directly: out-degree 0 and in-degree n − 1. **O(n + T)**.

---

## 6. Approach 3: One score = in − out (the file's solution)

### Code ([E03_FindTheTownJudge.java:33-41](E03_FindTheTownJudge.java#L33-L41))

```java
static int findJudge(int n, int[][] trust) {
    int[] score = new int[n + 1];
    for (int[] t : trust) {
        score[t[0]]--;                 // out-edge: trusting someone disqualifies you
        score[t[1]]++;                 // in-edge
    }
    for (int p = 1; p <= n; p++) if (score[p] == n - 1) return p;
    return -1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `score[t[0]]--` | the truster loses a point (an out-edge) |
| `score[t[1]]++` | the trusted person gains one (an in-edge) |
| `score[p] == n − 1` | trusted by all n − 1 others and trusting nobody |

**Why only the judge can score n − 1:** a score of n − 1 needs in-degree n − 1 (the maximum, since nobody trusts themselves and the pairs are distinct) and out-degree 0.

### Dry run: n = 3, [[1,3],[2,3],[3,1]]

| pair | score after (1, 2, 3) |
|---|---|
| 1 → 3 | (−1, 0, +1) |
| 2 → 3 | (−1, −1, +2) |
| 3 → 1 | (0, −1, +1) |

Nobody has 2 → **−1** ✓

### Dry run: n = 1, no pairs
score[1] = 0 = n − 1 → **1** ✓

### Complexity
- **Time O(n + T).**
- **Space O(n).**

---

## 7. Comparing the approaches

| | Check every person | Two arrays | One score |
|---|---|---|---|
| Time | O(n · T) | O(n + T) | O(n + T) |
| Space | O(n) | O(n) | O(n) |

The file checks the score version against the brute force on 1,000 random towns (half with a planted judge).

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1 with no pairs (person 1 is the judge), n = 2, a "judge" who also trusts someone, nobody qualifies.

**Mistakes:**
1. Checking only the in-degree (the judge might trust someone).
2. A 0-based array with 1-based labels (off by one).
3. Returning the first person with in-degree n − 1 without checking the out-degree.

---

## 9. Interview questions and answers

**Q1. Can there be two judges?**
> No. A judge is trusted by everyone else, including the other "judge", who would then trust someone and be disqualified.

**Q2. Why does a single score work?**
> The judge's in-degree is the maximum possible (n − 1) and their out-degree is 0. For anyone else, either the in-degree is smaller or the out-degree is positive, so the score is below n − 1.

### Follow-up questions

**F1. The celebrity problem (LC 277): only a `knows(a, b)` API is available.**
> Eliminate candidates in one pass (if a knows b, a isn't the celebrity; otherwise b isn't), then verify the survivor: O(n) calls.

**F2. Find the centre of a star graph.** → [E04](E04_FindCenterOfStarGraph.md)
> Also a degree question: the centre is in every edge.

**F3. Find the sources and sinks of a DAG.**
> In-degree 0 = sources, out-degree 0 = sinks (used by Kahn's topological sort).

---

## 10. Running the file

```bash
java E03_FindTheTownJudge.java      # 5/5 tests passed (including 1,000 random towns vs brute force)
```

**One-line summary:** *"score[a]−− and score[b]++ for each 'a trusts b'; the judge is the only person whose score equals n − 1."*
