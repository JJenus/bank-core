package com.jjenus.bank.core.transactions;

public record TransactionId(String value) {

    public TransactionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be blank");
        }
        if (!value.matches("^TXN-[A-Z0-9]{12}$")) {
            throw new IllegalArgumentException(
                "Transaction ID must be in format TXN-XXXXXXXXXXXX (12 alphanumeric characters)"
            );
        }
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    public static TransactionId generate() {
        String random = java.util.UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 12)
            .toUpperCase();
        return new TransactionId("TXN-" + random);
    }

    @Override
    public String toString() {
        return value;
    }
}
