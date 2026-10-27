# 00 · AI Interview Playbook and Glossary

AI engineering interviews now test three things:
1. **Concepts**: can you explain how an LLM, RAG or an agent works, simply and correctly?
2. **Design**: can you build a reliable, safe, affordable system around a model that's sometimes wrong?
3. **Production judgement**: evaluation, guardrails, cost, latency, observability, deployment.

This chapter gives you the vocabulary and a repeatable way to answer. The rest of the folder teaches each topic from basics to advanced, with a runnable Java simulation per chapter.

---

## 1. The one picture to keep in your head

```
            ┌──────────────────────── your application ──────────────────────────┐
user ──▶ guardrails ──▶ agent / orchestrator ──▶ LLM gateway ──▶ model provider(s)
 (input)  (ch 10)        (ch 07, 08)          │   (ch 12)        (ch 01, 14)
                           │    ▲              │
                    tools/MCP   │ context      └─▶ cache, routing, fallback, cost, logs
                     (ch 09)    │ (prompt, ch 02)
                           │    │
                      retrieval (RAG, ch 03-04) ◀── vector DB + documents
                           │
output ◀── guardrails ◀────┘          evaluation everywhere (ch 05, 06)
         (output, ch 10)              deployed on Cloud Run / k8s (ch 13)
```

Almost every interview question is about one box in this picture, or the arrow between two boxes.

---

## 2. How to answer any AI design question (the 6-step method)

| Step | Say | Example ("design a support bot for our docs") |
|---|---|---|
| 1. **Clarify** | users, scale, latency, accuracy needs, data, risk | 10k users/day, answers must cite docs, no PII leaks |
| 2. **Baseline** | the simplest thing that could work | prompt + RAG over the docs |
| 3. **Design** | components and the data flow | ingestion → chunk → embed → index; query → retrieve → rerank → generate with citations |
| 4. **Evaluate** | how you'll know it works, *before* launch | golden set of 200 Q&A, recall@5, faithfulness, LLM judge + human spot checks |
| 5. **Harden** | safety, cost, latency, failure modes | input/output guardrails, gateway with fallback and caching, timeouts, rate limits |
| 6. **Operate** | monitoring and iteration | traces, token cost per request, eval regression suite in CI, feedback loop |

**The sentence interviewers love:** *"I'd start with the simplest version, build an evaluation set first, and only add complexity (agents, fine-tuning, multi-agent) when the evals show it's needed."*

---

## 3. Choosing the right technique

```
Does the model already know it well enough?  ──yes──▶  prompt engineering (ch 02)
            │ no
Is it about facts / documents that change?   ──yes──▶  RAG (ch 04)
            │ no
Is it about style, format, or a narrow skill? ──yes──▶  fine-tuning (ch 14)
            │
Does it need to take actions or multiple steps? ─yes─▶  agent with tools (ch 07, 09)
            │
Are there clearly separate roles/specialities? ─yes─▶  multi-agent (ch 08), only if one agent struggles
```

| Need | Best first choice | Why not the others first |
|---|---|---|
| answer from company docs | RAG | fine-tuning doesn't reliably teach facts and goes stale |
| always reply in a JSON schema | structured output / constrained decoding | fine-tuning is overkill |
| a brand voice | prompting, then fine-tuning | |
| book a flight | agent + tools | a plain LLM can't act |
| cheaper/faster at scale | smaller model + routing via a gateway, distillation | |

---

## 4. Glossary (A–Z, the words interviewers expect you to know)

