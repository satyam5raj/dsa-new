"""
ML 18 : REPRESENTATION LEARNING AND LLM INFERENCE ALGORITHMS
Run:  ../.venv/bin/python representation_inference.py        (explanations: README.md)

  1. word2vec (skip-gram + negative sampling)  words that share contexts get similar vectors
  2. Contrastive learning (InfoNCE / SimCLR)   learn features from unlabelled data with augmentations
  3. CLIP-style loss                           matching two modalities in one embedding space
  4. Beam search                               better sequences than greedy, checked by brute force
  5. Speculative decoding                      a small draft model + exact target-model distribution
  6. Mixture of experts                        top-k routing, load balancing, experts specialise
  7. BatchNorm backward + residual connections the two tricks that make deep nets trainable
"""
import math
import sys
from itertools import product
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from sklearn.datasets import load_digits
from sklearn.linear_model import LogisticRegression

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402

torch.set_num_threads(1)


# ---------------------------------------------------------------------------
# 1. word2vec
# ---------------------------------------------------------------------------
def make_corpus(n_sent=3000, seed=0):
    rng = np.random.default_rng(seed)
    topics = {
        "animal": (["dog", "cat", "horse", "cow"], ["runs", "eats", "sleeps", "barks"]),
        "fruit": (["apple", "mango", "banana", "grape"], ["ripe", "sweet", "fresh", "juicy"]),
        "tech": (["laptop", "phone", "server", "router"], ["boots", "crashes", "updates", "charges"]),
    }
    sents = []
    for _ in range(n_sent):
        nouns, words = topics[rng.choice(list(topics))]
        sents.append(["the", rng.choice(nouns), rng.choice(words), rng.choice(nouns), "today"])
    return sents, topics


def word2vec_demo():
    section("1. word2vec: skip-gram with negative sampling")
    sents, topics = make_corpus()
    vocab = sorted({w for s in sents for w in s})
    idx = {w: i for i, w in enumerate(vocab)}
    pairs = [(idx[s[i]], idx[s[j]]) for s in sents for i in range(len(s)) for j in range(max(0, i - 2), min(len(s), i + 3)) if i != j]
    counts = np.bincount([c for _, c in pairs], minlength=len(vocab)).astype(float)
    noise = torch.tensor(counts ** 0.75 / (counts ** 0.75).sum())   # unigram^0.75: the word2vec noise distribution
    torch.manual_seed(0)
    d, k = 16, 5
    W_in, W_out = nn.Embedding(len(vocab), d), nn.Embedding(len(vocab), d)
    nn.init.normal_(W_in.weight, std=0.1)
    nn.init.zeros_(W_out.weight)
    opt = torch.optim.Adam(list(W_in.parameters()) + list(W_out.parameters()), lr=0.01)
    P = torch.tensor(pairs)
    for epoch in range(3):
        for b in torch.randperm(len(P)).split(512):
            c, o = P[b, 0], P[b, 1]
            neg = torch.multinomial(noise, len(b) * k, replacement=True).view(len(b), k)
            v = W_in(c)
            pos_loss = -F.logsigmoid((v * W_out(o)).sum(-1))                       # pull real context close
            neg_loss = -F.logsigmoid(-(W_out(neg) @ v[:, :, None]).squeeze(-1)).sum(-1)   # push random words away
            opt.zero_grad()
            (pos_loss + neg_loss).mean().backward()
            opt.step()
    E = F.normalize(W_in.weight.detach(), dim=1)
    sim = lambda a, b: float(E[idx[a]] @ E[idx[b]])            # noqa: E731
    nearest = lambda w: [str(vocab[i]) for i in torch.argsort(E @ E[idx[w]], descending=True)[1:4].tolist()]  # noqa: E731
    show(f"nearest to 'dog': {nearest('dog')}; to 'apple': {nearest('apple')}; to 'laptop': {nearest('laptop')}")
    show(f"cosine(dog, cat) {sim('dog', 'cat'):.2f}, cosine(dog, apple) {sim('dog', 'apple'):.2f}")
    check("words sharing contexts get similar vectors: cos(dog, cat) > cos(dog, apple) + 0.3", True, sim("dog", "cat") > sim("dog", "apple") + 0.3)
    groups = {w: t for t, (n, v) in topics.items() for w in n + v}
    top3_ok = np.mean([all(groups.get(x) == groups[w] for x in nearest(w)) for w in groups])
    show(f"{top3_ok:.0%} of the 24 topic words have all 3 nearest neighbours from their own topic")
    check("the embedding space organises words by topic, with NO labels", True, top3_ok > 0.8)


