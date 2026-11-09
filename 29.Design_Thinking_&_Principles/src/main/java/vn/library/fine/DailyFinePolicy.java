package vn.library.fine;

import java.math.BigDecimal;
import java.util.Objects;

public final class DailyFinePolicy implements FinePolicy {
    private final BigDecimal dailyRate;

    public DailyFinePolicy(BigDecimal dailyRate) {
        this.dailyRate = Objects.requireNonNull(dailyRate, "Daily rate is required");
        if (dailyRate.signum() < 0) {
            throw new IllegalArgumentException("Daily rate must not be negative");
        }
    }

    @Override
    public BigDecimal calculate(long overdueDays) {
        if (overdueDays < 0) {
            throw new IllegalArgumentException("Overdue days must not be negative");
        }
        return dailyRate.multiply(BigDecimal.valueOf(overdueDays));
    }
}
