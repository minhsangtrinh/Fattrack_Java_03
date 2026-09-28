package com.example.orders.application.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderCommand(String customerId, String currency, List<Item> items) {
    public record Item(String sku, int quantity, BigDecimal unitPrice) {}
}
