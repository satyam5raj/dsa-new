/*
 * ============================================================================
 *  PATTERN 3 : BUILDER  (Creational)
 * ============================================================================
 *
 *  Run:  java P3_Builder.java
 *
 *  PROBLEM : objects with MANY fields, several of them optional.
 *    Option A, TELESCOPING CONSTRUCTORS:
 *        new Pizza("L", "thin", true, false, true, null, 2)   // what is 'true' #2?
 *        unreadable, easy to swap two booleans, explodes in number of overloads.
 *    Option B, JAVABEANS (no-arg constructor + setters):
 *        readable, but the object is MUTABLE and can be observed in a
 *        half-built, INVALID state between setter calls. Can't be immutable.
 *
 *  INTENT : construct a complex object STEP BY STEP with readable, named
 *           calls, then produce an IMMUTABLE, VALIDATED object in one go.
 *
 *        HttpRequest req = HttpRequest.builder("https://api.x.com")
 *                              .method("POST")
 *                              .header("Auth", "token")
 *                              .timeoutSeconds(5)
 *                              .build();
 *
 *  INGREDIENTS
 *    1. Target class with a PRIVATE constructor that takes the Builder
 *    2. Static nested Builder class with the same fields
 *    3. Fluent setters that return 'this' (method chaining)
 *    4. build() validates everything and creates the immutable object
 *
 *  JDK/LIBRARY EXAMPLES: StringBuilder, HttpClient.newBuilder(),
 *    Stream.builder(), Lombok @Builder, Spring's ResponseEntity.ok().body()
 *
 *  USE WHEN: more than ~4 constructor params, many optional ones, or you
 *            need immutable objects with validation.
 */

import java.util.*;

public class P3_Builder {

    public static void main(String[] args) {
        System.out.println("===== BUILDER IN ACTION =====");
        HttpRequest get = HttpRequest.builder("https://api.shop.com/orders")
                .build();                                           // only required field, defaults for the rest
        HttpRequest post = HttpRequest.builder("https://api.shop.com/orders")
                .method("POST")
                .header("Authorization", "Bearer abc123")
                .header("Content-Type", "application/json")
                .body("{\"item\":\"laptop\"}")
                .timeoutSeconds(5)
                .retries(3)
                .build();
        System.out.println("   " + get);
        System.out.println("   " + post);

        System.out.println("\n===== VALIDATION IN build() =====");
        try {
            HttpRequest.builder("https://api.shop.com").method("GET").body("not allowed").build();
        } catch (IllegalStateException e) {
            System.out.println("   rejected: " + e.getMessage());
        }

        System.out.println("\n===== IMMUTABILITY =====");
        try {
            post.headers().put("X-Hack", "1");
        } catch (UnsupportedOperationException e) {
            System.out.println("   headers can't be changed after build()");
        }

        System.out.println("\n===== toBuilder(): modified copy =====");
        HttpRequest retried = post.toBuilder().retries(5).build();
        System.out.println("   original retries=" + post.retries() + ", copy retries=" + retried.retries());

        System.out.println("\n===== STEP BUILDER (enforces order at compile time) =====");
        Email email = Email.builder().from("me@x.com").to("you@y.com").subject("Hi").send();
        System.out.println("   " + email);
    }

    // =================================================================
    // Classic builder
    // =================================================================
    static final class HttpRequest {
        private final String url;                       // required
        private final String method;                    // optional, default GET
        private final Map<String, String> headers;      // optional
        private final String body;                      // optional
        private final int timeoutSeconds;               // optional, default 30
        private final int retries;                      // optional, default 0

        // PRIVATE: the only way in is through the Builder
        private HttpRequest(Builder b) {
            this.url = b.url;
            this.method = b.method;
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(b.headers));  // defensive copy
            this.body = b.body;
            this.timeoutSeconds = b.timeoutSeconds;
            this.retries = b.retries;
        }

        // Required params go into the builder() call itself
        static Builder builder(String url) { return new Builder(url); }

        Builder toBuilder() {
            Builder b = new Builder(url).method(method).timeoutSeconds(timeoutSeconds).retries(retries);
            headers.forEach(b::header);
            if (body != null) b.body(body);
            return b;
        }

        Map<String, String> headers() { return headers; }
        int retries() { return retries; }

        @Override
        public String toString() {
            return method + " " + url + " headers=" + headers.keySet() + (body != null ? " body=" + body : "")
                    + " timeout=" + timeoutSeconds + "s retries=" + retries;
        }

        static final class Builder {
            private final String url;
            private String method = "GET";
            private final Map<String, String> headers = new LinkedHashMap<>();
            private String body;
            private int timeoutSeconds = 30;
            private int retries = 0;

            private Builder(String url) {
                this.url = Objects.requireNonNull(url, "url");
            }

            // Each setter returns 'this' -> fluent chaining
            Builder method(String method) { this.method = method; return this; }
            Builder header(String k, String v) { headers.put(k, v); return this; }
            Builder body(String body) { this.body = body; return this; }
            Builder timeoutSeconds(int s) { this.timeoutSeconds = s; return this; }
            Builder retries(int r) { this.retries = r; return this; }

            // Cross-field validation happens ONCE, before the object exists
            HttpRequest build() {
                if (!url.startsWith("http")) throw new IllegalStateException("url must be http(s)");
                if (body != null && method.equals("GET")) throw new IllegalStateException("GET cannot have a body");
                if (timeoutSeconds <= 0) throw new IllegalStateException("timeout must be > 0");
                if (retries < 0 || retries > 10) throw new IllegalStateException("retries must be 0..10");
                return new HttpRequest(this);
            }
        }
    }

    // =================================================================
    // Step builder: each step returns an interface exposing ONLY the next
    // step, so you can't call send() before setting from/to/subject.
    // =================================================================
    record Email(String from, String to, String subject) {
        interface FromStep { ToStep from(String from); }
        interface ToStep { SubjectStep to(String to); }
        interface SubjectStep { FinalStep subject(String subject); }
        interface FinalStep { Email send(); }

        static FromStep builder() {
            return from -> to -> subject -> () -> new Email(from, to, subject);   // chained lambdas
        }
    }
}
