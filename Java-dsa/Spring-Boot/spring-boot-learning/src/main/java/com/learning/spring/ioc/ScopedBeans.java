package com.learning.spring.ioc;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * ============================================================================
 *  TOPIC 01 : BEAN SCOPES
 * ============================================================================
 *
 *  ---------------------------------------------------------------------------
 *   Scope        Instances                         Typical use
 *  ---------------------------------------------------------------------------
 *   singleton    ONE per container (DEFAULT)       stateless services, repositories
 *   prototype    a NEW one every time it's         stateful helpers, builders
 *                requested from the container
 *   request      one per HTTP request (web only)   per-request context (request id, user info)
 *   session      one per HTTP session              shopping cart in classic web apps
 *   application  one per ServletContext            rarely used
 *  ---------------------------------------------------------------------------
 *
 *  SINGLETON != the Singleton design pattern. A Spring singleton is "one per
 *  container", and you can still call 'new' yourself. Singletons are shared
 *  by ALL threads/requests -> they MUST be stateless or thread-safe.
 *
 *  THE CLASSIC TRAP: injecting a PROTOTYPE into a SINGLETON
 *    The singleton is created ONCE, so its dependencies are injected ONCE.
 *    It holds the SAME prototype instance forever: no "new per use".
 *    FIXES:
 *      - ObjectProvider<T>.getObject()   (shown below; lazy lookup each time)
 *      - @Lookup method injection
 *      - a scoped proxy: @Scope(value = "prototype", proxyMode = TARGET_CLASS)
 *
 *  REQUEST scope inside a singleton controller works because Spring injects a
 *  PROXY. Each call on the proxy is routed to the bean of the CURRENT request.
 *  (@RequestScope already sets proxyMode = TARGET_CLASS for you.)
 */
public final class ScopedBeans {

    private ScopedBeans() {}

    // ---------- singleton (default) ----------
    @Component
    public static class VisitCounter {
        private final AtomicInteger visits = new AtomicInteger();   // shared state -> must be thread-safe

        public int increment() { return visits.incrementAndGet(); }
    }

    // ---------- prototype ----------
    @Component
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public static class ReportBuilder {
        private static final AtomicInteger CREATED = new AtomicInteger();
        private final int id = CREATED.incrementAndGet();
        private final StringBuilder lines = new StringBuilder();   // per-instance state: fine for a prototype

        public ReportBuilder line(String text) { lines.append(text).append('\n'); return this; }
        public String build() { return "report#" + id + "\n" + lines; }
        public int id() { return id; }
    }

    // ---------- singleton that needs prototypes ----------
    @Service
    public static class ReportService {
        private final ReportBuilder injectedOnce;                 // TRAP: the same instance forever
        private final ObjectProvider<ReportBuilder> provider;     // FIX: ask the container each time

        public ReportService(ReportBuilder injectedOnce, ObjectProvider<ReportBuilder> provider) {
            this.injectedOnce = injectedOnce;
            this.provider = provider;
        }

        public int builderIdUsingInjectedField() { return injectedOnce.id(); }

        public int builderIdUsingProvider() { return provider.getObject().id(); }   // new prototype every call
    }

    // ---------- request scope ----------
    @Component
    @RequestScope
    public static class RequestContext {
        private final String requestId = UUID.randomUUID().toString().substring(0, 8);

        public String requestId() { return requestId; }
    }
}
