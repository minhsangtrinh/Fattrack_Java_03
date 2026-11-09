package vn.library.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import vn.library.domain.Book;
import vn.library.port.BookRepository;

public final class InMemoryBookRepository implements BookRepository {
    private final Map<UUID, Book> books = new HashMap<>();

    @Override
    public void save(Book book) {
        Objects.requireNonNull(book, "Book is required");
        books.put(book.id(), book);
    }

    @Override
    public Optional<Book> findById(UUID id) {
        return Optional.ofNullable(books.get(Objects.requireNonNull(id)));
    }
}
