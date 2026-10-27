"""
ML 11 : TRANSFORMERS AND LLMs
Run:  ../.venv/bin/python transformers_llms.py        (explanations: README.md)

  1. Attention from scratch     scaled dot-product attention in NumPy, why divide by sqrt(d)
  2. Masks and multi-head       causal mask, padding mask, heads == torch.nn.MultiheadAttention
  3. Positional encoding        attention alone ignores order; sinusoidal encodings
  4. Tokenisation               byte-pair encoding (BPE) from scratch
  5. A tiny GPT                 decoder-only transformer trained on text (PyTorch)
  6. Decoding                   greedy, temperature, top-k, top-p
  7. KV cache                   incremental decoding == full recompute, and why it's faster
  8. Scaling arithmetic         parameter count, FLOPs, memory, perplexity
"""
import math
import sys
import time
from collections import Counter
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402

torch.set_num_threads(1)


def softmax(z, axis=-1):
    z = z - z.max(axis=axis, keepdims=True)
    e = np.exp(z)
    return e / e.sum(axis=axis, keepdims=True)


# ---------------------------------------------------------------------------
# 1. attention
# ---------------------------------------------------------------------------
def attention(Q, K, V, mask=None):
    """softmax(Q K^T / sqrt(d)) V   -- every query looks at every key and takes a weighted average of values."""
    d = Q.shape[-1]
    scores = Q @ K.swapaxes(-1, -2) / np.sqrt(d)
    if mask is not None:
        scores = np.where(mask, scores, -1e9)          # masked positions get ~zero weight
    w = softmax(scores)
    return w @ V, w


def attention_basics():
    section("1. Scaled dot-product attention")
    # 3 tokens; the query of token 0 matches the key of token 2
    K = np.array([[1.0, 0.0], [0.0, 1.0], [5.0, 5.0]])
    V = np.array([[10.0, 0.0], [0.0, 10.0], [7.0, 7.0]])
    Q = np.array([[1.0, 1.0]])
    out, w = attention(Q, K, V)
    show(f"attention weights of the query over 3 keys: {np.round(w[0], 3).tolist()}")
    check("the query attends mostly to the matching key", 2, int(np.argmax(w[0])))
    check("weights are a probability distribution", 1.0, round(float(w.sum()), 12))
    check("output = weighted average of values (close to value 2)", True, np.allclose(out[0], [7, 7], atol=0.1))
    rng = np.random.default_rng(0)
    for d in (16, 256):
        q, k = rng.normal(size=(20000, d)), rng.normal(size=(20000, d))
        dots = (q * k).sum(1)
        near(f"var(q.k) for random unit-variance vectors, d={d}, equals d", d, dots.var(), 0.05 * d)
    q, k = rng.normal(size=(1, 8, 256)), rng.normal(size=(1, 8, 256))
    raw = softmax(q @ k.swapaxes(1, 2))
    scaled = softmax(q @ k.swapaxes(1, 2) / 16)
    show(f"max attention weight with d=256: unscaled {raw.max(-1).mean():.3f} (nearly one-hot), scaled {scaled.max(-1).mean():.3f}")
    check("dividing by sqrt(d) stops softmax saturating (tiny gradients)", True, raw.max(-1).mean() > scaled.max(-1).mean() + 0.2)


