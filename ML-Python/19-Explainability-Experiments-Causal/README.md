# 19 · Explainability, experiments, causal inference, uncertainty and few labels

> A model that predicts well is only part of the job. You also have to explain **why** it
> predicts what it does, prove a change **actually helped** (A/B tests), estimate what an action
> **causes** (not just what correlates), give **trustworthy intervals**, and cope when **labels are
> scarce**. These are the questions senior ML interviews and real stakeholders ask.

Run it: `../.venv/bin/python explain_experiment_causal.py` → **28/28 checks** (~23 s).

---

## 1. Permutation importance (and why impurity importance misleads)

**Permutation importance:** shuffle one feature's column in **held-out** data and measure how much
the score drops. Shuffling keeps the feature's distribution but breaks its link to the target.
It's model-agnostic.

```python
Xp[:, j] = rng.permutation(Xp[:, j])
importance_j = score(X) − score(Xp)        # averaged over 10 shuffles
```

The setup: a strong signal, a small real effect, and a **pure-noise ID column with 1,000 distinct
values** (like a user ID).

| Feature | Impurity importance (RF built-in) | Permutation importance (held out) |
|---|---|---|
| signal | 0.669 | 0.344 |
| **id_like (noise)** | **0.141** | **0.002** |
| small effect | 0.190 | 0.039 |

- **Impurity importance gives pure noise 14 % of the credit.** It's computed on training data, and
  a feature with many distinct values offers many split points, so deep trees use it to memorise.
- Permutation importance on **held-out** data correctly gives it ~0, and the scratch version ranks
  the features the same way as sklearn's `permutation_importance`.
- **Caveat:** with **correlated** features, permuting one leaves its twin carrying the
  information, so both look unimportant. Permute correlated groups together, or use conditional
  methods.

## 2. Shapley values (exact)

**Question:** how much did each feature contribute to *this one prediction*?

**Game theory's answer:** average each feature's marginal contribution over every order in which
features could be "added":

```
φⱼ = Σ_{S ⊆ others}  |S|! (d−|S|−1)! / d!  ·  [ v(S ∪ {j}) − v(S) ]
v(S) = mean prediction with the features in S fixed to x and the rest drawn from background data
```

The scratch version enumerates all 2ᵈ subsets and is checked against the Shapley axioms:

| Property | Check |
|---|---|
| **linear model** | `φⱼ = wⱼ (xⱼ − mean xⱼ)` exactly |
| **efficiency** | base value + Σφ = the prediction (2.5) |
| **dummy** | a feature the model ignores gets exactly **0** |
| **symmetry** | for `f = x₀ · x₁` at (1, 1): φ = [0.377, 0.382]; the interaction's credit splits equally (exact 0.375 each, plus sampling noise in the background) |

**Cost:** 2ᵈ coalitions. d = 4 needs 16; d = 30 needs 1,073,741,824. Hence:

- **KernelSHAP** (a weighted linear regression over sampled coalitions; model-agnostic);
- **TreeSHAP** (exact, polynomial time, for tree ensembles; `shap.TreeExplainer`).

SHAP values are what interviewers mean by "explain this fraud score to a reviewer or a regulator".

## 3. Partial dependence and LIME

**Partial dependence (global shape):** set feature j to a value g for **every** row, average the
predictions, and repeat over a grid. The result shows how the prediction responds to that feature
on average.

