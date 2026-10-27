"""
ML 10 : DEEP-LEARNING ARCHITECTURES
Run:  ../.venv/bin/python architectures.py        (explanations: README.md)

  1. Convolution from scratch      NumPy conv2d == torch, output-size formula, edge detection
  2. CNN vs MLP                    on 8x8 digits; parameter sharing; shifted images
  3. RNN vs LSTM                   remembering something from far back in a sequence
  4. Autoencoder                   compression + anomaly detection by reconstruction error
  5. VAE                           reparameterisation trick, KL term, sampling new digits
  6. GAN                           a generator learns to imitate a distribution
  7. GNN                           graph convolution from scratch (message passing)
  8. Diffusion                     forward noising, a tiny denoiser, sampling
"""
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


def digits():
    X, y = load_digits(return_X_y=True)
    X = (X / 16.0).astype(np.float32)                    # pixels 0..16 -> 0..1
    return train_test_split(X, y, test_size=0.25, stratify=y, random_state=0)


def train_classifier(model, X, y, epochs=40, lr=3e-3, seed=0):
    torch.manual_seed(seed)
    opt = torch.optim.Adam(model.parameters(), lr=lr)
    X, y = torch.tensor(X), torch.tensor(y)
    g = torch.Generator().manual_seed(seed)
    for _ in range(epochs):
        for idx in torch.randperm(len(X), generator=g).split(64):
            opt.zero_grad()
            F.cross_entropy(model(X[idx]), y[idx]).backward()
            opt.step()
    return model


def accuracy(model, X, y):
    model.eval()
    with torch.no_grad():
        return (model(torch.tensor(X)).argmax(1).numpy() == y).mean()


# ---------------------------------------------------------------------------
# 1. convolution from scratch
# ---------------------------------------------------------------------------
def conv2d(img, kernel, stride=1, padding=0):
    """One channel in, one channel out. (Deep-learning 'convolution' is really cross-correlation: no kernel flip.)"""
    img = np.pad(img, padding)
    k = kernel.shape[0]
    out = (img.shape[0] - k) // stride + 1
    res = np.zeros((out, out))
    for i in range(out):
        for j in range(out):
            patch = img[i * stride:i * stride + k, j * stride:j * stride + k]
            res[i, j] = np.sum(patch * kernel)            # slide the kernel, dot product with each patch
    return res


def convolution():
    section("1. Convolution from scratch")
    rng = np.random.default_rng(0)
    img, ker = rng.normal(size=(7, 7)), rng.normal(size=(3, 3))
    ours = conv2d(img, ker, stride=2, padding=1)
    theirs = F.conv2d(torch.tensor(img)[None, None], torch.tensor(ker)[None, None], stride=2, padding=1)[0, 0].numpy()
    check("NumPy conv2d == torch.nn.functional.conv2d", True, np.allclose(ours, theirs))
    out_size = lambda n, k, s, p: (n + 2 * p - k) // s + 1   # noqa: E731
    check("output size = (n + 2p - k)/s + 1 : 7x7, k3, s2, p1", 4, out_size(7, 3, 2, 1))
    check("'same' padding keeps the size: 28, k3, s1, p1", 28, out_size(28, 3, 1, 1))
    step = np.zeros((6, 6))
    step[:, 3:] = 1.0                                    # dark left half, bright right half
    sobel_x = np.array([[-1, 0, 1], [-2, 0, 2], [-1, 0, 1]], dtype=float)
    edges = conv2d(step, sobel_x)
    check("a vertical-edge kernel fires only at the edge", [0.0, 4.0, 4.0, 0.0], edges[0].tolist())
    pooled = F.max_pool2d(torch.arange(16.0).reshape(1, 1, 4, 4), 2)[0, 0].tolist()
    check("2x2 max pooling keeps the strongest value per window", [[5.0, 7.0], [13.0, 15.0]], pooled)
    dense_params = (224 * 224 * 3) * 64
    conv_params = 3 * 3 * 3 * 64 + 64
    show(f"first layer on a 224x224 RGB image: dense to 64 units {dense_params:,} weights; 64 3x3 conv filters {conv_params:,}")
    check("parameter sharing: conv uses ~5,000x fewer weights", True, dense_params / conv_params > 5000)


