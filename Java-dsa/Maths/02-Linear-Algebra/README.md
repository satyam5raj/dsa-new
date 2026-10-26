# 02 · Linear Algebra

| | |
|---|---|
| **Code** | [LinearAlgebra.java](LinearAlgebra.java): a small matrix library written from scratch, plus every example below (`java LinearAlgebra.java`, 64 checks) |
| **You need** | [00 · Foundations](../00-Foundations/README.md) |
| **Used in** | **all of ML**: data is a matrix, a model is matrices, training is matrix maths. Also graphics, search engines (PageRank), recommendation systems, compression, quantum computing |

If you learn one maths topic for ML, make it this one. A dataset is a matrix (rows = examples, columns = features). A neural-network layer is a matrix multiplication. An embedding is a vector. GPUs exist to multiply matrices fast.

**Best companion:** watch 3Blue1Brown's *Essence of Linear Algebra* on YouTube alongside this chapter. It shows the geometry described here.

---

## 1. Vectors

### Two ways to see a vector
A **vector** is a list of numbers: `a = [3, 4]`.

1. **As an arrow** from the origin to the point (3, 4): it has a direction and a length.
2. **As a data point**: a house with 3 bedrooms and 4 bathrooms; a word's embedding with 768 numbers; an image with 784 pixel values.

Both views matter. The arrow gives intuition; the list is what computers store (`double[]`).

### Basic operations
| Operation | Formula | Example (a = [3,4], b = [1,2]) | Picture |
|---|---|---|---|
| addition | add each component | a + b = [4, 6] | walk a, then walk b |
| scalar multiplication | multiply each component | 2a = [6, 8] | same direction, twice as long |
| subtraction | a − b | [2, 2] | the arrow from b to a |

### The dot product: the most important operation in ML
```
a · b = a₁b₁ + a₂b₂ + … + aₙbₙ                 [3,4] · [1,2] = 3·1 + 4·2 = 11
```
It's a single number that measures **how much two vectors point the same way**:

```
a · b = |a| · |b| · cos(θ)          (θ = the angle between them)
```
| Dot product | Angle | Meaning |
|---|---|---|
| large positive | small | point the same way (similar) |
| **0** | **90°** | **perpendicular** (unrelated) |
| negative | > 90° | point opposite ways |

Everywhere in ML:
- a neuron computes `w · x + b`: a weighted sum of its inputs;
- **attention** in transformers is dot products between query and key vectors;
- recommendation scores are user-vector · item-vector.

### Length (norm)
| Norm | Formula | [3, 4] | Where |
|---|---|---|---|
| **L2** (Euclidean) | √(Σ xᵢ²) = √(a · a) | **5** | ordinary distance, L2 regularisation |
| **L1** (Manhattan) | Σ |xᵢ| | 7 | taxi distance, L1 regularisation (lasso, sparsity) |
| **L∞** (max) | max |xᵢ| | 4 | worst-case error |

A **unit vector** has length 1: divide by the norm. [3,4]/5 = [0.6, 0.8].

### Cosine similarity: "how alike are these?"
```
cos(a, b) = (a · b) / (|a| · |b|)          between −1 and 1, ignores length
```
Toy word vectors with features [royalty, femininity, food]:
```
king  = [0.9, 0.1, 0.0]
queen = [0.9, 0.9, 0.0]
pizza = [0.0, 0.1, 0.9]
cos(king, queen) = 0.78    (related)
cos(king, pizza) = 0.01    (unrelated)
```
This is how semantic search and RAG retrieval find "documents with similar meaning": embed everything as vectors and compare cosines.

### Projection: the shadow of one vector on another
The projection of a onto b is the part of a pointing along b:
```
proj_b(a) = (a · b / b · b) · b               proj of [3,4] onto [1,0] = [3, 0]
```
The leftover `a − proj` is **perpendicular** to b (the code checks the dot product is 0). Least squares (section 10) is a projection.

---

## 2. Matrices

