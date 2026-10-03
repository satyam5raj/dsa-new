/*
 * ============================================================================
 *  PATTERN 6 : DECORATOR  (Structural)   aka Wrapper
 * ============================================================================
 *
 *  Run:  java P6_Decorator.java
 *
 *  INTENT : attach extra responsibilities to an object DYNAMICALLY by
 *           wrapping it in another object with the SAME interface.
 *           Decorators can be STACKED in any combination.
 *
 *        Component (interface)
 *           ^                ^
 *      ConcreteComponent   Decorator (implements Component, HAS-A Component)
 *                             ^            ^             ^
 *                         Logging       Retry         Caching
 *
 *     new Logging(new Retry(new Caching(new RealService())))
 *     A call passes through each layer: each one adds behaviour before
 *     and/or after delegating to the wrapped object.
 *
 *  WHY NOT SUBCLASSES? For N independent features you'd need 2^N subclasses
 *  (LoggingRetryCachingService, LoggingCachingService...). Decorators
 *  combine at RUNTIME with N classes. Composition over inheritance in action.
 *
 *  JDK EXAMPLE (the famous one): java.io streams
 *    new BufferedReader(new InputStreamReader(new FileInputStream("f")))
 *  Also: Collections.unmodifiableList(list), Collections.synchronizedMap(map)
 *  SPRING: AOP proxies (@Transactional, @Cacheable) are decorators generated at runtime.
 *
 *  DECORATOR vs PROXY: same structure. Decorator ADDS behaviour and is
 *  stacked by the client. Proxy CONTROLS ACCESS (lazy loading, security,
 *  remote calls) and usually manages the real object's lifecycle itself.
 *  DECORATOR vs ADAPTER: Decorator keeps the SAME interface. Adapter CHANGES it.
 */

import java.util.*;

public class P6_Decorator {

    public static void main(String[] args) {
        coffeeExample();
        serviceExample();
    }

    // =================================================================
    // 1. Classic: coffee with add-ons
    // =================================================================
    interface Beverage {
        String description();
        double cost();
    }

    static class Espresso implements Beverage {
        public String description() { return "Espresso"; }
        public double cost() { return 120; }
    }

    static class GreenTea implements Beverage {
        public String description() { return "Green Tea"; }
        public double cost() { return 80; }
    }

    // Base decorator: holds the wrapped beverage and delegates by default
    static abstract class AddOn implements Beverage {
        protected final Beverage inner;
        AddOn(Beverage inner) { this.inner = inner; }
    }

    static class Milk extends AddOn {
        Milk(Beverage b) { super(b); }
        public String description() { return inner.description() + " + Milk"; }
        public double cost() { return inner.cost() + 30; }
    }

    static class Caramel extends AddOn {
        Caramel(Beverage b) { super(b); }
        public String description() { return inner.description() + " + Caramel"; }
        public double cost() { return inner.cost() + 45; }
    }

    static class ExtraShot extends AddOn {
        ExtraShot(Beverage b) { super(b); }
        public String description() { return inner.description() + " + Extra Shot"; }
        public double cost() { return inner.cost() + 50; }
    }

    static void coffeeExample() {
        System.out.println("===== 1. COFFEE ADD-ONS =====");
        List<Beverage> orders = List.of(
                new Espresso(),
                new Milk(new Espresso()),
                new Caramel(new Milk(new ExtraShot(new Espresso()))),
                new Milk(new Milk(new GreenTea())));               // same decorator twice: double milk
        for (Beverage b : orders) System.out.printf("   %-45s Rs %.0f%n", b.description(), b.cost());
    }

    // =================================================================
    // 2. Real-world: add cross-cutting concerns to a service
    //    (this is exactly what Spring AOP does behind the scenes)
    // =================================================================
    interface PriceService {
        double getPrice(String productId);
    }

    // The real service: knows nothing about logging, caching, retries
    static class RemotePriceService implements PriceService {
        private int calls = 0;
        public double getPrice(String productId) {
            calls++;
            if (calls % 3 == 1) throw new RuntimeException("network glitch");   // flaky remote call
            return productId.length() * 100.0;
        }
    }

    static class LoggingPriceService implements PriceService {
        private final PriceService inner;
        LoggingPriceService(PriceService inner) { this.inner = inner; }
        public double getPrice(String id) {
            long start = System.nanoTime();
            System.out.println("   [log] -> getPrice(" + id + ")");
            double p = inner.getPrice(id);
            System.out.printf("   [log] <- %.0f in %d us%n", p, (System.nanoTime() - start) / 1000);
            return p;
        }
    }

    static class RetryingPriceService implements PriceService {
        private final PriceService inner;
        private final int maxAttempts;
        RetryingPriceService(PriceService inner, int maxAttempts) { this.inner = inner; this.maxAttempts = maxAttempts; }
        public double getPrice(String id) {
            for (int attempt = 1; ; attempt++) {
                try {
                    return inner.getPrice(id);
                } catch (RuntimeException e) {
                    System.out.println("   [retry] attempt " + attempt + " failed: " + e.getMessage());
                    if (attempt == maxAttempts) throw e;
                }
            }
        }
    }

    static class CachingPriceService implements PriceService {
        private final PriceService inner;
        private final Map<String, Double> cache = new HashMap<>();
        CachingPriceService(PriceService inner) { this.inner = inner; }
        public double getPrice(String id) {
            if (cache.containsKey(id)) {
                System.out.println("   [cache] hit for " + id);
                return cache.get(id);
            }
            double p = inner.getPrice(id);
            cache.put(id, p);
            return p;
        }
    }

    static void serviceExample() {
        System.out.println("\n===== 2. STACKED SERVICE DECORATORS =====");
        // Order matters! Cache outside Retry: cache hits skip retries entirely.
        PriceService service =
                new LoggingPriceService(
                        new CachingPriceService(
                                new RetryingPriceService(
                                        new RemotePriceService(), 3)));

        System.out.println("first call:");
        service.getPrice("laptop");
        System.out.println("second call (same id):");
        service.getPrice("laptop");
    }
}
