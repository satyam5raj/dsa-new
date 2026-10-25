# M04 · Design Twitter ⭐

| | |
|---|---|
| **LeetCode** | [#355 Design Twitter](https://leetcode.com/problems/design-twitter/) |
| **Difficulty** | Medium |
| **Pattern** | Per-user tweet lists + a **k-way merge** with a heap for the news feed |
| **Code** | [M04_DesignTwitter.java](M04_DesignTwitter.java) |
| **Run** | `java M04_DesignTwitter.java` |

⭐ = not in `leetcode.md`, added for a key technique. Design-problem basics are explained in [E01](../02-Easy/E01_LoggerRateLimiter.md#3-java-concepts-used-in-this-problem); `PriorityQueue` basics are in [Heap E01](../../08-Heap/02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Design a simplified Twitter:
- `postTweet(userId, tweetId)`;
- `follow(followerId, followeeId)` and `unfollow(followerId, followeeId)`;
- `getNewsFeed(userId)`: the **10 most recent** tweet ids posted by the user or anyone they follow, newest first.

| call | result |
|---|---|
| postTweet(1, 5) | |
| getNewsFeed(1) | [5] |
| follow(1, 2) | |
| postTweet(2, 6) | |
| getNewsFeed(1) | [6, 5] |
| unfollow(1, 2) | |
| getNewsFeed(1) | [5] |

---

## 2. How to think about it

### The data
```
tweets:  user → list of {time, tweetId}       (appended, so already sorted by time)
follows: user → set of followees
clock:   a global counter, so no two tweets share a time
```

### The news feed is "merge k sorted lists", but only the top 10
Each relevant user's tweet list is sorted by time. We want the 10 newest across all of them, so we don't need to merge everything:

```
push the NEWEST tweet of each relevant user into a max-heap by time
repeat up to 10 times:
    pop the newest; add it to the feed
    push the next-older tweet of THAT user (if there is one)
```

This is the k-way merge from [Merge K Sorted Lists](../../03-Linked-Lists/04-Hard/H02_MergeKSortedLists.md) (pattern 2 in [HeapPatterns](../../08-Heap/01-Basics/HeapPatterns.java)). With k relevant users it costs O(k + 10 log k), and we never touch old tweets.

### A system-design note
Real Twitter can't merge at read time for someone following thousands of accounts. It uses **fan-out on write** (each new tweet is pushed into every follower's precomputed timeline) for most users, and merge-on-read only for celebrities with millions of followers. This problem is the pure "pull" model.

### Dry run (from a real run)
Posts in order: user 1 → 101 (t0), user 2 → 201 (t1), user 1 → 102 (t2), user 2 → 202 (t3), user 3 → 301 (t4). User 1 follows user 2.

| step | heap (time:tweet) | pop | push | feed |
|---|---|---|---|---|
| seed | 3:202 (u2), 2:102 (u1) | | | |
| 1 | | 202 | 1:201 (u2's next-older) | [202] |
| 2 | 2:102, 1:201 | 102 | 0:101 (u1's next-older) | [202, 102] |
| 3 | 1:201, 0:101 | 201 | none | [202, 102, 201] |
| 4 | 0:101 | 101 | none | [202, 102, 201, 101] |

301 never appears: user 1 doesn't follow user 3.

---

## 3. Java concepts used in this problem

### 3.1 Heap entries that remember where they came from

```java
// {time, tweetId, user, index in that user's list}
heap.add(new int[]{t.get(i)[0], t.get(i)[1], u, i});
```

When an entry is popped we need that user's **next-older** tweet, so the entry carries the user and its position in the list. This is the standard trick for k-way merges.

### 3.2 A max-heap with a comparator

```java
new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));
```

Comparing `b` to `a` (reversed) puts the **largest** time on top. `Integer.compare` is used instead of `b[0] - a[0]`, which can overflow for large values.

### 3.3 Copying a set before changing it

```java
Set<Integer> users = new HashSet<>(follows.getOrDefault(userId, Set.of()));
users.add(userId);
```

- `Set.of()` is an empty, **immutable** set, used as the default for a user who follows nobody.
- `new HashSet<>(...)` makes a **copy**, so adding the user themselves doesn't modify the stored follow set (and works even when the default is the immutable `Set.of()`).

### 3.4 Guarding against self-follow

`if (followerId != followeeId)` in `follow` means a user never appears in their own follow set. Their own tweets are added separately in `getNewsFeed`, so `unfollow(1, 1)` can't hide them.

---

## 4. Approach 1: Scan a global log (the referee)

The test keeps one list of every tweet in time order ([M04_DesignTwitter.java:106-112](M04_DesignTwitter.java#L106-L112)) and builds the expected feed by walking it backwards:

```java
class Fragment {
    void expected(List<int[]> log, Map<Integer, Set<Integer>> fol, int u) {
        List<Integer> exp = new ArrayList<>();
        Set<Integer> f = fol.getOrDefault(u, Set.of());
        for (int j = log.size() - 1; j >= 0 && exp.size() < 10; j--)
            if (log.get(j)[0] == u || f.contains(log.get(j)[0])) exp.add(log.get(j)[1]);
    }
}
```

- Newest first, keep the tweets by the user or a followee, stop at 10.
- **O(total tweets)** per feed in the worst case: simple and obviously right.

---

## 5. Approach 2: Per-user lists + heap merge (the file's main solution)

### Code ([M04_DesignTwitter.java:38-80](M04_DesignTwitter.java#L38-L80))

```java
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
```

### Line by line

| Line | What it does |
|---|---|
| `clock++` | a unique, increasing time for each tweet |
| `postTweet` | append to the user's list (it stays sorted) |
| `users` | the followees plus the user |
| the seeding loop | each relevant user's newest tweet goes into the heap |
| the `while` | stop after 10 tweets or when everything is used up |
| `next = top[3] − 1` | the popped user's next-older tweet |
| `follow` | ignore self-follows; create the set on first use |
| `unfollow` | removing a missing followee is a harmless no-op |

### Complexity
- **post, follow, unfollow: O(1).**
- **getNewsFeed: O(k + 10 log k)** for k relevant users.
- **Space O(tweets + follow pairs).**

---

## 6. Comparing the approaches

| | Global log scan | Per-user lists + heap |
|---|---|---|
| getNewsFeed | O(total tweets) | **O(k + 10 log k)** |
| post | O(1) | O(1) |

The file runs 20,000 random operations against the global-log scan and checks every feed.

---

## 7. Edge cases and common mistakes

**Edge cases:** a user with no tweets, following someone with no tweets, following the same user twice, unfollowing someone you don't follow, `unfollow(u, u)`.

**Mistakes:**
1. Forgetting the user's own tweets.
2. Adding the user to their stored follow set (then `unfollow(u, u)` would hide their own tweets).
3. Pushing **all** tweets into the heap (O(n log n) instead of O(k + 10 log k)).
4. Sorting by tweet id instead of time (ids aren't in posting order).
5. `follows.getOrDefault(...).add(userId)` on the default `Set.of()`: it's immutable and throws.

---

## 8. Interview questions and answers

**Q1. Why a heap and not just sorting everyone's tweets?**
> We only need the top 10. Starting from each list's newest tweet and expanding lazily touches at most k + 10 entries.

**Q2. Why a global clock instead of real timestamps?**
> It gives every tweet a unique, strictly increasing time, so there are no ties to break.

**Q3. How would this scale to millions of users?**
> Fan-out on write: when someone tweets, push the id into each follower's cached timeline, so reads are O(1). For celebrities, merge their recent tweets in at read time instead (the hybrid model).

### Follow-up questions

**F1. Return the top N instead of 10.**
> The same loop with `feed.size() < N`: O(k + N log k).

**F2. Merge K Sorted Lists.** → [Linked Lists H02](../../03-Linked-Lists/04-Hard/H02_MergeKSortedLists.md)

**F3. Delete a tweet.**
> Mark it deleted in a set and skip it while building the feed (or remove it from the user's list, which costs O(n) unless you also keep a map from tweet to position).

---

## 9. Running the file

```bash
java M04_DesignTwitter.java      # 5/5 tests passed (including 20,000 random ops vs scanning the global log)
```

**One-line summary:** *"Keep each user's tweets in time order and build the feed with a k-way heap merge that starts from each relevant user's newest tweet and stops after 10."*
