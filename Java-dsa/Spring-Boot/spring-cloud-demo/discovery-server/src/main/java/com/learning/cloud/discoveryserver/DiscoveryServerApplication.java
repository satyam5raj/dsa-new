package com.learning.cloud.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/*
 * ============================================================================
 *  SERVICE DISCOVERY with EUREKA SERVER  (port 8761, dashboard at http://localhost:8761)
 * ============================================================================
 *
 *  PROBLEM: in the cloud, instances come and go (autoscaling, deploys, crashes)
 *  with changing IPs and ports. Hard-coding "inventory is at 10.0.3.17:8081" breaks constantly.
 *
 *  SOLUTION: a SERVICE REGISTRY (a phone book of live instances)
 *    1. REGISTER  : each service, on startup, tells Eureka "I'm inventory-service at host:port"
 *    2. HEARTBEAT : it renews its lease every N seconds. No heartbeat -> EVICTED
 *    3. DISCOVER  : clients fetch the registry and CACHE it locally (still works if Eureka blips)
 *    4. LOAD BALANCE: the client picks an instance (Spring Cloud LoadBalancer, round-robin)
 *  This is CLIENT-SIDE discovery and load balancing.
 *  (SERVER-SIDE alternative: the client calls a load balancer / k8s Service, which picks the instance.)
 *
 *  SELF-PRESERVATION: if MANY heartbeats stop at once, Eureka assumes a NETWORK
 *  problem (not mass death) and stops evicting. Safer in prod; disabled here so
 *  the demo reacts fast.
 *
 *  ALTERNATIVES: Consul, Zookeeper, and on Kubernetes the built-in DNS + Services
 *  (no Eureka needed: http://inventory-service just works).
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
