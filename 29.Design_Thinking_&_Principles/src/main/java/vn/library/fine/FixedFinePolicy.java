package vn.library.fine;

import java.math.BigDecimal;
import java.util.Objects;

public final class FixedFinePolicy implements FinePolicy {
    private final BigDecimal amount;

    public FixedFinePolicy(BigDecimal amount) {
        this.amount = Objects.requireNonNull(amount, "Fixed amount is required");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Fixed amount must not be negative");
        }
    }

    @Override
    public BigDecimal calculate(long overdueDays) {
        if (overdueDays < 0) {
            throw new IllegalArgumentException("Overdue days must not be negative");
        }
        return overdueDays == 0 ? BigDecimal.ZERO : amount;
    }
}
