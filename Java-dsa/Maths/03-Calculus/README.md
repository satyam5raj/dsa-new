# 03 · Calculus

| | |
|---|---|
| **Code** | [Calculus.java](Calculus.java): every example below, including backpropagation by hand and a neuron trained from scratch (`java Calculus.java`, 56 checks) |
| **You need** | [00 · Foundations](../00-Foundations/README.md); [02 · Linear Algebra](../02-Linear-Algebra/README.md) for the multivariable parts |
| **Used in** | **training every ML model** (gradients + gradient descent), backpropagation, optimisation, probability densities (integrals), physics and graphics simulations |

Calculus is the maths of **change**. Two big ideas:
- **derivatives**: how fast something changes right now (the slope);
- **integrals**: how much accumulates (the area).

For ML you mostly need derivatives, specifically **gradients** and the **chain rule**, because that's how models learn: measure how the error changes when you nudge each weight, then nudge the weights to reduce it.

---

## 1. Limits: the foundation

A **limit** is the value a function approaches as its input approaches some point, even if it never gets there.

```
(1 + 1/n)ⁿ   n = 10: 2.5937      n = 1,000,000: 2.7183      → e = 2.71828…
sin(x) / x   at x = 0.001: 1.0000                            → 1 as x → 0
```
`sin(0)/0` is "0/0", undefined, but the values **near** 0 settle on 1. Limits let us talk about "the slope at a single point", which is how derivatives are defined.

Another example: `(x² − 1)/(x − 1)` is undefined at x = 1, but it equals `x + 1` everywhere else, so the limit as x → 1 is **2**.

---

## 2. Derivatives: the slope at a point

### The idea
For a straight line, slope = rise / run. For a curve the slope keeps changing, so zoom in until the curve looks straight:

```
f'(x) = lim (h → 0)  [f(x + h) − f(x)] / h
```
"How much does the output change per unit of input change, right at this point?"

**Picture:** you're on a hill. The derivative is the steepness under your feet: positive going uphill, negative going downhill, **zero at the top or bottom**.

### Computing it numerically (what the code does)
Pick a tiny h and use the **central difference** `[f(x+h) − f(x−h)] / 2h` (more accurate than the one-sided version). At x = 3, f(x) = x² has slope **6.0000**. This "numerical derivative" is also how **gradient checking** works (section 8).

