/*
 * ============================================================================
 *  PATTERN 4 : STRATEGY  (Behavioral)
 * ============================================================================
 *
 *  Run:  java P4_Strategy.java
 *
 *  INTENT : define a FAMILY of algorithms, put each in its own class, and
 *           make them INTERCHANGEABLE. The client (the "context") picks or
 *           receives one and delegates to it, at runtime.
 *
 *         Context  ---has-a--->  Strategy (interface)
 *                                   ^        ^        ^
 *                              StrategyA StrategyB StrategyC
 *
 *  PROBLEM IT SOLVES: a method full of if/else for different ways of doing
 *  the same job (pricing rules, sorting, routing, payment, compression).
 *
 *  USE WHEN
 *    - You have several variants of an algorithm and switch between them
 *    - You want to add new variants without touching the context (OCP)
 *    - You want to unit-test each algorithm in isolation
 *
 *  STRATEGY vs STATE: same structure. In Strategy the CLIENT chooses the
 *  algorithm. In State the object changes its own behaviour as its internal
 *  state changes.
 *  STRATEGY vs TEMPLATE METHOD: Strategy uses composition (swap the whole
 *  algorithm). Template Method uses inheritance (override steps of a fixed
 *  skeleton).
 *
 *  JDK EXAMPLES: Comparator (passed to sort), ThreadPoolExecutor's
 *  RejectedExecutionHandler. In Java 8+ a strategy is often just a lambda.
 *
 *  LLD RELEVANCE: parking lot pricing, ride-sharing fare/matching,
 *  rate-limiter algorithms, payment methods, cache eviction policies.
 */

import java.util.*;

public class P4_Strategy {

    public static void main(String[] args) {
        System.out.println("===== PARKING FEE STRATEGIES =====");
        ParkingLot weekday = new ParkingLot(new HourlyPricing(40));
        ParkingLot weekend = new ParkingLot(new FlatRatePricing(150));
        ParkingLot mall = new ParkingLot(new TieredPricing());
        for (int hours : new int[]{1, 3, 8}) {
            System.out.printf("   %d h -> hourly=%d, flat=%d, tiered=%d%n",
                    hours, weekday.checkout(hours), weekend.checkout(hours), mall.checkout(hours));
        }

        System.out.println("\n===== SWAP STRATEGY AT RUNTIME =====");
        ParkingLot lot = new ParkingLot(new HourlyPricing(40));
        System.out.println("   normal day, 5h = " + lot.checkout(5));
        lot.setPricing(new SurgePricing(new HourlyPricing(40), 1.5));   // strategies can wrap each other
        System.out.println("   concert day (surge 1.5x), 5h = " + lot.checkout(5));

        System.out.println("\n===== STRATEGY AS LAMBDA =====");
        lot.setPricing(hours -> hours <= 2 ? 0 : (hours - 2) * 30);      // "first 2 hours free"
        System.out.println("   2h = " + lot.checkout(2) + ", 4h = " + lot.checkout(4));

        System.out.println("\n===== SELECTING A STRATEGY FROM CONFIG =====");
        Map<String, PricingStrategy> byName = Map.of(
                "HOURLY", new HourlyPricing(40),
                "FLAT", new FlatRatePricing(150),
                "TIERED", new TieredPricing());
        String configured = "TIERED";                                    // e.g. from application.yml
        System.out.println("   configured " + configured + ", 6h = " + new ParkingLot(byName.get(configured)).checkout(6));

        System.out.println("\n===== JDK: Comparator IS a strategy =====");
        List<String> names = new ArrayList<>(List.of("Zoya", "amit", "Bhavna", "chetan"));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        System.out.println("   case-insensitive: " + names);
        names.sort(Comparator.comparingInt(String::length));
        System.out.println("   by length:        " + names);
    }

    // ---------------- Strategy interface ----------------
    @FunctionalInterface
    interface PricingStrategy {
        int fee(int hours);
    }

    // ---------------- Concrete strategies ----------------
    static class HourlyPricing implements PricingStrategy {
        private final int ratePerHour;
        HourlyPricing(int ratePerHour) { this.ratePerHour = ratePerHour; }
        public int fee(int hours) { return hours * ratePerHour; }
    }

    static class FlatRatePricing implements PricingStrategy {
        private final int flat;
        FlatRatePricing(int flat) { this.flat = flat; }
        public int fee(int hours) { return flat; }
    }

    static class TieredPricing implements PricingStrategy {
        public int fee(int hours) {
            if (hours <= 2) return 50;                    // first 2 hours
            if (hours <= 6) return 50 + (hours - 2) * 30; // next 4 hours
            return 170 + (hours - 6) * 20;                // after that
        }
    }

    // A strategy that decorates another strategy (strategies compose nicely)
    static class SurgePricing implements PricingStrategy {
        private final PricingStrategy base;
        private final double multiplier;
        SurgePricing(PricingStrategy base, double multiplier) { this.base = base; this.multiplier = multiplier; }
        public int fee(int hours) { return (int) Math.round(base.fee(hours) * multiplier); }
    }

    // ---------------- Context ----------------
    static class ParkingLot {
        private PricingStrategy pricing;                  // HAS-A strategy

        ParkingLot(PricingStrategy pricing) { this.pricing = pricing; }

        void setPricing(PricingStrategy pricing) { this.pricing = pricing; }

        int checkout(int hours) {
            // The context doesn't know or care HOW the fee is computed
            return pricing.fee(hours);
        }
    }
}
