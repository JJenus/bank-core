package com.jjenus.bank.core.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency NGN = Currency.getInstance("NGN");

    @Test
    @DisplayName("Money creation with valid values")
    void createMoney_withValidValues() {
        Money money = new Money(new BigDecimal("100.50"), USD);

        assertEquals(new BigDecimal("100.5000"), money.amount());
        assertEquals(USD, money.currency());
    }

    @Test
    @DisplayName("Money.of() factory method")
    void of_factoryMethod() {
        Money money = Money.of("150.75", USD);

        assertEquals(new BigDecimal("150.7500"), money.amount());
        assertEquals(USD, money.currency());
    }

    @Test
    @DisplayName("Zero money creation")
    void zero_money() {
        Money zero = Money.zero(USD);

        assertEquals(0, zero.amount().compareTo(BigDecimal.ZERO));
        assertEquals(USD, zero.currency());
    }

    @Test
    @DisplayName("Add money with same currency")
    void add_sameCurrency() {
        Money money1 = Money.of("100.00", USD);
        Money money2 = Money.of("50.00", USD);

        Money result = money1.add(money2);

        assertEquals(new BigDecimal("150.0000"), result.amount());
        assertEquals(USD, result.currency());
    }

    @Test
    @DisplayName("Add money with different currency throws exception")
    void add_differentCurrency_throwsException() {
        Money money1 = Money.of("100.00", USD);
        Money money2 = Money.of("50.00", EUR);

        assertThrows(IllegalArgumentException.class, () -> money1.add(money2));
    }

    @Test
    @DisplayName("Subtract money")
    void subtract_money() {
        Money money1 = Money.of("100.00", USD);
        Money money2 = Money.of("30.00", USD);

        Money result = money1.subtract(money2);

        assertEquals(new BigDecimal("70.0000"), result.amount());
        assertEquals(USD, result.currency());
    }

    @Test
    @DisplayName("Multiply money")
    void multiply_money() {
        Money money = Money.of("100.00", USD);
        BigDecimal multiplier = new BigDecimal("1.5");

        Money result = money.multiply(multiplier);

        assertEquals(new BigDecimal("150.0000"), result.amount());
        assertEquals(USD, result.currency());
    }

    @Test
    @DisplayName("Negate money")
    void negate_money() {
        Money money = Money.of("100.00", USD);

        Money result = money.negate();

        assertEquals(new BigDecimal("-100.0000"), result.amount());
        assertEquals(USD, result.currency());
    }

    @ParameterizedTest
    @CsvSource({
            "100.00, 50.00, true",
            "50.00, 50.00, true",
            "30.00, 50.00, false"
    })
    @DisplayName("Is greater than or equal")
    void isGreaterThanOrEqual(String amount1, String amount2, boolean expected) {
        Money money1 = Money.of(amount1, USD);
        Money money2 = Money.of(amount2, USD);

        assertEquals(expected, money1.isGreaterThanOrEqual(money2));
    }

    @Test
    @DisplayName("Is positive")
    void isPositive() {
        Money positive = Money.of("100.00", USD);
        Money negative = Money.of("-100.00", USD);
        Money zero = Money.zero(USD);

        assertTrue(positive.isPositive());
        assertFalse(negative.isPositive());
        assertFalse(zero.isPositive());
    }

    @Test
    @DisplayName("Is negative")
    void isNegative() {
        Money positive = Money.of("100.00", USD);
        Money negative = Money.of("-100.00", USD);
        Money zero = Money.zero(USD);

        assertFalse(positive.isNegative());
        assertTrue(negative.isNegative());
        assertFalse(zero.isNegative());
    }

    @Test
    @DisplayName("Is zero")
    void isZero() {
        Money positive = Money.of("100.00", USD);
        Money negative = Money.of("-100.00", USD);
        Money zero = Money.zero(USD);

        assertFalse(positive.isZero());
        assertFalse(negative.isZero());
        assertTrue(zero.isZero());
    }

    @Test
    @DisplayName("Format uses currency code, not locale-sensitive symbol")
    void format_usesCurrencyCode() {
        Money usd = Money.of("1234.56", USD);
        Money eur = Money.of("999.00", EUR);
        Money ngn = Money.of("50000.00", NGN);

        // format() uses getCurrencyCode() — locale-independent, always the ISO 4217 code
        assertTrue(usd.format().contains("USD"));
        assertTrue(usd.format().contains("1234.56"));

        assertTrue(eur.format().contains("EUR"));
        assertTrue(eur.format().contains("999.00"));

        assertTrue(ngn.format().contains("NGN"));
        assertTrue(ngn.format().contains("50000.00"));
    }

    @Test
    @DisplayName("Format does not use locale-sensitive currency symbol")
    void format_notLocaleSensitive() {
        // getCurrencyCode() always returns "USD", never "$" or "US$" depending on JVM locale
        Money money = Money.of("100.00", USD);
        String formatted = money.format();

        assertFalse(formatted.contains("$"),
            "format() should use currency code (USD), not locale-sensitive symbol ($)");
        assertTrue(formatted.contains("USD"));
    }

    @Test
    @DisplayName("Money with null amount throws exception")
    void createMoney_withNullAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Money(null, USD));
    }

    @Test
    @DisplayName("Money with null currency throws exception")
    void createMoney_withNullCurrency_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("100.00"), null));
    }

    @Test
    @DisplayName("Is less than")
    void isLessThan() {
        Money smaller = Money.of("50.00", USD);
        Money larger = Money.of("100.00", USD);

        assertTrue(smaller.isLessThan(larger));
        assertFalse(larger.isLessThan(smaller));
        assertFalse(smaller.isLessThan(smaller));
    }

    @Test
    @DisplayName("Is less than or equal")
    void isLessThanOrEqual() {
        Money smaller = Money.of("50.00", USD);
        Money larger = Money.of("100.00", USD);

        assertTrue(smaller.isLessThanOrEqual(larger));
        assertTrue(smaller.isLessThanOrEqual(smaller));
        assertFalse(larger.isLessThanOrEqual(smaller));
    }

    @Test
    @DisplayName("Is greater than")
    void isGreaterThan() {
        Money larger = Money.of("100.00", USD);
        Money smaller = Money.of("50.00", USD);

        assertTrue(larger.isGreaterThan(smaller));
        assertFalse(smaller.isGreaterThan(larger));
        assertFalse(larger.isGreaterThan(larger));
    }
}
