package com.jjenus.bank.core.policy;

import com.jjenus.bank.core.accounts.Account;
import com.jjenus.bank.core.shared.Money;

/**
 * Domain policy defining whether an account may withdraw beyond its available balance.
 *
 * <p>Implementations live in the application or infrastructure layer and are injected
 * into domain operations that need overdraft decisions. The domain depends on this
 * interface, not on any concrete policy.
 *
 * <p>Example implementations:
 * <ul>
 *   <li>{@code NoOverdraftPolicy} — always returns false (default / current behaviour)</li>
 *   <li>{@code FixedLimitOverdraftPolicy} — allows overdraft up to a fixed monetary limit</li>
 *   <li>{@code AccountTypePolicyRouter} — delegates to a policy based on account type</li>
 * </ul>
 */
public interface OverdraftPolicy {

    /**
     * Returns true if {@code account} is permitted to overdraw by {@code shortfall}.
     *
     * @param account   the account attempting the withdrawal
     * @param shortfall the amount by which the withdrawal exceeds the current balance (always positive)
     * @return true if overdraft is allowed up to this shortfall, false otherwise
     */
    boolean allowsOverdraft(Account account, Money shortfall);

    /**
     * The maximum overdraft amount permitted for this account.
     * Returns {@code Money.zero(account.getCurrency())} if overdraft is not allowed.
     */
    Money overdraftLimit(Account account);

    // ── Built-in policies ─────────────────────────────────────────────────────

    /**
     * No overdraft allowed — hard stops at zero balance (current default behaviour).
     */
    static OverdraftPolicy none() {
        return new OverdraftPolicy() {
            @Override
            public boolean allowsOverdraft(Account account, Money shortfall) {
                return false;
            }

            @Override
            public Money overdraftLimit(Account account) {
                return Money.zero(account.getCurrency());
            }

            @Override
            public String toString() {
                return "OverdraftPolicy.none()";
            }
        };
    }

    /**
     * Fixed overdraft limit applied uniformly to all accounts.
     *
     * @param limit the maximum overdraft amount (must be positive)
     */
    static OverdraftPolicy fixedLimit(Money limit) {
        if (!limit.isPositive()) {
            throw new IllegalArgumentException("Overdraft limit must be positive");
        }
        return new OverdraftPolicy() {
            @Override
            public boolean allowsOverdraft(Account account, Money shortfall) {
                return shortfall.isLessThanOrEqual(limit);
            }

            @Override
            public Money overdraftLimit(Account account) {
                return limit;
            }

            @Override
            public String toString() {
                return "OverdraftPolicy.fixedLimit(" + limit.format() + ")";
            }
        };
    }
}
