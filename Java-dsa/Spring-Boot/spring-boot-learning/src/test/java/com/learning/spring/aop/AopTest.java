package com.learning.spring.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * TOPIC 07 proofs: the bean is a proxy, advice runs in order, and self-invocation is not intercepted.
 */
@SpringBootTest
@ActiveProfiles("test")
class AopTest {

    @Autowired InventoryService inventory;

    @BeforeEach
    void clear() {
        PerformanceAspect.RECORDED.clear();
    }

    @Test
    void injectedBeanIsACglibProxy() {
        System.out.println("   injected class: " + inventory.getClass().getName());
        assertThat(AopUtils.isAopProxy(inventory)).isTrue();
        assertThat(AopUtils.isCglibProxy(inventory)).isTrue();           // a generated SUBCLASS of InventoryService
    }

    @Test
    void adviceRunsAroundTheCall_inPrecedenceOrder() {
        // The service is a SINGLETON shared by every test in this context, so its stock may
        // already have been changed by another test. Compute the expected value instead of hard-coding it.
        int remaining = inventory.reserve("SKU-1", 1) - 1;
        PerformanceAspect.RECORDED.clear();

        inventory.reserve("SKU-1", 1);
        List<String> events = PerformanceAspect.RECORDED;
        events.forEach(e -> System.out.println("   aspect: " + e));

        // ORDER: @Around wraps EVERYTHING, so its "after" part (the finally block that
        // records TIMED) runs LAST, after @AfterReturning:
        //   @Around(start) -> @Before -> method -> @AfterReturning -> @After -> @Around(end)
        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isEqualTo("BEFORE reserve[SKU-1, 1]");
        assertThat(events.get(1)).isEqualTo("RETURNED reserve -> " + remaining);
        assertThat(events.get(2)).startsWith("TIMED reserve took");
    }

    @Test
    void afterThrowingAdvice() {
        assertThatThrownBy(() -> inventory.reserve("SKU-2", 100)).isInstanceOf(IllegalStateException.class);
        assertThat(PerformanceAspect.RECORDED).contains("THREW reserve -> Only 3 left of SKU-2");
    }

    @Test
    void selfInvocation_isNotIntercepted() {
        inventory.reserveAll(List.of("SKU-1", "SKU-1"));
        // Only reserveAll went through the proxy. The two internal this.reserve(...) calls are invisible to the aspect.
        assertThat(PerformanceAspect.RECORDED).noneMatch(e -> e.contains("TIMED reserve"));
        assertThat(PerformanceAspect.RECORDED).containsExactly("BEFORE reserveAll[[SKU-1, SKU-1]]", "RETURNED reserveAll -> 2");
    }
}
