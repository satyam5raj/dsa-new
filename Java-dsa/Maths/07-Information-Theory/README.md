# 07 · Information Theory

| | |
|---|---|
| **Code** | [InformationTheory.java](InformationTheory.java): every example below, including a Huffman coder and real zip compression (`java InformationTheory.java`, 39 checks) |
| **You need** | logs from [00 · Foundations](../00-Foundations/README.md), distributions from [04 · Probability](../04-Probability/README.md) |
| **Used in** | **loss functions** (cross-entropy trains nearly every classifier and every LLM), compression (zip, JPEG, video), decision trees, VAEs and diffusion models (KL), evaluating language models (perplexity), RLHF, communication systems |

Claude Shannon invented information theory in 1948 to answer "how much can you compress a message, and how fast can you send it over a noisy wire?". The answers turned out to be exactly what ML needs: the standard loss function, cross-entropy, is a quantity from this chapter.

---

## 1. Information: measuring surprise

### The idea
Hearing that something **unlikely** happened tells you a lot; hearing that something certain happened tells you nothing. Shannon defined the **information content** (surprise) of an event with probability p as:
```
I(p) = −log₂ p   bits
```
| Event | Probability | Information |
|---|---|---|
| a fair coin lands heads | 1/2 | **1 bit** |
| a die shows 4 | 1/6 | **2.585 bits** |
| a 1-in-1024 event | 1/1024 | **10 bits** |
| the sun rises | ≈ 1 | ≈ 0 bits |

Why the log? So that information **adds** for independent events: two coin flips have probability 1/4 and carry 1 + 1 = 2 bits, because log turns products into sums.

**One bit** = the answer to one fair yes/no question. 10 bits = 10 yes/no questions = one item out of 2¹⁰ = 1024. That's binary search again.

**Units:** with log₂ the unit is **bits**; with ln (natural log) it's **nats**. ML libraries use nats (1 nat ≈ 1.443 bits).

---

## 2. Entropy: the average surprise

### Definition
The **entropy** of a distribution is the expected (average) information:
```
H(p) = −Σ pᵢ log₂ pᵢ        bits
```
It measures **how uncertain or unpredictable** the outcome is.

| Distribution | Entropy |
|---|---|
| fair coin (½, ½) | **1.0000** bit, maximum uncertainty for 2 outcomes |
| biased coin (0.9, 0.1) | **0.4690** bits, mostly predictable |
| coin that always lands heads | **0** bits, no uncertainty at all |
| uniform over 8 outcomes | **3.0000** bits = log₂ 8 |

**Key facts:**
- H ≥ 0, and H = 0 only when the outcome is certain.
- For n outcomes, H is **largest when all are equally likely** (H = log₂ n). Any bias makes the outcome more predictable.

### Entropy of text
The letters of the code's sample sentence have entropy **4.18 bits per character**, less than the **4.75 bits** (log₂ 27) a fixed code for 26 letters plus a space would need, because letters aren't equally common (lots of 'e', 'o' and spaces; few 'q' and 'z'). Shannon estimated real English at about **1–1.5 bits per character** once you use context (after "th", "e" is very likely). That gap is why text compresses so well, and why language models work: they exploit context to predict the next character or token.

---

## 3. Entropy is the limit of compression

### Shannon's source coding theorem
You can't losslessly compress data below its entropy (on average), and you can get arbitrarily close to it.

### Huffman coding: an optimal prefix code
Give **frequent symbols short codes and rare symbols long ones** (like Morse code: E is `.`, Q is `--.-`). Huffman's algorithm builds the best possible prefix code greedily:
1. Put every symbol in a min-heap keyed by frequency.
2. Repeatedly take the **two rarest**, merge them into a node with the summed frequency, and push it back.
3. The resulting tree gives the codes: left = 0, right = 1.

For frequencies a:45, b:13, c:12, d:16, e:9, f:5 (per 100):
| Symbol | a | b | c | d | e | f |
|---|---|---|---|---|---|---|
| code length | 1 | 3 | 3 | 3 | 4 | 4 |

- average Huffman length: **2.24 bits** per symbol;
- entropy (the theoretical limit): **2.2199 bits**;
- a fixed-length code needs ⌈log₂ 6⌉ = **3 bits**.

Huffman always lands in **H ≤ average < H + 1**. ([Heaps](../../DSA/08-Heap/) are the data structure that makes it fast.)

### Redundancy is what compresses
Real `zip` (DEFLATE) on 100 KB:
| Input | Compressed |
|---|---|
| random bytes (maximum entropy: 8 bits/byte) | **100,041 bytes**: *bigger*, nothing to exploit |
| a repeating pattern (very low entropy) | **227 bytes** |

**Random data can't be compressed.** (And by the pigeonhole principle, [chapter 01](../01-Discrete-Math/README.md), no compressor can shrink *every* file.) Encrypted data looks random, so compress first, then encrypt.

---

