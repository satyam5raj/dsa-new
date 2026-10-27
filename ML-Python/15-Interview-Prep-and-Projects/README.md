# 15 · Interview prep and projects

> Everything from chapters 01–14, organised the way interviews test it: the algorithms you must
> know, coding drills, rapid-fire theory, deep-learning questions, ML system design, and the
> three portfolio projects that prove you can do it end to end.

Run the coding drills: `../.venv/bin/python ml_coding_drills.py` → **20/20 checks**.

---

## 1. The 30 algorithms to know cold

For each one, be able to say: the **intuition**, the **objective/loss**, how it's **trained**,
**key hyperparameters**, **complexity**, **when to use it**, and **failure modes**.

| # | Algorithm | Family | Chapter |
|---|---|---|---|
| 1 | Linear regression (OLS, normal equation, GD) | regression | [03](../03-Regression/README.md) |
| 2 | Ridge / Lasso / Elastic net | regularised regression | [03](../03-Regression/README.md) |
| 3 | Logistic regression | classification | [04](../04-Classification/README.md) |
| 4 | K-nearest neighbours | instance-based | [04](../04-Classification/README.md) |
| 5 | Naive Bayes | generative | [04](../04-Classification/README.md) |
| 6 | LDA / QDA | generative | [04](../04-Classification/README.md) |
| 7 | Support vector machines (+ kernels) | margin-based | [04](../04-Classification/README.md) |
| 8 | Decision trees (Gini / entropy) | trees | [04](../04-Classification/README.md) |
| 9 | Random forest | bagging | [06](../06-Ensembles/README.md) |
| 10 | Gradient boosting (XGBoost / LightGBM / CatBoost) | boosting | [06](../06-Ensembles/README.md) |
| 11 | AdaBoost | boosting | [06](../06-Ensembles/README.md) |
| 12 | Stacking | ensembling | [06](../06-Ensembles/README.md) |
| 13 | K-Means (+ k-means++) | clustering | [05](../05-Unsupervised/README.md) |
| 14 | Hierarchical clustering | clustering | [05](../05-Unsupervised/README.md) |
| 15 | DBSCAN | density clustering | [05](../05-Unsupervised/README.md) |
| 16 | Gaussian mixture / EM | soft clustering | [05](../05-Unsupervised/README.md) |
| 17 | PCA / SVD | dimensionality reduction | [02](../02-Math-for-ML/README.md), [05](../05-Unsupervised/README.md) |
| 18 | t-SNE / UMAP | visualisation | [05](../05-Unsupervised/README.md) |
| 19 | Isolation Forest / LOF / One-Class SVM | anomaly detection | [05](../05-Unsupervised/README.md), [13](../13-TimeSeries-RecSys-Anomaly-RL/README.md) |
| 20 | Apriori / association rules | pattern mining | [05](../05-Unsupervised/README.md) |
| 21 | Multilayer perceptron + backprop | neural nets | [08](../08-Neural-Networks-from-Scratch/README.md) |
| 22 | SGD / Momentum / Adam / AdamW | optimisation | [08](../08-Neural-Networks-from-Scratch/README.md) |
| 23 | CNN | deep learning | [10](../10-Deep-Learning-Architectures/README.md) |
| 24 | RNN / LSTM / GRU | deep learning | [10](../10-Deep-Learning-Architectures/README.md) |
| 25 | Autoencoder / VAE | representation, generative | [10](../10-Deep-Learning-Architectures/README.md) |
| 26 | Transformer / attention | deep learning | [11](../11-Transformers-and-LLMs/README.md) |
| 27 | LoRA / fine-tuning / distillation | adaptation | [12](../12-Fine-Tuning-LoRA-HF/README.md) |
| 28 | Matrix factorisation / ALS / two-tower | recommenders | [13](../13-TimeSeries-RecSys-Anomaly-RL/README.md) |
| 29 | ARIMA / exponential smoothing / Holt–Winters | time series | [13](../13-TimeSeries-RecSys-Anomaly-RL/README.md) |
| 30 | Q-learning / policy gradient / PPO | reinforcement learning | [13](../13-TimeSeries-RecSys-Anomaly-RL/README.md) |

**More algorithms**, each implemented from scratch with a detailed explanation, are in Part 2:

