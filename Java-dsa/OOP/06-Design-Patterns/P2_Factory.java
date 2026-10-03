/*
 * ============================================================================
 *  PATTERN 2 : FACTORY  (Creational)
 *              Simple Factory, Factory Method, Abstract Factory
 * ============================================================================
 *
 *  Run:  java P2_Factory.java
 *
 *  PROBLEM : client code that does  new EmailNotification()  /
 *            new SmsNotification() ... is coupled to every concrete class.
 *            Adding a type means editing every place that creates objects.
 *
 *  INTENT  : put object CREATION in one place, and let clients depend only on
 *            the interface of the product.
 *
 *  THREE FLAVOURS
 *  --------------------------------------------------------------------------
 *  1. SIMPLE FACTORY (not an official GoF pattern, but the most used)
 *     One class/method with a switch that returns the right implementation.
 *        NotificationFactory.create("SMS")
 *
 *  2. FACTORY METHOD (GoF)
 *     A superclass defines an abstract "create" method. SUBCLASSES decide
 *     which concrete product to create. The superclass's other methods use
 *     the product without knowing its class.
 *        abstract class Logistics { abstract Transport createTransport(); }
 *        RoadLogistics -> Truck, SeaLogistics -> Ship
 *
 *  3. ABSTRACT FACTORY (GoF)
 *     A factory of FAMILIES of related objects that must be used together.
 *        UIFactory: createButton(), createCheckbox()
 *        DarkThemeFactory -> DarkButton + DarkCheckbox (never mixed with Light ones)
 *  --------------------------------------------------------------------------
 *
 *  JDK EXAMPLES: Calendar.getInstance(), NumberFormat.getInstance(),
 *                List.of(), Executors.newFixedThreadPool(), Path.of()
 *  (static factory methods, Effective Java Item 1)
 *
 *  USE WHEN: the exact type is decided at runtime (config, user input),
 *            creation is complex, or you want to hide implementation classes.
 */

import java.util.*;
import java.util.function.Supplier;

public class P2_Factory {

    public static void main(String[] args) {
        simpleFactory();
        registryFactory();
        factoryMethod();
        abstractFactory();
    }

    // ---------------- Product interface + implementations ----------------
    interface Notification { void send(String to, String msg); }

    static class EmailNotification implements Notification {
        public void send(String to, String msg) { System.out.println("   EMAIL -> " + to + ": " + msg); }
    }

    static class SmsNotification implements Notification {
        public void send(String to, String msg) { System.out.println("   SMS   -> " + to + ": " + msg); }
    }

    static class PushNotification implements Notification {
        public void send(String to, String msg) { System.out.println("   PUSH  -> " + to + ": " + msg); }
    }

    // =================================================================
    // 1. SIMPLE FACTORY
    // =================================================================
    static class NotificationFactory {
        private NotificationFactory() {}          // static utility

        static Notification create(String channel) {
            return switch (channel.toUpperCase()) {
                case "EMAIL" -> new EmailNotification();
                case "SMS" -> new SmsNotification();
                case "PUSH" -> new PushNotification();
                default -> throw new IllegalArgumentException("Unknown channel: " + channel);
            };
        }
    }

    static void simpleFactory() {
        System.out.println("===== 1. SIMPLE FACTORY =====");
        // Channel could come from user preferences / config at runtime
        for (String channel : List.of("email", "sms", "push")) {
            NotificationFactory.create(channel).send("user42", "Your OTP is 1234");
        }
    }

    // =================================================================
    // 1b. REGISTRY-BASED FACTORY - removes the switch, so it's Open/Closed
    //     New channel = one register() call, factory code untouched.
    // =================================================================
    static class NotificationRegistry {
        private final Map<String, Supplier<Notification>> creators = new HashMap<>();

        void register(String channel, Supplier<Notification> creator) {
            creators.put(channel.toUpperCase(), creator);
        }

        Notification create(String channel) {
            Supplier<Notification> s = creators.get(channel.toUpperCase());
            if (s == null) throw new IllegalArgumentException("Unknown channel: " + channel);
            return s.get();
        }
    }

    static void registryFactory() {
        System.out.println("\n===== 1b. REGISTRY FACTORY =====");
        NotificationRegistry registry = new NotificationRegistry();
        registry.register("email", EmailNotification::new);
        registry.register("sms", SmsNotification::new);
        registry.register("whatsapp", () -> (to, msg) -> System.out.println("   WHATSAPP -> " + to + ": " + msg));
        registry.create("whatsapp").send("user42", "Order shipped");
    }

    // =================================================================
    // 2. FACTORY METHOD
    //    Logistics has the shared business logic (planDelivery).
    //    Subclasses override ONE thing: which Transport to create.
    // =================================================================
    interface Transport { String deliver(String cargo); }

    static class Truck implements Transport {
        public String deliver(String cargo) { return "Truck drives " + cargo + " by road"; }
    }

    static class Ship implements Transport {
        public String deliver(String cargo) { return "Ship carries " + cargo + " by sea in containers"; }
    }

    static abstract class Logistics {
        // THE FACTORY METHOD
        protected abstract Transport createTransport();

        // Business logic using the product, without knowing its concrete class
        public void planDelivery(String cargo) {
            Transport t = createTransport();
            System.out.println("   " + getClass().getSimpleName() + ": " + t.deliver(cargo));
        }
    }

    static class RoadLogistics extends Logistics {
        protected Transport createTransport() { return new Truck(); }
    }

    static class SeaLogistics extends Logistics {
        protected Transport createTransport() { return new Ship(); }
    }

    static void factoryMethod() {
        System.out.println("\n===== 2. FACTORY METHOD =====");
        List<Logistics> companies = List.of(new RoadLogistics(), new SeaLogistics());
        companies.forEach(l -> l.planDelivery("100 laptops"));
    }

    // =================================================================
    // 3. ABSTRACT FACTORY - families of related products
    // =================================================================
    interface Button { String render(); }
    interface Checkbox { String render(); }

    interface UIFactory {
        Button createButton();
        Checkbox createCheckbox();
    }

    static class LightFactory implements UIFactory {
        public Button createButton() { return () -> "[ Light Button ]"; }
        public Checkbox createCheckbox() { return () -> "[x] light checkbox"; }
    }

    static class DarkFactory implements UIFactory {
        public Button createButton() { return () -> "[## Dark Button ##]"; }
        public Checkbox createCheckbox() { return () -> "[#] dark checkbox"; }
    }

    // The client only knows the abstract factory. It can never mix themes by accident.
    static class SettingsScreen {
        private final Button save;
        private final Checkbox notifications;

        SettingsScreen(UIFactory factory) {
            this.save = factory.createButton();
            this.notifications = factory.createCheckbox();
        }

        void render() { System.out.println("   " + save.render() + "   " + notifications.render()); }
    }

    static void abstractFactory() {
        System.out.println("\n===== 3. ABSTRACT FACTORY =====");
        String theme = "dark";                                    // e.g. from OS setting
        UIFactory factory = theme.equals("dark") ? new DarkFactory() : new LightFactory();
        new SettingsScreen(factory).render();
        new SettingsScreen(new LightFactory()).render();
    }
}
