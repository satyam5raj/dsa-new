package com.learning.spring.tx;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/*
 * Entities for the transactions topic (repositories are in AccountRepository / AuditRepository:
 * Spring Data does NOT detect repository interfaces NESTED inside other classes by default).
 * Money is stored as a long number of paise (minor units) to avoid floating-point errors.
 */
public final class BankEntities {

    private BankEntities() {}

    @Entity
    @Table(name = "accounts")
    public static class Account {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String owner;
        private long balance;
        private boolean frozen;

        // OPTIMISTIC LOCKING: Hibernate adds "where version = ?" to every UPDATE and
        // increments it. If another transaction changed the row in between, 0 rows are
        // updated -> OptimisticLockException instead of a silent LOST UPDATE.
        @Version
        private long version;

        protected Account() {}

        public Account(String owner, long balance) {
            this.owner = owner;
            this.balance = balance;
        }

        public void withdraw(long amount) {
            if (amount > balance) throw new BankService.InsufficientFundsException(owner, balance, amount);
            balance -= amount;
        }

        public void deposit(long amount) { balance += amount; }

        public Long getId() { return id; }
        public String getOwner() { return owner; }
        public long getBalance() { return balance; }
        public boolean isFrozen() { return frozen; }
        public void setFrozen(boolean frozen) { this.frozen = frozen; }
    }

    @Entity
    @Table(name = "audit_entries")
    public static class AuditEntry {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String message;
        private Instant createdAt = Instant.now();

        protected AuditEntry() {}

        public AuditEntry(String message) { this.message = message; }

        public String getMessage() { return message; }
    }
}
