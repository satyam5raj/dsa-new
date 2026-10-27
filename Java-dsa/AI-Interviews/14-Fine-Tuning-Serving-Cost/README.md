# 14 · Fine-Tuning, Serving and Cost

| | |
|---|---|
| **Code** | [FineTuningServing.java](FineTuningServing.java): LoRA parameter maths, a tiny LoRA trained by gradient descent and merged, int8/int4 quantisation error and outliers, distillation soft labels, static vs continuous batching, API vs self-hosting break-even (`java FineTuningServing.java`, 24 checks) |
| **Maths behind it** | low-rank matrices and SVD ([Maths 02](../../Maths/02-Linear-Algebra/README.md)), gradient descent ([Maths 06](../../Maths/06-Optimization/README.md)), KL and temperature ([Maths 07](../../Maths/07-Information-Theory/README.md)) |
| **Interview weight** | medium–high: "RAG or fine-tuning?" is asked almost everywhere; serving and cost come up for platform roles |

---

## 1. Prompting vs RAG vs fine-tuning

| Goal | Best tool | Why |
|---|---|---|
| use **facts** from your documents, kept up to date, with citations | **RAG** (chapter 04) | update by re-indexing; citations; access control |
| a consistent **format, style or tone**; a narrow skill (classify our tickets, extract our fields) | **fine-tuning** | teaches *behaviour* from examples |
| make a **small model** do what a big one does (cheaper, faster) | **fine-tuning / distillation** | big-model quality at small-model cost for one task |
| shorten very long prompts (many instructions and examples on every call) | **fine-tuning** | the behaviour moves into the weights |
| new knowledge that changes weekly | **not** fine-tuning | stale quickly; unreliable at memorising facts; no citations |
| first attempt at anything | **prompting** | fastest to iterate; the baseline to beat |

**The usual order:** prompt engineering → RAG (if knowledge is needed) → fine-tuning (if behaviour, cost or latency still fall short). They **combine**: a fine-tuned model can still use RAG.

---

## 2. Kinds of fine-tuning

| Type | What changes | Data | Cost |
|---|---|---|---|
| **full fine-tuning** | every weight | thousands+ examples | very high (memory for weights, gradients and optimiser states: ~16 bytes per parameter with Adam) |
| **PEFT: LoRA** | small low-rank adapters only | hundreds to thousands | low |
| **QLoRA** | LoRA on top of a 4-bit quantised base model | same | fits big models on one GPU |
| **instruction tuning / SFT** | learn from (prompt, ideal response) pairs | | |
| **preference tuning: DPO / RLHF** | learn from (prompt, better, worse) pairs | | aligns tone and helpfulness |
| **continued pre-training** | more raw domain text (legal, medical) | millions of tokens | |
| **reinforcement fine-tuning** | rewards from a grader on verifiable tasks | | for reasoning skills |

Managed services (Vertex AI tuning, OpenAI fine-tuning, Bedrock) run supervised and preference tuning for you; open models are often tuned with Hugging Face TRL/PEFT, Axolotl or Unsloth.

---

## 3. LoRA: Low-Rank Adaptation

