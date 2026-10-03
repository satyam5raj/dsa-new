/*
 * ============================================================================
 *  HLD 09 : NEWS FEED (Twitter / Instagram / Facebook)  - fan-out strategies measured
 * ============================================================================
 *  Run:  java NewsFeedHld.java          (the full design answer: README.md)
 *
 *     [graph]     a power-law social graph: most users have few followers, a few
 *                 CELEBRITIES have a huge share
 *     [push]      FAN-OUT ON WRITE: on post, insert the post id into every follower's
 *                 precomputed timeline. Reads are O(1). Writes explode for celebrities.
 *     [pull]      FAN-OUT ON READ: on feed load, fetch recent posts of everyone you follow and
 *                 k-way MERGE them. Writes are O(1). Reads are expensive.
 *     [hybrid]    push for normal authors, pull for celebrities (and skip inactive users)
 *     [check]     all three produce the SAME feed; costs differ by orders of magnitude
 *     [ranking]   candidates -> score (recency decay x affinity) -> top N
 * ============================================================================
 */

import java.util.*;

public class NewsFeedHld {

    record Post(long id, int author, long ts) {}

    static final int USERS = 20_000, FEED_SIZE = 50, CELEBRITY_THRESHOLD = 1_000;

    // =====================================================================
    // [graph]
    // =====================================================================

    static final List<Set<Integer>> following = new ArrayList<>(), followers = new ArrayList<>();

    /** Each user follows 2-8 of the 30 "famous" accounts plus 40-80 ordinary users (power-law-ish). */
    static void buildGraph(Random rnd) {
        for (int i = 0; i < USERS; i++) { following.add(new HashSet<>()); followers.add(new HashSet<>()); }
        for (int u = 0; u < USERS; u++) {
            int famous = 2 + rnd.nextInt(7), normal = 40 + rnd.nextInt(41);
            for (int k = 0; k < famous; k++) follow(u, rnd.nextInt(30));
            for (int k = 0; k < normal; k++) follow(u, 30 + rnd.nextInt(USERS - 30));
        }
    }

    static void follow(int u, int target) { if (target != u && following.get(u).add(target)) followers.get(target).add(u); }

    static boolean isCelebrity(int u) { return followers.get(u).size() >= CELEBRITY_THRESHOLD; }

    // =====================================================================
    // strategies
    // =====================================================================

    interface FeedStrategy {
        void onPost(Post p);
        List<Long> feed(int user);
        long writeCost();                               // timeline inserts
        long readCost();                                // posts examined while building feeds
        default long sources() { return 0; }            // lists fetched per feed load (fan-in -> latency)
    }

    /** All posts by author, newest last (the "user timeline" table). */
    static final Map<Integer, List<Post>> postsByAuthor = new HashMap<>();

    static List<Post> recentOf(int author, int n) {
        List<Post> l = postsByAuthor.getOrDefault(author, List.of());
        return l.subList(Math.max(0, l.size() - n), l.size());
    }

    /** PUSH: precomputed home timelines (Redis lists capped at FEED_SIZE in production). */
    static final class Push implements FeedStrategy {
        final Map<Integer, Deque<Post>> timelines = new HashMap<>();
        long writes, reads;
        public void onPost(Post p) {
            for (int f : followers.get(p.author())) {
                Deque<Post> t = timelines.computeIfAbsent(f, k -> new ArrayDeque<>());
                t.addFirst(p); writes++;
                if (t.size() > FEED_SIZE) t.removeLast();
            }
        }
        long fetches;
        public List<Long> feed(int u) { fetches++; Deque<Post> t = timelines.getOrDefault(u, new ArrayDeque<>()); reads += t.size(); return t.stream().map(Post::id).toList(); }
        public long writeCost() { return writes; }
        public long readCost() { return reads; }
        public long sources() { return fetches; }
    }

    /** PULL: merge the recent posts of everyone I follow (k-way merge with a heap). */
    static final class Pull implements FeedStrategy {
        long reads, fetches;
        public void onPost(Post p) { }
        public List<Long> feed(int u) { fetches += following.get(u).size(); return merge(following.get(u), FEED_SIZE, this); }
        public long writeCost() { return 0; }
        public long readCost() { return reads; }
        public long sources() { return fetches; }

        static List<Long> merge(Collection<Integer> authors, int n, Pull counter) {
            PriorityQueue<Post> heap = new PriorityQueue<>(Comparator.comparingLong(Post::ts).reversed().thenComparing(Comparator.comparingLong(Post::id).reversed()));
            for (int a : authors) for (Post p : recentOf(a, n)) { heap.add(p); if (counter != null) counter.reads++; }
            List<Long> out = new ArrayList<>();
            while (!heap.isEmpty() && out.size() < n) out.add(heap.poll().id());
            return out;
        }
    }

