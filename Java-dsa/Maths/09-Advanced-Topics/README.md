# 09 · Advanced Topics for ML and AI

| | |
|---|---|
| **Code** | [AdvancedTopics.java](AdvancedTopics.java): every example below, including value iteration, Q-learning, a DFT, spectral clustering and an attention head (`java AdvancedTopics.java`, 43 checks) |
| **You need** | chapters [02](../02-Linear-Algebra/README.md) (eigenvalues), [03](../03-Calculus/README.md) (gradients), [04](../04-Probability/README.md) (Markov chains) and [07](../07-Information-Theory/README.md) (softmax) |
| **Used in** | writing numerically stable ML code, graph neural networks and clustering, reinforcement learning (game AI, robotics, RLHF), CNNs and audio processing, embeddings and vector search, SVMs, transformers |

These topics connect the core maths to the techniques behind modern AI. Each section is self-contained: read the ones you need.

---

## 1. Numerical methods: maths on a real computer

Computers store real numbers as **floating point** (IEEE 754): a `double` has about 16 significant decimal digits, a `float` about 7. Most of the time that's plenty. When it isn't, ML code produces `NaN`, `Infinity` or silently wrong answers.

### Rounding error
- `0.1 + 0.2 == 0.3` is **false**: 0.1 has no exact binary representation (like 1/3 in decimal), so the sum is **0.30000000000000004**. **Never compare doubles with `==`**; use a tolerance such as `|a − b| < 1e-9`.
- **Machine epsilon:** the gap between 1.0 and the next double is **2.22 × 10⁻¹⁶** (`Math.ulp(1.0)`).
- **Absorption:** `1e16 + 1 − 1e16 = 0.0`: the 1 is below the precision of 10¹⁶, so it vanishes.
- In `float`, 16,777,216 + 1 = 16,777,216 (2²⁴ is where float stops counting whole numbers). This bites in float16/bfloat16 training, which is why "mixed precision" keeps a float32 master copy of the weights.

### Catastrophic cancellation
Subtracting two nearly equal numbers wipes out the significant digits. `(1 − cos x)/x²` should be 0.5 for small x, but at x = 10⁻⁸, cos x rounds to exactly 1.0 and the formula gives **0.0**. Rewriting it algebraically as `2 sin²(x/2)/x²` avoids the subtraction and gives **0.5000**. **Algebraically equal formulas are not numerically equal.**

### Summing many numbers: Kahan summation
Adding 0.1 ten million times should give 1,000,000:
| Method | Result |
|---|---|
| naive `sum += x` | **999,999.9998389754** (the error grows with the count) |
| Kahan summation | **1,000,000.0000000000** |

Kahan's trick keeps a second variable holding the low-order bits lost in each addition and feeds them back in. Pairwise (tree) summation, which NumPy uses, is another fix.

### Overflow and the log-sum-exp trick
Softmax needs Σ e^(zᵢ). For z = [1000, 1001, 1002], e¹⁰⁰⁰ is beyond the largest double, so the result is **Infinity**. The fix is to subtract the maximum first:
```
log Σ eᶻⁱ = m + log Σ e^(zᵢ − m)          with m = max zᵢ
```
That gives **1002.4076**, with no overflow. Every softmax and cross-entropy implementation does this.

### Stable sigmoid
`1/(1 + e⁻ᶻ)` overflows for very negative z (e¹⁰⁰⁰). Use `eᶻ/(1 + eᶻ)` when z < 0. Libraries provide fused, stable functions like `log_softmax` and `BCEWithLogitsLoss` for exactly these reasons: **pass logits, not probabilities**.

### Conditioning
A problem is **ill-conditioned** when tiny input changes cause huge output changes. For linear systems, the condition number (the ratio of the largest to the smallest singular value) measures it: about log₁₀(condition number) digits of accuracy are lost. Fixes: pivoting ([chapter 02](../02-Linear-Algebra/README.md)), QR/SVD instead of the normal equations, feature scaling, regularisation (ridge adds λI, which improves the conditioning).

---