## 4. Cross-entropy: the loss function

### Definition
If the true distribution is p but you use a model q, the average surprise you experience is the **cross-entropy**:
```
H(p, q) = −Σ pᵢ log qᵢ
```
In coding terms: the average message length if the data comes from p but you built your code for q. It's always **≥ H(p)**; using the wrong model costs extra bits.

For p = [0.7, 0.2, 0.1] and q = [0.5, 0.3, 0.2]: H(p) = **1.1568** and H(p, q) = **1.2796**.

### Cross-entropy as a classification loss
For one training example the true distribution is **one-hot** (e.g. "it's a cat" = [1, 0, 0]), so the sum collapses to a single term:
```
loss = −log(q_true_class)          "the log of the probability the model gave the right answer"
```
For a binary label it's **log loss**: −[y log q + (1 − y) log(1 − q)].

| Model's probability for the true class | Loss (nats) |
|---|---|
| 0.9 (confident and right) | **0.1054** |
| 0.5 (unsure) | **0.6931** |
| 0.1 (confident and **wrong**) | **2.3026** |
| 0.001 (very confident and wrong) | **6.9078** |

The log makes **confident mistakes extremely expensive**, so the model learns to be calibrated: say 90% only when it's right about 90% of the time. Minimising cross-entropy is the same as maximum likelihood ([chapter 05](../05-Statistics/README.md)). And its gradient with softmax is the simple **p − y** ([chapter 03](../03-Calculus/README.md)).

**Every LLM is trained by minimising cross-entropy:** at each position, −log P(the actual next token).

---

## 5. KL divergence: the distance between distributions

### Definition
The **Kullback–Leibler divergence** is the *extra* cost of using q when the truth is p:
```
KL(p ‖ q) = Σ pᵢ log(pᵢ / qᵢ) = H(p, q) − H(p)
```
For the example above: KL(p ‖ q) = 1.2796 − 1.1568 = **0.1228** bits.

