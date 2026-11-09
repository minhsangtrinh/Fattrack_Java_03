package vn.library.domain;

import java.util.Objects;
import java.util.UUID;

/** One physical copy; books with the same title may have different IDs. */
public record Book(UUID id, String title, String author) {
    public Book {
        Objects.requireNonNull(id, "Book ID is required");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("Author is required");
        }
        title = title.strip();
        author = author.strip();
    }
}
