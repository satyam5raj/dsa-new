package com.learning.spring.config;

import com.learning.spring.autoconfig.AuditLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Shows the effect of properties, profiles, conditions and auto-configuration.
 *
 *   curl localhost:8080/api/config
 *   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev     -> then curl again
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.store.name=FromCLI
 */
@RestController
public class ConfigController {

    private final StoreProperties store;
    private final FeatureConfig.RecommendationEngine recommendations;
    private final FeatureConfig.EnvironmentBanner banner;
    private final AuditLogger auditLogger;
    private final Environment environment;
    private final String appName;

    public ConfigController(StoreProperties store,
                            FeatureConfig.RecommendationEngine recommendations,
                            FeatureConfig.EnvironmentBanner banner,
                            AuditLogger auditLogger,
                            Environment environment,
                            @Value("${spring.application.name}") String appName) {   // single value via @Value
        this.store = store;
        this.recommendations = recommendations;
        this.banner = banner;
        this.auditLogger = auditLogger;
        this.environment = environment;
        this.appName = appName;
    }

    @GetMapping("/api/config")
    public Map<String, Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appName (@Value)", appName);
        result.put("store (@ConfigurationProperties)", store);
        result.put("activeProfiles", Arrays.toString(environment.getActiveProfiles()));
        result.put("banner (@Profile)", banner.banner());
        result.put("recommendations (@ConditionalOnProperty)", recommendations.recommend("satyam"));
        result.put("auditLogger (custom auto-configuration)", auditLogger.getClass().getSimpleName());
        return result;
    }
}
