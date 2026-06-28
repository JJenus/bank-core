package com.jjenus.bank.core.policy;

import com.jjenus.bank.core.accounts.Account;
import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class FeePolicyTest {
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency NGN = Currency.getInstance("NGN");

    private Account account(Currency currency) {
        return Account.create(AccountId.generate(), "Test User", currency);
    }

    @Test
    @DisplayName("none() always returns zero fee")
    void none_alwaysZero() {
        FeePolicy policy = FeePolicy.none();
        Account acc = account(USD);

        Money fee = policy.calculateTransferFee(acc, Money.of("1000.00", USD));

        assertTrue(fee.isZero());
        assertEquals(USD, fee.currency());
    }

    @Test
    @DisplayName("flat() returns same fee regardless of amount")
    void flat_alwaysReturnsFlatAmount() {
        FeePolicy policy = FeePolicy.flat(Money.of("50.00", USD));
        Account acc = account(USD);

        assertEquals(0, policy.calculateTransferFee(acc, Money.of("100.00", USD)).amount()
            .compareTo(new BigDecimal("50.0000")));
        assertEquals(0, policy.calculateTransferFee(acc, Money.of("10000.00", USD)).amount()
            .compareTo(new BigDecimal("50.0000")));
    }

    @Test
    @DisplayName("flat() with zero amount throws exception")
    void flat_zeroAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            FeePolicy.flat(Money.zero(USD))
        );
    }

    @Test
    @DisplayName("percentage() calculates correctly within min/max range")
    void percentage_withinRange() {
        // 1% fee, min $10, max $500
        FeePolicy policy = FeePolicy.percentage(
            new BigDecimal("0.01"),
            Money.of("10.00", USD),
            Money.of("500.00", USD)
        );
        Account acc = account(USD);

        // 1% of $1000 = $10 (at min boundary)
        Money fee1 = policy.calculateTransferFee(acc, Money.of("1000.00", USD));
        assertEquals(0, fee1.amount().compareTo(new BigDecimal("10.0000")));

        // 1% of $5000 = $50 (within range)
        Money fee2 = policy.calculateTransferFee(acc, Money.of("5000.00", USD));
        assertEquals(0, fee2.amount().compareTo(new BigDecimal("50.0000")));
    }

    @Test
    @DisplayName("percentage() applies minimum floor")
    void percentage_appliesMinFloor() {
        FeePolicy policy = FeePolicy.percentage(
            new BigDecimal("0.001"),   // 0.1%
            Money.of("50.00", USD),    // min $50
            Money.of("2000.00", USD)   // max $2000
        );
        Account acc = account(USD);

        // 0.1% of $100 = $0.10 — below min, so min applies
        Money fee = policy.calculateTransferFee(acc, Money.of("100.00", USD));
        assertEquals(0, fee.amount().compareTo(new BigDecimal("50.0000")));
    }

    @Test
    @DisplayName("percentage() applies maximum ceiling")
    void percentage_appliesMaxCeiling() {
        FeePolicy policy = FeePolicy.percentage(
            new BigDecimal("0.001"),
            Money.of("10.00", USD),
            Money.of("2000.00", USD)
        );
        Account acc = account(USD);

        // 0.1% of $5,000,000 = $5000 — above max, so max applies
        Money fee = policy.calculateTransferFee(acc, Money.of("5000000.00", USD));
        assertEquals(0, fee.amount().compareTo(new BigDecimal("2000.0000")));
    }

    @Test
    @DisplayName("percentage() with rate > 100% throws exception")
    void percentage_rateOverHundredPercent_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            FeePolicy.percentage(
                new BigDecimal("1.5"),
                Money.of("0.00", USD),
                Money.of("1000.00", USD)
            )
        );
    }

    @Test
    @DisplayName("percentage() with min > max throws exception")
    void percentage_minGreaterThanMax_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            FeePolicy.percentage(
                new BigDecimal("0.01"),
                Money.of("500.00", USD),
                Money.of("10.00", USD)   // max < min
            )
        );
    }

    @Test
    @DisplayName("nigerianInterbank() policy applies NGN constraints")
    void nigerianInterbank_appliesCorrectly() {
        FeePolicy policy = FeePolicy.nigerianInterbank();
        Account acc = account(NGN);

        // 0.1% of ₦5000 = ₦5 — below ₦10 minimum
        Money feeSmall = policy.calculateTransferFee(acc, Money.of("5000.00", NGN));
        assertEquals(0, feeSmall.amount().compareTo(new BigDecimal("10.0000")));

        // 0.1% of ₦100000 = ₦100 — within range
        Money feeMid = policy.calculateTransferFee(acc, Money.of("100000.00", NGN));
        assertEquals(0, feeMid.amount().compareTo(new BigDecimal("100.0000")));

        // 0.1% of ₦5000000 = ₦5000 — above ₦2000 maximum
        Money feeLarge = policy.calculateTransferFee(acc, Money.of("5000000.00", NGN));
        assertEquals(0, feeLarge.amount().compareTo(new BigDecimal("2000.0000")));
    }

    @Test
    @DisplayName("All built-in policies return non-null description")
    void allPolicies_haveDescription() {
        assertNotNull(FeePolicy.none().description());
        assertNotNull(FeePolicy.flat(Money.of("10.00", USD)).description());
        assertNotNull(FeePolicy.percentage(
            new BigDecimal("0.01"),
            Money.zero(USD),
            Money.of("1000.00", USD)
        ).description());
        assertNotNull(FeePolicy.nigerianInterbank().description());

        assertFalse(FeePolicy.none().description().isEmpty());
    }
}
