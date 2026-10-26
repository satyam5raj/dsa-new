# 06 · Optimisation

| | |
|---|---|
| **Code** | [Optimization.java](Optimization.java): every optimiser below, run on the same problems so you can compare them (`java Optimization.java`, 27 checks) |
| **You need** | gradients from [03 · Calculus](../03-Calculus/README.md), eigenvalues and positive-definite matrices from [02 · Linear Algebra](../02-Linear-Algebra/README.md), MLE and MAP from [05 · Statistics](../05-Statistics/README.md) |
| **Used in** | **training every ML model**, scheduling and routing (linear programming), resource allocation, SVMs, reinforcement learning |

Training a model = **finding the parameters that minimise the loss**. That's an optimisation problem. This chapter covers how it's done, why it sometimes fails, and what the knobs (learning rate, batch size, momentum, Adam) actually do.

---

## 1. The setup

```
minimise  L(w)        over the parameters w (a few, or billions)
```
- **objective / loss** L: what we want small (prediction error);
- **parameters** w: what we can change (weights);
- **constraints** (sometimes): rules w must obey (weights ≥ 0, a budget).

Most ML losses can't be solved with algebra ("set the derivative to 0 and solve"), so we search **iteratively**: start somewhere, and repeatedly step to a better point.

---

## 2. Convexity: when optimisation is easy

### Convex functions: one valley
A function is **convex** if the straight line (chord) between any two points on its graph lies **above** the graph:
```
f(λa + (1−λ)b) ≤ λf(a) + (1−λ)f(b)        for all a, b and 0 ≤ λ ≤ 1
```
The code tests this inequality on 10,000 random pairs for x² + 2x + 3: it always holds.

**Why it matters:** for a convex function, **every local minimum is the global minimum**. Gradient descent can't get stuck in the wrong valley. Convex problems include linear regression (MSE), logistic regression (log loss), SVMs, ridge and lasso.

Ways to recognise convexity:
- the second derivative is ≥ 0 (in many dimensions, the Hessian is positive semi-definite);
- sums of convex functions are convex;
- norms, eˣ, x², and −log x are convex.

### Non-convex functions: many valleys
`f(x) = x⁴ − 3x² + x` has two valleys. Gradient descent ends up in whichever valley it starts in:
| Start | Lands at | f there |
|---|---|---|
| x = −2 | **−1.3008** | −3.5139 (**global** minimum) |
| x = +2 | **1.1309** | −1.0702 (just a **local** minimum) |

**Neural networks are highly non-convex.** Surprisingly, this matters less than you'd fear: in very high dimensions, most local minima have similar loss, and the real obstacles are saddle points and plateaus (section 10). Random initialisation, SGD noise and momentum all help.

---

## 3. Gradient descent and the learning rate

### The algorithm
```
repeat:   w ← w − η · ∇L(w)              (η = the learning rate)
```
Step downhill, opposite the gradient, by an amount proportional to the slope.

### The learning rate is the most important knob
On `f(x, y) = x² + 10y²` (a long narrow valley: curvature 2 along x, 20 along y), starting from (10, 1):
| Learning rate | Steps to converge |
|---|---|
| 0.001 | 8,051 (crawls) |
| 0.01 | 798 |
| 0.09 | **82** |
| 0.11 | **diverges**: the values blow up |

**Why there's a hard limit:** along a direction with curvature L, each step multiplies the error by (1 − ηL). If η > 2/L, then |1 − ηL| > 1, so every step **overshoots** and the error **grows**. Here the steep direction has L = 20, so the limit is η < 2/20 = **0.1**.

**The big lesson:** the steepest direction limits the learning rate, but the flattest direction sets how long it takes. The ratio of the largest to the smallest curvature (the **condition number**, here 10) determines how slow plain gradient descent is. Momentum and Adam (below) attack exactly this, as do feature scaling and batch normalisation.

### Learning-rate schedules
Big steps early (to make progress), small steps later (to settle in): step decay, exponential decay, **cosine annealing**, and **warm-up** (start small, ramp up), which is standard for training transformers.

---

## 4. Batch, mini-batch and stochastic gradient descent

The loss is an average over the training examples: L = (1/n) Σ ℓᵢ. Computing the exact gradient needs **all n examples** for every single step.

