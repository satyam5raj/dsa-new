# 11 · Transformers and LLMs

> Every modern LLM (GPT, Claude, Gemini, Llama) is a stack of the same block: **attention**
> plus a small MLP, with residual connections and LayerNorm. This chapter builds each piece,
> trains a tiny GPT, and covers the inference tricks (decoding, KV cache) and the scaling
> arithmetic that interviews ask about.

Run it: `../.venv/bin/python transformers_llms.py` → **37/37 checks** (~6 s on CPU).

---

## 1. Attention: every token looks at every other token

**Intuition:** to understand "it" in *"The animal didn't cross the street because **it** was
tired"*, the model must look back at "animal". Attention lets each token **ask a question
(query)**, compare it with every token's **label (key)**, and take a weighted average of their
**content (values)**.

```
Attention(Q, K, V) = softmax( Q Kᵀ / √d ) V
```

| Symbol | Shape | Meaning |
|---|---|---|
| Q = X W_Q | (T, d) | what each token is looking for |
| K = X W_K | (T, d) | what each token offers |
| V = X W_V | (T, d) | the information each token passes on |
| Q Kᵀ | (T, T) | how well every query matches every key |

A query `[1, 1]` against keys `[1, 0]`, `[0, 1]`, `[5, 5]` gives weights **`[0.002, 0.002, 0.997]`**:
it attends almost entirely to the matching key, and the output is close to that key's value.

**Why divide by √d?** The dot product of two random vectors with unit-variance entries has
**variance d** (measured: 15.97 for d = 16, 253.1 for d = 256). Large scores push the softmax into
saturation. With d = 256, the average max weight was **0.989 unscaled** (nearly one-hot, so tiny
gradients) vs **0.455 scaled**.

**Cost:** the `T × T` score matrix makes attention **O(T²·d)** in compute and O(T²) in memory, so
doubling the context quadruples the attention cost. FlashAttention computes the same thing in
tiles without materialising the T × T matrix; sparse, sliding-window and linear attention variants
trade exactness for longer contexts.

## 2. Masks and multi-head attention

**Causal mask** (GPT-style decoders): token i may only attend to tokens ≤ i.

```
allowed = lower-triangular matrix      scores[~allowed] = −∞  →  weight 0
```

Checked:

- the upper triangle of the weights is 0;
- the first token attends only to itself (weight 1.0);
- **changing the last token doesn't change any earlier output**.

That property is what makes next-token training valid and the KV cache (§7) possible.

**Padding mask:** batches pad short sequences; nobody may attend to a pad token (its column
weight is 0).

**Multi-head attention:** split `d_model` into `h` heads of size `d/h`, run attention in each
independently, concatenate, and project with `W_O`. Different heads learn different relations:
syntax, coreference, position. The from-scratch version matches `torch.nn.MultiheadAttention`
exactly. Parameters: **4 d²** (W_Q, W_K, W_V, W_O); for d = 16 that's 1,024.

Variants used by modern LLMs: **multi-query** (MQA) and **grouped-query attention** (GQA) share
K/V across heads to shrink the KV cache (§7).

## 3. Positional encoding: attention can't see order

Shuffle the input tokens and plain attention's outputs shuffle the same way:
**permutation-equivariant** (checked). "Dog bites man" and "man bites dog" would look identical.
So we **add position information** to the embeddings.

**Sinusoidal encoding** (the original transformer):

```
PE(pos, 2i) = sin(pos / 10000^(2i/d))      PE(pos, 2i+1) = cos(pos / 10000^(2i/d))
```

It has the properties we want (all checked):

- every position gets a **unique** vector;
- the values stay bounded in [−1, 1];
- nearby positions are more similar than distant ones;
- once added, reordering the tokens **changes** the output.

Modern choices:

- learned absolute positions (GPT-2, and the tiny GPT below);
- **RoPE** (rotary: rotates Q and K by a position-dependent angle so attention depends on
  *relative* distance; used by Llama and most recent LLMs);
- **ALiBi** (a linear distance penalty on the scores).

## 4. Tokenisation: byte-pair encoding (BPE)

LLMs read **tokens**, not characters or words.

| Unit | Problem |
|---|---|
| characters | very long sequences |
| words | huge vocabulary, and unknown words |
| **subwords** | the middle ground |

**BPE:** start from characters, then repeatedly merge the most frequent adjacent pair into a new
token.

The corpus is `{low: 5, lower: 2, newest: 6, widest: 3}`:

