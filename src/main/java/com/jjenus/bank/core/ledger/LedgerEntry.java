package com.jjenus.bank.core.ledger;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import java.time.Instant;

/**
 * A single leg of a double-entry journal entry.
 *
 * <p>Every financial movement in the system produces a pair of {@code LedgerEntry} records —
 * one DEBIT and one CREDIT — whose amounts must be equal. This pair is the atomic unit of
 * double-entry bookkeeping; the {@link Ledger} enforces that every posted entry is balanced.
 *
 * <p>This is distinct from {@code Transaction}: a {@code Transaction} is an account's
 * <em>statement view</em> of a movement (one account, one signed amount). A
 * {@code LedgerEntry} is the accounting <em>journal view</em> — it names both sides
 * (debit account and credit account) and always carries a positive amount.
 */
public record LedgerEntry(
    LedgerEntryId id,
    AccountId debitAccountId,   // Account that is debited (value flows out)
    AccountId creditAccountId,  // Account that is credited (value flows in)
    Money amount,               // Always positive; sign is encoded by debit/credit semantics
    String description,
    String reference,           // Idempotency key / external reference
    String sourceId,            // Originating transaction or transfer ID
    Instant postedAt
) {

    public LedgerEntry {
        if (id == null) throw new IllegalArgumentException("LedgerEntry ID cannot be null");
        if (debitAccountId == null) throw new IllegalArgumentException("Debit account ID cannot be null");
        if (creditAccountId == null) throw new IllegalArgumentException("Credit account ID cannot be null");
        if (debitAccountId.equals(creditAccountId)) {
            throw new IllegalArgumentException("Debit and credit accounts cannot be the same");
        }
        if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("LedgerEntry amount must be positive");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (postedAt == null) {
            postedAt = Instant.now();
        }
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Creates a deposit journal entry.
     * Debits the cash/suspense account, credits the customer deposit account.
     */
    public static LedgerEntry forDeposit(
        LedgerEntryId id,
        AccountId cashAccountId,
        AccountId customerAccountId,
        Money amount,
        String reference,
        String sourceTransactionId
    ) {
        return new LedgerEntry(
            id,
            cashAccountId,       // Debit: cash comes in
            customerAccountId,   // Credit: customer's liability to bank increases
            amount,
            "Deposit: " + reference,
            reference,
            sourceTransactionId,
            Instant.now()
        );
    }

    /**
     * Creates a withdrawal journal entry.
     * Debits the customer deposit account, credits the cash/suspense account.
     */
    public static LedgerEntry forWithdrawal(
        LedgerEntryId id,
        AccountId customerAccountId,
        AccountId cashAccountId,
        Money amount,
        String reference,
        String sourceTransactionId
    ) {
        return new LedgerEntry(
            id,
            customerAccountId,   // Debit: customer's liability to bank decreases
            cashAccountId,       // Credit: cash goes out
            amount,
            "Withdrawal: " + reference,
            reference,
            sourceTransactionId,
            Instant.now()
        );
    }

    /**
     * Creates a transfer journal entry between two customer accounts.
     * Debits the sender, credits the receiver.
     */
    public static LedgerEntry forTransfer(
        LedgerEntryId id,
        AccountId fromAccountId,
        AccountId toAccountId,
        Money amount,
        String reference,
        String sourceTransferId
    ) {
        return new LedgerEntry(
            id,
            fromAccountId,   // Debit: sender's balance decreases
            toAccountId,     // Credit: receiver's balance increases
            amount,
            "Transfer: " + reference,
            reference,
            sourceTransferId,
            Instant.now()
        );
    }

    /**
     * Creates a fee journal entry.
     * Debits the customer account, credits the fee income account.
     */
    public static LedgerEntry forFee(
        LedgerEntryId id,
        AccountId customerAccountId,
        AccountId feeIncomeAccountId,
        Money amount,
        String description,
        String sourceId
    ) {
        return new LedgerEntry(
            id,
            customerAccountId,    // Debit: customer pays the fee
            feeIncomeAccountId,   // Credit: bank earns income
            amount,
            description,
            "FEE",
            sourceId,
            Instant.now()
        );
    }

    /**
     * Creates a reversal journal entry — the mirror image of an original entry.
     * Debits the original credit account and credits the original debit account.
     */
    public static LedgerEntry forReversal(
        LedgerEntryId id,
        LedgerEntry originalEntry,
        String reason
    ) {
        return new LedgerEntry(
            id,
            originalEntry.creditAccountId(),   // Flip: debit the original credit side
            originalEntry.debitAccountId(),    // Flip: credit the original debit side
            originalEntry.amount(),
            "Reversal of " + originalEntry.id().value() + ": " + reason,
            originalEntry.reference(),
            originalEntry.id().value(),
            Instant.now()
        );
    }

    /**
     * Creates an interest credit journal entry.
     * Debits the interest expense account, credits the customer account.
     */
    public static LedgerEntry forInterest(
        LedgerEntryId id,
        AccountId interestExpenseAccountId,
        AccountId customerAccountId,
        Money amount,
        String reference,
        String sourceId
    ) {
        return new LedgerEntry(
            id,
            interestExpenseAccountId,   // Debit: bank's interest expense
            customerAccountId,           // Credit: customer earns interest
            amount,
            "Interest: " + reference,
            reference,
            sourceId,
            Instant.now()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the net effect of this entry on {@code accountId}.
     * Positive if the account is on the credit side (money in),
     * negative if on the debit side (money out).
     *
     * @throws IllegalArgumentException if accountId is not a participant in this entry
     */
    public Money netEffectOn(AccountId accountId) {
        if (creditAccountId.equals(accountId)) {
            return amount;          // Credit: positive effect
        } else if (debitAccountId.equals(accountId)) {
            return amount.negate(); // Debit: negative effect
        }
        throw new IllegalArgumentException(
            "Account " + accountId.value() + " is not a participant in entry " + id.value()
        );
    }

    public boolean involves(AccountId accountId) {
        return debitAccountId.equals(accountId) || creditAccountId.equals(accountId);
    }

    @Override
    public String toString() {
        return String.format("LedgerEntry[%s, Dr: %s, Cr: %s, Amount: %s, Ref: %s]",
            id.value(), debitAccountId.value(), creditAccountId.value(),
            amount.format(), reference != null ? reference : "—");
    }
}
