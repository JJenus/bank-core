package com.jjenus.bank.core.transfers;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import com.jjenus.bank.core.transactions.TransactionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class TransferTest {
    private static final Currency USD = Currency.getInstance("USD");
    private AccountId fromAccountId;
    private AccountId toAccountId;
    private Money amount;
    private TransferId transferId;

    @BeforeEach
    void setUp() {
        fromAccountId = AccountId.generate();
        toAccountId = AccountId.generate();
        amount = Money.of("100.00", USD);
        transferId = TransferId.generate();
    }

    @Test
    @DisplayName("Initiate transfer")
    void initiate() {
        Transfer transfer = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test transfer", "REF001"
        );

        assertEquals(transferId, transfer.id());
        assertEquals(fromAccountId, transfer.fromAccountId());
        assertEquals(toAccountId, transfer.toAccountId());
        assertEquals(amount, transfer.amount());
        assertEquals(TransferStatus.PENDING, transfer.status());
        assertEquals("Test transfer", transfer.description());
        assertEquals("REF001", transfer.reference());
        assertNotNull(transfer.createdAt());
        assertNull(transfer.completedAt());
        assertNull(transfer.debitTransactionId());
        assertNull(transfer.creditTransactionId());
        assertNull(transfer.failureReason());
        assertTrue(transfer.isPending());
        assertFalse(transfer.isCompleted());
        assertFalse(transfer.isFailed());
        assertTrue(transfer.canBeProcessed());
    }

    @Test
    @DisplayName("Initiate transfer to same account throws exception")
    void initiate_sameAccount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            Transfer.initiate(
                transferId, fromAccountId, fromAccountId, amount, "Test", "REF001"
            )
        );
    }

    @Test
    @DisplayName("Initiate transfer with zero amount throws exception")
    void initiate_zeroAmount_throwsException() {
        Money zeroAmount = Money.zero(USD);

        assertThrows(IllegalArgumentException.class, () ->
            Transfer.initiate(
                transferId, fromAccountId, toAccountId, zeroAmount, "Test", "REF001"
            )
        );
    }

    @Test
    @DisplayName("Initiate transfer with negative amount throws exception")
    void initiate_negativeAmount_throwsException() {
        Money negativeAmount = Money.of("-100.00", USD);

        assertThrows(IllegalArgumentException.class, () ->
            Transfer.initiate(
                transferId, fromAccountId, toAccountId, negativeAmount, "Test", "REF001"
            )
        );
    }

    @Test
    @DisplayName("Mark transfer as processing")
    void markProcessing() {
        Transfer pending = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );
        TransactionId debitTransactionId = TransactionId.generate();

        Transfer processing = pending.markProcessing(debitTransactionId);

        assertEquals(TransferStatus.PROCESSING, processing.status());
        assertEquals(debitTransactionId, processing.debitTransactionId());
        assertNull(processing.creditTransactionId());
        assertNull(processing.completedAt());
        assertFalse(processing.isPending());
        assertFalse(processing.isCompleted());
        assertFalse(processing.isFailed());
        assertFalse(processing.canBeProcessed());
    }

    @Test
    @DisplayName("Mark non-pending transfer as processing throws exception")
    void markProcessing_nonPending_throwsException() {
        Transfer pending = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );
        TransactionId debitTransactionId = TransactionId.generate();
        Transfer processing = pending.markProcessing(debitTransactionId);

        assertThrows(IllegalStateException.class, () ->
            processing.markProcessing(debitTransactionId)
        );
    }

    @Test
    @DisplayName("Complete transfer")
    void complete() {
        Transfer pending = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );
        TransactionId debitTransactionId = TransactionId.generate();
        TransactionId creditTransactionId = TransactionId.generate();

        Transfer processing = pending.markProcessing(debitTransactionId);
        Transfer completed = processing.complete(creditTransactionId);

        assertEquals(TransferStatus.COMPLETED, completed.status());
        assertEquals(debitTransactionId, completed.debitTransactionId());
        assertEquals(creditTransactionId, completed.creditTransactionId());
        assertNotNull(completed.completedAt());
        assertTrue(completed.isCompleted());
        assertFalse(completed.isPending());
        assertFalse(completed.isFailed());
        assertTrue(completed.status().isTerminal());
        assertTrue(completed.status().isSuccessful());
    }

    @Test
    @DisplayName("Complete non-processing transfer throws exception")
    void complete_nonProcessing_throwsException() {
        Transfer pending = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );
        TransactionId creditTransactionId = TransactionId.generate();

        assertThrows(IllegalStateException.class, () ->
            pending.complete(creditTransactionId)
        );
    }

    @Test
    @DisplayName("Fail transfer")
    void fail() {
        Transfer pending = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );
        String reason = "Insufficient funds";

        Transfer failed = pending.fail(reason);

        assertEquals(TransferStatus.FAILED, failed.status());
        assertEquals(reason, failed.failureReason());
        assertNotNull(failed.completedAt());
        assertTrue(failed.isFailed());
        assertFalse(failed.isPending());
        assertFalse(failed.isCompleted());
        assertTrue(failed.status().isTerminal());
        assertFalse(failed.status().isSuccessful());
    }

    @Test
    @DisplayName("Fail terminal transfer throws exception")
    void fail_terminal_throwsException() {
        Transfer pending = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );
        TransactionId debitTransactionId = TransactionId.generate();
        TransactionId creditTransactionId = TransactionId.generate();

        Transfer completed = pending.markProcessing(debitTransactionId)
                                   .complete(creditTransactionId);

        assertThrows(IllegalStateException.class, () ->
            completed.fail("Too late")
        );
    }

    @Test
    @DisplayName("Reverse completed transfer")
    void reverse() {
        Transfer pending = Transfer.initiate(
                transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );
        TransactionId debitTransactionId = TransactionId.generate();
        TransactionId creditTransactionId = TransactionId.generate();

        Transfer completed = pending.markProcessing(debitTransactionId)
                .complete(creditTransactionId);
        String reason = "Wrong amount";

        Transfer reversed = completed.reverse(reason);

        assertEquals(TransferStatus.REVERSED, reversed.status());
        assertEquals(reason, reversed.failureReason());
        assertTrue(reversed.description().contains("REVERSED"));
        assertNotNull(reversed.completedAt());

        assertFalse(reversed.isFailed());
        assertTrue(reversed.status().isTerminal());
        assertFalse(reversed.status().isSuccessful());  // REVERSED is not successful
    }

    @Test
    @DisplayName("Reverse non-completed transfer throws exception")
    void reverse_nonCompleted_throwsException() {
        Transfer pending = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );

        assertThrows(IllegalStateException.class, () ->
            pending.reverse("Too early")
        );
    }

    @Test
    @DisplayName("Transfer status descriptions")
    void statusDescriptions() {
        assertNotNull(TransferStatus.PENDING.getDescription());
        assertNotNull(TransferStatus.PROCESSING.getDescription());
        assertNotNull(TransferStatus.COMPLETED.getDescription());
        assertNotNull(TransferStatus.FAILED.getDescription());
        assertNotNull(TransferStatus.REVERSED.getDescription());
        assertNotNull(TransferStatus.CANCELLED.getDescription());

        assertFalse(TransferStatus.PENDING.getDescription().isEmpty());
        assertFalse(TransferStatus.COMPLETED.getDescription().isEmpty());
    }

    @Test
    @DisplayName("Transfer status terminal states")
    void statusTerminalStates() {
        assertFalse(TransferStatus.PENDING.isTerminal());
        assertFalse(TransferStatus.PROCESSING.isTerminal());
        assertTrue(TransferStatus.COMPLETED.isTerminal());
        assertTrue(TransferStatus.FAILED.isTerminal());
        assertTrue(TransferStatus.REVERSED.isTerminal());
        assertTrue(TransferStatus.CANCELLED.isTerminal());
    }

    @Test
    @DisplayName("Transfer status successful states")
    void statusSuccessfulStates() {
        assertFalse(TransferStatus.PENDING.isSuccessful());
        assertFalse(TransferStatus.PROCESSING.isSuccessful());
        assertTrue(TransferStatus.COMPLETED.isSuccessful());
        assertFalse(TransferStatus.FAILED.isSuccessful());
        assertFalse(TransferStatus.REVERSED.isSuccessful());
        assertFalse(TransferStatus.CANCELLED.isSuccessful());
    }

    @Test
    @DisplayName("ToString contains relevant information")
    void toString_containsRelevantInfo() {
        Transfer transfer = Transfer.initiate(
            transferId, fromAccountId, toAccountId, amount, "Test", "REF001"
        );
        String str = transfer.toString();

        assertTrue(str.contains(transferId.value()));
        assertTrue(str.contains(fromAccountId.value()));
        assertTrue(str.contains(toAccountId.value()));
        assertTrue(str.contains("100.00"));
        assertTrue(str.contains("PENDING"));
    }
}
