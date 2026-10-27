# 05 · LLM Evaluation

| | |
|---|---|
| **Code** | [LlmEvaluation.java](LlmEvaluation.java): reference metrics, LLM-judge biases and the swap trick, Cohen's kappa, bootstrap confidence intervals, slice-based regression tests, calibration (ECE), pass@k (`java LlmEvaluation.java`, 24 checks) |
| **Maths behind it** | [Maths 05 statistics](../../Maths/05-Statistics/README.md) (confidence intervals, hypothesis tests, precision/recall) |
| **Interview weight** | very high: "how do you know your LLM app works?" separates senior from junior candidates |

**Evaluation ("evals") is how you turn "it seems to work" into a number you can track.** With LLMs it's harder than with normal software: outputs are free text, non-deterministic, and "correct" is often a matter of degree. So you need a mix of methods.

---

## 1. Why evals come first

- Every prompt, model, retrieval or parameter change can fix some cases and **silently break others**.
- Without an eval set you tune by eyeballing 5 examples: that's **vibes**, not engineering.
- Evals let you compare models on **your** task (public benchmarks rarely match it), catch regressions in CI, and decide when something is good enough to ship.

**The workflow:**
```
collect real examples → label expected behaviour → pick metrics → run the baseline
→ change one thing → re-run → compare (with significance) → inspect failures → repeat
```

---

## 2. Building an eval dataset ("golden set")

| Source | Notes |
|---|---|
| **real user queries** (from logs) | the most representative; anonymise PII |
| expert-written cases | cover the important flows |
| **edge cases and failures** | every production bug becomes a test case |
| adversarial cases | jailbreaks, injections, off-topic requests (chapter 10) |
| synthetic (LLM-generated) | fast to scale up; review a sample, since they're easier than real data |

**Size:** 50–100 examples to start, hundreds to a few thousand for reliable comparisons (section 6). **Stratify by category** (billing, technical, refunds…) so you can see per-slice results (section 7). Keep a **held-out** portion you don't tune on.

Each example has: an input, the expected output **or a rubric** (what a good answer must contain or avoid), metadata (category, difficulty) and, for RAG, the relevant documents (chapter 06).

---

## 3. The three families of metrics

### a) Reference-based (compare with a gold answer)
| Metric | What it measures | Good for |
|---|---|---|
| **exact match (EM)** | identical strings (after normalising case, punctuation, articles) | short factual answers, classification |
| **token F1** | word overlap (precision/recall of tokens) | short answers with slight wording differences |
| **BLEU / ROUGE** | n-gram overlap (ROUGE-L = longest common subsequence) | translation and summarisation (historically) |
| **embedding similarity / BERTScore** | meaning overlap | paraphrases |

From the code:
| Prediction vs gold | Score |
|---|---|
| "Paris." vs "Paris": raw exact match | **false** (too strict) |
| normalised exact match | **true** |
| "the capital is Paris" vs "Paris": token F1 | **0.500** |
| "the cat was sitting on the mat" vs "the cat sat on the mat": F1 / ROUGE-L | **0.667** / **0.667** |
| "The French capital city" vs "Paris": F1 | **0.000**, although it's arguably correct! |

**The lesson:** overlap metrics punish correct answers that use different words, and reward wrong answers that share words. They're cheap, but weak for open-ended generation.

### b) Programmatic / deterministic checks (the most reliable, use them wherever you can)
- Is the output valid JSON matching the schema? (chapter 02)
- Does the code **compile and pass the unit tests**? (pass@k, section 9)
- Is the extracted date correct? Is the classification label right?
- Is the response under the length limit? Does it avoid banned phrases? Does it contain a citation?
- Did the agent call the right tool with the right arguments? (chapter 07)

### c) Model-based: LLM-as-a-judge
Use a strong LLM to grade outputs against a **rubric**. This scales human-like judgement to thousands of cases.

