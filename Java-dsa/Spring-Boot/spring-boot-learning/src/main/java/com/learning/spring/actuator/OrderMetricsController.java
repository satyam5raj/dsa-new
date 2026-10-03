package com.learning.spring.actuator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/*
 * ============================================================================
 *  TOPIC 08 : CUSTOM METRICS WITH MICROMETER
 * ============================================================================
 *
 *  Micrometer = "SLF4J for metrics": one API, many backends (Prometheus, Datadog,
 *  CloudWatch, New Relic...). Spring Boot auto-configures a MeterRegistry.
 *
 *  METER TYPES
 *    Counter      : only goes up (orders placed, errors)
 *    Gauge        : a current value (queue size, active sessions)
 *    Timer        : count + total time + max (+ percentiles/histograms) of an operation
 *    DistributionSummary : like Timer for non-time values (payload sizes)
 *  TAGS (dimensions) let you slice: orders.placed{channel="web"} vs {channel="app"}.
 *  Keep tag values LOW-CARDINALITY: never use user ids or order ids as tags!
 *
 *  Spring Boot already records http.server.requests, jvm.*, hikaricp.*, etc.
 *
 *  Try:
 *    curl -X POST 'localhost:8080/api/orders?channel=web'
 *    curl -u admin:admin123 localhost:8080/actuator/metrics/orders.placed
 *    curl -u admin:admin123 'localhost:8080/actuator/metrics/orders.placed?tag=channel:web'
 */
@RestController
public class OrderMetricsController {

    private final MeterRegistry registry;
    private final Timer checkoutTimer;

    public OrderMetricsController(MeterRegistry registry) {
        this.registry = registry;
        this.checkoutTimer = Timer.builder("orders.checkout.duration")
                .description("Time spent in checkout")
                .register(registry);
    }

    @PostMapping("/api/orders")
    public Map<String, Object> placeOrder(@RequestParam(defaultValue = "web") String channel) {
        // record() times the lambda
        String orderId = checkoutTimer.record(() -> {
            sleep(ThreadLocalRandom.current().nextInt(5, 20));        // pretend work
            return "ORD-" + ThreadLocalRandom.current().nextInt(10_000);
        });
        // Counter with a tag. registry.counter(...) returns the SAME counter for the same name + tags.
        Counter.builder("orders.placed")
                .description("Number of orders placed")
                .tag("channel", channel)
                .register(registry)
                .increment();
        return Map.of("orderId", orderId, "channel", channel);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