# ---------------------------------------------------------------------------
# 2. CNN vs MLP on digits
# ---------------------------------------------------------------------------
class SmallCNN(nn.Module):
    def __init__(self):
        super().__init__()
        self.features = nn.Sequential(
            nn.Conv2d(1, 16, 3, padding=1), nn.ReLU(),       # 8x8x16
            nn.Conv2d(16, 32, 3, padding=1), nn.ReLU(),      # 8x8x32
            nn.MaxPool2d(2),                                 # 4x4x32
        )
        self.head = nn.Sequential(nn.Flatten(), nn.Linear(4 * 4 * 32, 10))

    def forward(self, x):
        return self.head(self.features(x.view(-1, 1, 8, 8)))


def shift(X, dx):
    imgs = X.reshape(-1, 8, 8)
    out = np.zeros_like(imgs)
    out[:, :, dx:] = imgs[:, :, :8 - dx]                 # move every image right by dx pixels
    return out.reshape(-1, 64)


def cnn_vs_mlp():
    section("2. CNN vs MLP on 8x8 digits")
    X_tr, X_te, y_tr, y_te = digits()
    torch.manual_seed(0)
    mlp = train_classifier(nn.Sequential(nn.Linear(64, 128), nn.ReLU(), nn.Linear(128, 10)), X_tr, y_tr)
    torch.manual_seed(0)
    cnn = train_classifier(SmallCNN(), X_tr, y_tr)
    a_mlp, a_cnn = accuracy(mlp, X_te, y_te), accuracy(cnn, X_te, y_te)
    s_mlp, s_cnn = accuracy(mlp, shift(X_te, 1), y_te), accuracy(cnn, shift(X_te, 1), y_te)
    show(f"test accuracy: MLP {a_mlp:.3f}, CNN {a_cnn:.3f}  |  images shifted 1px right: MLP {s_mlp:.3f}, CNN {s_cnn:.3f}")
    check("the CNN is at least as accurate as the MLP", True, a_cnn >= a_mlp - 0.005)
    check("the CNN is more robust to shifts (translation equivariance + pooling)", True, s_cnn > s_mlp)
    torch.manual_seed(0)
    aug = train_classifier(SmallCNN(), np.vstack([X_tr, shift(X_tr, 1)]), np.r_[y_tr, y_tr])
    s_aug = accuracy(aug, shift(X_te, 1), y_te)
    show(f"CNN trained with shift augmentation: shifted-test accuracy {s_aug:.3f}")
    check("data augmentation teaches the invariance directly", True, s_aug > s_cnn)


# ---------------------------------------------------------------------------
# 3. RNN vs LSTM: long-range memory
# ---------------------------------------------------------------------------
class SeqModel(nn.Module):
    def __init__(self, cell, hidden=32):
        super().__init__()
        self.rnn = {"rnn": nn.RNN, "lstm": nn.LSTM, "gru": nn.GRU}[cell](1, hidden, batch_first=True)
        self.out = nn.Linear(hidden, 2)

    def forward(self, x):
        h, _ = self.rnn(x)
        return self.out(h[:, -1])                       # classify from the LAST hidden state


def memory_task(n, length, seed):
    """The label is the sign of the FIRST value; everything after it is distracting noise."""
    g = torch.Generator().manual_seed(seed)
    x = torch.randn(n, length, 1, generator=g)
    y = (x[:, 0, 0] > 0).long()
    return x, y


def run_memory(cell, length, seed, forget_bias=None, steps=300):
    torch.manual_seed(seed)
    m = SeqModel(cell)
    if forget_bias is not None:                          # PyTorch gate order: input, FORGET, cell, output
        H = m.rnn.hidden_size
        with torch.no_grad():
            for name, p in m.rnn.named_parameters():
                if "bias" in name:
                    p[H:2 * H].fill_(forget_bias / 2)    # two bias vectors (ih + hh) add up to forget_bias
    opt = torch.optim.Adam(m.parameters(), lr=1e-2)
    x, y = memory_task(2048, length, seed=1)
    for _ in range(steps):
        idx = torch.randint(0, 2048, (128,))
        opt.zero_grad()
        F.cross_entropy(m(x[idx]), y[idx]).backward()
        nn.utils.clip_grad_norm_(m.parameters(), 1.0)
        opt.step()
    xt, yt = memory_task(1000, length, seed=2)
    with torch.no_grad():
        return (m(xt).argmax(1) == yt).float().mean().item()


