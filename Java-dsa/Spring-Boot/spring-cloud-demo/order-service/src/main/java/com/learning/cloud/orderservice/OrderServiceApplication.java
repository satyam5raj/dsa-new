package com.learning.cloud.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/*
 * ============================================================================
 *  ORDER SERVICE  (port 8090) - calls inventory-service BY NAME
 * ============================================================================
 *
 *  @LoadBalanced on a RestClient.Builder adds an interceptor that:
 *    1. sees the host "inventory-service" in http://inventory-service/api/...
 *    2. asks the DiscoveryClient (Eureka) for the live instances of that name
 *    3. picks one with Spring Cloud LoadBalancer (round-robin by default)
 *    4. rewrites the URL to http://<host>:<port>/api/...
 *  No IPs or ports in our code. Scale inventory to 10 instances and nothing changes here.
 *
 *  IN PRODUCTION you'd add on top:
 *    - Resilience4j (spring-cloud-starter-circuitbreaker-resilience4j): timeouts,
 *      retries, CIRCUIT BREAKER (stop calling a failing service), bulkheads
 *    - Spring Cloud Gateway: one entry point (routing, auth, rate limiting)
 *    - Distributed tracing (Micrometer Tracing + Zipkin/Tempo/OTel): follow one
 *      request across services via a trace id
 *    - Declarative HTTP clients: @HttpExchange interfaces (or OpenFeign)
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    /*
     * GOTCHA (real one, hit while building this demo): Eureka's own HTTP client ALSO uses
     * the RestClient.Builder bean. If the ONLY builder is @LoadBalanced, Eureka tries to
     * resolve "localhost:8761" as a SERVICE NAME through itself -> circular failure, and
     * nothing is ever discovered.
     * FIX: a normal @Primary builder for everyone else, plus a separate @LoadBalanced one.
     * @LoadBalanced is itself a @Qualifier, so it selects the right bean at the injection point.
     * Both reuse Boot's RestClientBuilderConfigurer (message converters, customizers).
     */
    @Bean
    @Primary
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    @Bean
    @LoadBalanced                                   // resolve service names through Eureka
    RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    @RestController
    static class OrderController {
        private final RestClient inventory;
        private final DiscoveryClient discovery;
        private final int maxQuantity;

        OrderController(@LoadBalanced RestClient.Builder builder, DiscoveryClient discovery,
                        @Value("${order.max-quantity:1}") int maxQuantity) {    // value comes from the Config Server
            this.inventory = builder.baseUrl("http://inventory-service").build();   // a SERVICE NAME, not a host!
            this.discovery = discovery;
            this.maxQuantity = maxQuantity;
        }

        @GetMapping("/api/orders/check/{sku}")
        Map<String, Object> check(@PathVariable String sku, @RequestParam(defaultValue = "1") int qty) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stock = inventory.get().uri("/api/inventory/{sku}", sku).retrieve().body(Map.class);
            int available = ((Number) stock.get("available")).intValue();
            boolean ok = qty <= maxQuantity && qty <= available;
            return Map.of(
                    "sku", sku,
                    "requested", qty,
                    "canOrder", ok,
                    "maxQuantity (from config server)", maxQuantity,
                    "inventoryAnswer", stock);
        }

        // What Eureka currently knows about inventory-service
        @GetMapping("/api/orders/instances")
        List<String> instances() {
            return discovery.getInstances("inventory-service").stream()
                    .map(i -> i.getHost() + ":" + i.getPort())
                    .toList();
        }
    }
}
