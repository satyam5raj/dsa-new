# 08 · Neural networks from scratch (NumPy only)

> Before using PyTorch, build a neural network with nothing but NumPy: forward pass, loss,
> backpropagation, gradient check, optimisers, regularisation. After this, `loss.backward()` and
> `optimizer.step()` will never feel like magic again.

Run it: `../.venv/bin/python nn_from_scratch.py` → **39/39 checks**.

---

## 1. A neuron and activation functions

A neuron computes a weighted sum and passes it through a non-linearity:

```
z = w·x + b          a = activation(z)
x = [1, 2], w = [0.5, −0.25], b = 0.1   →   z = 0.1
```

A **layer** is many neurons at once: `A = act(X @ W + b)`. A **network** stacks layers.

**Why non-linearity is essential:** without it, `(X @ W1) @ W2 = X @ (W1 @ W2)`. Any stack of
linear layers collapses into **one** linear layer (checked). The activation is what lets deep nets
bend.

| Activation | Formula | Range | Derivative | Notes |
|---|---|---|---|---|
| sigmoid | `1/(1+e⁻ᶻ)` | (0, 1) | `σ(1−σ)`, **max 0.25** | saturates (derivative at z = 10 is < 1e-4); use only for output probabilities and gates |
| tanh | `(eᶻ−e⁻ᶻ)/(eᶻ+e⁻ᶻ)` | (−1, 1) | `1 − tanh²` | zero-centred (`tanh(0.5) = 0.462`); still saturates; used in RNN/LSTM |
| **ReLU** | `max(0, z)` | [0, ∞) | **exactly 1** for z > 0, else 0 | default for hidden layers; cheap; doesn't shrink gradients. Risk: "dead" units stuck at 0 |
| leaky ReLU | `z` if z > 0 else `0.01z` | ℝ | 1 or 0.01 | fixes dead units (`−3 → −0.03`) |
| **GELU** | `z·Φ(z)` (smooth ReLU) | ≈ [−0.17, ∞) | smooth | transformers (BERT, GPT); `GELU(1) = 0.841` |
| softmax | `eᶻⁱ / Σ eᶻʲ` | probabilities | see §2 | multiclass output layer |

## 2. Losses and a numerically stable softmax

| Task | Output layer | Loss |
|---|---|---|
| regression | linear | MSE (or MAE / Huber) |
| binary classification | 1 unit + sigmoid | binary cross-entropy |
| multiclass | k units + softmax | categorical cross-entropy |
| multi-label | k units + sigmoid each | binary cross-entropy per label |

`softmax([2, 1, 0.1])` gives `P(class 0) = 0.659`; the cross-entropy is `−ln 0.659 = 0.417`.
A uniform guess over 10 classes has loss `ln 10 = 2.303`. **Remember that number:** a fresh
10-class network should start near 2.3. If it doesn't, something is wrong.

**Stability trick:** `exp(1000)` overflows, so a naive softmax returns `nan`. Subtracting the row
max first gives the same probabilities with no overflow:

```python
z = z - z.max(axis=1, keepdims=True)
```

That's why frameworks give you `CrossEntropyLoss` on **logits**, not on probabilities.

**The gradient everyone should know:** `∂CE/∂logits = softmax(z) − one_hot(y)` (checked
numerically). It has the same shape as linear and logistic regression: *prediction − target*.

## 3. Backpropagation: the chain rule, layer by layer

Each layer needs two methods:

- **forward**: compute the output and **cache** what backward will need;
- **backward**: given `∂L/∂output`, return `∂L/∂input` and store the parameter gradients.

```python
class Dense:
    def forward(self, x):
        self.x = x
        return x @ self.W + self.b
    def backward(self, grad_out):
        self.dW = self.x.T @ grad_out          # (n_in, batch) @ (batch, n_out)
        self.db = grad_out.sum(axis=0)
        return grad_out @ self.W.T              # to the previous layer

class ReLU:
    def forward(self, x):
        self.mask = x > 0
        return x * self.mask
    def backward(self, g):
        return g * self.mask                    # gradient passes only where input > 0
```

Training step:

```
logits = forward(X)
grad   = (softmax(logits) − one_hot(y)) / batch_size
for layer in reversed(layers): grad = layer.backward(grad)
optimizer.step()
```

**Shape rule:** `dW` always has the same shape as `W`. If your shapes don't match, transpose
until they do, then verify with a gradient check.

## 4. Gradient check: never trust a hand-written backward pass

For each parameter, nudge it by ±1e-5 and compare the numerical slope with backprop:

