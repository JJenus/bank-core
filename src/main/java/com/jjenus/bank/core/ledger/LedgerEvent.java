package com.jjenus.bank.core.ledger;

import com.jjenus.bank.core.shared.DomainEvent;
import com.jjenus.bank.core.shared.Id;
import java.time.Instant;

/**
 * Domain events emitted by the {@link Ledger} aggregate.
 *
 * <p>Consistent with the pattern established by {@code AccountEvent}:
 * sealed interface, record subtypes, static factory methods on the interface.
 */
public sealed interface LedgerEvent extends DomainEvent {

    // ── Factory methods ───────────────────────────────────────────────────────

    static JournalEntryPosted journalEntryPosted(LedgerEntry entry) {
        return new JournalEntryPosted(Id.random(), Instant.now(), entry);
    }

    static JournalEntryReversed journalEntryReversed(LedgerEntry originalEntry, LedgerEntry reversalEntry, String reason) {
        return new JournalEntryReversed(Id.random(), Instant.now(), originalEntry, reversalEntry, reason);
    }

    // ── Event records ─────────────────────────────────────────────────────────

    /**
     * Emitted when a balanced journal entry is successfully posted to the ledger.
     */
    record JournalEntryPosted(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        LedgerEntry entry
    ) implements LedgerEvent {}

    /**
     * Emitted when a posted journal entry is reversed.
     */
    record JournalEntryReversed(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        LedgerEntry originalEntry,
        LedgerEntry reversalEntry,
        String reason
    ) implements LedgerEvent {}
}
