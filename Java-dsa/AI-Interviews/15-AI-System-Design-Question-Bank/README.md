# 15 · AI System Design Case Studies and Question Bank

This chapter puts everything together. Each case study follows the **6-step method** from the [playbook](../00-AI-Interview-Playbook/README.md#2-how-to-answer-any-ai-design-question-the-6-step-method): **clarify → baseline → design → evaluate → harden → operate**. Practise saying them out loud in 15–20 minutes each. A rapid-fire question bank with short model answers follows.

---

## Case study 1: A customer-support agent that can take actions

> "Design an AI assistant for an e-commerce site that answers questions and can check orders and issue refunds."

**1. Clarify.** 50K conversations/day; answers from help-centre docs; actions: order status, address change, refunds up to $100 without a human; English plus 3 languages; p95 TTFT < 2 s; no PII leaks; hand-off to humans.

**2. Baseline.** A single agent: system prompt + RAG over the help centre + 3 tools (`get_order`, `update_address`, `issue_refund`).

**3. Design.**
```
web/app chat ─▶ API gateway (auth, user identity) ─▶ input guardrails (Model Armor: injection, PII)
   ─▶ agent service on Cloud Run (LangGraph / ADK)
        ├─ router (small model): FAQ | order | refund | complaint → human
        ├─ RAG: hybrid search over the help centre (pgvector + BM25) + reranker, citations
        ├─ tools via an internal API / MCP server, called AS the logged-in user (their orders only)
        ├─ issue_refund: amount ≤ $100 → allowed; otherwise interrupt → human approval queue
        └─ session state in Redis/Firestore; checkpointer for resumable approvals
   ─▶ LLM gateway (routing: small model for FAQ, big for complex; fallback provider; caching)
   ─▶ output guardrails (PII, URL allowlist, faithfulness check for policy answers) ─▶ streamed reply
```
Key decisions: tools enforce **authorisation** (the user ID comes from the session, never from the model); refunds are **idempotent** (keyed by order + reason); hand-offs to humans carry a **summary**.

**4. Evaluate.** A golden set of 500 real conversations by intent; RAG metrics (recall@5, faithfulness); **agent trajectory evals** with mocked tools (right tool, right arguments, approval requested above $100); a safety set (injections, other users' order numbers); LLM judge validated against support agents (kappa).

**5. Harden.** Least-privilege tools, limits and approvals; prompt-injection screening of *user messages and retrieved pages*; rate limits per user; max steps per turn; timeouts with graceful "connecting you to an agent"; no secrets in the prompt; audit log of every action.

**6. Operate.** Traces per conversation; metrics: resolution rate, escalation rate, refund volume, cost per conversation, TTFT; weekly review of thumbs-down conversations → new eval cases; canary releases of prompt and model changes.

---

## Case study 2: Enterprise document Q&A (multi-tenant RAG)

> "Build ChatGPT-for-our-documents for 2,000 enterprise customers, each with their own files."

**Clarify:** 10M documents in total (PDFs, Word, wikis), per-customer isolation, document-level permissions (some users can't see HR files), answers with citations, documents updated daily.

**Design:**
- **Ingestion pipeline** (event-driven: upload → Pub/Sub → workers): parse (layout-aware, OCR, tables → markdown) → structure-aware chunking with headings as metadata → contextual chunk headers → embed → index with `tenant_id`, `doc_id`, `acl_groups`, `updated_at`. Re-index on change; delete on removal.
- **Index:** hybrid (vector + BM25) with **tenant and ACL pre-filters**; at large scale, a namespace or index per large tenant.
- **Query:** rewrite (conversation-aware) → hybrid top-50 filtered by `tenant_id` + the user's groups → rerank → top-6 → generate with "answer only from context, cite [doc, page], else say you don't know".
- **Caching:** embeddings, retrieval results for popular queries per tenant (never across tenants).

**Evaluate:** per-tenant golden sets (start with synthetic questions generated from their docs, then real queries); recall@k, faithfulness, citation accuracy, abstention on unanswerable questions; **leak tests** (try to retrieve other tenants' or restricted docs).

**Harden:** ACLs enforced at retrieval (never by the LLM); screen uploaded documents for injected instructions; PII handling per tenant policy; encryption and data residency.

**Operate:** ingestion lag, retrieval latency, "no answer" rate per tenant, cost per query; re-run the evals when the chunking, embedding model or reranker changes (plan re-embedding as a migration).

---

## Case study 3: An AI coding / code-review assistant

> "Design an agent that reviews pull requests and suggests fixes."

**Design:** a webhook on PR open → a queue → a review agent (async, minutes allowed):
1. **Context engineering:** the diff, plus relevant surrounding code (search by symbol: grep / embeddings over the repo), the project's conventions file, and CI results. **Compress**: summarise big files, include only the touched functions and their callers.
2. Tools: `read_file`, `search_code`, `run_tests` (in a **sandbox** with no secrets or network), `post_comment`.
3. A multi-pass flow: bug-finding, security and style reviewers in **parallel** (orchestrator–worker), then a **verifier** that filters out low-confidence or duplicate comments (precision matters: noisy reviewers get ignored).

**Evaluate:** historical PRs with known bugs (did it find them?) and the false-comment rate rated by engineers; pass@k for suggested fixes using the repo's tests.

**Harden:** code execution only in an isolated sandbox; the agent can comment but **not merge**; treat code and comments in the PR as untrusted (they can contain prompt injection); never print secrets from the repo.

**Operate:** comment acceptance rate, cost per PR, latency; let developers rate comments.

---

## Case study 4: An internal AI platform for 20 teams

> "Every team is calling LLM APIs directly. Design a platform."

**Design:** an **LLM gateway** (chapter 12) as the single entry point:
- virtual keys per team/app; model allowlists; **budgets and alerts**; rate limits (requests and tokens per minute);
- aliases (`fast`, `smart`, `embed`) mapped to providers; **fallback** across vendors; circuit breakers;
- central **guardrails** (Model Armor / DLP) applied to everyone; redacted logging; data-residency routing;
- **observability:** OpenTelemetry traces, cost dashboards per team/feature, latency SLOs;
- shared services: an embedding service, a vector-DB-as-a-service, a prompt registry, an **eval service** (golden sets + LLM judges + CI integration), and approved MCP servers;
- self-hosted open models (vLLM on GKE) behind the same API for private or high-volume work.

**Discuss:** build vs buy (LiteLLM/Portkey/Apigee vs in-house), the single point of failure (multi-region, health checks), latency overhead (keep the gateway thin; stream through), governance (approved models and use cases, model cards).

---

## Case study 5: A deep-research multi-agent system

> "Users ask complex questions; the system researches the web and internal data and writes a cited report."

**Design:** an orchestrator plans sub-questions → parallel researcher agents (web search, internal RAG, SQL), each with its own context and a token budget → each returns **compressed findings with sources** (not raw pages) → a writer synthesises → a citation checker verifies each claim against its source → the report. Long-running, so it's **asynchronous** with progress streaming; state is checkpointed so a crash resumes instead of restarting.

**Key concerns:** cost (often several times a single agent: set budgets per run); compounding errors (verification steps); prompt injection from web pages (researchers have **no** action tools; outputs are screened); source quality ranking; dedupe across workers; evals on question sets with expert-written reference reports (judge rubrics: coverage, accuracy, citation correctness).

---

## Rapid-fire question bank

### Fundamentals
1. **What's a token?** A sub-word unit (BPE); ~1.3 per English word; the unit of context, cost and latency.
2. **Why are output tokens slower and pricier?** Decoding is sequential (one forward pass per token); prefill is parallel.
3. **What does temperature do?** It scales the logits: low = deterministic, high = diverse.
4. **What is the KV cache?** Stored attention keys/values that avoid recomputation; it costs memory proportional to tokens × concurrency.
5. **Why do LLMs hallucinate?** They're trained to produce likely text, not verified truth; ground them with RAG and tools, and verify.
6. **What's a context window?** The maximum prompt + output tokens; long contexts cost more and suffer from "lost in the middle".
7. **Encoder vs decoder models?** Encoders (BERT) for embeddings and classification; decoders (GPT/Gemini/Claude) for generation.

### Prompting and outputs
8. **How do you get reliable JSON?** Native structured output or constrained decoding, plus schema validation and retry with the errors.
9. **Few-shot vs fine-tuning?** Few-shot for quick behaviour shaping; fine-tune when it's needed at scale or for cost.
10. **What is chain-of-thought?** Step-by-step reasoning before the answer; helps multi-step tasks; costs tokens.
11. **What is prompt caching?** The provider reuses the computation for a repeated prefix; put static content first.

### RAG
12. **Walk through RAG.** Ingest (parse, chunk, embed, index + metadata) → query (rewrite, hybrid retrieve, rerank, prompt with context, cite).
13. **Why hybrid search?** BM25 for exact terms, vectors for meaning; fuse with RRF.
14. **Why rerank?** Cross-encoders read the query and document together and are more precise; apply them to the top-k only.
15. **Chunk size?** A precision vs context trade-off; ~300–800 tokens with overlap; tune with recall@k.
16. **How do you secure multi-tenant RAG?** ACL/tenant pre-filters at retrieval; never rely on the model.
17. **What's GraphRAG?** An entity-relation graph plus community summaries, for multi-hop and global questions.
18. **What's HyDE?** Search with an LLM-generated hypothetical answer instead of the question.

### Evaluation
19. **How do you evaluate an LLM app?** A golden set, deterministic checks, a validated LLM judge, slicing, significance, CI gates, online feedback.
20. **What biases do LLM judges have?** Position, verbosity, self-preference; fix with swapped order, length control and human calibration.
21. **RAG metrics?** Retrieval: recall@k, MRR, nDCG. Generation: faithfulness, answer relevance. Context: precision and recall.
22. **Is 81% → 83% on 100 examples significant?** No; the standard error is about ±4 points. Use a bigger set and bootstrap CIs.
23. **What's pass@k?** P(at least one of k samples passes the tests) = 1 − C(n−c, k)/C(n, k).

### Agents
24. **Agent vs workflow?** An agent lets the model choose the steps; a workflow is a fixed code path. Use the simplest that works.
25. **What is ReAct?** Thought → Action (tool) → Observation, looped until the final answer.
26. **How do you stop runaway agents?** Max steps, timeouts, cost budgets, loop detection.
27. **How do you handle tool errors?** Return them as observations with actionable messages; the agent self-corrects.
28. **Human-in-the-loop?** Approval gates (interrupts) for irreversible or high-value actions.
29. **Agent memory?** Short-term in the context (summarised); long-term in a store, retrieved by relevance; user control.
30. **How do you evaluate agents?** Task success, trajectory correctness, efficiency, safety, robustness, with mocked tools and repeated runs.

### Multi-agent and protocols
31. **When multi-agent?** Parallel independent sub-tasks, specialisation, too many tools, independent review, org boundaries.
32. **Downsides?** Several times the tokens, compounding errors, lost context, debugging difficulty.
33. **MCP vs A2A?** MCP: agent ↔ tools/data. A2A: agent ↔ agent (Agent Cards, tasks).
34. **What does MCP standardise?** JSON-RPC 2.0 messages for tools, resources and prompts, discovered at runtime over stdio or HTTP.

### Context engineering
35. **What is context engineering?** Deciding what enters the context at each step: write, select, compress, isolate, order.
36. **How do you handle a 50K-token tool output?** Filter at the source; dedupe/project/aggregate; goal-aware extraction; summarise; offload with a handle. Never naive truncation.
37. **Too many tools?** Retrieve the top-k tools per request, or route to sub-agents owning small toolsets.

### Safety and security
38. **What is prompt injection?** Untrusted text treated as instructions; there's no full fix, so use defence in depth.
39. **Indirect injection?** Attacks hidden in content the system reads (web, email, docs, tool output).
40. **What's in the OWASP LLM Top 10?** Injection, sensitive info disclosure, supply chain, poisoning, improper output handling, excessive agency, system prompt leakage, vector/embedding weaknesses, misinformation, unbounded consumption.
41. **What is Model Armor?** Google Cloud's service screening prompts and responses (injection/jailbreak, sensitive data, malicious URLs, RAI filters) with templates, confidence thresholds, inspect/block modes and floor settings.
42. **Excessive agency?** Too many permissions or tools; fix with least privilege, allowlists, limits and approvals in code.
43. **How do you detect a system-prompt leak?** A canary token in the prompt; scan outputs; keep secrets out of prompts.
44. **Why not render or execute model output directly?** XSS and injection; escape, validate, sandbox.
45. **How do you measure a guardrail?** Detection rate on attacks and false-positive rate on benign traffic; red-team.

### Frameworks
46. **What is LCEL?** Runnables composed with `|`; parallel dicts; streaming, batch, fallbacks for free.
47. **What does LangGraph add?** Explicit state graphs: conditional edges, loops, checkpointers per thread, interrupts for humans.
48. **Java options?** Spring AI (ChatClient, advisors, VectorStore, @Tool), LangChain4j (AI Services), Google ADK for Java.
49. **Framework or not?** Use one for stateful agents and integrations; keep simple chains thin; understand what's underneath.

### Gateways, LLMOps, deployment
50. **Why an LLM gateway?** Keys, access, budgets, rate limits, routing, fallback, caching, guardrails and observability in one place.
51. **Semantic cache risk?** Loose thresholds return wrong answers for requests that differ in specifics; scope it tightly.
52. **How do you cut LLM cost?** Routing to small models, prompt and response caching, shorter context, output caps, batch APIs, distillation.
53. **What do you monitor?** TTFT/p95, errors and fallbacks, tokens and cost per feature, cache hits, guardrail findings, quality feedback.
54. **How do you deploy an agent on GCP?** A stateless container on Cloud Run ($PORT, health check, SIGTERM, JSON logs), a service account, Secret Manager, external session store, streaming, autoscaling settings, eval-gated CI/CD.
55. **Long-running agent tasks?** Asynchronous: queue + worker/job, progress state, poll/webhook/stream.
56. **Cold starts?** Min instances, CPU boost, small images, lazy init.

### Fine-tuning and serving
57. **RAG vs fine-tuning?** RAG for facts; fine-tuning for behaviour, format, or cost reduction.
58. **LoRA?** A frozen base + a trained low-rank ΔW = A·B (< 1% of the parameters); mergeable, swappable.
59. **Quantisation trade-off?** Less memory and more concurrency vs some accuracy loss (especially int4 and outliers).
60. **Continuous batching?** Refill GPU slots as requests finish; much higher throughput than static batches.
61. **Self-host or API?** Self-host at high steady utilisation or for data control; the API for spiky or low volume.
62. **Speculative decoding?** A draft model proposes tokens; the big model verifies them in parallel; same output, faster.

---

## Final tips for the interview

- **Draw the boxes** (chapter 00's picture) and talk through one request end to end.
- **Name the trade-offs:** quality vs latency vs cost vs safety, for every decision.
- **Always mention evaluation and safety** without being asked.
- **Use numbers:** tokens per request, cost per 1K requests, p95 targets, eval sizes.
- **Admit uncertainty well:** "Prompt injection has no complete solution; here's how I'd reduce the risk and limit the damage."
- **Show you've built things:** run the Java simulations in this folder, change them, and talk about what you observed.

**Back to:** [AI Interviews overview](../README.md)