- The top pairs are `('e','s')`, `('s','t')` and `('t','</w>')`, all tied at **9**. The first
  merge was a 9-count pair; the tie-break here chose `t</w>`. Merges then built `st</w>` and
  `est</w>`.
- After 10 merges:
  - `newest` → `['newest</w>']` (one token);
  - `low` → `['low</w>']`;
  - unseen `lowest` → `['low', 'est</w>']` (built from known pieces);
  - `zoo` → characters.

So there are **no unknown words**, just more tokens. Byte-level BPE (GPT-2 onwards) starts from
the 256 bytes, so any text, emoji or code can be encoded. **WordPiece** (BERT) merges the pair
that most increases the likelihood; **SentencePiece/Unigram** works on raw text, including spaces.

**Why tokenisation matters in interviews:**

- cost and context limits are counted in tokens (~4 English characters per token);
- non-English text and code often need more tokens;
- arithmetic and spelling errors partly come from tokens hiding characters;
- the tokenizer is fixed at pretraining, so changing it later means retraining the embeddings.

## 5. A tiny GPT (decoder-only transformer)

```
tokens → token embedding + position embedding
       → N × [ x = x + Attention(LayerNorm(x))   (causal, multi-head)
               x = x + MLP(LayerNorm(x))      ]  (d → 4d → d, GELU)
       → LayerNorm → linear to vocabulary logits (weights tied with the token embedding)
```

- **Residual connections** (`x + f(x)`) give gradients a straight path; that's why 100-layer
  transformers train at all.
- **Pre-norm** (LayerNorm before each sub-layer) is more stable than the original post-norm.
- **Training objective:** predict the next token at **every** position at once (targets = inputs
  shifted by one), with cross-entropy.

The run: a character-level model on a small repeated text (15-character vocabulary, **105,152**
parameters, 2 layers, d = 64, 4 heads).

| | Value |
|---|---|
| initial loss | **2.773** ≈ ln 15 = 2.708 (uniform guessing) |
| final loss | **0.059**: perplexity 16.0 → **1.06** |
| greedy completion of `the dog sat on the ` | `log. the cat` |

> **A bug caught by the sanity check.** The first version started at loss **45.8**, not 2.7.
> PyTorch's default `nn.Embedding` init is N(0, 1), and because the output layer is tied to the
> embedding, the initial logits were huge and confidently wrong. GPT-2-style init (N(0, 0.02))
> fixed it. That's chapter 08's rule: *the first loss should be ≈ ln(vocab size).*

**Perplexity** = `exp(cross-entropy)` = "the model is as unsure as choosing uniformly among this
many tokens". 16 means clueless; 1.06 means it has nearly memorised this tiny text.

**Encoder vs decoder:**

| | Attention | Pretraining | Use |
|---|---|---|---|
| Encoder-only (BERT) | bidirectional | masked-token prediction | classification, embeddings, search |
| Decoder-only (GPT) | causal | next-token prediction | generation, chat, nearly all LLMs |
| Encoder–decoder (T5, original transformer) | encoder bidirectional + cross-attention | span corruption / translation | translation, summarisation |

## 6. Decoding: turning probabilities into text

Logits `[3, 2, 1, 0.5, −1]` give probabilities `[0.624, 0.229, 0.084, 0.051, 0.011]`.

| Strategy | Rule | Behaviour |
|---|---|---|
| **greedy** | always the argmax | deterministic; can loop or be bland |
| **temperature T** | `softmax(logits / T)` | P(top) is 0.862 at T = 0.5, 0.624 at T = 1, 0.417 at T = 2. Low T = focused, high T = creative/random (higher entropy) |
| **top-k** | sample only from the k best, renormalised | k = 2 keeps tokens {0, 1} |
| **top-p (nucleus)** | the smallest set with cumulative probability ≥ p | p = 0.9 keeps {0, 1, 2} here, but **all 5** on a flat distribution: it adapts to the model's confidence |
| beam search | keep the b best partial sequences | translation and summarisation; rarely used for chat |

Typical settings: factual or code tasks use low temperature (0–0.3); creative writing 0.7–1.0
with top-p 0.9–0.95. Other controls: repetition and frequency penalties, stop sequences,
constrained or structured decoding (JSON schemas).

## 7. KV cache: why generation is fast

When generating token t+1, the keys and values of tokens 1…t **don't change** (that's the causal
mask from §2). So cache them and feed **only the new token** each step.

- Cached and full-recompute generation produced **exactly the same tokens**
  (`the cat sat on the mat. the dog sat`).
- Without a cache each step re-processes the whole prefix, O(T²) total work; with it, each new
  token costs O(T) attention over the cache. (The tiny model's millisecond timings are noisy;
  the gap grows with length and model size.)

