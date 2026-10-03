# Spring Boot: Setup & Procedure

A step-by-step guide to setting up, running, testing and extending the Spring Boot projects in this folder. It also covers how to create a new Spring Boot project from scratch, the same way these were made.

---

## 1. Prerequisites

| Tool | Needed? | Check | Install (macOS) |
|---|---|---|---|
| **JDK 21+** | Yes | `java -version` | `brew install openjdk@21` |
| **Maven** | No: each project ships the **Maven Wrapper** (`./mvnw`), which downloads Maven on first use | `./mvnw -v` | - |
| **curl** | For the demo scripts | `curl --version` | preinstalled |
| **Internet** | First build only (downloads Maven + dependencies into `~/.m2`) | - | - |

> `./mvnw` uses `JAVA_HOME` if it is set, otherwise the `java` on your `PATH`. If it picks the wrong JDK:
> ```bash
> export JAVA_HOME=$(/usr/libexec/java_home -v 21)
> ```

### Editor setup (VS Code)
1. Install **Extension Pack for Java** and **Spring Boot Extension Pack**.
2. Open `spring-boot-learning/` (or `spring-cloud-demo/`) **as its own folder**, or run *"Java: Import Java Projects"* from the command palette.
3. Wait for the import to finish (status bar). The red "cannot be resolved" import errors disappear once the `pom.xml` has been imported.

---

## 2. What's in this folder

```
Spring-Boot/
├── springboot.md                 ← this file
├── README.md                     ← topic map + interview questions
├── spring-boot-learning/         ← ONE app, one package per topic
│   ├── mvnw, mvnw.cmd, .mvn/     ← Maven Wrapper (no Maven install needed)
│   ├── pom.xml                   ← dependencies (starters)
│   ├── try-it.sh                 ← starts the app and curls every topic
│   └── src/
│       ├── main/java/com/learning/spring/
│       │   ├── SpringBootLearningApplication.java   ← entry point (main)
│       │   ├── ioc/ config/ autoconfig/ mvc/ jpa/ tx/ security/ aop/ actuator/ testing/
│       ├── main/resources/
│       │   ├── application.properties       ← default config
│       │   ├── application-dev.properties   ← "dev" profile overrides
│       │   └── META-INF/spring/...AutoConfiguration.imports   ← registers our custom auto-config
│       └── test/                             ← one test class per topic (the proof)
└── spring-cloud-demo/            ← 4 microservices, one Maven multi-module build
    ├── pom.xml                   ← aggregator (lists the modules)
    ├── run-demo.sh               ← starts everything and runs the demo
    ├── config-server/  discovery-server/  inventory-service/  order-service/
```

**Versions:** Spring Boot **4.0.8** · Spring Cloud **2025.1.3** · Java **21** · H2 in-memory database (no DB install needed).

---

## 3. Running `spring-boot-learning`

All commands are run from `Java-dsa/Spring-Boot/spring-boot-learning/`.

### 3.1 First build
```bash
./mvnw clean package          # compiles, runs all 56 tests, builds target/spring-boot-learning-0.0.1-SNAPSHOT.jar
```
The first run takes a few minutes (downloads). Later runs take seconds.

### 3.2 Start the app
```bash
./mvnw spring-boot:run                                       # default profile, port 8080
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev        # dev profile: SQL logging, different store name
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9090   # override any property
```
Or run the packaged jar, which is how you'd run it on a server:
```bash
java -jar target/spring-boot-learning-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```
The app is ready when the log shows `Started SpringBootLearningApplication in ... seconds`. Stop it with `Ctrl+C`.

### 3.3 The automatic tour
```bash
./try-it.sh      # builds, starts the app, calls one endpoint per topic, stops the app
```

### 3.4 Useful URLs while the app is running