# ---------------------------------------------------------------------------
# 2. masks and multi-head attention
# ---------------------------------------------------------------------------
def masks_and_heads():
    section("2. Causal mask, padding mask, multi-head attention")
    n = 4
    causal = np.tril(np.ones((n, n), dtype=bool))
    rng = np.random.default_rng(1)
    X = rng.normal(size=(n, 8))
    _, w = attention(X, X, X, causal)
    check("causal mask: token i only sees tokens <= i (upper triangle = 0)", True, np.allclose(np.triu(w, 1), 0, atol=1e-12))
    check("the first token can only attend to itself", 1.0, round(float(w[0, 0]), 12))
    X2 = X.copy()
    X2[3] = 100.0                                       # change the LAST token
    out1, _ = attention(X, X, X, causal)
    out2, _ = attention(X2, X2, X2, causal)
    check("changing a future token doesn't change earlier outputs", True, np.allclose(out1[:3], out2[:3]))
    pad = np.array([True, True, True, False])          # last position is padding
    _, wp = attention(X, X, X, pad[None, :])
    check("padding mask: nobody attends to the pad token", True, np.allclose(wp[:, 3], 0, atol=1e-12))

    # multi-head = split d_model into h heads, attend separately, concatenate, project
    torch.manual_seed(0)
    d_model, h, T = 16, 4, 5
    mha = nn.MultiheadAttention(d_model, h, batch_first=True, bias=False)
    x = torch.randn(1, T, d_model)
    ref, _ = mha(x, x, x, need_weights=False)
    Wq, Wk, Wv = mha.in_proj_weight.detach().chunk(3)
    Wo = mha.out_proj.weight.detach()
    xs = x[0].numpy()
    q, k, v = xs @ Wq.numpy().T, xs @ Wk.numpy().T, xs @ Wv.numpy().T
    dh = d_model // h
    heads = [attention(q[:, i * dh:(i + 1) * dh], k[:, i * dh:(i + 1) * dh], v[:, i * dh:(i + 1) * dh])[0] for i in range(h)]
    ours = np.concatenate(heads, axis=1) @ Wo.numpy().T
    check("multi-head attention from scratch == torch.nn.MultiheadAttention", True, np.allclose(ours, ref[0].detach().numpy(), atol=1e-5))
    check("attention parameters: 4 projections of d x d", 4 * d_model * d_model, sum(p.numel() for p in mha.parameters()))


