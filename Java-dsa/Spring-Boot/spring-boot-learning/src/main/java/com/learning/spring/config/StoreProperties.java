package com.learning.spring.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/*
 * ============================================================================
 *  TOPIC 02 : TYPE-SAFE CONFIGURATION with @ConfigurationProperties
 * ============================================================================
 *
 *  Binds every property under the prefix "app.store" to this record:
 *      app.store.name=Learning Store               -> name
 *      app.store.max-items-per-order=5             -> maxItemsPerOrder   (RELAXED binding:
 *      APP_STORE_MAXITEMSPERORDER=5 (env var)      -> maxItemsPerOrder    kebab, camel, env all work)
 *      app.store.order-timeout=30s                 -> Duration.ofSeconds(30)  (type conversion)
 *
 *  @Value("${app.store.name}") vs @ConfigurationProperties
 *    @Value                    : one value, string-based, no validation, scattered across classes
 *    @ConfigurationProperties  : a GROUP of settings, strongly typed, VALIDATED at
 *                                startup, IDE auto-completion, immutable as a record
 *    -> prefer @ConfigurationProperties for anything beyond a one-off value.
 *
 *  @Validated + Jakarta Bean Validation annotations = FAIL FAST.
 *  A bad value (max-items-per-order=0) stops the app at STARTUP with a clear
 *  message, instead of failing later on some random request.
 *  (Registered via @ConfigurationPropertiesScan on the main class.)
 */
@Validated
@ConfigurationProperties(prefix = "app.store")
public record StoreProperties(
        @NotBlank String name,
        @Min(1) @Max(100) int maxItemsPerOrder,
        @Email String supportEmail,
        @NotNull Duration orderTimeout) {
}
