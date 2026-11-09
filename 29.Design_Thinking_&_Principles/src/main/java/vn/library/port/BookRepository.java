package vn.library.port;

import java.util.Optional;
import java.util.UUID;
import vn.library.domain.Book;

/** Saves by ID; findById returns empty when the book does not exist. */
public interface BookRepository {
    void save(Book book);
    Optional<Book> findById(UUID id);
}
