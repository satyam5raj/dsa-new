# 19 · Stock Exchange Order Matching Engine (explained simply)

| | |
|---|---|
| **Code** | [OrderMatchingEngine.java](OrderMatchingEngine.java) |
| **Run** | `java OrderMatchingEngine.java` (20 self-checks, including a cross-check against a naive matcher on 300 random sessions) |
| **Design notes** | [README.md](README.md) (latency, O(1) cancel, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

The matching engine is the heart of every stock exchange: buyers and sellers send orders, and the engine decides **who trades with whom, at what price, in what order**. The rules are simple and strict (price-time priority), the data structure is elegant (sorted maps of FIFO queues), and the concurrency answer is surprising: **one thread per stock, no locks at all.**

---

## 1. The problem in plain words

For each stock (symbol) there's an **order book**:
- **bids**: people who want to buy, at a price or less;
- **asks**: people who want to sell, at a price or more.

When a new order arrives, if it can trade with orders on the other side, it does, best price first, and among equal prices, **whoever came first**. Whatever can't trade either **waits in the book** or is **cancelled**, depending on the order type.

Order types:
- **LIMIT**: "buy 80 at ₹100 or less";
- **MARKET**: "buy 150 at whatever the price is";
- **time in force**: GTC (stay in the book until cancelled), IOC (fill what you can now, cancel the rest), FOK (fill **everything** right now, or nothing).

Plus cancel and amend.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Matching rule? | Price-time priority |
| Trade price? | The **resting** (maker) order's price |
| Order types? | LIMIT and MARKET; time in force GTC / IOC / FOK |
| Partial fills? | Yes |
| Cancel / amend? | Yes; reducing quantity keeps priority, anything else loses it |
| Many symbols? | Yes, independent of each other |
| Throughput and determinism? | High; the same input must give the same trades |
| Prices? | `long` ticks (paise), never `double` |

### Step 2 · Requirements
1. Match an incoming order against the opposite side, best price first, then FIFO.
2. Trades at the maker's price; partial fills on both sides.
3. The remainder rests (GTC limit) or is cancelled (MARKET, IOC, FOK).
4. FOK: check that enough quantity exists **before** trading anything.
5. Cancel and amend.
6. Market data: best bid/ask, depth; trade events.
7. The book is never "crossed" (best bid < best ask) after an order is processed.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| side, type, time in force, status | four enums |
| order | `class Order` (mutable: filled, status) |
| trade | `record Trade(seq, symbol, buyOrder, sellOrder, price, qty, aggressor)` |
| order book | `class OrderBook` (bids, asks, resting index) |
| engine | `class MatchingEngine` (one book + one thread per symbol) |
| market-data subscriber | `Consumer<Trade>` listener |

### Step 4 · Relationships

```
MatchingEngine
  ├── "INFY" → OrderBook + single-thread sequencer
  └── "TCS"  → OrderBook + single-thread sequencer

OrderBook
  bids: TreeMap (highest price first)   102 → [O7, O9]   98 → [O3]
  asks: TreeMap (lowest price first)    103 → [O4]      104 → [O1, O5, O8]
                                        each price level = FIFO queue (ArrayDeque)
  resting: id → Order   (for cancel / amend)
```

### Step 5 · API

```java
List<Trade> submit(Order o)            // OrderBook
boolean cancel(String orderId)
Order amend(String orderId, long newPrice, long newQty)
OptionalLong bestBid() / bestAsk();    String depth(int n)

Future<List<Trade>> engine.submit(trader, symbol, side, type, tif, price, qty)
```

### Step 6 · Patterns

| What matters | Pattern / idea |
|---|---|
| best price + earliest first | **sorted map of FIFO queues** |
| no races in the book | **single writer** per symbol (LMAX-style sequencer) |
| trade feed to many consumers | **Observer** (listeners) |
| correctness proof | a **reference implementation** to cross-check against |

### Step 7 · Concurrency
Many traders submit at once, but each book is touched by **only one thread**: every command for a symbol goes into that symbol's queue and is applied in order. Different symbols run in parallel.

---

## 3. The key ideas, simply

### Idea 1: price-time priority, like a queue at a market stall
Sellers line up at different prices. A buyer goes to the **cheapest** stall first. At a stall where several sellers ask the same price, the one who **arrived first** sells first.

**Example from the demo:** sellers A (50 @ 100), B (50 @ 100), C (100 @ 101), then D (10 @ 99) arrives **last** but is the **cheapest**. A buyer E wants 80 at up to ₹100:

| Step | Best ask | Trade | E still wants |
|---|---|---|---|
| 1 | 99: [D] | 10 @ 99 with D | 70 |
| 2 | 100: [A, B] | 50 @ 100 with A | 20 |
| 3 | 100: [B] | 20 @ 100 with B (B has 30 left) | 0 |

C at 101 is never touched: E's limit is 100.

### Idea 2: trade at the maker's price
The order already resting in the book is the **maker**; the incoming one is the **taker** (aggressor). The trade happens at the **maker's** price. E was willing to pay 100 but bought D's shares at 99: the taker gets any price improvement.

### Idea 3: the data structure
- `TreeMap<price, queue>` keeps price levels **sorted**: the best price is `firstEntry()` in O(log P).
- Bids use `Comparator.reverseOrder()`, so "first" = **highest** bid; asks use the natural order, so "first" = **lowest** ask.
- Each level is an `ArrayDeque` (FIFO): earliest at the front.

### Idea 4: one thread per symbol (no locks)
Exchanges don't lock the book. They put every command for a symbol into a queue, and **one thread** applies them one by one. This is fast (no lock contention, cache-friendly), and **deterministic**: replaying the same input log gives exactly the same trades, which is essential for audits and recovery.

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `TreeMap` + `Comparator.reverseOrder()` | bids / asks | sorted keys; reversed for bids so the highest comes first |
| `firstEntry()` / `pollFirstEntry()` | matching | look at / remove the best price level |
| `ArrayDeque` (`peekFirst`, `pollFirst`, `addLast`) | price levels | a FIFO queue |
| `computeIfAbsent(price, p -> new ArrayDeque<>())` | resting an order | create the level if it's new |
| `Executors.newSingleThreadExecutor` | sequencer per symbol | one thread executing tasks strictly in submission order |
| `Future<List<Trade>>` | engine `submit` | the caller can wait for the result if it wants |
| `Consumer<Trade>` | listeners | market-data subscribers |
| `OptionalLong` | best bid/ask | "maybe a price" (the book side may be empty) |

---

## 5. The code, piece by piece

### 5.1 Can this order trade at that price?

```java
boolean crosses(long restingPrice) {
    if (type == OrderType.MARKET) return true;                       // a market order takes any price
    return side == Side.BUY ? price >= restingPrice : price <= restingPrice;
}
```

A buyer at 100 trades with asks at 100 or below; a seller at 100 trades with bids at 100 or above.

### 5.2 The whole matching algorithm

```java
List<Trade> submit(Order o) {
    o.seq = ++seq;
    TreeMap<Long, ArrayDeque<Order>> opposite = sideOf(o.side.opposite());

    if (o.tif == TimeInForce.FOK && available(o, opposite) < o.qty) {   // all or nothing: check BEFORE touching the book
        o.status = OrderStatus.CANCELLED;
        return List.of();
    }

    List<Trade> produced = new ArrayList<>();
    while (o.remaining() > 0 && !opposite.isEmpty()) {
        Map.Entry<Long, ArrayDeque<Order>> best = opposite.firstEntry();      // best price on the other side
        if (!o.crosses(best.getKey())) break;                                // too expensive: stop
        ArrayDeque<Order> level = best.getValue();
        Order maker = level.peekFirst();                                     // earliest at that price
        long q = Math.min(o.remaining(), maker.remaining());
        Trade t = new Trade(..., best.getKey() /* maker's price */, q, o.id);
        o.filled += q; maker.filled += q;
        if (maker.remaining() == 0) { level.pollFirst(); resting.remove(maker.id); }
        if (level.isEmpty()) opposite.pollFirstEntry();                      // price level used up
        ...publish t...
    }

    if (o.remaining() == 0) o.status = FILLED;
    else if (o.type == MARKET || o.tif != GTC) o.status = CANCELLED;        // never rests
    else { ...rest the remainder at o.price, at the BACK of its level... }
    return produced;
}
```

| Part | Simple meaning |
|---|---|
| FOK pre-check | count the quantity available at acceptable prices; if it's short, do nothing at all |
| the `while` loop | keep eating the best level until the order is filled, the other side is empty, or the price no longer works |
| `Math.min` | a trade is the smaller of the two remaining quantities (a partial fill on one side) |
| removing empty makers / levels | keeps `firstEntry()` always pointing at a real, live order |
| the final `if` | what happens to the leftover: done, cancelled (MARKET/IOC/FOK), or rests (GTC) |

**Why the book is never crossed:** an order only rests if the loop stopped because it **no longer crosses** the best opposite price. So a resting bid is always below the best ask, and vice versa.

### 5.3 Cancel and amend

```java
boolean cancel(String orderId) {
    Order o = resting.remove(orderId);
    if (o == null) return false;                     // unknown, or already filled
    ArrayDeque<Order> level = sideOf(o.side).get(o.price);
    level.remove(o);                                 // O(level size); real engines use an intrusive linked list for O(1)
    if (level.isEmpty()) sideOf(o.side).remove(o.price);
    o.status = CANCELLED; return true;
}

Order amend(String orderId, long newPrice, long newQty) {
    Order o = resting.get(orderId);
    if (newPrice == o.price && newQty < o.qty && newQty > o.filled) { o.qty = newQty; return o; }   // reduce: keep priority
    cancel(orderId);                                                                                 // anything else:
    Order replacement = new Order(o.id + "'", ..., newPrice, newQty - o.filled);                    // a new order at the back
    submit(replacement);
    return replacement;
}
```

**Why reducing keeps priority:** making your order smaller can only help the people behind you. Raising the quantity or changing the price would let you jump the queue unfairly, so it counts as a new order (and may even trade immediately).

---

## 6. Dry run (from the real output)

| # | Order | Trades | Book after |
|---|---|---|---|
| 1 | sells A 50@100, B 50@100, C 100@101, D 10@99 | none | asks 10@99 100@100 100@101 |
| 2 | **E** buy 80 @100 | 10@99 (D), 50@100 (A), 20@100 (B) | B: PARTIALLY_FILLED, 30 left, **still first** at 100; best ask 100, no bids |
| 3 | **F** buy 50 @100 | 30@100 (B) | bids 20@100 \| asks 100@101 |
| 4 | **G** MARKET buy 150 | 100@101 (C) | the other 50 cancelled: market orders never rest |
| 5 | H sell 40@105; **I** IOC buy 60 @105 | 40@105 | the other 20 cancelled |
| 6 | J sell 30@106; **K** FOK buy 50 @106 | none: only 30 available | K cancelled, J's 30 untouched |
| 7 | **L** FOK buy 30 @106 | 30@106 | L FILLED |
| 8 | M, N sell 10@110; cancel M | | `cancel(M)` true; `cancel(A)` false (already filled) |
| 9 | P sell 10@110; amend N → qty 5 | | level 110 = [N, P] (kept its place) |
| 10 | amend N → qty 20 | | level 110 = [P, N']: moved to the back |
| 11 | a limit order with price 0 | | rejected in the constructor |

### Cross-check (step 5 of the demo)
300 random sessions of 60 orders each (mixed LIMIT/MARKET and GTC/IOC/FOK, prices 95–105). The same orders go through the real book and through `naive()`, a deliberately slow matcher that **re-sorts a plain list every time** (obviously correct). Results:
- **identical trades** (prices, quantities and who traded with whom);
- the book was **never crossed** after any order;
- traded quantity = total bought = total sold, and no order ever filled more than its quantity.

**Testing tip worth saying in an interview:** for tricky algorithms, write a dumb-but-obviously-correct version and compare them on thousands of random inputs. That's how this design is checked.

### Concurrency (step 6 of the demo)
8 client threads submit 16,000 random orders to two symbols. Each symbol's sequencer applies its orders one by one. For both symbols, the book isn't crossed, and the trades in the book equal the quantity seen by the trade listener. (The final depth printed differs each run because the orders are random.)

---

## 7. Concurrency, explained simply

```
trader threads ──submit──▶ [ INFY queue ] ──▶ seq-INFY thread ──▶ INFY OrderBook (no locks)
               ──submit──▶ [ TCS queue  ] ──▶ seq-TCS thread  ──▶ TCS OrderBook  (no locks)
```

| Question | Answer |
|---|---|
| Why no locks in `OrderBook`? | Only its sequencer thread ever touches it. |
| What's the order of execution? | The order the commands entered the symbol's queue: that's the official "time" in price-time priority. |
| How do reads see a consistent book? | `query()` also runs on the sequencer thread, after everything submitted before it. |
| Where's the parallelism? | Across symbols. |
| Where's validation done? | On the caller's thread (in the `Order` constructor), so bad input never reaches the sequencer. |

This is the **single-writer principle** from the [concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md), also used in the [elevator](../02-Elevator-System/ElevatorSystem.md).

---

## 8. Edge cases and common mistakes

**Edge cases handled:** an empty opposite side, a partial fill of the maker or the taker, a level emptied mid-match, market orders with too little liquidity, IOC remainders, FOK with not enough / exactly enough, cancelling filled or unknown orders, amend down vs up, invalid prices and quantities.

**Common mistakes:**
1. Trading at the taker's price (it should be the maker's).
2. A single sorted list of all orders (O(n) inserts) instead of price levels.
3. Forgetting to remove empty price levels, so "best price" points at nothing.
4. FOK that trades partially and then tries to undo it (check first instead).
5. Locks around the book with many threads (slow and non-deterministic).
6. `double` prices (`100.1 + 0.2` comparisons go wrong).
7. Letting an amend that increases the quantity keep its time priority.

---

## 9. Interview questions and answers

**Q1. Which data structures does the order book use, and why?**
> Two sorted maps from price to a FIFO queue: bids sorted descending, asks ascending. The best price is the first entry in O(log P), and within a level the queue gives time priority. A hash map from id to order supports cancel and amend.

**Q2. Walk me through matching a buy order.**
> While it has quantity left and the best ask is at or below its limit (any price for market orders), trade min(remaining, maker remaining) at the maker's price, removing filled makers and empty levels. Then the remainder rests if it's a GTC limit; otherwise it's cancelled.

**Q3. How do IOC and FOK differ?**
> IOC fills whatever is available now and cancels the rest. FOK first checks that the full quantity is available at acceptable prices; if not, nothing trades at all.

**Q4. How do you handle concurrency?**
> One single-threaded sequencer per symbol applies all commands in order, so the book needs no locks and results are deterministic and replayable. Symbols run in parallel.

**Q5. What are the complexities?**
> Matching costs O(log P) per price level touched plus O(1) per trade; resting is O(log P); cancel is O(1) lookup plus removal from the level (O(1) with an intrusive doubly linked list).

**Q6. How did you test it?**
> Unit scenarios for each rule, plus a randomized cross-check against a naive reference matcher over 300 sessions, checking the same trades, a never-crossed book and conserved quantities.

### Follow-up questions

**F1. "Make cancel O(1)."**
> Make each order a node in a doubly linked list per price level, holding pointers to its neighbours; the id map points to the node, so removal is O(1).

**F2. "Survive a crash."**
> Write every input command to a journal before applying it (event sourcing). Because matching is deterministic, replaying the journal rebuilds the exact book; take periodic snapshots to replay from.

**F3. "Reduce latency further."**
> A ring buffer (LMAX Disruptor) instead of a blocking queue, pre-allocated objects (no garbage collection pauses), primitive maps, pinned threads, and kernel-bypass networking.

**F4. "Stop orders and iceberg orders."**
> Stop orders wait in a separate structure keyed by trigger price and are submitted when a trade crosses it. Iceberg orders show only part of their quantity; when that part fills, the next part rejoins at the back of the level.

**F5. "Risk checks before matching."**
> A pre-trade risk stage (on the caller's side or a separate sequencer step) checks the trader's buying power, position limits and price bands, rejecting orders before they reach the book.

**F6. "What's self-trade prevention?"**
> If the maker and taker belong to the same trader, cancel one (or both) instead of trading, according to the exchange's rule.

---

## 10. Running the file

```bash
java OrderMatchingEngine.java     # 20/20 tests passed (300 random sessions match the naive reference exactly)
```

**One-line summary:** *"Each symbol's book is two TreeMaps of FIFO queues (bids descending, asks ascending) giving price-time priority. An incoming order eats the best opposite levels at the makers' prices until it's filled or stops crossing, then rests (GTC) or is cancelled (MARKET/IOC; FOK checks liquidity first). Every command for a symbol is applied by its single sequencer thread, so the book has no locks and replays deterministically."*
