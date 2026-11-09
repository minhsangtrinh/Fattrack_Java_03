package vn.library;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import vn.library.adapter.InMemoryBookRepository;
import vn.library.adapter.InMemoryLoanRepository;
import vn.library.adapter.InMemoryMemberRepository;
import vn.library.domain.Book;
import vn.library.domain.Loan;
import vn.library.domain.Member;
import vn.library.fine.DailyFinePolicy;
import vn.library.fine.FinePolicy;
import vn.library.fine.FixedFinePolicy;
import vn.library.port.BookRepository;
import vn.library.port.LoanRepository;
import vn.library.port.MemberRepository;
import vn.library.service.BookCatalogService;
import vn.library.service.LendingService;

/** Composition root: choose adapters and policies here, outside business logic. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        BookRepository books = new InMemoryBookRepository();
        MemberRepository members = new InMemoryMemberRepository();
        LoanRepository loans = new InMemoryLoanRepository();
        FinePolicy daily = new DailyFinePolicy(new BigDecimal("5000"));
        FinePolicy fixed = new FixedFinePolicy(new BigDecimal("50000"));

        BookCatalogService catalog = new BookCatalogService(books);
        Book cleanCode = catalog.addBook("Clean Code", "Robert C. Martin");
        Book effectiveJava = catalog.addBook("Effective Java", "Joshua Bloch");
        Book refactoring = catalog.addBook("Refactoring", "Martin Fowler");
        Member member = new Member(UUID.randomUUID(), "Nguyen Van An");
        members.save(member);

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Clock borrowClock = Clock.fixed(Instant.parse("2026-11-01T03:00:00Z"), zone);
        LendingService borrowing = new LendingService(books, members, loans, daily, borrowClock);
        Loan dailyLoan = borrowing.borrowBook(member.id(), cleanCode.id(), 7);
        Loan fixedLoan = borrowing.borrowBook(member.id(), effectiveJava.id(), 7);
        Loan onTimeLoan = borrowing.borrowBook(member.id(), refactoring.id(), 7);

        Clock dueClock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), zone);
        LendingService onTimeReturns = new LendingService(books, members, loans, fixed, dueClock);
        print("On-time return", onTimeReturns.returnBook(onTimeLoan.id()));

        Clock lateClock = Clock.fixed(Instant.parse("2026-11-11T03:00:00Z"), zone);
        LendingService dailyReturns = new LendingService(books, members, loans, daily, lateClock);
        LendingService fixedReturns = new LendingService(books, members, loans, fixed, lateClock);
        print("Daily fine", dailyReturns.returnBook(dailyLoan.id()));
        print("Fixed fine", fixedReturns.returnBook(fixedLoan.id()));

        Loan nextLoan = dailyReturns.borrowBook(member.id(), cleanCode.id(), 7);
        System.out.println("Returned book can be borrowed again: " + nextLoan.isActive());
    }

    private static void print(String label, Loan loan) {
        System.out.printf("%s: overdue=%d days, fine=%s VND%n",
                label, loan.overdueDays(loan.returnedOn()), loan.fine().toPlainString());
    }
}
