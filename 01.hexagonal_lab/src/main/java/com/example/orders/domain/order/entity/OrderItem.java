package com.example.orders.domain.order.entity;

public record OrderItem(String sku, int quantity, Money unitPrice) {
    public OrderItem {
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU is required");
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be positive");
        if (unitPrice == null) throw new IllegalArgumentException("Unit price is required");
    }
    public Money subtotal() { return unitPrice.multiply(quantity); }
}
