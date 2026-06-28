package com.jjenus.bank.core.accounts;

import com.jjenus.bank.core.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    private static final Currency USD = Currency.getInstance("USD");
    private Account account;
    private AccountId accountId;

    @BeforeEach
    void setUp() {
        accountId = AccountId.generate();
        account = Account.create(accountId, "John Doe", USD);
    }

    @Test
    @DisplayName("Account creation with valid parameters")
    void createAccount_validParameters() {
        assertEquals(accountId, account.id());
        assertEquals("John Doe", account.customerId());
        assertEquals(0, account.balance().amount().compareTo(BigDecimal.ZERO));
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertNotNull(account.createdAt());
        assertNotNull(account.lastUpdatedAt());
        assertEquals(0L, account.version());
    }

    @Test
    @DisplayName("Deposit money to account")
    void deposit_money() {
        Money depositAmount = Money.of("100.00", USD);
        Account updated = account.deposit(depositAmount);

        assertEquals(0, updated.balance().amount().compareTo(depositAmount.amount()));
        assertEquals(AccountStatus.ACTIVE, updated.status());
        assertEquals(1L, updated.version());
    }

    @Test
    @DisplayName("Deposit negative amount throws exception")
    void deposit_negativeAmount_throwsException() {
        Money negativeAmount = Money.of("-50.00", USD);

        assertThrows(IllegalArgumentException.class, () -> account.deposit(negativeAmount));
    }

    @Test
    @DisplayName("Deposit zero amount throws exception")
    void deposit_zeroAmount_throwsException() {
        Money zeroAmount = Money.zero(USD);

        assertThrows(IllegalArgumentException.class, () -> account.deposit(zeroAmount));
    }

    @Test
    @DisplayName("Deposit to frozen account throws exception")
    void deposit_toFrozenAccount_throwsException() {
        Account frozenAccount = account.freeze();
        Money depositAmount = Money.of("100.00", USD);

        assertThrows(IllegalStateException.class, () -> frozenAccount.deposit(depositAmount));
    }

    @Test
    @DisplayName("Deposit to suspended account throws exception")
    void deposit_toSuspendedAccount_throwsException() {
        Account suspended = account.suspend();
        Money depositAmount = Money.of("100.00", USD);

        assertThrows(IllegalStateException.class, () -> suspended.deposit(depositAmount));
    }

    @Test
    @DisplayName("Deposit to DORMANT account succeeds")
    void deposit_toDormantAccount_succeeds() {
        Account dormant = account.deposit(Money.of("50.00", USD)).markDormant();
        Money depositAmount = Money.of("100.00", USD);

        Account updated = dormant.deposit(depositAmount);

        assertEquals(0, updated.balance().amount().compareTo(new BigDecimal("150.0000")));
        assertEquals(AccountStatus.DORMANT, updated.status());  // Status unchanged by deposit; caller emits AccountActivated event separately
    }

    @Test
    @DisplayName("Withdraw money from account")
    void withdraw_money() {
        // First deposit
        Account withBalance = account.deposit(Money.of("200.00", USD));

        // Then withdraw
        Account updated = withBalance.withdraw(Money.of("100.00", USD));

        assertEquals(0, updated.balance().amount().compareTo(new BigDecimal("100.0000")));
        assertEquals(2L, updated.version());
    }

    @Test
    @DisplayName("Withdraw insufficient funds throws exception")
    void withdraw_insufficientFunds_throwsException() {
        Account withBalance = account.deposit(Money.of("50.00", USD));

        assertThrows(IllegalStateException.class,
            () -> withBalance.withdraw(Money.of("100.00", USD)));
    }

    @Test
    @DisplayName("Withdraw from frozen account throws exception")
    void withdraw_fromFrozenAccount_throwsException() {
        Account withBalance = account.deposit(Money.of("200.00", USD));
        Account frozen = withBalance.freeze();

        assertThrows(IllegalStateException.class,
            () -> frozen.withdraw(Money.of("100.00", USD)));
    }

    @Test
    @DisplayName("Withdraw from DORMANT account throws exception")
    void withdraw_fromDormantAccount_throwsException() {
        Account dormant = account.deposit(Money.of("200.00", USD)).markDormant();

        assertThrows(IllegalStateException.class,
            () -> dormant.withdraw(Money.of("50.00", USD)));
    }

    @Test
    @DisplayName("Freeze active account")
    void freeze_account() {
        Account frozen = account.freeze();

        assertEquals(AccountStatus.FROZEN, frozen.status());
        assertEquals(1L, frozen.version());
    }

    @Test
    @DisplayName("Freeze already frozen account returns same")
    void freeze_alreadyFrozen() {
        Account frozen = account.freeze();
        Account refrozen = frozen.freeze();

        assertEquals(frozen, refrozen);
    }

    @Test
    @DisplayName("Suspend active account")
    void suspend_account() {
        Account suspended = account.suspend();

        assertEquals(AccountStatus.SUSPENDED, suspended.status());
        assertEquals(1L, suspended.version());
    }

    @Test
    @DisplayName("Suspend already suspended account returns same")
    void suspend_alreadySuspended() {
        Account suspended = account.suspend();
        Account reSuspended = suspended.suspend();

        assertEquals(suspended, reSuspended);
    }

    @Test
    @DisplayName("Suspend closed account throws exception")
    void suspend_closedAccount_throwsException() {
        Account closed = account.close();

        assertThrows(IllegalStateException.class, closed::suspend);
    }

    @Test
    @DisplayName("Activate account")
    void activate_account() {
        Account frozen = account.freeze();
        Account activated = frozen.activate();

        assertEquals(AccountStatus.ACTIVE, activated.status());
        assertEquals(2L, activated.version());
    }

    @Test
    @DisplayName("Activate suspended account")
    void activate_suspendedAccount() {
        Account suspended = account.suspend();
        Account activated = suspended.activate();

        assertEquals(AccountStatus.ACTIVE, activated.status());
    }

    @Test
    @DisplayName("Activate dormant account")
    void activate_dormantAccount() {
        Account dormant = account.markDormant();
        Account activated = dormant.activate();

        assertEquals(AccountStatus.ACTIVE, activated.status());
    }

    @Test
    @DisplayName("Activate already active account returns same")
    void activate_alreadyActive() {
        Account activated = account.activate();

        assertEquals(account, activated);
    }

    @Test
    @DisplayName("Mark account dormant")
    void markDormant_account() {
        Account dormant = account.markDormant();

        assertEquals(AccountStatus.DORMANT, dormant.status());
        assertEquals(1L, dormant.version());
    }

    @Test
    @DisplayName("Mark already dormant account returns same")
    void markDormant_alreadyDormant() {
        Account dormant = account.markDormant();
        Account reDormant = dormant.markDormant();

        assertEquals(dormant, reDormant);
    }

    @Test
    @DisplayName("Close account with zero balance")
    void close_account_withZeroBalance() {
        Account closed = account.close();

        assertEquals(AccountStatus.CLOSED, closed.status());
        assertEquals(1L, closed.version());
    }

    @Test
    @DisplayName("Close account with non-zero balance throws exception")
    void close_account_withNonZeroBalance_throwsException() {
        Account withBalance = account.deposit(Money.of("100.00", USD));

        assertThrows(IllegalStateException.class, withBalance::close);
    }

    @Test
    @DisplayName("Has sufficient funds")
    void hasSufficientFunds() {
        Account withBalance = account.deposit(Money.of("200.00", USD));

        assertTrue(withBalance.hasSufficientFunds(Money.of("100.00", USD)));
        assertTrue(withBalance.hasSufficientFunds(Money.of("200.00", USD)));
        assertFalse(withBalance.hasSufficientFunds(Money.of("300.00", USD)));
    }

    @Test
    @DisplayName("Has sufficient funds with different currency throws exception")
    void hasSufficientFunds_differentCurrency_throwsException() {
        Account withBalance = account.deposit(Money.of("200.00", USD));
        Money eurAmount = Money.of("100.00", Currency.getInstance("EUR"));

        assertThrows(IllegalArgumentException.class,
            () -> withBalance.hasSufficientFunds(eurAmount));
    }

    @Test
    @DisplayName("Is overdrawn - always false with current design")
    void isOverdrawn() {
        Account withBalance = account.deposit(Money.of("100.00", USD));
        Account exactlyZero = withBalance.withdraw(Money.of("100.00", USD));

        assertFalse(withBalance.isOverdrawn());
        assertFalse(account.isOverdrawn());
        assertFalse(exactlyZero.isOverdrawn());
    }

    @Test
    @DisplayName("Has positive balance")
    void hasPositiveBalance() {
        Account withBalance = account.deposit(Money.of("100.00", USD));

        assertTrue(withBalance.hasPositiveBalance());
        assertFalse(account.hasPositiveBalance());
    }

    @Test
    @DisplayName("Get currency")
    void getCurrency() {
        assertEquals(USD, account.getCurrency());
    }

    @Test
    @DisplayName("ToString contains relevant information")
    void toString_containsRelevantInfo() {
        String str = account.toString();

        assertTrue(str.contains(accountId.value()));
        assertTrue(str.contains("John Doe"));
        // Money.format() uses getCurrencyCode() — "USD 0.00", not "$ 0.00"
        assertTrue(str.contains("USD"));
        assertTrue(str.contains("0.00"));
        assertTrue(str.contains("ACTIVE"));
    }

    @Test
    @DisplayName("Account with null ID throws exception")
    void createAccount_withNullId_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new Account(null, "Owner", Money.zero(USD), AccountStatus.ACTIVE,
                java.time.Instant.now(), java.time.Instant.now(), 0L));
    }

    @Test
    @DisplayName("Account with blank owner name throws exception")
    void createAccount_withBlankOwnerName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new Account(AccountId.generate(), "", Money.zero(USD), AccountStatus.ACTIVE,
                java.time.Instant.now(), java.time.Instant.now(), 0L));
    }
}