### The basic rules (so you don't need limits every time)
| Function | Derivative | Check in the code |
|---|---|---|
| constant `c` | 0 | d/dx 5 = 0 |
| `xⁿ` (power rule) | `n·xⁿ⁻¹` | x² → 2x (6 at x = 3); x³ → 3x² (12 at x = 2) |
| `eˣ` | `eˣ` (it's its own slope!) | at 1: 2.7183 = e |
| `ln x` | `1/x` | at 4: 0.25 |
| `sin x` | `cos x` | at 0: 1 |
| `cos x` | `−sin x` | |
| `aˣ` | `aˣ · ln a` | |

That `eˣ` is its own derivative is what makes `e` special, and why it appears everywhere in ML (sigmoid, softmax, Gaussians).

### Combining rules
| Rule | Formula | Example |
|---|---|---|
| sum | `(f + g)' = f' + g'` | (x² + x)' = 2x + 1 |
| constant multiple | `(c·f)' = c·f'` | (5x²)' = 10x |
| **product** | `(f·g)' = f'g + fg'` | (x² sin x)' = 2x sin x + x² cos x |
| quotient | `(f/g)' = (f'g − fg') / g²` | (sin x / x)' = (x cos x − sin x)/x² |
| **chain** | `(f(g(x)))' = f'(g(x)) · g'(x)` | (sin(x²))' = cos(x²) · 2x |

The code checks every one numerically at x = 1.3.

### The chain rule: the most important rule in ML
When functions are **composed** (one feeds into another), their derivatives **multiply**:
```
y = f(g(x))            dy/dx = (dy/dg) · (dg/dx)
```
**Intuition:** if g changes 3× as fast as x, and f changes 2× as fast as g, then f changes 6× as fast as x. Rates multiply along a chain of gears.

Examples:
- `e^(3x)`: outer eᵘ, inner u = 3x → `e^(3x) · 3`.
- `sin(x²)`: outer sin u, inner u = x² → `cos(x²) · 2x`.

A neural network is a long composition, so its derivative is a long chain-rule product. **Backpropagation is the chain rule**, applied efficiently (section 8).

---

## 3. Using derivatives to find minimums and maximums

### The key fact
At a **minimum or maximum** of a smooth function, the slope is **zero**: `f'(x) = 0`. (At the bottom of a valley the ground is flat.)

**Example:** `f(x) = (x − 3)² + 1`. Then `f'(x) = 2(x − 3)`, which is 0 at **x = 3**: the minimum.

### Is it a min, a max or neither? The second derivative
`f''(x)` is the slope of the slope, which tells you the **curvature**:
| f''(x) at the critical point | Shape | Meaning |
|---|---|---|
| > 0 | curves up, like a cup ∪ | **minimum** |
| < 0 | curves down, like a cap ∩ | **maximum** |
| = 0 | unclear | could be a **saddle**: x³ at 0 has f' = 0 but is neither |

For (x − 3)² + 1: f''(3) = **2 > 0**, so it's a minimum.

### A classic word problem
Cut squares of side x from the corners of a 12 × 12 sheet and fold up a box. Volume V(x) = x(12 − 2x)². Setting V'(x) = 0 gives **x = 2**, and the maximum volume is **128**. (The code also scans every x to confirm it.)

### Gradient descent in one dimension
When you can't solve f'(x) = 0 by algebra (true for every real ML model), **walk downhill**:
```
x ← x − learning_rate · f'(x)
```
If the slope is positive, step left; if it's negative, step right; the steps shrink as the slope flattens. From x = −5 with learning rate 0.1, the code reaches **x = 3.0000** in 100 steps. This one line is how every neural network is trained (chapter 06 goes deeper).

---

## 4. Integrals: accumulating area

### The idea
The **integral** `∫ₐᵇ f(x) dx` is the **area under the curve** from a to b. Compute it by slicing the area into thin strips and adding them up (a **Riemann sum**); as the strips get thinner, the sum approaches the exact area.

Area under x² from 0 to 3 (exact: **9**):
| Method | 1,000 strips |
|---|---|
| left rectangles | 8.9865 |
| trapezoids | 9.0000 |

### The Fundamental Theorem of Calculus
Integrals and derivatives are **opposites**:
1. If F'(x) = f(x), then `∫ₐᵇ f(x) dx = F(b) − F(a)`.
2. The derivative of "the area so far" is the function itself.

So to integrate x², find a function whose derivative is x²: **x³/3**. Then ∫₀³ x² dx = 27/3 − 0 = **9**. The code checks that the derivative of the running area t³/3 at t = 2 is f(2) = 4.

| Function | Antiderivative ∫ f dx |
|---|---|
| `xⁿ` (n ≠ −1) | `xⁿ⁺¹/(n+1)` |
| `1/x` | `ln |x|` |
| `eˣ` | `eˣ` |
| `sin x` | `−cos x` |

### Where integrals appear in CS and ML
- **Probability:** for a continuous random variable, probability = the area under the density curve, and the total area is 1. The bell curve e^(−x²/2) has total area **√(2π) ≈ 2.5066** (the code computes it), which is why the Gaussian formula divides by √(2π).
- **Expected values:** E[X] = ∫ x·p(x) dx (chapter 04).
- **Monte Carlo integration:** estimate an area by random sampling. Throw 1,000,000 random darts at a square: the fraction inside the quarter circle × 4 gives **π ≈ 3.14**. This is how high-dimensional integrals are computed in Bayesian ML, and in rendering (ray tracing).

---

## 5. Multivariable calculus: functions of many inputs

A model's loss depends on **millions of weights** at once, so we need derivatives of functions with many inputs.

### Partial derivatives
The **partial derivative** ∂f/∂x is the derivative with respect to x, **treating every other variable as a constant**.

```
f(x, y) = x² + 3xy + y²
∂f/∂x = 2x + 3y          (y is treated as a constant)
∂f/∂y = 3x + 2y
At (1, 2):  ∂f/∂x = 8,  ∂f/∂y = 7
```

### The gradient: the direction of steepest ascent
The **gradient** collects all the partial derivatives into a vector:
```
∇f = [∂f/∂x, ∂f/∂y] = [8, 7] at (1, 2)
```
Two facts make it the centrepiece of ML:
1. **∇f points in the direction where f increases fastest** (straight uphill). The code checks that a small step along the gradient raises f more than a step sideways.
2. Its **length** |∇f| = √(8² + 7²) = **10.6301** is how steep that direction is.

So **−∇f points downhill**, and gradient descent in many dimensions is:
```
weights ← weights − learning_rate · ∇Loss(weights)
```

**The directional derivative** in direction u (a unit vector) is `∇f · u`. Along the gradient itself it equals |∇f|; perpendicular to it, it's 0 (that's a contour line, where f stays level).

