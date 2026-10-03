package com.learning.spring.ioc;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/*
 * A BeanPostProcessor (BPP) is a hook that sees EVERY bean the container
 * creates, right before and right after its initialization callbacks.
 *
 * Spring itself is built on BPPs:
 *   - AutowiredAnnotationBeanPostProcessor     -> processes @Autowired / @Value
 *   - CommonAnnotationBeanPostProcessor        -> calls @PostConstruct / @PreDestroy
 *   - AnnotationAwareAspectJAutoProxyCreator   -> wraps beans in AOP PROXIES
 *     (that's how @Transactional, @Async, @Cacheable and @PreAuthorize work!)
 *
 * The object returned from postProcessAfterInitialization is what other beans
 * receive. A BPP can return a WRAPPER (proxy) instead of the original bean.
 *
 * BPPs are created very early (before regular beans), so keep them free of
 * dependencies on other beans.
 */
@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof LifecycleDemoBean demo) {
            demo.record("4. BeanPostProcessor.before");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof LifecycleDemoBean demo) {
            demo.record("8. BeanPostProcessor.after (proxies are created here)");
        }
        return bean;   // could return a proxy wrapping 'bean' instead
    }
}
