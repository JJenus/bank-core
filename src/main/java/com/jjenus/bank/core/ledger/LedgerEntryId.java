package com.jjenus.bank.core.ledger;

public record LedgerEntryId(String value) {

    public LedgerEntryId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LedgerEntry ID cannot be blank");
        }
        if (!value.matches("^JNL-[A-Z0-9]{12}$")) {
            throw new IllegalArgumentException(
                "LedgerEntry ID must be in format JNL-XXXXXXXXXXXX (12 alphanumeric characters)"
            );
        }
    }

    public static LedgerEntryId of(String value) {
        return new LedgerEntryId(value);
    }

    public static LedgerEntryId generate() {
        String random = java.util.UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 12)
            .toUpperCase();
        return new LedgerEntryId("JNL-" + random);
    }

    @Override
    public String toString() {
        return value;
    }
}