# ---------------------------------------------------------------------------
# 2. contrastive learning (SimCLR-style)
# ---------------------------------------------------------------------------
def augment(x, g):
    """per-image random shift by up to 1 pixel, random contrast, pixel noise"""
    imgs = x.view(-1, 8, 8)
    shifts = torch.randint(-1, 2, (len(x), 2), generator=g)
    imgs = torch.stack([torch.roll(im, shifts=(int(a), int(b)), dims=(0, 1)) for im, (a, b) in zip(imgs, shifts)])
    imgs = imgs * (0.7 + 0.6 * torch.rand(len(x), 1, 1, generator=g)) + 0.1 * torch.randn(imgs.shape, generator=g)
    return imgs.reshape(-1, 64)


def info_nce(z1, z2, temperature=0.2):
    """each view's positive is the other view of the SAME image; the other 2N-2 views are negatives"""
    z = F.normalize(torch.cat([z1, z2]), dim=1)
    sim = z @ z.T / temperature
    sim.fill_diagonal_(-1e9)                                   # never match a view with itself
    n = len(z1)
    target = torch.cat([torch.arange(n, 2 * n), torch.arange(0, n)])
    return F.cross_entropy(sim, target)


def contrastive_demo():
    section("2. Contrastive learning (InfoNCE, SimCLR-style) on unlabelled digits")
    X, y = load_digits(return_X_y=True)
    X = torch.tensor(X / 16.0, dtype=torch.float32)
    near("InfoNCE at random init ~ ln(2N - 1) (pick 1 positive among 2N-1)", math.log(2 * 256 - 1),
         info_nce(torch.randn(256, 32), torch.randn(256, 32), 1e6).item(), 0.05)
    torch.manual_seed(0)
    backbone = nn.Sequential(nn.Linear(64, 128), nn.ReLU())                       # the representation we keep
    head = nn.Sequential(nn.Linear(128, 64), nn.ReLU(), nn.Linear(64, 32))        # projection head: thrown away later
    rand_backbone = nn.Sequential(nn.Linear(64, 128), nn.ReLU())
    opt = torch.optim.Adam(list(backbone.parameters()) + list(head.parameters()), lr=1e-3)
    g = torch.Generator().manual_seed(0)
    losses = []
    for step in range(1000):
        b = X[:1400][torch.randint(0, 1400, (256,), generator=g)]                 # NO labels used
        loss = info_nce(head(backbone(augment(b, g))), head(backbone(augment(b, g))))
        opt.zero_grad()
        loss.backward()
        opt.step()
        losses.append(loss.item())
    show(f"InfoNCE loss {losses[0]:.3f} -> {np.mean(losses[-50:]):.3f}")
    check("the contrastive loss falls a lot", True, np.mean(losses[-50:]) < 0.7 * losses[0])

    def instance_retrieval(bb, hd):                            # what InfoNCE trains: find your own other view
        gg = torch.Generator().manual_seed(1)
        with torch.no_grad():
            a = F.normalize(hd(bb(augment(X[1400:], gg))), dim=1)
            b = F.normalize(hd(bb(augment(X[1400:], gg))), dim=1)
        return (torch.argmax(a @ b.T, 1) == torch.arange(len(a))).float().mean().item()
    torch.manual_seed(5)
    rand_head = nn.Sequential(nn.Linear(128, 64), nn.ReLU(), nn.Linear(64, 32))
    r_trained, r_random = instance_retrieval(backbone, head), instance_retrieval(rand_backbone, rand_head)
    show(f"held-out images: a view finds its partner view among {len(X) - 1400}: trained {r_trained:.3f}, random encoder {r_random:.3f}")
    check("contrastive training makes two views of the same image find each other", True, r_trained > r_random + 0.1)

    few = np.concatenate([np.flatnonzero(y[:1400] == c)[:10] for c in range(10)])  # only 10 labels per class
    def probe(f):
        with torch.no_grad():
            Z = f(X).numpy()
        return LogisticRegression(max_iter=3000).fit(Z[few], y[few]).score(Z[1400:], y[1400:])
    a_raw, a_rand, a_con = probe(lambda v: v), probe(rand_backbone), probe(backbone)
    show(f"linear probe with 100 labels: raw pixels {a_raw:.3f}, random encoder {a_rand:.3f}, contrastive encoder {a_con:.3f}")
    show("honest result: on tiny, clean 8x8 digits raw pixels are already a good representation; SSL pays off on")
    show("large natural images with CNN/ViT encoders and strong augmentations (crops, colour jitter, blur)")


