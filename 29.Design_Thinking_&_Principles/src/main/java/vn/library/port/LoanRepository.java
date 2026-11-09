package vn.library.port;

import java.util.Optional;
import java.util.UUID;
import vn.library.domain.Loan;

public interface LoanRepository {
    /** Inserts a new loan or replaces the snapshot with the same ID. */
    void save(Loan loan);
    Optional<Loan> findById(UUID id);
    Optional<Loan> findActiveByBookId(UUID bookId);
}
