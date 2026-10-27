# 12 · LLM Gateways and LLMOps

| | |
|---|---|
| **Code** | [LlmGateway.java](LlmGateway.java): a complete gateway with virtual keys, budgets, a token-bucket rate limiter, model aliases and complexity routing, retries + fallback + circuit breaker, exact and semantic caching, cost accounting, redacted logs and latency percentiles, on a simulated clock (`java LlmGateway.java`, 21 checks) |
| **Related** | [LLD 04 Rate Limiter](../../LLD/04-Rate-Limiter/), [LLD 06 Cache](../../LLD/06-Cache-LRU-LFU/), [HLD 01 Load Balancing](../../HLD/01-Scalability-Load-Balancing/), [HLD 03 Caching](../../HLD/03-Caching/) |
| **Interview weight** | high for platform and backend roles: "how would you manage LLM usage across 20 teams?" |

---

## 1. What an LLM gateway is

An **LLM gateway** (also called an AI gateway or LLM proxy) is a service that sits **between your applications and the model providers**. Every LLM call goes through it.
```
app A ─┐                       ┌──▶ provider A (frontier models)
app B ─┼──▶  LLM GATEWAY  ─────┼──▶ provider B (another vendor)
agent ─┘   (one API, one place  └──▶ self-hosted models (vLLM)
            for policy)
```
**The analogy:** an API gateway (like Kong or Apigee) specialised for LLM traffic: it understands tokens, models, prompts and costs.

### Why companies add one
Without a gateway, every team embeds provider keys, writes its own retries, has no cost visibility, and is locked into one vendor. A gateway centralises:
| Concern | What the gateway does |
|---|---|
| **unified API** | one request format (often OpenAI-compatible) for all providers |
| **key management** | apps get **virtual keys**; the real provider keys stay inside the gateway |
| **access control** | which team may use which models |
| **budgets and quotas** | per-team spend limits, tokens per minute |
| **rate limiting** | protect providers' limits and your budget |
| **routing** | aliases, cost/latency/complexity-based routing, A/B and canary tests of models |
| **reliability** | retries, fallbacks across providers, circuit breakers, timeouts |
| **caching** | exact and semantic caches; provider prompt caching |
| **guardrails** | PII redaction, prompt-injection screening (Model Armor, chapter 10) for every app at once |
| **observability** | logs, traces, tokens, cost, latency (TTFT), errors per team/app/model |
| **compliance** | audit trails, data residency, redacted logging |

### Examples
**LiteLLM** (open-source proxy), **Portkey**, **Kong AI Gateway**, **Apigee** (with AI policies), **Cloudflare AI Gateway**, **Envoy AI Gateway**, **Azure API Management** AI features, **AWS Bedrock** (managed multi-model access), **Vertex AI** Model Garden, **OpenRouter**, and many companies' in-house gateways.

---

## 2. Access control, budgets and rate limits

### Virtual keys
Each team or app gets its own gateway key, mapped to a team, a budget and an allowed-model list (from the code):
| Request | Result |
|---|---|
| a key that doesn't exist (`sk-stolen`) | **401** unknown key |
| the interns' key asking for the big model | **403** model big-model not allowed for interns |
| the interns' key after spending its tiny budget | **402** monthly budget exhausted |

Revoking or rotating a team's key never touches the provider keys.

### Rate limiting: a token bucket
Each key has a bucket of 5 tokens refilling at 2 per second: a **burst of 8 simultaneous requests → 5 allowed, 3 rejected with 429**; after waiting, requests pass again. LLM gateways usually limit **requests per minute and tokens per minute** (a few huge prompts can cost more than many small ones). The algorithms are in [LLD 04 Rate Limiter](../../LLD/04-Rate-Limiter/).

---

## 3. Routing: the biggest cost lever

| Kind | Example |
|---|---|
| **aliases** | apps ask for `"fast"` or `"smart"`; the platform team decides which real model that means, and can swap models without app changes |
| **complexity routing** | short, simple prompts → a small model; reasoning, code, analysis → a big model (`auto` in the code) |
| **cost/latency-based** | pick the cheapest model that meets a quality bar for the task type |
| **A/B and canary** | send 5% of traffic to a new model and compare evals and feedback |
| **geographic / compliance** | EU users → EU-hosted models |

From the code: "What are your opening hours?" → **small-model**; "Analyze this contract step by step…" → **big-model**.

**The economics** (example prices, 1,000 input + 200 output tokens):
| Model | Cost per request |
|---|---|
| big | $0.00600 |
| small | $0.00027 |

Routing **70% of traffic to the small model saves 67%** of the cost. Routers can be rules, a small classifier, or an LLM judge trained on which model succeeds for which queries. **Validate routing with evals**: a cheap model that answers wrongly isn't cheap.

---

## 4. Reliability: retries, fallbacks, circuit breakers

Providers have outages, rate limits (429), and slow tails. The gateway hides this from apps:
1. **Retry** transient errors (429, 5xx, timeouts) with **exponential backoff and jitter**, but don't retry 400s (bad requests).
2. **Fallback** to an equivalent model at **another provider** when one is down.
3. **Circuit breaker:** after repeated failures, **stop calling the broken provider** for a cool-down period, then try again.

From the code (provider A down):
| Request | What happened | Latency |
|---|---|---|
| 1st | A failed twice (with backoff) → the **breaker opens** → fell back to provider B | **4,400 ms** |
| 2nd | the breaker is open: A **skipped**, B called directly (1 call) | **1,400 ms** |
| after the 30 s cool-down, A healthy again | A used again | normal |