| Style | How | Notes |
|---|---|---|
| **single-answer grading** | "Score 1–5 for correctness, using this rubric…" | prefer binary or narrow scales with clear definitions |
| **pairwise comparison** | "Which answer is better, A or B?" | more reliable for comparing two systems |
| **reference-guided** | give the judge the gold answer | much more accurate on factual tasks |
| **criteria checklists** | "Does it mention X? Does it avoid Y?" (yes/no per item) | the most robust; easy to debug |

---

## 4. LLM judges are biased: measure and correct

The code simulates a judge with the two classic biases:

| Bias | Demo result | Fix |
|---|---|---|
| **position bias** | with two **equally good** answers, the one shown **first** wins **77%** of the time | **run both orders** (A,B) and (B,A); count a win only if it wins both, otherwise a tie. In the code, A and B then win equally often |
| **verbosity bias** | the **longer** of two equal answers wins **95%**, even with positions balanced | instruct the judge to ignore length; length-controlled scoring; compare at similar lengths |
| self-preference | judges favour their own model family's outputs | use a different model family as the judge |
| leniency, scale drift | everything gets 4/5 | binary or checklist rubrics, few-shot graded examples |

**Always validate the judge against humans** before trusting it (next section).

---

## 5. Does the judge agree with humans? Cohen's kappa

Have humans label ~100 examples, run the judge on the same ones, and compare. **Raw agreement can mislead**:
| Judge | Raw agreement with humans | Cohen's kappa |
|---|---|---|
| a real judge | 0.900 | **0.780** (strong) |
| a "judge" that says PASS to everything | 0.650 | **0.000** (useless!) |

```
κ = (observed agreement − agreement expected by chance) / (1 − chance agreement)
```
Kappa corrects for the agreement you'd get by chance (when 65% of answers are passes, always saying "pass" agrees 65% of the time). Rough guide: > 0.6 is good, > 0.8 is excellent. If it's low, fix the rubric, add examples to the judge prompt, or use a stronger judge.

---

## 6. Is the improvement real? Statistical significance

Evals are samples: small eval sets produce **noisy** numbers. Suppose model B is truly about 3 points better than A:
| Eval size | 95% bootstrap CI for (B − A) | Conclusion |
|---|---|---|
| 50 questions | [+0.000, +0.100] | **includes 0: can't tell** |
| 2,000 questions | [+0.035, +0.052] | **B is better** |

- **Bootstrap:** resample the questions with replacement thousands of times and recompute the difference each time. The middle 95% of those differences is the confidence interval. **Resample paired**: the same questions for both models, which removes question-difficulty noise.
- **Rule of thumb:** the standard error of an accuracy is √(p(1−p)/n). At n = 100 and 80% accuracy it's **±4 points**, so a 2-point "improvement" on 100 examples is noise.
- Run non-deterministic systems several times and average.

---

## 7. Regression testing: always slice the results

Prompt v2 vs v1, 100 examples per category:
| Category | v1 | v2 |
|---|---|---|
| billing | 80% | 88% |
| technical | 70% | 82% |
| account | 75% | 83% |
| **refunds** | **90%** | **72%** ⚠️ |
| **overall** | 0.788 | **0.813** (looks like a win) |

The average improved while **refunds collapsed**. If refunds are your most sensitive flow, v2 is a disaster. **Report per-slice results, and set per-slice thresholds in CI.**

### Evals in CI/CD
- a fast **smoke suite** (deterministic checks and a few dozen cases) on every PR;
- the **full suite** nightly or before a release;
- **block merges** on regressions beyond a threshold;
- version **everything** (prompt, model, retrieval settings, eval set) so a result can be reproduced.

---

## 8. Calibration: does "90% confident" mean right 90% of the time?

If a model (or classifier) reports confidence, check it:
```
ECE (expected calibration error) = weighted average over confidence bins of |avg confidence − accuracy|
```
| Model | ECE |
|---|---|
| well calibrated | **0.003** |
| over-confident (says 0.9, is right 0.65) | **0.252** |

