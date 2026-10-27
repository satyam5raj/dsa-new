# 06 · RAG Evaluation

| | |
|---|---|
| **Code** | [RagEvaluation.java](RagEvaluation.java): hit rate, precision@k, recall@k, MRR, nDCG, RAGAS-style faithfulness / answer relevance / context precision / context recall, and a retriever-vs-generator diagnosis (`java RagEvaluation.java`, 23 checks) |
| **You need** | [04 · RAG](../04-RAG/README.md), [05 · LLM evaluation](../05-LLM-Evaluation/README.md) |
| **Interview weight** | high: "your RAG gives wrong answers; how do you debug it?" |

**The key idea: a RAG system has two parts that fail in different ways, so measure them separately.**
```
question ──▶ RETRIEVER ──▶ context ──▶ GENERATOR ──▶ answer
             did we fetch the          did we answer correctly
             right chunks?             and ONLY from the context?
```
If the right chunk never reaches the prompt, no prompt engineering can fix the answer.

---

## 1. The evaluation dataset

For each test question, store:
| Field | Used for |
|---|---|
| question | the input |
| **relevant chunk/document IDs** | retrieval metrics |
| **reference (ground-truth) answer** | answer correctness, context recall |
| metadata (category, difficulty, single- vs multi-hop) | slicing results |

**How to build it:** sample real user questions and have experts label the relevant documents and answers. Or **generate synthetically**: pick a chunk, have an LLM write a question that the chunk answers (the chunk is then the relevant ID), and review a sample. Synthetic questions tend to reuse the chunk's wording, which makes keyword retrieval look too good, so ask the generator to paraphrase. Include questions with **no** answer in the corpus, to test "I don't know".

---

## 2. Retrieval metrics

Four test queries, top 5 retrieved each (from the code):
| Query | Retrieved (in order) | Relevant |
|---|---|---|
| q1 | d3, **d1**, d7, **d2**, d9 | d1, d2 |
| q2 | **d5**, d4, d8, d6, d0 | d5 |
| q3 | d2, d6, d4, d1, d3 | d9 (missed entirely) |
| q4 | d7, **d8**, **d0**, **d4**, d5 | d0, d4, d8 |

| Metric | Question it answers | Formula | q1 value |
|---|---|---|---|
| **hit rate@k** | did *any* relevant chunk appear in the top k? | 1 or 0, averaged | all queries: **0.750** (q3 missed) |
| **precision@k** | what fraction of the top k is relevant? (noise in the prompt) | relevant in top k / k | @3 = **0.333** |
| **recall@k** | what fraction of the relevant chunks did we get? | relevant in top k / total relevant | @3 = **0.500** |
| **MRR** | how high is the *first* relevant chunk? | mean of 1/rank of the first hit | q1 = 0.5; mean **0.500** |
| **nDCG@k** | are relevant chunks near the top? (position-weighted) | DCG / ideal DCG, where DCG = Σ rel / log₂(rank + 1) | q1 **0.651**; mean **0.596** |

**Which to use:**
- **recall@k is the most important retrieval metric for RAG**: if the needed chunk isn't retrieved, the answer can't be right.
- **MRR / nDCG** matter because models attend best to the first and last chunks, and you'll truncate to a few.
- **precision@k** tells you how much distracting noise reaches the model.

### Choosing k
| k | mean recall | mean precision |
|---|---|---|
| 1 | 0.250 | 0.250 |
| 3 | 0.542 | 0.333 |
| 5 | 0.750 | 0.300 |

Recall never drops as k grows. Precision is **not** strictly monotonic (here it rose from k = 1 to 3), but past the ranks where the relevant chunks sit, extra chunks only add noise (precision@5 < precision@3). The usual pattern: **retrieve with a large k for recall, then rerank and keep a small k for precision** (chapter 04).

---

## 3. Generation metrics (RAGAS-style)

