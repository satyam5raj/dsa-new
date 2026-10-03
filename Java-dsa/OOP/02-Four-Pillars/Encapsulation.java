/*
 * ============================================================================
 *  PILLAR 1 : ENCAPSULATION
 * ============================================================================
 *
 *  Run:  java Encapsulation.java
 *
 *  DEFINITION
 *    Bundling data (fields) with the methods that operate on it, and HIDING
 *    the internal state so it can only change through a controlled API.
 *
 *  HOW (mechanics)
 *    - Fields private
 *    - Expose behaviour through public methods that ENFORCE RULES (invariants)
 *    - Don't leak internal mutable objects (return copies or unmodifiable views)
 *
 *  WHY (this is what interviewers actually want to hear)
 *    1. PROTECT INVARIANTS: a BankAccount balance can never go negative,
 *       because the ONLY way to change it is withdraw(), which checks.
 *    2. FREEDOM TO CHANGE INTERNALS: callers depend on the methods, not the
 *       fields. You can switch double -> BigDecimal or ArrayList -> HashMap
 *       without breaking any caller.
 *    3. EASIER DEBUGGING: a bug in 'balance' can only come from a few methods.
 *    4. THREAD SAFETY: if all access goes through methods, you can add
 *       synchronization in one place.
 *
 *  COMMON MISCONCEPTION
 *    "Encapsulation = private fields + getters/setters for everything."
 *    NO. A public setter for every field is just a public field with extra
 *    steps. Expose INTENT-REVEALING behaviour (deposit, withdraw) instead
 *    of raw setters (setBalance). "Tell, don't ask."
 */

import java.util.*;

public class Encapsulation {

    public static void main(String[] args) {
        badDesign();
        goodDesign();
        leakingInternals();
    }

    // =================================================================
    // BAD: public fields - anyone can put the object into an invalid state
    // =================================================================
    static class BadAccount {
        public String owner;
        public double balance;          // nothing stops balance = -1_000_000
    }

    static void badDesign() {
        System.out.println("===== BAD: NO ENCAPSULATION =====");
        BadAccount acc = new BadAccount();
        acc.owner = "Ravi";
        acc.balance = -1_000_000;       // invalid state, no one checked
        System.out.println(acc.owner + " balance = " + acc.balance + "  <- corrupted, and we can't tell who did it");
    }

    // =================================================================
    // GOOD: private state, behaviour-based API, invariants enforced
    // =================================================================
    static class BankAccount {
        private final String accountNumber;     // can't change after creation
        private final String owner;
        private double balance;                  // only changed via deposit/withdraw
        private final List<String> transactions = new ArrayList<>();

        BankAccount(String accountNumber, String owner, double openingBalance) {
            if (openingBalance < 0) throw new IllegalArgumentException("opening balance < 0");
            this.accountNumber = accountNumber;
            this.owner = owner;
            this.balance = openingBalance;
            log("OPEN " + openingBalance);
        }

        // Intent-revealing methods, not setBalance()
        public void deposit(double amount) {
            requirePositive(amount);
            balance += amount;
            log("DEPOSIT " + amount);
        }

        public void withdraw(double amount) {
            requirePositive(amount);
            if (amount > balance) {
                throw new IllegalStateException("Insufficient funds: balance " + balance + ", asked " + amount);
            }
            balance -= amount;
            log("WITHDRAW " + amount);
        }

        public void transferTo(BankAccount other, double amount) {
            this.withdraw(amount);              // reuses the same rules
            other.deposit(amount);
        }

        // Read-only access. No setter for balance at all.
        public double getBalance() { return balance; }
        public String getOwner() { return owner; }

        // Don't return the internal list - return an unmodifiable view
        public List<String> getTransactions() {
            return Collections.unmodifiableList(transactions);
        }

        // Private helpers - internal details, invisible to callers
        private void requirePositive(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        }

        private void log(String entry) {
            transactions.add(entry);
        }

        @Override
        public String toString() {
            // Mask sensitive data even in toString
            return owner + " [****" + accountNumber.substring(accountNumber.length() - 4) + "] balance=" + balance;
        }
    }

    static void goodDesign() {
        System.out.println("\n===== GOOD: ENCAPSULATED =====");
        BankAccount ravi = new BankAccount("1234567890", "Ravi", 1000);
        BankAccount asha = new BankAccount("9876543210", "Asha", 500);

        ravi.deposit(200);
        ravi.transferTo(asha, 700);
        System.out.println(ravi);
        System.out.println(asha);

        try {
            ravi.withdraw(10_000);
        } catch (IllegalStateException e) {
            System.out.println("blocked: " + e.getMessage());
        }
        try {
            ravi.getTransactions().add("HACKED 1000000");
        } catch (UnsupportedOperationException e) {
            System.out.println("blocked: can't modify the transaction log from outside");
        }
        System.out.println("ravi's log = " + ravi.getTransactions());
    }

    // =================================================================
    // SUBTLE LEAK: private field, but the getter hands out the mutable object
    // =================================================================
    static class LeakyTeam {
        private final List<String> members = new ArrayList<>();
        private final Date createdAt = new Date();               // Date is mutable!

        void addMember(String name) {
            if (members.size() >= 3) throw new IllegalStateException("team full (max 3)");
            members.add(name);
        }

        List<String> getMembers() { return members; }            // LEAK: caller gets the real list
        Date getCreatedAt() { return createdAt; }                // LEAK: caller can call setTime()
    }

    static class SafeTeam {
        private final List<String> members = new ArrayList<>();
        private final Date createdAt = new Date();

        void addMember(String name) {
            if (members.size() >= 3) throw new IllegalStateException("team full (max 3)");
            members.add(name);
        }

        List<String> getMembers() { return List.copyOf(members); }     // defensive copy
        Date getCreatedAt() { return new Date(createdAt.getTime()); }  // defensive copy
        // (Even better: use the immutable java.time.Instant instead of Date.)
    }

    static void leakingInternals() {
        System.out.println("\n===== LEAKING INTERNALS =====");
        LeakyTeam leaky = new LeakyTeam();
        leaky.addMember("A");
        for (int i = 0; i < 10; i++) leaky.getMembers().add("intruder" + i);   // bypasses the max-3 rule!
        System.out.println("leaky team size = " + leaky.getMembers().size() + " (rule broken)");

        SafeTeam safe = new SafeTeam();
        safe.addMember("A");
        try {
            safe.getMembers().add("intruder");
        } catch (UnsupportedOperationException e) {
            System.out.println("safe team: the returned copy is unmodifiable, size stays " + safe.getMembers().size());
        }
        safe.getCreatedAt().setTime(0);   // only changes the copy
        System.out.println("safe team date not affected: " + (safe.getCreatedAt().getTime() != 0));
    }
}
