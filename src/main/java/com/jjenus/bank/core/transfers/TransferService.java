package com.jjenus.bank.core.transfers;

import com.jjenus.bank.core.accounts.Account;
import com.jjenus.bank.core.shared.Money;
import com.jjenus.bank.core.shared.Result;
import com.jjenus.bank.core.transactions.Transaction;
import com.jjenus.bank.core.transactions.TransactionId;
import java.util.List;

/**
 * Application service / command handler for transfer operations.
 *
 * <p>All methods are stateless and pure. No persistence or event publishing happens
 * here; that is the responsibility of the calling application layer, which receives
 * structured {@link TransferEvent} objects to publish downstream.
 */
public final class TransferService {

    private TransferService() {}

    // ── Result records ────────────────────────────────────────────────────────

    /**
     * Result of a successfully executed transfer.
     *
     * <p>{@code domainEvents} contains structured {@link TransferEvent} objects
     * (replacing the previous {@code List<String>}) that the application layer
     * should publish to the event bus / event store after persisting account and
     * transaction state.
     */
    public record TransferExecutionResult(
        Transfer transfer,
        Account updatedFromAccount,
        Account updatedToAccount,
        Transaction debitTransaction,
        Transaction creditTransaction,
        List<TransferEvent> domainEvents
    ) {
        /**
         * Convenience accessor kept for backward compatibility with callers that
         * previously iterated {@code events()} as strings.
         *
         * @deprecated Use {@link #domainEvents()} for typed event handling.
         */
        @Deprecated(since = "1.1.0", forRemoval = true)
        public List<String> events() {
            return domainEvents.stream()
                .map(e -> e.getClass().getSimpleName() + ": " + e.transferId().value())
                .toList();
        }
    }

    /**
     * Result of a successful transfer reversal.
     *
     * <p>{@code updatedReceiverAccount} is the original recipient — debited back.
     * {@code updatedSenderAccount} is the original sender — credited back.
     */
    public record ReversalResult(
        Transfer reversedTransfer,
        Account updatedReceiverAccount,
        Account updatedSenderAccount,
        Transaction reversalDebitTransaction,
        Transaction reversalCreditTransaction,
        List<TransferEvent> domainEvents
    ) {
        /** @deprecated Use {@link #domainEvents()} for typed event handling. */
        @Deprecated(since = "1.1.0", forRemoval = true)
        public List<String> events() {
            return domainEvents.stream()
                .map(e -> e.getClass().getSimpleName() + ": " + e.transferId().value())
                .toList();
        }
    }

    // ── Execute transfer ──────────────────────────────────────────────────────

