package com.jjenus.bank.core.accounts;

public enum AccountStatus {
    ACTIVE("Account is active and operational"),
    FROZEN("Account is frozen and cannot transact"),
    CLOSED("Account is closed and cannot be reactivated"),
    SUSPENDED("Account is temporarily suspended"),
    DORMANT("Account has been inactive for a long period");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransact() {
        return this == ACTIVE;
    }

    public boolean canDeposit() {
        return this == ACTIVE || this == DORMANT;
    }

    public boolean canWithdraw() {
        return this == ACTIVE;
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