**The price is memory:**

```
KV bytes = 2 (K and V) × layers × tokens × d_model × bytes per value
         = 2 × 32 × 4096 × 4096 × 2 (fp16) = 2.0 GiB per sequence      (7B-class model, 4k context)
```

That's why serving engines care so much about the cache:

- **GQA/MQA** (fewer K/V heads);
- **PagedAttention** (vLLM: cache in pages, like virtual memory);
- KV quantisation;
- prefix caching (share the cache for a common system prompt).

The two phases behave differently:

- **prefill** (processing the prompt) is compute-bound;
- **decode** (one token at a time) is memory-bandwidth-bound.

## 8. Scaling arithmetic (back-of-the-envelope interview maths)

| Quantity | Rule of thumb | Example |
|---|---|---|
| parameters per block | **12 d²** (4d² attention + 8d² MLP), plus small bias/LayerNorm terms | d = 64: `12·64² + 13·64 = 49,984` (exact count, checked) |
| model size | `12 · L · d² + V · d` | L = 32, d = 4096, V = 32k → **6.57 B** (Llama-7B's shape; its gated MLP is a bit wider) |
| training compute | **≈ 6 × params × tokens** FLOPs | 7B params on 1T tokens ≈ 4.2e22 FLOPs |
| compute-optimal data (Chinchilla) | **≈ 20 tokens per parameter** | 7B → 140B tokens (modern models train far longer, because inference is cheaper for smaller models) |
| weight memory | params × bytes | 7B: fp16 **13.0 GiB**, int4 **3.3 GiB** |
| attention cost | ∝ T² | 2× the context → 4× the attention work |

Training memory is far more than the weights: with AdamW in mixed precision, roughly 16 bytes per
parameter (weights, gradients, two optimizer moments, fp32 master copy) **plus activations**.
That's why chapter 12's LoRA/QLoRA matter.

---

## Exercises

1. Why must you mask future tokens during **training** of a decoder, even though you feed the
   whole sentence at once?
2. A model has d = 2048, 24 layers and V = 50k. Estimate its parameter count.
3. With temperature → 0, what does sampling become? With temperature → ∞?
4. KV cache for a 70B model (80 layers, d = 8192, GQA with 8 of 64 heads for K/V), 8k tokens,
   fp16?

<details><summary>Answers</summary>

1. Each position is trained to predict the **next** token. Without the mask, position i could
   simply look at token i+1, and the model would learn to copy instead of predict, then fail at
   generation time when the future isn't available.
2. `12·24·2048² + 50,000·2048 ≈ 1.21B + 0.10B ≈ 1.3B`.
3. T → 0 becomes **greedy** (all the mass on the argmax). T → ∞ becomes **uniform** over the
   vocabulary.
4. Without GQA: `2 × 80 × 8192 × 8192 × 2 bytes = 20 GiB`. GQA stores 8 of 64 heads, i.e. ⅛, so
   **≈ 2.5 GiB** per sequence. That reduction is why GQA exists.
</details>

## Interview questions

**Q: Explain self-attention.** Each token is projected to a query, key and value. Scores are
query–key dot products scaled by √d; softmax turns them into weights; the output is the weighted
sum of values. Multi-head runs several of these in parallel subspaces. It's O(T²) in sequence
length. A causal mask makes it autoregressive.

**Q: Why are transformers better than RNNs for language?** Training is parallel across positions
(no sequential hidden state), and there's a direct path between any two tokens (no vanishing
gradient through time), so they scale to huge data and models. The costs are O(T²) attention and
the need for positional encodings.

**Q: What is the KV cache and what does it cost?** It stores past keys and values so each new
token only computes attention against the cache, which makes decoding roughly linear per token.
Memory grows as `2·L·T·d·bytes` per sequence and limits batch size and context length. GQA,
paging and quantisation reduce it.

**Q: Top-k vs top-p?** Top-k keeps a fixed number of candidates regardless of confidence. Top-p
keeps however many are needed to reach probability mass p: few when the model is confident, many
when it isn't. Top-p usually behaves better.

**Q: How do you estimate training compute or the memory needed?** FLOPs ≈ 6·N·D (N params,
D tokens). Inference memory = weights (N × bytes) + KV cache. Training memory ≈ 16 bytes/param
for AdamW mixed precision + activations, reduced by sharding, checkpointing or PEFT.

---

**Next:** [12 · Fine-tuning, LoRA and alignment](../12-Fine-Tuning-LoRA-HF/README.md)
