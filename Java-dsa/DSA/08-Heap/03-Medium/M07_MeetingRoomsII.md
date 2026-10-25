# M07 · Meeting Rooms II

| | |
|---|---|
| **LeetCode** | [#253 Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/) (premium) |
| **Difficulty** | Medium |
| **Pattern** | Sort by start + a min-heap of end times (or a sweep over +1/−1 events) |
| **Code** | [M07_MeetingRoomsII.java](M07_MeetingRoomsII.java) |
| **Run** | `java M07_MeetingRoomsII.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Meetings are half-open intervals [start, end). What's the minimum number of rooms so that no two meetings in the same room overlap? A meeting ending at 10 frees its room for one starting at 10.

| intervals | answer |
|---|---|
| `[[0,30],[5,10],[15,20]]` | 2 |
| `[[7,10],[2,4]]` | 1 |
| `[[1,5],[5,9]]` | 1 (back to back) |

---

## 2. How to think about it

### The heap: simulate a building manager
- sort the meetings by **start**
- a min-heap holds the **end times** of the meetings currently occupying a room
- for each meeting: if the earliest-ending room is free by now (`heap.peek() <= start`), reuse it (poll); then push this meeting's end time

The number of rooms = the largest heap size reached. Since we only poll when we immediately push again, the heap never shrinks, so that's just its **final size**.

```
[0,30]:  heap {30}
[5,10]:  30 > 5 → a new room           → {10, 30}
[15,20]: 10 ≤ 15 → reuse: poll 10, push 20 → {20, 30}      → 2 rooms
```

### The sweep: two sorted arrays (no heap)
Sort the starts and the ends separately. Walk through the starts; for each start, if the next end ≤ start, a meeting has finished (advance the end pointer, reuse its room); otherwise, we need one more room. Equivalently: +1 at every start, −1 at every end, and take the maximum running sum.

**Why can starts and ends be separated?** The room count only depends on **how many** meetings are running at each moment, not on which one ends.

---

## 3. Java concepts used in this problem

### 3.1 Sorting `int[][]` by one column

```java
Arrays.sort(m, Comparator.comparingInt(iv -> iv[0]));
```

It sorts the rows by their first element. `intervals.clone()` is sorted instead of the input, so the caller's order is kept (it's a shallow copy, which is enough because the rows aren't modified).

### 3.2 `peek() <= start` for half-open intervals

`<=` (not `<`) is what lets a meeting starting exactly when another ends reuse the room. With closed intervals, it would be `<`.

### 3.3 Brute force by time points (the referee)

The referee counts, for every integer time t in [0, 60], how many meetings contain t (`start ≤ t < end`). The maximum is the answer, because the number of rooms needed equals the maximum overlap.

---

## 4. Approach 1: Maximum overlap at integer times (the referee)

### Code ([M07_MeetingRoomsII.java:66-74](M07_MeetingRoomsII.java#L66-L74))

```java
static int brute(int[][] iv) {          // max overlap at any integer time point
    int best = 0;
    for (int t = 0; t <= 60; t++) {
        int c = 0;
        for (int[] x : iv) if (x[0] <= t && t < x[1]) c++;
        best = Math.max(best, c);
    }
    return best;
}
```

It only works for small integer times (the test uses 0..60), but it's clearly correct: the minimum number of rooms is the maximum number of meetings running at the same moment.

---

## 5. Approach 2: Sort + a min-heap of end times (the file's main solution)

### Code ([M07_MeetingRoomsII.java:41-50](M07_MeetingRoomsII.java#L41-L50))

```java
static int minMeetingRooms(int[][] intervals) {
    int[][] m = intervals.clone();
    Arrays.sort(m, Comparator.comparingInt(iv -> iv[0]));
    PriorityQueue<Integer> ends = new PriorityQueue<>();
    for (int[] iv : m) {
        if (!ends.isEmpty() && ends.peek() <= iv[0]) ends.poll();   // reuse the room that frees first
        ends.offer(iv[1]);
    }
    return ends.size();
}
```

### Line by line

| Line | What it does |
|---|---|
| sort by start | handle the meetings in the order they begin |
| `ends` | the end times of the rooms in use |
| `ends.peek() <= iv[0]` | the room that frees up first is free by this meeting's start |
| `ends.poll()` | reuse it (its old meeting is over) |
| `ends.offer(iv[1])` | this meeting now occupies a room until its end |
| `ends.size()` | the number of rooms ever opened |

### Dry run: `[[0,30],[5,10],[15,20]]`

| meeting | peek | reuse? | heap after | rooms |
|---|---|---|---|---|
| [0, 30] | — | | {30} | 1 |
| [5, 10] | 30 | 30 > 5 → no | {10, 30} | 2 |
| [15, 20] | 10 | 10 ≤ 15 → poll | {20, 30} | 2 |

**2** ✓

### Complexity
- **Time O(n log n).**
- **Space O(n).**

---

## 6. Approach 3: Sweep with sorted starts and ends

### Code ([M07_MeetingRoomsII.java:52-64](M07_MeetingRoomsII.java#L52-L64))

```java
static int minMeetingRoomsSweep(int[][] intervals) {
    int n = intervals.length;
    int[] starts = new int[n], ends = new int[n];
    for (int i = 0; i < n; i++) { starts[i] = intervals[i][0]; ends[i] = intervals[i][1]; }
    Arrays.sort(starts);
    Arrays.sort(ends);
    int rooms = 0, e = 0;
    for (int s : starts) {
        if (ends[e] <= s) e++;          // a meeting ended: its room is reused
        else rooms++;                   // all busy: need a new room
    }
    return rooms;
}
```

### Line by line

| Line | What it does |
|---|---|
| `starts`, `ends` | sorted independently |
| `ends[e] <= s` | some meeting has finished by time s → reuse its room (and move to the next end) |
| `rooms++` | every open room is busy → open another |

### Dry run

starts = [0, 5, 15], ends = [10, 20, 30]

| s | ends[e] | ≤ s? | e | rooms |
|---|---|---|---|---|
| 0 | 10 | no | 0 | 1 |
| 5 | 10 | no | 0 | 2 |
| 15 | 10 | yes | 1 | 2 |

**2** ✓

### Complexity
- **Time O(n log n)** (the two sorts).
- **Space O(n).**

---

## 7. Comparing the approaches

| | Time points (referee) | Heap of ends | Sweep |
|---|---|---|---|
| Time | O(T · n) | O(n log n) | O(n log n) |
| Space | O(1) | O(n) | O(n) |
| Can assign actual rooms? | no | yes (store a room id with each end) | no |

The file checks both real approaches against the referee on 1,000 random schedules.

---

## 8. Edge cases and common mistakes

**Edge cases:** no meetings, one meeting, back-to-back meetings, identical meetings, one long meeting covering all the others.

**Mistakes:**
1. Using `<` instead of `<=` (back-to-back meetings would need two rooms).
2. Forgetting to sort by start.
3. Polling **every** finished meeting before pushing (with a `while`). That's fine too, but then the answer is the **maximum** heap size, not the final one.

---

## 9. Interview questions and answers

**Q1. Why does the heap's final size equal the number of rooms?**
> Each meeting either reuses a room (one poll, one offer: the size stays the same) or opens a new one (one offer: the size grows). The size never decreases, so it ends at the total number of rooms opened.

**Q2. Why is the answer the maximum overlap?**
> At the busiest moment, every running meeting needs its own room, so that many rooms are necessary. The greedy reuse shows that this many is also enough.

### Follow-up questions

**F1. Meeting Rooms I (LC 252): can one person attend them all?**
> Sort by start and check that no meeting starts before the previous one ends.

**F2. Meeting Rooms III (LC 2402): numbered rooms, delayed meetings, which room is used most?**
> Two heaps: free rooms (by number) and busy rooms (by end time, then number).

**F3. Car Pooling (LC 1094).**
> The same sweep: +passengers at pickup, −passengers at drop-off; check against the capacity.

---

## 10. Running the file

```bash
java M07_MeetingRoomsII.java      # 5/5 tests passed (including 1,000 random schedules vs max overlap)
```

**One-line summary:** *"Sort by start and keep a min-heap of the rooms' end times: reuse the earliest-ending room if it's free, otherwise open a new one; the heap size is the answer."*
