package com.jjenus.bank.core.transfers;

public record TransferId(String value) {

    public TransferId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Transfer ID cannot be blank");
        }
        if (!value.matches("^TRF-[A-Z0-9]{12}$")) {
            throw new IllegalArgumentException(
                "Transfer ID must be in format TRF-XXXXXXXXXXXX (12 alphanumeric characters)"
            );
        }
    }

    public static TransferId of(String value) {
        return new TransferId(value);
    }

    public static TransferId generate() {
        String random = java.util.UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 12)
            .toUpperCase();
        return new TransferId("TRF-" + random);
    }

    @Override
    public String toString() {
        return value;
    }
}
