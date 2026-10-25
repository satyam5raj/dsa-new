# M11 · Car Fleet

| | |
|---|---|
| **LeetCode** | [#853 Car Fleet](https://leetcode.com/problems/car-fleet/) |
| **Difficulty** | Medium |
| **Pattern** | Sort by position + a monotonic stack of arrival times |
| **Code** | [M11_CarFleet.java](M11_CarFleet.java) |
| **Run** | `java M11_CarFleet.java` |

---

## 1. The problem

Cars drive toward `target` on a **one-lane** road (**no overtaking**). A faster car that catches a slower one ahead joins it and drives at the slower speed: together they form a **fleet**. How many fleets arrive at the target? (A car that catches another exactly at the target also joins its fleet.)

```
target = 12
position = [10, 8, 0, 5, 3]
speed    = [ 2, 4, 1, 1, 3]       → 3 fleets
```

| target | position | speed | fleets |
|---|---|---|---|
| 12 | `[10,8,0,5,3]` | `[2,4,1,1,3]` | 3 |
| 10 | `[3]` | `[3]` | 1 |
| 100 | `[0,2,4]` | `[4,2,1]` | 1 |
| 10 | `[6,8,0]` | `[3,2,1]` | 3 |

**Constraints:** `1 ≤ n ≤ 10⁵`, the positions are distinct.

---

## 2. How to think about it

### Key ideas
1. Compute each car's **arrival time if it drove alone**: `time = (target − position) / speed`.
2. Process the cars from the **closest** to the target to the **farthest** (sort by position, descending).
3. A car **behind** catches the fleet ahead of it **iff its solo time ≤ that fleet's time**: it would arrive earlier or at the same time, but it can't pass, so it merges (and from then on arrives with that fleet's time). Otherwise it's slower and forms a **new** fleet, which becomes the "fleet ahead" for the cars behind it.

### A stack of fleet times
The fleet times are **strictly increasing** as you go back from the target.

```
for each car, closest first:
    if the stack is empty or time > stack.top → push time    (a new, slower fleet)
    else                                     → it merges     (do nothing)
answer = the stack's size
```

Only the top is ever compared, so a single variable would also do. The stack shows the monotonic structure.

---

## 3. Java concepts used in this problem

### 3.1 Sorting a 2D array with a comparator

```java
double[][] cars = new double[n][2];                        // [position, arrival time]
Arrays.sort(cars, (a, b) -> Double.compare(b[0], a[0]));   // descending by position
```

- To sort "rows" together, put the related values into one row (`[position, time]`) and sort the rows.
- The comparator `(a, b) -> Double.compare(b[0], a[0])` compares **b before a**, which gives a **descending** order.
- `Double.compare` is the safe way to compare doubles (`a − b` would need a cast to int and could lose precision).

### 3.2 Floating-point division

```java
(double) (target - position[i]) / speed[i]
```

The cast to `double` must happen **before** the division (otherwise the int division truncates first; see [Arrays E23](../../01-Arrays/02-Easy/E23_MaximumAverageSubarrayI.md)). Here the times are compared with `>` and `<=`. Equal times from different cars produce exactly the same double when the fractions are equal, but in general, comparing doubles for equality needs care (for an exact version, compare `(target − p1) · s2` with `(target − p2) · s1` using longs).

### 3.3 `Deque<Double>`

The stack holds boxed `Double`s; `car[1] > fleets.peek()` unboxes the top for the comparison.

---

## 4. Approach 1: Simulate step by step (impractical)

You could simulate time ticks, moving each car and merging when a car reaches the one ahead. But the positions can be up to 10⁶, the times are fractional, and merges happen between ticks. It's messy and slow. The **arrival-time** reformulation avoids any simulation, which is the main insight of this problem.

---

## 5. Approach 2: Sort + a stack of fleet times (the file's solution)

### Code ([M11_CarFleet.java:46-59](M11_CarFleet.java#L46-L59))

```java
static int carFleet(int target, int[] position, int[] speed) {
    int n = position.length;
    double[][] cars = new double[n][2];                       // [position, arrival time]
    for (int i = 0; i < n; i++) cars[i] = new double[]{position[i], (double) (target - position[i]) / speed[i]};
    Arrays.sort(cars, (a, b) -> Double.compare(b[0], a[0]));  // closest to the target first

    Deque<Double> fleets = new ArrayDeque<>();
    for (double[] car : cars) {
        if (fleets.isEmpty() || car[1] > fleets.peek()) {
            fleets.push(car[1]);                              // can't catch the fleet ahead
        }                                                     // else: merges into it
    }
    return fleets.size();
}
```

### Line by line

| Line | What it does |
|---|---|
| `cars[i] = {position, time}` | each car's position and solo arrival time |
| `Arrays.sort(cars, ...)` | the closest car to the target comes first |
| `if (fleets.isEmpty() \|\| car[1] > fleets.peek())` | slower than the fleet directly ahead → it can never catch it → a new fleet |
| (else) | it catches the fleet ahead and merges; its own time is irrelevant now |
| `return fleets.size();` | the number of fleets |

### Dry run: target 12

| car (position, speed) | time = (12 − p) / s |
|---|---|
| (10, 2) | 1 |
| (8, 4) | 1 |
| (5, 1) | 7 |
| (3, 3) | 3 |
| (0, 1) | 12 |

Processing in position order 10, 8, 5, 3, 0:

| time | compare with the top | action | stack |
|---|---|---|---|
| 1 | empty | push | [1] |
| 1 | 1 ≤ 1 | merges | [1] |
| 7 | 7 > 1 | push | [1, 7] |
| 3 | 3 ≤ 7 | merges | [1, 7] |
| 12 | 12 > 7 | push | [1, 7, 12] |

**3 fleets** ✓

### Dry run 2: target 10, positions [6, 8, 0], speeds [3, 2, 1]
Times: 6 → 4/3 ≈ 1.33, 8 → 1, 0 → 10. Order by position: 8 (1), 6 (1.33), 0 (10). Each time is bigger than the previous → **3 fleets** ✓

### Complexity
- **Time O(n log n)** (the sort), **Space O(n).**

---

## 6. Comparing the approaches

| | Simulation | Sort + times |
|---|---|---|
| Time | impractical | **O(n log n)** |
| Precision issues | many | only in the time comparison (avoidable with cross-multiplication) |

---

## 7. Edge cases and common mistakes

**Edge cases:** a single car, all cars merging, no car catching any other, equal arrival times (they merge).

**Mistakes:**
1. Processing the cars from the **back** (farthest first). The fleet structure depends on the cars ahead.
2. Comparing speeds instead of arrival times (a faster car might still be too far behind to catch up before the target).
3. Integer division when computing the times.
4. Using `>=` (equal times merge, so they shouldn't create a new fleet).

---

## 8. Interview questions and answers

**Q1. Why compare arrival times instead of speeds?**
> Catching up depends on both the distance and the speed. If the car behind would arrive no later than the car ahead (driving alone), it must catch it before or at the target. That's exactly time ≤ time.

**Q2. Why process from the closest car?**
> The car closest to the target is never slowed down, so its time is final. Each subsequent car only interacts with the fleet immediately ahead.

### Follow-up questions

**F1. Car Fleet II (LC 1776): the collision times between consecutive cars.**
> A monotonic stack from right to left, using collision times instead of arrival times. Pop the cars ahead that would merge into a slower car before the current car reaches them.

**F2. Exact arithmetic.**
> Compare `(target − p1) * s2` with `(target − p2) * s1` using `long`, which avoids floating point.

**F3. Count the cars in each fleet.**
> Keep (time, count) on the stack and increment the count when a car merges.

---

## 9. Running the file

```bash
java M11_CarFleet.java      # 4/4 tests passed
```

**One-line summary:** *"Sort the cars by position, closest to the target first; a car starts a new fleet only if its solo arrival time exceeds the fleet ahead's."*
