# AI Interviews: LLMs, RAG, Agents and Production AI (Basics to Advanced)

Interview preparation for **AI engineer / applied-AI / backend-with-AI** roles, written to be easy to learn. It covers how LLMs work, prompting, embeddings, **RAG**, **LLM and RAG evaluation**, **AI agents** and **multi-agent systems**, **MCP and context engineering** (including **dynamic tool-output compression**), **guardrails and AI security** (OWASP LLM Top 10, **Model Armor**), **LangChain/LangGraph** and Java frameworks, **LLM gateways** and LLMOps, **deploying an agent to Cloud Run**, fine-tuning, serving and cost, and system-design case studies.

Every technical chapter has:
- a **`README.md`**: simple explanations and analogies first, then the details, real code snippets for the popular frameworks, and interview questions with model answers;
- a **runnable Java simulation** (no dependencies, no API keys, deterministic fake LLMs) that **builds the idea from scratch and checks it**: a BPE tokenizer, a RAG pipeline, eval metrics, a ReAct agent, an MCP-style server, a guardrail pipeline, an LLM gateway, a Cloud-Run-ready HTTP service, a LoRA trained by gradient descent… Every number in the READMEs comes from these programs.

Run everything with `./run-all.sh` (14 programs, 284 checks).

---

## Chapters

| # | Chapter | You'll learn | Program |
|---|---|---|---|
| 00 | [Playbook and glossary](00-AI-Interview-Playbook/README.md) | the big-picture architecture, the 6-step answer method, choosing prompt vs RAG vs fine-tune vs agent, an A–Z glossary | – |
| 01 | [LLM fundamentals](01-LLM-Fundamentals/README.md) | tokens (BPE), next-token prediction, transformers, sampling (temperature/top-k/top-p), context windows, KV cache, training stages, memory and cost maths | 28 checks |
| 02 | [Prompting and structured output](02-Prompt-Engineering/README.md) | prompt anatomy, few-shot, CoT, delimiting untrusted input, JSON via validate-and-retry, self-consistency, prompt cost and caching | 19 |
| 03 | [Embeddings and vector search](03-Embeddings-Vector-Search/README.md) | BM25, embeddings, cosine, hybrid search with RRF, IVF/HNSW, quantised vectors, choosing a vector DB | 17 |
| 04 | [RAG](04-RAG/README.md) | chunking, metadata and ACLs, retrieve + rerank, query rewriting, grounded generation with citations, advanced RAG, failure modes | 18 |
| 05 | [LLM evaluation](05-LLM-Evaluation/README.md) | golden sets, EM/F1/ROUGE, LLM-as-a-judge and its biases, Cohen's kappa, bootstrap CIs, slice-based regression tests, calibration, pass@k, online evaluation | 24 |
| 06 | [RAG evaluation](06-RAG-Evaluation/README.md) | recall@k, MRR, nDCG, faithfulness, answer relevance, context precision/recall (RAGAS), retriever-vs-generator diagnosis | 23 |
| 07 | [AI agents](07-AI-Agents/README.md) | the ReAct loop, tool calling, validation, error recovery, loop limits, human approval, memory, planning patterns, agent evals | 18 |
| 08 | [Multi-agent systems](08-Multi-Agent-Systems/README.md) | orchestrator–worker (parallel), handoffs, writer/reviewer, A2A and agent cards, shared state, cost and risks | 14 |
| 09 | [Tools, MCP, context engineering](09-Tools-MCP-Context-Engineering/README.md) | tool design, **MCP** (JSON-RPC: initialize, tools/list, tools/call), dynamic tool selection, context budgets, **dynamic tool-output compression**, offloading | 28 |
| 10 | [Guardrails and AI security](10-Guardrails-AI-Security/README.md) | OWASP LLM Top 10, direct and indirect prompt injection, spotlighting, PII redaction, topic rails, output screening, excessive agency, **Model Armor**, measuring guardrails | 28 |
| 11 | [LangChain, LangGraph and frameworks](11-LangChain-Frameworks/README.md) | LCEL Runnables, the RAG chain, LangGraph state graphs/checkpoints/interrupts, **Spring AI**, **LangChain4j**, Google ADK, LlamaIndex, when to use a framework | 10 |
| 12 | [LLM gateways and LLMOps](12-LLM-Gateways-LLMOps/README.md) | virtual keys, budgets, rate limits, routing, retries/fallback/circuit breakers, exact and semantic caching, observability, LLMOps | 21 |
| 13 | [Deploying an agent to Cloud Run](13-Deploy-Agent-Cloud-Run/README.md) | Cloud Run concepts, a stateless agent service ($PORT, health, SSE streaming, sessions, SIGTERM), Dockerfile, gcloud deploy, IAM and secrets, production architecture | 12 |
| 14 | [Fine-tuning, serving and cost](14-Fine-Tuning-Serving-Cost/README.md) | RAG vs fine-tuning, LoRA/QLoRA, distillation, quantisation, continuous batching, vLLM-style serving, API vs self-hosting maths | 24 |
| 15 | [System design case studies and question bank](15-AI-System-Design-Question-Bank/README.md) | 5 full designs (support agent, multi-tenant RAG, code reviewer, AI platform, deep research) and 62 rapid-fire Q&As | – |

---

## How to study

1. Read **00** first: it gives you the map and the answer method.
2. Do **01 → 04** in order (fundamentals → RAG), then **05–06** (evaluation): this is the core of most interviews.
3. Then **07–09** (agents) and **10** (security).
4. Then **11–14** depending on the role (framework user, platform engineer, ML engineer).
5. Finish with **15**: practise the case studies out loud, then drill the question bank.

For every chapter: read the README, **run the Java file**, change something (a threshold, a prompt, k, a budget) and predict the result before re-running, then answer the interview questions aloud.

**Maths background** for the deeper parts (attention, embeddings, losses, LoRA) is in [Java-dsa/Maths](../Maths/README.md).

> The AI tooling ecosystem moves fast. The concepts here are stable; specific product names, flags and API fields change, so check the current docs before relying on exact syntax.
