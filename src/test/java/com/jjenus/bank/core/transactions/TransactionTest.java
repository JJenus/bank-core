package com.jjenus.bank.core.transactions;

import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {
    private static final Currency USD = Currency.getInstance("USD");
    private AccountId accountId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        accountId = AccountId.generate();
    }

    @Test
    @DisplayName("Create deposit transaction")
    void createDeposit() {
        Money amount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("100.00", USD);
        TransactionId id = TransactionId.generate();

        Transaction transaction = Transaction.createDeposit(
            id, accountId, amount, balanceAfter, "DEP001"
        );

        assertEquals(id, transaction.id());
        assertEquals(accountId, transaction.accountId());
        assertEquals(TransactionType.DEPOSIT, transaction.type());
        assertEquals(0, transaction.balanceAfter().amount().compareTo(balanceAfter.amount()));
        assertEquals("Deposit to account", transaction.description());
        assertEquals("DEP001", transaction.reference());
        assertTrue(transaction.isCredit());
        assertFalse(transaction.isDebit());
    }

    @Test
    @DisplayName("Create withdrawal transaction")
    void createWithdrawal() {
        Money amount = Money.of("50.00", USD);
        Money balanceAfter = Money.of("50.00", USD);
        TransactionId id = TransactionId.generate();

        Transaction transaction = Transaction.createWithdrawal(
            id, accountId, amount, balanceAfter, "WITH001"
        );

        assertEquals(id, transaction.id());
        assertEquals(accountId, transaction.accountId());
        assertEquals(TransactionType.WITHDRAWAL, transaction.type());
        assertTrue(transaction.amount().isNegative());
        assertEquals(0, transaction.getAbsoluteAmount().amount().compareTo(amount.amount()));
        assertEquals(0, transaction.balanceAfter().amount().compareTo(balanceAfter.amount()));
        assertEquals("Withdrawal from account", transaction.description());
        assertEquals("WITH001", transaction.reference());
        assertFalse(transaction.isCredit());
        assertTrue(transaction.isDebit());
    }

    @Test
    @DisplayName("Create transfer out transaction")
    void createTransferOut() {
        Money amount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("400.00", USD);
        TransactionId id = TransactionId.generate();
        String relatedId = "TXN-RELATED123";

        Transaction transaction = Transaction.createTransferOut(
            id, accountId, amount, balanceAfter, "TRF001", relatedId
        );

        assertEquals(id, transaction.id());
        assertEquals(accountId, transaction.accountId());
        assertEquals(TransactionType.TRANSFER_OUT, transaction.type());
        assertTrue(transaction.amount().isNegative());
        assertEquals(0, transaction.getAbsoluteAmount().amount().compareTo(amount.amount()));
        assertEquals(0, transaction.balanceAfter().amount().compareTo(balanceAfter.amount()));
        assertEquals("Transfer to another account", transaction.description());
        assertEquals("TRF001", transaction.reference());
        assertEquals(relatedId, transaction.relatedTransactionId());
        assertFalse(transaction.isCredit());
        assertTrue(transaction.isDebit());
    }

    @Test
    @DisplayName("Create transfer in transaction")
    void createTransferIn() {
        Money amount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("600.00", USD);
        TransactionId id = TransactionId.generate();
        String relatedId = "TXN-RELATED456";

        Transaction transaction = Transaction.createTransferIn(
            id, accountId, amount, balanceAfter, "TRF002", relatedId
        );

        assertEquals(id, transaction.id());
        assertEquals(accountId, transaction.accountId());
        assertEquals(TransactionType.TRANSFER_IN, transaction.type());
        assertTrue(transaction.amount().isPositive());
        assertEquals(0, transaction.getAbsoluteAmount().amount().compareTo(amount.amount()));
        assertEquals(0, transaction.balanceAfter().amount().compareTo(balanceAfter.amount()));
        assertEquals("Transfer from another account", transaction.description());
        assertEquals("TRF002", transaction.reference());
        assertEquals(relatedId, transaction.relatedTransactionId());
        assertTrue(transaction.isCredit());
        assertFalse(transaction.isDebit());
    }

    @Test
    @DisplayName("Create fee transaction")
    void createFee() {
        Money amount = Money.of("5.00", USD);
        Money balanceAfter = Money.of("95.00", USD);
        TransactionId id = TransactionId.generate();

        Transaction transaction = Transaction.createFee(
            id, accountId, amount, balanceAfter, "Monthly maintenance fee"
        );

        assertEquals(id, transaction.id());
        assertEquals(accountId, transaction.accountId());
        assertEquals(TransactionType.FEE, transaction.type());
        assertTrue(transaction.amount().isNegative());
        assertEquals(0, transaction.getAbsoluteAmount().amount().compareTo(amount.amount()));
        assertEquals(0, transaction.balanceAfter().amount().compareTo(balanceAfter.amount()));
        assertEquals("Monthly maintenance fee", transaction.description());
        assertEquals("SYSTEM_FEE", transaction.reference());
        assertFalse(transaction.isCredit());
        assertTrue(transaction.isDebit());
    }

    @Test
    @DisplayName("Credit transaction with negative amount throws exception")
    void creditTransaction_withNegativeAmount_throwsException() {
        Money negativeAmount = Money.of("-100.00", USD);
        Money balanceAfter = Money.of("100.00", USD);
        TransactionId id = TransactionId.generate();

        assertThrows(IllegalArgumentException.class, () ->
            new Transaction(
                id, accountId, TransactionType.DEPOSIT,
                negativeAmount, balanceAfter, "Deposit",
                "REF001", java.time.Instant.now(), null, null
            )
        );
    }

    @Test
    @DisplayName("Debit transaction with positive amount throws exception")
    void debitTransaction_withPositiveAmount_throwsException() {
        Money positiveAmount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("100.00", USD);
        TransactionId id = TransactionId.generate();

        assertThrows(IllegalArgumentException.class, () ->
            new Transaction(
                id, accountId, TransactionType.WITHDRAWAL,
                positiveAmount, balanceAfter, "Withdrawal",
                "REF002", java.time.Instant.now(), null, null
            )
        );
    }

    @Test
    @DisplayName("Transaction with null ID throws exception")
    void transaction_withNullId_throwsException() {
        Money amount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("100.00", USD);

        assertThrows(IllegalArgumentException.class, () ->
            new Transaction(
                null, accountId, TransactionType.DEPOSIT,
                amount, balanceAfter, "Deposit",
                "REF003", java.time.Instant.now(), null, null
            )
        );
    }

    @Test
    @DisplayName("Transaction with null account ID throws exception")
    void transaction_withNullAccountId_throwsException() {
        Money amount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("100.00", USD);
        TransactionId id = TransactionId.generate();

        assertThrows(IllegalArgumentException.class, () ->
            new Transaction(
                id, null, TransactionType.DEPOSIT,
                amount, balanceAfter, "Deposit",
                "REF004", java.time.Instant.now(), null, null
            )
        );
    }

    @Test
    @DisplayName("Transaction with blank description throws exception")
    void transaction_withBlankDescription_throwsException() {
        Money amount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("100.00", USD);
        TransactionId id = TransactionId.generate();

        assertThrows(IllegalArgumentException.class, () ->
            new Transaction(
                id, accountId, TransactionType.DEPOSIT,
                amount, balanceAfter, "",
                "REF005", java.time.Instant.now(), null, null
            )
        );
    }

    @Test
    @DisplayName("Get signed amount")
    void getSignedAmount() {
        Money depositAmount = Money.of("100.00", USD);
        Money withdrawalAmount = Money.of("50.00", USD).negate();
        Money balanceAfter = Money.of("100.00", USD);

        Transaction deposit = Transaction.createDeposit(
            TransactionId.generate(), accountId, depositAmount, balanceAfter, "DEP001"
        );
        Transaction withdrawal = Transaction.createWithdrawal(
            TransactionId.generate(), accountId, Money.of("50.00", USD), balanceAfter, "WITH001"
        );

        assertTrue(deposit.getSignedAmount().isPositive());
        assertTrue(withdrawal.getSignedAmount().isNegative());
    }

    @Test
    @DisplayName("Get absolute amount")
    void getAbsoluteAmount() {
        Money amount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("100.00", USD);

        Transaction deposit = Transaction.createDeposit(
            TransactionId.generate(), accountId, amount, balanceAfter, "DEP001"
        );
        Transaction withdrawal = Transaction.createWithdrawal(
            TransactionId.generate(), accountId, amount, balanceAfter, "WITH001"
        );

        assertEquals(0, deposit.getAbsoluteAmount().amount().compareTo(amount.amount()));
        assertEquals(0, withdrawal.getAbsoluteAmount().amount().compareTo(amount.amount()));
        assertTrue(deposit.getAbsoluteAmount().isPositive());
        assertTrue(withdrawal.getAbsoluteAmount().isPositive());
    }

    @Test
    @DisplayName("ToString contains relevant information")
    void toString_containsRelevantInfo() {
        Money amount = Money.of("100.00", USD);
        Money balanceAfter = Money.of("100.00", USD);
        TransactionId id = TransactionId.generate();

        Transaction transaction = Transaction.createDeposit(id, accountId, amount, balanceAfter, "DEP001");
        String str = transaction.toString();

        assertTrue(str.contains(id.value()));
        assertTrue(str.contains(accountId.value()));
        assertTrue(str.contains("DEPOSIT"));
        assertTrue(str.contains("100.00"));
    }
}
