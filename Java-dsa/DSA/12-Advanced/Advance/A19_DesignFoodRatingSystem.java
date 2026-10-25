/*
 * ============================================================================
 *  ADVANCE 19 : DESIGN A FOOD RATING SYSTEM  ⭐ bonus   LeetCode #2353
 *  https://leetcode.com/problems/design-a-food-rating-system/
 *  Pattern: HashMaps (food -> rating, food -> cuisine) + a TreeSet PER CUISINE
 * ============================================================================
 *
 *  PROBLEM
 *    FoodRatings(foods, cuisines, ratings)
 *    changeRating(food, newRating)
 *    highestRated(cuisine) -> the food with the highest rating in that cuisine;
 *                             ties -> the lexicographically smaller name
 *      foods    kimchi miso     sushi    moussaka ramen    bulgogi
 *      cuisine  korean japanese japanese greek    japanese korean
 *      rating   9      12       8        15       14       7
 *      highestRated(korean) = kimchi    highestRated(japanese) = ramen
 *      changeRating(sushi, 16)  -> japanese = sushi
 *      changeRating(ramen, 16)  -> japanese = ramen   (16 tie, "ramen" < "sushi")
 *
 *  ---------------------------------------------------------------------------
 *  THREE QUESTIONS
 *    "what is this food's rating?"            HashMap food -> rating
 *    "which cuisine is this food in?"         HashMap food -> cuisine
 *    "best food of a cuisine, kept sorted"    HashMap cuisine -> TreeSet<Dish>,
 *                                             ordered (rating desc, name asc)
 *  changeRating = remove the OLD (rating, name) entry from its cuisine's set, insert the
 *  new one. This is why we need the rating map: to rebuild the exact old entry.
 *
 *  THE TRAP: a TreeSet finds entries using its comparator. If you change an entry's
 *  rating while it's inside the set, the set is now sorted wrongly and remove() can't
 *  find it. Entries here are immutable records: remove the old one, add a new one.
 *
 *  ALTERNATIVE: a max-heap per cuisine with LAZY DELETION - push the new entry and skip
 *  tops whose rating no longer matches the rating map (like A02).
 *
 *  DRY RUN japanese set: (14 ramen) (12 miso) (8 sushi)        first = ramen
 *    changeRating(sushi,16): remove (8 sushi), add (16 sushi) -> (16 sushi) (14 ramen) (12 miso)
 *    changeRating(ramen,16): remove (14 ramen), add (16 ramen) -> (16 ramen) (16 sushi) (12 miso)
 *
 *  COMPLEXITY   changeRating / highestRated O(log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A19_DesignFoodRatingSystem {

    record Dish(int rating, String name) {}

    static final Comparator<Dish> BEST_FIRST = (a, b) ->
            a.rating() != b.rating() ? Integer.compare(b.rating(), a.rating()) : a.name().compareTo(b.name());

    // ---------------- Approach 1: scan the cuisine's foods (the referee) ----------------
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

    // ---------------- Approach 2: a max-heap per cuisine + lazy deletion ----------------
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

    // ---------------- Approach 3: a TreeSet per cuisine (main solution) ----------------
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

    public static void main(String[] args) {
        String[] foods = {"kimchi", "miso", "sushi", "moussaka", "ramen", "bulgogi"};
        String[] cuisines = {"korean", "japanese", "japanese", "greek", "japanese", "korean"};
        int[] ratings = {9, 12, 8, 15, 14, 7};
        FoodRatings fr = new FoodRatings(foods, cuisines, ratings);
        check("korean", "kimchi", fr.highestRated("korean"));
        check("japanese", "ramen", fr.highestRated("japanese"));
        fr.changeRating("sushi", 16);
        check("after sushi -> 16", "sushi", fr.highestRated("japanese"));
        fr.changeRating("ramen", 16);
        check("tie on 16: ramen < sushi", "ramen", fr.highestRated("japanese"));
        fr.changeRating("ramen", 1);
        check("a rating can go down", "sushi", fr.highestRated("japanese"));

        Random rnd = new Random(19);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(10);
            String[] f = new String[n], c = new String[n];
            int[] r = new int[n];
            for (int i = 0; i < n; i++) { f[i] = "f" + i; c[i] = "c" + rnd.nextInt(3); r[i] = 1 + rnd.nextInt(5); }
            FoodRatings main = new FoodRatings(f, c, r);
            HeapFoodRatings heap = new HeapFoodRatings(f, c, r);
            ScanFoodRatings ref = new ScanFoodRatings(f, c, r);
            for (int i = 0; i < 200; i++) {
                if (rnd.nextBoolean()) {
                    String food = f[rnd.nextInt(n)];
                    int nr = 1 + rnd.nextInt(5);
                    main.changeRating(food, nr); heap.changeRating(food, nr); ref.changeRating(food, nr);
                } else {
                    String cuisine = c[rnd.nextInt(n)];
                    String want = ref.highestRated(cuisine);
                    ok &= main.highestRated(cuisine).equals(want) && heap.highestRated(cuisine).equals(want);
                }
            }
        }
        check("60,000 random ops: TreeSet == heap == scan", true, ok);
        summary();
    }

    // ---------------- tiny test helper (same in every problem file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-34s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
