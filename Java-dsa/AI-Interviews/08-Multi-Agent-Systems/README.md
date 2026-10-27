# 08 · Multi-Agent Systems

| | |
|---|---|
| **Code** | [MultiAgentSystems.java](MultiAgentSystems.java): parallel orchestrator–worker with failure handling, handoffs, a writer/reviewer loop, A2A-style discovery, a shared blackboard, the token cost (`java MultiAgentSystems.java`, 14 checks) |
| **You need** | [07 · AI agents](../07-AI-Agents/README.md); concurrency from [Multithreading](../../Multithreading/) |
| **Interview weight** | high and rising: "when would you use multiple agents?" and "how do agents talk to each other?" |

---

## 1. What a multi-agent system is (and when NOT to build one)

A **multi-agent system** splits work across several agents, each with its **own instructions, tools and context**, coordinated by code, by an orchestrator agent, or by handing off to each other.

**The analogy:** a company. One generalist could do everything, but past a certain complexity you hire specialists (billing, support, legal) and a manager who delegates.

### Good reasons to use multiple agents
| Reason | Example |
|---|---|
| **parallelism**: independent sub-tasks | research 3 topics at once (3× faster in the demo) |
| **specialisation**: different instructions or tools per role | a billing agent can't touch the deployment tools |
| **separation of context**: each agent sees only what it needs | smaller prompts, fewer distractions, less data exposure |
| **too many tools for one agent** | split 40 tools across 4 agents of 10 |
| **independent checking** | a reviewer/critic separate from the writer |
| **organisational boundaries** | agents owned by different teams or vendors (A2A protocol) |

### Bad reasons
- "It sounds more advanced." Multi-agent systems are **harder to debug, slower and more expensive** (section 7).
- A task that is really a sequence: use a workflow.
- **Start with one well-designed agent; split only when evals show it's struggling.** Say this in an interview.

---

## 2. Coordination patterns