- On data with true effect `2 sin(x₀)`, the PD of a gradient-boosting model follows `2 sin` (corr
  > 0.98): `[0.11, 0.42, 1.78, 3.39]` at x₀ = `[−2.4, −0.6, 0.0, 1.8]`. (The values are offset by
  the other features' average contribution.)
- It equals sklearn's `partial_dependence(method="brute")` exactly.
- sklearn's default `recursion` method for boosting has the same shape but a **constant offset**
  (−1.52 here), because it excludes the model's initial prediction. Compare shapes, not levels.
- **ICE** curves plot one line per row instead of the average, which reveals interactions the
  average hides.

**LIME (local):** perturb around one point, weight the samples by closeness, fit a **linear**
surrogate:

- at x = (0, 2, 0) the local slopes are **[1.81, 1.94, −0.01]**;
- the true local gradient is `[2cos(0) = 2, x₁ = 2, 0]`.

The weakness is instability: different kernel widths or random perturbations can give different
explanations.

## 4. A/B testing

### Sample size before you start

To detect a lift from **10 % → 11 %** with 80 % power at α = 0.05 (two-sided):

```
n per arm = (z_{α/2} + z_β)² · [p₀(1−p₀) + p₁(1−p₁)] / δ²  =  14,749
```

Small effects on small baselines need a lot of traffic. That's why companies care about variance
reduction (CUPED: regress out the pre-experiment metric).

### The test does what it promises, if you follow the rules

| Simulation (2,000 experiments of 14,749 per arm) | Result |
|---|---|
| no real effect: false-positive rate | **0.050** (= α) |
| real +1 point: power | **0.800** |

### The peeking problem

Check the p-value after every 1,000 users and stop at the first p < 0.05, **with no real effect**:
the **false-positive rate is 0.212**, four times the promised 5 %. Every look is another chance for
noise to cross the line.

Fixes:

- fix the sample size up front and look once;
- group-sequential designs with corrected boundaries (O'Brien–Fleming);
- always-valid inference (mSPRT);
- Bayesian decision rules with a pre-registered loss.

### Bayesian A/B

Beta(1 + conversions, 1 + failures) posteriors for each arm; sample them and compute P(B > A).

- With 10.0 % vs 10.8 % on 10,000 users each: **P(B > A) = 0.968**, expected lift 0.0080.
- It agrees with the frequentist one-sided p-value here, but answers the question stakeholders
  actually ask: "how likely is B better, and by how much?"

**Other A/B pitfalls:**

- **sample-ratio mismatch** (a 50/50 split arriving as 52/48 means a bug);
- novelty effects;
- network effects (users influence each other, so randomise by cluster);
- testing many metrics (multiple comparisons, chapter 02);
- Simpson's paradox across segments.

## 5. Causal inference: correlation is not causation

**Scenario:** engaged customers are both **more likely to get a coupon** (marketing targets them)
and **more likely to buy anyway**. Engagement is a **confounder**. The true average effect of the
coupon is +0.0746.

| Estimate | Value |
|---|---|
| naive: buy rate(coupon) − buy rate(no coupon) | **0.2239**, 3× too big: it measures "engaged people buy more" |
| **regression adjustment** (outcome ~ treatment + confounders) | **0.0773** |
| **inverse propensity weighting** | **0.0741** |

**IPW:**

1. Model the **propensity** `e(x) = P(treated | x)`.
2. Weight each treated person by `1/e(x)` and each untreated person by `1/(1 − e(x))`.
3. That re-creates a population where treatment is independent of the confounders, like a
   randomised trial:

```
ATE = mean( T·Y / e(X) ) − mean( (1−T)·Y / (1 − e(X)) )
```

**Assumptions** (these are what interviews probe):

1. **No unmeasured confounding:** everything that drives both treatment and outcome is in X.
   That's untestable, and the biggest risk.
2. **Overlap:** every type of customer has some chance of each treatment. Here propensities range
   **0.002–0.996**, so some weights are ~500, which makes IPW noisy. Trim or clip the extremes, or
   use **doubly robust** estimators (AIPW), which combine both methods and are consistent if either
   model is right.
3. **No interference** between units.

When you can, **randomise** (§4): it removes confounding by design. Other tools when you can't:
difference-in-differences, regression discontinuity, instrumental variables, synthetic control.

## 6. Uplift modelling: who should get the treatment?

The ATE tells you whether a coupon works **on average**. Marketing needs to know **for whom**. In
this simulation the coupon adds +0.05 for everyone, plus +0.08 for older customers.

**T-learner:** train one model on the treated group and one on the control group (from a
**randomised** experiment). Predicted uplift = `m₁(x) − m₀(x)`.

| | True uplift |
|---|---|
| everyone | 0.0747 |
| top 30 % by predicted uplift | **0.1197** (the best possible is ~0.13) |

**87 %** of the targeted group is older customers, against 31 % of all customers.

> Getting there needed **60,000** customers and very smooth models (`min_samples_leaf=500`). With
> 20,000 customers and small leaves, the targeted uplift was only 0.095. Uplift is a *difference of
> two noisy predictions*, so it needs much more data than either prediction alone.

Better learners: **X-learner**, **DR-learner**, causal forests (`econml`, `causalml`). Evaluate
with **Qini/uplift curves** on a held-out randomised set.

## 7. Conformal prediction: intervals with a guarantee

**Split conformal (regression):**

1. Train any model on the training split.
2. On a **calibration** split, compute the scores `|y − ŷ|`.
3. `q` = the ⌈(n+1)·0.9⌉/n quantile of those scores.
4. Interval for a new point: `ŷ ± q`.

**Guarantee:** if the data is exchangeable (no drift), coverage is at least 90 %, **whatever the
model or the data distribution**.

| Method | Coverage (overall) | Low-noise region | High-noise region |
|---|---|---|---|
| split conformal, constant width ± 2.33 | **0.902** | 1.00 | **0.76** |
| conformalised quantile regression (CQR) | **0.899** | 0.91 | 0.90 |

- The guarantee is **marginal** (on average over all points). A constant width over-covers easy
  regions and under-covers hard ones (noise grows with x here).
- **CQR** conformalises a quantile-regression interval (5 % and 95 % gradient-boosting models), so
  the widths **adapt**: coverage is now ~0.90 everywhere.
- For classification, conformal methods output **prediction sets** (e.g. {cat, dog} when unsure)
  with guaranteed coverage. This is useful for triage and human review, and increasingly for LLM
  outputs.

## 8. Few labels: semi-supervised and active learning

### Label propagation

Build a similarity graph (RBF kernel) and let the labels **flow along it**:

```
S = D^−½ W D^−½;   F ← α S F + (1 − α) Y      (α = 0.99: mostly listen to neighbours, keep a little of the original labels)
```

On two moons with **only 2 labelled points out of 400**:

| | Accuracy |
|---|---|
| logistic regression on the 2 labels | 0.780 |
| **label propagation** | **0.995** (matches sklearn's `LabelSpreading`) |

**The kernel width is everything:** σ = 0.05 → 0.995, 0.1 → 0.995, 0.2 → 0.930, **0.3 → 0.853**.
Too wide a kernel connects the two moons across the gap, and the labels leak. This is the
**cluster assumption**: semi-supervised learning works when the classes are separated by
low-density regions. Modern versions are pseudo-labelling and consistency regularisation
(FixMatch).

### Active learning

You can afford 40 labels. **Which** 40? **Uncertainty sampling:** repeatedly train, then ask a
human to label the pool point the model is least sure about (p closest to 0.5).

| Strategy (40 labels, mean of 5 runs) | Test accuracy |
|---|---|
| random labelling | 0.891 |
| **uncertainty sampling** | **0.916** |

Labels near the decision boundary carry the most information.

Other strategies:

- query-by-committee (where models disagree);
- expected model change;
- diversity sampling (so you don't pick 40 near-identical points).

The risk is that uncertainty sampling can fixate on noisy or outlier regions, so mix in some random
picks.

---

## Exercises

1. A churn model's top SHAP feature is "number of support calls". Can the product team conclude
   that reducing support calls reduces churn?
2. Your A/B test hit p = 0.04 on day 3 of a planned 14-day test. Ship?
3. Why does IPW blow up when propensity scores are near 0 or 1, and what do you do?
4. Conformal prediction guarantees 90 % coverage. Under what condition does that guarantee break?

<details><summary>Answers</summary>

1. No. SHAP explains the **model**, not the world. Support calls are probably a *symptom* of
   problems (confounded by product issues). Estimate causal effects with an experiment, or causal
   methods with explicit assumptions.
2. No: that's peeking. Early significance is often noise (the peeking simulation had a 21 % false
   positive rate). Run to the planned sample size, or use a sequential design with corrected
   boundaries. Also check for novelty effects and sample-ratio mismatch.
3. The weights are `1/e` or `1/(1 − e)`: a few units get enormous weight, so the variance explodes,
   and it signals poor overlap (some people are almost never treated, so there is nothing to
   compare them with). Trim or clip the propensities, restrict to the overlap region, use
   stabilised weights or doubly robust estimators, and report the population you actually estimated
   for.
4. When the calibration and test data aren't exchangeable: **distribution shift**, time trends,
   feedback loops. Use weighted or adaptive conformal methods, recalibrate on recent data, and
   monitor coverage in production (chapter 14).
</details>

## Interview questions

**Q: How would you explain a model's prediction to a non-technical stakeholder?** Use local
explanations: SHAP values for the top contributing features ("this transaction's score is high
mainly because of a new device and an unusual amount"). Back them with global views (permutation
importance, partial dependence), keep explanations faithful to the model, and say clearly that they
describe the model's reasoning, not causation.

**Q: Design an A/B test for a new ranking model.** Pick the primary metric and guardrails (latency,
revenue, complaints); randomise by user; compute the sample size from the minimum detectable effect
and variance (use CUPED); fix the duration (full weekly cycles); check sample-ratio mismatch; analyse
once at the end (or use a sequential design); look at segments with multiple-comparison care; watch
for novelty effects and interference.

**Q: Correlation vs causation in ML: where does it bite?** Models learn correlations, so acting on
them can fail when the correlation was confounded ("people who get coupons buy more"). Use
randomised experiments, or causal methods (regression adjustment, IPW/AIPW, diff-in-diff,
instrumental variables) with explicit assumptions, and uplift models for targeting decisions.

**Q: What is conformal prediction?** A wrapper that turns any model's predictions into intervals or
sets with guaranteed marginal coverage under exchangeability, using a held-out calibration set's
errors. CQR or normalised scores make the intervals adaptive.

**Q: You have 1 million unlabelled images and budget for 2,000 labels. What do you do?**
Self-supervised pretraining on all the images (chapter 12 §1, chapter 18 §2); label an initial
diverse sample; train; use active learning to choose the next labels (uncertainty plus diversity);
use semi-supervised methods (pseudo-labelling, FixMatch); validate on a held-out labelled set chosen
at random, not by the active learner.

---

**Back to:** [Course overview](../README.md) · [15 · Interview prep](../15-Interview-Prep-and-Projects/README.md)
