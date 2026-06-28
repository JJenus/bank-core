package com.jjenus.bank.core.accounts;

import java.util.Currency;
import java.util.List;

public final class AccountFactory {

    private AccountFactory() {
        // Utility class
    }

    public static Account createNew(AccountId id, String ownerName, Currency currency) {
        return Account.create(id, ownerName, currency);
    }

    public static Account createFromCommand(AccountCommand.CreateAccount command) {
        Currency currency = Currency.getInstance(command.currencyCode());
        return Account.create(command.accountId(), command.ownerName(), currency);
    }

    public static Account reconstituteFromEvents(AccountId id, List<AccountEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Cannot reconstitute account from empty event list");
        }

        Account account = null;

        for (AccountEvent event : events) {
            account = applyEvent(account, event);
        }

        if (account == null) {
            throw new IllegalStateException("Failed to reconstitute account from events");
        }

        return account;
    }

    private static Account applyEvent(Account current, AccountEvent event) {
        return EventApplier.apply(current, event);
    }

    public static Account applyCommand(Account account, AccountCommand command) {
        return CommandApplier.apply(account, command);
    }

    // ── Event applier ─────────────────────────────────────────────────────────

    private static class EventApplier {
        static Account apply(Account current, AccountEvent event) {
            if (event instanceof AccountEvent.AccountCreated created) {
                return handleAccountCreated(created);
            } else if (event instanceof AccountEvent.MoneyDeposited deposited) {
                return handleMoneyDeposited(current, deposited);
            } else if (event instanceof AccountEvent.MoneyWithdrawn withdrawn) {
                return handleMoneyWithdrawn(current, withdrawn);
            } else if (event instanceof AccountEvent.AccountFrozen) {
                return handleAccountFrozen(current);
            } else if (event instanceof AccountEvent.AccountSuspended) {
                return handleAccountSuspended(current);
            } else if (event instanceof AccountEvent.AccountActivated) {
                return handleAccountActivated(current);
            } else if (event instanceof AccountEvent.AccountMarkedDormant) {
                return handleAccountMarkedDormant(current);
            } else if (event instanceof AccountEvent.AccountClosed) {
                return handleAccountClosed(current);
            }
            throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
        }

        private static Account handleAccountCreated(AccountEvent.AccountCreated created) {
            Currency currency = Currency.getInstance(created.currencyCode());
            return Account.create(created.accountId(), created.ownerName(), currency);
        }

        private static Account handleMoneyDeposited(Account current, AccountEvent.MoneyDeposited deposited) {
            validateAccount(current);
            return current.deposit(deposited.amount());
        }

        private static Account handleMoneyWithdrawn(Account current, AccountEvent.MoneyWithdrawn withdrawn) {
            validateAccount(current);
            return current.withdraw(withdrawn.amount());
        }

        private static Account handleAccountFrozen(Account current) {
            validateAccount(current);
            return current.freeze();
        }

        private static Account handleAccountSuspended(Account current) {
            validateAccount(current);
            return current.suspend();
        }

        private static Account handleAccountActivated(Account current) {
            validateAccount(current);
            return current.activate();
        }

        private static Account handleAccountMarkedDormant(Account current) {
            validateAccount(current);
            return current.markDormant();
        }

        private static Account handleAccountClosed(Account current) {
            validateAccount(current);
            return current.close();
        }

        private static void validateAccount(Account account) {
            if (account == null) {
                throw new IllegalStateException("Account not initialized");
            }
        }
    }

    // ── Command applier ───────────────────────────────────────────────────────

    private static class CommandApplier {
        static Account apply(Account account, AccountCommand command) {
            if (command instanceof AccountCommand.DepositMoney deposit) {
                return handleDeposit(account, deposit);
            } else if (command instanceof AccountCommand.WithdrawMoney withdrawal) {
                return handleWithdrawal(account, withdrawal);
            } else if (command instanceof AccountCommand.FreezeAccount) {
                return handleFreeze(account);
            } else if (command instanceof AccountCommand.SuspendAccount) {
                return handleSuspend(account);
            } else if (command instanceof AccountCommand.ActivateAccount) {
                return handleActivate(account);
            } else if (command instanceof AccountCommand.MarkAccountDormant) {
                return handleMarkDormant(account);
            } else if (command instanceof AccountCommand.CloseAccount) {
                return handleClose(account);
            }
            throw new IllegalArgumentException(
                    "Unsupported command for direct application: " + command.getClass().getSimpleName()
            );
        }

        private static Account handleDeposit(Account account, AccountCommand.DepositMoney deposit) {
            return account.deposit(deposit.amount());
        }

        private static Account handleWithdrawal(Account account, AccountCommand.WithdrawMoney withdrawal) {
            return account.withdraw(withdrawal.amount());
        }

        private static Account handleFreeze(Account account) {
            return account.freeze();
        }

        private static Account handleSuspend(Account account) {
            return account.suspend();
        }

        private static Account handleActivate(Account account) {
            return account.activate();
        }

        private static Account handleMarkDormant(Account account) {
            return account.markDormant();
        }

        private static Account handleClose(Account account) {
            return account.close();
        }
    }
}
