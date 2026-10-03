/*
 * ============================================================================
 *  HLD 06 : API DESIGN - pagination, idempotency, concurrency control, limits, versioning
 * ============================================================================
 *  Run:  java ApiDesignPatterns.java          (concepts: README.md)
 *
 *  A tiny in-process "HTTP" layer (Request -> Response with status codes and headers) lets
 *  us show real API behaviour without a server:
 *
 *     [pagination]   OFFSET pagination duplicates/skips items when data changes between pages;
 *                    CURSOR (keyset) pagination doesn't, and costs O(page) not O(offset)
 *     [idempotency]  POST /payments retried after a timeout: duplicate charge without an
 *                    Idempotency-Key; same response replayed with one; key reuse with a
 *                    DIFFERENT body -> 422
 *     [etag]         lost update with read-modify-write; If-Match + ETag -> 412 Precondition Failed
 *     [ratelimit]    429 + Retry-After + X-RateLimit-Remaining (algorithms: LLD 04)
 *     [versioning]   additive changes are safe for tolerant readers; renames need a new version
 *     [errors]       one error shape (RFC 7807 problem+json) everywhere
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class ApiDesignPatterns {

    record Request(String method, String path, Map<String, String> headers, Map<String, String> query, String body) {
        static Request get(String path, Map<String, String> q) { return new Request("GET", path, Map.of(), q, ""); }
        String header(String h) { return headers.getOrDefault(h, null); }
    }

    record Response(int status, Map<String, String> headers, String body) {
        static Response ok(String body) { return new Response(200, Map.of(), body); }
        /** RFC 7807 problem+json: the same error shape for every endpoint. */
        static Response problem(int status, String title, String detail) {
            return new Response(status, Map.of("Content-Type", "application/problem+json"),
                    "{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
        }
    }

    // =====================================================================
    // [pagination]
    // =====================================================================

    record Post(long id, long createdAt, String text) {}

    /** A feed sorted newest first. New posts arrive while the client is paging. */
    static final class FeedApi {
        final List<Post> posts = new ArrayList<>();                    // kept sorted by (createdAt desc, id desc)
        long rowsTouched;
        private long ids;

        void insert(long createdAt) { posts.add(new Post(++ids, createdAt, "post" + ids)); posts.sort(Comparator.comparingLong(Post::createdAt).thenComparingLong(Post::id).reversed()); }

        /** GET /feed?offset=20&limit=10   -> the DB must walk past `offset` rows every time. */
        List<Post> offsetPage(int offset, int limit) {
            rowsTouched += offset + limit;
            return posts.subList(Math.min(offset, posts.size()), Math.min(offset + limit, posts.size()));
        }

        /**
         * GET /feed?cursor=<createdAt>_<id>&limit=10   -> "items strictly older than the last one I saw".
         * SQL: WHERE (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT 10 - an index seek.
         */
        List<Post> cursorPage(String cursor, int limit) {
            long[] c = cursor == null ? new long[]{Long.MAX_VALUE, Long.MAX_VALUE} : Arrays.stream(cursor.split("_")).mapToLong(Long::parseLong).toArray();
            List<Post> out = new ArrayList<>();
            for (Post p : posts) {                                       // a real index seeks directly; we count only rows returned
                boolean older = p.createdAt() < c[0] || (p.createdAt() == c[0] && p.id() < c[1]);
                if (older) { out.add(p); if (out.size() == limit) break; }
            }
            rowsTouched += out.size();
            return out;
        }
        static String cursorOf(Post p) { return p.createdAt() + "_" + p.id(); }     // opaque to clients (base64 it in real APIs)
    }

    // =====================================================================
    // [idempotency]
    // =====================================================================

    static final class PaymentsApi {
        int charges;
        private final Map<String, Response> stored = new HashMap<>();
        private final Map<String, String> requestHash = new HashMap<>();

        Response post(Request r) {
            String key = r.header("Idempotency-Key");
            if (key != null) {
                String hash = Integer.toHexString(r.body().hashCode());
                if (stored.containsKey(key)) {
                    if (!requestHash.get(key).equals(hash)) return Response.problem(422, "Idempotency key reused", "same key, different request body");
                    Response first = stored.get(key);
                    Map<String, String> h = new HashMap<>(first.headers());
                    h.put("Idempotent-Replayed", "true");
                    return new Response(first.status(), h, first.body());   // replay, don't re-execute
                }
                requestHash.put(key, hash);
            }
            charges++;                                                      // the side effect
            Response res = new Response(201, Map.of("Location", "/payments/pay_" + charges), "{\"id\":\"pay_" + charges + "\",\"status\":\"succeeded\"}");
            if (key != null) stored.put(key, res);
            return res;
        }
    }

    // =====================================================================
    // [etag]
    // =====================================================================

    static final class ProfileApi {
        String bio = "hello";
        long version = 1;

        Response get() { return new Response(200, Map.of("ETag", "\"v" + version + "\""), bio); }

        /** PUT with optional If-Match. Without it, last writer wins (and silently overwrites). */
        Response put(Request r) {
            String ifMatch = r.header("If-Match");
            if (ifMatch != null && !ifMatch.equals("\"v" + version + "\"")) return Response.problem(412, "Precondition Failed", "resource changed; re-fetch and retry");
            bio = r.body();
            version++;
            return new Response(200, Map.of("ETag", "\"v" + version + "\""), bio);
        }
    }

    // =====================================================================
    // [ratelimit]
    // =====================================================================

    static final class LimitedApi {
        private final int limit; private final long windowMs; private long windowStart = -1; private int used;
        LimitedApi(int limit, long windowMs) { this.limit = limit; this.windowMs = windowMs; }
        Response handle(long now) {
            long w = now - now % windowMs;
            if (w != windowStart) { windowStart = w; used = 0; }
            if (used >= limit) return new Response(429, Map.of("Retry-After", Long.toString((w + windowMs - now + 999) / 1000), "X-RateLimit-Remaining", "0"), "{\"title\":\"Too Many Requests\"}");
            used++;
            return new Response(200, Map.of("X-RateLimit-Limit", "" + limit, "X-RateLimit-Remaining", "" + (limit - used)), "ok");
        }
    }

    // =====================================================================
    // [versioning]
    // =====================================================================

    /** A TOLERANT READER: takes the fields it knows, ignores the rest. Tiny JSON-ish parser for flat objects. */
    static Map<String, String> parseFlat(String json) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String part : json.replaceAll("[{}\"]", "").split(",")) { String[] kv = part.split(":", 2); if (kv.length == 2) m.put(kv[0].trim(), kv[1].trim()); }
        return m;
    }

    record UserV1View(String id, String name) {
        static UserV1View from(String json) {
            Map<String, String> m = parseFlat(json);
            if (!m.containsKey("name")) throw new IllegalStateException("v1 client broken: field 'name' missing");
            return new UserV1View(m.get("id"), m.get("name"));
        }
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. Offset vs cursor pagination while new posts arrive");
        FeedApi feed = new FeedApi();
        for (int t = 1; t <= 100; t++) feed.insert(t);
        List<Post> page1 = feed.offsetPage(0, 10);
        Post lastSeen = page1.get(9);
        List<Long> p1 = page1.stream().map(Post::id).toList();
        feed.insert(101); feed.insert(102); feed.insert(103);              // 3 new posts on top
        List<Long> p2Offset = feed.offsetPage(10, 10).stream().map(Post::id).toList();
        List<Long> p2Cursor = feed.cursorPage(FeedApi.cursorOf(lastSeen), 10).stream().map(Post::id).toList();
        long dupes = p2Offset.stream().filter(p1::contains).count();
        System.out.println("   page 1 " + p1 + "\n   page 2 (offset) " + p2Offset + "\n   page 2 (cursor) " + p2Cursor);
        check("offset: 3 items shown AGAIN on page 2", 3L, dupes);
        check("cursor: continues exactly after the last seen item", "[90, 89, 88, 87, 86, 85, 84, 83, 82, 81]", p2Cursor);
        feed.rowsTouched = 0; feed.offsetPage(5_000 - 10, 10);
        long offsetCost = feed.rowsTouched;
        for (int t = 104; t < 5_200; t++) feed.insert(t);
        feed.rowsTouched = 0; feed.cursorPage(FeedApi.cursorOf(feed.posts.get(4_990)), 10);
        check("deep page: offset walks ~5,000 rows, cursor reads 10", "5000 vs 10", offsetCost + " vs " + feed.rowsTouched);

        System.out.println("2. Idempotency keys on POST /payments");
        PaymentsApi pay = new PaymentsApi();
        Request noKey = new Request("POST", "/payments", Map.of(), Map.of(), "{\"amount\":500}");
        pay.post(noKey); pay.post(noKey);                                   // client timed out and retried
        check("no key: customer charged twice", 2, pay.charges);
        Request withKey = new Request("POST", "/payments", Map.of("Idempotency-Key", "8f1c-…"), Map.of(), "{\"amount\":500}");
        Response first = pay.post(withKey), retry = pay.post(withKey);
        check("with key: one more charge only", 3, pay.charges);
        check("retry gets the SAME response", first.body(), retry.body());
        check("...marked as replayed", "true", retry.headers().get("Idempotent-Replayed"));
        Response misuse = pay.post(new Request("POST", "/payments", Map.of("Idempotency-Key", "8f1c-…"), Map.of(), "{\"amount\":900}"));
        check("same key, different body -> 422", 422, misuse.status());

        System.out.println("3. Lost update vs ETag / If-Match");
        ProfileApi profile = new ProfileApi();
        String etagAlice = profile.get().headers().get("ETag"), etagBob = profile.get().headers().get("ETag");
        profile.put(new Request("PUT", "/me", Map.of(), Map.of(), "alice's edit"));
        profile.put(new Request("PUT", "/me", Map.of(), Map.of(), "bob's edit"));
        check("without If-Match: alice's edit silently lost", "bob's edit", profile.bio);
        ProfileApi p2 = new ProfileApi();
        etagAlice = p2.get().headers().get("ETag"); etagBob = p2.get().headers().get("ETag");
        Response a = p2.put(new Request("PUT", "/me", Map.of("If-Match", etagAlice), Map.of(), "alice's edit"));
        Response b = p2.put(new Request("PUT", "/me", Map.of("If-Match", etagBob), Map.of(), "bob's edit"));
        check("with If-Match: second writer gets 412", "200/412", a.status() + "/" + b.status());
        check("error body uses problem+json", "application/problem+json", b.headers().get("Content-Type"));
        check("alice's edit survives; bob must re-fetch and merge", "alice's edit", p2.bio);

        System.out.println("4. Rate-limit responses");
        LimitedApi api = new LimitedApi(3, 60_000);
        List<Integer> statuses = new ArrayList<>();
        Response last = null;
        for (int i = 0; i < 5; i++) { last = api.handle(10_000); statuses.add(last.status()); }
        check("3 OK then 429s", "[200, 200, 200, 429, 429]", statuses);
        check("Retry-After in seconds until the window resets", "50", last.headers().get("Retry-After"));

        System.out.println("5. Versioning: additive changes are safe, renames are not");
        String v1 = "{\"id\":\"u1\",\"name\":\"Asha\"}";
        String v1PlusField = "{\"id\":\"u1\",\"name\":\"Asha\",\"avatarUrl\":\"https://cdn/x.png\"}";
        String renamed = "{\"id\":\"u1\",\"displayName\":\"Asha\"}";
        check("v1 client reads v1", "Asha", UserV1View.from(v1).name());
        check("v1 client tolerates a NEW field (backward compatible)", "Asha", UserV1View.from(v1PlusField).name());
        String broke;
        try { UserV1View.from(renamed); broke = "fine"; } catch (IllegalStateException e) { broke = "broken"; }
        check("renaming a field breaks old clients -> needs /v2 or a deprecation period", "broken", broke);
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