| Term | One-line meaning | Chapter |
|---|---|---|
| **A2A** | Agent2Agent: an open protocol for agents from different vendors to talk to each other | 08 |
| **agent** | an LLM in a loop that decides which tools to call until a goal is met | 07 |
| **ANN** | approximate nearest neighbour search (HNSW, IVF): fast vector search | 03 |
| **attention** | each token mixes in information from the others, weighted by relevance | 01 |
| **BM25** | the classic keyword-relevance formula behind search engines | 03 |
| **chain-of-thought (CoT)** | asking the model to reason step by step before answering | 02 |
| **chunking** | splitting documents into retrievable pieces | 04 |
| **context window** | the maximum tokens a model can read at once (prompt + output) | 01 |
| **context engineering** | deciding what goes into the context window, in what form, at each step | 09 |
| **cross-encoder / reranker** | a model that scores a (query, document) pair together: slower, more accurate | 04 |
| **DPO / RLHF** | ways to align a model with human preferences after pre-training | 01, 14 |
| **embedding** | a vector of numbers representing meaning; similar meaning = nearby vectors | 03 |
| **eval / golden set** | a fixed set of inputs with expected outputs used to measure quality | 05 |
| **faithfulness / groundedness** | is every claim in the answer supported by the retrieved context? | 06 |
| **few-shot** | putting examples in the prompt | 02 |
| **fine-tuning** | further training a model on your data (full, or LoRA/QLoRA adapters) | 14 |
| **function / tool calling** | the model outputs a structured request to call a function you define | 09 |
| **gateway (LLM gateway)** | a proxy in front of models: routing, fallback, caching, keys, rate limits, cost | 12 |
| **guardrails** | checks around the model: block injections, redact PII, validate outputs | 10 |
| **hallucination** | a fluent, confident, false statement | 04, 05 |
| **hybrid search** | combining keyword (BM25) and vector search | 03 |
| **jailbreak** | a prompt that tricks the model into ignoring its safety rules | 10 |
| **KV cache** | stored attention keys/values so generation doesn't recompute the prompt | 01 |
| **LangChain / LangGraph** | popular frameworks for chains, agents and stateful agent graphs | 11 |
| **LLM-as-a-judge** | using a strong model to grade outputs against a rubric | 05 |
| **LoRA** | fine-tuning by training small low-rank matrices instead of all weights | 14 |
| **MCP** | Model Context Protocol: a standard way to plug tools and data into AI apps | 09 |
| **Model Armor** | Google Cloud's service that screens prompts and responses for injections, jailbreaks, sensitive data, malicious URLs and harmful content | 10 |
| **MRR / nDCG / recall@k** | retrieval ranking metrics | 06 |
| **multi-agent** | several specialised agents coordinated by an orchestrator or by handoffs | 08 |
| **observability / tracing** | recording each LLM call, tool call, tokens, latency and cost | 12 |
| **prompt injection** | untrusted text (a web page, an email) that smuggles instructions into the model | 10 |
| **quantisation** | storing weights in fewer bits (int8, int4) to save memory and cost | 14 |
| **RAG** | retrieval-augmented generation: fetch relevant documents, then answer from them | 04 |
| **ReAct** | the Reason → Act (tool) → Observe loop that most agents use | 07 |
| **semantic cache** | reuse an earlier answer when a new prompt *means* the same thing | 12 |
| **structured output** | forcing the model's output to match a JSON schema | 02 |
| **system prompt** | the developer's instructions that frame every conversation | 02 |
| **temperature / top-p** | sampling settings controlling randomness | 01 |
| **token** | a chunk of text (≈ ¾ of an English word) that models read and bill by | 01 |
| **TTFT** | time to first token: the latency users feel with streaming | 12 |
| **tool output compression** | shrinking big tool results (logs, HTML, JSON) to fit the context budget | 09 |
| **vector database** | a store optimised for similarity search over embeddings | 03 |

---

## 5. Ten things that make you sound senior

1. **"Evals first."** You can't improve what you can't measure; build a golden set before tuning prompts.
2. **"Treat the model as an untrusted component."** Validate its outputs like user input; never execute them blindly.
3. **"Least privilege for agents."** Give tools the minimum permissions; require human approval for irreversible actions.
4. **"Retrieval quality caps answer quality."** If the right chunk isn't retrieved, no prompt fixes it. Measure recall@k separately.
5. **"Separate retrieval metrics from generation metrics"** when debugging RAG.
6. **"Put a gateway in front of providers"** for fallback, cost control and observability, and to avoid vendor lock-in.
7. **"Context is a budget."** Compress tool outputs, summarise history, and retrieve instead of stuffing everything in.
8. **"Make agents bounded."** Max steps, timeouts, cost limits, idempotent tools.
9. **"Prompt injection has no complete fix."** Use defence in depth: isolate untrusted content, constrain tools, screen inputs and outputs.
10. **"Start simple."** A well-built single agent with good tools beats a fragile multi-agent system.

---

## 6. The roadmap of this folder

| # | Chapter | Level |
|---|---|---|
| 01 | [LLM fundamentals](../01-LLM-Fundamentals/README.md) | basics |
| 02 | [Prompting and structured output](../02-Prompt-Engineering/README.md) | basics |
| 03 | [Embeddings and vector search](../03-Embeddings-Vector-Search/README.md) | basics |
| 04 | [RAG](../04-RAG/README.md) | core |
| 05 | [LLM evaluation](../05-LLM-Evaluation/README.md) | core |
| 06 | [RAG evaluation](../06-RAG-Evaluation/README.md) | core |
| 07 | [AI agents](../07-AI-Agents/README.md) | core |
| 08 | [Multi-agent systems](../08-Multi-Agent-Systems/README.md) | advanced |
| 09 | [Tools, MCP, context engineering, tool-output compression](../09-Tools-MCP-Context-Engineering/README.md) | advanced |
| 10 | [Guardrails and AI security (Model Armor)](../10-Guardrails-AI-Security/README.md) | advanced |
| 11 | [LangChain, LangGraph and frameworks](../11-LangChain-Frameworks/README.md) | advanced |
| 12 | [LLM gateways and LLMOps](../12-LLM-Gateways-LLMOps/README.md) | advanced |
| 13 | [Deploying an agent to Cloud Run](../13-Deploy-Agent-Cloud-Run/README.md) | advanced |
| 14 | [Fine-tuning, serving and cost](../14-Fine-Tuning-Serving-Cost/README.md) | advanced |
| 15 | [System design case studies and question bank](../15-AI-System-Design-Question-Bank/README.md) | interview |

**Next:** [01 · LLM fundamentals](../01-LLM-Fundamentals/README.md)
