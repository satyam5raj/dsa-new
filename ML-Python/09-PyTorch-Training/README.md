# 09 · PyTorch training: the real-world toolkit

> Chapter 08 built everything by hand. PyTorch gives you the same pieces (tensors, automatic
> gradients, layers, optimisers), fast, on GPUs, and battle-tested. This chapter is the training
> loop you'll write hundreds of times.

Run it: `../.venv/bin/python pytorch_training.py` → **30/30 checks** (runs on CPU in ~2 s).

| Chapter 08 (NumPy) | PyTorch |
|---|---|
| `np.ndarray` | `torch.Tensor` (plus a device and autograd) |
| hand-written `backward()` | `loss.backward()` (autograd) |
| `Dense`, `ReLU`, `Dropout` classes | `nn.Linear`, `nn.ReLU`, `nn.Dropout`, … |
| `Adam` class | `torch.optim.AdamW` |
| manual batching | `Dataset` + `DataLoader` |

---

## 1. Tensors

```python
x = torch.arange(6, dtype=torch.float32).reshape(2, 3)
x + torch.tensor([10., 20., 30.])        # broadcasting, exactly like NumPy
x @ torch.ones(3, 4)                     # (2, 4)
x.to("cuda")  /  x.to("mps")             # move to a GPU (NVIDIA / Apple silicon)
```

- The default float type is `float32`. Deep learning rarely needs float64; training often uses
  `bfloat16`/`float16` mixed precision.
- `torch.from_numpy(a)` **shares memory**: changing `a[0]` to 99 changed the tensor too. Use
  `torch.tensor(a)` for a copy.
- Shape helpers: `reshape`/`view`, `unsqueeze(0)` (`(3,)` → `(1, 3)`), `squeeze`, `permute`,
  `transpose`.
- On this machine MPS (Apple GPU) is available. The course uses CPU so the self-checks are
  bit-reproducible.

**Most common bug classes:**

- shape mismatches (print `.shape` everywhere);
- tensors on different devices;
- integer labels with the wrong dtype (`CrossEntropyLoss` needs `long`).

## 2. Autograd

Set `requires_grad=True`, compute, call `.backward()`, and read `.grad`:

```python
w = torch.tensor(3.0, requires_grad=True)
(w**2 + 2*w).backward()      # w.grad = 2w + 2 = 8
```

- PyTorch records the operations in a graph as they run (**define-by-run**) and applies the chain
  rule backwards: chapter 08's backprop, automated.
- **Gradients accumulate.** A second `backward()` made `w.grad = 16`. That's why every training
  step starts with `optimizer.zero_grad()`. (Accumulation is also a feature: sum gradients over
  several small batches to simulate a big one.)
- The logistic-regression gradient from autograd **equals** the hand-derived `Xᵀ(p − y)/n` from
  chapter 04.
- `torch.no_grad()` (inference) and `.detach()` stop tracking, saving memory and time.

## 3. `nn.Module`: define a model

```python
class MLP(nn.Module):
    def __init__(self, n_in, n_hidden, n_out, dropout=0.2):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(n_in, n_hidden), nn.BatchNorm1d(n_hidden), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(n_hidden, n_hidden), nn.BatchNorm1d(n_hidden), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(n_hidden, n_out))          # logits! CrossEntropyLoss applies log-softmax itself
    def forward(self, x):
        return self.net(x)
```

- **26,634 parameters**: `(64·128+128) + 2·128 [BN] + (128·128+128) + 2·128 [BN] + (128·10+10)`.
- `model.train()` / `model.eval()` toggle Dropout and BatchNorm behaviour. **Forgetting `eval()`
  at inference is one of the most common bugs.**
- Don't put a softmax before `nn.CrossEntropyLoss`: it expects raw logits (that's the numerical
  stability trick from chapter 08).

## 4. `Dataset` and `DataLoader`

```python
class DigitsDataset(Dataset):
    def __len__(self): return len(self.y)
    def __getitem__(self, i): return self.X[i], self.y[i]

loader = DataLoader(ds, batch_size=64, shuffle=True, num_workers=4, pin_memory=True)
```

- A `Dataset` answers "how many?" and "give me item i". It can load images from disk lazily,
  augment them, and tokenise text.
- A `DataLoader` batches, shuffles and loads in parallel worker processes.
- 1,149 training digits with batch 64 give **18** batches; with `drop_last=True`, **17**.
- `TensorDataset(X, y)` is the one-line version for in-memory tensors.

## 5. The training loop

```python
for epoch in range(epochs):
    model.train()
    for xb, yb in loader:
        xb, yb = xb.to(device), yb.to(device)
        optimizer.zero_grad()                              # 1. clear old gradients
        loss = F.cross_entropy(model(xb), yb)              # 2. forward + loss
        loss.backward()                                    # 3. backprop
        nn.utils.clip_grad_norm_(model.parameters(), 1.0)  # 4. clip exploding gradients
        optimizer.step()                                   # 5. update weights
        scheduler.step()                                   # 6. update the learning rate
    val_loss = evaluate(model, X_val, y_val)               # model.eval() + torch.no_grad()
    # early stopping: keep the best state_dict, stop after `patience` epochs without improvement
```

On sklearn's 8 × 8 digits (train / validation / test split, scaler fit on train only):

| | Value |
|---|---|
| untrained validation loss | **2.304** ≈ ln 10 (the chapter 08 sanity check) |
| epochs run | **32** of 60 (early stopping, patience 8) |
| best validation loss | 0.0907 |
| **test accuracy** | **0.972** |

