"""
ML 12 : FINE-TUNING, LoRA, QUANTISATION, DISTILLATION, ALIGNMENT
Run:  ../.venv/bin/python fine_tuning.py        (explanations: README.md)

  1. Transfer learning        pretrain on task A, adapt to task B: linear probe vs full fine-tune
  2. LoRA from scratch        W + (alpha/r) B A; zero-init B; merge; trainable-parameter count
  3. Forgetting               full fine-tuning forgets task A; a LoRA adapter can be switched off
  4. Low-rank updates         the singular values of a real fine-tuning update
  5. SFT loss masking         train only on the response tokens (ignore_index=-100)
  6. Quantisation             int8 / int4 round-trip error; why QLoRA works
  7. Distillation             a small student learns from a big teacher's soft labels
  8. Reward model + DPO       Bradley-Terry reward model; the DPO loss from scratch
"""
import copy
import math
import sys
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from sklearn.datasets import load_digits
from sklearn.model_selection import train_test_split

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402

torch.set_num_threads(1)


# ---------------------------------------------------------------------------
# data: task A = digits 0-4, task B = digits 5-9 (relabelled 0-4)
# ---------------------------------------------------------------------------
def tasks():
    X, y = load_digits(return_X_y=True)
    X = torch.tensor(X / 16.0, dtype=torch.float32)
    y = torch.tensor(y)
    out = {}
    for name, keep, shift in (("A", y < 5, 0), ("B", y >= 5, 5)):
        Xa, ya = X[keep], y[keep] - shift
        idx_tr, idx_te = train_test_split(np.arange(len(ya)), test_size=0.3, stratify=ya.numpy(), random_state=0)
        out[name] = (Xa[idx_tr], ya[idx_tr], Xa[idx_te], ya[idx_te])
    return out


class Net(nn.Module):
    def __init__(self):
        super().__init__()
        self.backbone = nn.Sequential(nn.Linear(64, 128), nn.ReLU(), nn.Linear(128, 128), nn.ReLU())
        self.head = nn.Linear(128, 5)

    def forward(self, x):
        return self.head(self.backbone(x))


def fit(model, X, y, epochs=60, lr=3e-3, params=None, seed=0):
    torch.manual_seed(seed)
    opt = torch.optim.Adam(params if params is not None else [p for p in model.parameters() if p.requires_grad], lr=lr)
    for _ in range(epochs):
        for idx in torch.randperm(len(X)).split(64):
            opt.zero_grad()
            F.cross_entropy(model(X[idx]), y[idx]).backward()
            opt.step()
    return model


def acc(model, X, y):
    with torch.no_grad():
        return (model(X).argmax(1) == y).float().mean().item()


def trainable(model):
    return sum(p.numel() for p in model.parameters() if p.requires_grad)


# ---------------------------------------------------------------------------
# 1. transfer learning
# ---------------------------------------------------------------------------
def adapt(pretrained, X, y, epochs=200):
    """Return (linear-probe model, full-fine-tune model), each with a fresh task head."""
    probe = copy.deepcopy(pretrained)
    probe.head = nn.Linear(128, 5)
    for p in probe.backbone.parameters():
        p.requires_grad = False                                       # freeze the backbone: linear probe
    fit(probe, X, y, epochs=epochs)
    full = copy.deepcopy(pretrained)
    full.head = nn.Linear(128, 5)
    fit(full, X, y, epochs=epochs, lr=1e-3)
    return probe, full


