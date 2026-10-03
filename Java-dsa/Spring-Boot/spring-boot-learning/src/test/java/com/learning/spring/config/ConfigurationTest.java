package com.learning.spring.config;

import com.learning.spring.autoconfig.AuditAutoConfiguration;
import com.learning.spring.autoconfig.AuditLogger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * TOPIC 02 proofs: property binding, profiles, conditional beans, auto-configuration.
 */
class ConfigurationTest {

    // ------------------------------------------------------------------
    // Default profile (+ "test"): values from application.properties
    // ------------------------------------------------------------------
    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    class DefaultProfile {
        @Autowired StoreProperties store;
        @Autowired FeatureConfig.RecommendationEngine recommendations;
        @Autowired FeatureConfig.EnvironmentBanner banner;

        @Test
        void propertiesAreBoundAndConverted() {
            assertThat(store.name()).isEqualTo("Learning Store");
            assertThat(store.maxItemsPerOrder()).isEqualTo(5);                   // kebab-case -> camelCase
            assertThat(store.orderTimeout()).isEqualTo(Duration.ofSeconds(30));  // "30s" -> Duration
        }

        @Test
        void featureToggleOn_personalizedEngineCreated() {
            assertThat(recommendations.recommend("satyam").getFirst()).contains("Because satyam liked Java");
        }

        @Test
        void notDevProfile_defaultBanner() {
            assertThat(banner.banner()).isEqualTo("DEFAULT profile");
        }
    }

    // ------------------------------------------------------------------
    // "dev" profile: application-dev.properties OVERRIDES the defaults
    // ------------------------------------------------------------------
    @Nested
    @SpringBootTest
    @ActiveProfiles({"test", "dev"})
    class DevProfile {
        @Autowired StoreProperties store;
        @Autowired FeatureConfig.RecommendationEngine recommendations;
        @Autowired FeatureConfig.EnvironmentBanner banner;

        @Test
        void devOverridesAndProfileBeans() {
            assertThat(store.name()).isEqualTo("Learning Store (DEV)");          // overridden
            assertThat(store.maxItemsPerOrder()).isEqualTo(5);                   // inherited from default
            assertThat(banner.banner()).startsWith("DEVELOPMENT");               // @Profile("dev") bean
            assertThat(recommendations.recommend("x").getFirst()).startsWith("Top seller"); // toggle is off in dev
        }
    }

    // ------------------------------------------------------------------
    // Highest priority: properties passed directly (like CLI args / env vars)
    // ------------------------------------------------------------------
    @Nested
    @SpringBootTest(properties = "app.store.name=From Test Properties")
    @ActiveProfiles("test")
    class OverriddenByProperties {
        @Autowired StoreProperties store;

        @Test
        void inlinePropertiesWin() {
            assertThat(store.name()).isEqualTo("From Test Properties");
        }
    }

    // ------------------------------------------------------------------
    // ApplicationContextRunner: a tiny, fast context for testing configuration.
    // This is how Spring Boot tests its OWN auto-configurations.
    // ------------------------------------------------------------------
    @Test
    void autoConfiguration_providesDefaultBean() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(AuditLogger.class)
                        .getBean(AuditLogger.class).isInstanceOf(AuditLogger.ConsoleAuditLogger.class));
    }

    @Test
    void autoConfiguration_backsOffWhenUserDefinesTheirOwn() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class))
                .withUserConfiguration(CustomAuditConfig.class)
                .run(ctx -> assertThat(ctx.getBean(AuditLogger.class).log("x")).isEqualTo("custom:x"));   // @ConditionalOnMissingBean
    }

    @Test
    void autoConfiguration_canBeSwitchedOff() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class))
                .withPropertyValues("app.audit.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditLogger.class));                   // @ConditionalOnProperty
    }

    @Test
    void invalidProperties_failFastAtStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
                .withUserConfiguration(PropsConfig.class)
                .withPropertyValues("app.store.name=Shop", "app.store.max-items-per-order=0",   // violates @Min(1)
                        "app.store.support-email=not-an-email", "app.store.order-timeout=10s")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    String failure = ctx.getStartupFailure().toString() + ctx.getStartupFailure().getCause();
                    System.out.println("   startup failed as expected: " + ctx.getStartupFailure().getMessage());
                    assertThat(failure).contains("app.store");
                });
    }

    @Configuration
    static class CustomAuditConfig {
        @Bean
        AuditLogger myAuditLogger() { return event -> "custom:" + event; }
    }

    @Configuration
    @EnableConfigurationProperties(StoreProperties.class)
    static class PropsConfig {}
}