# ---------------------------------------------------------------------------
# 3. CLIP loss
# ---------------------------------------------------------------------------
def clip_demo():
    section("3. CLIP-style loss: two modalities, one space")
    torch.manual_seed(1)
    n, d = 64, 16
    concept = torch.randn(n, 8)                                # the shared meaning behind each image-caption pair
    img = concept @ torch.randn(8, 20) + 0.1 * torch.randn(n, 20)      # "image features"
    txt = concept @ torch.randn(8, 12) + 0.1 * torch.randn(n, 12)      # "caption features"
    fi, ft = nn.Linear(20, d), nn.Linear(12, d)
    log_t = nn.Parameter(torch.tensor(math.log(1 / 0.07)))     # learnable temperature, as in CLIP
    opt = torch.optim.Adam(list(fi.parameters()) + list(ft.parameters()) + [log_t], lr=1e-2)

    def clip_loss():
        a, b = F.normalize(fi(img), dim=1), F.normalize(ft(txt), dim=1)
        logits = a @ b.T * log_t.exp()
        labels = torch.arange(n)
        return (F.cross_entropy(logits, labels) + F.cross_entropy(logits.T, labels)) / 2, logits   # symmetric
    l0, logits0 = clip_loss()
    for _ in range(300):
        loss, logits = clip_loss()
        opt.zero_grad()
        loss.backward()
        opt.step()
    acc0 = (logits0.argmax(1) == torch.arange(n)).float().mean().item()
    acc = (logits.argmax(1) == torch.arange(n)).float().mean().item()
    show(f"symmetric CLIP loss {l0.item():.3f} -> {loss.item():.3f}; image->caption retrieval accuracy {acc0:.3f} -> {acc:.3f} ({n} candidates)")
    check("after training each image retrieves its own caption", True, acc > 0.95)


# ---------------------------------------------------------------------------
# 4. beam search
# ---------------------------------------------------------------------------
VOCAB = ["<s>", "a", "b", "c", "</s>"]
rng_lm = np.random.default_rng(3)
LM = rng_lm.dirichlet(np.ones(5) * 0.6, size=5)              # toy bigram LM: P(next | previous)
LM[:, 0] = 0
LM /= LM.sum(1, keepdims=True)
LM[4] = [0, 0, 0, 0, 1]                                        # </s> is absorbing


def seq_logp(seq):
    with np.errstate(divide="ignore"):                         # impossible transitions -> log 0 = -inf
        return sum(np.log(LM[a, b]) for a, b in zip([0] + seq[:-1], seq))


def greedy(T):
    seq, prev = [], 0
    for _ in range(T):
        prev = int(np.argmax(LM[prev]))
        seq.append(prev)
    return seq


def beam_search(T, width):
    beams = [([], 0.0)]
    for _ in range(T):
        cand = []
        for seq, lp in beams:
            prev = seq[-1] if seq else 0
            for tok in range(1, 5):
                if LM[prev, tok] > 0:
                    cand.append((seq + [tok], lp + np.log(LM[prev, tok])))
        beams = sorted(cand, key=lambda c: -c[1])[:width]      # keep only the best `width` partial sequences
    return beams[0]


def beam_demo():
    section("4. Beam search vs greedy (checked by brute force)")
    T = 6
    brute = max((list(s) for s in product(range(1, 5), repeat=T) if seq_logp(list(s)) > -np.inf), key=seq_logp)
    g = greedy(T)
    results = {"greedy": seq_logp(g)}
    for w in (2, 5, 64):
        results[f"beam {w}"] = beam_search(T, w)[1]
    results["exact (brute force over 4^6)"] = seq_logp(brute)
    show(f"bigram LM row for <s>: {np.round(LM[0], 3).tolist()}")
    show("log P(best length-6 sequence): " + ", ".join(f"{k} {v:.3f}" for k, v in results.items()))
    check("beam search never does worse than greedy", True, all(results[f"beam {w}"] >= results["greedy"] - 1e-12 for w in (2, 5, 64)))
    check("a wide enough beam finds the exact optimum", True, np.isclose(results["beam 64"], results["exact (brute force over 4^6)"]))
    check("greedy is NOT optimal here (a locally best token leads somewhere worse)", True, results["greedy"] < results["exact (brute force over 4^6)"] - 1e-9)