    /** HYBRID: push for normal authors; celebrities' posts are pulled at read time and merged in. */
    static final class Hybrid implements FeedStrategy {
        final Push push = new Push();
        long extraReads, fetches;
        public void onPost(Post p) { if (!isCelebrity(p.author())) push.onPost(p); }
        public long sources() { return fetches; }
        public List<Long> feed(int u) {
            fetches += 1 + following.get(u).stream().filter(NewsFeedHld::isCelebrity).count();
            Deque<Post> pre = push.timelines.getOrDefault(u, new ArrayDeque<>());
            List<Post> candidates = new ArrayList<>(pre);
            push.reads += pre.size();
            // Only celebrity posts NEWER than the oldest precomputed entry can make the top 50 (when it's full):
            // walk each celebrity's timeline from the newest and stop at the cutoff.
            long cutoff = pre.size() == FEED_SIZE ? pre.peekLast().ts() : Long.MIN_VALUE;
            for (int c : following.get(u)) if (isCelebrity(c)) {
                List<Post> r = recentOf(c, FEED_SIZE);
                for (int i = r.size() - 1; i >= 0 && r.get(i).ts() > cutoff; i--) { candidates.add(r.get(i)); extraReads++; }
            }
            candidates.sort(Comparator.comparingLong(Post::ts).reversed().thenComparing(Comparator.comparingLong(Post::id).reversed()));
            return candidates.stream().limit(FEED_SIZE).map(Post::id).toList();
        }
        public long writeCost() { return push.writes; }
        public long readCost() { return push.reads + extraReads; }
    }

    // =====================================================================
    // [ranking]
    // =====================================================================

    /** score = affinity(viewer, author) * 0.5^(age / halfLife). Real systems: ML models over many features. */
    static List<Long> ranked(List<Post> candidates, Map<Integer, Double> affinity, long now, long halfLife, int n) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble((Post p) -> -affinity.getOrDefault(p.author(), 0.1) * Math.pow(0.5, (double) (now - p.ts()) / halfLife)))
                .limit(n).map(Post::id).toList();
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        Random rnd = new Random(1);
        buildGraph(rnd);
        long celebs = java.util.stream.IntStream.range(0, USERS).filter(NewsFeedHld::isCelebrity).count();
        int maxFollowers = followers.stream().mapToInt(Set::size).max().getAsInt();
        System.out.printf("Graph: %,d users, %d celebrities (>= %,d followers), biggest account %,d followers%n", USERS, celebs, CELEBRITY_THRESHOLD, maxFollowers);

        FeedStrategy push = new Push(), pull = new Pull(), hybrid = new Hybrid();
        long ts = 0, id = 0;
        for (int i = 0; i < 20_000; i++) {                                 // posts: authors uniformly random
            int author = rnd.nextInt(USERS);
            if (i % 10 == 0) author = rnd.nextInt(20);                     // celebrities post often too
            Post p = new Post(++id, author, ++ts);
            postsByAuthor.computeIfAbsent(author, k -> new ArrayList<>()).add(p);
            push.onPost(p); pull.onPost(p); hybrid.onPost(p);
        }
        List<Integer> viewers = new ArrayList<>();
        for (int i = 0; i < 2_000; i++) viewers.add(rnd.nextInt(USERS));
        boolean same = true;
        for (int v : viewers) {
            List<Long> a = push.feed(v), b = pull.feed(v), c = hybrid.feed(v);
            same &= a.equals(b) && b.equals(c);
        }

        System.out.println("1. Same feeds, different costs (20,000 posts, 2,000 feed loads)");
        for (var e : List.of(Map.entry("push", push), Map.entry("pull", pull), Map.entry("hybrid", hybrid)))
            System.out.printf("   %-7s timeline writes %,10d   lists fetched per feed load %5.1f   posts read %,9d%n",
                    e.getKey(), e.getValue().writeCost(), e.getValue().sources() / 2000.0, e.getValue().readCost());
        check("all three strategies return identical feeds", true, same);
        check("push: 1 list per feed load (precomputed)", 1.0, push.sources() / 2000.0);
        check("pull: no write amplification, but fan-in = every followee", true, pull.writeCost() == 0 && pull.sources() / 2000.0 > 40);
        check("hybrid: ~7x fewer timeline writes than push", true, hybrid.writeCost() * 5 < push.writeCost());
        check("hybrid: fan-in = 1 + celebrities followed (<= 9)", true, hybrid.sources() / 2000.0 <= 9);
        System.out.println("2. The celebrity problem in one number");
        int celeb = 0;
        for (int u = 0; u < USERS; u++) if (followers.get(u).size() > followers.get(celeb).size()) celeb = u;
        System.out.printf("   one post by user %d = %,d timeline writes with push (real world: 100M+)%n", celeb, followers.get(celeb).size());
        check("biggest account is a celebrity -> pulled, not pushed", true, isCelebrity(celeb));

        System.out.println("3. Ranking re-orders candidates");
        long now = 1_000;
        List<Post> cands = List.of(new Post(1, 7, 990), new Post(2, 8, 900), new Post(3, 9, 999));
        Map<Integer, Double> affinity = Map.of(7, 0.2, 8, 0.9, 9, 0.1);    // you interact with author 8 a lot
        check("chronological", "[3, 1, 2]", cands.stream().sorted(Comparator.comparingLong(Post::ts).reversed()).map(Post::id).toList());
        check("ranked: close friend's older post first", "[2, 1, 3]", ranked(cands, affinity, now, 200, 3));
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
