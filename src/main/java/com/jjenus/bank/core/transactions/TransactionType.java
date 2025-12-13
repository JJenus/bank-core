package com.jjenus.bank.core.transactions;

public enum TransactionType {
    DEPOSIT("Money deposited into account"),
    WITHDRAWAL("Money withdrawn from account"),
    TRANSFER_OUT("Money transferred out to another account"),
    TRANSFER_IN("Money transferred in from another account"),
    FEE("Account fee charged"),
    INTEREST("Interest credited"),
    REFUND("Transaction refund"),
    REVERSAL("Transaction reversal");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCredit() {
        return this == DEPOSIT ||
               this == TRANSFER_IN ||
               this == INTEREST ||
               this == REFUND;
    }

    public boolean isDebit() {
        return this == WITHDRAWAL ||
               this == TRANSFER_OUT ||
               this == FEE ||
               this == REVERSAL;
    }

    public boolean isTransferRelated() {
        return this == TRANSFER_IN || this == TRANSFER_OUT;
    }
}
