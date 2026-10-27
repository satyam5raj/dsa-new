# 11 · LangChain, LangGraph and Other Frameworks

| | |
|---|---|
| **Code** | [MiniLangChain.java](MiniLangChain.java): LangChain's `Runnable`/pipe (LCEL), the RAG chain shape, fallbacks, and a LangGraph-style `StateGraph` with conditional edges, per-thread checkpoints, interrupt/resume and a recursion limit, rebuilt in ~200 lines so you can see what the frameworks do (`java MiniLangChain.java`, 10 checks) |
| **You need** | [04 · RAG](../04-RAG/README.md), [07 · agents](../07-AI-Agents/README.md) |
| **Interview weight** | high: "have you used LangChain/LangGraph?" and "why or why not use a framework?" |

Frameworks save you from rewriting the same plumbing: prompt templates, model clients, retrievers, tool calling, memory, agent loops, tracing. Interviewers want to know **that you understand what's underneath**, so you can debug it, and **when a framework helps vs hurts**.

> API names change quickly in this ecosystem. The snippets below show the stable *concepts* and typical current usage; check the docs for the version you use.

---

## 1. LangChain: the core idea

**LangChain** provides standard interfaces for LLM-app building blocks, plus integrations with hundreds of models, vector stores and tools.

| Concept | What it is |
|---|---|
| **chat models** | one interface over OpenAI, Anthropic, Google, local models… (`invoke`, `stream`, `batch`) |
| **prompt templates** | `ChatPromptTemplate` with `{variables}` |
| **output parsers / structured output** | `StrOutputParser`, `.with_structured_output(PydanticModel)` |
| **retrievers** | anything that maps query → documents (vector stores, BM25, hybrid) |
| **document loaders / text splitters** | ingestion (PDF, web, …) and chunking (`RecursiveCharacterTextSplitter`) |
| **tools** | functions with schemas (`@tool`) that models can call (`.bind_tools`) |
| **Runnables / LCEL** | the composition system (below) |
| **callbacks / tracing** | hooks for logging, streaming, LangSmith |

### LCEL: everything is a Runnable, and `|` composes them
Every component implements one interface (`invoke`, `batch`, `stream`), and `a | b` means "feed a's output into b". The code rebuilds exactly this:
```java
interface Runnable<I, O> {
    O invoke(I input);
    default <R> Runnable<I, R> pipe(Runnable<O, R> next) { return in -> next.invoke(invoke(in)); }   // the "|" operator
}
```
`promptTemplate | model | parser` with `{topic: "owls"}` → `"answer to [Tell me a fact about owls]"`.

In Python:
```python
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_google_vertexai import ChatVertexAI          # or ChatOpenAI, ChatAnthropic, …

prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a concise assistant."),
    ("user", "Tell me a fact about {topic}"),
])
chain = prompt | ChatVertexAI(model="gemini-2.5-flash") | StrOutputParser()
chain.invoke({"topic": "owls"})
```

### The classic RAG chain
```python
from langchain_core.runnables import RunnablePassthrough

rag_chain = (
    {"context": retriever | format_docs, "question": RunnablePassthrough()}   # RunnableParallel
    | prompt
    | llm
    | StrOutputParser()
)
rag_chain.invoke("How long does a refund take?")
```
A dict of runnables runs them **in parallel on the same input**: the retriever fetches the context while `RunnablePassthrough` forwards the original question. The demo shows both reaching the prompt: `Context: Refunds take 5 business days. / Question: How long does a refund take?`.

### Reliability helpers
- `.with_fallbacks([backup])`: if the primary model fails (e.g. a 429), try another. In the demo the backup model answers.
- `.with_retry(...)`, `.batch([...])` (concurrent), `.stream(...)` (token streaming), `.with_structured_output(Schema)`.

---

## 2. LangGraph: agents as explicit state machines

LangChain chains are linear pipelines. Real agents **loop, branch, pause and resume**. **LangGraph** models an agent as a **graph**:
| Concept | Meaning |
|---|---|
| **state** | a typed dict that flows through the graph (messages, intent, results…), with reducers for merging updates |
| **nodes** | functions `state → updated state` (an LLM call, a tool call, any code) |
| **edges** | fixed transitions `A → B` |
| **conditional edges** | a router function picks the next node from the state |
| **START / END** | entry and exit |
| **checkpointer** | saves the state after every step, per **thread_id**, for memory, resumption and time travel |
| **interrupt** | pause the graph (e.g. for human approval) and resume later with `Command(resume=…)` |

