/*
 * ============================================================================
 *  SOLID - D : DEPENDENCY INVERSION PRINCIPLE (DIP)
 * ============================================================================
 *
 *  Run:  java D_DependencyInversion.java
 *
 *  1. High-level modules should not depend on low-level modules.
 *     BOTH should depend on ABSTRACTIONS.
 *  2. Abstractions should not depend on details. Details should depend on
 *     abstractions.
 *
 *  HIGH-LEVEL = business policy  (OrderService: "place an order")
 *  LOW-LEVEL  = mechanisms       (MySQL, Kafka, SMTP, Stripe SDK)
 *
 *  WITHOUT DIP:    OrderService ---> MySqlOrderRepository
 *                  (business logic is chained to MySQL; can't test without a DB)
 *
 *  WITH DIP:       OrderService ---> OrderRepository (interface)  <--- MySqlOrderRepository
 *                                                                <--- InMemoryOrderRepository
 *  The arrow from the low-level module now points UP to the abstraction:
 *  the dependency is INVERTED. The interface is owned by the HIGH-level
 *  module and shaped by what IT needs.
 *
 *  DIP vs DI vs IoC (often confused)
 *    DIP (principle)            : depend on abstractions, not concretions
 *    Dependency Injection (DI)  : a TECHNIQUE. Dependencies are passed in
 *                                 (constructor/setter) rather than created inside with 'new'
 *    Inversion of Control (IoC) : a broader idea. A framework (Spring) creates
 *                                 objects and wires them. The "IoC container"
 *                                 does the DI for you.
 *    Prefer CONSTRUCTOR injection: dependencies are explicit, can be final,
 *    and the object is never half-built.
 */

import java.util.*;

public class D_DependencyInversion {

    public static void main(String[] args) {
        System.out.println("===== BAD: hard-wired to concrete classes =====");
        new BadOrderService().placeOrder("ORD-1", 999);

        System.out.println("\n===== GOOD: production wiring =====");
        OrderService prod = new OrderService(new MySqlOrderRepository(), new KafkaEventPublisher());
        prod.placeOrder("ORD-2", 1499);

        System.out.println("\n===== GOOD: test wiring (no DB, no Kafka) =====");
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        RecordingPublisher events = new RecordingPublisher();
        OrderService test = new OrderService(repo, events);
        test.placeOrder("ORD-3", 50);
        System.out.println("   assert saved     : " + repo.findAll());
        System.out.println("   assert published : " + events.published);

        System.out.println("\n===== Mini IoC container (what Spring does for you) =====");
        MiniContainer container = new MiniContainer();
        container.register(OrderRepository.class, new InMemoryOrderRepository());
        container.register(EventPublisher.class, new KafkaEventPublisher());
        OrderService wired = new OrderService(container.get(OrderRepository.class), container.get(EventPublisher.class));
        wired.placeOrder("ORD-4", 10);
    }

    // =================================================================
    // BAD: OrderService creates its own low-level dependencies
    // =================================================================
    static class BadOrderService {
        private final MySqlOrderRepository repo = new MySqlOrderRepository();  // concrete + 'new' inside
        private final KafkaEventPublisher publisher = new KafkaEventPublisher();

        void placeOrder(String id, double amount) {
            repo.save(id, amount);
            publisher.publish("OrderPlaced:" + id);
            // Want Postgres? Edit this class. Want a unit test? You need MySQL + Kafka running.
        }
    }

    // =================================================================
    // GOOD: abstractions owned by the business layer
    // =================================================================
    interface OrderRepository {
        void save(String id, double amount);
        List<String> findAll();
    }

    interface EventPublisher {
        void publish(String event);
    }

    // High-level policy depends ONLY on interfaces, received via the constructor
    static class OrderService {
        private final OrderRepository repository;
        private final EventPublisher publisher;

        OrderService(OrderRepository repository, EventPublisher publisher) {   // constructor injection
            this.repository = Objects.requireNonNull(repository);
            this.publisher = Objects.requireNonNull(publisher);
        }

        void placeOrder(String id, double amount) {
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
            repository.save(id, amount);
            publisher.publish("OrderPlaced:" + id);
        }
    }

    // Low-level details implement the abstractions
    static class MySqlOrderRepository implements OrderRepository {
        public void save(String id, double amount) { System.out.println("   [MySQL] INSERT order " + id + " amount " + amount); }
        public List<String> findAll() { return List.of(); }
    }

    static class KafkaEventPublisher implements EventPublisher {
        public void publish(String event) { System.out.println("   [Kafka] topic=orders event=" + event); }
    }

    static class InMemoryOrderRepository implements OrderRepository {
        private final List<String> rows = new ArrayList<>();
        public void save(String id, double amount) { rows.add(id + "=" + amount); }
        public List<String> findAll() { return List.copyOf(rows); }
    }

    static class RecordingPublisher implements EventPublisher {
        final List<String> published = new ArrayList<>();
        public void publish(String event) { published.add(event); }
    }

    // =================================================================
    // A toy IoC container: a map from an interface type to an implementation.
    // Spring does the same, plus scanning, lifecycles, proxies, scopes...
    // =================================================================
    static class MiniContainer {
        private final Map<Class<?>, Object> beans = new HashMap<>();

        <T> void register(Class<T> type, T impl) { beans.put(type, impl); }

        <T> T get(Class<T> type) {
            Object bean = beans.get(type);
            if (bean == null) throw new IllegalStateException("No bean for " + type.getSimpleName());
            return type.cast(bean);
        }
    }
}