def transfer(T):
    section("1. Transfer learning: pretrain, then adapt to task B with 25 labels")
    XA, yA, XA_te, yA_te = T["A"]
    XB, yB, XB_te, yB_te = T["B"]
    torch.manual_seed(0)
    base = fit(Net(), XA, yA)
    show(f"supervised pretraining on task A (digits 0-4): test accuracy {acc(base, XA_te, yA_te):.3f}")
    # self-supervised pretraining: a denoising autoencoder on ALL unlabelled training images (no labels used)
    torch.manual_seed(0)
    ssl, dec = Net(), nn.Linear(128, 64)
    opt = torch.optim.Adam(list(ssl.backbone.parameters()) + list(dec.parameters()), lr=3e-3)
    X_unlab = torch.cat([XA, XB])
    for _ in range(100):
        for idx in torch.randperm(len(X_unlab)).split(64):
            x = X_unlab[idx]
            opt.zero_grad()
            F.mse_loss(torch.sigmoid(dec(ssl.backbone(x * (torch.rand_like(x) > 0.3)))), x).backward()   # mask 30% of pixels, rebuild
            opt.step()
    few = torch.cat([torch.nonzero(yB == c)[:5, 0] for c in range(5)])   # only 5 labelled examples per class for B
    torch.manual_seed(0)
    scratch = fit(Net(), XB[few], yB[few], epochs=200)
    sup_probe, sup_full = adapt(base, XB[few], yB[few])
    ssl_probe, ssl_full = adapt(ssl, XB[few], yB[few])
    r = {k: acc(m, XB_te, yB_te) for k, m in (("scratch", scratch), ("sup_probe", sup_probe), ("sup_full", sup_full), ("ssl_probe", ssl_probe), ("ssl_full", ssl_full))}
    show(f"task B accuracy, 25 labels: from scratch {r['scratch']:.3f}")
    show(f"  narrow supervised pretraining (A): linear probe {r['sup_probe']:.3f}, full fine-tune {r['sup_full']:.3f}")
    show(f"  self-supervised on unlabelled images: linear probe {r['ssl_probe']:.3f}, full fine-tune {r['ssl_full']:.3f}")
    show(f"trainable parameters: full {trainable(sup_full):,}, linear probe {trainable(sup_probe):,}")
    check("features from a narrow task (digits 0-4) transfer badly to 5-9", True, r["sup_probe"] < r["scratch"])
    check("broad self-supervised features beat training from scratch on tiny data", True, min(r["ssl_probe"], r["ssl_full"]) > r["scratch"])
    check("a linear probe trains only the head (128*5 + 5)", 645, trainable(sup_probe))
    return base


# ---------------------------------------------------------------------------
# 2. LoRA from scratch
# ---------------------------------------------------------------------------
class LoRALinear(nn.Module):
    """y = x W^T + b + (alpha / r) * x A^T B^T.  W is frozen; only A (r x in) and B (out x r) train."""

    def __init__(self, base: nn.Linear, r=4, alpha=8):
        super().__init__()
        self.base = base
        for p in self.base.parameters():
            p.requires_grad = False
        self.A = nn.Parameter(torch.randn(r, base.in_features) / math.sqrt(base.in_features))
        self.B = nn.Parameter(torch.zeros(base.out_features, r))        # zero init -> starts as the base model
        self.scale = alpha / r
        self.enabled = True

    def forward(self, x):
        out = self.base(x)
        if self.enabled:
            out = out + self.scale * (x @ self.A.T @ self.B.T)
        return out

    def merged(self):
        """Fold the adapter into one ordinary Linear: no extra inference latency."""
        m = nn.Linear(self.base.in_features, self.base.out_features)
        with torch.no_grad():
            m.weight.copy_(self.base.weight + self.scale * self.B @ self.A)
            m.bias.copy_(self.base.bias)
        return m


def add_lora(model, r=4, alpha=8):
    m = copy.deepcopy(model)
    for p in m.parameters():
        p.requires_grad = False
    m.backbone[0] = LoRALinear(m.backbone[0], r, alpha)
    m.backbone[2] = LoRALinear(m.backbone[2], r, alpha)
    m.head = nn.Linear(128, 5)                                          # new task head (trainable)
    return m


def lora(T, base):
    section("2. LoRA from scratch")
    XB, yB, XB_te, yB_te = T["B"]
    layer = nn.Linear(128, 128)
    lo = LoRALinear(copy.deepcopy(layer), r=4)
    x = torch.randn(10, 128)
    check("B = 0 at init: the LoRA layer starts identical to the base layer", True, torch.allclose(lo(x), layer(x)))
    with torch.no_grad():
        lo.B.normal_()
    check("merged weight W + (alpha/r) B A gives the same outputs", True, torch.allclose(lo(x), lo.merged()(x), atol=1e-4))
    check("LoRA params for a 128x128 layer at r=4: r*(in+out)", 4 * (128 + 128), lo.A.numel() + lo.B.numel())
    d = 4096
    show(f"for a 4096x4096 attention matrix: full {d * d:,} weights vs LoRA r=8 {8 * 2 * d:,} ({8 * 2 * d / (d * d):.2%})")
    lm = add_lora(base, r=4)
    fit(lm, XB, yB, epochs=60)
    full = copy.deepcopy(base)
    full.head = nn.Linear(128, 5)
    fit(full, XB, yB, epochs=60, lr=1e-3)
    a_lora, a_full = acc(lm, XB_te, yB_te), acc(full, XB_te, yB_te)
    show(f"task B with all labels: full fine-tune {a_full:.3f} ({trainable(full):,} params), LoRA r=4 {a_lora:.3f} ({trainable(lm):,} params)")
    check("LoRA trains far fewer parameters", True, trainable(lm) < 0.25 * trainable(full))
    check("...and gets close to full fine-tuning accuracy", True, a_lora > a_full - 0.03)
    return lm, full


