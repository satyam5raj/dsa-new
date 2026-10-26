# 05 · Statistics

| | |
|---|---|
| **Code** | [Statistics.java](Statistics.java): every example below (`java Statistics.java`, 46 checks) |
| **You need** | [04 · Probability](../04-Probability/README.md); [02 · Linear Algebra](../02-Linear-Algebra/README.md) for regression |
| **Used in** | training ML models (MLE = the loss function), evaluating them (metrics, cross-validation), A/B testing products, monitoring systems, data analysis |

**Probability** goes from a known model to data: "if the coin is fair, how likely are 60 heads?". **Statistics** goes the other way, from data to a model: "I saw 60 heads; is the coin fair?". Machine learning is statistics at scale: given data, find the model that explains it, and check that it works on data it hasn't seen.

---

## 1. Describing data

### Centre: mean vs median
| Measure | Definition | Strength / weakness |
|---|---|---|
| **mean** | the sum divided by the count | uses every value, but one outlier can drag it |
| **median** | the middle value when sorted | **robust**: ignores how extreme the extremes are |
| **mode** | the most common value | works for categories |

Salaries (in $1000s) of 9 employees and one CEO: `30 32 35 38 40 41 45 50 52 1000`
- mean = **136.3** (nobody earns anything like that);
- median = **40.5** (a typical salary);
- the mean without the CEO is 40.3.

Report the **median** for skewed data (incomes, house prices, latencies). For server latency, people report **percentiles**: p50 (the median), p95, p99.

### Spread: variance and standard deviation
For `2 4 4 4 5 5 7 9` (mean 5):
```
variance σ² = average squared distance from the mean = (9+1+1+1+0+0+4+16)/8 = 4.0
standard deviation σ = √4 = 2.0        (same units as the data)
```
**Sample variance** divides by **n − 1** instead: 32/7 = **4.5714** (section 2 explains why).

### Percentiles, quartiles, outliers
- **Q1** (25th percentile) = 4.0, **Q3** (75th) = 5.5, **IQR** = Q3 − Q1 = 1.5 (the spread of the middle half).
- **Tukey's outlier rule:** a value above Q3 + 1.5·IQR (= 7.75) or below Q1 − 1.5·IQR is an outlier. So 9 is an outlier.
- **z-score:** `z = (x − mean)/σ` = how many standard deviations from the mean. For 9: (9 − 5)/2 = **2.0**. |z| > 3 is unusual for normal data.

**ML connection: feature scaling.** Standardising each feature to z-scores (mean 0, std 1) makes gradient descent converge much faster and stops features with big units from dominating distance-based models (k-NN, k-means, SVM).

---

## 2. Estimators: guessing the truth from a sample

### Population vs sample
The **population** is everything (every user, every possible coin flip), with true parameters like μ and σ that we usually can't see. A **sample** is the data we have. An **estimator** is a formula that guesses a parameter from the sample, e.g. the sample mean x̄ for μ.

### Bias and variance of an estimator
- **Bias**: on average, is the guess too high or too low? (E[estimate] − truth)
- **Variance**: how much does the guess jump around between samples?

### Why the sample variance divides by n − 1
Dividing by n gives estimates that are **too small on average**: the sample mean is closer to the sample's own points than the true mean is. The code draws 200,000 samples of size 5 from data with true variance 4:
| Formula | Average estimate |
|---|---|
| divide by n | **3.19** (biased low; theory: 4 · 4/5 = 3.2) |
| divide by **n − 1** | **3.99** (unbiased) |

This is **Bessel's correction**: n − 1 is the number of "free" deviations once the mean is fixed.

