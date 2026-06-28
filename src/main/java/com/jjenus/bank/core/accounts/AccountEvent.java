package com.jjenus.bank.core.accounts;

import com.jjenus.bank.core.shared.Id;
import com.jjenus.bank.core.shared.DomainEvent;
import com.jjenus.bank.core.shared.Money;
import java.time.Instant;

public sealed interface AccountEvent extends DomainEvent {

    AccountId accountId();

    // ── Factory methods ───────────────────────────────────────────────────────

    static AccountCreated accountCreated(AccountId accountId, String ownerName, String currency) {
        return new AccountCreated(Id.random(), Instant.now(), accountId, ownerName, currency);
    }

    static MoneyDeposited moneyDeposited(AccountId accountId, Money amount, String reference) {
        return new MoneyDeposited(Id.random(), Instant.now(), accountId, amount, reference);
    }

    static MoneyWithdrawn moneyWithdrawn(AccountId accountId, Money amount, String reference) {
        return new MoneyWithdrawn(Id.random(), Instant.now(), accountId, amount, reference);
    }

    static AccountFrozen accountFrozen(AccountId accountId, String reason) {
        return new AccountFrozen(Id.random(), Instant.now(), accountId, reason);
    }

    static AccountSuspended accountSuspended(AccountId accountId, String reason) {
        return new AccountSuspended(Id.random(), Instant.now(), accountId, reason);
    }

    static AccountActivated accountActivated(AccountId accountId) {
        return new AccountActivated(Id.random(), Instant.now(), accountId);
    }

    static AccountMarkedDormant accountMarkedDormant(AccountId accountId) {
        return new AccountMarkedDormant(Id.random(), Instant.now(), accountId);
    }

    static AccountClosed accountClosed(AccountId accountId, String reason) {
        return new AccountClosed(Id.random(), Instant.now(), accountId, reason);
    }

    // ── Event records ─────────────────────────────────────────────────────────

    record AccountCreated(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        AccountId accountId,
        String ownerName,
        String currencyCode
    ) implements AccountEvent {}

    record MoneyDeposited(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        AccountId accountId,
        Money amount,
        String reference
    ) implements AccountEvent {}

    record MoneyWithdrawn(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        AccountId accountId,
        Money amount,
        String reference
    ) implements AccountEvent {}

    record AccountFrozen(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        AccountId accountId,
        String reason
    ) implements AccountEvent {}

    record AccountSuspended(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        AccountId accountId,
        String reason
    ) implements AccountEvent {}

    record AccountActivated(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        AccountId accountId
    ) implements AccountEvent {}

    record AccountMarkedDormant(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        AccountId accountId
    ) implements AccountEvent {}

    record AccountClosed(
        Id<DomainEvent> eventId,
        Instant occurredOn,
        AccountId accountId,
        String reason
    ) implements AccountEvent {}
}