- [16](../16-Classical-Algorithms-from-Scratch/README.md): SVM, kernels, LDA, CART, XGBoost-style
  boosting, DBSCAN, EM, Isolation Forest, Apriori, t-SNE.
- [17](../17-Probabilistic-Models/README.md): HMMs, Kalman filter, Gaussian processes, MCMC,
  Bayesian optimisation.
- [18](../18-Representation-and-LLM-Inference/README.md): word2vec, contrastive learning/CLIP, beam
  search, speculative decoding, MoE.
- [19](../19-Explainability-Experiments-Causal/README.md): SHAP, A/B testing, causal inference,
  uplift, conformal prediction, semi-supervised and active learning.

**Quick-compare cheat sheet:**

| Question | Answer |
|---|---|
| Needs feature scaling | KNN, K-Means, SVM, PCA, regularised linear models, neural nets |
| Doesn't | trees, random forest, gradient boosting |
| Handles missing values natively | XGBoost / LightGBM / HistGradientBoosting |
| Interpretable | linear/logistic (coefficients), shallow trees, GAMs |
| Best default for tabular data | gradient boosting; then random forest; logistic regression as a baseline |
| Best default for images / text / audio | pretrained deep nets (CNN/ViT, transformers), fine-tuned |
| Calibrated probabilities out of the box | logistic regression (mostly). Naive Bayes and boosting usually need calibration |
| Training cost | KNN zero (but slow prediction); SVM `O(n²)`+; trees `O(n log n · d)` |

## 2. Round 1: ML coding

Typical prompt: *"Implement X from scratch in NumPy. You have 30–45 minutes."*

| Drill | Key idea checked |
|---|---|
| D1 linear regression (GD) | equals sklearn's `LinearRegression` |
| D2 logistic regression | the `Xᵀ(p − y)/n` gradient; equals sklearn |
| D3 K-Means | assign/update loop; same inertia as sklearn |
| D4 KNN | `‖a−b‖² = ‖a‖² − 2a·b + ‖b‖²`, `argpartition`; equals sklearn |
| D5 ROC-AUC | **rank-based (Mann–Whitney) with tie handling**, O(n log n); equals sklearn |
| D6 stratified split | per-class sampling; disjoint, complete |
| D7 decision tree | recursive Gini splits on midpoints; same root-split quality as sklearn |
| D8 PCA | eigen-decomposition of the covariance; equals sklearn up to sign |
| D9 softmax cross-entropy | stable softmax; gradient `(p − onehot)/n` checked numerically |
| D10 attention (causal) | equals `torch.scaled_dot_product_attention` |
| D11 IoU + NMS | object-detection post-processing |
| D12 TF-IDF + cosine top-k | equals sklearn's `TfidfVectorizer` |

> **D7 surprise:** on iris, petal length and petal width both separate setosa perfectly at the
> root. My tree picked feature 2 and sklearn picked feature 3, because sklearn **shuffles features**
> before searching for splits. Both have the same weighted Gini (0.333). The final accuracies
> differ (0.933 vs 0.956) because the subtrees grow differently after the tie. Ties are common;
> "different from the library" isn't automatically "wrong".

**How to do well in an ML coding round:**

1. Restate the problem; ask about input shapes, edge cases (empty clusters, ties, k > n) and the
   expected complexity.
2. Write the simplest correct version first, **vectorised** (no Python loops over samples if you
   can avoid them).
3. Talk through shapes as you go: "X is (n, d), w is (d,), so `X @ w` is (n,)".
4. Test on a tiny example you can verify by hand, then a sanity check (loss decreasing, an AUC of
   0.5 for random scores).
5. Mention production concerns: numerical stability (subtract the max in softmax, clip the log),
   memory (`argpartition` instead of a full sort, chunking), and how you'd scale it.

## 3. Round 2: ML theory (rapid fire)

<details><summary><b>Bias–variance, overfitting, regularisation</b></summary>

- **Bias–variance trade-off?** Error = bias² + variance + noise. Simple models underfit (high
  bias); complex ones overfit (high variance). Diagnose with train vs validation curves (ch. 03).
- **How do you reduce overfitting?** More data, augmentation, regularisation (L1/L2, dropout,
  early stopping), a simpler model, ensembles (bagging), and better features. Then check for
  leakage (ch. 07).
