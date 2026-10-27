# 12 · Fine-tuning, LoRA, quantisation, distillation and alignment

> Nobody trains a big model from scratch for a new task. You start from a **pretrained** model
> and **adapt** it: cheaply (LoRA), safely (without forgetting), smaller (quantisation,
> distillation), and towards what humans want (SFT, reward models, DPO, RLHF).

Run it: `../.venv/bin/python fine_tuning.py` → **24/24 checks** (~3 s). Every idea runs here on
small models; the Hugging Face code at the end shows the same ideas at LLM scale.

---

## 1. Transfer learning: pretraining is only useful if it's broad

Task B is classifying digits 5–9 with only **25 labelled images** (5 per class).

| Starting point | Linear probe (freeze backbone, train the head) | Full fine-tune |
|---|---|---|
| random init (train from scratch) | | **0.881** |
| supervised pretraining on a **narrow** task (digits 0–4 only) | **0.580** | 0.859 |
| **self-supervised** pretraining on all unlabelled images (denoising: mask 30 % of pixels, rebuild) | **0.926** | **0.914** |

This is the most important lesson about pretraining, and it surprised me while building the demo:

- Features learned to tell apart 0–4 are **specialised**. They don't describe 5–9 well, so a
  frozen probe on them is worse than starting from scratch.
- Features learned from **broad data with a self-supervised objective** (no labels needed)
  describe every digit, and they transfer.

That's exactly why foundation models are pretrained on enormous, diverse data with self-supervised
objectives: next-token prediction for LLMs, masked modelling, contrastive learning for vision.

| Strategy | Trainable params | When |
|---|---|---|
| **linear probe / feature extraction** | 645 (just the head) | tiny data, a similar domain, a quick baseline |
| **full fine-tuning** | 25,477 (everything) | lots of data, a domain shift, maximum quality |
| **PEFT (LoRA, adapters, prefix/prompt tuning)** | ~1–10 % | the LLM default: cheap, swappable, little forgetting |

## 2. LoRA from scratch

**Idea:** fine-tuning changes the weights by some `ΔW`. LoRA bets that `ΔW` is **low-rank**, so it
learns it as a product of two thin matrices while the original W stays **frozen**:

```
y = x Wᵀ + (α / r) · x Aᵀ Bᵀ          A: (r × in), B: (out × r), r ≪ d
```

```python
self.A = nn.Parameter(torch.randn(r, in) / sqrt(in))
self.B = nn.Parameter(torch.zeros(out, r))          # zero → starts exactly as the base model
```

Checked:

- With `B = 0` at init the LoRA layer is **identical** to the base layer, so training starts from
  the pretrained behaviour.
- **Merging** `W' = W + (α/r)·BA` gives the same outputs, so after training you fold the adapter
  in and there is **zero extra inference latency**.
- Parameters per layer: `r·(in + out)`. That's 1,024 for 128 × 128 at r = 4; for a 4096 × 4096
  attention matrix at r = 8 it's 65,536 vs 16.8 M (**0.39 %**).

On task B with all labels:

| | Trainable params | Accuracy |
|---|---|---|
| full fine-tuning | 25,477 | 0.985 |
| **LoRA r = 4** (+ new head) | **2,437** | **0.970** |

**Knobs:**

- `r`: 4–64 (higher = more capacity);
- `α`: the scale, often 2r;
- which modules to adapt (attention q/k/v/o, and often the MLP too);
- LoRA dropout.

The learning rate is usually higher than for full fine-tuning (~1e-4 to 2e-4 for LLMs).

## 3. Catastrophic forgetting

Task A accuracy (digits 0–4), re-attaching the original A head:

| Model | Task A accuracy |
|---|---|
| before any fine-tuning | 0.993 |
| after **full** fine-tuning on B | **0.904**: the shared backbone drifted |
| LoRA model with the **adapter switched off** | **0.993**: identical, because the base weights never changed |

With LoRA you can keep **one base model and many small adapters** (one per customer or task) and
swap them per request. Serving systems batch requests for different adapters on the same base.

Other anti-forgetting tools:

- mix some original-task data into fine-tuning (replay);
- a lower learning rate and fewer epochs;
- regularise towards the original weights (L2-SP, EWC).