## 2. Spectral graph theory: graphs as matrices

### The graph Laplacian
For a graph with **degree matrix D** (degrees on the diagonal) and **adjacency matrix A**:
```
L = D − A
```
Its eigenvalues and eigenvectors (its **spectrum**) reveal the graph's structure:
- L is symmetric and positive semi-definite, so all eigenvalues are ≥ 0.
- **The number of zero eigenvalues = the number of connected components.** Two separate triangles give **2** zero eigenvalues.
- The second-smallest eigenvalue λ₂ (the **algebraic connectivity**) measures how well connected the graph is: small means there's a "bottleneck".
- Its eigenvector, the **Fiedler vector**, shows **where to cut**.

### Spectral clustering
Two dense groups of 4 nodes joined by a single bridge edge: the Fiedler vector's signs come out `----++++`, separating the two groups exactly, with λ₂ = **0.3187** (small: the graph is easy to cut). Spectral clustering uses the bottom k eigenvectors as coordinates and runs k-means on them. It can find clusters of any shape, which plain k-means can't.

### Where it's used
- **Graph Neural Networks (GNNs):** a GCN layer computes roughly `H' = σ(D̃^(−½) Ã D̃^(−½) H W)`, a normalised adjacency (Laplacian-like) operator applied to node features. It's a "graph convolution".
- **PageRank** ([chapter 04](../04-Probability/README.md)): the eigenvector of a transition matrix.
- Community detection, image segmentation, graph embeddings, measuring network robustness.

---

## 3. Markov Decision Processes and reinforcement learning

### The setup
An **agent** interacts with an **environment**:
```
state s → agent chooses action a → environment returns reward r and next state s'
```
An **MDP** is (states, actions, transition probabilities, rewards, discount γ). The goal is to find a **policy** π(s) → action that maximises the **expected discounted return**:
```
G = r₁ + γr₂ + γ²r₃ + …          (0 ≤ γ < 1: a reward now is worth more than one later)
```

### Value functions and the Bellman equation
- **V(s)** = the expected return from state s when acting optimally.
- **Q(s, a)** = the expected return after taking action a in s, then acting optimally.

The **Bellman optimality equation** says the value of a state is the best immediate reward plus the discounted value of where you land:
```
V(s) = max over a of [ r(s, a) + γ · V(s') ]
```

### Value iteration (when you know the rules)
Start with V = 0 and apply the Bellman equation to every state repeatedly until nothing changes. On a 4 × 4 grid (start at the top-left, **G** = goal +10, **P** = pit −10, every step −1, γ = 0.9):
```
v v > v
v v P v          the optimal policy: go around the pit to the goal
v v v v
> > > G
```
- V(start) = −1 − 0.9 − 0.81 − 0.729 − 0.6561 + 10·0.9⁵ = **1.8098** (five steps, then the goal).
- Many routes tie for optimal; the code breaks ties by preferring "down".

### Q-learning (when you don't know the rules)
Real environments don't hand you the transition probabilities. **Q-learning** learns from experience: take an action, see the reward and the next state, and nudge Q towards the Bellman target:
```
Q(s, a) ← Q(s, a) + α [ r + γ · max over a' of Q(s', a') − Q(s, a) ]
```
Use **ε-greedy** exploration: usually take the best-known action, sometimes (20% in the code) a random one, so you don't miss better routes. After 5,000 episodes, the greedy path reaches the goal in the optimal **6 steps**, and Q(start) matches V(start).

### From here to modern AI
- **Deep Q-Networks (DQN):** a neural network approximates Q (it learned Atari games from pixels).
- **Policy gradient** methods (REINFORCE, PPO) optimise π directly using ∇ log π(a|s) · return.
- **RLHF:** fine-tuning LLMs with a learned reward model and PPO, plus a KL penalty to stay close to the base model ([chapter 07](../07-Information-Theory/README.md)).
- AlphaGo/AlphaZero combine RL with tree search.

---

## 4. The Fourier transform: signals as sums of waves