### The demo: a support graph
```
START → classify ──(intent = faq)────▶ faq ────▶ END
                 └─(intent = refund)─▶ refund ─▶ END
                                         └── interrupt: "Approve a refund of $120?"
```
| Run | Result |
|---|---|
| thread-1: "What are your opening hours?" | path `[classify, faq]` → "Here is our FAQ answer." |
| thread-2: "I want a refund" ($120) | **pauses**: "Approve a refund of $120?"; state checkpointed |
| thread-2 resumed with `approved = true` (could be hours later, on another server) | path `[classify, refund]` → "Refund of $120 issued." |
| thread-1's state | unchanged: threads are isolated |
| a node that loops to itself forever | **"recursion limit reached"** (LangGraph also has a recursion limit) |

In Python:
```python
from typing import TypedDict
from langgraph.graph import StateGraph, START, END
from langgraph.checkpoint.memory import MemorySaver     # Postgres/Redis/etc. savers in production
from langgraph.types import interrupt, Command

class State(TypedDict, total=False):
    message: str
    intent: str
    amount: int
    reply: str

def classify(state: State):
    return {"intent": "refund" if "refund" in state["message"] else "faq"}

def refund(state: State):
    approved = interrupt(f"Approve a refund of ${state['amount']}?")   # pauses here
    return {"reply": "Refund issued." if approved else "Refund declined."}

g = StateGraph(State)
g.add_node("classify", classify)
g.add_node("faq", lambda s: {"reply": "Here is our FAQ answer."})
g.add_node("refund", refund)
g.add_edge(START, "classify")
g.add_conditional_edges("classify", lambda s: s["intent"], {"faq": "faq", "refund": "refund"})
g.add_edge("faq", END)
g.add_edge("refund", END)
app = g.compile(checkpointer=MemorySaver())

cfg = {"configurable": {"thread_id": "thread-2"}}
app.invoke({"message": "I want a refund", "amount": 120}, cfg)   # stops at the interrupt
app.invoke(Command(resume=True), cfg)                              # human approved → continues
```
LangGraph also ships prebuilt agents (a ReAct agent: an LLM node plus a tool node in a loop), **multi-agent** patterns (supervisor, swarm, handoffs), **streaming** of tokens and intermediate steps, and **LangGraph Platform** for deployment. **LangSmith** provides tracing and evals for both.

---

## 3. Java frameworks (relevant for Java/Spring developers)

### Spring AI
Spring's official AI project: a portable `ChatClient` over many providers, with Spring Boot auto-configuration.
```java
@RestController
class SupportController {
    private final ChatClient chat;

    SupportController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chat = builder
                .defaultSystem("You are Acme's support assistant. Answer only from the provided context.")
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))        // RAG: retrieve and add context
                .build();
    }

    @GetMapping("/ask")
    String ask(@RequestParam String q) {
        return chat.prompt().user(q).call().content();
    }

    record Ticket(String category, boolean urgent) {}

    @PostMapping("/classify")
    Ticket classify(@RequestBody String text) {
        return chat.prompt().user("Classify this ticket: " + text).call().entity(Ticket.class);   // structured output
    }
}

class OrderTools {
    @Tool(description = "Get the shipping status of an order by its id")
    String orderStatus(String orderId) { return "shipped"; }
}
// chat.prompt().user(q).tools(new OrderTools()).call().content();
```
Key pieces: `ChatClient`, **advisors** (RAG, chat memory, logging, guardrails as interceptors), `VectorStore` abstraction (pgvector, Redis, Elasticsearch…), `@Tool` methods, `.entity(Class)` for structured output, MCP client and server support, and observability via Micrometer.

### LangChain4j
A Java port of LangChain's ideas, with a very Java-friendly "AI Services" style: declare an interface, and LangChain4j implements it.
```java
interface Assistant {
    @SystemMessage("You are a helpful travel assistant.")
    String chat(@UserMessage String message);
}

class WeatherTools {
    @Tool("Get the current weather for a city")
    String weather(String city) { return "18C, light rain"; }
}

Assistant assistant = AiServices.builder(Assistant.class)
        .chatModel(model)                                        // e.g. a Gemini, OpenAI or Ollama chat model
        .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
        .tools(new WeatherTools())
        .contentRetriever(retriever)                             // RAG
        .build();

assistant.chat("Do I need an umbrella in Paris?");
```

