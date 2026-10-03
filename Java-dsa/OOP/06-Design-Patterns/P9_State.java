/*
 * ============================================================================
 *  PATTERN 9 (bonus, needed for LLD) : STATE  (Behavioral)
 * ============================================================================
 *
 *  Run:  java P9_State.java
 *
 *  INTENT : let an object change its BEHAVIOUR when its internal STATE
 *           changes. It looks as if the object changed its class.
 *
 *  PROBLEM : a vending machine where every method is a big switch:
 *      void insertCoin() {
 *          switch (state) {
 *              case IDLE -> ...; case HAS_MONEY -> ...; case SOLD_OUT -> ...; case DISPENSING -> ...
 *          }
 *      }
 *      ...repeated in selectProduct(), dispense(), refund()...
 *    Adding a state means editing EVERY method. Transitions are scattered.
 *
 *  SOLUTION
 *      Context (VendingMachine) ---has-a---> State (interface)
 *                                             ^       ^        ^
 *                                           Idle  HasMoney  SoldOut
 *    - Each state is a class that implements ALL actions for that state.
 *    - The context DELEGATES every action to its current state object.
 *    - States decide the TRANSITIONS: machine.setState(new HasMoneyState()).
 *
 *  STATE vs STRATEGY: identical structure. Strategy is chosen by the CLIENT
 *  and rarely changes. State changes ITSELF in response to events, and states
 *  know about each other (transitions).
 *
 *  LLD PROBLEMS THAT ARE STATE MACHINES: Vending machine, ATM, Elevator,
 *  Order lifecycle (PLACED -> PAID -> SHIPPED -> DELIVERED / CANCELLED),
 *  Traffic light, Booking (AVAILABLE -> LOCKED -> BOOKED).
 *
 *  A lighter alternative for simple machines: an enum with a transition
 *  table (shown at the end).
 */

import java.util.*;

public class P9_State {

    public static void main(String[] args) {
        System.out.println("===== VENDING MACHINE (State pattern) =====");
        VendingMachine vm = new VendingMachine(Map.of("A1", new Product("Chips", 20, 1), "B2", new Product("Soda", 40, 5)));

        vm.select("A1");               // Idle: must insert money first
        vm.insertMoney(10);
        vm.select("A1");               // not enough money yet
        vm.insertMoney(20);
        vm.select("A1");               // dispenses + change, Chips now out of stock
        vm.insertMoney(50);
        vm.select("A1");               // out of stock
        vm.select("B2");               // ok
        vm.refund();                   // nothing to refund in Idle

        System.out.println("\n===== ORDER LIFECYCLE (enum transition table) =====");
        OrderStatus s = OrderStatus.PLACED;
        for (OrderStatus next : List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.PLACED, OrderStatus.DELIVERED)) {
            if (s.canMoveTo(next)) {
                System.out.println("   " + s + " -> " + next);
                s = next;
            } else {
                System.out.println("   " + s + " -> " + next + " REJECTED (illegal transition)");
            }
        }
    }

    // =================================================================
    // State interface: one method per user action
    // =================================================================
    interface VendingState {
        void insertMoney(VendingMachine m, int amount);
        void select(VendingMachine m, String code);
        void refund(VendingMachine m);
    }

    static class Product {
        final String name;
        final int price;
        int stock;
        Product(String name, int price, int stock) { this.name = name; this.price = price; this.stock = stock; }
    }

    // =================================================================
    // Context: holds data + current state, delegates every action
    // =================================================================
    static class VendingMachine {
        private VendingState state = new IdleState();
        private final Map<String, Product> slots;
        int balance = 0;

        VendingMachine(Map<String, Product> slots) { this.slots = new HashMap<>(slots); }

        // Public API: pure delegation, no if/else on state
        void insertMoney(int amount) { state.insertMoney(this, amount); }
        void select(String code) { state.select(this, code); }
        void refund() { state.refund(this); }

        void setState(VendingState next) {
            System.out.println("      [state] " + state.getClass().getSimpleName() + " -> " + next.getClass().getSimpleName());
            state = next;
        }

        Product product(String code) { return slots.get(code); }
    }

    // =================================================================
    // Concrete states: each knows the behaviour AND the transitions
    // =================================================================
    static class IdleState implements VendingState {
        public void insertMoney(VendingMachine m, int amount) {
            m.balance += amount;
            System.out.println("   inserted " + amount + ", balance " + m.balance);
            m.setState(new HasMoneyState());
        }

        public void select(VendingMachine m, String code) {
            System.out.println("   please insert money first");
        }

        public void refund(VendingMachine m) {
            System.out.println("   nothing to refund");
        }
    }

    static class HasMoneyState implements VendingState {
        public void insertMoney(VendingMachine m, int amount) {
            m.balance += amount;
            System.out.println("   added " + amount + ", balance " + m.balance);   // stay in the same state
        }

        public void select(VendingMachine m, String code) {
            Product p = m.product(code);
            if (p == null) {
                System.out.println("   invalid code " + code);
            } else if (p.stock == 0) {
                System.out.println("   " + p.name + " is OUT OF STOCK, pick another or refund");
            } else if (m.balance < p.price) {
                System.out.println("   " + p.name + " costs " + p.price + ", balance only " + m.balance);
            } else {
                m.setState(new DispensingState());
                p.stock--;
                m.balance -= p.price;
                System.out.println("   >>> dispensed " + p.name + (m.balance > 0 ? ", change " + m.balance : ""));
                m.balance = 0;
                m.setState(new IdleState());
            }
        }

        public void refund(VendingMachine m) {
            System.out.println("   refunded " + m.balance);
            m.balance = 0;
            m.setState(new IdleState());
        }
    }

    // A short-lived state: all actions are rejected while dispensing
    static class DispensingState implements VendingState {
        public void insertMoney(VendingMachine m, int amount) { System.out.println("   busy, please wait"); }
        public void select(VendingMachine m, String code) { System.out.println("   busy, please wait"); }
        public void refund(VendingMachine m) { System.out.println("   busy, please wait"); }
    }

    // =================================================================
    // Lightweight alternative: enum + allowed-transitions table
    // =================================================================
    enum OrderStatus {
        PLACED, PAID, SHIPPED, DELIVERED, CANCELLED;

        private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);
        static {
            ALLOWED.put(PLACED, EnumSet.of(PAID, CANCELLED));
            ALLOWED.put(PAID, EnumSet.of(SHIPPED, CANCELLED));
            ALLOWED.put(SHIPPED, EnumSet.of(DELIVERED));
            ALLOWED.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
            ALLOWED.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        }

        boolean canMoveTo(OrderStatus next) { return ALLOWED.get(this).contains(next); }
    }
}
