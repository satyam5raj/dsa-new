"""
ML 09 : PYTORCH TRAINING (the real-world toolkit)
Run:  ../.venv/bin/python pytorch_training.py        (explanations: README.md)

  1. Tensors              shapes, dtypes, devices, NumPy interop
  2. Autograd             requires_grad, backward(), matches the chapter-08 maths
  3. nn.Module            define a model, count parameters
  4. Dataset/DataLoader   batching and shuffling
  5. The training loop    train/eval modes, zero_grad, clipping, early stopping, checkpoints
  6. LR schedules         warm-up + cosine decay
  7. Save / load          state_dict round trip, reproducibility
"""
import copy
import io
import math
import sys
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from sklearn.datasets import load_digits
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from torch.utils.data import DataLoader, Dataset, TensorDataset

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402

torch.manual_seed(0)
torch.set_num_threads(1)              # deterministic CPU results for the self-checks
DEVICE = "cpu"                        # use "cuda" / "mps" for speed; CPU keeps this file reproducible


# ---------------------------------------------------------------------------
# 1. tensors
# ---------------------------------------------------------------------------
def tensors():
    section("1. Tensors")
    x = torch.arange(6, dtype=torch.float32).reshape(2, 3)
    check("a tensor has a shape", (2, 3), tuple(x.shape))
    check("default float dtype", "torch.float32", str(x.dtype))
    check("broadcasting works like NumPy: (2,3) + (3,)", [[10.0, 21.0, 32.0], [13.0, 24.0, 35.0]], (x + torch.tensor([10.0, 20.0, 30.0])).tolist())
    check("matrix multiply with @", (2, 4), tuple((x @ torch.ones(3, 4)).shape))
    a = np.ones(3)
    t = torch.from_numpy(a)
    a[0] = 99
    check("torch.from_numpy SHARES memory with the NumPy array", 99.0, t[0].item())
    check("view/reshape vs unsqueeze: (3,) -> (1,3)", (1, 3), tuple(torch.zeros(3).unsqueeze(0).shape))
    show(f"devices available here: cpu, mps={torch.backends.mps.is_available()}, cuda={torch.cuda.is_available()}")


# ---------------------------------------------------------------------------
# 2. autograd
# ---------------------------------------------------------------------------
def autograd():
    section("2. Autograd")
    w = torch.tensor(3.0, requires_grad=True)
    y = w ** 2 + 2 * w                      # dy/dw = 2w + 2 = 8
    y.backward()
    check("d(w^2 + 2w)/dw at w=3", 8.0, w.grad.item())
    y2 = w ** 2 + 2 * w
    y2.backward()
    check("gradients ACCUMULATE if you forget zero_grad: 8 + 8", 16.0, w.grad.item())
    w.grad.zero_()
    # logistic-regression gradient equals the (p - y) x formula from chapter 04
    torch.manual_seed(1)
    X, yb = torch.randn(50, 3), torch.randint(0, 2, (50,)).float()
    wt = torch.zeros(3, requires_grad=True)
    loss = F.binary_cross_entropy_with_logits(X @ wt, yb)
    loss.backward()
    manual = X.T @ (torch.sigmoid(X @ wt.detach()) - yb) / 50
    check("autograd == the hand-derived X^T(p - y)/n", True, torch.allclose(wt.grad, manual, atol=1e-6))
    with torch.no_grad():
        z = wt * 2
    check("inside torch.no_grad() nothing is tracked (inference)", False, z.requires_grad)
    check(".detach() cuts a tensor out of the graph", False, wt.detach().requires_grad)


# ---------------------------------------------------------------------------
# 3. nn.Module
# ---------------------------------------------------------------------------
class MLP(nn.Module):
    def __init__(self, n_in, n_hidden, n_out, dropout=0.2):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(n_in, n_hidden), nn.BatchNorm1d(n_hidden), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(n_hidden, n_hidden), nn.BatchNorm1d(n_hidden), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(n_hidden, n_out),                         # logits: CrossEntropyLoss applies log-softmax itself
        )

    def forward(self, x):
        return self.net(x)


