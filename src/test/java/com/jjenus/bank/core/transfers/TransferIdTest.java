package com.jjenus.bank.core.transfers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class TransferIdTest {

    @Test
    @DisplayName("TransferId creation with valid format")
    void create_withValidFormat() {
        String validId = "TRF-ABC123456789";
        TransferId transferId = TransferId.of(validId);

        assertEquals(validId, transferId.value());
    }

    @Test
    @DisplayName("TransferId generate produces valid format")
    void generate_producesValidFormat() {
        TransferId transferId = TransferId.generate();
        String value = transferId.value();

        assertTrue(value.startsWith("TRF-"));
        assertEquals(16, value.length()); // "TRF-" + 12 chars
        assertTrue(value.substring(4).matches("[A-Z0-9]{12}"));
    }

    @Test
    @DisplayName("TransferId with null value throws exception")
    void create_withNullValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TransferId.of(null));
    }

    @Test
    @DisplayName("TransferId with blank value throws exception")
    void create_withBlankValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TransferId.of(""));
        assertThrows(IllegalArgumentException.class, () -> TransferId.of("   "));
    }

    @Test
    @DisplayName("TransferId with invalid format throws exception")
    void create_withInvalidFormat_throwsException() {
        // Missing prefix
        assertThrows(IllegalArgumentException.class, () -> TransferId.of("ABC123456789"));

        // Wrong prefix
        assertThrows(IllegalArgumentException.class, () -> TransferId.of("TFR-ABC123456789"));

        // Too short
        assertThrows(IllegalArgumentException.class, () -> TransferId.of("TRF-ABC123"));

        // Too long
        assertThrows(IllegalArgumentException.class, () -> TransferId.of("TRF-ABC1234567890"));

        // Invalid characters
        assertThrows(IllegalArgumentException.class, () -> TransferId.of("TRF-abc12345678")); // lowercase
        assertThrows(IllegalArgumentException.class, () -> TransferId.of("TRF-ABC!@#$%^&*()"));
    }

    @Test
    @DisplayName("TransferId toString returns value")
    void toString_returnsValue() {
        String value = "TRF-ABC123456789";
        TransferId transferId = TransferId.of(value);

        assertEquals(value, transferId.toString());
    }

    @Test
    @DisplayName("TransferId equals and hashCode")
    void equals_andHashCode() {
        TransferId id1 = TransferId.of("TRF-ABC123456789");
        TransferId id2 = TransferId.of("TRF-ABC123456789");
        TransferId id3 = TransferId.of("TRF-DEF123456789");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1.hashCode(), id3.hashCode());
    }
}
