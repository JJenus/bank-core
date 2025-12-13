package com.jjenus.bank.core.transactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTypeTest {

    @Test
    @DisplayName("Credit transaction types")
    void creditTransactionTypes() {
        assertTrue(TransactionType.DEPOSIT.isCredit());
        assertTrue(TransactionType.TRANSFER_IN.isCredit());
        assertTrue(TransactionType.INTEREST.isCredit());
        assertTrue(TransactionType.REFUND.isCredit());

        assertFalse(TransactionType.DEPOSIT.isDebit());
        assertFalse(TransactionType.TRANSFER_IN.isDebit());
        assertFalse(TransactionType.INTEREST.isDebit());
        assertFalse(TransactionType.REFUND.isDebit());
    }

    @Test
    @DisplayName("Debit transaction types")
    void debitTransactionTypes() {
        assertTrue(TransactionType.WITHDRAWAL.isDebit());
        assertTrue(TransactionType.TRANSFER_OUT.isDebit());
        assertTrue(TransactionType.FEE.isDebit());
        assertTrue(TransactionType.REVERSAL.isDebit());

        assertFalse(TransactionType.WITHDRAWAL.isCredit());
        assertFalse(TransactionType.TRANSFER_OUT.isCredit());
        assertFalse(TransactionType.FEE.isCredit());
        assertFalse(TransactionType.REVERSAL.isCredit());
    }

    @Test
    @DisplayName("Transfer related transaction types")
    void transferRelatedTypes() {
        assertTrue(TransactionType.TRANSFER_IN.isTransferRelated());
        assertTrue(TransactionType.TRANSFER_OUT.isTransferRelated());

        assertFalse(TransactionType.DEPOSIT.isTransferRelated());
        assertFalse(TransactionType.WITHDRAWAL.isTransferRelated());
        assertFalse(TransactionType.FEE.isTransferRelated());
        assertFalse(TransactionType.INTEREST.isTransferRelated());
        assertFalse(TransactionType.REFUND.isTransferRelated());
        assertFalse(TransactionType.REVERSAL.isTransferRelated());
    }

    @Test
    @DisplayName("Get description")
    void getDescription() {
        assertNotNull(TransactionType.DEPOSIT.getDescription());
        assertNotNull(TransactionType.WITHDRAWAL.getDescription());
        assertNotNull(TransactionType.TRANSFER_IN.getDescription());
        assertNotNull(TransactionType.TRANSFER_OUT.getDescription());
        assertNotNull(TransactionType.FEE.getDescription());
        assertNotNull(TransactionType.INTEREST.getDescription());
        assertNotNull(TransactionType.REFUND.getDescription());
        assertNotNull(TransactionType.REVERSAL.getDescription());

        assertFalse(TransactionType.DEPOSIT.getDescription().isEmpty());
        assertFalse(TransactionType.WITHDRAWAL.getDescription().isEmpty());
    }

    @Test
    @DisplayName("Transaction type values")
    void values() {
        TransactionType[] values = TransactionType.values();

        assertEquals(8, values.length);
        assertArrayEquals(new TransactionType[]{
            TransactionType.DEPOSIT,
            TransactionType.WITHDRAWAL,
            TransactionType.TRANSFER_OUT,
            TransactionType.TRANSFER_IN,
            TransactionType.FEE,
            TransactionType.INTEREST,
            TransactionType.REFUND,
            TransactionType.REVERSAL
        }, values);
    }
}
