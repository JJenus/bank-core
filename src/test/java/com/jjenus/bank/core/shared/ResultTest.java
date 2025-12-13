package com.jjenus.bank.core.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    @DisplayName("Success result creation")
    void success_creation() {
        String value = "test";
        Result<String> result = Result.success(value);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(value, result.getOrThrow());
        assertNull(result.getErrorOrNull());
    }

    @Test
    @DisplayName("Failure result creation")
    void failure_creation() {
        String error = "Error occurred";
        Result<String> result = Result.failure(error);

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertEquals(error, result.getErrorOrNull());
        assertThrows(IllegalStateException.class, result::getOrThrow);
    }

    @Test
    @DisplayName("Success with null value throws exception")
    void success_withNullValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Result.success(null));
    }

    @Test
    @DisplayName("Failure with blank error throws exception")
    void failure_withBlankError_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Result.failure(""));
        assertThrows(IllegalArgumentException.class, () -> Result.failure("   "));
    }

    @Test
    @DisplayName("Get or else returns value on success")
    void getOrElse_success() {
        Result<String> result = Result.success("value");
        String defaultValue = "default";

        assertEquals("value", result.getOrElse(defaultValue));
    }

    @Test
    @DisplayName("Get or else returns default on failure")
    void getOrElse_failure() {
        Result<String> result = Result.failure("error");
        String defaultValue = "default";

        assertEquals(defaultValue, result.getOrElse(defaultValue));
    }

    @Test
    @DisplayName("Map success result")
    void map_success() {
        Result<String> original = Result.success("123");
        Result<Integer> mapped = original.map(Integer::parseInt);

        assertTrue(mapped.isSuccess());
        assertEquals(123, mapped.getOrThrow());
    }

    @Test
    @DisplayName("Map failure result")
    void map_failure() {
        Result<String> original = Result.failure("error");
        Result<Integer> mapped = original.map(Integer::parseInt);

        assertTrue(mapped.isFailure());
        assertEquals("error", mapped.getErrorOrNull());
    }

    @Test
    @DisplayName("FlatMap success result")
    void flatMap_success() {
        Result<String> original = Result.success("123");
        Result<Integer> flatMapped = original.flatMap(s -> Result.success(Integer.parseInt(s)));

        assertTrue(flatMapped.isSuccess());
        assertEquals(123, flatMapped.getOrThrow());
    }

    @Test
    @DisplayName("FlatMap failure result")
    void flatMap_failure() {
        Result<String> original = Result.failure("error");
        Result<Integer> flatMapped = original.flatMap(s -> Result.success(Integer.parseInt(s)));

        assertTrue(flatMapped.isFailure());
        assertEquals("error", flatMapped.getErrorOrNull());
    }

    @Test
    @DisplayName("IfSuccess consumer called on success")
    void ifSuccess_calledOnSuccess() {
        Result<String> result = Result.success("value");
        StringBuilder sb = new StringBuilder();

        result.ifSuccess(sb::append);

        assertEquals("value", sb.toString());
    }

    @Test
    @DisplayName("IfSuccess consumer not called on failure")
    void ifSuccess_notCalledOnFailure() {
        Result<String> result = Result.failure("error");
        StringBuilder sb = new StringBuilder();

        result.ifSuccess(sb::append);

        assertEquals("", sb.toString());
    }

    @Test
    @DisplayName("IfFailure consumer called on failure")
    void ifFailure_calledOnFailure() {
        Result<String> result = Result.failure("error");
        StringBuilder sb = new StringBuilder();

        result.ifFailure(sb::append);

        assertEquals("error", sb.toString());
    }

    @Test
    @DisplayName("IfFailure consumer not called on success")
    void ifFailure_notCalledOnSuccess() {
        Result<String> result = Result.success("value");
        StringBuilder sb = new StringBuilder();

        result.ifFailure(sb::append);

        assertEquals("", sb.toString());
    }

    @Test
    @DisplayName("From optional with value returns success")
    void fromOptional_withValue() {
        java.util.Optional<String> optional = java.util.Optional.of("value");
        Result<String> result = Result.fromOptional(optional, "not found");

        assertTrue(result.isSuccess());
        assertEquals("value", result.getOrThrow());
    }

    @Test
    @DisplayName("From optional empty returns failure")
    void fromOptional_empty() {
        java.util.Optional<String> optional = java.util.Optional.empty();
        Result<String> result = Result.fromOptional(optional, "not found");

        assertTrue(result.isFailure());
        assertEquals("not found", result.getErrorOrNull());
    }

    @Test
    @DisplayName("TryOf with successful operation")
    void tryOf_success() {
        Result<String> result = Result.tryOf(() -> "success");

        assertTrue(result.isSuccess());
        assertEquals("success", result.getOrThrow());
    }

    @Test
    @DisplayName("TryOf with throwing operation")
    void tryOf_failure() {
        Result<String> result = Result.tryOf(() -> {
            throw new RuntimeException("error");
        });

        assertTrue(result.isFailure());
        assertEquals("error", result.getErrorOrNull());
    }

    @Test
    @DisplayName("OfNullable with non-null value")
    void ofNullable_nonNull() {
        Result<String> result = Result.ofNullable("value", "is null");

        assertTrue(result.isSuccess());
        assertEquals("value", result.getOrThrow());
    }

    @Test
    @DisplayName("OfNullable with null value")
    void ofNullable_null() {
        Result<String> result = Result.ofNullable(null, "is null");

        assertTrue(result.isFailure());
        assertEquals("is null", result.getErrorOrNull());
    }
}