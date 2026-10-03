/*
 * ============================================================================
 *  PATTERN 8 : TEMPLATE METHOD  (Behavioral)
 * ============================================================================
 *
 *  Run:  java P8_TemplateMethod.java
 *
 *  INTENT : define the SKELETON of an algorithm in a base-class method,
 *           and let subclasses redefine certain STEPS without changing the
 *           algorithm's overall structure.
 *
 *     abstract class DataImporter {
 *         public final void importData() {     // the TEMPLATE METHOD (final!)
 *             open();                          //   fixed step
 *             List rows = parse();             //   ABSTRACT step: subclasses must implement
 *             if (shouldValidate()) validate(); //  HOOK: optional override with a default
 *             save(rows);                      //   fixed step
 *         }
 *     }
 *
 *  KEY IDEAS
 *    - The template method is usually FINAL so subclasses can't reorder steps.
 *    - Abstract steps   : MUST be implemented.
 *    - Hook methods     : CAN be overridden; they have a default (often empty).
 *    - "Hollywood Principle": "Don't call us, we'll call you". The parent
 *      calls the child's steps, not the other way round.
 *
 *  USE WHEN: several classes run the same process with small variations
 *    (import CSV/JSON/XML, a game turn, a report generator, a payment flow:
 *    validate -> debit -> credit -> notify).
 *
 *  TEMPLATE vs STRATEGY: Template = INHERITANCE, varies STEPS, fixed at
 *  compile time. Strategy = COMPOSITION, varies the WHOLE algorithm, swappable
 *  at runtime. Modern Java often replaces template subclasses with lambdas
 *  passed as strategies.
 *
 *  JDK / SPRING EXAMPLES: AbstractList (get/size abstract, the rest
 *  templated), HttpServlet.service() calling doGet/doPost, Spring's
 *  JdbcTemplate / RestTemplate (template + callbacks).
 */

import java.util.*;

public class P8_TemplateMethod {

    public static void main(String[] args) {
        System.out.println("===== CSV IMPORT =====");
        new CsvImporter("name,age\nRavi,30\nAsha,27").importData();

        System.out.println("\n===== JSON IMPORT (overrides the hook to skip validation) =====");
        new JsonImporter("[{\"name\":\"Kabir\"},{\"name\":\"Meera\"}]").importData();

        System.out.println("\n===== TEMPLATE + CALLBACK (JdbcTemplate style) =====");
        TransactionTemplate tx = new TransactionTemplate();
        tx.execute(() -> System.out.println("   debit A, credit B"));
        tx.execute(() -> { throw new IllegalStateException("insufficient funds"); });
    }

    // =================================================================
    // Abstract class holding the template method
    // =================================================================
    static abstract class DataImporter {
        protected final String source;
        DataImporter(String source) { this.source = source; }

        // THE TEMPLATE METHOD: fixed order, final so nobody can break it
        public final void importData() {
            System.out.println("   1. open " + getClass().getSimpleName());
            List<Map<String, String>> rows = parse();            // abstract step
            System.out.println("   2. parsed " + rows.size() + " rows");
            if (shouldValidate()) {                              // hook
                rows.forEach(this::validate);
                System.out.println("   3. validated");
            } else {
                System.out.println("   3. validation skipped");
            }
            save(rows);                                          // shared step
            afterImport(rows.size());                            // hook (empty by default)
        }

        protected abstract List<Map<String, String>> parse();

        protected boolean shouldValidate() { return true; }      // HOOK with default

        protected void validate(Map<String, String> row) {
            if (row.getOrDefault("name", "").isBlank()) throw new IllegalArgumentException("name missing");
        }

        private void save(List<Map<String, String>> rows) {      // private: not a step to override
            System.out.println("   4. saved " + rows);
        }

        protected void afterImport(int count) {}                 // HOOK, empty
    }

    static class CsvImporter extends DataImporter {
        CsvImporter(String src) { super(src); }

        @Override
        protected List<Map<String, String>> parse() {
            String[] lines = source.split("\n");
            String[] header = lines[0].split(",");
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i < lines.length; i++) {
                String[] cells = lines[i].split(",");
                Map<String, String> row = new LinkedHashMap<>();
                for (int c = 0; c < header.length; c++) row.put(header[c], cells[c]);
                rows.add(row);
            }
            return rows;
        }

        @Override
        protected void afterImport(int count) { System.out.println("   5. CSV hook: emailed report for " + count + " rows"); }
    }

    static class JsonImporter extends DataImporter {
        JsonImporter(String src) { super(src); }

        @Override
        protected List<Map<String, String>> parse() {
            // Tiny fake JSON parser, just enough for the demo
            List<Map<String, String>> rows = new ArrayList<>();
            for (String part : source.split("\\},\\{")) {
                String name = part.replaceAll(".*\"name\":\"([^\"]+)\".*", "$1");
                rows.add(Map.of("name", name));
            }
            return rows;
        }

        @Override
        protected boolean shouldValidate() { return false; }    // trusted internal source
    }

    // =================================================================
    // Template + callback: the modern, composition-friendly variant.
    // The template owns the fixed steps (begin/commit/rollback). The caller
    // passes just the variable part as a lambda.
    // =================================================================
    static class TransactionTemplate {
        void execute(Runnable work) {
            System.out.println("   BEGIN");
            try {
                work.run();
                System.out.println("   COMMIT");
            } catch (RuntimeException e) {
                System.out.println("   ROLLBACK (" + e.getMessage() + ")");
            }
        }
    }
}
