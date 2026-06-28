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

    /**
     * Whether the account can perform any transaction (withdraw or transfer out).
     * DORMANT accounts may receive deposits but cannot initiate outgoing transactions.
     */
    public boolean canTransact() {
        return this == ACTIVE;
    }

    /**
     * Whether the account can receive a deposit or incoming transfer.
     * DORMANT accounts can receive money (reactivating them implicitly via the event).
     */
    public boolean canDeposit() {
        return this == ACTIVE || this == DORMANT;
    }

    /**
     * Whether the account can process a withdrawal.
     */
    public boolean canWithdraw() {
        return this == ACTIVE;
    }

    /**
     * Whether the account is in a terminal state (no further state changes allowed).
     */
    public boolean isTerminal() {
        return this == CLOSED;
    }
}