```
relative error = ‖g_backprop − g_numeric‖ / (‖g_backprop‖ + ‖g_numeric‖)
```

On a 4-6-5-3 tanh network (**83 parameters**: `(4·6+6) + (6·5+5) + (5·3+3)`), the worst relative
error was **9.4e-11**. Rule of thumb: < 1e-7 is great, 1e-5 is suspicious, > 1e-3 is a bug. (ReLU's
kink at 0 can cause small mismatches, which is why the check uses tanh.)

## 5. Training on spirals

Three interleaved spirals are impossible for a straight-line classifier:

| Model | Accuracy |
|---|---|
| logistic regression | **0.523** |
| MLP 2 → 64 → 64 → 3 (ReLU, Adam, 300 epochs) | **1.000** on train, > 0.9 on a fresh sample |

The loss went from 0.659 to 0.000. The hidden layers learn a new representation in which the
spirals **become** linearly separable for the final layer. That's what "representation learning"
means.

## 6. Initialisation and vanishing / exploding gradients

**Zero init:** every hidden unit computes the same thing and gets the same gradient, so they stay
identical forever (checked after 30 epochs). Random init **breaks the symmetry**.

**Scale matters:** activation std after 10 ReLU layers of width 256:

| Weight std | Activation std |
|---|---|
| 0.01 (too small) | **2.6e-10**: the signal vanishes |
| **He: `√(2/fan_in)`** | **0.747**: stays O(1) |
| 1.0 (too large) | **2.6e+10**: explodes |

Each layer multiplies the signal's scale by about `std · √fan_in` (times `√½` for ReLU, which
zeroes half the units). He init sets that factor to exactly 1. Use **Xavier/Glorot**
(`√(1/fan_in)` or `√(2/(fan_in+fan_out))`) for tanh/sigmoid and **He** for ReLU.

**Vanishing gradients, measured** (10 hidden layers, mean |gradient|):

| Network | First layer | Last layer | Ratio |
|---|---|---|---|
| sigmoid + Xavier | 7.3e-09 | 0.197 | **3.7e-08** |
| ReLU + He | 0.0148 | 0.0192 | **0.77** |

Each sigmoid layer multiplies the gradient by at most 0.25, and 0.25¹⁰ ≈ 1e-6, so the early layers
of a deep sigmoid net never learn. The fixes, in historical order:

1. ReLU;
2. good initialisation;
3. **batch/layer normalisation**;
4. **residual connections** (`x + f(x)` gives the gradient a highway);
5. for exploding gradients, **gradient clipping**.

## 7. Optimisers

The test bed is a narrow ravine, `f = w₀² + 100 w₁²`, starting at 101. One direction is 100 times
steeper than the other.

| Optimiser | Update | Loss after 100 steps |
|---|---|---|
| SGD | `w −= η g` | **2.64**: η is capped by the steep direction, so it crawls along the flat one |
| Momentum | `v = 0.9v + g;  w −= η v` | **0.003**: velocity builds up along the consistent direction |
| RMSProp | `s = 0.9s + 0.1g²;  w −= η g/√s` | 0.82: divides each direction by its own gradient size |
| **Adam** | momentum + RMSProp + bias correction | **0.004** |

```python
m = b1*m + (1-b1)*g          # 1st moment: momentum (b1 = 0.9)
v = b2*v + (1-b2)*g*g        # 2nd moment: per-parameter scale (b2 = 0.999)
m_hat, v_hat = m/(1-b1**t), v/(1-b2**t)   # bias correction: m and v start at 0
w -= lr * m_hat / (sqrt(v_hat) + eps)
```

### Adam + L2 vs AdamW (a favourite interview question)

The setup: two weights start at 5.0. The "quiet" one gets no task gradient; the "noisy" one gets
gradients of ±50.

| After 500 steps | Quiet weight | Noisy weight |
|---|---|---|
| Adam + L2 (`g + λw`) | **1.30** | 5.27 |
| **AdamW** (`w −= η λ w` separately) | **3.03** | 3.32 |

With Adam, the L2 term is added to the gradient and then **divided by `√v`**. Weights with large
gradients get their decay divided away; quiet weights get decayed hard. Regularisation ends up
depending on gradient history, which isn't what L2 is meant to do. **AdamW** decouples the decay
from the gradient, so every weight shrinks by the same `(1 − ηλ)` per step: `5 · 0.999⁵⁰⁰ = 3.03`.
AdamW is the default for transformers.

**Learning-rate schedules** (chapter 09 runs them):

