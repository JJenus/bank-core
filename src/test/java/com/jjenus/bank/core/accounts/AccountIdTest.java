package com.jjenus.bank.core.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class AccountIdTest {

    @Test
    @DisplayName("AccountId creation with valid format")
    void create_withValidFormat() {
        String validId = "ACC-ABC1234567";
        AccountId accountId = AccountId.of(validId);

        assertEquals(validId, accountId.value());
    }

    @Test
    @DisplayName("AccountId generate produces valid format")
    void generate_producesValidFormat() {
        AccountId accountId = AccountId.generate();
        String value = accountId.value();

        assertTrue(value.startsWith("ACC-"));
        assertEquals(14, value.length()); // "ACC-" + 10 chars
        assertTrue(value.substring(4).matches("[A-Z0-9]{10}"));
    }

    @Test
    @DisplayName("AccountId with null value throws exception")
    void create_withNullValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> AccountId.of(null));
    }

    @Test
    @DisplayName("AccountId with blank value throws exception")
    void create_withBlankValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> AccountId.of(""));
        assertThrows(IllegalArgumentException.class, () -> AccountId.of("   "));
    }

    @Test
    @DisplayName("AccountId with invalid format throws exception")
    void create_withInvalidFormat_throwsException() {
        // Missing prefix
        assertThrows(IllegalArgumentException.class, () -> AccountId.of("ABC1234567"));

        // Wrong prefix
        assertThrows(IllegalArgumentException.class, () -> AccountId.of("ACT-ABC1234567"));

        // Too short
        assertThrows(IllegalArgumentException.class, () -> AccountId.of("ACC-ABC123"));

        // Too long
        assertThrows(IllegalArgumentException.class, () -> AccountId.of("ACC-ABC12345678"));

        // Invalid characters
        assertThrows(IllegalArgumentException.class, () -> AccountId.of("ACC-abc123456")); // lowercase
        assertThrows(IllegalArgumentException.class, () -> AccountId.of("ACC-ABC!@#$%^"));
    }

    @Test
    @DisplayName("AccountId toString returns value")
    void toString_returnsValue() {
        String value = "ACC-ABC1234567";
        AccountId accountId = AccountId.of(value);

        assertEquals(value, accountId.toString());
    }

    @Test
    @DisplayName("AccountId equals and hashCode")
    void equals_andHashCode() {
        AccountId id1 = AccountId.of("ACC-ABC1234567");
        AccountId id2 = AccountId.of("ACC-ABC1234567");
        AccountId id3 = AccountId.of("ACC-DEF1234567");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1.hashCode(), id3.hashCode());
    }
}
