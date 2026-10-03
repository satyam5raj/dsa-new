package com.learning.spring.autoconfig;

/**
 * The "library API" that our custom auto-configuration provides a default for.
 * See AuditAutoConfiguration.
 */
public interface AuditLogger {

    String log(String event);

    /** The default implementation that the auto-configuration registers. */
    class ConsoleAuditLogger implements AuditLogger {
        private final String prefix;

        public ConsoleAuditLogger(String prefix) { this.prefix = prefix; }

        @Override
        public String log(String event) {
            String line = prefix + " " + event;
            System.out.println(line);
            return line;
        }
    }
}
