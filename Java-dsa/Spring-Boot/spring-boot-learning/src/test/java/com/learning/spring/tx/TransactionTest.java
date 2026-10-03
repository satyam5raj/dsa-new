package com.learning.spring.tx;

import com.learning.spring.tx.BankEntities.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * TOPIC 05 proofs: commit, rollback, propagation, rollback rules, self-invocation.
 * No @Transactional on the test class: each service call commits/rolls back
 * exactly like in production, and we read the results back afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionTest {

    @Autowired BankService bank;
    @Autowired AuditService auditService;
    @Autowired AccountRepository accounts;
    @Autowired AuditRepository audits;

    @BeforeEach
    void setUp() {
        audits.deleteAll();
        accounts.deleteAll();
        accounts.save(new Account("ravi", 1_000));
        accounts.save(new Account("asha", 500));
        Account frozen = new Account("fred", 0);
        frozen.setFrozen(true);
        accounts.save(frozen);
    }

    @Test
    void successfulTransfer_commitsBothSides() {
        bank.transfer("ravi", "asha", 300);
        assertThat(balance("ravi")).isEqualTo(700);
        assertThat(balance("asha")).isEqualTo(800);
        assertThat(auditMessages()).containsExactly("ATTEMPT ravi -> asha : 300", "SUCCESS ravi -> asha : 300");
    }

    @Test
    void runtimeException_rollsBackEverything_butRequiresNewAuditSurvives() {
        // The debit was already flushed to the DB when the frozen check failed...
        assertThatThrownBy(() -> bank.transfer("ravi", "fred", 300))
                .isInstanceOf(BankService.AccountFrozenException.class);
        // ...but the whole transaction rolled back:
        assertThat(balance("ravi")).isEqualTo(1_000);
        // REQUIRES_NEW audit committed independently. The REQUIRED "SUCCESS" entry never happened.
        assertThat(auditMessages()).containsExactly("ATTEMPT ravi -> fred : 300");
    }

    @Test
    void insufficientFunds_rollsBack() {
        assertThatThrownBy(() -> bank.transfer("asha", "ravi", 9_999))
                .isInstanceOf(BankService.InsufficientFundsException.class);
        assertThat(balance("asha")).isEqualTo(500);
    }

    @Test
    void checkedException_COMMITSByDefault_partialWorkSurvives() {
        accounts.save(new Account("rich", 50_000));
        assertThatThrownBy(() -> bank.transferWithLimitCheck("rich", "asha", 20_000))
                .isInstanceOf(BankService.TransferLimitException.class);
        // The debit was COMMITTED even though the method threw: 20,000 disappeared!
        assertThat(balance("rich")).isEqualTo(30_000);
        assertThat(balance("asha")).isEqualTo(500);
    }

    @Test
    void checkedException_withRollbackFor_rollsBack() {
        accounts.save(new Account("rich", 50_000));
        assertThatThrownBy(() -> bank.transferWithLimitCheckSafe("rich", "asha", 20_000))
                .isInstanceOf(BankService.TransferLimitException.class);
        assertThat(balance("rich")).isEqualTo(50_000);              // rolled back
    }

    @Test
    void selfInvocation_bypassesTheProxy_noTransaction_partialUpdateSurvives() {
        assertThatThrownBy(() -> bank.transferViaSelfInvocation("ravi", "fred", 300))
                .isInstanceOf(BankService.AccountFrozenException.class);
        // Same failure as before, but this time the debit is NOT rolled back,
        // because transfer() ran without a transaction (this.transfer() skipped the proxy).
        assertThat(balance("ravi")).isEqualTo(700);
    }

    @Test
    void mandatoryPropagation_failsWithoutAnExistingTransaction() {
        assertThatThrownBy(() -> auditService.recordMandatory("x"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private long balance(String owner) {
        return bank.balanceOf(owner);
    }

    private java.util.List<String> auditMessages() {
        return audits.findAllByOrderByIdAsc().stream().map(BankEntities.AuditEntry::getMessage).toList();
    }

}
