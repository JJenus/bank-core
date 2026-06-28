package com.jjenus.bank.core.accounts;

import com.jjenus.bank.core.shared.Money;
import java.time.Instant;

public sealed interface AccountCommand {

    AccountId accountId();
    Instant timestamp();

    record CreateAccount(
        AccountId accountId,
        String ownerName,
        String currencyCode,
        Instant timestamp
    ) implements AccountCommand {

        public CreateAccount {
            if (ownerName == null || ownerName.isBlank()) {
                throw new IllegalArgumentException("Owner name cannot be blank");
            }
            if (currencyCode == null || currencyCode.isBlank()) {
                throw new IllegalArgumentException("Currency code cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static CreateAccount now(AccountId accountId, String ownerName, String currencyCode) {
            return new CreateAccount(accountId, ownerName, currencyCode, Instant.now());
        }
    }

    record DepositMoney(
        AccountId accountId,
        Money amount,
        String reference,
        Instant timestamp
    ) implements AccountCommand {

        public DepositMoney {
            if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
            if (reference == null || reference.isBlank()) {
                throw new IllegalArgumentException("Reference cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static DepositMoney now(AccountId accountId, Money amount, String reference) {
            return new DepositMoney(accountId, amount, reference, Instant.now());
        }
    }

    record WithdrawMoney(
        AccountId accountId,
        Money amount,
        String reference,
        Instant timestamp
    ) implements AccountCommand {

        public WithdrawMoney {
            if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
            if (reference == null || reference.isBlank()) {
                throw new IllegalArgumentException("Reference cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static WithdrawMoney now(AccountId accountId, Money amount, String reference) {
            return new WithdrawMoney(accountId, amount, reference, Instant.now());
        }
    }

    record FreezeAccount(
        AccountId accountId,
        String reason,
        Instant timestamp
    ) implements AccountCommand {

        public FreezeAccount {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Reason cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static FreezeAccount now(AccountId accountId, String reason) {
            return new FreezeAccount(accountId, reason, Instant.now());
        }
    }

    record SuspendAccount(
        AccountId accountId,
        String reason,
        Instant timestamp
    ) implements AccountCommand {

        public SuspendAccount {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Reason cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static SuspendAccount now(AccountId accountId, String reason) {
            return new SuspendAccount(accountId, reason, Instant.now());
        }
    }

    record ActivateAccount(
        AccountId accountId,
        Instant timestamp
    ) implements AccountCommand {

        public ActivateAccount {
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static ActivateAccount now(AccountId accountId) {
            return new ActivateAccount(accountId, Instant.now());
        }
    }

    record MarkAccountDormant(
        AccountId accountId,
        Instant timestamp
    ) implements AccountCommand {

        public MarkAccountDormant {
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static MarkAccountDormant now(AccountId accountId) {
            return new MarkAccountDormant(accountId, Instant.now());
        }
    }

    record CloseAccount(
        AccountId accountId,
        String reason,
        Instant timestamp
    ) implements AccountCommand {

        public CloseAccount {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Reason cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static CloseAccount now(AccountId accountId, String reason) {
            return new CloseAccount(accountId, reason, Instant.now());
        }
    }
}