- **warm-up** (start small while Adam's statistics are unreliable);
- then **cosine** or **step** decay;
- one-cycle.

Typical settings: Adam/AdamW `lr = 1e-3` for small nets, `1e-4` to `3e-4` for transformers;
SGD + momentum, often with better final accuracy for CNNs.

## 8. Regularisation

60 noisy points per class and a big 2-256-256-3 network (train / validation accuracy):

| | Train | Validation | Gap |
|---|---|---|---|
| none | 0.967 | 0.862 | **0.105** (overfitting) |
| L2 1e-3 | 0.856 | **0.960** | negative |
| dropout 0.3 | 0.911 | 0.937 | 0.026 |

**Dropout:** during training, zero each unit with probability p. The network can't rely on any
single unit, so it learns redundant features (like training an ensemble of thinned networks).

**Inverted dropout** divides the survivors by `1 − p` during training: the mean activation stays
**1.0** (measured 0.998), about half the units are zero at p = 0.5, and at inference dropout simply
does nothing. Forgetting `model.eval()` in PyTorch leaves dropout on at inference, a classic bug.

Other regularisers:

- early stopping;
- data augmentation (chapter 10);
- weight decay;
- label smoothing;
- smaller models;
- more data (the best one).

## 9. Batch normalisation

For each feature, normalise over the **batch**, then let the network rescale:

```
x̂ = (x − μ_batch) / √(σ²_batch + ε)        y = γ x̂ + β      (γ, β learned)
```

- **Training:** batch statistics; the output has mean 0 and std 1 (checked).
- **Inference:** **running averages** collected during training (they converged to the true
  mean 5.0), so a single example gives a deterministic output.
- Effects: allows higher learning rates, eases initialisation, adds slight regularisation.
- **LayerNorm** normalises over the features of **each example** instead. It doesn't depend on
  batch size or other examples, so transformers and RNNs use it.

---

## Exercises

1. A layer has `W` of shape (128, 64) and a batch of 32. What are the shapes of `x`, `grad_out`,
   `dW` and the returned gradient?
2. Your 10-class network's first loss is 23.0 instead of ~2.3. Name two likely causes.
3. Why does ReLU help with vanishing gradients but not fully solve training very deep nets?
4. You get validation accuracy 0.3 while training is 0.99, and you used dropout. What's the
   first thing to check?

<details><summary>Answers</summary>

1. `x`: (32, 128); `grad_out`: (32, 64); `dW = x.T @ grad_out`: (128, 64); returned
   `grad_out @ W.T`: (32, 128).
2. Weights initialised too large (huge logits, hence confident wrong predictions), or the loss is
   applied to the wrong inputs (e.g. softmax applied twice, or unnormalised pixels in 0–255).
   Also possible: wrong labels or a sum instead of a mean in the loss.
3. ReLU's derivative is 1 for active units, but products of many weight matrices can still shrink
   or blow up, and units can die. Deep nets also need normalisation and residual connections.
4. The obvious overfitting aside, check that you switch to **eval mode** consistently. Also check
   for train/validation preprocessing differences, a label or ordering bug in the validation set,
   or leakage that inflated training.
</details>

## Interview questions

**Q: Explain backpropagation.** It's the chain rule applied from the loss backwards. Each layer
receives `∂L/∂output`, computes gradients for its parameters, and passes `∂L/∂input` to the layer
before. It reuses the forward pass's cached values, so a full gradient costs about the same as a
forward pass (reverse-mode automatic differentiation).

**Q: Why does Adam need bias correction?** `m` and `v` start at 0, so early averages are biased
towards 0. Dividing by `1 − βᵗ` rescales them, which matters in the first few hundred steps.

**Q: Adam vs AdamW?** Adam with L2 adds `λw` to the gradient, which then gets divided by `√v`,
so decay strength depends on each weight's gradient history. AdamW applies the decay directly to
the weights, giving true weight decay and better generalisation.

**Q: BatchNorm vs LayerNorm?** BatchNorm normalises each feature across the batch and needs
running statistics for inference; it struggles with small batches and sequences. LayerNorm
normalises each example across its features; it is batch-independent and is standard in
transformers.

**Q: What causes vanishing or exploding gradients, and how do you fix them?** Repeated
multiplication by Jacobians with norm < 1 or > 1 through depth (or through time in RNNs). Fixes:
ReLU-family activations, He/Xavier initialisation, normalisation layers, residual connections,
LSTM/GRU gating, and gradient clipping for explosions.

---

**Next:** [09 · PyTorch training](../09-PyTorch-Training/README.md)
