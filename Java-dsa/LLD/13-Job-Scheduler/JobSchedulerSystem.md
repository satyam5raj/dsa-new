# 13 · Job Scheduler, like cron or Quartz (explained simply)

| | |
|---|---|
| **Code** | [JobSchedulerSystem.java](JobSchedulerSystem.java) |
| **Run** | `java JobSchedulerSystem.java` (17 self-checks, including 8 workers racing over 2,000 runs) |
| **Design notes** | [README.md](README.md) (the database version, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This question tests: different kinds of **schedules**, **retries** with backoff, a **dead-letter queue**, **leases** for crashed workers, and the most important honest answer in distributed systems: you can guarantee **at-least-once**, not exactly-once, so **handlers must be idempotent**.

---

## 1. The problem in plain words

Applications need work done **later** or **regularly**:
- "send this reminder at 9 am tomorrow" (**once**);
- "build the report every hour" (**fixed rate**);
- "clean up, then wait 5 minutes, then clean up again" (**fixed delay**);
- "run every day at 09:00" (**daily**, a tiny cron).

Several **worker** machines take due jobs and run them. A job can **fail** (retry it later, with growing delays), keep failing (give up and park it in a **dead-letter queue** for a human), run **too long** (timeout), or the worker can **crash** in the middle (someone else must pick it up).

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Schedule types? | Once, fixed rate, fixed delay, daily at HH:MM, and more later |
| Retries? | Yes, configurable: none, fixed, exponential with jitter |
| Max attempts? | Per job; after that → dead-letter queue |
| Several workers? | Yes, so each run must be claimed by exactly one |
| Worker crashes? | Detected by a **lease** that expires |
| Exactly-once? | Not possible for side effects: **at-least-once** + idempotent handlers |
| Priorities? | Yes, among jobs due at the same time |
| Persistence? | In memory here; a database table in production |

### Step 2 · Requirements
1. Submit a job definition (handler, schedule, retry policy, max attempts, timeout, priority).
2. Workers poll for due runs and **claim** each one atomically.
3. Success → schedule the next occurrence (for recurring jobs).
4. Failure → retry with backoff, or move to the DLQ after the last attempt.
5. A crashed worker's run is retried after its lease expires.
6. Cancel stops future runs and pending retries.
7. Missed slots ("misfires") don't cause a burst of catch-up runs.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| a job (the definition) | `class JobDefinition` (built with a Builder) |
| what the job does | `interface JobHandler` |
| when it runs | `interface Schedule` + `Once`, `FixedRate`, `FixedDelay`, `DailyAt` |
| retry delays | `interface RetryPolicy` + `FixedBackoff`, `ExponentialBackoff` |
| one execution attempt | `class JobRun` (status, lease) |
| run status | `enum RunStatus { SCHEDULED, RUNNING, SUCCEEDED, FAILED, DEAD, CANCELLED }` |
| the queue of due runs | `class RunStore` (a priority queue by time and priority) |
| the scheduler | `class Scheduler` |

**The key modelling idea:** a **job** (the definition, "send invoices daily") is different from a **run** (one attempt at one occurrence, "the 9 am run on 3 Oct, attempt 2"). It's the Book vs BookCopy idea again: one definition, many runs.

### Step 4 · Relationships

```
Scheduler
   ├── JobDefinition(s) ── Schedule, RetryPolicy, JobHandler
   ├── RunStore ── due queue (ordered by time, then priority) + all runs
   ├── deadLetters
   └── SchedulerListener(s)
JobRun: runId (this attempt) + logicalId (this occurrence; the same across retries)
```

### Step 5 · API and flow

```java
void submit(JobDefinition d)
void cancel(String jobId)
int pollAndRun(String worker, int batch)   // called by each worker in a loop
int recoverExpiredLeases()                 // a periodic sweep
```

```
submit → first run SCHEDULED
worker polls → takes due runs → CLAIMS one (CAS SCHEDULED→RUNNING, with a lease) → executes
   success → SUCCEEDED → schedule the next occurrence
   failure → FAILED → attempts left? new run after a backoff (same logicalId) : DEAD → DLQ
worker crashed → the lease expires → the sweep marks it FAILED → retried like any failure
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| when the next run happens | **Strategy** (`Schedule`) |
| how long to wait before a retry | **Strategy** (`RetryPolicy`) |
| a job with many optional settings | **Builder** (`JobDefinition.builder`) |
| runs as objects in a queue | **Command** (`JobRun`) |
| reacting to success / retry / dead | **Observer** (`SchedulerListener`) |

### Step 7 · Concurrency
The contended resource is **one run**: many workers poll at once, and exactly one may execute it. The protection is a CAS claim plus a lease.

---

## 3. Five ideas explained simply

### 3.1 Fixed rate vs fixed delay

```
Fixed rate (every 1000 ms from the SCHEDULED time):  0 ─── 1000 ─── 2000 ─── 3000    steady, like a metronome
Fixed delay (1000 ms after the run FINISHES):        0 [300] ──1000── 1300 [300] ──1000── 2600   drifts, never overlaps
```

Fixed rate is right for "every hour on the hour". Fixed delay is right for "don't start the next cleanup until the last one is done".

### 3.2 Misfires: don't stampede after downtime
If the scheduler was down for 3.5 periods, a naive fixed rate would immediately run 3 missed runs back to back. This one **skips the missed slots** and runs at the next future slot.

### 3.3 Exponential backoff with jitter
Wait 1 s, then 2 s, then 4 s, then 8 s… up to a cap. **Jitter** picks a random time in [0, that delay], so if 10,000 jobs failed together because the database blinked, they don't all retry at the same millisecond (a "thundering herd").

### 3.4 Leases: detecting dead workers
When a worker claims a run, it gets a **lease**: "you own this until 12:00:30". If the worker crashes, the lease simply runs out, a sweep notices, and the run is retried. Like a library book with a due date: if it's not returned, the library assumes it's lost.

### 3.5 At-least-once, and why handlers must be idempotent
A worker can **finish the work and crash before recording success**. The lease expires and another worker runs it **again**. So the system can only promise **at-least-once**. To make the effect happen "effectively once", the handler uses an **idempotency key**: the run's `logicalId` (job + scheduled time), which stays the same across retries. "Has invoice@2026-10-03T09:00 already been emailed? Then skip."

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| records implementing an interface | `Once`, `FixedRate`, `FixedDelay`, `DailyAt`, `FixedBackoff` | small immutable strategies |
| `OptionalLong` | `Schedule.next` | "maybe a next time" (empty for a one-time job) |
| `ZonedDateTime`, `Instant`, `ZoneOffset.UTC` | `DailyAt` | date-time arithmetic: "today at 09:00, or tomorrow if that's passed" |
| `base << n` | exponential backoff | multiply by 2ⁿ with a bit shift |
| `@FunctionalInterface` | `JobHandler` | a lambda can be a job |
| a static nested `Builder` with a private constructor | `JobDefinition` | the only way to create a definition is through the validated builder |
| `PriorityBlockingQueue` with a comparator | due runs | a thread-safe priority queue ordered by time, then priority, then id |
| `synchronized (due)` | `takeDue` | peek + poll as one step across pollers |
| `AtomicReference<RunStatus>` + CAS | claim, finish, recover | one winner for every status change |
| `volatile` fields | `worker`, `leaseUntil`, `error` | visible to other threads (the sweep) |

---

## 5. The code, piece by piece

### 5.1 Schedules

```java
interface Schedule {
    long first(long now);
    OptionalLong next(long prevScheduled, long finishedAt);      // empty = no more runs
}

record Once(long at) implements Schedule {
    public long first(long now) { return Math.max(now, at); }
    public OptionalLong next(long p, long f) { return OptionalLong.empty(); }
}

record FixedRate(long period) implements Schedule {
    public long first(long now) { return now; }
    public OptionalLong next(long prev, long finished) {
        long n = prev + period;
        while (n <= finished) n += period;          // MISFIRE: we were late -> skip missed slots, don't stampede
        return OptionalLong.of(n);
    }
}

record FixedDelay(long delay) implements Schedule {
    public long first(long now) { return now; }
    public OptionalLong next(long prev, long finished) { return OptionalLong.of(finished + delay); }
}

record DailyAt(int hour, int minute) implements Schedule {
    long nextAfter(long t) {
        ZonedDateTime z = Instant.ofEpochMilli(t).atZone(ZoneOffset.UTC);
        ZonedDateTime cand = z.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        if (!cand.isAfter(z)) cand = cand.plusDays(1);
        return cand.toInstant().toEpochMilli();
    }
}
```

### 5.2 Retry policies

```java
static class ExponentialBackoff implements RetryPolicy {
    public long backoffMs(int attempt) {
        long cap = Math.min(max, base << Math.min(attempt - 1, 30));
        return rnd == null ? cap : 1 + (long) (rnd.nextDouble() * cap);
    }
}
```

`base << (attempt − 1)` doubles each time (1 s, 2 s, 4 s, …), capped at `max`. With a `Random`, it returns a random value up to that ("full jitter"); without one (as in the test), it returns the exact cap, so the test is deterministic.

### 5.3 The builder

```java
JobDefinition.builder("flaky", ctx -> { ... })
        .schedule(new Once(clock.now))
        .retry(new ExponentialBackoff(1000, 60_000, null))
        .maxAttempts(5)
        .build();
```

The id and handler are required (constructor arguments); everything else has a default (run once now, 1 s fixed backoff, 3 attempts, priority 5, 60 s timeout). `build()` validates (`maxAttempts >= 1`, `timeout > 0`).

### 5.4 A run and its claim

```java
static final class JobRun {
    final String runId, jobId, logicalId;
    final long scheduledAt;
    final int attempt, priority;
    final AtomicReference<RunStatus> status = new AtomicReference<>(RunStatus.SCHEDULED);
    volatile String worker;
    volatile long leaseUntil;

    boolean claim(String worker, long leaseUntil) {
        if (!status.compareAndSet(RunStatus.SCHEDULED, RunStatus.RUNNING)) return false;
        this.worker = worker; this.leaseUntil = leaseUntil;
        return true;
    }
}
```

`runId` is unique per attempt (R1, R2, …); `logicalId` is `job@scheduledTime` and is **shared by all retries of one occurrence**: the idempotency key.

### 5.5 Taking due runs

```java
List<JobRun> takeDue(long now, int max) {
    List<JobRun> out = new ArrayList<>();
    synchronized (due) {                          // peek+poll must be atomic across pollers
        while (out.size() < max) {
            JobRun head = due.peek();
            if (head == null || head.scheduledAt > now) break;
            due.poll();
            if (head.status.get() == RunStatus.SCHEDULED) out.add(head);
        }
    }
    return out;
}
```

The queue is ordered by **time, then priority** (lower number = more urgent). Cancelled runs are dropped as they come off the queue.

### 5.6 Executing a run

```java
private void execute(JobRun r) {
    JobDefinition d = jobs.get(r.jobId);
    long start = clock.getAsLong();
    try {
        d.handler.execute(new RunContext(r.jobId, r.logicalId, r.attempt));
        if (clock.getAsLong() - start > d.timeoutMs) throw new TimeoutException("took " + (clock.getAsLong() - start) + "ms > " + d.timeoutMs);
        if (!r.status.compareAndSet(RunStatus.RUNNING, RunStatus.SUCCEEDED)) return;   // lease was lost meanwhile
        listeners.forEach(l -> l.onSuccess(r));
        scheduleNextOccurrence(d, r);
    } catch (Exception e) {
        r.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        if (!r.status.compareAndSet(RunStatus.RUNNING, RunStatus.FAILED)) return;
        handleFailure(d, r);
    }
}
```

- Success → SUCCEEDED → schedule the next occurrence.
- Exception (or took too long) → FAILED → `handleFailure`.
- The CAS from RUNNING matters: if the lease expired and the sweep already failed this run, a slow worker finishing late doesn't overwrite that.
- **Simplification:** the timeout is checked after the handler returns. A real scheduler would run the handler on another thread with `future.get(timeout)` and interrupt it when time runs out.

### 5.7 Failure: retry or dead-letter

```java
private void handleFailure(JobDefinition d, JobRun r) {
    if (cancelledJobs.contains(d.id)) return;
    if (r.attempt < d.maxAttempts) {
        JobRun retry = newRun(d, clock.getAsLong() + d.retry.backoffMs(r.attempt), r.attempt + 1, r.logicalId);   // SAME logical id
        store.add(retry);
        listeners.forEach(l -> l.onRetry(r, retry));
    } else {
        r.status.set(RunStatus.DEAD);
        deadLetters.add(r);                           // park it for a human / a replay tool
        listeners.forEach(l -> l.onDead(r));
        scheduleNextOccurrence(d, r);                 // a recurring job keeps its cadence
    }
}
```

A **dead-letter queue (DLQ)** is a parking lot for runs that keep failing, so they don't retry forever and a person (or a replay tool) can look at them.

### 5.8 Recovering crashed workers

```java
int recoverExpiredLeases() {
    long now = clock.getAsLong();
    int n = 0;
    for (JobRun r : store.all.values())
        if (r.status.get() == RunStatus.RUNNING && r.leaseUntil <= now && r.status.compareAndSet(RunStatus.RUNNING, RunStatus.FAILED)) {
            r.error = "lease expired (worker " + r.worker + " presumed dead)";
            handleFailure(jobs.get(r.jobId), r);
            n++;
        }
    return n;
}
```

### 5.9 Cancel

```java
void cancel(String jobId) {
    cancelledJobs.add(jobId);
    store.all.values().stream().filter(r -> r.jobId.equals(jobId)).forEach(r -> r.status.compareAndSet(RunStatus.SCHEDULED, RunStatus.CANCELLED));
}
```

Future occurrences and pending retries stop. A run that's already RUNNING finishes (threads aren't killed).

---

## 6. Dry run (from the real output)

### Schedules
- `report` at a fixed rate of 1000 ms: its runs are exactly 1000 ms apart.
- `cleanup` with a fixed delay of 1000 ms, each run taking 300 ms: the runs are ≥ 1300 ms apart (it drifts).
- After cancelling both, the report count stays at 6: no more runs.
- Daily at 09:00, asked at 23:30 on 3 Oct → **04 Oct 09:00**.
- Misfire: fixed rate of 1000, previous slot 0, finished at 3500 → next slot **4000** (no burst of 3 catch-up runs).

### Retries and the DLQ
`flaky` fails twice ("DB timeout"), then succeeds; exponential backoff starting at 1 s:

| Run | Result | Next |
|---|---|---|
| attempt 1 | FAILED | retry #2 in 1000 ms |
| attempt 2 | FAILED | retry #3 in 2000 ms |
| attempt 3 | SUCCEEDED | |

All three runs share **one** logical id. `broken` always throws, with max 2 attempts → it ends up in the **dead-letter queue**. Jittered backoffs always stay within [1, min(cap, base × 2ⁿ)].

### Timeout
`slow` takes 5 s with a 2 s timeout and 1 attempt → DEAD, with a `TimeoutException` recorded.

### Worker crash
1. Worker `w-crashy` claims the `invoice` run and does the work (the side effect, keyed by logical id)…
2. …then "crashes" before recording success.
3. 31 s later the sweep finds the expired lease → FAILED → a retry is scheduled.
4. Worker `w2` runs the retry.

Result: **the handler ran twice (at-least-once), but the side effect happened once (idempotent).**

### Concurrency
8 workers poll 2,000 due runs at once → 2,000 runs executed, **each job exactly once**.

---

## 7. Concurrency, explained simply

**The danger:** 8 workers poll at the same moment and two of them take the same run.

**The protection:**
1. Taking runs off the due queue is `synchronized`, so a run is handed to one poller.
2. The `claim` is a CAS from SCHEDULED to RUNNING: even if a run somehow reached two workers, only one wins.
3. Finishing is a CAS from RUNNING: a worker whose lease already expired can't overwrite the sweep's decision.

**In a database**, the same idea is `SELECT … FROM runs WHERE status='SCHEDULED' AND scheduled_at <= now() ORDER BY scheduled_at, priority LIMIT 16 FOR UPDATE SKIP LOCKED`: each worker locks a different batch of rows, and nobody waits for anyone else's.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** one-time jobs, jobs that keep failing, timeouts, cancelled jobs with pending retries, missed slots after downtime, a worker crashing after doing the work, duplicate job ids.

**Common mistakes:**
1. Promising exactly-once.
2. No lease (a crashed worker's job is stuck RUNNING forever).
3. Retrying immediately and forever (no backoff, no limit, no DLQ).
4. Fixed backoff without jitter for many jobs (a thundering herd).
5. Catching up every missed slot after downtime (a stampede).
6. A new idempotency key for each retry (then the handler can't detect duplicates).
7. Using `Thread.sleep` in tests instead of an injected clock.

---

## 9. Interview questions and answers

**Q1. Can you guarantee a job runs exactly once?**
> Not for side effects. A worker can do the work and crash before recording success, so the system must retry: at-least-once. You get "effectively once" by making handlers idempotent with a stable key (here the logical id).

**Q2. How do you detect a crashed worker?**
> Each claim comes with a lease (an expiry time). A periodic sweep marks RUNNING runs with expired leases as failed and retries them. A healthy long-running worker would renew its lease (a heartbeat).

**Q3. Fixed rate vs fixed delay?**
> Fixed rate schedules from the previous scheduled time (steady cadence); fixed delay schedules from when the previous run finished (no overlap, but it drifts).

**Q4. What is a misfire and how do you handle it?**
> A slot missed because the scheduler or workers were down. Skip to the next future slot (as here), or run once to catch up, but never fire every missed slot at once.

**Q5. Why exponential backoff with jitter?**
> Failures are often caused by an overloaded dependency. Growing delays give it time to recover, and jitter spreads retries out so they don't all hit at the same moment.

**Q6. How do several workers avoid taking the same job?**
> An atomic claim: a CAS here, or `SELECT … FOR UPDATE SKIP LOCKED` / a conditional `UPDATE … WHERE status='SCHEDULED'` in a database.

**Q7. What's the dead-letter queue for?**
> To stop retrying a run that keeps failing, while keeping it (and its error) for a human to inspect and replay once the cause is fixed.

### Follow-up questions

**F1. "Support full cron expressions (`0 9 * * MON-FRI`)."**
> A `CronSchedule` implementing `Schedule.next` by parsing the expression and finding the next matching time. Nothing else changes.

**F2. "Make the scheduler itself highly available."**
> Store runs in a database; any number of stateless scheduler/worker nodes poll it with `SKIP LOCKED`. Or elect a single leader (ZooKeeper, etcd) to enqueue due runs into a message queue that workers consume.

**F3. "Long jobs that run for hours."**
> Workers renew their lease periodically (a heartbeat); jobs checkpoint progress so a retry can resume.

**F4. "Jobs that depend on other jobs (a DAG)."**
> Model dependencies; a run becomes due only when all its parents succeeded (like Airflow).

**F5. "Limit concurrency per job type (at most 5 report jobs at once)."**
> A semaphore or counter per type, checked when claiming.

---

## 10. Running the file

```bash
java JobSchedulerSystem.java      # 17/17 tests passed (including a worker crash and 8 workers over 2,000 runs)
```

**One-line summary:** *"Separate job definitions (built with a Builder) from runs; schedules and retry policies are Strategies; workers claim due runs with a CAS and a lease; failures retry with jittered exponential backoff under the same logical id, then go to a dead-letter queue; expired leases are swept and retried, which gives at-least-once, so handlers use the logical id to stay idempotent."*
