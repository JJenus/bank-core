package com.jjenus.bank.core.shared;

import java.util.UUID;

public record Id<T>(String value) {

    public Id {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ID cannot be blank");
        }
    }

    public static <T> Id<T> of(String value) {
        return new Id<>(value);
    }

    public static <T> Id<T> random() {
        return new Id<>(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
