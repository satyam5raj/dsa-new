/*
 * ============================================================================
 *  PATTERN 7 : ADAPTER  (Structural)   aka Wrapper (interface translator)
 * ============================================================================
 *
 *  Run:  java P7_Adapter.java
 *
 *  INTENT : convert the interface of an existing class into ANOTHER interface
 *           that clients expect. It lets classes work together that couldn't
 *           otherwise, because their interfaces are incompatible.
 *           Real-life analogy: a travel plug adapter.
 *
 *        Client ---> Target (interface your code expects)
 *                         ^
 *                      Adapter  ---has-a--->  Adaptee (third-party / legacy class
 *                                                      with a different API)
 *
 *  WHEN
 *    - Integrating a third-party SDK or legacy code whose API you can't change
 *    - Supporting several vendors behind ONE internal interface
 *      (Stripe, PayPal, Razorpay -> PaymentProcessor)
 *    - Protecting your domain from vendor-specific types
 *      (an "anti-corruption layer" in DDD terms)
 *
 *  TWO FORMS
 *    Object adapter : HAS-A adaptee (composition). Preferred and flexible.
 *    Class adapter  : extends the adaptee AND implements the target
 *                     (inheritance). Rare in Java because of single inheritance.
 *
 *  JDK EXAMPLES
 *    Arrays.asList(array)              array        -> List
 *    InputStreamReader(inputStream)    byte stream  -> character Reader
 *    Collections.enumeration(list)     Collection   -> legacy Enumeration
 */

import java.util.*;

public class P7_Adapter {

    public static void main(String[] args) {
        paymentExample();
        legacyExample();
    }

    // =================================================================
    // 1. Unify different payment SDKs behind our own interface
    // =================================================================

    // TARGET: the interface OUR checkout code is written against
    interface PaymentProcessor {
        PaymentResult pay(String orderId, double amountInRupees);
    }

    record PaymentResult(boolean success, String reference) {}

    // ADAPTEE 1: third-party SDK we can't modify. Amounts in paise, returns a Map.
    static class StripeSdk {
        Map<String, Object> createCharge(long amountInPaise, String currency, String description) {
            return Map.of("status", "succeeded", "id", "ch_" + description.hashCode());
        }
    }

    // ADAPTEE 2: a different vendor with a completely different style
    static class LegacyBankGateway {
        int doTransaction(String merchantRef, String amountStr) {    // returns a code: 0 = OK
            System.out.println("      (legacy gateway got amount string '" + amountStr + "')");
            return 0;
        }
    }

    // ADAPTER 1
    static class StripeAdapter implements PaymentProcessor {
        private final StripeSdk stripe;
        StripeAdapter(StripeSdk stripe) { this.stripe = stripe; }

        public PaymentResult pay(String orderId, double rupees) {
            long paise = Math.round(rupees * 100);                         // translate units
            Map<String, Object> resp = stripe.createCharge(paise, "inr", orderId);
            boolean ok = "succeeded".equals(resp.get("status"));          // translate the response
            return new PaymentResult(ok, (String) resp.get("id"));
        }
    }

    // ADAPTER 2
    static class LegacyBankAdapter implements PaymentProcessor {
        private final LegacyBankGateway gateway;
        LegacyBankAdapter(LegacyBankGateway gateway) { this.gateway = gateway; }

        public PaymentResult pay(String orderId, double rupees) {
            int code = gateway.doTransaction("M-" + orderId, String.format("%.2f", rupees));
            return new PaymentResult(code == 0, "BANK-" + orderId);
        }
    }

    // CLIENT: knows only PaymentProcessor, never imports vendor classes
    static class CheckoutService {
        private final PaymentProcessor processor;
        CheckoutService(PaymentProcessor processor) { this.processor = processor; }

        void checkout(String orderId, double amount) {
            PaymentResult r = processor.pay(orderId, amount);
            System.out.println("   " + orderId + " -> success=" + r.success() + " ref=" + r.reference());
        }
    }

    static void paymentExample() {
        System.out.println("===== 1. PAYMENT ADAPTERS =====");
        new CheckoutService(new StripeAdapter(new StripeSdk())).checkout("ORD-1", 499.99);
        new CheckoutService(new LegacyBankAdapter(new LegacyBankGateway())).checkout("ORD-2", 1250);
    }

    // =================================================================
    // 2. Adapting legacy APIs to modern ones (both directions)
    // =================================================================
    // Old code returns an Enumeration; we want to use it in for-each / streams
    static Enumeration<String> legacyUserIds() {
        return new Vector<>(List.of("u1", "u2", "u3")).elements();
    }

    // Our own adapter: Enumeration -> Iterator (the JDK also offers Enumeration.asIterator())
    static class EnumerationIterator<T> implements Iterator<T> {
        private final Enumeration<T> e;
        EnumerationIterator(Enumeration<T> e) { this.e = e; }
        public boolean hasNext() { return e.hasMoreElements(); }
        public T next() { return e.nextElement(); }
    }

    static void legacyExample() {
        System.out.println("\n===== 2. LEGACY API ADAPTERS =====");
        Iterator<String> it = new EnumerationIterator<>(legacyUserIds());
        System.out.print("   Enumeration adapted to Iterator: ");
        while (it.hasNext()) System.out.print(it.next() + " ");
        System.out.println();

        String[] arr = {"x", "y"};
        List<String> asList = Arrays.asList(arr);                   // JDK adapter: array -> List view
        System.out.println("   Arrays.asList adapter: " + asList + " (size " + asList.size() + ")");
    }
}
