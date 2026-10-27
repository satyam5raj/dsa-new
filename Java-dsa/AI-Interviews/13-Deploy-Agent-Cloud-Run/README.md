# 13 · Deploying an AI Agent to Cloud Run

| | |
|---|---|
| **Code** | [AgentServer.java](AgentServer.java): a dependency-free, Cloud-Run-ready agent service (`$PORT`, `/healthz`, JSON `/chat`, SSE `/chat/stream`, external session store, bearer auth, request deadlines, JSON logs, graceful SIGTERM shutdown). `java AgentServer.java` runs a self-test over real HTTP (12 checks); `PORT=8080 java AgentServer.java serve` runs it for real |
| **Files** | [Dockerfile](Dockerfile) (multi-stage, non-root) · [deploy.sh](deploy.sh) (gcloud reference commands; **not** executed by the tests) |
| **Related** | [Spring-Boot](../../Spring-Boot/) (for a production Java service), [HLD](../../HLD/) |
| **Interview weight** | medium–high: "how would you deploy and scale this agent?" |

---

## 1. What Cloud Run is

**Cloud Run** is Google Cloud's **serverless container platform**: you give it a container image, and it runs it behind an HTTPS URL, **scales the number of instances automatically** with traffic (including **to zero**), and bills per use. There are no servers or Kubernetes clusters to manage.

| Concept | Meaning for an agent |
|---|---|
| **service** | your agent's HTTPS endpoint; each deploy creates a new immutable **revision** |
| **instance** | one running container; Cloud Run adds and removes them automatically |
| **concurrency** | how many requests **one instance** handles at once (default 80). The self-test serves **40 concurrent requests** on one instance |
| **min / max instances** | min > 0 avoids **cold starts**; max caps cost and protects downstream quotas (e.g. model rate limits) |
| **request timeout** | up to 60 minutes; agents with long tool chains need more than the default 5 |
| **billing** | **request-based** (CPU only while handling requests) or **instance-based** (CPU always on, needed for background work after the response) |
| **jobs** | Cloud Run *jobs* run batch tasks to completion (e.g. nightly RAG re-indexing) |

**Why it suits agents:** agents are mostly I/O-bound (waiting on model APIs and tools), so high concurrency per instance is cheap; traffic is bursty (scale to zero overnight); and it's plain HTTP or streaming. (Other options: GKE for full control or self-hosted GPU models; **Vertex AI Agent Engine** for a fully managed agent runtime; App Engine; and, on other clouds, AWS ECS/Fargate, Lambda, Azure Container Apps.)

---

## 2. The rules your container must follow (and how the code follows them)

| Rule | Why | In `AgentServer.java` |
|---|---|---|
| listen on **`$PORT`**, bind `0.0.0.0` | Cloud Run injects the port | `PORT` env, `new InetSocketAddress("0.0.0.0", port)` |
| **stateless** instances | any instance may serve any request; instances are created and destroyed at will | conversation history lives in a `SessionStore` (in-memory for the demo; **Firestore / Memorystore Redis / Cloud SQL** in production). A follow-up in session `s1` sees its history; `s2` has its own |
| start **fast** | cold starts add latency | no heavy framework; lazy-load clients |
| a **health endpoint** | startup and liveness probes | `GET /healthz` → `{"status":"ok"}` (503 while draining) |
| **handle SIGTERM** | Cloud Run sends SIGTERM, then gives about 10 s before killing an instance | a shutdown hook marks the service as draining, stops accepting connections, finishes in-flight requests, and shuts down its thread pools |
| **log to stdout as JSON** | Cloud Logging parses `severity`, fields and trace IDs | `{"severity":"INFO","message":"chat","session":"s1","latency_ms":"3",…}` |
| **no secrets in the image or logs** | images and logs are widely readable | the token comes from an env var mounted from **Secret Manager**; the test asserts it never appears in the logs |
| **time-box everything** | a stuck model call must not hang the request | `future.get(10, SECONDS)` → 504 on timeout |

---

## 3. Streaming responses

