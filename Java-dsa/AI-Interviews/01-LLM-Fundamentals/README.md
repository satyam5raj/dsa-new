# 01 · LLM Fundamentals

| | |
|---|---|
| **Code** | [LlmFundamentals.java](LlmFundamentals.java): a BPE tokenizer, a tiny language model, every sampling method, the KV cache and cost maths (`java LlmFundamentals.java`, 28 checks) |
| **Maths behind it** | [Maths 02 linear algebra](../../Maths/02-Linear-Algebra/README.md), [07 information theory](../../Maths/07-Information-Theory/README.md), [09 attention](../../Maths/09-Advanced-Topics/README.md#8-the-maths-of-attention-transformers) |
| **Interview weight** | very high: every AI interview starts here |

---

## 1. What an LLM actually is

A **large language model** is a neural network trained to do one thing: **predict the next token**.
```
"The capital of France is"  ──▶  model  ──▶  Paris 64%, Lyon 23%, Nice 9%, …
```
To generate text, it repeats that: pick a token, append it, predict again. Everything an LLM appears to do (answer questions, write code, reason, call tools) emerges from doing next-token prediction very well, after training on trillions of tokens.

**The analogy:** an extremely well-read autocomplete. It has read most of the public internet and many books, so the most likely continuation of a good question is often a good answer. But it isn't a database, so it can confidently continue with something false. That's a **hallucination**.

---

## 2. Tokens

Models don't read characters or words; they read **tokens**, chunks of text from a fixed vocabulary (typically 32K–200K entries).

### How a tokenizer is built: Byte-Pair Encoding (BPE)
1. Start with individual characters (or bytes).
2. Count every adjacent pair in a big corpus.
3. **Merge the most frequent pair** into a new token, and repeat thousands of times.

The code trains BPE on a small corpus ("low lower lowest newer newest wider widest …"):
- the first merge is **`l` + `o`** (the most common pair);
- "lowest newest" becomes **`[low, est_, new, est_]`**: 4 tokens instead of 14 characters (`_` marks the end of a word);
- "slowest", which never appeared, still works: **`[s, low, est_]`**. Unknown words are split into known pieces, so nothing is ever "out of vocabulary".

### Why tokens matter in practice
| Fact | Consequence |
|---|---|
| ~**1.3 tokens per English word** (~4 characters per token) | estimate cost and context quickly |
| you're **billed per token** (input and output separately) | long prompts cost money on every call |
| the context window is measured in tokens | |
| other languages and code use more tokens per word | non-English apps cost more |
| models see tokens, not letters | they're bad at "how many r's are in strawberry?" and at character-level tasks |
| numbers split oddly ("12345" may be 2–3 tokens) | weak arithmetic; use a calculator tool |

---

## 3. From tokens to predictions: the transformer

### The pipeline
```
text → tokens → embeddings → [transformer block × N] → scores over the vocabulary → softmax → probabilities
```
1. **Embedding:** each token ID becomes a vector (say 4,096 numbers) by looking up a row of a learned matrix.
2. **Positional information** is added (RoPE in modern models), so word order matters.
3. **Transformer blocks** (dozens of them), each with:
   - **self-attention:** every token looks at the earlier tokens and mixes in the relevant ones. In "The animal didn't cross the street because **it** was tired", attention links "it" to "animal".
   - a **feed-forward network**: per-token processing, where much of the "knowledge" is stored.
   - residual connections and normalisation.
4. **Output:** a score (logit) for every vocabulary token, turned into probabilities by softmax.

The full maths of attention, `softmax(QKᵀ/√d)·V`, is in [Maths 09](../../Maths/09-Advanced-Topics/README.md#8-the-maths-of-attention-transformers).

### A miniature language model you can inspect
The code's **bigram model** just counts which word follows which in "the cat sat on the mat . the cat ate the fish . the dog sat on the rug .":
```
after "the":  cat 2, dog 1, fish 1, mat 1, rug 1      →  P(cat | the) = 2/6 = 0.33
```
A real LLM does the same job, but conditions on **thousands** of previous tokens, using billions of learned parameters instead of a count table.

**Decoder-only vs encoder:** GPT, Claude, Gemini and Llama are **decoder-only** (they predict the next token, and each token sees only the past). **Encoder** models like BERT see the whole text in both directions and are used for embeddings and classification (chapter 03).

---

## 4. Choosing the next token: sampling

The model gives probabilities; a **decoding strategy** picks the token. Example logits for "The capital of France is …" with tokens [Paris, Lyon, Nice, banana, purple]:

| Setting | Probabilities | Effect |
|---|---|---|
| temperature 1.0 | [0.64, 0.23, 0.09, 0.03, 0.01] | the model's own distribution |
| **temperature 0.5** | [0.86, 0.12, 0.02, 0.00, 0.00] | sharper: more predictable |
| **temperature 2.0** | [0.43, 0.26, 0.16, 0.10, 0.06] | flatter: more varied, more errors |
| **top-k = 2** | [0.73, 0.27, 0, 0, 0] | only the 2 best tokens survive (renormalised) |
| **top-p = 0.9** | [0.67, 0.24, 0.09, 0, 0] | the smallest set covering 90% of the probability |

- **Greedy** (always the top token, like temperature → 0) is deterministic but can **loop**. The bigram model's greedy output is "the cat **ate the cat ate**". Real systems add repetition penalties or sampling.
- Sampling picks "banana" about **3%** of the time (100,000 simulated draws agree): that's why high temperature produces odd outputs.

**Practical defaults:** temperature 0–0.3 for extraction, code, classification and agents (consistent tool calls); 0.7–1.0 for creative writing. Use either top-p or temperature changes, not aggressive amounts of both. **Temperature 0 isn't perfectly deterministic** on real infrastructure (batching and floating-point effects), so don't rely on byte-identical outputs.

---

## 5. The context window

The **context window** is the most tokens the model can attend to at once, and it includes **both the prompt and the output**. It ranges from about 8K to 1M+ tokens depending on the model.

**Budgeting** (from the code): an 8,000-token window, a 500-token system prompt, room for a 1,000-token answer and a 200-token question leaves **6,300 tokens** for chat history and retrieved documents. Keeping the newest turns first, 6 of 8 history turns fit (6,000 tokens); the oldest must be dropped or summarised.

**Long context isn't free:**
- cost and latency grow with prompt length (attention is O(n²) in the naive form);
- quality drops for facts buried in the middle of long prompts ("lost in the middle");
- so **retrieval (RAG) and context engineering (chapter 09)** still matter even with huge windows.

---

## 6. Inference: why output is slow and the KV cache

Generation has two phases:
| Phase | What happens | Speed |
|---|---|---|
| **prefill** | process the whole prompt in parallel | fast per token (GPU-friendly) |
| **decode** | generate tokens **one at a time**, each depending on the previous | slower, sequential |

That's why **time to first token (TTFT)** and **tokens per second** are separate latency metrics, and why output tokens cost several times more than input tokens.

### The KV cache
During attention, each token produces "keys" and "values". Without caching, every new token would recompute them for the **entire** sequence again. The **KV cache** stores them, so each step only processes the one new token.

For a 1,000-token prompt and 200 generated tokens:
| | Token positions processed |
|---|---|
| without a KV cache | **220,100** |
| with a KV cache | **1,200** |

That's **183×** less work. The price is **memory**: the cache is `2 × layers × KV-heads × head-dim × tokens × bytes`. For a typical 7–8B model (32 layers, 8 KV heads × 128 dims, fp16) at 8K tokens, that's **1.07 GB per request**, so **20 concurrent requests need about 21.5 GB just for KV cache**. This is why long context and high concurrency are expensive, and why serving systems use paged attention (vLLM), grouped-query attention (GQA, fewer KV heads) and **prompt caching** (reusing the KV cache for a shared prefix like a long system prompt).

---

## 7. How LLMs are trained (the three stages)

| Stage | Data | Result |
|---|---|---|
| 1. **Pre-training** | trillions of tokens of web, books and code; next-token prediction (cross-entropy) | a "base model": knowledgeable, but just continues text |
| 2. **Supervised fine-tuning (SFT / instruction tuning)** | tens of thousands of (instruction, good response) pairs | follows instructions, chats |
| 3. **Preference alignment** (RLHF, DPO, RLAIF / constitutional methods) | humans or AI rank responses; the model is optimised towards the preferred ones | more helpful, honest and harmless |

Then: **reasoning models** are further trained (often with reinforcement learning on verifiable tasks like maths and code) to "think" in long hidden chains before answering, trading latency and cost for accuracy.

**Knowledge cutoff:** a model knows nothing after its training data ends. That's another reason for RAG and tools.

---

## 8. Memory and cost: back-of-the-envelope maths

### Model memory
Each parameter is stored in some number of bytes:
| 7B parameters in… | Memory |
|---|---|
| fp16 / bf16 (2 bytes) | **14 GB** |
| int8 (1 byte) | **7 GB** |
| int4 (0.5 bytes) | **3.5 GB** |

Rule: **memory ≈ parameters × bytes per parameter**, plus the KV cache and overhead. Quantisation (chapter 14) is how big models fit on smaller GPUs.

### API cost
With an example price of $3 per million input tokens and $15 per million output tokens:
```
one request: 4,000 input + 500 output  =  0.004 × $3 + 0.0005 × $15 = $0.0195
1,000,000 requests a month             ≈  $19,500
```
Levers to cut cost (chapter 12): shorter prompts, **prompt caching**, a **smaller model** for easy requests (routing), semantic caching, batching, and limiting output length.

---

## 9. Model families and choosing one

| Kind | Examples | Notes |
|---|---|---|
| frontier closed models | Claude, GPT, Gemini | strongest; API only; pay per token |
| open-weight models | Llama, Mistral, Qwen, Gemma, DeepSeek | self-host or fine-tune; you run the infrastructure |
| small / edge models | 1–8B models | cheap, fast, private; weaker reasoning |
| specialised | embedding models, rerankers, code models, speech, vision | |
| reasoning models | "thinking" variants | better at maths and planning; slower and pricier |
| **multimodal** | text + images (+ audio, video) | images are also turned into tokens |

**How to choose:** quality on **your** eval set (chapter 05) × latency × cost × context length × data-privacy requirements. Often: a strong model for hard steps, and a small one for routing, classification and summarisation.

---

## 10. Limitations to always mention

1. **Hallucination:** fluent but false. Mitigate with RAG, citations, tools, and verification.
2. **No real-time knowledge** past the training cutoff.
3. **Non-determinism:** the same prompt can give different answers.
4. **Prompt sensitivity:** small wording changes can change results.
5. **Weak at exact computation and counting characters:** use tools (code interpreter, calculator).
6. **Security:** follows instructions found in data (prompt injection, chapter 10).
7. **Bias** inherited from training data.

---

## 11. Interview questions

**Q1. How does an LLM generate text?**
> It predicts a probability distribution over the next token given all previous tokens, picks one (greedy or sampling), appends it, and repeats until a stop token or length limit.

**Q2. What's a token, and why does it matter?**
> A sub-word unit from a learned vocabulary (BPE). Context limits, cost and latency are all measured in tokens; ~1.3 tokens per English word. Tokenisation explains weaknesses with spelling and arithmetic.

**Q3. Explain temperature, top-k and top-p.**
> Temperature divides the logits before the softmax (low = sharper, high = flatter). Top-k keeps the k most likely tokens; top-p keeps the smallest set whose probabilities add up to p. All of them trade consistency for diversity.

**Q4. What is the KV cache, and what's its downside?**
> It stores each token's attention keys and values so decoding only processes the new token (orders of magnitude less compute). The downside is memory that grows with sequence length × concurrency, which is the main constraint on long-context serving.

**Q5. Why are output tokens more expensive than input tokens?**
> Input is processed in parallel during prefill; output is generated sequentially, one token per forward pass, so it uses far more GPU time per token.

**Q6. What are the stages of training an LLM?**
> Pre-training on massive text (next-token prediction), supervised instruction tuning, then preference alignment (RLHF, DPO). Reasoning models add RL on verifiable tasks.

**Q7. Why do LLMs hallucinate?**
> They're trained to produce likely text, not true text; when the knowledge is missing or uncertain, a plausible continuation still has high probability. Grounding with retrieval and tools, plus evaluation, reduces it.

**Q8. How much GPU memory does a 70B model need in fp16?**
> About 140 GB for the weights alone (70B × 2 bytes), plus the KV cache: several GPUs, or quantise to int4 (~35 GB).

---

**Next:** [02 · Prompt engineering and structured output](../02-Prompt-Engineering/README.md)