# ---------------------------------------------------------------------------
# 3. catastrophic forgetting
# ---------------------------------------------------------------------------
def forgetting(T, base, lm, full):
    section("3. Catastrophic forgetting")
    XA, yA, XA_te, yA_te = T["A"]
    before = acc(base, XA_te, yA_te)

    def task_a_acc(model):
        """Re-attach the ORIGINAL task-A head to the model's (possibly changed) backbone."""
        probe = copy.deepcopy(model)
        probe.head = copy.deepcopy(base.head)
        return acc(probe, XA_te, yA_te)

    after_full = task_a_acc(full)
    for layer in (lm.backbone[0], lm.backbone[2]):
        layer.enabled = False                                         # switch the adapter off
    after_lora_off = task_a_acc(lm)
    for layer in (lm.backbone[0], lm.backbone[2]):
        layer.enabled = True
    show(f"task A accuracy: before {before:.3f}; after full fine-tune on B {after_full:.3f}; LoRA model with adapter OFF {after_lora_off:.3f}")
    check("full fine-tuning on B damages task A (catastrophic forgetting)", True, after_full < before - 0.05)
    check("with LoRA the base weights never changed: switch the adapter off and A is intact", round(before, 6), round(after_lora_off, 6))


# ---------------------------------------------------------------------------
# 4. how low-rank is a real fine-tuning update?
# ---------------------------------------------------------------------------
def low_rank(base, full):
    section("4. Fine-tuning updates are approximately low-rank")
    delta = (full.backbone[2].weight - base.backbone[2].weight).detach()
    s = torch.linalg.svdvals(delta)
    energy = torch.cumsum(s ** 2, 0) / torch.sum(s ** 2)
    k90 = int(torch.searchsorted(energy, torch.tensor(0.9)).item()) + 1
    rand_s = torch.linalg.svdvals(torch.randn(128, 128))
    rand_k90 = int(torch.searchsorted(torch.cumsum(rand_s ** 2, 0) / torch.sum(rand_s ** 2), torch.tensor(0.9)).item()) + 1
    show(f"weight update of a 128x128 layer: 90% of its energy in the top {k90} singular directions (a random matrix needs {rand_k90})")
    check("the update is concentrated in few directions -> low rank suffices", True, k90 < rand_k90 / 2)


# ---------------------------------------------------------------------------
# 5. SFT loss masking
# ---------------------------------------------------------------------------
def sft_masking():
    section("5. Supervised fine-tuning: loss only on the response")
    vocab = 10
    prompt, response = [1, 2, 3, 4], [5, 6, 7]
    tokens = torch.tensor(prompt + response)
    labels = tokens.clone()
    labels[:len(prompt)] = -100                                       # don't train the model to predict the prompt
    torch.manual_seed(0)
    logits = torch.randn(len(tokens), vocab)
    # next-token: logits at position t predict token t+1
    loss_masked = F.cross_entropy(logits[:-1], labels[1:], ignore_index=-100)
    manual = F.cross_entropy(logits[len(prompt) - 1:-1], tokens[len(prompt):])
    check("ignore_index=-100 averages over response tokens only", True, torch.allclose(loss_masked, manual))
    check("number of positions contributing to the loss", 3, int((labels[1:] != -100).sum()))
    template = "<|user|>\n{prompt}\n<|assistant|>\n{response}<|end|>"
    show(f"chat template: {template!r}  (train and serve with the SAME template)")


# ---------------------------------------------------------------------------
# 6. quantisation
# ---------------------------------------------------------------------------
def quantise(w, bits, per_channel=True):
    qmax = 2 ** (bits - 1) - 1
    amax = w.abs().amax(dim=1, keepdim=True) if per_channel else w.abs().max()
    scale = amax / qmax
    q = torch.clamp(torch.round(w / scale), -qmax - 1, qmax)          # integers
    return q * scale                                                   # dequantised


