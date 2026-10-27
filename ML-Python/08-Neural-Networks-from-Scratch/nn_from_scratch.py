"""
ML 08 : NEURAL NETWORKS FROM SCRATCH (NumPy only)
Run:  ../.venv/bin/python nn_from_scratch.py        (explanations: README.md)

  1. A neuron and activations      sigmoid, tanh, ReLU, leaky ReLU, GELU + derivatives
  2. Losses                        MSE, binary/categorical cross-entropy, softmax stability
  3. An MLP with backprop          Dense + ReLU + softmax-CE layers, forward and backward
  4. Gradient check                analytic gradients == numerical gradients
  5. Training on spirals           a non-linear problem a linear model can't solve
  6. Initialisation                zeros (symmetry), too large, He; vanishing gradients
  7. Optimisers                    SGD, momentum, RMSProp, Adam, AdamW
  8. Regularisation                L2 / weight decay, dropout (inverted)
  9. Batch normalisation           forward pass in training vs inference
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


# ---------------------------------------------------------------------------
# 1. activations
# ---------------------------------------------------------------------------
def sigmoid(z): return 1 / (1 + np.exp(-z))
def relu(z): return np.maximum(0, z)
def gelu(z): return 0.5 * z * (1 + np.tanh(np.sqrt(2 / np.pi) * (z + 0.044715 * z ** 3)))


def activations():
    section("1. A neuron and its activation functions")
    x, w, b = np.array([1.0, 2.0]), np.array([0.5, -0.25]), 0.1
    z = w @ x + b
    check("a neuron: z = w.x + b = 0.5 - 0.5 + 0.1", 0.1, round(z, 12))
    check("ReLU(-3), ReLU(2)", [0.0, 2.0], relu(np.array([-3.0, 2.0])).tolist())
    check("leaky ReLU keeps a small slope for negatives", -0.03, np.where(-3.0 > 0, -3.0, 0.01 * -3.0))
    near("tanh is zero-centred: tanh(0.5)", 0.4621, np.tanh(0.5), 1e-4)
    near("GELU(1) (used in transformers) ~ 0.841", 0.8412, gelu(1.0), 1e-3)
    near("max derivative of sigmoid is 0.25 (at z=0)", 0.25, sigmoid(0) * (1 - sigmoid(0)), 1e-12)
    check("sigmoid saturates: derivative at z=10 is ~0", True, sigmoid(10) * (1 - sigmoid(10)) < 1e-4)
    check("ReLU's derivative is exactly 1 for z>0 (no shrinking)", [0.0, 1.0], (np.array([-2.0, 3.0]) > 0).astype(float).tolist())
    # without a non-linearity, stacked layers collapse into one linear layer
    rng = np.random.default_rng(0)
    W1, W2, X = rng.normal(size=(3, 4)), rng.normal(size=(4, 2)), rng.normal(size=(5, 3))
    check("two linear layers == one linear layer (W1 @ W2)", True, np.allclose((X @ W1) @ W2, X @ (W1 @ W2)))


# ---------------------------------------------------------------------------
# 2. losses and a numerically stable softmax
# ---------------------------------------------------------------------------
def softmax(z):
    z = z - z.max(axis=1, keepdims=True)              # subtract the max: same result, no overflow
    e = np.exp(z)
    return e / e.sum(axis=1, keepdims=True)


def cross_entropy(probs, y):
    return float(-np.mean(np.log(probs[np.arange(len(y)), y] + 1e-12)))


def losses():
    section("2. Losses and a stable softmax")
    logits = np.array([[2.0, 1.0, 0.1]])
    p = softmax(logits)
    near("softmax([2,1,0.1]) -> P(class 0)", 0.6590, p[0, 0], 1e-4)
    check("softmax rows sum to 1", 1.0, round(p.sum(), 12))
    big = np.array([[1000.0, 1001.0]])
    with np.errstate(over="ignore", invalid="ignore"):
        naive = np.exp(big) / np.exp(big).sum()
    check("naive softmax overflows on logits of 1000 -> nan", True, bool(np.isnan(naive).any()))
    near("stable softmax handles it: P(second) = sigmoid(1)", sigmoid(1.0), softmax(big)[0, 1], 1e-12)
    near("cross-entropy of the right class at p=0.659", -np.log(0.6590), cross_entropy(p, np.array([0])), 1e-4)
    near("cross-entropy of a uniform guess over 10 classes = ln 10", np.log(10), cross_entropy(np.full((1, 10), 0.1), np.array([3])), 1e-9)
    # the famous gradient: d(CE)/d(logits) = softmax - onehot
    y = np.array([0])
    grad = p.copy()
    grad[0, y] -= 1
    eps, num = 1e-6, np.zeros(3)
    for j in range(3):
        lp, lm = logits.copy(), logits.copy()
        lp[0, j] += eps
        lm[0, j] -= eps
        num[j] = (cross_entropy(softmax(lp), y) - cross_entropy(softmax(lm), y)) / (2 * eps)
    check("d(cross-entropy)/d(logits) = softmax - one_hot (checked numerically)", True, np.allclose(grad[0], num, atol=1e-6))


# ---------------------------------------------------------------------------
# 3. an MLP with manual backprop
# ---------------------------------------------------------------------------
class Dense:
    def __init__(self, n_in, n_out, rng, init="he"):
        scale = {"he": np.sqrt(2 / n_in), "xavier": np.sqrt(1 / n_in), "zeros": 0.0, "large": 1.0, "small": 0.01}[init]
        self.W = rng.normal(size=(n_in, n_out)) * scale
        self.b = np.zeros(n_out)

    def forward(self, x):
        self.x = x                                      # cache the input: backward needs it
        return x @ self.W + self.b

    def backward(self, grad_out):
        self.dW = self.x.T @ grad_out                   # dL/dW = x^T . dL/dout
        self.db = grad_out.sum(axis=0)
        return grad_out @ self.W.T                      # dL/dx, passed to the previous layer

    def params(self):
        return [(self.W, "dW"), (self.b, "db")]


class ReLU:
    def forward(self, x):
        self.mask = x > 0
        return x * self.mask

    def backward(self, grad_out):
        return grad_out * self.mask                     # gradient flows only where the input was positive

    def params(self):
        return []


class Tanh:
    def forward(self, x):
        self.out = np.tanh(x)
        return self.out

    def backward(self, grad_out):
        return grad_out * (1 - self.out ** 2)

    def params(self):
        return []


class Sigmoid:
    def forward(self, x):
        self.out = sigmoid(x)
        return self.out

    def backward(self, grad_out):
        return grad_out * self.out * (1 - self.out)

    def params(self):
        return []


class Dropout:
    def __init__(self, p, rng):
        self.p, self.rng, self.training = p, rng, True

    def forward(self, x):
        if not self.training or self.p == 0:
            return x
        self.mask = (self.rng.random(x.shape) >= self.p) / (1 - self.p)   # inverted dropout: rescale now
        return x * self.mask

    def backward(self, grad_out):
        return grad_out * self.mask if self.training and self.p > 0 else grad_out

    def params(self):
        return []


class MLP:
    def __init__(self, sizes, rng, act=ReLU, init="he", dropout=0.0):
        self.layers = []
        for i in range(len(sizes) - 1):
            self.layers.append(Dense(sizes[i], sizes[i + 1], rng, init))
            if i < len(sizes) - 2:
                self.layers.append(act())
                if dropout:
                    self.layers.append(Dropout(dropout, rng))

    def forward(self, x):
        for layer in self.layers:
            x = layer.forward(x)
        return x                                        # logits

    def loss_and_backward(self, x, y, l2=0.0):
        logits = self.forward(x)
        probs = softmax(logits)
        loss = cross_entropy(probs, y)
        grad = probs.copy()
        grad[np.arange(len(y)), y] -= 1
        grad /= len(y)                                  # mean over the batch
        for layer in reversed(self.layers):             # backprop = chain rule, last layer first
            grad = layer.backward(grad)
        if l2:
            for layer in self.layers:
                if isinstance(layer, Dense):
                    loss += 0.5 * l2 * float(np.sum(layer.W ** 2))
                    layer.dW += l2 * layer.W
        return loss

    def param_grads(self):
        return [(p, getattr(layer, g)) for layer in self.layers for p, g in layer.params()]

    def set_training(self, flag):
        for layer in self.layers:
            if isinstance(layer, Dropout):
                layer.training = flag

    def predict(self, x):
        self.set_training(False)
        out = np.argmax(self.forward(x), axis=1)
        self.set_training(True)
        return out


def gradient_check():
    section("3-4. MLP forward/backward + gradient check")
    rng = np.random.default_rng(1)
    net = MLP([4, 6, 5, 3], rng, act=Tanh, init="xavier")
    X, y = rng.normal(size=(8, 4)), rng.integers(0, 3, 8)
    net.loss_and_backward(X, y)
    worst = 0.0
    for p, g in net.param_grads():
        num = np.zeros_like(p)
        it = np.nditer(p, flags=["multi_index"])
        for _ in it:
            i = it.multi_index
            old = p[i]
            p[i] = old + 1e-5
            lp = cross_entropy(softmax(net.forward(X)), y)
            p[i] = old - 1e-5
            lm = cross_entropy(softmax(net.forward(X)), y)
            p[i] = old
            num[i] = (lp - lm) / 2e-5
        rel = np.linalg.norm(g - num) / (np.linalg.norm(g) + np.linalg.norm(num))
        worst = max(worst, rel)
    show(f"worst relative error between backprop and numerical gradients: {worst:.2e}")
    check("backprop is correct (relative error < 1e-7)", True, worst < 1e-7)
    n_params = sum(p.size for p, _ in net.param_grads())
    check("parameter count: (4*6+6) + (6*5+5) + (5*3+3)", 83, n_params)


# ---------------------------------------------------------------------------
# 5. training on spirals
# ---------------------------------------------------------------------------
def spirals(n_per_class=200, k=3, seed=0):
    rng = np.random.default_rng(seed)
    X, y = [], []
    for c in range(k):
        r = np.linspace(0.05, 1, n_per_class)
        t = np.linspace(c * 4, (c + 1) * 4, n_per_class) + rng.normal(scale=0.2, size=n_per_class)
        X.append(np.column_stack([r * np.sin(t), r * np.cos(t)]))
        y.append(np.full(n_per_class, c))
    return np.vstack(X), np.concatenate(y)


class Adam:
    def __init__(self, lr=0.01, b1=0.9, b2=0.999, eps=1e-8, weight_decay=0.0):
        self.lr, self.b1, self.b2, self.eps, self.wd, self.t, self.m, self.v = lr, b1, b2, eps, weight_decay, 0, {}, {}

    def step(self, pairs):
        self.t += 1
        for p, g in pairs:
            k = id(p)
            self.m[k] = self.b1 * self.m.get(k, 0) + (1 - self.b1) * g          # 1st moment: momentum
            self.v[k] = self.b2 * self.v.get(k, 0) + (1 - self.b2) * g * g      # 2nd moment: per-parameter scale
            m_hat = self.m[k] / (1 - self.b1 ** self.t)                         # bias correction for early steps
            v_hat = self.v[k] / (1 - self.b2 ** self.t)
            p -= self.lr * m_hat / (np.sqrt(v_hat) + self.eps)
            if self.wd:
                p -= self.lr * self.wd * p                                      # AdamW: decoupled weight decay


def train(net, X, y, epochs=300, batch=64, opt=None, rng=None, l2=0.0):
    opt = opt or Adam(0.01)
    rng = rng or np.random.default_rng(0)
    history = []
    for _ in range(epochs):
        order = rng.permutation(len(X))
        for s in range(0, len(X), batch):
            idx = order[s:s + batch]
            net.loss_and_backward(X[idx], y[idx], l2=l2)
            opt.step(net.param_grads())
        net.set_training(False)
        history.append(cross_entropy(softmax(net.forward(X)), y))
        net.set_training(True)
    return history


def training():
    section("5. Training an MLP on spirals")
    X, y = spirals()
    from sklearn.linear_model import LogisticRegression
    lin = LogisticRegression(max_iter=1000).fit(X, y).score(X, y)
    net = MLP([2, 64, 64, 3], np.random.default_rng(0))
    hist = train(net, X, y, epochs=300)
    acc = np.mean(net.predict(X) == y)
    show(f"accuracy: logistic regression {lin:.3f}, MLP 2-64-64-3 {acc:.3f}; loss {hist[0]:.3f} -> {hist[-1]:.3f}")
    check("a linear model can't separate spirals", True, lin < 0.6)
    check("the MLP learns the spirals", True, acc > 0.95)
    Xv, yv = spirals(seed=1)
    check("...and generalises to a fresh sample of spirals", True, np.mean(net.predict(Xv) == yv) > 0.9)


# ---------------------------------------------------------------------------
# 6. initialisation and vanishing gradients
# ---------------------------------------------------------------------------
def initialisation():
    section("6. Initialisation and vanishing gradients")
    X, y = spirals()
    zero = MLP([2, 32, 3], np.random.default_rng(0), init="zeros")
    train(zero, X, y, epochs=30)
    W = zero.layers[0].W
    check("zero init: every hidden unit stays identical (symmetry never breaks)", True, np.allclose(W, W[:, :1]))

    rng = np.random.default_rng(0)
    x = rng.normal(size=(1000, 256))
    stds = {}
    for init in ("small", "he", "large"):
        h = x
        r = np.random.default_rng(1)
        for _ in range(10):
            h = relu(h @ (r.normal(size=(256, 256)) * {"small": 0.01, "he": np.sqrt(2 / 256), "large": 1.0}[init]))
        stds[init] = float(h.std())
    show("activation std after 10 ReLU layers: " + ", ".join(f"{k} {v:.3g}" for k, v in stds.items()))
    check("init too small -> signal vanishes", True, stds["small"] < 1e-6)
    check("init too large -> signal explodes", True, stds["large"] > 1e10)
    check("He init (std sqrt(2/fan_in)) keeps it ~O(1)", True, 0.1 < stds["he"] < 10)

    # vanishing gradients: gradient reaching the FIRST layer of a 10-layer net
    grads = {}
    for act in (Sigmoid, ReLU):
        net = MLP([2] + [32] * 10 + [3], np.random.default_rng(2), act=act, init="xavier" if act is Sigmoid else "he")
        net.loss_and_backward(X[:64], y[:64])
        dense = [layer for layer in net.layers if isinstance(layer, Dense)]
        grads[act.__name__] = (np.abs(dense[0].dW).mean(), np.abs(dense[-1].dW).mean())
    for name, (first, last) in grads.items():
        show(f"{name:<8} 10 hidden layers: mean |grad| first layer {first:.2e}, last layer {last:.2e}, ratio {first / last:.1e}")
    check("sigmoid: the first layer gets a vanishing gradient (< 1/1000 of the last)", True, grads["Sigmoid"][0] / grads["Sigmoid"][1] < 1e-3)
    check("ReLU + He init: gradients reach the first layer", True, grads["ReLU"][0] / grads["ReLU"][1] > 0.05)


# ---------------------------------------------------------------------------
# 7. optimisers on a ravine
# ---------------------------------------------------------------------------
def optimisers():
    section("7. Optimisers")
    grad_f = lambda w: np.array([2 * w[0], 200 * w[1]])     # noqa: E731  f = w0^2 + 100 w1^2 : a narrow ravine
    f = lambda w: w[0] ** 2 + 100 * w[1] ** 2               # noqa: E731
    results = {}

    w = np.array([10.0, 1.0])
    for _ in range(100):
        w = w - 0.009 * grad_f(w)                          # lr limited by the steep direction (< 2/200)
    results["SGD"] = f(w)

    w, v = np.array([10.0, 1.0]), np.zeros(2)
    for _ in range(100):
        v = 0.9 * v + grad_f(w)
        w = w - 0.009 * v
    results["momentum"] = f(w)

    w, s = np.array([10.0, 1.0]), np.zeros(2)
    for _ in range(100):
        g = grad_f(w)
        s = 0.9 * s + 0.1 * g * g
        w = w - 0.1 * g / (np.sqrt(s) + 1e-8)
    results["RMSProp"] = f(w)

    w = np.array([10.0, 1.0])
    adam = Adam(lr=0.5)
    for _ in range(100):
        adam.step([(w, grad_f(w))])
    results["Adam"] = f(w)
    show("loss after 100 steps on f = w0^2 + 100 w1^2 (start 101): " + ", ".join(f"{k} {v:.2e}" for k, v in results.items()))
    check("plain SGD crawls along the flat direction", True, results["SGD"] > 1.0)
    check("momentum builds speed along the ravine", True, results["momentum"] < results["SGD"] / 100)
    check("adaptive methods (RMSProp, Adam) rescale each direction", True, max(results["RMSProp"], results["Adam"]) < results["SGD"])

    # Adam + L2 vs AdamW. Weight 0 gets no task gradient ("quiet"); weight 1 gets large noisy gradients.
    # With Adam, an L2 term is added to the gradient and then divided by sqrt(v): quiet weights are decayed hard,
    # noisy ones hardly at all. AdamW applies the decay directly to the weights: every weight shrinks equally.
    w_l2, w_dw = np.array([5.0, 5.0]), np.array([5.0, 5.0])
    a1, a2 = Adam(lr=0.01), Adam(lr=0.01, weight_decay=0.1)
    noise = np.random.default_rng(5).choice([-1.0, 1.0], size=500)
    for t in range(500):
        task_grad = np.array([0.0, 50.0 * noise[t]])
        a1.step([(w_l2, task_grad + 0.1 * w_l2)])        # L2 folded into the gradient
        a2.step([(w_dw, task_grad)])                     # decoupled weight decay
    show(f"after 500 steps (start 5.0): quiet weight Adam+L2 {w_l2[0]:.2f} vs AdamW {w_dw[0]:.2f}; "
         f"noisy weight Adam+L2 {w_l2[1]:.2f} vs AdamW {w_dw[1]:.2f}")
    near("AdamW shrinks every weight by the same factor (1 - lr*wd)^500", 5.0 * (1 - 0.01 * 0.1) ** 500, w_dw[0], 0.05)
    check("Adam+L2 decays the quiet weight much more than the noisy one", True, w_l2[0] < 0.5 * abs(w_l2[1]))


# ---------------------------------------------------------------------------
# 8-9. regularisation and batch norm
# ---------------------------------------------------------------------------
def regularisation():
    section("8. Regularisation: L2 and dropout")
    rng = np.random.default_rng(3)
    X, y = spirals(60, seed=3)
    X = X + 0.15 * rng.normal(size=X.shape)                # few, noisy points -> easy to overfit
    Xv, yv = spirals(300, seed=4)
    res = {}
    for name, kw, l2 in (("none", {}, 0.0), ("L2 1e-3", {}, 1e-3), ("dropout 0.3", {"dropout": 0.3}, 0.0)):
        net = MLP([2, 256, 256, 3], np.random.default_rng(0), **kw)
        train(net, X, y, epochs=400, batch=32, l2=l2)
        res[name] = (np.mean(net.predict(X) == y), np.mean(net.predict(Xv) == yv))
    show("train / validation accuracy: " + ", ".join(f"{k} {a:.3f}/{b:.3f}" for k, (a, b) in res.items()))
    check("without regularisation: big train-validation gap (overfitting)", True, res["none"][0] - res["none"][1] > 0.08)
    check("L2 and dropout both narrow the train-validation gap", True,
          all(res[k][0] - res[k][1] < res["none"][0] - res["none"][1] for k in ("L2 1e-3", "dropout 0.3")))
    d = Dropout(0.5, np.random.default_rng(0))
    out = d.forward(np.ones((1000, 100)))
    near("inverted dropout keeps the expected activation the same (1.0)", 1.0, out.mean(), 0.01)
    check("about half the units are zeroed with p=0.5", True, 0.48 < np.mean(out == 0) < 0.52)
    d.training = False
    check("at inference dropout does nothing", True, np.array_equal(d.forward(np.ones((2, 2))), np.ones((2, 2))))

    section("9. Batch normalisation (forward)")
    x = rng.normal(5.0, 3.0, size=(256, 4))
    gamma, beta = np.ones(4), np.zeros(4)
    mu, var = x.mean(axis=0), x.var(axis=0)
    out = gamma * (x - mu) / np.sqrt(var + 1e-5) + beta
    check("training: normalise with the BATCH mean/var -> mean 0, std 1", True, np.allclose(out.mean(0), 0, atol=1e-7) and np.allclose(out.std(0), 1, atol=1e-3))
    running_mu = 0.0
    for _ in range(200):
        running_mu = 0.9 * running_mu + 0.1 * rng.normal(5.0, 3.0, size=(256, 4)).mean(axis=0)
    near("inference uses RUNNING averages collected during training (-> 5)", 5.0, running_mu.mean(), 0.1)


if __name__ == "__main__":
    activations()
    losses()
    gradient_check()
    training()
    initialisation()
    optimisers()
    regularisation()
    summary()