### What a matrix is
A **matrix** is a grid of numbers with m rows and n columns (an "m × n matrix"):
```
A = | 1  2 |        a 2 × 2 matrix
    | 3  4 |
```
Three ways to see it:
1. **A table of data**: rows = examples, columns = features (a dataset with 1,000 houses and 5 features is 1000 × 5).
2. **A stack of vectors** (rows or columns).
3. **A function that transforms space**: the most powerful view (below).

### Matrix × vector
Each output entry is a **row of the matrix dotted with the vector**:
```
| 1 2 3 |   | 1 |   | 1·1 + 2·2 + 3·3 |   | 14 |
| 4 5 6 | · | 2 | = | 4·1 + 5·2 + 6·3 | = | 32 |
            | 3 |
```
Sizes must match: an (m × **n**) matrix times an **n**-vector gives an m-vector. **A neural-network layer is exactly this**: `output = W · input + b`, where W has one row of weights per neuron.

### Matrix × matrix
`C = AB`: entry `C[i][j]` = (row i of A) · (column j of B). An (m × **n**) times (**n** × p) gives m × p. The code's triple loop is the definition.

**Order matters: AB ≠ BA in general.**
```
A = |1 2|   B = |0 1|      AB = |2 1|      BA = |3 4|
    |3 4|       |1 0|           |4 3|           |1 2|
```
(B swaps things: on the right it swaps A's columns, on the left it swaps A's rows.)

### Matrices as transformations
A matrix **moves every point in space**. Watch where the basis arrows (1,0) and (0,1) go; the columns of the matrix tell you:

| Matrix | What it does | Example |
|---|---|---|
| `[[2,0],[0,3]]` | stretch x by 2, y by 3 | (1,1) → (2,3) |
| `[[0,−1],[1,0]]` | rotate 90° anticlockwise | (1,0) → (0,1) |
| `[[1,0],[0,−1]]` | mirror in the x-axis | |
| `[[1,1],[0,1]]` | shear | |
| identity `I` | does nothing | `AI = IA = A` |

**Multiplying matrices = doing one transformation after another** (composition, from chapter 00). Rotating 90° twice = rotating 180°: `rot90 · rot90 = [[−1,0],[0,−1]]`. That's why order matters: "rotate then stretch" ≠ "stretch then rotate".

### Transpose
`Aᵀ` flips rows and columns: `(Aᵀ)[i][j] = A[j][i]`.
```
A = |1 2|    Aᵀ = |1 3|
    |3 4|         |2 4|
```
Rules: `(AB)ᵀ = BᵀAᵀ` (the order reverses). Dot product as matrices: `a · b = aᵀb`. A **symmetric** matrix has `A = Aᵀ` (covariance matrices, distance matrices, graph adjacency matrices).

---

## 3. Systems of linear equations

### The problem
```
 2x +  y −  z =   8
−3x −  y + 2z = −11          ⟺     A x = b
−2x +  y + 2z =  −3
```
Solving `Ax = b` is the core computation of linear algebra.

### Gaussian elimination (how computers solve it)
1. Use the first equation to **eliminate x** from the equations below it (subtract multiples of it).
2. Use the new second equation to eliminate y from the ones below it.
3. Now it's a triangle: solve the last equation for z, substitute up (**back substitution**).

The code solves this one: **x = 2, y = 3, z = −1**, and checks `A·x = b`. It also uses **partial pivoting** (always pick the largest available number as the pivot) because dividing by tiny numbers amplifies rounding errors (see chapter 09).

Cost: **O(n³)** for n equations.

### Three possible outcomes
| Outcome | Geometry (2 equations = 2 lines) | When |
|---|---|---|
| exactly one solution | the lines cross at one point | A is invertible |
| no solution | parallel lines | inconsistent equations |
| infinitely many | the same line twice | dependent equations |

### Rank: how much real information is there?
The **rank** of a matrix = the number of **independent** rows (equivalently, columns) = the number of non-zero pivots after elimination.
```
| 1 2 3 |
| 2 4 6 |     ← 2 × row 1: no new information        rank = 2
| 1 0 1 |
```
An n × n matrix is **invertible iff its rank is n** ("full rank"). Low rank means the data really lives in fewer dimensions, which is the idea behind compression and PCA (sections 8 and 11).

