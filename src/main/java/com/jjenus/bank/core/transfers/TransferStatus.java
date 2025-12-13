package com.jjenus.bank.core.transfers;

enum TransferStatus {
    PENDING("Transfer is pending execution"),
    PROCESSING("Transfer is being processed"),
    COMPLETED("Transfer completed successfully"),
    FAILED("Transfer failed"),
    REVERSED("Transfer was reversed"),
    CANCELLED("Transfer was cancelled");

    private final String description;

    TransferStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED ||
                this == FAILED ||
                this == REVERSED ||
                this == CANCELLED;
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}
