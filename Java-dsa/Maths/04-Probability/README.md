# 04 · Probability

| | |
|---|---|
| **Code** | [Probability.java](Probability.java): every formula below next to a seeded simulation that agrees with it (`java Probability.java`, 45 checks) |
| **You need** | [00 · Foundations](../00-Foundations/README.md), counting from [01 · Discrete Maths](../01-Discrete-Math/README.md), integrals from [03 · Calculus](../03-Calculus/README.md) |
| **Used in** | ML (every model outputs probabilities; losses come from probability), randomised algorithms and hashing, A/B testing, networking, LLM sampling, reinforcement learning |

Probability is the maths of **uncertainty**. ML models don't say "this is a cat"; they say "92% cat". To understand what that means, how it's trained, and when to trust it, you need probability.

**A tip that makes probability easy:** when in doubt, **simulate**. The code checks every formula by running the experiment a million times. If the formula and the simulation disagree, the formula is wrong.

---

## 1. The basics

### Sample space, events, probability
| Term | Meaning | Example: roll one die |
|---|---|---|
| **experiment** | something with an uncertain result | rolling a die |
| **sample space** Ω | all possible outcomes | {1, 2, 3, 4, 5, 6} |
| **event** | a set of outcomes | "even" = {2, 4, 6} |
| **P(event)** | a number from 0 (impossible) to 1 (certain) | P(even) = 3/6 = 0.5 |

When all outcomes are equally likely:
```
P(A) = (number of outcomes in A) / (total number of outcomes)
```
Two dice summing to 7: (1,6), (2,5), (3,4), (4,3), (5,2), (6,1), which is 6 of 36, so **1/6 ≈ 0.1667**.

### The three axioms (all of probability follows from these)
1. P(A) ≥ 0.
2. P(Ω) = 1 (something happens).
3. If A and B can't both happen: P(A or B) = P(A) + P(B).

### Rules you'll use all the time
| Rule | Formula |
|---|---|
| **complement** | P(not A) = 1 − P(A) |
| union (inclusion–exclusion) | P(A ∪ B) = P(A) + P(B) − P(A ∩ B) |
| independent events | P(A ∩ B) = P(A) · P(B) |

### The complement trick
"At least one" problems are easier backwards:
```
P(at least one 6 in 4 rolls) = 1 − P(no 6 in 4 rolls) = 1 − (5/6)⁴ = 0.5177
```
The simulation (1,000,000 tries) gives 0.52.

---

## 2. Conditional probability and independence

### Conditional probability: probability with extra information
**P(A | B)** = "the probability of A, **given that** B happened":
```
P(A | B) = P(A and B) / P(B)
```
**Picture:** B shrinks the world. Of the outcomes where B happened, what fraction also have A?

**Example:** P(sum = 8 | first die = 3). Given the first die is 3, there are 6 equally likely outcomes, and only (3, 5) sums to 8: **1/6**. Without the condition, P(sum = 8) = 5/36 ≈ 0.1389. The information changed the probability.

### Independence
A and B are **independent** if knowing one tells you nothing about the other:
```
P(A | B) = P(A)      equivalently      P(A and B) = P(A) · P(B)
```
| Events | Independent? |
|---|---|
| first die is even, second die is > 4 | **yes**: P(both) = 1/2 · 1/3 = 1/6 |
| sum ≥ 10, first die is 6 | **no**: P(sum ≥ 10) = 1/6, but P(sum ≥ 10 | first = 6) = **1/2** |