| Variant | Examples per step | Updates per epoch (n = 1,000) | Character |
|---|---|---|---|
| **batch GD** | all n | 1 | exact but slow; one big step per pass over the data |
| **mini-batch GD** | e.g. 32–512 | 32 (batch 32) | **the standard**: GPU-friendly, a little noise |
| **stochastic GD** | 1 | 1,000 | very noisy but very many cheap updates |

Fitting y = 3x + 2 for 50 epochs with each:
| Variant | Updates | (w, b) |
|---|---|---|
| batch | 50 | **(2.46, 1.99)**: still not there |
| mini-batch 32 | 1,600 | **(3.00, 2.01)** ✓ |
| stochastic | 50,000 | **(2.98, 2.00)**: jittering around the answer |

Mini-batches win because a gradient estimated from 32 random examples is almost as good as one from all of them (its noise shrinks like 1/√batch, from the CLT in chapter 04) but costs 1/30th as much. The noise even helps: it shakes the model out of saddle points and sharp minima, which tend to generalise worse.

---

## 5. Momentum

### The idea
Plain GD in a narrow valley zig-zags: it bounces across the steep walls while creeping along the flat floor. **Momentum** gives the descent inertia, like a heavy ball rolling downhill:
```
v ← β·v − η·∇L          (β ≈ 0.9: keep 90% of the previous velocity)
w ← w + v
```
- Across the valley, the gradients alternate in sign, so they **cancel** in v (less zig-zag).
- Along the valley, they keep the same sign, so they **accumulate** (faster progress).

Same problem, same learning rate 0.01:
| Method | Steps |
|---|---|
| plain GD | 798 |
| **momentum 0.9** | **269** (about 3× fewer) |

**Nesterov momentum** looks ahead (computes the gradient at w + βv) and is slightly better again.

---

## 6. Adaptive methods: RMSProp and Adam

### The problem
In `f = x² + 1000y²` the two directions have curvatures 2 and 2000. One learning rate can't suit both: it must be below 2/2000 for y, which makes x painfully slow. Plain GD takes **4,728** steps.

### RMSProp: a step size per parameter
Keep a running average of each parameter's **squared** gradient, and divide by its square root:
```
s ← 0.9·s + 0.1·g²          w ← w − η · g / (√s + ε)
```
Parameters with consistently big gradients get **smaller** steps, and small-gradient ones get **bigger** steps. Every direction moves at a similar pace: **80** steps.

### Adam: momentum + RMSProp (the default optimiser)
```
m ← β₁m + (1 − β₁)g          (momentum: running mean of gradients, β₁ = 0.9)
s ← β₂s + (1 − β₂)g²         (RMSProp: running mean of squared gradients, β₂ = 0.999)
m̂ = m/(1 − β₁ᵗ),  ŝ = s/(1 − β₂ᵗ)    (bias correction: m and s start at 0)
w ← w − η · m̂ / (√ŝ + ε)
```
It solves the same problem in **144** steps and works well across a huge range of problems with little tuning (η = 10⁻³ is a common default). **AdamW** (Adam with decoupled weight decay) is the standard for training transformers.

| Optimiser | Steps on x² + 1000y² |
|---|---|
| plain GD (largest safe η) | 4,728 |
| RMSProp | 80 |
| Adam | 144 |

(Exact counts depend on the settings; the point is the order of magnitude.)

---

## 7. Newton's method: using curvature

### The idea
Gradient descent uses only the slope. **Newton's method** also uses the curvature (the second derivative, or the Hessian H) to jump to the bottom of the local parabola:
```
1-D:  x ← x − f'(x) / f''(x)              n-D:  w ← w − H⁻¹ ∇L
```
- On a quadratic, **one step lands exactly on the minimum**: from (10, 1) on x² + 10y², a single step reaches (0, 0).
- Near a minimum, the error **squares** every step (quadratic convergence). For f(x) = x − ln x (minimum at x = 1), starting at 0.3:
  ```
  0.51000000   0.75990000   0.94235199   0.99667671   0.99998896
  ```
  The errors are 0.49, 0.24, 0.058, 0.0033, 0.000011: each is the square of the one before.

### Why deep learning doesn't use it
The Hessian of a model with n parameters has **n²** entries; for a billion parameters that's 10¹⁸ numbers, and inverting it costs O(n³). Practical compromises: **quasi-Newton** methods like L-BFGS (approximate H⁻¹ from recent gradients; great for small and medium models), and diagonal approximations. Adam's √s is, loosely, a cheap diagonal curvature estimate.

---

## 8. Constrained optimisation: Lagrange multipliers

