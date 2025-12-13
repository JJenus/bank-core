package com.jjenus.bank.core.accounts;

import com.jjenus.bank.core.shared.Money;
import java.time.Instant;
import java.util.Currency;

public record Account(
    AccountId id,
    String customerId,
    Money balance,
    AccountStatus status,
    Instant createdAt,
    Instant lastUpdatedAt,
    long version
) {

    public Account {
        if (id == null) throw new IllegalArgumentException("Account ID cannot be null");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Owner name cannot be blank");
        }
        if (balance == null) throw new IllegalArgumentException("Balance cannot be null");
        if (status == null) throw new IllegalArgumentException("Status cannot be null");
        if (createdAt == null) throw new IllegalArgumentException("Created at cannot be null");
        if (lastUpdatedAt == null) {
            lastUpdatedAt = createdAt;
        }
        if (version < 0) throw new IllegalArgumentException("Version cannot be negative");
    }

    public static Account create(AccountId id, String ownerName, Currency currency) {
        Instant now = Instant.now();
        return new Account(
            id,
            ownerName,
            Money.zero(currency),
            AccountStatus.ACTIVE,
            now,
            now,
            0L
        );
    }

    public Account deposit(Money amount) {
        validateActive();
        validatePositiveAmount(amount);
        validateSameCurrency(amount);

        Instant now = Instant.now();
        return new Account(
            id,
                customerId,
            balance.add(amount),
            status,
            createdAt,
            now,
            version + 1
        );
    }

    public Account withdraw(Money amount) {
        validateActive();
        validatePositiveAmount(amount);
        validateSameCurrency(amount);
        validateSufficientFunds(amount);

        Instant now = Instant.now();
        return new Account(
            id,
            customerId,
            balance.subtract(amount),
            status,
            createdAt,
            now,
            version + 1
        );
    }

    public Account freeze() {
        if (status == AccountStatus.FROZEN) {
            return this;
        }
        validateNotTerminal();

        Instant now = Instant.now();
        return new Account(
            id,
            customerId,
            balance,
            AccountStatus.FROZEN,
            createdAt,
            now,
            version + 1
        );
    }

    public Account activate() {
        if (status == AccountStatus.ACTIVE) {
            return this;
        }
        validateNotTerminal();

        Instant now = Instant.now();
        return new Account(
            id,
            customerId,
            balance,
            AccountStatus.ACTIVE,
            createdAt,
            now,
            version + 1
        );
    }

    public Account close() {
        if (status == AccountStatus.CLOSED) {
            return this;
        }

        if (!balance.isZero()) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }

        Instant now = Instant.now();
        return new Account(
            id,
            customerId,
            balance,
            AccountStatus.CLOSED,
            createdAt,
            now,
            version + 1
        );
    }

    public boolean hasSufficientFunds(Money amount) {
        validateSameCurrency(amount);
        return balance.isGreaterThanOrEqual(amount);
    }

    // Validation methods
    private void validateActive() {
        if (!status.canTransact()) {
            throw new IllegalStateException(
                String.format("Account is %s and cannot transact", status)
            );
        }
    }

    private void validateNotTerminal() {
        if (status.isTerminal()) {
            throw new IllegalStateException(
                String.format("Account is %s and cannot be modified", status)
            );
        }
    }

    private void validatePositiveAmount(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void validateSameCurrency(Money amount) {
        if (!balance.currency().equals(amount.currency())) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: account is %s, transaction is %s",
                    balance.currency(), amount.currency())
            );
        }
    }

    private void validateSufficientFunds(Money amount) {
        if (!hasSufficientFunds(amount)) {
            throw new IllegalStateException(
                String.format("Insufficient funds: balance is %s, attempting to withdraw %s",
                    balance.format(), amount.format())
            );
        }
    }

    // Helper methods
    public boolean isOverdrawn() {
        return balance.isNegative();
    }

    public boolean hasPositiveBalance() {
        return balance.isPositive();
    }

    public Currency getCurrency() {
        return balance.currency();
    }

    @Override
    public String toString() {
        return String.format("Account[%s, Owner: %s, Balance: %s, Status: %s]",
            id.value(), customerId, balance.format(), status);
    }
}
