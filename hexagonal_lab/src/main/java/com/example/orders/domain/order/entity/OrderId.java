package com.example.orders.domain.order.entity;

import java.util.UUID;

public record OrderId(UUID value) {
    public OrderId {
        if (value == null) throw new IllegalArgumentException("Order id is required");
    }
    public static OrderId generate() { return new OrderId(UUID.randomUUID()); }
}