### Properties
| Property | Example |
|---|---|
| **KL ≥ 0 always** (Gibbs' inequality) | checked on 10,000 random pairs |
| KL(p ‖ q) = 0 only when p = q | KL(p ‖ p) = 0 |
| **not symmetric**: KL(p ‖ q) ≠ KL(q ‖ p) | **0.1228** vs **0.1328** |

Because it isn't symmetric, KL isn't a true distance ("divergence" is the honest name).

### Why minimising cross-entropy = minimising KL
H(p, q) = H(p) + KL(p ‖ q), and H(p), the entropy of the data, doesn't depend on the model. So training with cross-entropy is exactly pushing the model's distribution towards the data's distribution.

### Where KL appears in ML
| Where | Role |
|---|---|
| **VAEs** | a KL term keeps the latent distribution close to a standard normal |
| **knowledge distillation** | a small "student" model minimises KL to a big "teacher" model's outputs |
| **RLHF / PPO** | a KL penalty stops the fine-tuned LLM drifting too far from the original model |
| **diffusion models** | the training objective is a sum of KL terms |
| **t-SNE** | matches neighbour distributions by minimising KL |

**The asymmetry matters:** minimising KL(p ‖ q) makes q **cover all** of p's modes ("mean-seeking"); minimising KL(q ‖ p) makes q **pick one** mode ("mode-seeking"). This is a real design choice in variational inference.

---

## 6. Mutual information: how much does X tell you about Y?

### Definition
```
I(X; Y) = H(X) − H(X | Y)
        = how much your uncertainty about X drops once you learn Y
        = KL( P(X,Y) ‖ P(X)P(Y) )
```
- I = 0 ⟺ X and Y are **independent**.
- Unlike correlation ([chapter 04](../04-Probability/README.md)), it catches **any** relationship, not just linear ones.

Weather and umbrellas (the joint table from chapter 04: sunny/umbrella 0.05, sunny/none 0.55, rainy/umbrella 0.30, rainy/none 0.10):
```
I(weather; umbrella) = 0.3613 bits = H(weather) − H(weather | umbrella)
```
Seeing whether someone carries an umbrella removes about 0.36 of the 0.97 bits of uncertainty about the weather. For an independent table (rows proportional to each other), I = **0**.

**Uses:** feature selection (keep the features with high mutual information with the label), contrastive learning (InfoNCE maximises a bound on mutual information), measuring how much a representation keeps.

---

## 7. Information gain: how decision trees choose splits

A decision tree asks the question that **reduces entropy the most**:
```
information gain = H(parent) − Σ (weighted) H(children)
```
The classic "play tennis?" data: 14 days, 9 yes and 5 no:
- H(parent) = **0.9403** bits.
- Split on **outlook**: sunny (2 yes / 3 no), overcast (4 / 0), rain (3 / 2). Gain = **0.2467**.
- Split on **windy**: false (6 / 2), true (3 / 3). Gain = **0.0481**.

Outlook wins: the "overcast" branch becomes perfectly pure (entropy 0). The tree splits on outlook first and repeats in each branch. (The Gini impurity, 1 − Σ pᵢ², is a cheaper alternative used by CART and scikit-learn.)

---

## 8. Perplexity: how language models are scored

### Definition
```
perplexity = 2^(cross-entropy in bits) = the geometric mean of 1 / P(actual token)
```
**Intuition:** perplexity is the number of choices the model is effectively torn between at each step.
- A model that's uniform over 10 tokens has perplexity **10**.
- A perfect model (always 100% on the right token) has perplexity **1**.

A model gave the 5 actual tokens of a sentence probabilities 0.5, 0.25, 0.8, 0.1, 0.4:
```
cross-entropy = average of −log₂ q = 1.5932 bits/token
perplexity = 2^1.5932 = 3.0171 = (1/(0.5·0.25·0.8·0.1·0.4))^(1/5)
```
So the model is about as uncertain as choosing among **3** equally likely options. Lower perplexity on held-out text = a better language model.

---

## 9. Temperature: controlling randomness when sampling

LLMs produce scores (logits) and sample from softmax(logits / T):
```
p_i = e^(z_i / T) / Σ e^(z_j / T)
```
For logits [3, 2, 1, 0]:
| Temperature | Entropy | P(top token) | Behaviour |
|---|---|---|---|
| T = 0.25 | **0.134** bits | **0.982** | almost always the top token: focused, repetitive |
| T = 1 | 1.367 bits | 0.644 | the model's own distribution |
| T = 4 | **1.945** bits | 0.350 | close to uniform (max 2 bits): creative, more random |

Low T is for code and factual answers; higher T is for brainstorming. T → 0 is "greedy" decoding. Top-k and top-p (nucleus) sampling cut off the unlikely tail.

---

## 10. Channel capacity (the other half of Shannon's work)

A noisy channel that flips each bit with probability f has **capacity C = 1 − H(f)** bits per transmitted bit. Shannon proved that you can communicate essentially error-free at any rate below C, using error-correcting codes. That's what Wi-Fi, 4G/5G, QR codes, RAID and deep-space probes rely on (Hamming codes, Reed–Solomon, LDPC, turbo codes).

---

## 11. Exercises (answers below)

1. What's the entropy of a fair 4-sided die? Of a die with probabilities (½, ¼, ⅛, ⅛)?
2. Design an optimal prefix code for the second die in question 1. What's its average length?
3. A classifier gives the true class probability 0.25. What's the cross-entropy loss in bits and in nats?
4. Is KL(p ‖ q) = KL(q ‖ p)? When is KL = 0?
5. A language model has cross-entropy 2 bits per token on test data. What's its perplexity?
6. Why can't you compress a file of random bytes?
7. In a decision tree, a split produces two children that are both 50/50. What's the information gain if the parent was also 50/50?

<details>
<summary>Answers</summary>

1. log₂ 4 = 2 bits. Second: ½·1 + ¼·2 + ⅛·3 + ⅛·3 = 1.75 bits.
2. Codes 0, 10, 110, 111: lengths 1, 2, 3, 3, average 1.75 bits, exactly the entropy (all probabilities are powers of ½).
3. −log₂ 0.25 = 2 bits; −ln 0.25 ≈ 1.386 nats.
4. No, KL isn't symmetric. It's 0 only when p = q.
5. 2² = 4.
6. Random bytes already have the maximum entropy (8 bits per byte): there's no redundancy to remove, and by pigeonhole no code can shorten every file.
7. 0: the children are exactly as uncertain as the parent.
</details>

---

## 12. Interview questions

**Q1. What is cross-entropy, and why is it used as a loss for classification?**
> −Σ p log q: the average surprise under the model q when the data comes from p. With one-hot labels it's −log(probability of the true class). Minimising it is maximum likelihood, it heavily punishes confident mistakes, and with softmax its gradient is simply p − y.

**Q2. What is KL divergence? Is it a distance?**
> Σ p log(p/q): the extra cost of using q instead of p. It's ≥ 0 and 0 only when p = q, but it isn't symmetric and doesn't satisfy the triangle inequality, so it isn't a metric.

**Q3. Why does minimising cross-entropy also minimise KL?**
> H(p, q) = H(p) + KL(p‖q), and H(p) is fixed by the data.

**Q4. What is perplexity?**
> 2 to the power of the cross-entropy (in bits): the effective number of equally likely choices the model is unsure between. Lower is better.

**Q5. How does a decision tree choose a split?**
> It picks the feature and threshold with the largest information gain (the drop in entropy) or the largest drop in Gini impurity.

**Q6. What does temperature do in LLM sampling?**
> It divides the logits before the softmax: T < 1 sharpens the distribution (less random), T > 1 flattens it (more random).

---

**Next:** [08 · Theory of Computation](../08-Theory-of-Computation/README.md): automata, grammars, Turing machines, P vs NP.
