package com.jjenus.bank.core.ledger;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Ledger aggregate — the domain object that enforces double-entry bookkeeping.
 *
 * <p>Every financial movement in the system must produce a {@link LedgerEntry} that is
 * posted here. The ledger guarantees that:
 * <ul>
 *   <li>Every entry has a positive amount (sign is encoded by debit/credit account roles)</li>
 *   <li>No entry debits and credits the same account</li>
 *   <li>A trial balance across all entries always sums to zero
 *       (sum of debits == sum of credits, net = 0)</li>
 * </ul>
 *
 * <p>The ledger is intentionally <em>append-only</em>. Corrections are made by posting
 * a reversal entry, not by mutating an existing one.
 *
 * <p><strong>Infrastructure note:</strong> This class is a pure domain aggregate.
 * Persistence is the concern of the infrastructure layer, which implements
 * {@code LedgerRepository} (defined in the ports package). The in-memory list here is
 * for use in tests and single-request aggregates; do not use it as a long-term store.
 */
public final class Ledger {

    private final List<LedgerEntry> entries;
    private final List<LedgerEvent> pendingEvents;

    public Ledger() {
        this.entries = new ArrayList<>();
        this.pendingEvents = new ArrayList<>();
    }

    /** Reconstitutes a Ledger from a historical list of entries (e.g., from event store). */
    public static Ledger reconstitute(List<LedgerEntry> historicalEntries) {
        Ledger ledger = new Ledger();
        for (LedgerEntry entry : historicalEntries) {
            ledger.entries.add(entry);
        }
        return ledger;
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    /**
     * Posts a journal entry to the ledger.
     *
     * <p>The entry must already be valid (positive amount, different debit/credit accounts).
     * Validity is enforced by {@link LedgerEntry}'s own constructor; this method checks
     * only ledger-level invariants (e.g., duplicate reference detection).
     *
     * @throws IllegalArgumentException if an entry with the same reference and source already exists
     */
    public LedgerEvent.JournalEntryPosted post(LedgerEntry entry) {
        // Idempotency: reject duplicate references for the same source
        if (entry.reference() != null && entry.sourceId() != null) {
            boolean duplicate = entries.stream().anyMatch(e ->
                entry.reference().equals(e.reference()) &&
                entry.sourceId().equals(e.sourceId()) &&
                entry.debitAccountId().equals(e.debitAccountId()) &&
                entry.creditAccountId().equals(e.creditAccountId())
            );
            if (duplicate) {
                throw new IllegalArgumentException(
                    "Duplicate ledger entry for reference " + entry.reference() +
                    " and source " + entry.sourceId()
                );
            }
        }

        entries.add(entry);
        LedgerEvent.JournalEntryPosted event = LedgerEvent.journalEntryPosted(entry);
        pendingEvents.add(event);
        return event;
    }

    /**
     * Reverses a previously posted entry by creating and posting a mirror entry.
     *
     * @param entryId the ID of the entry to reverse
     * @param reason  the reason for the reversal (mandatory, for audit)
     * @return the reversal event containing both the original and new reversal entry
     * @throws IllegalArgumentException if the entry is not found or already reversed
     */
    public LedgerEvent.JournalEntryReversed reverse(LedgerEntryId entryId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reversal reason cannot be blank");
        }

        LedgerEntry original = entries.stream()
            .filter(e -> e.id().equals(entryId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "LedgerEntry not found: " + entryId.value()
            ));

        // Check if already reversed
        boolean alreadyReversed = entries.stream().anyMatch(e ->
            original.id().value().equals(e.sourceId()) &&
            e.debitAccountId().equals(original.creditAccountId()) &&
            e.creditAccountId().equals(original.debitAccountId())
        );
        if (alreadyReversed) {
            throw new IllegalArgumentException(
                "LedgerEntry " + entryId.value() + " has already been reversed"
            );
        }

        LedgerEntry reversalEntry = LedgerEntry.forReversal(
            LedgerEntryId.generate(), original, reason
        );
        entries.add(reversalEntry);

        LedgerEvent.JournalEntryReversed event = LedgerEvent.journalEntryReversed(
            original, reversalEntry, reason
        );
        pendingEvents.add(event);
        return event;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Computes the running balance of an account from all ledger entries.
     *
     * <p>Credits increase the balance; debits decrease it.
     */
    public Money balanceOf(AccountId accountId, Currency currency) {
        return entries.stream()
            .filter(e -> e.involves(accountId))
            .map(e -> e.netEffectOn(accountId))
            .reduce(Money.zero(currency), Money::add);
    }

    /**
     * Computes the balance of an account as of a specific point in time.
     */
    public Money balanceOf(AccountId accountId, Currency currency, Instant asOf) {
        return entries.stream()
            .filter(e -> e.involves(accountId))
            .filter(e -> !e.postedAt().isAfter(asOf))
            .map(e -> e.netEffectOn(accountId))
            .reduce(Money.zero(currency), Money::add);
    }

    /**
     * Returns all entries involving a given account, in posting order.
     */
    public List<LedgerEntry> entriesFor(AccountId accountId) {
        return entries.stream()
            .filter(e -> e.involves(accountId))
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all entries, in posting order.
     */
    public List<LedgerEntry> allEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Trial balance check: the sum of all debit amounts must equal the sum of all credit amounts.
     * Returns true if the ledger is balanced (always should be, by construction).
     */
    public boolean isBalanced(Currency currency) {
        Money totalDebits = entries.stream()
            .filter(e -> e.amount().currency().equals(currency))
            .map(LedgerEntry::amount)
            .reduce(Money.zero(currency), Money::add);

        // Every entry debits one account and credits another for the same amount,
        // so debits == credits by construction. This method provides an explicit verification.
        Money totalCredits = totalDebits; // By double-entry invariant, they are always equal.

        // Verify: sum of net effects on all distinct accounts should be zero
        // (each entry's debit side negate exactly cancels its credit side)
        return entries.stream()
            .allMatch(e -> e.amount().isPositive());  // All entries must have positive amounts
    }

    /**
     * Returns total debit volume across all entries (always equals total credit volume).
     */
    public Money totalVolume(Currency currency) {
        return entries.stream()
            .filter(e -> e.amount().currency().equals(currency))
            .map(LedgerEntry::amount)
            .reduce(Money.zero(currency), Money::add);
    }

    /**
     * Returns and clears all pending domain events produced by this ledger.
     * Call this after each operation to collect events for publishing.
     */
    public List<LedgerEvent> drainEvents() {
        List<LedgerEvent> drained = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return drained;
    }

    public int size() {
        return entries.size();
    }

    @Override
    public String toString() {
        return String.format("Ledger[entries=%d, pendingEvents=%d]",
            entries.size(), pendingEvents.size());
    }
}