---

## 4. Vector spaces, span, independence, basis

These words describe **what you can build from a set of vectors**.

| Term | Meaning | Example |
|---|---|---|
| **linear combination** | c₁v₁ + c₂v₂ + … | 2·[1,0] + 3·[0,1] = [2,3] |
| **span** | everything you can build | span{[1,0],[0,1]} = the whole plane |
| **linearly independent** | no vector is a combination of the others | [1,0] and [0,1] ✓; [1,2] and [2,4] ✗ |
| **basis** | an independent set that spans the space | the standard basis {[1,0],[0,1]} |
| **dimension** | the number of vectors in any basis | the plane has dimension 2 |
| **subspace** | a "flat" piece through the origin, closed under + and scaling | a line or plane through 0 |

Two important subspaces of a matrix A:
- the **column space**: all possible outputs Ax (its dimension = the rank);
- the **null space**: all x with Ax = 0 (the directions A "squashes flat").

**Rank–nullity theorem:** rank + dim(null space) = number of columns.

**ML intuition:** 1,000-dimensional data often lies near a low-dimensional subspace. A face image has 10,000 pixels, but faces vary in far fewer ways. Finding that subspace is dimensionality reduction.

---

## 5. Determinants

### What the determinant means
The **determinant** of a square matrix is **how much it scales area** (in 2-D) or volume (in 3-D):

| Matrix | det | Meaning |
|---|---|---|
| stretch `[[2,0],[0,3]]` | **6** | areas become 6× bigger |
| rotation `[[0,−1],[1,0]]` | **1** | areas unchanged |
| `[[1,2],[2,4]]` | **0** | squashes the plane onto a line |
| negative det | | flips orientation (like a mirror) |

### Formulas
```
2 × 2:   det | a b |  = ad − bc          det [[3,1],[2,4]] = 3·4 − 1·2 = 10
             | c d |
```
For bigger matrices, eliminate to a triangle; the determinant is the **product of the pivots** (with a sign flip per row swap). That's what the code does: the 3 × 3 system above has det = −1.