Calibration matters when you **use the confidence**, e.g. "auto-answer if confidence > 0.8, else escalate to a human". Verbalised LLM confidences ("I'm 95% sure") are often poorly calibrated; prefer token log-probabilities, self-consistency agreement, or a separately calibrated classifier.

---

## 9. pass@k for code generation

Generate n samples per problem, count c that pass the unit tests; the unbiased estimate of "at least one of k samples passes" is:
```
pass@k = 1 − C(n − c, k) / C(n, k)
```
With 10 samples of which 3 pass: **pass@1 = 0.300, pass@5 = 0.917, pass@10 = 1.000**. pass@1 matters for a single suggestion; pass@k matters when you can run the tests and pick a winner.

---

## 10. Online evaluation (after launch)

Offline evals don't catch everything. In production, measure:
| Signal | Examples |
|---|---|
| **explicit feedback** | thumbs up/down, ratings, "report a problem" |
| **implicit feedback** | the user copies the answer, retries, rephrases, abandons, escalates to a human |
| **business metrics** | ticket resolution rate, conversion, time saved |
| **sampled LLM-judge scoring** of live traffic | faithfulness, safety, tone on a % of requests |
| **operational** | latency (TTFT, total), cost per request, error and refusal rates |
| **A/B tests** | new prompt or model vs old, on real users (with significance, [Maths 05](../../Maths/05-Statistics/README.md)) |

Feed bad production cases back into the golden set: this is the **data flywheel**.

### Public benchmarks (know the names, don't trust them blindly)
MMLU / MMLU-Pro (knowledge), GPQA (hard science), GSM8K / MATH (maths), HumanEval / SWE-bench (code), MT-Bench / Chatbot Arena (chat quality via human preference), TruthfulQA (truthfulness), HELM (holistic). They're useful for **shortlisting** models; contamination and mismatch with your task mean **your own evals decide**.

### Tools
OpenAI Evals, LangSmith, Langfuse, Arize Phoenix, Braintrust, promptfoo, DeepEval, Ragas (RAG), Vertex AI Gen AI evaluation service, MLflow. They all do roughly the same thing: datasets + runs + metrics + judges + comparisons + traces.

---

## 11. Interview questions

**Q1. How would you evaluate an LLM application?**
> Build a golden set from real queries plus edge cases, stratified by category. Use deterministic checks where possible (schema, tests, exact labels), reference metrics for short answers, and a validated LLM judge with clear rubrics for open-ended quality. Compare versions with paired significance tests, slice by category, run it in CI, then monitor online with feedback and sampled judging.

**Q2. What are the weaknesses of LLM-as-a-judge, and how do you mitigate them?**
> Position, verbosity and self-preference biases, and leniency. Swap the order and require consistency, control for length, use a different model family, prefer binary/checklist rubrics with examples, give the reference answer, and measure agreement with human labels (kappa).

**Q3. Why aren't BLEU/ROUGE good for LLM outputs?**
> They measure word overlap, so they penalise correct paraphrases and reward wrong answers that share words; they correlate poorly with human judgement for open-ended generation.

**Q4. Your new prompt raised the eval score from 81% to 83% on 100 examples. Ship it?**
> Not on that evidence: the standard error is about ±4 points. Use a larger or paired eval with a bootstrap CI, check per-category regressions, then A/B test online.

**Q5. How do you evaluate a non-deterministic system?**
> Run each case several times and report the mean and variance (or pass rate); use temperature 0 for the regression suite where possible; use statistical tests rather than single runs.

**Q6. What is pass@k?**
> The probability that at least one of k sampled solutions passes the tests, estimated without bias as 1 − C(n−c, k)/C(n, k) from n samples with c correct.

**Q7. What's the difference between offline and online evaluation?**
> Offline runs on a fixed labelled dataset before deployment (reproducible, safe); online measures real user behaviour and business outcomes after deployment (realistic, but noisy and slower). You need both, connected by a flywheel that adds production failures to the offline set.

---

**Next:** [06 · RAG evaluation](../06-RAG-Evaluation/README.md)