**Warning:** independent ≠ mutually exclusive. Mutually exclusive events (can't both happen) are strongly *dependent*: if one happens, the other definitely didn't.

### The multiplication rule
`P(A and B) = P(A) · P(B | A)`. Chaining it gives `P(A, B, C) = P(A) · P(B|A) · P(C|A,B)`. **This is exactly how a language model scores a sentence**: P(word₁) · P(word₂ | word₁) · P(word₃ | word₁, word₂) · …

---

## 3. Bayes' theorem: updating beliefs with evidence

### The formula
```
P(A | B) = P(B | A) · P(A) / P(B)

posterior = likelihood × prior / evidence
```
| Part | Meaning |
|---|---|
| **prior** P(A) | what you believed before the evidence |
| **likelihood** P(B \| A) | how likely the evidence is if A is true |
| **evidence** P(B) | how likely the evidence is overall |
| **posterior** P(A \| B) | what you believe after seeing the evidence |

P(B) is usually found with the **law of total probability**: `P(B) = P(B | A)·P(A) + P(B | not A)·P(not A)`.

### The medical test paradox
A disease affects **1%** of people. A test catches **99%** of sick people, but also flags **5%** of healthy people. You test positive. How likely is it that you're sick?

```
P(positive) = 0.99 · 0.01 + 0.05 · 0.99 = 0.0594
P(sick | positive) = 0.99 · 0.01 / 0.0594 = 0.1667        only 16.7%!
```
**Why so low?** Picture 10,000 people: 100 are sick (99 test positive) and 9,900 are healthy (**495** test positive). Of the 594 positives, only 99 are sick. The rare disease is swamped by false alarms from the huge healthy group. The simulation on 1,000,000 people agrees (0.1646).

**Test again:** use 0.1667 as the new prior, and a second positive gives **0.7984**. Each piece of evidence updates the belief: that's Bayesian reasoning.

**Lessons:** base rates matter enormously (ignoring them is the "base rate fallacy"). This is why fraud detection and anomaly detection, where the positives are rare, are hard, and why accuracy alone is a misleading metric.

### Naive Bayes: a spam filter in one formula
Assume words appear independently given the class (that's the "naive" part):
```
P(spam | words) ∝ P(spam) · Π P(word | spam)
```
With P(spam) = 0.4, P("free" | spam) = 0.30, P("win" | spam) = 0.20, P("free" | ham) = 0.02, P("win" | ham) = 0.01:
```
spam: 0.4 · 0.30 · 0.20 = 0.024        ham: 0.6 · 0.02 · 0.01 = 0.00012
P(spam | "free win") = 0.024 / (0.024 + 0.00012) = 0.9950
```
It was one of the first successful ML methods, and it's still a strong baseline for text classification. (Real implementations add logs, from chapter 00, to avoid underflow.)

---

## 4. Famous puzzles that break intuition

### The birthday paradox
How many people do you need for a 50% chance that two share a birthday? Only **23**.
```
P(all 23 different) = 365/365 · 364/365 · … · 343/365        P(some match) = 0.5073
```
**Why:** you're not comparing against *your* birthday but checking **all pairs**: 23 people make C(23, 2) = 253 pairs.

**CS impact:** hash collisions appear after only about **√(table size)** items. A 32-bit hash has 4 billion values but collides after about 77,000 items. "Birthday attacks" on cryptographic hashes exploit this, which is why secure hashes have 256 bits.

### Monty Hall
Three doors: one car, two goats. You pick a door. The host, who **knows** where the car is, opens another door with a goat and offers you a switch. Should you?

**Yes: switching wins 2/3 of the time.** Simulation over 300,000 games: stay **0.33**, switch **0.67**.

**Why:** your first pick is right 1/3 of the time. The host's action doesn't change that, because he can always show a goat. So the other closed door carries the remaining 2/3.

---

## 5. Random variables, expectation and variance

### Random variables
A **random variable** X assigns a number to each outcome: the value of a die, the number of heads in 10 flips, a server's response time.
- **discrete**: countable values (die rolls, counts), described by a **PMF**, P(X = x);
- **continuous**: any value in a range (height, time), described by a **PDF** p(x), where probability = the area under it (an integral);
- **CDF**, F(x) = P(X ≤ x): works for both.

### Expectation (the mean): the long-run average
```
E[X] = Σ x · P(X = x)            (continuous: ∫ x · p(x) dx)
E[die] = (1 + 2 + 3 + 4 + 5 + 6)/6 = 3.5
```
You never roll 3.5, but it's the average over many rolls.

**A bad bet:** win $10 with probability 0.1, lose $2 otherwise: E = 1 − 1.8 = **−0.80** per play. Casinos, insurance and RL rewards all run on expected values.

### Linearity of expectation (the superpower)
```
E[X + Y] = E[X] + E[Y]          ALWAYS, even if X and Y are dependent
E[aX + b] = a·E[X] + b
```
**The hat-check problem:** n people get their hats back at random. Expected number who get their own? Let Xᵢ = 1 if person i does: E[Xᵢ] = 1/n. Then E[total] = n · 1/n = **1**, for any n. The simulation with n = 10 gives 1.00. No need to analyse the messy dependencies.

### Variance: how spread out
```
Var(X) = E[(X − E[X])²] = E[X²] − (E[X])²         standard deviation σ = √Var
Var(die) = 91/6 − 3.5² = 35/12 ≈ 2.9167
```
| Rule | Note |
|---|---|
| `Var(aX + b) = a²·Var(X)` | shifting adds no spread; scaling by a multiplies variance by a² |
| `Var(X + Y) = Var(X) + Var(Y)` | **only if X and Y are independent** (otherwise add 2·Cov) |

---

## 6. The distributions you'll meet

### Discrete
| Distribution | Models | P(X = k) | Mean | Variance |
|---|---|---|---|---|
| **Bernoulli(p)** | one yes/no trial | p or 1−p | p | p(1−p) |
| **Binomial(n, p)** | number of successes in n trials | C(n,k) pᵏ(1−p)ⁿ⁻ᵏ | np | np(1−p) |
| **Geometric(p)** | trials until the first success | (1−p)ᵏ⁻¹ p | 1/p | (1−p)/p² |
| **Poisson(λ)** | events in a fixed time (calls, requests, typos) | λᵏ e⁻λ / k! | λ | λ |
| **Categorical** | one of K classes (a die, a softmax output) | pₖ | | |
| **Multinomial** | counts of each class over n trials | | | |

Examples from the code:
- Binomial(10, 0.5): P(exactly 3 heads) = 120/1024 = **0.1172**.
- Binomial(10, 0.7): P(at least 8) = **0.3828**.
- Binomial(20, 0.3): simulated mean **6.0** = np.
- Geometric: average rolls until a six = **6.0** = 1/p.
- Poisson(3) (a server gets 3 requests per second on average): P(0 requests in a second) = e⁻³ = **0.0498**, P(exactly 2) = **0.2240**.

### Continuous
| Distribution | Models | Density | Mean | Variance |
|---|---|---|---|---|
| **Uniform(a, b)** | "anything in a range" | 1/(b−a) | (a+b)/2 | (b−a)²/12 (1/12 on [0,1]) |
| **Normal / Gaussian (μ, σ²)** | sums of many small effects, noise | (1/σ√2π) e^(−(x−μ)²/2σ²) | μ | σ² |
| **Exponential(λ)** | waiting time between Poisson events | λe^(−λx) | 1/λ | 1/λ² |
| **Beta(α, β)** | a probability itself (e.g. a click-through rate) | on [0, 1] | α/(α+β) | |
| **Dirichlet** | a whole probability vector (topic models) | | | |

### The normal distribution: the 68–95–99.7 rule
For a bell curve, the share of values within 1, 2 and 3 standard deviations of the mean:
```
±1σ: 68.27%      ±2σ: 95.45%      ±3σ: 99.73%
```
1,000,000 simulated samples give 68.28%, 95.45%, 99.73%. Outliers beyond 3σ are rare, which is the idea behind anomaly detection and "six sigma".

**Why it's everywhere:** the Central Limit Theorem (section 8).

**Sampling trick (inverse CDF):** if U is uniform on (0, 1), then −ln(1 − U)/λ is Exponential(λ). The code uses it and gets a mean of **0.50** for λ = 2. Generators turn uniform random numbers into any distribution this way.

---

## 7. Joint distributions, covariance and correlation

### Joint, marginal, conditional
A **joint distribution** gives probabilities for combinations:
| | umbrella | no umbrella | **marginal** |
|---|---|---|---|
| sunny | 0.05 | 0.55 | 0.60 |
| rainy | 0.30 | 0.10 | **0.40** |

- **Marginal:** sum over the other variable: P(rainy) = 0.30 + 0.10 = 0.40.
- **Conditional:** P(umbrella | rainy) = 0.30/0.40 = **0.75**.

### Covariance and correlation
```
Cov(X, Y) = E[(X − μₓ)(Y − μᵧ)]          positive: they move together; negative: opposite
Corr(X, Y) = Cov(X, Y) / (σₓ σᵧ)          always between −1 and +1, unit-free
```
| Pair | Correlation |
|---|---|
| x and 2x + small noise | **0.97** |
| x and an independent z | ≈ 0 |
| x and **x²** | ≈ **0**, even though x² is completely determined by x! |

**Correlation only measures *linear* relationships.** x² rises on both sides of 0, so the linear trends cancel. Zero correlation does **not** mean independent. And correlation is not causation.

The **covariance matrix** of a vector of features (variances on the diagonal, covariances off it) is what PCA diagonalises ([chapter 02](../02-Linear-Algebra/README.md)).

---

## 8. The two big theorems

### Law of Large Numbers (LLN)
The average of many independent samples approaches the true mean. Running averages of die rolls: n = 10: 4.00; n = 1,000: 3.51; n = 1,000,000: **3.50**.

That's why Monte Carlo estimates work, why casinos always win in the long run, and why more training data gives more reliable estimates.

### Central Limit Theorem (CLT)
Take the **average of n independent samples** from *almost any* distribution. For large n, that average is approximately **normal**, with
```
mean = μ,      standard deviation = σ / √n
```
The code averages 30 uniform numbers (individually flat, nothing like a bell curve), 200,000 times:
- the standard deviation of those averages is **0.0527** = √(1/12)/√30 ✓;
- **68%** of them land within one standard deviation, the normal-distribution signature ✓.

**Why it matters:**
- the normal distribution is everywhere because many effects add up;
- **error bars:** the uncertainty of an average shrinks like 1/√n, so 4× more data halves it;
- it's the basis of confidence intervals and A/B tests (chapter 05);
- mini-batch gradients in ML are averages, so their noise also shrinks like 1/√(batch size).

---

## 9. Markov chains

### The idea
A **Markov chain** moves between states randomly, where the next state depends **only on the current state**, not on the history ("memoryless"). It's described by a **transition matrix**:
```
               to sunny  to rainy
from sunny   [   0.9       0.1   ]
from rainy   [   0.5       0.5   ]
```

### The steady state
Run the chain long enough and the fraction of time in each state settles to the **stationary distribution** π, which satisfies `π = πP`: an **eigenvector with eigenvalue 1** ([chapter 02](../02-Linear-Algebra/README.md)). For the weather: **(5/6, 1/6) = (0.8333, 0.1667)**. A 1,000,000-day simulation is sunny 0.83 of the time.

### PageRank: Google's original algorithm
A "random surfer" clicks links at random (with probability 0.85) or jumps to a random page (0.15). A page's rank is the fraction of time the surfer spends there: the steady state of that Markov chain. For 4 pages with links 0→{1,2}, 1→2, 2→0, 3→2:
```
rank = [0.3725, 0.1958, 0.3941, 0.0375]
```
Page 2, which most pages link to, ranks highest; page 3, which nobody links to, ranks lowest.

**More uses:** text generation (an LLM is a Markov chain over a very long state), MCMC sampling in Bayesian ML, queueing models, and Markov Decision Processes in reinforcement learning (chapter 09).

---

## 10. Probability in algorithms

| Idea | Example | Result |
|---|---|---|
| **random pivot** | quicksort | expected about **2n ln n** comparisons for any input (the code: 2,240,099 for n = 100,000, vs n ln n = 1,151,293) |
| **reservoir sampling** | pick a uniform item from a stream of unknown length | keep item i with probability 1/(i+1); each of 10 items ended up ~10% |
| **hashing** | expected O(1) lookups | needs a good spread of hash values; collisions follow the birthday bound |
| **Bloom filters** | false-positive rate ≈ (1 − e^(−kn/m))ᵏ | [12-Advanced Bloom filter](../../DSA/12-Advanced/01-Basics/BloomFilterBasics.java) |
| **Monte Carlo** | estimate π, high-dimensional integrals | error shrinks like 1/√samples |
| **Fisher–Yates shuffle** | a uniformly random permutation | the code's `shuffled` |

---

## 11. Exercises (answers below)

1. Two cards are drawn without replacement. What's P(both aces)?
2. P(A) = 0.5, P(B) = 0.4, P(A ∩ B) = 0.2. Are A and B independent? What's P(A ∪ B)?
3. 2% of emails are phishing. A filter catches 95% of phishing and wrongly flags 1% of normal emails. If it flags an email, what's P(phishing)?
4. A coin with P(heads) = 0.6 is flipped 5 times. P(exactly 4 heads)?
5. X has E[X] = 4 and Var(X) = 9. Find E[3X − 2] and Var(3X − 2).
6. A server receives 2 requests per second on average (Poisson). P(more than 0 requests in one second)?
7. You average 100 independent measurements, each with σ = 10. What's the standard deviation of the average?
8. Why does zero correlation not imply independence? Give an example.

<details>
<summary>Answers</summary>

1. 4/52 · 3/51 = 12/2652 ≈ 0.0045.
2. P(A)P(B) = 0.2 = P(A ∩ B), so yes. P(A ∪ B) = 0.5 + 0.4 − 0.2 = 0.7.
3. (0.95 · 0.02) / (0.95 · 0.02 + 0.01 · 0.98) = 0.019 / 0.0288 ≈ 0.66.
4. C(5,4) · 0.6⁴ · 0.4 = 5 · 0.1296 · 0.4 ≈ 0.2592.
5. E = 3·4 − 2 = 10; Var = 9·9 = 81.
6. 1 − e⁻² ≈ 0.8647.
7. 10/√100 = 1.
8. Correlation measures only linear relationships. For X symmetric around 0, Y = X² is fully determined by X, but Corr(X, Y) = 0.
</details>

---

## 12. Interview questions

**Q1. Explain Bayes' theorem with an example.**
> P(A|B) = P(B|A)P(A)/P(B). With a 1% disease rate, a 99%-sensitive test and a 5% false-positive rate, a positive result means only about a 17% chance of disease, because healthy people vastly outnumber sick ones.

**Q2. What's the difference between independent and mutually exclusive?**
> Independent: one tells you nothing about the other (P(A∩B) = P(A)P(B)). Mutually exclusive: they can't both happen (P(A∩B) = 0), which makes them highly dependent.

**Q3. What does the Central Limit Theorem say, and why does it matter?**
> The average of many independent samples is approximately normal with standard deviation σ/√n, whatever the original distribution. It justifies confidence intervals, A/B tests and Gaussian noise models, and explains why more data shrinks uncertainty like 1/√n.

**Q4. How many items can you hash into a 32-bit space before collisions are likely?**
> About √(2³²) ≈ 65,000–77,000, by the birthday paradox.

**Q5. What is linearity of expectation, and why is it so useful?**
> E[X+Y] = E[X] + E[Y] even for dependent variables. You can split a complicated count into simple indicator variables and add their expectations.

**Q6. How do you sample uniformly from a stream whose length you don't know?**
> Reservoir sampling: keep the i-th item with probability 1/i (or k/i for k items), replacing the current choice.

**Q7. How is PageRank computed?**
> As the stationary distribution of a random-surfer Markov chain (follow a link with probability 0.85, jump anywhere with 0.15), found by power iteration.

---

**Next:** [05 · Statistics](../05-Statistics/README.md): learning from data: estimation, MLE, hypothesis tests, regression.