These are usually computed with an LLM judge or an NLI (natural-language inference) model. The code uses a simple "supported" check (≥ 80% of a claim's content words appear in one context chunk) so you can see exactly how each metric works.

Question: *"How many days of paid leave do employees get and can it carry over?"* Retrieved context: (1) "Full-time employees get 24 days of paid leave per year." (2) "The office is closed on public holidays." (3) "Unused leave can carry over up to 5 days."

### Faithfulness (groundedness): is every claim backed by the context?
```
faithfulness = claims supported by the context / total claims in the answer
```
| Answer | Faithfulness |
|---|---|
| "Employees get 24 days of paid leave per year. Unused leave can carry over up to 5 days." | **1.000** |
| "Employees get 24 days of paid leave per year. Leave can be cashed out at 150 percent salary." | **0.500**: the second claim is invented |

**This is the main hallucination metric for RAG.** How the judge version works: (1) split the answer into atomic claims; (2) for each claim, ask "can this be inferred from the context?"; (3) take the fraction.

### Answer relevance: does it actually address the question?
An answer can be perfectly faithful and still useless. "The office is closed on public holidays." has **faithfulness 1.000** (it's in the context) but **low relevance** (it doesn't answer the question). RAGAS computes relevance by generating questions from the answer and measuring their similarity to the original question; the code uses the share of question words addressed.

### Answer correctness
Compare with the reference answer (semantic similarity, plus factual overlap of claims, or an LLM judge with the reference).

---

## 4. Context metrics: judging the retrieved context against the question

| Metric | Question | Example |
|---|---|---|
| **context precision** | are the **useful** chunks ranked at the **top**? (rank-weighted) | useful chunks at ranks 1 and 3 → **0.833**; at ranks 1 and 2 → **1.000** |
| **context recall** | does the context contain **everything needed** for the reference answer? | all needed chunks present → **1.000**; carry-over chunk missing → **0.500** |

Context recall needs a reference answer: it checks whether each claim **of the reference** can be found in the retrieved context. That's what makes it a retrieval metric you can compute without labelled chunk IDs.

---

## 5. Diagnosis: retriever or generator?

| Context recall | Faithfulness | Correctness | Diagnosis | Fixes |
|---|---|---|---|---|
| **low** | – | low | **retrieval** problem | chunking, hybrid search, query rewriting, larger k, reranker, better embeddings, metadata filters |
| high | **low** | low | **grounding** problem: the model adds things | "only from the context" instructions, citations, lower temperature, a faithfulness check before returning |
| high | high | **low** | **reasoning or completeness** problem | a stronger model, better instructions, examples, decomposition |
| high | high | high | healthy | monitor for drift |

The code's `diagnose()` encodes this table. In an interview, saying "I'd look at context recall first, then faithfulness" shows you know how to debug RAG systematically.

---

## 6. End-to-end and online RAG evaluation

- **End-to-end correctness** (an LLM judge with the reference answer) is the headline number. The component metrics explain *why* it moves.
- **Citation accuracy:** do the cited chunks actually support the sentence they're attached to?
- **"I don't know" behaviour:** on unanswerable questions, does the system abstain? Measure the abstain rate on answerable vs unanswerable questions (you want high on the second, low on the first).
- **Latency and cost per stage:** retrieval, rerank, generation.
- **Online:** user feedback, sampled faithfulness judging on live traffic, the rate of "no relevant documents", escalations.
- **Re-run the retrieval evals** whenever you change the chunking, the embedding model, k, the reranker, or the corpus.

### Tools
**Ragas** (faithfulness, answer relevancy, context precision/recall), **TruLens** (the "RAG triad": context relevance, groundedness, answer relevance), DeepEval, LangSmith, Arize Phoenix, Vertex AI evaluation. Under the hood, all of them do what this chapter's code does, using an LLM as the "supported?" and "relevant?" judge.

---

## 7. Interview questions

**Q1. How do you evaluate a RAG system?**
> Separately: retrieval with recall@k, MRR/nDCG and hit rate against labelled relevant chunks (or context recall against reference answers); generation with faithfulness, answer relevance and correctness (an LLM judge or NLI). Then end-to-end correctness, citation accuracy, abstention on unanswerable questions, latency and cost, sliced by question type.

**Q2. What is faithfulness, and how is it computed?**
> The fraction of the answer's claims that are supported by the retrieved context. Decompose the answer into atomic claims and have a judge (LLM or NLI) verify each against the context.

**Q3. Users say the bot gives wrong answers. How do you debug it?**
> Collect the failing cases; check whether the right chunk was retrieved (context recall/recall@k). If not, fix retrieval (chunking, hybrid search, rewriting, reranking). If it was retrieved, check faithfulness: if that's low, fix the grounding prompt or add verification; if faithful but wrong, the model or the instructions are the problem. Add the cases to the eval set.

**Q4. Precision@k vs recall@k for RAG: which matters more?**
> Recall: a missing chunk makes a correct answer impossible. Precision matters second, because noise distracts the model; so retrieve broadly, then rerank to a small, precise set.

**Q5. How do you build a RAG eval set quickly?**
> Sample real queries and label the relevant chunks and answers; supplement with LLM-generated questions from chunks (paraphrased, then reviewed); include multi-hop and unanswerable questions; stratify by topic.

**Q6. What's the difference between context precision and context recall?**
> Context precision: are the useful chunks ranked first (signal vs noise at the top)? Context recall: does the retrieved context contain all the information the reference answer needs?

---

**Next:** [07 · AI agents](../07-AI-Agents/README.md)
