package com.learning.spring.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * ============================================================================
 *  TOPIC 07 : AOP - ASPECT-ORIENTED PROGRAMMING
 * ============================================================================
 *
 *  PROBLEM: some concerns cut ACROSS every layer: logging, timing, security
 *  checks, transactions, retries, caching, auditing. Copy-pasting them into every
 *  method tangles business code with plumbing.
 *  AOP lets you write that logic ONCE and declare WHERE it applies.
 *
 *  VOCABULARY
 *    Aspect     : the module holding the cross-cutting logic (this class)
 *    Join point : a point during execution. In Spring AOP: always a METHOD EXECUTION
 *    Pointcut   : an expression that SELECTS join points
 *                   execution(* com.learning.spring.aop.InventoryService.*(..))
 *                   @annotation(com.learning.spring.aop.LogExecutionTime)
 *                   within(com.learning.spring..*)   bean(*Service)
 *    Advice     : WHAT runs and WHEN:
 *                   @Before, @AfterReturning, @AfterThrowing, @After (finally), @Around (wraps everything)
 *    Weaving    : linking aspects to targets. Spring does it at RUNTIME with PROXIES.
 *
 *  HOW SPRING AOP WORKS: PROXIES
 *    At startup a BeanPostProcessor (topic 01!) sees that InventoryService matches a
 *    pointcut and returns a PROXY instead of the real bean:
 *      - JDK dynamic proxy  : if the bean implements an interface (proxy implements it)
 *      - CGLIB subclass     : otherwise. Spring Boot defaults to CGLIB for everything
 *    Callers hold the proxy -> proxy runs the advice -> calls the real method.
 *
 *  ADVICE ORDER for one call (within one aspect):
 *    @Around (code before proceed) -> @Before -> METHOD -> @AfterReturning / @AfterThrowing
 *    -> @After -> @Around (code after proceed / finally)      <- @Around finishes LAST
 *  Across several aspects, use @Order(n) on the aspect class: lower n = outer wrapper.
 *  (This is why @Transactional and a custom @Retry aspect must be ordered carefully:
 *   retry must wrap the transaction, not the other way round.)
 *
 *  LIMITATIONS (same as @Transactional, because @Transactional IS an aspect)
 *    - SELF-INVOCATION isn't intercepted (this.method() skips the proxy)
 *    - only public (non-final) methods of Spring BEANS
 *    - method execution only (full AspectJ with compile/load-time weaving can do fields, constructors...)
 *
 *  Built-in aspects you already use: @Transactional, @Cacheable, @Async,
 *  @PreAuthorize, @Retryable, Micrometer's @Timed/@Observed.
 */
@Aspect
@Component
public class PerformanceAspect {

    // Visible for tests/demo: what the aspect observed
    public static final List<String> RECORDED = new CopyOnWriteArrayList<>();

    // A NAMED, reusable pointcut: every method of InventoryService
    @Pointcut("execution(* com.learning.spring.aop.InventoryService.*(..))")
    void inventoryMethods() {}

    @Before("inventoryMethods()")
    public void logCall(JoinPoint jp) {
        RECORDED.add("BEFORE " + jp.getSignature().getName() + Arrays.toString(jp.getArgs()));
    }

    // @Around: full control. You decide IF and WHEN the real method runs (pjp.proceed()),
    // and you can change arguments or the return value. Used for timing, retries, caching.
    @Around("@annotation(com.learning.spring.aop.LogExecutionTime)")
    public Object time(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();                 // call the real method (or the next advice)
        } finally {
            long micros = (System.nanoTime() - start) / 1_000;
            RECORDED.add("TIMED " + pjp.getSignature().getName() + " took " + micros + "us");
        }
    }

    @AfterReturning(pointcut = "inventoryMethods()", returning = "result")
    public void logResult(JoinPoint jp, Object result) {
        RECORDED.add("RETURNED " + jp.getSignature().getName() + " -> " + result);
    }

    @AfterThrowing(pointcut = "inventoryMethods()", throwing = "ex")
    public void logFailure(JoinPoint jp, Exception ex) {
        RECORDED.add("THREW " + jp.getSignature().getName() + " -> " + ex.getMessage());   // e.g. send to alerting
    }
}