### Google ADK (Agent Development Kit)
Google's open-source framework for building agents (Python and Java), deployable to **Cloud Run** or Vertex AI **Agent Engine** (chapter 13).
```python
from google.adk.agents import Agent

def get_weather(city: str) -> dict:
    """Returns the current weather for a city."""
    return {"city": city, "weather": "18C, light rain"}

root_agent = Agent(
    name="weather_agent",
    model="gemini-2.5-flash",
    description="Answers weather questions.",
    instruction="Use the get_weather tool, then answer in one sentence.",
    tools=[get_weather],
)
```
ADK also provides workflow agents (`SequentialAgent`, `ParallelAgent`, `LoopAgent`), sub-agent delegation, sessions and memory, evaluation, and a dev UI (`adk web`).

### Others to recognise
| Framework | Known for |
|---|---|
| **LlamaIndex** | data/RAG-first: ingestion, indexes, query engines (`VectorStoreIndex.from_documents(docs).as_query_engine()`) |
| **Haystack** | production search and RAG pipelines |
| **Semantic Kernel** (Microsoft) | C#/Python/Java plugins and planners for enterprise apps |
| **OpenAI Agents SDK** | agents, handoffs, guardrails, tracing |
| **CrewAI**, **AutoGen/AG2** | multi-agent role and conversation frameworks |
| **DSPy** | "programming, not prompting": optimises prompts and few-shot examples against a metric |
| **Pydantic AI**, **Instructor** | type-safe structured outputs and agents |

---

## 4. Framework or no framework?

| Use a framework when… | Write it yourself when… |
|---|---|
| prototyping quickly, many integrations | the flow is simple (one or two LLM calls) |
| you need standard agent infrastructure: checkpoints, interrupts, streaming, tracing | you need full control over prompts, latency and dependencies |
| the team benefits from shared abstractions | abstraction layers make debugging hard |
| LangGraph-style state machines fit your agent | the framework's churn is a maintenance risk |

**A balanced interview answer:** *"I'd use LangGraph (or ADK) for agents that need state, loops, human-in-the-loop and checkpointing, since those are hard to build well. For simple chains I keep things thin, often calling the model SDK directly, so prompts and behaviour stay transparent. Either way, I know what the framework does under the hood (as in this chapter's mini version), so I can debug it."*

---

## 5. Interview questions

**Q1. What is LCEL?**
> The LangChain Expression Language: every component is a Runnable with invoke/batch/stream, and `|` composes them into a pipeline; dicts of Runnables run in parallel. You get streaming, batching, retries and fallbacks for free.

**Q2. Chains vs agents vs LangGraph?**
> A chain is a fixed sequence of steps; an agent lets the model choose tools in a loop; LangGraph represents agents (or any workflow) as an explicit graph of nodes and conditional edges over shared state, adding checkpointing, interrupts and controllable loops.

**Q3. How does LangGraph support human-in-the-loop?**
> A node calls `interrupt(...)`; the graph saves its state via the checkpointer under a thread_id and returns. Later the app resumes the same thread with `Command(resume=value)`, and execution continues from that node.

**Q4. What is a checkpointer used for?**
> Persisting graph state per thread after each step: conversation memory, resuming after crashes or interrupts, replaying or "time travel" debugging, and running across stateless servers (with a Postgres or Redis saver).

**Q5. How would you build a RAG endpoint in Spring Boot?**
> Spring AI: configure a ChatModel and a VectorStore (e.g. pgvector), ingest documents through an ETL pipeline (reader → splitter → embedding → store), and build a ChatClient with a QuestionAnswerAdvisor and a grounding system prompt; return `.call().content()` or a typed `.entity()`, with observability through Micrometer.

**Q6. What are the downsides of LangChain?**
> Abstraction layers can hide prompts and make debugging harder, APIs change frequently, and it adds dependencies. Mitigate by using the parts you need, pinning versions, tracing everything (LangSmith), and dropping to the raw SDK for simple paths.

---

**Next:** [12 · LLM gateways and LLMOps](../12-LLM-Gateways-LLMOps/README.md)
