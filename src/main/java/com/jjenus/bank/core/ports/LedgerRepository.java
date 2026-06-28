package com.jjenus.bank.core.ports;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.ledger.LedgerEntry;
import com.jjenus.bank.core.ledger.LedgerEntryId;
import com.jjenus.bank.core.shared.Money;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * Output port for ledger persistence.
 *
 * <p>The ledger store is append-only by design. Implementations must never
 * allow mutation of posted entries; only appending new entries (including reversals) is permitted.
 */
public interface LedgerRepository {

    /** Appends a journal entry to the persistent ledger. Entry IDs must be unique. */
    LedgerEntry post(LedgerEntry entry);

    /** Appends multiple entries atomically. All succeed or none are persisted. */
    List<LedgerEntry> postAll(List<LedgerEntry> entries);

    /** Finds a ledger entry by its ID. */
    Optional<LedgerEntry> findById(LedgerEntryId id);

    /** Returns all entries involving a given account, in posting order. */
    List<LedgerEntry> findByAccountId(AccountId accountId);

    /** Returns all entries involving an account posted before or at the given timestamp. */
    List<LedgerEntry> findByAccountIdAsOf(AccountId accountId, Instant asOf);

    /** Returns all entries for a given external reference (useful for idempotency queries). */
    List<LedgerEntry> findByReference(String reference);

    /**
     * Computes the current running balance for an account from all posted entries.
     * Equivalent to {@code SUM(creditAmounts) - SUM(debitAmounts)} for the account.
     */
    Money computeBalance(AccountId accountId, Currency currency);

    /**
     * Computes the balance for an account as of a point in time.
     */
    Money computeBalanceAsOf(AccountId accountId, Currency currency, Instant asOf);

    /**
     * Checks if a journal entry with the given reference and sourceId already exists.
     * Used for idempotent posting.
     */
    boolean existsByReferenceAndSource(String reference, String sourceId);

    /** Returns the total number of posted entries. */
    long count();
}
