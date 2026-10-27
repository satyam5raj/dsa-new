# 04 · RAG: Retrieval-Augmented Generation

| | |
|---|---|
| **Code** | [RagPipeline.java](RagPipeline.java): chunking, indexing with metadata, filtered retrieval, reranking, query rewriting, grounded generation with citations, a faithfulness check (`java RagPipeline.java`, 18 checks) |
| **You need** | [03 · Embeddings and vector search](../03-Embeddings-Vector-Search/README.md) |
| **Interview weight** | **the most asked topic** in applied AI interviews |

---

## 1. What RAG is, and why

**RAG** = before the model answers, **retrieve** relevant documents and put them in the prompt, so the model answers **from your data**.

```
question ──▶ retrieve relevant chunks ──▶ prompt = instructions + chunks + question ──▶ LLM ──▶ answer with citations
```

**The analogy:** an open-book exam. The model is a smart student who hasn't read your company's handbook; RAG hands them the right pages before each question.

### Why not just fine-tune or use a long context?
| Option | Problem |
|---|---|
| rely on the model's knowledge | it doesn't know your private or recent data; it hallucinates |
| fine-tune on the documents | expensive, slow to update, poor at reliably memorising facts, no citations |
| paste everything into a huge context | costly per request, slower, "lost in the middle", can't scale to millions of docs |
| **RAG** | up to date (re-index, don't retrain), cites sources, works with access control, cheaper |

---

## 2. The two pipelines

```
INGESTION (offline, on document changes)              QUERY (online, per request)
documents → clean/parse → chunk → embed → index        question → (rewrite) → retrieve top-k → rerank
           (PDF, HTML,    (+ metadata:      (vector +      → build prompt → LLM → answer + citations
            tables, OCR)   source, dept,     keyword)       → (verify / guardrails)
                           date, ACL)
```

---

## 3. Ingestion

### Parsing is underrated
PDFs, slides, tables, scanned documents and HTML boilerplate are the source of many RAG failures. Garbage in, garbage retrieved. Use layout-aware parsers, keep the table structure (convert to markdown), and strip navigation menus and footers.

### Chunking: splitting documents into retrievable pieces
| Strategy | Idea | Pros / cons |
|---|---|---|
| **fixed size** (e.g. 512 tokens) | cut every N tokens | simple; can cut facts in half |
| **fixed size + overlap** | each chunk repeats the end of the previous one | fewer split facts; more chunks to store |
| **sentence / paragraph** | split on natural boundaries | keeps facts intact; uneven sizes |
| **recursive** | try paragraphs, then sentences, then words, until chunks fit | the common default (LangChain's RecursiveCharacterTextSplitter) |
| **structure-aware** | split by headings or sections, keeping the heading path as metadata | great for docs, wikis, code |
| **semantic** | split where the embedding similarity between sentences drops | topic-coherent; costlier |

**The demo (from the code):** with 12-word chunks and **no overlap**, the sentence "Contractors are not eligible for paid leave…" is **cut in two**: neither half can answer "are contractors eligible?". With a **6-word overlap**, one chunk holds it whole, at the cost of 8 chunks instead of 5. **Sentence chunking** keeps every fact intact.

**Chunk-size trade-off:**
- small chunks → precise retrieval, but missing surrounding context;
- large chunks → more context, but diluted embeddings and more tokens.

Common starting points are 200–800 tokens with 10–20% overlap. **Tune it with retrieval evals** (chapter 06).

### Metadata: store it with every chunk
Source, title, section, date, department, tenant, **access-control list**. It enables:
- **filtering** (only HR docs, only the last year, only this customer's data);
- **citations** (link back to the source);
- **security** (section 5).

---

## 4. Retrieval

### Retrieve, then rerank (two stages)
1. **Retrieve** a generous top-k (20–100) with fast **hybrid search** (BM25 + vectors; chapter 03).
2. **Rerank** with a **cross-encoder**: a model that reads the query and each chunk **together** and scores relevance precisely. It's too slow for millions of chunks but fine for 50.
3. Keep the top 3–10 for the prompt.

**The demo:** for "How many days of paid leave do **contractors** get?", BM25 ranks the **full-time** rule first ("Full-time employees get 24 days of paid leave…"), because the common words *days, paid, leave* outweigh *contractors*. A reranker reads them together and moves **"Contractors are not eligible for paid leave…"** to the top:
```
BM25:     [leave-policy#0, leave-policy#2, …]      ← wrong chunk first
reranked: [leave-policy#2, leave-policy#0, …]      ← right
```
(The code's reranker is a simple stand-in that rewards the query's most specific term; real ones are trained models such as Cohere Rerank, BGE-reranker, or cross-encoders from sentence-transformers.)

### Query transformation
User questions are often poor search queries.
| Technique | What it does | Example |
|---|---|---|
| **query rewriting** | turn a follow-up into a standalone question using the chat history | "How long does it take?" → "How long does it take for travel expenses to be reimbursed?" |
| **multi-query** | generate several phrasings, retrieve for each, merge | catches different vocabulary |
| **HyDE** | have the LLM write a hypothetical answer, then search with *that* | answers look more like documents than questions do |
| **decomposition** | split a multi-part question into sub-questions | "compare A and B" → retrieve A, retrieve B |
| **routing** | pick the right index or tool (docs vs SQL vs web) | |

**The demo:** the raw follow-up "How long does it take?" retrieves an **irrelevant** chunk (it matches "take" in "may **take** unpaid leave"). After rewriting, it retrieves **"Travel expenses are reimbursed within 30 days…"**. **Conversational RAG must rewrite follow-ups.**

---

## 5. Security: access control in retrieval

**Never rely on the LLM to keep secrets that are in its context.** If a chunk reaches the prompt, assume it can reach the user (prompt injection, chapter 10).

So **filter at retrieval time, before scoring**:
- An employee asking "What is the senior engineer salary band?" gets **nothing** from `hr-confidential`.
- An HR admin with access gets `salary-bands#0`.

Implement it with per-chunk ACL metadata and pre-filters, separate indexes per tenant, or row-level security (e.g. pgvector + Postgres RLS). Also keep the index in sync when permissions change or documents are deleted (the right to be forgotten).

---

## 6. Generation: grounded answers with citations

### The prompt (from the code)
```
Answer using ONLY the context below. Cite chunk ids in [brackets].
If the answer is not in the context, say "I don't know".

<context>
[expense-policy#1] The daily meal allowance for business travel is 50 dollars.
…
</context>

Question: What is the daily meal allowance for business travel?
```
→ "The daily meal allowance for business travel is 50 dollars. **[expense-policy#1]**"

### Three rules that prevent most RAG hallucinations
1. **"Only from the context"**, with an explicit **way out** ("I don't know"). For "What is the CEO's favourite colour?" the system answers **"I don't know based on the available documents."** instead of inventing something.
2. **Citations** for every claim, so users and evaluators can verify.
3. A **relevance threshold**: if the best retrieval score is too low, don't call the model at all; say you don't know, or route to a human.

### Verify faithfulness
Check that the answer's claims are **supported by the retrieved context**. The code's simple version requires every answer sentence to appear in the context: the real answer passes, and a hallucinated "**75** dollars" is **caught**. Production uses an LLM judge or an NLI model for this (chapter 06).

---

## 7. Advanced RAG patterns

| Pattern | Idea | Use when |
|---|---|---|
| **parent-document / small-to-big** | retrieve small precise chunks, then pass their larger parent section to the model | precise matching *and* enough context |
| **contextual chunk headers / contextual retrieval** | prepend the document title or an LLM-written summary to each chunk before embedding | chunks that are ambiguous on their own ("it costs $50") |
| **sentence-window retrieval** | retrieve a sentence, include its neighbours | fine-grained facts |
| **hierarchical / summary indexes** | first find the right document via summaries, then chunks | large collections |
| **GraphRAG / knowledge graphs** | extract entities and relations; answer "global" questions by traversing or summarising communities | multi-hop questions ("which suppliers of X are in Y?") and corpus-wide themes |
| **agentic RAG** | an agent decides when and what to retrieve, can search several times and check its answer | complex research questions (chapter 07) |
| **corrective / self-RAG** | grade the retrieved docs; if they're poor, rewrite the query or fall back to web search | noisy corpora |
| **structured + unstructured** | text-to-SQL for tables, RAG for text, routed by question type | business data |
| **caching** | cache embeddings, retrieval results and answers for frequent questions | cost and latency (chapter 12) |

---

## 8. Common RAG failure modes (and fixes)

| Symptom | Likely cause | Fix |
|---|---|---|
| "I don't know" although the answer exists | the retriever missed it | hybrid search, better chunking, query rewriting, larger k + reranker |
| right chunk retrieved, wrong answer | too many distracting chunks; lost in the middle | rerank, fewer and better chunks, put the best chunk first or last |
| confident wrong answer | no grounding instruction; low relevance | the "only from context" rule, a threshold, a faithfulness check |
| answer mixes two documents | chunks lack context | contextual headers, parent-document retrieval |
| outdated answer | the index isn't refreshed | incremental re-indexing, date metadata, prefer the newest |
| leaks restricted data | no ACL filtering | pre-filter by permissions |
| tables answered badly | parsing destroyed the table structure | table-aware parsing, markdown tables, text-to-SQL |
| slow and expensive | huge k, a big model for everything | rerank to small k, caching, a smaller model for simple questions |

**Debugging rule:** first check **retrieval** (is the right chunk in the top-k? recall@k), and only then **generation** (given the right chunk, is the answer faithful and complete?). Chapter 06 measures each separately.

---

## 9. Interview questions

**Q1. Walk me through a RAG system.**
> Ingestion: parse → chunk (with overlap or structure) → embed → index with metadata. Query: rewrite if conversational → hybrid retrieve top-k with ACL filters → rerank → build a prompt with instructions to answer only from the context and cite → generate → optionally verify faithfulness → return the answer with sources. Measure retrieval and generation separately.

**Q2. How do you choose the chunk size?**
> It's a precision vs context trade-off; start around 300–800 tokens with ~15% overlap, respect document structure, and tune using retrieval metrics on a golden set.

**Q3. Why rerank?**
> First-stage retrieval (bi-encoder/BM25) scores the query and document separately for speed; a cross-encoder reads them together and is much more precise, so you retrieve broadly and rerank the top few dozen.

**Q4. How do you reduce hallucinations in RAG?**
> Retrieve well (hybrid + rerank), instruct the model to answer only from the context with an "I don't know" path, require citations, apply a relevance threshold, and check faithfulness with an evaluator before returning.

**Q5. How do you handle follow-up questions?**
> Rewrite them into standalone queries using the conversation history (an LLM call) before retrieval; keep a summarised history for the generator.

**Q6. RAG vs fine-tuning?**
> RAG for knowledge that changes or must be cited and access-controlled; fine-tuning for behaviour, style or format, or narrow skills. They combine well: fine-tune for format, RAG for facts.

**Q7. How do you secure a multi-tenant RAG system?**
> Tenant and ACL metadata on every chunk, enforced as pre-filters (or separate indexes); never trust the model to withhold data in its context; audit logging; screen retrieved content for injected instructions.

**Q8. What is GraphRAG, and when is it worth it?**
> Building a knowledge graph of entities and relations (often with community summaries) from the corpus, to answer multi-hop or corpus-wide questions that chunk retrieval handles poorly. It's costlier to build, so use it when those question types matter.

---

**Next:** [05 · LLM evaluation](../05-LLM-Evaluation/README.md)