| URL | What | Login |
|---|---|---|
| http://localhost:8080/api/products | MVC demo | public |
| http://localhost:8080/api/config | properties, profiles, conditions | public |
| http://localhost:8080/api/library/book-counts?strategy=n-plus-one | JPA / N+1 (watch the SQL with the `dev` profile) | public |
| http://localhost:8080/api/bank | transactions demo state | public |
| http://localhost:8080/actuator/health | health checks | public |
| http://localhost:8080/actuator/metrics | metrics | admin |
| http://localhost:8080/h2-console | database browser: JDBC URL `jdbc:h2:mem:learning`, user `sa`, empty password | public |

### 3.5 Calling secured endpoints

Demo users: `user` / `password` (role USER) and `admin` / `admin123` (roles USER, ADMIN).

```bash
# 1. Log in and get a JWT
TOKEN=$(curl -s -X POST localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 2. Send it as a Bearer token
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/me
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/admin/stats

# Basic auth also works (handy for quick checks)
curl -u admin:admin123 localhost:8080/actuator/metrics/orders.placed
```

---

## 4. Running the tests

```bash
./mvnw test                                                  # all 56 tests
./mvnw test -Dtest=JpaRepositoryTest                         # one class
./mvnw test -Dtest='JpaRepositoryTest#nPlusOne_vs_fixes'     # one method
./mvnw test -Dtest='*Security*'                              # a pattern
```
- Results are in `target/surefire-reports/` (one `.txt` file per test class).
- Integration tests run with the `test` profile (`@ActiveProfiles("test")`). It turns off the sample-data seeders and keeps the logs short. Settings: `src/test/resources/application-test.properties`.
- Some tests print what they demonstrate. Look for lines like `SQL statements -> N+1: 6 | JOIN FETCH: 1 ...` or `lifecycle: ...` in the output.

---

## 5. Running `spring-cloud-demo`

From `Java-dsa/Spring-Boot/spring-cloud-demo/`:

```bash
./run-demo.sh                    # build, start all services, run the demo, stop everything
KEEP_RUNNING=1 ./run-demo.sh     # leave everything running to explore (Ctrl+C stops all)
```

**Start order matters**, because each service needs the ones before it:

| # | Service | Port | Why this order |
|---|---|---|---|
| 1 | config-server | 8888 | other services fetch their configuration from it at startup |
| 2 | discovery-server (Eureka) | 8761 | services register here. Dashboard: http://localhost:8761 |
| 3 | inventory-service ×2 | 8081, 8082 | register in Eureka |
| 4 | order-service | 8090 | discovers inventory instances through Eureka and load-balances between them |

Starting them by hand, each in its own terminal:
```bash
./mvnw -q package -DskipTests
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar
java -jar discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar
java -jar inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar --PORT=8081
java -jar inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar --PORT=8082
java -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar

curl localhost:8090/api/orders/instances        # what Eureka knows (takes ~5-10 s to fill)
curl localhost:8090/api/orders/check/SKU-1      # repeat: servedByPort alternates 8081 / 8082
curl localhost:8888/inventory-service/default   # raw config served by the Config Server
```

---

## 6. Creating a NEW Spring Boot project (the procedure used here)

### Option A: website
1. Open **https://start.spring.io**
2. Choose: **Maven**, **Java**, Spring Boot **4.0.x**, Java **21**, Packaging **Jar**
3. Fill in Group (`com.learning`), Artifact (`my-app`), Package name (`com.learning.myapp`)
4. Click **ADD DEPENDENCIES**, for example: *Spring Web*, *Spring Data JPA*, *H2 Database*, *Validation*, *Spring Security*, *Spring Boot Actuator*
5. Click **GENERATE**, unzip, then run `./mvnw spring-boot:run`

### Option B: command line (exactly how `spring-boot-learning` was created)
```bash
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java \
  -d bootVersion=4.0.8.RELEASE -d javaVersion=21 \
  -d groupId=com.learning -d artifactId=my-app -d packageName=com.learning.myapp \
  -d dependencies=web,data-jpa,h2,validation,security,actuator \
  -o my-app.zip
unzip my-app.zip -d my-app && cd my-app
```
> ⚠️ **Gotcha seen while setting up:** start.spring.io wrote the parent version as `4.0.8.RELEASE` in `pom.xml`, but Maven Central publishes it as `4.0.8`. If the build says it can't find `spring-boot-starter-parent`, change the version to `4.0.8`.

