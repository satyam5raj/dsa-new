# Spring / Spring Boot

Two real projects. This topic can't be single files, because Spring needs a build tool and many dependencies.

➡️ **New here? Start with [springboot.md](springboot.md)**: setup, how to run and test everything, and how to create a new Spring Boot project step by step.

| Project | What it is | Verify it |
|---|---|---|
| [spring-boot-learning/](spring-boot-learning/) | One Spring Boot app with **one package per subtopic** and a test class proving every behaviour | `./mvnw test` (56 tests) · `./try-it.sh` (starts the app and curls every topic) |
| [spring-cloud-demo/](spring-cloud-demo/) | 4 microservices: Config Server, Eureka, inventory (×2 instances), order | `./run-demo.sh` (starts all of them and shows config, discovery, load balancing and failover) |

**Stack:** Java 21 · **Spring Boot 4.0.8** (Spring Framework 7, Spring Security 7, Hibernate 7) · **Spring Cloud 2025.1.3** · H2 in-memory DB.
Nothing needs installing: `./mvnw` downloads Maven and the dependencies on first run.

> **VS Code tip:** red import errors in these folders mean the Java extension hasn't imported the Maven projects yet. Open `spring-boot-learning/` (or `spring-cloud-demo/`) as its own folder, or run "Java: Import Java Projects", and the errors disappear. The command-line builds are the source of truth: they compile and pass.

## Where each topic lives

Paths below are under `spring-boot-learning/src/main/java/com/learning/spring/`. Tests are in the matching `src/test/...` package.

| # | Topic (from JAVA.md) | Code | Proof (test) |
|---|---|---|---|
| 1 | **IoC & DI**: bean lifecycle, scopes, injection, @Primary/@Qualifier, @Bean | `ioc/` LifecycleDemoBean, LoggingBeanPostProcessor, ScopedBeans, Notifications, AppConfig | `IocContainerTest`: the exact lifecycle order, prototype-in-singleton trap and fix |
| 2 | **Auto-configuration, starters, properties, profiles** | `config/` StoreProperties, FeatureConfig · `autoconfig/` a **custom auto-configuration** + `META-INF/spring/...imports` · `application(-dev).properties` | `ConfigurationTest`: binding, profile overrides, conditions, back-off, fail-fast validation (ApplicationContextRunner) |
| 3 | **Spring MVC**: controllers, mapping, validation, @ControllerAdvice | `mvc/` ProductController, GlobalExceptionHandler (ProblemDetail) | `ProductControllerTest`: **@WebMvcTest + MockMvc + @MockitoBean** |
| 4 | **Spring Data JPA**: entities, repositories, query methods, **N+1**, lazy vs eager | `jpa/` Author/Book, AuthorRepository (JOIN FETCH, @EntityGraph, DTO projection), BookRepository | `JpaRepositoryTest`: **N+1 measured: 6 statements vs 1**, a real LazyInitializationException, pagination |
| 5 | **Transactions**: @Transactional, propagation, isolation, rollback rules | `tx/` BankService, AuditService (REQUIRES_NEW / REQUIRED / MANDATORY), @Version optimistic locking | `TransactionTest`: rollback, audit survives via REQUIRES_NEW, **checked exceptions COMMIT**, **self-invocation skips the transaction** |
| 6 | **Spring Security + JWT**: filter chain, authN/authZ | `security/` SecurityConfig (stateless JWT resource server, BCrypt, roles), AuthController (issues JWTs), @PreAuthorize | `SecurityTest`: 401 vs 403, tampered token, method security using arguments |
| 7 | **AOP**: aspects, proxies, use cases | `aop/` PerformanceAspect (@Before/@Around/@AfterReturning/@AfterThrowing), @LogExecutionTime | `AopTest`: CGLIB proxy, **real advice order**, self-invocation not intercepted |
| 8 | **Actuator**: health, metrics | `actuator/` custom HealthIndicator, Micrometer Counter/Timer with tags | `ActuatorTest`: DOWN component → HTTP 503, metric values |
| 9 | **Testing**: JUnit 5, Mockito, @SpringBootTest, MockMvc | `testing/` CheckoutService (designed for testability) | `CheckoutServiceTest`: pure Mockito, @ParameterizedTest, ArgumentCaptor, @Nested. Every other test shows a different Spring test style |
| 10 | **Microservices with Spring Cloud**: config server, service discovery | `spring-cloud-demo/` | `run-demo.sh` |

## Test styles at a glance

