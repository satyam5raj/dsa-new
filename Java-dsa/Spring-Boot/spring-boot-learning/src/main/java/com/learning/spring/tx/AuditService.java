package com.learning.spring.tx;

import com.learning.spring.tx.BankEntities.AuditEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/*
 * ============================================================================
 *  TOPIC 05 : TRANSACTION PROPAGATION
 * ============================================================================
 *
 *  Propagation = what happens when a @Transactional method is called while a
 *  transaction is ALREADY running.
 *
 *  ---------------------------------------------------------------------------------
 *   Propagation     Existing tx?  ->  behaviour
 *  ---------------------------------------------------------------------------------
 *   REQUIRED (def.) yes -> JOIN it       no -> start a new one
 *                   (if the inner method fails, the WHOLE tx is marked rollback-only)
 *   REQUIRES_NEW    yes -> SUSPEND it and start an INDEPENDENT tx; it commits or
 *                          rolls back on its own. Uses a SECOND DB connection!
 *   NESTED          yes -> SAVEPOINT inside the same tx (partial rollback);
 *                          JDBC DataSourceTransactionManager only
 *   SUPPORTS        yes -> join          no -> run without a tx
 *   NOT_SUPPORTED   yes -> suspend it, run without a tx
 *   MANDATORY       yes -> join          no -> IllegalTransactionStateException
 *   NEVER           yes -> exception     no -> run without a tx
 *  ---------------------------------------------------------------------------------
 *
 *  CLASSIC USE OF REQUIRES_NEW: audit/attempt logs that must survive even when
 *  the business transaction rolls back ("a transfer of 500 was ATTEMPTED and FAILED").
 *  Beware: REQUIRES_NEW under load can exhaust the connection pool (each caller holds 2).
 */
@Service
public class AuditService {

    private final AuditRepository audits;

    public AuditService(AuditRepository audits) {
        this.audits = audits;
    }

    // Independent transaction: commits even if the caller's transaction rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(String message) {
        audits.save(new AuditEntry(message));
    }

    // Joins the caller's transaction: rolled back together with it
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordInCallerTransaction(String message) {
        audits.save(new AuditEntry(message));
    }

    // Must be called inside an existing transaction, otherwise it fails
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordMandatory(String message) {
        audits.save(new AuditEntry(message));
    }
}