def rnn_vs_lstm():
    section("3. RNN vs LSTM: remember the first step of a long sequence")
    short = {c: run_memory(c, 5, 0) for c in ("rnn", "lstm")}
    show(f"length 5: RNN {short['rnn']:.3f}, LSTM {short['lstm']:.3f}")
    check("both solve the task when the sequence is short (5 steps)", True, min(short.values()) > 0.9)
    rnn = [run_memory("rnn", 40, s) for s in (0, 1, 2)]
    lstm_default = run_memory("lstm", 40, 0)
    lstm_fb = [run_memory("lstm", 40, s, forget_bias=3.0) for s in (0, 1, 2)]
    show(f"length 40, seeds 0/1/2: RNN {[round(a, 3) for a in rnn]}; LSTM default init {lstm_default:.3f}; "
         f"LSTM forget-bias 3 {[round(a, 3) for a in lstm_fb]}")
    check("the plain RNN is unreliable over 40 steps (fails on some seeds)", True, min(rnn) < 0.75)
    check("an LSTM with default init (forget gate ~0.5) also forgets: 0.5^40 ~ 0", True, lstm_default < 0.6)
    check("forget-gate bias 3 (gate ~0.95 -> 'remember by default') solves it on every seed", True, min(lstm_fb) > 0.9)
    lstm = nn.LSTM(1, 32)
    check("LSTM parameters = 4 gates x (32*(1+32) + 2*32 biases)", 4 * (32 * (1 + 32) + 2 * 32), sum(p.numel() for p in lstm.parameters()))


# ---------------------------------------------------------------------------
# 4. autoencoder for anomaly detection
# ---------------------------------------------------------------------------
def autoencoder():
    section("4. Autoencoder: compress, reconstruct, flag what doesn't fit")
    X_tr, X_te, y_tr, y_te = digits()
    normal_tr = X_tr[y_tr != 8]                          # train on every digit EXCEPT 8
    torch.manual_seed(0)
    ae = nn.Sequential(nn.Linear(64, 32), nn.ReLU(), nn.Linear(32, 8),   # encoder -> 8-number bottleneck
                       nn.ReLU(), nn.Linear(8, 32), nn.ReLU(), nn.Linear(32, 64), nn.Sigmoid())
    opt = torch.optim.Adam(ae.parameters(), lr=3e-3)
    Xt = torch.tensor(normal_tr)
    for _ in range(300):
        for idx in torch.randperm(len(Xt)).split(128):
            opt.zero_grad()
            F.mse_loss(ae(Xt[idx]), Xt[idx]).backward()
            opt.step()
    with torch.no_grad():
        err = ((ae(torch.tensor(X_te)) - torch.tensor(X_te)) ** 2).mean(1).numpy()
    e_norm, e_anom = err[y_te != 8].mean(), err[y_te == 8].mean()
    show(f"reconstruction error: seen digits {e_norm:.4f}, never-seen digit 8 {e_anom:.4f}")
    check("an unseen kind of input reconstructs badly", True, e_anom > 1.5 * e_norm)
    from sklearn.metrics import roc_auc_score
    auc = roc_auc_score(y_te == 8, err)
    show(f"ROC-AUC of 'reconstruction error' as an anomaly score for 8s: {auc:.3f}")
    check("reconstruction error is a useful anomaly score", True, auc > 0.8)


# ---------------------------------------------------------------------------
# 5. variational autoencoder
# ---------------------------------------------------------------------------
class VAE(nn.Module):
    def __init__(self, z=4):
        super().__init__()
        self.enc = nn.Sequential(nn.Linear(64, 64), nn.ReLU())
        self.mu, self.logvar = nn.Linear(64, z), nn.Linear(64, z)
        self.dec = nn.Sequential(nn.Linear(z, 64), nn.ReLU(), nn.Linear(64, 64), nn.Sigmoid())

    def forward(self, x):
        h = self.enc(x)
        mu, logvar = self.mu(h), self.logvar(h)
        z = mu + torch.exp(0.5 * logvar) * torch.randn_like(mu)      # reparameterisation trick
        return self.dec(z), mu, logvar


def kl_to_standard_normal(mu, logvar):
    return (-0.5 * (1 + logvar - mu ** 2 - logvar.exp()).sum(1)).mean()


