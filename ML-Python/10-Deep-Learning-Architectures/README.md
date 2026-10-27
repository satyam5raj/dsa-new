# 10 · Deep-learning architectures

> An architecture is a **built-in assumption** about the data. Images have local patterns that
> can appear anywhere → convolutions. Sequences have order → recurrence (or attention, chapter
> 11). Graphs have neighbours → message passing. Choosing the right assumption is worth more
> than any amount of tuning.

Run it: `../.venv/bin/python architectures.py` → **29/29 checks** (~15 s on CPU).

| Architecture | Data | Built-in assumption |
|---|---|---|
| **CNN** | images, audio spectrograms | local patterns, translation equivariance |
| **RNN / LSTM / GRU** | sequences, time series | order; a running memory |
| **Autoencoder** | anything | data lives on a low-dimensional manifold |
| **VAE** | anything (generative) | a smooth latent space you can sample from |
| **GAN** | images (generative) | learn to generate by fooling a critic |
| **GNN** | graphs: social, molecules, fraud rings | a node is described by its neighbourhood |
| **Diffusion** | images, audio, video (generative) | learn to undo gradual noise |
| **Transformer** | text, everything | chapter 11 |

---

## 1. Convolution from scratch

Slide a small **kernel** (filter) over the image and take a dot product with each patch:

```python
for i in range(out):
    for j in range(out):
        res[i, j] = np.sum(img[i*s:i*s+k, j*s:j*s+k] * kernel)
```

