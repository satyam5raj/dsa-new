/*
 * ============================================================================
 *  SOLID - O : OPEN/CLOSED PRINCIPLE (OCP)
 * ============================================================================
 *
 *  Run:  java O_OpenClosed.java
 *
 *  "Software entities should be OPEN for extension, but CLOSED for modification."
 *                                                          - Bertrand Meyer
 *
 *  Meaning: you should be able to add NEW behaviour by ADDING new code
 *  (a new class), NOT by EDITING existing, tested, deployed code.
 *
 *  WHY
 *    - Every edit to working code risks breaking it (regressions).
 *    - if/else-on-type chains grow forever and get copied around the codebase.
 *    - New features become isolated: new class + new test, nothing else touched.
 *
 *  HOW
 *    - Depend on an abstraction (interface).
 *    - Each variant is a separate implementation (polymorphism / strategy).
 *    - Register implementations (list, map, or a DI container like Spring
 *      that auto-collects all beans of an interface).
 *
 *  RED FLAG: switch(type) or if (x instanceof A) ... else if (x instanceof B)
 *  repeated in several places.
 *
 *  PRAGMATISM: you can't be closed against EVERY change. Apply OCP to the
 *  axes of change you've actually seen (or clearly expect). Here: new
 *  discount types keep coming from the marketing team.
 *
 *  (Modern twist: with a SEALED interface + exhaustive switch, adding a type
 *   makes the compiler flag every switch. That's a valid alternative when the
 *   set of types is closed and the OPERATIONS keep growing.)
 */

import java.util.*;

public class O_OpenClosed {

    public static void main(String[] args) {
        Order order = new Order("PREMIUM_USER", 5000, true);

        System.out.println("===== BAD: edit this method for every new discount =====");
        System.out.println("   discount = " + new BadDiscountCalculator().discount(order, "FESTIVE"));

        System.out.println("\n===== GOOD: add a class, change nothing else =====");
        DiscountEngine engine = new DiscountEngine(List.of(
                new FestiveDiscount(),
                new PremiumUserDiscount(),
                new FirstOrderDiscount(),
                new BulkOrderDiscount()          // added later: no existing code edited
        ));
        System.out.println("   best discount = " + engine.bestDiscount(order));
    }

    record Order(String userType, double amount, boolean firstOrder) {}

    // =================================================================
    // BAD
    // =================================================================
    static class BadDiscountCalculator {
        double discount(Order o, String campaign) {
            if (campaign.equals("FESTIVE")) {
                return o.amount() * 0.10;
            } else if (campaign.equals("PREMIUM")) {
                return o.userType().equals("PREMIUM_USER") ? o.amount() * 0.15 : 0;
            } else if (campaign.equals("FIRST_ORDER")) {
                return o.firstOrder() ? 200 : 0;
            }
            // Next week: "BULK"... edit again, retest everything, redeploy.
            return 0;
        }
    }

    // =================================================================
    // GOOD
    // =================================================================
    interface DiscountRule {
        boolean applies(Order o);
        double amount(Order o);
        default String name() { return getClass().getSimpleName(); }
    }

    static class FestiveDiscount implements DiscountRule {
        public boolean applies(Order o) { return true; }
        public double amount(Order o) { return o.amount() * 0.10; }
    }

    static class PremiumUserDiscount implements DiscountRule {
        public boolean applies(Order o) { return o.userType().equals("PREMIUM_USER"); }
        public double amount(Order o) { return o.amount() * 0.15; }
    }

    static class FirstOrderDiscount implements DiscountRule {
        public boolean applies(Order o) { return o.firstOrder(); }
        public double amount(Order o) { return 200; }
    }

    // NEW requirement = NEW class only
    static class BulkOrderDiscount implements DiscountRule {
        public boolean applies(Order o) { return o.amount() >= 10_000; }
        public double amount(Order o) { return o.amount() * 0.20; }
    }

    // CLOSED for modification: never changes when rules are added
    static class DiscountEngine {
        private final List<DiscountRule> rules;
        DiscountEngine(List<DiscountRule> rules) { this.rules = List.copyOf(rules); }

        String bestDiscount(Order o) {
            return rules.stream()
                    .filter(r -> r.applies(o))
                    .peek(r -> System.out.println("   " + r.name() + " -> " + r.amount(o)))
                    .max(Comparator.comparingDouble(r -> r.amount(o)))
                    .map(r -> r.name() + " (" + r.amount(o) + ")")
                    .orElse("none");
        }
    }
}