def vae():
    section("5. Variational autoencoder")
    mu, logvar = torch.tensor([[1.0, 0.0]]), torch.tensor([[0.0, math.log(4.0)]])
    # closed form vs Monte Carlo: KL(N(mu, s^2) || N(0,1)) = 0.5 (s^2 + mu^2 - 1 - ln s^2)
    closed = kl_to_standard_normal(mu, logvar).item()
    torch.manual_seed(0)
    z = mu + torch.exp(0.5 * logvar) * torch.randn(400_000, 2)
    log_q = torch.distributions.Normal(mu, torch.exp(0.5 * logvar)).log_prob(z).sum(1)
    log_p = torch.distributions.Normal(0.0, 1.0).log_prob(z).sum(1)
    near("KL term: closed form == Monte Carlo estimate", (log_q - log_p).mean().item(), closed, 0.02)
    near("KL for mu=(1,0), var=(1,4): 0.5*(1+1-1-0) + 0.5*(4-1-ln4)", 0.5 + 0.5 * (3 - math.log(4)), closed, 1e-6)

    X_tr, X_te, _, _ = digits()
    torch.manual_seed(0)
    model = VAE()
    opt = torch.optim.Adam(model.parameters(), lr=3e-3)
    Xt = torch.tensor(X_tr)
    for _ in range(150):
        for idx in torch.randperm(len(Xt)).split(128):
            opt.zero_grad()
            recon, m, lv = model(Xt[idx])
            loss = F.binary_cross_entropy(recon, Xt[idx], reduction="none").sum(1).mean() + kl_to_standard_normal(m, lv)
            loss.backward()
            opt.step()
    model.eval()
    with torch.no_grad():
        recon, m, lv = model(torch.tensor(X_te))
        kl = kl_to_standard_normal(m, lv).item()
        samples = model.dec(torch.randn(500, 4))
    show(f"test KL per image {kl:.2f} nats; generated samples: mean pixel {samples.mean():.3f} vs real {X_te.mean():.3f}")
    check("the KL term keeps codes close to N(0, I) so random z decode to plausible images", True, abs(samples.mean().item() - X_te.mean()) < 0.05)
    from sklearn.neighbors import KNeighborsClassifier
    X_all, _, y_all, _ = digits()
    knn = KNeighborsClassifier(5).fit(X_all, y_all)
    classes = set(knn.predict(samples.numpy()).tolist())
    show(f"a KNN digit classifier recognises the generated samples as {len(classes)} different digits")
    check("sampling the latent space produces many different digits", True, len(classes) >= 6)


# ---------------------------------------------------------------------------
# 6. GAN
# ---------------------------------------------------------------------------
def gan():
    section("6. GAN: generator vs discriminator")
    torch.manual_seed(0)
    real = lambda n: 4.0 + 1.25 * torch.randn(n, 1)      # noqa: E731  target distribution N(4, 1.25^2)
    G = nn.Sequential(nn.Linear(4, 32), nn.ReLU(), nn.Linear(32, 1))                  # 4-D noise in, 1 number out
    D = nn.Sequential(nn.Linear(1, 32), nn.LeakyReLU(0.2), nn.Linear(32, 1))
    og, od = torch.optim.Adam(G.parameters(), lr=1e-3, betas=(0.5, 0.999)), torch.optim.Adam(D.parameters(), lr=1e-3, betas=(0.5, 0.999))
    bce = nn.BCEWithLogitsLoss()
    before = G(torch.randn(5000, 4)).detach()
    for _ in range(3000):
        x_real, x_fake = real(128), G(torch.randn(128, 4))
        od.zero_grad()                                   # D: real -> 1, fake -> 0
        (bce(D(x_real), torch.ones(128, 1)) + bce(D(x_fake.detach()), torch.zeros(128, 1))).backward()
        od.step()
        og.zero_grad()                                   # G: make D say 1 for fakes ("non-saturating" loss)
        bce(D(G(torch.randn(128, 4))), torch.ones(128, 1)).backward()
        og.step()
    with torch.no_grad():
        out = G(torch.randn(20000, 4))
    show(f"generator output: before mean {before.mean():.2f} std {before.std():.2f}  ->  after mean {out.mean():.2f} std {out.std():.2f}  (target 4.00, 1.25)")
    near("the generator learns the target mean", 4.0, out.mean().item(), 0.3)
    near("...and roughly the spread", 1.25, out.std().item(), 0.35)


