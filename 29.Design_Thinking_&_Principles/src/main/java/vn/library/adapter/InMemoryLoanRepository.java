package vn.library.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import vn.library.domain.Loan;
import vn.library.port.LoanRepository;

public final class InMemoryLoanRepository implements LoanRepository {
    private final Map<UUID, Loan> loans = new HashMap<>();

    @Override
    public void save(Loan loan) {
        Objects.requireNonNull(loan, "Loan is required");
        loans.put(loan.id(), loan);
    }

    @Override
    public Optional<Loan> findById(UUID id) {
        return Optional.ofNullable(loans.get(Objects.requireNonNull(id)));
    }

    @Override
    public Optional<Loan> findActiveByBookId(UUID bookId) {
        Objects.requireNonNull(bookId, "Book ID is required");
        return loans.values().stream()
                .filter(loan -> loan.bookId().equals(bookId) && loan.isActive())
                .findFirst();
    }
}
