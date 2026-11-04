package com.example.perfproject.repository;

import com.example.perfproject.dto.BookView;
import com.example.perfproject.model.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("""
            select new com.example.perfproject.dto.BookView(b.id, b.title, b.publicationYear)
            from Book b where b.publicationYear = :year order by b.id
            """)
    List<BookView> findByYear(@Param("year") int year);
}