# ---------------------------------------------------------------------------
# 7. graph neural network
# ---------------------------------------------------------------------------
def gnn():
    section("7. GNN: graph convolution from scratch")
    rng = np.random.default_rng(0)
    n = 40
    community = np.r_[np.zeros(20, int), np.ones(20, int)]
    A = np.zeros((n, n))
    for i in range(n):
        for j in range(i + 1, n):
            p = 0.3 if community[i] == community[j] else 0.02          # dense inside, sparse between
            if rng.random() < p:
                A[i, j] = A[j, i] = 1
    A_hat = A + np.eye(n)                                               # add self-loops
    d = A_hat.sum(1)
    A_norm = A_hat / np.sqrt(d)[:, None] / np.sqrt(d)[None, :]          # D^-1/2 (A+I) D^-1/2
    check("normalised adjacency is symmetric", True, np.allclose(A_norm, A_norm.T))
    # one GCN layer = average your neighbours' features, then a linear map: H' = relu(A_norm H W)
    X = torch.eye(n)                                                    # no features: just node identity
    An = torch.tensor(A_norm, dtype=torch.float32)
    labelled = [0, 20]                                                  # ONE labelled node per community
    torch.manual_seed(0)
    W1, W2 = nn.Linear(n, 16), nn.Linear(16, 2)
    opt = torch.optim.Adam(list(W1.parameters()) + list(W2.parameters()), lr=0.05, weight_decay=5e-4)
    y = torch.tensor(community)
    for _ in range(100):
        opt.zero_grad()
        logits = An @ W2(F.relu(An @ W1(X)))                            # 2 rounds of message passing
        F.cross_entropy(logits[labelled], y[labelled]).backward()
        opt.step()
    pred = (An @ W2(F.relu(An @ W1(X)))).argmax(1).numpy()
    acc = (pred == community).mean()
    show(f"semi-supervised node classification with 2 labels out of 40 nodes: accuracy {acc:.3f}")
    check("message passing spreads the 2 labels through the graph", True, acc > 0.85)
    mlp_logits = W2(F.relu(W1(X)))                                      # same weights, NO graph
    check("without the graph, 2 labels can't generalise", True, (mlp_logits.argmax(1).numpy() == community).mean() < acc)


# ---------------------------------------------------------------------------
# 8. diffusion
# ---------------------------------------------------------------------------
def diffusion():
    section("8. Diffusion: learn to remove noise, then generate from pure noise")
    T = 50
    betas = torch.linspace(1e-4, 0.2, T)
    alphas = 1 - betas
    abar = torch.cumprod(alphas, 0)                                     # signal kept after t steps
    check("forward process destroys the signal: abar_T ~ 0", True, abar[-1].item() < 0.01)
    torch.manual_seed(0)
    data = lambda n: torch.where(torch.rand(n, 1) < 0.5, -2.0, 2.0) + 0.3 * torch.randn(n, 1)  # noqa: E731  two bumps
    x0 = data(200_000)
    t = 20
    xt = abar[t].sqrt() * x0 + (1 - abar[t]).sqrt() * torch.randn_like(x0)  # jump to step t in one shot
    near("closed-form q(x_t|x_0): Var(x_t) = abar*Var(x0) + (1-abar)", (abar[t] * x0.var() + 1 - abar[t]).item(), xt.var().item(), 0.02)

    net = nn.Sequential(nn.Linear(2, 64), nn.SiLU(), nn.Linear(64, 64), nn.SiLU(), nn.Linear(64, 1))  # predicts the noise
    opt = torch.optim.Adam(net.parameters(), lr=2e-3)
    for _ in range(3000):
        x0 = data(256)
        tt = torch.randint(0, T, (256,))
        eps = torch.randn_like(x0)
        xt = abar[tt].sqrt()[:, None] * x0 + (1 - abar[tt]).sqrt()[:, None] * eps
        opt.zero_grad()
        F.mse_loss(net(torch.cat([xt, tt[:, None] / T], 1)), eps).backward()   # loss: ||eps - eps_theta(x_t, t)||^2
        opt.step()
    with torch.no_grad():
        x = torch.randn(4000, 1)                                       # start from pure noise
        for step in reversed(range(T)):
            eps_hat = net(torch.cat([x, torch.full_like(x, step / T)], 1))
            x = (x - betas[step] / (1 - abar[step]).sqrt() * eps_hat) / alphas[step].sqrt()
            if step > 0:
                x = x + betas[step].sqrt() * torch.randn_like(x)
    left, right = (x < 0).float().mean().item(), (x > 0).float().mean().item()
    near_mode = ((x.abs() - 2).abs() < 0.8).float().mean().item()
    show(f"generated samples: {left:.0%} near the left bump, {right:.0%} right; {near_mode:.0%} within 0.8 of a mode")
    near("both modes are generated (~50/50)", 0.5, left, 0.1)
    check("samples land on the data, not in the empty middle", True, near_mode > 0.85)


if __name__ == "__main__":
    convolution()
    cnn_vs_mlp()
    rnn_vs_lstm()
    autoencoder()
    vae()
    gan()
    gnn()
    diffusion()
    summary()
