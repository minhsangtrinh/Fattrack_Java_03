package com.example.orders.domain.order.entity;

import java.time.Instant;
import java.util.List;

public record Order(OrderId id, CustomerId customerId, List<OrderItem> items, Money total, Instant createdAt) {
    public Order {
        if (id == null || customerId == null || total == null) throw new IllegalArgumentException("Order data is required");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("An order must contain at least one item");
        items = List.copyOf(items);
        if (createdAt == null) createdAt = Instant.now();
    }
    public static Order place(OrderId id, CustomerId customerId, List<OrderItem> items, Money total) {
        return new Order(id, customerId, items, total, Instant.now());
    }
}
