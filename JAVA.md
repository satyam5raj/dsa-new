Here's the full breakdown across each area.

**Core Java**
- Syntax fundamentals, data types, operators, control flow (fast for you)
- Collections Framework: List/Set/Map implementations (ArrayList vs LinkedList, HashMap internals — buckets, treeification, load factor, resize — TreeMap, LinkedHashMap, ConcurrentHashMap)
- Generics, wildcards, bounded types
- Exception handling: checked vs unchecked, custom exceptions, try-with-resources
- Java 8+ features: Streams API, lambdas, functional interfaces, Optional, method references
- String pool, immutability, StringBuilder vs StringBuffer
- Java I/O and NIO basics
- Records, sealed classes, pattern matching (Java 17+, since senior roles increasingly expect modern Java)
- Serialization basics

**OOP**
- Four pillars: encapsulation, abstraction, inheritance, polymorphism — with the ability to explain *why*, not just define
- Composition vs inheritance, and when to prefer each
- Interfaces vs abstract classes (including Java 8+ default/static methods on interfaces)
- SOLID principles — deeply, since this is a constant interview topic at senior level
- Design patterns: Singleton, Factory, Builder, Strategy, Observer, Decorator, Adapter, Template Method — know when to use each, not just implement them
- Object equality: equals/hashCode contract, immutability design

**Multithreading & Concurrency**
- Thread lifecycle, creation (Thread vs Runnable vs Callable)
- synchronized, locks (ReentrantLock, ReadWriteLock), volatile
- Java Memory Model: happens-before, visibility, reordering
- Executor framework: ExecutorService, thread pools, ThreadPoolExecutor tuning
- Concurrent collections: ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue
- CompletableFuture, Future, async patterns
- Deadlocks, race conditions, livelocks — causes and prevention
- CountDownLatch, Semaphore, CyclicBarrier
- Fork/Join framework basics

**JVM Internals**
- Memory model: heap (young/old gen), stack, metaspace, method area
- Garbage collection: GC algorithms (Serial, Parallel, G1, ZGC), tuning flags, when GC pauses matter
- Class loading: classloader hierarchy, linking, initialization
- JIT compilation basics: interpreter vs compiled code, HotSpot
- Memory leaks: common causes, how to diagnose (heap dumps, profilers)
- JVM tuning flags relevant to production (-Xmx, -Xms, GC logging)

**Spring / Spring Boot**
- IoC and Dependency Injection — bean lifecycle, scopes (singleton, prototype, request)
- Spring Boot auto-configuration, starters, application properties/profiles
- Spring MVC: controllers, request mapping, exception handling (@ControllerAdvice)
- Spring Data JPA: repositories, entity mapping, query methods, N+1 problem, lazy vs eager loading
- Transactions: @Transactional, propagation, isolation levels
- Spring Security: authentication/authorization, filters, JWT integration (you already know this conceptually from Node — map it to Spring's filter chain)
- AOP: aspects, proxies, common use cases (logging, transactions)
- Spring Boot Actuator, health checks, metrics
- Testing: JUnit 5, Mockito, @SpringBootTest, MockMvc
- Microservices with Spring Cloud (config server, service discovery) — useful given your existing microservices background

**DSA Topics & Patterns**
- Arrays & Strings: two pointers, sliding window, prefix sums
- Hashing: hash maps/sets for O(1) lookups, frequency counting
- Linked Lists: reversal, cycle detection (Floyd's), merge operations
- Stacks & Queues: monotonic stack, queue-based BFS
- Trees: traversals (in/pre/post-order), BST operations, height/balance problems, LCA
- Graphs: BFS/DFS, topological sort, Union-Find, Dijkstra, shortest path problems
- Heaps/Priority Queues: k-th largest/smallest, merge k lists
- Recursion & Backtracking: permutations, combinations, subsets, N-Queens style problems
- Dynamic Programming: 1D/2D DP, knapsack variants, longest common subsequence, DP on strings/arrays
- Greedy algorithms: interval scheduling, activity selection
- Binary Search: on arrays and on answer space
- Sorting algorithms: know merge sort/quick sort deeply (implementation + complexity), not just usage
- Bit manipulation basics
- Time/space complexity analysis — fluent, not memorized

**System Design**
*Low-Level Design (LLD)* — this leans on your existing strengths:
- Class design from requirements, applying SOLID and design patterns
- Common LLD problems: parking lot, rate limiter, elevator system, notification service (you've literally built one), library management
- Concurrency-safe design in class structures

*High-Level Design (HLD)*:
- Requirements gathering → capacity estimation → high-level architecture → deep dive → tradeoffs (structured answer format)
- Scalability: horizontal vs vertical scaling, load balancing
- Database: SQL vs NoSQL tradeoffs, sharding, replication, indexing (you have real dual-write migration experience — use it)
- Caching: cache-aside, write-through, Redis patterns, cache invalidation
- Message queues: pub-sub vs point-to-point, at-least-once vs exactly-once (your RabbitMQ background maps directly)
- CAP theorem, consistency models
- API design: rate limiting, pagination, idempotency
- Common HLD problems: URL shortener, chat system, news feed, ride-sharing, distributed cache, notification system