Also: **timeouts** (stream with a time-to-first-token deadline), **hedged requests** for latency-critical paths (send to two providers and take the first answer, at double the cost), and **graceful degradation** ("the assistant is busy; here are help articles").

**Watch out:** fallback models may behave differently (prompts, tool-calling formats, safety behaviour). Run your evals on the fallback model too.

---

## 5. Caching

| Cache | How | Hit when | Risk |
|---|---|---|---|
| **exact** | hash the normalised prompt (+ model + parameters) | the identical request | stale answers (use a TTL) |
| **semantic** | embed the prompt; return a cached answer if similarity ≥ threshold | a paraphrase of an earlier request | **wrong answers** if the threshold is loose |
| **provider prompt caching** | the provider reuses the KV cache for a repeated prefix | same system prompt / documents | none (it's transparent); just put static content first |

From the code:
- "  what is the REFUND policy?  " hits the **exact** cache (after normalising case and spaces): **no provider call**.
- "refund policy what is it" hits the **semantic** cache (same meaning, different words).
- With a **loose threshold (0.5)**, "cancel my order 9876" returns the cached answer for **order 1234**. A semantic cache must not be used for requests that depend on specific entities, user data or time. Use strict thresholds, include the user/tenant and key entities in the cache key, and only cache "FAQ-like" generic answers.

Never share cached answers across tenants or users when the answer contains private data.

---

## 6. Observability and cost management (LLMOps)

### What to log for every LLM call
`trace_id`, team/app, model, prompt **version**, input/output tokens, **cost**, latency (**TTFT** and total), cache hit, retries/fallbacks, finish reason, guardrail findings, and (redacted) prompt and response, so you can debug and evaluate.

From the code: per-team spend is tracked; **p50 latency 300 ms** (mostly the small model), **p95 1,200 ms** (the big-model requests); and logs contain **[EMAIL]**, never raw addresses.

### Tracing
Agents make many calls per user request. Use **distributed tracing** (OpenTelemetry, with the GenAI semantic conventions) so one trace shows every LLM call, tool call and retrieval with its latency and tokens. Tools: LangSmith, Langfuse, Arize Phoenix, Helicone, Datadog / New Relic LLM observability, Google Cloud Trace.

### Metrics and alerts
- cost per day, per team, per feature; budget alerts;
- error rate by provider; 429 rate; fallback rate;
- latency p50/p95/p99, TTFT;
- cache hit rate; tokens per request (prompt bloat creeps in);
- quality signals: thumbs-down rate, sampled LLM-judge scores (chapter 05).

### LLMOps: the lifecycle
| Practice | Meaning |
|---|---|
| **prompt management** | prompts versioned in git or a prompt registry; logged with each trace |
| **evaluation gates** | an eval suite in CI blocks regressions (chapter 05) |
| **model versioning** | pin model versions; plan migrations when providers deprecate models |
| **canary releases** | new prompt or model to a small % of traffic first |
| **feedback loop** | production failures → labelled → added to the evals |
| **cost reviews** | regularly check the biggest spenders; apply routing and caching |

---

## 7. Self-hosting vs APIs (a frequent design question)

| | Provider APIs | Self-hosted (vLLM, TGI, Ollama on GPUs) |
|---|---|---|
| quality | the best frontier models | open-weight models (good and improving) |
| ops burden | none | GPUs, scaling, upgrades |
| cost | pay per token | pay for GPUs whether used or not; cheaper at high, steady volume |
| data control | depends on the provider's terms and region | full control |
| latency | network + queueing | can be lower; you control batching |

A gateway lets you mix both behind one API and move traffic as economics change.

---

## 8. Interview questions

**Q1. Why put a gateway in front of LLM providers?**
> One place for keys, access control, budgets, rate limits, routing, retries and fallbacks, caching, guardrails and observability. It avoids every team reinventing them, prevents vendor lock-in, and gives cost visibility.

**Q2. How would you design rate limiting for LLM traffic?**
> Token buckets per virtual key (or user/team), limiting both requests and tokens per minute, enforced in the gateway (distributed: Redis plus atomic Lua scripts), returning 429 with Retry-After, with priority tiers and separate limits that respect the providers' own quotas.

**Q3. How do you make an LLM app resilient to provider outages?**
> Retries with backoff and jitter for transient errors, circuit breakers per provider, fallback to equivalent models at another provider (validated with evals), timeouts on TTFT, and graceful degradation.

**Q4. What is semantic caching, and what's the risk?**
> Returning a cached answer when a new prompt's embedding is close enough to an earlier one. It saves cost and latency for paraphrased FAQs, but a loose threshold returns wrong answers for requests that differ in important details (IDs, dates, users), so scope it carefully.

**Q5. How do you reduce LLM costs at scale?**
> Route easy requests to smaller models, use provider prompt caching (static prefix first), cache answers, trim prompts and retrieved context, limit output tokens, batch offline work, compress tool outputs, and track cost per feature to find the waste.

**Q6. What do you monitor for an LLM service?**
> Latency (TTFT, p95), errors and fallbacks per provider, tokens and cost per request/team/feature, cache hit rate, rate-limit hits, guardrail findings, and quality signals (feedback, sampled judge scores), with end-to-end traces linking them.

---

**Next:** [13 · Deploying an AI agent to Cloud Run](../13-Deploy-Agent-Cloud-Run/README.md)
