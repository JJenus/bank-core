package com.jjenus.bank.core.transfers;

import com.jjenus.bank.core.accounts.Account;
import com.jjenus.bank.core.shared.Money;
import com.jjenus.bank.core.shared.Result;
import com.jjenus.bank.core.transactions.Transaction;
import com.jjenus.bank.core.transactions.TransactionId;
import java.util.List;

public final class TransferService {

    private TransferService() {}

    public record TransferExecutionResult(
        Transfer transfer,
        Account updatedFromAccount,
        Account updatedToAccount,
        Transaction debitTransaction,
        Transaction creditTransaction,
        List<String> events
    ) {}

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

            // 3. Process withdrawal from source account
            Account updatedFrom = fromAccount.withdraw(command.amount());
            Transaction debitTransaction = Transaction.createTransferOut(
                TransactionId.generate(),
                command.fromAccountId(),
                command.amount(),
                updatedFrom.balance(),
                command.reference(),
                null  // Will be updated after credit transaction is created
            );

            // 4. Mark transfer as processing
            transfer = transfer.markProcessing(debitTransaction.id());

            // 5. Process deposit to target account
            Account updatedTo = toAccount.deposit(command.amount());
            Transaction creditTransaction = Transaction.createTransferIn(
                TransactionId.generate(),
                command.toAccountId(),
                command.amount(),
                updatedTo.balance(),
                command.reference(),
                debitTransaction.id().value()
            );

            // 6. Update debit transaction with related transaction ID
            debitTransaction = new Transaction(
                debitTransaction.id(),
                debitTransaction.accountId(),
                debitTransaction.type(),
                debitTransaction.amount(),
                debitTransaction.balanceAfter(),
                debitTransaction.description(),
                debitTransaction.reference(),
                debitTransaction.timestamp(),
                creditTransaction.id().value(),  // Link to credit transaction
                debitTransaction.metadata()
            );

            // 7. Complete the transfer
            transfer = transfer.complete(creditTransaction.id());

            // 8. Collect events
            List<String> events = List.of(
                "Transfer initiated: " + command.transferId().value(),
                "Debit transaction created: " + debitTransaction.id().value(),
                "Credit transaction created: " + creditTransaction.id().value(),
                "Transfer completed successfully"
            );

            return Result.success(new TransferExecutionResult(
                transfer,
                updatedFrom,
                updatedTo,
                debitTransaction,
                creditTransaction,
                events
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    public static Result<Transfer> reverseTransfer(
        Transfer transfer,
        Account fromAccount,  // The account that received the money (to be debited back)
        Account toAccount,    // The account that sent the money (to be credited back)
        String reason
    ) {
        try {
            if (!transfer.isCompleted()) {
                return Result.failure("Only completed transfers can be reversed");
            }

            if (transfer.amount().isZero()) {
                return Result.failure("Cannot reverse zero-amount transfer");
            }

            // Reverse the transfer
            Transfer reversed = transfer.reverse(reason);

            return Result.success(reversed);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

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

    // Validation methods
    private static void validateAccounts(Account fromAccount, Account toAccount, Money amount) {
        // Check if accounts are active
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

        // Check currency compatibility
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s",
                    fromAccount.getCurrency(), toAccount.getCurrency())
            );
        }

        if (!amount.currency().equals(fromAccount.getCurrency())) {
            throw new IllegalArgumentException(
                String.format("Transfer currency %s does not match source account currency %s",
                    amount.currency(), fromAccount.getCurrency())
            );
        }

        // Check sufficient funds
        if (!fromAccount.hasSufficientFunds(amount)) {
            throw new IllegalStateException(
                String.format("Insufficient funds in source account. Balance: %s, Required: %s",
                    fromAccount.balance().format(), amount.format())
            );
        }

        // Check transfer limits (example: max $10,000 per transfer) not realistic right now
//        Money maxTransfer = Money.of("10000.00", amount.currency());
//        if (amount.isGreaterThan(maxTransfer)) {
//            throw new IllegalArgumentException(
//                String.format("Transfer amount %s exceeds maximum limit of %s",
//                    amount.format(), maxTransfer.format())
//            );
//        }
    }

    // Batch transfer validation
    public static Result<Boolean> validateBatchTransfer(
        List<Account> sourceAccounts,
        Account targetAccount,
        Money totalAmount
    ) {
        try {
            // Check total amount
            Money totalSourceBalance = sourceAccounts.stream()
                .map(Account::balance)
                .reduce(Money.zero(totalAmount.currency()), Money::add);

            if (!totalSourceBalance.isGreaterThanOrEqual(totalAmount)) {
                return Result.failure(
                    String.format("Total source balance %s is insufficient for transfer amount %s",
                        totalSourceBalance.format(), totalAmount.format())
                );
            }

            // Check all accounts are active
            for (Account account : sourceAccounts) {
                if (!account.status().canTransact()) {
                    return Result.failure(
                        String.format("Account %s is not active: %s",
                            account.id().value(), account.status())
                    );
                }
            }

            if (!targetAccount.status().canDeposit()) {
                return Result.failure(
                    String.format("Target account %s cannot receive deposits: %s",
                        targetAccount.id().value(), targetAccount.status())
                );
            }

            return Result.success(true);

        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }
    }

    // Utility method to get transfer status
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
