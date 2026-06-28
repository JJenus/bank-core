package com.jjenus.bank.core.transactions;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import java.time.Instant;

public record Transaction(
    TransactionId id,
    AccountId accountId,
    TransactionType type,
    Money amount,
    Money balanceAfter,
    String description,
    String reference,
    Instant timestamp,
    String relatedTransactionId,  // For linking transfers, refunds, reversals, etc.
    String metadata               // JSON string for additional data
) {

    public Transaction {
        if (id == null) throw new IllegalArgumentException("Transaction ID cannot be null");
        if (accountId == null) throw new IllegalArgumentException("Account ID cannot be null");
        if (type == null) throw new IllegalArgumentException("Transaction type cannot be null");
        if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
        if (balanceAfter == null) throw new IllegalArgumentException("Balance after cannot be null");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        // Validate amount sign matches transaction type
        validateAmountMatchesType(amount, type);
    }

    private static void validateAmountMatchesType(Money amount, TransactionType type) {
        if (type.isCredit() && amount.isNegative()) {
            throw new IllegalArgumentException("Credit transactions must have positive amount");
        }
        if (type.isDebit() && amount.isPositive()) {
            throw new IllegalArgumentException("Debit transactions must have negative amount");
        }
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    public static Transaction createDeposit(
        TransactionId id,
        AccountId accountId,
        Money amount,
        Money balanceAfter,
        String reference
    ) {
        return new Transaction(
            id,
            accountId,
            TransactionType.DEPOSIT,
            amount,
            balanceAfter,
            "Deposit to account",
            reference,
            Instant.now(),
            null,
            null
        );
    }

    public static Transaction createWithdrawal(
        TransactionId id,
        AccountId accountId,
        Money amount,
        Money balanceAfter,
        String reference
    ) {
        return new Transaction(
            id,
            accountId,
            TransactionType.WITHDRAWAL,
            amount.negate(),  // Store as negative
            balanceAfter,
            "Withdrawal from account",
            reference,
            Instant.now(),
            null,
            null
        );
    }

    public static Transaction createTransferOut(
        TransactionId id,
        AccountId accountId,
        Money amount,
        Money balanceAfter,
        String reference,
        String relatedTransactionId
    ) {
        return new Transaction(
            id,
            accountId,
            TransactionType.TRANSFER_OUT,
            amount.negate(),  // Store as negative
            balanceAfter,
            "Transfer to another account",
            reference,
            Instant.now(),
            relatedTransactionId,
            null
        );
    }

    public static Transaction createTransferIn(
        TransactionId id,
        AccountId accountId,
        Money amount,
        Money balanceAfter,
        String reference,
        String relatedTransactionId
    ) {
        return new Transaction(
            id,
            accountId,
            TransactionType.TRANSFER_IN,
            amount,
            balanceAfter,
            "Transfer from another account",
            reference,
            Instant.now(),
            relatedTransactionId,
            null
        );
    }

    public static Transaction createFee(
        TransactionId id,
        AccountId accountId,
        Money amount,
        Money balanceAfter,
        String description
    ) {
        return new Transaction(
            id,
            accountId,
            TransactionType.FEE,
            amount.negate(),  // Store as negative
            balanceAfter,
            description,
            "SYSTEM_FEE",
            Instant.now(),
            null,
            null
        );
    }

    public static Transaction createInterest(
        TransactionId id,
        AccountId accountId,
        Money amount,
        Money balanceAfter,
        String reference
    ) {
        return new Transaction(
            id,
            accountId,
            TransactionType.INTEREST,
            amount,           // Credit — positive
            balanceAfter,
            "Interest credited",
            reference,
            Instant.now(),
            null,
            null
        );
    }

    public static Transaction createRefund(
        TransactionId id,
        AccountId accountId,
        Money amount,
        Money balanceAfter,
        String reference,
        String originalTransactionId
    ) {
        return new Transaction(
            id,
            accountId,
            TransactionType.REFUND,
            amount,           // Credit — positive
            balanceAfter,
            "Transaction refund",
            reference,
            Instant.now(),
            originalTransactionId,
            null
        );
    }

    public static Transaction createReversal(
        TransactionId id,
        AccountId accountId,
        Money amount,
        Money balanceAfter,
        String reference,
        String originalTransactionId
    ) {
        return new Transaction(
            id,
            accountId,
            TransactionType.REVERSAL,
            amount.negate(),  // Debit — negative
            balanceAfter,
            "Transaction reversal",
            reference,
            Instant.now(),
            originalTransactionId,
            null
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isCredit() {
        return type.isCredit();
    }

    public boolean isDebit() {
        return type.isDebit();
    }

    public Money getSignedAmount() {
        return amount;  // Already signed based on transaction type
    }

    public Money getAbsoluteAmount() {
        return amount.isNegative() ? amount.negate() : amount;
    }

    @Override
    public String toString() {
        return String.format("Transaction[%s, Account: %s, Type: %s, Amount: %s, Balance After: %s]",
            id.value(), accountId.value(), type, getAbsoluteAmount().format(), balanceAfter.format());
    }
}