## 4. Why low rank works: look at a real update

Take the 128 × 128 layer's weight change from full fine-tuning (`W_after − W_before`) and compute
its singular values:

- **90 % of the update's energy lies in just 4 directions**;
- a random 128 × 128 matrix needs 65.

Real fine-tuning updates are approximately low-rank. That's the empirical observation behind LoRA
(and chapter 02's SVD in action).

## 5. Supervised fine-tuning (SFT): train on the answer, not the question

Instruction tuning trains on `(prompt, response)` pairs formatted with a **chat template**:

```
<|user|>\n{prompt}\n<|assistant|>\n{response}<|end|>
```

The model should learn to produce **responses**, not to predict user prompts, so prompt tokens get
the label `−100`, which `cross_entropy(ignore_index=−100)` skips. Checked: the masked loss equals
the loss over the 3 response tokens only.

> **Use the same template at training and inference.** A template mismatch is one of the most
> common causes of a fine-tuned model "not working".

SFT data quality beats quantity. A few thousand clean, diverse, well-formatted examples often beat
100k noisy ones. Deduplicate and decontaminate against your eval sets (chapter 07's leakage
rules apply).

## 6. Quantisation: smaller and faster weights

Map floats to low-bit integers with a scale: `q = round(w / s)`, `ŵ = q · s`,
`s = max|w| / (2^(bits−1) − 1)`.

| Format | Relative weight error | Memory vs fp32 |
|---|---|---|
| int8 per-channel | **0.68 %** | 25 % |
| int4 per-channel | **12.3 %** | 12.5 % |

Even with 12 % weight error, the int4 model's task-B accuracy was **unchanged (0.985)**. Networks
are robust to small, unbiased weight noise.

**Outliers are the enemy.** Add one weight of 50 and use a **single scale for the whole tensor**:
every other weight rounds to 0 (error **1.000**). **Per-channel** scales (one per output row) keep
the error at 0.123. LLMs have outlier features, which is why real methods use small **groups**
(e.g. 64–128 weights per scale) or handle outliers specially.

| Method | Idea |
|---|---|
| **PTQ** (post-training quantisation) | quantise a trained model; calibrate on a little data |
| GPTQ / AWQ | smarter int4 PTQ for LLMs (minimise layer output error; protect salient weights) |
| **QAT** | simulate quantisation during training |
| **QLoRA** | keep the base model in **4-bit NF4**, train LoRA adapters in bf16 on top. Fine-tune a 65B model on one 48 GB GPU |
| GGUF (llama.cpp) | quantised formats for CPU and laptop inference |

## 7. Knowledge distillation: a small student learns from a big teacher

The **teacher** (85k parameters) is trained on all labels. The **student** (16 hidden units) gets
only **150 labelled images**, but the teacher can label everything.

| | Test accuracy |
|---|---|
| teacher | 0.969 |
| student, 150 hard labels only | 0.908 |
| **student, distilled** (teacher's soft labels on all training images + the 150 hard labels) | **0.968** |

```
loss = α · T² · KL( softmax(teacher/T) ‖ softmax(student/T) ) + (1 − α) · CE(student, labels)
```

- **Temperature reveals "dark knowledge".** At T = 1 the teacher's second-best class probability
  for a test image is ~0.0000; at **T = 4** it's **0.039**. Softened outputs say *which wrong
  classes are close* (a 3 looks a bit like an 8), which is much richer than a one-hot label.
- The `T²` factor keeps the gradient scale comparable as T changes.
- For LLMs, distillation means training a small model on a big model's outputs: logits if you
  have them, otherwise generated text (sequence-level distillation, synthetic data).

## 8. Alignment: reward models, RLHF and DPO

**The RLHF pipeline:**

1. **SFT**: teach the format and helpfulness from demonstrations.
2. **Reward model**: humans compare two answers; train a model to score the preferred one higher.
3. **RL** (PPO): optimise the policy to maximise reward **minus a KL penalty** that keeps it close
   to the SFT model, so it doesn't drift into reward-hacking gibberish.

### Reward model: Bradley–Terry

```
P(a preferred over b) = σ(r(a) − r(b))         loss = −log σ(r(chosen) − r(rejected))
```

On 2,000 simulated comparisons with hidden quality weights `[2, −1, 0.5]`, the learned weights
were **`[1.97, −0.94, 0.50]`** (cosine similarity > 0.99).

### DPO: skip the reward model and the RL

Direct Preference Optimisation shows that the RLHF objective can be optimised **directly on
preference pairs**:

```
L = −log σ( β · [ (log π(chosen) − log π_ref(chosen)) − (log π(rejected) − log π_ref(rejected)) ] )
```

- At the start (π = π_ref) the loss is exactly **ln 2** (checked).
- Toy policy over 4 answers. The reference is `[0.366, 0.366, 0.134, 0.134]`. Preferences have
  realistic disagreement: 80 % prefer 0 over 1, 90 % prefer 0 over 2 and over 3, 70 % prefer 2
  over 3.

| β | Final policy | KL to reference |
|---|---|---|
| 0.1 | `[1.0, 0.0, 0.0, 0.0]` | 1.006: collapsed onto one answer |
| **1.0** | `[0.75, 0.188, 0.042, 0.021]` | **0.327** |

With β = 1 the order is 0 > 1 > 2 > 3, as the raters said, while staying near the reference.
**β is the KL leash:** small β trusts the preferences fully and moves far (risking over-optimisation
and lost diversity); large β stays conservative.

> While building this demo, a version with **unanimous** preferences showed no β effect. With
> perfectly consistent preferences, DPO's optimum is "push the margin to infinity", and Adam's
> normalised steps then hide β. Disagreement between raters is what makes the optimum finite.
> Real preference data always has some.

Related methods:

- **IPO** (fixes DPO's overfitting to deterministic preferences);
- **KTO** (thumbs up/down instead of pairs);
- **ORPO** (SFT + preference in one loss);
- **GRPO** (RL with group-relative advantages, used for reasoning models);
- **RLAIF / Constitutional AI** (AI feedback guided by principles).

---

## The same ideas with Hugging Face (not executed in this course)

These libraries aren't in the course venv (`pip install transformers peft trl datasets bitsandbytes`),
and their APIs change often, so **check the current docs** before copying.

```python
# --- load a model and tokenizer, generate ---
from transformers import AutoTokenizer, AutoModelForCausalLM
tok = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-0.5B-Instruct")
model = AutoModelForCausalLM.from_pretrained("Qwen/Qwen2.5-0.5B-Instruct", torch_dtype="auto")
msgs = [{"role": "user", "content": "Explain LoRA in one sentence."}]
ids = tok.apply_chat_template(msgs, add_generation_prompt=True, return_tensors="pt")  # the chat template
out = model.generate(ids, max_new_tokens=60, do_sample=True, temperature=0.7, top_p=0.9)
print(tok.decode(out[0][ids.shape[1]:], skip_special_tokens=True))

# --- LoRA with PEFT (section 2, at scale) ---
from peft import LoraConfig, get_peft_model
cfg = LoraConfig(r=16, lora_alpha=32, lora_dropout=0.05,
                 target_modules=["q_proj", "k_proj", "v_proj", "o_proj"], task_type="CAUSAL_LM")
model = get_peft_model(model, cfg)
model.print_trainable_parameters()          # typically < 1% trainable

# --- QLoRA: 4-bit base + LoRA (section 6) ---
from transformers import BitsAndBytesConfig
bnb = BitsAndBytesConfig(load_in_4bit=True, bnb_4bit_quant_type="nf4", bnb_4bit_compute_dtype="bfloat16")
base = AutoModelForCausalLM.from_pretrained(name, quantization_config=bnb)   # needs a CUDA GPU

# --- SFT and DPO with TRL (sections 5 and 8) ---
from trl import SFTTrainer, SFTConfig, DPOTrainer, DPOConfig
SFTTrainer(model=model, train_dataset=chat_ds, args=SFTConfig(output_dir="sft")).train()
DPOTrainer(model=model, ref_model=None, train_dataset=pref_ds,      # columns: prompt, chosen, rejected
           args=DPOConfig(output_dir="dpo", beta=0.1), processing_class=tok).train()

# --- after training: merge the adapter for serving (section 2) ---
merged = model.merge_and_unload()
merged.save_pretrained("my-model")
```

**The Hugging Face ecosystem:**

| Library | Role |
|---|---|
| `transformers` | models and tokenizers |
| `datasets` | data loading and streaming |
| `peft` | LoRA and other PEFT methods |
| `trl` | SFT, DPO, PPO, GRPO |
| `accelerate` | multi-GPU / mixed precision |
| `bitsandbytes` | 4/8-bit quantisation |
| `evaluate` | metrics |
| the Hub | sharing models and data |
| serving | vLLM, TGI, llama.cpp (chapter 14) |

## When to fine-tune at all (decision order)

1. **Prompting** (+ few-shot examples): free, instant. Try first.
2. **RAG**: when the model lacks **knowledge** (your documents, fresh facts). See
   `Java-dsa/AI-Interviews/04`.
3. **Fine-tuning (LoRA/SFT)**: when you need a **behaviour**, format, style or domain language
   consistently, lower latency/cost with a smaller model, or prompts have become huge.
4. **Preference tuning (DPO/RLHF)**: when "good" is easier to judge than to demonstrate (tone,
   safety, helpfulness).
5. **Pretraining / continued pretraining**: a new language or a very different domain, with huge
   data and budget.

---

## Exercises

1. Why must LoRA's `B` start at zero rather than random?
2. A 7B model in bf16 is 14 GB. Roughly how much GPU memory does **full** AdamW fine-tuning need,
   compared with QLoRA?
3. Your DPO-tuned model became verbose and repetitive. Which knob, and why?
4. Distillation vs quantisation: which reduces latency more, and can you combine them?

<details><summary>Answers</summary>

1. With `B = 0`, `BA = 0`, so the adapted model starts exactly as the pretrained one, and early
   training doesn't destroy its behaviour. (`A` is random so gradients flow into `B`; if both were
   zero, neither would ever learn.)
2. Full AdamW at ~16 bytes/param ≈ **112 GB** plus activations: several GPUs or sharding. QLoRA:
   ~3.5–4 GB for the 4-bit weights + small adapters and their optimizer states + activations:
   **one 16–24 GB GPU**.
3. Increase **β** (a stronger pull towards the reference), use fewer steps or a lower learning
   rate, and fix the data: preference data often favours longer answers (length bias), so balance
   or length-normalise it.
4. Distillation shrinks the **architecture** (fewer layers/parameters → fewer FLOPs → lower
   latency); quantisation shrinks **bytes per weight** (memory-bandwidth-bound decoding gets
   faster). They're complementary: distil to a small model, then quantise it.
</details>

## Interview questions

**Q: Explain LoRA.** Freeze the pretrained weights and learn a low-rank update `ΔW = BA` (rank r)
for chosen layers. It trains < 1 % of the parameters, needs much less memory, and the adapters are
tiny and swappable. It can be merged into W for zero inference overhead. It works because
fine-tuning updates have low intrinsic rank.

**Q: Fine-tuning vs RAG?** RAG adds **knowledge** at inference time (updatable, citeable, no
training). Fine-tuning changes **behaviour** (format, style, skills, domain language). They're often
combined: fine-tune for format and tool use, RAG for facts.

**Q: RLHF vs DPO?** RLHF trains a reward model on preferences, then runs RL (PPO) with a KL
penalty to the reference model: powerful but complex and unstable. DPO optimises a
classification-like loss on preference pairs directly, with β playing the KL role: simpler, stable,
no reward model or sampling loop. Online RL methods still win for some goals (e.g. reasoning with
verifiable rewards).

**Q: What is catastrophic forgetting and how do you prevent it?** Fine-tuning on new data
overwrites what the model knew. Prevent it with PEFT (frozen base), replay of original data,
lower learning rates and fewer epochs, regularisation towards the original weights, and by
evaluating on the original tasks too.

**Q: How does QLoRA fit a 65B model on one GPU?** The base weights are stored in 4-bit NF4 with
double-quantised scales, LoRA adapters train in bf16, and paged optimizers absorb memory spikes.
Gradients flow through the frozen 4-bit weights (dequantised on the fly) into the adapters only.

---

**Next:** [13 · Time series, recommenders, anomaly detection and RL](../13-TimeSeries-RecSys-Anomaly-RL/README.md)