List the available dependency ids and Boot versions:
```bash
curl -s https://start.spring.io/metadata/client -H 'Accept: application/json' | python3 -m json.tool | less
```

### Then: first steps in a new project
1. **Main class:** keep `@SpringBootApplication` in the **root package**, with every other class in sub-packages, so component scanning finds them.
2. **Configuration:** put settings in `src/main/resources/application.properties` and create `application-dev.properties` for the dev profile.
3. **Layers:** `controller → service → repository`, with DTOs (records) at the API boundary.
4. **Tests:** start with a `@WebMvcTest` for each controller and plain Mockito tests for services.
5. **Run:** `./mvnw spring-boot:run`, then `./mvnw test` before every commit.

---

## 7. Adding a dependency later

1. Find the starter's artifact name (start.spring.io → *Explore* shows the generated `pom.xml`).
2. Add it to `<dependencies>` in `pom.xml` **without a `<version>`**. The Spring Boot parent manages versions, so all libraries stay compatible:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-aspectj</artifactId>
   </dependency>
   ```
3. Re-run `./mvnw compile` (VS Code reloads the project automatically, or run *"Java: Reload Projects"*).

**Boot 4 starter names you'll need** (they changed from Boot 3):

| Feature | Boot 4 artifact | Boot 3 name |
|---|---|---|
| REST / MVC | `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| AOP | `spring-boot-starter-aspectj` | `spring-boot-starter-aop` |
| RestClient bean | `spring-boot-starter-restclient` | (part of web) |
| Test support | one `-test` starter per module, e.g. `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` |

**Spring Cloud:** import the matching BOM. The Cloud version must match the Boot version (Boot 4.0.x ↔ Cloud 2025.1.x). See the `<dependencyManagement>` section of any `spring-cloud-demo` module.

---

## 8. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Port 8080 was already in use` | another app (or an old run) holds the port | `lsof -ti tcp:8080 \| xargs kill`, or run with `--server.port=9090` |
| Red import errors in VS Code, but `./mvnw` works | the editor hasn't imported the Maven project | open the project folder itself, or run *"Java: Import Java Projects"* |
| `release version 21 not supported` | Maven is using an older JDK | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| `Could not find artifact ...:4.0.8.RELEASE` | start.spring.io version quirk | use `4.0.8` in `pom.xml` |
| App fails at startup with `Could not bind properties to 'StoreProperties'` | an invalid value in `app.store.*` (validation fails fast) | fix the property: the message says which field |
| `NoUniqueBeanDefinitionException` | two beans of the same type | add `@Primary` to one, or `@Qualifier` at the injection point |
| `LazyInitializationException` | a lazy association was accessed after the transaction closed | fetch it inside a `@Transactional` service method (JOIN FETCH / `@EntityGraph`) |
| 401 on an endpoint | no or invalid token | get a token from `/auth/token` and send `Authorization: Bearer ...` |
| 403 on an endpoint | logged in, but the wrong role | use the `admin` user |
| Eureka shows instances but order-service finds none | the load-balanced `RestClient.Builder` is also being used by Eureka | see the comment in `OrderServiceApplication`: use a `@Primary` plain builder plus a `@LoadBalanced` one |
| See *why* an auto-configuration did or didn't apply | - | run with `--debug` (conditions report) or GET `/actuator/conditions` as admin |

---

## 9. Daily workflow cheat sheet

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # develop with SQL logging
./mvnw test -Dtest=SomeTest                            # fast feedback on what you changed
./mvnw clean package                                    # full build + all tests before committing
java -jar target/*.jar                                  # run like production
./mvnw dependency:tree                                  # see where a library comes from
./mvnw clean                                            # delete target/ (build output, ~60 MB)
```
