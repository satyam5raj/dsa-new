# 09 · Tools, MCP, Context Engineering and Dynamic Tool-Output Compression

| | |
|---|---|
| **Code** | [ToolsMcpContext.java](ToolsMcpContext.java): tool schemas, an MCP-style JSON-RPC server, dynamic tool selection, context assembly under a budget, six tool-output compression strategies on a 75,000-token log, dynamic budgets, offloading (`java ToolsMcpContext.java`, 28 checks) |
| **You need** | [07 · AI agents](../07-AI-Agents/README.md) |
| **Interview weight** | high for agent roles: "how do you stop tool outputs blowing up the context window?" |

An agent is only as good as **(1) the tools it can use** and **(2) what you put in its context window at each step**. This chapter covers both.

---

## 1. Designing tools for LLMs

### The schema is a prompt
The model chooses tools using only their **name, description and parameter schema**:
```json
{"name": "get_order_status",
 "description": "Look up the shipping status of one order. Use when the user asks where their order is.",
 "input_schema": {"type": "object",
   "properties": {"order_id": {"type": "string", "description": "The order number, e.g. A-1234"},
                  "include_items": {"type": "boolean", "description": "Whether to list the items"}},
   "required": ["order_id"]}}
```

### Rules for good tools
| Rule | Why |
|---|---|
| clear **verb_noun names** (`get_order_status`, not `helper2`) | the model picks by name |
| descriptions say **what it does and when to use it** ("Use when…") | disambiguates similar tools |
| describe every parameter, with **examples and formats** | fewer malformed calls |
| use **enums and types** instead of free text | validation is easy |
| **few, high-level tools** beat many low-level ones (`book_meeting` rather than 6 calendar CRUD calls) | fewer steps, fewer errors |
| return **concise, structured, relevant** output (section 5) | context stays small |
| return **helpful errors** ("expected 'number op number'") | the agent can self-correct |
| make side effects **idempotent** and flag **risky** tools | safe retries, approval gates |
| enforce **authorisation inside the tool** (as the end user), not in the prompt | prompts can be bypassed |

---

## 2. MCP: the Model Context Protocol

### The problem it solves
Every AI app (an IDE assistant, a chat app, an agent) used to write custom integrations for every tool (GitHub, Slack, databases). That's N apps × M tools. **MCP** (an open standard introduced by Anthropic in late 2024, now widely adopted) makes it N + M: build an **MCP server** once, and any **MCP client** can use it. Think of it as a "USB-C port for AI applications".

### The architecture
```
Host app (IDE, chat app, your agent)
   └── MCP client  ──JSON-RPC 2.0──▶  MCP server (GitHub, Postgres, your internal API…)
                    (stdio for local, Streamable HTTP for remote)
```

### What a server can offer
| Primitive | Controlled by | Example |
|---|---|---|
| **tools** | the model (it decides to call them) | `search_docs`, `create_ticket` |
| **resources** | the application (data it can attach as context) | files, database rows, API responses |
| **prompts** | the user (reusable templates) | "summarise this PR" |

