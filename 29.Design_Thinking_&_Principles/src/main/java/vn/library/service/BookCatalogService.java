package vn.library.service;

import java.util.Objects;
import java.util.UUID;
import vn.library.domain.Book;
import vn.library.port.BookRepository;

public final class BookCatalogService {
    private final BookRepository books;

    public BookCatalogService(BookRepository books) {
        this.books = Objects.requireNonNull(books);
    }

    public Book addBook(String title, String author) {
        Book book = new Book(UUID.randomUUID(), title, author);
        books.save(book);
        return book;
    }
}
