# ML-Python: machine learning from basics to advanced, in runnable Python

A complete **ML engineer / deep-learning engineer** course: classical ML, deep learning,
transformers and LLMs, fine-tuning, specialised domains, MLOps and interview prep.

Every chapter is two things:

- a **README** that explains it simply: intuition first, then the maths, then the code, the
  pitfalls and interview questions;
- a **self-checking Python program** whose real output is the source of every number in the
  README.

```bash
cd ML-Python
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt   # once (~1 GB with PyTorch)
.venv/bin/python 04-Classification/classification.py                 # one chapter
./run_all.sh                                                          # everything: 15 programs, 467 checks, ~70 s on a laptop CPU
```

The stack is NumPy, pandas, scikit-learn, SciPy and PyTorch. No GPU or internet needed; every
dataset is synthetic or bundled with scikit-learn.

## Chapters

| # | Chapter | You'll be able to… | Checks |
|---|---|---|---|
| 00 | [Roadmap and how to study](00-Roadmap-and-How-to-Study/README.md) | pick your track (classical / DL / LLM / MLOps), follow a 6-month plan | — |
| 01 | [Python and data fundamentals](01-Python-Data-Fundamentals/README.md) | vectorise with NumPy, clean data with pandas, write SQL window functions, build a leak-free sklearn pipeline | 28 |
| 02 | [Maths for ML](02-Math-for-ML/README.md) | use vectors, SVD/PCA, gradients and the chain rule, Bayes, MLE, bootstrap, entropy and KL | 43 |
| 03 | [Regression](03-Regression/README.md) | code linear regression two ways, choose MAE/RMSE/R², explain bias–variance, ridge vs lasso, compare 10 regressors | 31 |
| 04 | [Classification](04-Classification/README.md) | code logistic regression, KNN, naive Bayes and metrics from scratch; handle imbalanced fraud; set cost-based thresholds; calibrate | 42 |
| 05 | [Unsupervised learning](05-Unsupervised/README.md) | K-Means(++), choose k, hierarchical, DBSCAN, GMM, PCA vs t-SNE, Isolation Forest, association rules | 28 |
| 06 | [Ensembles](06-Ensembles/README.md) | explain why averaging works, code bagging, gradient boosting and AdaBoost from scratch, stack models | 21 |
| 07 | [Feature engineering and evaluation](07-Feature-Engineering-and-Evaluation/README.md) | impute, encode, scale, engineer features, pick the right CV, tune, and catch **five kinds of leakage** | 37 |
| 08 | [Neural networks from scratch](08-Neural-Networks-from-Scratch/README.md) | write backprop in NumPy, gradient-check it, initialise properly, code SGD/Momentum/Adam/AdamW, dropout, BatchNorm | 39 |
| 09 | [PyTorch training](09-PyTorch-Training/README.md) | tensors, autograd, `nn.Module`, `DataLoader`, a real training loop with warm-up + cosine, clipping, early stopping, checkpoints | 30 |
| 10 | [Deep-learning architectures](10-Deep-Learning-Architectures/README.md) | CNNs, RNN/LSTM, autoencoders, VAE, GAN, GNN and diffusion, each trained and tested | 29 |
| 11 | [Transformers and LLMs](11-Transformers-and-LLMs/README.md) | attention, masks, multi-head, positions, BPE, a tiny GPT, decoding, the KV cache, scaling arithmetic | 37 |
| 12 | [Fine-tuning, LoRA and alignment](12-Fine-Tuning-LoRA-HF/README.md) | transfer learning, LoRA from scratch, forgetting, SFT masking, quantisation, distillation, reward models, DPO (+ Hugging Face code) | 24 |
| 13 | [Time series, recommenders, anomalies, RL](13-TimeSeries-RecSys-Anomaly-RL/README.md) | backtest forecasts, matrix factorisation and NDCG, robust anomaly detection, bandits, Q-learning, policy gradients and PPO | 29 |
| 14 | [MLOps and production](14-MLOps-Production/README.md) | track experiments, gate and roll back models, serve over HTTP, catch train/serve skew, measure drift, handle the **degrading fraud model** | 29 |
| 15 | [Interview prep and projects](15-Interview-Prep-and-Projects/README.md) | the 30 algorithms, 12 coding drills, theory/DL Q&A, a system-design framework + cases, 3 portfolio project blueprints | 20 |

## Honest lessons the self-checks caught

While building this course, several of my first guesses were wrong. The checks caught them, and
the corrected results became some of the best lessons:

- **Joins duplicate rows silently:** a "missing" customer ID existed after all (ch. 01).
- **Random-forest feature subsampling can hurt** when half the features are noise (ch. 06).
- **Selection bias:** on pure noise, the best of 90 tuned configs scored 0.61 (ch. 07).
- **An LSTM with default init forgot a 40-step dependency;** a forget-gate bias of 3 fixed it on
  every seed (ch. 10).
- **A GAN mode-collapsed** with 1-D noise (ch. 10).
- **The tiny GPT's first loss was 45.8 instead of ln 15 = 2.7:** the embedding initialisation was
  too large (ch. 11).
- **Narrow supervised pretraining transferred worse than training from scratch;** broad
  self-supervised pretraining transferred well (ch. 12).
- **DPO's β only matters when preferences disagree** (ch. 12).
- **A MASE bug** produced absurd values (ch. 13).
- **UCB1 over-explored** and Thompson sampling won (ch. 13).
- **Input drift was invisible** while the fraud model collapsed, but the prediction distribution
  showed it (ch. 14).

## Related material in this repo

- [Java-dsa/Maths](../Java-dsa/Maths/README.md): the full maths course (linear algebra, calculus,
  probability, statistics, optimisation, information theory).
- [Java-dsa/AI-Interviews](../Java-dsa/AI-Interviews/README.md): LLM application engineering:
  RAG, agents, multi-agent systems, evaluation, guardrails, LLM gateways, deployment.
- [Java-dsa/HLD](../Java-dsa/HLD) and [Java-dsa/LLD](../Java-dsa/LLD): system design, which you'll need for ML system-design rounds.

## Notes

- Libraries evolve. Snippets for packages this course doesn't install (XGBoost, LightGBM,
  `transformers`, `peft`, `trl`, MLflow, FastAPI) are marked; check their current docs before
  copying.
- Timings (latency, speed-ups) depend on your machine; the self-checks only test robust
  relationships, never exact milliseconds.
