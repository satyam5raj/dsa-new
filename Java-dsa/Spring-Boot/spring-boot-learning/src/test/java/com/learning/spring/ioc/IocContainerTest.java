package com.learning.spring.ioc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * TOPIC 01 proofs: scopes, lifecycle order, qualifiers, @Bean lifecycle methods.
 *
 * @SpringBootTest starts the WHOLE application context (no web server by default:
 * a mock servlet environment). Contexts are CACHED between test classes with the
 * same configuration, so many @SpringBootTest classes don't each pay the startup cost.
 */
@SpringBootTest
@ActiveProfiles("test")
class IocContainerTest {

    @Autowired ApplicationContext context;
    @Autowired ScopedBeans.ReportService reportService;
    @Autowired Notifications.NotificationService notificationService;
    @Autowired AppConfig.LegacyConnectionPool pool;

    @Test
    void singletonScope_sameInstanceEveryTime() {
        var a = context.getBean(ScopedBeans.VisitCounter.class);
        var b = context.getBean(ScopedBeans.VisitCounter.class);
        assertThat(a).isSameAs(b);
    }

    @Test
    void prototypeScope_newInstanceEveryTime() {
        var a = context.getBean(ScopedBeans.ReportBuilder.class);
        var b = context.getBean(ScopedBeans.ReportBuilder.class);
        assertThat(a).isNotSameAs(b);
        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void prototypeInsideSingleton_trapAndFix() {
        // TRAP: the injected prototype was created once -> same id forever
        assertThat(reportService.builderIdUsingInjectedField())
                .isEqualTo(reportService.builderIdUsingInjectedField());
        // FIX: ObjectProvider asks the container every time -> a fresh prototype
        assertThat(reportService.builderIdUsingProvider())
                .isNotEqualTo(reportService.builderIdUsingProvider());
    }

    @Test
    void qualifiersPrimaryListAndMapInjection() {
        assertThat(notificationService.notifyDefault("ravi", "hi")).startsWith("EMAIL");   // @Primary
        assertThat(notificationService.notifyBySms("ravi", "hi")).startsWith("SMS");       // @Qualifier("sms")
        assertThat(notificationService.channelCount()).isEqualTo(3);                       // List<> of all
        assertThat(notificationService.notifyVia("push", "ravi", "hi")).startsWith("PUSH"); // Map<name, bean>
        assertThatThrownBy(() -> notificationService.notifyVia("fax", "ravi", "hi"))
                .hasMessageContaining("Unknown channel");
    }

    @Test
    void beanWithInitMethod_isOpenedByTheContainer() {
        assertThat(pool.isOpen()).isTrue();   // initMethod = "open" was called
    }

    @Test
    void fullLifecycleOrder_includingDestroyCallbacks() {
        // Build a tiny container by hand: an ApplicationContext is just a Java object
        LifecycleDemoBean.EVENTS.clear();
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(LifecycleDemoBean.class, LoggingBeanPostProcessor.class);
        ctx.close();                                         // triggers the destroy callbacks

        assertThat(LifecycleDemoBean.EVENTS).containsExactly(
                "1. constructor",
                "3. BeanNameAware.setBeanName(lifecycleDemoBean)",
                "4. BeanPostProcessor.before",
                "5. @PostConstruct",
                "6. InitializingBean.afterPropertiesSet",
                "8. BeanPostProcessor.after (proxies are created here)",
                "9. @PreDestroy",
                "10. DisposableBean.destroy");
        LifecycleDemoBean.EVENTS.forEach(e -> System.out.println("   lifecycle: " + e));
    }
}
