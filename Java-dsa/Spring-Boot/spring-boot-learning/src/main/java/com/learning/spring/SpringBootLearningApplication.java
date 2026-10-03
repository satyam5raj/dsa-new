package com.learning.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/*
 * ============================================================================
 *  ENTRY POINT
 * ============================================================================
 *
 *  @SpringBootApplication is a shortcut for three annotations:
 *
 *    @SpringBootConfiguration   - this class is a source of bean definitions
 *                                 (a specialised @Configuration)
 *    @EnableAutoConfiguration   - "look at the classpath and my properties and
 *                                 configure sensible beans for me" (see the
 *                                 autoconfig package and topic 02)
 *    @ComponentScan             - find @Component / @Service / @Repository /
 *                                 @Controller / @Configuration classes in THIS
 *                                 package and all sub-packages
 *
 *  That's why this class sits in the ROOT package (com.learning.spring): every
 *  topic package below it gets scanned automatically.
 *
 *  @ConfigurationPropertiesScan finds @ConfigurationProperties classes
 *  (like config.StoreProperties) and registers them as beans.
 *
 *  WHAT HAPPENS IN SpringApplication.run(...)
 *    1. Create the Environment (properties, profiles, env vars, CLI args)
 *    2. Create the ApplicationContext (the IoC container)
 *    3. Load bean DEFINITIONS (component scan + @Bean methods + auto-configs)
 *    4. Instantiate singletons, inject dependencies, run lifecycle callbacks
 *    5. Start the embedded web server (Tomcat by default)
 *    6. Call CommandLineRunner / ApplicationRunner beans
 *    7. Publish ApplicationReadyEvent: the app is ready for traffic
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringBootLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootLearningApplication.class, args);
    }
}