### The idea
Fine-tuning changes a weight matrix W by some ΔW. **LoRA's bet: the useful change is low-rank**, so write it as the product of two thin matrices:
```
W_new = W₀ + A·B          A is d × r,  B is r × d,  with r tiny (4–64)
```
W₀ stays **frozen**; only A and B are trained. (Low rank is the same idea as truncated SVD in [Maths 02](../../Maths/02-Linear-Algebra/README.md#9-matrix-decompositions-factorisations).)

### The numbers
| | Parameters |
|---|---|
| one 4096 × 4096 matrix | **16,777,216** |
| its rank-8 adapter (A + B) | **65,536** (**0.39%**) |
| adapters on the q and v projections of 32 layers | **4,194,304** (**0.06%** of a 7B model) |
| adapter file size (fp16) | **8.4 MB** (vs ~14 GB for the full model) |

### Training a tiny LoRA (from the code)
A frozen 8 × 8 "pre-trained" matrix W₀; the new task needs W₀ + u·vᵀ (a rank-1 change). LoRA rank 1, with **A random and B = 0**, so the model starts **exactly** as the pre-trained one:
- loss **0.771 → 5.9 × 10⁻³²** after training;
- it trained **16 numbers instead of 64**;
- the learned A·Bᵀ **equals** the true change u·vᵀ;
- **merging** for serving: W = W₀ + A·Bᵀ gives identical outputs, so there's **no extra inference latency**.

### Why LoRA is so popular
- Training needs far less GPU memory (no gradients or optimiser states for the frozen weights).
- **Many adapters share one base model**: swap adapters per customer or task at serving time (multi-LoRA serving in vLLM etc.).
- Adapters are tiny to store and ship; you can merge them or keep them separate.
- Quality is close to full fine-tuning for most adaptation tasks.

---

## 4. Data for fine-tuning (where most projects succeed or fail)

1. **Quality over quantity:** a few hundred excellent, consistent examples often beat thousands of noisy ones.
2. **Cover the real distribution**, including edge cases and the refusals you want.
3. Use the **same format at training and inference** (the same system prompt and chat template).
4. Hold out an **eval set**; compare against the prompted base model (chapter 05).
5. Watch for **catastrophic forgetting**: the model gets worse at general skills; mix in general data and keep the learning rate low.
6. **Synthetic data** from a stronger model (distillation) is common; review it.
7. Never train on data you don't have rights to, and strip PII.

---

## 5. Distillation: a big teacher trains a small student

The student learns to match the teacher's **probability distribution**, not just its final answer. For an image of a cat, the teacher's logits are [cat 6.0, lynx 4.5, car 0.5, toaster 0.2]:
| Label | Distribution |
|---|---|
| hard label | [1, 0, 0, 0]: "cat", nothing else (0 bits of extra information) |
| teacher at T = 1 | [0.81, 0.18, 0.00, 0.00]: lynx is the runner-up |
| teacher at **T = 4** | [0.46, 0.32, 0.12, 0.11]: the **"dark knowledge"** that a cat looks much more like a lynx than a car |

Soft labels (at a higher temperature) carry more information per example, so the student learns faster and generalises better. The loss is the KL divergence between the teacher's and student's softened distributions ([Maths 07](../../Maths/07-Information-Theory/README.md)), often mixed with the normal loss on hard labels. **For LLMs**, "distillation" usually means fine-tuning a small model on outputs generated by a big one.

---

## 6. Quantisation: fewer bits per weight

Store weights in int8 or int4 instead of 16-bit floats:
| | Memory (7B model) | Relative error on Gaussian weights (from the code) |
|---|---|---|
| fp16 | 14 GB | – |
| **int8** | 7 GB | **0.0097** (< 1%) |
| **int4** | 3.5 GB | **0.1758** (about 18× worse) |

**The outlier problem:** with one scale per tensor, a single huge weight (1.0 among weights around 0.02) stretches the scale for everyone, and int8 error jumps to **0.1125**. That's why real schemes use **per-channel or per-group scales** and outlier handling (GPTQ, AWQ, bitsandbytes/NF4, SmoothQuant, GGUF formats).

**Benefits:** fit bigger models on smaller GPUs, more room for the KV cache (more concurrent users), faster memory-bound decoding. **Costs:** some quality loss (measure it with evals), especially at 4 bits and below.

---

## 7. Serving LLMs efficiently

| Technique | What it does |
|---|---|
| **continuous batching** | add new requests to the running batch as soon as any request finishes |
| **paged attention** (vLLM) | stores the KV cache in pages like virtual memory, so little memory is wasted and there's more concurrency |
| **prefix / prompt caching** | reuse the KV cache of shared prefixes (system prompts) |
| **speculative decoding** | a small draft model proposes several tokens; the big model verifies them in one pass, giving the same output faster |
| **quantisation** | section 6 |
| **tensor / pipeline parallelism** | split one model across several GPUs |
| **FlashAttention** | memory-efficient, faster attention kernels |
| **multi-LoRA serving** | one base model, many adapters loaded on demand |

### Static vs continuous batching (from the code)
64 requests with output lengths between 10 and 400 tokens, 8 GPU slots:
| | Decode steps | Slot utilisation |
|---|---|---|
| **static** (wait for the batch's longest request) | 2,918 | **56%** |
| **continuous** (refill a slot the moment a request finishes) | 1,856 | **89%** |

With static batching, short requests finish and their slots sit **idle** until the longest one ends. Continuous batching is why vLLM, TGI, TensorRT-LLM and SGLang serve several times more traffic on the same hardware.

**Serving stacks:** vLLM, Hugging Face TGI, NVIDIA TensorRT-LLM / Triton, SGLang, llama.cpp / Ollama (local), and managed endpoints (Vertex AI, SageMaker, Bedrock).

### Latency metrics
**TTFT** (time to first token, dominated by prefill and queueing), **TPOT / ITL** (time per output token), end-to-end latency, and throughput (tokens per second per GPU). Batching raises throughput but can raise latency: tune for your SLO.

---

## 8. Cost: API vs self-hosting

With a $4/hour GPU serving a mid-size open model at 2,500 tokens/s (batched):
| | Cost per 1M tokens |
|---|---|
| self-hosted at **full** utilisation | **$0.44** |
| self-hosted at **20%** utilisation (idle nights, spiky traffic) | **$2.22** |
| a comparable API (example) | $1.00 |

One GPU running 24/7 costs **$2,880/month**. You break even with the API at **2,880M tokens/month**, which means keeping the GPU **44%** busy around the clock. **Utilisation decides it**: self-hosting wins at high, steady volume (or when data must stay in-house), and APIs win for spiky or low traffic. Remember the hidden costs too: engineers, on-call, upgrades, and lower quality than frontier models.

### The cost-reduction checklist
1. Route easy traffic to **smaller models** (chapter 12).
2. **Prompt caching** (static prefix first) and response caching.
3. Shorter prompts: compress context and tool outputs (chapter 09); fewer few-shot examples (or fine-tune them in).
4. Cap **output tokens**; ask for concise answers.
5. **Batch APIs** for offline jobs (often around 50% cheaper).
6. **Distil or fine-tune** a small model for high-volume narrow tasks.
7. Measure the **cost per successful task**, not per token.

---

## 9. Interview questions

**Q1. RAG or fine-tuning for a company knowledge assistant?**
> RAG: the knowledge changes, answers need citations and access control, and fine-tuning is unreliable for memorising facts. Fine-tune only for format or behaviour, or to shrink to a cheaper model, possibly combined with RAG.

**Q2. How does LoRA work, and why is it efficient?**
> It freezes the pre-trained weights and learns a low-rank update ΔW = A·B (rank r ≪ d), training well under 1% of the parameters, so it needs much less memory, the adapters are tiny and swappable, and they can be merged into the weights for zero added latency.

**Q3. What's QLoRA?**
> LoRA training on top of a 4-bit quantised base model (with tricks like NF4 and double quantisation), so large models can be fine-tuned on a single GPU.

**Q4. What are the risks of fine-tuning?**
> Overfitting to small data, catastrophic forgetting of general abilities, degraded safety behaviour, stale knowledge, and maintenance cost (re-tuning when the base model updates). Mitigate with good data, held-out evals, safety evals, and low learning rates.

**Q5. What is quantisation, and what's the trade-off?**
> Storing weights (and sometimes activations or the KV cache) in fewer bits: less memory, more concurrency and often faster decoding, at the cost of some accuracy (worse at 4 bits and with outliers). Per-group scales and methods like GPTQ/AWQ reduce the loss; verify with evals.

**Q6. Why does continuous batching matter?**
> Requests have very different output lengths; static batches waste GPU slots waiting for the longest one. Continuous batching refills slots immediately, raising utilisation and throughput several-fold.

**Q7. When does self-hosting an open model beat an API?**
> At high, steady volume (high GPU utilisation), with strict data-residency requirements, or when a fine-tuned small model is good enough. Otherwise the API is usually cheaper once you count utilisation and the engineering effort.

---

**Next:** [15 · AI system design case studies and question bank](../15-AI-System-Design-Question-Bank/README.md)