# ---------------------------------------------------------------------------
# 5. speculative decoding
# ---------------------------------------------------------------------------
def speculative_demo():
    section("5. Speculative decoding: draft fast, verify exactly")
    rng = np.random.default_rng(4)
    V = 6
    target = rng.dirichlet(np.ones(V))                         # big model's next-token distribution (one context)
    draft = 0.6 * target + 0.4 * rng.dirichlet(np.ones(V))     # small model: similar but not equal
    out, accepted = [], 0
    N = 200_000
    for _ in range(N):
        x = rng.choice(V, p=draft)                             # draft proposes
        if rng.random() < min(1.0, target[x] / draft[x]):      # accept with prob min(1, p/q)
            out.append(x)
            accepted += 1
        else:                                                  # on rejection, resample from the residual max(p - q, 0)
            resid = np.maximum(target - draft, 0)
            out.append(rng.choice(V, p=resid / resid.sum()))
    emp = np.bincount(out, minlength=V) / N
    show(f"target {np.round(target, 3).tolist()}")
    show(f"output {np.round(emp, 3).tolist()}  (draft {np.round(draft, 3).tolist()})")
    check("the accept/reject rule reproduces the TARGET distribution exactly (max error < 0.005)", True, np.abs(emp - target).max() < 0.005)
    near("acceptance rate = sum(min(p, q)) = 1 - total variation distance", np.minimum(target, draft).sum(), accepted / N, 0.005)
    a, k = np.minimum(target, draft).sum(), 4
    expected_tokens = (1 - a ** (k + 1)) / (1 - a)             # tokens produced per target-model call with k drafted tokens
    show(f"acceptance {a:.2f}: with 4 drafted tokens, each big-model call yields {expected_tokens:.2f} tokens on average instead of 1")
    check("speculative decoding emits > 2 tokens per target forward pass here", True, expected_tokens > 2)


# ---------------------------------------------------------------------------
# 6. mixture of experts
# ---------------------------------------------------------------------------
class MoE(nn.Module):
    def __init__(self, d_in, n_experts=4, k=1, hidden=16):
        super().__init__()
        self.router = nn.Linear(d_in, n_experts)
        self.experts = nn.ModuleList([nn.Sequential(nn.Linear(d_in, hidden), nn.ReLU(), nn.Linear(hidden, 1)) for _ in range(n_experts)])
        self.k, self.n = k, n_experts

    def forward(self, x):
        probs = F.softmax(self.router(x), dim=1)
        top_p, top_i = probs.topk(self.k, dim=1)               # each token goes to only k experts: sparse compute
        top_p = top_p / top_p.sum(1, keepdim=True)
        out = torch.zeros(len(x), 1)
        for j in range(self.k):
            for e in range(self.n):
                m = top_i[:, j] == e
                if m.any():
                    out[m] += top_p[m, j:j + 1] * self.experts[e](x[m])
        frac = torch.bincount(top_i[:, 0], minlength=self.n).float() / len(x)   # tokens routed to each expert
        aux = self.n * (frac * probs.mean(0)).sum()            # Switch-Transformer load-balancing loss (1.0 = balanced)
        return out, top_i[:, 0], aux