    public static Result<TransferExecutionResult> executeTransfer(
        Account fromAccount,
        Account toAccount,
        TransferCommand.InitiateTransfer command
    ) {
        try {
            // 1. Validate accounts
            validateAccounts(fromAccount, toAccount, command.amount());

            // 2. Create transfer record
            Transfer transfer = Transfer.initiate(
                command.transferId(),
                command.fromAccountId(),
                command.toAccountId(),
                command.amount(),
                command.description(),
                command.reference()
            );

            // 3. Emit: transfer initiated
            TransferEvent initiated = TransferEvent.transferInitiated(
                transfer.id(),
                command.fromAccountId(),
                command.toAccountId(),
                command.amount(),
                command.reference()
            );

            // 4. Process withdrawal from source account
            Account updatedFrom = fromAccount.withdraw(command.amount());
            Transaction debitTransaction = Transaction.createTransferOut(
                TransactionId.generate(),
                command.fromAccountId(),
                command.amount(),
                updatedFrom.balance(),
                command.reference(),
                null  // linked after credit transaction is created
            );

            // 5. Mark transfer as processing
            transfer = transfer.markProcessing(debitTransaction.id());

            TransferEvent debited = TransferEvent.transferDebited(
                transfer.id(),
                command.fromAccountId(),
                command.amount(),
                debitTransaction.id()
            );

            // 6. Process deposit to target account
            Account updatedTo = toAccount.deposit(command.amount());
            Transaction creditTransaction = Transaction.createTransferIn(
                TransactionId.generate(),
                command.toAccountId(),
                command.amount(),
                updatedTo.balance(),
                command.reference(),
                debitTransaction.id().value()
            );

            // 7. Link debit transaction to its credit counterpart
            debitTransaction = new Transaction(
                debitTransaction.id(),
                debitTransaction.accountId(),
                debitTransaction.type(),
                debitTransaction.amount(),
                debitTransaction.balanceAfter(),
                debitTransaction.description(),
                debitTransaction.reference(),
                debitTransaction.timestamp(),
                creditTransaction.id().value(),
                debitTransaction.metadata()
            );

            TransferEvent credited = TransferEvent.transferCredited(
                transfer.id(),
                command.toAccountId(),
                command.amount(),
                creditTransaction.id()
            );

            // 8. Complete the transfer
            transfer = transfer.complete(creditTransaction.id());

            TransferEvent completed = TransferEvent.transferCompleted(
                transfer.id(),
                command.fromAccountId(),
                command.toAccountId(),
                command.amount()
            );

            return Result.success(new TransferExecutionResult(
                transfer,
                updatedFrom,
                updatedTo,
                debitTransaction,
                creditTransaction,
                List.of(initiated, debited, credited, completed)
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    // ── Reverse transfer ──────────────────────────────────────────────────────

    /**
     * Reverses a completed transfer.
     *
     * <p>Money flow: debit {@code receiverAccount} (original toAccount),
     * credit {@code senderAccount} (original fromAccount).
     *
     * @param transfer        the completed transfer to reverse
     * @param receiverAccount the account that originally received the money (will be debited)
     * @param senderAccount   the account that originally sent the money (will be credited)
     * @param reason          mandatory reason for the reversal
     */
    public static Result<ReversalResult> reverseTransfer(
        Transfer transfer,
        Account receiverAccount,
        Account senderAccount,
        String reason
    ) {
        try {
            if (!transfer.isCompleted()) {
                return Result.failure("Only completed transfers can be reversed");
            }

            if (transfer.amount().isZero()) {
                return Result.failure("Cannot reverse zero-amount transfer");
            }

            if (!receiverAccount.hasSufficientFunds(transfer.amount())) {
                return Result.failure(String.format(
                    "Receiver account has insufficient funds for reversal. Balance: %s, Required: %s",
                    receiverAccount.balance().format(),
                    transfer.amount().format()
                ));
            }

            // 1. Debit money back from the original receiver
            Account updatedReceiver = receiverAccount.withdraw(transfer.amount());
            Transaction reversalDebit = Transaction.createReversal(
                TransactionId.generate(),
                receiverAccount.id(),
                transfer.amount(),
                updatedReceiver.balance(),
                transfer.reference(),
                transfer.creditTransactionId() != null
                    ? transfer.creditTransactionId().value()
                    : null
            );

            // 2. Credit money back to the original sender
            Account updatedSender = senderAccount.deposit(transfer.amount());
            Transaction reversalCredit = Transaction.createRefund(
                TransactionId.generate(),
                senderAccount.id(),
                transfer.amount(),
                updatedSender.balance(),
                transfer.reference(),
                reversalDebit.id().value()
            );

            // 3. Mark the transfer as reversed
            Transfer reversed = transfer.reverse(reason);

            TransferEvent reversedEvent = TransferEvent.transferReversed(
                transfer.id(),
                transfer.fromAccountId(),
                transfer.toAccountId(),
                transfer.amount(),
                reason,
                reversalDebit.id(),
                reversalCredit.id()
            );

            return Result.success(new ReversalResult(
                reversed,
                updatedReceiver,
                updatedSender,
                reversalDebit,
                reversalCredit,
                List.of(reversedEvent)
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    // ── Cancel transfer ───────────────────────────────────────────────────────

    public static Result<Transfer> cancelTransfer(Transfer transfer, String reason) {
        try {
            if (!transfer.canBeProcessed()) {
                return Result.failure("Only pending transfers can be cancelled");
            }

            Transfer cancelled = transfer.fail("CANCELLED: " + reason);
            return Result.success(cancelled);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private static void validateAccounts(Account fromAccount, Account toAccount, Money amount) {
        if (!fromAccount.status().canTransact()) {
            throw new IllegalStateException(
                "Source account is not active: " + fromAccount.status()
            );
        }

        if (!toAccount.status().canDeposit()) {
            throw new IllegalStateException(
                "Target account cannot receive deposits: " + toAccount.status()
            );
        }

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new IllegalArgumentException(String.format(
                "Currency mismatch: %s vs %s",
                fromAccount.getCurrency(), toAccount.getCurrency()
            ));
        }

        if (!amount.currency().equals(fromAccount.getCurrency())) {
            throw new IllegalArgumentException(String.format(
                "Transfer currency %s does not match source account currency %s",
                amount.currency(), fromAccount.getCurrency()
            ));
        }

        if (!fromAccount.hasSufficientFunds(amount)) {
            throw new IllegalStateException(String.format(
                "Insufficient funds in source account. Balance: %s, Required: %s",
                fromAccount.balance().format(), amount.format()
            ));
        }
    }

    // ── Batch validation ──────────────────────────────────────────────────────

    public static Result<Boolean> validateBatchTransfer(
        java.util.List<Account> sourceAccounts,
        Account targetAccount,
        Money totalAmount
    ) {
        try {
            Money totalSourceBalance = sourceAccounts.stream()
                .map(Account::balance)
                .reduce(Money.zero(totalAmount.currency()), Money::add);

            if (!totalSourceBalance.isGreaterThanOrEqual(totalAmount)) {
                return Result.failure(String.format(
                    "Total source balance %s is insufficient for transfer amount %s",
                    totalSourceBalance.format(), totalAmount.format()
                ));
            }

            for (Account account : sourceAccounts) {
                if (!account.status().canTransact()) {
                    return Result.failure(String.format(
                        "Account %s is not active: %s",
                        account.id().value(), account.status()
                    ));
                }
            }

            if (!targetAccount.status().canDeposit()) {
                return Result.failure(String.format(
                    "Target account %s cannot receive deposits: %s",
                    targetAccount.id().value(), targetAccount.status()
                ));
            }

            return Result.success(true);

        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    public static String getTransferSummary(Transfer transfer) {
        return String.format(
            "Transfer %s: %s → %s, Amount: %s, Status: %s, Created: %s",
            transfer.id().value(),
            transfer.fromAccountId().value(),
            transfer.toAccountId().value(),
            transfer.amount().format(),
            transfer.status(),
            transfer.createdAt()
        );
    }
}
