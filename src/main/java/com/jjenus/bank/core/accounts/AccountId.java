package com.jjenus.bank.core.accounts;

public record AccountId(String value) {

    public AccountId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be blank");
        }
        if (!value.matches("^ACC-[A-Z0-9]{10}$")) {
            throw new IllegalArgumentException(
                "Account ID must be in format ACC-XXXXXXXXXX (10 alphanumeric characters)"
            );
        }
    }

    public static AccountId of(String value) {
        return new AccountId(value);
    }

    public static AccountId generate() {
        String random = java.util.UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 10)
            .toUpperCase();
        return new AccountId("ACC-" + random);
    }

    @Override
    public String toString() {
        return value;
    }
}
