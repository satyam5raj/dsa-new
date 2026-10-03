/*
 * ============================================================================
 *  PATTERN 5 : OBSERVER  (Behavioral)   aka Publish-Subscribe, Listener
 * ============================================================================
 *
 *  Run:  java P5_Observer.java
 *
 *  INTENT : define a ONE-TO-MANY dependency so that when one object (the
 *           SUBJECT / publisher) changes state, all its dependents
 *           (OBSERVERS / subscribers) are notified automatically.
 *
 *        Subject                         Observer (interface)
 *        - observers: List<Observer>        + update(event)
 *        + subscribe(o)                        ^        ^         ^
 *        + unsubscribe(o)                  EmailObs  SmsObs  AnalyticsObs
 *        + notify(event) -> for each o: o.update(event)
 *
 *  WHY: the subject doesn't know WHO is listening or WHAT they do. New
 *  reactions (send SMS, update the dashboard, award loyalty points) are added
 *  by subscribing a new observer, without touching the subject (OCP + loose coupling).
 *
 *  REAL WORLD: GUI event listeners, Spring's ApplicationEventPublisher /
 *  @EventListener, stock tickers, notification systems. At the distributed
 *  level, the same idea is Kafka/RabbitMQ pub-sub.
 *
 *  PITFALLS (senior-level discussion)
 *    1. MEMORY LEAKS ("lapsed listener"): forgetting to unsubscribe keeps
 *       observers alive. Fix: always unsubscribe, or use weak references.
 *    2. One SLOW or THROWING observer blocks or breaks the others. Fix:
 *       isolate each call (try/catch), or notify asynchronously (executor).
 *    3. ORDER of notification is usually not guaranteed. Don't rely on it.
 *    4. CONCURRENT MODIFICATION: an observer unsubscribing during notify.
 *       Fix: iterate over a copy, or use CopyOnWriteArrayList.
 *    5. Cascades / infinite loops if observers trigger more events.
 *
 *  java.util.Observable is DEPRECATED (it's a class, not an interface, and
 *  not thread-safe). Write your own or use the java.beans / Flow API.
 */

import java.util.*;
import java.util.concurrent.*;

public class P5_Observer {

    public static void main(String[] args) throws Exception {
        System.out.println("===== BASIC OBSERVER: order status updates =====");
        OrderTracker tracker = new OrderTracker();
        OrderListener email = e -> System.out.println("   [Email]     to customer: order " + e.orderId() + " is " + e.status());
        OrderListener sms = e -> System.out.println("   [SMS]       " + e.orderId() + " -> " + e.status());
        AnalyticsListener analytics = new AnalyticsListener();

        tracker.subscribe(email);
        tracker.subscribe(sms);
        tracker.subscribe(analytics);
        tracker.updateStatus("ORD-1", "SHIPPED");

        System.out.println("\n===== UNSUBSCRIBE =====");
        tracker.unsubscribe(sms);                       // customer turned off SMS
        tracker.updateStatus("ORD-1", "DELIVERED");
        System.out.println("   analytics counts = " + analytics.counts);

        System.out.println("\n===== ONE FAILING OBSERVER DOESN'T BREAK OTHERS =====");
        tracker.subscribe(e -> { throw new RuntimeException("loyalty service down"); });
        tracker.updateStatus("ORD-2", "PLACED");

        System.out.println("\n===== SELF-UNSUBSCRIBING OBSERVER (no ConcurrentModificationException) =====");
        OrderTracker t2 = new OrderTracker();
        t2.subscribe(new OneTimeListener(t2));
        t2.updateStatus("ORD-3", "PLACED");
        t2.updateStatus("ORD-3", "SHIPPED");           // OneTimeListener is gone by now

        System.out.println("\n===== ASYNC EVENT BUS (topic-based) =====");
        try (EventBus bus = new EventBus()) {
            bus.subscribe("payment.success", msg -> System.out.println("   [Invoice]  generate invoice for " + msg));
            bus.subscribe("payment.success", msg -> System.out.println("   [Inventory] reserve stock for " + msg));
            bus.subscribe("payment.failed", msg -> System.out.println("   [Retry]    schedule retry for " + msg));
            bus.publish("payment.success", "ORD-9");
            bus.publish("payment.failed", "ORD-10");
        }   // close() waits for async handlers to finish
    }

    // ---------------- Event + Observer interface ----------------
    record OrderEvent(String orderId, String status) {}

    @FunctionalInterface
    interface OrderListener {
        void onEvent(OrderEvent event);
    }

    // ---------------- Subject ----------------
    static class OrderTracker {
        // CopyOnWriteArrayList: iteration works on a snapshot, so listeners may
        // (un)subscribe during notification safely. Good for "many reads, few writes".
        private final List<OrderListener> listeners = new CopyOnWriteArrayList<>();

        void subscribe(OrderListener l) { listeners.add(l); }
        void unsubscribe(OrderListener l) { listeners.remove(l); }

        void updateStatus(String orderId, String status) {
            // ... business logic: save new status to the DB ...
            notifyAll(new OrderEvent(orderId, status));
        }

        private void notifyAll(OrderEvent e) {
            for (OrderListener l : listeners) {
                try {
                    l.onEvent(e);
                } catch (RuntimeException ex) {
                    // Isolate failures: log and continue with the other observers
                    System.out.println("   [WARN]      a listener failed: " + ex.getMessage());
                }
            }
        }
    }

    // ---------------- Concrete observers ----------------
    static class AnalyticsListener implements OrderListener {
        final Map<String, Integer> counts = new TreeMap<>();
        public void onEvent(OrderEvent e) {
            counts.merge(e.status(), 1, Integer::sum);
            System.out.println("   [Analytics] recorded " + e.status());
        }
    }

    static class OneTimeListener implements OrderListener {
        private final OrderTracker tracker;
        OneTimeListener(OrderTracker tracker) { this.tracker = tracker; }
        public void onEvent(OrderEvent e) {
            System.out.println("   [OneTime]   welcome offer sent for first event " + e.status() + ", unsubscribing");
            tracker.unsubscribe(this);
        }
    }

    // ---------------- Topic-based async event bus ----------------
    static class EventBus implements AutoCloseable {
        private final Map<String, List<java.util.function.Consumer<String>>> topics = new ConcurrentHashMap<>();
        private final ExecutorService executor = Executors.newFixedThreadPool(2);

        void subscribe(String topic, java.util.function.Consumer<String> handler) {
            topics.computeIfAbsent(topic, t -> new CopyOnWriteArrayList<>()).add(handler);
        }

        void publish(String topic, String message) {
            // Each handler runs on the pool: the publisher isn't blocked by slow subscribers
            for (var handler : topics.getOrDefault(topic, List.of())) {
                executor.submit(() -> handler.accept(message));
            }
        }

        @Override
        public void close() throws InterruptedException {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
