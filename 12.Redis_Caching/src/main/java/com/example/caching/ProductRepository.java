package com.example.caching;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Product> findById(long id) {
        return jdbc.query("SELECT id, name, price FROM products WHERE id = ?",
                (row, index) -> new Product(row.getLong("id"), row.getString("name"),
                        row.getBigDecimal("price")),
                id).stream().findFirst();
    }

    public int deleteById(long id) {
        return jdbc.update("DELETE FROM products WHERE id = ?", id);
    }
}