### Key facts
- **det(A) = 0 ⟺ A is not invertible** ⟺ its rows are dependent ⟺ rank < n. (You can't undo a squash.)
- `det(AB) = det(A) · det(B)` (scale factors multiply).
- `det(Aᵀ) = det(A)`; `det(A⁻¹) = 1/det(A)`.

**ML use:** the multivariate Gaussian density has det(Σ) in its formula; normalising flows track how a transformation stretches probability volume (log-determinants of Jacobians).

---

## 6. Inverse matrices

The **inverse** A⁻¹ undoes A: `A·A⁻¹ = A⁻¹·A = I`. It exists iff det(A) ≠ 0.

```
2 × 2:  | a b |⁻¹ = 1/(ad − bc) · |  d −b |
        | c d |                   | −c  a |

| 4 7 |⁻¹ = 1/10 · |  6 −7 | = | 0.6 −0.7 |
| 2 6 |            | −2  4 |   |−0.2  0.4 |
```
The code computes it column by column (solving A·x = eᵢ) and checks `A·A⁻¹ = I`.

**Practical rule:** to solve Ax = b, **don't compute A⁻¹** and multiply. Solve directly with elimination (or a decomposition): it's faster and more accurate. You'll see `np.linalg.solve(A, b)` preferred over `inv(A) @ b`.

---

## 7. Eigenvalues and eigenvectors

### The idea
Most vectors change direction when you multiply them by A. **Eigenvectors are the special directions that don't**: A only stretches them, by a factor λ (the **eigenvalue**):
```
A v = λ v
```
For `A = [[2,1],[1,2]]`:
```
A·[1, 1]  = [3, 3]  = 3·[1, 1]     → eigenvector [1,1], eigenvalue 3
A·[1, −1] = [1, −1] = 1·[1, −1]    → eigenvector [1,−1], eigenvalue 1
A·[1, 0]  = [2, 1]                 → NOT a multiple of [1,0]: not an eigenvector
```
**Picture:** A stretches the plane by 3 along the diagonal and leaves the anti-diagonal alone. Eigenvectors are the "axes" of a transformation.

### Finding them
- **By hand (2 × 2):** solve `det(A − λI) = 0` (the **characteristic equation**). For the example: (2−λ)² − 1 = 0 → λ = 3 or 1.
- **Power iteration:** multiply a random vector by A again and again (normalising each time). It turns towards the eigenvector with the largest eigenvalue. The code gets λ = 3 and v = [0.7071, 0.7071]. **Google's PageRank is power iteration** on the web's link matrix.
- **Real libraries:** the QR algorithm, or Jacobi rotations for symmetric matrices (what the code uses).

### Useful facts
- **sum of eigenvalues = trace** (sum of the diagonal); **product of eigenvalues = det**. The code checks both on a 3 × 3 matrix (trace 12, det 43).
- **Symmetric matrices** (A = Aᵀ) have **real** eigenvalues and **perpendicular** eigenvectors. Covariance matrices are symmetric, which is why PCA works so cleanly.
- **Eigendecomposition:** A = V Λ V⁻¹ (Λ = the eigenvalues on a diagonal). Then **Aⁿ = V Λⁿ V⁻¹**: powers become easy.

### Why eigenvalues matter in CS and ML
| Where | What the eigenvalues tell you |
|---|---|
| Fibonacci | `[[1,1],[1,0]]ⁿ` contains F(n) (the code gets F(30) = 832,040); its largest eigenvalue is the golden ratio **1.618**, which is why F(n) grows like 1.618ⁿ |
| Markov chains / PageRank | the steady state is the eigenvector with eigenvalue 1 |
| PCA | eigenvectors of the covariance matrix = the main directions of variation |
| training stability | eigenvalues of the Hessian = curvature; huge ones force a small learning rate |
| RNNs | repeated multiplication by W explodes if the largest eigenvalue is > 1 and vanishes if it's < 1 (exploding/vanishing gradients) |
| graphs | eigenvalues of the graph Laplacian reveal clusters (spectral clustering, chapter 09) |

---

## 8. Orthogonality

### Orthogonal and orthonormal
- Vectors are **orthogonal** when their dot product is 0 (perpendicular).
- **Orthonormal** = orthogonal **and** each has length 1.
- A matrix Q whose columns are orthonormal satisfies **QᵀQ = I**, so **Q⁻¹ = Qᵀ** (inverting is free!).
- Q is a pure rotation or reflection: it **keeps lengths and angles**, `|Qx| = |x|`. Numerically it's the best-behaved kind of matrix.

### Gram–Schmidt: making vectors orthonormal
Take vectors one at a time; subtract their projections onto the ones already processed; normalise:
```
q₁ = v₁ / |v₁|
w₂ = v₂ − (v₂·q₁) q₁          q₂ = w₂ / |w₂|
w₃ = v₃ − (v₃·q₁) q₁ − (v₃·q₂) q₂ …
```
The code turns {[1,1,0], [1,0,1], [0,1,1]} into an orthonormal set and checks QᵀQ = I. This is the **QR decomposition** (A = QR), used for least squares and eigenvalue algorithms.

---

## 9. Matrix decompositions (factorisations)

Breaking a matrix into simpler pieces is like factoring a number: it reveals structure and makes computation fast.

| Decomposition | Form | Requires | Used for |
|---|---|---|---|
| **LU** | A = LU (lower × upper triangular) | square | solving Ax = b many times (it's Gaussian elimination, saved) |
| **QR** | A = QR (orthogonal × upper) | any | least squares, eigenvalue algorithms |
| **Eigen** | A = VΛV⁻¹ | square, enough eigenvectors | powers, Markov chains, PCA |
| **Cholesky** | A = LLᵀ | symmetric positive definite | fast solving, sampling Gaussians |
| **SVD** | A = UΣVᵀ | **any** matrix | everything below |

### SVD: the Swiss army knife
**Every** matrix (any shape) can be written as
```
A = U Σ Vᵀ
```
- **V**: an orthonormal set of input directions;
- **Σ**: a diagonal of **singular values** σ₁ ≥ σ₂ ≥ … ≥ 0 (how much A stretches each direction);
- **U**: an orthonormal set of output directions.

**Picture:** every matrix is **rotate → stretch along axes → rotate**. SVD tells you the rotations and the stretches.

**How to get it:** the singular values are the square roots of the eigenvalues of AᵀA. For `A = [[3,0],[4,5]]`: AᵀA = [[25,20],[20,25]], eigenvalues 45 and 5, so **σ = √45 = 6.7082 and √5 = 2.2361**. Their product is 15 = |det A| ✓.

### Low-rank approximation: the big idea
Keep only the top k singular values: `Aₖ = σ₁u₁v₁ᵀ + … + σₖuₖvₖᵀ`. **The Eckart–Young theorem:** this is the **best possible** rank-k approximation, and its error is exactly the dropped singular values.

The code uses a 4 × 3 "user × movie ratings" table:
```
          movie1 movie2 movie3
user1  [   5      4      1  ]       users 1-2 like movies 1-2
user2  [   4      4      1  ]
user3  [   1      1      5  ]       users 3-4 like movie 3
user4  [   2      1      4  ]
```
- singular values: **9.7185, 5.2982, 0.6924**;
- σ₁² + σ₂² + σ₃² = **123** = the sum of all squared entries (that's always true);
- the top direction alone keeps **77%** of that total;
- keeping 2 directions leaves an error of exactly **0.6924**, the dropped σ₃.

There are really two "tastes" here (action vs romance, say), and SVD finds them without being told. This is the idea behind the Netflix Prize recommenders, **LSA** for documents, **image compression**, noise removal, and **LoRA** (fine-tuning large models with low-rank weight updates).

---

## 10. Positive definite matrices

A symmetric matrix A is **positive definite** if `xᵀAx > 0` for every non-zero x. Equivalent: **all its eigenvalues are positive**.

| Matrix | Eigenvalues | xᵀAx |
|---|---|---|
| P = `[[2,−1],[−1,2]]` | **1, 3** | > 0 for all 1,000 random x tried |
| N = `[[1,2],[2,1]]` | **−1, 3** | negative for some x |

**Picture:** `f(x) = xᵀAx` is a bowl when A is positive definite (one lowest point), and a saddle when it has eigenvalues of both signs.

**Why ML cares:**
- **covariance matrices** are always positive semi-definite (eigenvalues ≥ 0);
- a function is convex near a point when its **Hessian** (chapter 03) is positive definite, which means that point is a true minimum, not a saddle;
- kernel matrices in SVMs must be positive semi-definite.

---

## 11. Least squares: linear algebra meets ML

### The problem
You have more equations than unknowns, and no exact solution: 200 noisy points (xᵢ, yᵢ) and you want the best line y = w₀ + w₁x.

Write it as `Xw ≈ y`, where each row of X is [1, xᵢ]. Find the w that minimises the squared error `|Xw − y|²`.

### The answer: the normal equations
```
XᵀX w = Xᵀy
```
**Why:** the best Xw is the **projection** of y onto the column space of X, so the error `y − Xw` must be **perpendicular** to every column: `Xᵀ(y − Xw) = 0`.

The code generates data from y = 1 + 2x + noise, solves the normal equations and recovers **intercept ≈ 1.0, slope ≈ 2.0**, and checks the residual is perpendicular (Xᵀr = 0). **This is linear regression**, the first ML model, and it's pure linear algebra.

---

## 12. PCA: finding the important directions

**Principal Component Analysis:**
1. Centre the data (subtract the mean).
2. Compute the **covariance matrix** C = XᵀX / (n − 1).
3. Its **eigenvectors** are the principal directions; its **eigenvalues** say how much variance each one captures.
4. Keep the top k directions and project the data onto them.

The code makes 5,000 points stretched along the diagonal: the top direction comes out as **(0.7, 0.7)**, within 1° of 45°, and it explains **97%** of the variance. So 2-D data is essentially 1-D.

Uses: visualising high-dimensional data, compressing features, removing noise. (PCA = SVD of the centred data matrix.)

---

## 13. Tensors

A **tensor** generalises the idea to more dimensions:

| Rank | Name | Example | Shape |
|---|---|---|---|
| 0 | scalar | a loss value | `()` |
| 1 | vector | an embedding | `(768,)` |
| 2 | matrix | a weight matrix, a grayscale image | `(784, 128)` |
| 3 | 3-D tensor | a colour image | `(3, 224, 224)` |
| 4 | 4-D tensor | a **batch** of colour images | `(32, 3, 224, 224)` |

A batch of 32 RGB images at 224 × 224 is **4,816,896 numbers ≈ 18.4 MB** in float32. That's why GPU memory matters.

### Cost of matrix multiplication
Multiplying two n × n matrices takes **n³** multiply-adds: for n = 1024, about **1.07 billion**. Training a large model is mostly this, which is why hardware is built for it (GPUs, TPUs), and why low-rank tricks (section 9) save so much.

---

## 14. Exercises (answers below)

1. a = [1, 2, 2], b = [2, 0, 1]. Compute a · b, |a|, and the cosine similarity.
2. Multiply `[[1,0],[2,1]] · [[3,1],[0,2]]`.
3. Find the determinant and inverse of `[[2,1],[5,3]]`.
4. Is {[1,2,3], [2,4,6], [0,1,0]} linearly independent? What's the rank?
5. Find the eigenvalues of `[[4,1],[2,3]]`.
6. A matrix has singular values 10, 3, 0.1. What's its rank? What's the error of its best rank-2 approximation?
7. Why is a rotation matrix's determinant 1, and what's its inverse?
8. A layer maps 512 inputs to 256 outputs. What shape is W, and how many weights (plus biases)?

<details>
<summary>Answers</summary>

1. a · b = 2 + 0 + 2 = 4; |a| = 3; |b| = √5; cos = 4/(3√5) ≈ 0.596.
2. `[[3,1],[6,4]]`.
3. det = 6 − 5 = 1; inverse = `[[3,−1],[−5,2]]`.
4. No: the second vector is 2 × the first. Rank 2.
5. (4−λ)(3−λ) − 2 = λ² − 7λ + 10 = 0 → λ = 5, 2 (check: sum 7 = trace, product 10 = det).
6. Rank 3 (three non-zero singular values); the rank-2 error is 0.1.
7. It keeps areas and orientation, so it scales area by 1. Its inverse is its transpose (rotate back).
8. W is 256 × 512: 131,072 weights + 256 biases = 131,328 parameters.
</details>

---

## 15. Interview questions

**Q1. What does a dot product tell you, and where is it used in ML?**
> How aligned two vectors are (|a||b|cos θ). It's every neuron's weighted sum, attention scores in transformers, and similarity search with embeddings.

**Q2. What is an eigenvector, intuitively?**
> A direction that a matrix only stretches (by its eigenvalue) without rotating. Eigenvectors are the natural axes of the transformation.

**Q3. Explain SVD and one application.**
> Any matrix factors as UΣVᵀ: rotate, stretch along axes by the singular values, rotate. Truncating to the top k singular values gives the best rank-k approximation, used for recommendation systems, compression, PCA, and LoRA fine-tuning.

**Q4. How does PCA work?**
> Centre the data, compute the covariance matrix, take its top eigenvectors (or the SVD of the data), and project onto them. The eigenvalues say how much variance each component keeps.

**Q5. Why not compute A⁻¹ to solve Ax = b?**
> It's slower and less accurate than solving directly via LU/QR factorisation, and it fails badly for nearly singular matrices.

**Q6. What does it mean for a matrix to be positive definite, and why care?**
> xᵀAx > 0 for all non-zero x (all eigenvalues positive). Covariance matrices are positive semi-definite, and a positive-definite Hessian means a strict local minimum.

**Q7. What's the rank of a matrix, and why does low rank matter?**
> The number of independent rows/columns. Low rank means the data has fewer real degrees of freedom, so it can be compressed or approximated cheaply.

---

**Next:** [03 · Calculus](../03-Calculus/README.md): derivatives, gradients and the chain rule: how models learn.