### Standard error: how precise is the sample mean?
```
SE = σ / √n
```
For σ = 15 and n = 100, SE = **1.5** (the code's simulated spread: 1.506). It's the CLT from chapter 04: **4× more data halves the uncertainty**.

---

## 3. Maximum Likelihood Estimation (MLE): where loss functions come from

### The idea
Choose the parameters that make the **observed data most probable**.
```
θ̂ = argmax over θ of  P(data | θ)        "the likelihood"
```
In practice we maximise the **log-likelihood**: products become sums (no underflow, easier derivatives; chapter 00), and the maximum is in the same place.

### Example: a coin with 7 heads in 10 flips
```
L(p) = C(10,7) p⁷ (1 − p)³          log L(p) = 7 log p + 3 log(1 − p) + const
d/dp log L = 7/p − 3/(1 − p) = 0  →  p̂ = 7/10
```
The code searches p from 0.001 to 0.999 and finds **0.700**. At p = 0.7 the likelihood is **0.2668**; at the "fair" p = 0.5 it's only 0.1172.

### Gaussian data
For normally distributed data, the MLE of μ is the **sample mean** and the MLE of σ² is the variance divided by n. With 100,000 heights from N(170, 8²), the code recovers **170.01** and **7.99**.

### The big connection to ML
**Minimising a standard loss function is the same as maximising a likelihood:**
| Assume the noise / output is… | Maximising the likelihood = minimising… | Model |
|---|---|---|
| Gaussian | **mean squared error** | linear regression |
| Bernoulli (yes/no) | **binary cross-entropy (log loss)** | logistic regression |
| Categorical (K classes) | **cross-entropy** | softmax classifier, every LLM |

When you train a neural network with cross-entropy, you're doing MLE.

---

## 4. Bayesian estimation and MAP

### The Bayesian view
MLE gives one "best" number. Bayesian statistics gives a **whole distribution of belief** about the parameter:
```
posterior ∝ likelihood × prior
P(θ | data) ∝ P(data | θ) · P(θ)
```

### Example: coin flips with a Beta prior
The **Beta(α, β)** distribution describes beliefs about a probability; think of α − 1 as imaginary heads and β − 1 as imaginary tails seen before. Its magic: after seeing h heads and t tails, the posterior is just **Beta(α + h, β + t)** (a "conjugate prior").

Prior Beta(2, 2) ("probably around 0.5"), then data with 7 heads and 3 tails:
| Estimate | Value |
|---|---|
| posterior | **Beta(9, 5)** |
| posterior mean 9/14 | **0.6429** |
| **MAP** (the posterior's peak) (9−1)/(9+5−2) | **0.6667** |
| MLE (ignores the prior) | 0.7000 |

### Why priors matter for small data
After **one** flip that lands heads, MLE says p = **1.0** ("this coin always lands heads!"). The posterior mean is **0.6**, which is much more sensible. With 1,000 flips (700 heads), the posterior mean is **0.6992**: data overwhelms the prior.

### MAP = regularisation
MAP maximises likelihood × prior. Taking logs: **log-likelihood + log-prior**.
- A **Gaussian prior** on the weights adds −λ|w|²: that's **L2 regularisation** (ridge, weight decay).
- A **Laplace prior** adds −λ|w|₁: that's **L1 regularisation** (lasso).

So regularisation isn't a hack; it's a prior belief that weights should be small.

---

## 5. Confidence intervals

### What a 95% CI is
An interval computed from data such that, **if you repeated the experiment many times, 95% of the intervals would contain the true value**:
```
x̄ ± 1.96 · SE              (1.96 because 95% of a normal lies within ±1.96σ)
```
The code runs 20,000 experiments (n = 40 each), and **94.3%** of the intervals contain the true mean.

Why slightly under 95%? With n = 40, we estimated σ from the data, which adds uncertainty. The exact fix uses the **t-distribution** (multiplier 2.02 instead of 1.96 for n = 40). For large n they agree.

**A common misreading:** "there's a 95% probability the true mean is in *this* interval" is not the frequentist meaning. The 95% refers to the procedure. (A Bayesian **credible interval** does mean that.)

### Polls: margin of error
42% of 1,000 people say yes: margin = 1.96 · √(0.42 · 0.58 / 1000) = **3.06%**. So "42% ± 3%". That's why polls use about 1,000 people: 4,000 would only halve the margin.

---

## 6. Hypothesis testing

### The logic
1. **Null hypothesis H₀**: "nothing interesting" (the coin is fair; the new button changes nothing).
2. Compute how surprising the data would be **if H₀ were true**: the **p-value**.
3. If p < α (usually 0.05), **reject H₀**.

**p-value** = P(data at least this extreme | H₀ is true). It is **not** the probability that H₀ is true.

### Example: 60 heads in 100 flips
| Method | p-value |
|---|---|
| exact binomial (two-sided) | **0.0569** → not significant at 0.05 |
| normal approximation, z = (60 − 50)/5 = 2.0 | 0.0455 |

The approximation is close but crosses the 0.05 line. Use exact tests for small counts.

### A/B testing (what product teams do every day)
Button A: 200 conversions out of 2,000 (10%). Button B: 250 out of 2,000 (12.5%). Is B really better?
```
pooled rate p = 450/4000 = 0.1125
z = (0.125 − 0.10) / √(p(1−p)(1/2000 + 1/2000)) = 2.502         p-value = 0.0124
```
Significant at 0.05: B very likely is better.

### Permutation test: no formulas needed
If the button made no difference, the labels "A" and "B" are arbitrary. So **shuffle the labels** 20,000 times and count how often a gap as big as 2.5% appears by chance: **0.0136**, matching the formula. This works for any statistic (medians, revenue per user) without assumptions.

### Errors
| | H₀ really true | H₀ really false |
|---|---|---|
| reject H₀ | **Type I error** (false alarm), rate α | correct (**power**) |
| keep H₀ | correct | **Type II error** (miss), rate β |

### The multiple testing trap
Run 20 independent tests at α = 0.05 when nothing is going on: P(at least one false "discovery") = 1 − 0.95²⁰ = **64%**. Test 20 metrics in an A/B test and one will "win" by luck. Fixes: **Bonferroni** (use α/20 = 0.0025 per test), the false discovery rate (Benjamini–Hochberg), or one primary metric decided in advance.

---

## 7. Regression

### Simple linear regression
Fit y ≈ a + bx by least squares:
```
slope b = Cov(x, y) / Var(x)         intercept a = ȳ − b·x̄
```
For x = 1..5, y = 2, 4, 5, 4, 5: **slope 0.6, intercept 2.2**.

### R²: how much of the variation is explained?
```
R² = 1 − SS_residual / SS_total        (0 = no better than predicting the mean, 1 = perfect)
```
Here **R² = 0.6**: the line explains 60% of the variance. For simple linear regression, R² = correlation².

### Assumptions (when the formulas are trustworthy)
Linear relationship; independent errors; constant error variance; roughly normal errors (for the p-values). The matrix version for many features is the normal equations from [chapter 02](../02-Linear-Algebra/README.md): w = (XᵀX)⁻¹Xᵀy.

### Logistic regression
For yes/no outcomes, model the **probability** with a sigmoid: P(y = 1) = σ(w·x + b), fitted by MLE (cross-entropy). [Chapter 03](../03-Calculus/README.md) trained one from scratch.

---

## 8. The bias–variance trade-off

### The central problem of ML
A model can be wrong in two ways:
| | Underfitting (high **bias**) | Overfitting (high **variance**) |
|---|---|---|
| model | too simple | too flexible |
| training error | high | very low |
| test error | high | **high** |
| analogy | a student who didn't study | a student who memorised the answers without understanding |

```
expected test error = bias² + variance + irreducible noise
```

### The experiment (from the code)
10 noisy points from y = sin(πx), fitted with polynomials:
| Degree | Train error | Test error | Diagnosis |
|---|---|---|---|
| 1 (a line) | 0.335 | 0.265 | underfit: a line can't bend like a sine |
| **3** | 0.026 | **0.056** | about right |
| 9 | **0.000** | 0.467 | overfit: passes through every noisy point, wild in between |

Training error **always** falls as the model gets more flexible. **Test error is U-shaped.** That's why you must always measure on data the model didn't train on.

### Cross-validation: choosing the complexity
**k-fold cross-validation:** split the data into k parts; train on k − 1 and test on the remaining one; rotate, and average the k test errors. Choose the setting with the lowest average. On 40 points, 5-fold CV over degrees 1–12 picks **degree 3**.

### How to fight overfitting
- more data;
- a simpler model;
- **regularisation** (L1/L2, i.e. a prior; section 4);
- early stopping, dropout and data augmentation (deep learning);
- ensembles (random forests average away variance).

**Never tune on the test set.** Use train / validation / test splits: tune on validation, report on test once.

---

## 9. Evaluating classifiers

### The confusion matrix
A fraud model on 1,000 transactions (120 real frauds):
| | predicted fraud | predicted OK |
|---|---|---|
| **actually fraud** | TP = 80 | FN = 40 |
| **actually OK** | FP = 20 | TN = 860 |

| Metric | Formula | Value | Question it answers |
|---|---|---|---|
| accuracy | (TP+TN)/all | 0.94 | how often is it right? |
| **precision** | TP/(TP+FP) | **0.80** | when it flags fraud, how often is it right? |
| **recall** (sensitivity) | TP/(TP+FN) | **0.667** | of all frauds, how many did it catch? |
| **F1** | 2PR/(P+R) | **0.727** | a balance of the two (harmonic mean) |
| specificity | TN/(TN+FP) | 0.977 | of the good transactions, how many passed? |

### The accuracy trap
A useless model that **always says "OK"** scores **88% accuracy**, because 88% of transactions are fine, and catches **zero** frauds (recall 0). For imbalanced problems (fraud, disease, defects), report precision, recall, F1 and the precision-recall curve, not accuracy.

### The precision–recall trade-off
Lowering the decision threshold catches more fraud (recall ↑) but flags more good customers (precision ↓). Which matters more depends on the cost: missing cancer is worse than a false alarm; a spam filter that blocks real email is worse than letting some spam through. The **ROC curve** (true positive rate vs false positive rate over all thresholds) and its **AUC** summarise this.

---

## 10. Exercises (answers below)

1. Data: 3, 7, 7, 2, 9, 4. Find the mean, median and sample variance.
2. A coin gives 3 heads in 3 flips. What's the MLE of p? The posterior mean with a Beta(1, 1) (uniform) prior?
3. A sample of 64 has mean 100 and standard deviation 16. Give a 95% CI for the mean.
4. A test gives p = 0.03. What does this mean, and what does it NOT mean?
5. Train accuracy 99%, validation accuracy 70%. What's wrong, and name two fixes.
6. A cancer screen has precision 0.10 and recall 0.95. Interpret both numbers.
7. Why is minimising MSE the same as MLE under Gaussian noise?

<details>
<summary>Answers</summary>

1. Mean 32/6 ≈ 5.33; median (4 + 7)/2 = 5.5; sample variance = Σ(x − 5.33)²/5 = 37.33/5 ≈ 7.47.
2. MLE = 3/3 = 1.0. Posterior Beta(4, 1), mean 4/5 = 0.8.
3. SE = 16/√64 = 2, so 100 ± 1.96·2 = (96.08, 103.92).
4. If H₀ were true, data this extreme would happen 3% of the time. It does NOT mean "there's a 3% chance H₀ is true" or "a 97% chance the effect is real".
5. Overfitting. Fixes: more data, regularisation, a simpler model, early stopping, dropout, data augmentation.
6. 95% of cancers are caught (good recall), but only 10% of positive screens are actually cancer (many false alarms); follow-up tests are needed.
7. With y = f(x) + Gaussian noise, the log-likelihood is −Σ(y − f(x))²/(2σ²) + const, so maximising it means minimising the sum of squared errors.
</details>

---

## 11. Interview questions

**Q1. Explain the bias–variance trade-off.**
> Simple models have high bias (they miss the pattern); flexible models have high variance (they fit the noise). Test error = bias² + variance + noise, so you pick the complexity that minimises the sum, usually with cross-validation.

**Q2. What is MLE, and how does it relate to loss functions?**
> Choose the parameters that maximise the probability of the observed data. Gaussian noise gives MSE, Bernoulli outputs give log loss, categorical outputs give cross-entropy.

**Q3. What's the difference between MLE and MAP?**
> MAP also multiplies by a prior over the parameters. With a Gaussian prior it becomes L2-regularised MLE; with a Laplace prior, L1.

**Q4. How would you run an A/B test?**
> Pick one primary metric and the sample size up front (power analysis), randomise users, run for the planned time, then compute a two-proportion z-test or a permutation test. Don't peek repeatedly or test many metrics without correction.

**Q5. What is a p-value?**
> The probability of seeing data at least this extreme if the null hypothesis were true. Small means the data is surprising under H₀; it's not the probability that H₀ is true.

**Q6. When is accuracy a bad metric?**
> With class imbalance: predicting the majority class gets high accuracy but is useless. Use precision, recall, F1, PR-AUC or ROC-AUC.

**Q7. Why divide by n − 1 for the sample variance?**
> The sample mean is fitted to the data, so deviations from it are systematically smaller than from the true mean. Dividing by n − 1 corrects that bias.

---

**Next:** [06 · Optimisation](../06-Optimization/README.md): how models actually find the best parameters.
