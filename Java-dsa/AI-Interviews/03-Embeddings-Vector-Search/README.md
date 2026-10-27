# 03 · Embeddings and Vector Search

| | |
|---|---|
| **Code** | [VectorSearch.java](VectorSearch.java): BM25, toy embeddings, hybrid search with RRF, an IVF approximate index, int8 quantisation (`java VectorSearch.java`, 17 checks) |
| **Maths behind it** | dot product and cosine similarity ([Maths 02](../../Maths/02-Linear-Algebra/README.md)), high-dimensional geometry ([Maths 09](../../Maths/09-Advanced-Topics/README.md#6-high-dimensional-geometry-intuition-breaks)) |
| **Interview weight** | high: the "R" in RAG; every RAG question leads here |

Search is the foundation of RAG (chapter 04), memory for agents, recommendations and deduplication. There are two families: **keyword search** (match the words) and **semantic search** (match the meaning). Production systems usually use **both**.

---

## 1. Keyword search with BM25

### The idea
Score a document higher when it contains the query's words, especially **rare** words, without letting long documents or repeated words dominate.

```
BM25(q, d) = Σ over query terms t of  IDF(t) · tf·(k₁+1) / (tf + k₁·(1 − b + b·|d|/avgLen))
```
| Piece | Meaning |
|---|---|
| **IDF** (inverse document frequency) | rare words matter more: "ERR-4521" beats "the" |
| **tf** with saturation (k₁ ≈ 1.2) | the 10th mention of a word adds less than the 1st |
| **length normalisation** (b ≈ 0.75) | long documents don't win just by being long |

### What it's good and bad at (from the code)
| Query | BM25 result |
|---|---|
| `ERR-4521` | ✅ finds the error document (exact codes, IDs, names, jargon) |
| `home insurance` | ✅ the home-insurance document |
| `automobile insurance` | ❌ ranks "**automobile** repair shops" first; it can't know automobile = car |

BM25 needs no training, is fast and explainable, and is excellent for exact terms. It fails on **synonyms and paraphrases**. Elasticsearch, OpenSearch and Lucene use it.

---

## 2. Embeddings: vectors that capture meaning

### The idea
An **embedding model** maps text to a vector (say 384–3,072 numbers) such that **texts with similar meaning get nearby vectors**. "car" and "automobile" end up close; "car" and "dog" far apart.

The code uses a toy version: each word gets a vector over hidden "meaning axes" (vehicle, insurance, home, animal/food, account, error/payment), and a text is the average of its word vectors.
- cos(car, automobile) = **1.00**; cos(car, dog) = **0.00**.
- `automobile insurance` now finds the **car-insurance** document first ✅.
- But `ERR-4521` is unknown to the embedding: **every score is 0** ❌. Embeddings are weak on rare exact strings, codes and new jargon.

Real embedding models learn their axes from billions of sentences, using **contrastive learning**: pull matching pairs (a question and its answer) together and push random pairs apart.

### Practical facts
| Topic | Guidance |
|---|---|
| model choice | check retrieval benchmarks (e.g. MTEB) and, more importantly, **your own eval set** |
| query vs document | some models expect prefixes like "query:" / "passage:" (asymmetric search) |
| dimensions | more dims = more nuance, more memory; some models allow truncation (Matryoshka embeddings) |
| re-embedding | **changing the embedding model means re-embedding every document**: vectors from different models aren't comparable |
| max input | embedding models have token limits; long documents must be chunked (chapter 04) |
| multilingual / multimodal | cross-language and image–text models exist (search images with text) |

---

## 3. Similarity metrics

| Metric | Formula | Notes |
|---|---|---|
| **cosine** | a·b / (\|a\|\|b\|) | ignores length, compares direction; the most common for text |
| **dot product** | a·b | rewards length too: (1,1)·(1,0) = 1.0 but (10,10)·(1,0) = 10.0 |
| **Euclidean (L2)** | \|a − b\| | |

**Normalise vectors to unit length once at index time**: then dot product = cosine (the code checks this), and dot product is the fastest to compute. Use the metric the embedding model was trained with.

---

## 4. Hybrid search: the best of both

Run **BM25 and vector search**, then merge the two ranked lists. The standard merge is **Reciprocal Rank Fusion (RRF)**:
```
RRF(d) = Σ over each ranking of  1 / (k + rank of d)        (k = 60)
```
It uses only ranks, so it doesn't need to compare BM25 scores with cosine scores (they're on different scales).

| Query | BM25 alone | vectors alone | **hybrid (RRF)** |
|---|---|---|---|
| automobile insurance | ❌ | ✅ | **✅** |
| payment error ERR-4521 | ✅ | partly | **✅** |

**Hybrid search is the default recommendation for production RAG.** Most vector databases support it, often with a reranker on top (chapter 04).

---

## 5. Vector databases and approximate nearest neighbour (ANN) search

### The problem
Exact search compares the query with **every** vector: 20,000 comparisons here, a billion at large scale. That's too slow, so we use **approximate** indexes that look at only a small fraction and accept a small chance of missing a true neighbour.

### IVF (inverted file index): the idea in the code
1. **Cluster** all vectors with k-means (100 clusters).
2. Store each vector in its cluster's list.
3. At query time, find the **nprobe** closest cluster centres and search only those lists.

Results on 20,000 clustered 32-d vectors (200 queries):
| nprobe | recall@10 | vectors compared per query |
|---|---|---|
| 1 | **0.73** | 339 (1.7% of the data) |
| **4** | **1.00** | 925 |
| 10 | 1.00 | 2,088 |
| 30 | 1.00 | 6,044 |
| brute force | 1.00 | 20,000 |

**nprobe is the speed/recall knob**: one probe misses true neighbours sitting just across a cluster boundary, and a few probes fix it. (This data is strongly clustered; real data usually needs more probes for the same recall.)

### The main index families
| Index | Idea | Trade-off |
|---|---|---|
| **flat (brute force)** | compare everything | exact; fine up to ~100K vectors |
| **IVF** | clusters + probe a few | fast, needs training; tune nprobe |
| **HNSW** | a multi-layer "small world" graph; greedily walk towards the query | **the most popular**: great recall and speed, more memory; tune `ef`/`M` |
| **PQ (product quantisation)** | compress vectors into short codes | huge memory savings; some accuracy loss |
| **DiskANN / ScaNN** | disk-based or optimised graph and quantisation methods | billion scale |

### Quantisation
Store each number as **int8 instead of float32**: 64 dimensions take **64 bytes instead of 256** (4× smaller), and in the code the top-1 result was identical for **100 of 100** queries. Binary quantisation (1 bit per dimension) is 32× smaller, usually followed by re-scoring the top candidates with full vectors.

### Choosing a vector store
| Option | When |
|---|---|
| **pgvector** (Postgres) | you already use Postgres; up to millions of vectors; joins with business data |
| Elasticsearch / OpenSearch | you need keyword + vector hybrid search and filters |
| dedicated (Pinecone, Weaviate, Qdrant, Milvus) | large scale, managed, rich filtering |
| cloud-native (Vertex AI Vector Search, AlloyDB, etc.) | you're on that cloud |
| FAISS / hnswlib (libraries) | in-process, research, batch jobs |

**Metadata filtering** (tenant, date, permissions) is essential, both for relevance and for **security**: one customer must never retrieve another customer's chunks. Filter *inside* the search, not after it (post-filtering can return too few results).

---

## 6. Interview questions

**Q1. What is an embedding?**
> A dense vector representation of text (or images) produced by a model trained so that similar meanings land close together. Cosine similarity between embeddings measures semantic similarity.

**Q2. BM25 vs semantic search: when does each fail?**
> BM25 misses synonyms and paraphrases; embeddings miss exact identifiers, codes, rare names and new jargon. Hybrid search with RRF combines them.

**Q3. How does HNSW work?**
> It builds a layered proximity graph: sparse long-range links on top, dense local links at the bottom. A query greedily walks from the top layer down towards nearer and nearer nodes. It's approximate, tunable with `ef` (search breadth) and `M` (links per node).

**Q4. What's the trade-off in ANN search?**
> Recall vs latency vs memory. More probes (IVF) or a larger `ef` (HNSW) raise recall but cost time; quantisation saves memory but loses some accuracy, often recovered by re-scoring the top candidates.

**Q5. What happens if you change your embedding model?**
> All stored vectors must be regenerated with the new model (vectors from different models live in different spaces), and the retrieval evals re-run. Plan re-indexing as a migration.

**Q6. How do you enforce access control in vector search?**
> Store tenant and permission metadata with each chunk and apply it as a pre-filter inside the query (or use separate indexes or namespaces per tenant). Never rely on the LLM to hide data it was given.

**Q7. Why normalise embeddings?**
> So dot product equals cosine similarity: faster, and consistent scores across documents of different lengths.

---

**Next:** [04 · RAG](../04-RAG/README.md)
