package com.example.perfproject;

import com.example.perfproject.dto.BookView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping("/authors")
    public List<CatalogService.AuthorView> authors() {
        return service.authorsWithBooks();
    }

    @GetMapping("/books")
    public List<BookView> books(@RequestParam("year") int year) {
        return service.booksByYear(year);
    }
}
