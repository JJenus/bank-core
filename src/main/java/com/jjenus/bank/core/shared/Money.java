package com.jjenus.bank.core.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    private static final int DEFAULT_SCALE = 4;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        // Ensure proper scaling
        amount = amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public Money add(Money other) {
        checkCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        checkCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), currency);
    }

    public boolean isGreaterThan(Money other) {
        checkCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        checkCurrency(other);
        return amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        checkCurrency(other);
        return amount.compareTo(other.amount) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        checkCurrency(other);
        return amount.compareTo(other.amount) <= 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    private void checkCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", currency, other.currency)
            );
        }
    }

    public String format() {
        return String.format("%s %.2f", currency.getSymbol(), amount);
    }

    @Override
    public String toString() {
        return format();
    }
}
