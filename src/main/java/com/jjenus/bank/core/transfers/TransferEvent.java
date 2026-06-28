package com.jjenus.bank.core.transfers;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.DomainEvent;
import com.jjenus.bank.core.shared.Id;
import com.jjenus.bank.core.shared.Money;
import com.jjenus.bank.core.transactions.TransactionId;
import java.time.Instant;

/**
 * Typed domain events emitted by the transfer flow in {@link TransferService}.
 *
 * <p>Replaces the {@code List<String>} events field in {@code TransferExecutionResult}
 * and {@code ReversalResult} with structured, subscribable domain events. Consumers
 * in the application/infrastructure layer can pattern-match on these to trigger
 * downstream side-effects (notifications, audit logs, ledger postings, etc.).
 *
 * <p>Consistent with the patterns established by {@code AccountEvent} and
 * {@code LedgerEvent}: sealed interface, record subtypes, static factory methods.
 */
public sealed interface TransferEvent extends DomainEvent {

    TransferId transferId();

    // ── Factory methods ───────────────────────────────────────────────────────

    static TransferInitiated transferInitiated(
        TransferId transferId,
        AccountId fromAccountId,
        AccountId toAccountId,
        Money amount,
        String reference
    ) {
        return new TransferInitiated(
            Id.random(), Instant.now(),
            transferId, fromAccountId, toAccountId, amount, reference
        );
    }

    static TransferDebited transferDebited(
        TransferId transferId,
        AccountId fromAccountId,
        Money amount,
        TransactionId debitTransactionId
    ) {
        return new TransferDebited(
            Id.random(), Instant.now(),
            transferId, fromAccountId, amount, debitTransactionId
        );
    }

    static TransferCredited transferCredited(
        TransferId transferId,
        AccountId toAccountId,
        Money amount,
        TransactionId creditTransactionId
    ) {
        return new TransferCredited(
            Id.random(), Instant.now(),
            transferId, toAccountId, amount, creditTransactionId
        );
    }

    static TransferCompleted transferCompleted(
        TransferId transferId,
        AccountId fromAccountId,
        AccountId toAccountId,
        Money amount
    ) {
        return new TransferCompleted(
            Id.random(), Instant.now(),
            transferId, fromAccountId, toAccountId, amount
        );
    }

    static TransferFailed transferFailed(
        TransferId transferId,
        String reason
    ) {
        return new TransferFailed(Id.random(), Instant.now(), transferId, reason);
    }

    static TransferReversed transferReversed(
        TransferId transferId,
        AccountId originalFromAccountId,
        AccountId originalToAccountId,
        Money amount,
        String reason,
        TransactionId reversalDebitId,
        TransactionId reversalCreditId
    ) {
        return new TransferReversed(
            Id.random(), Instant.now(),
            transferId, originalFromAccountId, originalToAccountId,
            amount, reason, reversalDebitId, reversalCreditId
        );
    }

    static TransferCancelled transferCancelled(TransferId transferId, String reason) {
        return new TransferCancelled(Id.random(), Instant.now(), transferId, reason);
    }

    // ── Event records ─────────────────────────────────────────────────────────

    /** Emitted when a transfer command is accepted and a pending transfer record is created. */
    record TransferInitiated(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        TransferId transferId,
        AccountId fromAccountId,
        AccountId toAccountId,
        Money amount,
        String reference
    ) implements TransferEvent {}

    /** Emitted when the source account has been successfully debited. */
    record TransferDebited(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        TransferId transferId,
        AccountId fromAccountId,
        Money amount,
        TransactionId debitTransactionId
    ) implements TransferEvent {}

    /** Emitted when the target account has been successfully credited. */
    record TransferCredited(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        TransferId transferId,
        AccountId toAccountId,
        Money amount,
        TransactionId creditTransactionId
    ) implements TransferEvent {}

    /** Emitted when both legs have settled and the transfer is COMPLETED. */
    record TransferCompleted(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        TransferId transferId,
        AccountId fromAccountId,
        AccountId toAccountId,
        Money amount
    ) implements TransferEvent {}

    /** Emitted when a transfer cannot be executed and is marked FAILED. */
    record TransferFailed(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        TransferId transferId,
        String reason
    ) implements TransferEvent {}

    /** Emitted when a completed transfer is fully reversed — both money-movement legs. */
    record TransferReversed(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        TransferId transferId,
        AccountId originalFromAccountId,
        AccountId originalToAccountId,
        Money amount,
        String reason,
        TransactionId reversalDebitTransactionId,
        TransactionId reversalCreditTransactionId
    ) implements TransferEvent {}

    /** Emitted when a pending transfer is cancelled before execution. */
    record TransferCancelled(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        TransferId transferId,
        String reason
    ) implements TransferEvent {}
}