### The Jacobian: the derivative of a vector-valued function
If a function outputs a **vector**, F(x, y) = (x²y, x + y), its derivative is a **matrix** of all the partials:
```
J = | ∂F₁/∂x  ∂F₁/∂y |  =  | 2xy  x² |   at (2, 3):  | 12  4 |
    | ∂F₂/∂x  ∂F₂/∂y |     |  1    1 |               |  1  1 |
```
Each row is the gradient of one output. A neural-network layer maps a vector to a vector, so backprop multiplies Jacobians together.

### The Hessian: second derivatives (curvature)
The **Hessian** is the matrix of second partial derivatives:
```
H = | ∂²f/∂x²   ∂²f/∂x∂y |      for f = x² + 3xy + y²:  H = | 2  3 |
    | ∂²f/∂y∂x  ∂²f/∂y²  |                                  | 3  2 |
```
Its eigenvalues (chapter 02) describe the shape at a critical point:
| Hessian eigenvalues | Shape | Point |
|---|---|---|
| all > 0 (positive definite) | bowl | minimum |
| all < 0 | upside-down bowl | maximum |
| mixed signs | **saddle** | neither |

This H has eigenvalues 5 and −1, so (0, 0) is a **saddle**: f(1, 1) = 5 > 0 but f(1, −1) = −1 < 0. In deep learning, most critical points in high dimensions are saddles, not local minima.

---

## 6. Backpropagation: the chain rule on a computation graph

### The setup: one neuron
```
inputs x = (2, 1),  weights w = (0.5, −0.3),  bias b = 0.1,  target y = 1

z = w₁x₁ + w₂x₂ + b        (weighted sum)
a = σ(z) = 1/(1 + e⁻ᶻ)      (sigmoid activation)
L = (a − y)²               (squared-error loss)
```
We want ∂L/∂w₁, ∂L/∂w₂ and ∂L/∂b: how the loss changes if we nudge each parameter.

### Forward pass: compute the values
| Step | Value |
|---|---|
| z = 0.5·2 − 0.3·1 + 0.1 | **0.8** |
| a = σ(0.8) | **0.6900** |
| L = (0.69 − 1)² | **0.0961** |

### Backward pass: chain rule from the loss back to each weight
Go backwards, multiplying the local derivatives:
| Local derivative | Formula | Value |
|---|---|---|
| ∂L/∂a | 2(a − y) | −0.6200 |
| ∂a/∂z | σ(z)(1 − σ(z)) | 0.2139 |
| **∂L/∂z** | ∂L/∂a · ∂a/∂z | −0.1326 |
| ∂L/∂w₁ | ∂L/∂z · x₁ | **−0.2653** |
| ∂L/∂w₂ | ∂L/∂z · x₂ | −0.1326 |
| ∂L/∂b | ∂L/∂z · 1 | −0.1326 |

The negative gradient says: **increase** w₁ to reduce the loss (we want a bigger output, closer to 1).

**Why "back"-propagation?** ∂L/∂z is computed **once** and reused for every weight feeding into z. In a deep network, the backward pass reuses each layer's result for the layer before it, so all the millions of gradients cost about **as much as one forward pass**. Computing each gradient separately would be millions of times slower. This is **reverse-mode automatic differentiation**, which PyTorch and TensorFlow do for you.

### Gradient checking
To verify a hand-written backward pass, nudge a weight by a tiny ε and measure the loss change: `[L(w + ε) − L(w − ε)] / 2ε`. The code gets **−0.2653** both ways. Always gradient-check a new layer.

### Training: putting it together
The code trains this neuron to learn **OR** of two bits with gradient descent (2,000 steps): the loss drops from **0.2500** to below 0.01, and the predictions for 00, 01, 10, 11 become **0, 1, 1, 1**. That's **logistic regression**, built from nothing but derivatives.

---

## 7. Taylor series: approximating functions with polynomials

### The idea
Near a point a, any smooth function looks like a polynomial built from its derivatives:
```
f(x) ≈ f(a) + f'(a)(x − a) + f''(a)(x − a)²/2! + f'''(a)(x − a)³/3! + …
```
Each extra term matches one more derivative, so the fit improves.

| Function | Series around 0 |
|---|---|
| eˣ | 1 + x + x²/2! + x³/3! + … |
| sin x | x − x³/3! + x⁵/5! − … |
| cos x | 1 − x²/2! + x⁴/4! − … |
| ln(1 + x) | x − x²/2 + x³/3 − … |

e¹ with 2, 4 and 8 terms: **2.0000, 2.6667, 2.7183**. Near 0, sin x ≈ x (the error at 0.01 is below 2 × 10⁻⁷).

### Why ML cares
- **Gradient descent is a first-order Taylor approximation:** near the current weights, the loss is roughly `L(w) + ∇L · Δw`, so moving against the gradient decreases it, as long as the step is small enough for the approximation to hold. That's why the learning rate can't be too big.
- **Newton's method uses the second-order term** (the Hessian): it jumps straight to the minimum of the local parabola.
- Activation functions and the fast approximations GPUs use are often truncated series.

