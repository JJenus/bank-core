package com.jjenus.bank.core.policy;

import com.jjenus.bank.core.accounts.Account;
import com.jjenus.bank.core.accounts.AccountId;
import com.jjenus.bank.core.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class OverdraftPolicyTest {
    private static final Currency USD = Currency.getInstance("USD");

    private Account account(Currency currency) {
        return Account.create(AccountId.generate(), "Test User", currency);
    }

    @Test
    @DisplayName("none() never allows overdraft")
    void none_neverAllowsOverdraft() {
        OverdraftPolicy policy = OverdraftPolicy.none();
        Account acc = account(USD);
        Money shortfall = Money.of("100.00", USD);

        assertFalse(policy.allowsOverdraft(acc, shortfall));
        assertTrue(policy.overdraftLimit(acc).isZero());
    }

    @Test
    @DisplayName("fixedLimit() allows overdraft within limit")
    void fixedLimit_withinLimit_allowed() {
        Money limit = Money.of("500.00", USD);
        OverdraftPolicy policy = OverdraftPolicy.fixedLimit(limit);
        Account acc = account(USD);

        assertTrue(policy.allowsOverdraft(acc, Money.of("500.00", USD)));
        assertTrue(policy.allowsOverdraft(acc, Money.of("1.00", USD)));
        assertTrue(policy.allowsOverdraft(acc, Money.of("499.99", USD)));
    }

    @Test
    @DisplayName("fixedLimit() rejects overdraft exceeding limit")
    void fixedLimit_exceedsLimit_rejected() {
        Money limit = Money.of("500.00", USD);
        OverdraftPolicy policy = OverdraftPolicy.fixedLimit(limit);
        Account acc = account(USD);

        assertFalse(policy.allowsOverdraft(acc, Money.of("500.01", USD)));
        assertFalse(policy.allowsOverdraft(acc, Money.of("1000.00", USD)));
    }

    @Test
    @DisplayName("fixedLimit() returns correct overdraft limit")
    void fixedLimit_returnsCorrectLimit() {
        Money limit = Money.of("1000.00", USD);
        OverdraftPolicy policy = OverdraftPolicy.fixedLimit(limit);
        Account acc = account(USD);

        assertEquals(0, policy.overdraftLimit(acc).amount()
            .compareTo(new BigDecimal("1000.0000")));
    }

    @Test
    @DisplayName("fixedLimit() with non-positive limit throws exception")
    void fixedLimit_nonPositiveLimit_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            OverdraftPolicy.fixedLimit(Money.zero(USD))
        );
        assertThrows(IllegalArgumentException.class, () ->
            OverdraftPolicy.fixedLimit(Money.of("-100.00", USD))
        );
    }

    @Test
    @DisplayName("toString is descriptive for all built-in policies")
    void toString_descriptive() {
        assertNotNull(OverdraftPolicy.none().toString());
        assertNotNull(OverdraftPolicy.fixedLimit(Money.of("500.00", USD)).toString());
        assertFalse(OverdraftPolicy.none().toString().isEmpty());
    }
}