- **L1 vs L2?** L1 gives sparse weights (feature selection; the diamond-shaped constraint); L2
  shrinks smoothly and handles collinearity (ch. 03).
- **Why does early stopping regularise?** It limits how far the weights travel from their small
  initial values, similar to L2.
</details>

<details><summary><b>Evaluation and metrics</b></summary>

- **Precision vs recall, and when each matters?** Ch. 04 §4. Spam filter → precision; cancer
  screening or fraud → recall.
- **ROC-AUC vs PR-AUC?** PR-AUC for rare positives. ROC hides false positives when negatives are
  plentiful (ch. 04 §6: 0.949 vs 0.542).
- **How do you choose a threshold?** From business costs on a validation set:
  `p > C_FP / (C_FP + C_FN)` for calibrated p (ch. 04).
- **Why cross-validate? Which kind?** For a lower-variance estimate. Stratified for
  classification, group for repeated entities, time-series split for temporal data (ch. 07).
- **What is calibration and when does it matter?** Predicted 0.8 should mean 80 % positive. It
  matters for pricing, risk, expected-cost decisions and combining models (ch. 04 §7).
- **MAE vs RMSE?** RMSE penalises large errors more and is the Gaussian MLE; MAE is robust and is
  the Laplace MLE (ch. 03).
</details>

<details><summary><b>Data</b></summary>

- **Data leakage examples?** Five, measured, in ch. 07 §7.
- **Handling missing values?** MCAR/MAR/MNAR; impute inside the pipeline; add missing indicators
  (ch. 07 §1).
- **Class imbalance?** The right metric, class weights or resampling on the training fold only,
  threshold tuning, calibration (ch. 04 §6).
- **High-cardinality categoricals?** Cross-fitted target encoding, hashing, embeddings (ch. 07 §2).
- **Correlated features: problem?** For linear models: unstable coefficients (use ridge). For
  trees: split importance between features (use permutation importance or SHAP, ch. 03).
</details>

<details><summary><b>Algorithms</b></summary>

- **Random forest vs gradient boosting?** Parallel deep trees reduce variance; sequential shallow
  trees reduce bias (ch. 06).
- **Why do random forests rarely overfit as trees are added?** Averaging more trees only reduces
  variance; the error plateaus at `ρσ²` (ch. 06 §1).
- **How does an SVM work, and what is the kernel trick?** Maximum margin; similarity in a feature
  space without building it (ch. 04 §5).
- **K-Means assumptions and how to choose k?** Spherical, similar-size clusters, scaled features;
  elbow and silhouette (ch. 05).
- **PCA in one minute.** Ch. 02 §3 and ch. 05 §6.
- **Generative vs discriminative?** Ch. 04 interview questions.
</details>

## 4. Round 3: deep learning

<details><summary><b>Training</b></summary>

- **Explain backprop.** The chain rule backwards with cached activations; cost ≈ one forward pass
  (ch. 08 §3).
- **Vanishing/exploding gradients: causes and fixes?** ReLU, He init, normalisation, residuals,
  gating, clipping. Ch. 08 measured a sigmoid first-layer gradient 3.7e-8 times the last layer's.
- **Adam vs AdamW? SGD vs Adam?** Ch. 08 §7. AdamW decouples weight decay. SGD + momentum can
  generalise better for CNNs; Adam converges faster with less tuning.
- **Why warm up the learning rate?** Ch. 09 §6.
- **BatchNorm vs LayerNorm?** Ch. 08 §9.
- **Dropout at inference?** It's off, thanks to inverted-dropout scaling. Forgetting `eval()` is
  a classic bug (ch. 09).
- **Your loss is NaN. What do you check?** The learning rate (too high), log of 0 or division by
  0 (use the fused loss on logits), bad inputs (NaNs in the data), exploding gradients (clip),
  fp16 overflow (bf16 or loss scaling).
- **The first loss should be?** `ln(num_classes)`. Ch. 11 caught a bad initialisation with
  exactly this check (45.8 instead of 2.7).
</details>

<details><summary><b>Architectures</b></summary>

- **Why CNNs for images?** Locality, weight sharing, translation equivariance (ch. 10 §1–2).
- **LSTM vs RNN?** The additive cell state and gates; initialise the forget bias positive
  (ch. 10 §3).
