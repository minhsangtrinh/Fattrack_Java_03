package vn.library.fine;

import java.math.BigDecimal;

/**
 * All implementations accept overdueDays >= 0, return a non-null,
 * non-negative amount, and return zero when overdueDays == 0.
 * Negative input must throw IllegalArgumentException.
 */
public interface FinePolicy {
    BigDecimal calculate(long overdueDays);
}