def modules():
    section("3. nn.Module")
    model = MLP(64, 128, 10)
    n = sum(p.numel() for p in model.parameters())
    check("parameters: (64*128+128) + 2*128 + (128*128+128) + 2*128 + (128*10+10)", 26634, n)
    check("a fresh model is in training mode", True, model.training)
    model.eval()
    check("model.eval() switches dropout off and BatchNorm to running stats", False, model.training)
    names = [name for name, _ in model.named_parameters()][:2]
    check("named parameters", ["net.0.weight", "net.0.bias"], names)


# ---------------------------------------------------------------------------
# 4. Dataset / DataLoader
# ---------------------------------------------------------------------------
class DigitsDataset(Dataset):
    """A custom Dataset needs __len__ and __getitem__. Real ones read files or a database lazily."""

    def __init__(self, X, y):
        self.X, self.y = torch.tensor(X, dtype=torch.float32), torch.tensor(y, dtype=torch.long)

    def __len__(self):
        return len(self.y)

    def __getitem__(self, i):
        return self.X[i], self.y[i]


def load_data():
    X, y = load_digits(return_X_y=True)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, stratify=y, random_state=0)
    X_tr, X_va, y_tr, y_va = train_test_split(X_tr, y_tr, test_size=0.2, stratify=y_tr, random_state=0)
    scaler = StandardScaler().fit(X_tr)
    return [(scaler.transform(a), b) for a, b in ((X_tr, y_tr), (X_va, y_va), (X_te, y_te))]


