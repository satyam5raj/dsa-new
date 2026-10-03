package com.learning.spring.ioc;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * ============================================================================
 *  TOPIC 01 : IoC & DI  -  THE BEAN LIFECYCLE
 * ============================================================================
 *
 *  IoC (Inversion of Control): you don't create your objects with 'new' and
 *  wire them together. The CONTAINER (ApplicationContext) does it. You only
 *  describe WHAT you need. Control over creation is "inverted".
 *  DI (Dependency Injection) is HOW IoC hands objects their dependencies:
 *  through the constructor (preferred), a setter, or a field.
 *
 *  A BEAN = an object created and managed by the Spring container.
 *
 *  FULL LIFECYCLE OF A SINGLETON BEAN (this class records every step):
 *     1. Instantiate              -> constructor (constructor injection happens here)
 *     2. Populate properties      -> setter / field injection
 *     3. Aware callbacks          -> BeanNameAware.setBeanName, ApplicationContextAware...
 *     4. BeanPostProcessor        -> postProcessBeforeInitialization
 *     5. @PostConstruct
 *     6. InitializingBean.afterPropertiesSet()
 *     7. custom init-method       -> @Bean(initMethod = "...")
 *     8. BeanPostProcessor        -> postProcessAfterInitialization  (AOP PROXIES are created HERE!)
 *     ---- bean is READY and used by the application ----
 *     9. @PreDestroy                     (on context close)
 *    10. DisposableBean.destroy()
 *    11. custom destroy-method
 *  NOTE: PROTOTYPE beans get steps 1-8 but NO destroy callbacks. The container
 *  hands them out and forgets them.
 *
 *  In real code: use @PostConstruct / @PreDestroy (or init/destroy methods).
 *  The Spring interfaces (InitializingBean...) couple your class to Spring.
 */
@Component
public class LifecycleDemoBean implements BeanNameAware, InitializingBean, DisposableBean {

    // Static so tests can read the events even after the context is closed
    public static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    public LifecycleDemoBean() {
        EVENTS.add("1. constructor");
    }

    @Override
    public void setBeanName(String name) {
        EVENTS.add("3. BeanNameAware.setBeanName(" + name + ")");
    }

    @PostConstruct
    void postConstruct() {
        EVENTS.add("5. @PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        EVENTS.add("6. InitializingBean.afterPropertiesSet");
    }

    @PreDestroy
    void preDestroy() {
        EVENTS.add("9. @PreDestroy");
    }

    @Override
    public void destroy() {
        EVENTS.add("10. DisposableBean.destroy");
    }

    void record(String event) {          // used by LoggingBeanPostProcessor
        EVENTS.add(event);
    }
}