| Pattern | Shape | Use for |
|---|---|---|
| **orchestrator–worker** (manager / supervisor) | a lead agent plans, delegates sub-tasks, merges the results | research, report writing, coding across many files |
| **handoffs / router** | a triage agent transfers the conversation to a specialist | customer support, help desks |
| **sequential pipeline** | agent A → B → C, each transforming the output | extract → analyse → write |
| **evaluator–optimiser** (writer / critic) | one produces, another reviews and requests changes | content quality, code review |
| **parallel + vote / debate** | several agents answer independently, then aggregate or argue | higher accuracy on hard questions (chapter 02's voting maths) |
| **hierarchical** | managers of managers | very large tasks |
| **blackboard / shared state** | agents read and write a shared store and react to changes | loosely coupled collaboration |
| **swarm / peer-to-peer** | agents hand off freely to each other | flexible, but the hardest to control |

---

## 3. Orchestrator–worker (from the code)

*"Compare three cloud providers for our ML workload"*:
1. The orchestrator **plans** sub-tasks: GPU pricing, managed ML services, data-residency rules.
2. **Workers run in parallel** (a thread pool), each with its own context.
3. The orchestrator **merges** the findings into one report.

| | Time |
|---|---|
| sequential | **464 ms** |
| parallel | **156 ms** (about 3× faster for 3 independent sub-tasks) |

### Handling worker failure
One worker fails ("managed ML services"). The orchestrator **retries once**, then **continues with partial results and reports the gap**:
```
Report on: Compare three cloud providers for our ML workload | 2 sections | MISSING: [managed ML services]
```
**Never silently drop a failed sub-task**: the final answer would look complete but be wrong. Use timeouts per worker (`future.get(timeout)`), bounded retries, and explicit "unknown" sections.

---

## 4. Handoffs

A cheap **triage** agent (or classifier) reads the message and transfers it:
| Message | Routed to |
|---|---|
| "I was charged twice, I want a refund" | **billing** |
| "The app crashes on start" | **tech_support** |
| anything else | general |

The **context travels with the handoff** (the specialist sees "user said: I was charged twice…"), so the user doesn't repeat themselves. In the OpenAI Agents SDK and Google ADK, a handoff is modelled as a special tool call (`transfer_to_billing`); in LangGraph it's an edge to another node.

**Design tips:** keep the triage model small and fast; give it an "I'm not sure → ask a clarifying question" option; log every handoff; avoid ping-pong (A → B → A) with a hop limit.

---

## 5. Evaluator–optimiser: the writer/reviewer loop

A writer drafts; a reviewer checks a list of criteria and returns issues; the writer revises:
```
round 0: "Announcing our new Pro plan with faster support."   → issues: mention the price, state the deadline
round 1: "… The price is $40/month. Offer ends Friday (deadline)."   → issues: none → approved
```
Issues per round **[2, 0]**: approved after **1** revision. **Always cap the rounds** (here 3): two LLMs can disagree forever. Give the reviewer **concrete, checkable criteria**; vague critics ("make it better") cause endless churn.

---

## 6. How agents communicate: protocols

### Inside one application
Direct function calls, a shared state object (LangGraph state), message queues, or a **blackboard**. In the demo, a flight agent and a hotel agent write to a shared map concurrently, and a planner waits for both (a `CountDownLatch`) and combines them: *"Itinerary: LH400 Mon 09:00 + Hotel Adlon, 3 nights"*. Shared state needs the usual concurrency care: thread-safe structures, clear ownership of each key, versioning.

### Between applications: A2A (Agent2Agent)
**A2A** is an open protocol (introduced by Google in 2025 and now under the Linux Foundation) for agents built by **different teams or vendors, on different frameworks**, to work together.
- Each agent publishes an **Agent Card** (a JSON document, usually served at a well-known URL) describing its name, endpoint, **skills**, and authentication requirements.
- Clients **discover** agents by reading cards, then send **tasks** (with messages and artifacts) over HTTP (JSON-RPC), with streaming and push notifications for long-running work.
- The agents stay **opaque**: they don't share internal memory or tools, just tasks and results.

The demo reads a registry of agent cards and picks **the cheapest agent with the `book_flight` skill** (`cheap-travel`). When **no agent** has a skill (`approve_budget`), it escalates to a human.

### A2A vs MCP (a very common question)
| | **MCP** (Model Context Protocol) | **A2A** |
|---|---|---|
| connects | an agent/app ↔ **tools and data** | an agent ↔ **another agent** |
| the other side is | a tool server (functions, resources, prompts) | an autonomous agent with its own reasoning |
| analogy | plugging in a USB device | calling a colleague in another department |
| chapter | [09](../09-Tools-MCP-Context-Engineering/README.md) | here |

They're complementary: an agent can use MCP for its tools and A2A to delegate to other agents. (Protocol details evolve; check the current specs before relying on exact field names.)

---

## 7. The costs and risks of multi-agent systems

### Tokens
Every agent needs its own instructions and context, and the orchestrator re-reads the workers' outputs:
| Setup | Tokens per task (illustrative) |
|---|---|
| one agent | **2,500** |
| orchestrator + 3 workers + synthesis | **17,200** (≈ **6.9×**) |

Multi-agent research systems commonly use several times more tokens than single agents. It's worth it only when the task's value justifies it.

### Other risks
| Risk | Mitigation |
|---|---|
| errors compound across agents | verify at each hand-over; structured outputs between agents |
| lost context in handoffs | pass a structured summary; share state explicitly |
| infinite delegation loops | hop limits, budgets, termination conditions |
| hard to debug | **distributed tracing** across agents (one trace ID per user request; chapter 12) |
| prompt injection spreads agent to agent | treat other agents' outputs as untrusted; least privilege per agent |
| responsibility gaps | one owner agent accountable for the final answer |
| latency | parallelise independent work; use small models for routing |

---

## 8. Frameworks for multi-agent systems

| Framework | Style |
|---|---|
| **LangGraph** | an explicit graph of agent nodes with shared state; supervisor and swarm patterns (chapter 11) |
| **Google ADK** (Agent Development Kit) | `SequentialAgent`, `ParallelAgent`, `LoopAgent`, LLM-driven delegation to sub-agents; deploys to Cloud Run / Agent Engine (chapter 13) |
| **OpenAI Agents SDK** | agents + handoffs + guardrails + tracing |
| **CrewAI** | role-based "crews" with tasks |
| **AutoGen / AG2** | conversational multi-agent programming |
| **Semantic Kernel**, **Spring AI**, **LangChain4j** | enterprise and Java-friendly options |

---

## 9. Interview questions

**Q1. When would you use multiple agents instead of one?**
> When sub-tasks are independent (parallelism), need different tools, permissions or instructions (specialisation), when one agent has too many tools or too much context, when you want independent review, or across team/vendor boundaries. Otherwise one agent or a workflow is simpler, cheaper and easier to debug.

**Q2. Describe the orchestrator–worker pattern.**
> A lead agent decomposes the goal into sub-tasks, dispatches them to workers (often in parallel, each with its own context), then synthesises the results, handling failures with timeouts, retries and explicit gaps.

**Q3. What is a handoff?**
> Transferring control of a conversation from one agent to another (usually a specialist) along with the relevant context, typically implemented as a tool call or graph edge, with hop limits to avoid ping-pong.

**Q4. What's the difference between MCP and A2A?**
> MCP standardises how an agent connects to tools and data sources; A2A standardises how independent agents discover each other (Agent Cards) and exchange tasks. They're complementary.

**Q5. What are the downsides of multi-agent systems?**
> Many times more tokens and latency, compounding errors, lost context, coordination loops, harder debugging and evaluation, and a wider attack surface for injection.

**Q6. How do you debug a multi-agent system?**
> End-to-end distributed tracing with one trace ID per request, structured messages between agents, logging every handoff and tool call, replaying recorded traces, and evaluating each agent in isolation plus the system end to end.

---

**Next:** [09 · Tools, MCP, context engineering and tool-output compression](../09-Tools-MCP-Context-Engineering/README.md)