**Gradient clipping:** a gradient `[300, 400, 0]` has norm **500**. `clip_grad_norm_(…, 1.0)`
rescales it to `[0.6, 0.8, 0]`: same direction, length 1. This is essential for RNNs and
transformers, where one bad batch can blow up the weights.

**Debugging a training run, in order:**

1. Is the initial loss ≈ `ln(num_classes)`?
2. Can the model **overfit one small batch** to ~0 loss? If not, there's a bug (in the data,
   labels, loss or learning rate).
3. Plot train vs validation loss.
4. Try a few learning rates (it's the most important hyperparameter).
5. Check data augmentation, normalisation and label alignment.

## 6. Learning-rate schedule: warm-up + cosine

```python
def warmup_cosine(step, warmup, total):
    if step < warmup:
        return (step + 1) / warmup                         # linear ramp up
    progress = (step - warmup) / (total - warmup)
    return 0.5 * (1 + cos(pi * progress))                  # smooth decay to 0
scheduler = LambdaLR(optimizer, lambda s: warmup_cosine(s, total // 10, total))
```

From the run:

- the first recorded learning rates were `5.6e-05, 8.3e-05, 1.1e-04, …`, rising to the base
  **3e-3**;
- the full schedule ends at 0;
- this run stopped early at epoch 32, mid-decay, at 1.6e-3.

**Why warm up?** At the start, Adam's second-moment estimates are based on a handful of steps
and are unreliable, and the weights are random, so large steps can push the model somewhere bad
and unrecoverable. Warm-up is standard for transformers and large batch sizes. **Why decay?**
Big steps explore; small steps settle into a minimum.

Other schedulers:

- `StepLR` (drop ×0.1 every N epochs);
- `ReduceLROnPlateau` (drop when validation stalls);
- `OneCycleLR`.

## 7. Save, load, reproduce

```python
torch.save(model.state_dict(), "model.pt")                # weights only (111 KB here)
model = MLP(64, 128, 10)
model.load_state_dict(torch.load("model.pt", weights_only=True))
model.eval()
```

- Save the **`state_dict`** (a dict of tensors), not the whole pickled model object, which breaks
  when your code changes. `weights_only=True` refuses to unpickle arbitrary code, so it's safe for
  files from elsewhere.
- For resumable training, also save the optimizer and scheduler `state_dict`s, the epoch, and the
  RNG states.
- The reloaded model gave **bit-identical** predictions.
- Reproducibility: `torch.manual_seed`, seeded `DataLoader` generators, a fixed number of threads.
  The same seed gave an identical run; a different seed gave a different one. On GPUs also set
  `torch.use_deterministic_algorithms(True)` (slower).

## Scaling up: what changes on real hardware

| Technique | What it does |
|---|---|
| **Mixed precision** (`torch.autocast`, bf16/fp16) | ~2× faster, half the memory; fp16 needs a `GradScaler` to avoid underflow |
| **Gradient accumulation** | effective batch = batch × accumulation steps, when memory is short |
| **Data parallel (DDP)** | one model copy per GPU, gradients averaged each step |
| **FSDP / ZeRO** | shard the parameters, gradients and optimizer state across GPUs, for models that don't fit on one |
| **`torch.compile`** | graph capture + kernel fusion for speed |
| **Activation checkpointing** | recompute activations in backward: less memory, more compute |

---

## Exercises

1. You forget `optimizer.zero_grad()`. What happens to training?
2. Your validation accuracy is lower in `eval()` mode than in `train()` mode. Name a likely
   cause.
3. Why does the scheduler step once per **batch** here instead of once per epoch?
4. Write the line that freezes all layers except the last `nn.Linear`.

<details><summary>Answers</summary>

1. Gradients from every step add up, so effective step sizes grow without bound and training
   becomes erratic or diverges.
2. BatchNorm's running statistics don't match the data: tiny batches during training, a
   distribution shift between train and validation, or too few training steps for the running
   averages to settle. (Dropout makes train-mode accuracy **lower**, not higher.)
3. Warm-up over the first ~10 % of **steps** needs per-step resolution; with 18 batches per epoch,
   per-epoch stepping would be far too coarse. Either works if the lambda is written for that unit.
4. `for p in model.parameters(): p.requires_grad = False`, then
   `for p in model.net[-1].parameters(): p.requires_grad = True`, and pass only the trainable
   parameters to the optimizer.
</details>

## Interview questions

**Q: Walk me through a PyTorch training step.** `zero_grad`, forward, compute the loss,
`backward` (autograd fills `.grad`), optionally clip, `optimizer.step`, `scheduler.step`. Wrap
evaluation in `model.eval()` and `torch.no_grad()`.

**Q: What does `model.eval()` change?** Dropout is disabled and BatchNorm uses running statistics
instead of batch statistics. It does **not** turn off gradient tracking; that's `torch.no_grad()`
(or `torch.inference_mode()`).

**Q: How do you train a model that doesn't fit in GPU memory?** Smaller batches + gradient
accumulation, mixed precision, activation checkpointing, then sharding (FSDP/ZeRO), tensor or
pipeline parallelism, or parameter-efficient fine-tuning (LoRA, chapter 12).

**Q: Why warm-up?** Early in training the optimizer statistics are unreliable and the
representations are random, so large steps destabilise training (especially with Adam, large
batches and transformers). A short linear warm-up lets training settle before the full learning
rate.

---

**Next:** [10 · Deep-learning architectures](../10-Deep-Learning-Architectures/README.md)
