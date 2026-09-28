package com.example.orders.domain.order.entity;

public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Customer id is required");
    }
}
