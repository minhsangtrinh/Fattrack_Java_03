package com.example.perfproject;

import com.example.perfproject.dto.BookView;
import com.example.perfproject.repository.AuthorRepository;
import com.example.perfproject.repository.BookRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final AuthorRepository authors;
    private final BookRepository books;

    public CatalogService(AuthorRepository authors, BookRepository books) {
        this.authors = authors;
        this.books = books;
    }

    public List<AuthorView> authorsWithBooks() {
        return authors.findAllWithBooks().stream()
                .map(author -> new AuthorView(author.getId(), author.getName(),
                        author.getBooks().stream()
                                .map(book -> new BookView(book.getId(), book.getTitle(),
                                        book.getPublicationYear()))
                                .toList()))
                .toList();
    }

    public List<BookView> booksByYear(int year) {
        return books.findByYear(year);
    }

    public record AuthorView(Long id, String name, List<BookView> books) {}
}
