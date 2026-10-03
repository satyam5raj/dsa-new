package com.learning.cloud.inventoryservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
 * ============================================================================
 *  INVENTORY SERVICE  (run 2 instances: ports 8081 and 8082)
 * ============================================================================
 *
 *  - Pulls its configuration from the Config Server (spring.config.import)
 *  - Registers itself in Eureka as "inventory-service" (just having the
 *    eureka-client starter on the classpath is enough; no annotation needed)
 *  - Answers with ITS OWN PORT so you can watch the load balancer alternate instances
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    // @RefreshScope: this bean is re-created after POST /actuator/refresh,
    // picking up changed values from the Config Server without restarting the app.
    @Component
    @RefreshScope
    static class WarehouseInfo {
        private final String name;

        WarehouseInfo(@Value("${inventory.warehouse-name}") String name) {
            this.name = name;
        }

        String name() { return name; }
    }

    @RestController
    static class InventoryController {
        private final WarehouseInfo warehouse;
        private final int port;
        private final Map<String, Integer> stock = Map.of("SKU-1", 10, "SKU-2", 0);

        InventoryController(WarehouseInfo warehouse, @Value("${server.port}") int port) {
            this.warehouse = warehouse;
            this.port = port;
        }

        @GetMapping("/api/inventory/{sku}")
        Map<String, Object> stock(@PathVariable String sku) {
            return Map.of(
                    "sku", sku,
                    "available", stock.getOrDefault(sku, 0),
                    "warehouse", warehouse.name(),
                    "servedByPort", port);
        }
    }
}
