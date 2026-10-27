# Networking 05 · DNS: the Domain Name System

> DNS turns names into addresses. It's a globally distributed, heavily cached database, and "it's
> always DNS" is a running joke among SREs because so many outages trace back to it.

Run it: `java Dns.java` → **22/22 checks**: a recursive resolver walking a simulated
root → TLD → authoritative hierarchy, caching and TTLs, every record type, negative caching,
split-horizon, a real wire-format query, reverse DNS, and the "intermittent DNS failure" scenario.

---

## 1. The hierarchy

```
                         . (root)          13 named root servers (hundreds of anycast instances)
            ┌────────────┼─────────────┐
          com.          org.          in.        TLD servers (run by registries)
           │
      example.com.                               authoritative servers (yours: Route 53, Cloudflare, ...)
      ┌────┴─────┐
   www  api  mail                                records live here
```

| Role | Knows | Example |
|---|---|---|
| **root servers** | who runs each TLD | "for `.com`, ask the Verisign servers" |
| **TLD servers** | who is authoritative for each domain | "for `example.com`, ask `ns1.example`" |
| **authoritative servers** | the actual records for a zone | `example.com A 93.184.216.34` |
| **recursive resolver** | nothing, at first; it asks the others and **caches** | your ISP, `8.8.8.8`, `1.1.1.1`, the VPC resolver `169.254.169.253`, CoreDNS in Kubernetes |
| **stub resolver** | which recursive resolver to ask | the OS library (`/etc/resolv.conf`); Java's `InetAddress` |

## 2. Recursive vs iterative queries

- **Recursive:** "give me the final answer". Your laptop asks the resolver this way (the **RD**
  flag = recursion desired).
- **Iterative:** "tell me what you know, or who to ask next". The resolver asks root, then the TLD,
  then the authoritative server, **following referrals**.

On a cold cache, `example.com A` costs **3 upstream queries** (root → `.com` → `ns1.example`) and
returns `93.184.216.34`.

## 3. Caching and TTL

Every record has a **TTL** (time to live, in seconds). Resolvers, operating systems, browsers and
language runtimes cache answers for that long.

| Lookup | Upstream queries |
|---|---|
| first `example.com` | 3 |
| second, within the 300 s TTL | **0** (a cache hit) |
| after the TTL expires | 3 more |

Real resolvers also cache the **NS referrals** (TTL ~2 days), so after warm-up they skip root and
the TLD entirely. That's why the root servers aren't overwhelmed.

**Why doesn't a DNS change take effect immediately?** ("DNS propagation") Nothing is pushed: every
cache keeps the **old** answer until its TTL expires. With a TTL of 86400 that's up to a day.
Layers of caches each add their own: browser → OS → local resolver → upstream. Some clients ignore
TTLs; the JVM historically cached forever under a security manager
(`networkaddress.cache.ttl`).

**The playbook for a planned change:**

1. Lower the TTL (e.g. to 60 s) **at least one old-TTL period** before the change.
2. Make the change, and keep the old target running until traffic drains.
3. Raise the TTL back.

**A cached chain lives as long as its shortest TTL.** `www → CDN name (60 s) → edge IP (20 s)`
expires after **20 s**. CDNs use short TTLs to steer traffic quickly.

## 4. Record types

| Type | Maps | Example from the run |
|---|---|---|
| **A** | name → IPv4 | `example.com → 93.184.216.34` |
| **AAAA** | name → IPv6 | `example.com → 2606:2800:220:1::248` |
| **CNAME** | alias → canonical name | `www.example.com → example.edgekey.cdnprovider.com → 23.45.67.89` |
| **MX** | domain → mail servers with a preference | `10 mail…` is tried before `20 backup-mail…` (lowest first) |
| **TXT** | arbitrary text | SPF (`v=spf1 …`), DKIM, DMARC, domain-ownership verification |
| **NS** | zone → its authoritative servers | delegation (the referrals above) |
| **SRV** | service → priority, weight, **port**, host | `_sip._tcp.example.com → 10 60 5060 sip1…` (used by Kubernetes, Consul, XMPP, LDAP) |
| **PTR** | IP → name (reverse) | §6 |
| SOA | zone metadata (serial, negative-cache TTL) | |
| CAA | which CAs may issue certificates for the domain | |

**CNAME rules:**

- a CNAME can't coexist with other records at the same name;
- so you can't put a CNAME at the **zone apex** (`example.com`). Providers offer ALIAS/ANAME or
  "CNAME flattening" for that;
- every CNAME hop adds a lookup.

## 5. Negative caching

`missing.example.com` returns **NXDOMAIN**, and resolvers **cache that too** (for the SOA's
minimum/negative TTL). The second query sends nothing upstream.

> **Gotcha:** if a deployment script queries a name **before** the record is created, resolvers
> may answer "does not exist" for minutes after you create it.

## 6. Reverse DNS

To map an IP to a name, reverse the octets under `in-addr.arpa` and query **PTR**:
`93.184.216.34` → `34.216.184.93.in-addr.arpa.` (IPv6 uses `ip6.arpa`, nibble by nibble).

It's used by mail servers (spam checks: does the PTR match the forward record?), logs, and
traceroute output. The IP's **owner** (your cloud provider or ISP) controls it, not the domain's
owner.

## 7. Split-horizon DNS

The **same name gives different answers depending on who asks**:

- from inside the VPC: `db.corp.example.com → 10.0.96.15`;
- from the Internet: **NXDOMAIN**.

