# M02 · Daily Temperatures

| | |
|---|---|
| **LeetCode** | [#739 Daily Temperatures](https://leetcode.com/problems/daily-temperatures/) |
| **Difficulty** | Medium |
| **Pattern** | Monotonic (decreasing) stack of **indices**: "how far to the next greater?" |
| **Code** | [M02_DailyTemperatures.java](M02_DailyTemperatures.java) |
| **Run** | `java M02_DailyTemperatures.java` |

---

## 1. The problem

For each day, how many days do you wait until a **warmer** temperature? 0 if it never comes.

| temps | answer |
|---|---|
| `[73,74,75,71,69,72,76,73]` | `[1,1,4,2,1,1,0,0]` |
| `[30,40,50,60]` | `[1,1,1,0]` |
| `[30,60,90]` | `[1,1,0]` |
| `[50,50,50]` | `[0,0,0]` (equal is not warmer) |

**Constraints:** `1 ≤ n ≤ 10⁵`, `30 ≤ temp ≤ 100`.

---

## 2. How to think about it

It's [Next Greater Element (E03)](../02-Easy/E03_NextGreaterElementI.md), but the answer is a **distance**, so the stack must hold **indices** (values alone can't tell you how far apart two days are).

```
stack = the indices of days still waiting for a warmer day (temperatures decreasing bottom → top)
for each day i:
    while temps[i] > temps[stack.top]:     // day i is the warmer day they were waiting for
        j = stack.pop(); answer[j] = i − j
    push i
days left on the stack never got a warmer day → their answer stays 0
```

---

## 3. Java concepts used in this problem

### 3.1 Indices on the stack, values through the array

```java
temps[stack.peek()]     // look up the temperature of the waiting day
```

Storing indices gives you both the **position** (for distances) and, through the array, the **value**. It also handles duplicate values correctly, which a value-keyed map can't.

### 3.2 A default of 0 for free

`new int[n]` starts at all zeros, which is exactly "no warmer day", so the days left on the stack need no extra handling.

### 3.3 Strict `>`

"Warmer" means strictly greater; equal temperatures don't resolve each other (the `[50,50,50]` test).

---

## 4. Approach 1: Brute force

### Code ([M02_DailyTemperatures.java:55-61](M02_DailyTemperatures.java#L55-L61))

```java
static int[] brute(int[] t) {
    int[] a = new int[t.length];
    for (int i = 0; i < t.length; i++)
        for (int j = i + 1; j < t.length; j++)
            if (t[j] > t[i]) { a[i] = j - i; break; }
    return a;
}
```

For each day, scan forward to the first warmer day. **O(n²)** worst case (a decreasing array). It's the reference for the random tests.

---

## 5. Approach 2: Monotonic stack of indices (the file's solution)

### Code ([M02_DailyTemperatures.java:42-53](M02_DailyTemperatures.java#L42-L53))

```java
static int[] dailyTemperatures(int[] temps) {
    int[] answer = new int[temps.length];
    Deque<Integer> stack = new ArrayDeque<>();
    for (int i = 0; i < temps.length; i++) {
        while (!stack.isEmpty() && temps[i] > temps[stack.peek()]) {
            int j = stack.pop();
            answer[j] = i - j;
        }
        stack.push(i);
    }
    return answer;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] answer = new int[n];` | all 0 by default |
| `Deque<Integer> stack` | the indices waiting for a warmer day |
| `while (... temps[i] > temps[stack.peek()])` | today is warmer than the most recent waiting day |
| `int j = stack.pop(); answer[j] = i - j;` | day j waited `i − j` days |
| `stack.push(i);` | today waits for its own warmer day |

### Dry run: `[73,74,75,71,69,72,76,73]` (the stack shows index(temp))

| i | temp | pops (j: answer) | stack after |
|---|---|---|---|
| 0 | 73 | — | 0(73) |
| 1 | 74 | 0: 1 | 1(74) |
| 2 | 75 | 1: 1 | 2(75) |
| 3 | 71 | — | 2(75) 3(71) |
| 4 | 69 | — | 2 3 4(69) |
| 5 | 72 | 4: 1, 3: 2 | 2(75) 5(72) |
| 6 | 76 | 5: 1, 2: 4 | 6(76) |
| 7 | 73 | — | 6 7(73) |

Days 6 and 7 stay 0 → **`[1,1,4,2,1,1,0,0]`** ✓

### Complexity
- **Time O(n)** (each index is pushed and popped once), **Space O(n).**

---

## 6. (Bonus) Approach 3: Scan from the right with jumps (O(1) extra)

Process the days from right to left; to find the next warmer day after i, start at `i + 1` and **jump** using the already-computed answers:

```java
static int[] dailyTemperaturesJump(int[] t) {
    int n = t.length;
    int[] ans = new int[n];
    for (int i = n - 2; i >= 0; i--) {
        int j = i + 1;
        while (j < n && t[j] <= t[i]) {
            if (ans[j] == 0) { j = n; break; }   // j never gets warmer, so nothing after it beats t[i] either
            j += ans[j];                         // jump to j's warmer day
        }
        if (j < n) ans[i] = j - i;
    }
    return ans;
}
```

It reuses the answer array as "next pointers", so it needs no stack: amortised O(n) time, O(1) extra space. It's a nice optimisation to mention.

---

## 7. Comparing the approaches

| | Brute | Monotonic stack | Right-to-left jumps |
|---|---|---|---|
| Time | O(n²) | **O(n)** | O(n) amortised |
| Extra space | O(1) | O(n) | **O(1)** |

---

## 8. Edge cases and common mistakes

**Edge cases:** strictly decreasing (all 0), strictly increasing (all 1), equal temperatures, a single day.

**Mistakes:**
1. Storing temperatures instead of indices (you can't compute the distance).
2. Using `>=` (equal days aren't warmer).
3. Computing `j − i` instead of `i − j` (the popped index j is earlier).

---

## 9. Interview questions and answers

**Q1. Why store indices?**
> The answer is a distance between days, so we need the positions. The values are available through the array.

**Q2. Why is the stack decreasing?**
> Every index on it is still waiting for something warmer; a warmer day would have popped any colder day before it. So from the bottom to the top the temperatures are non-increasing.

### Follow-up questions

**F1. Online Stock Span (LC 901).** → [M07](M07_OnlineStockSpan.md)
> The same stack, looking **backwards**: the span = the distance to the previous greater price.

**F2. Next greater element in a circular array.** → [M03](M03_NextGreaterElementII.md)

**F3. The number of visible people in a queue (LC 1944).**
> A monotonic stack, counting the pops plus one for the blocker.

---

## 10. Running the file

```bash
java M02_DailyTemperatures.java      # 5/5 tests passed (including 500 random arrays)
```

**One-line summary:** *"A stack of waiting indices; a warmer day pops them and answers i − j."*
