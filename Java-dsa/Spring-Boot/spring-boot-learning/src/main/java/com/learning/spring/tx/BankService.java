package com.learning.spring.tx;

import com.learning.spring.tx.BankEntities.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/*
 * ============================================================================
 *  TOPIC 05 : @Transactional - HOW IT WORKS, ROLLBACK RULES, THE SELF-INVOCATION TRAP
 * ============================================================================
 *
 *  HOW: Spring wraps this bean in a PROXY (AOP). Callers get the proxy, not the bean.
 *        caller -> [proxy: begin tx] -> real method -> [proxy: commit, or rollback on exception]
 *  One transaction = one DB connection + one persistence context. Every entity
 *  loaded inside is MANAGED: changes are auto-detected ("dirty checking") and
 *  flushed as UPDATEs at commit, even without calling save().
 *
 *  ROLLBACK RULES (the #1 surprise)
 *    - RuntimeException (unchecked) and Error -> ROLLBACK
 *    - CHECKED exceptions (extends Exception) -> COMMIT!  (historic EJB convention)
 *      Change it with  @Transactional(rollbackFor = MyCheckedException.class)
 *    - Catching the exception INSIDE the method -> the proxy never sees it -> COMMIT
 *
 *  WHY @Transactional SOMETIMES "DOES NOTHING"
 *    1. SELF-INVOCATION: this.transfer(...) from another method of the SAME class
 *       bypasses the proxy -> NO transaction. (Demonstrated below.)
 *       Fixes: call it via another bean, inject self lazily, or use TransactionTemplate.
 *    2. The method is private/final, or the class is final -> it can't be proxied.
 *    3. The object was created with 'new' instead of being injected by Spring.
 *
 *  ISOLATION LEVELS (which concurrency anomalies are prevented)
 *  ---------------------------------------------------------------------------
 *   Level             Dirty read   Non-repeatable read   Phantom read
 *   READ_UNCOMMITTED  possible     possible              possible
 *   READ_COMMITTED    prevented    possible              possible      (Postgres/Oracle default)
 *   REPEATABLE_READ   prevented    prevented             possible*     (MySQL InnoDB default)
 *   SERIALIZABLE      prevented    prevented             prevented     (slowest)
 *  ---------------------------------------------------------------------------
 *   dirty read          : you read another tx's UNCOMMITTED change
 *   non-repeatable read : reading the same row twice gives different values
 *   phantom read        : re-running a query returns NEW rows
 *   (*InnoDB also prevents most phantoms with gap locks / MVCC.)
 *   LOST UPDATES are prevented by locking: optimistic (@Version, see Account) or
 *   pessimistic (@Lock(PESSIMISTIC_WRITE) -> SELECT ... FOR UPDATE).
 *
 *  Put @Transactional on SERVICE methods (the unit of work), not on controllers or repositories.
 */
@Service
public class BankService {

    private final AccountRepository accounts;
    private final AuditService audit;

    public BankService(AccountRepository accounts, AuditService audit) {
        this.accounts = accounts;
        this.audit = audit;
    }

    // ---------------------------------------------------------------
    // The happy path: all-or-nothing transfer
    // ---------------------------------------------------------------
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void transfer(String from, String to, long amount) {
        audit.recordIndependently("ATTEMPT " + from + " -> " + to + " : " + amount);  // REQUIRES_NEW: survives rollback
        Account source = find(from);
        Account target = find(to);

        source.withdraw(amount);                  // may throw InsufficientFundsException (runtime -> rollback)
        accounts.saveAndFlush(source);            // the debit is SENT to the DB now (still uncommitted)

        if (target.isFrozen()) {
            throw new AccountFrozenException(to); // runtime -> the debit above is ROLLED BACK
        }
        target.deposit(amount);                   // no save() needed: managed entity, dirty checking
        audit.recordInCallerTransaction("SUCCESS " + from + " -> " + to + " : " + amount); // REQUIRED: joins this tx
    }

    // ---------------------------------------------------------------
    // Checked exception: by default the transaction COMMITS anyway
    // ---------------------------------------------------------------
    @Transactional
    public void transferWithLimitCheck(String from, String to, long amount) throws TransferLimitException {
        Account source = find(from);
        source.withdraw(amount);                  // debit happens...
        if (amount > 10_000) {
            throw new TransferLimitException(amount);   // ...CHECKED exception -> COMMIT -> money vanishes!
        }
        find(to).deposit(amount);
    }

    // Same logic, but checked exceptions roll back too
    @Transactional(rollbackFor = TransferLimitException.class)
    public void transferWithLimitCheckSafe(String from, String to, long amount) throws TransferLimitException {
        Account source = find(from);
        source.withdraw(amount);
        if (amount > 10_000) throw new TransferLimitException(amount);   // -> ROLLBACK
        find(to).deposit(amount);
    }

    // ---------------------------------------------------------------
    // SELF-INVOCATION TRAP: no @Transactional here, and calling
    // this.transfer() skips the proxy, so transfer() runs WITHOUT a transaction.
    // Each repository call commits on its own -> partial updates survive a failure.
    // ---------------------------------------------------------------
    public void transferViaSelfInvocation(String from, String to, long amount) {
        this.transfer(from, to, amount);          // 'this' = the raw bean, not the proxy
    }

    @Transactional(readOnly = true)
    public long balanceOf(String owner) {
        return find(owner).getBalance();
    }

    private Account find(String owner) {
        return accounts.findByOwner(owner).orElseThrow(() -> new BankingException("No account for " + owner));
    }

    // ---------------- exceptions ----------------
    public static class BankingException extends RuntimeException {
        public BankingException(String msg) { super(msg); }
    }

    public static class InsufficientFundsException extends BankingException {
        public InsufficientFundsException(String owner, long balance, long amount) {
            super(owner + " has " + balance + ", cannot withdraw " + amount);
        }
    }

    public static class AccountFrozenException extends BankingException {
        public AccountFrozenException(String owner) { super("Account of " + owner + " is frozen"); }
    }

    /** CHECKED exception (extends Exception). */
    public static class TransferLimitException extends Exception {
        public TransferLimitException(long amount) { super("Transfer of " + amount + " exceeds the limit"); }
    }
}