It's used for private services, internal load-balancer addresses, and cloud private DNS zones.

**Why can a domain resolve on one machine but fail on another?** (a senior question)

- different **resolvers** (corporate VPN vs public; container vs host; Kubernetes CoreDNS vs the
  node);
- a different **view** (split horizon);
- **stale caches** (OS, browser, JVM, nscd), or negative caching;
- **`/etc/hosts`** overrides (the run resolves `localhost` that way, with no DNS query);
- **search domains / ndots** (§10);
- IPv6 vs IPv4 differences (an AAAA record exists but its target is broken);
- DNS-over-HTTPS in the browser bypassing the system resolver;
- firewalls blocking UDP or TCP 53 on one network.

## 8. The wire format

DNS uses **UDP port 53** for most queries (one small packet each way; no handshake). It falls back
to **TCP 53** for large answers (truncated responses, zone transfers); EDNS0 lets UDP go above 512
bytes.

The program's query for `example.com A`:

```
be ef 01 00 00 01 00 00 00 00 00 00 | 07 65 78 61 6d 70 6c 65 03 63 6f 6d 00 | 00 01 00 01
└ID─┘ └flags: RD=1┘ └QDCOUNT=1 ...─┘  7 "example"           3 "com"   end     A     IN
```

That's **29 bytes**: a 12-byte header, length-prefixed labels, then the type and class.

The **transaction ID** (plus a random source port) is what stops off-path attackers from forging
answers. A predictable ID enabled the 2008 **Kaminsky cache-poisoning** attack (chapter 08).

## 9. DNS privacy and security

| | |
|---|---|
| **DoT** (DNS over TLS, port 853) / **DoH** (DNS over HTTPS, 443) | encrypt client → resolver queries: privacy from the network, harder to tamper with |
| **DNSSEC** | **signs** records so resolvers can verify authenticity (stops poisoning); doesn't encrypt |
| spoofing / poisoning | forged answers are cached; defend with random IDs and ports, DNSSEC, and trusted resolvers |

## 10. Troubleshooting intermittent DNS failures in production

The program simulates the classic case: `resolv.conf` lists **two nameservers, one dead**, with
`options rotate`. About **50 %** of uncached lookups wait for the **5-second timeout** before the
healthy server answers, so the mean is 2,522 ms. It looks like "random 5-second latency spikes".

**Investigation checklist:**

1. **Reproduce and time it:** `dig @<each resolver> name` and look at the query time; run `dig` in
   a loop and count SERVFAIL or timeouts.
2. **Check every resolver in `/etc/resolv.conf`** (or the pod's), and the `timeout`, `attempts`
   and `rotate` options.
3. **Kubernetes `ndots:5`:** a name with fewer than 5 dots is first tried with every search domain.
   `api.example.com` becomes **4 lookups** (`api.example.com.default.svc.cluster.local`, …), doubled
   for A + AAAA. Use FQDNs with a trailing dot, lower ndots, or run NodeLocal DNSCache.
4. **Resolver limits:** cloud resolvers rate-limit (e.g. 1024 packets/s per network interface on
   AWS); CoreDNS can be CPU-starved.
5. **UDP drops:** conntrack table full, or the parallel A/AAAA lookups race in Linux conntrack (a
   well-known Kubernetes 5 s timeout bug; fixed with `single-request-reopen` or NodeLocal DNS).
6. **Caching in the app:** connection pools and HTTP clients that resolve on every request multiply
   the load. Cache sensibly, but respect TTLs.
7. Watch **SERVFAIL** (an upstream or DNSSEC failure) vs **NXDOMAIN** (the name doesn't exist).

---

## Senior-level questions

**1. What happens when you enter `https://example.com` in a browser?**

1. The browser checks its caches (HSTS forces HTTPS; the DNS cache).
2. The OS resolver: `/etc/hosts`, the OS cache, then a query to the recursive resolver (RD = 1).
3. The resolver walks root → `.com` → authoritative (or answers from cache) and returns A/AAAA
   records.
4. The browser opens TCP to port 443 (or QUIC for HTTP/3), races IPv4 and IPv6 (Happy Eyeballs).
5. The TLS handshake with SNI = `example.com`; certificate validation; ALPN chooses h2 or http/1.1.
6. The HTTP request; the response may redirect; the HTML loads, and its sub-resources repeat the
   process (connections reused or multiplexed).
7. The page renders.

(Chapters 04 and 06 cover the transport and TLS parts.)

**2. Why might a DNS change not take effect immediately?** Cached answers live until their TTL
expires, at every layer (browser, OS, resolver, runtime); negative answers are cached too; some
clients ignore TTLs. Lower the TTL in advance, keep old targets alive, then raise it again.

**3. Why can a domain resolve on one machine but fail on another?** Different resolvers or views,
stale or negative caches, `/etc/hosts`, search domains and ndots, VPN split DNS, DoH in the browser,
firewalled port 53, IPv6-specific issues (§7).

**4. How would you troubleshoot intermittent DNS resolution failures in production?** Measure
first (resolution latency and error metrics, `dig` loops against each resolver). Check the
resolver config (dead servers, timeouts, rotate), Kubernetes ndots and search domains, resolver rate
limits and CPU, UDP/conntrack drops and the A/AAAA race, and app-level caching. Fix with healthy
redundant resolvers, node-local caches, FQDNs, sensible timeouts and retries, and monitoring of
SERVFAIL rates (§10).

---

**Next:** [06 · HTTP, HTTPS and TLS](../06-HTTP-HTTPS-TLS/README.md)