Clients can also offer features to servers, such as **sampling** (the server asks the client's model to generate something) and **elicitation** (asking the user for input).

### The message flow (in the code)
All messages are **JSON-RPC 2.0**: `{"jsonrpc": "2.0", "id": …, "method": …, "params": …}`.
1. `initialize`: client and server agree on the protocol version and capabilities. Calling anything first is rejected (error **−32002**).
2. `tools/list` → `[search_docs, get_ticket]`: tools are **discovered at runtime**, not hard-coded.
3. `tools/call {name: "get_ticket", arguments: {id: "T-77"}}` → content blocks: *"Ticket T-77: status=open, priority=high"*.
4. Errors use standard codes: unknown tool → **−32602** (invalid params), unknown method → **−32601** (method not found). Each response **echoes the request id**, so asynchronous replies can be matched up.

### MCP security (frequently asked)
- An MCP server runs with **your** permissions: only install trusted servers, and pin versions.
- Tool descriptions and tool outputs can contain **prompt injection** ("tool poisoning").
- Remote servers need proper **auth** (OAuth) and **least privilege**; never pass broad tokens through.
- Log and review tool calls; require approval for risky ones.

---

## 3. Too many tools: dynamic tool selection

Every tool schema costs tokens on **every** request, and models choose worse when there are dozens of similar tools. With 50 tools (5 verbs × 10 entities):
| | Tool-schema tokens per request |
|---|---|
| send all 50 | **2,673** |
| select the relevant 5 | **266** (~90% fewer) |

For *"where is my shipment and can I cancel the order"*, a simple router that matches the **entity** first and then the **action** selects `[cancel_order, cancel_shipment, get_order, get_shipment, create_order]`. In production this is "**tool RAG**": embed the tool descriptions and retrieve the top-k for each request, or route to a sub-agent that owns a group of tools (chapter 08). MCP servers make it easy to load tool groups on demand.

---

## 4. Context engineering

**Context engineering** = deciding, at **each step**, exactly what goes into the context window and in what form. It has largely replaced "prompt engineering" as the key skill for agents, because an agent's context is rebuilt and grows at every step.

### What competes for the window
```
[system prompt] [tool schemas] [long-term memories] [conversation history] [retrieved docs] [tool results] [user message] … [room for the answer]
```

### Budgeting (from the code)
A 16K window, minus 2K reserved for the answer, minus the fixed parts (system 1,200 + tools 700 + memories 300 + message 150) leaves **11,650 tokens** for history and tool results. The full history needs **13,300**: it doesn't fit. **Summarise the old turns (400 tokens) and keep the last 3 verbatim (5,200)**: 5,600 in total, leaving **6,050** for tool results.

### The techniques
| Technique | What it does |
|---|---|
| **write** | save information **outside** the window (scratchpads, memory stores, files) for later |
| **select** | retrieve only what's relevant now (RAG over docs, memories, tools) |
| **compress** | summarise history; shrink tool outputs (section 5) |
| **isolate** | give sub-agents their own clean contexts (chapter 08) |
| **order** | put stable content first (prompt caching), and the most important content at the start or end (lost in the middle) |

Failure modes it prevents: **context overflow** (errors), **context rot / distraction** (quality drops as irrelevant tokens pile up), **context poisoning** (a hallucination saved into the context gets repeated), and runaway cost.

---

## 5. Dynamic tool-output compression

### The problem
Tools often return **far more** than the model needs: logs, HTML pages, database dumps, API JSON with dozens of fields. One call in the demo returns a **5,000-line log, about 75,000 tokens**, while the budget for this tool result is **1,000 tokens**. And the line that matters, `ERROR payment-svc: connection pool exhausted (max=20) order=A-9931`, sits on **line 3,127**, right in the middle.

"Dynamic" means the compression **adapts**: to the remaining budget (section 6), to the type of output, and to **what the agent is currently trying to do**.

### Six strategies, measured
| Strategy | Tokens | Error kept? | Notes |
|---|---|---|---|
| raw output | 75,000 | yes | doesn't fit |
| **head + tail truncation** | 1,008 | **❌ no** | the naive default; loses everything in the middle |
| **dedupe repeated lines** (ignoring timestamps, with counts) | **55** | ✅ | logs are extremely repetitive: `[x4980] … INFO processed request ok` |
| **severity filter** (keep WARN/ERROR, count the rest) | 171 | ✅ | domain-aware rule for logs |
| **query-focused extraction** (rank lines by relevance to the agent's goal "why are payments failing") | 187 | ✅ (first line) | works for any text; this is RAG over the tool output |
| **JSON projection + aggregation** (300 rows: keep `id, status, amount`, show 10, add totals) | 50,509 → **88** | ✅ "300 rows total, 9 failed" | drop huge internal fields; summarise instead of listing |
| **LLM summarisation** (a small, cheap model summarises with the goal in mind) | small | usually | flexible but costs a call and can drop details; keep the key identifiers |

**The key lesson:** naive truncation silently throws away the most important information. **Structure-aware** (dedupe, projection) and **goal-aware** (query-focused) compression keep it at a fraction of the size.

### Where to compress
1. **In the tool itself** (best): return summaries, pagination, field selection (`fields=id,status`), filters (`level=ERROR`).
2. **In a middleware layer** between the tool and the model (a "tool-output processor"): pick the strategy by content type (log, JSON, HTML → main text, table → schema + sample + aggregates).
3. **In the agent loop**: re-compress older tool results as the context fills (they matter less over time).

---

## 6. Dynamic budgets: compress harder as the window fills

```
budget_for_this_tool_result = clamp(remaining_context / 4, floor = 200, cap = 4,000)
```
| Situation (128K window) | Budget per tool result |
|---|---|
| early in a run (2K used) | **4,000** (the cap) |
| nearly full (120K used) | **2,000** |
| almost out (127.5K used) | **200** (the floor) |

Combine this with **re-compressing old results** and **summarising history** so long-running agents never overflow.

---

## 7. Offloading: store it, give the model a handle

Instead of putting the full output in the context, **store it** (in memory, a file, object storage) and give the model:
```
Output stored at artifact://1a2b3c (5000 lines). Preview:
<first 3 lines>
Call read_artifact(handle, query) to search it.
```
That's **under 120 tokens**. Later, the agent calls `read_artifact(handle, "error payments failing")`, and the targeted read **finds the connection-pool error** in the full output. This is **progressive disclosure**: the model pulls detail only when it needs it. Coding agents do the same thing with files: they `grep`, then read the relevant lines.

---

## 8. Interview questions

**Q1. What makes a good tool for an LLM agent?**
> A clear verb_noun name; a description of what it does and when to use it; typed, documented parameters with enums and examples; concise, structured output; actionable error messages; idempotent side effects; authorisation enforced inside the tool; and few high-level tools rather than many low-level ones.

**Q2. What is MCP, and why does it matter?**
> The Model Context Protocol: an open JSON-RPC-based standard for connecting AI applications (clients) to tools, resources and prompts exposed by servers, over stdio or HTTP. It turns N×M custom integrations into N+M, and enables runtime tool discovery.

**Q3. MCP vs A2A?**
> MCP connects an agent to tools and data; A2A connects agents to other agents (discovery via Agent Cards, task exchange). They're complementary.

**Q4. How do you handle huge tool outputs?**
> Compress dynamically: ask the tool for less (filters, fields, pagination); apply structure-aware compression (dedupe logs, project JSON fields, aggregate); goal-aware extraction (rank chunks against the current sub-goal); summarise with a small model as a last resort; or offload the full output and give the model a handle plus a preview to query later. Never blindly truncate: it loses the middle.

**Q5. What is context engineering?**
> Curating what enters the model's context at each step (writing to external memory, selecting relevant items, compressing, isolating in sub-agents, ordering for caching and attention) under a token budget, to keep quality high and cost low.

**Q6. Your agent has 80 tools and keeps picking the wrong one. What do you do?**
> Reduce what it sees: retrieve the top-k relevant tools per request (tool RAG) or route to sub-agents that each own a small toolset; merge low-level tools into higher-level ones; improve names and descriptions; add examples; evaluate tool-selection accuracy.

**Q7. What are the security risks of MCP servers?**
> They run with your credentials; their descriptions and outputs can carry prompt injection; malicious or compromised servers can exfiltrate data. Use trusted, pinned servers, OAuth with least privilege, approval for risky tools, and treat their outputs as untrusted.

---

**Next:** [10 · Guardrails and AI security](../10-Guardrails-AI-Security/README.md)
