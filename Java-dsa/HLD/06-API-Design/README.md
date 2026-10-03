# HLD 06 · API Design

> Fundamentals. Designing APIs that survive real clients and real networks: resource modelling, status codes, **pagination** (offset vs cursor, measured), **idempotency keys**, **optimistic concurrency** with ETags, **rate-limit headers**, **versioning**, a consistent **error model**, and REST vs gRPC vs GraphQL.
> Code: [`ApiDesignPatterns.java`](ApiDesignPatterns.java). Run with `java ApiDesignPatterns.java` (17 checks).

---

## 1. Resource-oriented REST basics
- Nouns, not verbs: `POST /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel` (an action sub-resource is fine when a state transition isn't a CRUD update).
- Methods and semantics:

| Method | Safe | Idempotent | Use |
|---|---|---|---|
| GET | ✓ | ✓ | read |
| PUT | ✗ | ✓ | replace / create at a known id |
| DELETE | ✗ | ✓ | remove |
| PATCH | ✗ | not necessarily | partial update |
| POST | ✗ | ✗ (make it so with an **Idempotency-Key**) | create, actions |

- Status codes: `200/201/202/204`, `400` (validation), `401` (who are you), `403` (not allowed), `404`, `409` (conflict), `412` (precondition failed), `422` (semantically invalid), `429` (rate limited), `5xx` (our fault; retryable with backoff).

## 2. Pagination

| | Offset (`?offset=20&limit=10`) | Cursor / keyset (`?cursor=…&limit=10`) |
|---|---|---|
| Consistency while data changes | **duplicates or skips**: the demo shows 3 posts repeated on page 2 after 3 new posts arrived | stable: continues exactly after the last seen item |
| Cost of deep pages | O(offset): the DB walks and discards rows (5,000 rows for page 500) | O(page) with an index seek (10 rows) |
| Jump to page N | ✓ | ✗ (only next/prev) |
| Use for | admin tables, small datasets | feeds, timelines, infinite scroll, sync APIs, large tables |

**Keyset query:** `WHERE (created_at, id) < (:ts, :id) ORDER BY created_at DESC, id DESC LIMIT 10`. The `id` tie-breaker makes the order total. Return the cursor **opaque** (base64) so you can change its contents later.

## 3. Idempotency keys
Networks time out *after* the server did the work; clients retry. For non-idempotent POSTs (payments, orders):
- The client generates `Idempotency-Key: <uuid>` per logical operation.
- The server stores `key → (request hash, response)` atomically with the effect (or before, in a "processing" state, to block concurrent duplicates).
- A retry with the same key **replays the stored response** (the demo: same body, `Idempotent-Replayed: true`, no second charge).
- The same key with a **different body** → `422` (a client bug).
- Expire keys after 24 hours or so. This is exactly how Stripe works. (Implementation with concurrent duplicates: [LLD 00 toolkit](../../LLD/00-LLD-Playbook/ConcurrencyToolkit.java), [LLD 18](../../LLD/18-Payment-Wallet/).)

## 4. Optimistic concurrency: ETag + If-Match
Read-modify-write by two clients loses an update (the demo: Alice's edit silently overwritten). Fix:
- `GET` returns `ETag: "v7"`.
- `PUT` sends `If-Match: "v7"`; if the resource changed meanwhile, the server returns **412 Precondition Failed** and the client re-fetches and merges.
- Same idea as a `version` column (`UPDATE … WHERE id=? AND version=?`). `If-None-Match` also enables cheap `304 Not Modified` caching for GETs.

## 5. Rate limiting at the API
Return `429 Too Many Requests` with `Retry-After`, plus `X-RateLimit-Limit` / `-Remaining` / `-Reset` on every response so clients can self-throttle. Limit per API key/user/IP and per endpoint cost. Algorithms and the distributed design: [LLD 04](../../LLD/04-Rate-Limiter/).

## 6. Versioning and evolution
- **Backward-compatible (no new version needed):** adding optional request fields, adding response fields, adding endpoints, adding enum values *if* clients are tolerant readers (they ignore unknown fields; the demo shows a v1 client fine with a new field).
- **Breaking:** renaming/removing fields, changing types or meaning, new required inputs, changed error semantics (the demo: renaming `name` → `displayName` breaks v1).
- Strategies: URI (`/v2/users`), header (`Accept: application/vnd.x.v2+json`), or date-based (Stripe). Support N−1, announce deprecations (`Deprecation`/`Sunset` headers), and measure usage before removing anything.

## 7. One error model
Use RFC 7807 `application/problem+json`: `{type, title, status, detail, instance}` plus machine-readable codes, so clients handle every error the same way. Never leak stack traces; include a request/trace id.

## 8. REST vs gRPC vs GraphQL (and others)

| | REST/JSON | gRPC (HTTP/2 + protobuf) | GraphQL |
|---|---|---|---|
| Best for | public APIs, browsers, caching | internal service-to-service, streaming, low latency | client-driven data needs (mobile/BFF), many entity types |
| Contract | OpenAPI (optional) | `.proto` (strict, codegen) | schema (strict) |
| Payload | text, larger | binary, compact | JSON, exactly what was asked |
| Caching | HTTP caching built in | custom | hard (POST, varied queries) |
| Pitfalls | over/under-fetching | browser support needs gRPC-web; harder debugging | N+1 resolvers, query cost limits needed |

Also: **WebSockets/SSE** for server push (chat, live updates), **webhooks** for server-to-server callbacks (sign them, retry them, make receivers idempotent), **async 202 Accepted + status URL** for long-running jobs.

## 9. Other API essentials
- **AuthN/AuthZ:** OAuth2/OIDC, short-lived JWT access tokens + refresh tokens, scopes; mTLS for internal traffic.
- **Timeouts and retries:** clients set deadlines; servers propagate them (gRPC deadlines); retry only idempotent calls.
- **Filtering/sorting/field selection:** `?status=PAID&sort=-createdAt&fields=id,total`.
- **Bulk endpoints** to cut chattiness, with per-item results.
- **Observability:** request ids, structured logs, per-endpoint latency/error metrics.

## 10. Interview Q&A

**Q1. Offset or cursor pagination for a social feed?**
Cursor (keyset): stable under inserts and O(page) at any depth. Offset duplicates or skips items when new posts arrive and gets slower with depth.

**Q2. How do you make POST /payments safe to retry?**
A client-supplied idempotency key; store the key with the request hash and response atomically with the charge; replay the response on retries; reject the key with a different payload; expire old keys.

**Q3. Which methods are idempotent?**
GET, PUT, DELETE (and HEAD, OPTIONS). POST and PATCH aren't inherently; make them so with keys or conditional requests.

**Q4. How do you prevent lost updates through an API?**
Optimistic concurrency: ETag on GET, If-Match on PUT/PATCH, 412 on mismatch. Or explicit version fields in the payload.

**Q5. How do you version an API?**
Avoid breaking changes; evolve additively with tolerant readers. When you must break, introduce a new version (URI or header), run both, deprecate with notice, and track usage.

**Q6. REST vs gRPC?**
REST for public/browser clients and HTTP caching; gRPC for internal low-latency, strongly typed, streaming RPCs with code generation.

**Q7. What should a 429 response contain?**
`Retry-After`, plus limit/remaining/reset headers. Clients back off with jitter.

**Q8. How do you design a long-running operation?**
`202 Accepted` with a `Location` of a status resource (or a webhook/callback); the job is idempotent and resumable; the client polls with backoff or subscribes.

**Q9. How do you handle partial failures in a batch endpoint?**
Return per-item statuses (`207 Multi-Status`-style or an array of results) and make items individually idempotent so the client can retry only the failures.

**Q10. What makes an error response good?**
A consistent structure (problem+json), a correct HTTP status, a machine-readable code, a human message, a request id for support, and no internal details.

## 11. Common mistakes
- Offset pagination on infinite feeds.
- POST without idempotency for money.
- Last-writer-wins updates on shared resources.
- Breaking field renames without a new version.
- 200 with `{"error": …}`.
- Retrying non-idempotent calls automatically.