Users judge LLM apps by **time to first token**, so stream. Cloud Run supports streaming responses, **Server-Sent Events (SSE)**, WebSockets, gRPC streaming and HTTP/2.

`GET /chat/stream` sends each token as an SSE event:
```
data: Order
data: B-7
…
event: done
data: [DONE]
```
The self-test reads it over real HTTP. Make sure the request **timeout** covers the longest stream, and that any proxy in front doesn't buffer it.

---

## 4. Packaging: the Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk AS build        # stage 1: compile
COPY AgentServer.java .
RUN javac -d /out AgentServer.java

FROM eclipse-temurin:21-jre                 # stage 2: small runtime image
COPY --from=build /out /app
RUN useradd --system --uid 10001 agent
USER agent                                  # don't run as root
CMD ["java", "-XX:MaxRAMPercentage=75", "-cp", "/app", "AgentServer", "serve"]
```
- **Multi-stage** keeps the image small (no compiler shipped), which also means faster cold starts.
- `MaxRAMPercentage` makes the JVM respect the container's memory limit.
- For Spring Boot apps: use layered jars or Jib, consider GraalVM native images or CRaC for faster startup, and use buildpacks (`gcloud run deploy --source .` without a Dockerfile builds one automatically).

---

## 5. Deploying (see [deploy.sh](deploy.sh))

```bash
gcloud run deploy acme-agent \
  --source . --region us-central1 \
  --service-account agent-sa@PROJECT.iam.gserviceaccount.com \
  --no-allow-unauthenticated \
  --set-secrets "APP_API_TOKEN=app-api-token:latest" \
  --set-env-vars "MODEL_NAME=gemini-2.5-flash" \
  --concurrency 40 --min-instances 1 --max-instances 20 \
  --timeout 300 --cpu 1 --memory 1Gi --cpu-boost
```
| Flag | Why |
|---|---|
| `--source .` | Cloud Build builds the image (from the Dockerfile) and stores it in Artifact Registry |
| `--service-account` | a **dedicated identity with least privilege** (e.g. `roles/aiplatform.user` to call Vertex AI models). No API keys needed for Google models: the runtime identity authenticates |
| `--no-allow-unauthenticated` | callers need IAM permission (`roles/run.invoker`) and an identity token; put an API gateway or Identity-Aware Proxy in front for end users |
| `--set-secrets` | mount Secret Manager secrets as env vars or files |
| `--concurrency` | tune to your workload; agents waiting on I/O can handle many requests per instance |
| `--min-instances 1` | no cold start for the first user (costs a little 24/7) |
| `--max-instances` | cost cap; also protects the model provider's rate limits |
| `--timeout` | long agent runs need more than the default |
| `--cpu-boost` | extra CPU during startup → faster cold starts |

**For Google ADK agents (Python):** the ADK CLI can package and deploy an agent directly (`adk deploy cloud_run --project … --region … ./my_agent`). Under the hood it does the same thing: builds a container that serves the agent over HTTP and deploys it to Cloud Run. Alternatively, deploy to **Vertex AI Agent Engine**, which manages sessions and memory for you.

### Safer rollouts
Deploy a new revision with **no traffic** and a tag, smoke-test it at its tagged URL, then **shift traffic gradually** (10% → 50% → 100%). Rolling back is a traffic switch. Combine this with your **eval suite** (chapter 05) as a gate in CI/CD (Cloud Build or GitHub Actions).

---

## 6. A production architecture on Google Cloud

```
users ─▶ HTTPS load balancer / API Gateway (auth, WAF: Cloud Armor)
            │
            ▼
       Cloud Run: agent service (stateless, autoscaled)
        │    │     │            │                 │
        │    │     │            │                 └─▶ Model Armor (screen prompts and responses, ch 10)
        │    │     │            └─▶ LLM gateway or Vertex AI (Gemini, partner models) via the service account (ch 12)
        │    │     └─▶ tools: internal APIs, MCP servers (also on Cloud Run), Cloud SQL, BigQuery
        │    └─▶ RAG: Vertex AI Vector Search / AlloyDB / pgvector + Cloud Storage documents (ch 04)
        └─▶ session state + memory: Firestore / Memorystore (Redis)
