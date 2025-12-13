package com.jjenus.bank.core.transactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class TransactionIdTest {

    @Test
    @DisplayName("TransactionId creation with valid format")
    void create_withValidFormat() {
        String validId = "TXN-ABC123456789";
        TransactionId transactionId = TransactionId.of(validId);

        assertEquals(validId, transactionId.value());
    }

    @Test
    @DisplayName("TransactionId generate produces valid format")
    void generate_producesValidFormat() {
        TransactionId transactionId = TransactionId.generate();
        String value = transactionId.value();

        assertTrue(value.startsWith("TXN-"));
        assertEquals(16, value.length()); // "TXN-" + 12 chars
        assertTrue(value.substring(4).matches("[A-Z0-9]{12}"));
    }

    @Test
    @DisplayName("TransactionId with null value throws exception")
    void create_withNullValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of(null));
    }

    @Test
    @DisplayName("TransactionId with blank value throws exception")
    void create_withBlankValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of(""));
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of("   "));
    }

    @Test
    @DisplayName("TransactionId with invalid format throws exception")
    void create_withInvalidFormat_throwsException() {
        // Missing prefix
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of("ABC123456789"));

        // Wrong prefix
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of("TRN-ABC123456789"));

        // Too short
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of("TXN-ABC123"));

        // Too long
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of("TXN-ABC1234567890"));

        // Invalid characters
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of("TXN-abc12345678")); // lowercase
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of("TXN-ABC!@#$%^&*()"));
    }

    @Test
    @DisplayName("TransactionId toString returns value")
    void toString_returnsValue() {
        String value = "TXN-ABC123456789";
        TransactionId transactionId = TransactionId.of(value);

        assertEquals(value, transactionId.toString());
    }

    @Test
    @DisplayName("TransactionId equals and hashCode")
    void equals_andHashCode() {
        TransactionId id1 = TransactionId.of("TXN-ABC123456789");
        TransactionId id2 = TransactionId.of("TXN-ABC123456789");
        TransactionId id3 = TransactionId.of("TXN-DEF123456789");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1.hashCode(), id3.hashCode());
    }
}