def moe_demo():
    section("6. Mixture of experts: top-1 routing + load balancing")
    torch.manual_seed(0)
    x = torch.rand(4000, 2) * 4 - 2
    regime = (x[:, 0] > 0).long() * 2 + (x[:, 1] > 0).long()   # 4 regions, each a different function
    fns = [lambda v: torch.sin(3 * v[:, 0]), lambda v: v[:, 1] ** 2, lambda v: -v[:, 0] * v[:, 1], lambda v: torch.cos(2 * v[:, 1])]
    y = torch.stack([fns[r](x[i:i + 1])[0] for i, r in enumerate(regime.tolist())])[:, None]
    res = {}
    for name, lb in (("no balancing", 0.0), ("with balancing loss", 0.1)):
        torch.manual_seed(0)
        moe = MoE(2)
        opt = torch.optim.Adam(moe.parameters(), lr=1e-2)
        for _ in range(800):
            out, _, aux = moe(x)
            loss = F.mse_loss(out, y) + lb * aux
            opt.zero_grad()
            loss.backward()
            opt.step()
        with torch.no_grad():
            out, route, aux = moe(x)
        usage = torch.bincount(route, minlength=4).float() / len(x)
        purity = np.mean([torch.bincount(route[regime == r], minlength=4).max().item() / (regime == r).sum().item() for r in range(4)])
        res[name] = (F.mse_loss(out, y).item(), usage, aux.item(), purity)
        show(f"{name:<20} MSE {res[name][0]:.4f}; expert usage {[round(float(u), 2) for u in usage]}; balance loss {aux.item():.2f}; regime purity {purity:.2f}")
    check("the balancing loss spreads tokens more evenly across experts", True, bool(res["with balancing loss"][1].min() > res["no balancing"][1].min()))
    check("with balancing, every expert gets real work (> 10% of tokens)", True, bool(res["with balancing loss"][1].min() > 0.1))
    check("without balancing an expert can starve (gets 0 tokens: wasted parameters)", True, bool(res["no balancing"][1].min() < 0.05))
    torch.manual_seed(0)
    dense = nn.Sequential(nn.Linear(2, 16), nn.ReLU(), nn.Linear(16, 1))
    n_moe = sum(p.numel() for p in MoE(2).parameters())
    per_token = sum(p.numel() for p in MoE(2).experts[0].parameters()) + sum(p.numel() for p in MoE(2).router.parameters())
    show(f"MoE: {n_moe} parameters in total but only {per_token} used per token (dense net of one expert's size: {sum(p.numel() for p in dense.parameters())})")
    check("top-1 MoE has ~4x the parameters of one expert at ~1 expert's compute", True, n_moe > 3.5 * (per_token - 12))


# ---------------------------------------------------------------------------
# 7. BatchNorm backward + residual connections
# ---------------------------------------------------------------------------
def batchnorm_forward(x, gamma, beta, eps=1e-5):
    mu, var = x.mean(0), x.var(0)
    xhat = (x - mu) / np.sqrt(var + eps)
    return gamma * xhat + beta, (xhat, var, eps, gamma)


def batchnorm_backward(dout, cache):
    xhat, var, eps, gamma = cache
    n = len(xhat)
    dgamma, dbeta = (dout * xhat).sum(0), dout.sum(0)
    dxhat = dout * gamma
    dx = (n * dxhat - dxhat.sum(0) - xhat * (dxhat * xhat).sum(0)) / (n * np.sqrt(var + eps))   # the compact closed form
    return dx, dgamma, dbeta


def bn_residual_demo():
    section("7. BatchNorm backward (from scratch) + residual connections")
    rng = np.random.default_rng(5)
    x, gamma, beta = rng.normal(3, 2, (16, 4)), rng.normal(size=4), rng.normal(size=4)
    w = rng.normal(size=(16, 4))                               # loss = sum(w * BN(x)) to get a non-trivial upstream gradient
    _, cache = batchnorm_forward(x, gamma, beta)
    dx, dg, db = batchnorm_backward(w, cache)
    num = np.zeros_like(x)
    for i in range(16):
        for j in range(4):
            e = np.zeros_like(x)
            e[i, j] = 1e-6
            num[i, j] = ((w * batchnorm_forward(x + e, gamma, beta)[0]).sum() - (w * batchnorm_forward(x - e, gamma, beta)[0]).sum()) / 2e-6
    check("BatchNorm backward: analytic dx == numerical gradient", True, np.allclose(dx, num, atol=1e-6))
    check("dx sums to 0 over the batch (shifting the whole batch changes nothing)", True, np.allclose(dx.sum(0), 0, atol=1e-9))

    torch.manual_seed(0)
    depth, width = 50, 32
    xb = torch.randn(64, width)
    norms = {}
    for residual in (False, True):
        layers = [nn.Linear(width, width) for _ in range(depth)]
        h = xb
        for L in layers:
            h = h + torch.tanh(L(h)) * 0.5 if residual else torch.tanh(L(h))
        h.pow(2).mean().backward()
        norms[residual] = layers[0].weight.grad.norm().item()
    show(f"50-layer tanh net, gradient norm at the FIRST layer: plain {norms[False]:.2e}, residual {norms[True]:.2e}")
    check("residual connections (x + f(x)) keep gradients alive through depth", True, norms[True] > 100 * norms[False])


if __name__ == "__main__":
    word2vec_demo()
    contrastive_demo()
    clip_demo()
    beam_demo()
    speculative_demo()
    moe_demo()
    bn_residual_demo()
    summary()