| Annotation | Starts | Speed | Use for |
|---|---|---|---|
| none (+ `@ExtendWith(MockitoExtension.class)`) | nothing: plain objects + mocks | ms | business logic (most tests) |
| `@WebMvcTest(X.class)` | MVC layer only (controllers, advice, security, JSON) | fast | HTTP contract: status, JSON, validation |
| `@DataJpaTest` | JPA layer only + embedded DB | fast | queries, mappings, N+1 checks |
| `ApplicationContextRunner` | a tiny custom context | very fast | configuration / auto-configuration |
| `@SpringBootTest` (+ `@AutoConfigureMockMvc`) | the whole app | slowest (context is cached between test classes) | wiring, security end-to-end, transactions |

## Things that bit me while building this (good interview stories)

1. **Spring Data ignores repository interfaces nested inside other classes** (`considerNestedRepositories=false`), so they must be top-level.
2. **AOP advice order:** `@AfterReturning` runs *before* `@Around`'s "after" code, because `@Around` wraps everything.
3. **Shared singleton state between tests:** a test that hard-coded "stock = 8" broke because another test had changed the same singleton bean.
4. **Eureka + `@LoadBalanced` (Spring Cloud 2025):** Eureka's own client uses the `RestClient.Builder` bean. If the *only* builder is load-balanced, discovery silently breaks. The fix is a `@Primary` plain builder plus a separate `@LoadBalanced` one. The full explanation is in `OrderServiceApplication`.
5. **Spring Security 7 adds `FACTOR_*` authorities** (e.g. `FACTOR_BEARER`), which record how the user authenticated. They are the basis for multi-factor rules.

## Boot 3 → Boot 4: what you'll notice

- **Modular starters:** `spring-boot-starter-webmvc` (not just `-web`), `spring-boot-starter-aspectj` (was `-aop`), and a separate `-test` starter per module.
- **Test annotations moved packages:** e.g. `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`.
- **`@MockitoBean`** (Spring Framework) replaces the removed `@MockBean`.
- **Health API** moved to `org.springframework.boot.health.contributor`.
- **Jackson 3** (`tools.jackson` packages), **Hibernate 7**, **Jakarta EE 11**.

## Interview questions to test yourself

1. What are IoC and DI? Why is constructor injection preferred? *(ioc/ScopeController)*
2. Walk through the full bean lifecycle. Where are AOP proxies created? *(LifecycleDemoBean, IocContainerTest)*
3. Singleton vs prototype vs request scope. What happens when you inject a prototype into a singleton? *(ScopedBeans)*
4. Two beans implement the same interface. How does Spring choose? *(Notifications)*
5. How does auto-configuration work? How would you write your own starter? *(autoconfig/)*
6. `@Value` vs `@ConfigurationProperties`? What is the order of property sources? *(StoreProperties, application.properties)*
7. Describe the request flow through DispatcherServlet. How do you handle exceptions globally? *(ProductController, GlobalExceptionHandler)*
8. What is the N+1 problem? Give 4 fixes. Lazy vs eager defaults? *(AuthorRepository, JpaRepositoryTest)*
9. What causes LazyInitializationException? Why disable open-in-view? *(JpaRepositoryTest, application.properties)*
10. How does @Transactional work? Name 3 reasons it silently doesn't apply. *(BankService, TransactionTest)*
11. Explain REQUIRED vs REQUIRES_NEW with a real use case. Which exceptions trigger rollback? *(AuditService, BankService)*
12. Explain the isolation levels and the anomalies each prevents. Optimistic vs pessimistic locking? *(BankService, BankEntities)*
13. Walk through the Spring Security filter chain for a JWT request. 401 vs 403? *(SecurityConfig, SecurityTest)*
14. Why is CSRF disabled for a JWT API, and when must you keep it on? *(SecurityConfig)*
15. URL security vs @PreAuthorize? *(SecuredEndpoints)*
16. How does Spring AOP work? JDK proxy vs CGLIB? What are its limitations? *(PerformanceAspect, AopTest)*
17. Liveness vs readiness probes? How do you add a custom metric? *(actuator/)*
18. @SpringBootTest vs @WebMvcTest vs @DataJpaTest: when do you use each? *(tests)*
19. How do services find each other in Spring Cloud? Client-side vs server-side load balancing? *(spring-cloud-demo)*
20. Why use a Config Server? How do you refresh config without restarting? *(ConfigServerApplication, InventoryServiceApplication)*

Next topic: **DSA Topics & Patterns**.
