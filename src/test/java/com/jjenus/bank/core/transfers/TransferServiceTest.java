package com.jjenus.bank.core.transfers;

import com.jjenus.bank.core.accounts.Account;
import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import com.jjenus.bank.core.shared.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.util.Currency;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {
    private static final Currency USD = Currency.getInstance("USD");
    private Account fromAccount;
    private Account toAccount;
    private AccountId fromId;
    private AccountId toId;

    @BeforeEach
    void setUp() {
        fromId = AccountId.generate();
        toId = AccountId.generate();
        fromAccount = Account.create(fromId, "John Doe", USD)
                .deposit(Money.of("1000.00", USD));
        toAccount = Account.create(toId, "Jane Smith", USD)
                .deposit(Money.of("500.00", USD));
    }

    @Test
    @DisplayName("Execute successful transfer")
    void executeTransfer_success() {
        TransferId transferId = TransferId.generate();
        Money transferAmount = Money.of("300.00", USD);

        TransferCommand.InitiateTransfer command = TransferCommand.InitiateTransfer.now(
                transferId, fromId, toId, transferAmount, "Rent payment", "INV001"
        );

        Result<TransferService.TransferExecutionResult> result =
                TransferService.executeTransfer(fromAccount, toAccount, command);

        assertTrue(result.isSuccess());
        TransferService.TransferExecutionResult execution = result.getOrThrow();

        assertEquals(transferId, execution.transfer().id());
        assertEquals(fromId, execution.transfer().fromAccountId());
        assertEquals(toId, execution.transfer().toAccountId());
        assertEquals(transferAmount, execution.transfer().amount());
        assertTrue(execution.transfer().isCompleted());

        assertEquals(0, execution.updatedFromAccount().balance().amount()
                .compareTo(Money.of("700.00", USD).amount()));
        assertEquals(0, execution.updatedToAccount().balance().amount()
                .compareTo(Money.of("800.00", USD).amount()));

        assertNotNull(execution.debitTransaction());
        assertNotNull(execution.creditTransaction());
        assertEquals(4, execution.domainEvents().size());
    }

    @Test
    @DisplayName("Transfer with insufficient funds fails")
    void executeTransfer_insufficientFunds() {
        TransferId transferId = TransferId.generate();
        Money transferAmount = Money.of("2000.00", USD);

        TransferCommand.InitiateTransfer command = TransferCommand.InitiateTransfer.now(
                transferId, fromId, toId, transferAmount, "Large payment", "INV002"
        );

        Result<TransferService.TransferExecutionResult> result =
                TransferService.executeTransfer(fromAccount, toAccount, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("Transfer from frozen account fails")
    void executeTransfer_fromFrozenAccount() {
        Account frozenAccount = fromAccount.freeze();
        TransferId transferId = TransferId.generate();
        Money transferAmount = Money.of("100.00", USD);

        TransferCommand.InitiateTransfer command = TransferCommand.InitiateTransfer.now(
                transferId, fromId, toId, transferAmount, "Payment", "INV003"
        );

        Result<TransferService.TransferExecutionResult> result =
                TransferService.executeTransfer(frozenAccount, toAccount, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("not active"));
    }

    @Test
    @DisplayName("Transfer to closed account fails")
    void executeTransfer_toClosedAccount() {
        Account closedAccount = Account.create(AccountId.generate(), "Closed Account", USD).close();

        TransferId transferId = TransferId.generate();
        Money transferAmount = Money.of("100.00", USD);

        TransferCommand.InitiateTransfer command = TransferCommand.InitiateTransfer.now(
                transferId, fromId, closedAccount.id(), transferAmount, "Payment", "INV004"
        );

        Result<TransferService.TransferExecutionResult> result =
                TransferService.executeTransfer(fromAccount, closedAccount, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("cannot receive deposits"));
    }

    @Test
    @DisplayName("Transfer with currency mismatch fails")
    void executeTransfer_currencyMismatch() {
        Account eurAccount = Account.create(AccountId.generate(), "Euro User",
                Currency.getInstance("EUR"));
        TransferId transferId = TransferId.generate();
        Money transferAmount = Money.of("100.00", USD);

        TransferCommand.InitiateTransfer command = TransferCommand.InitiateTransfer.now(
                transferId, fromId, eurAccount.id(), transferAmount, "Payment", "INV005"
        );

        Result<TransferService.TransferExecutionResult> result =
                TransferService.executeTransfer(fromAccount, eurAccount, command);

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("Currency mismatch"));
    }

    @Test
    @DisplayName("Transfer to DORMANT account succeeds (DORMANT canDeposit)")
    void executeTransfer_toDormantAccount_succeeds() {
        Account dormantTo = toAccount.markDormant();
        TransferId transferId = TransferId.generate();
        Money transferAmount = Money.of("100.00", USD);

        TransferCommand.InitiateTransfer command = TransferCommand.InitiateTransfer.now(
                transferId, fromId, dormantTo.id(), transferAmount, "Payment", "INV006"
        );

        Result<TransferService.TransferExecutionResult> result =
                TransferService.executeTransfer(fromAccount, dormantTo, command);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getOrThrow().updatedToAccount().balance().amount()
                .compareTo(Money.of("600.00", USD).amount()));
    }

    @Test
    @DisplayName("Cancel pending transfer")
    void cancelTransfer_pending() {
        Transfer transfer = Transfer.initiate(
                TransferId.generate(), fromId, toId,
                Money.of("100.00", USD), "Test", "REF001"
        );

        Result<Transfer> result = TransferService.cancelTransfer(transfer, "Changed mind");

        assertTrue(result.isSuccess());
        assertTrue(result.getOrThrow().isFailed());
        assertTrue(result.getOrThrow().failureReason().contains("CANCELLED"));
    }

    @Test
    @DisplayName("Cancel completed transfer fails")
    void cancelTransfer_completed_fails() {
        Transfer transfer = Transfer.initiate(
                TransferId.generate(), fromId, toId,
                Money.of("100.00", USD), "Test", "REF002"
        );
        transfer = transfer.markProcessing(null);
        transfer = transfer.complete(null);

        Result<Transfer> result = TransferService.cancelTransfer(transfer, "Too late");

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("Only pending transfers"));
    }

    @Test
    @DisplayName("Reverse completed transfer - moves money back and returns ReversalResult")
    void reverseTransfer_completed() {
        Transfer transfer = Transfer.initiate(
                TransferId.generate(), fromId, toId,
                Money.of("100.00", USD), "Test", "REF003"
        );
        transfer = transfer.markProcessing(null);
        transfer = transfer.complete(null);

        Result<TransferService.ReversalResult> result = TransferService.reverseTransfer(
                transfer, toAccount, fromAccount, "Wrong amount"
        );

        assertTrue(result.isSuccess());
        TransferService.ReversalResult reversal = result.getOrThrow();

        assertEquals(TransferStatus.REVERSED, reversal.reversedTransfer().status());

        // Receiver (toAccount) should have money debited back: 500 - 100 = 400
        assertEquals(0, reversal.updatedReceiverAccount().balance().amount()
                .compareTo(Money.of("400.00", USD).amount()));

        // Sender (fromAccount) should have money credited back: 1000 + 100 = 1100
        assertEquals(0, reversal.updatedSenderAccount().balance().amount()
                .compareTo(Money.of("1100.00", USD).amount()));

        assertNotNull(reversal.reversalDebitTransaction());
        assertNotNull(reversal.reversalCreditTransaction());
        assertEquals(4, reversal.domainEvents().size());
    }

    @Test
    @DisplayName("Reverse completed transfer - receiver insufficient funds fails")
    void reverseTransfer_receiverInsufficientFunds_fails() {
        // Receiver has 0 balance but transfer amount is 100
        Account brokenReceiver = Account.create(AccountId.generate(), "Broke Receiver", USD);
        Transfer transfer = Transfer.initiate(
                TransferId.generate(), fromId, brokenReceiver.id(),
                Money.of("100.00", USD), "Test", "REF007"
        );
        transfer = transfer.markProcessing(null);
        transfer = transfer.complete(null);

        Result<TransferService.ReversalResult> result = TransferService.reverseTransfer(
                transfer, brokenReceiver, fromAccount, "Reversal"
        );

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("insufficient funds"));
    }

    @Test
    @DisplayName("Reverse pending transfer fails")
    void reverseTransfer_pending_fails() {
        Transfer transfer = Transfer.initiate(
                TransferId.generate(), fromId, toId,
                Money.of("100.00", USD), "Test", "REF004"
        );

        Result<TransferService.ReversalResult> result = TransferService.reverseTransfer(
                transfer, toAccount, fromAccount, "Reason"
        );

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("Only completed transfers"));
    }

    @Test
    @DisplayName("Validate batch transfer success")
    void validateBatchTransfer_success() {
        List<Account> sourceAccounts = List.of(
                Account.create(AccountId.generate(), "Source1", USD)
                        .deposit(Money.of("400.00", USD)),
                Account.create(AccountId.generate(), "Source2", USD)
                        .deposit(Money.of("600.00", USD))
        );

        Account targetAccount = Account.create(AccountId.generate(), "Target", USD);
        Money totalAmount = Money.of("800.00", USD);

        Result<Boolean> result = TransferService.validateBatchTransfer(
                sourceAccounts, targetAccount, totalAmount
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getOrThrow());
    }

    @Test
    @DisplayName("Validate batch transfer insufficient funds")
    void validateBatchTransfer_insufficientFunds() {
        List<Account> sourceAccounts = List.of(
                Account.create(AccountId.generate(), "Source1", USD)
                        .deposit(Money.of("300.00", USD)),
                Account.create(AccountId.generate(), "Source2", USD)
                        .deposit(Money.of("400.00", USD))
        );

        Account targetAccount = Account.create(AccountId.generate(), "Target", USD);
        Money totalAmount = Money.of("800.00", USD);

        Result<Boolean> result = TransferService.validateBatchTransfer(
                sourceAccounts, targetAccount, totalAmount
        );

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("insufficient"));
    }

    @Test
    @DisplayName("Validate batch transfer with inactive source account")
    void validateBatchTransfer_inactiveSource() {
        List<Account> sourceAccounts = List.of(
                Account.create(AccountId.generate(), "Source1", USD)
                        .deposit(Money.of("500.00", USD)),
                Account.create(AccountId.generate(), "Source2", USD)
                        .deposit(Money.of("500.00", USD))
                        .freeze()
        );

        Account targetAccount = Account.create(AccountId.generate(), "Target", USD);
        Money totalAmount = Money.of("800.00", USD);

        Result<Boolean> result = TransferService.validateBatchTransfer(
                sourceAccounts, targetAccount, totalAmount
        );

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("not active"));
    }

    @Test
    @DisplayName("Validate batch transfer with target that cannot receive")
    void validateBatchTransfer_targetCannotReceive() {
        List<Account> sourceAccounts = List.of(
                Account.create(AccountId.generate(), "Source1", USD)
                        .deposit(Money.of("500.00", USD)),
                Account.create(AccountId.generate(), "Source2", USD)
                        .deposit(Money.of("500.00", USD))
        );

        Account targetAccount = Account.create(AccountId.generate(), "Target", USD).close();
        Money totalAmount = Money.of("800.00", USD);

        Result<Boolean> result = TransferService.validateBatchTransfer(
                sourceAccounts, targetAccount, totalAmount
        );

        assertTrue(result.isFailure());
        assertTrue(result.getErrorOrNull().contains("cannot receive deposits"));
    }

    @Test
    @DisplayName("Get transfer summary")
    void getTransferSummary() {
        Transfer transfer = Transfer.initiate(
                TransferId.generate(), fromId, toId,
                Money.of("100.00", USD), "Test", "REF005"
        );

        String summary = TransferService.getTransferSummary(transfer);

        assertTrue(summary.contains(transfer.id().value()));
        assertTrue(summary.contains(fromId.value()));
        assertTrue(summary.contains(toId.value()));
        assertTrue(summary.contains("100.00"));
        assertTrue(summary.contains("PENDING"));
    }
}