### The idea
**Any signal can be written as a sum of sine waves** of different frequencies. The Fourier transform turns a signal in **time** into its **frequencies**:
```
X[k] = Σₜ x[t] · e^(−2πi·k·t/N)          (the Discrete Fourier Transform, DFT)
```
Each X[k] measures how much of frequency k the signal contains (e^(iθ) = cos θ + i sin θ is a rotating arrow, and the sum measures how well the signal lines up with it).

### Example
The code builds `3·sin(2π·5t) + 1·sin(2π·20t)` sampled 256 times. The DFT finds peaks at exactly **frequencies 5 and 20**, with amplitudes **3.0000** and **1.0000**. The ingredients are recovered from the mixture.

### The FFT
The direct DFT costs O(N²). The **Fast Fourier Transform** (Cooley–Tukey: split into even and odd samples recursively, which is divide and conquer) does it in **O(N log N)**. It's one of the most important algorithms ever, behind MP3, JPEG, Wi-Fi/4G, MRI, earthquake analysis, and multiplying huge numbers.

### Where it's used in ML
- **Audio:** speech models work on spectrograms (Fourier transforms of short windows) and mel-frequency features.
- **Positional encodings** in the original transformer are sines and cosines of different frequencies.
- **FNet** replaces attention with Fourier transforms; long convolutions are computed via the FFT.

---

## 5. Convolution: what CNNs compute

### Definition
Slide a small **kernel** (filter) across the input, and at each position take the weighted sum:
```
(f * g)[n] = Σₖ f[k] · g[n − k]
```

### Examples on a 1-D "image" `[0 0 0 10 10 10 0 0 0]` (a bright band)
| Kernel | Output | Effect |
|---|---|---|
| blur `[⅓ ⅓ ⅓]` | `[0 0 0 3.33 6.67 10 6.67 3.33 0 0 0]` | averages neighbours; the edges soften |
| edge `[1 −1]` | `[0 0 0 10 0 0 −10 0 0 0]` | fires **only where brightness changes** |

In 2-D the same idea gives blurring, sharpening and edge detection (Sobel filters). **A CNN learns its kernels**: early layers end up as edge and texture detectors, deeper layers as parts and objects. Convolution shares the same small set of weights across the whole image (few parameters, and a pattern is recognised wherever it appears).

### Convolution = polynomial multiplication
Convolving `[1, 2]` with `[3, 1, 1]` gives **`[3, 7, 3, 2]`**, which is exactly the coefficients of (1 + 2x)(3 + x + x²) = 3 + 7x + 3x² + 2x³.

### The convolution theorem
**Convolution in time = multiplication in frequency**: `F(f * g) = F(f) · F(g)`. So a big convolution can be done as FFT → multiply → inverse FFT in O(N log N) instead of O(N²). It's also why fast polynomial and big-integer multiplication works.

---

## 6. High-dimensional geometry: intuition breaks

Embeddings live in hundreds or thousands of dimensions, and our 3-D intuition is wrong there.

### Distances concentrate
For random points in a d-dimensional cube, the ratio of the **farthest** to the **nearest** neighbour (of 200 random points):
| d | max/min distance |
|---|---|
| 2 | **18.69** |
| 100 | **1.36** |
| 10,000 | **1.03** |

In high dimensions, **everything is about the same distance from everything else**. That's the **curse of dimensionality**: "nearest neighbour" becomes less meaningful, and k-NN and clustering on raw high-dimensional data degrade. Fixes: learn a better representation (embeddings), reduce dimensions (PCA), and use cosine similarity on normalised vectors.

### Random vectors are almost perpendicular
Two random 1,000-dimensional Gaussian vectors have |cos θ| below 0.15 (typically about 1/√d ≈ 0.03). There's an enormous number of nearly orthogonal directions in high dimensions, which is part of why embeddings can store so many distinct concepts, and why random projections preserve distances (Johnson–Lindenstrauss).

