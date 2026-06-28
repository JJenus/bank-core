package com.jjenus.bank.core.accounts;

import com.jjenus.bank.core.shared.Money;
import com.jjenus.bank.core.shared.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {
    private static final Currency USD = Currency.getInstance("USD");
    private Account account;
    private AccountId accountId;

    @BeforeEach
    void setUp() {
        accountId = AccountId.generate();
        account = Account.create(accountId, "John Doe", USD);
    }

    @Test
    @DisplayName("createAccount returns account, event, and success result")
    void createAccount_success() {
        AccountCommand.CreateAccount command = AccountCommand.CreateAccount.now(
            AccountId.generate(), "Jane Smith", "USD"
        );

        Result<AccountService.AccountCreationResult> result = AccountService.createAccount(command);

        assertTrue(result.isSuccess());
        AccountService.AccountCreationResult creation = result.getOrThrow();
        assertNotNull(creation.account());
        assertNotNull(creation.event());
        assertEquals("Jane Smith", creation.account().customerId());
        assertEquals(AccountStatus.ACTIVE, creation.account().status());
        assertEquals(creation.account().id(), creation.event().accountId());
    }

    @Test
    @DisplayName("deposit returns updated account, event, and transaction")
    void deposit_success() {
        Money amount = Money.of("250.00", USD);
        AccountCommand.DepositMoney command = AccountCommand.DepositMoney.now(
            accountId, amount, "DEP-001"
        );

        Result<AccountService.DepositResult> result = AccountService.deposit(account, command);

        assertTrue(result.isSuccess());
        AccountService.DepositResult deposit = result.getOrThrow();

        assertEquals(0, deposit.updatedAccount().balance().amount()
            .compareTo(new BigDecimal("250.0000")));
        assertEquals("DEP-001", deposit.event().reference());
        assertEquals("DEP-001", deposit.transaction().reference());
        assertEquals(amount, deposit.event().amount());
    }

    @Test
    @DisplayName("deposit to frozen account returns failure")
    void deposit_frozenAccount_returnsFailure() {
        Account frozen = account.freeze();
        AccountCommand.DepositMoney command = AccountCommand.DepositMoney.now(
            accountId, Money.of("100.00", USD), "DEP-002"
        );

        Result<AccountService.DepositResult> result = AccountService.deposit(frozen, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("FROZEN"));
    }

    @Test
    @DisplayName("deposit to DORMANT account succeeds")
    void deposit_dormantAccount_succeeds() {
        Account dormant = account.deposit(Money.of("50.00", USD)).markDormant();
        AccountCommand.DepositMoney command = AccountCommand.DepositMoney.now(
            accountId, Money.of("100.00", USD), "DEP-003"
        );

        Result<AccountService.DepositResult> result = AccountService.deposit(dormant, command);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getOrThrow().updatedAccount().balance().amount()
            .compareTo(new BigDecimal("150.0000")));
    }

    @Test
    @DisplayName("withdraw returns updated account, event, and transaction")
    void withdraw_success() {
        Account withBalance = account.deposit(Money.of("500.00", USD));
        Money amount = Money.of("150.00", USD);
        AccountCommand.WithdrawMoney command = AccountCommand.WithdrawMoney.now(
            accountId, amount, "WITH-001"
        );

        Result<AccountService.WithdrawalResult> result = AccountService.withdraw(withBalance, command);

        assertTrue(result.isSuccess());
        AccountService.WithdrawalResult withdrawal = result.getOrThrow();

        assertEquals(0, withdrawal.updatedAccount().balance().amount()
            .compareTo(new BigDecimal("350.0000")));
        assertEquals("WITH-001", withdrawal.event().reference());
        assertTrue(withdrawal.transaction().amount().isNegative());
    }

    @Test
    @DisplayName("withdraw insufficient funds returns failure")
    void withdraw_insufficientFunds_returnsFailure() {
        AccountCommand.WithdrawMoney command = AccountCommand.WithdrawMoney.now(
            accountId, Money.of("100.00", USD), "WITH-002"
        );

        Result<AccountService.WithdrawalResult> result = AccountService.withdraw(account, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("freeze returns updated account and AccountFrozen event")
    void freeze_success() {
        AccountCommand.FreezeAccount command = AccountCommand.FreezeAccount.now(
            accountId, "Suspicious activity"
        );

        Result<AccountService.AccountStatusChangeResult> result = AccountService.freeze(account, command);

        assertTrue(result.isSuccess());
        AccountService.AccountStatusChangeResult change = result.getOrThrow();
        assertEquals(AccountStatus.FROZEN, change.updatedAccount().status());
        assertInstanceOf(AccountEvent.AccountFrozen.class, change.event());
    }

    @Test
    @DisplayName("freeze closed account returns failure")
    void freeze_closedAccount_returnsFailure() {
        Account closed = account.close();
        AccountCommand.FreezeAccount command = AccountCommand.FreezeAccount.now(
            accountId, "Attempt to freeze closed account"
        );

        Result<AccountService.AccountStatusChangeResult> result = AccountService.freeze(closed, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("CLOSED"));
    }

    @Test
    @DisplayName("suspend returns updated account and AccountSuspended event")
    void suspend_success() {
        AccountCommand.SuspendAccount command = AccountCommand.SuspendAccount.now(
            accountId, "Regulatory hold"
        );

        Result<AccountService.AccountStatusChangeResult> result = AccountService.suspend(account, command);

        assertTrue(result.isSuccess());
        assertEquals(AccountStatus.SUSPENDED, result.getOrThrow().updatedAccount().status());
        assertInstanceOf(AccountEvent.AccountSuspended.class, result.getOrThrow().event());
    }

    @Test
    @DisplayName("activate frozen account returns ACTIVE")
    void activate_frozenAccount() {
        Account frozen = account.freeze();
        AccountCommand.ActivateAccount command = AccountCommand.ActivateAccount.now(accountId);

        Result<AccountService.AccountStatusChangeResult> result = AccountService.activate(frozen, command);

        assertTrue(result.isSuccess());
        assertEquals(AccountStatus.ACTIVE, result.getOrThrow().updatedAccount().status());
        assertInstanceOf(AccountEvent.AccountActivated.class, result.getOrThrow().event());
    }

    @Test
    @DisplayName("activate suspended account returns ACTIVE")
    void activate_suspendedAccount() {
        Account suspended = account.suspend();
        AccountCommand.ActivateAccount command = AccountCommand.ActivateAccount.now(accountId);

        Result<AccountService.AccountStatusChangeResult> result = AccountService.activate(suspended, command);

        assertTrue(result.isSuccess());
        assertEquals(AccountStatus.ACTIVE, result.getOrThrow().updatedAccount().status());
    }

    @Test
    @DisplayName("activate dormant account returns ACTIVE")
    void activate_dormantAccount() {
        Account dormant = account.markDormant();
        AccountCommand.ActivateAccount command = AccountCommand.ActivateAccount.now(accountId);

        Result<AccountService.AccountStatusChangeResult> result = AccountService.activate(dormant, command);

        assertTrue(result.isSuccess());
        assertEquals(AccountStatus.ACTIVE, result.getOrThrow().updatedAccount().status());
    }

    @Test
    @DisplayName("activate closed account returns failure")
    void activate_closedAccount_returnsFailure() {
        Account closed = account.close();
        AccountCommand.ActivateAccount command = AccountCommand.ActivateAccount.now(accountId);

        Result<AccountService.AccountStatusChangeResult> result = AccountService.activate(closed, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("CLOSED"));
    }

    @Test
    @DisplayName("markDormant returns DORMANT account and event")
    void markDormant_success() {
        AccountCommand.MarkAccountDormant command = AccountCommand.MarkAccountDormant.now(accountId);

        Result<AccountService.AccountStatusChangeResult> result = AccountService.markDormant(account, command);

        assertTrue(result.isSuccess());
        assertEquals(AccountStatus.DORMANT, result.getOrThrow().updatedAccount().status());
        assertInstanceOf(AccountEvent.AccountMarkedDormant.class, result.getOrThrow().event());
    }

    @Test
    @DisplayName("closeAccount with zero balance returns CLOSED account and event")
    void closeAccount_zeroBalance_success() {
        AccountCommand.CloseAccount command = AccountCommand.CloseAccount.now(
            accountId, "Customer request"
        );

        Result<AccountService.AccountStatusChangeResult> result = AccountService.closeAccount(account, command);

        assertTrue(result.isSuccess());
        assertEquals(AccountStatus.CLOSED, result.getOrThrow().updatedAccount().status());
        assertInstanceOf(AccountEvent.AccountClosed.class, result.getOrThrow().event());
    }

    @Test
    @DisplayName("closeAccount with non-zero balance returns failure")
    void closeAccount_nonZeroBalance_returnsFailure() {
        Account withBalance = account.deposit(Money.of("100.00", USD));
        AccountCommand.CloseAccount command = AccountCommand.CloseAccount.now(
            accountId, "Customer request"
        );

        Result<AccountService.AccountStatusChangeResult> result = AccountService.closeAccount(withBalance, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("non-zero balance"));
    }

    @Test
    @DisplayName("deposit result contains correct balance after in transaction")
    void deposit_transactionBalanceAfterIsCorrect() {
        Account withSomeMoney = account.deposit(Money.of("100.00", USD));
        AccountCommand.DepositMoney command = AccountCommand.DepositMoney.now(
            accountId, Money.of("50.00", USD), "DEP-BAL"
        );

        Result<AccountService.DepositResult> result = AccountService.deposit(withSomeMoney, command);

        assertTrue(result.isSuccess());
        // balanceAfter on the transaction should be 100 + 50 = 150
        assertEquals(0, result.getOrThrow().transaction().balanceAfter().amount()
            .compareTo(new BigDecimal("150.0000")));
    }
}
