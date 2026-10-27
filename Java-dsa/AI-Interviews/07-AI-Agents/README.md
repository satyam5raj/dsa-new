# 07 · AI Agents

| | |
|---|---|
| **Code** | [AiAgents.java](AiAgents.java): a complete ReAct agent with a tool registry, argument validation, error recovery, loop detection, a step budget, human approval, long-term memory, and a plan-and-execute cost comparison (`java AiAgents.java`, 18 checks) |
| **You need** | [01](../01-LLM-Fundamentals/README.md), [02](../02-Prompt-Engineering/README.md) (structured output) |
| **Interview weight** | very high in 2025–26: "agentic" systems are what companies are building |

---

## 1. What an agent is

A plain LLM call is **one question → one answer**. An **agent** is an LLM **in a loop**, able to **use tools** and decide its own next step until a goal is reached:
```
goal ──▶ [ LLM decides: call a tool, or answer? ] ──tool──▶ run it ──▶ result added to context ──┐
               ▲                                                                                   │
               └───────────────────────────────────────────────────────────────────────────────────┘
                                         …until "final answer" (or a limit is hit)
```
**The analogy:** a new employee with a to-do list and access to some apps. They look at the task, use an app, read the result, decide what to do next, and report back when done.

### The building blocks
| Block | What it is |
|---|---|
| **model** | the reasoning engine that picks the next action |
| **instructions** | the system prompt: role, goal, rules, when to stop, when to ask a human |
| **tools** | functions the model can call: APIs, search, code execution, databases (chapter 09) |
| **memory** | short-term (this conversation and trace) and long-term (facts across sessions) |
| **orchestration loop** | your code: calls the model, runs the tools, enforces the limits |

**"Workflow" vs "agent"** (an important distinction):
- A **workflow** follows a path *you* wrote in code (step 1 → step 2 → step 3, perhaps with LLM calls inside): predictable, cheaper, easier to test.
- An **agent** lets the *model* choose the path: flexible, but less predictable.
- **Use the simplest one that works.** Many "agents" in production are really workflows with an LLM at a few decision points.

---

## 2. The ReAct loop: Reason + Act

The most common agent pattern interleaves **thinking** with **tool use**. The real trace from the code, for *"What is the weather in Paris in Fahrenheit?"*:
```
Thought: I need the current weather in Paris.
Action: get_weather {city=Paris}
Observation: 18C, light rain
Thought: Convert 18C to F: F = C * 9/5 + 32. First C * 1.8.
Action: calculator {expression=18 * 1.8}
Observation: 32.4
Thought: Now add 32.
Action: calculator {expression=32.4 + 32}
Observation: 64.4
Final answer: It is 64.4F (18C, light rain) in Paris.
```
**3 tool calls, 4 LLM calls** (one per step plus the final answer). Every step resends the growing trace to the model, so **cost and latency grow with the number of steps**.

### How tool calling works mechanically
1. You describe the tools to the model (name, description, JSON-schema parameters).
2. The model replies with a **structured tool call** instead of text: `{"name": "get_weather", "arguments": {"city": "Paris"}}`.
3. **Your code** validates and runs it (the model never executes anything itself).
4. You append the result as a **tool message** and call the model again.

---

## 3. Making agents reliable: the engineering

Everything in this section is in the code's `Agent` class.

### a) Validate before executing
- **Unknown tool** → return `ERROR: no tool named browse_web. Available: [calculator, get_weather, send_email]` instead of crashing.
- **Missing argument** → `ERROR: missing argument 'city'`, caught before the tool runs.
- In real systems, validate the argument types and ranges against the schema too, and check **authorisation** (is this user allowed to do this?).

### b) Tool errors are observations, not crashes
For "What is 23 times 7?", the model's first call `calculator {expression=23x7}` fails with **"expected 'number op number'"**. Because the error goes back to the model as an observation, it **fixes its own arguments** (`23 * 7`) and answers **161**. Write error messages **for the model**: say what went wrong and what valid input looks like.

### c) Always bound the loop
| Guard | Demo |
|---|---|
| **loop detection** | asked for the weather in Atlantis, the agent repeats the same failing call. The detector stops it after **2** wasted calls instead of 10 |
| **step budget** | `maxSteps = 2` on a 3-step task → "STOPPED: step budget (2) exhausted" |
| also | wall-clock timeouts, token/cost budgets, per-tool rate limits |

An unbounded agent can loop forever and burn money; bounding it is mandatory in production.

### d) Human-in-the-loop for risky actions
Tools are tagged **risky** (`send_email` is irreversible). Before running one, the agent asks a human:
- **denied** → nothing is sent, and the agent says *"I did not send the email because you did not approve it."*;
- **approved** → sent exactly once.

Use approval gates for anything that **spends money, sends messages, deletes or modifies data, or has legal effects**. In LangGraph this is an **interrupt** (chapter 11).

### e) Least privilege
Give each agent **only the tools it needs**, with the **narrowest permissions**: read-only database credentials, a scoped API token, a sandbox for code execution. Prompt injection (chapter 10) can make an agent try to misuse whatever it has access to.

### f) Idempotency
Agents retry. Make side-effecting tools **idempotent** (an idempotency key per action) so a retried "charge the card" doesn't charge twice. This is the same idea as [LLD 18 Payment Wallet](../../LLD/18-Payment-Wallet/).

