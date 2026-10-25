# M07 · Online Stock Span

| | |
|---|---|
| **LeetCode** | [#901 Online Stock Span](https://leetcode.com/problems/online-stock-span/) |
| **Difficulty** | Medium |
| **Pattern** | A monotonic stack that **merges** spans ("previous greater" on a stream) |
| **Code** | [M07_OnlineStockSpan.java](M07_OnlineStockSpan.java) |
| **Run** | `java M07_OnlineStockSpan.java` |

---

## 1. The problem

Prices arrive one per day (`next(price)` is called once per day). **span(today)** = the number of consecutive days ending **today** (going backwards) with price **≤** today's price.

| day | price | span | why |
|---|---|---|---|
| 1 | 100 | 1 | just today |
| 2 | 80 | 1 | 100 > 80 |
| 3 | 60 | 1 | |
| 4 | 70 | 2 | 70, 60 |
| 5 | 60 | 1 | |
| 6 | 75 | 4 | 75, 60, 70, 60 |
| 7 | 85 | 6 | 85, 75, 60, 70, 60, 80 |

**Constraints:** up to 10⁴ calls.

---

## 2. How to think about it

- **Brute force:** for each day, walk backwards until a higher price: O(n) per call, O(n²) in total.
- **Key insight:** once today's price p "absorbs" a previous day (that day's price ≤ p), **that day can never be the stopping point for a future day**:
  - if a future price is ≥ p, it walks past today anyway (and therefore past that day too)
  - if a future price is < p, it stops at today first

  So we can **merge** the absorbed days into today. Keep a stack of **(price, span)** with **decreasing** prices. When a new price comes, pop every entry with price ≤ it and **add their spans** to today's span.

This is "the distance to the **previous greater** element", computed on a stream.

---

## 3. Java concepts used in this problem

### 3.1 A stateful class answering a stream of calls

```java
static class StockSpanner {
    private final Deque<int[]> stack = new ArrayDeque<>();
    public int next(int price) { ... }
}
```

The object keeps the stack **between** calls. Each `next` uses everything remembered so far. That's the "online" part: answers come as the data arrives.

### 3.2 Compressing history

The stack doesn't store every past day, only the ones that could still matter (the decreasing "record highs" looking backwards), each with a span that counts the days it absorbed. That's a powerful general idea: **keep a compressed summary instead of the raw history**.

---

## 4. Approach 1: Brute force (store all prices)

```java
static class StockSpannerBrute {
    private final List<Integer> prices = new ArrayList<>();
    public int next(int price) {
        prices.add(price);
        int span = 0;
        for (int j = prices.size() - 1; j >= 0 && prices.get(j) <= price; j--) span++;
        return span;
    }
}
```

**O(n) per call** in the worst case (a rising price sequence), O(n²) in total.

---

## 5. Approach 2: A monotonic stack of (price, span) (the file's solution)

### Code ([M07_OnlineStockSpan.java:41-52](M07_OnlineStockSpan.java#L41-L52))

```java
static class StockSpanner {
    private final Deque<int[]> stack = new ArrayDeque<>();   // [price, span], prices decreasing

    public int next(int price) {
        int span = 1;
        while (!stack.isEmpty() && stack.peek()[0] <= price) {
            span += stack.pop()[1];                          // absorb the smaller day's whole span
        }
        stack.push(new int[]{price, span});
        return span;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `int span = 1;` | today always counts |
| `while (... stack.peek()[0] <= price)` | the most recent kept day has a price ≤ today's |
| `span += stack.pop()[1];` | absorb it **and** everything it had already absorbed |
| `stack.push(new int[]{price, span});` | today becomes a kept day with its total span |
| `return span;` | |

### Dry run: 100, 80, 60, 70, 60, 75, 85

| price | pops (price, span) | today's span | stack after (bottom → top) |
|---|---|---|---|
| 100 | — | 1 | (100,1) |
| 80 | — | 1 | (100,1) (80,1) |
| 60 | — | 1 | … (60,1) |
| 70 | (60,1) | 1 + 1 = **2** | (100,1) (80,1) (70,2) |
| 60 | — | 1 | … (70,2) (60,1) |
| 75 | (60,1), (70,2) | 1 + 1 + 2 = **4** | (100,1) (80,1) (75,4) |
| 85 | (75,4), (80,1) | 1 + 4 + 1 = **6** | (100,1) (85,6) |

**Spans `1, 1, 1, 2, 1, 4, 6`** ✓

### Complexity
- **Amortised O(1) per call**: each day is pushed once and popped at most once.
- **Space O(n)** in the worst case (a decreasing price sequence).

---

## 6. Comparing the approaches

| | Brute force | Monotonic stack |
|---|---|---|
| Time per call | O(n) | **O(1) amortised** |
| Space | O(n) | O(n) worst case |

---

## 7. Edge cases and common mistakes

**Edge cases:** strictly increasing prices (each call pops everything), strictly decreasing (nothing is ever popped), equal prices (`≤` counts them).

**Mistakes:**
1. Using `<` instead of `<=` (equal prices must be included in the span).
2. Adding 1 per popped entry instead of its stored span (that loses the absorbed days).
3. Storing only prices (then you'd need indices to compute the distances).

---

## 8. Interview questions and answers

**Q1. Why can absorbed days be forgotten?**
> A day with a price ≤ today's can't stop any future walk that reaches today: a bigger future price walks past both, and a smaller one stops at today first.

**Q2. What does each stack entry represent?**
> A "record" day (higher than everything after it so far) and how many consecutive days, ending at it, it covers.

### Follow-up questions

**F1. An index-based version (offline array).** → [M02 Daily Temperatures](M02_DailyTemperatures.md)
> Store indices; span = `i − (index of the previous greater element)` (or `i + 1` if there's none).

**F2. Spans where the price is strictly less.**
> Use `<` in the while condition.

**F3. Sum of Subarray Minimums (LC 907).** → [M15](M15_SumOfSubarrayMinimums.md)
> The previous smaller and next smaller elements give how many subarrays each element is the minimum of: the same span idea, but in both directions.

---

## 9. Running the file

```bash
java M07_OnlineStockSpan.java      # 2/2 tests passed (including 200 random price streams)
```

**One-line summary:** *"Keep (price, span) with decreasing prices; a new price pops the ≤ entries and adds their spans to its own."*
