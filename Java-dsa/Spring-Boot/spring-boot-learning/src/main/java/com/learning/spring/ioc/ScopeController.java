package com.learning.spring.ioc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
 * A SINGLETON controller that uses a REQUEST-scoped bean.
 * 'requestContext' is actually a proxy. Each call is routed to the
 * RequestContext of the current HTTP request.
 *
 * Try:  curl localhost:8080/api/scopes   (twice: the requestId changes, the visit count grows)
 */
@RestController
public class ScopeController {

    private final ScopedBeans.RequestContext requestContext;   // proxy
    private final ScopedBeans.VisitCounter counter;            // real singleton
    private final ScopedBeans.ReportService reportService;

    // CONSTRUCTOR INJECTION (the recommended style):
    //  - dependencies are explicit and can be 'final' (immutable, thread-safe)
    //  - the object can never exist half-initialized
    //  - easy to unit test: just call 'new ScopeController(mockA, mockB, mockC)'
    //  - with a single constructor, @Autowired is optional
    public ScopeController(ScopedBeans.RequestContext requestContext,
                           ScopedBeans.VisitCounter counter,
                           ScopedBeans.ReportService reportService) {
        this.requestContext = requestContext;
        this.counter = counter;
        this.reportService = reportService;
    }

    @GetMapping("/api/scopes")
    public Map<String, Object> scopes() {
        return Map.of(
                "requestId (request scope)", requestContext.requestId(),
                "requestId again, same request", requestContext.requestId(),
                "visits (singleton)", counter.increment(),
                "prototype id via injected field", reportService.builderIdUsingInjectedField(),
                "prototype id via ObjectProvider", reportService.builderIdUsingProvider(),
                "injected RequestContext class (a proxy!)", requestContext.getClass().getSimpleName());
    }
}