### Newton's method for solving equations
To solve g(t) = 0, follow the tangent line to where it crosses zero:
```
t ← t − g(t) / g'(t)
```
For √2, solve t² − 2 = 0 starting at t = 1:
```
1.5000000000   1.4166666667   1.4142156863   1.4142135624
```
The number of correct digits roughly **doubles** each step ("quadratic convergence"). This is how computers compute square roots and division.

---

## 8. Derivatives you'll see in ML

| Function | Formula | Derivative | Note |
|---|---|---|---|
| sigmoid | σ(z) = 1/(1 + e⁻ᶻ) | **σ(z)(1 − σ(z))** | at most **0.25** → gradients shrink through many layers (vanishing gradients) |
| tanh | (eᶻ − e⁻ᶻ)/(eᶻ + e⁻ᶻ) | **1 − tanh²(z)** | zero-centred; at most 1 |
| ReLU | max(0, z) | **1 if z > 0, else 0** | cheap; doesn't shrink gradients for z > 0 (why deep nets use it) |
| squared error | (a − y)² | 2(a − y) | regression |
| softmax + cross-entropy | −log softmax(z)ᵧ | **p − y** | classification |

### The beautiful softmax + cross-entropy gradient
For scores [2, 1, 0.1], softmax gives probabilities **[0.6590, 0.2424, 0.0986]**. If the true class is 0 (one-hot y = [1, 0, 0]), the gradient of the cross-entropy loss with respect to the scores is simply
```
p − y = [−0.3410, 0.2424, 0.0986]
```
The code confirms it numerically. "Predicted minus actual": this is why softmax and cross-entropy are always paired.

---

## 9. Exercises (answers below)

1. Differentiate: (a) 4x³ − 2x + 7 (b) e^(x²) (c) ln(3x + 1) (d) x·eˣ.
2. Find the minimum of f(x) = x² − 6x + 10. Is it a minimum or a maximum?
3. Compute ∫₀² (3x² + 1) dx.
4. f(x, y) = x²y + y³. Find ∇f at (1, 2).
5. For a = σ(z) and L = −log(a) with target 1, find dL/dz.
6. Why does a learning rate that's too large make gradient descent diverge?
7. Write the first three terms of the Taylor series of e^(−x) around 0.

<details>
<summary>Answers</summary>

1. (a) 12x² − 2 (b) 2x·e^(x²) (chain rule) (c) 3/(3x + 1) (d) eˣ + x·eˣ (product rule).
2. f'(x) = 2x − 6 = 0 → x = 3; f(3) = 1; f'' = 2 > 0, so it's a minimum.
3. [x³ + x]₀² = 8 + 2 = 10.
4. ∂f/∂x = 2xy = 4; ∂f/∂y = x² + 3y² = 13. ∇f = [4, 13].
5. dL/da = −1/a and da/dz = a(1 − a), so dL/dz = −(1 − a) = a − 1 (again "predicted minus actual").
6. The first-order (linear) approximation only holds for small steps; a big step overshoots the valley, lands higher on the other side, and each step overshoots more.
7. 1 − x + x²/2.
</details>

---

## 10. Interview questions

**Q1. What is a gradient, and why do we move against it?**
> The vector of partial derivatives; it points in the direction of steepest increase. Moving against it decreases the function fastest (locally), which is what minimising a loss needs.

**Q2. Explain backpropagation.**
> The chain rule applied from the loss backwards through the computation graph. Each node multiplies the incoming gradient by its local derivative and passes it on, reusing intermediate results, so all gradients cost about one extra pass.

**Q3. Why do sigmoid networks suffer from vanishing gradients, and how does ReLU help?**
> σ' ≤ 0.25, so the chain-rule product through many sigmoid layers shrinks exponentially. ReLU's derivative is 1 for positive inputs, so the product doesn't shrink there.

**Q4. What's the derivative of softmax + cross-entropy?**
> p − y: the predicted probabilities minus the one-hot label.

**Q5. What is a saddle point, and why does it matter in deep learning?**
> A point with zero gradient that's a minimum in some directions and a maximum in others (the Hessian has eigenvalues of both signs). In high dimensions most critical points are saddles, which gradient noise and momentum help escape.

**Q6. How would you check that a gradient implementation is correct?**
> Gradient checking: compare it with the finite difference [L(w + ε) − L(w − ε)] / 2ε for each parameter on a small example.

---

**Next:** [04 · Probability](../04-Probability/README.md): uncertainty, Bayes' theorem and distributions.
