# 18 · Representation learning and LLM inference algorithms

> Two groups of algorithms that power modern AI. **Representation learning** turns raw data into
> useful vectors without labels: word2vec, contrastive learning, CLIP. **Inference algorithms**
> make big models usable: beam search, speculative decoding, mixture-of-experts routing. Plus the
> two architectural tricks behind deep networks: BatchNorm's backward pass and residual connections.

Run it: `../.venv/bin/python representation_inference.py` → **19/19 checks** (~7 s).

---

## 1. word2vec: skip-gram with negative sampling

**Distributional hypothesis:** *"you shall know a word by the company it keeps."* Words that appear
in similar contexts should get similar vectors.

**Skip-gram:** for each word, predict the words around it (window ±2). Each word has an **input**
vector `v` and an **output** vector `u`. A full softmax over the vocabulary is expensive, so
**negative sampling** turns it into binary classification:

```
loss = −log σ(v_center · u_context)                    ← a real (center, context) pair: pull together
       − Σ_{k=1..5} log σ(−v_center · u_noise_k)        ← 5 random "noise" words: push apart
noise distribution ∝ count^0.75                         ← word2vec's trick: rare words sampled a bit more often
```

The corpus is 3,000 synthetic sentences from 3 topics (animals, fruit, tech), with **no topic
labels** anywhere:

| Word | 3 nearest neighbours (cosine) |
|---|---|
| dog | horse, cow, cat |
| apple | mango, grape, banana |
| laptop | router, phone, server |

`cos(dog, cat) = 0.96`, `cos(dog, apple) = 0.26`, and **100 %** of the 24 topic words have all 3
nearest neighbours from their own topic.

**Why it matters:**

- word2vec (2013) is the ancestor of every embedding: item2vec and node2vec (recommenders, graphs),
  and the token embeddings in transformers.
- Mathematically, SGNS implicitly factorises a shifted PMI (pointwise mutual information) matrix.
  It's matrix factorisation again (chapter 13).
- Limitation: one vector per word ("bank" the river = "bank" the money). Contextual embeddings from
  transformers fix that.

## 2. Contrastive learning (InfoNCE / SimCLR)

**Idea:** create two **augmented views** of the same image (shifted, contrast-changed, noisy).
Train an encoder so that the two views of one image are close and views of *different* images are
far apart. No labels needed.

```
z = normalise(head(backbone(augment(x))))     for both views of a batch of N images → 2N vectors
InfoNCE = cross-entropy where, for each view, the "right class" is its partner among the other 2N − 1
logits = zᵢ · zⱼ / temperature
```

- At random init the loss is **ln(2N − 1)** (= 6.24 for N = 256; checked): "guess your partner
  uniformly".
- Training on unlabelled digits: loss 6.190 → 3.273.
- **Instance retrieval** on held-out images (what the objective trains): a view finds its partner
  among 397 candidates **40.8 %** of the time, vs 5.8 % with a random encoder.
