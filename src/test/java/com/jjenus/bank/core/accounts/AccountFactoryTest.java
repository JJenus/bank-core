package com.jjenus.bank.core.accounts;

import com.jjenus.bank.core.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.Currency;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AccountFactoryTest {
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    @DisplayName("Create new account")
    void createNew() {
        AccountId id = AccountId.generate();
        Account account = AccountFactory.createNew(id, "John Doe", USD);

        assertEquals(id, account.id());
        assertEquals("John Doe", account.customerId());
        assertEquals(0, account.balance().amount().compareTo(BigDecimal.ZERO));
        assertEquals(AccountStatus.ACTIVE, account.status());
    }

    @Test
    @DisplayName("Create from command")
    void createFromCommand() {
        AccountId id = AccountId.generate();
        AccountCommand.CreateAccount command = new AccountCommand.CreateAccount(
            id, "Jane Smith", "USD", java.time.Instant.now()
        );

        Account account = AccountFactory.createFromCommand(command);

        assertEquals(id, account.id());
        assertEquals("Jane Smith", account.customerId());
        assertEquals(0, account.balance().amount().compareTo(BigDecimal.ZERO));
        assertEquals(AccountStatus.ACTIVE, account.status());
    }

    @Test
    @DisplayName("Reconstitute from events - account created only")
    void reconstituteFromEvents_accountCreated() {
        AccountId id = AccountId.generate();
        List<AccountEvent> events = new ArrayList<>();
        events.add(AccountEvent.accountCreated(id, "John Doe", "USD"));

        Account account = AccountFactory.reconstituteFromEvents(id, events);

        assertEquals(id, account.id());
        assertEquals("John Doe", account.customerId());
        assertEquals(0, account.balance().amount().compareTo(BigDecimal.ZERO));
        assertEquals(AccountStatus.ACTIVE, account.status());
    }

    @Test
    @DisplayName("Reconstitute from events - with deposits and withdrawals")
    void reconstituteFromEvents_withTransactions() {
        AccountId id = AccountId.generate();
        List<AccountEvent> events = new ArrayList<>();
        events.add(AccountEvent.accountCreated(id, "John Doe", "USD"));
        events.add(AccountEvent.moneyDeposited(id, Money.of("200.00", USD), "DEP001"));
        events.add(AccountEvent.moneyWithdrawn(id, Money.of("50.00", USD), "WITH001"));
        events.add(AccountEvent.moneyDeposited(id, Money.of("100.00", USD), "DEP002"));

        Account account = AccountFactory.reconstituteFromEvents(id, events);

        assertEquals(id, account.id());
        assertEquals("John Doe", account.customerId());
        assertEquals(0, account.balance().amount().compareTo(new BigDecimal("250.0000")));
        assertEquals(AccountStatus.ACTIVE, account.status());
    }

    @Test
    @DisplayName("Reconstitute from events - account frozen and activated via factory method")
    void reconstituteFromEvents_statusChanges() {
        AccountId id = AccountId.generate();
        List<AccountEvent> events = new ArrayList<>();
        events.add(AccountEvent.accountCreated(id, "John Doe", "USD"));
        events.add(AccountEvent.accountFrozen(id, "Suspicious activity"));
        events.add(AccountEvent.accountActivated(id));  // Use factory method, not manual construction

        Account account = AccountFactory.reconstituteFromEvents(id, events);

        assertEquals(id, account.id());
        assertEquals(AccountStatus.ACTIVE, account.status());
    }

    @Test
    @DisplayName("Reconstitute from events - account suspended and reactivated")
    void reconstituteFromEvents_suspendAndActivate() {
        AccountId id = AccountId.generate();
        List<AccountEvent> events = new ArrayList<>();
        events.add(AccountEvent.accountCreated(id, "John Doe", "USD"));
        events.add(AccountEvent.accountSuspended(id, "Regulatory hold"));
        events.add(AccountEvent.accountActivated(id));

        Account account = AccountFactory.reconstituteFromEvents(id, events);

        assertEquals(AccountStatus.ACTIVE, account.status());
    }

    @Test
    @DisplayName("Reconstitute from events - account marked dormant")
    void reconstituteFromEvents_markDormant() {
        AccountId id = AccountId.generate();
        List<AccountEvent> events = new ArrayList<>();
        events.add(AccountEvent.accountCreated(id, "John Doe", "USD"));
        events.add(AccountEvent.accountMarkedDormant(id));

        Account account = AccountFactory.reconstituteFromEvents(id, events);

        assertEquals(AccountStatus.DORMANT, account.status());
    }

    @Test
    @DisplayName("Reconstitute from events - dormant account can receive deposit")
    void reconstituteFromEvents_dormantReceivesDeposit() {
        AccountId id = AccountId.generate();
        List<AccountEvent> events = new ArrayList<>();
        events.add(AccountEvent.accountCreated(id, "John Doe", "USD"));
        events.add(AccountEvent.accountMarkedDormant(id));
        events.add(AccountEvent.moneyDeposited(id, Money.of("100.00", USD), "DEP001"));

        Account account = AccountFactory.reconstituteFromEvents(id, events);

        assertEquals(AccountStatus.DORMANT, account.status());
        assertEquals(0, account.balance().amount().compareTo(new BigDecimal("100.0000")));
    }

    @Test
    @DisplayName("Reconstitute from events - account closed")
    void reconstituteFromEvents_accountClosed() {
        AccountId id = AccountId.generate();
        List<AccountEvent> events = new ArrayList<>();
        events.add(AccountEvent.accountCreated(id, "John Doe", "USD"));
        events.add(AccountEvent.accountClosed(id, "Customer request"));

        Account account = AccountFactory.reconstituteFromEvents(id, events);

        assertEquals(id, account.id());
        assertEquals(AccountStatus.CLOSED, account.status());
    }

    @Test
    @DisplayName("Reconstitute from empty events throws exception")
    void reconstituteFromEvents_emptyList_throwsException() {
        AccountId id = AccountId.generate();
        List<AccountEvent> events = new ArrayList<>();

        assertThrows(IllegalArgumentException.class,
            () -> AccountFactory.reconstituteFromEvents(id, events));
    }

    @Test
    @DisplayName("Apply deposit command")
    void applyCommand_deposit() {
        Account account = Account.create(AccountId.generate(), "John Doe", USD);
        AccountCommand.DepositMoney command = new AccountCommand.DepositMoney(
            account.id(), Money.of("100.00", USD), "DEP001", java.time.Instant.now()
        );

        Account updated = AccountFactory.applyCommand(account, command);

        assertEquals(0, updated.balance().amount().compareTo(new BigDecimal("100.0000")));
    }

    @Test
    @DisplayName("Apply withdrawal command")
    void applyCommand_withdrawal() {
        Account account = Account.create(AccountId.generate(), "John Doe", USD)
            .deposit(Money.of("200.00", USD));
        AccountCommand.WithdrawMoney command = new AccountCommand.WithdrawMoney(
            account.id(), Money.of("50.00", USD), "WITH001", java.time.Instant.now()
        );

        Account updated = AccountFactory.applyCommand(account, command);

        assertEquals(0, updated.balance().amount().compareTo(new BigDecimal("150.0000")));
    }

    @Test
    @DisplayName("Apply freeze command")
    void applyCommand_freeze() {
        Account account = Account.create(AccountId.generate(), "John Doe", USD);
        AccountCommand.FreezeAccount command = new AccountCommand.FreezeAccount(
            account.id(), "Suspicious activity", java.time.Instant.now()
        );

        Account updated = AccountFactory.applyCommand(account, command);

        assertEquals(AccountStatus.FROZEN, updated.status());
    }

    @Test
    @DisplayName("Apply suspend command")
    void applyCommand_suspend() {
        Account account = Account.create(AccountId.generate(), "John Doe", USD);
        AccountCommand.SuspendAccount command = AccountCommand.SuspendAccount.now(
            account.id(), "Regulatory hold"
        );

        Account updated = AccountFactory.applyCommand(account, command);

        assertEquals(AccountStatus.SUSPENDED, updated.status());
    }

    @Test
    @DisplayName("Apply activate command")
    void applyCommand_activate() {
        Account account = Account.create(AccountId.generate(), "John Doe", USD).freeze();
        AccountCommand.ActivateAccount command = AccountCommand.ActivateAccount.now(account.id());

        Account updated = AccountFactory.applyCommand(account, command);

        assertEquals(AccountStatus.ACTIVE, updated.status());
    }

    @Test
    @DisplayName("Apply mark dormant command")
    void applyCommand_markDormant() {
        Account account = Account.create(AccountId.generate(), "John Doe", USD);
        AccountCommand.MarkAccountDormant command = AccountCommand.MarkAccountDormant.now(account.id());

        Account updated = AccountFactory.applyCommand(account, command);

        assertEquals(AccountStatus.DORMANT, updated.status());
    }

    @Test
    @DisplayName("Apply close command")
    void applyCommand_close() {
        Account account = Account.create(AccountId.generate(), "John Doe", USD);
        AccountCommand.CloseAccount command = new AccountCommand.CloseAccount(
            account.id(), "Customer request", java.time.Instant.now()
        );

        Account updated = AccountFactory.applyCommand(account, command);

        assertEquals(AccountStatus.CLOSED, updated.status());
    }

    @Test
    @DisplayName("Apply unsupported command throws exception")
    void applyCommand_unsupported_throwsException() {
        Account account = Account.create(AccountId.generate(), "John Doe", USD);
        AccountCommand.CreateAccount command = new AccountCommand.CreateAccount(
            AccountId.generate(), "Jane", "USD", java.time.Instant.now()
        );

        assertThrows(IllegalArgumentException.class,
            () -> AccountFactory.applyCommand(account, command));
    }
}
