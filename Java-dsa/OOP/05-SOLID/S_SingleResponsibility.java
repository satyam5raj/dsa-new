/*
 * ============================================================================
 *  SOLID - S : SINGLE RESPONSIBILITY PRINCIPLE (SRP)
 * ============================================================================
 *
 *  Run:  java S_SingleResponsibility.java
 *
 *  "A class should have one, and only one, REASON TO CHANGE."  - Robert C. Martin
 *
 *  The precise meaning: a class should be responsible to ONE ACTOR
 *  (one stakeholder / source of change requests). Not "do one tiny thing".
 *
 *  Example: an Invoice class that
 *    - calculates totals          -> changes when the FINANCE team changes tax rules
 *    - formats a PDF/HTML         -> changes when the DESIGN team changes the layout
 *    - saves to the database      -> changes when the DBA changes the schema
 *    - emails the customer        -> changes when MARKETING changes the email
 *  That's 4 reasons to change. A layout tweak could accidentally break the
 *  tax math. Merge conflicts happen because 4 teams edit one file.
 *
 *  SYMPTOMS OF VIOLATION
 *    - Class names like Manager, Processor, Util, Helper that do "everything"
 *    - Hard-to-name classes; descriptions with "and" ("calculates AND saves AND emails")
 *    - Many unrelated imports (SQL + HTTP + PDF in one file)
 *    - Tests need a database just to check a calculation
 *
 *  HOW TO APPLY
 *    Split by reason-to-change, then let a small coordinating class
 *    (a "service" / "use case") wire them together.
 *
 *  DON'T OVERDO IT: one method per class is not the goal. Group things that
 *  change together (high cohesion), separate things that change for
 *  different reasons.
 */

import java.util.*;

public class S_SingleResponsibility {

    public static void main(String[] args) {
        System.out.println("===== BAD: one class, four reasons to change =====");
        new BadInvoiceService().process("cust@mail.com", List.of(new Item("Keyboard", 1500, 2), new Item("Mouse", 700, 1)));

        System.out.println("\n===== GOOD: each class has one reason to change =====");
        InvoiceService service = new InvoiceService(
                new TaxCalculator(0.18),
                new TextInvoiceFormatter(),
                new InMemoryInvoiceRepository(),
                new ConsoleEmailSender());
        service.process("cust@mail.com", List.of(new Item("Keyboard", 1500, 2), new Item("Mouse", 700, 1)));

        System.out.println("\n===== Bonus: tests are easy now (no DB, no email) =====");
        TaxCalculator calc = new TaxCalculator(0.18);
        System.out.println("   total for 1000 = " + calc.total(List.of(new Item("X", 1000, 1))) + " (expected 1180.0)");
    }

    record Item(String name, double price, int qty) {
        double subtotal() { return price * qty; }
    }

    // =================================================================
    // BAD: a "God class"
    // =================================================================
    static class BadInvoiceService {
        void process(String email, List<Item> items) {
            // 1) business rules (Finance)
            double subtotal = 0;
            for (Item i : items) subtotal += i.subtotal();
            double total = subtotal * 1.18;

            // 2) presentation (Design)
            StringBuilder sb = new StringBuilder("INVOICE\n");
            for (Item i : items) sb.append("  ").append(i.name()).append(" x").append(i.qty()).append('\n');
            sb.append("  TOTAL: ").append(total);

            // 3) persistence (DBA)
            System.out.println("   INSERT INTO invoices VALUES ('" + email + "', " + total + ")");

            // 4) notification (Marketing)
            System.out.println("   Sending email to " + email + ":\n" + sb);
        }
    }

    // =================================================================
    // GOOD: one responsibility each
    // =================================================================

    // Finance rules only
    static class TaxCalculator {
        private final double taxRate;
        TaxCalculator(double taxRate) { this.taxRate = taxRate; }

        double subtotal(List<Item> items) {
            return items.stream().mapToDouble(Item::subtotal).sum();
        }

        double total(List<Item> items) {
            return Math.round(subtotal(items) * (1 + taxRate) * 100) / 100.0;
        }
    }

    // Presentation only
    interface InvoiceFormatter { String format(List<Item> items, double total); }

    static class TextInvoiceFormatter implements InvoiceFormatter {
        public String format(List<Item> items, double total) {
            StringBuilder sb = new StringBuilder("   INVOICE\n");
            for (Item i : items) sb.append(String.format("     %-10s x%d  %8.2f%n", i.name(), i.qty(), i.subtotal()));
            sb.append(String.format("     TOTAL (incl. tax)   %8.2f", total));
            return sb.toString();
        }
    }

    // Persistence only
    interface InvoiceRepository { int save(String email, double total); }

    static class InMemoryInvoiceRepository implements InvoiceRepository {
        private final Map<Integer, Double> store = new HashMap<>();
        public int save(String email, double total) {
            int id = store.size() + 1;
            store.put(id, total);
            System.out.println("   saved invoice #" + id);
            return id;
        }
    }

    // Notification only
    interface EmailSender { void send(String to, String body); }

    static class ConsoleEmailSender implements EmailSender {
        public void send(String to, String body) { System.out.println("   email to " + to + ":\n" + body); }
    }

    // Coordinator: knows the ORDER of steps, delegates the work
    static class InvoiceService {
        private final TaxCalculator calculator;
        private final InvoiceFormatter formatter;
        private final InvoiceRepository repository;
        private final EmailSender emailSender;

        InvoiceService(TaxCalculator c, InvoiceFormatter f, InvoiceRepository r, EmailSender e) {
            this.calculator = c;
            this.formatter = f;
            this.repository = r;
            this.emailSender = e;
        }

        void process(String email, List<Item> items) {
            double total = calculator.total(items);
            repository.save(email, total);
            emailSender.send(email, formatter.format(items, total));
        }
    }
}
