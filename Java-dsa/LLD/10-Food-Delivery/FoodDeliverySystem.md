# 10 · Food Delivery, like Swiggy or Zomato (explained simply)

| | |
|---|---|
| **Code** | [FoodDeliverySystem.java](FoodDeliverySystem.java) |
| **Run** | `java FoodDeliverySystem.java` (18 self-checks, including a stock race and 500 "cancel vs accept" races) |
| **Design notes** | [README.md](README.md) (more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This question has **four** kinds of actors (customer, restaurant, delivery agent, the system itself) all moving the same order forward. The key idea: the state machine isn't just "from → to", it's **"from → to, and who is allowed to do it"**. It also features a real race bug that leaked delivery agents, and how it was fixed.

---

## 1. The problem in plain words

A **customer** builds a **cart** from one **restaurant**, applies a **coupon**, pays, and places an **order**. Then:
1. the **restaurant** accepts (or rejects) it, starts preparing it, and marks it ready;
2. a **delivery agent** is assigned, picks it up, and delivers it;
3. the customer can **cancel**: a full refund before the restaurant accepts, 50% after (the food is being cooked), and not at all once it's picked up;
4. if the restaurant doesn't answer in time, the **system** rejects the order and refunds it.

Some menu items have **limited stock** ("only 2 portions of the chef's special today"). Everyone (customer app, restaurant tablet, agent app) sees live updates.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Can a cart mix restaurants? | No: one restaurant per order |
| Stock limits? | Optional per item (`-1` = unlimited) |
| Pricing? | Items + delivery fee (₹20 + ₹8/km) + 5% tax after discount |
| Coupons? | Percent off (with a cap and a minimum order) or flat off |
| Cancellation rules? | Free before acceptance, 50% after, impossible after pickup |
| Agent assignment? | The nearest available agent to the restaurant, when the restaurant accepts |
| Who can change the order status? | Each step only by the responsible actor |

### Step 2 · Requirements
1. Cart with the single-restaurant rule.
2. Billing with coupons.
3. Place: reserve stock (all-or-nothing), charge, create the order.
4. Lifecycle: accept/reject → preparing → ready → picked up → delivered, **each by the right actor**.
5. Cancel and reject with the right refunds; stock returned when nothing was cooked.
6. Assign an agent atomically; never leak an agent.
7. Live events to every party.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| restaurant, menu item | `class Restaurant`, `class MenuItem` (with atomic stock) |
| customer | `record Customer` |
| delivery agent | `class DeliveryAgent` (status by CAS) |
| cart | `class Cart` |
| bill | `record Bill(items, deliveryFee, tax, discount, total)` |
| coupon | `interface DiscountRule` + `PercentOff`, `FlatOff` |
| order | `class Order` with `enum OrderStatus` |
| who acts | `enum Actor { CUSTOMER, RESTAURANT, AGENT, SYSTEM }` |
| the lifecycle rules | `RULES`: (from, to) → allowed actors |
| live updates | `class OrderEventBus` |
| the service | `class OrderService` |

### Step 4 · Relationships

```
Customer ──builds──> Cart ──(one)──> Restaurant
OrderService
   ├── PriceCalculator ── DiscountRule(s)
   ├── AgentAssignmentStrategy (nearest agent)
   ├── PaymentGateway
   └── OrderEventBus ──> customer app, restaurant tablet, agent app
Order ── has ──> Customer, Restaurant, lines, Bill, DeliveryAgent, status, history
```

### Step 5 · API

```java
Order place(Cart cart, String coupon)
void accept(id) / reject(id) / startPreparing(id) / markReady(id)      // restaurant
void pickUp(id, agentId) / deliver(id, agentId)                        // agent
long cancelByCustomer(id)                                              // returns the refund
void autoRejectIfUnanswered(id)                                        // system timer
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| the order lifecycle with permissions | **State machine** with an actor-aware transition table |
| coupons | **Strategy** (`DiscountRule`) |
| choosing an agent | **Strategy** (`AgentAssignmentStrategy`) |
| live updates to many apps | **Observer / event bus** with topics |
| payments | an interface (`PaymentGateway`) |

### Step 7 · Concurrency
Three contended resources: **limited stock** (CAS), **an agent** (CAS on status), and **one order** being moved by several actors at once (the order's own lock), plus the subtle agent-leak race (section 7).

---

## 3. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| a CAS loop on `AtomicInteger` | `MenuItem.reserve` | read, check there's enough, swap only if unchanged; retry if someone else changed it |
| `updateAndGet` | `MenuItem.release` | atomically apply a function to the number |
| nested `Map.of(...)` | `RULES` | from → (to → allowed actors), all as data |
| `synchronized` method | `Order.transition` | one transition at a time per order |
| `synchronized (o)` block | `cancelByCustomer`, `assignAgent` | do several steps on the order under its lock |
| re-entrant locking | `transition` (synchronized) called inside `synchronized (o)` | the same thread can re-enter a lock it already holds |
| `Map.merge(key, qty, Integer::sum)` | adding to the cart | add to the quantity if the item is already there |
| records implementing an interface | `PercentOff`, `FlatOff` | small immutable rule objects |
| `Map.copyOf` | the order's lines | the order keeps its own unchangeable copy of the cart |
| topics as strings | `"customer:asha"` | the event bus routes by name |

---

## 4. The code, piece by piece

### 4.1 Limited stock with a CAS loop

```java
boolean reserve(int qty) {
    while (true) {
        int cur = stock.get();
        if (cur == -1) return true;                      // unlimited
        if (cur < qty) return false;                     // not enough left
        if (stock.compareAndSet(cur, cur - qty)) return true;
    }
}
void release(int qty) { stock.updateAndGet(s -> s == -1 ? -1 : s + qty); }
```

The **optimistic** pattern from [tool 2](../00-LLD-Playbook/ConcurrencyToolkit.md#tool-2--optimistic-locking--version--compare-and-set-retry-on-conflict): read the stock; if there's enough, swap in the new value only if nobody changed it meanwhile; otherwise loop. The stock can never go below zero.

### 4.2 The cart's single-restaurant rule

```java
void add(Restaurant r, String itemId, int qty) {
    if (qty <= 0) throw new IllegalArgumentException("qty must be positive");
    if (restaurant != null && restaurant != r) throw new IllegalStateException("Cart has items from " + restaurant.name);
    MenuItem m = r.menu.get(itemId);
    if (m == null) throw new IllegalArgumentException("No item " + itemId);
    restaurant = r;
    lines.merge(m, qty, Integer::sum);
}
```

(In the real app, this is when you see "Your cart has items from another restaurant. Start a new cart?")

### 4.3 Billing and coupons

```java
record PercentOff(String code, int percent, long maxOff, long minOrder) implements DiscountRule {
    public long discountPaise(Cart c, long items) { return items < minOrder ? 0 : Math.min(maxOff, items * percent / 100); }
}
record FlatOff(String code, long off, long minOrder) implements DiscountRule {
    public long discountPaise(Cart c, long items) { return items < minOrder ? 0 : Math.min(off, items); }
}

Bill bill(Cart cart, String couponCode) {
    long items = cart.itemsTotal();
    double km = cart.restaurant.location.dist(cart.customer.address);
    long delivery = 2_000 + Math.round(km * 800);                       // Rs 20 + Rs 8/km
    long discount = 0;
    if (couponCode != null) {
        DiscountRule r = coupons.get(couponCode);
        if (r == null) throw new IllegalArgumentException("Invalid coupon " + couponCode);
        discount = r.discountPaise(cart, items);
        if (discount == 0) throw new IllegalArgumentException("Coupon " + couponCode + " not applicable");
    }
    long tax = (items - discount) * 5 / 100;                            // 5% GST on food after discount
    return new Bill(items, delivery, tax, discount, items - discount + delivery + tax);
}
```

Each coupon is a small rule object. A new kind of coupon (buy-one-get-one, free delivery) is a new `DiscountRule` class.

### 4.4 The multi-actor state machine: one table

```java
enum Actor { CUSTOMER, RESTAURANT, AGENT, SYSTEM }
enum OrderStatus { PLACED, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED, REJECTED, CANCELLED }

/** (from, to) -> who may do it. THE single source of truth for the lifecycle. */
static final Map<OrderStatus, Map<OrderStatus, Set<Actor>>> RULES = Map.of(
        OrderStatus.PLACED, Map.of(OrderStatus.ACCEPTED, Set.of(Actor.RESTAURANT), OrderStatus.REJECTED, Set.of(Actor.RESTAURANT, Actor.SYSTEM),
                OrderStatus.CANCELLED, Set.of(Actor.CUSTOMER, Actor.SYSTEM)),
        OrderStatus.ACCEPTED, Map.of(OrderStatus.PREPARING, Set.of(Actor.RESTAURANT), OrderStatus.CANCELLED, Set.of(Actor.CUSTOMER, Actor.RESTAURANT)),
        OrderStatus.PREPARING, Map.of(OrderStatus.READY, Set.of(Actor.RESTAURANT), OrderStatus.CANCELLED, Set.of(Actor.CUSTOMER, Actor.RESTAURANT)),
        OrderStatus.READY, Map.of(OrderStatus.PICKED_UP, Set.of(Actor.AGENT)),
        OrderStatus.PICKED_UP, Map.of(OrderStatus.DELIVERED, Set.of(Actor.AGENT)));
```

| From | To | Who may do it |
|---|---|---|
| PLACED | ACCEPTED | restaurant |
| PLACED | REJECTED | restaurant, system (timeout) |
| PLACED | CANCELLED | customer, system |
| ACCEPTED | PREPARING | restaurant |
| ACCEPTED / PREPARING | CANCELLED | customer, restaurant |
| PREPARING | READY | restaurant |
| READY | PICKED_UP | agent |
| PICKED_UP | DELIVERED | agent |

```java
synchronized OrderStatus transition(OrderStatus to, Actor by) {
    Set<Actor> allowed = RULES.getOrDefault(status, Map.of()).getOrDefault(to, Set.of());
    if (!allowed.contains(by)) throw new IllegalStateException(by + " cannot move order " + id + " from " + status + " to " + to);
    OrderStatus from = status;
    status = to;
    history.add(to + " by " + by);
    return from;
}
```

**Why "who" matters:** a customer must never be able to mark their own order "accepted", and an agent must never mark an order "prepared". With the actor in the table, no actor can push an order into a state it isn't responsible for, and all the rules can be reviewed in one place.

### 4.5 Placing an order: stock all-or-nothing, then pay

```java
Order place(Cart cart, String coupon) {
    if (cart.lines.isEmpty()) throw new IllegalStateException("Empty cart");
    if (!cart.restaurant.open) throw new IllegalStateException(cart.restaurant.name + " is closed");
    List<Map.Entry<MenuItem, Integer>> reserved = new ArrayList<>();
    for (var line : cart.lines.entrySet()) {
        if (!line.getKey().reserve(line.getValue())) {
            reserved.forEach(r -> r.getKey().release(r.getValue()));
            throw new IllegalStateException(line.getKey().name + " is sold out");
        }
        reserved.add(line);
    }
    Bill bill = pricing.bill(cart, coupon);
    String id = "O" + seq.incrementAndGet();
    if (!payments.charge(id, bill.total())) {
        reserved.forEach(r -> r.getKey().release(r.getValue()));
        throw new IllegalStateException("Payment failed");
    }
    Order o = new Order(id, cart, bill);
    orders.put(id, o);
    publish(o, Actor.CUSTOMER);
    return o;
}
```

Reserve every line; if one is sold out, give back the ones already reserved. If payment fails, give everything back. The same all-or-nothing idea as [movie seats](../07-Movie-Ticket-Booking/MovieBookingSystem.md#45-phase-1-hold-all-seats-or-none).

### 4.6 Cancelling: the refund depends on when

```java
long cancelByCustomer(String id) {
    Order o = order(id);
    OrderStatus before;
    DeliveryAgent toRelease;
    synchronized (o) {                                                      // transition + agent read together
        before = o.transition(OrderStatus.CANCELLED, Actor.CUSTOMER);       // throws once READY/PICKED_UP
        toRelease = o.agent;
    }
    long refund = before == OrderStatus.PLACED ? o.bill.total() : o.bill.total() / 2;
    if (before == OrderStatus.PLACED) o.lines.forEach(MenuItem::release);      // nothing cooked yet: stock back
    if (toRelease != null) toRelease.release();
    payments.refund(o.id, refund);
    o.refundPaise = refund;
    publish(o, Actor.CUSTOMER);
    return refund;
}
```

| Cancelled from | Refund | Stock returned? |
|---|---|---|
| PLACED | 100% | yes |
| ACCEPTED / PREPARING | 50% | no (the food is being made) |
| READY or later | not allowed | |

### 4.7 Assigning an agent, safely

```java
private void assignAgent(Order o) {
    for (DeliveryAgent a : assignment.rank(o, agents.values())) {
        if (!a.tryClaim()) continue;                                     // CAS: one order per agent
        synchronized (o) {
            OrderStatus st = o.status();
            if (st == OrderStatus.ACCEPTED || st == OrderStatus.PREPARING) { o.agent = a; return; }
        }
        a.release();                                                     // order was cancelled meanwhile
        return;
    }
}
```

1. Rank the available agents by distance to the restaurant.
2. **Claim** one with a CAS (AVAILABLE → BUSY), so two orders can't take the same agent.
3. Under the **order's lock**, re-check that the order is still active, and only then attach the agent.
4. If the order was cancelled in between, **release** the agent.

### 4.8 The event bus

```java
static class OrderEventBus {
    private final Map<String, List<Consumer<OrderEvent>>> subscribers = new ConcurrentHashMap<>();
    void subscribe(String topic, Consumer<OrderEvent> c) { ... }
    void publish(String topic, OrderEvent e) { subscribers.getOrDefault(topic, List.of()).forEach(c -> c.accept(e)); }
}
```

Every status change is published to `customer:<id>`, `restaurant:<id>` and (once assigned) `agent:<id>`. Each app subscribes to its own topic, like channels on a radio.

---

## 5. Dry run (from the real output)

Meghana Foods is at (0, 0); Asha is at (3, 4), i.e. 5 km away. The `near` agent is 0.5 km from the restaurant, the `far` agent 4 km.

### The bill: 2 biryanis with WELCOME50 (50% off, max ₹100, min order ₹150)

| Line | Amount |
|---|---|
| items: 2 × ₹300 | ₹600 |
| discount: 50% = ₹300, capped | −₹100 |
| delivery: ₹20 + 5 km × ₹8 | ₹60 |
| tax: 5% of (600 − 100) | ₹25 |
| **total** | **₹585** |

FLAT75 needs a ₹500 minimum, so it's "not applicable" to a single biryani. Adding an item from Truffles to Meghana's cart is rejected.

### The full lifecycle

| Step | Actor | Result |
|---|---|---|
| place | customer | PLACED |
| the customer tries ACCEPTED | customer | "CUSTOMER cannot move order O1 from PLACED to ACCEPTED" |
| accept | restaurant | ACCEPTED; the **nearest agent** (`a-near`) is assigned |
| prepare, ready | restaurant | PREPARING, READY |
| `a-far` tries to pick up | agent | "a-far is not assigned to O1" |
| pick up | `a-near` | PICKED_UP |
| the customer tries to cancel | customer | "CUSTOMER cannot move … from PICKED_UP to CANCELLED" |
| deliver | `a-near` | DELIVERED; the agent is AVAILABLE again |

Asha's feed saw every step: PLACED, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED.

### Refunds (a single biryani: ₹300 + ₹60 + ₹15 = ₹375)

| Case | Refund |
|---|---|
| cancel before acceptance | ₹375 (full) |
| cancel after acceptance | ₹187.50 (half), and the agent is released |
| the restaurant never answers | the system rejects it: ₹375 |

### Races
- **Stock:** 10 customers order the chef's special (2 portions) at once → exactly 2 orders, stock ends at 0.
- **Cancel vs accept, 500 rounds:** the refund always matched who won (in this run cancel won 39 times, accept 461), and **no agent was left BUSY**.

---

## 6. Concurrency, explained simply

### The stock race
CAS loop on the stock count: never below zero, no locks.

### Two orders, one agent
CAS on the agent's status: only one order can claim an agent.

### The agent-leak bug (a real one, found by the race test)

Without the re-check in `assignAgent`, this interleaving happened:

```
restaurant thread:  PLACED → ACCEPTED
customer thread:    ACCEPTED → CANCELLED; looks at order.agent → null, so releases nobody
restaurant thread:  claims agent X for the order (now CANCELLED) → X is BUSY forever
```

Agent X would never get another order. **The fix:** after claiming the agent, take the **order's lock**, re-check that the order is still ACCEPTED or PREPARING, and only then attach the agent; otherwise release the agent. The customer's cancel reads `o.agent` under the same lock. So either the cancel sees the agent (and releases it), or the assignment sees the cancel (and releases it). The 500-round test confirms no agent is ever leaked.

**General lesson:** when two actors can act on the same object at the same time, **check the state again under the lock** right before you commit.

---

## 7. Edge cases and common mistakes

**Edge cases handled:** an empty cart, a closed restaurant, items from two restaurants, sold-out items, failed payments (stock returned), invalid or inapplicable coupons, the wrong agent picking up, cancelling too late, the restaurant not responding.

**Common mistakes:**
1. A state machine without actors (anyone can move the order anywhere).
2. Status checks scattered across methods instead of one table.
3. Stock decremented with a plain `get` then `set` (oversold).
4. Assigning an agent without re-checking the order's status (the leak above).
5. Returning stock on a late cancel (the food was already made).
6. Refunding a fixed amount regardless of when the cancel happened.

---

## 8. Interview questions and answers

**Q1. Why put the actor in the state machine?**
> Different parties are responsible for different steps. Encoding (from, to, actor) in one table means no actor can push the order into a state it isn't responsible for, and the whole policy is reviewable in one place.

**Q2. How do you stop overselling limited items?**
> An atomic stock counter with a CAS loop (or a conditional `UPDATE … SET stock = stock - ? WHERE stock >= ?`). Reserve all lines; roll back on any failure.

**Q3. What race did you find, and how did you fix it?**
> A customer's cancel racing with the restaurant's accept could leave an agent claimed for a cancelled order. The fix: claim the agent, then re-check the order status under the order's lock before attaching; the cancel reads the agent under the same lock. One of the two always sees the other.

**Q4. How do refunds depend on the state?**
> Full refund before acceptance (and stock returned); 50% after acceptance (the food is being prepared); no customer cancellation after pickup. The status before the cancel decides.

**Q5. How do all the apps get updates?**
> An event bus with topics per customer, restaurant and agent. In production, events go to a broker (Kafka) and are pushed to devices over WebSockets or push notifications.

**Q6. What happens if no agent is free when the restaurant accepts?**
> The order stays ACCEPTED without an agent; in production a retry timer, or the event "an agent became free", triggers assignment again.

### Follow-up questions

**F1. "Batch two orders from the same restaurant onto one agent."**
> Let an agent hold several orders with a route; assignment considers agents already heading to that restaurant with spare capacity.

**F2. "Estimated delivery time."**
> Preparation time per restaurant + the agent's travel time to the restaurant + travel time to the customer, updated on each event.

**F3. "Scale this."**
> Separate services (catalog, cart, order, dispatch, payment) communicating through events; the order's state transitions become conditional updates on a version column; stock reservations live in the restaurant service's database.

**F4. "Restaurants can mark an item unavailable mid-day."**
> Set its stock to 0 (or a flag); new reservations fail; existing orders are unaffected.

**F5. "Apply the best coupon automatically."**
> Evaluate every applicable `DiscountRule` and take the one with the largest discount.

---

## 9. Running the file

```bash
java FoodDeliverySystem.java      # 18/18 tests passed (prints how often cancel vs accept won the race)
```

**One-line summary:** *"The order lifecycle is one (from, to, actor) table so each party can only make its own moves; stock is reserved all-or-nothing with CAS; agents are claimed by CAS and attached only after re-checking the order under its lock (fixing a real agent-leak race); refunds depend on the status before cancelling; and an event bus keeps every app updated."*
