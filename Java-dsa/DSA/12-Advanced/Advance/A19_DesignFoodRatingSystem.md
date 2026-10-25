# A19 · Design a Food Rating System ⭐

| | |
|---|---|
| **LeetCode** | [#2353 Design a Food Rating System](https://leetcode.com/problems/design-a-food-rating-system/) |
| **Difficulty** | Medium |
| **Mix of structures** | **HashMaps** (food → rating, food → cuisine) + a **TreeSet per cuisine** (or a heap with lazy deletion) |
| **Code** | [A19_DesignFoodRatingSystem.java](A19_DesignFoodRatingSystem.java) |
| **Run** | `java A19_DesignFoodRatingSystem.java` |

⭐ = not in `leetcode.md`, added because "a sorted set per group, with updates" shows up in many design questions.

---

## 1. The problem

- `FoodRatings(foods, cuisines, ratings)`: food i belongs to `cuisines[i]` and has rating `ratings[i]`.
- `changeRating(food, newRating)`.
- `highestRated(cuisine)`: the food with the highest rating in that cuisine; on a tie, the **lexicographically smaller** name.

```
foods    kimchi  miso      sushi     moussaka  ramen     bulgogi
cuisine  korean  japanese  japanese  greek     japanese  korean
rating   9       12        8         15        14        7

highestRated("korean")   → kimchi
highestRated("japanese") → ramen
changeRating("sushi", 16)
highestRated("japanese") → sushi
changeRating("ramen", 16)
highestRated("japanese") → ramen      (both 16; "ramen" < "sushi")
```

---

## 2. How to think about it

### Three questions, three maps
| Question | Structure |
|---|---|
| "What's this food's rating right now?" | `rating: food → int` |
| "Which cuisine is it in?" | `cuisineOf: food → cuisine` |
| "What's the best food in this cuisine?" | `byCuisine: cuisine → TreeSet<Dish>` sorted by (rating desc, name asc) |

`highestRated` is `first()` of the cuisine's set.

### Updating a sorted set correctly
`changeRating(food, r)` must **move** the food inside its cuisine's set:

```
set.remove(new Dish(oldRating, food))    ← needs the old rating: that's what the rating map is for
set.add(new Dish(newRating, food))
rating.put(food, newRating)
```

### The trap: never change an element while it's inside a TreeSet
A `TreeSet` places each element according to the comparator **at insertion time**. If an element's rating changed while inside (say the `Dish` were mutable, or the comparator read the rating map), the tree would be ordered wrongly, and `remove` would search the wrong branch and fail silently. Using **immutable** records, and remove-then-add, avoids this completely.

### Alternative: a heap per cuisine with lazy deletion
Push `(newRating, food)` on each change and leave the old entry in the heap. In `highestRated`, pop tops whose rating no longer matches the rating map (the same trick as [A02](A02_StockPriceFluctuation.md)).

### Dry run: the japanese set
| Step | TreeSet (best first) | `first()` |
|---|---|---|
| start | (14 ramen) (12 miso) (8 sushi) | ramen |
| sushi → 16 | remove (8 sushi), add (16 sushi) → (16 sushi) (14 ramen) (12 miso) | sushi |
| ramen → 16 | remove (14 ramen), add (16 ramen) → (16 ramen) (16 sushi) (12 miso) | ramen |

---

## 3. Java concepts used in this problem

### 3.1 A comparator with a tie-break
```java
static final Comparator<Dish> BEST_FIRST = (a, b) ->
        a.rating() != b.rating() ? Integer.compare(b.rating(), a.rating()) : a.name().compareTo(b.name());
```
Rating **descending** (`compare(b, a)`), then name **ascending** (`String.compareTo`). One comparator serves both the TreeSet and the heap.

### 3.2 How `TreeSet.remove` finds an element
A `TreeSet` doesn't use `equals`/`hashCode`; it walks the tree with the **comparator** and removes the element that compares as 0. `new Dish(oldRating, food)` compares equal to the stored entry (same rating, same name), so it's found, even though it's a different object.

### 3.3 Records are immutable
`record Dish(int rating, String name)` has final fields: once inside a set, it can't change. To "change" it, remove it and add a new one.

### 3.4 `computeIfAbsent` with a sorted set
```java
byCuisine.computeIfAbsent(cuisines[i], k -> new TreeSet<>(BEST_FIRST)).add(new Dish(ratings[i], foods[i]));
```
It creates the cuisine's set (with the comparator) on first use, then adds.

---

## 4. Approach 1: Scan the cuisine's foods (the referee)

### Code ([A19_DesignFoodRatingSystem.java:54-71](A19_DesignFoodRatingSystem.java#L54-L71))
```java
static class ScanFoodRatings {
    private final Map<String, Integer> rating = new HashMap<>();
    private final Map<String, String> cuisineOf = new HashMap<>();

    ScanFoodRatings(String[] foods, String[] cuisines, int[] ratings) {
        for (int i = 0; i < foods.length; i++) { rating.put(foods[i], ratings[i]); cuisineOf.put(foods[i], cuisines[i]); }
    }
    public void changeRating(String food, int newRating) { rating.put(food, newRating); }
    public String highestRated(String cuisine) {
        Dish best = null;
        for (String f : rating.keySet())
            if (cuisineOf.get(f).equals(cuisine)) {
                Dish d = new Dish(rating.get(f), f);
                if (best == null || BEST_FIRST.compare(d, best) < 0) best = d;
            }
        return best.name();
    }
}
```

`changeRating` is O(1), but `highestRated` scans every food: **O(n)**.

---

## 5. Approach 2: A max-heap per cuisine + lazy deletion

### Code ([A19_DesignFoodRatingSystem.java:74-95](A19_DesignFoodRatingSystem.java#L74-L95))
```java
static class HeapFoodRatings {
    private final Map<String, Integer> rating = new HashMap<>();
    private final Map<String, String> cuisineOf = new HashMap<>();
    private final Map<String, PriorityQueue<Dish>> byCuisine = new HashMap<>();

    HeapFoodRatings(String[] foods, String[] cuisines, int[] ratings) {
        for (int i = 0; i < foods.length; i++) {
            rating.put(foods[i], ratings[i]);
            cuisineOf.put(foods[i], cuisines[i]);
            byCuisine.computeIfAbsent(cuisines[i], k -> new PriorityQueue<>(BEST_FIRST)).add(new Dish(ratings[i], foods[i]));
        }
    }
    public void changeRating(String food, int newRating) {
        rating.put(food, newRating);
        byCuisine.get(cuisineOf.get(food)).add(new Dish(newRating, food));   // the old entry stays, now stale
    }
    public String highestRated(String cuisine) {
        PriorityQueue<Dish> heap = byCuisine.get(cuisine);
        while (heap.peek().rating() != rating.get(heap.peek().name())) heap.poll();
        return heap.peek().name();
    }
}
```

| Line | What it does |
|---|---|
| `changeRating` | push the new entry; don't search for the old one |
| the `while` | the top is stale if the food's real rating differs: discard it |

`changeRating` is O(log n); `highestRated` is amortised O(log n). The heap grows with every change.

---

## 6. Approach 3: A TreeSet per cuisine (main solution)

### Code ([A19_DesignFoodRatingSystem.java:98-119](A19_DesignFoodRatingSystem.java#L98-L119))
```java
static class FoodRatings {
    private final Map<String, Integer> rating = new HashMap<>();
    private final Map<String, String> cuisineOf = new HashMap<>();
    private final Map<String, TreeSet<Dish>> byCuisine = new HashMap<>();

    FoodRatings(String[] foods, String[] cuisines, int[] ratings) {
        for (int i = 0; i < foods.length; i++) {
            rating.put(foods[i], ratings[i]);
            cuisineOf.put(foods[i], cuisines[i]);
            byCuisine.computeIfAbsent(cuisines[i], k -> new TreeSet<>(BEST_FIRST)).add(new Dish(ratings[i], foods[i]));
        }
    }

    public void changeRating(String food, int newRating) {
        TreeSet<Dish> set = byCuisine.get(cuisineOf.get(food));
        set.remove(new Dish(rating.get(food), food));                  // rebuild the exact old entry to remove it
        set.add(new Dish(newRating, food));
        rating.put(food, newRating);
    }

    public String highestRated(String cuisine) { return byCuisine.get(cuisine).first().name(); }
}
```

### Line by line
| Line | What it does |
|---|---|
| the constructor | fill all three maps; each cuisine's set is ordered best-first |
| `cuisineOf.get(food)` | which set to update |
| `remove(new Dish(old, food))` | the old rating comes from the `rating` map |
| `add(new Dish(newRating, food))` | re-insert at the right position |
| `rating.put` | update the source of truth **after** using the old value |
| `first()` | the best dish of the cuisine |

### Complexity
- `changeRating`: **O(log n)**. `highestRated`: **O(log n)** (`first()` walks down the tree).
- Space: **O(n)**, with no stale entries.

---

## 7. Comparing the approaches

| | changeRating | highestRated | Memory |
|---|---|---|---|
| Scan | O(1) | O(n) | O(n) |
| Heap + lazy | O(log n) | amortised O(log n) | grows with changes |
| **TreeSet** | O(log n) | O(log n) | O(n) |

The file runs 60,000 random operations and checks all three agree.

---

## 8. Edge cases and common mistakes

**Edge cases:** ties on rating (smaller name wins), a rating that goes down, changing a food back to an old rating (the heap then has two "valid" entries, which is harmless), a cuisine with one food.

**Mistakes:**
1. Updating `rating` **before** removing the old entry (you lose the old rating needed for `remove`).
2. A TreeSet whose comparator reads a mutable map, so updates corrupt the tree.
3. Comparing names in the wrong direction, or ratings ascending.
4. A `TreeSet<String>` of names only: equal ratings between different foods then aren't ordered properly.
5. Comparing two `Integer` objects with `!=` (it compares identity). In the heap check, `heap.peek().rating()` is an `int`, so `rating.get(...)` is unboxed and compared by value, which is safe. With two `Integer`s, use `equals`.

---

## 9. Interview questions and answers

**Q1. Why a TreeSet per cuisine?**
> It keeps each cuisine's foods sorted by (rating desc, name asc), so the best is `first()`, and an update is a remove plus an add, both O(log n).

**Q2. Why do you need the food → rating map?**
> To build the exact old entry for `TreeSet.remove`, which finds elements with the comparator. Without the old rating you can't locate it.

**Q3. Heap or TreeSet?**
> The TreeSet removes stale entries immediately (bounded memory). The heap with lazy deletion is simpler to update but accumulates stale entries. Both are O(log n).

### Follow-up questions

**F1. "Top 3 foods of a cuisine."**
> Iterate the TreeSet from `first()` and take 3: O(log n + 3).

**F2. "Remove a food."**
> Remove its entry from its cuisine's set, and delete it from both maps.

**F3. "Similar problems?"**
> Design a Number Container System (2349) and Design Movie Rental System (1912): a HashMap to find things, plus a sorted set per group.

---

## 10. Running the file

```bash
java A19_DesignFoodRatingSystem.java     # 6/6 tests passed (LeetCode example, tie, rating going down, 60,000 random ops)
```

**One-line summary:** *"HashMaps give each food's rating and cuisine; each cuisine has a TreeSet of immutable (rating, name) records ordered best-first, so changeRating removes the old record and adds the new one, and highestRated is first()."*
