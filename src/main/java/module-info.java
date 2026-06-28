/**
 * bank-core — Pure Java 17 domain library for banking operations.
 *
 * <p>Exports only the public domain API. Internal implementation details
 * (factory internals, private applier classes) are encapsulated by the module system.
 *
 * <p>No infrastructure dependencies are declared here. Consuming modules
 * (e.g., a Spring Boot application module) implement the port interfaces
 * exported from {@code com.jjenus.bank.core.ports} and inject them at startup.
 */
module com.jjenus.bank.core {

    // ── Public domain API ────────────────────────────────────────────────────
    exports com.jjenus.bank.core.shared;
    exports com.jjenus.bank.core.accounts;
    exports com.jjenus.bank.core.transactions;
    exports com.jjenus.bank.core.transfers;
    exports com.jjenus.bank.core.ledger;
    exports com.jjenus.bank.core.policy;
    exports com.jjenus.bank.core.ports;

    // ── Test access (open to JUnit/AssertJ reflection) ───────────────────────
    opens com.jjenus.bank.core.accounts   to org.junit.platform.commons;
    opens com.jjenus.bank.core.transactions to org.junit.platform.commons;
    opens com.jjenus.bank.core.transfers  to org.junit.platform.commons;
    opens com.jjenus.bank.core.ledger     to org.junit.platform.commons;
    opens com.jjenus.bank.core.policy     to org.junit.platform.commons;
    opens com.jjenus.bank.core.shared     to org.junit.platform.commons;
    opens com.jjenus.bank.core.ports      to org.junit.platform.commons;
}
