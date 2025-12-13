package com.jjenus.bank.core.transfers;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import com.jjenus.bank.core.transactions.TransactionId;
import java.time.Instant;

public record Transfer(
    TransferId id,
    AccountId fromAccountId,
    AccountId toAccountId,
    Money amount,
    TransferStatus status,
    String description,
    String reference,
    Instant createdAt,
    Instant completedAt,
    TransactionId debitTransactionId,   // Transaction on fromAccount
    TransactionId creditTransactionId,  // Transaction on toAccount
    String failureReason
) {

    public Transfer {
        if (id == null) throw new IllegalArgumentException("Transfer ID cannot be null");
        if (fromAccountId == null) throw new IllegalArgumentException("From account ID cannot be null");
        if (toAccountId == null) throw new IllegalArgumentException("To account ID cannot be null");
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (status == null) throw new IllegalArgumentException("Status cannot be null");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static Transfer initiate(
        TransferId id,
        AccountId fromAccountId,
        AccountId toAccountId,
        Money amount,
        String description,
        String reference
    ) {
        return new Transfer(
            id,
            fromAccountId,
            toAccountId,
            amount,
            TransferStatus.PENDING,
            description,
            reference,
            Instant.now(),
            null,
            null,
            null,
            null
        );
    }

    public Transfer markProcessing(TransactionId debitTransactionId) {
        if (status != TransferStatus.PENDING) {
            throw new IllegalStateException("Only pending transfers can be marked as processing");
        }
        return new Transfer(
            id,
            fromAccountId,
            toAccountId,
            amount,
            TransferStatus.PROCESSING,
            description,
            reference,
            createdAt,
            null,
            debitTransactionId,
            null,
            null
        );
    }

    public Transfer complete(TransactionId creditTransactionId) {
        if (status != TransferStatus.PROCESSING) {
            throw new IllegalStateException("Only processing transfers can be completed");
        }
        Instant now = Instant.now();
        return new Transfer(
            id,
            fromAccountId,
            toAccountId,
            amount,
            TransferStatus.COMPLETED,
            description,
            reference,
            createdAt,
            now,
            debitTransactionId,
            creditTransactionId,
            null
        );
    }

    public Transfer fail(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Terminal transfer status cannot be changed");
        }
        return new Transfer(
            id,
            fromAccountId,
            toAccountId,
            amount,
            TransferStatus.FAILED,
            description,
            reference,
            createdAt,
            Instant.now(),
            debitTransactionId,
            creditTransactionId,
            reason
        );
    }

    public Transfer reverse(String reason) {
        if (status != TransferStatus.COMPLETED) {
            throw new IllegalStateException("Only completed transfers can be reversed");
        }
        return new Transfer(
            id,
            fromAccountId,
            toAccountId,
            amount,
            TransferStatus.REVERSED,
            description + " (REVERSED)",
            reference,
            createdAt,
            Instant.now(),
            debitTransactionId,
            creditTransactionId,
            reason
        );
    }

    public boolean isCompleted() {
        return status == TransferStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == TransferStatus.FAILED;
    }

    public boolean isPending() {
        return status == TransferStatus.PENDING;
    }

    public boolean canBeProcessed() {
        return status == TransferStatus.PENDING;
    }

    @Override
    public String toString() {
        return String.format("Transfer[%s, From: %s, To: %s, Amount: %s, Status: %s]",
            id.value(), fromAccountId.value(), toAccountId.value(), amount.format(), status);
    }
}