This matches `torch.nn.functional.conv2d` exactly. (Deep-learning "convolution" is technically
cross-correlation: the kernel isn't flipped.)

**Output size** = `(n + 2p − k)/s + 1`:

- 7×7 input, kernel 3, stride 2, padding 1 → **4×4**;
- kernel 3 with padding 1 and stride 1 keeps the size ("same" padding).

**What a filter detects:** a Sobel kernel `[[−1,0,1],[−2,0,2],[−1,0,1]]` on an image that's dark on
the left and bright on the right gives `[0, 4, 4, 0]`: it fires **only at the edge**. A CNN
**learns** its filters. Early layers find edges and colours, middle layers textures and parts, and
late layers objects.

**Max pooling** (2×2): `[[5, 7], [13, 15]]` from a 4×4 grid keeps the strongest response per
window. It downsamples and adds a little shift tolerance.

**Why CNNs scale:**

- **Parameter sharing:** the first layer on a 224×224 RGB image needs **9,633,792** weights as a
  dense layer to 64 units, but only **1,792** as 64 3×3 conv filters, because the same filter is
  used at every position.
- **Locality:** each output looks only at a small patch, and stacking layers grows the
  **receptive field**.

## 2. CNN vs MLP on handwritten digits

| Model | Test accuracy | Test images shifted 1 px right |
|---|---|---|
| MLP 64-128-10 | 0.978 | **0.460** |
| CNN (2 conv + pool + linear) | **0.982** | **0.651** |
| CNN + **shift augmentation** in training | | **0.987** |

The MLP treats pixel (3, 4) and pixel (3, 5) as unrelated inputs, so a one-pixel shift wrecks it.
Convolutions are **translation-equivariant** (shift the input, and the feature map shifts with
it), and pooling adds some invariance.

Even the CNN only partly generalises to shifts. **Data augmentation** (training on shifted copies)
teaches the invariance directly. Standard augmentations for images: flips, crops, rotations,
colour jitter, Cutout/Mixup/CutMix.

**Landmark CNNs to know:**

- LeNet (1998);
- AlexNet (2012, ReLU + GPUs);
- VGG (stacks of 3×3);
- **ResNet** (2015, residual connections `x + f(x)` allowing 100+ layers);
- EfficientNet (scaling rules);
- Vision Transformers (ViT, chapter 11), which now rival CNNs given lots of data.

**Transfer learning:** take a network pretrained on ImageNet, replace the final layer, and
fine-tune. It's the default for real image tasks with limited data (Project 2 in chapter 15).

## 3. RNNs and LSTMs: memory over time

An RNN reads a sequence one step at a time, updating a hidden state: `hₜ = tanh(W xₜ + U hₜ₋₁ + b)`.
The same weights are used at every step.

**The task:** the label is the sign of the **first** number; the next 39 numbers are noise.

| Model | Length 5 | Length 40, seeds 0 / 1 / 2 |
|---|---|---|
| RNN | 0.995 | 0.496 / 0.987 / 0.643: **unreliable** |
| LSTM, default init | 0.996 | 0.485: forgets too |
| **LSTM, forget-gate bias = 3** | | **0.983 / 0.985 / 0.990** |

**Why the plain RNN struggles:** the gradient from step 40 back to step 1 is a product of 39
Jacobians, so it vanishes (or explodes). That's chapter 08's vanishing-gradient problem, through
time. One seed got lucky; the others didn't.

**LSTM** adds a **cell state** `c` with gates (sigmoids between 0 and 1):

```
f = σ(·)  forget gate:  how much of the old memory to keep
i = σ(·)  input gate:   how much new information to write
o = σ(·)  output gate:  how much memory to expose as h
c_t = f ⊙ c_{t−1} + i ⊙ tanh(·)          ← additive update: a gradient highway
h_t = o ⊙ tanh(c_t)
```

**The surprise from this run: initialisation of the forget gate matters.**

- With PyTorch's default biases the forget gate starts at about σ(0) = 0.5, so memory decays like
  0.5⁴⁰ ≈ 0, and the LSTM failed exactly like the RNN.
- Setting the forget bias to 3 makes the gate start at σ(3) ≈ 0.95: "remember by default". Then
  it solved the task on every seed.
- This "forget-bias trick" (Jozefowicz et al., 2015) is standard practice. Even a gated
  architecture needs the right starting point.

| | RNN | LSTM | GRU |
|---|---|---|---|
| Gates | none | forget, input, output + cell state | update, reset (no separate cell) |
| Parameters (hidden 32, input 1) | 1,120 | **4,480** (4 × (32·33 + 64)) | 3,360 |
| Long-range memory | poor | good | good |

Today **transformers** have replaced RNNs for most language tasks (parallel training, direct
attention to any position). RNNs and GRUs remain useful for streaming and on-device models and for
small time-series problems; state-space models (Mamba) revive the recurrent idea at scale.

## 4. Autoencoders: compression and anomaly detection

**Encoder** 64 → 32 → **8** (bottleneck) → **decoder** 8 → 32 → 64, trained to reproduce its
input. To get through an 8-number bottleneck it must learn the **structure** of the data.

The demo trains on every digit **except 8**:

| | Mean reconstruction error |
|---|---|
| digits it has seen | 0.0218 |
| never-seen digit 8 | **0.0422** (about 2×) |

As an anomaly score, the reconstruction error separates 8s from the rest with **ROC-AUC 0.886**.
This is how autoencoders flag:

- defective products;
- fraud;
- unusual network traffic;

when you only have examples of "normal".

Variants:

- **denoising** AE (input corrupted, target clean);
- **sparse** AE;
- **convolutional** AE for images.

## 5. Variational autoencoders (VAE): a latent space you can sample

A plain autoencoder's codes can be scattered anywhere, so a random code decodes to garbage. A VAE
makes the encoder output a **distribution** `N(μ, σ²)` per input and pulls it towards `N(0, I)`:

```
loss = reconstruction loss + KL( N(μ, σ²) ‖ N(0, I) )
KL   = −½ Σ (1 + log σ² − μ² − σ²)        ← closed form
```

- For `μ = (1, 0)`, `σ² = (1, 4)`: KL = **1.307**, and a 400,000-sample Monte Carlo estimate
  agrees.
- **Reparameterisation trick:** you can't backpropagate through "sample z". Instead write
  `z = μ + σ ⊙ ε` with `ε ~ N(0, I)`: the randomness is an input, so gradients flow to μ and σ.

After training on digits, **random `z ~ N(0, I)` decode to plausible digits**:

- the mean pixel brightness is 0.306 (real digits 0.305);
- a KNN classifier recognises the samples as **all 10** different digits;
- test KL is 3.16 nats per image: the price paid for an organised latent space.

VAE samples tend to be blurry (the Gaussian likelihood averages over possibilities), which GANs
and diffusion improve on. The VAE idea lives on as the **latent autoencoder in Stable Diffusion**.

## 6. GANs: a forger and a detective

- **Generator G:** noise → fake sample. Goal: fool D.
- **Discriminator D:** sample → "real or fake?". Goal: catch G.

```
D step: push D(real) → 1 and D(G(z)) → 0
G step: push D(G(z)) → 1          ("non-saturating" loss: stronger gradients early on)
```

1-D demo: real data is `N(4, 1.25²)`. With 4-D noise in, the generator goes from mean −0.19,
std 0.30 to **mean 3.99, std 1.17**.

**GANs are unstable.** While building this demo, a version with **1-D noise** got the mean right
but the spread wrong (std **0.40** after 3,000 steps). In another configuration it swung from std
2.3 to 0.47 just by training longer. Producing too little variety is **mode collapse**: the
generator finds a few outputs that fool D and stops exploring.

Remedies:

- a Wasserstein loss with gradient penalty (WGAN-GP);
- spectral normalisation;
- two-time-scale learning rates;
- careful architecture (DCGAN rules), minibatch discrimination.

GANs gave the sharpest images of their era (StyleGAN). **Diffusion models** have largely replaced
them because they train stably.

## 7. GNNs: learning on graphs

**Graph convolution (GCN):** each node averages its neighbours' features (including its own),
then applies a shared linear layer:

```
H' = ReLU( D^−½ (A + I) D^−½ · H · W )
```

- `A + I`: the adjacency matrix with self-loops.
- `D^−½ … D^−½`: symmetric normalisation, so high-degree nodes don't dominate.

Each layer is one round of **message passing**; k layers let information travel k hops.

**Demo:** 40 nodes in 2 communities (edges are dense inside, sparse between), node features are
just identities, and only **2 nodes are labelled**. A 2-layer GCN classifies all 40 correctly
(**accuracy 1.000**). The same weights *without* the graph can't generalise beyond the 2 labels.
The labels spread through the edges.

Uses:

- fraud rings (accounts sharing devices or cards);
- recommendation (user–item graphs);
- molecules and drug discovery;
- traffic;
- knowledge graphs.

Variants: GraphSAGE (sampled neighbours, inductive), GAT (attention over neighbours), message
passing with edge features. Libraries: PyTorch Geometric, DGL.

## 8. Diffusion models

**Forward process (fixed, no learning):** add a little Gaussian noise at each of T steps until only
noise remains. There's a closed form to jump straight to step t:

```
x_t = √ᾱ_t · x_0 + √(1 − ᾱ_t) · ε,     ᾱ_t = Π (1 − β_s)
```

With T = 50 steps, ᾱ_T < 0.01: the signal is gone. The variance formula
`ᾱ·Var(x₀) + (1 − ᾱ)` matches the simulation (2.277).

**Reverse process (learned):** train a network `ε_θ(x_t, t)` to **predict the noise** that was
added, with a plain MSE loss: `‖ε − ε_θ(x_t, t)‖²`. To generate, start from pure noise and
repeatedly subtract the predicted noise (DDPM sampling).

**Demo:** data is two bumps at −2 and +2. Starting from pure noise:

- **46 % / 54 %** of the samples land on the two bumps;
- **99 %** are within 0.8 of a mode, not in the empty middle.

Real systems:

- a U-Net or transformer denoiser;
- **latent diffusion** (diffuse in a VAE's latent space: Stable Diffusion);
- text conditioning via cross-attention;
- classifier-free guidance;
- faster samplers (DDIM: tens of steps instead of 1,000).

---

## Exercises

1. A 32×32×3 input goes through `Conv2d(3, 16, kernel_size=5, stride=1, padding=0)`. What's the
   output shape, and how many parameters?
2. Why do RNNs process tokens sequentially while transformers can process them in parallel during
   training?
3. You train an autoencoder for fraud on **all** transactions, fraud included. Problem?
4. What does the KL term in a VAE trade off against?

<details><summary>Answers</summary>

1. `(32 − 5)/1 + 1 = 28` → **28×28×16**. Parameters `5·5·3·16 + 16 = 1,216`.
2. `hₜ` depends on `hₜ₋₁`, so step t can't start until step t−1 finishes. Attention computes all
   positions at once from the input (with a causal mask for generation).
3. It learns to reconstruct fraud too, especially if fraud is common or repetitive, so fraud's
   reconstruction error stays low. Train on (mostly) clean normal data, and validate the anomaly
   score on labelled cases.
4. Reconstruction quality. A strong KL keeps the latent space smooth and samplable but blurs the
   reconstructions; a weak KL gives sharp reconstructions but holes in the latent space (β-VAE
   tunes this weight).
</details>

## Interview questions

**Q: Why do CNNs work well on images?** Locality (pixels relate to nearby pixels), weight sharing
(the same filter everywhere, so far fewer parameters and translation equivariance), and a
hierarchy (deeper layers combine simpler features), plus pooling for some invariance.

**Q: Explain the LSTM.** It's an RNN with a cell state updated **additively**:
`c = f⊙c + i⊙g`. The forget, input and output gates control what to keep, write and expose.
The additive path lets gradients flow over many steps, mitigating vanishing gradients. Initialise
the forget-gate bias positive so it remembers by default.

**Q: GANs vs VAEs vs diffusion?** VAEs are stable, give an explicit latent and likelihood bound,
but produce blurry samples. GANs give sharp samples and fast generation, but unstable training and
mode collapse. Diffusion is stable to train, highest quality and diverse, but slow to sample
(iterative); latent diffusion and distilled samplers speed it up.

**Q: How does a GNN aggregate information?** Each layer, every node aggregates (sum, mean, max or
attention-weighted) its neighbours' messages, combines that with its own state, and applies a
shared transformation. Stacking k layers gives a k-hop receptive field. Too many layers cause
**over-smoothing**: all nodes start to look alike.

---

**Next:** [11 · Transformers and LLMs](../11-Transformers-and-LLMs/README.md)