### Volume moves to the surface
- The unit ball's volume is π ≈ **3.1416** in 2-D, but only **0.0258** in 20-D; it tends to 0, while the cube around it has volume 2ᵈ.
- In 1,000 dimensions, **essentially 100%** of a cube's volume lies in its outer 1% shell (1 − 0.98¹⁰⁰⁰ ≈ 1).
- High-dimensional Gaussians are like thin shells at radius ≈ √d, not fuzzy balls. Almost no samples are near the mean.

### Vector search
RAG systems and recommenders find the nearest embeddings among millions. Exact search is too slow, so they use **approximate nearest neighbour** indexes: HNSW (a navigable graph), IVF (cluster first), and product quantisation. They're fast because they accept a tiny error.

---

## 7. Kernel methods: the kernel trick

### The problem
Some data isn't linearly separable. The classic example is **XOR**: (1, 1) and (−1, −1) are one class, (1, −1) and (−1, 1) the other; no straight line separates them. But add the feature **x₁·x₂** and it becomes trivially separable (positive vs negative). **Mapping to a higher-dimensional feature space can make problems linear.**

### The trick
Many algorithms (SVMs, ridge regression, PCA) only need **dot products** between data points. A **kernel** computes the dot product in a feature space **without ever building the features**:
```
k(x, y) = φ(x) · φ(y)
```
For the degree-2 polynomial kernel with x = (1, 2), y = (3, 4):
- explicit features φ(x) = (x₁², √2·x₁x₂, x₂²), dot product = **121**;
- kernel (x · y)² = 11² = **121**, the same number without building φ.

| Kernel | Formula | Feature space |
|---|---|---|
| linear | x · y | the original space |
| polynomial | (x · y + c)ᵈ | all monomials up to degree d |
| **RBF (Gaussian)** | exp(−\|x − y\|²/2σ²) | **infinite**-dimensional! |

**Mercer's condition:** a valid kernel's matrix K (Kᵢⱼ = k(xᵢ, xⱼ)) must be **positive semi-definite** ([chapter 02](../02-Linear-Algebra/README.md)). The code checks zᵀKz ≥ 0 for the RBF kernel on 1,000 random z.

**Connections:** SVMs with RBF kernels were the state of the art before deep learning. Attention can be viewed as a kernel smoother. Gaussian processes are kernel methods for Bayesian regression. Neural tangent kernels describe very wide networks.

---

## 8. The maths of attention (transformers)

### The formula
```
Attention(Q, K, V) = softmax( Q Kᵀ / √d ) · V
```
For each token:
1. A **query** q ("what am I looking for?") is dotted with every token's **key** k ("what do I contain?"), giving similarity scores ([chapter 02](../02-Linear-Algebra/README.md): dot products).
2. **Softmax** turns the scores into weights that sum to 1 ([chapter 07](../07-Information-Theory/README.md)).
3. The output is the **weighted average of the values** v ("what I pass on").

A tiny head with 3 tokens (d = 2): token 0's weights are **[0.40, 0.20, 0.40]**, so its output is 0.4·[10, 0] + 0.2·[0, 10] + 0.4·[5, 5] = **[6.02, 3.98]** (shown rounded; exact weights 0.4011, 0.1978, 0.4011).

### Why divide by √d?
If q and k have d independent entries with variance 1, their dot product has variance d, so its standard deviation is √d. The code measures **4** for d = 16 and **32** for d = 1024. Without scaling, scores like 32 or −32 push softmax to saturation: it outputs **1.0000** for the top score (one-hot), its gradient is about 0, and learning stalls. Dividing by √d brings the scores back to unit scale, giving a healthy **0.6652**. One small piece of probability theory makes transformers trainable.

### The rest of a transformer, mathematically
| Part | Maths |
|---|---|
| embeddings | vectors (lookup rows of a matrix) |
| Q, K, V projections | matrix multiplications |
| multi-head attention | several attention heads in parallel, concatenated |
| feed-forward layer | two matrix multiplications with a non-linearity (GELU) |
| residual connections | x + f(x): keeps gradients flowing ([chapter 03](../03-Calculus/README.md)) |
| layer normalisation | z-scores per token ([chapter 05](../05-Statistics/README.md)) |
| output | softmax over the vocabulary, trained with cross-entropy |
| training | AdamW + warm-up + cosine decay ([chapter 06](../06-Optimization/README.md)) |
| cost | attention is O(n²) in sequence length, which is why long-context tricks exist |

