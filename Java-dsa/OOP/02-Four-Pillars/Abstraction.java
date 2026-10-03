/*
 * ============================================================================
 *  PILLAR 2 : ABSTRACTION
 * ============================================================================
 *
 *  Run:  java Abstraction.java
 *
 *  DEFINITION
 *    Showing WHAT an object does and hiding HOW it does it. The caller works
 *    with a simple concept ("send a notification", "charge a payment") and
 *    never sees the complex details (SMTP, retries, API keys, HTTP...).
 *
 *  ENCAPSULATION vs ABSTRACTION (very common interview question)
 *    Encapsulation = HIDING DATA. Protects state inside one class (private fields).
 *    Abstraction   = HIDING COMPLEXITY. Defines a simple contract and hides
 *                    the implementation (interfaces, abstract classes,
 *                    well-named methods).
 *    Encapsulation is about the object's internals. Abstraction is about
 *    the view the outside world gets.
 *    Example: a car's steering wheel is an ABSTRACTION (you turn it, you
 *    don't think about the rack and pinion). The engine bay being bolted
 *    shut is ENCAPSULATION (you can't touch the internals directly).
 *
 *  TOOLS IN JAVA
 *    - interface      : a pure contract ("can do X")
 *    - abstract class : a partial implementation plus a contract
 *    - Even a plain method is an abstraction (sort() hides the algorithm).
 *
 *  WHY
 *    1. Callers depend on a simple, STABLE contract, not on volatile details.
 *    2. You can swap implementations (Stripe -> Razorpay) without touching callers.
 *    3. Easier testing: pass a fake implementation of the interface.
 *    4. Reduces cognitive load: think in concepts, not mechanics.
 */

import java.util.*;

public class Abstraction {

    public static void main(String[] args) {
        interfaceAbstraction();
        abstractClassAbstraction();
        methodLevelAbstraction();
    }

    // =================================================================
    // 1. Interface as an abstraction
    //    The checkout code knows only "PaymentGateway.charge()".
    //    It has no idea about API calls, signatures, or retries.
    // =================================================================
    interface PaymentGateway {
        /** Charges the amount and returns a transaction id. */
        String charge(String customerId, double amount);
    }

    static class StripeGateway implements PaymentGateway {
        @Override
        public String charge(String customerId, double amount) {
            // Imagine: build HTTP request, sign with secret key, handle retries...
            authenticate();
            return "stripe_txn_" + Math.abs((customerId + amount).hashCode());
        }

        private void authenticate() { /* hidden complexity */ }
    }

    static class RazorpayGateway implements PaymentGateway {
        @Override
        public String charge(String customerId, double amount) {
            // Completely different internals, SAME contract
            long paise = Math.round(amount * 100);        // Razorpay works in paise
            return "rzp_" + customerId + "_" + paise;
        }
    }

    // The high-level code depends ONLY on the abstraction
    static class CheckoutService {
        private final PaymentGateway gateway;

        CheckoutService(PaymentGateway gateway) { this.gateway = gateway; }

        String checkout(String customerId, List<Double> cart) {
            double total = cart.stream().mapToDouble(Double::doubleValue).sum();
            return gateway.charge(customerId, total);
        }
    }

    static void interfaceAbstraction() {
        System.out.println("===== 1. INTERFACE ABSTRACTION =====");
        List<Double> cart = List.of(499.0, 1299.5);
        System.out.println("Stripe   -> " + new CheckoutService(new StripeGateway()).checkout("cust42", cart));
        System.out.println("Razorpay -> " + new CheckoutService(new RazorpayGateway()).checkout("cust42", cart));

        // Testing: a fake gateway, no real money moved
        PaymentGateway fake = (customer, amount) -> "FAKE-" + amount;
        System.out.println("Fake     -> " + new CheckoutService(fake).checkout("test", cart));
    }

    // =================================================================
    // 2. Abstract class as an abstraction
    //    The abstract class defines the WHAT (send) and some shared HOW
    //    (validation, formatting). Subclasses fill in the channel-specific part.
    //    - Can't be instantiated:  new Notifier()  -> compile error
    //    - Can have constructors, fields, concrete methods, abstract methods
    // =================================================================
    static abstract class Notifier {
        private final String sender;

        protected Notifier(String sender) { this.sender = sender; }

        // Public, simple API. Callers only ever call this.
        public final void send(String to, String message) {
            if (to == null || to.isBlank()) throw new IllegalArgumentException("recipient required");
            String formatted = "[" + sender + "] " + message.trim();
            deliver(to, formatted);                    // the varying part
        }

        // Each channel implements just the hard part
        protected abstract void deliver(String to, String formattedMessage);
    }

    static class EmailNotifier extends Notifier {
        EmailNotifier() { super("noreply@shop.com"); }

        @Override
        protected void deliver(String to, String msg) {
            System.out.println("   EMAIL to " + to + ": " + msg + "  (via SMTP, TLS, headers...)");
        }
    }

    static class SmsNotifier extends Notifier {
        SmsNotifier() { super("SHOPIN"); }

        @Override
        protected void deliver(String to, String msg) {
            String truncated = msg.length() > 40 ? msg.substring(0, 37) + "..." : msg;  // SMS limits
            System.out.println("   SMS to " + to + ": " + truncated);
        }
    }

    static void abstractClassAbstraction() {
        System.out.println("\n===== 2. ABSTRACT CLASS ABSTRACTION =====");
        List<Notifier> channels = List.of(new EmailNotifier(), new SmsNotifier());
        for (Notifier n : channels) {
            n.send("user-1", "  Your order #123 has been shipped and will arrive tomorrow  ");
        }
    }

    // =================================================================
    // 3. Abstraction at the method level
    //    Good method names hide steps. The reader sees the story, not the details.
    // =================================================================
    static void methodLevelAbstraction() {
        System.out.println("\n===== 3. METHOD-LEVEL ABSTRACTION =====");
        String raw = "  Hello,   WORLD!! Java   is FUN  ";
        System.out.println("slug = " + toSlug(raw));
    }

    // High level: reads like a sentence
    static String toSlug(String text) {
        String cleaned = removePunctuation(text);
        String normalized = collapseWhitespace(cleaned).toLowerCase();
        return normalized.replace(' ', '-');
    }

    // Low-level details hidden in small helpers
    private static String removePunctuation(String s) { return s.replaceAll("[^A-Za-z0-9 ]", ""); }
    private static String collapseWhitespace(String s) { return s.trim().replaceAll("\\s+", " "); }
}