def quantisation(full, T):
    section("6. Quantisation")
    w = full.backbone[2].weight.detach()
    for bits in (8, 4):
        err = (quantise(w, bits) - w).norm() / w.norm()
        show(f"int{bits} per-channel: relative weight error {err:.4f}, memory {bits / 32:.0%} of fp32")
    check("int8 error is tiny (< 1%)", True, bool(((quantise(w, 8) - w).norm() / w.norm()) < 0.01))
    check("int4 error is larger but usable (< 15%)", True, bool(((quantise(w, 4) - w).norm() / w.norm()) < 0.15))
    w_out = w.clone()
    w_out[0, 0] = 50.0                                                # one outlier weight
    e_tensor = (quantise(w_out, 4, per_channel=False) - w_out)[1:].norm() / w_out[1:].norm()
    e_chan = (quantise(w_out, 4, per_channel=True) - w_out)[1:].norm() / w_out[1:].norm()
    show(f"with one outlier weight: int4 per-tensor error on the other rows {e_tensor:.3f}, per-channel {e_chan:.3f}")
    check("an outlier ruins a single shared scale; per-channel (or per-group) scales contain it", True, bool(e_tensor > 3 * e_chan))
    XB, yB, XB_te, yB_te = T["B"]
    q = copy.deepcopy(full)
    with torch.no_grad():
        for layer in (q.backbone[0], q.backbone[2], q.head):
            layer.weight.copy_(quantise(layer.weight, 4))
    show(f"task B accuracy: fp32 {acc(full, XB_te, yB_te):.3f}, all weights int4 {acc(q, XB_te, yB_te):.3f}")
    check("int4 weights barely change accuracy here", True, acc(q, XB_te, yB_te) > acc(full, XB_te, yB_te) - 0.03)


# ---------------------------------------------------------------------------
# 7. distillation
# ---------------------------------------------------------------------------
def distillation():
    section("7. Knowledge distillation")
    X, y = load_digits(return_X_y=True)
    X = torch.tensor(X / 16.0, dtype=torch.float32)
    y = torch.tensor(y)
    idx_tr, idx_te = train_test_split(np.arange(len(y)), test_size=0.5, stratify=y.numpy(), random_state=1)
    X_tr, y_tr, X_te, y_te = X[idx_tr], y[idx_tr], X[idx_te], y[idx_te]
    small = idx_tr[:150]                                              # the student only sees 150 labelled images
    torch.manual_seed(0)
    teacher = nn.Sequential(nn.Linear(64, 256), nn.ReLU(), nn.Linear(256, 256), nn.ReLU(), nn.Linear(256, 10))
    fit(teacher, X_tr, y_tr, epochs=60)
    make_student = lambda: nn.Sequential(nn.Linear(64, 16), nn.ReLU(), nn.Linear(16, 10))  # noqa: E731

    torch.manual_seed(1)
    hard = fit(make_student(), X[small], y[small], epochs=300)
    torch.manual_seed(1)
    student = make_student()
    opt = torch.optim.Adam(student.parameters(), lr=3e-3)
    Tmp, alpha = 4.0, 0.7
    with torch.no_grad():
        soft_all = F.softmax(teacher(X_tr) / Tmp, 1)                  # the teacher labels ALL training images
    for _ in range(300):
        for idx in torch.randperm(len(X_tr)).split(64):
            opt.zero_grad()
            s = student(X_tr[idx])
            kd = F.kl_div(F.log_softmax(s / Tmp, 1), soft_all[idx], reduction="batchmean") * Tmp ** 2
            labelled = torch.isin(torch.tensor(idx_tr)[idx], torch.tensor(small))
            ce = F.cross_entropy(s[labelled], y_tr[idx][labelled]) if labelled.any() else torch.tensor(0.0)
            (alpha * kd + (1 - alpha) * ce).backward()
            opt.step()
    res = {"teacher (85k params)": acc(teacher, X_te, y_te), "student, 150 hard labels": acc(hard, X_te, y_te), "student, distilled": acc(student, X_te, y_te)}
    show(", ".join(f"{k} {v:.3f}" for k, v in res.items()))
    check("distillation (teacher soft labels on unlabelled data) beats hard labels alone", True, res["student, distilled"] > res["student, 150 hard labels"] + 0.02)
    with torch.no_grad():
        p1, p4 = F.softmax(teacher(X_te[:1]), 1)[0], F.softmax(teacher(X_te[:1]) / 4, 1)[0]
    show(f"teacher's 2nd-best class probability: T=1 {p1.sort(descending=True).values[1]:.4f}, T=4 {p4.sort(descending=True).values[1]:.4f}")
    check("temperature reveals the 'dark knowledge' in the non-top classes", True, bool(p4.sort(descending=True).values[1] > p1.sort(descending=True).values[1]))