---

## 4. Memory

| Type | What it holds | Implementation |
|---|---|---|
| **short-term / working** | this conversation and the current trace | the message list in the context window; summarise or trim when it gets long |
| **long-term** | facts and preferences across sessions ("favourite city = Tokyo") | a key-value or vector store; write after important turns; retrieve relevant memories at the start of a run |
| **episodic** | past task runs and their outcomes | logs of previous traces, retrieved when a similar task comes up |
| **procedural** | how to do things | the system prompt, tool descriptions, stored "skills" or playbooks |

**The demo:** session 1 doesn't know the user's favourite city ("I don't know your favourite city yet."). After the fact is saved to long-term memory, session 2 recalls it: *"In your favourite city, Tokyo, it is 25C, sunny."*

**Memory pitfalls:** storing wrong or outdated facts, privacy (users must be able to see and delete what's remembered), and **memory poisoning** (an attacker gets malicious "facts" stored).

---

## 5. Planning patterns

| Pattern | How it works | Trade-off |
|---|---|---|
| **ReAct** | decide one step at a time | adaptive; one big-model call per step |
| **plan-and-execute** | the model writes a plan once; steps run (by code or a cheaper model); re-plan only on failure | fewer expensive calls; less adaptive |
| **reflection / self-critique** | after an answer, a critic step reviews it and the agent revises | higher quality; more calls |
| **tree search** (Tree of Thoughts, LATS) | explore several reasoning paths and keep the best | for hard problems; expensive |
| **routing** | a classifier picks which specialised flow or agent handles the request | cheap and robust |

**Cost demo:** for 6 independent sub-steps, ReAct makes **7** big-model calls; plan-and-execute makes **2** (plan + final answer). If the steps run on a small model, the cost drops from **$0.140 to $0.046** per task (example prices).

---

## 6. Common agent failure modes

| Failure | Mitigation |
|---|---|
| loops and repetition | loop detection, step and cost budgets |
| wrong tool or wrong arguments | clear tool names and descriptions, fewer tools, examples, schema validation |
| hallucinated tool results ("I checked and…") | only trust tool outputs; trace and verify |
| too many tools (the model gets confused) | group tools, route first, load tools dynamically (chapter 09) |
| context overflow from big tool outputs | **tool-output compression** (chapter 09) |
| giving up or declaring success too early | explicit success criteria, verification steps |
| prompt injection via tool results | treat tool output as untrusted data; guardrails (chapter 10) |
| irreversible mistakes | human approval, dry-run modes, undo logs |
| non-determinism | low temperature for tool selection; evals with multiple runs |

---

## 7. Evaluating agents

Measure more than the final answer:
| Level | Metric |
|---|---|
| **final outcome** | task success rate (did it actually book the meeting?), correctness |
| **trajectory** | did it call the right tools, in a sensible order, with correct arguments? number of steps vs optimal |
| **efficiency** | LLM calls, tokens, cost, latency per task |
| **safety** | unauthorised actions attempted, approvals bypassed, data leaked |
| **robustness** | success rate with tool failures injected, with ambiguous inputs |

Use **simulated environments** (mock tools with recorded responses) so agent evals are repeatable, and run each task several times because agents are stochastic. Benchmarks you may hear about: SWE-bench (coding agents), WebArena (web agents), τ-bench (tool-using customer-service agents), GAIA (general assistants).

---

## 8. Interview questions

**Q1. What's the difference between an LLM call, a workflow and an agent?**
> A call is one prompt and one response. A workflow is a fixed code path with LLM calls at chosen points. An agent lets the LLM choose actions in a loop, calling tools until it decides the goal is met. Prefer the simplest that meets the need.

**Q2. Explain the ReAct pattern.**
> The model alternates reasoning ("Thought") with actions (tool calls), observes each result, and repeats until it can give a final answer. It grounds reasoning in real data and lets the agent adapt as it goes.

**Q3. How do you make an agent safe and reliable in production?**
> Validate tool calls against schemas and permissions; return errors as observations; bound the loop (max steps, timeouts, cost limits, loop detection); least-privilege tools; human approval for irreversible actions; idempotent side effects; guardrails against injection; full tracing; and agent evals on success, trajectory and safety.

**Q4. How does tool calling actually work?**
> You pass tool schemas to the model; it returns a structured call (name + JSON arguments); your code validates and executes it and sends the result back as a tool message; the model continues. The model never executes code itself.

**Q5. How do agents remember things?**
> Short-term: the conversation and trace in the context window (summarised as it grows). Long-term: facts and preferences stored in a database or vector store and retrieved when relevant, with user control and protection against poisoning.

**Q6. ReAct vs plan-and-execute?**
> ReAct decides each step with the big model (adaptive but expensive); plan-and-execute plans once and executes the steps more cheaply, re-planning only when needed. Use plan-and-execute for long, predictable tasks.

**Q7. How do you evaluate an agent?**
> Task success on realistic scenarios with mocked tools, plus trajectory checks (right tools and arguments), efficiency (steps, tokens, cost), safety violations, and robustness to tool failures, each over several runs.

---

**Next:** [08 · Multi-agent systems](../08-Multi-Agent-Systems/README.md)