# ---------------------------------------------------------------------------
# 3. positional encoding
# ---------------------------------------------------------------------------
def sinusoidal(T, d):
    pos = np.arange(T)[:, None]
    i = np.arange(d // 2)[None, :]
    angle = pos / (10000 ** (2 * i / d))
    pe = np.zeros((T, d))
    pe[:, 0::2], pe[:, 1::2] = np.sin(angle), np.cos(angle)
    return pe


def positional():
    section("3. Positional encoding")
    rng = np.random.default_rng(2)
    X = rng.normal(size=(5, 8))
    perm = [4, 2, 0, 3, 1]
    out, _ = attention(X, X, X)
    out_p, _ = attention(X[perm], X[perm], X[perm])
    check("without positions, attention is permutation-equivariant (order is invisible)", True, np.allclose(out[perm], out_p))
    pe = sinusoidal(50, 16)
    check("each position gets a unique encoding", 50, len({tuple(np.round(r, 6)) for r in pe}))
    check("values stay bounded in [-1, 1]", True, np.abs(pe).max() <= 1.0)
    sim = pe @ pe[10]
    check("nearby positions are more similar than distant ones", True, sim[11] > sim[30])
    out_pe, _ = attention(X + pe[:5, :8], X + pe[:5, :8], X + pe[:5, :8])
    out_pe_p, _ = attention(X[perm] + pe[:5, :8], X[perm] + pe[:5, :8], X[perm] + pe[:5, :8])
    check("with positions added, reordering the tokens changes the result", False, np.allclose(out_pe[perm], out_pe_p))


# ---------------------------------------------------------------------------
# 4. BPE tokenisation
# ---------------------------------------------------------------------------
def bpe_train(words, n_merges):
    """Start from characters; repeatedly merge the most frequent adjacent pair into a new token."""
    vocab = Counter({tuple(w) + ("</w>",): c for w, c in words.items()})
    merges = []
    for _ in range(n_merges):
        pairs = Counter()
        for word, c in vocab.items():
            for a, b in zip(word, word[1:]):
                pairs[(a, b)] += c
        if not pairs:
            break
        best = max(pairs, key=lambda p: (pairs[p], p))
        merges.append(best)
        new_vocab = Counter()
        for word, c in vocab.items():
            out, i = [], 0
            while i < len(word):
                if i < len(word) - 1 and (word[i], word[i + 1]) == best:
                    out.append(word[i] + word[i + 1])
                    i += 2
                else:
                    out.append(word[i])
                    i += 1
            new_vocab[tuple(out)] += c
        vocab = new_vocab
    return merges


def bpe_encode(word, merges):
    tokens = list(word) + ["</w>"]
    for a, b in merges:
        i, out = 0, []
        while i < len(tokens):
            if i < len(tokens) - 1 and tokens[i] == a and tokens[i + 1] == b:
                out.append(a + b)
                i += 2
            else:
                out.append(tokens[i])
                i += 1
        tokens = out
    return tokens


def tokenisation():
    section("4. Byte-pair encoding (BPE) from scratch")
    words = {"low": 5, "lower": 2, "newest": 6, "widest": 3}
    merges = bpe_train(words, 10)
    pair_counts = Counter()
    for w, c in words.items():
        for a, b in zip(tuple(w) + ("</w>",), tuple(w)[1:] + ("</w>",)):
            pair_counts[(a, b)] += c
    show(f"top pair counts: {pair_counts.most_common(4)}")
    show(f"first merges: {merges[:4]}")
    check("the first merge is a most-frequent pair (9 occurrences; ties broken consistently)", 9, pair_counts[merges[0]])
    check("'newest' becomes a single token after 10 merges", ["newest</w>"], bpe_encode("newest", merges))
    check("a frequent word is short", ["low</w>"], bpe_encode("low", merges))
    check("an unseen word still encodes (falls back to pieces)", ["low", "est</w>"], bpe_encode("lowest", merges))
    check("a completely new word falls back to characters", ["z", "o", "o", "</w>"], bpe_encode("zoo", merges))


# ---------------------------------------------------------------------------
# 5. a tiny GPT
# ---------------------------------------------------------------------------
class Block(nn.Module):
    def __init__(self, d, h):
        super().__init__()
        self.ln1, self.ln2 = nn.LayerNorm(d), nn.LayerNorm(d)
        self.qkv, self.proj = nn.Linear(d, 3 * d), nn.Linear(d, d)
        self.mlp = nn.Sequential(nn.Linear(d, 4 * d), nn.GELU(), nn.Linear(4 * d, d))
        self.h = h

    def attn(self, x, cache=None):
        B, T, D = x.shape
        q, k, v = self.qkv(x).split(D, dim=2)
        q, k, v = (t.view(B, T, self.h, D // self.h).transpose(1, 2) for t in (q, k, v))  # (B, h, T, dh)
        if cache is not None:                            # KV cache: append this step's keys/values
            if "k" in cache:
                k, v = torch.cat([cache["k"], k], 2), torch.cat([cache["v"], v], 2)
            cache["k"], cache["v"] = k, v
            y = F.scaled_dot_product_attention(q, k, v)   # the new query may see every cached position
        else:
            y = F.scaled_dot_product_attention(q, k, v, is_causal=True)
        return self.proj(y.transpose(1, 2).reshape(B, T, D))

    def forward(self, x, cache=None):
        x = x + self.attn(self.ln1(x), cache)            # pre-norm + residual
        return x + self.mlp(self.ln2(x))


class TinyGPT(nn.Module):
    def __init__(self, vocab, d=64, h=4, layers=2, ctx=64):
        super().__init__()
        self.tok, self.pos = nn.Embedding(vocab, d), nn.Embedding(ctx, d)
        self.blocks = nn.ModuleList([Block(d, h) for _ in range(layers)])
        self.ln, self.head = nn.LayerNorm(d), nn.Linear(d, vocab, bias=False)
        self.head.weight = self.tok.weight               # weight tying: output layer reuses the embeddings
        self.ctx = ctx
        self.apply(self._init_weights)

    @staticmethod
    def _init_weights(m):
        # GPT-2 style: N(0, 0.02). PyTorch's default Embedding init is N(0, 1), which (tied to the output layer)
        # makes the initial logits huge: the first loss was ~46 instead of ln(vocab) ~ 2.7.
        if isinstance(m, (nn.Linear, nn.Embedding)):
            nn.init.normal_(m.weight, std=0.02)
        if isinstance(m, nn.Linear) and m.bias is not None:
            nn.init.zeros_(m.bias)

    def forward(self, idx, caches=None, start=0):
        T = idx.shape[1]
        x = self.tok(idx) + self.pos(torch.arange(start, start + T))
        for i, b in enumerate(self.blocks):
            x = b(x, None if caches is None else caches[i])
        return self.head(self.ln(x))


TEXT = ("the cat sat on the mat. the dog sat on the log. the cat saw the dog. "
        "the dog saw the cat. a cat and a dog sat on a mat and a log. ") * 40


def train_gpt():
    section("5. A tiny GPT (decoder-only transformer)")
    chars = sorted(set(TEXT))
    stoi = {c: i for i, c in enumerate(chars)}
    data = torch.tensor([stoi[c] for c in TEXT])
    torch.manual_seed(0)
    model = TinyGPT(len(chars))
    n_params = sum(p.numel() for p in model.parameters())
    show(f"vocabulary {len(chars)} characters, {n_params:,} parameters")
    opt = torch.optim.AdamW(model.parameters(), lr=3e-3)
    ctx, B = 48, 32
    losses = []
    g = torch.Generator().manual_seed(0)
    for step in range(600):
        ix = torch.randint(0, len(data) - ctx - 1, (B,), generator=g)
        x = torch.stack([data[i:i + ctx] for i in ix])
        y = torch.stack([data[i + 1:i + ctx + 1] for i in ix])          # targets = inputs shifted by one
        loss = F.cross_entropy(model(x).view(-1, len(chars)), y.view(-1))
        opt.zero_grad()
        loss.backward()
        opt.step()
        losses.append(loss.item())
    near("initial loss ~ ln(vocab) (uniform guessing)", math.log(len(chars)), losses[0], 0.6)
    show(f"loss {losses[0]:.3f} -> {np.mean(losses[-50:]):.3f}; perplexity {math.exp(losses[0]):.1f} -> {math.exp(np.mean(losses[-50:])):.2f}")
    check("the model learns the text (perplexity < 2)", True, math.exp(np.mean(losses[-50:])) < 2.0)
    model.eval()
    prompt = "the dog sat on the "
    idx = torch.tensor([[stoi[c] for c in prompt]])
    with torch.no_grad():
        for _ in range(12):
            nxt = model(idx[:, -ctx:])[:, -1].argmax(-1, keepdim=True)
            idx = torch.cat([idx, nxt], 1)
    completion = "".join(chars[i] for i in idx[0].tolist())[len(prompt):]
    show(f"greedy completion of '{prompt}' -> '{completion}'")
    check("greedy decoding continues with a phrase from the training text", True, (prompt + completion)[:len(prompt) + 4] in TEXT)
    return model, stoi, chars


# ---------------------------------------------------------------------------
# 6. decoding strategies
# ---------------------------------------------------------------------------
def decoding():
    section("6. Decoding: greedy, temperature, top-k, top-p")
    logits = np.array([3.0, 2.0, 1.0, 0.5, -1.0])
    p = softmax(logits)
    check("greedy picks the argmax", 0, int(np.argmax(p)))
    hot, cold = softmax(logits / 2.0), softmax(logits / 0.5)
    show(f"P(top token): T=0.5 {cold[0]:.3f}, T=1 {p[0]:.3f}, T=2 {hot[0]:.3f}")
    check("low temperature sharpens, high temperature flattens", True, cold[0] > p[0] > hot[0])
    entropy = lambda q: float(-(q * np.log(q)).sum())  # noqa: E731
    check("higher temperature = higher entropy (more random)", True, entropy(hot) > entropy(p) > entropy(cold))
    topk = np.argsort(-p)[:2]
    pk = np.zeros_like(p)
    pk[topk] = p[topk] / p[topk].sum()
    check("top-k=2 keeps only the 2 best tokens, renormalised", [0, 1], sorted(np.flatnonzero(pk).tolist()))
    order = np.argsort(-p)
    cum = np.cumsum(p[order])
    keep = order[: int(np.searchsorted(cum, 0.9) + 1)]
    show(f"probabilities {np.round(p, 3).tolist()}; top-p 0.9 keeps tokens {sorted(keep.tolist())}")
    check("top-p (nucleus) keeps the smallest set with total probability >= 0.9", [0, 1, 2], sorted(keep.tolist()))
    flat = softmax(np.zeros(5))
    cum_flat = np.cumsum(np.sort(flat)[::-1])
    check("...and it adapts: on a flat distribution it keeps more tokens", 5, int(np.searchsorted(cum_flat, 0.9 - 1e-12) + 1))


# ---------------------------------------------------------------------------
# 7. KV cache
# ---------------------------------------------------------------------------
def kv_cache(model, stoi, chars):
    section("7. KV cache: reuse past keys/values during generation")
    prompt = torch.tensor([[stoi[c] for c in "the cat sat on "]])
    n_new = 20

    def generate_full(idx):
        with torch.no_grad():
            for _ in range(n_new):
                nxt = model(idx)[:, -1].argmax(-1, keepdim=True)     # recompute EVERYTHING each step
                idx = torch.cat([idx, nxt], 1)
        return idx

    def generate_cached(idx):
        caches = [dict() for _ in model.blocks]
        with torch.no_grad():
            logits = model(idx, caches, start=0)                      # process the prompt once
            pos = idx.shape[1]
            for _ in range(n_new):
                nxt = logits[:, -1].argmax(-1, keepdim=True)
                idx = torch.cat([idx, nxt], 1)
                logits = model(nxt, caches, start=pos)                # feed ONLY the new token
                pos += 1
        return idx

    t0 = time.perf_counter()
    full = generate_full(prompt)
    t_full = time.perf_counter() - t0
    t0 = time.perf_counter()
    cached = generate_cached(prompt)
    t_cached = time.perf_counter() - t0
    show(f"generated: '{''.join(chars[i] for i in cached[0].tolist())}'")
    check("cached generation produces exactly the same tokens", True, torch.equal(full, cached))
    show(f"time for {n_new} tokens: full recompute {t_full * 1000:.1f} ms, KV cache {t_cached * 1000:.1f} ms (tiny model; the gap grows with length)")
    L, d, n_layers, bytes_per = 4096, 4096, 32, 2
    kv_bytes = 2 * n_layers * L * d * bytes_per
    show(f"KV cache size for a 7B-class model (32 layers, d=4096, fp16) at 4,096 tokens: {kv_bytes / 2**30:.1f} GiB per sequence")
    check("KV memory = 2 (K and V) x layers x tokens x d x bytes", 2.0, kv_bytes / 2 ** 30)


# ---------------------------------------------------------------------------
# 8. scaling arithmetic
# ---------------------------------------------------------------------------
def scaling():
    section("8. Scaling arithmetic")
    d, layers = 64, 2
    per_block = sum(p.numel() for p in Block(d, 4).parameters())
    check("one block ~ 12 d^2 parameters (4d^2 attention + 8d^2 MLP) + biases/LayerNorms", 12 * d * d + 13 * d, per_block)
    d, layers, vocab = 4096, 32, 32000
    approx = 12 * layers * d * d + vocab * d
    show(f"a 32-layer, d=4096 model: 12*L*d^2 + V*d = {approx / 1e9:.2f}B parameters (the 'LLaMA-7B' shape, minus its wider MLP)")
    check("rule of thumb lands in the right ballpark (6-7B)", True, 6e9 < approx < 7e9)
    tokens = 1e12
    flops = 6 * 7e9 * tokens
    show(f"training compute ~ 6 x params x tokens = {flops:.1e} FLOPs for 7B params on 1T tokens")
    check("Chinchilla rule of thumb: ~20 tokens per parameter for compute-optimal training", 140e9, 20 * 7e9)
    mem_fp16 = 7e9 * 2 / 2 ** 30
    mem_int4 = 7e9 * 0.5 / 2 ** 30
    show(f"weights of a 7B model: fp16 {mem_fp16:.1f} GiB, int4 {mem_int4:.1f} GiB (quantisation, chapter 12)")
    check("attention cost grows with the square of the context length: 2x tokens", 4, (2048 ** 2) // (1024 ** 2))


if __name__ == "__main__":
    attention_basics()
    masks_and_heads()
    positional()
    tokenisation()
    m, stoi, chars = train_gpt()
    decoding()
    kv_cache(m, stoi, chars)
    scaling()
    summary()
