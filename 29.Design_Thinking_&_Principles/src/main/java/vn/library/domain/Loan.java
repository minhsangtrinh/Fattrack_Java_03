package vn.library.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/** Immutable loan; returning produces a new snapshot with the same ID. */
public record Loan(UUID id, UUID bookId, UUID memberId,
                   LocalDate borrowedOn, LocalDate dueOn,
                   LocalDate returnedOn, BigDecimal fine) {
    public Loan {
        Objects.requireNonNull(id, "Loan ID is required");
        Objects.requireNonNull(bookId, "Book ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(borrowedOn, "Borrow date is required");
        Objects.requireNonNull(dueOn, "Due date is required");
        Objects.requireNonNull(fine, "Fine is required");
        if (!dueOn.isAfter(borrowedOn)) {
            throw new IllegalArgumentException("Due date must follow borrow date");
        }
        if (returnedOn != null && returnedOn.isBefore(borrowedOn)) {
            throw new IllegalArgumentException("Return date precedes borrow date");
        }
        if (fine.signum() < 0 || (returnedOn == null && fine.signum() != 0)) {
            throw new IllegalArgumentException("Invalid fine for loan state");
        }
    }

    public static Loan borrow(UUID bookId, UUID memberId, LocalDate date, int loanDays) {
        if (loanDays <= 0) {
            throw new IllegalArgumentException("Loan duration must be positive");
        }
        return new Loan(UUID.randomUUID(), bookId, memberId, date,
                date.plusDays(loanDays), null, BigDecimal.ZERO);
    }

    public boolean isActive() {
        return returnedOn == null;
    }

    public long overdueDays(LocalDate date) {
        Objects.requireNonNull(date, "Date is required");
        return Math.max(0, ChronoUnit.DAYS.between(dueOn, date));
    }

    public Loan close(LocalDate date, BigDecimal calculatedFine) {
        if (!isActive()) {
            throw new IllegalStateException("Loan has already been returned");
        }
        Objects.requireNonNull(date, "Return date is required");
        return new Loan(id, bookId, memberId, borrowedOn, dueOn, date, calculatedFine);
    }
}