- The **projection head** is thrown away afterwards. Representations *before* it transfer better
  (SimCLR's finding), so the probe uses the backbone.

**Honest result:** with only 100 labels, a linear probe on the learned features scored **0.748**,
vs **0.783** for raw pixels and 0.763 for a random encoder.

I tried stronger and lighter augmentations, temperatures 0.1–0.5, 3,000 steps with batch 512, kNN
probes, and shifted test images. In none of them did contrastive features clearly beat raw pixels
on these 8×8 digits. The reasons:

- 8×8 grey digits are already almost a good metric space in raw pixel form;
- a small MLP encoder can't learn spatial invariances from 1-pixel shifts;
- 1,400 images is tiny.

Self-supervised learning pays off on **large, natural images** (where pixel distance is
meaningless), with CNN/ViT encoders, strong augmentations (random crops, colour jitter, blur) and
lots of data. That's how SimCLR, MoCo, BYOL and DINO pretrain vision backbones. Chapter 12's
denoising autoencoder was a different self-supervised objective that did help on this data.

> Lesson: an SSL method "working" depends on the data, architecture and augmentations. Always
> compare against **raw features** and a **random encoder**. Both baselines are stronger than
> people expect.

## 3. CLIP: two modalities in one space

The same contrastive idea, across modalities. A batch of N (image, caption) pairs: the image
encoder gives `a`, the text encoder gives `b`, and the N × N similarity matrix should be
**diagonal**:

```
logits = normalise(a) · normalise(b)ᵀ · exp(log_t)          ← learnable temperature (init 1/0.07)
loss = ½ [ CE(logits, diag) + CE(logitsᵀ, diag) ]           ← symmetric: image→text and text→image
```

On a synthetic paired dataset (image and caption features generated from a shared hidden
"meaning"), the loss goes **7.673 → 0.005**, and image→caption retrieval among 64 candidates goes
from **1.6 % → 100 %**.

Real CLIP learned from 400M internet pairs. That gives you:

- **zero-shot classification** (embed "a photo of a {class}" for every class and pick the nearest);
- text-to-image search;
- the text encoder that conditions diffusion models (chapter 10 §8).

## 4. Beam search

Greedy decoding picks the best next token each step, but **a locally best token can lead
somewhere worse**. **Beam search** keeps the `b` best partial sequences at every step.

A toy bigram LM (`P(next | previous)` over `a, b, c, </s>`) is decoded for 6 tokens. The ground
truth is brute force over all 4⁶ = 4,096 sequences.

| Method | log P(best length-6 sequence) |
|---|---|
| greedy | −4.104 |
| beam 2 | −4.104 |
| beam 5 | **−2.333** |
| beam 64 | −2.333 |
| exact (brute force) | −2.333 |

**Beam search never does worse than greedy** (checked), and a wide enough beam finds the exact
optimum. Cost: `O(T · b · V)` instead of `Vᵀ`.

**Caveats:**

- Maximum-probability text is often **bland and repetitive** for open-ended generation. Chat
  models sample (temperature / top-p, chapter 11 §6).
- Beams favour short sequences, because every token multiplies in a probability < 1. Use
  **length normalisation**.
- Beam search remains standard for translation, speech recognition, captioning and constrained
  generation.

## 5. Speculative decoding

A big model generates **one token per forward pass**, and that pass is memory-bound (chapter 11
§7). Idea: let a **small draft model** propose k tokens cheaply, then verify them all in **one**
big-model pass.

**The accept/reject rule keeps the output distribution exactly equal to the big model's:**

```
draft proposes x ~ q
accept with probability min(1, p(x) / q(x))
if rejected: sample from the residual  max(p − q, 0) / Σ max(p − q, 0)
```

Checked over 200,000 samples:

- **target** `[0.406, 0.046, 0.347, 0.015, 0.101, 0.085]`;
- **output** `[0.408, 0.046, 0.346, 0.015, 0.101, 0.084]` (max error < 0.005);
- even though the draft was quite different (`[0.284, 0.126, 0.253, …]`).

The acceptance rate is **78 %** = `Σ min(p, q)` = 1 − total-variation distance (checked).

With k drafted tokens and acceptance rate a, each big-model call yields on average
`(1 − a^(k+1)) / (1 − a)` tokens: **3.26 tokens per call** instead of 1 here. That's a 2–3×
real-world speed-up with **no quality loss**, which is why vLLM, TGI and API providers use it.
Variants: Medusa (extra decoding heads), EAGLE, lookahead decoding, prompt lookup (copy n-grams
from the context).

## 6. Mixture of experts (MoE)

Replace one big feed-forward block with **E experts** and a **router** that sends each token to only
the top-k of them:

```
probs = softmax(router(x));  pick the top-k experts;  output = Σ (renormalised prob) · expert(x)
```

You get **many parameters** but **little compute per token**. In the demo: 272 parameters in
total, 77 used per token. Mixtral, DeepSeek-V3 and (reportedly) GPT-4 are MoE models.

**Problem: routing collapse.** Without help, the router sends most tokens to a few experts, and the
others never learn. The **Switch-Transformer load-balancing loss**:

```
aux = E · Σₑ (fraction of tokens routed to e) · (mean router probability for e)     = 1.0 when perfectly balanced
```

The data has 4 regions with 4 different functions (top-1 routing, 4 experts):

| | MSE | Expert usage | Balance loss | Regime purity |
|---|---|---|---|---|
| no balancing | **0.0535** | `[0.10, 0.55, 0.00, 0.35]`: **expert 3 starved** | 1.20 | 0.80 |
| with balancing (0.1 × aux) | 0.0690 | `[0.25, 0.25, 0.25, 0.25]` | 1.00 | 0.51 |

An **honest trade-off** showed up. Balancing achieved perfect load spreading, but on this tiny
problem the experts specialised *less* by region (purity 0.51) and the MSE was slightly worse. In
real MoE training, balance is essential for hardware efficiency (each expert sits on a different
GPU, so imbalance means idle GPUs and dropped tokens), and the auxiliary weight is kept small
(~0.01). Newer designs use auxiliary-loss-free balancing (bias terms on the router, as in
DeepSeek-V3).

## 7. BatchNorm backward, and residual connections

### BatchNorm backward (from scratch)

The forward pass was in chapter 08 §9. The backward pass is a classic interview derivation, because
every example's output depends on the whole batch's mean and variance:

```
x̂ = (x − μ) / √(σ² + ε),  y = γ x̂ + β
∂L/∂γ = Σ dy · x̂            ∂L/∂β = Σ dy
∂L/∂x = (N·dx̂ − Σ dx̂ − x̂ · Σ(dx̂ · x̂)) / (N · √(σ² + ε)),     dx̂ = dy · γ
```

- It matches the numerical gradient (checked).
- `∂L/∂x` sums to **0** over the batch: shifting every example by the same amount doesn't change
  BatchNorm's output, so that direction has no gradient.

### Residual connections

The same 50-layer tanh network, with and without `h = h + f(h)`. Gradient norm at the **first**
layer:

| | Gradient norm |
|---|---|
| plain | **4.8e-14**: completely vanished |
| residual | **1.03** |

The identity path makes `∂h_L/∂h_0 = I + (small terms)` instead of a product of 50 shrinking
Jacobians, so the gradient has a highway. That's the ResNet (2015) insight, and why every
transformer block is `x + Attention(LN(x))` and `x + MLP(LN(x))` (chapter 11).

---

## Exercises

1. Why does word2vec use negative sampling instead of a softmax over the vocabulary?
2. In InfoNCE, what happens with a very small temperature? A very large one?
3. Prove (in words) that speculative decoding never changes the output distribution.
4. Why does a top-2 MoE with 8 experts of size S cost about the same per token as a dense model of
   size 2S, but have the capacity of 8S?

<details><summary>Answers</summary>

1. The softmax normaliser sums over the whole vocabulary (100k+ words) for every training pair.
   Negative sampling replaces it with k + 1 binary logistic losses: one positive and k sampled
   negatives, `O(k)` instead of `O(V)`.
2. Small T sharpens the softmax: the loss focuses on the hardest negatives, which risks instability
   and over-separating near-duplicates. Large T flattens it: all negatives count equally and the
   gradients are weak. Typical values are 0.05–0.5, or learned (CLIP).
3. A token x is output either by acceptance, with probability `q(x)·min(1, p(x)/q(x)) = min(p, q)`,
   or by rejection followed by a residual sample, with probability
   `(1 − Σ min(p, q)) · max(p − q, 0)/Σ max(p − q, 0)`. Since `Σ max(p − q, 0) = 1 − Σ min(p, q)`,
   the second term is `max(p − q, 0)`. The total is `min(p, q) + max(p − q, 0) = p(x)`.
4. Each token passes through only 2 experts (≈ 2S FLOPs plus a tiny router), but the model stores
   8S parameters, and different tokens use different experts. Memory and communication grow with
   8S, which is MoE's real cost.
</details>

## Interview questions

**Q: How do embeddings like word2vec learn meaning without labels?** By predicting context: words
that co-occur with similar neighbours receive similar gradient updates, so their vectors converge.
The objective implicitly factorises a word–context co-occurrence (PMI) matrix.

**Q: What is contrastive learning, and why does it need negatives?** It learns representations by
pulling together augmented views of the same example and pushing apart different examples. Without
negatives (or tricks like BYOL's predictor and stop-gradient, or DINO's centring) the encoder can
collapse to a constant vector that trivially makes all pairs similar.

**Q: How does CLIP do zero-shot classification?** Embed the image; embed prompts like "a photo of
a {class}" for each class; predict the class whose text embedding has the highest cosine similarity.
No task-specific training.

**Q: How does speculative decoding speed up inference without changing outputs?** A cheap draft
proposes several tokens; the target model scores them all in one parallel pass; tokens are accepted
with probability `min(1, p/q)`, and on rejection resampled from the normalised residual. The output
distribution is exactly the target's. The speed-up depends on the acceptance rate and the
draft-to-target cost ratio.

**Q: What problems do MoE models have?** Load imbalance and routing collapse (fixed with an
auxiliary balance loss or bias-based balancing, plus capacity factors), training instability,
token dropping when experts overflow, large total memory, and cross-device communication
(all-to-all) in expert parallelism.

---

**Next:** [19 · Explainability, experiments and causal inference](../19-Explainability-Experiments-Causal/README.md)