observability: Cloud Logging, Cloud Trace (OpenTelemetry), Cloud Monitoring alerts, cost dashboards
async work: Pub/Sub + Cloud Tasks (long jobs), Cloud Run jobs (re-indexing), Eventarc triggers
secrets: Secret Manager · images: Artifact Registry · CI/CD: Cloud Build with eval gates
```

### Things interviewers probe
| Topic | Answer |
|---|---|
| **long-running agent tasks** (minutes to hours) | don't hold the HTTP request open: return a task ID, run the work asynchronously (Pub/Sub / Cloud Tasks → a worker service, or a Cloud Run job), store progress, and let the client poll or receive a webhook/SSE |
| **conversation state** | outside the instance (Firestore/Redis), keyed by session ID; LangGraph/ADK checkpointers can use these stores |
| **cold starts** | min instances, startup CPU boost, small images, lazy initialisation, keep the JVM small (or native images) |
| **scaling limits** | the model provider's quota is usually the bottleneck: set max instances, queue requests, use the gateway's rate limits |
| **cost** | request-based billing and scale to zero for spiky traffic; concurrency tuning; the model tokens usually cost far more than the compute |
| **security** | per-service service accounts, no public ingress unless needed, VPC connectors / Private Service Connect for internal tools, Secret Manager, Binary Authorization for trusted images, Model Armor for content |
| **multi-region** | deploy in several regions behind a global load balancer, for latency and resilience |
| **sandboxed code execution** | never run model-generated code in the main service; use a separate locked-down sandbox (gVisor-isolated service, no credentials, no network egress) |

---

## 7. Interview questions

**Q1. How would you deploy an LLM agent to production on Google Cloud?**
> Containerise a stateless HTTP service (listening on $PORT, health endpoint, JSON logs, graceful SIGTERM), deploy it to Cloud Run with a dedicated least-privilege service account, secrets from Secret Manager, IAM or a gateway for auth, tuned concurrency and min/max instances, streaming responses, sessions in Firestore/Redis, model calls via Vertex AI or an LLM gateway, Model Armor screening, tracing and cost monitoring, and CI/CD with eval gates and gradual traffic shifts.

**Q2. Why must a Cloud Run service be stateless, and where does conversation memory go?**
> Instances are created, scaled and stopped automatically, and consecutive requests can land on different instances. Keep state in an external store (Firestore, Memorystore, Cloud SQL) keyed by session ID.

**Q3. How do you handle an agent task that takes 20 minutes?**
> Make it asynchronous: accept the request, enqueue it (Pub/Sub or Cloud Tasks), process it in a worker (a Cloud Run service with a long timeout, or a Cloud Run job), persist progress, and notify or poll; stream progress events if the user is waiting.

**Q4. What is concurrency in Cloud Run, and how would you set it for an agent?**
> The number of simultaneous requests one instance serves. Agents mostly wait on I/O, so a higher concurrency (e.g. 20–80) is efficient; lower it if requests are CPU- or memory-heavy. Load-test to find the point where latency rises.

**Q5. How do you reduce cold-start latency?**
> Min instances > 0, startup CPU boost, small images (multi-stage), fast frameworks or native images, and lazy initialisation of heavy clients.

**Q6. How does the service authenticate to Vertex AI without an API key?**
> Through its attached service account: Google client libraries fetch short-lived credentials from the metadata server (Application Default Credentials). Grant only the needed roles, such as `roles/aiplatform.user`.

**Q7. How do you roll out a new prompt or model safely?**
> Run the offline eval suite in CI; deploy a new revision with no traffic and a tag; smoke-test it; shift a small percentage of traffic and watch the quality, latency and cost metrics; then promote or roll back by switching traffic.

---

**Next:** [14 · Fine-tuning, serving and cost](../14-Fine-Tuning-Serving-Cost/README.md)
