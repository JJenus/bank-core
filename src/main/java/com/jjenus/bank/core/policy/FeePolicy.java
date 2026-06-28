package com.jjenus.bank.core.policy;

import com.jjenus.bank.core.accounts.Account;
import com.jjenus.bank.core.shared.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Domain policy defining how fees are calculated for account and transfer operations.
 *
 * <p>Implementations live in the application or infrastructure layer and are passed
 * into domain services. The domain depends on this interface as an output port,
 * never on a concrete implementation.
 *
 * <p>Example implementations:
 * <ul>
 *   <li>{@code NoFeePolicy} — always returns zero (useful for internal/interbank transfers)</li>
 *   <li>{@code PercentageFeePolicy} — charges a percentage of the transfer amount with min/max caps</li>
 *   <li>{@code FlatFeePolicy} — charges a fixed fee regardless of amount</li>
 * </ul>
 */
public interface FeePolicy {

    /**
     * Calculates the fee for a transfer from {@code account} of {@code transferAmount}.
     *
     * @param account        the account being charged
     * @param transferAmount the gross transfer amount (always positive)
     * @return the fee to deduct from the account (zero if no fee applies)
     */
    Money calculateTransferFee(Account account, Money transferAmount);

    /**
     * Returns a human-readable description of this fee policy,
     * suitable for audit logs and transaction descriptions.
     */
    String description();

    // ── Built-in policies ─────────────────────────────────────────────────────

    /**
     * No fee — always returns zero. Suitable for internal transfers or test scenarios.
     */
    static FeePolicy none() {
        return new FeePolicy() {
            @Override
            public Money calculateTransferFee(Account account, Money transferAmount) {
                return Money.zero(transferAmount.currency());
            }

            @Override
            public String description() {
                return "No fee";
            }

            @Override
            public String toString() {
                return "FeePolicy.none()";
            }
        };
    }

    /**
     * Percentage-based fee with optional minimum and maximum caps.
     *
     * <p>Fee = max(min, min(max, transferAmount * rate))
     *
     * @param rate the fee rate as a decimal (e.g., 0.001 for 0.1%)
     * @param min  the minimum fee amount (inclusive floor); use {@code Money.zero()} for no floor
     * @param max  the maximum fee amount (inclusive ceiling); use a very large value for no ceiling
     */
    static FeePolicy percentage(BigDecimal rate, Money min, Money max) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee rate must be non-negative");
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Fee rate cannot exceed 100% (1.0)");
        }
        if (min == null) throw new IllegalArgumentException("Minimum fee cannot be null");
        if (max == null) throw new IllegalArgumentException("Maximum fee cannot be null");
        if (!min.currency().equals(max.currency())) {
            throw new IllegalArgumentException("Min and max fee must use the same currency");
        }
        if (min.isGreaterThan(max)) {
            throw new IllegalArgumentException("Minimum fee cannot exceed maximum fee");
        }

        return new FeePolicy() {
            @Override
            public Money calculateTransferFee(Account account, Money transferAmount) {
                if (!transferAmount.currency().equals(min.currency())) {
                    throw new IllegalArgumentException(
                        String.format("Transfer currency %s does not match fee currency %s",
                            transferAmount.currency(), min.currency())
                    );
                }

                Money computed = transferAmount.multiply(rate.setScale(10, RoundingMode.HALF_EVEN));

                // Apply floor
                if (computed.isLessThan(min)) {
                    computed = min;
                }

                // Apply ceiling
                if (computed.isGreaterThan(max)) {
                    computed = max;
                }

                return computed;
            }

            @Override
            public String description() {
                return String.format("%.4f%% fee (min %s, max %s)",
                    rate.multiply(BigDecimal.valueOf(100)), min.format(), max.format());
            }

            @Override
            public String toString() {
                return "FeePolicy.percentage(" + rate + ", min=" + min + ", max=" + max + ")";
            }
        };
    }

    /**
     * Flat fee — charges the same amount regardless of transfer size.
     *
     * @param flatAmount the fixed fee (must be positive)
     */
    static FeePolicy flat(Money flatAmount) {
        if (!flatAmount.isPositive()) {
            throw new IllegalArgumentException("Flat fee must be positive");
        }
        return new FeePolicy() {
            @Override
            public Money calculateTransferFee(Account account, Money transferAmount) {
                return flatAmount;
            }

            @Override
            public String description() {
                return "Flat fee: " + flatAmount.format();
            }

            @Override
            public String toString() {
                return "FeePolicy.flat(" + flatAmount + ")";
            }
        };
    }

    /**
     * Convenience factory: Nigerian inter-bank transfer fee style.
     * 0.1% of transfer amount, minimum ₦10, maximum ₦2,000.
     */
    static FeePolicy nigerianInterbank() {
        Currency ngn = Currency.getInstance("NGN");
        return percentage(
            new BigDecimal("0.001"),
            Money.of("10.00", ngn),
            Money.of("2000.00", ngn)
        );
    }
}
