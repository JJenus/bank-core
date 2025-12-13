package com.jjenus.bank.core.shared;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {
        public Success {
            if (value == null) {
                throw new IllegalArgumentException("Success value cannot be null");
            }
        }
    }

    record Failure<T>(String error) implements Result<T> {
        public Failure {
            if (error == null || error.isBlank()) {
                throw new IllegalArgumentException("Error message cannot be blank");
            }
        }

        public static <T> Result<T> of(String error) {
            return new Failure<>(error);
        }
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }

    default T getOrThrow() {
        if (this instanceof Success<T> success) {
            return success.value();
        } else if (this instanceof Failure<T> failure) {
            throw new IllegalStateException(failure.error());
        }
        throw new IllegalStateException("Unknown Result type");
    }

    default String getErrorOrNull() {
        if (this instanceof Failure<T> failure) {
            return failure.error();
        }
        return null;
    }

    // Additional helpful methods

    default T getOrElse(T defaultValue) {
        if (this instanceof Success<T> success) {
            return success.value();
        }
        return defaultValue;
    }

    default T getOrElseGet(java.util.function.Supplier<T> supplier) {
        if (this instanceof Success<T> success) {
            return success.value();
        }
        return supplier.get();
    }

    default void ifSuccess(java.util.function.Consumer<T> consumer) {
        if (this instanceof Success<T> success) {
            consumer.accept(success.value());
        }
    }

    default void ifFailure(java.util.function.Consumer<String> consumer) {
        if (this instanceof Failure<T> failure) {
            consumer.accept(failure.error());
        }
    }

    default <U> Result<U> map(java.util.function.Function<T, U> mapper) {
        if (this instanceof Success<T> success) {
            return new Success<>(mapper.apply(success.value()));
        } else if (this instanceof Failure<T> failure) {
            @SuppressWarnings("unchecked")
            Result<U> result = (Result<U>) this;
            return result;
        }
        throw new IllegalStateException("Unknown Result type");
    }

    default <U> Result<U> flatMap(java.util.function.Function<T, Result<U>> mapper) {
        if (this instanceof Success<T> success) {
            return mapper.apply(success.value());
        } else if (this instanceof Failure<T> failure) {
            @SuppressWarnings("unchecked")
            Result<U> result = (Result<U>) this;
            return result;
        }
        throw new IllegalStateException("Unknown Result type");
    }

    // Static factory methods
    public static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }

    static <T> Result<T> ofNullable(T value, String errorIfNull) {
        return value != null ? success(value) : failure(errorIfNull);
    }

    static <T> Result<T> fromOptional(
            java.util.Optional<T> optional,
            String errorIfEmpty
    ) {
        return optional.map(Result::success)
                .orElse(failure(errorIfEmpty));
    }

    // Try-catch helper
    static <T> Result<T> tryOf(ThrowingSupplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e.getMessage());
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}