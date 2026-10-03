package com.learning.spring.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/*
 * ============================================================================
 *  TOPIC 02 : HOW SPRING BOOT AUTO-CONFIGURATION WORKS (by writing one)
 * ============================================================================
 *
 *  THE MECHANISM
 *   1. @EnableAutoConfiguration (inside @SpringBootApplication) reads every file named
 *        META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *      on the classpath. Each line is an auto-configuration class.
 *      (Before Boot 2.7 this was META-INF/spring.factories.)
 *   2. Each auto-config is a @Configuration guarded by CONDITIONS:
 *        @ConditionalOnClass(DataSource.class)   -> only if the library is present
 *        @ConditionalOnMissingBean               -> only if YOU didn't define one
 *        @ConditionalOnProperty(...)             -> only if enabled
 *   3. Auto-configs are processed AFTER your own beans, so @ConditionalOnMissingBean
 *      can see whether you already provided one. That's "convention over
 *      configuration" with an escape hatch: "back off when the user defines their own".
 *
 *  A "STARTER" (spring-boot-starter-data-jpa, -webmvc, -security...) is just a POM
 *  that pulls in the libraries PLUS the matching auto-configuration module.
 *  Example: adding spring-boot-starter-data-jpa puts Hibernate + HikariCP on the
 *  classpath -> DataSourceAutoConfiguration and HibernateJpaAutoConfiguration
 *  activate -> you get a connection pool, an EntityManagerFactory and a
 *  transaction manager without writing a single bean.
 *
 *  This class is listed in src/main/resources/META-INF/spring/...AutoConfiguration.imports.
 *  It is NOT picked up by component scanning (Boot excludes auto-config classes from the scan).
 */
@AutoConfiguration
@ConditionalOnProperty(name = "app.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditLogger.class)        // back off if the app defines its own AuditLogger
    public AuditLogger auditLogger(Environment env) {
        String prefix = "[AUDIT " + env.getProperty("spring.application.name", "app") + "]";
        return new AuditLogger.ConsoleAuditLogger(prefix);
    }
}