---

## 9. Where to go deeper

| Topic | Next step |
|---|---|
| measure-theoretic probability | needed for research-level ML theory and stochastic calculus (diffusion models) |
| information geometry | the natural gradient, Fisher information |
| optimal transport | Wasserstein distances, used in GANs and generative models |
| differential equations | neural ODEs, physics-informed networks, diffusion as an SDE |
| statistical learning theory | VC dimension, PAC learning, generalisation bounds |
| graph theory, advanced | GNN expressiveness, Weisfeiler–Lehman tests |
| category theory, type theory | functional programming, programming-language design |

---

## 10. Exercises (answers below)

1. Why is `if (a == b)` dangerous for doubles, and what should you write instead?
2. Compute log(e¹⁰⁰⁰ + e¹⁰⁰⁰) without overflow.
3. A graph's Laplacian has eigenvalues 0, 0, 0, 1.2, 3.5. How many connected components does it have?
4. In the Bellman equation, what does γ = 0 mean? And γ close to 1?
5. A signal contains a 50 Hz hum. How would you remove it?
6. Why does k-nearest-neighbours work badly on raw 10,000-dimensional data?
7. Why does attention divide by √d instead of d?

<details>
<summary>Answers</summary>

1. Rounding makes mathematically equal results differ in the last bits (0.1 + 0.2 ≠ 0.3). Use `Math.abs(a - b) < tolerance` (relative tolerance for large values).
2. m = 1000: 1000 + log(e⁰ + e⁰) = 1000 + log 2 ≈ 1000.693.
3. Three (the number of zero eigenvalues).
4. γ = 0: the agent only cares about the immediate reward (greedy). γ → 1: it weighs future rewards almost as much as current ones (far-sighted, slower to converge).
5. Take the FFT, zero out the 50 Hz component (a notch filter), and inverse FFT.
6. Distances concentrate, so the "nearest" neighbours are barely nearer than random points; many irrelevant dimensions add noise. Reduce dimensions or learn embeddings first.
7. The dot product's standard deviation grows like √d (its variance like d). Dividing by √d gives unit variance; dividing by d would shrink scores towards 0 and make attention nearly uniform.
</details>

---

## 11. Interview questions

**Q1. Why is softmax implemented by subtracting the max logit?**
> e^z overflows for large z. Subtracting the max doesn't change the result (it cancels in the ratio) but keeps every exponent ≤ 0.

**Q2. What is the graph Laplacian, and what does its spectrum tell you?**
> L = D − A. The number of zero eigenvalues equals the number of connected components; the second eigenvector (Fiedler) gives a good 2-way cut, which is the basis of spectral clustering and GNN graph convolutions.

**Q3. Explain the Bellman equation and Q-learning.**
> The value of a state is the best immediate reward plus the discounted value of the next state. Q-learning estimates Q(s, a) from experience by moving it towards r + γ max Q(s', ·), with ε-greedy exploration.

**Q4. What does the convolution theorem say, and why does it matter?**
> Convolution in the signal domain is pointwise multiplication in the frequency domain, so large convolutions (and polynomial multiplication) can be done in O(N log N) with the FFT.

**Q5. What is the curse of dimensionality?**
> As dimensions grow, volume concentrates at the edges and pairwise distances become nearly equal, so distance-based methods lose meaning and you need exponentially more data to cover the space.

**Q6. What is the kernel trick?**
> Replacing dot products φ(x)·φ(y) with a kernel k(x, y) computed directly in the input space, so algorithms that only use dot products can work in huge (even infinite) feature spaces cheaply.

**Q7. Why is attention scaled by 1/√d?**
> q·k has variance d for unit-variance entries; large scores saturate the softmax and kill the gradients. Scaling restores unit variance.

---

**Back to:** [Maths overview](../README.md)
