/*
 * ============================================================================
 *  MEDIUM 04 : DESIGN TWITTER  ⭐ bonus       LeetCode #355
 *  https://leetcode.com/problems/design-twitter/
 *  Pattern: per-user tweet lists + K-WAY MERGE with a heap for the news feed
 * ============================================================================
 *
 *  PROBLEM
 *    postTweet(user, tweetId); follow(a, b); unfollow(a, b);
 *    getNewsFeed(user) = the 10 most recent tweet ids from the user and everyone
 *    they follow, newest first.
 *
 *  ---------------------------------------------------------------------------
 *  DATA
 *    tweets:  user -> list of {time, tweetId} (appended -> sorted by time)
 *    follows: user -> set of followees
 *    a global counter as the clock (ties impossible)
 *
 *  NEWS FEED = MERGE K SORTED LISTS, but we only need the TOP 10:
 *    push the NEWEST tweet of each relevant user into a max-heap by time
 *    repeat 10 times: pop the newest; push the next-newest tweet of THAT user
 *  This is HeapPatterns #2 (k-way merge) from 08-Heap. Cost O(k + 10 log k) for k
 *  followees - we never touch old tweets.
 *
 *  SYSTEM DESIGN NOTE (HLD preview): real Twitter can't merge at read time for users
 *  following thousands of accounts. It uses "fan-out on write" (push each tweet into
 *  followers' precomputed timelines) for most users, and merge-on-read only for
 *  celebrities with millions of followers. This problem is the "pull" model.
 *
 *  COMPLEXITY   post/follow/unfollow O(1);  getNewsFeed O(k + 10 log k)
 * ============================================================================
 */

import java.util.*;

public class M04_DesignTwitter {

    static class Twitter {
        private int clock = 0;
        private final Map<Integer, List<int[]>> tweets = new HashMap<>();       // user -> {time, id}
        private final Map<Integer, Set<Integer>> follows = new HashMap<>();

        public void postTweet(int userId, int tweetId) {
            tweets.computeIfAbsent(userId, k -> new ArrayList<>()).add(new int[]{clock++, tweetId});
        }

        public List<Integer> getNewsFeed(int userId) {
            Set<Integer> users = new HashSet<>(follows.getOrDefault(userId, Set.of()));
            users.add(userId);                                                  // your own tweets too
            // heap entries {time, tweetId, user, index in that user's list}; newest first
            PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));
            for (int u : users) {
                List<int[]> t = tweets.get(u);
                if (t != null && !t.isEmpty()) {
                    int i = t.size() - 1;
                    heap.add(new int[]{t.get(i)[0], t.get(i)[1], u, i});
                }
            }
            List<Integer> feed = new ArrayList<>();
            while (!heap.isEmpty() && feed.size() < 10) {
                int[] top = heap.poll();
                feed.add(top[1]);
                int next = top[3] - 1;                                          // that user's next-older tweet
                if (next >= 0) {
                    int[] t = tweets.get(top[2]).get(next);
                    heap.add(new int[]{t[0], t[1], top[2], next});
                }
            }
            return feed;
        }

        public void follow(int followerId, int followeeId) {
            if (followerId != followeeId) follows.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
        }

        public void unfollow(int followerId, int followeeId) {
            Set<Integer> s = follows.get(followerId);
            if (s != null) s.remove(followeeId);
        }
    }

    public static void main(String[] args) {
        Twitter tw = new Twitter();
        tw.postTweet(1, 5);
        check("feed(1)", "[5]", tw.getNewsFeed(1));
        tw.follow(1, 2);
        tw.postTweet(2, 6);
        check("feed(1) after following 2", "[6, 5]", tw.getNewsFeed(1));
        tw.unfollow(1, 2);
        check("feed(1) after unfollowing", "[5]", tw.getNewsFeed(1));
        tw.unfollow(1, 1);                                                      // can't unfollow yourself
        check("own tweets always shown", "[5]", tw.getNewsFeed(1));

        // random check vs a global log, filtered
        Random rnd = new Random(5);
        Twitter mine = new Twitter();
        List<int[]> log = new ArrayList<>();                                    // {user, tweetId} in time order
        Map<Integer, Set<Integer>> fol = new HashMap<>();
        boolean ok = true;
        for (int i = 0; i < 20_000; i++) {
            int u = rnd.nextInt(10), v = rnd.nextInt(10);
            switch (rnd.nextInt(4)) {
                case 0 -> { mine.postTweet(u, i); log.add(new int[]{u, i}); }
                case 1 -> { mine.follow(u, v); if (u != v) fol.computeIfAbsent(u, k -> new HashSet<>()).add(v); }
                case 2 -> { mine.unfollow(u, v); if (fol.containsKey(u)) fol.get(u).remove(v); }
                default -> {
                    List<Integer> exp = new ArrayList<>();
                    Set<Integer> f = fol.getOrDefault(u, Set.of());
                    for (int j = log.size() - 1; j >= 0 && exp.size() < 10; j--)
                        if (log.get(j)[0] == u || f.contains(log.get(j)[0])) exp.add(log.get(j)[1]);
                    ok &= mine.getNewsFeed(u).equals(exp);
                }
            }
        }
        check("20,000 random ops vs scanning the global log", true, ok);
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
