# 00 · Roadmap and how to study this course

> **Goal:** go from "I can write Python" to "I can build, train, evaluate, deploy and *explain*
> ML and deep-learning systems in an interview".

Every chapter has two parts:

| Part | What it is |
|---|---|
| `README.md` | The explanation: intuition first, then the maths, then code walkthroughs, pitfalls, interview Q&A |
| `*.py` | A runnable, **self-checking** program. Every number quoted in the README comes from running it |

```bash
cd ML-Python
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt   # once
.venv/bin/python 03-Regression/regression.py                          # one chapter
./run_all.sh                                                          # every chapter
```

Each script prints `PASS`/`FAIL` lines. If you change something and a check fails, the label tells
you which idea you broke. That is the fastest way to learn: **change a number, predict, re-run.**

---

## 1. The map

```
            ┌─────────────────────────── FOUNDATIONS ───────────────────────────┐
            │ 01 Python, NumPy, pandas, SQL, EDA       02 Maths for ML          │
            └───────────────────────────────┬───────────────────────────────────┘
                                            ▼
            ┌──────────────────────── CLASSICAL ML ─────────────────────────────┐
            │ 03 Regression   04 Classification   05 Unsupervised   06 Ensembles│
            │ 07 Feature engineering, validation, tuning, leakage               │
            └───────────────────────────────┬───────────────────────────────────┘
                                            ▼
            ┌──────────────────────── DEEP LEARNING ────────────────────────────┐
            │ 08 Neural nets from scratch (NumPy)   09 PyTorch training         │
            │ 10 CNN / RNN / LSTM / autoencoders / VAE / GAN / GNN / diffusion  │
            │ 11 Transformers and LLMs        12 Fine-tuning, LoRA, DPO         │
            └───────────────────────────────┬───────────────────────────────────┘
                                            ▼
            ┌────────────────────── SPECIALISED + PRODUCTION ───────────────────┐
            │ 13 Time series, recommenders, anomaly detection, RL               │
            │ 14 MLOps: tracking, registry, serving, drift, monitoring          │
            │ 15 Interview prep: 30 algorithms, system design, 3 projects       │
            └───────────────────────────────────────────────────────────────────┘
```

## 2. What "ML engineer" means: four tracks

The title covers different jobs. Pick your track, then weight the chapters.

| Track | Day-to-day | Weight these chapters |
|---|---|---|
| **A. Classical ML / data scientist** | tabular data, churn, fraud, pricing, A/B tests | 01–07, 13, 14 |
| **B. Deep learning engineer** | vision, speech, sequence models, training at scale | 08–11, 14 |
| **C. LLM / GenAI engineer** | fine-tuning, RAG, agents, evaluation, serving | 09, 11, 12, 14 + `Java-dsa/AI-Interviews` |
| **D. ML platform / MLOps** | pipelines, feature stores, serving, monitoring | 07, 14, 15 + `HLD/` |

Everyone needs 01–04, 07 and 14. Interviews for **every** track ask about bias/variance, metrics,
leakage, and "how would you deploy and monitor this?".

## 3. A six-month plan (about 10–12 hours a week)

| Month | Chapters | Output to show |
|---|---|---|
| 1 | 01, 02, 03 | You can derive linear regression's gradient and code it in NumPy |
| 2 | 04, 05, 06, 07 | **Project 1: fraud detection** on imbalanced data with threshold tuning |
| 3 | 08, 09 | A NumPy MLP with a passing gradient check; the same in PyTorch |
| 4 | 10, 11 | **Project 2: CNN image classifier**; a tiny GPT that generates text |
| 5 | 12, 13 | **Project 3: fine-tune a small LM with LoRA**; a walk-forward backtest |
| 6 | 14, 15 | Serve one project behind an API with drift monitoring; mock interviews |

Weekly rhythm: **2 sessions of reading + running**, **1 session changing the code**,
**1 session explaining it out loud** (record yourself answering the chapter's interview questions).

## 4. How to study one chapter

1. Read the **intuition** section only. Close the file. Explain it to an imaginary friend.
2. Run the script. For every `PASS` line, find the line of code that produced it.
3. Break something on purpose (remove scaling, change a learning rate, add leakage) and predict
   what fails *before* you re-run.
4. Do the exercises, then open the `<details>` answers.
5. Answer the interview questions out loud in under 90 seconds each.

## 5. Resources (all free unless marked)

| Topic | Resource |
|---|---|
| Classical ML | *An Introduction to Statistical Learning* (ISLP, Python edition); scikit-learn user guide |
| Maths | *Mathematics for Machine Learning* (Deisenroth et al.); 3Blue1Brown linear algebra and calculus; this repo's `Java-dsa/Maths` |
| Deep learning | *Dive into Deep Learning* (d2l.ai); Andrej Karpathy, *Neural Networks: Zero to Hero*; *Understanding Deep Learning* (Prince) |
| PyTorch | official PyTorch tutorials |
| Transformers / LLMs | *The Illustrated Transformer* (Jay Alammar); Hugging Face course; Karpathy's nanoGPT |
| Practical tabular ML | Kaggle Learn; the XGBoost and LightGBM docs |
| MLOps | *Designing Machine Learning Systems* (Chip Huyen, paid); Made With ML; MLflow docs |
| Interviews | *Machine Learning System Design Interview* (Aminian and Xu, paid); Chip Huyen's ML interviews book (free online) |

Libraries change fast. When a snippet here mentions an API that this course does not install
(XGBoost, Hugging Face `transformers`, `peft`, MLflow, FastAPI), check the current docs before you
copy it into a project.

## 6. Interview categories (covered in chapter 15)

1. **ML coding:** implement K-Means, logistic regression, a metric, or attention in NumPy in 30–45 minutes.
2. **ML theory:** bias/variance, regularisation, why trees don't need scaling, ROC vs PR curves.
3. **Deep learning:** backprop, vanishing gradients, BatchNorm vs LayerNorm, attention complexity.
4. **ML system design:** "design a fraud / recommendation / search ranking system", end to end.

---

**Next:** [01 · Python and data fundamentals](../01-Python-Data-Fundamentals/README.md)
