package com.learning.spring.actuator;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/*
 * ============================================================================
 *  TOPIC 08 : ACTUATOR - HEALTH CHECKS
 * ============================================================================
 *
 *  Spring Boot Actuator adds production endpoints under /actuator:
 *    /actuator/health      UP/DOWN + components (db, diskSpace, ping, + yours)
 *    /actuator/health/liveness, /readiness  -> Kubernetes probes (auto-enabled on k8s)
 *    /actuator/info        build/git/app info
 *    /actuator/metrics     Micrometer metrics (JVM, HTTP, DB pool, custom)
 *    /actuator/prometheus  scrape endpoint (add micrometer-registry-prometheus)
 *    /actuator/beans, /conditions, /env, /mappings, /loggers, /threaddump, /heapdump ...
 *  Expose them with management.endpoints.web.exposure.include, and SECURE the
 *  sensitive ones (env and heapdump can leak secrets!). See SecurityConfig.
 *
 *  A custom HealthIndicator: its bean name minus "HealthIndicator" becomes the
 *  component name -> "inventory". If ANY component is DOWN, the overall status is
 *  DOWN and the endpoint returns HTTP 503, so the load balancer / k8s stops routing to it.
 *
 *  LIVENESS vs READINESS (Kubernetes)
 *    liveness  : "is the process stuck?"            -> fail = RESTART the pod
 *    readiness : "can it serve traffic right now?"  -> fail = remove from the Service endpoints
 *  Don't put external dependencies (the DB) into LIVENESS. A DB outage would
 *  restart every pod for nothing.
 */
@Component
public class InventoryHealthIndicator implements HealthIndicator {

    private final AtomicBoolean warehouseReachable = new AtomicBoolean(true);

    @Override
    public Health health() {
        if (warehouseReachable.get()) {
            return Health.up()
                    .withDetail("warehouse", "reachable")
                    .withDetail("pendingSyncs", 0)
                    .build();
        }
        return Health.down()
                .withDetail("warehouse", "unreachable")
                .withDetail("hint", "check the warehouse API credentials")
                .build();
    }

    // Lets the demo/test simulate an outage
    public void setWarehouseReachable(boolean reachable) {
        warehouseReachable.set(reachable);
    }
}
