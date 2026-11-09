package vn.library.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import vn.library.domain.Loan;
import vn.library.fine.FinePolicy;
import vn.library.port.BookRepository;
import vn.library.port.LoanRepository;
import vn.library.port.MemberRepository;

public final class LendingService {
    private final BookRepository books;
    private final MemberRepository members;
    private final LoanRepository loans;
    private final FinePolicy finePolicy;
    private final Clock clock;

    public LendingService(BookRepository books, MemberRepository members,
                          LoanRepository loans, FinePolicy finePolicy, Clock clock) {
        this.books = Objects.requireNonNull(books);
        this.members = Objects.requireNonNull(members);
        this.loans = Objects.requireNonNull(loans);
        this.finePolicy = Objects.requireNonNull(finePolicy);
        this.clock = Objects.requireNonNull(clock);
    }

    public Loan borrowBook(UUID memberId, UUID bookId, int loanDays) {
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(bookId, "Book ID is required");
        if (loanDays <= 0) {
            throw new IllegalArgumentException("Loan duration must be positive");
        }
        members.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found: " + memberId));
        books.findById(bookId)
                .orElseThrow(() -> new NoSuchElementException("Book not found: " + bookId));
        if (loans.findActiveByBookId(bookId).isPresent()) {
            throw new IllegalStateException("Book is already on loan");
        }
        Loan loan = Loan.borrow(bookId, memberId, LocalDate.now(clock), loanDays);
        loans.save(loan);
        return loan;
    }

    public Loan returnBook(UUID loanId) {
        Objects.requireNonNull(loanId, "Loan ID is required");
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan not found: " + loanId));
        if (!loan.isActive()) {
            throw new IllegalStateException("Loan has already been returned");
        }
        LocalDate returnedOn = LocalDate.now(clock);
        if (returnedOn.isBefore(loan.borrowedOn())) {
            throw new IllegalArgumentException("Return date precedes borrow date");
        }
        BigDecimal fine = finePolicy.calculate(loan.overdueDays(returnedOn));
        Loan returnedLoan = loan.close(returnedOn, fine);
        loans.save(returnedLoan);
        return returnedLoan;
    }
}
