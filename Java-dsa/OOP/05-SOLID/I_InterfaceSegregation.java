/*
 * ============================================================================
 *  SOLID - I : INTERFACE SEGREGATION PRINCIPLE (ISP)
 * ============================================================================
 *
 *  Run:  java I_InterfaceSegregation.java
 *
 *  "Clients should not be forced to depend on methods they do not use."
 *
 *  A FAT interface forces implementers to write dummy methods (often throwing
 *  UnsupportedOperationException, which also breaks LSP). It also couples
 *  every client to every method: a change to scan() forces recompiling and
 *  retesting code that only prints.
 *
 *  FIX: split into small, ROLE-based interfaces. A class implements only the
 *  roles it truly supports. A client asks only for the role it needs.
 *
 *  JDK EXAMPLES OF GOOD SEGREGATION
 *    Comparable, Runnable, AutoCloseable, Iterable, Supplier...
 *    tiny interfaces, each describing one capability.
 *
 *  SRP vs ISP: SRP is about CLASSES having one reason to change.
 *              ISP is about INTERFACES being shaped by what CLIENTS need.
 */

import java.util.*;

public class I_InterfaceSegregation {

    public static void main(String[] args) {
        bad();
        good();
    }

    // =================================================================
    // BAD: one fat interface for every kind of machine
    // =================================================================
    interface MultiFunctionDevice {
        void print(String doc);
        void scan(String doc);
        void fax(String doc, String number);
        void staple(String doc);
    }

    static class CheapPrinter implements MultiFunctionDevice {
        public void print(String doc) { System.out.println("   printing " + doc); }
        // Forced to implement things it can't do:
        public void scan(String doc) { throw new UnsupportedOperationException("no scanner"); }
        public void fax(String doc, String n) { throw new UnsupportedOperationException("no fax"); }
        public void staple(String doc) { /* silently does nothing: even worse! */ }
    }

    static void bad() {
        System.out.println("===== BAD: FAT INTERFACE =====");
        MultiFunctionDevice d = new CheapPrinter();
        d.print("report.pdf");
        try {
            d.scan("id-card.png");
        } catch (UnsupportedOperationException e) {
            System.out.println("   runtime surprise: " + e.getMessage());
        }
    }

    // =================================================================
    // GOOD: role interfaces
    // =================================================================
    interface Printer { void print(String doc); }
    interface Scanner { String scan(String doc); }
    interface Fax { void fax(String doc, String number); }

    // Interfaces can be combined when a role really is a bundle
    interface Copier extends Printer, Scanner {
        default void copy(String doc) { print(scan(doc)); }
    }

    static class SimplePrinter implements Printer {
        public void print(String doc) { System.out.println("   [SimplePrinter] printing " + doc); }
    }

    static class OfficeMachine implements Copier, Fax {
        public void print(String doc) { System.out.println("   [OfficeMachine] printing " + doc); }
        public String scan(String doc) {
            System.out.println("   [OfficeMachine] scanning " + doc);
            return "scan-of-" + doc;
        }
        public void fax(String doc, String n) { System.out.println("   [OfficeMachine] faxing " + doc + " to " + n); }
    }

    // Clients depend on the SMALLEST role they need
    static class ReportService {
        private final Printer printer;                 // doesn't care about scanning or faxing
        ReportService(Printer printer) { this.printer = printer; }
        void printMonthly() { printer.print("monthly-report.pdf"); }
    }

    static class KycService {
        private final Scanner scanner;
        KycService(Scanner scanner) { this.scanner = scanner; }
        String captureId() { return scanner.scan("aadhaar.png"); }
    }

    static void good() {
        System.out.println("\n===== GOOD: SEGREGATED INTERFACES =====");
        OfficeMachine office = new OfficeMachine();
        new ReportService(new SimplePrinter()).printMonthly();   // works with a cheap printer
        new ReportService(office).printMonthly();                // and with the big machine
        System.out.println("   KYC got: " + new KycService(office).captureId());
        office.copy("contract.pdf");
        // new KycService(new SimplePrinter());  // COMPILE ERROR: caught at compile time, not runtime
    }
}
