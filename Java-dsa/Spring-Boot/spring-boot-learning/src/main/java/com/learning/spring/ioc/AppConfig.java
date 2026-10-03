package com.learning.spring.ioc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/*
 * ============================================================================
 *  TOPIC 01 : JAVA CONFIGURATION WITH @Configuration + @Bean
 * ============================================================================
 *
 *  Two ways to declare beans:
 *    1. Annotate YOUR class with @Component/@Service... (component scanning)
 *    2. Write a @Bean factory method in a @Configuration class. Use this for
 *       classes you DON'T own (third-party libraries, JDK types like Clock),
 *       or when creation needs logic.
 *
 *  @Configuration classes are proxied with CGLIB ("full mode"): calling one
 *  @Bean method from another returns the SAME singleton, not a new object.
 *  (@Configuration(proxyBeanMethods = false) = "lite mode": faster startup,
 *   but then don't call @Bean methods directly from each other.)
 */
@Configuration
public class AppConfig {

    // A JDK class we can't annotate. Injecting a Clock instead of calling
    // Instant.now() directly makes time-based code TESTABLE (use a fixed clock in tests).
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // Lifecycle methods for a class with NO Spring annotations at all
    @Bean(initMethod = "open", destroyMethod = "close")
    public LegacyConnectionPool legacyConnectionPool() {
        return new LegacyConnectionPool(5);
    }

    /** Pretend third-party class: knows nothing about Spring. */
    public static class LegacyConnectionPool {
        private final int size;
        private boolean open;

        public LegacyConnectionPool(int size) { this.size = size; }
        public void open() { open = true; }      // called by Spring after construction (step 7)
        public void close() { open = false; }    // called by Spring on shutdown (step 11)
        public boolean isOpen() { return open; }
        public int size() { return size; }
    }
}
