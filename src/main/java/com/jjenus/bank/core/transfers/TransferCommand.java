package com.jjenus.bank.core.transfers;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import java.time.Instant;

public sealed interface TransferCommand {

    TransferId transferId();
    Instant timestamp();

    record InitiateTransfer(
        TransferId transferId,
        AccountId fromAccountId,
        AccountId toAccountId,
        Money amount,
        String description,
        String reference,
        Instant timestamp
    ) implements TransferCommand {

        public InitiateTransfer {
            if (fromAccountId == null) throw new IllegalArgumentException("From account ID cannot be null");
            if (toAccountId == null) throw new IllegalArgumentException("To account ID cannot be null");
            if (fromAccountId.equals(toAccountId)) {
                throw new IllegalArgumentException("Cannot transfer to the same account");
            }
            if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
            if (!amount.isPositive()) {
                throw new IllegalArgumentException("Transfer amount must be positive");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("Description cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static InitiateTransfer now(
            TransferId transferId,
            AccountId fromAccountId,
            AccountId toAccountId,
            Money amount,
            String description,
            String reference
        ) {
            return new InitiateTransfer(
                transferId,
                fromAccountId,
                toAccountId,
                amount,
                description,
                reference,
                Instant.now()
            );
        }
    }

    record ProcessTransfer(
        TransferId transferId,
        String processorId,
        Instant timestamp
    ) implements TransferCommand {

        public ProcessTransfer {
            if (processorId == null || processorId.isBlank()) {
                throw new IllegalArgumentException("Processor ID cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static ProcessTransfer now(TransferId transferId, String processorId) {
            return new ProcessTransfer(transferId, processorId, Instant.now());
        }
    }

    record CompleteTransfer(
        TransferId transferId,
        String confirmationCode,
        Instant timestamp
    ) implements TransferCommand {

        public CompleteTransfer {
            if (confirmationCode == null || confirmationCode.isBlank()) {
                throw new IllegalArgumentException("Confirmation code cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static CompleteTransfer now(TransferId transferId, String confirmationCode) {
            return new CompleteTransfer(transferId, confirmationCode, Instant.now());
        }
    }

    record FailTransfer(
        TransferId transferId,
        String reason,
        Instant timestamp
    ) implements TransferCommand {

        public FailTransfer {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Failure reason cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static FailTransfer now(TransferId transferId, String reason) {
            return new FailTransfer(transferId, reason, Instant.now());
        }
    }

    record ReverseTransfer(
        TransferId transferId,
        String reason,
        Instant timestamp
    ) implements TransferCommand {

        public ReverseTransfer {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Reversal reason cannot be blank");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public static ReverseTransfer now(TransferId transferId, String reason) {
            return new ReverseTransfer(transferId, reason, Instant.now());
        }
    }
}