def dataloaders():
    section("4. Dataset and DataLoader")
    (X_tr, y_tr), _, _ = load_data()
    ds = DigitsDataset(X_tr, y_tr)
    check("len(dataset)", len(y_tr), len(ds))
    x0, y0 = ds[0]
    check("one item: (features, label)", ((64,), "torch.int64"), (tuple(x0.shape), str(y0.dtype)))
    loader = DataLoader(ds, batch_size=64, shuffle=True, generator=torch.Generator().manual_seed(0))
    xb, yb = next(iter(loader))
    check("a batch stacks items: (64, 64)", (64, 64), tuple(xb.shape))
    check("number of batches = ceil(1149 / 64)", math.ceil(len(ds) / 64), len(loader))
    check("drop_last=True drops the ragged final batch", len(ds) // 64, len(DataLoader(ds, batch_size=64, drop_last=True)))


# ---------------------------------------------------------------------------
# 5-6. training loop + warm-up/cosine schedule
# ---------------------------------------------------------------------------
def warmup_cosine(step, warmup, total):
    if step < warmup:
        return (step + 1) / warmup                              # linear warm-up from ~0 to 1
    progress = (step - warmup) / max(1, total - warmup)
    return 0.5 * (1 + math.cos(math.pi * progress))           # cosine decay 1 -> 0


def evaluate(model, X, y):
    model.eval()
    with torch.no_grad():
        logits = model(X)
        loss = F.cross_entropy(logits, y).item()
        acc = (logits.argmax(1) == y).float().mean().item()
    model.train()
    return loss, acc


def fit(model, train_loader, X_va, y_va, epochs=60, lr=3e-3, patience=8, clip=1.0):
    opt = torch.optim.AdamW(model.parameters(), lr=lr, weight_decay=1e-2)
    total = epochs * len(train_loader)
    sched = torch.optim.lr_scheduler.LambdaLR(opt, lambda s: warmup_cosine(s, warmup=total // 10, total=total))
    best, best_state, bad, lrs, history = float("inf"), None, 0, [], []
    for epoch in range(epochs):
        model.train()
        for xb, yb in train_loader:
            xb, yb = xb.to(DEVICE), yb.to(DEVICE)
            opt.zero_grad()                                    # 1. clear old gradients
            loss = F.cross_entropy(model(xb), yb)              # 2. forward + loss
            loss.backward()                                    # 3. backprop
            nn.utils.clip_grad_norm_(model.parameters(), clip) # 4. clip exploding gradients
            opt.step()                                         # 5. update weights
            sched.step()                                       # 6. update the learning rate (per step)
            lrs.append(opt.param_groups[0]["lr"])
        va_loss, va_acc = evaluate(model, X_va, y_va)
        history.append((va_loss, va_acc))
        if va_loss < best - 1e-4:
            best, best_state, bad = va_loss, copy.deepcopy(model.state_dict()), 0
        else:
            bad += 1
            if bad >= patience:                                # early stopping
                break
    model.load_state_dict(best_state)                          # restore the best checkpoint
    return history, lrs, epoch + 1


def training_loop():
    section("5-6. The training loop, warm-up + cosine schedule, early stopping")
    (X_tr, y_tr), (X_va, y_va), (X_te, y_te) = load_data()
    to_t = lambda X, y: (torch.tensor(X, dtype=torch.float32), torch.tensor(y, dtype=torch.long))  # noqa: E731
    X_va_t, y_va_t = to_t(X_va, y_va)
    X_te_t, y_te_t = to_t(X_te, y_te)
    loader = DataLoader(DigitsDataset(X_tr, y_tr), batch_size=64, shuffle=True, generator=torch.Generator().manual_seed(0))
    torch.manual_seed(0)
    model = MLP(64, 128, 10).to(DEVICE)
    first_loss = evaluate(model, X_va_t, y_va_t)[0]
    near("an untrained 10-class model's loss ~ ln 10 = 2.30", math.log(10), first_loss, 0.3)
    history, lrs, epochs_run = fit(model, loader, X_va_t, y_va_t)
    te_loss, te_acc = evaluate(model, X_te_t, y_te_t)
    show(f"ran {epochs_run} epochs; best val loss {min(h[0] for h in history):.4f}; test accuracy {te_acc:.3f}")
    check("the MLP classifies handwritten digits well", True, te_acc > 0.95)
    warm = lrs[:5]                                             # recorded after each sched.step()
    show(f"learning rate: first steps {[f'{v:.1e}' for v in warm]}, peak {max(lrs):.1e}, last {lrs[-1]:.1e}")
    check("warm-up: the LR starts tiny and rises", True, lrs[0] < 0.05 * max(lrs) and lrs[10] > lrs[0])
    check("the schedule peaks at the base LR 3e-3", True, abs(max(lrs) - 3e-3) < 1e-9)
    full = [3e-3 * warmup_cosine(s, 60 * len(loader) // 10, 60 * len(loader)) for s in range(60 * len(loader))]
    near("cosine decay reaches ~0 at the very end of the full schedule", 0.0, full[-1], 1e-6)

    # gradient clipping: a huge gradient is rescaled to norm 1
    p = nn.Parameter(torch.zeros(3))
    p.grad = torch.tensor([300.0, 400.0, 0.0])
    norm_before = nn.utils.clip_grad_norm_([p], max_norm=1.0)
    check("clip_grad_norm_ reports the original norm (sqrt(300^2+400^2))", 500.0, round(norm_before.item(), 3))
    check("...and rescales the gradient to norm 1, same direction", [0.6, 0.8, 0.0], [round(v, 4) for v in p.grad.tolist()])
    return model, X_te_t, y_te_t


# ---------------------------------------------------------------------------
# 7. save / load and reproducibility
# ---------------------------------------------------------------------------
def save_load(model, X_te, y_te):
    section("7. Save / load and reproducibility")
    buf = io.BytesIO()
    torch.save(model.state_dict(), buf)                        # save the WEIGHTS, not the pickled object
    show(f"state_dict size: {buf.tell() / 1024:.0f} KB")
    buf.seek(0)
    restored = MLP(64, 128, 10)
    restored.load_state_dict(torch.load(buf, weights_only=True))
    restored.eval()
    model.eval()
    with torch.no_grad():
        same = torch.equal(model(X_te), restored(X_te))
    check("a reloaded model gives bit-identical predictions", True, same)

    def one_run(seed):
        torch.manual_seed(seed)
        m = nn.Linear(4, 1)
        o = torch.optim.SGD(m.parameters(), lr=0.1)
        x = torch.randn(32, 4)
        for _ in range(5):
            o.zero_grad()
            m(x).pow(2).mean().backward()
            o.step()
        return m.weight.detach().clone()

    check("same seed -> identical training run", True, torch.equal(one_run(7), one_run(7)))
    check("different seed -> different run", False, torch.equal(one_run(7), one_run(8)))
    _ = TensorDataset  # TensorDataset(X, y) is the one-line alternative to a custom Dataset


if __name__ == "__main__":
    tensors()
    autograd()
    modules()
    dataloaders()
    m, X_te, y_te = training_loop()
    save_load(m, X_te, y_te)
    summary()
