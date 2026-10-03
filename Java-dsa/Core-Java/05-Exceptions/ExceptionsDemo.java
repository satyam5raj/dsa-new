/*
 * ============================================================================
 *  TOPIC 5 : EXCEPTION HANDLING
 * ============================================================================
 *
 *  Run:  java ExceptionsDemo.java
 *
 *  An EXCEPTION is an event that disrupts normal flow. Java represents it as
 *  an object that is "thrown" up the call stack until something "catches" it.
 *  If nothing catches it, the thread dies and the stack trace is printed.
 *
 *  HIERARCHY
 *  ---------
 *                        Throwable
 *                  _________|__________
 *                 |                    |
 *               Error               Exception
 *         (don't catch these)    _______|__________________
 *         OutOfMemoryError      |                          |
 *         StackOverflowError  RuntimeException       IOException, SQLException,
 *                             (UNCHECKED)            InterruptedException ...
 *                             NullPointerException   (CHECKED)
 *                             IllegalArgumentException
 *                             ArithmeticException
 *                             IndexOutOfBoundsException
 *                             ClassCastException
 *
 *  CHECKED vs UNCHECKED
 *  --------------------
 *   CHECKED   (Exception but not RuntimeException)
 *     - Compiler FORCES you to handle (catch) or declare (throws).
 *     - For recoverable, EXTERNAL problems: file missing, network down.
 *   UNCHECKED (RuntimeException and its subclasses, plus Errors)
 *     - Compiler doesn't force anything.
 *     - Usually PROGRAMMING BUGS: null access, bad index, bad argument.
 *
 *  KEYWORDS
 *    try      - code that might throw
 *    catch    - handles a specific exception type
 *    finally  - ALWAYS runs (cleanup), whether or not an exception occurred
 *    throw    - actually throw an exception object:   throw new X("msg");
 *    throws   - in a method signature, declares what it might throw
 */

import java.io.*;
import java.util.*;

public class ExceptionsDemo {

    public static void main(String[] args) {
        basicTryCatch();
        multipleCatch();
        finallyBlock();
        checkedVsUnchecked();
        customExceptions();
        tryWithResources();
        exceptionChaining();
        bestPractices();
    }

    // =================================================================
    // 1. Basic try / catch
    // =================================================================
    static void basicTryCatch() {
        System.out.println("===== 1. TRY / CATCH =====");
        try {
            int result = 10 / 0;                       // throws ArithmeticException
            System.out.println("never printed " + result);
        } catch (ArithmeticException e) {
            System.out.println("caught: " + e);                  // class + message
            System.out.println("message: " + e.getMessage());
        }
        System.out.println("program continues normally after the catch");
    }

    // =================================================================
    // 2. Multiple catch blocks
    //    Order matters: SPECIFIC first, GENERAL last. A parent type before
    //    its child is a compile error (the child block would be unreachable).
    // =================================================================
    static void multipleCatch() {
        System.out.println("\n===== 2. MULTIPLE CATCH =====");
        String[] inputs = {"42", "abc", null};
        for (String in : inputs) {
            try {
                int n = Integer.parseInt(in.trim());
                System.out.println("parsed " + n);
            } catch (NumberFormatException e) {
                System.out.println("not a number: " + in);
            } catch (NullPointerException e) {
                System.out.println("input was null");
            } catch (Exception e) {                      // general fallback LAST
                System.out.println("something else: " + e);
            }
        }

        // Multi-catch (Java 7): one block for unrelated types, joined with |
        try {
            Object o = "text";
            Integer i = (Integer) o;
            System.out.println(i);
        } catch (ClassCastException | IllegalStateException e) {
            System.out.println("multi-catch handled: " + e.getClass().getSimpleName());
        }
    }

    // =================================================================
    // 3. finally - runs no matter what (even if try has a return!)
    //    Only skipped if the JVM exits (System.exit) or the thread is killed.
    // =================================================================
    static void finallyBlock() {
        System.out.println("\n===== 3. FINALLY =====");
        System.out.println("returned: " + methodWithFinally());
        System.out.println("tricky returned: " + trickyFinally());
    }

    static String methodWithFinally() {
        try {
            System.out.println("in try, about to return");
            return "from try";
        } finally {
            System.out.println("finally still runs before the method returns");
        }
    }

    // INTERVIEW TRAP: a return in finally OVERRIDES the try's return
    // (and silently swallows any exception). Never return from finally.
    @SuppressWarnings("finally")
    static int trickyFinally() {
        try {
            return 1;
        } finally {
            return 2;   // wins
        }
    }

    // =================================================================
    // 4. Checked vs Unchecked in practice
    // =================================================================
    static void checkedVsUnchecked() {
        System.out.println("\n===== 4. CHECKED vs UNCHECKED =====");

        // Checked: must catch OR declare 'throws' - this won't compile without the try
        try {
            readFirstLine("does-not-exist.txt");
        } catch (IOException e) {
            System.out.println("checked IOException handled: " + e.getClass().getSimpleName());
        }

        // Unchecked: compiler doesn't care, but it still crashes if unhandled
        try {
            validateAge(-5);
        } catch (IllegalArgumentException e) {
            System.out.println("unchecked: " + e.getMessage());
        }
    }

