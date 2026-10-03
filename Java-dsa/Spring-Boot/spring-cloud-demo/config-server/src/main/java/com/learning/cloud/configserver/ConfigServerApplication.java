package com.learning.cloud.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/*
 * ============================================================================
 *  SPRING CLOUD CONFIG SERVER  (port 8888)
 * ============================================================================
 *
 *  PROBLEM: 20 microservices x 4 environments = 80 property files baked into
 *  80 jars. Changing a timeout means rebuilding and redeploying.
 *
 *  SOLUTION: ONE central server that serves configuration over HTTP:
 *      GET http://localhost:8888/{application}/{profile}
 *      GET http://localhost:8888/inventory-service/default
 *  Each service fetches its config AT STARTUP (spring.config.import=configserver:...)
 *  before creating any beans.
 *
 *  BACKENDS
 *    git (most common in production): config lives in a Git repo -> versioned,
 *        reviewed in PRs, auditable, easy rollback.
 *        spring.cloud.config.server.git.uri=https://github.com/acme/config-repo
 *    native : files on the classpath/filesystem (used HERE, so the demo needs no Git repo)
 *    vault  : secrets from HashiCorp Vault
 *  File naming in the backend:
 *    application.properties                 -> shared by ALL services
 *    inventory-service.properties           -> one service, all profiles
 *    inventory-service-prod.properties      -> one service, "prod" profile
 *
 *  REFRESH WITHOUT RESTART: @RefreshScope beans + POST /actuator/refresh
 *  (or Spring Cloud Bus to broadcast the refresh to every instance via Kafka/RabbitMQ).
 *
 *  KUBERNETES NOTE: on k8s, ConfigMaps/Secrets often replace the config server.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
