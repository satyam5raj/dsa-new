package com.learning.spring.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/*
 * ============================================================================
 *  TOPIC 02 : CONDITIONAL BEANS & PROFILES
 * ============================================================================
 *
 *  @ConditionalOnProperty : create the bean only if a property has a value.
 *                           The building block for FEATURE TOGGLES. Also what
 *                           Spring Boot's own auto-configurations use everywhere.
 *  @Profile("dev")        : create the bean only when that profile is active.
 *  @Profile("!dev")       : ... only when it is NOT active.
 *
 *  Other conditions you'll meet in auto-configuration:
 *    @ConditionalOnClass(X.class)     X is on the classpath (a library is present)
 *    @ConditionalOnMissingBean        the user hasn't defined their own bean of this type
 *    @ConditionalOnBean, @ConditionalOnWebApplication, @ConditionalOnExpression ...
 *
 *  Debug which conditions matched:  run with --debug  (prints the CONDITIONS
 *  EVALUATION REPORT), or GET /actuator/conditions.
 */
@Configuration
public class FeatureConfig {

    public interface RecommendationEngine {
        List<String> recommend(String userId);
    }

    @Bean
    @ConditionalOnProperty(name = "app.features.personalized-recommendations", havingValue = "true")
    public RecommendationEngine personalizedRecommendations() {
        return userId -> List.of("Because " + userId + " liked Java: 'Effective Java'", "'Java Concurrency in Practice'");
    }

    @Bean
    @ConditionalOnProperty(name = "app.features.personalized-recommendations", havingValue = "false", matchIfMissing = true)
    public RecommendationEngine popularItems() {
        return userId -> List.of("Top seller: 'Clean Code'", "Top seller: 'Designing Data-Intensive Applications'");
    }

    // ---------- profile-specific beans ----------
    public interface EnvironmentBanner {
        String banner();
    }

    @Bean
    @Profile("dev")
    public EnvironmentBanner devBanner() {
        return () -> "DEVELOPMENT - SQL logging on, fake data allowed";
    }

    @Bean
    @Profile("!dev")
    public EnvironmentBanner defaultBanner() {
        return () -> "DEFAULT profile";
    }
}