    // 'throws IOException' pushes the responsibility to the caller
    static String readFirstLine(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        }
    }

    static void validateAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("age cannot be negative: " + age);  // 'throw'
        }
    }

    // =================================================================
    // 5. Custom exceptions
    //    - extend Exception         -> checked
    //    - extend RuntimeException  -> unchecked (the modern default choice)
    //    Add useful fields (e.g. error code, amount) to help the caller.
    // =================================================================
    static class InsufficientFundsException extends Exception {   // checked
        private final double shortBy;

        InsufficientFundsException(String message, double shortBy) {
            super(message);              // pass message to Throwable
            this.shortBy = shortBy;
        }

        double getShortBy() { return shortBy; }
    }

    static class AccountNotFoundException extends RuntimeException { // unchecked
        AccountNotFoundException(String id) {
            super("Account not found: " + id);
        }
    }

    static class BankAccount {
        private final String id;
        private double balance;

        BankAccount(String id, double balance) { this.id = id; this.balance = balance; }

        void withdraw(double amount) throws InsufficientFundsException {
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
            if (amount > balance) {
                throw new InsufficientFundsException("Not enough money in " + id, amount - balance);
            }
            balance -= amount;
        }
    }

    static void customExceptions() {
        System.out.println("\n===== 5. CUSTOM EXCEPTIONS =====");
        Map<String, BankAccount> accounts = Map.of("A1", new BankAccount("A1", 100));
        try {
            BankAccount acc = Optional.ofNullable(accounts.get("A1"))
                    .orElseThrow(() -> new AccountNotFoundException("A1"));
            acc.withdraw(150);
        } catch (InsufficientFundsException e) {
            System.out.println(e.getMessage() + " (short by " + e.getShortBy() + ")");
        }

        try {
            if (!accounts.containsKey("ZZ")) throw new AccountNotFoundException("ZZ");
        } catch (AccountNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    // =================================================================
    // 6. try-with-resources (Java 7)
    //    Any object implementing AutoCloseable declared in try(...) is
    //    closed AUTOMATICALLY at the end, in REVERSE order of creation,
    //    even if an exception is thrown. Replaces messy finally { close() }.
    //
    //    SUPPRESSED EXCEPTIONS: if the body throws AND close() throws, the
    //    body's exception is the main one; close()'s is attached as
    //    "suppressed" (getSuppressed()) instead of being lost.
    // =================================================================
    static class Resource implements AutoCloseable {
        private final String name;
        private final boolean failOnClose;

        Resource(String name, boolean failOnClose) {
            this.name = name;
            this.failOnClose = failOnClose;
            System.out.println("   open  " + name);
        }

        @Override
        public void close() {
            System.out.println("   close " + name);
            if (failOnClose) throw new IllegalStateException("close failed: " + name);
        }
    }

    static void tryWithResources() {
        System.out.println("\n===== 6. TRY-WITH-RESOURCES =====");
        try (Resource db = new Resource("db", false);
             Resource file = new Resource("file", false)) {
            System.out.println("   using both resources");
        } // file closed first, then db

        System.out.println("-- with failures --");
        try (Resource r = new Resource("conn", true)) {
            throw new RuntimeException("body failed");
        } catch (RuntimeException e) {
            System.out.println("   main: " + e.getMessage());
            for (Throwable s : e.getSuppressed()) {
                System.out.println("   suppressed: " + s.getMessage());
            }
        }
    }

    // =================================================================
    // 7. Exception chaining - wrap a low-level exception in a higher-level
    //    one WITHOUT losing the original cause (keeps the full stack trace).
    // =================================================================
    static class DataAccessException extends RuntimeException {
        DataAccessException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    static void loadUser() {
        try {
            throw new IOException("socket timeout");      // pretend the DB call failed
        } catch (IOException e) {
            throw new DataAccessException("could not load user", e);  // keep 'e' as the cause!
        }
    }

    static void exceptionChaining() {
        System.out.println("\n===== 7. CHAINING =====");
        try {
            loadUser();
        } catch (DataAccessException e) {
            System.out.println("high level: " + e.getMessage());
            System.out.println("root cause: " + e.getCause().getMessage());
        }
    }

    // =================================================================
    // 8. Best practices (say these in interviews)
    // =================================================================
    static void bestPractices() {
        System.out.println("\n===== 8. BEST PRACTICES =====");
        System.out.println("""
                1. Catch the MOST SPECIFIC exception you can handle.
                2. Never swallow: catch (Exception e) {}  hides bugs. At least log it.
                3. Don't use exceptions for normal control flow (they're slow: stack trace capture).
                4. Fail fast: validate arguments early, throw IllegalArgumentException.
                5. Always wrap with the cause: new XException("msg", e).
                6. Use try-with-resources for anything closeable.
                7. Prefer unchecked custom exceptions for business errors in modern code
                   (Spring does this); use checked when the caller can truly recover.
                8. Don't catch Throwable/Error (OutOfMemoryError can't be meaningfully handled).
                9. Never return from finally.
                10. Overriding methods can't throw broader CHECKED exceptions than the parent.""");
    }
}