### The problem
Minimise f(x) **subject to** a constraint g(x) = 0.

### The key insight
At the best point, you can't improve f by sliding along the constraint, so the gradient of f must be **perpendicular to the constraint**, i.e. **parallel to ∇g**:
```
∇f = λ ∇g          (λ is the "Lagrange multiplier")
```
Solve these equations together with g(x) = 0.

### Example 1: maximise xy subject to x + y = 10
∇(xy) = (y, x) and ∇(x + y) = (1, 1), so y = λ and x = λ, hence x = y; with x + y = 10, **x = y = 5**, xy = **25**. (A grid search agrees.) The square is the rectangle with the biggest area for a given perimeter.

### Example 2: the closest point to the origin on the line x + 2y = 5
Minimise x² + y²: (2x, 2y) = λ(1, 2), so y = 2x; substituting gives **(1, 2)** with λ = 2. Geometrically, the shortest path to a line meets it at a right angle.

### Inequality constraints: KKT
For constraints like g(x) ≤ 0, the **Karush–Kuhn–Tucker (KKT)** conditions generalise this: either the constraint is inactive (λ = 0, as if it weren't there) or it's tight (the solution sits on the boundary, λ ≥ 0). **Support Vector Machines** are derived this way: only the points on the margin get λ > 0, and those are the "support vectors".

---

## 9. Linear programming

### The problem
Maximise a **linear** objective subject to **linear** inequalities. A factory makes products x and y:
```
maximise   3x + 5y            (profit)
subject to x ≤ 4,   2y ≤ 12,   3x + 2y ≤ 18,   x, y ≥ 0
```
The constraints cut out a polygon of feasible plans (the **feasible region**).

### The key fact: the best answer is at a corner
A linear objective is a "tilted plane": moving across a flat region always improves things until you hit an edge, and then a corner. So check the **vertices**. The code intersects every pair of constraint lines, keeps the feasible points, and finds the best corner **(2, 6)** with profit **36**.

**Real solvers:** the **simplex method** walks from corner to better corner (fast in practice); **interior-point methods** go through the middle (polynomial time).

**Uses:** scheduling, routing, supply chains, airline crews, network flow, and as subroutines in combinatorial algorithms. If variables must be integers, it's **integer programming**, which is NP-hard in general (chapter 08).

---

## 10. Regularisation: ridge (L2) vs lasso (L1)

### Why regularise
Add a penalty on large weights to prevent overfitting ([chapter 05](../05-Statistics/README.md): this is MAP with a prior):
```
ridge (L2):  minimise |y − Xw|² + λ Σ wⱼ²
lasso (L1):  minimise |y − Xw|² + λ Σ |wⱼ|
```

### The experiment
6 features, but only the first two matter (true w = [4, −3, 0, 0, 0, 0]):
```
true  [4.00, -3.00,  0.00,  0.00,  0.00,  0.00]
ridge [2.98, -2.40,  0.05,  0.01, -0.15, -0.06]     all 6 non-zero, everything shrunk
lasso [3.89, -2.93,  0.00, -0.00, -0.00, -0.00]     useless features set exactly to 0
```

### Why L1 gives exact zeros
- The L2 penalty's slope (2λw) fades to nothing near 0, so there's no push to reach exactly 0.
- The L1 penalty's slope is a constant λ all the way to 0, so a small weight gets pushed all the way there and **stays** ("soft thresholding" in the code: `max(0, |ρ| − λ/2)`).

**Geometry:** the L1 constraint region is a diamond with corners on the axes, and the loss contours usually touch it at a corner, where some weights are 0.

| | Ridge (L2) | Lasso (L1) |
|---|---|---|
| effect | shrinks all weights | sets some weights to exactly 0 |
| use | many small effects; correlated features | **feature selection**, sparse models |
| prior (MAP) | Gaussian | Laplace |
| in deep learning | **weight decay** | sometimes, for sparsity |

**Elastic net** = both penalties together.

---

## 11. Saddle points and other traps

A **saddle point** has zero gradient but is a minimum in some directions and a maximum in others (the Hessian has eigenvalues of both signs; [chapter 03](../03-Calculus/README.md)).

`f(x, y) = x² − y² + y⁴/4` has a saddle at (0, 0) and minima at y = ±√2:
| Start | Result |
|---|---|
| exactly on the ridge (1, 0), plain GD | **stuck at (0, 0)**: the gradient in y is exactly 0 forever |
| same, plus tiny random noise (like SGD) | **escapes** to \|y\| = **1.4142** = √2 |

In high dimensions saddle points vastly outnumber local minima (for a critical point to be a minimum, *every* one of millions of directions must curve up). SGD noise and momentum help escape them.

**Other traps:**
- **plateaus**: near-zero gradients over a big flat area (sigmoid saturation);
- **exploding gradients**: fixed with gradient clipping;
- **vanishing gradients**: fixed with ReLU, residual connections and normalisation;
- **sharp vs flat minima**: flat minima tend to generalise better.

---

## 12. A practical recipe for training models

1. **Scale your features** (z-scores): this improves the condition number.
2. Start with **Adam/AdamW**, learning rate around 10⁻³ (smaller, like 10⁻⁴ or 10⁻⁵, for fine-tuning).
3. Use **mini-batches** (32–512).
4. Add a **warm-up** and a decay schedule (cosine).
5. If the loss explodes, **lower the learning rate** and add gradient clipping. If it's stuck, raise the learning rate or check for a bug (gradient check, [chapter 03](../03-Calculus/README.md)).
6. Regularise with **weight decay**, dropout and early stopping, guided by **validation** loss.

---

## 13. Exercises (answers below)

1. Is f(x) = |x| convex? Is f(x) = sin x?
2. For f(x) = 5x², what's the largest learning rate that doesn't diverge?
3. Why is mini-batch SGD preferred to full-batch gradient descent?
4. Explain in one sentence each what momentum and RMSProp fix.
5. Use Lagrange multipliers to maximise x + y subject to x² + y² = 2.
6. Why does L1 regularisation produce sparse weights and L2 doesn't?
7. Why isn't Newton's method used to train large neural networks?

<details>
<summary>Answers</summary>

1. |x|: yes (a V shape; every chord lies above it). sin x: no (it has many hills and valleys).
2. f'' = 10, so η < 2/10 = 0.2.
3. Each update is much cheaper; the gradient estimate is nearly as good; there are many more updates per epoch; the noise helps escape saddle points; and it fits GPU memory.
4. Momentum averages gradients over time, cancelling zig-zags across steep directions and speeding up along consistent ones. RMSProp scales each parameter's step by its recent gradient size, so steep and flat directions progress at similar speeds.
5. ∇(x + y) = (1, 1) = λ(2x, 2y), so x = y; then 2x² = 2 gives x = y = 1 (max, value 2) or x = y = −1 (min, value −2).
6. The L1 penalty's gradient stays at a constant λ no matter how small the weight, pushing it exactly to 0; L2's gradient 2λw vanishes as w → 0.
7. The Hessian has n² entries (impossible for billions of parameters) and inverting it costs O(n³); quasi-Newton or first-order methods are used instead.
</details>

---

## 14. Interview questions

**Q1. What happens if the learning rate is too high or too low?**
> Too high: each step overshoots and the loss oscillates or diverges (for a quadratic, any η > 2/L diverges). Too low: training is very slow and may stall on plateaus.

**Q2. Explain Adam.**
> It keeps an exponential moving average of gradients (momentum) and of squared gradients (RMSProp), bias-corrects both, and steps by m̂/(√ŝ + ε), giving each parameter an adaptive step size.

**Q3. SGD vs batch gradient descent?**
> Batch uses every example per step (exact but slow). SGD/mini-batch use a random subset: noisier but far cheaper per step, more updates per epoch, and the noise helps escape saddle points.

**Q4. What is a convex function, and why do we like convex losses?**
> One where every chord lies above the graph. Every local minimum is global, so gradient descent reliably finds the best solution.

**Q5. What are Lagrange multipliers?**
> A way to optimise subject to constraints: at the optimum, ∇f = λ∇g. The KKT conditions extend it to inequalities, and SVMs are derived from them.

**Q6. L1 vs L2 regularisation?**
> L2 shrinks all weights smoothly (Gaussian prior, weight decay); L1 drives some weights exactly to zero (Laplace prior), giving feature selection.

**Q7. Why do saddle points matter more than local minima in deep learning?**
> In high dimensions, a critical point is much more likely to curve up in some directions and down in others than to curve up in all of them; gradients near saddles are tiny, which slows training until noise or momentum pushes past.

---

**Next:** [07 · Information Theory](../07-Information-Theory/README.md): entropy, cross-entropy and KL divergence: what the loss functions are measuring.
