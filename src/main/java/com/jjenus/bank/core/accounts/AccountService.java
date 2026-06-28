package com.jjenus.bank.core.accounts;

import com.jjenus.bank.core.shared.Result;
import com.jjenus.bank.core.transactions.Transaction;
import com.jjenus.bank.core.transactions.TransactionId;

/**
 * Application service / command handler for account operations.
 *
 * <p>Mirrors the pattern established by {@code TransferService}: takes a command,
 * orchestrates domain objects, and returns a rich result record that includes the
 * updated account, the resulting event, and — where applicable — the transaction
 * record for the ledger.
 *
 * <p>All methods are stateless and pure. No persistence or event publishing happens
 * here; that is the responsibility of the calling application layer.
 */
public final class AccountService {

    private AccountService() {}

    // ── Result records ────────────────────────────────────────────────────────

    public record DepositResult(
        Account updatedAccount,
        AccountEvent.MoneyDeposited event,
        Transaction transaction
    ) {}

    public record WithdrawalResult(
        Account updatedAccount,
        AccountEvent.MoneyWithdrawn event,
        Transaction transaction
    ) {}

    public record AccountStatusChangeResult(
        Account updatedAccount,
        AccountEvent event
    ) {}

    public record AccountCreationResult(
        Account account,
        AccountEvent.AccountCreated event
    ) {}

    // ── Command handlers ──────────────────────────────────────────────────────

    public static Result<AccountCreationResult> createAccount(AccountCommand.CreateAccount command) {
        try {
            Account account = AccountFactory.createFromCommand(command);
            AccountEvent.AccountCreated event = AccountEvent.accountCreated(
                account.id(),
                account.customerId(),
                account.getCurrency().getCurrencyCode()
            );
            return Result.success(new AccountCreationResult(account, event));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    public static Result<DepositResult> deposit(Account account, AccountCommand.DepositMoney command) {
        try {
            Account updated = account.deposit(command.amount());
            AccountEvent.MoneyDeposited event = AccountEvent.moneyDeposited(
                account.id(), command.amount(), command.reference()
            );
            Transaction transaction = Transaction.createDeposit(
                TransactionId.generate(),
                account.id(),
                command.amount(),
                updated.balance(),
                command.reference()
            );
            return Result.success(new DepositResult(updated, event, transaction));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    public static Result<WithdrawalResult> withdraw(Account account, AccountCommand.WithdrawMoney command) {
        try {
            Account updated = account.withdraw(command.amount());
            AccountEvent.MoneyWithdrawn event = AccountEvent.moneyWithdrawn(
                account.id(), command.amount(), command.reference()
            );
            Transaction transaction = Transaction.createWithdrawal(
                TransactionId.generate(),
                account.id(),
                command.amount(),
                updated.balance(),
                command.reference()
            );
            return Result.success(new WithdrawalResult(updated, event, transaction));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    public static Result<AccountStatusChangeResult> freeze(Account account, AccountCommand.FreezeAccount command) {
        try {
            Account updated = account.freeze();
            AccountEvent.AccountFrozen event = AccountEvent.accountFrozen(account.id(), command.reason());
            return Result.success(new AccountStatusChangeResult(updated, event));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    public static Result<AccountStatusChangeResult> suspend(Account account, AccountCommand.SuspendAccount command) {
        try {
            Account updated = account.suspend();
            AccountEvent.AccountSuspended event = AccountEvent.accountSuspended(account.id(), command.reason());
            return Result.success(new AccountStatusChangeResult(updated, event));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    public static Result<AccountStatusChangeResult> activate(Account account, AccountCommand.ActivateAccount command) {
        try {
            Account updated = account.activate();
            AccountEvent event = AccountEvent.accountActivated(account.id());
            return Result.success(new AccountStatusChangeResult(updated, event));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    public static Result<AccountStatusChangeResult> markDormant(Account account, AccountCommand.MarkAccountDormant command) {
        try {
            Account updated = account.markDormant();
            AccountEvent event = AccountEvent.accountMarkedDormant(account.id());
            return Result.success(new AccountStatusChangeResult(updated, event));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }

    public static Result<AccountStatusChangeResult> closeAccount(Account account, AccountCommand.CloseAccount command) {
        try {
            Account updated = account.close();
            AccountEvent event = AccountEvent.accountClosed(account.id(), command.reason());
            return Result.success(new AccountStatusChangeResult(updated, event));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.failure(e.getMessage());
        }
    }
}