- **Self-attention, complexity, why scale by √d?** Ch. 11 §1.
- **Encoder vs decoder transformers?** Ch. 11 §5.
- **Positional encodings, and what is RoPE?** Ch. 11 §3.
- **KV cache: what and why?** Ch. 11 §7.
- **VAE vs GAN vs diffusion?** Ch. 10 interview questions.
- **LoRA, QLoRA, DPO vs RLHF?** Ch. 12.
- **How would you reduce inference latency?** Distillation, quantisation, pruning, batching, KV
  caching, speculative decoding, compiled kernels, a smaller context.
</details>

## 5. Round 4: ML system design

### The framework (use it out loud)

1. **Clarify the goal:** the business objective → an ML objective → metrics. Offline: PR-AUC,
   NDCG… Online: conversion, losses prevented, latency SLOs. Ask about scale, latency budget,
   labels, constraints (privacy, fairness, explainability).
2. **Data:** sources, labels (how, how delayed, how noisy), sampling, the class balance,
   point-in-time correctness.
3. **Features:** user, item, context, aggregates (velocity windows), embeddings. Where are they
   computed (batch vs streaming)? A feature store.
4. **Model:** start with a baseline (rules or logistic regression), then the main model (gradient
   boosting for tabular data, two-tower + ranker for recsys, fine-tuned transformers for text),
   and a training-data strategy.
5. **Evaluation:** an offline split mirroring production (time-based), slices, calibration; then
   online A/B tests with guardrail metrics.
6. **Serving:** online vs batch, latency budget, caching, fallbacks, shadow → canary.
7. **Monitoring and iteration:** drift, prediction distributions, delayed labels, alerts,
   retraining, feedback loops (ch. 14).

### Case: real-time payment fraud detection

| Step | Design |
|---|---|
| Goal | minimise fraud losses + manual-review costs + customer friction. Metric: expected cost, recall at a fixed review budget, PR-AUC. Latency < 100 ms |
| Labels | chargebacks (arrive 30–90 days later), manual-review outcomes (fast but biased), customer disputes |
| Features | amount vs the user's typical amount, **velocity** (counts and sums in 1 min / 1 h / 24 h), device and IP reputation, merchant risk, geo-distance from the last transaction, account age, graph features (shared cards/devices, a GNN, ch. 10 §7) |
| Model | rules for known patterns + gradient boosting on tabular features; an anomaly score for novel patterns; a calibrated output |
| Decision | three bands: approve / step-up auth (OTP) / block or review. Thresholds from costs (ch. 04 §6) |
| Training | time-based splits; handle label delay (train on mature data only); class weights; feedback-loop bias (blocked transactions never get labels, so keep a small randomised holdout) |
| Monitoring | prediction distribution, PSI on key features, review-queue precision, chargeback rate. **The degradation scenario is ch. 14 §7** |

### Case: recommendations (feed, products, videos)

- **Two-stage:** candidate generation (two-tower embeddings + ANN, co-visitation, trending)
  → ranking (a GBM or deep model predicting watch time / purchase) → re-ranking (diversity,
  freshness, business rules). See ch. 13 §3.
- **Objective:** a weighted mix (click, watch time, purchase, long-term retention). Watch for
  clickbait.
- **Cold start, position bias, exploration** (bandits), offline–online metric mismatch.

### Case: search ranking

- Query understanding (spelling, intent, embeddings) → retrieval (BM25 + dense vectors, hybrid)
  → learning-to-rank (LambdaMART, cross-encoders) with features: text match, quality, freshness,
  personalisation, CTR.
- Labels from clicks (position-debiased) and human relevance ratings. Metrics: NDCG@10, MRR;
  online: successful sessions.

### Case: an LLM-powered support assistant

Covered in depth in [Java-dsa/AI-Interviews](../../Java-dsa/AI-Interviews/README.md):

- RAG over the help centre;
- guardrails;
- evaluation (faithfulness, answer relevance);
- an LLM gateway;
- cost and latency;
- escalation to humans.

The ML-side additions are fine-tuning a small model for intent routing (ch. 12) and monitoring
answer quality with sampled human review.

### Case: ETA / demand forecasting