# ---------------------------------------------------------------------------
# 8. reward model + DPO
# ---------------------------------------------------------------------------
def dpo_loss(pi_c, pi_r, ref_c, ref_r, beta):
    """-log sigmoid(beta * [(log pi(c) - log ref(c)) - (log pi(r) - log ref(r))])"""
    return -F.logsigmoid(beta * ((pi_c - ref_c) - (pi_r - ref_r))).mean()


def alignment():
    section("8. Reward model (Bradley-Terry) and DPO")
    # reward model: hidden quality = w_true . features; humans prefer the higher-quality answer
    torch.manual_seed(0)
    w_true = torch.tensor([2.0, -1.0, 0.5])
    a, b = torch.randn(2000, 3), torch.randn(2000, 3)
    prefer_a = torch.rand(2000) < torch.sigmoid(a @ w_true - b @ w_true)    # Bradley-Terry: P(a > b) = sigmoid(r_a - r_b)
    chosen = torch.where(prefer_a[:, None], a, b)
    rejected = torch.where(prefer_a[:, None], b, a)
    w = torch.zeros(3, requires_grad=True)
    opt = torch.optim.Adam([w], lr=0.05)
    for _ in range(300):
        opt.zero_grad()
        (-F.logsigmoid(chosen @ w - rejected @ w).mean()).backward()
        opt.step()
    show(f"learned reward weights {[round(v, 2) for v in w.tolist()]} (true [2, -1, 0.5])")
    check("the reward model recovers the hidden preference direction", True, F.cosine_similarity(w.detach(), w_true, 0).item() > 0.99)

    # DPO on a toy policy: 4 possible answers to one prompt
    ref_logits = torch.tensor([1.0, 1.0, 0.0, 0.0])
    ref_logp = F.log_softmax(ref_logits, 0)
    near("DPO loss at the start (policy == reference) = ln 2", math.log(2), dpo_loss(ref_logp[0], ref_logp[1], ref_logp[0], ref_logp[1], 0.1).item(), 1e-6)
    # preference data with disagreement: (chosen, rejected, fraction of raters who agree)
    pairs = [(0, 1, 0.8), (0, 2, 0.9), (0, 3, 0.9), (2, 3, 0.7)]
    finals = {}
    for beta in (0.1, 1.0):
        logits = ref_logits.clone().requires_grad_(True)
        opt = torch.optim.Adam([logits], lr=0.05)
        for _ in range(2000):
            lp = F.log_softmax(logits, 0)
            loss = sum(q * dpo_loss(lp[c], lp[r], ref_logp[c], ref_logp[r], beta)
                       + (1 - q) * dpo_loss(lp[r], lp[c], ref_logp[r], ref_logp[c], beta) for c, r, q in pairs) / len(pairs)
            opt.zero_grad()
            loss.backward()
            opt.step()
        p = F.softmax(logits.detach(), 0)
        finals[beta] = (p, (p * (p.log() - F.softmax(ref_logits, 0).log())).sum().item())
        show(f"beta={beta}: policy {[round(v, 3) for v in p.tolist()]} (reference {[round(v, 3) for v in F.softmax(ref_logits, 0).tolist()]}), KL to reference {finals[beta][1]:.3f}")
    p1 = finals[1.0][0]
    check("DPO moves probability onto the preferred answer", True, bool(p1[0] > 0.5))
    check("...and orders the answers as the preferences say: 0 > 1 > 2 > 3", [0, 1, 2, 3], torch.argsort(p1, descending=True).tolist())
    check("larger beta keeps the policy closer to the reference (smaller KL)", True, finals[1.0][1] < finals[0.1][1])

if __name__ == "__main__":
    T = tasks()
    base = transfer(T)
    lm, full = lora(T, base)
    forgetting(T, base, lm, full)
    low_rank(base, full)
    sft_masking()
    quantisation(full, T)
    distillation()
    alignment()
    summary()