- Time-based features, holidays and events; gradient boosting or deep sequence models; **quantile
  forecasts** for planning (P50 and P90).
- Walk-forward backtests (ch. 13 §2); hierarchical reconciliation (city → zone).
- Monitor error by segment and horizon.

## 6. Three portfolio projects

Each project should end as: a clean repo (README with problem, data, approach, results and
limitations), reproducible training (pinned environment, seeds, tracked runs), an evaluation
report, and a deployed or deployable artefact. Interviewers ask "what went wrong and what did you
learn?", so keep notes.

### Project 1: fraud detection (classical ML + MLOps)

| | |
|---|---|
| Data | Kaggle "Credit Card Fraud Detection" (284k transactions, 0.17 % fraud), or the IEEE-CIS fraud data (richer features) |
| Must show | EDA of the imbalance; a time-based split; baselines (always-legit, logistic regression); gradient boosting; **PR-AUC**, recall at a fixed precision; **cost-based threshold** (ch. 04 §6); calibration; SHAP explanations |
| Production | a FastAPI service + Docker (ch. 14 files), input validation, prediction logging, a PSI drift report, a retraining script, a model card |
| Stretch | velocity features from raw events (SQL window functions, ch. 01), simulated concept drift and recovery (ch. 14 §7) |

### Project 2: CNN image classifier (deep learning)

| | |
|---|---|
| Data | CIFAR-10 to start; then a real problem (plant disease, defect detection, X-rays) |
| Must show | (1) a small CNN from scratch; (2) **transfer learning** from a pretrained ResNet / EfficientNet / ViT (`torchvision.models`); augmentation; a LR schedule with warm-up; early stopping (ch. 09); a confusion matrix; error analysis of misclassified images |
| Experiments | frozen backbone vs full fine-tune (ch. 12 §1), augmentation on/off, model size vs accuracy vs latency |
| Production | export (TorchScript/ONNX), int8 quantisation and accuracy impact, a small demo app (Gradio/Streamlit), batch-inference throughput |
| Stretch | Grad-CAM visualisations, test-time augmentation, robustness to shifts and noise (ch. 10 §2) |

### Project 3: fine-tune a small language model (LLMs)

| | |
|---|---|
| Model | a 0.5B–3B instruct model (Qwen, Llama, Gemma, Phi, SmolLM), on a free Colab/Kaggle GPU with QLoRA |
| Task | something measurable: support-ticket classification and routing, structured JSON extraction from emails, SQL generation, a domain Q&A style |
| Must show | a **baseline first** (prompting with few-shot examples, ch. 12 decision order); clean SFT data with the chat template; **loss masking** on prompts (ch. 12 §5); LoRA/QLoRA hyperparameters; an **evaluation set built before training** (exact match / F1 / JSON validity / LLM-as-judge with spot checks) |
| Experiments | rank r, data size (does 500 examples vs 5,000 matter?), base vs instruct starting point, a DPO round on preference pairs |
| Production | merge the adapter, quantise (GGUF/AWQ), serve with vLLM or llama.cpp, measure tokens/s and cost per 1k requests |
| Stretch | distil from a large model's outputs (ch. 12 §7); compare with a RAG approach (AI-Interviews 04) |

## 7. Final checklist before an interview

- [ ] I can derive the gradients of linear and logistic regression and softmax cross-entropy on a
      whiteboard.
- [ ] I can explain bias–variance, regularisation, cross-validation and leakage with examples.
- [ ] I know which metric to pick for imbalanced data, and how to set a threshold from costs.
- [ ] I can compare RF vs GBM, K-Means vs GMM vs DBSCAN, PCA vs t-SNE in two sentences each.
- [ ] I can explain backprop, Adam/AdamW, BatchNorm/LayerNorm and vanishing gradients.
- [ ] I can explain attention, the causal mask, the KV cache, LoRA, and DPO vs RLHF.
- [ ] I can run a system-design answer through the 7-step framework in ~35 minutes.
- [ ] I have a story for each project: the goal, the hardest problem, a mistake, the result, what
      I'd do next.
- [ ] I've re-run `./run_all.sh` and can explain every `PASS` line in at least five chapters.

---

**Next:** [16 · Classical algorithms from scratch](../16-Classical-Algorithms-from-Scratch/README.md) · **Back to:** [Course overview](../README.md)
